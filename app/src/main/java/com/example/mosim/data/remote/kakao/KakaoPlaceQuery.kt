package com.example.mosim.data.remote.kakao

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.GeoPoint
import java.net.URLEncoder
import java.util.Locale

/**
 * PlaceKind를 카카오 로컬 검색 요청으로 바꾼다.
 * 근거: 카카오 개발자 문서 "카카오맵 REST API" 카테고리 / 키워드로 장소 검색 (2026-11-12 확인).
 *
 * 카테고리 코드가 딱 맞는 종류만 카테고리 검색을 쓰고, 나머지는 키워드 검색을 쓴다.
 */
sealed interface KakaoPlaceQuery {
    data class Category(val groupCode: String) : KakaoPlaceQuery
    /**
     * 키워드 검색은 이름에 검색어가 들어간 다른 업종도 돌려준다(예: "휴게소" → 분식집 "○○휴게소").
     * 그래서 분류 마지막 단계에 [categoryMustContain]이 들어간 곳만 남긴다.
     */
    data class Keyword(val text: String, val categoryMustContain: String) : KakaoPlaceQuery {
        fun accepts(categoryLabel: String?): Boolean = categoryLabel?.contains(categoryMustContain) == true
    }

    companion object {
        private const val BASE_URL = "https://dapi.kakao.com/v2/local/search"

        /** 문서상 radius 최대값. */
        const val MAX_RADIUS_METERS = 20_000

        /** 문서상 한 페이지 최대 개수. 첫 페이지만 쓴다(가까운 곳 하나만 제안한다). */
        const val PAGE_SIZE = 15

        fun of(kind: PlaceKind): KakaoPlaceQuery = when (kind) {
            PlaceKind.CAFE -> Category("CE7")
            PlaceKind.CONVENIENCE_STORE -> Category("CS2")
            // OL7은 "주유소, 충전소"를 함께 묶어 주유소가 섞인다. [확인 필요] 키워드 결과 품질
            PlaceKind.EV_CHARGER -> Keyword("전기차충전소", categoryMustContain = "충전")
            // 화장실·휴게소는 카테고리 코드가 없다. [확인 필요] 도심에서 "휴게소" 분류 결과가 적다(시청 2km 안 1곳)
            PlaceKind.RESTROOM -> Keyword("공중화장실", categoryMustContain = "화장실")
            PlaceKind.REST_AREA -> Keyword("휴게소", categoryMustContain = "휴게소")
            PlaceKind.PARK -> Keyword("공원", categoryMustContain = "공원")
        }

        /** 카카오 좌표는 x = 경도, y = 위도다. GeoPoint(위도, 경도)와 순서가 반대라 여기서만 바꾼다. */
        fun url(query: KakaoPlaceQuery, center: GeoPoint, radiusMeters: Int): String {
            val parameters = buildList {
                when (query) {
                    is Category -> add("category_group_code" to query.groupCode)
                    is Keyword -> add("query" to query.text)
                }
                add("x" to coordinate(center.longitude))
                add("y" to coordinate(center.latitude))
                add("radius" to radiusMeters.coerceIn(0, MAX_RADIUS_METERS).toString())
                add("sort" to "distance")
                add("size" to PAGE_SIZE.toString())
            }
            val path = when (query) {
                is Category -> "category.json"
                is Keyword -> "keyword.json"
            }
            val queryString = parameters.joinToString("&") { (name, value) -> "$name=${encode(value)}" }
            return "$BASE_URL/$path?$queryString"
        }

        private fun coordinate(value: Double): String = String.format(Locale.US, "%.7f", value)

        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
