package com.example.mosim.feature.escort

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.RoadType

/**
 * 휴식 장소로 무엇을 찾을지 정한다(팀 결정, 2026-11-12).
 * - 고속도로: 휴게소만. 휴게소는 수십 km 간격이라 반경을 넓힌다.
 * - 국도: 휴게소 → 공원 → 카페 순으로, 반경 안에 있는 첫 종류의 가장 가까운 곳.
 *   도심·국도에서는 카페가 거의 항상 가장 가까워서, 거리만 보면 늘 카페가 된다. 쉬기 좋은 곳을 먼저 본다.
 * - 도로 종류를 모르면 국도 규칙을 쓴다. 고속도로 규칙은 도심에서 거의 못 찾기 때문이다.
 */
object RestPlaceRule {

    /** [확인 필요] 국도 검색 반경 */
    const val LOCAL_RADIUS_METERS = 2_000

    /** 카카오 로컬 radius 최대값. [확인 필요] 진행 방향 반대편·지나온 휴게소가 나올 수 있다 */
    const val HIGHWAY_RADIUS_METERS = 20_000

    data class SearchPlan(val kindsInOrder: List<PlaceKind>, val radiusMeters: Int)

    fun plan(roadType: RoadType?): SearchPlan = when (roadType) {
        RoadType.HIGHWAY -> SearchPlan(listOf(PlaceKind.REST_AREA), HIGHWAY_RADIUS_METERS)
        RoadType.LOCAL, null -> SearchPlan(
            listOf(PlaceKind.REST_AREA, PlaceKind.PARK, PlaceKind.CAFE),
            LOCAL_RADIUS_METERS,
        )
    }
}
