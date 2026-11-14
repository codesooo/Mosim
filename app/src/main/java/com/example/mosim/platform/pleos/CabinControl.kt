package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.StateFlow

/**
 * 실내 설정(동승석 목표 온도, 음악 음량, 조수석 등받이) 경계다.
 *
 * 온도는 PleosCabinControl이 Vehicle SDK로 바꾼다(에뮬레이터 검증 됨, sdk-status.md 2026-11-12).
 * 등받이는 에뮬레이터에서 이동을 확인하지 못했고, 음량은 제어 방법이 미정이라 둘 다 Mock이다.
 *
 * 실제 SDK처럼 "요청 성공"과 "실제 반영"을 나눈다. set*가 Success를 돌려줘도 값은 [state]로 나중에 바뀐다.
 */
interface CabinControl {

    /** Mock으로 처리하는 기능. 실행 기록에 "Mock 실행"을 기능별로 표시하는 데 쓴다(project-status.md 안전 규칙). */
    val simulatedFeatures: Set<CabinFeature>

    /** 현재 실내 상태. 값을 모르면 각 필드가 null이다. */
    val state: StateFlow<CabinState>

    /** 조절 가능한 범위. Vehicle SDK의 check*Capabilities에 해당한다. */
    suspend fun capabilities(): SourceResult<CabinCapabilities>

    /**
     * 온도를 바꾸기 전에 부른다. 실제 차량은 공조가 꺼져 있으면 온도를 읽을 수 없어 켠다(팀 결정 2026-11-12).
     * 공조를 새로 켰으면 Success(true). 켜야 할 것이 없는 구현(Mock)은 Success(false).
     */
    suspend fun prepareTemperatureControl(): SourceResult<Boolean> = SourceResult.Success(false)

    suspend fun setTargetTemperature(celsius: Float): SourceResult<Unit>

    suspend fun setMusicVolume(level: Int): SourceResult<Unit>

    /**
     * 목표 각도를 받는다.
     * [확인 필요] 실제 SDK는 FORWARD/BACKWARD/STOP 방향 이동이라, 실제 구현은 위치를 구독하며 목표에서 멈춰야 한다
     * (docs/reference/pleos/08 문서 5절). 각도 단위와 값 범위도 미확인이다.
     */
    suspend fun setPassengerBackrestAngle(degrees: Int): SourceResult<Unit>
}

enum class CabinFeature { TEMPERATURE, MUSIC_VOLUME, BACKREST }

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
