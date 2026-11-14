package com.example.mosim

import android.app.Application
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk

/**
 * 카카오맵 SDK는 지도를 그리기 전에 한 번 초기화해야 하고, 문서가 Application에서 부르기를 권장한다.
 *
 * 초기화가 실패해도 앱을 죽이지 않는다. 지도만 못 그리는 일이라 그때는 자리 표시(MapPlaceholder)로 넘어간다.
 * 실제로 실패하는 경우가 있다: 카카오 벡터맵 AAR에는 arm64-v8a와 armeabi-v7a 네이티브 라이브러리만 들어 있어서
 * **x86_64 Pleos 에뮬레이터에서는 libK3fAndroid.so를 찾지 못한다**(2026-11-15 확인).
 */
class MosimApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) return
        mapReady = try {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
            true
        } catch (error: UnsatisfiedLinkError) {
            // 키를 로그에 남기지 않도록 예외 종류와 메시지만 적는다.
            Log.w(TAG, "카카오맵 네이티브 라이브러리를 적재하지 못했다(ABI 불일치): ${error.message}")
            false
        } catch (error: Exception) {
            Log.w(TAG, "카카오맵 초기화 실패: ${error.javaClass.simpleName} ${error.message}")
            false
        }
    }

    companion object {
        private const val TAG = "MosimApplication"

        @Volatile
        private var mapReady: Boolean = false

        /** 지도를 실제로 그릴 수 있는가. 키가 없거나 초기화에 실패하면 false다. */
        val isKakaoMapAvailable: Boolean get() = mapReady
    }
}
