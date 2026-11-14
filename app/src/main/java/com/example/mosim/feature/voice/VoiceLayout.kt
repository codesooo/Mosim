package com.example.mosim.feature.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.InfoRow
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * F-2 화면의 생김새만 모아 둔 파일. STT 엔진과 상태 관리는 VoiceViewModel에 그대로 둔다.
 *
 * 레이아웃 원칙
 *  - 가운데 한 덩어리로 모은다. 운전 중에 훑어야 하므로 볼 것을 하나로 둔다.
 *  - 인식 문장이 들어갈 자리를 미리 비워 둔다. 글자가 생길 때 파형이 위로 튀지 않는다.
 *  - 파형은 **상태** 표시다. 음량 데이터가 없으므로 음량에 반응하는 척하지 않는다.
 *    듣는 중일 때만 정해진 주기로 움직이고, 나머지 상태에서는 멈춘다.
 */

/** 막대 기본 높이 비율. 가운데가 높은 대칭형으로 피그마 F-2 파형을 따랐다. */
private val BarFractions = listOf(0.28f, 0.52f, 0.78f, 1f, 0.78f, 0.52f, 0.28f)
/** 상태가 바뀔 때 막대가 자라는 시간과 막대별 지연. 잔물결처럼 번져 가게 한다. */
private const val BarTweenMillis = 260
private const val BarStaggerMillis = 45

/** 듣지 않을 때 막대 높이 비율. 낮게 깔아 않는다는 걸 보여 준다. */
private const val IdleBarScale = 0.42f
private val DotSize = 20.dp

@Composable
internal fun VoiceLayout(
    state: VoiceUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugControls: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onCancel: () -> Unit = {},
    analysisMessage: String? = null,
) {
    MosimAppContainer(
        title = "동승자 요청",
        subtitle = "이동 중 음성 요청",
        onBack = onBack,
        modifier = modifier,
        bottomActions = if (showDebugControls) {
            {
                if (state.active) {
                    PleosButton("취소", onCancel)
                    PleosButton("인식 중지", onStop, enabled = state.phase != VoicePhase.Stopping)
                } else {
                    PleosButton("다시 듣기", onStart, type = PleosButtonType.Filled)
                }
            }
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // 듣고 있는 쪽이 주인공이라 넓게 둔다(G-1의 600 : 980과 같은 생각).
            InfoCard(
                title = "음성 인식",
                modifier = Modifier.weight(980f).fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VoiceIndicator(state.phase)
                    Spacer(Modifier.height(MosimDimens.SectionGap))
                    Text(
                        // 인식 문장이 없는 동안에는 이 줄이 화면의 주인공이라 작게 쓰지 않는다.
                        text = state.statusLabel(analysisMessage),
                        style = PleosTypography.TitleSmall,
                        color = state.phase.statusColor(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(MosimDimens.CardGap))
                    Transcript(state)
                }
            }
            ExampleCard(modifier = Modifier.weight(600f).fillMaxHeight())
        }
    }
}

/**
 * 뭐라고 말해야 할지 모르면 아무 말도 못 한다. 고정 예시라 AI가 고른 문장이 아니다.
 * F-2는 문장을 그대로 검색어로 넘기므로 장소를 말하는 예시만 둔다.
 */
@Composable
private fun ExampleCard(modifier: Modifier = Modifier) {
    InfoCard(title = "이렇게 말해 보세요", modifier = modifier) {
        VoiceExamples.forEachIndexed { index, example ->
            InfoRow(
                label = "“$example”",
                value = "",
                showDivider = index != VoiceExamples.lastIndex,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val VoiceExamples = listOf(
    "커피 마시고 싶어",
    "화장실 가고 싶어",
    "잠깐 쉬었다 가자",
)

/**
 * 인식 문장 자리. 글자가 없어도 높이를 차지해 레이아웃이 튀지 않는다.
 * 부분 인식과 최종 인식 모두 같은 크기로 보여 준다. 운전 중에 읽어야 해서 작게 쓰지 않는다.
 */
@Composable
private fun Transcript(state: VoiceUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MosimDimens.VoiceTranscriptMinHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (state.text.isNotBlank()) {
            Text(
                text = "“${state.text}”",
                style = PleosTypography.HeadlineSmall.strong(),
                color = PleosColor.TextDarkPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 상태에 따라 파형(듣는 중)과 점 세 개(분석 중)를 바꿔 보여 준다. */
@Composable
private fun VoiceIndicator(phase: VoicePhase, modifier: Modifier = Modifier) {
    when (phase) {
        // 분석은 STT가 아니라 그다음 단계다. 모양을 달리해 구분한다.
        VoicePhase.Analyzing -> AnalyzingDots(modifier)
        else -> VoiceWaveform(phase, modifier)
    }
}

/**
 * 상태가 바뀔 때만 막대가 자라고 줄어든다.
 *
 * 계속 도는 애니메이션을 쓰지 않는 이유는 두 가지다.
 *  - 음량 데이터가 없는데 움직이면 음량에 반응하는 것처럼 보여 거짓말이 된다.
 *  - 끝나지 않는 애니메이션은 Compose가 영원히 idle이 아니게 만들어 기기 테스트가 멈춘다.
 *
 * 듣기 시작·종료 순간에 막대가 잔물결처럼 번져 상태가 바뀜 걸 알리고, 그 뒤엔 가만히 멈춘다.
 */
@Composable
private fun VoiceWaveform(phase: VoicePhase, modifier: Modifier = Modifier) {
    val listening = phase == VoicePhase.Listening || phase == VoicePhase.Stopping
    val tone = if (listening) PleosColor.TextDarkPrimary else PleosColor.TextDarkQuaternary

    Row(
        modifier = modifier.semantics { contentDescription = "음성 요청" },
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.VoiceBarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarFractions.forEachIndexed { index, base ->
            val fraction by animateFloatAsState(
                targetValue = if (listening) base else base * IdleBarScale,
                animationSpec = tween(
                    durationMillis = BarTweenMillis,
                    delayMillis = index * BarStaggerMillis,
                    easing = FastOutSlowInEasing,
                ),
                label = "voice-bar-$index",
            )
            Bar(fraction = fraction, tone = tone)
        }
    }
}

@Composable
private fun Bar(fraction: Float, tone: Color) {
    Box(
        Modifier
            .size(
                width = MosimDimens.VoiceBarWidth,
                height = MosimDimens.VoiceBarMaxHeight * fraction,
            )
            .background(tone, MosimShape.Pill),
    )
}

/** 분석 중 표시. 파형과 모양을 달리해 STT 단계와 구분한다. 역시 계속 도는 애니메이션은 쓰지 않는다. */
@Composable
private fun AnalyzingDots(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(MosimDimens.VoiceBarMaxHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.VoiceBarGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "요청 확인 중" },
        ) {
            repeat(3) { Dot() }
        }
    }
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(DotSize)
            .background(PleosColor.TextDarkPrimary, MosimShape.Pill),
    )
}

/** 화면에 띄울 상태 문구. 분석 단계는 외부 모듈이 준 [analysisMessage]를 먼저 쓴다. */
internal fun VoiceUiState.statusLabel(analysisMessage: String?): String = when (phase) {
    VoicePhase.Idle -> "음성 요청 대기 중"
    VoicePhase.Preparing -> "음성 인식 준비 중"
    VoicePhase.Listening -> "듣고 있어요"
    VoicePhase.Stopping -> "인식을 마무리하고 있어요"
    VoicePhase.Analyzing -> analysisMessage ?: "요청을 확인하고 있어요"
    VoicePhase.Completed -> "음성 인식 완료"
    VoicePhase.Error -> error?.message ?: SpeechError.Unknown.message
    VoicePhase.Cancelled -> "음성 인식을 취소했어요"
}

private fun VoicePhase.statusColor(): Color = when (this) {
    // 오류만 눈에 띄게 한다. 나머지는 문장이 주인공이라 차분하게 둔다.
    VoicePhase.Error -> PleosColor.TextDarkPrimary
    VoicePhase.Listening -> PleosColor.TextDarkSecondary
    else -> PleosColor.TextDarkTertiary
}
