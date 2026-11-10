package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.StateFlow

/**
 * 실내 설정(동승석 목표 온도, 음악 음량, 조수석 등받이) 경계다.
 *
 * 실제 구현은 아직 없다. 제어 API(setHvacTemperature, moveSeatBackrestAngle)는 Vehicle Control 🔜이라
 * 에뮬레이터 검증 전에는 호출 코드를 쓰지 않는다(sdk-status.md). 음량은 제어 방법 자체가 미정이다.
 *
 * 실제 SDK처럼 "요청 성공"과 "실제 반영"을 나눈다. set*가 Success를 돌려줘도 값은 [state]로 나중에 바뀐다.
 */
interface CabinControl {

    /** 현재 실내 상태. 값을 모르면 각 필드가 null이다. */
    val state: StateFlow<CabinState>

    /** 조절 가능한 범위. Vehicle SDK의 check*Capabilities에 해당한다. */
    suspend fun capabilities(): SourceResult<CabinCapabilities>

    suspend fun setTargetTemperature(celsius: Float): SourceResult<Unit>

    suspend fun setMusicVolume(level: Int): SourceResult<Unit>

    /**
     * 목표 각도를 받는다.
     * [확인 필요] 실제 SDK는 FORWARD/BACKWARD/STOP 방향 이동이라, 실제 구현은 위치를 구독하며 목표에서 멈춰야 한다
     * (docs/reference/pleos/08 문서 5절). 각도 단위와 값 범위도 미확인이다.
     */
    suspend fun setPassengerBackrestAngle(degrees: Int): SourceResult<Unit>
}

data class CabinState(
    val targetTemperatureCelsius: Float?,
    val musicVolume: Int?,
    val passengerBackrestAngle: Int?,
)

/** 범위가 null이면 해당 기능을 지원하지 않는다. */
data class CabinCapabilities(
    val temperatureRangeCelsius: ClosedFloatingPointRange<Float>?,
    val temperatureStepCelsius: Float,
    val musicVolumeRange: IntRange?,
    val backrestAngleRange: IntRange?,
)
