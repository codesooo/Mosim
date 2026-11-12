package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.mosim.data.mock.MockPassengerProfiles
import com.example.mosim.feature.passenger.PassengerRegisterScreen
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
        composable(MosimRoute.PASSENGER) { entry ->
            val registeredId by entry.savedStateHandle
                .getStateFlow<String?>("registeredPassengerId", null).collectAsState()
            PassengerSelectScreen(
                onBack = { navController.popBackStack() },
                onAddPassenger = { navController.navigate(MosimRoute.PASSENGER_REGISTER) },
                registeredPassengerId = registeredId,
            )
        }
        composable(MosimRoute.PASSENGER_REGISTER) {
            PassengerRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegister = { passenger ->
                    MockPassengerProfiles.passengers.add(passenger)
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set("registeredPassengerId", passenger.id)
                    navController.popBackStack()
                },
            )
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
