package com.example.mosim.platform.pleos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 온도만 실제 Vehicle SDK로 바꾸고, 음량·등받이는 [others](Mock)에 맡기는 CabinControl.
 *
 * - 어느 공조 구역을 바꿀지는 착석 감지로 정한다([ClimateZoneRule]).
 * - 실제 온도는 [refreshIntervalMillis]마다 다시 읽는다. 리스너 대신 주기 조회를 쓴 이유:
 *   착석이 바뀌면 보는 구역도 바뀌는데, 구역마다 리스너를 붙였다 떼는 관리를 하지 않으려고.
 * - 서비스 연결이 비동기라 처음 조회는 실패할 수 있다. 주기 조회가 다음 차례에 다시 시도한다.
 */
class PleosCabinControl(
    private val scope: CoroutineScope,
    private val climate: ClimateApi,
    private val others: CabinControl,
    private val refreshIntervalMillis: Long = 5_000,
    /** [확인 필요] 에뮬레이터에서 보통 2~3초, 부팅 직후에는 8초를 넘겼다. 실차 값은 모른다. */
    private val powerOnTimeoutMillis: Long = 15_000,
    private val pollIntervalMillis: Long = 500,
) : CabinControl {

    override val simulatedFeatures: Set<CabinFeature> = others.simulatedFeatures - CabinFeature.TEMPERATURE

    // combine/stateIn을 쓰지 않는 이유: 그쪽은 다른 코루틴에서 늦게 갱신돼, 공조를 켜고 바로 읽은 온도가
    // prepareTemperatureControl()이 끝난 시점에 아직 state에 없었다(단위 테스트로 확인). 온도는 바로 쓴다.
    private val mutableState = MutableStateFlow(others.state.value.copy(targetTemperatureCelsius = null))
    override val state: StateFlow<CabinState> = mutableState.asStateFlow()

    /** 구역 목록은 차량이 바뀌지 않는 한 같아서 한 번 받으면 다시 묻지 않는다. */
    private var cachedZones: ClimateZones? = null

    init {
        scope.launch {
            others.state.collect { rest ->
                mutableState.update { rest.copy(targetTemperatureCelsius = it.targetTemperatureCelsius) }
            }
        }
        scope.launch {
            while (isActive) {
                refreshTemperature()
                delay(refreshIntervalMillis)
            }
        }
    }

    override suspend fun capabilities(): SourceResult<CabinCapabilities> {
        val rest = when (val result = others.capabilities()) {
            is SourceResult.Success -> result.value
            is SourceResult.Failure -> return result
        }
        val zones = when (val result = loadZones()) {
            is SourceResult.Success -> result.value
            is SourceResult.Failure -> return result
        }
        val targets = targetZones(zones)
        return SourceResult.Success(
            rest.copy(temperatureRangeCelsius = commonRange(targets), temperatureStepCelsius = zones.stepCelsius),
        )
    }

    override suspend fun prepareTemperatureControl(): SourceResult<Boolean> {
        val isOn = when (val result = climate.isPowerOn()) {
            is SourceResult.Success -> result.value == true
            is SourceResult.Failure -> return result
        }
        if (isOn) {
            refreshTemperature()
            return SourceResult.Success(false)
        }
        (climate.turnPowerOn() as? SourceResult.Failure)?.let { return it }
        // 켜기 요청은 바로 답하지만 실제로 켜지기까지 3초 넘게 걸렸다(sdk-status.md 검증 기록).
        if (!waitUntilPowerOn()) {
            return SourceResult.Failure(FailureReason.UNKNOWN, "공조가 ${powerOnTimeoutMillis}ms 안에 켜지지 않았다")
        }
        refreshTemperature()
        return SourceResult.Success(true)
    }

    override suspend fun setTargetTemperature(celsius: Float): SourceResult<Unit> {
        val zones = when (val result = loadZones()) {
            is SourceResult.Success -> result.value
            is SourceResult.Failure -> return result
        }
        val targets = targetZones(zones)
        if (targets.isEmpty()) return SourceResult.Failure(FailureReason.NOT_SUPPORTED, "바꿀 공조 구역 없음")
        for (zone in targets) {
            (climate.setTargetTemperature(zone, celsius) as? SourceResult.Failure)?.let { return it }
        }
        // 응답은 요청값이다. 실제 반영은 조금 뒤 다시 읽어 상태에 올린다.
        scope.launch {
            delay(pollIntervalMillis)
            refreshTemperature()
        }
        return SourceResult.Success(Unit)
    }

    override suspend fun setMusicVolume(level: Int): SourceResult<Unit> = others.setMusicVolume(level)

    override suspend fun setPassengerBackrestAngle(degrees: Int): SourceResult<Unit> =
        others.setPassengerBackrestAngle(degrees)

    /** 실패하면 값을 모름(null)으로 둔다. 0이나 이전 값으로 채우지 않는다. */
    private suspend fun refreshTemperature() {
        val zones = (loadZones() as? SourceResult.Success)?.value
        val displayZone = zones?.let { targetZones(it).firstOrNull() }
        val celsius = displayZone?.let { (climate.targetTemperature(it) as? SourceResult.Success)?.value }
        mutableState.update { it.copy(targetTemperatureCelsius = celsius) }
    }

    private suspend fun loadZones(): SourceResult<ClimateZones> {
        cachedZones?.let { return SourceResult.Success(it) }
        return climate.temperatureZones().also { result ->
            if (result is SourceResult.Success) cachedZones = result.value
        }
    }

    /** 착석 감지가 실패하면 모름(null)으로 넘겨 동승석 기본 규칙을 쓴다. */
    private suspend fun targetZones(zones: ClimateZones): List<ClimateZone> {
        val occupied = (climate.occupiedSeats() as? SourceResult.Success)?.value
        return ClimateZoneRule.targetZones(occupied, zones.zones)
    }

    private suspend fun waitUntilPowerOn(): Boolean {
        val attempts = (powerOnTimeoutMillis / pollIntervalMillis).toInt()
        repeat(attempts) {
            delay(pollIntervalMillis)
            if ((climate.isPowerOn() as? SourceResult.Success)?.value == true) return true
        }
        return false
    }

    /** 여러 구역을 함께 바꿀 때는 모든 구역이 받을 수 있는 범위만 쓴다. */
    private fun commonRange(zones: List<ClimateZone>): ClosedFloatingPointRange<Float>? {
        if (zones.isEmpty()) return null
        val minimum = zones.maxOf { it.rangeCelsius.start }
        val maximum = zones.minOf { it.rangeCelsius.endInclusive }
        return if (minimum <= maximum) minimum..maximum else null
    }
}
