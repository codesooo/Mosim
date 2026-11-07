package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.VehicleStatus
import com.example.mosim.platform.pleos.VehicleStatusSource
import kotlinx.coroutines.delay

/**
 * SDK 없이 화면을 개발하기 위한 고정값이다. 실제 차량 데이터가 아니다.
 */
class MockVehicleStatusSource : VehicleStatusSource {
    override suspend fun load(): VehicleStatus {
        delay(300) // 로딩 상태를 눈으로 확인하기 위한 지연
        return VehicleStatus(
            batteryPercent = 72,
            rangeKm = 310,
            doorsLocked = true,
            seatsAvailable = 3
        )
    }
}
