package com.example.mosim

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.mosim.feature.voice.VoiceContent
import com.example.mosim.feature.voice.VoicePhase
import com.example.mosim.feature.voice.VoiceScreen
import com.example.mosim.feature.voice.VoiceUiState
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.platform.pleos.SpeechMode
import com.example.mosim.ui.theme.MosimTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VoiceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun drivingContentShowsPartialWithoutManualControlsOrInferredIntent() {
        compose.setContent {
            MosimTheme { VoiceContent(VoiceUiState(VoicePhase.Listening, "커피 마시고 싶어"), {}) }
        }
        compose.onNodeWithText("듣고 있어요").assertIsDisplayed()
        compose.onNodeWithText("“커피 마시고 싶어”").assertIsDisplayed()
        compose.onNodeWithText("인식 중지").assertDoesNotExist()
        compose.onNodeWithText("의도: 경유지 추가").assertDoesNotExist()
    }

    @Test
    fun errorOffersRetryInDebugControls() {
        var retries = 0
        compose.setContent {
            MosimTheme {
                VoiceContent(
                    VoiceUiState(VoicePhase.Error, error = SpeechError.Timeout), {},
                    showDebugControls = true, onStart = { retries++ },
                )
            }
        }
        compose.onNodeWithText("다시 듣기").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun mockDisplaysFinalAndDeliversCallbackOnce() {
        val results = mutableListOf<String>()
        compose.setContent {
            MosimTheme { VoiceScreen({}, { results.add(it) }, mode = SpeechMode.Mock) }
        }
        compose.waitUntil(10_000) { results.size == 1 }
        compose.onNodeWithText("“커피 마시고 싶은데 근처에 카페 있을까?”").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, results.size) }
    }
}