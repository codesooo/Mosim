package com.example.mosim.data.mock

import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceUiModel
import com.example.mosim.platform.pleos.GeoPoint
import kotlinx.coroutines.delay

/**
 * Mock: 장소 검색 담당자의 구현이 붙기 전까지 F-3을 돌려 보기 위한 가짜 검색.
 *
 * 실제 API처럼 비동기로 응답하고, 결과 없음과 실패도 낼 수 있다.
 * 값은 피그마 F-3 프레임의 카페 3곳을 그대로 옮겼다.
 *
 * 원래 이름은 MockPlaceSearchSource였다. 에스코트(data.place.PlaceSearchSource)의 Mock과 파일·클래스 이름이 같아
 * merge에서 이쪽이 사라졌기 때문에 이름을 나눴다. 이 클래스는 F-3용 feature.waypoint.PlaceSearchSource를 구현한다.
 */
class MockWaypointSearchSource(
    private val result: PlaceSearchResult = PlaceSearchResult.Found(mockPlaces()),
    private val delayMillis: Long = 900L,
) : PlaceSearchSource {

    override suspend fun search(query: String): PlaceSearchResult {
        delay(delayMillis)
        return result
    }
}

// Mock 좌표다. 실제 장소가 아니라 MockTripSource 기본 위치(서울 도심) 근처의 임의 값이다.
fun mockPlaces() = listOf(
    PlaceUiModel(
        "mock-cafe-a", "카페 A", "0.4km · 추가 이동 3분", isMock = true,
        location = GeoPoint(latitude = 37.5690, longitude = 126.9800),
    ),
    PlaceUiModel(
        "mock-cafe-b", "카페 B", "0.7km · 추가 이동 5분", isMock = true,
        location = GeoPoint(latitude = 37.5720, longitude = 126.9760),
    ),
    PlaceUiModel(
        "mock-cafe-c", "카페 C", "0.9km · 추가 이동 6분", isMock = true,
        location = GeoPoint(latitude = 37.5610, longitude = 126.9850),
    ),
)
