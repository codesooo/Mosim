package com.example.mosim.feature.waypoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** F-3이 그릴 수 있는 상태. 검색은 비동기라 대기·결과 없음·실패를 모두 화면이 다뤄야 한다. */
enum class WaypointRequest { Idle, Requested, NotConnected, Applied, Failed }

sealed interface WaypointUiState {
    data object Searching : WaypointUiState

    data class Ready(
        val places: List<PlaceUiModel>,
        val selected: PlaceUiModel,
        /**
         * 경유지 추가 요청의 진행 상태.
         * **[WaypointRequest.Requested]는 "요청을 보냈다"일 뿐 경로가 바뀜다는 뜻이 아니다.**
         */
        val request: WaypointRequest = WaypointRequest.Idle,
    ) : WaypointUiState

    data object Empty : WaypointUiState

    data class Failed(val message: String) : WaypointUiState
}

/**
 * F-3 경유지 후보 상태.
 *
 * [source]는 장소 검색 담당자가 넣어 줄 경계다. 화면이 직접 네트워크를 타지 않는다.
 * 첫 후보를 미리 골라 두는 이유: 피그마 F-3에서 1번이 이미 선택돼 있고, 버튼 문구도 그 이름을 쓴다.
 */
class WaypointViewModel(
    private val source: PlaceSearchSource,
    private val query: String,
    private val searchScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope get() = searchScope ?: viewModelScope
    private val mutableState = MutableStateFlow<WaypointUiState>(WaypointUiState.Searching)
    val state = mutableState.asStateFlow()
    private var search: Job? = null

    init {
        search()
    }

    fun search() {
        search?.cancel()
        mutableState.value = WaypointUiState.Searching
        search = scope.launch {
            mutableState.value = when (val result = try {
                source.search(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PlaceSearchResult.Failed("장소를 불러오지 못했어요")
            }) {
                is PlaceSearchResult.Found ->
                    result.places.firstOrNull()
                        ?.let { WaypointUiState.Ready(result.places.take(3), it) }
                        ?: WaypointUiState.Empty
                PlaceSearchResult.Empty -> WaypointUiState.Empty
                is PlaceSearchResult.Failed -> WaypointUiState.Failed(result.message)
            }
        }
    }

    fun select(place: PlaceUiModel) {
        val ready = state.value as? WaypointUiState.Ready ?: return
        if (place !in ready.places || ready.request == WaypointRequest.Requested ||
            ready.request == WaypointRequest.Applied) return
        mutableState.value = ready.copy(selected = place)
    }

    /** 고른 장소를 경유지로 **요청**한다. 실제 추가는 NaviHelper 담당자 몫이다. */
    fun requestWaypoint(): PlaceUiModel? {
        val ready = state.value as? WaypointUiState.Ready ?: return null
        if (ready.request == WaypointRequest.Requested || ready.request == WaypointRequest.Applied) return null
        mutableState.value = ready.copy(request = WaypointRequest.Requested)
        return ready.selected
    }

    /** NaviHelper 담당자가 실패를 알려 올 때 부른다. */
    fun waypointFailed() {
        val ready = state.value as? WaypointUiState.Ready ?: return
        mutableState.value = ready.copy(request = WaypointRequest.Failed)
    }

    fun updateRequest(result: WaypointRequest) {
        val ready = state.value as? WaypointUiState.Ready ?: return
        if (ready.request == WaypointRequest.Requested) mutableState.value = ready.copy(request = result)
    }

    override fun onCleared() {
        search?.cancel()
    }
}
