package com.example.mosim.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mosim.MosimApplication
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.PleosColor
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

/** 지도에 찍을 점. 화면 코드는 카카오 타입 대신 이것만 넘긴다. */
data class MapMarker(val location: GeoPoint, val kind: MapMarkerKind)

enum class MapMarkerKind { CURRENT_LOCATION, PLACE }

/** 점 개수에 따라 카메라를 어떻게 둘지. SDK 호출과 떼어 단위 테스트한다. */
sealed interface MapCamera {
    data object Default : MapCamera
    data class Center(val point: GeoPoint) : MapCamera
    data class Fit(val points: List<GeoPoint>) : MapCamera

    companion object {
        /**
         * 점을 맞출 때 가장자리에서 띄울 거리. 지도의 짧은 변의 1/4이다.
         * 고정값(220dp)을 쓰면 좁은 창의 낮은 지도(약 250dp)에서 여백이 지도를 다 먹어 점이 안 보였다.
         */
        fun fitPaddingPx(widthPx: Int, heightPx: Int): Int = minOf(widthPx, heightPx) / FIT_PADDING_DIVISOR

        fun of(markers: List<MapMarker>): MapCamera = when (markers.size) {
            0 -> Default
            1 -> Center(markers.single().location)
            else -> Fit(markers.map { it.location })
        }
    }
}

/**
 * 카카오맵을 Compose에서 쓰는 래퍼다.
 * 키가 없거나 Preview이거나 지도 오류가 나면 [MapPlaceholder]를 대신 그린다.
 *
 * MapView는 Activity가 끝날 때 스스로 정리되지만, 화면만 바뀔 때는 정리되지 않는다.
 * 그래서 setFinishManually(true)로 두고 이 Composable이 사라질 때 finish()를 부른다.
 */
@Composable
fun MosimMap(
    markers: List<MapMarker>,
    placeholderText: String,
    modifier: Modifier = Modifier,
) {
    var hasError by remember { mutableStateOf(false) }
    if (LocalInspectionMode.current || !MosimApplication.isKakaoMapAvailable || hasError) {
        MapPlaceholder(
            text = if (hasError) MAP_ERROR_TEXT else placeholderText,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    // 점이 바뀌면 지도를 새로 만든다. F-1은 제안 하나에 점이 고정이라 이 방식이 가장 단순하다.
    key(markers) {
        val mapView = remember { MapView(context) }
        // onMapReady는 배치가 끝난 뒤 불리므로 그때는 아래에서 잰 값이 들어 있다.
        var fitPaddingPx by remember { mutableIntStateOf(0) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(mapView, lifecycleOwner) {
            mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit

                    override fun onMapError(error: Exception) {
                        // 키를 남기지 않도록 예외 종류와 메시지만 남긴다.
                        Log.w(LOG_TAG, "카카오맵 오류: ${error.javaClass.simpleName} ${error.message}")
                        hasError = true
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        drawMarkers(kakaoMap, markers)
                        moveCamera(kakaoMap, MapCamera.of(markers), fitPaddingPx)
                    }
                },
            )
            mapView.setFinishManually(true)
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.resume()
                    Lifecycle.Event.ON_PAUSE -> mapView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.finish()
            }
        }
        BoxWithConstraints(modifier = modifier.clip(MosimShape.Card)) {
            // 지도 크기는 배치(넓은 창·좁은 창)마다 다르다. SDK가 px를 받아 제약의 px 값을 그대로 쓴다.
            fitPaddingPx = MapCamera.fitPaddingPx(constraints.maxWidth, constraints.maxHeight)
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun drawMarkers(kakaoMap: KakaoMap, markers: List<MapMarker>) {
    val labelManager = kakaoMap.labelManager ?: return
    val layer = labelManager.layer ?: return
    markers.forEach { marker ->
        val styles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(markerBitmap(marker.kind))))
        layer.addLabel(LabelOptions.from(marker.location.toLatLng()).setStyles(styles))
    }
}

private fun moveCamera(kakaoMap: KakaoMap, camera: MapCamera, fitPaddingPx: Int) {
    when (camera) {
        MapCamera.Default -> Unit
        is MapCamera.Center -> kakaoMap.moveCamera(
            CameraUpdateFactory.newCenterPosition(camera.point.toLatLng(), SINGLE_POINT_ZOOM_LEVEL),
        )
        is MapCamera.Fit -> kakaoMap.moveCamera(
            CameraUpdateFactory.fitMapPoints(camera.points.map { it.toLatLng() }.toTypedArray(), fitPaddingPx),
        )
    }
}

private fun GeoPoint.toLatLng(): LatLng = LatLng.from(latitude, longitude)

/** 마커 이미지 파일을 따로 두지 않으려고 테두리 있는 원을 직접 그린다. */
private fun markerBitmap(kind: MapMarkerKind): Bitmap {
    val bitmap = Bitmap.createBitmap(MARKER_SIZE_PX, MARKER_SIZE_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = MARKER_SIZE_PX / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, center, paint)
    paint.color = when (kind) {
        MapMarkerKind.CURRENT_LOCATION -> PleosColor.ButtonFilledEnabled.toArgb()
        MapMarkerKind.PLACE -> MosimColor.CautionAccent.toArgb()
    }
    canvas.drawCircle(center, center, center - MARKER_BORDER_PX, paint)
    return bitmap
}

private const val LOG_TAG = "MosimMap"
private const val MAP_ERROR_TEXT = "지도를 불러오지 못했어요"

/** [확인 필요] 화면에서 보고 정한 값이다. */
private const val SINGLE_POINT_ZOOM_LEVEL = 15
/** 점이 지도 가장자리와 마커 옆 지명에 붙지 않을 만큼. 넓은 창 지도(약 900dp)에서 기존 220dp와 비슷하다. */
private const val FIT_PADDING_DIVISOR = 4
private const val MARKER_SIZE_PX = 48
private const val MARKER_BORDER_PX = 8
