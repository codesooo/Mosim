package com.example.mosim.platform.pleos

/**
 * 앱이 쓰는 차량 상태 모델이다.
 * Pleos SDK 타입을 그대로 쓰지 않는다. SDK 타입이 바뀌어도 화면 코드는 건드리지 않게 한다.
 */
data class VehicleStatus(
    val batteryPercent: Int,
    val rangeKm: Int,
    val doorsLocked: Boolean,
    val seatsAvailable: Int
)
