package com.example.mosim.platform.pleos

import com.example.mosim.platform.pleos.CabinSeat.ROW_1_LEFT
import com.example.mosim.platform.pleos.CabinSeat.ROW_1_RIGHT
import com.example.mosim.platform.pleos.CabinSeat.ROW_2_LEFT
import com.example.mosim.platform.pleos.CabinSeat.ROW_2_RIGHT
import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateZoneRuleTest {

    // 에뮬레이터처럼 1열 좌우만 따로 정하는 차
    private val driverZone = ClimateZone(setOf(ROW_1_LEFT), 15f..30f)
    private val passengerZone = ClimateZone(setOf(ROW_1_RIGHT), 15f..30f)
    private val frontOnly = listOf(driverZone, passengerZone)

    @Test
    fun `동승석에 사람이 있으면 동승석 구역만 바꾼다`() {
        assertEquals(listOf(passengerZone), ClimateZoneRule.targetZones(setOf(ROW_1_LEFT, ROW_1_RIGHT), frontOnly))
    }

    @Test
    fun `운전석은 동승자가 아니라 바꾸지 않는다`() {
        assertEquals(listOf(passengerZone), ClimateZoneRule.targetZones(setOf(ROW_1_LEFT), frontOnly))
    }

    @Test
    fun `착석을 모르면 동승석 구역을 쓴다`() {
        assertEquals(listOf(passengerZone), ClimateZoneRule.targetZones(null, frontOnly))
    }

    @Test
    fun `뒷좌석 구역이 없는 차에서 뒷좌석 승객은 동승석 구역을 쓴다`() {
        assertEquals(listOf(passengerZone), ClimateZoneRule.targetZones(setOf(ROW_2_LEFT), frontOnly))
    }

    @Test
    fun `뒷좌석 구역이 있으면 앉은 좌석마다 그 구역을 바꾼다`() {
        val rearLeft = ClimateZone(setOf(ROW_2_LEFT), 16f..28f)
        val rearRight = ClimateZone(setOf(ROW_2_RIGHT), 16f..28f)
        val zones = frontOnly + rearLeft + rearRight

        assertEquals(
            listOf(passengerZone, rearRight),
            ClimateZoneRule.targetZones(setOf(ROW_1_LEFT, ROW_1_RIGHT, ROW_2_RIGHT), zones),
        )
    }

    @Test
    fun `여러 좌석을 덮는 구역은 한 번만 바꾼다`() {
        val rightSide = ClimateZone(setOf(ROW_1_RIGHT, ROW_2_RIGHT), 15f..30f)

        assertEquals(
            listOf(rightSide),
            ClimateZoneRule.targetZones(setOf(ROW_1_RIGHT, ROW_2_RIGHT), listOf(driverZone, rightSide)),
        )
    }
}
