package com.example.mosim.feature.waypoint

/**
 * F-3이 보여 주는 장소 한 곳. 화면에 그릴 수 있게 이미 다듬어진 문자열만 담는다.
 *
 * 좌표를 넣지 않은 이유: 경유지 추가는 NaviHelper 담당자 몫이고, 그쪽이 쓰는 좌표 타입이 아직 정해지지 않았다.
 * [externalId]로 원본 검색 결과를 되찾아 쓰라는 뜻이다.
 */
data class PlaceUiModel(
    /** 검색 모듈이 만든 식별자. 선택 결과를 돌려줄 때 그대로 실어 보낸다. */
    val externalId: String,
    val name: String,
    /** 목록 두 번째 줄. 예: "0.4km · 경유 +3분" */
    val detail: String = "",
    /** 이름 옆 작은 태그. 예: "드라이브스루" */
    val tag: String? = null,
    val isMock: Boolean = false,
)

/** 장소 검색 결과. 화면이 다뤄야 하는 경우를 전부 나눠 둔다. */
sealed interface PlaceSearchResult {
    data class Found(val places: List<PlaceUiModel>) : PlaceSearchResult

    /** 검색은 됐지만 조건에 맞는 곳이 없다. */
    data object Empty : PlaceSearchResult

    /** 검색 자체가 실패했다. [message]는 화면에 그대로 보여 준다. */
    data class Failed(val message: String) : PlaceSearchResult
}

/**
 * 장소 검색 경계. **구현은 장소 검색 담당자 몫이다.**
 *
 * F-3은 이 인터페이스만 보고, 화면 안에서 직접 네트워크를 타지 않는다.
 * 지금은 `data/mock/MockPlaceSearchSource.kt`가 유일한 구현이다.
 */
interface PlaceSearchSource {
    /** [query]는 F-2가 인식한 문장이다. 실패를 예외로 던지지 말고 [PlaceSearchResult.Failed]로 돌려준다. */
    suspend fun search(query: String): PlaceSearchResult
}
