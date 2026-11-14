package com.example.mosim.feature.arrival

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.data.EscortSummary
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.InfoRow
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme

/*
 * G-1 도착 · 에스코트 종료 (design/screen-G1.png, 이용 흐름 07단계).
 *
 * 개발용 임시 메인 화면의 "도착" 버튼에서 진입한다. B-1~B-3 흐름과 연결하지 않는다.
 * 화면에 보이는 값은 전부 아래 Mock 상수다. 하차 감지(Vehicle SDK), 도착 감지(NaviHelper),
 * 이동 기록 집계는 하지 않는다. 데이터 출처와 집계 방식이 정해지면 Mock을 교체한다. [확인 필요]
 */

/** 하차 감지 카드 한 줄. 값 없이 칩만 있는 InfoRow로 그린다(ui-components.md 1-4). */
private data class DropOffSignal(val label: String, val chipText: String)

/** 이번 에스코트 카드 한 줄. */
private data class TripSummaryRow(val label: String, val value: String)

// Mock: screen-G1 design example values. 실제 차량·내비 데이터가 아니다.
private const val MockSubtitle = "09:45 서울대병원 본관 · 에스코트 종료"

// Mock: screen-G1 하차 감지 신호. 감지 판단 로직은 없다.
private val MockDropOffSignals = listOf(
    DropOffSignal("목적지 도착", "확인"),
    DropOffSignal("조수석 도어", "열림 → 닫힘"),
    DropOffSignal("조수석 착석", "해제"),
    DropOffSignal("차량 상태", "정차"),
)

// Mock: screen-G1 이번 에스코트 요약. 이동 시간·거리·횟수를 계산하지 않고 디자인 값을 그대로 쓴다.
private val MockTripSummary = listOf(
    TripSummaryRow("이동 시간", "76분 (정체 포함)"),
    TripSummaryRow("이동 거리", "22.8km"),
    TripSummaryRow("경유", "충전소 · 카페 A"),
    TripSummaryRow("자동 조정", "2회 (온도, 볼륨)"),
    TripSummaryRow("음성 요청", "1회"),
)

/**
 * 실제로 센 값(경유, 자동 조정, 음성 요청)만 요약에서 가져온다.
 * 이동 시간·거리는 집계 근거가 없어 Mock 값을 그대로 쓰고 값 옆에 Mock이라고 적는다.
 */
private fun summaryRows(summary: EscortSummary): List<TripSummaryRow> = listOf(
    TripSummaryRow("이동 시간", "76분 (Mock)"),
    TripSummaryRow("이동 거리", "22.8km (Mock)"),
    TripSummaryRow(
        "경유",
        if (summary.waypointNames.isEmpty()) "없음" else summary.waypointNames.joinToString(" · "),
    ),
    TripSummaryRow("자동 조정", "${summary.autoAdjustCount}회" + if (summary.isSimulated) " (Mock)" else ""),
    TripSummaryRow("음성 요청", "${summary.voiceRequestCount}회"),
)

/**
 * 프로필 업데이트 확인은 G-2(프로필 업데이트)로 간다. 이동은 호출부(MosimNavHost)가 정한다.
 */
@Composable
fun ArrivalScreen(
    onBack: () -> Unit,
    onConfirmProfileUpdate: () -> Unit,
    /** 방금 끝난 에스코트 요약. 없으면(개발용 직접 진입) 아래 Mock 값을 보여 준다. */
    summary: EscortSummary? = null,
) {
    val rows = summary?.let(::summaryRows) ?: MockTripSummary
    MosimAppContainer(
        title = "도착",
        subtitle = summary?.let { "${it.destinationName} · ${it.passengerName} 에스코트 종료" } ?: MockSubtitle,
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // 열 너비 비율은 screen-G1에서 잰 값(약 600 : 980)이다.
            InfoCard(
                title = "하차 감지",
                modifier = Modifier
                    .weight(600f)
                    .fillMaxHeight(),
            ) {
                MockDropOffSignals.forEachIndexed { index, signal ->
                    InfoRow(
                        label = signal.label,
                        value = "",
                        chipText = signal.chipText,
                        chipTone = ChipTone.Normal,
                        showDivider = index != MockDropOffSignals.lastIndex,
                    )
                }
            }
            // 버튼이 오른쪽 카드 안 맨 아래에 있다(ui-components.md G-1).
            InfoCard(
                title = "이번 에스코트",
                modifier = Modifier
                    .weight(980f)
                    .fillMaxHeight(),
            ) {
                rows.forEachIndexed { index, row ->
                    InfoRow(
                        label = row.label,
                        value = row.value,
                        showDivider = index != rows.lastIndex,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                PleosButton(
                    text = "프로필 업데이트 확인",
                    onClick = onConfirmProfileUpdate,
                    type = PleosButtonType.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "G-1 도착", widthDp = 1700, heightDp = 1184)
@Composable
private fun ArrivalScreenPreview() {
    MosimTheme {
        ArrivalScreen(onBack = {}, onConfirmProfileUpdate = {})
    }
}
