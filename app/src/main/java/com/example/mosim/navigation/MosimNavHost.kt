    package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mosim.feature.arrival.ArrivalScreen
import com.example.mosim.feature.charging.ChargingStationSelectScreen
import com.example.mosim.feature.escort.EscortPlanScreen
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
import com.example.mosim.feature.profile.ProfileUpdateScreen
import com.example.mosim.feature.route.RouteRecommendScreen
import com.example.mosim.feature.vehicle.VehicleStatusScreen

@Composable
fun MosimNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MosimRoute.HOME) {
        composable(MosimRoute.HOME) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(MosimRoute.PASSENGER) {
            PassengerSelectScreen(onBack = { navController.popBackStack() })
        }
        composable(MosimRoute.ROUTE) {
            RouteRecommendScreen(onBack = { navController.popBackStack() })
        }
        composable(MosimRoute.VEHICLE) {
            // 다시 점검은 Mock이라 할 일이 없다.
            VehicleStatusScreen(
                onBack = { navController.popBackStack() },
                onShowChargingStations = { navController.navigate(MosimRoute.CHARGING_STATION) },
                onShowEscortPlan = { navController.navigate(MosimRoute.ESCORT_PLAN) },
                onRecheck = {},
            )
        }
        composable(MosimRoute.CHARGING_STATION) {
            // 두 버튼 모두 B-3으로 간다. 경유지 추가만 고른 충전소 이름을 B-3에 넘긴다.
            // 경유지를 실제로 등록하는 기능(NaviHelper)은 미연동이다.
            ChargingStationSelectScreen(
                onBack = { navController.popBackStack() },
                onSkip = { navController.navigate(MosimRoute.ESCORT_PLAN) },
                onAddWaypoint = { stationName -> navController.navigate(MosimRoute.escortPlan(stationName)) },
            )
        }
        composable(
            route = MosimRoute.ESCORT_PLAN_PATTERN,
            arguments = listOf(
                navArgument(MosimRoute.ESCORT_PLAN_STATION_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            // 안내 시작 이후 화면이 정해지지 않아 이동하지 않는다. [확인 필요]
            EscortPlanScreen(
                chargingStationName = entry.arguments?.getString(MosimRoute.ESCORT_PLAN_STATION_ARG),
                onBack = { navController.popBackStack() },
                onStartGuide = {},
            )
        }
        composable(MosimRoute.ARRIVAL) {
            ArrivalScreen(
                onBack = { navController.popBackStack() },
                onConfirmProfileUpdate = { navController.navigate(MosimRoute.PROFILE_UPDATE) },
            )
        }
        composable(MosimRoute.PROFILE_UPDATE) {
            // 저장 안 함, 선택 항목 저장 이후 화면과 저장 기능이 정해지지 않아 이동하지 않는다. [확인 필요]
            ProfileUpdateScreen(
                onBack = { navController.popBackStack() },
                onSkipSave = {},
                onSaveSelected = {},
            )
        }
        composable(MosimRoute.PICKUP) {
            PickupGuideScreen(onBack = { navController.popBackStack() })
        }
    }
}
