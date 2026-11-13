package com.example.mosim.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.R
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.InfoRow
import com.example.mosim.ui.components.JudgmentBanner
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.MosimStatus
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme

/*
 * B-1 출발 전 점검 (피그마 모심 화면 v2, 이용 흐름 02단계).
 *
 * 기존 VEHICLE route 자리를 그대로 쓴다. 화면에 보이는 값은 전부 아래 Mock 상수이고,
 * VehicleStatusSource나 Pleos SDK는 읽지 않는다.
 * 데이터 출처(Vehicle SDK, NaviHelper, 날씨 API)와 판단 규칙이 요구사항으로 정해지면 Mock을 교체한다.
 */

/** 카드 안 한 줄. InfoRow에 그대로 넘길 값만 담는다. */
private data class CheckRow(
    val label: String,
    val value: String,
    val icon: Int, // R.drawable.ic_*
    val chipText: String? = null,
    val chipTone: ChipTone = ChipTone.Neutral,
)

private data class CheckCard(val title: String, val rows: List<CheckRow>)

// Mock: 피그마 B-1 예시 값을 그대로 옮겼다. 실제 차량·내비·날씨 데이터가 아니다.
// "충전 필요" 판단, 거리·도착 시각 계산은 하지 않고 피그마에 표시된 결과 문구를 쓴다.
private const val MockSubtitle = "엄마 · 강남역 2번 출구 08:30 → 서울대병원"
private val MockBannerStatus = MosimStatus.Caution
private const val MockBannerTitle = "픽업 전 충전이 필요합니다"
private const val MockBannerDescription = "목적지 도착 예상 잔량 4% · 기준 15% 미만 (전체 이동 22.8km)"

// Mock: 피그마 B-1 카드 3개의 예시 값.
private val MockCards = listOf(
    CheckCard(
        title = "차량 상태",
        rows = listOf(
            CheckRow("배터리", "12%", R.drawable.ic_battery, chipText = "부족", chipTone = ChipTone.Caution),
            CheckRow("주행 가능", "30km", R.drawable.ic_map_pin),
            CheckRow("실내 온도", "16℃", R.drawable.ic_thermometer),
            // 값 없이 칩만 있는 행이다(ui-components.md 1-4).
            CheckRow("동승석", "", R.drawable.ic_users, chipText = "짐 있음 추정", chipTone = ChipTone.Caution),
        ),
    ),
    CheckCard(
        title = "이동 상태",
        rows = listOf(
            CheckRow("픽업까지", "8.2km", R.drawable.ic_map_pin),
            CheckRow("목적지까지", "14.6km", R.drawable.ic_map_pin),
            CheckRow("픽업 도착 예정", "08:22", R.drawable.ic_clock, chipText = "8분 여유", chipTone = ChipTone.Normal),
            CheckRow("목적지 도착", "09:05", R.drawable.ic_map_pin),
        ),
    ),
    CheckCard(
        title = "외부 환경",
        rows = listOf(
            CheckRow("날씨", "비 · 7℃", R.drawable.ic_cloud_rain),
            CheckRow("픽업 위치", "4번 출구 제안", R.drawable.ic_map_pin),
            CheckRow("사전 공조", "10분 일찍 시작", R.drawable.ic_thermometer),
        ),
    ),
)

/**
 * 버튼 콜백은 구조만 둔다.
 * 다시 점검은 Mock이라 갱신할 데이터가 없다.
 */
@Composable
fun VehicleStatusScreen(
    onBack: () -> Unit,
    onShowChargingStations: () -> Unit,
    onShowEscortPlan: () -> Unit,
    onRecheck: () -> Unit,
) {
    MosimAppContainer(
        title = "출발 전 점검",
        subtitle = MockSubtitle,
        onBack = onBack,
        bottomActions = {
            // 너비를 주지 않으면 PleosButton 최소 너비 280이 적용된다. 피그마 값과 같다.
            PleosButton(text = "다시 점검", onClick = onRecheck)
            PleosButton(
                text = "에스코트 계획 보기",
                onClick = onShowEscortPlan,
                type = PleosButtonType.Filled,
                modifier = Modifier.width(MosimDimens.ButtonWideWidth),
            )
        },
    ) {
        JudgmentBanner(
            status = MockBannerStatus,
            title = MockBannerTitle,
            description = MockBannerDescription,
            actionText = "충전소 보기",
            onAction = onShowChargingStations,
        )
        Spacer(modifier = Modifier.height(MosimDimens.SectionGap))
        // 피그마는 카드 너비가 조금씩 다르지만 폼팩터 대응을 위해 균등 배치한다(팀 결정).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            MockCards.forEach { card ->
                CheckCardView(
                    card = card,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun CheckCardView(card: CheckCard, modifier: Modifier = Modifier) {
    InfoCard(title = card.title, modifier = modifier) {
        card.rows.forEachIndexed { index, row ->
            // 피그마처럼 행들이 카드 높이를 나눠 갖게 weight를 준다. 마지막 행 아래에는 구분선이 없다.
            InfoRow(
                label = row.label,
                value = row.value,
                modifier = Modifier.weight(1f),
                icon = painterResource(row.icon),
                chipText = row.chipText,
                chipTone = row.chipTone,
                showDivider = index != card.rows.lastIndex,
            )
        }
    }
}

// 앱 영역 크기(Connect-S-17 landscape, MosimDimens.AppContentWidth × AppContentHeight)로 본다.
@Preview(name = "B-1 출발 전 점검", widthDp = 1700, heightDp = 1184)
@Composable
private fun VehicleStatusScreenPreview() {
    MosimTheme {
        VehicleStatusScreen(
            onBack = {},
            onShowChargingStations = {},
            onShowEscortPlan = {},
            onRecheck = {},
        )
    }
}
