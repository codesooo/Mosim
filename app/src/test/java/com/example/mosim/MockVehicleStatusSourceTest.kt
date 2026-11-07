package com.example.mosim

import com.example.mosim.data.mock.MockVehicleStatusSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MockVehicleStatusSourceTest {

    @Test
    fun `mock 차량 상태는 화면이 바로 쓸 수 있는 범위 안이다`() = runBlocking {
        val status = MockVehicleStatusSource().load()

        assertTrue(status.batteryPercent in 0..100)
        assertTrue(status.rangeKm >= 0)
        assertTrue(status.seatsAvailable >= 0)
    }
}
