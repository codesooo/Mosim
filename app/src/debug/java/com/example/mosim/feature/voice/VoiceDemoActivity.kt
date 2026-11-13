package com.example.mosim.feature.voice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mosim.platform.pleos.SpeechMode
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosTypography

/** Debug-only entry point; mode selection and manual controls are not driving UI. */
class VoiceDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosimTheme {
                var mode by rememberSaveable { mutableStateOf<SpeechMode?>(null) }
                var lastResult by rememberSaveable { mutableStateOf("") }
                val selected = mode
                if (selected == null) {
                    MosimAppContainer(title = "F-2 개발용 테스트") {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                        ) {
                            PleosButton("Pleos STT", { mode = SpeechMode.Pleos })
                            PleosButton("Mock 성공", { mode = SpeechMode.Mock })
                            PleosButton("Mock 오류", { mode = SpeechMode.MockFailure })
                            if (lastResult.isNotEmpty()) {
                                Text("최종 결과: $lastResult", style = PleosTypography.BodyLarge)
                            }
                        }
                    }
                } else {
                    VoiceScreen(
                        onBack = { mode = null },
                        onSpeechRecognized = { lastResult = it },
                        mode = selected,
                        showDebugControls = true,
                    )
                }
            }
        }
    }
}