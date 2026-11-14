package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import com.example.mosim.platform.pleos.TripSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class MockTripSourceTest {

    private val place = GeoPoint(37.57, 126.98)

    @Test
    fun `경유지를 추가하면 재탐색 뒤 도착이 늦어지고 경유지 수가 는다`() = runBlocking {
        val trip = MockTripSource(scope = this, requestDelayMillis = 0, routingDelayMillis = 20, detourMinutes = 5)

        assertEquals(SourceResult.Success(Unit), trip.addWaypoint("카페 A", place))

        val updated = withTimeout(1_000) { trip.tripStatus.first { it?.waypointCount == 1 } }!!
        assertEquals(LocalTime.of(9, 17), updated.eta)
        assertEquals(48, updated.remainMinutes)
    }

    @Test
    fun `경유지는 두 개까지만 받는다`() = runBlocking {
        val full = MockTripSource.DEFAULT_STATUS.copy(waypointCount = TripSource.MAX_WAYPOINTS)
        val trip = MockTripSource(scope = this, initialStatus = full, requestDelayMillis = 0)

        val result = trip.addWaypoint("카페 A", place)

        assertTrue(result is SourceResult.Failure && result.reason == FailureReason.INVALID_REQUEST)
    }

    @Test
    fun `안내 중인 경로가 없으면 경유지를 추가할 수 없다`() = runBlocking {
        val trip = MockTripSource(scope = this, initialStatus = null, requestDelayMillis = 0)

        val result = trip.addWaypoint("카페 A", place)

        assertTrue(result is SourceResult.Failure && result.reason == FailureReason.INVALID_REQUEST)
    }

    @Test
    fun `위치를 모르면 임의 좌표 대신 실패를 돌려준다`() = runBlocking {
        val trip = MockTripSource(scope = this, location = null, requestDelayMillis = 0)

        assertTrue(trip.currentLocation() is SourceResult.Failure)
    }

    @Test
    fun `정체 시뮬레이션은 도착 시각과 남은 시간을 늘린다`() = runBlocking {
        val trip = MockTripSource(scope = this)

        trip.simulateTrafficDelay(30)

        assertEquals(LocalTime.of(9, 42), trip.tripStatus.value?.eta)
        assertEquals(73, trip.tripStatus.value?.remainMinutes)
    }
}
