package com.example.mosim.platform.pleos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.util.concurrent.Executors

class PleosTripSourceTest {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val navi = FakeNaviApi()
    private val hospital = GeoPoint(37.5795590, 126.9990026)

    @After
    fun tearDown() {
        scope.cancel()
        dispatcher.close()
    }

    private fun createSource() = PleosTripSource(
        scope = scope,
        navi = navi,
        clock = { LocalTime.of(8, 29, 40) },
        refreshIntervalMillis = 30,
    )

    private fun PleosTripSource.awaitStatus(predicate: (TripStatus?) -> Boolean): TripStatus? =
        runBlocking { withTimeout(2_000) { tripStatus.first(predicate) } }

    @Test
    fun `남은 초를 지금 시각에 더해 도착 시각을 만들고 분은 올림한다`() {
        // 에뮬레이터 실측: 23km, 3780초
        navi.progress = RouteProgress("서울대학교병원 본관", 23_000, 3_780)

        val status = createSource().awaitStatus { it != null }!!

        assertEquals(LocalTime.of(9, 32), status.eta)
        assertEquals(23_000, status.remainDistanceMeters)
        assertEquals(63, status.remainMinutes)
        assertEquals(0, status.waypointCount)
    }

    @Test
    fun `경로가 없으면 상태는 null이다`() {
        navi.progress = null
        val source = createSource()

        assertNull(source.awaitStatus { true })
    }

    @Test
    fun `경로가 없으면 목적지로 경로를 요청하고, 있으면 요청하지 않는다`() {
        navi.progress = null
        val source = createSource()

        assertEquals(SourceResult.Success(Unit), runBlocking { source.ensureRoute("서울대병원 본관", hospital) })
        assertEquals(listOf("서울대병원 본관"), navi.routeRequests)
        source.awaitStatus { it != null }

        runBlocking { source.ensureRoute("서울대병원 본관", hospital) }
        assertEquals(1, navi.routeRequests.size)
    }

    @Test
    fun `경유지는 비어 있는 첫 자리에 넣는다`() {
        navi.progress = RouteProgress("병원", 23_000, 3_780)
        navi.waypoints = 1
        val source = createSource()
        source.awaitStatus { it != null }

        val result = runBlocking { source.addWaypoint("카페 A", GeoPoint(37.56, 126.97)) }

        assertEquals(SourceResult.Success(Unit), result)
        assertEquals(listOf(1 to "카페 A"), navi.waypointRequests)
        assertEquals(2, source.awaitStatus { it?.waypointCount == 2 }!!.waypointCount)
    }

    @Test
    fun `경유지가 다 찼거나 경로가 없으면 요청하지 않고 실패한다`() {
        navi.progress = RouteProgress("병원", 23_000, 3_780)
        navi.waypoints = 2
        val source = createSource()
        source.awaitStatus { it != null }

        val full = runBlocking { source.addWaypoint("카페 A", GeoPoint(37.56, 126.97)) }

        assertEquals(FailureReason.INVALID_REQUEST, (full as SourceResult.Failure).reason)
        assertTrue(navi.waypointRequests.isEmpty())
    }

    @Test
    fun `조회가 한 번 실패해도 이전 도착 시각을 지우지 않는다`() {
        navi.progress = RouteProgress("병원", 23_000, 3_780)
        val source = createSource()
        source.awaitStatus { it != null }

        navi.progressFailure = SourceResult.Failure(FailureReason.CONNECTION_FAILED)
        runBlocking { kotlinx.coroutines.delay(100) }

        assertEquals(LocalTime.of(9, 32), source.tripStatus.value?.eta)
    }

    @Test
    fun `내비가 경로 요청을 거절하면 그 이유를 돌려준다`() {
        navi.progress = null
        navi.routeFailure = SourceResult.Failure(FailureReason.NETWORK, "NaviErrorCode.CAUSE_SEARCH_ROUTE")

        val result = runBlocking { createSource().ensureRoute("병원", hospital) }

        assertEquals(FailureReason.NETWORK, (result as SourceResult.Failure).reason)
    }

    /** 에뮬레이터에서 본 NaviHelper 동작을 흉내 낸다. 경로 요청이 성공하면 경로가 생기고, 경유지를 넣으면 개수가 는다. */
    private class FakeNaviApi : NaviApi {
        @Volatile var progress: RouteProgress? = null
        @Volatile var waypoints = 0
        @Volatile var progressFailure: SourceResult.Failure? = null
        @Volatile var routeFailure: SourceResult.Failure? = null
        val routeRequests = java.util.concurrent.CopyOnWriteArrayList<String>()
        val waypointRequests = java.util.concurrent.CopyOnWriteArrayList<Pair<Int, String>>()

        override suspend fun routeProgress(): SourceResult<RouteProgress?> = progressFailure ?: SourceResult.Success(progress)

        override suspend fun waypointCount(): SourceResult<Int> = SourceResult.Success(waypoints)

        override suspend fun currentLocation(): SourceResult<GeoPoint> = SourceResult.Success(GeoPoint(37.4127, 127.0947))

        override suspend fun requestRoute(destinationName: String, location: GeoPoint): SourceResult<Unit> {
            routeFailure?.let { return it }
            routeRequests += destinationName
            progress = RouteProgress(destinationName, 23_000, 3_780)
            return SourceResult.Success(Unit)
        }

        override suspend fun addWaypoint(slot: Int, name: String, location: GeoPoint): SourceResult<Unit> {
            waypointRequests += slot to name
            waypoints++
            return SourceResult.Success(Unit)
        }
    }
}
