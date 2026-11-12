package com.example.mosim.feature.escort

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.RoadType
import org.junit.Assert.assertEquals
import org.junit.Test

class RestPlaceRuleTest {

    @Test
    fun `고속도로에서는 휴게소만 넓은 반경으로 찾는다`() {
        val plan = RestPlaceRule.plan(RoadType.HIGHWAY)

        assertEquals(listOf(PlaceKind.REST_AREA), plan.kindsInOrder)
        assertEquals(20_000, plan.radiusMeters)
    }

    @Test
    fun `국도에서는 휴게소, 공원, 카페 순으로 찾는다`() {
        val plan = RestPlaceRule.plan(RoadType.LOCAL)

        assertEquals(listOf(PlaceKind.REST_AREA, PlaceKind.PARK, PlaceKind.CAFE), plan.kindsInOrder)
        assertEquals(2_000, plan.radiusMeters)
    }

    @Test
    fun `도로 종류를 모르면 국도 규칙을 쓴다`() {
        assertEquals(RestPlaceRule.plan(RoadType.LOCAL), RestPlaceRule.plan(null))
    }
}
