package com.example.mosim.platform.pleos

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

/**
 * 공식 v3.3.1 ON_DEVICE API. 이번 작업의 개발자 승인 예외로 구현했다.
 * 각 발화에 세션 번호와 별도 SDK 객체를 사용해 중지 뒤 늦게 오는 콜백을 차단한다.
 * 실제 음성 출력·Speech Service의 오디오 포커스 상호작용은 에뮬레이터 검증이 필요하다.
 */
class PleosTtsAdapter(context: Context) : SpeechSynthesizer {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private var client: TextToSpeech? = null
    private var listener: EventListener? = null
    private var callback: ((SpeechOutputEvent) -> Unit)? = null
    private var focus: AudioFocusRequest? = null
    private var timeout: Runnable? = null
    private var session = 0L
    private var closed = false

    override fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit) {
        stop()
        if (closed || text.isBlank()) {
            onEvent(SpeechOutputEvent.Failed(SpeechOutputError.Unsupported))
            return
        }
        val current = ++session
        callback = onEvent
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener({ change ->
                if (change < 0 && current == session) stop()
            }, handler)
            .build()
        focus = request
        try {
            if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                finish(SpeechOutputEvent.Failed(SpeechOutputError.Connection))
                return
            }
            val sdk = TextToSpeech(appContext, Mode.ON_DEVICE)
            client = sdk
            val events = object : EventListener {
                override fun onReady() = Unit
                override fun onStart() = post(current) { callback?.invoke(SpeechOutputEvent.Started) }
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onDone() = post(current) { finish(SpeechOutputEvent.Done) }
                override fun onStop() = post(current) { finish(SpeechOutputEvent.Stopped) }
                override fun onError(errorType: ErrorType) = post(current) {
                    finish(SpeechOutputEvent.Failed(mapError(errorType)))
                }
            }
            listener = events
            // 앱 임시 정책: 초기화/출력 콜백이 유실돼도 포커스를 무기한 점유하지 않는다.
            timeout = Runnable {
                if (current == session) finish(SpeechOutputEvent.Failed(SpeechOutputError.Connection))
            }.also { handler.postDelayed(it, 60_000L) }
            sdk.initialize(object : OnInitializeListener {
                override fun onSuccess() = post(current) {
                    try {
                        sdk.addEventListener(events)
                        sdk.setAudioAttributes(request.audioAttributes)
                        sdk.setLocale(Locale.KOREA)
                        sdk.speak(text)
                    } catch (error: Exception) {
                        finish(SpeechOutputEvent.Failed(exceptionError(error)))
                    }
                }

                override fun onFailure(errorType: ErrorType) = post(current) {
                    finish(SpeechOutputEvent.Failed(mapError(errorType)))
                }
            })
        } catch (error: Exception) {
            finish(SpeechOutputEvent.Failed(exceptionError(error)))
        } catch (_: LinkageError) {
            finish(SpeechOutputEvent.Failed(SpeechOutputError.Unsupported))
        }
    }

    private fun post(current: Long, action: () -> Unit) {
        handler.post { if (current == session && !closed) action() }
    }

    private fun finish(event: SpeechOutputEvent) {
        val result = callback
        callback = null
        session++
        timeout?.let(handler::removeCallbacks)
        timeout = null
        val sdk = client
        val events = listener
        client = null
        listener = null
        if (sdk != null) {
            runCatching { sdk.stop() }
            if (events != null) runCatching { sdk.removeEventListener(events) }
            runCatching { sdk.release() }
        }
        focus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
        focus = null
        result?.invoke(event)
    }

    override fun stop() = finish(SpeechOutputEvent.Stopped)

    override fun close() {
        stop()
        closed = true
    }

    private fun exceptionError(error: Exception) =
        if (error is SecurityException) SpeechOutputError.Permission else SpeechOutputError.Connection

    private fun mapError(error: ErrorType): SpeechOutputError = when (error) {
        ErrorType.PERMISSION_DENIED -> SpeechOutputError.Permission
        ErrorType.NOT_SUPPORTED_API, ErrorType.ON_DEVICE_ENGINE_ERROR,
        ErrorType.UNAVAILABLE_LANGUAGE -> SpeechOutputError.Unsupported
        ErrorType.NETWORK_ERROR, ErrorType.SERVER_INTERNAL_ERROR, ErrorType.TIMEOUT -> SpeechOutputError.Connection
        else -> SpeechOutputError.Unknown
    }
}
