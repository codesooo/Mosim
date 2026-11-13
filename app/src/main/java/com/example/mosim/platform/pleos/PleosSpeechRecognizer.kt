package com.example.mosim.platform.pleos

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale

/**
 * ON_DEVICE integration approved for F-2; recognition from a live microphone is not yet verified.
 *
 * The engine's endpoint detection closes a session at the first pause, so one spoken sentence comes
 * back as several short finals — "커피 마시고", then "싶은데". Neither the SDK nor its AIDL exposes a
 * way to tune that, so this adapter re-arms the same instance after each final and joins the pieces,
 * reporting a single final only once the speaker has stayed quiet for [continuationWindowMillis].
 * Re-arming in place matters: a fresh instance per piece would drop audio across the gap.
 */
class PleosSpeechRecognizer(
    context: Context,
    private val continuationWindowMillis: Long = 2_000L,
) : SpeechRecognizer {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var sdk: SpeechToText? = null
    private var resultListener: ResultListener? = null
    private var listenerAdded = false
    private var generation = 0L
    private var closed = false
    private var initialized = false
    private var stopping = false
    private var output: ((SpeechEvent) -> Unit)? = null

    /** Finals already accepted for the current utterance. Touched on the main thread only. */
    private val segments = StringBuilder()
    private var finalize: Runnable? = null

    override fun start(onEvent: (SpeechEvent) -> Unit) {
        if (closed || sdk != null) return
        val session = ++generation
        output = onEvent
        stopping = false
        segments.setLength(0)
        try {
            val engine = SpeechToText(appContext, Mode.ON_DEVICE)
            sdk = engine
            val results = object : ResultListener {
                override fun onReady() = emit(session, SpeechEvent.Ready)

                override fun onStartedRecognition() {
                    onMain(session) { cancelFinalize() }
                    emit(session, SpeechEvent.Listening)
                }

                override fun onUpdated(stt: String, completed: Boolean) = onMain(session) {
                    cancelFinalize()
                    if (!completed) {
                        emit(session, SpeechEvent.Text(joined(stt), false))
                        return@onMain
                    }
                    if (stt.isNotBlank()) {
                        if (segments.isNotEmpty()) segments.append(' ')
                        segments.append(stt.trim())
                    }
                    // A final here ends a piece, not the sentence. Keep it interim and listen on.
                    if (stopping) return@onMain finishUtterance(session)
                    emit(session, SpeechEvent.Text(joined(null), false))
                    reArm(session)
                    scheduleFinalize(session)
                }

                override fun onUpdatedEpdData(on: Long, off: Long) = Unit
                override fun onUpdatedRms(rms: Double) = Unit

                /** A piece boundary. The final for it still follows, so the utterance is not over. */
                override fun onEndedRecognition() = Unit

                override fun onError(errorType: ErrorType) =
                    emit(session, SpeechEvent.Failed(mapError(errorType)))
            }
            resultListener = results
            engine.initialize(object : OnInitializeListener {
                override fun onSuccess() = onMain(session) {
                    try {
                        initialized = true
                        engine.addListener(results)
                        listenerAdded = true
                        engine.setLocale(Locale.KOREA)
                        if (stopping) emit(session, SpeechEvent.Ended) else engine.request()
                    } catch (_: SecurityException) {
                        emit(session, SpeechEvent.Failed(SpeechError.Permission))
                    } catch (_: Exception) {
                        emit(session, SpeechEvent.Failed(SpeechError.Connection))
                    }
                }

                override fun onFailure(errorType: ErrorType) =
                    emit(session, SpeechEvent.Failed(mapError(errorType)))
            })
        } catch (_: SecurityException) {
            emit(session, SpeechEvent.Failed(SpeechError.Permission))
        } catch (_: LinkageError) {
            emit(session, SpeechEvent.Failed(SpeechError.Unsupported))
        } catch (_: Exception) {
            emit(session, SpeechEvent.Failed(SpeechError.Connection))
        }
    }

    override fun stop() {
        stopping = true
        if (!initialized) return
        onMain(generation) { session ->
            runCatching { sdk?.stop() }
            // Report what was collected even if the trailing piece never arrives.
            cancelFinalize()
            val task = Runnable { if (!closed && generation == session) finishUtterance(session) }
            finalize = task
            handler.postDelayed(task, STOP_GRACE_MILLIS)
        }
    }

    override fun cancel() {
        // Invalidate callbacks before stop/release: the SDK has stop(), no separate cancel API.
        generation++
        output = null
        cancelFinalize()
        segments.setLength(0)
        val engine = sdk
        val results = resultListener
        sdk = null
        resultListener = null
        if (engine != null) {
            if (initialized) runCatching { engine.stop() }
            if (listenerAdded && results != null) runCatching { engine.removeListener(results) }
            runCatching { engine.release() }
        }
        initialized = false
        listenerAdded = false
        stopping = false
    }

    override fun close() {
        cancel()
        closed = true
    }

    private fun emit(session: Long, event: SpeechEvent) {
        handler.post { if (!closed && generation == session) output?.invoke(event) }
    }

    /** Runs [block] on the main thread, skipping it if the session has moved on. */
    private fun onMain(session: Long, block: (Long) -> Unit) {
        handler.post { if (!closed && generation == session) block(session) }
    }

    private fun joined(partial: String?): String {
        val tail = partial?.trim().orEmpty()
        return when {
            segments.isEmpty() -> tail
            tail.isEmpty() -> segments.toString()
            else -> "$segments $tail"
        }
    }

    private fun cancelFinalize() {
        finalize?.let(handler::removeCallbacks)
        finalize = null
    }

    private fun scheduleFinalize(session: Long) {
        cancelFinalize()
        val task = Runnable { if (!closed && generation == session) finishUtterance(session) }
        finalize = task
        handler.postDelayed(task, continuationWindowMillis)
    }

    private fun finishUtterance(session: Long) {
        cancelFinalize()
        runCatching { sdk?.stop() }
        val text = segments.toString().trim()
        emit(session, SpeechEvent.Text(text.ifEmpty { null }, true))
        emit(session, SpeechEvent.Ended)
    }

    private fun reArm(session: Long) {
        if (closed || generation != session || stopping) return
        val engine = sdk ?: return
        runCatching { engine.stop() }
        runCatching { engine.request() }
            .onFailure { emit(session, SpeechEvent.Failed(SpeechError.Connection)) }
    }

    private fun mapError(error: ErrorType): SpeechError = when (error) {
        ErrorType.PERMISSION_DENIED -> SpeechError.Permission
        ErrorType.MIC_ERROR, ErrorType.AUDIO_ERROR -> SpeechError.Microphone
        ErrorType.NOT_SUPPORTED_API, ErrorType.UNAVAILABLE_LANGUAGE,
        ErrorType.ON_DEVICE_ENGINE_ERROR -> SpeechError.Unsupported
        ErrorType.TIMEOUT -> SpeechError.Timeout
        ErrorType.NETWORK_ERROR, ErrorType.SERVER_INTERNAL_ERROR -> SpeechError.Connection
        else -> SpeechError.Unknown
    }

    private companion object {
        /** How long to wait for a trailing piece after the screen asks to stop. */
        const val STOP_GRACE_MILLIS = 700L
    }
}
