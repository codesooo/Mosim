package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.CabinCapabilities
import com.example.mosim.platform.pleos.CabinControl
import com.example.mosim.platform.pleos.CabinFeature
import com.example.mosim.platform.pleos.CabinState
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mock이다. 실제 차량 값이 아니다.
 * 흉내 내는 API: HVAC setHvacTemperature / checkHvacTemperatureCapabilities, Seat moveSeatBackrestAngle.
 * 음량은 대응하는 SDK가 없어 앱 내부 값으로 둔다. [확인 필요] 음악 제어 방식(project-status.md 미결)
 *
 * 실제 SDK 성질을 따른다.
 * - 요청은 IPC라 바로 끝나지 않는다([requestDelayMillis]).
 * - Success는 "요청이 받아들여졌다"는 뜻이다. 값은 [applyDelayMillis] 뒤에 [state]로 바뀐다.
 * - [failure]를 주면 모든 요청이 그 사유로 실패한다(권한 없음, 주행 중 제한 등 재현용).
 *
 * @param scope 반영 지연을 돌릴 범위. 화면에서는 ViewModel 범위, 테스트에서는 테스트 범위를 넘긴다.
 */
class MockCabinControl(
    private val scope: CoroutineScope,
    initialState: CabinState = DEFAULT_STATE,
    private val capabilities: CabinCapabilities = DEFAULT_CAPABILITIES,
    private val failure: FailureReason? = null,
    private val requestDelayMillis: Long = 100,
    private val applyDelayMillis: Long = 500,
) : CabinControl {

    override val simulatedFeatures: Set<CabinFeature> = CabinFeature.entries.toSet()

    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<CabinState> = mutableState.asStateFlow()

    override suspend fun capabilities(): SourceResult<CabinCapabilities> {
        delay(requestDelayMillis)
        failure?.let { return SourceResult.Failure(it) }
        return SourceResult.Success(capabilities)
    }

    override suspend fun setTargetTemperature(celsius: Float): SourceResult<Unit> {
        val range = capabilities.temperatureRangeCelsius
        val isOnStep = isMultipleOf(celsius, capabilities.temperatureStepCelsius)
        val rejection = when {
            range == null -> SourceResult.Failure(FailureReason.NOT_SUPPORTED)
            celsius !in range || !isOnStep -> SourceResult.Failure(FailureReason.INVALID_REQUEST, "온도 $celsius")
            else -> null
        }
        return request(rejection) { it.copy(targetTemperatureCelsius = celsius) }
    }

    override suspend fun setMusicVolume(level: Int): SourceResult<Unit> {
        val range = capabilities.musicVolumeRange
        val rejection = when {
            range == null -> SourceResult.Failure(FailureReason.NOT_SUPPORTED)
            level !in range -> SourceResult.Failure(FailureReason.INVALID_REQUEST, "음량 $level")
            else -> null
        }
        return request(rejection) { it.copy(musicVolume = level) }
    }

    override suspend fun setPassengerBackrestAngle(degrees: Int): SourceResult<Unit> {
        val range = capabilities.backrestAngleRange
        val rejection = when {
            range == null -> SourceResult.Failure(FailureReason.NOT_SUPPORTED)
            degrees !in range -> SourceResult.Failure(FailureReason.INVALID_REQUEST, "등받이 $degrees")
            else -> null
        }
        return request(rejection) { it.copy(passengerBackrestAngle = degrees) }
    }

    /** 세 set 요청의 공통 흐름: 지연 → 실패 확인 → 나중에 반영. */
    private suspend fun request(
        rejection: SourceResult.Failure?,
        apply: (CabinState) -> CabinState,
    ): SourceResult<Unit> {
        delay(requestDelayMillis)
        failure?.let { return SourceResult.Failure(it) }
        rejection?.let { return it }

        scope.launch {
            delay(applyDelayMillis)
            mutableState.update(apply)
        }
        return SourceResult.Success(Unit)
    }

    private fun isMultipleOf(value: Float, step: Float): Boolean {
        val steps = value / step
        return kotlin.math.abs(steps - kotlin.math.round(steps)) < 0.001f
    }

    companion object {
        // 피그마 E-1 예시 값
        val DEFAULT_STATE = CabinState(
            targetTemperatureCelsius = 24f,
            musicVolume = 4,
            passengerBackrestAngle = 110,
        )

        val DEFAULT_CAPABILITIES = CabinCapabilities(
            // Virtual Console Target Temperature 범위와 단위
            temperatureRangeCelsius = 16f..30f,
            temperatureStepCelsius = 0.5f,
            // [확인 필요] 음량 범위. 제어 방식이 정해지면 바꾼다
            musicVolumeRange = 0..10,
            // [확인 필요] 등받이 범위. Virtual Console에 좌석 위치 카드가 없어 임의 값이다
            backrestAngleRange = 90..135,
        )
    }
}
