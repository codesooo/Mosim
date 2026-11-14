package com.example.mosim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mosim.navigation.MosimNavHost
import com.example.mosim.ui.theme.MosimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosimTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // STT·TTS·경로는 실제 Pleos SDK를 쓴다(기본값).
                    // 장소 검색과 의도 해석은 NavHost가 에스코트(EscortViewModel) 쪽 구현에 연결한다.
                    // 여기서 Mock을 넘기면 실제 카카오 검색과 VoiceCommandParser를 가린다.
                    MosimNavHost(
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

}
