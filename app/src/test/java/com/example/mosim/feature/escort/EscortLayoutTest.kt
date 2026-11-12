package com.example.mosim.feature.escort

import org.junit.Assert.assertEquals
import org.junit.Test

class EscortLayoutTest {

    @Test
    fun 분할된_반쪽_창은_좁은_배치다() {
        assertEquals(EscortLayout.COMPACT, EscortLayout.of(840f))
    }

    @Test
    fun 앱_영역_전체는_넓은_배치다() {
        assertEquals(EscortLayout.WIDE, EscortLayout.of(1682f))
    }

    @Test
    fun 경계값부터_넓은_배치다() {
        assertEquals(EscortLayout.COMPACT, EscortLayout.of(1199f))
        assertEquals(EscortLayout.WIDE, EscortLayout.of(1200f))
    }
}
