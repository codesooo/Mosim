package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 에스코트에 필요한 NaviHelper 호출만 모은 경계다.
 * 실제 구현은 [PleosNaviApi]이고, PleosTripSource 단위 테스트에서는 가짜로 바꿔 끼운다.
 *
 * 단위는 Pleos 에뮬레이터에서 확인했다(sdk-status.md 검증 기록): 거리 m, 시간 초.
 */
interface NaviApi {

    /**
     * 목적지 도착 이벤트(onDestinationArrived). 기본값은 비어 있는 흐름이라
     * 도착을 알려 주지 않는 구현(테스트용 가짜)은 그대로 둬도 된다.
     */
    val arrivals: Flow<Unit> get() = emptyFlow()

    /** 안내 중인 경로의 목적지 진행 상황. 경로가 없으면 Success(null). */
    suspend fun routeProgress(): SourceResult<RouteProgress?>

    suspend fun waypointCount(): SourceResult<Int>

    suspend fun currentLocation(): SourceResult<GeoPoint>

    /** 경로 탐색이 시작되면(onRouteStarted) Success. */
    suspend fun requestRoute(destinationName: String, location: GeoPoint): SourceResult<Unit>

    /** 경유지 변경 이벤트(ADD)가 오면 Success. [slot]은 0(FIRST) 또는 1(SECOND). */
    suspend fun addWaypoint(slot: Int, name: String, location: GeoPoint): SourceResult<Unit>
}

data class RouteProgress(
    val destinationName: String,
    val remainDistanceMeters: Int,
    val remainSeconds: Int,
)
