package com.example.mosim.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.platform.pleos.SpeechEvent
import com.example.mosim.platform.pleos.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class VoicePhase {
    Idle,
    Preparing,
    Listening,
    Stopping,

    /**
     * 인식은 끝났고 외부 AI 의도 분석·장소 검색을 기다리는 중이다.
     * STT의 듣는 상태가 아니라 그 뒤의 별도 단계라 따로 둔다.
     */
    Analyzing,
    Completed,
    Error,
    Cancelled,
}

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.Idle,
    val text: String = "",
    val error: SpeechError? = null,
) {
    val active: Boolean
        get() = phase in listOf(VoicePhase.Preparing, VoicePhase.Listening, VoicePhase.Stopping)
}

class VoiceViewModel(
    private val recognizer: SpeechRecognizer,
    private val eventScope: CoroutineScope? = null,
    private val timeoutMillis: Long = 20_000L,
    private val finalWaitMillis: Long = 1_500L,
) : ViewModel() {
    private val scope get() = eventScope ?: viewModelScope
    private val mutableState = MutableStateFlow(VoiceUiState())
    val state = mutableState.asStateFlow()
    // A channel retains a final result until the screen collects it, without replay on recomposition.
    private val results = Channel<String>(Channel.UNLIMITED)
    val finalResults = results.receiveAsFlow()
    private var timeout: Job? = null
    private var finalWait: Job? = null
    private var session = 0L
    private var disposed = false

    fun start() {
        if (disposed || state.value.active || state.value.phase == VoicePhase.Analyzing) return
        val current = ++session
        mutableState.value = VoiceUiState(VoicePhase.Preparing)
        timeout = scope.launch {
            delay(timeoutMillis)
            if (current == session && state.value.active) fail(SpeechError.Timeout)
        }
        recognizer.start { event ->
            if (current != session || !state.value.active) return@start
            when (event) {
                SpeechEvent.Ready -> Unit
                SpeechEvent.Listening -> if (state.value.phase != VoicePhase.Stopping) {
                    mutableState.value = state.value.copy(phase = VoicePhase.Listening)
                }
                is SpeechEvent.Text -> {
                    val text = event.text.orEmpty().trim()
                    if (event.isFinal) {
                        if (text.isEmpty()) fail(SpeechError.NoSpeech)
                        else {
                            finish()
                            mutableState.value = VoiceUiState(VoicePhase.Completed, text)
                            results.trySend(text)
                        }
                    } else {
                        mutableState.value = state.value.copy(text = text)
                    }
                }
                SpeechEvent.Ended -> awaitFinal()
                is SpeechEvent.Failed -> fail(event.error)
            }
        }
    }

    fun stop() {
        if (!state.value.active || state.value.phase == VoicePhase.Stopping) return
        mutableState.value = state.value.copy(phase = VoicePhase.Stopping)
        recognizer.stop()
        if (state.value.active) awaitFinal()
    }

    private fun awaitFinal() {
        if (finalWait != null) return
        finalWait = scope.launch {
            // Do not promote a partial result: the SDK can end before its final callback arrives.
            delay(finalWaitMillis)
            if (state.value.active) fail(SpeechError.NoSpeech)
        }
    }

    fun cancel() {
        if (!state.value.active && state.value.phase != VoicePhase.Analyzing) return
        finish()
        mutableState.value = VoiceUiState(VoicePhase.Cancelled)
    }

    /**
     * 최종 텍스트를 외부 AI 분석으로 넘겼다. 그 결과를 기다리는 동안 화면이 멈춰 보이지 않게 한다.
     * 분석 자체는 다른 담당자 몫이라 여기서는 상태만 바꾼다.
     */
    fun markAnalyzing() {
        if (disposed || state.value.phase != VoicePhase.Completed) return
        mutableState.value = state.value.copy(phase = VoicePhase.Analyzing)
    }

    /** 외부 분석·검색이 실패했다고 알려 올 때 부른다. */
    fun analysisFailed(error: SpeechError = SpeechError.Unknown) {
        if (disposed || state.value.phase != VoicePhase.Analyzing) return
        mutableState.value = VoiceUiState(VoicePhase.Error, error = error)
    }

    private fun fail(error: SpeechError) {
        finish()
        mutableState.value = VoiceUiState(VoicePhase.Error, error = error)
    }

    private fun finish() {
        session++
        timeout?.cancel()
        finalWait?.cancel()
        timeout = null
        finalWait = null
        recognizer.cancel()
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        finish()
        recognizer.close()
        results.close()
    }

    override fun onCleared() = dispose()
}
