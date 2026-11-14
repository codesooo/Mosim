package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.platform.pleos.SpeechEvent
import com.example.mosim.platform.pleos.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mock of Pleos request/ResultListener: asynchronous ready, partial, final and end events. */
class MockSpeechRecognizer(
    private val scope: CoroutineScope,
    private val result: String? = "커피 마시고 싶은데 근처에 카페 있을까?",
    private val failure: SpeechError? = null,
    private val eventDelayMillis: Long = 700L,
) : SpeechRecognizer {
    private var job: Job? = null
    private var listener: ((SpeechEvent) -> Unit)? = null
    private var closed = false

    override fun start(onEvent: (SpeechEvent) -> Unit) {
        if (closed || listener != null) return
        listener = onEvent
        job = scope.launch {
            delay(eventDelayMillis)
            onEvent(SpeechEvent.Ready)
            delay(eventDelayMillis)
            onEvent(SpeechEvent.Listening)
            // 실제 SDK처럼 소리 크기를 먼저 몇 번 보낸다. 화면이 음량에 반응하는지 Mock으로도 본다.
            repeat(MOCK_LEVEL_COUNT) {
                delay(eventDelayMillis / MOCK_LEVEL_COUNT)
                onEvent(SpeechEvent.Level(MOCK_LEVELS[it % MOCK_LEVELS.size]))
            }
            if (failure != null) {
                listener = null
                onEvent(SpeechEvent.Failed(failure))
            } else {
                onEvent(SpeechEvent.Text(result?.take(result.length / 2), false))
                delay(eventDelayMillis)
                listener = null
                onEvent(SpeechEvent.Text(result, true))
                onEvent(SpeechEvent.Ended)
            }
        }
    }

    private companion object {
        const val MOCK_LEVEL_COUNT = 4
        val MOCK_LEVELS = listOf(0.2, 0.8, 0.5, 1.0)
    }

    override fun stop() {
        val onEvent = listener ?: return
        job?.cancel()
        job = scope.launch {
            delay(eventDelayMillis)
            listener = null
            if (failure != null) onEvent(SpeechEvent.Failed(failure))
            else onEvent(SpeechEvent.Text(result, true))
            onEvent(SpeechEvent.Ended)
        }
    }

    override fun cancel() {
        listener = null
        job?.cancel()
        job = null
    }

    override fun close() {
        cancel()
        closed = true
    }
}