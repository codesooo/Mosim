package com.example.mosim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.mosim.data.EscortSession
import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.feature.escort.EscortMessages
import com.example.mosim.feature.escort.EscortViewModel
import com.example.mosim.feature.escort.ReplanProposal
import com.example.mosim.feature.escort.ReplanReason
import com.example.mosim.feature.escort.VoiceRouting
import com.example.mosim.feature.planrevision.PlanSuggestion
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.ui.components.MapMarker
import com.example.mosim.ui.components.MapMarkerKind
import com.example.mosim.ui.components.MosimMap
import java.time.LocalTime
import com.example.mosim.data.mock.MockPassengerProfiles
import com.example.mosim.data.mock.MockWaypointSearchSource
import com.example.mosim.data.mock.MockSpeechSynthesizer
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.feature.arrival.ArrivalScreen
import com.example.mosim.feature.boarding.BoardingConfirmScreen
import com.example.mosim.feature.charging.ChargingStationSelectScreen
import com.example.mosim.feature.escort.EscortDependencies
import com.example.mosim.feature.escort.EscortPlanScreen
import com.example.mosim.feature.escort.EscortReplanScreen
import com.example.mosim.feature.escort.EscortScreen
import com.example.mosim.feature.home.HomeScreen
import com.example.mosim.feature.passenger.PassengerRegisterScreen
import com.example.mosim.feature.passenger.PassengerSelectScreen
import com.example.mosim.feature.pickup.PickupEnRouteScreen
import com.example.mosim.feature.pickup.PickupGuideScreen
import com.example.mosim.feature.pickup.PickupPrepScreen
import com.example.mosim.feature.planrevision.PlanRevisionScreen
import com.example.mosim.feature.planrevision.SuggestedPlace
import com.example.mosim.feature.planrevision.WaypointRequestState
import com.example.mosim.feature.profile.ProfileUpdateScreen
import com.example.mosim.feature.route.PlaceSearchScreen
import com.example.mosim.feature.route.RouteRecommendScreen
import com.example.mosim.feature.vehicle.VehicleStatusScreen
import com.example.mosim.feature.voice.VoiceAnalysisResult
import com.example.mosim.feature.voice.VoiceScreen
import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceUiModel
import com.example.mosim.feature.waypoint.WaypointRequest
import com.example.mosim.feature.waypoint.WaypointScreen
import com.example.mosim.platform.pleos.PleosTtsAdapter
import com.example.mosim.platform.pleos.SpeechMode
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.delay

/**
 * F-2에서 인식 결과를 기다리는 시간. 이 시간이 지나면 장소 후보(F-3)로 넘어간다.
 * STT 앱 타임아웃 20초보다 짧게 둬서 시연이 오류 화면으로 끝나지 않게 한 임시 값이다.
 */
private const val VOICE_AUTO_ADVANCE_MILLIS = 8_000L

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
    val mockSearch = remember { MockWaypointSearchSource() }
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
        composable(MosimRoute.HOME) {
            HomeScreen(onNavigate = { navController.navigate(it) })
        }
        composable(MosimRoute.PASSENGER) { entry ->
            val registeredId by entry.savedStateHandle
                .getStateFlow<String?>("registeredPassengerId", null).collectAsState()
            PassengerSelectScreen(
                onBack = { navController.popBackStack() },
                onAddPassenger = { navController.navigate(MosimRoute.PASSENGER_REGISTER) },
                onMoveSettings = { id ->
                    // 여기서 고른 동승자를 이동 한 건의 주인공으로 둔다. E-1·G-1이 같은 사람을 보여 준다.
                    EscortSession.passenger = MockPassengerProfiles.passengers.find { it.id == id }
                    navController.navigate("${MosimRoute.ROUTE}?passengerId=$id")
                },
                registeredPassengerId = registeredId,
            )
        }
        composable(MosimRoute.PASSENGER_REGISTER) {
            PassengerRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegister = { passenger ->
                    MockPassengerProfiles.passengers.add(passenger)
                    EscortSession.passenger = passenger
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set("registeredPassengerId", passenger.id)
                    navController.navigate("${MosimRoute.ROUTE}?passengerId=${passenger.id}") {
                        popUpTo(MosimRoute.PASSENGER_REGISTER) { inclusive = true }
                    }
                },
            )
        }
        composable(
            "${MosimRoute.ROUTE}?passengerId={passengerId}",
            arguments = listOf(androidx.navigation.navArgument("passengerId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entry ->
            val passenger = MockPassengerProfiles.passengers.find { it.id == entry.arguments?.getString("passengerId") }
            val pickup by entry.savedStateHandle.getStateFlow("pickup", "강남역 2번 출구").collectAsState()
            val destination by entry.savedStateHandle.getStateFlow("destination", "서울대병원 본관").collectAsState()
            // A-3에서 정한 픽업지·목적지를 이후 단계(E-1 안내, G-1 요약)가 같이 본다.
            LaunchedEffect(pickup, destination) {
                EscortSession.pickup = pickup
                EscortSession.destination = destination
            }
            RouteRecommendScreen(
                pickup = pickup,
                destination = destination,
                onEditPickup = { navController.navigate("${MosimRoute.PLACE_SEARCH}/pickup") },
                onEditDestination = { navController.navigate("${MosimRoute.PLACE_SEARCH}/destination") },
                onBack = { navController.popBackStack() },
                subtitle = passenger?.let { "${it.name} · ${it.personalization}" },
                // A-3에서 바로 출발 전 점검(B-1)으로 간다. A 단계로 되돌아갈 수 있게 스택은 그대로 둔다.
                onStartCheck = { navController.navigate(MosimRoute.VEHICLE) },
            )
        }
        composable("${MosimRoute.PLACE_SEARCH}/{field}") { entry ->
            val field = requireNotNull(entry.arguments?.getString("field"))
            PlaceSearchScreen(
                isPickup = field == "pickup",
                onBack = { navController.popBackStack() },
                onSelect = { name ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(field, name)
                    navController.popBackStack()
                },
            )
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
            EscortPlanScreen(
                chargingStationName = entry.arguments?.getString(MosimRoute.ESCORT_PLAN_STATION_ARG),
                onBack = { navController.popBackStack() },
                // B-3에서 안내를 시작하면 픽업지로 이동한다(C-1).
                // 점검 단계로 Back해서 돌아가도 할 일이 없어 B 화면들을 스택에서 뺀다.
                onStartGuide = {
                    navController.navigate(MosimRoute.PICKUP_EN_ROUTE) {
                        popUpTo(MosimRoute.VEHICLE) { inclusive = true }
                    }
                },
            )
        }
        composable(MosimRoute.ARRIVAL) {
            ArrivalScreen(
                onBack = { navController.popBackStack() },
                onConfirmProfileUpdate = { navController.navigate(MosimRoute.PROFILE_UPDATE) },
                // 방금 끝난 에스코트가 있으면 실제로 한 일을 보여 준다. 없으면 화면의 Mock 값이다.
                summary = EscortSession.summary,
            )
        }
        composable(MosimRoute.PROFILE_UPDATE) {
            // 저장은 프로세스 안에서만 기억한다. 서버·영속 저장은 없다 [확인 필요].
            ProfileUpdateScreen(
                onBack = { navController.popBackStack() },
                onSkipSave = { navController.endTrip() },
                onSaveSelected = {
                    EscortSession.profileSaved = true
                    navController.endTrip()
                },
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
                // 탑승을 확인하면 에스코트가 시작된다. 뒤로 돌아가면 탑승 신호를 처음부터 다시 받게 되어
                // D-1을 스택에서 뺀다. 탑승 시각은 이 시점으로 기록해 E-1 부제목에 쓴다.
                onConfirmBoarding = {
                    EscortSession.boardedAt = LocalTime.now().withSecond(0).withNano(0)
                    navController.navigate(MosimRoute.ESCORT_FLOW) {
                        popUpTo(MosimRoute.BOARDING_CONFIRM) { inclusive = true }
                    }
                },
            )
        }

        navigation(
            startDestination = MosimRoute.ESCORT,
            route = MosimRoute.ESCORT_FLOW,
        ) {
            composable(MosimRoute.ESCORT) { entry ->
                EscortScreen(
                    viewModel = escortViewModel(navController, entry),
                    onProposal = {
                        navController.navigate(MosimRoute.ESCORT_REPLAN) {
                            launchSingleTop = true
                        }
                    },
                    // 동승자 음성 요청(F-2). 주행 제한이 걸리면 화면이 시작 버튼을 막는다.
                    onVoiceRequest = {
                        navController.navigate(MosimRoute.VOICE) { launchSingleTop = true }
                    },
                    // 도착하면 G-1로 간다. 끝난 에스코트로 Back해 돌아가지 못하게 흐름을 통째로 뺀다.
                    onArrived = {
                        navController.navigate(MosimRoute.ARRIVAL) {
                            popUpTo(MosimRoute.ESCORT_FLOW) { inclusive = true }
                        }
                    },
                    interactionAllowed = demoFlow || interactionAllowed,
                )
            }

            // F-1. E가 만든 제안을 F-1 화면(planrevision)으로 그린다.
            // 화면과 TTS는 F-1 구현을 그대로 쓰고, 경유지 추가는 에스코트의 실제 경로 기능을 쓴다.
            composable(MosimRoute.ESCORT_REPLAN) { entry ->
                val model = escortViewModel(navController, entry)
                val escortState by model.uiState.collectAsState()
                val proposal = escortState.proposal
                if (proposal == null) {
                    // 제안이 사라졌으면(승인·거절) E-1로 돌아간다. 그리는 중에 이동하지 않도록 효과로 미룬다.
                    LaunchedEffect(Unit) { navController.popBackStack(MosimRoute.ESCORT, inclusive = false) }
                } else {
                    PlanRevisionScreen(
                        suggestion = proposal.toSuggestion(escortState.passengerName, escortState.isSimulated),
                        synthesizer = speech,
                        interactionAllowed = demoFlow || interactionAllowed,
                        onBack = { model.keepCurrentRoute() },
                        onKeepRoute = { model.keepCurrentRoute() },
                        onRequestWaypoint = { place ->
                            onWaypointRequested(WaypointRequestSource.AiSuggestion(place))
                        },
                        submitWaypoint = {
                            // 실제 경유지 추가가 성공했을 때만 Applied다.
                            if (model.acceptProposalAndWait()) {
                                WaypointRequestState.Applied
                            } else {
                                WaypointRequestState.Failed
                            }
                        },
                        mapContent = {
                            MosimMap(
                                markers = proposalMarkers(proposal),
                                placeholderText = "지도 · 현재 위치 + 제안 장소",
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    )
                }
            }

            // F-2 음성 요청. 에스코트 그래프 안에 둬서 E와 같은 ViewModel에 결과를 넘긴다.
            composable(MosimRoute.VOICE) { entry ->
                val model = escortViewModel(navController, entry)
                VoiceScreen(
                    onBack = { navController.returnToEscort() },
                    onSpeechRecognized = onSpeechRecognized,
                    mode = if (demoFlow) SpeechMode.Mock else SpeechMode.Pleos,
                    beforeStart = speech::stop,
                    awaitsAnalysis = true,
                    // 의도 해석은 에스코트의 VoiceCommandParser를 그대로 쓴다.
                    analyzeRequest = analyzeRequest ?: { text ->
                        when (val routing = model.handleVoiceText(text)) {
                            is VoiceRouting.SearchPlaces -> VoiceAnalysisResult.SearchPlaces(routing.query)
                            is VoiceRouting.Handled ->
                                VoiceAnalysisResult.Message(routing.message ?: "요청을 처리했어요")
                        }
                    },
                    // 실기 마이크 인식이 미검증이라, 인식 결과가 없어도 시연이 F-2에서 멈추지 않게
                    // 이 시간이 지나면 장소 후보 화면으로 넘긴다. 인식이 검증되면 0으로 되돌린다.
                    autoAdvanceMillis = VOICE_AUTO_ADVANCE_MILLIS,
                    onAnalysisResult = { result ->
                        when (result) {
                            // 장소 요청이면 후보를 고르는 F-3으로. 되돌아올 단계가 아니라 F-2를 뺀다.
                            is VoiceAnalysisResult.SearchPlaces ->
                                navController.navigate(MosimRoute.waypoint(result.query)) {
                                    popUpTo(MosimRoute.VOICE) { inclusive = true }
                                }
                            // 그 밖의 명령은 E가 이미 처리했다. 결과 안내는 E-1 토스트로 보인다.
                            is VoiceAnalysisResult.Message -> navController.returnToEscort()
                        }
                    },
                )
            }

            // F-3 경유지 후보.
            composable(
                MosimRoute.WAYPOINT,
                arguments = listOf(navArgument(MosimRoute.WAYPOINT_ARG_QUERY) {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val model = escortViewModel(navController, entry)
                val query = entry.arguments?.getString(MosimRoute.WAYPOINT_ARG_QUERY).orEmpty()
                // 검색 기준 위치는 검색할 때 정해진다. 지도에 현재 위치를 찍는 데 쓴다.
                var origin by remember { mutableStateOf<GeoPoint?>(null) }
                val source = placeSearch ?: remember(model) {
                    escortPlaceSearch(model) { origin = it }
                }
                WaypointScreen(
                    query = query,
                    source = source,
                    interactionAllowed = demoFlow || interactionAllowed,
                    onBack = { navController.returnToEscort() },
                    origin = origin,
                    onPlaceSelected = { onWaypointRequested(WaypointRequestSource.DriverPick(it)) },
                    submitWaypoint = { place ->
                        val location = place.location
                        if (location == null) {
                            // 좌표를 모르는 후보는 경유지로 보낼 수 없다. 성공으로 꾸미지 않는다.
                            WaypointRequest.NotConnected
                        } else if (model.addWaypointAt(place.name, location)) {
                            // 성공하면 E-1로 돌아간다. 추가 결과는 E-1 토스트로 보인다.
                            navController.returnToEscort()
                            WaypointRequest.Applied
                        } else {
                            WaypointRequest.Failed
                        }
                    },
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
private fun escortViewModel(
    navController: NavController,
    entry: NavBackStackEntry,
): EscortViewModel {
    val flowEntry = remember(entry) {
        navController.getBackStackEntry(MosimRoute.ESCORT_FLOW)
    }
    val context = LocalContext.current
    return viewModel(
        viewModelStoreOwner = flowEntry,
    ) {
        EscortDependencies.createViewModel(context)
    }
}

sealed interface WaypointRequestSource {
    data class AiSuggestion(val place: SuggestedPlace?) : WaypointRequestSource
    data class DriverPick(val place: PlaceUiModel) : WaypointRequestSource
}

/** Applied는 외부 모듈에서 실제 경로 반영을 확인한 경우에만 반환한다. */
enum class WaypointSubmissionResult {
    NotConnected,
    Applied,
    Failed,
}

@Composable
private fun rememberPleosSynthesizer(): SpeechSynthesizer {
    val context = LocalContext.current.applicationContext
    val speech = remember(context) { PleosTtsAdapter(context) }
    DisposableEffect(speech) {
        onDispose { speech.close() }
    }
    return speech
}

/**
 * 음성 요청 흐름(F-2 · F-3)에서 에스코트 화면으로 돌아간다.
 * 개발용 HOME에서 바로 F-2에 들어온 경우에는 에스코트가 스택에 없어 한 단계만 되돌린다.
 */
private fun NavController.returnToEscort() {
    if (!popBackStack(MosimRoute.ESCORT, inclusive = false)) popBackStack()
}

/** 이동 한 건이 끝났다. 기록을 비우고 개발용 HOME으로 돌아간다. */
private fun NavController.endTrip() {
    EscortSession.reset()
    navigate(MosimRoute.HOME) { popUpTo(MosimRoute.HOME) { inclusive = true } }
}

/**
 * 에스코트가 만든 제안을 F-1 화면이 쓰는 형태로 바꾼다.
 *
 * 변경 후 도착 시각과 추가 소요시간은 담지 않는다. 경유지를 넣고 재탐색을 해야 알 수 있는 값이라
 * 지금 계산하면 지어내는 것이 된다. 화면은 모르는 값을 "—"로 보여 준다 [확인 필요].
 */
private fun ReplanProposal.toSuggestion(passengerName: String, isMock: Boolean) = PlanSuggestion(
    id = id,
    headline = message,
    reason = when (val reason = reason) {
        is ReplanReason.Delay ->
            "도착이 ${EscortMessages.clock(reason.plannedEta)} → ${EscortMessages.clock(reason.delayedEta)}로 늦어졌어요"
        is ReplanReason.VoiceRequest -> "동승자 요청 ${EscortMessages.spokenRequest(reason.spokenText)}"
    },
    place = SuggestedPlace(
        name = place.name,
        highlight = EscortMessages.placeSummary(place.categoryLabel, place.distanceMeters).orEmpty(),
        externalId = place.id,
    ),
    speech = message,
    passengerName = passengerName,
    isMock = isMock,
)

private fun proposalMarkers(proposal: ReplanProposal): List<MapMarker> = listOfNotNull(
    proposal.searchOrigin?.let { MapMarker(it, MapMarkerKind.CURRENT_LOCATION) },
    MapMarker(proposal.place.location, MapMarkerKind.PLACE),
)

/**
 * F-3의 장소 검색을 에스코트 쪽 검색(카카오 또는 Mock)에 연결하는 어댑터다.
 * 검색 로직은 [EscortViewModel]에 있는 것을 그대로 쓰고, 여기서는 화면이 쓰는 모델로만 바꾼다.
 */
private fun escortPlaceSearch(
    model: EscortViewModel,
    onOrigin: (GeoPoint?) -> Unit,
) = object : PlaceSearchSource {
    override suspend fun search(query: String): PlaceSearchResult {
        val outcome = model.searchPlacesFor(query)
        onOrigin(outcome.origin)
        val places = outcome.places
            ?: return PlaceSearchResult.Failed(outcome.failureMessage ?: "장소를 찾지 못했어요")
        if (places.isEmpty()) return PlaceSearchResult.Empty
        return PlaceSearchResult.Found(places.map { it.toUiModel() })
    }
}

/** Mock 여부는 Mock 검색이 붙이는 id 접두사로 본다. 실제 카카오 결과에는 이 접두사가 없다. */
private fun PlaceCandidate.toUiModel() = PlaceUiModel(
    externalId = id,
    name = name,
    detail = EscortMessages.placeSummary(categoryLabel, distanceMeters).orEmpty(),
    isMock = id.startsWith("mock-"),
    location = location,
)
