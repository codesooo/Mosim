package com.example.mosim.feature.planrevision

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.example.mosim.navigation.MosimNavHost
import com.example.mosim.platform.pleos.PleosTtsAdapter
import com.example.mosim.platform.pleos.SpeechOutputEvent
import com.example.mosim.platform.pleos.SpeechSynthesizer
import com.example.mosim.ui.theme.MosimTheme

/** Debug 전용. real_tts=true는 실제 SDK 이벤트만 로그로 확인하며 발화/개인정보는 기록하지 않는다. */
class FScreenDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val realTts = intent.getBooleanExtra("real_tts", false)
        setContent {
            val output = remember {
                object : SpeechSynthesizer {
                    private val sdk = PleosTtsAdapter(applicationContext)
                    override fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit) {
                        sdk.speak(text) { event ->
                            val label = when (event) {
                                SpeechOutputEvent.Started -> "Started"
                                SpeechOutputEvent.Done -> "Done"
                                SpeechOutputEvent.Stopped -> "Stopped"
                                is SpeechOutputEvent.Failed -> "Failed:${event.error}"
                            }
                            Log.i("MOSIM_TTS_CHECK", label)
                            onEvent(event)
                        }
                    }
                    override fun stop() = sdk.stop()
                    override fun close() = sdk.close()
                }
            }
            DisposableEffect(output) { onDispose { output.close() } }
            MosimTheme {
                MosimNavHost(
                    demoFlow = !realTts,
                    synthesizer = output,
                    interactionAllowed = true,
                )
            }
        }
    }
}
