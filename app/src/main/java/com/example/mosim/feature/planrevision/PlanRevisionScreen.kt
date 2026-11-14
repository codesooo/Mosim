package com.example.mosim.feature.planrevision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mosim.data.mock.MockSpeechSynthesizer
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.data.mock.mockPlanSuggestionWithoutTimes
import com.example.mosim.platform.pleos.SpeechSynthesizer
import com.example.mosim.ui.theme.MosimTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * F-1 이동 계획 수정 (피그마 "모심 화면 v2" F-1을 운전자 판단 중심으로 다시 짰다).
 *
 * 운전자가 봐야 하는 것은 "쉬어갈지 말지" 하나다. 그래서 제안 한 줄을 가장 크게 두고,
 * 장소·시간·음성 제어·결정 버튼을 한 카드 안에 중요도 순으로 쌓았다.
 * 바뀌기 전 소요·도착 시각은 판단에 쓰이지 않아 뺐다.
 *
 * 생김새는 `PlanRevisionLayout.kt`에 있다. 이 파일은 상태와 SDK 호출만 다룬다.
 * 제안·장소·시간은 외부 AI 모듈이 [PlanSuggestion]으로 넘겨 주고,
 * 경유지 추가는 여기서 **요청만** 한다. 실제 반영은 NaviHelper 담당자 몫이다.
 */
@Composable
fun PlanRevisionScreen(
    suggestion: PlanSuggestion,
    synthesizer: SpeechSynthesizer,
    onBack: () -> Unit,
    onKeepRoute: () -> Unit,
    onRequestWaypoint: (SuggestedPlace?) -> Unit,
    modifier: Modifier = Modifier,
    submitWaypoint: (suspend (SuggestedPlace) -> WaypointRequestState)? = null,
    interactionAllowed: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val model: PlanRevisionViewModel = viewModel(
        key = "plan-revision",
        factory = viewModelFactory {
            initializer { PlanRevisionViewModel(suggestion, synthesizer, AnnouncementHistory.session) }
        },
    )
    val state by model.state.collectAsState()
    LaunchedEffect(suggestion, synthesizer) {
        model.attachSynthesizer(synthesizer)
        model.update(suggestion)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(model, lifecycleOwner) {
        // 화면을 벗어나면 읽던 안내를 끊는다. 뒤에서 혼자 말하고 있으면 안 된다.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) model.stopSpeaking()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            model.stopSpeaking()
        }
    }

    PlanRevisionContent(
        state = state,
        onBack = { model.stopSpeaking(); onBack() },
        onKeepRoute = { model.keepRoute(); onKeepRoute() },
        onRequestWaypoint = {
            val place = state.suggestion.place
            if (interactionAllowed && place != null && model.state.value.request != WaypointRequestState.Requested) {
                val id = state.suggestion.id
                model.requestWaypoint()
                onRequestWaypoint(place)
                scope.launch {
                    val result = try {
                        submitWaypoint?.invoke(place) ?: WaypointRequestState.NotConnected
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        WaypointRequestState.Failed
                    }
                    if (model.state.value.suggestion.id == id) model.updateRequest(result)
                }
            }
        },
        onReplay = model::replay,
        onStopSpeaking = model::stopSpeaking,
        modifier = modifier,
        interactionAllowed = interactionAllowed,
    )
}

/** 상태 없는 본문. Preview와 Compose 테스트가 이걸 직접 그린다. */
@Composable
fun PlanRevisionContent(
    state: PlanRevisionUiState,
    onBack: () -> Unit,
    onKeepRoute: () -> Unit = {},
    onRequestWaypoint: () -> Unit = {},
    onReplay: () -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    modifier: Modifier = Modifier,
    interactionAllowed: Boolean = true,
) {
    PlanRevisionLayout(
        state = state,
        onBack = onBack,
        onKeepRoute = onKeepRoute,
        onRequestWaypoint = onRequestWaypoint,
        onReplay = onReplay,
        onStopSpeaking = onStopSpeaking,
        modifier = modifier,
        interactionAllowed = interactionAllowed,
    )
}

/** Preview·테스트에서 Mock 합성기로 화면 전체를 돌려 볼 때 쓴다. */
fun mockSynthesizer(): SpeechSynthesizer =
    MockSpeechSynthesizer(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

private const val PreviewWidth = 1700
private const val PreviewHeight = 1184

@Preview(name = "F-1 기본 AI 제안", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionPreview() {
    MosimTheme {
        PlanRevisionContent(PlanRevisionUiState(mockPlanSuggestion()), onBack = {})
    }
}

@Preview(name = "F-1 TTS 재생 중", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionSpeakingPreview() {
    MosimTheme {
        PlanRevisionContent(
            PlanRevisionUiState(mockPlanSuggestion(), announcement = AnnouncementState.Speaking),
            onBack = {},
        )
    }
}

@Preview(name = "F-1 TTS 실패", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionSpeechFailedPreview() {
    MosimTheme {
        PlanRevisionContent(
            PlanRevisionUiState(mockPlanSuggestion(), announcement = AnnouncementState.Failed),
            onBack = {},
        )
    }
}

@Preview(name = "F-1 시간 정보 없음", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionNoTimesPreview() {
    MosimTheme {
        PlanRevisionContent(PlanRevisionUiState(mockPlanSuggestionWithoutTimes()), onBack = {})
    }
}

@Preview(name = "F-1 경유 요청 중", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionRequestedPreview() {
    MosimTheme {
        PlanRevisionContent(
            PlanRevisionUiState(
                suggestion = mockPlanSuggestion(),
                request = WaypointRequestState.Requested,
            ),
            onBack = {},
        )
    }
}

@Preview(name = "F-1 주행 중 조작 제한", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun PlanRevisionRestrictedPreview() {
    MosimTheme {
        PlanRevisionContent(
            PlanRevisionUiState(mockPlanSuggestion()),
            onBack = {},
            interactionAllowed = false,
        )
    }
}
