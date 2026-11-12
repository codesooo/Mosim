package com.example.mosim.ui.components

import com.example.mosim.platform.pleos.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class MapCameraTest {

    private val here = GeoPoint(37.5665, 126.978)
    private val place = GeoPoint(37.58, 126.99)

    @Test
    fun `점이 없으면 카메라를 옮기지 않는다`() {
        assertEquals(MapCamera.Default, MapCamera.of(emptyList()))
    }

    @Test
    fun `점이 하나면 그 점을 가운데에 둔다`() {
        assertEquals(MapCamera.Center(place), MapCamera.of(listOf(MapMarker(place, MapMarkerKind.PLACE))))
    }

    @Test
    fun `점이 여럿이면 모두 보이게 맞춘다`() {
        val markers = listOf(MapMarker(here, MapMarkerKind.CURRENT_LOCATION), MapMarker(place, MapMarkerKind.PLACE))

        assertEquals(MapCamera.Fit(listOf(here, place)), MapCamera.of(markers))
    }

    @Test
    fun `맞춤 여백은 지도의 짧은 변의 4분의 1이다`() {
        // 넓은 창 지도(가로 844, 세로 900)와 좁은 창 지도(가로 744, 세로 250)
        assertEquals(211, MapCamera.fitPaddingPx(widthPx = 844, heightPx = 900))
        assertEquals(62, MapCamera.fitPaddingPx(widthPx = 744, heightPx = 250))
    }
}
