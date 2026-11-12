package com.example.mosim.platform.pleos

/** 좌석 위치. Vehicle SDK SeatValue와 이름이 같다. 화면·ViewModel이 SDK 타입을 쓰지 않도록 따로 둔다. */
enum class CabinSeat {
    ROW_1_LEFT, ROW_1_CENTER, ROW_1_RIGHT,
    ROW_2_LEFT, ROW_2_CENTER, ROW_2_RIGHT,
    ROW_3_LEFT, ROW_3_CENTER, ROW_3_RIGHT,
}

/**
 * 공조 온도를 따로 정할 수 있는 구역 하나. 차량마다 묶음이 다르다.
 * 에뮬레이터는 1열 왼쪽, 1열 오른쪽 두 구역이다(sdk-status.md 검증 기록).
 */
data class ClimateZone(
    val seats: Set<CabinSeat>,
    val rangeCelsius: ClosedFloatingPointRange<Float>,
)

/** 차량이 알려 준 공조 구역 목록과 온도 단위. */
data class ClimateZones(
    val zones: List<ClimateZone>,
    val stepCelsius: Float,
)

/**
 * 동승자 요청으로 어느 공조 구역을 바꿀지 정한다(팀 결정 2026-11-12: 착석 감지 기준, 뒷좌석 포함).
 *
 * - 사람이 앉은 좌석 중 운전석을 뺀 좌석마다, 그 좌석을 덮는 구역을 고른다.
 * - 그 좌석을 덮는 구역이 없으면(뒷좌석 전용 구역이 없는 차) 동승석 구역을 쓴다.
 * - 착석을 모르거나 동승자가 감지되지 않으면 동승석 구역을 쓴다.
 */
object ClimateZoneRule {

    /** [확인 필요] 좌핸들 차량 기준이다. */
    val DRIVER_SEAT = CabinSeat.ROW_1_LEFT
    val DEFAULT_PASSENGER_SEAT = CabinSeat.ROW_1_RIGHT

    fun targetZones(occupiedSeats: Set<CabinSeat>?, zones: List<ClimateZone>): List<ClimateZone> {
        val passengerSeats = (occupiedSeats.orEmpty() - DRIVER_SEAT).ifEmpty { setOf(DEFAULT_PASSENGER_SEAT) }
        val passengerZone = zones.firstOrNull { DEFAULT_PASSENGER_SEAT in it.seats }
        return passengerSeats
            .sorted()
            .mapNotNull { seat -> zones.firstOrNull { seat in it.seats } ?: passengerZone }
            .distinct()
    }
}
