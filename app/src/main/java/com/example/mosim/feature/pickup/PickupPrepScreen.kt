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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.platform.pleos.BoardingSignals
import com.example.mosim.platform.pleos.CabinPrepSource
import com.example.mosim.platform.pleos.DoorSignal
import com.example.mosim.platform.pleos.PickupSourceProvider
import com.example.mosim.platform.pleos.PrepApplyState
import com.example.mosim.platform.pleos.PrepItemStatus
import com.example.mosim.platform.pleos.PrepKind
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/*
 * C-2 엄마 맞이 준비 (픽업지 정차 중).
 * 저장된 개인화 값을 차량에 적용하고(CabinPrepSource), 항목마다 적용 상태를 보여 준다.
 * 조수석 도어가 열리면(BoardingSignals) 탑승이 시작된 것으로 보고 D-1으로 넘어간다.
 * 설정 온도는 저장 당시 외기와 쌍으로 비교한다(project-status.md 개인화 저장 규칙).
 */

data class PickupPrepUiState(
    val passengerName: String,
    val stopConfirmedAt: String,
    val savedSetTemp: Double,
    val savedOutsideTemp: Double,
    val currentOutsideTemp: Double,
    val currentWeather: String,
    val prepItems: List<PrepItem>,
) {
    val recommendation: CabinTempRecommendation
        get() = recommendCabinTemp(savedSetTemp, savedOutsideTemp, currentOutsideTemp)

    val doneCount: Int
        get() = prepItems.count { it.state == PrepApplyState.Applied }

    // 이 차량에 없는 기능은 할 일로 세지 않는다.
    val totalCount: Int
        get() = prepItems.count { it.state != PrepApplyState.Unsupported }

    // 실패도 끝난 것으로 본다. 실패한 항목은 "설정 조정"에서 다시 맞추는 자리다.
    val isReady: Boolean
        get() = prepItems.none { it.state == PrepApplyState.Pending || it.state == PrepApplyState.Applying }

    /** 차량에서 받은 항목별 상태를 화면 항목에 붙인다. 아직 상태가 안 온 항목은 그대로 둔다. */
    fun withStatuses(statuses: List<PrepItemStatus>): PickupPrepUiState = copy(
        prepItems = prepItems.map { item ->
            item.copy(state = statuses.firstOrNull { it.kind == item.kind }?.state ?: item.state)
        },
    )
}

data class PrepItem(
    val kind: PrepKind,
    val label: String,
    val value: String,
    val state: PrepApplyState = PrepApplyState.Pending,
)

// Mock: 피그마 C-2 예시 값. 24 − 0.1 × (7 − 5) = 23.8 → 0.5 단위 반올림 24℃
private const val MOCK_SAVED_SET_TEMP = 24.0
private const val MOCK_SAVED_OUTSIDE_TEMP = 5.0
private const val MOCK_CURRENT_OUTSIDE_TEMP = 7.0
private val MockAppliedTemp =
    recommendCabinTemp(MOCK_SAVED_SET_TEMP, MOCK_SAVED_OUTSIDE_TEMP, MOCK_CURRENT_OUTSIDE_TEMP).temp

// Mock: 피그마 C-2 예시 값. 공조 행은 실내 온도 카드와 같은 추천 온도를 보여 준다.
// 적용 상태는 처음에 모두 대기이고, 화면에서 CabinPrepSource 결과로 바꾼다.
private val MockPickupPrepState = PickupPrepUiState(
    passengerName = "엄마",
    stopConfirmedAt = "08:27",
    savedSetTemp = MOCK_SAVED_SET_TEMP,
    savedOutsideTemp = MOCK_SAVED_OUTSIDE_TEMP,
    currentOutsideTemp = MOCK_CURRENT_OUTSIDE_TEMP,
    currentWeather = "비",
    prepItems = listOf(
        PrepItem(PrepKind.Climate, "공조", "${formatTemp(MockAppliedTemp)} · 풍량 2"),
        PrepItem(PrepKind.Seat, "좌석", "조수석 · 110° · 열선 1단"),
        PrepItem(PrepKind.Music, "음악", "조용한 음악 · 볼륨 4"),
        PrepItem(PrepKind.Lighting, "조명", "웰컴 (지원 차량)"),
    ),
)

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
    prepSource: CabinPrepSource = PickupSourceProvider.cabinPrep,
    boardingSignals: StateFlow<BoardingSignals> = PickupSourceProvider.boardingSignals,
) {
    val baseState = MockPickupPrepState

    // CabinPrepSource는 항목별 실패를 Failed로 보낸다. catch는 예상 못 한 예외에 앱이 꺼지지 않게 하는 안전장치다.
    val statusFlow = remember(prepSource) {
        prepSource.apply(baseState.prepItems.map { it.kind }).catch { }
    }
    val statuses by statusFlow.collectAsState(initial = emptyList())

    // 도어가 열리면 탑승이 시작된 것으로 본다. D-1은 같은 boardingSignals를 이어서 본다.
    val currentOnBoardingDetected by rememberUpdatedState(onBoardingDetected)
    LaunchedEffect(boardingSignals) {
        boardingSignals.first { it.passengerDoor == DoorSignal.Open || it.passengerDoor == DoorSignal.ClosedAfterOpen }
        currentOnBoardingDetected()
    }

    PickupPrepContent(
        state = baseState.withStatuses(statuses),
        onBack = onBack,
        onAdjustSettings = onAdjustSettings,
    )
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
                    val recommendation = state.recommendation
                    Row(horizontalArrangement = Arrangement.spacedBy(CompareBoxGap)) {
                        TempCompareBox(
                            label = "저장 당시",
                            value = "외부 ${formatTemp(state.savedOutsideTemp)} · 설정 ${formatTemp(state.savedSetTemp)}",
                            modifier = Modifier.weight(1f),
                        )
                        TempCompareBox(
                            label = "현재",
                            value = "외부 ${formatTemp(state.currentOutsideTemp)} · ${state.currentWeather}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppliedTempBox(temp = formatTemp(recommendation.temp))
                    Text(
                        text = tempNote(state.savedSetTemp, recommendation),
                        style = MosimTypography.Caption,
                        color = MosimColor.TextHint,
                    )
                }
            }

            InfoCard(
                title = "준비 항목",
                modifier = Modifier
                    .weight(PREP_COLUMN_WEIGHT)
                    .fillMaxHeight(),
            ) {
                state.prepItems.forEach { item ->
                    val chip = prepChip(item.state)
                    InfoRow(
                        label = item.label,
                        value = item.value,
                        chipText = chip.text,
                        chipTone = chip.tone,
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

private fun tempNote(savedSetTemp: Double, recommendation: CabinTempRecommendation): String =
    if (!recommendation.adjusted) {
        "저장 당시와 현재 외부 기온 차이가 작아 저장값을 그대로 적용"
    } else {
        // [확인 필요] 조정한 경우의 문구는 피그마에 없다. 디자인이 나오면 교체한다.
        "외부 기온 차이를 반영해 저장값 ${formatTemp(savedSetTemp)}에서 조정"
    }

private data class PrepChip(val text: String, val tone: ChipTone)

// "적용"(Normal)은 피그마 C-2를 따른다.
// [확인 필요] 대기, 적용 중, 미지원, 실패 문구와 칩 색은 피그마에 없어서 임시로 정했다.
private fun prepChip(state: PrepApplyState): PrepChip = when (state) {
    PrepApplyState.Pending -> PrepChip("대기", ChipTone.Neutral)
    PrepApplyState.Applying -> PrepChip("적용 중", ChipTone.Neutral)
    PrepApplyState.Applied -> PrepChip("적용", ChipTone.Normal)
    PrepApplyState.Unsupported -> PrepChip("미지원", ChipTone.Neutral)
    PrepApplyState.Failed -> PrepChip("실패", ChipTone.Caution)
}

/** "저장 당시" / "현재" 회색 비교 박스 */
@Composable
private fun TempCompareBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(PleosColor.BackgroundPrimary, MosimShape.Field)
            .padding(CompareBoxPadding),
        verticalArrangement = Arrangement.spacedBy(CompareBoxItemGap),
    ) {
        Text(text = label, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
        Text(text = value, style = PleosTypography.LabelMedium.strong())
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
        if (state.isReady) {
            LightStatusIndicator(text = "탑승 대기 중", dotColor = MosimColor.NormalAccent)
        } else {
            // [확인 필요] 준비 중 문구와 회색 점은 피그마에 없어서 임시로 정했다.
            LightStatusIndicator(text = "준비 중", dotColor = PleosColor.TextDarkQuaternary)
        }
    }
}

/** 밝은 배경용 점 + 문구. StatusPill은 진한 배경 전용이라 따로 만든다. */
@Composable
private fun LightStatusIndicator(text: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WaitingDotTextGap),
    ) {
        Box(
            modifier = Modifier
                .size(WaitingDotSize)
                .background(dotColor, MosimShape.Pill),
        )
        Text(
            text = text,
            style = PleosTypography.BodyMedium.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Mock: 피그마 C-2처럼 네 항목이 모두 적용된 상태
private val MockAllAppliedState = MockPickupPrepState.copy(
    prepItems = MockPickupPrepState.prepItems.map { it.copy(state = PrepApplyState.Applied) },
)

@Preview(name = "C-2 엄마 맞이 준비", widthDp = 1700, heightDp = 1184)
@Composable
private fun PickupPrepPreview() {
    MosimTheme {
        PickupPrepContent(state = MockAllAppliedState, onBack = {}, onAdjustSettings = {})
    }
}

@Preview(name = "C-2 적용 중", widthDp = 1700, heightDp = 1184)
@Composable
private fun PickupPrepApplyingPreview() {
    MosimTheme {
        // Mock: 공조는 적용, 좌석은 적용 중, 나머지는 대기
        PickupPrepContent(
            state = MockPickupPrepState.withStatuses(
                listOf(
                    PrepItemStatus(PrepKind.Climate, PrepApplyState.Applied),
                    PrepItemStatus(PrepKind.Seat, PrepApplyState.Applying),
                ),
            ),
            onBack = {},
            onAdjustSettings = {},
        )
    }
}

@Preview(name = "C-2 외기 차이로 조정", widthDp = 1700, heightDp = 1184)
@Composable
private fun PickupPrepAdjustedPreview() {
    MosimTheme {
        // Mock: 외기가 5℃ → 25℃로 올라 24 − 0.1 × 20 = 22℃로 낮춘 경우
        PickupPrepContent(
            state = MockAllAppliedState.copy(currentOutsideTemp = 25.0, currentWeather = "맑음"),
            onBack = {},
            onAdjustSettings = {},
        )
    }
}
