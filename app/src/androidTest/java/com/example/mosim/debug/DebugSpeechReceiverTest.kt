package com.example.mosim.debug

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mosim.feature.escort.EscortDependencies
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSpeechReceiverTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val speechInput = EscortDependencies.mockSpeechInput

    @Test
    fun 받은_문장을_에스코트_음성_입력으로_넘긴다() = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) { speechInput.recognizedTexts.first() }
        speechInput.listenerCount.first { it > 0 }

        DebugSpeechReceiver().onReceive(context, sayIntent("좀 추운데"))

        assertEquals("좀 추운데", withTimeout(2_000) { received.await() })
    }

    @Test
    fun 빈_문장은_넘기지_않는다() = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(500) { speechInput.recognizedTexts.first() }
        }
        speechInput.listenerCount.first { it > 0 }

        DebugSpeechReceiver().onReceive(context, sayIntent(" "))

        assertNull(received.await())
    }

    private fun sayIntent(text: String) =
        Intent(DebugSpeechReceiver.ACTION_SAY).putExtra(DebugSpeechReceiver.EXTRA_TEXT, text)
}
