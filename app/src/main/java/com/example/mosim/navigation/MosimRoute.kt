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

    /** 에스코트 흐름(E-1 · F-1)을 묶는 중첩 그래프. 두 화면이 EscortViewModel 하나를 같이 쓴다. */
    const val ESCORT_FLOW = "escort_flow"
    const val ESCORT = "escort"
    const val ESCORT_REPLAN = "escort_replan"
}
