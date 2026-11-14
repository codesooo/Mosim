package com.example.mosim

import com.example.mosim.feature.pickup.PickupPrepUiState
import com.example.mosim.feature.pickup.PrepItem
import com.example.mosim.feature.pickup.remainingDistanceText
import com.example.mosim.platform.pleos.PrepApplyState
import com.example.mosim.platform.pleos.PrepItemStatus
import com.example.mosim.platform.pleos.PrepKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C-1·C-2 화면이 차량 신호를 화면 값으로 바꾸는 규칙 */
class PickupScreenStateTest {

    private val kinds = listOf(PrepKind.Climate, PrepKind.Seat, PrepKind.Music, PrepKind.Lighting)

    private fun prepStateWith(vararg states: PrepApplyState) = PickupPrepUiState(
        passengerName = "엄마",
        stopConfirmedAt = "08:27",
        savedSetTemp = 24.0,
        savedOutsideTemp = 5.0,
        currentOutsideTemp = 7.0,
        currentWeather = "비",
        prepItems = kinds.zip(states.toList()) { kind, state -> PrepItem(kind, kind.name, "", state) },
    )

    @Test
    fun `C-1 남은 거리는 km 소수 한 자리로 표시한다`() {
        assertEquals("남은 거리 3.1km", remainingDistanceText(3_100))
        assertEquals("남은 거리 0.3km", remainingDistanceText(300))
        assertEquals("남은 거리 확인 중", remainingDistanceText(null))
    }

    @Test
    fun `C-2 완료 개수는 적용된 항목만 세고 미지원은 전체에서 뺀다`() {
        val state = prepStateWith(
            PrepApplyState.Applied,
            PrepApplyState.Applied,
            PrepApplyState.Applied,
            PrepApplyState.Unsupported,
        )

        assertEquals(3, state.doneCount)
        assertEquals(3, state.totalCount)
        assertTrue(state.isReady)
    }

    @Test
    fun `C-2 적용 중인 항목이 있으면 아직 준비 중이다`() {
        val state = prepStateWith(
            PrepApplyState.Applied,
            PrepApplyState.Applying,
            PrepApplyState.Pending,
            PrepApplyState.Pending,
        )

        assertEquals(1, state.doneCount)
        assertEquals(4, state.totalCount)
        assertFalse(state.isReady)
    }

    @Test
    fun `C-2 차량 상태를 받으면 해당 항목에만 붙는다`() {
        val state = prepStateWith(
            PrepApplyState.Pending,
            PrepApplyState.Pending,
            PrepApplyState.Pending,
            PrepApplyState.Pending,
        ).withStatuses(listOf(PrepItemStatus(PrepKind.Seat, PrepApplyState.Failed)))

        assertEquals(PrepApplyState.Failed, state.prepItems.first { it.kind == PrepKind.Seat }.state)
        assertEquals(PrepApplyState.Pending, state.prepItems.first { it.kind == PrepKind.Climate }.state)
    }
}
