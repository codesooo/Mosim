package com.example.mosim

import com.example.mosim.feature.pickup.formatTemp
import com.example.mosim.feature.pickup.recommendCabinTemp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinTempRecommenderTest {

    @Test
    fun `피그마 C-2 예시는 저장값 24도를 그대로 쓴다`() {
        // 24 − 0.1 × (7 − 5) = 23.8 → 0.5 단위 반올림 24.0
        val result = recommendCabinTemp(savedSetTemp = 24.0, savedOutsideTemp = 5.0, currentOutsideTemp = 7.0)

        assertEquals(24.0, result.temp, 0.0)
        assertFalse(result.adjusted)
    }

    @Test
    fun `밖이 더 따뜻해지면 낮춘다`() {
        val result = recommendCabinTemp(savedSetTemp = 24.0, savedOutsideTemp = 5.0, currentOutsideTemp = 25.0)

        assertEquals(22.0, result.temp, 0.0)
        assertTrue(result.adjusted)
    }

    @Test
    fun `밖이 더 추워지면 올린다`() {
        val result = recommendCabinTemp(savedSetTemp = 22.0, savedOutsideTemp = 20.0, currentOutsideTemp = 0.0)

        assertEquals(24.0, result.temp, 0.0)
    }

    @Test
    fun `차량 설정 단위인 0점5도로 반올림한다`() {
        // 24 − 0.1 × 3 = 23.7 → 23.5
        val result = recommendCabinTemp(savedSetTemp = 24.0, savedOutsideTemp = 5.0, currentOutsideTemp = 8.0)

        assertEquals(23.5, result.temp, 0.0)
    }

    @Test
    fun `결과는 18도에서 28도 사이로 자른다`() {
        val high = recommendCabinTemp(savedSetTemp = 27.0, savedOutsideTemp = 0.0, currentOutsideTemp = -30.0)
        val low = recommendCabinTemp(savedSetTemp = 19.0, savedOutsideTemp = 0.0, currentOutsideTemp = 20.0)

        assertEquals(28.0, high.temp, 0.0)
        assertEquals(18.0, low.temp, 0.0)
    }

    @Test
    fun `정수 온도는 소수점 없이 표시한다`() {
        assertEquals("24℃", formatTemp(24.0))
        assertEquals("23.5℃", formatTemp(23.5))
        assertEquals("-3℃", formatTemp(-3.0))
    }
}
