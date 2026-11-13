package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechSynthesizer
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
import com.example.mosim.feature.planrevision.PlanRevisionScreen
import com.example.mosim.feature.planrevision.PlanSuggestion
import com.example.mosim.feature.planrevision.SuggestedPlace
import com.example.mosim.feature.planrevision.WaypointRequestState
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
            VehicleStatusScreen(onBack = { navController.popBackStack() })
        }
        composable(MosimRoute.PICKUP) {
            PickupGuideScreen(onBack = { navController.popBackStack() })
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
