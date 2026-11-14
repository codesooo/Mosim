package com.example.mosim

import com.example.mosim.data.mock.MockSpeechRecognizer
import com.example.mosim.feature.voice.VoicePhase
import com.example.mosim.feature.voice.VoiceViewModel
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.platform.pleos.SpeechEvent
import com.example.mosim.platform.pleos.SpeechRecognizer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceViewModelTest {
    @Test
    fun `mock emits asynchronous partial and final text and forwards final once`() = runBlocking {
        val model = VoiceViewModel(MockSpeechRecognizer(this, eventDelayMillis = 10), this)
        try {
            val result = async(start = CoroutineStart.UNDISPATCHED) { model.finalResults.first() }
            model.start()
            assertEquals(VoicePhase.Preparing, model.state.value.phase)
            withTimeout(2_000) { model.state.first { it.text.isNotEmpty() && it.active } }
            assertEquals("커피 마시고 싶은데 근처에 카페 있을까?", withTimeout(2_000) { result.await() })
            assertEquals(VoicePhase.Completed, model.state.value.phase)
            assertNull(withTimeoutOrNull(30) { model.finalResults.first() })
        } finally { model.dispose() }
    }

    @Test
    fun `mock error null and blank results are not forwarded`() = runBlocking {
        for ((text, failure) in listOf(null to null, "   " to null, "text" to SpeechError.Permission)) {
            val model = VoiceViewModel(MockSpeechRecognizer(this, text, failure, 1), this)
            try {
                model.start()
                val state = withTimeout(2_000) { model.state.first { it.phase == VoicePhase.Error } }
                assertEquals(failure ?: SpeechError.NoSpeech, state.error)
                assertNull(withTimeoutOrNull(20) { model.finalResults.first() })
            } finally { model.dispose() }
        }
    }

    @Test
    fun `duplicate starts are ignored and cancelled callbacks cannot affect retry`() = runBlocking {
        val fake = FakeRecognizer()
        val model = VoiceViewModel(fake, this)
        try {
            model.start()
            val old = fake.callback
            model.start()
            assertEquals(1, fake.starts)
            model.cancel()
            assertEquals(VoicePhase.Cancelled, model.state.value.phase)
            model.start()
            old(SpeechEvent.Text("old result", true))
            assertEquals(VoicePhase.Preparing, model.state.value.phase)
            fake.callback(SpeechEvent.Text("new result", true))
            fake.callback(SpeechEvent.Text("duplicate", true))
            assertEquals("new result", model.finalResults.first())
            assertNull(withTimeoutOrNull(20) { model.finalResults.first() })
        } finally { model.dispose() }
    }

    @Test
    fun `stop waits for final and never promotes partial`() = runBlocking {
        val fake = FakeRecognizer()
        val model = VoiceViewModel(fake, this, finalWaitMillis = 20)
        try {
            model.start()
            fake.callback(SpeechEvent.Text("partial", false))
            model.stop()
            assertEquals(VoicePhase.Stopping, model.state.value.phase)
            assertEquals(1, fake.stops)
            val state = withTimeout(2_000) { model.state.first { it.phase == VoicePhase.Error } }
            assertEquals(SpeechError.NoSpeech, state.error)
            assertNull(withTimeoutOrNull(20) { model.finalResults.first() })
        } finally { model.dispose() }
    }

    @Test
    fun `final arriving after ended is accepted within grace period`() = runBlocking {
        val fake = FakeRecognizer()
        val model = VoiceViewModel(fake, this)
        try {
            model.start()
            fake.callback(SpeechEvent.Ended)
            fake.callback(SpeechEvent.Text("final", true))
            assertEquals("final", model.finalResults.first())
            assertEquals(VoicePhase.Completed, model.state.value.phase)
        } finally { model.dispose() }
    }

    @Test
    fun `timeout releases session and retry can succeed`() = runBlocking {
        val fake = FakeRecognizer()
        val model = VoiceViewModel(fake, this, timeoutMillis = 20)
        try {
            model.start()
            val state = withTimeout(2_000) { model.state.first { it.phase == VoicePhase.Error } }
            assertEquals(SpeechError.Timeout, state.error)
            assertTrue(fake.cancels > 0)
            model.start()
            fake.callback(SpeechEvent.Text("retry", true))
            assertEquals("retry", model.finalResults.first())
        } finally { model.dispose() }
    }

    @Test
    fun `mock cancellation stops pending results and dispose closes once`() = runBlocking {
        val mock = MockSpeechRecognizer(this, eventDelayMillis = 5)
        val model = VoiceViewModel(mock, this)
        model.start()
        model.cancel()
        delay(40)
        assertEquals(VoicePhase.Cancelled, model.state.value.phase)
        assertNull(withTimeoutOrNull(20) { model.finalResults.first() })
        model.dispose()
        model.start()
        assertEquals(VoicePhase.Cancelled, model.state.value.phase)
        val fake = FakeRecognizer()
        val other = VoiceViewModel(fake, this)
        other.start()
        other.dispose()
        other.dispose()
        assertEquals(1, fake.closes)
    }

    @Test
    fun `stopping mock returns final asynchronously`() = runBlocking {
        val model = VoiceViewModel(MockSpeechRecognizer(this, eventDelayMillis = 10), this)
        try {
            model.start()
            model.stop()
            assertEquals(VoicePhase.Stopping, model.state.value.phase)
            assertEquals("커피 마시고 싶은데 근처에 카페 있을까?", withTimeout(2_000) { model.finalResults.first() })
        } finally { model.dispose() }
    }

    private class FakeRecognizer : SpeechRecognizer {
        var starts = 0
        var stops = 0
        var cancels = 0
        var closes = 0
        var callback: (SpeechEvent) -> Unit = {}
        override fun start(onEvent: (SpeechEvent) -> Unit) { starts++; callback = onEvent }
        override fun stop() { stops++ }
        override fun cancel() { cancels++ }
        override fun close() { closes++ }
    }
}