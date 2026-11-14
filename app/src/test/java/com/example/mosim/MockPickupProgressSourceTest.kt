package com.example.mosim

import com.example.mosim.data.mock.MockPickupProgressSource
import com.example.mosim.data.mock.MockPickupProgressSource.Scenario
import com.example.mosim.platform.pleos.PickupProgress
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPickupProgressSourceTest {

    @Test
    fun `남은 거리는 줄어들기만 하고 마지막에 한 번만 도착한다`() = runBlocking {
        val emitted = MockPickupProgressSource(Scenario.Normal, stepDelayMs = 0).progress().toList()
        val distances = emitted.mapNotNull { it.remainingDistanceMeters }

        assertEquals(3_100, distances.first())
        assertEquals(distances.sortedDescending(), distances)
        assertTrue(emitted.last().arrived)
        assertEquals(1, emitted.count { it.arrived })
    }

    @Test
    fun `연결이 끊기면 도착 신호 없이 예외로 끝난다`() = runBlocking {
        val emitted = mutableListOf<PickupProgress>()
        var error: Throwable? = null

        MockPickupProgressSource(Scenario.ConnectionFailed, stepDelayMs = 0).progress()
            .catch { error = it }
            .collect { emitted += it }

        assertTrue(error is IllegalStateException)
        assertFalse(emitted.any { it.arrived })
    }
}
