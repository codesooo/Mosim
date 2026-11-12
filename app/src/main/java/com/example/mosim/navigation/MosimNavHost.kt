package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.mosim.feature.escort.EscortDependencies
import com.example.mosim.feature.escort.EscortReplanScreen
import com.example.mosim.feature.escort.EscortScreen
import com.example.mosim.feature.escort.EscortViewModel
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

        navigation(startDestination = MosimRoute.ESCORT, route = MosimRoute.ESCORT_FLOW) {
            composable(MosimRoute.ESCORT) { entry ->
                EscortScreen(
                    viewModel = escortViewModel(navController, entry),
                    // [확인 필요] 임시 메인에서 들어오므로 돌아갈 수 있게 둔다. 실제 흐름에서는 최상단(null) 검토.
                    onBack = { navController.popBackStack() },
                    onProposal = { navController.navigate(MosimRoute.ESCORT_REPLAN) { launchSingleTop = true } },
                )
            }
            composable(MosimRoute.ESCORT_REPLAN) { entry ->
                EscortReplanScreen(
                    viewModel = escortViewModel(navController, entry),
                    onClosed = { navController.popBackStack(MosimRoute.ESCORT, inclusive = false) },
                )
            }
        }
    }
}

/**
 * 에스코트 흐름 그래프에 붙은 ViewModel을 꺼낸다.
 * 그래프 단위로 붙이면 E-1과 F-1이 같은 상태를 보고, 흐름을 벗어나면 ViewModel과 Mock이 함께 정리된다.
 */
@Composable
private fun escortViewModel(navController: NavController, entry: NavBackStackEntry): EscortViewModel {
    val flowEntry = remember(entry) { navController.getBackStackEntry(MosimRoute.ESCORT_FLOW) }
    val context = LocalContext.current
    return viewModel(viewModelStoreOwner = flowEntry) { EscortDependencies.createViewModel(context) }
}
