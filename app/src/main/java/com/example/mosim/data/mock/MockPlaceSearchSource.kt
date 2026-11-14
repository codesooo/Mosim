package com.example.mosim.data.mock

import com.example.mosim.feature.waypoint.PlaceSearchResult
import com.example.mosim.feature.waypoint.PlaceSearchSource
import com.example.mosim.feature.waypoint.PlaceUiModel
import kotlinx.coroutines.delay

/**
 * Mock: 장소 검색 담당자의 구현이 붙기 전까지 F-3을 돌려 보기 위한 가짜 검색.
 *
 * 실제 API처럼 비동기로 응답하고, 결과 없음과 실패도 낼 수 있다.
 * 값은 피그마 F-3 프레임의 카페 3곳을 그대로 옮겼다.
 */
class MockPlaceSearchSource(
    private val result: PlaceSearchResult = PlaceSearchResult.Found(mockPlaces()),
    private val delayMillis: Long = 900L,
) : PlaceSearchSource {

    override suspend fun search(query: String): PlaceSearchResult {
        delay(delayMillis)
        return result
    }
}

fun mockPlaces() = listOf(
    PlaceUiModel("mock-cafe-a", "카페 A", "0.4km · 추가 이동 3분", isMock = true),
    PlaceUiModel("mock-cafe-b", "카페 B", "0.7km · 추가 이동 5분", isMock = true),
    PlaceUiModel("mock-cafe-c", "카페 C", "0.9km · 추가 이동 6분", isMock = true),
)
