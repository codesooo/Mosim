package com.example.mosim

import com.example.mosim.data.mock.MockSpeechSynthesizer
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.data.mock.mockPlanSuggestionWithoutTimes
import com.example.mosim.feature.planrevision.AnnouncementState
import com.example.mosim.feature.planrevision.PlanRevisionViewModel
import com.example.mosim.feature.planrevision.WaypointRequestState
import com.example.mosim.platform.pleos.SpeechOutputError
import com.example.mosim.platform.pleos.SpeechOutputEvent
import com.example.mosim.platform.pleos.SpeechSynthesizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanRevisionViewModelTest {

    @Test
    fun `새 제안은 한 번만 자동으로 읽고 같은 제안은 다시 읽지 않는다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            assertEquals(1, tts.speakCount)
            // 같은 id로 갱신해도 다시 읽지 않는다 (Compose 재구성 상황).
            model.update(mockPlanSuggestion())
            assertEquals(1, tts.speakCount)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `다른 제안이 들어오면 읽던 안내를 끊고 새로 읽는다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            model.update(mockPlanSuggestionWithoutTimes())
            assertEquals(2, tts.speakCount)
            assertTrue(tts.stopCount > 0)
            assertEquals("mock-rest-stop-no-times", model.state.value.suggestion.id)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `발화문이 없으면 음성 안내를 하지 않는다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion().copy(speech = null), tts)
        try {
            assertEquals(0, tts.speakCount)
            model.replay()
            assertEquals(0, tts.speakCount)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `다시 듣기와 그만 듣기가 상태를 바꾼다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            tts.emit(SpeechOutputEvent.Started)
            assertEquals(AnnouncementState.Speaking, model.state.value.announcement)

            // 읽는 중에는 다시 듣기를 눌러도 겹쳐 읽지 않는다.
            model.replay()
            assertEquals(1, tts.speakCount)

            model.stopSpeaking()
            tts.emit(SpeechOutputEvent.Stopped)
            assertEquals(AnnouncementState.Idle, model.state.value.announcement)

            model.replay()
            assertEquals(2, tts.speakCount)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `음성 실패해도 화면 상태와 버튼 동작은 그대로다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            tts.emit(SpeechOutputEvent.Failed(SpeechOutputError.Unsupported))
            assertEquals(AnnouncementState.Failed, model.state.value.announcement)
            // 실패는 음성에만 영향을 준다. 경유 요청은 그대로 된다.
            model.requestWaypoint()
            assertEquals(WaypointRequestState.Requested, model.state.value.request)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `경유 요청은 요청 상태까지만 간다 - 완료로 바뀌지 않는다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            assertEquals(WaypointRequestState.Idle, model.state.value.request)
            model.requestWaypoint()
            assertEquals(WaypointRequestState.Requested, model.state.value.request)
            // 중복 요청은 무시한다.
            model.requestWaypoint()
            assertEquals(WaypointRequestState.Requested, model.state.value.request)
            // NaviHelper가 실패를 알려 주면 실패로 바뀐다. 성공 상태는 아직 없다.
            model.waypointFailed()
            assertEquals(WaypointRequestState.Failed, model.state.value.request)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `시간 정보가 없는 제안도 그대로 들고 있는다`() {
        val tts = FakeSynthesizer()
        val model = PlanRevisionViewModel(mockPlanSuggestionWithoutTimes(), tts)
        try {
            assertNull(model.state.value.suggestion.arrival)
            assertNull(model.state.value.suggestion.extra)
        } finally {
            model.dispose()
        }
    }

    @Test
    fun `Mock 합성기로도 자동 안내가 한 번 돈다`(): Unit = runBlocking {
        val tts = MockSpeechSynthesizer(this, startDelayMillis = 1, millisPerChar = 0)
        val model = PlanRevisionViewModel(mockPlanSuggestion(), tts)
        try {
            withTimeout(2_000) {
                model.state.first { it.announcement == AnnouncementState.Speaking }
            }
            withTimeout(2_000) {
                model.state.first { it.announcement == AnnouncementState.Idle }
            }
        } finally {
            model.dispose()
        }
    }

    private class FakeSynthesizer : SpeechSynthesizer {
        var speakCount = 0
        var stopCount = 0
        private var listener: ((SpeechOutputEvent) -> Unit)? = null

        override fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit) {
            speakCount++
            listener = onEvent
        }

        override fun stop() {
            stopCount++
        }

        override fun close() {
            listener = null
        }

        fun emit(event: SpeechOutputEvent) = listener?.invoke(event)
    }
}
