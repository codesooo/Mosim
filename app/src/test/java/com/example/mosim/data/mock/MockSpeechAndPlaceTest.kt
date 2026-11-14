package com.example.mosim.data.mock

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSpeechAndPlaceTest {

    private val center = MockTripSource.DEFAULT_LOCATION

    @Test
    fun `say로 넣은 문장이 듣는 쪽에 전달된다`() = runBlocking {
        val speech = MockSpeechInput()
        val received = async(start = CoroutineStart.UNDISPATCHED) { speech.recognizedTexts.first() }

        speech.say("좀 덥다")

        assertEquals("좀 덥다", withTimeout(1_000) { received.await() })
    }

    @Test
    fun `장소 검색은 반경 안의 장소를 가까운 순으로 준다`() = runBlocking {
        val search = MockPlaceSearchSource(requestDelayMillis = 0)

        val result = search.searchNearby(PlaceKind.CAFE, center, radiusMeters = 1_000)

        val names = (result as SourceResult.Success).value.map { it.name }
        assertEquals(listOf("카페 A", "카페 B"), names)
    }

    @Test
    fun `결과가 없으면 실패가 아니라 빈 목록이다`() = runBlocking {
        val search = MockPlaceSearchSource(places = emptyMap(), requestDelayMillis = 0)

        val result = search.searchNearby(PlaceKind.CAFE, GeoPoint(0.0, 0.0), radiusMeters = 1_000)

        assertEquals(SourceResult.Success(emptyList<Any>()), result)
    }

    @Test
    fun `실패 사유를 주면 검색이 실패한다`() = runBlocking {
        val search = MockPlaceSearchSource(failure = FailureReason.NETWORK, requestDelayMillis = 0)

        val result = search.searchNearby(PlaceKind.CAFE, center, radiusMeters = 1_000)

        assertTrue(result is SourceResult.Failure && result.reason == FailureReason.NETWORK)
    }
}
