package com.example.mosim.feature.escort

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mosim.data.EscortSession
import com.example.mosim.data.EscortSummary
import com.example.mosim.data.passenger.EscortTrip
import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.data.place.PlaceKind
import com.example.mosim.data.place.PlaceSearchSource
import com.example.mosim.feature.escort.command.VoiceCommand
import com.example.mosim.feature.escort.command.VoiceCommandParser
import com.example.mosim.platform.pleos.CabinCapabilities
import com.example.mosim.platform.pleos.CabinControl
import com.example.mosim.platform.pleos.CabinState
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.CabinFeature
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.RoadType
import com.example.mosim.platform.pleos.SourceResult
import com.example.mosim.platform.pleos.SpeechInput
import com.example.mosim.platform.pleos.TripSource
import com.example.mosim.platform.pleos.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import kotlin.math.round

/**
 * 에스코트 모드(E-1 · E-2 · F-1)의 상태와 판단을 맡는다.
 *
 * 하는 일은 세 가지다.
 * 1. 음성 문장 → 명령 → 실내 설정 자동 실행 (동의 범위 안일 때만)
 * 2. 장소 요청이나 지연 감지 → 경유 제안(F-1). 경로 변경은 자동 실행하지 않고 운전자 승인을 기다린다.
 * 3. 실행 결과를 성공 / 실패 / 건너뜀, 그리고 Mock 여부로 기록한다(project-status.md 안전 규칙).
 *
 * [scope]를 생성자로 받는 이유: 테스트에서 메인 스레드 없이 범위를 넘기기 위해서다.
 * ViewModel(CoroutineScope) 생성자에 넘기면 viewModelScope가 이 범위가 되고, ViewModel이 끝날 때 취소된다.
 */
class EscortViewModel(
    scope: CoroutineScope,
    private val trip: EscortTrip,
    private val speechInput: SpeechInput,
    private val cabinControl: CabinControl,
    private val tripSource: TripSource,
    private val placeSearch: PlaceSearchSource,
    private val alerts: EscortAlerts,
    /** 경로·장소 쪽이 Mock인지. 실내 설정은 cabinControl.simulatedFeatures로 따로 본다. */
    private val tripIsSimulated: Boolean,
    private val noticeDurationMillis: Long = NOTICE_DURATION_MILLIS,
    private val delayThresholdMinutes: Long = DELAY_THRESHOLD_MINUTES,
) : ViewModel(scope) {

    private val mutableState = MutableStateFlow(initialState())
    val uiState: StateFlow<EscortUiState> = mutableState.asStateFlow()

    private var cachedCapabilities: CabinCapabilities? = null
    private var nextNoticeId = 0L
    private var nextProposalId = 0L

    /** 경유지로 실제 추가에 성공한 장소. G-1 요약에 쓴다. 실패한 요청은 넣지 않는다. */
    private val addedWaypoints = mutableListOf<String>()

    /** 동승자 음성 요청을 처리한 횟수. G-1 요약에 쓴다. */
    private var voiceRequestCount = 0

    /** 지연을 비교할 기준 도착 시각. 제안에 답하면 그때 시각으로 다시 잡아 같은 지연을 두 번 묻지 않는다. */
    private var baselineEta: LocalTime? = null
    private var baselineMinutes: Int? = null
    private var delayCheckJob: Job? = null

    /** 마지막으로 받은 도로 종류. 휴식 장소 기준이 달라진다. */
    private var roadType: RoadType? = null

    /** 마지막 장소 검색에 쓴 위치. 제안에 붙여 F-1 지도에 현재 위치로 찍는다. */
    private var lastSearchOrigin: GeoPoint? = null

    init {
        viewModelScope.launch { cabinControl.state.collect(::applyCabinState) }
        viewModelScope.launch { tripSource.tripStatus.collect(::onTripStatus) }
        viewModelScope.launch { ensureRoute() }
        // 명령은 들어온 순서대로 하나씩 처리한다. 이전 조절이 끝나기 전에 다음 조절이 끼어들지 않게 하기 위해서다.
        viewModelScope.launch { speechInput.recognizedTexts.collect { text -> handleSpeech(text) } }
    }

    // ---- 화면이 부르는 동작 ----

    /** F-1 "○○ 경유". 경유지 추가가 성공해야 함께 처리할 설정을 실행한다. */
    fun acceptProposal() {
        viewModelScope.launch { acceptProposalAndWait() }
    }

    /**
     * [acceptProposal]과 같은 일을 하되 결과를 기다려 돌려준다.
     * F-1 화면이 "요청 중 → 성공/실패"를 보여 주려면 결과가 필요해서 둔다.
     * **true는 내비가 경유지 추가를 받아들였다는 뜻이다.** 실패를 성공으로 바꾸지 않는다.
     */
    suspend fun acceptProposalAndWait(): Boolean {
        val proposal = mutableState.value.proposal ?: return false
        if (!proposal.canAddWaypoint) return false
        updateProposal { it.copy(isSubmitting = true, errorMessage = null) }

        val place = proposal.place
        return when (val result = tripSource.addWaypoint(place.name, place.location)) {
            is SourceResult.Success -> {
                addedWaypoints += place.name
                proposal.bundledActions.forEach { runBundledAction(it) }
                closeProposal()
                report(
                    "경유지 추가: ${place.name}",
                    ActionOutcome.SUCCEEDED,
                    EscortMessages.waypointAdded(place.name),
                )
                true
            }
            is SourceResult.Failure -> {
                record("경유지 추가: ${place.name}", ActionOutcome.FAILED)
                updateProposal {
                    it.copy(isSubmitting = false, errorMessage = EscortMessages.failure(result.reason))
                }
                false
            }
        }
    }

    /** F-1 "기존 경로 유지" */
    fun keepCurrentRoute() {
        val proposal = mutableState.value.proposal ?: return
        if (proposal.isSubmitting) return
        record("경유 제안 거절: ${proposal.place.name}", ActionOutcome.SKIPPED)
        closeProposal()
    }

    /**
     * F-2 화면이 넘긴 최종 인식 문장 하나를 처리한다.
     *
     * 장소 요청이면 화면(F-3)에서 후보를 고르게 [VoiceRouting.SearchPlaces]를 돌려주고,
     * 그 밖의 명령은 여기서 실행한 뒤 결과를 E-1 토스트로 알린다([VoiceRouting.Handled]).
     * 해석하지 못한 문장도 Handled이며 성공으로 처리하지 않는다(안내 문구만 바뀐다).
     */
    suspend fun handleVoiceText(text: String): VoiceRouting {
        voiceRequestCount++
        return when (VoiceCommandParser.parse(text)) {
            is VoiceCommand.FindPlace -> VoiceRouting.SearchPlaces(text)
            else -> {
                handleSpeech(text)
                VoiceRouting.Handled(mutableState.value.notice?.message)
            }
        }
    }

    /**
     * F-3이 쓰는 장소 검색이다. E-1의 음성 제안과 같은 경로(현재 위치 + PlaceSearchSource)를 쓴다.
     * 장소 종류를 못 알아들으면 검색하지 않고 이유를 돌려준다.
     */
    suspend fun searchPlacesFor(query: String): PlaceSearchOutcome {
        val kind = (VoiceCommandParser.parse(query) as? VoiceCommand.FindPlace)?.kind
            ?: return PlaceSearchOutcome(null, null, EscortMessages.NOT_UNDERSTOOD)
        val location = when (val result = tripSource.currentLocation()) {
            is SourceResult.Success -> result.value.also { lastSearchOrigin = it }
            is SourceResult.Failure -> return PlaceSearchOutcome(null, null, EscortMessages.LOCATION_UNAVAILABLE)
        }
        return when (val result = placeSearch.searchNearby(kind, location, SEARCH_RADIUS_METERS)) {
            is SourceResult.Success -> PlaceSearchOutcome(location, result.value, null)
            is SourceResult.Failure -> PlaceSearchOutcome(location, null, EscortMessages.failure(result.reason))
        }
    }

    /**
     * F-3에서 고른 장소를 경유지로 추가한다. **실제로 성공했을 때만 true다.**
     * 실패는 이유를 토스트로 알리고 false를 돌려준다. 화면은 다시 시도하거나 취소할 수 있다.
     */
    suspend fun addWaypointAt(name: String, location: GeoPoint): Boolean =
        when (val result = tripSource.addWaypoint(name, location)) {
            is SourceResult.Success -> {
                addedWaypoints += name
                report("경유지 추가: $name", ActionOutcome.SUCCEEDED, EscortMessages.waypointAdded(name))
                true
            }
            is SourceResult.Failure -> {
                record("경유지 추가: $name", ActionOutcome.FAILED)
                showNotice(EscortMessages.failure(result.reason))
                false
            }
        }

    /** 도착했다. 이번 에스코트에서 실제로 한 일만 모아 G-1이 읽을 자리에 남긴다. */
    private fun finishEscort() {
        val state = mutableState.value
        EscortSession.summary = EscortSummary(
            passengerName = trip.passenger.name,
            destinationName = trip.destinationName,
            waypointNames = addedWaypoints.toList(),
            autoAdjustCount = state.history.count {
                it.outcome == ActionOutcome.SUCCEEDED && !it.description.startsWith("경유지 추가")
            },
            voiceRequestCount = voiceRequestCount,
            isSimulated = tripIsSimulated || state.history.any { it.isSimulated },
        )
        mutableState.update { it.copy(arrived = true) }
    }

    // ---- 음성 명령 ----

    private suspend fun handleSpeech(text: String) {
        when (val command = VoiceCommandParser.parse(text)) {
            is VoiceCommand.AdjustTemperature ->
                withCurrentTemperature { current -> changeTemperature(current, current + command.deltaCelsius) }
            is VoiceCommand.SetTemperature ->
                withCurrentTemperature { current -> changeTemperature(current, command.targetCelsius) }
            is VoiceCommand.AdjustVolume -> changeVolume(command.delta)
            is VoiceCommand.AdjustBackrest -> changeBackrest(command.deltaDegrees)
            is VoiceCommand.FindPlace -> proposePlace(command.kind, text)
            is VoiceCommand.Unknown -> showNotice(EscortMessages.NOT_UNDERSTOOD, detail = text)
        }
    }

    /**
     * 공조를 쓸 수 있게 준비한 뒤 현재 목표 온도를 넘긴다.
     * 실제 차량은 공조가 꺼져 있으면 온도를 읽지 못해 먼저 켠다. 켰으면 그것도 실행 기록에 남긴다.
     * 화면 상태(mutableState)가 아니라 cabinControl.state를 읽는 이유: 막 켠 뒤의 값이 화면 상태에는 아직 없을 수 있다.
     */
    private suspend fun withCurrentTemperature(change: suspend (Float) -> Unit) {
        when (val prepared = cabinControl.prepareTemperatureControl()) {
            is SourceResult.Failure -> return report(
                "공조 켜기",
                ActionOutcome.FAILED,
                EscortMessages.failure(prepared.reason),
                detail = "공조 켜기",
                feature = CabinFeature.TEMPERATURE,
            )
            is SourceResult.Success -> if (prepared.value) {
                record("공조 켜기", ActionOutcome.SUCCEEDED, CabinFeature.TEMPERATURE)
            }
        }
        val current = cabinControl.state.value.targetTemperatureCelsius
            ?: return skip("온도 조절", EscortMessages.UNKNOWN_VALUE)
        change(current)
    }

    private suspend fun changeTemperature(current: Float, requested: Float) {
        val capabilities = loadCapabilities("온도 조절") ?: return
        val vehicleRange = capabilities.temperatureRangeCelsius
            ?: return skip("온도 조절", EscortMessages.failure(FailureReason.NOT_SUPPORTED))

        // 차량은 0.5℃ 같은 단위만 받는다. 먼저 단위에 맞춘 뒤 범위를 본다.
        val target = roundToStep(requested, capabilities.temperatureStepCelsius)
        val passenger = trip.passenger
        val consentRange = passenger.autoTemperatureRangeCelsius
        val description = "온도 ${EscortMessages.celsius(current)} → ${EscortMessages.celsius(target)}"

        when {
            target !in vehicleRange -> skip(description, EscortMessages.OUT_OF_VEHICLE_RANGE)
            consentRange == null || target !in consentRange ->
                skip(description, EscortMessages.outsidePreferredRange(passenger.name, consentRange))
            target == current -> skip(description, EscortMessages.ALREADY_SET)
            else -> execute(
                description = description,
                feature = CabinFeature.TEMPERATURE,
                successMessage = EscortMessages.temperatureChanged(current, target),
                successDetail = EscortMessages.AUTO_ADJUST_DETAIL,
            ) { cabinControl.setTargetTemperature(target) }
        }
    }

    private suspend fun changeVolume(delta: Int) {
        val current = mutableState.value.musicVolume
            ?: return skip("음량 조절", EscortMessages.UNKNOWN_VALUE)
        val capabilities = loadCapabilities("음량 조절") ?: return
        val range = capabilities.musicVolumeRange
            ?: return skip("음량 조절", EscortMessages.failure(FailureReason.NOT_SUPPORTED))
        val target = current + delta
        val description = "음량 $current → $target"

        if (target !in range) return skip(description, EscortMessages.OUT_OF_VEHICLE_RANGE)
        execute(description, CabinFeature.MUSIC_VOLUME, EscortMessages.volumeChanged(current, target)) {
            cabinControl.setMusicVolume(target)
        }
    }

    private suspend fun changeBackrest(deltaDegrees: Int) {
        val current = mutableState.value.passengerBackrestAngle
            ?: return skip("등받이 조절", EscortMessages.UNKNOWN_VALUE)
        val capabilities = loadCapabilities("등받이 조절") ?: return
        val range = capabilities.backrestAngleRange
            ?: return skip("등받이 조절", EscortMessages.failure(FailureReason.NOT_SUPPORTED))
        val target = current + deltaDegrees
        val description = "등받이 $current° → $target°"

        if (target !in range) return skip(description, EscortMessages.OUT_OF_VEHICLE_RANGE)
        execute(description, CabinFeature.BACKREST, EscortMessages.backrestChanged(current, target)) {
            cabinControl.setPassengerBackrestAngle(target)
        }
    }

    /** 실내 설정 요청 하나를 보내고 결과를 기록·안내한다. 실제 값은 cabinControl.state로 나중에 바뀐다. */
    private suspend fun execute(
        description: String,
        feature: CabinFeature,
        successMessage: String,
        successDetail: String? = null,
        request: suspend () -> SourceResult<Unit>,
    ) {
        when (val result = request()) {
            is SourceResult.Success ->
                report(description, ActionOutcome.SUCCEEDED, successMessage, successDetail, feature)
            is SourceResult.Failure ->
                report(description, ActionOutcome.FAILED, EscortMessages.failure(result.reason), description, feature)
        }
    }

    /**
     * 조절 범위를 한 번만 조회해 둔다. 실패하면 이 자리에서 기록·안내까지 끝내고 null을 돌려준다.
     * 부르는 쪽은 null이면 그냥 멈춘다. 여기서 띄운 실패 안내를 다른 문구로 덮어쓰지 않기 위해서다.
     */
    private suspend fun loadCapabilities(description: String): CabinCapabilities? {
        cachedCapabilities?.let { return it }
        return when (val result = cabinControl.capabilities()) {
            is SourceResult.Success -> result.value.also { cachedCapabilities = it }
            is SourceResult.Failure -> {
                report(description, ActionOutcome.FAILED, EscortMessages.failure(result.reason), detail = description)
                null
            }
        }
    }

    // ---- 경유 제안 (F-1) ----

    private suspend fun proposePlace(kind: PlaceKind, spokenText: String) {
        // "쉬었다 가자"도 지연 휴식 제안과 같은 기준으로 찾는다.
        val place = (if (kind == PlaceKind.REST_AREA) findRestPlace() else findNearestPlace(kind)) ?: return
        showProposal(
            ReplanProposal(
                id = "voice-${++nextProposalId}",
                reason = ReplanReason.VoiceRequest(spokenText),
                message = EscortMessages.voicePlaceProposal(kind, place.name),
                place = place,
                freeWaypointSlots = freeWaypointSlots(),
                bundledActions = emptyList(),
                searchOrigin = lastSearchOrigin,
            ),
        )
    }

    private fun onTripStatus(status: TripStatus?) {
        roadType = status?.roadType
        if (status?.arrived == true && !mutableState.value.arrived) finishEscort()
        mutableState.update {
            it.copy(
                eta = status?.eta,
                remainDistanceMeters = status?.remainDistanceMeters,
                remainMinutes = status?.remainMinutes,
            )
        }
        val eta = status?.eta ?: return
        val baseline = baselineEta
        if (baseline == null) {
            setBaseline(eta, status.remainMinutes)
            return
        }

        val delayedMinutes = Duration.between(baseline, eta).toMinutes()
        val isAlreadyHandling = mutableState.value.proposal != null || delayCheckJob?.isActive == true
        if (delayedMinutes >= delayThresholdMinutes && !isAlreadyHandling) {
            delayCheckJob = viewModelScope.launch { handleDelay(baseline, eta, status.remainMinutes, delayedMinutes) }
        }
    }

    private suspend fun handleDelay(
        plannedEta: LocalTime,
        delayedEta: LocalTime,
        delayedMinutes: Int?,
        lateBy: Long,
    ) {
        val passenger = trip.passenger
        if (!passenger.prefersRestOnLongTrip) {
            // 휴식을 원하지 않는 동승자에게는 경유를 묻지 않고 늦어진다는 사실만 알린다.
            showNotice(EscortMessages.delayNotice(lateBy))
            setBaseline(delayedEta, delayedMinutes)
            return
        }

        val place = findRestPlace()
        if (place == null) {
            setBaseline(delayedEta, delayedMinutes)
            return
        }
        showProposal(
            ReplanProposal(
                id = "delay-${++nextProposalId}",
                reason = ReplanReason.Delay(plannedEta, delayedEta, baselineMinutes, delayedMinutes),
                message = EscortMessages.restProposal(passenger.name),
                place = place,
                freeWaypointSlots = freeWaypointSlots(),
                bundledActions = restBundledActions(),
                searchOrigin = lastSearchOrigin,
            ),
        )
    }

    private fun restBundledActions(): List<CabinAction> {
        val restVolume = trip.passenger.restMusicVolume ?: return emptyList()
        val currentVolume = mutableState.value.musicVolume ?: return emptyList()
        if (currentVolume <= restVolume) return emptyList()
        return listOf(CabinAction.SetMusicVolume(from = currentVolume, to = restVolume))
    }

    private suspend fun runBundledAction(action: CabinAction) {
        when (action) {
            is CabinAction.SetMusicVolume -> {
                val description = "음량 ${action.from} → ${action.to} (경유와 함께)"
                val outcome = when (cabinControl.setMusicVolume(action.to)) {
                    is SourceResult.Success -> ActionOutcome.SUCCEEDED
                    is SourceResult.Failure -> ActionOutcome.FAILED
                }
                record(description, outcome, CabinFeature.MUSIC_VOLUME)
            }
        }
    }

    /** 현재 위치 기준으로 가장 가까운 장소 하나. 못 찾으면 이유를 알리고 null. */
    private suspend fun findNearestPlace(kind: PlaceKind): PlaceCandidate? {
        val location = currentLocationOrNotice() ?: return null
        return when (val result = placeSearch.searchNearby(kind, location, SEARCH_RADIUS_METERS)) {
            is SourceResult.Success -> result.value.firstOrNull().also { nearest ->
                if (nearest == null) showNotice(EscortMessages.noPlaceFound(kind))
            }
            is SourceResult.Failure -> {
                showNotice(EscortMessages.failure(result.reason))
                null
            }
        }
    }

    /** [RestPlaceRule] 순서대로 찾아 처음 나온 곳. 검색이 실패하면 다음 종류로 넘어가지 않고 이유를 알린다. */
    private suspend fun findRestPlace(): PlaceCandidate? {
        val location = currentLocationOrNotice() ?: return null
        val plan = RestPlaceRule.plan(roadType)
        for (kind in plan.kindsInOrder) {
            when (val result = placeSearch.searchNearby(kind, location, plan.radiusMeters)) {
                is SourceResult.Success -> result.value.firstOrNull()?.let { return it }
                is SourceResult.Failure -> {
                    showNotice(EscortMessages.failure(result.reason))
                    return null
                }
            }
        }
        showNotice(EscortMessages.noPlaceFound(PlaceKind.REST_AREA))
        return null
    }

    private suspend fun currentLocationOrNotice(): GeoPoint? = when (val result = tripSource.currentLocation()) {
        is SourceResult.Success -> result.value.also { lastSearchOrigin = it }
        is SourceResult.Failure -> {
            showNotice(EscortMessages.LOCATION_UNAVAILABLE)
            null
        }
    }

    private fun freeWaypointSlots(): Int {
        val used = tripSource.tripStatus.value?.waypointCount ?: return 0
        return (TripSource.MAX_WAYPOINTS - used).coerceAtLeast(0)
    }

    private fun showProposal(proposal: ReplanProposal) {
        alerts.showProposal(proposal.message)
        mutableState.update { it.copy(proposal = proposal) }
    }

    private fun updateProposal(change: (ReplanProposal) -> ReplanProposal) {
        mutableState.update { state -> state.copy(proposal = state.proposal?.let(change)) }
    }

    private fun closeProposal() {
        alerts.clearProposal()
        mutableState.update { it.copy(proposal = null) }
        val state = mutableState.value
        state.eta?.let { setBaseline(it, state.remainMinutes) }
    }

    private fun setBaseline(eta: LocalTime, remainMinutes: Int?) {
        baselineEta = eta
        baselineMinutes = remainMinutes
    }

    // ---- 상태 반영, 안내, 기록 ----

    private fun applyCabinState(cabin: CabinState) {
        mutableState.update {
            it.copy(
                cabinTemperatureCelsius = cabin.targetTemperatureCelsius,
                musicVolume = cabin.musicVolume,
                passengerBackrestAngle = cabin.passengerBackrestAngle,
            )
        }
    }

    /** 실내 설정은 기능별로, 나머지(경유지 등)는 경로 소스 기준으로 Mock 여부를 정한다. */
    private fun isSimulated(feature: CabinFeature?): Boolean =
        if (feature == null) tripIsSimulated else feature in cabinControl.simulatedFeatures

    private fun skip(description: String, reason: String) {
        report(description, ActionOutcome.SKIPPED, reason, detail = description)
    }

    /**
     * 기록과 안내를 한 번에 반영한다.
     * 따로 반영하면 "기록은 있는데 토스트는 없는" 중간 상태가 화면에 잠깐 보일 수 있다.
     */
    private fun report(
        description: String,
        outcome: ActionOutcome,
        message: String,
        detail: String? = null,
        feature: CabinFeature? = null,
    ) {
        val entry = EscortActionRecord(description, outcome, isSimulated = isSimulated(feature))
        val notice = newNotice(message, detail)
        alerts.showNotice(message)
        mutableState.update { it.copy(history = it.history + entry, notice = notice) }
        scheduleNoticeHide(notice.id)
    }

    /** 내비에 경로가 없으면 에스코트 목적지로 요청한다. 실패해도 에스코트는 계속하고 이유만 알린다. */
    private suspend fun ensureRoute() {
        val location = trip.destinationLocation ?: return
        val result = tripSource.ensureRoute(trip.destinationName, location)
        if (result is SourceResult.Failure) {
            showNotice(EscortMessages.ROUTE_NOT_SET, detail = EscortMessages.failure(result.reason))
        }
    }

    private fun showNotice(message: String, detail: String? = null) {
        val notice = newNotice(message, detail)
        alerts.showNotice(message)
        mutableState.update { it.copy(notice = notice) }
        scheduleNoticeHide(notice.id)
    }

    private fun newNotice(message: String, detail: String?) = EscortNotice(++nextNoticeId, message, detail)

    private fun scheduleNoticeHide(id: Long) {
        viewModelScope.launch {
            delay(noticeDurationMillis)
            // 그사이 새 알림이 떴으면 그 알림은 지우지 않는다.
            mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it }
        }
    }

    private fun record(description: String, outcome: ActionOutcome, feature: CabinFeature? = null) {
        val entry = EscortActionRecord(description, outcome, isSimulated = isSimulated(feature))
        mutableState.update { it.copy(history = it.history + entry) }
    }

    private fun initialState(): EscortUiState {
        val cabin = cabinControl.state.value
        val status = tripSource.tripStatus.value
        return EscortUiState(
            isSimulated = tripIsSimulated,
            passengerName = trip.passenger.name,
            destinationName = trip.destinationName,
            boardedAt = trip.boardedAt,
            eta = status?.eta,
            remainDistanceMeters = status?.remainDistanceMeters,
            remainMinutes = status?.remainMinutes,
            cabinTemperatureCelsius = cabin.targetTemperatureCelsius,
            musicVolume = cabin.musicVolume,
            passengerBackrestAngle = cabin.passengerBackrestAngle,
        )
    }

    private fun roundToStep(value: Float, step: Float): Float = round(value / step) * step

    companion object {
        /** [확인 필요] 토스트 표시 시간 */
        const val NOTICE_DURATION_MILLIS = 4_000L

        /** [확인 필요] 계획보다 이만큼 늦어지면 휴식을 제안한다 */
        const val DELAY_THRESHOLD_MINUTES = 15L

        /** [확인 필요] 음성으로 요청한 장소 검색 반경. 휴식 장소는 RestPlaceRule을 따른다 */
        const val SEARCH_RADIUS_METERS = 2_000
    }
}

/** [EscortViewModel.handleVoiceText]의 결과. 화면이 다음에 무엇을 할지 정한다. */
sealed interface VoiceRouting {
    /** 장소 요청이다. [query]를 검색어로 F-3을 연다. */
    data class SearchPlaces(val query: String) : VoiceRouting

    /** 여기서 처리를 끝냈다. [message]는 화면에 이미 토스트로 뜬 안내이며 없을 수 있다. */
    data class Handled(val message: String?) : VoiceRouting
}

/**
 * F-3에 넘길 장소 검색 결과다.
 * [places]가 null이면 검색하지 못한 것이고 [failureMessage]에 이유가 있다.
 * 비어 있는 목록은 "조건에 맞는 곳이 없음"이며 실패가 아니다.
 */
data class PlaceSearchOutcome(
    val origin: GeoPoint?,
    val places: List<PlaceCandidate>?,
    val failureMessage: String?,
)
