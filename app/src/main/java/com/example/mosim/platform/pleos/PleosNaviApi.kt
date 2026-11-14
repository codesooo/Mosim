package com.example.mosim.platform.pleos

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointCmdType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * NaviHelper를 suspend 함수로 바꾼다. SDK 타입은 이 파일 밖으로 나가지 않는다.
 *
 * NaviHelper 조회·요청 API는 값을 돌려주지 않고 결과를 리스너 콜백으로 준다(sdk-status.md 3.2).
 * 그래서 콜백을 모두 [events]로 흘려보내고, 각 요청은 자기 결과 이벤트를 기다린다.
 * 콜백이 안 올 수 있어 모든 대기에 시간 제한을 둔다.
 *
 * requestTrip은 쓰지 않는다. 에뮬레이터에서 부른 직후 안내 중이던 경로가 취소됐다(sdk-status.md 검증 기록).
 */
class PleosNaviApi(
    private val naviHelper: NaviHelper,
    private val queryTimeoutMillis: Long = 5_000,
    private val routeTimeoutMillis: Long = 20_000,
) : NaviApi {

    private sealed interface NaviEvent {
        data class Destination(val info: DestinationInfo) : NaviEvent
        data class Waypoints(val list: List<WaypointInfo>) : NaviEvent
        data class Location(val info: CurrentLocationInfo) : NaviEvent
        data class RouteStarted(val info: RouteStartInfo) : NaviEvent
        data class WaypointChanged(val info: WaypointChangedInfo) : NaviEvent
        data class Error(val code: NaviErrorCode) : NaviEvent
        data object Arrived : NaviEvent
    }

    /** 콜백은 SDK 스레드에서 온다. 버퍼를 두어 받는 쪽이 늦어도 tryEmit이 버리지 않게 한다. */
    private val events = MutableSharedFlow<NaviEvent>(extraBufferCapacity = 64)

    private val connection = CompletableDeferred<SourceResult<Unit>>()

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            events.tryEmit(NaviEvent.Destination(destinationInfo))
        }

        override fun onWayPointInfo(waypointInfo: List<WaypointInfo>) {
            events.tryEmit(NaviEvent.Waypoints(waypointInfo))
        }

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            events.tryEmit(NaviEvent.Location(currentLocationInfo))
        }

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            events.tryEmit(NaviEvent.RouteStarted(routeStartInfo))
        }

        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) {
            events.tryEmit(NaviEvent.WaypointChanged(waypointChangedInfo))
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            events.tryEmit(NaviEvent.Arrived)
        }

        override fun onError(errorCode: NaviErrorCode) {
            events.tryEmit(NaviEvent.Error(errorCode))
        }
    }

    /** 목적지 도착 이벤트만 걸러서 내보낸다. 도착 뒤에도 같은 이벤트가 또 오면 그대로 흘린다. */
    override val arrivals: Flow<Unit> = events.filterIsInstance<NaviEvent.Arrived>().map { }

    /** 앱(에스코트 흐름) 시작 때 한 번 부른다. 연결 전 요청은 연결될 때까지 기다린다. */
    fun connect() {
        naviHelper.initialize(
            onConnected = {
                naviHelper.addListener(listener)
                connection.complete(SourceResult.Success(Unit))
            },
            onFailed = { error ->
                connection.complete(SourceResult.Failure(FailureReason.CONNECTION_FAILED, error.message))
            },
        )
    }

    fun release() {
        naviHelper.removeListener(listener)
        naviHelper.release()
    }

    override suspend fun routeProgress(): SourceResult<RouteProgress?> =
        query({ naviHelper.getDestinationInfo() }) { (it as? NaviEvent.Destination)?.info }
            .map { info ->
                // 경로가 없으면 이름이 비고 남은 거리가 0인 값이 온다(에뮬레이터에서 확인).
                if (info.name.isBlank() && info.remainDistance == 0) null
                else RouteProgress(info.name, info.remainDistance, info.remainTime)
            }

    override suspend fun waypointCount(): SourceResult<Int> =
        query({ naviHelper.getWaypointInfo() }) { (it as? NaviEvent.Waypoints)?.list }.map { it.size }

    override suspend fun currentLocation(): SourceResult<GeoPoint> =
        query({ naviHelper.getCurrentLocationInfo() }) { (it as? NaviEvent.Location)?.info }
            .map { GeoPoint(latitude = it.latitude, longitude = it.longitude) }

    override suspend fun requestRoute(destinationName: String, location: GeoPoint): SourceResult<Unit> =
        command(
            request = {
                naviHelper.requestRoute(
                    RouteInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        displayLatitude = null,
                        displayLongitude = null,
                        poiName = destinationName,
                        poiId = "",
                        address = "",
                        poiSubId = "0",
                    ),
                )
            },
            // 에뮬레이터에서 onRoutingCompleted는 오지 않았고 onRouteStarted는 왔다.
            isSuccess = { it is NaviEvent.RouteStarted },
        )

    override suspend fun addWaypoint(slot: Int, name: String, location: GeoPoint): SourceResult<Unit> {
        val index = when (slot) {
            0 -> WaypointIndex.FIRST
            1 -> WaypointIndex.SECOND
            else -> return SourceResult.Failure(FailureReason.INVALID_REQUEST, "경유지 자리 $slot")
        }
        return command(
            request = {
                naviHelper.addWaypoint(
                    RequestWaypointInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        poiName = name,
                        waypointIndex = index,
                        poiId = "",
                        address = "",
                        poiSubId = "0",
                    ),
                )
            },
            isSuccess = { it is NaviEvent.WaypointChanged && it.info.cmdType == WaypointCmdType.ADD },
        )
    }

    /** 조회 요청을 보내고 대응하는 콜백 값을 기다린다. */
    private suspend fun <T : Any> query(request: () -> Unit, pick: (NaviEvent) -> T?): SourceResult<T> {
        (connection.await() as? SourceResult.Failure)?.let { return it }
        val value = try {
            awaitAfter(queryTimeoutMillis, request) { event -> pick(event) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return exception.toFailure()
        }
        return value?.let { SourceResult.Success(it) }
            ?: SourceResult.Failure(FailureReason.CONNECTION_FAILED, "NaviHelper 응답 없음(${queryTimeoutMillis}ms)")
    }

    /** 경로·경유지 요청을 보내고 성공 이벤트나 onError 중 먼저 오는 것을 기다린다. */
    private suspend fun command(request: () -> Unit, isSuccess: (NaviEvent) -> Boolean): SourceResult<Unit> {
        (connection.await() as? SourceResult.Failure)?.let { return it }
        val outcome = try {
            awaitAfter(routeTimeoutMillis, request) { event ->
                when {
                    isSuccess(event) -> SourceResult.Success(Unit)
                    event is NaviEvent.Error -> event.code.toFailure()
                    else -> null
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return exception.toFailure()
        }
        return outcome ?: SourceResult.Failure(FailureReason.CONNECTION_FAILED, "NaviHelper 응답 없음(${routeTimeoutMillis}ms)")
    }

    /** 요청 전에 먼저 구독해 두어야, 응답이 아주 빨리 와도 놓치지 않는다. */
    private suspend fun <T : Any> awaitAfter(timeoutMillis: Long, request: () -> Unit, pick: (NaviEvent) -> T?): T? =
        coroutineScope {
            val answer = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(timeoutMillis) { pick(events.first { pick(it) != null })!! }
            }
            try {
                request()
            } catch (exception: Exception) {
                // 권한 누락 같은 오류는 콜백이 아니라 호출 자리에서 던져진다.
                answer.cancel()
                throw exception
            }
            answer.await()
        }

    private fun NaviErrorCode.toFailure(): SourceResult.Failure {
        val reason = when (this) {
            NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE,
            NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> FailureReason.INVALID_REQUEST
            NaviErrorCode.CAUSE_SEARCH_ROUTE, NaviErrorCode.CAUSE_SEARCH_TRIP -> FailureReason.NETWORK
            else -> FailureReason.UNKNOWN
        }
        return SourceResult.Failure(reason, "NaviErrorCode.$name")
    }
}

private inline fun <T, R> SourceResult<T>.map(transform: (T) -> R): SourceResult<R> = when (this) {
    is SourceResult.Success -> SourceResult.Success(transform(value))
    is SourceResult.Failure -> this
}
