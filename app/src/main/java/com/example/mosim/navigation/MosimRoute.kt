package com.example.mosim.navigation

import android.net.Uri

/**
 * 화면 경로를 문자열 상수로 모아둔다.
 * 화면을 추가하면 여기에 route를 먼저 넣고 MosimNavHost에 composable을 붙인다.
 */
object MosimRoute {
    const val VOICE = "voice"
    const val HOME = "home"
    const val PASSENGER = "passenger"
    const val ROUTE = "route"
    const val VEHICLE = "vehicle"
    const val PICKUP = "pickup"

    /** F-1 이동 계획 수정 */
    const val PLAN_REVISION = "plan_revision"

    /** F-3 경유지 후보. F-2가 인식한 문장을 검색어로 받는다. */
    const val WAYPOINT_ARG_QUERY = "query"
    const val WAYPOINT = "waypoint?$WAYPOINT_ARG_QUERY={$WAYPOINT_ARG_QUERY}"

    fun waypoint(query: String) = "waypoint?$WAYPOINT_ARG_QUERY=${Uri.encode(query)}"
}
