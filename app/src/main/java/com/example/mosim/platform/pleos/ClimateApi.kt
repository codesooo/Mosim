package com.example.mosim.platform.pleos

/**
 * 공조와 착석 감지에 필요한 Vehicle SDK 호출만 모은 경계다.
 * 실제 구현은 [PleosClimateApi]이고, PleosCabinControl 단위 테스트에서는 가짜로 바꿔 끼운다.
 */
interface ClimateApi {

    suspend fun temperatureZones(): SourceResult<ClimateZones>

    /** 사람이 앉은 좌석. 착석 감지를 지원하지 않으면 Failure다. */
    suspend fun occupiedSeats(): SourceResult<Set<CabinSeat>>

    /** 목표 온도(℃). 공조가 꺼져 있으면 Failure다(sdk-status.md 검증 기록). */
    suspend fun targetTemperature(zone: ClimateZone): SourceResult<Float?>

    /** Success는 요청이 받아들여졌다는 뜻이다. 실제 값은 다시 조회해 확인한다. */
    suspend fun setTargetTemperature(zone: ClimateZone, celsius: Float): SourceResult<Unit>

    suspend fun isPowerOn(): SourceResult<Boolean?>

    suspend fun turnPowerOn(): SourceResult<Unit>
}
