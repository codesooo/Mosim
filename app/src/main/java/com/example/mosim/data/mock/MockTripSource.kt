package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import com.example.mosim.platform.pleos.TripSource
import com.example.mosim.platform.pleos.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mock이다. 실제 경로 정보가 아니다.
 * 흉내 내는 API: NaviHelper requestTrip/onTripInfo, getCurrentLocationInfo, addWaypoint/onWaypointChanged.
 *
 * - addWaypoint는 요청만 받고, [routingDelayMillis] 뒤 재탐색이 끝난 것처럼 ETA를 [detourMinutes]만큼 늦춘다.
 * - 활성 경로가 없거나 경유지가 꽉 차면 실패한다(NaviHelper 경유지 최대 2개).
 * - [simulateTrafficDelay]는 실제 SDK에 없는 Mock 전용 조작이다. 지연 감지(F-1) 시연과 테스트에 쓴다.
 */
class MockTripSource(
    private val scope: CoroutineScope,
    initialStatus: TripStatus? = DEFAULT_STATUS,
    private val location: GeoPoint? = DEFAULT_LOCATION,
    private val failure: FailureReason? = null,
    private val requestDelayMillis: Long = 100,
    private val routingDelayMillis: Long = 500,
    private val detourMinutes: Long = 5,
) : TripSource {

    private val mutableStatus = MutableStateFlow(initialStatus)
    override val tripStatus: StateFlow<TripStatus?> = mutableStatus.asStateFlow()

    override suspend fun currentLocation(): SourceResult<GeoPoint> {
        delay(requestDelayMillis)
        failure?.let { return SourceResult.Failure(it) }
        // 실제 NaviHelper도 위치를 못 주는 경우가 있다. null을 임의 좌표로 바꾸지 않는다.
        return location?.let { SourceResult.Success(it) }
            ?: SourceResult.Failure(FailureReason.UNKNOWN, "현재 위치 없음")
    }

    override suspend fun addWaypoint(name: String, location: GeoPoint): SourceResult<Unit> {
        delay(requestDelayMillis)
        failure?.let { return SourceResult.Failure(it) }

        val status = mutableStatus.value
            ?: return SourceResult.Failure(FailureReason.INVALID_REQUEST, "활성 경로 없음")
        if (status.waypointCount >= TripSource.MAX_WAYPOINTS) {
            return SourceResult.Failure(FailureReason.INVALID_REQUEST, "경유지 ${TripSource.MAX_WAYPOINTS}개 초과")
        }

        scope.launch {
            delay(routingDelayMillis)
            mutableStatus.update { current ->
                current?.copy(
                    eta = current.eta?.plusMinutes(detourMinutes),
                    remainMinutes = current.remainMinutes?.plus(detourMinutes.toInt()),
                    waypointCount = current.waypointCount + 1,
                )
            }
        }
        return SourceResult.Success(Unit)
    }

    /** Mock 전용. 정체로 도착이 [extraMinutes]분 늦어진 상황을 만든다. */
    fun simulateTrafficDelay(extraMinutes: Long) {
        mutableStatus.update { current ->
            current?.copy(
                eta = current.eta?.plusMinutes(extraMinutes),
                remainMinutes = current.remainMinutes?.plus(extraMinutes.toInt()),
            )
        }
    }

    companion object {
        // 피그마 E-1 예시 값: 09:12 도착, 남은 거리 14.6km · 43분
        val DEFAULT_STATUS = TripStatus(
            eta = java.time.LocalTime.of(9, 12),
            remainDistanceMeters = 14_600,
            remainMinutes = 43,
            waypointCount = 0,
        )

        // 임의 좌표(서울 도심). 실제 위치가 아니다.
        val DEFAULT_LOCATION = GeoPoint(latitude = 37.5665, longitude = 126.9780)
    }
}
