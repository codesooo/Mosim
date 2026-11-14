package com.example.mosim

import com.example.mosim.data.mock.MockBoardingSignalSource
import com.example.mosim.data.mock.MockBoardingSignalSource.Scenario
import com.example.mosim.platform.pleos.BoardingSignals
import com.example.mosim.platform.pleos.DoorSignal
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockBoardingSignalSourceTest {

    private fun source(scenario: Scenario) =
        MockBoardingSignalSource(scenario, stepDelayMs = 0, waitForPassengerMs = 0)

    @Test
    fun `정상 탑승이면 신호가 하나씩 들어와 마지막에 다 모인다`() = runBlocking {
        val emitted = source(Scenario.Normal).signals().toList()

        assertFalse(emitted.first().allDetected)
        assertNull(emitted.first().passengerDoor)
        assertTrue(emitted.last().allDetected)
        // 마지막 값 하나에서만 다 모인다. 중간에 버튼이 미리 켜지지 않는다.
        assertEquals(1, emitted.count { it.allDetected })
    }

    @Test
    fun `벨트를 안 매면 끝까지 다 모이지 않는다`() = runBlocking {
        val last = source(Scenario.BeltNotBuckled).signals().toList().last()

        assertEquals(DoorSignal.ClosedAfterOpen, last.passengerDoor)
        assertEquals(false, last.passengerSeatBeltBuckled)
        assertFalse(last.allDetected)
    }

    @Test
    fun `연결 실패면 착석 신호 전에 예외로 끝난다`() = runBlocking {
        val emitted = mutableListOf<BoardingSignals>()
        var error: Throwable? = null

        source(Scenario.ConnectionFailed).signals()
            .catch { error = it }
            .collect { emitted += it }

        assertTrue(error is IllegalStateException)
        assertNull(emitted.last().passengerSeatOccupied)
        assertFalse(emitted.last().allDetected)
    }
}
