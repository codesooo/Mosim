package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            VehicleStatusScreen(onBack = { navController.popBackStack() })
        }
        composable(MosimRoute.PICKUP) {
            PickupGuideScreen(onBack = { navController.popBackStack() })
        }
    }
}
