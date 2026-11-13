package com.example.mosim.platform.pleos

import android.content.Context
import com.example.mosim.data.mock.MockSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

enum class SpeechMode { Pleos, Mock, MockFailure }

object SpeechRecognizerProvider {
    fun create(context: Context, mode: SpeechMode): SpeechRecognizer = when (mode) {
        SpeechMode.Pleos -> PleosSpeechRecognizer(context)
        SpeechMode.Mock, SpeechMode.MockFailure -> MockSpeechRecognizer(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            failure = if (mode == SpeechMode.MockFailure) SpeechError.Timeout else null,
        )
    }
}