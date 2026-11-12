package com.example.mosim.data.remote.kakao

import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.platform.pleos.GeoPoint
import org.json.JSONException
import org.json.JSONObject

/**
 * 카카오 로컬 검색 응답(JSON)을 PlaceCandidate 목록으로 바꾼다.
 * 문서상 documents의 값은 전부 문자열이다. x/y/distance도 문자열이고, distance는 x/y를 보냈을 때만 채워진다.
 */
object KakaoPlaceParser {

    /** 응답 형식이 문서와 다르면 [JSONException]을 던진다. 호출하는 쪽이 실패로 바꾼다. */
    fun parse(body: String): List<PlaceCandidate> {
        val documents = JSONObject(body).getJSONArray("documents")
        return (0 until documents.length()).mapNotNull { index -> toCandidate(documents.getJSONObject(index)) }
    }

    /** 좌표가 없거나 숫자가 아니면 경유지로 넘길 수 없어 뺀다. */
    private fun toCandidate(document: JSONObject): PlaceCandidate? {
        val longitude = document.optString("x").toDoubleOrNull() ?: return null
        val latitude = document.optString("y").toDoubleOrNull() ?: return null
        return PlaceCandidate(
            id = document.getString("id"),
            name = document.getString("place_name"),
            categoryLabel = categoryLabel(document.optString("category_name")),
            address = document.optString("road_address_name").ifBlank { document.optString("address_name") }
                .ifBlank { null },
            location = GeoPoint(latitude = latitude, longitude = longitude),
            distanceMeters = document.optString("distance").toIntOrNull(),
        )
    }

    /** "음식점 > 카페 > 커피전문점"에서 가장 구체적인 마지막 단계만 보여 준다. */
    private fun categoryLabel(categoryName: String): String? =
        categoryName.split(">").map { it.trim() }.lastOrNull { it.isNotEmpty() }
}
