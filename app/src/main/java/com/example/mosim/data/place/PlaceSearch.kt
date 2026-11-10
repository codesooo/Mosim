package com.example.mosim.data.place

import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult

/** 동승자가 요청할 수 있는 장소 종류다. 카카오 카테고리 코드와의 대응은 구현체가 정한다. */
enum class PlaceKind {
    CAFE,
    RESTROOM,
    CONVENIENCE_STORE,
    REST_AREA,
    EV_CHARGER,
}

/**
 * 검색된 장소 하나다.
 * 경유 시 늘어나는 시간은 장소 API가 주지 않는다. NaviHelper requestTrip으로 따로 구한다. [확인 필요]
 */
data class PlaceCandidate(
    val id: String,
    val name: String,
    val categoryLabel: String?,
    val address: String?,
    val location: GeoPoint,
    val distanceMeters: Int?,
)

/**
 * 근처 장소 검색 경계다. 실제 구현은 카카오 로컬 REST(data/remote/kakao), 개발용은 MockPlaceSearchSource.
 * 결과가 없으면 실패가 아니라 빈 목록을 돌려준다.
 */
interface PlaceSearchSource {
    suspend fun searchNearby(
        kind: PlaceKind,
        center: GeoPoint,
        radiusMeters: Int,
    ): SourceResult<List<PlaceCandidate>>
}
