package com.example.mosim.feature.escort

import com.example.mosim.data.mock.MockCabinControl
import com.example.mosim.data.mock.MockEscortTrips
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechInput
import com.example.mosim.data.mock.MockTripSource
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import com.example.mosim.platform.pleos.TripSource
import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.RoadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.util.concurrent.Executors

/**
 * 앱의 메인 스레드처럼 스레드 하나에서 돌린다.
 * kotlinx-coroutines-test를 추가하지 않기 위해 실제 시간을 짧게 기다리는 방식으로 검증한다.
 */
class EscortViewModelTest {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val speech = MockSpeechInput()
    private var cabin = MockCabinControl(scope, requestDelayMillis = 0, applyDelayMillis = 10)
    private var trip = MockTripSource(scope, requestDelayMillis = 0, routingDelayMillis = 10)
    private var places = MockPlaceSearchSource(requestDelayMillis = 0)
    private var passenger = MockEscortTrips.MOTHER

    @After
    fun tearDown() {
        scope.cancel()
        dispatcher.close()
    }

    private fun createViewModel(noticeDurationMillis: Long = 5_000): EscortViewModel {
        val viewModel = EscortViewModel(
            scope = scope,
            trip = MockEscortTrips.MOTHER_TO_HOSPITAL.copy(passenger = passenger),
            speechInput = speech,
            cabinControl = cabin,
            tripSource = trip,
            placeSearch = places,
            sourcesAreSimulated = true,
            noticeDurationMillis = noticeDurationMillis,
        )
        runBlocking { withTimeout(TIMEOUT) { speech.listenerCount.first { it > 0 } } }
        return viewModel
    }

    private fun EscortViewModel.awaitState(predicate: (EscortUiState) -> Boolean): EscortUiState =
        runBlocking { withTimeout(TIMEOUT) { uiState.first(predicate) } }

    /** "일어나지 않음"을 확인할 때 처리할 시간을 준다. */
    private fun settle() = runBlocking { delay(150) }

    // ---- 실내 설정 ----

    @Test
    fun `덥다고 하면 선호 범위 안에서 온도를 낮추고 토스트를 띄운다`() {
        val viewModel = createViewModel()

        speech.say("좀 덥다")

        val state = viewModel.awaitState { it.cabinTemperatureCelsius == 23f }
        assertEquals("온도를 24℃ → 23℃로 낮췄어요", state.notice?.message)
        assertEquals(EscortMessages.AUTO_ADJUST_DETAIL, state.notice?.detail)
        val record = state.history.single()
        assertEquals(ActionOutcome.SUCCEEDED, record.outcome)
        assertTrue(record.isSimulated)
    }

    @Test
    fun `선호 범위 밖 온도는 실행하지 않고 이유를 알린다`() {
        val viewModel = createViewModel()

        speech.say("20도로 맞춰")

        val state = viewModel.awaitState { it.history.isNotEmpty() }
        settle()
        assertEquals(24f, viewModel.uiState.value.cabinTemperatureCelsius)
        assertEquals(ActionOutcome.SKIPPED, state.history.single().outcome)
        assertTrue(state.notice!!.message.contains("선호 범위"))
    }

    @Test
    fun `차량 범위 밖 온도는 실행하지 않는다`() {
        val viewModel = createViewModel()

        speech.say("40도로 맞춰")

        val state = viewModel.awaitState { it.history.isNotEmpty() }
        assertEquals(ActionOutcome.SKIPPED, state.history.single().outcome)
        assertEquals(EscortMessages.OUT_OF_VEHICLE_RANGE, state.notice?.message)
    }

    @Test
    fun `동의 범위가 없는 동승자는 자동 온도 조절을 하지 않는다`() {
        passenger = passenger.copy(autoTemperatureRangeCelsius = null)
        val viewModel = createViewModel()

        speech.say("더워")

        val state = viewModel.awaitState { it.history.isNotEmpty() }
        assertEquals(ActionOutcome.SKIPPED, state.history.single().outcome)
    }

    @Test
    fun `시끄럽다고 하면 음량을 한 단계 낮춘다`() {
        val viewModel = createViewModel()

        speech.say("좀 시끄러워")

        val state = viewModel.awaitState { it.musicVolume == 3 }
        assertEquals("음악 볼륨을 4 → 3단계로 낮췄어요", state.notice?.message)
    }

    @Test
    fun `의자를 젖히라고 하면 등받이 각도가 늘어난다`() {
        val viewModel = createViewModel()

        speech.say("의자 좀 젖혀줘")

        viewModel.awaitState { it.passengerBackrestAngle == 115 }
    }

    @Test
    fun `차량이 거절하면 실패로 기록하고 사유를 알린다`() {
        cabin = MockCabinControl(scope, failure = FailureReason.DRIVING_RESTRICTED, requestDelayMillis = 0)
        val viewModel = createViewModel()

        speech.say("소리 줄여")

        viewModel.awaitState { it.notice != null }
        settle()
        // 실패 안내가 다른 문구로 덮이지 않아야 한다(범위 조회 실패 뒤 "범위를 벗어나요"가 뜨던 버그).
        val state = viewModel.uiState.value
        assertEquals(EscortMessages.failure(FailureReason.DRIVING_RESTRICTED), state.notice?.message)
        assertEquals(ActionOutcome.FAILED, state.history.single().outcome)
        assertEquals(4, state.musicVolume)
    }

    @Test
    fun `알아듣지 못한 문장은 다시 말해 달라고 안내한다`() {
        val viewModel = createViewModel()

        speech.say("오늘 날씨 어때")

        val state = viewModel.awaitState { it.notice != null }
        assertEquals(EscortMessages.NOT_UNDERSTOOD, state.notice?.message)
        assertEquals("오늘 날씨 어때", state.notice?.detail)
    }

    @Test
    fun `토스트는 정해진 시간 뒤 사라진다`() {
        val viewModel = createViewModel(noticeDurationMillis = 50)

        speech.say("오늘 날씨 어때")

        viewModel.awaitState { it.notice != null }
        viewModel.awaitState { it.notice == null }
    }

    // ---- 음성 장소 요청 ----

    @Test
    fun `장소를 요청하면 자동 실행하지 않고 가장 가까운 곳을 제안한다`() {
        val viewModel = createViewModel()

        speech.say("근처 카페 가자")

        val proposal = viewModel.awaitState { it.proposal != null }.proposal!!
        assertEquals("카페 A", proposal.place.name)
        assertEquals(ReplanReason.VoiceRequest("근처 카페 가자"), proposal.reason)
        assertEquals(2, proposal.freeWaypointSlots)
        assertEquals(0, trip.tripStatus.value?.waypointCount)
    }

    @Test
    fun `제안을 승인하면 경유지를 추가하고 제안을 닫는다`() {
        val viewModel = createViewModel()
        speech.say("근처 카페 가자")
        viewModel.awaitState { it.proposal != null }

        viewModel.acceptProposal()

        // 제안을 닫은 뒤 토스트를 띄우므로 둘 다 반영된 상태를 기다린다.
        viewModel.awaitState { it.proposal == null && it.notice?.message == "카페 A를 경유지로 추가했어요" }
        runBlocking { withTimeout(TIMEOUT) { trip.tripStatus.first { it?.waypointCount == 1 } } }
    }

    @Test
    fun `경유지가 꽉 차 있으면 승인해도 추가하지 않는다`() {
        val full = MockTripSource.DEFAULT_STATUS.copy(waypointCount = TripSource.MAX_WAYPOINTS)
        trip = MockTripSource(scope, initialStatus = full, requestDelayMillis = 0)
        val viewModel = createViewModel()
        speech.say("근처 카페 가자")
        val proposal = viewModel.awaitState { it.proposal != null }.proposal!!
        assertFalse(proposal.canAddWaypoint)

        viewModel.acceptProposal()

        settle()
        assertEquals(proposal, viewModel.uiState.value.proposal)
    }

    @Test
    fun `경유 요청이 실패하면 제안을 유지하고 오류를 보여 준다`() {
        val failingTrip = WaypointRejectingTripSource(trip, FailureReason.CONNECTION_FAILED)
        val viewModel = EscortViewModel(
            scope = scope,
            trip = MockEscortTrips.MOTHER_TO_HOSPITAL,
            speechInput = speech,
            cabinControl = cabin,
            tripSource = failingTrip,
            placeSearch = places,
            sourcesAreSimulated = true,
        )
        runBlocking { withTimeout(TIMEOUT) { speech.listenerCount.first { it > 0 } } }
        speech.say("근처 카페 가자")
        viewModel.awaitState { it.proposal != null }

        viewModel.acceptProposal()

        val state = viewModel.awaitState { it.proposal?.errorMessage != null }
        assertEquals(EscortMessages.failure(FailureReason.CONNECTION_FAILED), state.proposal!!.errorMessage)
        assertFalse(state.proposal!!.isSubmitting)
        assertEquals(ActionOutcome.FAILED, state.history.last().outcome)
    }

    /** 위치·경로 정보는 Mock 그대로 쓰고 경유지 추가만 실패시키는 테스트 대역. */
    private class WaypointRejectingTripSource(
        private val delegate: MockTripSource,
        private val reason: FailureReason,
    ) : TripSource by delegate {
        override suspend fun addWaypoint(name: String, location: GeoPoint): SourceResult<Unit> =
            SourceResult.Failure(reason)
    }

    @Test
    fun `근처에 장소가 없으면 제안 대신 안내만 한다`() {
        places = MockPlaceSearchSource(places = emptyMap(), requestDelayMillis = 0)
        val viewModel = createViewModel()

        speech.say("편의점 들르자")

        val state = viewModel.awaitState { it.notice != null }
        assertEquals("근처에서 편의점을 찾지 못했어요", state.notice?.message)
        assertNull(state.proposal)
    }

    // ---- 지연 감지 ----

    @Test
    fun `도착이 기준 이상 늦어지면 휴식 장소와 함께 처리할 음량을 제안한다`() {
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(30)

        val proposal = viewModel.awaitState { it.proposal != null }.proposal!!
        assertEquals(
            ReplanReason.Delay(
                plannedEta = LocalTime.of(9, 12),
                delayedEta = LocalTime.of(9, 42),
                plannedMinutes = 43,
                delayedMinutes = 73,
            ),
            proposal.reason,
        )
        assertEquals("휴식 장소 A", proposal.place.name)
        assertEquals(listOf(CabinAction.SetMusicVolume(from = 4, to = 3)), proposal.bundledActions)
        assertTrue(proposal.message.startsWith("엄마는"))
    }

    // 국도 반경(2km) 밖, 고속도로 반경(20km) 안에 휴게소 하나만 있는 Mock
    private val farRestAreaPlaces = MockPlaceSearchSource.DEFAULT_PLACES + (
        PlaceKind.REST_AREA to listOf(
            PlaceCandidate("far-rest", "먼 휴게소", "고속도로휴게소", null, GeoPoint(37.45, 127.1), 15_000),
        )
    )

    @Test
    fun `국도에서 반경 안에 휴게소가 없으면 공원을 카페보다 먼저 제안한다`() {
        places = MockPlaceSearchSource(places = farRestAreaPlaces, requestDelayMillis = 0)
        trip = MockTripSource(
            scope,
            initialStatus = MockTripSource.DEFAULT_STATUS.copy(roadType = RoadType.LOCAL),
            requestDelayMillis = 0,
        )
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(30)

        assertEquals("공원 A", viewModel.awaitState { it.proposal != null }.proposal!!.place.name)
    }

    @Test
    fun `고속도로에서는 멀어도 휴게소를 제안한다`() {
        places = MockPlaceSearchSource(places = farRestAreaPlaces, requestDelayMillis = 0)
        trip = MockTripSource(
            scope,
            initialStatus = MockTripSource.DEFAULT_STATUS.copy(roadType = RoadType.HIGHWAY),
            requestDelayMillis = 0,
        )
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(30)

        assertEquals("먼 휴게소", viewModel.awaitState { it.proposal != null }.proposal!!.place.name)
    }

    @Test
    fun `고속도로에서 휴게소가 없으면 카페나 공원을 제안하지 않는다`() {
        places = MockPlaceSearchSource(
            places = MockPlaceSearchSource.DEFAULT_PLACES - PlaceKind.REST_AREA,
            requestDelayMillis = 0,
        )
        trip = MockTripSource(
            scope,
            initialStatus = MockTripSource.DEFAULT_STATUS.copy(roadType = RoadType.HIGHWAY),
            requestDelayMillis = 0,
        )
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(30)

        val state = viewModel.awaitState { it.notice != null }
        assertEquals("근처에서 휴식 장소를 찾지 못했어요", state.notice?.message)
        assertNull(state.proposal)
    }

    @Test
    fun `지연 제안을 승인하면 함께 처리할 음량도 바꾼다`() {
        val viewModel = createViewModel()
        trip.simulateTrafficDelay(30)
        viewModel.awaitState { it.proposal != null }

        viewModel.acceptProposal()

        viewModel.awaitState { it.proposal == null && it.musicVolume == 3 }
    }

    @Test
    fun `기준보다 적게 늦어지면 제안하지 않는다`() {
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(10)

        settle()
        assertNull(viewModel.uiState.value.proposal)
    }

    @Test
    fun `기존 경로를 유지하면 같은 지연을 다시 묻지 않는다`() {
        val viewModel = createViewModel()
        trip.simulateTrafficDelay(30)
        viewModel.awaitState { it.proposal != null }

        viewModel.keepCurrentRoute()
        trip.simulateTrafficDelay(5)

        settle()
        assertNull(viewModel.uiState.value.proposal)
        assertEquals(ActionOutcome.SKIPPED, viewModel.uiState.value.history.last().outcome)
    }

    @Test
    fun `휴식을 원하지 않는 동승자는 지연을 알리기만 한다`() {
        passenger = passenger.copy(prefersRestOnLongTrip = false)
        val viewModel = createViewModel()

        trip.simulateTrafficDelay(30)

        val state = viewModel.awaitState { it.notice != null }
        assertEquals("도착이 30분 늦어질 것 같아요", state.notice?.message)
        assertNull(state.proposal)
    }

    private companion object {
        const val TIMEOUT = 2_000L
    }
}
