package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/**
 * 이동 중 경로 정보 경계다. 실제 구현은 PleosTripSource(NaviHelper), 개발용은 MockTripSource.
 *
 * NaviHelper 대응:
 * - [tripStatus] ← requestTrip → onTripInfo(eta), getDestinationInfo → onDestinationInfo(remainDistance, remainTime)
 * - [currentLocation] ← getCurrentLocationInfo → onCurrentLocationInfo
 *   (FusedLocation이 🔜이라 NaviHelper로 위치를 얻는다)
 * - [addWaypoint] ← addWaypoint(RequestWaypointInfo). 결과는 onWaypointChanged / onError로 온다
 *
 * NaviHelper 조회 API는 값을 바로 돌려주지 않고 콜백으로 준다. 콜백이 안 올 수도 있으므로
 * 실제 구현은 시간 제한을 두고 [FailureReason.CONNECTION_FAILED]로 끝내야 한다.
 */
interface TripSource {

    /** 안내 중인 경로가 없거나 아직 모르면 null이다. */
    val tripStatus: StateFlow<TripStatus?>

    suspend fun currentLocation(): SourceResult<GeoPoint>

    /** 요청이 받아들여지면 Success다. 경로 재탐색이 끝난 뒤 [tripStatus]가 바뀐다. */
    suspend fun addWaypoint(name: String, location: GeoPoint): SourceResult<Unit>

    /**
     * 안내 중인 경로가 없으면 [location]으로 경로를 요청한다. 이미 있으면 그대로 둔다.
     * 목적지 설정은 원래 이동 설정(A-3) 몫이다. A-3이 생기기 전까지 에스코트 시작 때 부른다(팀 결정 2026-11-12).
     */
    suspend fun ensureRoute(destinationName: String, location: GeoPoint): SourceResult<Unit> = SourceResult.Success(Unit)

    companion object {
        /** NaviHelper WaypointIndex는 FIRST, SECOND 두 개뿐이다. */
        const val MAX_WAYPOINTS = 2
    }
}

data class TripStatus(
    val eta: LocalTime?,
    val remainDistanceMeters: Int?,
    val remainMinutes: Int?,
    val waypointCount: Int,
    /** 모르면 null이다. 휴식 장소 기준(RestPlaceRule)이 달라진다. */
    val roadType: RoadType? = null,
    /**
     * 목적지에 도착했다고 내비가 알려 왔는가(NaviHelper onDestinationArrived).
     * **도착은 하차가 아니다.** 하차 감지는 별도 신호이며 아직 없다 [확인 필요].
     */
    val arrived: Boolean = false,
)

/**
 * 지금 달리는 도로 종류.
 * [확인 필요] NaviHelper에 도로 종류 필드가 없다. 후보 신호: TBTInfo의 TurnType.TURN_RESTAREA /
 * TURN_ENTER_SERVICEAREA, onCurrentRoadSpeedLimit, TrafficCongestionInfo.roadName. 실제 구현 때 정한다.
 */
enum class RoadType { HIGHWAY, LOCAL }
