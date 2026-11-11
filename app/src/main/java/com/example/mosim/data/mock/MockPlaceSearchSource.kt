package com.example.mosim.data.mock

import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.data.place.PlaceKind
import com.example.mosim.data.place.PlaceSearchSource
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.delay

/**
 * Mock이다. 실제 장소가 아니다. 이름은 피그마 예시("카페 A", "휴식 장소 A")를 따른다.
 * 흉내 내는 API: 카카오 로컬 카테고리 검색 (반경 안, 가까운 순)
 *
 * 좌표는 MockTripSource 기본 위치 근처의 임의 값이다. 검색 중심과 상관없이 같은 목록에서 고른다.
 */
class MockPlaceSearchSource(
    private val places: Map<PlaceKind, List<PlaceCandidate>> = DEFAULT_PLACES,
    private val failure: FailureReason? = null,
    private val requestDelayMillis: Long = 300,
) : PlaceSearchSource {

    override suspend fun searchNearby(
        kind: PlaceKind,
        center: GeoPoint,
        radiusMeters: Int,
    ): SourceResult<List<PlaceCandidate>> {
        delay(requestDelayMillis)
        failure?.let { return SourceResult.Failure(it) }

        val found = places[kind].orEmpty()
            .filter { place -> place.distanceMeters == null || place.distanceMeters <= radiusMeters }
            .sortedBy { place -> place.distanceMeters ?: Int.MAX_VALUE }
        return SourceResult.Success(found)
    }

    companion object {
        private fun place(
            id: String,
            name: String,
            label: String,
            distanceMeters: Int,
            latitude: Double,
            longitude: Double,
        ) = PlaceCandidate(
                id = id,
                name = name,
                categoryLabel = label,
                address = null,
                location = GeoPoint(latitude, longitude),
                distanceMeters = distanceMeters,
            )

        val DEFAULT_PLACES: Map<PlaceKind, List<PlaceCandidate>> = mapOf(
            PlaceKind.CAFE to listOf(
                place("mock-cafe-a", "카페 A", "카페 · 드라이브스루", 400, 37.5690, 126.9800),
                place("mock-cafe-b", "카페 B", "카페", 900, 37.5720, 126.9760),
                place("mock-cafe-c", "카페 C", "카페", 1_600, 37.5610, 126.9850),
            ),
            PlaceKind.REST_AREA to listOf(
                // EscortViewModel 검색 반경(2km) 안에 들어오게 둔다.
                place("mock-rest-a", "휴식 장소 A", "화장실 · 카페", 1_800, 37.5800, 126.9900),
            ),
            PlaceKind.RESTROOM to listOf(
                place("mock-restroom-a", "공중화장실 A", "화장실", 700, 37.5650, 126.9820),
            ),
            PlaceKind.CONVENIENCE_STORE to listOf(
                place("mock-store-a", "편의점 A", "편의점", 300, 37.5670, 126.9790),
            ),
            PlaceKind.EV_CHARGER to listOf(
                place("mock-charger-a", "충전소 A", "급속 충전", 1_200, 37.5600, 126.9700),
            ),
        )
    }
}
