package com.example.mosim

import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.mockPlaces
import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceUiModel
import com.example.mosim.feature.waypoint.WaypointRequest
import com.example.mosim.feature.waypoint.WaypointUiState
import com.example.mosim.feature.waypoint.WaypointViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaypointViewModelTest {

    @Test
    fun `검색 중으로 시작해 결과를 받으면 첫 후보가 선택된다`(): Unit = runBlocking {
        val model = viewModel(PlaceSearchResult.Found(mockPlaces()), this)
        assertEquals(WaypointUiState.Searching, model.state.value)
        val ready = model.awaitReady()
        assertEquals(3, ready.places.size)
        assertEquals("카페 A", ready.selected.name)
        assertEquals(WaypointRequest.Idle, ready.request)
    }

    @Test
    fun `결과 없음과 실패를 구분해 상태로 올린다`(): Unit = runBlocking {
        val empty = viewModel(PlaceSearchResult.Empty, this)
        withTimeout(2_000) { empty.state.first { it == WaypointUiState.Empty } }

        val failed = viewModel(PlaceSearchResult.Failed("장소를 불러오지 못했어요"), this)
        val state = withTimeout(2_000) {
            failed.state.first { it is WaypointUiState.Failed } as WaypointUiState.Failed
        }
        assertEquals("장소를 불러오지 못했어요", state.message)
    }

    @Test
    fun `빈 목록을 돌려줘도 결과 없음으로 본다`(): Unit = runBlocking {
        val model = viewModel(PlaceSearchResult.Found(emptyList()), this)
        withTimeout(2_000) { model.state.first { it == WaypointUiState.Empty } }
    }

    @Test
    fun `장소를 고르면 선택이 바뀌고 목록 밖 장소는 무시한다`(): Unit = runBlocking {
        val model = viewModel(PlaceSearchResult.Found(mockPlaces()), this)
        val ready = model.awaitReady()

        model.select(ready.places[2])
        assertEquals("카페 C", (model.state.value as WaypointUiState.Ready).selected.name)

        model.select(PlaceUiModel("not-in-list", "카페 Z", "1.0km"))
        assertEquals("카페 C", (model.state.value as WaypointUiState.Ready).selected.name)
    }

    @Test
    fun `경유 요청은 고른 장소를 돌려주고 요청 상태까지만 간다`(): Unit = runBlocking {
        val model = viewModel(PlaceSearchResult.Found(mockPlaces()), this)
        model.awaitReady()

        val requested = model.requestWaypoint()
        assertEquals("카페 A", requested?.name)
        assertEquals(WaypointRequest.Requested, (model.state.value as WaypointUiState.Ready).request)

        // 중복 요청은 막는다.
        assertNull(model.requestWaypoint())
        // 요청 뒤에는 선택을 바꾸지 못한다.
        model.select(mockPlaces()[1])
        assertEquals("카페 A", (model.state.value as WaypointUiState.Ready).selected.name)

        model.waypointFailed()
        assertEquals(WaypointRequest.Failed, (model.state.value as WaypointUiState.Ready).request)
    }

    @Test
    fun `검색 전에는 경유 요청이 아무 것도 돌려주지 않는다`(): Unit = runBlocking {
        val model = viewModel(PlaceSearchResult.Found(mockPlaces()), this)
        assertNull(model.requestWaypoint())
    }

    @Test
    fun `다시 찾기는 검색 중으로 되돌린 뒤 결과를 다시 받는다`(): Unit = runBlocking {
        val source = CountingSource(PlaceSearchResult.Found(mockPlaces()))
        val model = WaypointViewModel(source, "커피", this)
        model.awaitReady()
        assertEquals(1, source.calls)

        model.search()
        model.awaitReady()
        assertEquals(2, source.calls)
    }

    @Test
    fun `검색어는 그대로 검색 모듈에 전달된다`(): Unit = runBlocking {
        val source = CountingSource(PlaceSearchResult.Found(mockPlaces()))
        val model = WaypointViewModel(source, "커피 마시고 싶은데 근처에 카페 있을까?", this)
        model.awaitReady()
        assertEquals("커피 마시고 싶은데 근처에 카페 있을까?", source.lastQuery)
        assertTrue(source.calls == 1)
    }

    private fun viewModel(result: PlaceSearchResult, scope: CoroutineScope) =
        WaypointViewModel(MockPlaceSearchSource(result, delayMillis = 1), "커피", scope)

    private suspend fun WaypointViewModel.awaitReady(): WaypointUiState.Ready =
        withTimeout(2_000) { state.first { it is WaypointUiState.Ready } } as WaypointUiState.Ready

    private class CountingSource(private val result: PlaceSearchResult) : PlaceSearchSource {
        var calls = 0
        var lastQuery: String? = null

        override suspend fun search(query: String): PlaceSearchResult {
            calls++
            lastQuery = query
            return result
        }
    }
}
