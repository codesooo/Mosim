package com.example.mosim.feature.pickup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
 * C-1 픽업지로 이동 중 (주행 중 화면).
 * 운전 중이라 버튼을 두지 않고 글자를 크게 쓴다. 다음 화면(C-2)으로는 픽업지 정차 감지로 넘어간다.
 */

data class PickupEnRouteUiState(
    val passengerName: String,
    val pickupPlace: String,
    val eta: String,
    val appointmentText: String,
    val remainingDistanceText: String,
    val chargeText: String,
    val arrivalBatteryText: String,
    val prepSteps: List<PrepStep>,
    val shareLabel: String,
    val shareStatus: String,
)

data class PrepStep(
    val label: String,
    val chipText: String,
    val chipTone: ChipTone,
)

// Mock: 피그마 C-1 예시 값
private val MockPickupEnRouteState = PickupEnRouteUiState(
    passengerName = "엄마",
    pickupPlace = "강남역 4번 출구",
    eta = "08:27",
    appointmentText = "약속 08:30 · 3분 여유",
    remainingDistanceText = "남은 거리 3.1km",
    chargeText = "충전 완료 · 잔량 45%",
    arrivalBatteryText = "목적지 도착 예상 잔량 31%",
    prepSteps = listOf(
        PrepStep("사전 공조", "08:17 시작 · 진행 중", ChipTone.Dark),
        PrepStep("실내 준비", "픽업지 정차 후", ChipTone.Neutral),
        PrepStep("픽업 안내", "비 · 지붕 있는 4번 출구", ChipTone.Neutral),
    ),
    shareLabel = "엄마에게 픽업 위치 공유",
    shareStatus = "공유됨",
)

// Mock: 실제로는 차량 위치·정차 신호로 픽업지 도착을 판단한다. 지금은 시연용으로 일정 시간 뒤 넘긴다.
private const val MOCK_ARRIVAL_DELAY_MS = 5_000L

// 피그마 C-1 배치 값 (이 화면에서만 쓴다)
private const val HERO_COLUMN_WEIGHT = 960f
private const val SIDE_COLUMN_WEIGHT = 620f
private val HeroItemGap = 12.dp
private val HeroChipDistanceGap = 16.dp // [확인 필요] 칩과 "남은 거리" 사이 간격은 피그마에서 확인하지 못했다.
private val PrepRowPaddingVertical = 24.dp
private val ShareCardPaddingVertical = 28.dp

@Composable
fun PickupEnRouteScreen(
    onBack: () -> Unit,
    onArrived: () -> Unit,
) {
    val currentOnArrived by rememberUpdatedState(onArrived)
    LaunchedEffect(Unit) {
        delay(MOCK_ARRIVAL_DELAY_MS)
        currentOnArrived()
    }
    PickupEnRouteContent(state = MockPickupEnRouteState, onBack = onBack)
}

@Composable
fun PickupEnRouteContent(
    state: PickupEnRouteUiState,
    onBack: () -> Unit,
) {
    MosimAppContainer(
        title = "픽업지로 이동 중",
        subtitle = "${state.passengerName} · ${state.pickupPlace}",
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            PickupEtaCard(
                state = state,
                modifier = Modifier
                    .weight(HERO_COLUMN_WEIGHT)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(SIDE_COLUMN_WEIGHT)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                InfoCard(
                    title = "다음 준비",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    state.prepSteps.forEach { step -> PrepStepRow(step) }
                }
                ShareStatusCard(label = state.shareLabel, status = state.shareStatus)
            }
        }
    }
}

/** 왼쪽 큰 카드. EscortHeroPanel과 달리 동승자 이름 줄이 없고 아래에 충전 정보가 붙는다. */
@Composable
private fun PickupEtaCard(
    state: PickupEnRouteUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(HeroItemGap),
    ) {
        Text(text = "픽업 도착 예정", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Text(text = state.eta, style = MosimTypography.HeroEta, maxLines = 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeroChipDistanceGap),
        ) {
            StatusChip(text = state.appointmentText, tone = ChipTone.Normal, size = ChipSize.Small)
            Text(
                text = state.remainingDistanceText,
                style = MosimTypography.HeroSubSmall,
                color = PleosColor.TextDarkTertiary,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = state.chargeText, style = PleosTypography.BodyMedium.strong(), maxLines = 1)
        Text(
            text = state.arrivalBatteryText,
            style = PleosTypography.LabelMedium,
            color = PleosColor.TextDarkTertiary,
            maxLines = 1,
        )
    }
}

/** "다음 준비" 한 줄. InfoRow는 라벨이 26 회색이라 30 Bold 검정 라벨용으로 따로 만든다. */
@Composable
private fun PrepStepRow(step: PrepStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = MosimDimens.RowDividerThickness.toPx()
                val y = size.height - stroke / 2
                drawLine(MosimColor.RowDivider, Offset(0f, y), Offset(size.width, y), stroke)
            }
            .padding(vertical = PrepRowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = step.label,
            style = PleosTypography.BodyLarge.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        StatusChip(text = step.chipText, tone = step.chipTone, size = ChipSize.Small)
    }
}

/** 오른쪽 아래 한 줄 카드. CompactInfoCard는 26 회색 라벨 + 36 값이라 글자 구성이 달라 따로 만든다. */
@Composable
private fun ShareStatusCard(
    label: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(horizontal = MosimDimens.CardPadding, vertical = ShareCardPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = PleosTypography.BodyMedium.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(text = status, style = PleosTypography.LabelMedium.strong(), color = MosimColor.TextHint, maxLines = 1)
    }
}

@Preview(name = "C-1 픽업지 이동 중", widthDp = 1700, heightDp = 1184)
@Composable
private fun PickupEnRoutePreview() {
    MosimTheme {
        PickupEnRouteContent(state = MockPickupEnRouteState, onBack = {})
    }
}
