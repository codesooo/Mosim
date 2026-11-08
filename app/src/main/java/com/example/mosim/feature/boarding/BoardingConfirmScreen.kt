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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.platform.pleos.BoardingSignals
import com.example.mosim.platform.pleos.DoorSignal
import com.example.mosim.platform.pleos.PickupSourceProvider
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
import kotlinx.coroutines.flow.StateFlow

/*
 * D-1 탑승 확인 (정차 중).
 * 차량 신호(도어·착석·벨트·정차)만으로는 누가 탔는지 알 수 없어서 운전자가 한 번 확인한 뒤 에스코트 모드로 넘긴다.
 * 신호 네 개가 다 모여야 "탑승 완료" 버튼이 켜진다. "다른 동승자"는 언제든 누를 수 있다.
 * 탑승 감지 오탐(좌석 위 짐 등) 처리는 미결이다(project-status.md 6장).
 */

data class BoardingConfirmUiState(
    val detectedAt: String,
    val question: String,
    val signals: BoardingSignals,
)

// Mock: 피그마 D-1 예시 값.
// 질문 문구는 이름 뒤 조사(가/이)가 달라져서 이름만 받지 않고 문장 전체를 받는다.
private const val MOCK_DETECTED_AT = "08:29"
private const val MOCK_QUESTION = "엄마가 탑승하셨나요?"

// 피그마 D-1 배치 값 (이 화면에서만 쓴다)
private const val SIGNAL_COLUMN_WEIGHT = 640f
private const val CONFIRM_COLUMN_WEIGHT = 940f
private val ConfirmCardItemGap = 16.dp

@Composable
fun BoardingConfirmScreen(
    onBack: () -> Unit,
    onSelectOtherPassenger: () -> Unit,
    onConfirmBoarding: () -> Unit,
    boardingSignals: StateFlow<BoardingSignals> = PickupSourceProvider.boardingSignals,
) {
    // C-2에서 시작한 탑승 신호를 이어서 본다(PickupSourceProvider.boardingSignals 설명 참고).
    // 실패하면 마지막으로 받은 신호가 그대로 남는다. 버튼은 꺼진 채로 남고 "다른 동승자"·Back은 쓸 수 있다.
    // [확인 필요] 신호 실패를 화면에 어떻게 보여 줄지 디자인이 없다.
    val signals by boardingSignals.collectAsState()

    BoardingConfirmContent(
        state = BoardingConfirmUiState(
            detectedAt = MOCK_DETECTED_AT,
            question = MOCK_QUESTION,
            signals = signals,
        ),
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
                signalRows(state.signals).forEach { row ->
                    InfoRow(
                        label = row.label,
                        value = "",
                        chipText = row.chipText,
                        chipTone = row.chipTone,
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
                    // 1700 기준 글자 자리 354dp, Asta Sans Bold 32 실측 321px라 잘리지 않는다(2026-11-08 확인).
                    PleosButton(
                        text = "탑승 완료 · 에스코트 시작",
                        onClick = onConfirmBoarding,
                        type = PleosButtonType.Filled,
                        enabled = state.signals.allDetected,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private data class SignalRow(
    val label: String,
    val chipText: String,
    val chipTone: ChipTone,
)

// 감지된 값의 문구("열림 → 닫힘", "감지", "착용", "정차")는 피그마 D-1을 따른다.
// [확인 필요] 아직 감지 전("대기"), 열림, 미착용, 주행 중 문구와 칩 색은 피그마에 없어서 임시로 정했다.
private const val WAITING = "대기"

private fun signalRows(signals: BoardingSignals): List<SignalRow> = listOf(
    when (signals.passengerDoor) {
        DoorSignal.ClosedAfterOpen -> SignalRow("조수석 도어", "열림 → 닫힘", ChipTone.Normal)
        DoorSignal.Open -> SignalRow("조수석 도어", "열림", ChipTone.Neutral)
        DoorSignal.Closed, null -> SignalRow("조수석 도어", WAITING, ChipTone.Neutral)
    },
    when (signals.passengerSeatOccupied) {
        true -> SignalRow("조수석 착석", "감지", ChipTone.Normal)
        false, null -> SignalRow("조수석 착석", WAITING, ChipTone.Neutral)
    },
    when (signals.passengerSeatBeltBuckled) {
        true -> SignalRow("안전벨트", "착용", ChipTone.Normal)
        false -> SignalRow("안전벨트", "미착용", ChipTone.Caution)
        null -> SignalRow("안전벨트", WAITING, ChipTone.Neutral)
    },
    when (signals.vehicleStopped) {
        true -> SignalRow("차량 상태", "정차", ChipTone.Normal)
        false -> SignalRow("차량 상태", "주행 중", ChipTone.Caution)
        null -> SignalRow("차량 상태", WAITING, ChipTone.Neutral)
    },
)

// Mock: 피그마 D-1처럼 신호가 다 모인 상태
private val MockAllDetectedSignals = BoardingSignals(
    passengerDoor = DoorSignal.ClosedAfterOpen,
    passengerSeatOccupied = true,
    passengerSeatBeltBuckled = true,
    vehicleStopped = true,
)

@Preview(name = "D-1 탑승 확인", widthDp = 1700, heightDp = 1184)
@Composable
private fun BoardingConfirmPreview() {
    MosimTheme {
        BoardingConfirmContent(
            state = BoardingConfirmUiState(MOCK_DETECTED_AT, MOCK_QUESTION, MockAllDetectedSignals),
            onBack = {},
            onSelectOtherPassenger = {},
            onConfirmBoarding = {},
        )
    }
}

@Preview(name = "D-1 신호 수신 중", widthDp = 1700, heightDp = 1184)
@Composable
private fun BoardingConfirmWaitingPreview() {
    MosimTheme {
        BoardingConfirmContent(
            // Mock: 도어가 열리고 착석까지 감지된 중간 상태
            state = BoardingConfirmUiState(
                detectedAt = MOCK_DETECTED_AT,
                question = MOCK_QUESTION,
                signals = BoardingSignals(
                    passengerDoor = DoorSignal.Open,
                    passengerSeatOccupied = true,
                    vehicleStopped = true,
                ),
            ),
            onBack = {},
            onSelectOtherPassenger = {},
            onConfirmBoarding = {},
        )
    }
}
