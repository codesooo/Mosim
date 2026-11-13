package com.example.mosim.feature.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Debug-only check that ON_DEVICE Korean recognition works without a live microphone.
 *
 * The emulator has no usable mic input, so `request()` only ever hears silence. The SDK documents
 * `sendAudio()` as an ON_DEVICE-only way to feed the app's own PCM instead, which is what this
 * screen exercises. `assets/ko.pcm` is 16 kHz / 16-bit / mono PCM of a Korean sentence.
 *
 * Results are logged under the STTCHECK tag. Not part of the F-2 feature and not in release.
 */
class SttAudioCheckActivity : ComponentActivity() {

    private var sdk: SpeechToText? = null
    private var listener: ResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audio = assets.open(FILE_NAME).use { it.readBytes() }
        Log.i(TAG, "pcm bytes=${audio.size} seconds=${audio.size / BYTES_PER_SECOND.toFloat()}")

        val results = object : ResultListener {
            override fun onReady() {
                Log.i(TAG, "onReady")
            }

            override fun onStartedRecognition() {
                Log.i(TAG, "onStartedRecognition")
            }

            override fun onUpdated(stt: String, completed: Boolean) {
                Log.i(TAG, "onUpdated completed=$completed text=[$stt]")
            }

            override fun onUpdatedEpdData(on: Long, off: Long) {
                Log.i(TAG, "onEpd on=$on off=$off")
            }

            override fun onUpdatedRms(rms: Double) = Unit

            override fun onEndedRecognition() {
                Log.i(TAG, "onEndedRecognition")
            }

            override fun onError(errorType: ErrorType) {
                Log.e(TAG, "onError ${errorType.name}")
            }
        }
        listener = results

        val engine = SpeechToText(applicationContext, Mode.ON_DEVICE)
        sdk = engine
        engine.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                Log.i(TAG, "initialize onSuccess")
                engine.addListener(results)
                engine.setLocale(Locale.KOREA)
                // Chunked at real-time speed: the docs' streaming sample, which is the shape the
                // engine's endpointing expects. Off the main thread because of the sleeps.
                thread {
                    audio.asList().chunked(BYTES_PER_100_MS).forEach { chunk ->
                        engine.sendAudio(chunk.toByteArray())
                        Thread.sleep(100L)
                    }
                    Log.i(TAG, "completeAudioSend")
                    engine.completeAudioSend()
                }
            }

            override fun onFailure(errorType: ErrorType) {
                Log.e(TAG, "initialize onFailure ${errorType.name}")
            }
        })
    }

    override fun onDestroy() {
        listener?.let { sdk?.removeListener(it) }
        sdk?.release()
        sdk = null
        listener = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "STTCHECK"
        const val FILE_NAME = "ko.pcm"
        const val BYTES_PER_100_MS = 3_200
        const val BYTES_PER_SECOND = 32_000
    }
}
