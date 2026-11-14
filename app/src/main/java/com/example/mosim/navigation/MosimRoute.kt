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
    const val CHARGING_STATION = "charging_station"
    const val ESCORT_PLAN = "escort_plan"
    const val ARRIVAL = "arrival"
    const val PROFILE_UPDATE = "profile_update"

    // B-3은 B-2에서 고른 충전소 이름을 선택 인자로 받는다. B-1, B-2 건너뛰기에서는 인자 없이 ESCORT_PLAN으로 간다.
    const val ESCORT_PLAN_STATION_ARG = "chargingStation"
    const val ESCORT_PLAN_PATTERN = "$ESCORT_PLAN?$ESCORT_PLAN_STATION_ARG={$ESCORT_PLAN_STATION_ARG}"

    fun escortPlan(chargingStationName: String) =
        "$ESCORT_PLAN?$ESCORT_PLAN_STATION_ARG=${Uri.encode(chargingStationName)}"

    /** F-1 이동 계획 수정 */
    const val PLAN_REVISION = "plan_revision"

    /** F-3 경유지 후보. F-2가 인식한 문장을 검색어로 받는다. */
    const val WAYPOINT_ARG_QUERY = "query"
    const val WAYPOINT = "waypoint?$WAYPOINT_ARG_QUERY={$WAYPOINT_ARG_QUERY}"

    fun waypoint(query: String) = "waypoint?$WAYPOINT_ARG_QUERY=${Uri.encode(query)}"

    // C·D 픽업 · 탑승 (모심 화면 v2)
    const val PICKUP_EN_ROUTE = "pickup/en-route" // C-1
    const val PICKUP_PREP = "pickup/prep" // C-2
    const val BOARDING_CONFIRM = "boarding/confirm" // D-1
}
