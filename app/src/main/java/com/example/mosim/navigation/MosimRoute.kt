package com.example.mosim.navigation

import android.net.Uri

/**
 * 화면 경로를 문자열 상수로 모아둔다.
 * 화면을 추가하면 여기에 route를 먼저 넣고 MosimNavHost에 composable을 붙인다.
 */
object MosimRoute {
    const val HOME = "home"
    const val PASSENGER = "passenger"
    const val ROUTE = "route"
    const val VEHICLE = "vehicle"
    const val PICKUP = "pickup"
    const val CHARGING_STATION = "charging_station"
    const val ESCORT_PLAN = "escort_plan"
    const val ARRIVAL = "arrival"

    // B-3은 B-2에서 고른 충전소 이름을 선택 인자로 받는다. B-1, B-2 건너뛰기에서는 인자 없이 ESCORT_PLAN으로 간다.
    const val ESCORT_PLAN_STATION_ARG = "chargingStation"
    const val ESCORT_PLAN_PATTERN = "$ESCORT_PLAN?$ESCORT_PLAN_STATION_ARG={$ESCORT_PLAN_STATION_ARG}"

    fun escortPlan(chargingStationName: String) =
        "$ESCORT_PLAN?$ESCORT_PLAN_STATION_ARG=${Uri.encode(chargingStationName)}"
}
