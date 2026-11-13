package com.example.mosim.feature.escort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.ChipSize
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * B-3 에스코트 계획 (design/screen-B3.png). B-1 "에스코트 계획 보기", B-2 "건너뛰기"·"경유지 추가"에서 진입한다.
 *
 * 화면에 보이는 값은 전부 아래 Mock 상수다. 계획 생성, 시각 계산, 운전자 승인 처리, 자동 처리 실행은 하지 않는다.
 * 계획 데이터 출처와 생성 방식이 요구사항으로 정해지면 Mock을 교체한다. [확인 필요]
 */

/** 단계별 계획 한 줄. 왼쪽 시각 + 번호 원 + 제목 + 설명. */
private data class PlanStep(val time: String, val title: String, val description: String)

// Mock: screen-B3 design example values.
// Actual escort-plan calculation is not defined yet.
private const val MockSubtitle = "AI가 엄마를 모시는 계획을 세웠습니다"

// Mock: screen-B3 충전 단계 기본 충전소. B-2에서 고른 충전소가 없을 때 쓴다.
// Mock fallback: no charging station was selected in B-2 (B-1 직접 진입, B-2 건너뛰기).
// Actual skip-flow behavior is not defined yet. 건너뛰기 때 충전 단계를 뺄지는 정해지지 않아 디자인 값을 그대로 둔다. [확인 필요]
private const val MockDefaultStationName = "EV 스테이션 강남"

// Mock: screen-B3 충전 단계의 충전 시간·잔량. 충전소별 값이 없고 계산하지 않아 어느 충전소든 같은 값을 쓴다. [확인 필요]
private const val MockChargeDetail = "20분 · 잔량 45%"

// Mock: screen-B3 단계별 계획. 시각은 계산하지 않고 디자인 값을 그대로 쓴다.
// 충전 단계의 충전소 이름만 B-2 선택값으로 바뀐다.
private fun mockSteps(chargingStationName: String?) = listOf(
    PlanStep("07:55", "충전", "${chargingStationName ?: MockDefaultStationName} · $MockChargeDetail"),
    PlanStep("08:17", "사전 공조", "픽업 10분 전 · 비 예보로 앞당김"),
    PlanStep("08:27", "픽업", "강남역 4번 출구 (지붕 있는 출구)"),
    PlanStep("08:27", "실내 준비", "정차 후 좌석 · 음악 · 온도 24°C"),
    PlanStep("09:12", "도착", "서울대병원 본관"),
)

// Mock: screen-B3 운전자 승인 항목. 승인 상태를 저장하거나 바꾸는 기능은 없다.
private val MockApprovals = listOf("충전소 경유", "픽업 출구 변경")
private const val MockApprovedChip = "승인됨"

// Mock: screen-B3 자동 처리 항목. 실제 차량 제어를 하지 않는다(Vehicle Control SDK 미제공).
private val MockAutoActions = listOf("공조 24°C", "좌석 조수석 110°", "음악 조용한 음악 · 볼륨 4")

/**
 * chargingStationName: B-2 "경유지 추가"로 고른 충전소 이름. B-1 직접 진입, B-2 건너뛰기는 null이다.
 * 안내 시작 이후 화면과 안내 기능은 정해지지 않아 onStartGuide 콜백만 둔다. [확인 필요]
 */
@Composable
fun EscortPlanScreen(
    chargingStationName: String?,
    onBack: () -> Unit,
    onStartGuide: () -> Unit,
) {
    MosimAppContainer(
        title = "에스코트 계획",
        subtitle = MockSubtitle,
        onBack = onBack,
    ) {
        val steps = mockSteps(chargingStationName)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // 열 너비 비율은 screen-B3에서 잰 값(약 1020 : 560)이다.
            InfoCard(
                title = "단계별 계획",
                modifier = Modifier
                    .weight(1020f)
                    .fillMaxHeight(),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                steps.forEachIndexed { index, step ->
                    PlanStepRow(number = index + 1, step = step, showDivider = index != steps.lastIndex)
                }
            }
            // 버튼이 오른쪽 열 안에 있는 화면이라 bottomActions 대신 열 맨 아래에 둔다(ui-components.md 1-6).
            Column(
                modifier = Modifier
                    .weight(560f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                LabeledCard(label = "운전자 승인", modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MockApprovals.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item,
                                    style = PleosTypography.BodyLarge.strong(),
                                    modifier = Modifier.weight(1f),
                                )
                                StatusChip(text = MockApprovedChip, tone = ChipTone.Normal, size = ChipSize.Small)
                            }
                        }
                    }
                }
                LabeledCard(
                    label = "자동 처리 (동승자 동의 범위)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MockAutoActions.forEach { item ->
                            Text(text = item, style = PleosTypography.BodyLarge.strong())
                        }
                    }
                }
                PleosButton(
                    text = "안내 시작",
                    onClick = onStartGuide,
                    type = PleosButtonType.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 타임라인 행. ui-components.md 3장 "타임라인 행"이다.
 * NumberedOptionItem과 모양이 비슷하지만 누를 수 없고 앞에 시각이 붙어서, B-3에서만 쓰는 지금은 화면 안에 둔다.
 * 번호 원 크기는 NumberedOptionItem과 같은 56을 썼다. screen-B3에서는 조금 작아 보인다. [확인 필요]
 */
@Composable
private fun PlanStepRow(number: Int, step: PlanStep, showDivider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.bottomDivider() else Modifier)
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = step.time, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(PleosColor.ButtonFilledEnabled, MosimShape.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = PleosTypography.BodySmall.strong(), color = PleosColor.TextLightPrimary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = step.title,
                style = PleosTypography.TitleSmall.strong(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = step.description, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
        }
    }
}

/**
 * 오른쪽 카드. InfoCard와 달리 제목이 흐린 라벨(26 Regular, 64%)이라 화면 안에 따로 둔다.
 * screen-B3의 운전자 승인, 자동 처리 두 카드에만 쓴다.
 */
@Composable
private fun LabeledCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = label, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        content()
    }
}

// InfoCard.kt, OptionRows.kt의 행 구분선과 같은 색·두께다. 두 파일 모두 private이라 여기서도 같은 방식으로 그린다.
private fun Modifier.bottomDivider() = drawBehind {
    val stroke = MosimDimens.RowDividerThickness.toPx()
    val y = size.height - stroke / 2
    drawLine(MosimColor.RowDivider, Offset(0f, y), Offset(size.width, y), stroke)
}

@Preview(name = "B-3 에스코트 계획", widthDp = 1700, heightDp = 1184)
@Composable
private fun EscortPlanScreenPreview() {
    MosimTheme {
        EscortPlanScreen(chargingStationName = null, onBack = {}, onStartGuide = {})
    }
}
