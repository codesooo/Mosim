package com.example.mosim.feature.boarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.ChipSize
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.InfoRow
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * D-1 탑승 확인 (정차 중).
 * 차량 신호(도어·착석·벨트·정차)만으로는 누가 탔는지 알 수 없어서 운전자가 한 번 확인한 뒤 에스코트 모드로 넘긴다.
 * 탑승 감지 오탐(좌석 위 짐 등) 처리는 미결이다(project-status.md 6장).
 */

data class BoardingConfirmUiState(
    val detectedAt: String,
    val question: String,
    val signals: List<BoardingSignal>,
)

data class BoardingSignal(
    val label: String,
    val state: String,
)

// Mock: 피그마 D-1 예시 값.
// 질문 문구는 이름 뒤 조사(가/이)가 달라져서 이름만 받지 않고 문장 전체를 받는다.
private val MockBoardingConfirmState = BoardingConfirmUiState(
    detectedAt = "08:29",
    question = "엄마가 탑승하셨나요?",
    signals = listOf(
        BoardingSignal("조수석 도어", "열림 → 닫힘"),
        BoardingSignal("조수석 착석", "감지"),
        BoardingSignal("안전벨트", "착용"),
        BoardingSignal("차량 상태", "정차"),
    ),
)

// 피그마 D-1 배치 값 (이 화면에서만 쓴다)
private const val SIGNAL_COLUMN_WEIGHT = 640f
private const val CONFIRM_COLUMN_WEIGHT = 940f
private val ConfirmCardItemGap = 16.dp

@Composable
fun BoardingConfirmScreen(
    onBack: () -> Unit,
    onSelectOtherPassenger: () -> Unit,
    onConfirmBoarding: () -> Unit,
) {
    BoardingConfirmContent(
        state = MockBoardingConfirmState,
        onBack = onBack,
        onSelectOtherPassenger = onSelectOtherPassenger,
        onConfirmBoarding = onConfirmBoarding,
    )
}

@Composable
fun BoardingConfirmContent(
    state: BoardingConfirmUiState,
    onBack: () -> Unit,
    onSelectOtherPassenger: () -> Unit,
    onConfirmBoarding: () -> Unit,
) {
    MosimAppContainer(
        title = "탑승 확인",
        subtitle = "차량 신호 감지 · ${state.detectedAt}",
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            InfoCard(
                title = "감지된 신호",
                modifier = Modifier
                    .weight(SIGNAL_COLUMN_WEIGHT)
                    .fillMaxHeight(),
            ) {
                state.signals.forEach { signal ->
                    InfoRow(
                        label = signal.label,
                        value = "",
                        chipText = signal.state,
                        chipTone = ChipTone.Normal,
                        chipSize = ChipSize.Small,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "차량 신호만으로는 누가 탔는지 확정할 수 없어 운전자 확인 후 전환합니다",
                    style = MosimTypography.Caption,
                    color = MosimColor.TextHint,
                )
            }

            Column(
                modifier = Modifier
                    .weight(CONFIRM_COLUMN_WEIGHT)
                    .fillMaxHeight()
                    .background(PleosColor.SurfaceBasic, MosimShape.Card)
                    .padding(MosimDimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(ConfirmCardItemGap),
            ) {
                Text(text = state.question, style = PleosTypography.HeadlineMedium.strong())
                Text(
                    text = "확인하면 에스코트 모드로 전환하고 운전 중 조작을 최소화합니다",
                    style = PleosTypography.BodySmall,
                    color = PleosColor.TextDarkTertiary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                ) {
                    PleosButton(
                        text = "다른 동승자",
                        onClick = onSelectOtherPassenger,
                        modifier = Modifier.weight(1f),
                    )
                    PleosButton(
                        text = "탑승 완료 · 에스코트 시작",
                        onClick = onConfirmBoarding,
                        type = PleosButtonType.Filled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview(name = "D-1 탑승 확인", widthDp = 1700, heightDp = 1184)
@Composable
private fun BoardingConfirmPreview() {
    MosimTheme {
        BoardingConfirmContent(
            state = MockBoardingConfirmState,
            onBack = {},
            onSelectOtherPassenger = {},
            onConfirmBoarding = {},
        )
    }
}
