package com.example.mosim.feature.voice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.ui.theme.MosimTheme

/**
 * F-2 본문. Preview와 Compose 테스트가 이걸 직접 그린다.
 *
 * 생김새는 `VoiceLayout.kt`에 있다. STT 엔진·ViewModel·결과 콜백은 그대로 둔다.
 *
 * @param isMock 어떤 인식 구현이 도는지. **화면에 표시하지 않는다.**
 *   시연 화면에 개발용 표시가 나오면 안 된다는 팀 요청이라, 구분은 로그와 debug 진입점으로 한다.
 */
@Composable
fun VoiceContent(
    state: VoiceUiState,
    onBack: () -> Unit,
    isMock: Boolean = false,
    showDebugControls: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onCancel: () -> Unit = {},
    analysisMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    VoiceLayout(
        state = state,
        onBack = onBack,
        modifier = modifier,
        showDebugControls = showDebugControls,
        onStart = onStart,
        onStop = onStop,
        onCancel = onCancel,
        analysisMessage = analysisMessage,
    )
}

private const val PreviewWidth = 1700
private const val PreviewHeight = 1184

@Preview(name = "F-2 인식 준비", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun VoicePreparingPreview() {
    MosimTheme { VoiceContent(VoiceUiState(VoicePhase.Preparing), {}) }
}

@Preview(name = "F-2 음성 인식 중", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun VoiceListeningPreview() {
    MosimTheme {
        // 부분 인식 중. 문장은 아직 끝나지 않았다.
        VoiceContent(VoiceUiState(VoicePhase.Listening, "커피 마시고 싶은데"), {})
    }
}

@Preview(name = "F-2 인식 결과", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun VoiceCompletedPreview() {
    MosimTheme {
        VoiceContent(VoiceUiState(VoicePhase.Completed, "커피 마시고 싶은데 근처에 카페 있을까?"), {})
    }
}

@Preview(name = "F-2 AI 분석 중", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun VoiceAnalyzingPreview() {
    MosimTheme {
        VoiceContent(VoiceUiState(VoicePhase.Analyzing, "커피 마시고 싶은데 근처에 카페 있을까?"), {})
    }
}

@Preview(name = "F-2 오류", widthDp = PreviewWidth, heightDp = PreviewHeight)
@Composable
private fun VoiceErrorPreview() {
    MosimTheme {
        VoiceContent(VoiceUiState(VoicePhase.Error, error = SpeechError.Timeout), {})
    }
}
