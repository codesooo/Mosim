package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mosim.feature.boarding.BoardingConfirmScreen
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupEnRouteScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
import com.example.mosim.feature.pickup.PickupPrepScreen
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

        // C-1 → C-2 → D-1. 정차·탑승 감지로 넘어가는 화면이라 앞 화면을 스택에서 뺀다.
        // 그래야 Back으로 돌아갔을 때 Mock 자동 전환이 다시 돌지 않는다.
        composable(MosimRoute.PICKUP_EN_ROUTE) {
            PickupEnRouteScreen(
                onBack = { navController.popBackStack() },
                onArrived = {
                    navController.navigate(MosimRoute.PICKUP_PREP) {
                        popUpTo(MosimRoute.PICKUP_EN_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        composable(MosimRoute.PICKUP_PREP) {
            PickupPrepScreen(
                onBack = { navController.popBackStack() },
                onBoardingDetected = {
                    navController.navigate(MosimRoute.BOARDING_CONFIRM) {
                        popUpTo(MosimRoute.PICKUP_PREP) { inclusive = true }
                    }
                },
                // TODO: "설정 조정" 화면이 피그마 v2에 없다. 화면이 정해지면 연결한다.
                onAdjustSettings = {},
            )
        }
        composable(MosimRoute.BOARDING_CONFIRM) {
            BoardingConfirmScreen(
                onBack = { navController.popBackStack() },
                // TODO: A-1 동승자 선택 화면이 생기면 그쪽으로 보낸다. 지금은 이전 화면으로 돌아간다.
                onSelectOtherPassenger = { navController.popBackStack() },
                // TODO: E-1 에스코트 모드 화면이 생기면 navigate(MosimRoute.ESCORT)로 연결한다.
                onConfirmBoarding = {},
            )
        }
    }
}
