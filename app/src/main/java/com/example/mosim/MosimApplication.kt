package com.example.mosim

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

/**
 * 카카오맵 SDK는 지도를 그리기 전에 한 번 초기화해야 하고, 문서가 Application에서 부르기를 권장한다.
 * 키가 없으면 초기화하지 않는다. 그때 지도 자리는 MapPlaceholder로 그린다(MosimMap).
 */
class MosimApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (isKakaoMapAvailable) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }

    companion object {
        val isKakaoMapAvailable: Boolean get() = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()
    }
}
