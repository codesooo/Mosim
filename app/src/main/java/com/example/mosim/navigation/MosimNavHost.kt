    package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mosim.feature.charging.ChargingStationSelectScreen
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
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
            // B-3(에스코트 계획 확정) 화면이 아직 없어 이동하지 않는다. 다시 점검은 Mock이라 할 일이 없다.
            VehicleStatusScreen(
                onBack = { navController.popBackStack() },
                onShowChargingStations = { navController.navigate(MosimRoute.CHARGING_STATION) },
                onShowEscortPlan = {},
                onRecheck = {},
            )
        }
        composable(MosimRoute.CHARGING_STATION) {
            // B-3 화면이 아직 없어 건너뛰기, 경유지 추가는 이동하지 않는다. 경유지 등록도 미연동이다.
            ChargingStationSelectScreen(
                onBack = { navController.popBackStack() },
                onSkip = {},
                onAddWaypoint = {},
            )
        }
        composable(MosimRoute.PICKUP) {
            PickupGuideScreen(onBack = { navController.popBackStack() })
        }
    }
}
