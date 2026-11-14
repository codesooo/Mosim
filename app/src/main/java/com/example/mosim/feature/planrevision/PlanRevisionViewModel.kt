package com.example.mosim.feature.planrevision

import androidx.lifecycle.ViewModel
import com.example.mosim.platform.pleos.SpeechOutputEvent
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 음성 안내가 지금 어떤 상태인지. 화면은 이걸로 "다시 듣기 / 그만 듣기"를 고른다. */
enum class AnnouncementState {
    /** 아직 안 읽었거나, 읽기를 마쳤다. */
    Idle,

    Speaking,

    /** 읽지 못했다. 화면과 터치는 그대로 쓸 수 있다. */
    Failed,
}

data class PlanRevisionUiState(
    val suggestion: PlanSuggestion,
    val announcement: AnnouncementState = AnnouncementState.Idle,
    val request: WaypointRequestState = WaypointRequestState.Idle,
)

/**
 * F-1 상태.
 *
 * 음성 안내는 **제안 하나당 한 번만** 자동 재생한다. 기준은 [PlanSuggestion.id]라서
 * Compose가 다시 그려도, 화면을 되돌아와도 같은 제안이면 다시 읽지 않는다.
 * 새 제안이 들어오면 읽던 안내를 끊고 새 문장으로 바꾼다.
 */
class PlanRevisionViewModel(
    suggestion: PlanSuggestion,
    private var synthesizer: SpeechSynthesizer,
    private val history: AnnouncementHistory = AnnouncementHistory(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlanRevisionUiState(suggestion))
    val state = mutableState.asStateFlow()

    /** 중지·교체 뒤에 늦게 도착하는 재생 콜백을 차단한다. */
    private var playback = 0L
    private var disposed = false

    init {
        announceOnce()
    }

    fun attachSynthesizer(output: SpeechSynthesizer) {
        if (disposed || synthesizer === output) return
        stopSpeaking()
        synthesizer = output
    }

    /** 새 제안으로 갈아 끼운다. 읽던 안내는 끊는다. */
    fun update(suggestion: PlanSuggestion) {
        if (disposed || suggestion.id == state.value.suggestion.id) return
        stopSpeaking()
        mutableState.value = PlanRevisionUiState(suggestion)
        announceOnce()
    }

    private fun announceOnce() {
        val suggestion = state.value.suggestion
        if (disposed || !history.claim(suggestion.id)) return
        speak(suggestion)
    }

    /** 다시 듣기. 자동 안내와 달리 몇 번이든 부를 수 있다. */
    fun replay() {
        if (disposed || state.value.announcement == AnnouncementState.Speaking) return
        speak(state.value.suggestion)
    }

    fun stopSpeaking() {
        if (disposed) return
        playback++
        synthesizer.stop()
        mutableState.value = state.value.copy(announcement = AnnouncementState.Idle)
    }

    private fun speak(suggestion: PlanSuggestion) {
        val text = suggestion.speech?.takeIf { it.isNotBlank() } ?: return
        val target = ++playback
        mutableState.value = state.value.copy(announcement = AnnouncementState.Speaking)
        synthesizer.speak(text) { event ->
            if (disposed || playback != target) return@speak
            mutableState.value = state.value.copy(
                announcement = when (event) {
                    SpeechOutputEvent.Started -> AnnouncementState.Speaking
                    SpeechOutputEvent.Done, SpeechOutputEvent.Stopped -> AnnouncementState.Idle
                    // 음성은 보조 수단이다. 실패해도 화면은 그대로 둔다.
                    is SpeechOutputEvent.Failed -> AnnouncementState.Failed
                },
            )
        }
    }

    fun keepRoute() {
        if (disposed) return
        stopSpeaking()
    }

    /**
     * 경유지 추가를 **요청**한다. 경로가 바뀌었다는 뜻이 아니다.
     * 실제 반영은 NaviHelper 응답을 받아야 알 수 있고, 그 연결은 아직 없다.
     */
    fun requestWaypoint() {
        if (disposed || state.value.request == WaypointRequestState.Requested ||
            state.value.request == WaypointRequestState.Applied) return
        stopSpeaking()
        mutableState.value = state.value.copy(request = WaypointRequestState.Requested)
    }

    /** NaviHelper 담당자가 실패를 알려 줄 때 부른다. */
    fun waypointFailed() {
        if (disposed) return
        mutableState.value = state.value.copy(request = WaypointRequestState.Failed)
    }

    fun dispose() {
        if (disposed) return
        stopSpeaking()
        disposed = true
        // 합성기는 NavHost 또는 주입한 호출자가 소유한다.
    }

    fun updateRequest(result: WaypointRequestState) {
        if (!disposed && state.value.request != WaypointRequestState.Idle) {
            mutableState.value = state.value.copy(request = result)
        }
    }

    override fun onCleared() = dispose()
}
