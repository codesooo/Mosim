package com.example.mosim.platform.pleos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.ceil

/**
 * NaviHelper로 경로 상태를 읽고 경유지를 추가하는 TripSource.
 *
 * - NaviHelper는 남은 시간을 "초"로 준다(TripInfo.eta도 시각이 아니라 초였다). 도착 시각은 지금 + 남은 초다.
 * - 남은 시간이 밀어 주는 이벤트로 오지 않아 [refreshIntervalMillis]마다 다시 묻는다.
 * - 차가 서 있으면 남은 초가 그대로라 도착 시각이 시간만큼 늦어진다. 정체와 같은 결과라 지연 감지가 그대로 동작한다.
 * - 도로 종류(roadType)는 아직 읽지 않는다. [확인 필요] TBTInfo의 IC·휴게소 진입 안내 등으로 판단할지.
 */
class PleosTripSource(
    private val scope: CoroutineScope,
    private val navi: NaviApi,
    private val clock: () -> LocalTime = LocalTime::now,
    private val refreshIntervalMillis: Long = 5_000,
) : TripSource {

    private val mutableStatus = MutableStateFlow<TripStatus?>(null)
    override val tripStatus: StateFlow<TripStatus?> = mutableStatus.asStateFlow()

    init {
        // 도착 이벤트는 밀어 주는 쪽이라 polling과 별도로 받는다. 남은 거리 0을 도착으로 추측하지 않는다.
        scope.launch {
            navi.arrivals.collect {
                mutableStatus.update { current -> current?.copy(arrived = true) }
            }
        }
        scope.launch {
            while (isActive) {
                refresh()
                delay(refreshIntervalMillis)
            }
        }
    }

    override suspend fun currentLocation(): SourceResult<GeoPoint> = navi.currentLocation()

    override suspend fun ensureRoute(destinationName: String, location: GeoPoint): SourceResult<Unit> {
        when (val progress = navi.routeProgress()) {
            is SourceResult.Failure -> return progress
            is SourceResult.Success -> if (progress.value != null) return SourceResult.Success(Unit)
        }
        val requested = navi.requestRoute(destinationName, location)
        if (requested is SourceResult.Success) refresh()
        return requested
    }

    override suspend fun addWaypoint(name: String, location: GeoPoint): SourceResult<Unit> {
        if (mutableStatus.value == null) {
            return SourceResult.Failure(FailureReason.INVALID_REQUEST, "안내 중인 경로가 없다")
        }
        val used = when (val count = navi.waypointCount()) {
            is SourceResult.Success -> count.value
            is SourceResult.Failure -> return count
        }
        if (used >= TripSource.MAX_WAYPOINTS) {
            return SourceResult.Failure(FailureReason.INVALID_REQUEST, "경유지 ${TripSource.MAX_WAYPOINTS}개가 모두 찼다")
        }
        val added = navi.addWaypoint(slot = used, name = name, location = location)
        if (added is SourceResult.Success) refresh()
        return added
    }

    /** 실패하면 이전 값을 지우지 않는다. 한 번 응답이 늦었다고 화면의 도착 시각을 "—"로 바꾸지 않기 위해서다. */
    private suspend fun refresh() {
        val progress = when (val result = navi.routeProgress()) {
            is SourceResult.Success -> result.value
            is SourceResult.Failure -> return
        }
        if (progress == null) {
            mutableStatus.value = null
            return
        }
        val waypoints = (navi.waypointCount() as? SourceResult.Success)?.value
            ?: mutableStatus.value?.waypointCount
            ?: 0
        mutableStatus.value = TripStatus(
            eta = clock().plusSeconds(progress.remainSeconds.toLong()).withSecond(0).withNano(0),
            remainDistanceMeters = progress.remainDistanceMeters,
            remainMinutes = ceil(progress.remainSeconds / SECONDS_PER_MINUTE).toInt(),
            waypointCount = waypoints,
            // 도착은 이벤트로만 정한다. 다시 조회했다고 도착 상태를 지우지 않는다.
            arrived = mutableStatus.value?.arrived == true,
        )
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
    }
}
