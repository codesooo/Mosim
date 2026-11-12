package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/**
 * 이동 중 경로 정보 경계다. 실제 구현은 NaviHelper(✅)로 한다. 이번 단계는 Mock만 둔다.
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
)

/**
 * 지금 달리는 도로 종류.
 * [확인 필요] NaviHelper에 도로 종류 필드가 없다. 후보 신호: TBTInfo의 TurnType.TURN_RESTAREA /
 * TURN_ENTER_SERVICEAREA, onCurrentRoadSpeedLimit, TrafficCongestionInfo.roadName. 실제 구현 때 정한다.
 */
enum class RoadType { HIGHWAY, LOCAL }
