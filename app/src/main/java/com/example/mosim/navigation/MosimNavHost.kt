package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechSynthesizer
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.feature.arrival.ArrivalScreen
import com.example.mosim.feature.boarding.BoardingConfirmScreen
import com.example.mosim.feature.charging.ChargingStationSelectScreen
import com.example.mosim.feature.escort.EscortPlanScreen
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupEnRouteScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
import com.example.mosim.feature.pickup.PickupPrepScreen
import com.example.mosim.feature.planrevision.PlanRevisionScreen
import com.example.mosim.feature.planrevision.PlanSuggestion
import com.example.mosim.feature.planrevision.SuggestedPlace
import com.example.mosim.feature.planrevision.WaypointRequestState
import com.example.mosim.feature.profile.ProfileUpdateScreen
import com.example.mosim.feature.route.RouteRecommendScreen
import com.example.mosim.feature.vehicle.VehicleStatusScreen
import com.example.mosim.feature.voice.VoiceAnalysisResult
import com.example.mosim.feature.voice.VoiceScreen
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.PlaceUiModel
import com.example.mosim.feature.waypoint.WaypointRequest
import com.example.mosim.feature.waypoint.WaypointScreen
import com.example.mosim.platform.pleos.PleosTtsAdapter
import com.example.mosim.platform.pleos.SpeechMode
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.delay

/** 흐름 A와 B는 독립 진입한다. 외부 함수가 없으면 미연결로 표시하며 성공을 만들지 않는다. */
@Composable
fun MosimNavHost(
    onSpeechRecognized: (String) -> Unit = {},
    onWaypointRequested: (WaypointRequestSource) -> Unit = {},
    placeSearch: PlaceSearchSource? = null,
    synthesizer: SpeechSynthesizer = rememberPleosSynthesizer(),
    demoFlow: Boolean = false,
    suggestion: PlanSuggestion? = null,
    analyzeRequest: (suspend (String) -> VoiceAnalysisResult)? = null,
    submitWaypoint: (suspend (WaypointRequestSource) -> WaypointSubmissionResult)? = null,
    interactionAllowed: Boolean = false,
    onSuggestionKept: (String) -> Unit = {},
    onSuggestionCancelled: (String) -> Unit = {},
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    var shownSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(suggestion?.id, entry?.destination?.route) {
        val id = suggestion?.id
        // 음성 요청 도중의 선제 제안은 현재 요청 흐름이 끝난 후에 표시한다.
        if (id != null && id != shownSuggestion &&
            entry?.destination?.route == MosimRoute.HOME) {
            shownSuggestion = id
            navController.navigate(MosimRoute.PLAN_REVISION) { launchSingleTop = true }
        }
    }
    val mockSearch = remember { MockPlaceSearchSource() }
    val searchSource = placeSearch ?: if (demoFlow) mockSearch else remember {
        object : PlaceSearchSource {
            override suspend fun search(query: String) = PlaceSearchResult.Failed("장소 검색 기능 연결 예정")
        }
    }
    val scope = rememberCoroutineScope()
    val demoSynthesizer = remember(scope) { MockSpeechSynthesizer(scope) }
    val speech = if (demoFlow) demoSynthesizer else synthesizer
    DisposableEffect(demoSynthesizer) { onDispose { demoSynthesizer.close() } }

    NavHost(navController = navController, startDestination = MosimRoute.HOME) {
        composable(MosimRoute.PLAN_REVISION) {
            PlanRevisionScreen(
                suggestion = suggestion ?: mockPlanSuggestion(),
                synthesizer = speech,
                interactionAllowed = demoFlow || interactionAllowed,
                onBack = {
                    onSuggestionCancelled((suggestion ?: mockPlanSuggestion()).id)
                    navController.popBackStack()
                },
                onKeepRoute = {
                    onSuggestionKept((suggestion ?: mockPlanSuggestion()).id)
                    navController.popBackStack()
                },
                onRequestWaypoint = { onWaypointRequested(WaypointRequestSource.AiSuggestion(it)) },
                submitWaypoint = submitWaypoint?.let { submit ->
                    { place ->
                        when (submit(WaypointRequestSource.AiSuggestion(place))) {
                            WaypointSubmissionResult.Applied -> WaypointRequestState.Applied
                            WaypointSubmissionResult.Failed -> WaypointRequestState.Failed
                            WaypointSubmissionResult.NotConnected -> WaypointRequestState.NotConnected
                        }
                    }
                },
            )
        }
        composable(MosimRoute.VOICE) {
            VoiceScreen(
                onBack = { navController.popBackStack() },
                onSpeechRecognized = onSpeechRecognized,
                mode = if (demoFlow) SpeechMode.Mock else SpeechMode.Pleos,
                beforeStart = speech::stop,
                awaitsAnalysis = true,
                analyzeRequest = analyzeRequest ?: if (demoFlow) {
                    { text ->
                        // Mock 분석 지연. 실제 의도를 분류하거나 외부 API를 호출하지 않는다.
                        delay(900)
                        VoiceAnalysisResult.SearchPlaces(text)
                    }
                } else null,
                onAnalysisResult = { result ->
                    if (result is VoiceAnalysisResult.SearchPlaces) {
                        navController.navigate(MosimRoute.waypoint(result.query)) {
                            popUpTo(MosimRoute.VOICE) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            MosimRoute.WAYPOINT,
            arguments = listOf(navArgument(MosimRoute.WAYPOINT_ARG_QUERY) {
                type = NavType.StringType
                defaultValue = ""
            }),
        ) { entry ->
            WaypointScreen(
                query = entry.arguments?.getString(MosimRoute.WAYPOINT_ARG_QUERY).orEmpty(),
                source = searchSource,
                interactionAllowed = demoFlow || interactionAllowed,
                onBack = { navController.popBackStack() },
                onPlaceSelected = { onWaypointRequested(WaypointRequestSource.DriverPick(it)) },
                submitWaypoint = submitWaypoint?.let { submit ->
                    { place ->
                        when (submit(WaypointRequestSource.DriverPick(place))) {
                            WaypointSubmissionResult.Applied -> WaypointRequest.Applied
                            WaypointSubmissionResult.Failed -> WaypointRequest.Failed
                            WaypointSubmissionResult.NotConnected -> WaypointRequest.NotConnected
                        }
                    }
                },
            )
        }
        composable(MosimRoute.HOME) {
            HomeScreen(onNavigate = { navController.navigate(it) })
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

        // C-1 → C-2 → D-1. 도착 신호, 도어 열림 신호로 넘어가는 화면이라 앞 화면을 스택에서 뺀다.
        // 이미 지나간 단계로 Back해서 돌아가면 신호를 처음부터 다시 받게 되기 때문이다.
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

sealed interface WaypointRequestSource {
    data class AiSuggestion(val place: SuggestedPlace?) : WaypointRequestSource
    data class DriverPick(val place: PlaceUiModel) : WaypointRequestSource
}

/** Applied는 외부 모듈에서 실제 경로 반영을 확인한 경우에만 반환한다. */
enum class WaypointSubmissionResult { NotConnected, Applied, Failed }

@Composable
private fun rememberPleosSynthesizer(): SpeechSynthesizer {
    val context = LocalContext.current.applicationContext
    val speech = remember(context) { PleosTtsAdapter(context) }
    DisposableEffect(speech) { onDispose { speech.close() } }
    return speech
}
