package com.example.mosim.data.remote.kakao

import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.data.place.PlaceKind
import com.example.mosim.data.place.PlaceSearchSource
import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 응답 코드와 본문. 실패 응답 본문은 형식이 문서에 없어 읽지 않는다. */
data class KakaoHttpResponse(val statusCode: Int, val body: String?)

/** 네트워크 호출만 떼어 둔 자리다. 테스트에서 가짜 응답을 넣으려고 둔다. */
fun interface KakaoHttpClient {
    /** 연결 실패는 [IOException]을 던진다. */
    suspend fun get(url: String, authorization: String): KakaoHttpResponse
}

/**
 * 카카오 로컬 REST로 근처 장소를 찾는다.
 * 키(REST API 키)는 local.properties → BuildConfig로 받는다. 코드와 로그에 키를 남기지 않는다.
 */
class KakaoPlaceSearchSource(
    private val restApiKey: String,
    private val httpClient: KakaoHttpClient = UrlConnectionKakaoHttpClient(),
) : PlaceSearchSource {

    override suspend fun searchNearby(
        kind: PlaceKind,
        center: GeoPoint,
        radiusMeters: Int,
    ): SourceResult<List<PlaceCandidate>> {
        if (restApiKey.isBlank()) {
            return SourceResult.Failure(FailureReason.PERMISSION_DENIED, "카카오 REST API 키가 없다")
        }
        val query = KakaoPlaceQuery.of(kind)
        val url = KakaoPlaceQuery.url(query, center, radiusMeters)
        val response = try {
            httpClient.get(url, authorization = "KakaoAK $restApiKey")
        } catch (exception: IOException) {
            return SourceResult.Failure(FailureReason.NETWORK, exception.message)
        }
        return toResult(query, response)
    }

    private fun toResult(query: KakaoPlaceQuery, response: KakaoHttpResponse): SourceResult<List<PlaceCandidate>> {
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            return SourceResult.Failure(failureReason(response.statusCode), "카카오 응답 ${response.statusCode}")
        }
        val body = response.body ?: return SourceResult.Failure(FailureReason.UNKNOWN, "카카오 응답 본문이 없다")
        return try {
            // 카카오가 거리순으로 준다. 같은 거리 기준을 쓰도록 다시 정렬하지 않는다.
            val places = KakaoPlaceParser.parse(body)
            SourceResult.Success(if (query is KakaoPlaceQuery.Keyword) places.filter { query.accepts(it.categoryLabel) } else places)
        } catch (exception: JSONException) {
            SourceResult.Failure(FailureReason.UNKNOWN, "카카오 응답 형식이 다르다: ${exception.message}")
        }
    }

    /**
     * 카카오 오류 코드 문서 기준: 401 앱 키 오류, 403 권한·기능 미설정, 400 인자 오류나 쿼터 초과(-10),
     * 429 무료 제공량 소진(FAQ). 쿼터 초과를 따로 구분할 이유가 아직 없어 400은 INVALID_REQUEST로 둔다.
     */
    private fun failureReason(statusCode: Int): FailureReason = when (statusCode) {
        HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> FailureReason.PERMISSION_DENIED
        HttpURLConnection.HTTP_BAD_REQUEST -> FailureReason.INVALID_REQUEST
        TOO_MANY_REQUESTS -> FailureReason.UNKNOWN
        in 500..599 -> FailureReason.CONNECTION_FAILED
        else -> FailureReason.UNKNOWN
    }

    private companion object {
        const val TOO_MANY_REQUESTS = 429
    }
}

/** 라이브러리를 추가하지 않으려고 기본 HttpURLConnection을 쓴다. */
class UrlConnectionKakaoHttpClient(
    private val timeoutMillis: Int = 5_000,
) : KakaoHttpClient {

    override suspend fun get(url: String, authorization: String): KakaoHttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Authorization", authorization)
            val statusCode = connection.responseCode
            val body = if (statusCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
            KakaoHttpResponse(statusCode, body)
        } finally {
            connection.disconnect()
        }
    }
}
