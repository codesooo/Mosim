package com.example.mosim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.mosim.data.mock.MockWaypointSearchSource
import com.example.mosim.feature.voice.VoiceAnalysisResult
import com.example.mosim.navigation.MosimNavHost
import com.example.mosim.ui.theme.MosimTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosimTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // STT와 TTS는 실제 Pleos SDK를 쓴다(기본값). 아래 둘만 담당자 구현을 기다리는 자리다.
                    MosimNavHost(
                        // TODO(장소 검색 담당자): 구현이 나오면 이 자리를 바꾼다.
                        placeSearch = remember { MockWaypointSearchSource() },
                        // TODO(AI 의도 분석 담당자): 지금은 인식한 문장을 그대로 검색어로 넘긴다.
                        // 의도를 분류하지 않으므로 "경유지 추가"가 아닌 요청도 장소 검색으로 간다.
                        analyzeRequest = { text ->
                            delay(ANALYSIS_DELAY_MILLIS)
                            VoiceAnalysisResult.SearchPlaces(text)
                        },
                        // [확인 필요] 주행 중 조작 제한은 AAOS CarUxRestrictions로 판단해야 한다.
                        // 그 연동이 없어 정차 상태로 가정한다.
                        interactionAllowed = true,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isVisible = true
    }

    override fun onStop() {
        AppVisibility.isVisible = false
        super.onStop()
    }

    private companion object {
        /** 분석 중 상태가 눈에 보이도록 둔 대기 시간. 실제 분석 시간이 아니다. */
        const val ANALYSIS_DELAY_MILLIS = 700L
    }
}
