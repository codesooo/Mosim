package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.SpeechOutputError
import com.example.mosim.platform.pleos.SpeechOutputEvent
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mock: Pleos TextToSpeech의 speak/EventListener를 흉내 낸다.
 *
 * 실제 SDK처럼 비동기로 시작·완료를 알리고, 읽는 데 시간이 걸리며, 실패도 낼 수 있다.
 * 읽는 길이는 글자 수에 비례시켜 "읽는 중"이 눈에 보이게 했다.
 */
class MockSpeechSynthesizer(
    private val scope: CoroutineScope,
    private val failure: SpeechOutputError? = null,
    private val startDelayMillis: Long = 300L,
    private val millisPerChar: Long = 60L,
) : SpeechSynthesizer {
    private var job: Job? = null
    private var listener: ((SpeechOutputEvent) -> Unit)? = null
    private var closed = false

    override fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit) {
        if (closed) return
        // 새 문장은 앞의 안내를 끊고 교체한다.
        stop()
        listener = onEvent
        job = scope.launch {
            delay(startDelayMillis)
            if (failure != null) {
                listener = null
                onEvent(SpeechOutputEvent.Failed(failure))
                return@launch
            }
            onEvent(SpeechOutputEvent.Started)
            delay(millisPerChar * text.length)
            listener = null
            onEvent(SpeechOutputEvent.Done)
        }
    }

    override fun stop() {
        val onEvent = listener ?: return
        listener = null
        job?.cancel()
        job = null
        onEvent(SpeechOutputEvent.Stopped)
    }

    override fun close() {
        listener = null
        job?.cancel()
        job = null
        closed = true
    }
}
