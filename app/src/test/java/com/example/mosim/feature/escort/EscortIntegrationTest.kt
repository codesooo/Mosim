package com.example.mosim.feature.escort

import com.example.mosim.data.EscortSession
import com.example.mosim.data.mock.MockCabinControl
import com.example.mosim.data.mock.MockEscortTrips
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechInput
import com.example.mosim.data.mock.MockTripSource
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

/**
 * E ↔ F-2 · F-3 · G 연결 부분만 본다. 명령 해석과 실내 조절 자체는 EscortViewModelTest가 본다.
 *
 * 실제 화면 대신 ViewModel의 공개 함수를 화면과 같은 순서로 부른다.
 * (F-2가 받은 문장 → handleVoiceText → F-3 검색 → addWaypointAt → 도착)
 */
class EscortIntegrationTest {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val speech = MockSpeechInput()
    private val cabin = MockCabinControl(scope, requestDelayMillis = 0, applyDelayMillis = 10)
    private var trip = MockTripSource(scope, requestDelayMillis = 0, routingDelayMillis = 10)
    private var places = MockPlaceSearchSource(requestDelayMillis = 0)

    @Before
    fun clearSession() = EscortSession.reset()

    @After
    fun tearDown() {
        EscortSession.reset()
        scope.cancel()
        dispatcher.close()
    }

    private fun createViewModel() = EscortViewModel(
        scope = scope,
        trip = MockEscortTrips.MOTHER_TO_HOSPITAL,
        speechInput = speech,
        cabinControl = cabin,
        tripSource = trip,
        placeSearch = places,
        alerts = SilentAlerts(),
        tripIsSimulated = true,
    )

    @Test
    fun 장소_요청_문장은_F3으로_보낼_검색어를_돌려준다() = runBlocking {
        val routing = withTimeout(TIMEOUT) { createViewModel().handleVoiceText("커피 마시고 싶은데") }
        assertEquals(VoiceRouting.SearchPlaces("커피 마시고 싶은데"), routing)
    }

    @Test
    fun 장소_요청이_아닌_명령은_여기서_처리하고_화면을_넘기지_않는다() = runBlocking {
        val model = createViewModel()
        val routing = withTimeout(TIMEOUT) { model.handleVoiceText("소리 줄여") }
        assertTrue(routing is VoiceRouting.Handled)
        // 실행 결과가 기록으로 남는다. 화면은 이 안내를 E-1 토스트로 본다.
        val state = withTimeout(TIMEOUT) { model.uiState.first { it.history.isNotEmpty() } }
        assertEquals(1, state.history.size)
    }

    @Test
    fun 해석하지_못한_문장도_성공으로_처리하지_않는다() = runBlocking {
        val model = createViewModel()
        val routing = withTimeout(TIMEOUT) { model.handleVoiceText("창문 열어 줘") }
        assertTrue(routing is VoiceRouting.Handled)
        val state = withTimeout(TIMEOUT) { model.uiState.first { it.notice != null } }
        assertEquals(EscortMessages.NOT_UNDERSTOOD, state.notice?.message)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun F3_검색은_현재_위치와_후보를_함께_돌려준다() = runBlocking {
        val outcome = withTimeout(TIMEOUT) { createViewModel().searchPlacesFor("근처 카페") }
        assertEquals(MockTripSource.DEFAULT_LOCATION, outcome.origin)
        assertNull(outcome.failureMessage)
        assertEquals("카페 A", outcome.places?.firstOrNull()?.name)
    }

    @Test
    fun F3_검색_실패는_이유를_돌려주고_후보를_만들지_않는다() = runBlocking {
        places = MockPlaceSearchSource(failure = FailureReason.NETWORK, requestDelayMillis = 0)
        val outcome = withTimeout(TIMEOUT) { createViewModel().searchPlacesFor("근처 카페") }
        assertNull(outcome.places)
        assertNotNull(outcome.failureMessage)
    }

    @Test
    fun F3에서_고른_장소는_경유지로_추가된다() = runBlocking {
        val model = createViewModel()
        val added = withTimeout(TIMEOUT) { model.addWaypointAt("카페 A", GeoPoint(37.5, 127.0)) }
        assertTrue(added)
        val state = withTimeout(TIMEOUT) { model.uiState.first { it.history.isNotEmpty() } }
        assertEquals(ActionOutcome.SUCCEEDED, state.history.first().outcome)
    }

    @Test
    fun 경유지_추가_실패는_false이고_성공으로_바꾸지_않는다() = runBlocking {
        // 활성 경로가 없으면 Mock도 실제 NaviHelper처럼 실패한다.
        trip = MockTripSource(scope, initialStatus = null, requestDelayMillis = 0)
        val model = createViewModel()
        val added = withTimeout(TIMEOUT) { model.addWaypointAt("카페 A", GeoPoint(37.5, 127.0)) }
        assertFalse(added)
        val state = withTimeout(TIMEOUT) { model.uiState.first { it.history.isNotEmpty() } }
        assertEquals(ActionOutcome.FAILED, state.history.first().outcome)
    }

    @Test
    fun 도착하면_이번_에스코트_요약을_남긴다() = runBlocking {
        val model = createViewModel()
        withTimeout(TIMEOUT) { model.addWaypointAt("카페 A", GeoPoint(37.5, 127.0)) }
        withTimeout(TIMEOUT) { model.handleVoiceText("소리 줄여") }
        trip.simulateArrival()

        val state = withTimeout(TIMEOUT) { model.uiState.first { it.arrived } }
        assertTrue(state.arrived)
        val summary = EscortSession.summary
        assertNotNull(summary)
        assertEquals(listOf("카페 A"), summary?.waypointNames)
        assertEquals(1, summary?.voiceRequestCount)
        // 경유지 추가는 자동 조정 횟수에 넣지 않는다. 음량 조절 1건만 센다.
        assertEquals(1, summary?.autoAdjustCount)
        assertTrue(summary?.isSimulated == true)
    }

    /** 테스트에서는 시스템 알림을 띄우지 않는다. 무엇을 보냈는지는 여기서 보지 않는다. */
    private class SilentAlerts : EscortAlerts {
        override fun showNotice(message: String) = Unit
        override fun showProposal(message: String) = Unit
        override fun clearProposal() = Unit
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
