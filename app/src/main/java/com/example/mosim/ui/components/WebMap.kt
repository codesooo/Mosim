package com.example.mosim.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.PleosColor

/**
 * 네이티브 지도를 쓸 수 없는 기기에서 대신 그리는 지도다.
 *
 * 왜 필요한가: 카카오 벡터맵 AAR에는 arm 네이티브 라이브러리만 들어 있어 x86_64 Pleos 에뮬레이터에서는
 * 지도 엔진이 적재되지 않는다(2026-11-15 확인). WebView는 시스템 크로미움을 쓰므로 x86_64에서도 돈다.
 *
 * 지도 타일은 OpenStreetMap, 지도 라이브러리는 Leaflet을 **실행 시점에 CDN에서** 받는다.
 * Gradle 의존성을 추가하지 않는다. 네트워크가 없으면 빈 회색 판으로 남는다.
 * [확인 필요] OSM 타일 사용 정책과 표기는 팀에서 확인한다. Leaflet이 지도 위에 저작자 표시를 붙인다.
 *
 * 실제 차량과 폰(arm)에서는 이 화면이 아니라 카카오 네이티브 지도가 그려진다.
 */
@Composable
fun WebMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    focus: GeoPoint? = null,
) {
    val context = LocalContext.current
    // 점이나 중심이 바뀌면 페이지를 다시 만든다. 점이 몇 개뿐이라 이 방식이 가장 단순하다.
    key(markers, focus) {
        val webView = remember {
            @SuppressLint("SetJavaScriptEnabled")
            WebView(context).apply {
                // 부모 크기를 채우라고 명시한다. 이게 없으면 WebView 뷰포트 높이가 0이 되어
                // CSS의 height:100%가 0으로 계산되고 지도가 그려지지 않는다(에뮬레이터에서 확인).
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // 지도만 그리면 되므로 파일 접근은 열지 않는다.
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(PleosColor.SurfaceHigh.toArgb())
                // 지도가 비어 보일 때 원인을 찾을 수 있게 로그만 남긴다. 화면에는 아무것도 띄우지 않는다.
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        Log.i(LOG_TAG, "console: ${message.message()} (${message.lineNumber()})")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        Log.w(LOG_TAG, "로드 실패 ${request.url}: ${error.errorCode} ${error.description}")
                    }
                }
            }
        }
        DisposableEffect(webView) {
            // 로컬 기준 주소로 올린다. 저장소에 HTML 파일을 두지 않기 위해서다.
            webView.loadDataWithBaseURL(BASE_URL, mapHtml(markers, focus), "text/html", "utf-8", null)
            onDispose {
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
        AndroidView(
            factory = { webView },
            modifier = modifier.clip(MosimShape.Card).fillMaxSize(),
        )
    }
}

/** 점과 중심을 넣은 한 장짜리 지도 페이지. 값은 숫자와 고정 문자열뿐이라 따로 이스케이프하지 않는다. */
private fun mapHtml(markers: List<MapMarker>, focus: GeoPoint?): String {
    val points = markers.joinToString(",") { marker ->
        val kind = if (marker.kind == MapMarkerKind.CURRENT_LOCATION) "me" else "place"
        """{"lat":${marker.location.latitude},"lng":${marker.location.longitude},"kind":"$kind"}"""
    }
    val center = focus?.let { """{"lat":${it.latitude},"lng":${it.longitude}}""" } ?: "null"
    return """
<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<style>
  html, body { height: 100%; margin: 0; background: #EDEEF2; }
  /* 뷰포트 높이에 기대지 않고 화면을 꽉 채운다. */
  #map { position: fixed; inset: 0; background: #EDEEF2; }
  .leaflet-control-attribution { font-size: 10px; }
</style>
</head>
<body>
<div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
  var points = [$points];
  var focus = $center;
  var map = L.map('map', { zoomControl: false, attributionControl: true });
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap'
  }).addTo(map);

  points.forEach(function (p) {
    L.circleMarker([p.lat, p.lng], {
      radius: p.kind === 'me' ? 9 : 11,
      color: '#FFFFFF',
      weight: 3,
      fillColor: p.kind === 'me' ? '#1E88E5' : '#2B2B2E',
      fillOpacity: 1
    }).addTo(map);
  });

  if (focus) {
    map.setView([focus.lat, focus.lng], $FOCUS_ZOOM);
  } else if (points.length === 1) {
    map.setView([points[0].lat, points[0].lng], $FOCUS_ZOOM);
  } else if (points.length > 1) {
    map.fitBounds(L.latLngBounds(points.map(function (p) { return [p.lat, p.lng]; })).pad(0.3));
  } else {
    map.setView([$DEFAULT_LAT, $DEFAULT_LNG], $DEFAULT_ZOOM);
  }
</script>
</body>
</html>
    """.trimIndent()
}

/** 점 하나를 가운데 둘 때의 확대 수준. 카카오 네이티브 지도의 단일 점 확대와 비슷하게 맞췄다. */
private const val FOCUS_ZOOM = 15

// 점이 하나도 없을 때 보여 줄 기본 화면(서울 도심). 실제 위치가 아니다.
private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780
private const val DEFAULT_ZOOM = 12

private const val BASE_URL = "https://localhost/"

private const val LOG_TAG = "MosimWebMap"
