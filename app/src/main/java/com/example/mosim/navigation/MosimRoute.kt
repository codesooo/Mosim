package com.example.mosim.navigation

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

    // C·D 픽업 · 탑승 (모심 화면 v2)
    const val PICKUP_EN_ROUTE = "pickup/en-route" // C-1
    const val PICKUP_PREP = "pickup/prep" // C-2
    const val BOARDING_CONFIRM = "boarding/confirm" // D-1
}
