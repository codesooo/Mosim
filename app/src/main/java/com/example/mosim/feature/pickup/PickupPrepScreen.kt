package com.example.mosim.feature.pickup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.ChipSize
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.InfoRow
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong
import kotlinx.coroutines.delay

/*
 * C-2 엄마 맞이 준비 (픽업지 정차 중).
 * 저장된 개인화 값을 차량에 적용한 결과를 보여 준다. 다음 화면(D-1)으로는 탑승 신호 감지로 넘어간다.
 * 설정 온도는 저장 당시 외기와 쌍으로 비교한다(project-status.md 개인화 저장 규칙).
 */

data class PickupPrepUiState(
    val passengerName: String,
    val stopConfirmedAt: String,
    val savedCondition: TempCondition,
    val currentCondition: TempCondition,
    val appliedTemp: String,
    val tempNote: String,
    val prepItems: List<PrepItem>,
    val doneCount: Int,
    val totalCount: Int,
    val waitingText: String,
)

data class TempCondition(
    val label: String,
    val value: String,
)

data class PrepItem(
    val label: String,
    val value: String,
    val chipText: String,
)

// Mock: 피그마 C-2 예시 값
private val MockPickupPrepState = PickupPrepUiState(
    passengerName = "엄마",
    stopConfirmedAt = "08:27",
    savedCondition = TempCondition("저장 당시", "외부 5℃ · 설정 24℃"),
    currentCondition = TempCondition("현재", "외부 7℃ · 비"),
    appliedTemp = "24℃",
    tempNote = "저장 당시와 현재 외부 기온 차이가 작아 저장값을 그대로 적용",
    prepItems = listOf(
        PrepItem("공조", "24℃ · 풍량 2", "적용"),
        PrepItem("좌석", "조수석 · 110° · 열선 1단", "적용"),
        PrepItem("음악", "조용한 음악 · 볼륨 4", "적용"),
        PrepItem("조명", "웰컴 (지원 차량)", "적용"),
    ),
    doneCount = 4,
    totalCount = 4,
    waitingText = "탑승 대기 중",
)

// Mock: 실제로는 도어·착석·벨트 신호로 탑승을 감지한다. 지금은 시연용으로 일정 시간 뒤 넘긴다.
private const val MOCK_BOARDING_DELAY_MS = 5_000L

// 피그마 C-2 배치 값 (이 화면에서만 쓴다)
private const val TEMP_COLUMN_WEIGHT = 620f
private const val PREP_COLUMN_WEIGHT = 536f
private const val STATUS_COLUMN_WEIGHT = 400f
private val TempCardItemGap = 24.dp
private val CompareBoxGap = 16.dp
private val CompareBoxPadding = 28.dp
private val CompareBoxItemGap = 10.dp
private val HighlightBoxPaddingHorizontal = 32.dp
private val HighlightBoxPaddingVertical = 28.dp
private val StatusCardItemGap = 12.dp
private val WaitingDotSize = 20.dp
private val WaitingDotTextGap = 16.dp

@Composable
fun PickupPrepScreen(
    onBack: () -> Unit,
    onBoardingDetected: () -> Unit,
    onAdjustSettings: () -> Unit,
) {
    val currentOnBoardingDetected by rememberUpdatedState(onBoardingDetected)
    LaunchedEffect(Unit) {
        delay(MOCK_BOARDING_DELAY_MS)
        currentOnBoardingDetected()
    }
    PickupPrepContent(state = MockPickupPrepState, onBack = onBack, onAdjustSettings = onAdjustSettings)
}

@Composable
fun PickupPrepContent(
    state: PickupPrepUiState,
    onBack: () -> Unit,
    onAdjustSettings: () -> Unit,
) {
    MosimAppContainer(
        title = "${state.passengerName} 맞이 준비",
        subtitle = "픽업지 도착 · 정차 확인 ${state.stopConfirmedAt}",
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            InfoCard(
                title = "실내 온도",
                modifier = Modifier
                    .weight(TEMP_COLUMN_WEIGHT)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.padding(top = TempCardItemGap),
                    verticalArrangement = Arrangement.spacedBy(TempCardItemGap),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CompareBoxGap)) {
                        TempCompareBox(state.savedCondition, Modifier.weight(1f))
                        TempCompareBox(state.currentCondition, Modifier.weight(1f))
                    }
                    AppliedTempBox(temp = state.appliedTemp)
                    Text(text = state.tempNote, style = MosimTypography.Caption, color = MosimColor.TextHint)
                }
            }

            InfoCard(
                title = "준비 항목",
                modifier = Modifier
                    .weight(PREP_COLUMN_WEIGHT)
                    .fillMaxHeight(),
            ) {
                state.prepItems.forEach { item ->
                    InfoRow(
                        label = item.label,
                        value = item.value,
                        chipText = item.chipText,
                        chipTone = ChipTone.Normal,
                        valueStyle = PleosTypography.BodyMedium.strong(),
                        chipSize = ChipSize.Small,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(STATUS_COLUMN_WEIGHT)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                PrepStatusCard(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                PleosButton(text = "설정 조정", onClick = onAdjustSettings, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** "저장 당시" / "현재" 회색 비교 박스 */
@Composable
private fun TempCompareBox(
    condition: TempCondition,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(PleosColor.BackgroundPrimary, MosimShape.Field)
            .padding(CompareBoxPadding),
        verticalArrangement = Arrangement.spacedBy(CompareBoxItemGap),
    ) {
        Text(text = condition.label, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
        Text(text = condition.value, style = PleosTypography.LabelMedium.strong())
    }
}

/** 진한 "적용 온도 24℃" 박스 */
@Composable
private fun AppliedTempBox(
    temp: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.ButtonFilledEnabled, MosimShape.Field)
            .padding(horizontal = HighlightBoxPaddingHorizontal, vertical = HighlightBoxPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "적용 온도", style = MosimTypography.HighlightLabel, color = PleosColor.TextLightPrimary)
        Text(text = temp, style = MosimTypography.HighlightValue, color = PleosColor.TextLightPrimary, maxLines = 1)
    }
}

/** 오른쪽 위 "준비 상태 4 / 4 완료" 카드 */
@Composable
private fun PrepStatusCard(
    state: PickupPrepUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(StatusCardItemGap),
    ) {
        Text(text = "준비 상태", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Text(text = "${state.doneCount} / ${state.totalCount} 완료", style = MosimTypography.BannerTitle, maxLines = 1)
        Spacer(modifier = Modifier.weight(1f))
        LightStatusIndicator(text = state.waitingText)
    }
}

/** 밝은 배경용 점 + 문구. StatusPill은 진한 배경 전용이라 따로 만든다. */
@Composable
private fun LightStatusIndicator(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WaitingDotTextGap),
    ) {
        Box(
            modifier = Modifier
                .size(WaitingDotSize)
                .background(MosimColor.NormalAccent, MosimShape.Pill),
        )
        Text(
            text = text,
            style = PleosTypography.BodyMedium.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "C-2 엄마 맞이 준비", widthDp = 1700, heightDp = 1184)
@Composable
private fun PickupPrepPreview() {
    MosimTheme {
        PickupPrepContent(state = MockPickupPrepState, onBack = {}, onAdjustSettings = {})
    }
}
