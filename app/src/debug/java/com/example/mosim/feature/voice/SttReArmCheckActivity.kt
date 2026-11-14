package com.example.mosim.feature.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import java.util.Locale

/**
 * Debug-only probe for one question: can request() be called again on the same SpeechToText
 * instance after stop(), without release() and re-initialize()?
 *
 * The ON_DEVICE engine ends a session as soon as its endpoint detection sees a pause, so joining
 * the fragments back into one sentence needs the adapter to re-arm in place. Creating a fresh
 * instance per fragment would drop audio across the gap.
 *
 * Runs three request/stop cycles against silence and logs every callback under STTREARM.
 */
class SttReArmCheckActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var sdk: SpeechToText? = null
    private var listener: ResultListener? = null
    private var cycle = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val results = object : ResultListener {
            override fun onReady() {
                Log.i(TAG, "cycle=$cycle onReady")
            }

            override fun onStartedRecognition() {
                Log.i(TAG, "cycle=$cycle onStartedRecognition")
            }

            override fun onUpdated(stt: String, completed: Boolean) {
                Log.i(TAG, "cycle=$cycle onUpdated completed=$completed text=[$stt]")
            }

            override fun onUpdatedEpdData(on: Long, off: Long) {
                Log.i(TAG, "cycle=$cycle onEpd on=$on off=$off")
            }

            override fun onUpdatedRms(rms: Double) = Unit

            override fun onEndedRecognition() {
                Log.i(TAG, "cycle=$cycle onEndedRecognition")
            }

            override fun onError(errorType: ErrorType) {
                Log.e(TAG, "cycle=$cycle onError ${errorType.name}")
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
                runCycle(engine)
            }

            override fun onFailure(errorType: ErrorType) {
                Log.e(TAG, "initialize onFailure ${errorType.name}")
            }
        })
    }

    private fun runCycle(engine: SpeechToText) {
        if (cycle >= CYCLES) {
            Log.i(TAG, "done: $CYCLES cycles on one instance")
            return
        }
        cycle++
        Log.i(TAG, "cycle=$cycle request()")
        runCatching { engine.request() }
            .onFailure { Log.e(TAG, "cycle=$cycle request threw ${it::class.java.simpleName}: ${it.message}") }
        handler.postDelayed({
            Log.i(TAG, "cycle=$cycle stop()")
            runCatching { engine.stop() }
                .onFailure { Log.e(TAG, "cycle=$cycle stop threw ${it::class.java.simpleName}") }
            handler.postDelayed({ runCycle(engine) }, GAP_MILLIS)
        }, LISTEN_MILLIS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        listener?.let { sdk?.removeListener(it) }
        sdk?.release()
        sdk = null
        listener = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "STTREARM"
        const val CYCLES = 3
        const val LISTEN_MILLIS = 4_000L
        const val GAP_MILLIS = 800L
    }
}
