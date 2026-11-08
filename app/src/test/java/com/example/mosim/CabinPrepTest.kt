package com.example.mosim

import com.example.mosim.data.mock.MockCabinPrepSource
import com.example.mosim.data.mock.MockCabinPrepSource.Scenario
import com.example.mosim.platform.pleos.PrepApplyState
import com.example.mosim.platform.pleos.PrepItemStatus
import com.example.mosim.platform.pleos.PrepKind
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinPrepTest {

    private val kinds = listOf(PrepKind.Climate, PrepKind.Seat, PrepKind.Music, PrepKind.Lighting)

    private fun run(scenario: Scenario): List<List<PrepItemStatus>> = runBlocking {
        MockCabinPrepSource(scenario, stepDelayMs = 0).apply(kinds).toList()
    }

    private fun List<PrepItemStatus>.stateOf(kind: PrepKind) = first { it.kind == kind }.state

    @Test
    fun `정상이면 모든 항목이 적용 중을 거쳐 적용으로 끝난다`() {
        val emitted = run(Scenario.Normal)

        assertTrue(emitted.first().all { it.state == PrepApplyState.Pending })
        assertTrue(emitted.last().all { it.state == PrepApplyState.Applied })
        // Set 성공과 실제 반영은 다르다: 어느 항목도 대기에서 바로 적용으로 건너뛰지 않는다.
        kinds.forEach { kind ->
            assertTrue(emitted.any { it.stateOf(kind) == PrepApplyState.Applying })
        }
    }

    @Test
    fun `조명이 없는 차량이면 조명만 미지원이고 나머지는 적용된다`() {
        val last = run(Scenario.LightingUnsupported).last()

        assertEquals(PrepApplyState.Unsupported, last.stateOf(PrepKind.Lighting))
        assertEquals(3, last.count { it.state == PrepApplyState.Applied })
    }

    @Test
    fun `좌석이 실패해도 다음 항목은 계속 적용한다`() {
        val last = run(Scenario.SeatFailed).last()

        assertEquals(PrepApplyState.Failed, last.stateOf(PrepKind.Seat))
        assertEquals(PrepApplyState.Applied, last.stateOf(PrepKind.Music))
    }
}
