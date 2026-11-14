package com.example.mosim

import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.data.mock.mockPlaces
import com.example.mosim.feature.planrevision.AnnouncementHistory
import com.example.mosim.feature.planrevision.AnnouncementState
import com.example.mosim.feature.planrevision.PlanRevisionViewModel
import com.example.mosim.feature.planrevision.WaypointRequestState
import com.example.mosim.feature.voice.VoicePhase
import com.example.mosim.feature.voice.VoiceViewModel
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.WaypointUiState
import com.example.mosim.feature.waypoint.WaypointViewModel
import com.example.mosim.platform.pleos.SpeechEvent
import com.example.mosim.platform.pleos.SpeechRecognizer
import com.example.mosim.platform.pleos.SpeechOutputEvent
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class FScreenRegressionTest {
    @Test
    fun sameProposalDoesNotAutoReplayAfterScreenReentryOrInterveningProposal() {
        val tts = FakeOutput()
        val history = AnnouncementHistory()
        val first = PlanRevisionViewModel(mockPlanSuggestion(), tts, history)
        first.update(mockPlanSuggestion().copy(id = "second"))
        first.update(mockPlanSuggestion())
        assertEquals(2, tts.calls)
        first.dispose()
        val reopened = PlanRevisionViewModel(mockPlanSuggestion(), tts, history)
        assertEquals(2, tts.calls)
        reopened.replay()
        assertEquals(3, tts.calls)
        reopened.dispose()
    }

    @Test
    fun stopCancelsPendingPlaybackAndIgnoresLateCallback() {
        val tts = FakeOutput()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        val late = tts.callback
        model.stopSpeaking()
        late(SpeechOutputEvent.Started)
        assertEquals(AnnouncementState.Idle, model.state.value.announcement)
        assertTrue(tts.stops > 0)
        model.dispose()
    }

    @Test
    fun externalResultIsRequiredForAppliedState() {
        val model = PlanRevisionViewModel(mockPlanSuggestion(), FakeOutput())
        model.keepRoute()
        assertEquals(WaypointRequestState.Idle, model.state.value.request)
        model.requestWaypoint()
        assertEquals(WaypointRequestState.Requested, model.state.value.request)
        model.updateRequest(WaypointRequestState.NotConnected)
        assertEquals(WaypointRequestState.NotConnected, model.state.value.request)
        model.requestWaypoint()
        model.updateRequest(WaypointRequestState.Applied)
        assertEquals(WaypointRequestState.Applied, model.state.value.request)
        model.dispose()
    }

    @Test
    fun searchExceptionBecomesFailureAndCandidatesAreLimitedToThree() = runBlocking {
        val failing = object : PlaceSearchSource {
            override suspend fun search(query: String): PlaceSearchResult = error("network")
        }
        val model = WaypointViewModel(failing, "커피", this)
        withTimeout(2_000) { model.state.first { it is WaypointUiState.Failed } }
        val source = object : PlaceSearchSource {
            override suspend fun search(query: String) = PlaceSearchResult.Found(mockPlaces() + mockPlaces())
        }
        val candidates = WaypointViewModel(source, "커피", this)
        val ready = withTimeout(2_000) {
            candidates.state.first { it is WaypointUiState.Ready }
        } as WaypointUiState.Ready
        assertEquals(3, ready.places.size)
    }

    @Test
    fun analysisWaitDoesNotRestartRecognitionAndCanBeCancelled() = runBlocking {
        val input = object : SpeechRecognizer {
            var calls = 0
            var callback: (SpeechEvent) -> Unit = {}
            override fun start(onEvent: (SpeechEvent) -> Unit) { calls++; callback = onEvent }
            override fun stop() = Unit
            override fun cancel() = Unit
            override fun close() = Unit
        }
        val model = VoiceViewModel(input, this)
        model.start()
        input.callback(SpeechEvent.Text("커피", true))
        assertEquals("커피", model.finalResults.first())
        model.markAnalyzing()
        model.start()
        assertEquals(VoicePhase.Analyzing, model.state.value.phase)
        assertFalse(model.state.value.active)
        assertEquals(1, input.calls)
        model.cancel()
        assertEquals(VoicePhase.Cancelled, model.state.value.phase)
        model.dispose()
    }

    private class FakeOutput : SpeechSynthesizer {
        var calls = 0
        var stops = 0
        var callback: (SpeechOutputEvent) -> Unit = {}
        override fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit) { calls++; callback = onEvent }
        override fun stop() { stops++ }
        override fun close() = Unit
    }
}
