package com.example.mosim.platform.pleos

import com.example.mosim.data.mock.MockVehicleStatusSource

/**
 * Mock과 실제 SDK를 바꾸는 단일 지점이다.
 * SDK 연동이 끝나면 USE_MOCK만 false로 바꾼다.
 */
object VehicleStatusSourceProvider {

    const val USE_MOCK = true

    val source: VehicleStatusSource by lazy {
        if (USE_MOCK) MockVehicleStatusSource() else PleosVehicleStatusSource()
    }
}
