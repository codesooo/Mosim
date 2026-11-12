package com.example.mosim.data.remote.kakao

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KakaoPlaceSearchSourceTest {

    private val center = GeoPoint(latitude = 37.5665, longitude = 126.978)

    // 카카오 문서 응답 예시와 같은 형식. 값은 테스트용이다.
    private val sampleBody = """
        {
          "meta": {"same_name": null, "pageable_count": 2, "total_count": 2, "is_end": true},
          "documents": [
            {
              "id": "100", "place_name": "카페 가", "category_name": "음식점 > 카페 > 커피전문점",
              "category_group_code": "CE7", "category_group_name": "카페", "phone": "",
              "address_name": "서울 중구 태평로1가 1", "road_address_name": "서울 중구 세종대로 110",
              "x": "126.9790", "y": "37.5670", "place_url": "", "distance": "418"
            },
            {
              "id": "200", "place_name": "좌표 없는 곳", "category_name": "", "category_group_code": "",
              "category_group_name": "", "phone": "", "address_name": "", "road_address_name": "",
              "x": "", "y": "", "place_url": "", "distance": ""
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `카테고리가 맞는 종류는 카테고리 검색을, 나머지는 키워드 검색을 쓴다`() {
        assertEquals(KakaoPlaceQuery.Category("CE7"), KakaoPlaceQuery.of(PlaceKind.CAFE))
        assertEquals(KakaoPlaceQuery.Category("CS2"), KakaoPlaceQuery.of(PlaceKind.CONVENIENCE_STORE))
        assertTrue(KakaoPlaceQuery.of(PlaceKind.REST_AREA) is KakaoPlaceQuery.Keyword)
        assertTrue(KakaoPlaceQuery.of(PlaceKind.RESTROOM) is KakaoPlaceQuery.Keyword)
        assertTrue(KakaoPlaceQuery.of(PlaceKind.EV_CHARGER) is KakaoPlaceQuery.Keyword)
    }

    @Test
    fun `URL은 x에 경도, y에 위도를 넣고 거리순으로 요청한다`() {
        val url = KakaoPlaceQuery.url(KakaoPlaceQuery.Category("CE7"), center, 2_000)

        assertEquals(
            "https://dapi.kakao.com/v2/local/search/category.json?category_group_code=CE7" +
                "&x=126.9780000&y=37.5665000&radius=2000&sort=distance&size=15",
            url,
        )
    }

    @Test
    fun `키워드는 인코딩하고 반경은 문서 최대값을 넘지 않는다`() {
        val url = KakaoPlaceQuery.url(KakaoPlaceQuery.Keyword("휴게소", "휴게소"), center, 50_000)

        assertTrue(url.startsWith("https://dapi.kakao.com/v2/local/search/keyword.json?query=%ED%9C%B4%EA%B2%8C%EC%86%8C&"))
        assertTrue(url.contains("radius=20000"))
    }

    @Test
    fun `응답을 장소로 바꾸고 좌표가 없는 문서는 뺀다`() {
        val places = KakaoPlaceParser.parse(sampleBody)

        assertEquals(1, places.size)
        val place = places.single()
        assertEquals("카페 가", place.name)
        assertEquals("커피전문점", place.categoryLabel)
        assertEquals("서울 중구 세종대로 110", place.address)
        assertEquals(GeoPoint(latitude = 37.5670, longitude = 126.9790), place.location)
        assertEquals(418, place.distanceMeters)
    }

    @Test
    fun `거리가 빈 문자열이면 0이 아니라 null이다`() {
        val body = sampleBody.replace("\"distance\": \"418\"", "\"distance\": \"\"")

        assertNull(KakaoPlaceParser.parse(body).single().distanceMeters)
    }

    @Test
    fun `성공하면 인증 헤더에 KakaoAK를 붙여 요청한다`() = runBlocking {
        var sentAuthorization = ""
        val source = KakaoPlaceSearchSource("test-key") { _, authorization ->
            sentAuthorization = authorization
            KakaoHttpResponse(200, sampleBody)
        }

        val result = source.searchNearby(PlaceKind.CAFE, center, 2_000)

        assertEquals("KakaoAK test-key", sentAuthorization)
        assertEquals(1, (result as SourceResult.Success).value.size)
    }

    @Test
    fun `키워드 검색은 분류가 맞지 않는 곳을 뺀다`() = runBlocking {
        // 실제 응답(2026-11-12)에서 "휴게소" 검색 1등이 분식집이었다.
        val body = sampleBody
            .replace("음식점 > 카페 > 커피전문점", "음식점 > 분식")
            .replace("\"x\": \"\", \"y\": \"\"", "\"x\": \"126.97\", \"y\": \"37.55\"")
            .replace("\"category_name\": \"\", \"category_group_code\": \"\"", "\"category_name\": \"교통,수송 > 휴게소\", \"category_group_code\": \"\"")
        val source = KakaoPlaceSearchSource("test-key") { _, _ -> KakaoHttpResponse(200, body) }

        val places = (source.searchNearby(PlaceKind.REST_AREA, center, 2_000) as SourceResult.Success).value

        assertEquals(listOf("좌표 없는 곳"), places.map { it.name })
    }

    @Test
    fun `결과가 없으면 실패가 아니라 빈 목록이다`() = runBlocking {
        val empty = """{"meta": {"total_count": 0, "pageable_count": 0, "is_end": true}, "documents": []}"""
        val source = KakaoPlaceSearchSource("test-key") { _, _ -> KakaoHttpResponse(200, empty) }

        assertEquals(SourceResult.Success(emptyList<Any>()), source.searchNearby(PlaceKind.CAFE, center, 2_000))
    }

    @Test
    fun `키가 없으면 요청하지 않고 권한 실패로 돌려준다`() = runBlocking {
        var requested = false
        val source = KakaoPlaceSearchSource("") { _, _ ->
            requested = true
            KakaoHttpResponse(200, sampleBody)
        }

        assertEquals(FailureReason.PERMISSION_DENIED, failureReason(source))
        assertFalse(requested)
    }

    @Test
    fun `응답 코드와 연결 오류를 실패 이유로 바꾼다`() = runBlocking {
        fun sourceReturning(statusCode: Int) = KakaoPlaceSearchSource("test-key") { _, _ ->
            KakaoHttpResponse(statusCode, null)
        }

        assertEquals(FailureReason.PERMISSION_DENIED, failureReason(sourceReturning(401)))
        assertEquals(FailureReason.PERMISSION_DENIED, failureReason(sourceReturning(403)))
        assertEquals(FailureReason.INVALID_REQUEST, failureReason(sourceReturning(400)))
        assertEquals(FailureReason.UNKNOWN, failureReason(sourceReturning(429)))
        assertEquals(FailureReason.CONNECTION_FAILED, failureReason(sourceReturning(503)))
        assertEquals(
            FailureReason.NETWORK,
            failureReason(KakaoPlaceSearchSource("test-key") { _, _ -> throw IOException("offline") }),
        )
    }

    @Test
    fun `응답 형식이 다르면 예외 대신 실패로 돌려준다`() = runBlocking {
        val source = KakaoPlaceSearchSource("test-key") { _, _ -> KakaoHttpResponse(200, "{\"meta\": {}}") }

        assertEquals(FailureReason.UNKNOWN, failureReason(source))
    }

    private suspend fun failureReason(source: KakaoPlaceSearchSource): FailureReason =
        (source.searchNearby(PlaceKind.CAFE, center, 2_000) as SourceResult.Failure).reason
}
