package com.example.mosim.feature.escort

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.platform.pleos.TripSource
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MapMarker
import com.example.mosim.ui.components.MapMarkerKind
import com.example.mosim.ui.components.MosimMap
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.MosimStatus
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
import java.time.LocalTime

/**
 * F-1 이동 계획 수정. 지연 감지와 음성 장소 요청이 같은 화면을 쓴다(F-2/F-3을 따로 두지 않기로 함).
 *
 * 제안이 사라지면(승인·거절) [onClosed]로 E-1에 돌아간다. 화면이 스스로 이동하지 않고 상태를 보고 따른다.
 * Back은 "기존 경로 유지"와 같게 처리한다. 제안을 남겨 둔 채 E-1로 가면 E-1이 다시 F-1을 띄우기 때문이다.
 *
 * [확인 필요] 주행 중 승인 버튼(docs/design/escort.md 11장 #5). 지금은 디자인대로 버튼을 둔다.
 */
@Composable
fun EscortReplanScreen(
    viewModel: EscortViewModel,
    onClosed: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val proposal = state.proposal
    if (proposal == null) {
        // 그리는 중에 이동하지 않도록 효과로 미룬다.
        LaunchedEffect(Unit) { onClosed() }
        return
    }
    EscortReplanContent(
        proposal = proposal,
        onKeepRoute = viewModel::keepCurrentRoute,
        onAddWaypoint = viewModel::acceptProposal,
    )
}

@Composable
fun EscortReplanContent(
    proposal: ReplanProposal,
    onKeepRoute: () -> Unit = {},
    onAddWaypoint: () -> Unit = {},
) {
    MosimAppContainer(title = "이동 계획 수정", onBack = onKeepRoute) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(LEFT_WEIGHT)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                when (val reason = proposal.reason) {
                    is ReplanReason.Delay -> DelayComparisonCard(reason)
                    is ReplanReason.VoiceRequest -> SpokenRequestCard(reason.spokenText)
                }
                SuggestionCard(proposal = proposal, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
                    PleosButton(
                        text = EscortMessages.KEEP_CURRENT_ROUTE,
                        onClick = onKeepRoute,
                        enabled = !proposal.isSubmitting,
                        modifier = Modifier.weight(1f),
                    )
                    PleosButton(
                        text = EscortMessages.addWaypointButton(proposal.place.name),
                        onClick = onAddWaypoint,
                        type = PleosButtonType.Filled,
                        enabled = proposal.canAddWaypoint,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            MosimMap(
                markers = mapMarkers(proposal),
                placeholderText = mapCaption(proposal.reason),
                modifier = Modifier
                    .weight(RIGHT_WEIGHT)
                    .fillMaxHeight(),
            )
        }
    }
}

/** 위 주황 비교 카드 "예상 소요 ~~30분~~ → 60분 / 도착 ~~09:12~~ → 09:42" */
@Composable
private fun DelayComparisonCard(delay: ReplanReason.Delay, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MosimStatus.Caution.container, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComparisonRow(
            label = "예상 소요",
            before = EscortMessages.minutesValue(delay.plannedMinutes),
            after = EscortMessages.minutesValue(delay.delayedMinutes),
        )
        ComparisonRow(
            label = "도착",
            before = EscortMessages.clock(delay.plannedEta),
            after = EscortMessages.clock(delay.delayedEta),
        )
    }
}

@Composable
private fun ComparisonRow(label: String, before: String, after: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = label, style = PleosTypography.BodyMedium, color = MosimColor.CautionContent)
        Text(
            text = before,
            style = PleosTypography.BodyMedium.copy(textDecoration = TextDecoration.LineThrough),
            color = PleosColor.TextDarkTertiary,
        )
        Text(text = "→", style = PleosTypography.BodyMedium, color = MosimColor.CautionContent)
        Text(text = after, style = PleosTypography.TitleSmall.strong(), color = MosimColor.CautionContent)
    }
}

/** 음성 요청일 때 비교 카드 자리에 동승자가 한 말을 보여 준다. 피그마에 없는 자리다 [확인 필요]. */
@Composable
private fun SpokenRequestCard(spokenText: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "동승자 요청", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Text(text = EscortMessages.spokenRequest(spokenText), style = PleosTypography.BodyLarge.strong())
    }
}

/**
 * "AI 제안" 카드. 제안 문구는 프로필 규칙으로 만든 고정 문장이다.
 * [확인 필요] LLM을 쓰지 않는데 "AI 제안"이라는 제목을 그대로 쓸지(docs/design/escort.md 11장 #6).
 */
@Composable
private fun SuggestionCard(proposal: ReplanProposal, modifier: Modifier = Modifier) {
    InfoCard(title = "AI 제안", modifier = modifier) {
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Text(text = proposal.message, style = PleosTypography.BodyLarge.strong())
        Spacer(modifier = Modifier.padding(top = 24.dp))
        PlaceBox(proposal)
        EscortMessages.bundledActions(proposal.bundledActions)?.let { line ->
            Text(
                text = line,
                style = PleosTypography.BodySmall,
                color = PleosColor.TextDarkSecondary,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        proposal.errorMessage?.let { error ->
            Text(
                text = error,
                style = PleosTypography.BodySmall.strong(),
                color = MosimColor.WarningContent,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

/** 카드 안 회색 제안 박스 "휴식 장소 A / 설명 / 경유지 1/2 여유" */
@Composable
private fun PlaceBox(proposal: ReplanProposal) {
    val place = proposal.place
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceLow, MosimShape.Field)
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = place.name, style = PleosTypography.BodyLarge.strong())
            EscortMessages.placeSummary(place.categoryLabel, place.distanceMeters)?.let { summary ->
                Text(text = summary, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
            }
        }
        StatusChip(
            text = EscortMessages.waypointSlots(proposal.freeWaypointSlots, TripSource.MAX_WAYPOINTS),
            tone = if (proposal.freeWaypointSlots > 0) ChipTone.Neutral else ChipTone.Caution,
        )
    }
}

private fun mapMarkers(proposal: ReplanProposal): List<MapMarker> = listOfNotNull(
    proposal.searchOrigin?.let { MapMarker(it, MapMarkerKind.CURRENT_LOCATION) },
    MapMarker(proposal.place.location, MapMarkerKind.PLACE),
)

/** 지도 키가 없을 때 MapPlaceholder에 보여 줄 설명. [확인 필요] 정체 구간 표시는 NaviHelper 연동 후 */
private fun mapCaption(reason: ReplanReason): String = when (reason) {
    is ReplanReason.Delay -> "지도 · 정체 구간 + 휴식 장소"
    is ReplanReason.VoiceRequest -> "지도 · 현재 위치 + 제안 장소"
}

// [확인 필요] 피그마 F-1 정확한 비율을 확인하지 못했다. 왼쪽 카드 열이 지도보다 조금 좁다.
private const val LEFT_WEIGHT = 760f
private const val RIGHT_WEIGHT = 844f

// Mock: 피그마 F-1 예시 값
private val previewProposal = ReplanProposal(
    reason = ReplanReason.Delay(
        plannedEta = LocalTime.of(9, 12),
        delayedEta = LocalTime.of(9, 42),
        plannedMinutes = 30,
        delayedMinutes = 60,
    ),
    message = EscortMessages.restProposal("엄마"),
    place = PlaceCandidate(
        id = "mock-rest-a",
        name = "휴식 장소 A",
        categoryLabel = "화장실 · 카페",
        address = null,
        location = GeoPoint(37.58, 126.99),
        distanceMeters = 1_800,
    ),
    freeWaypointSlots = 1,
    bundledActions = listOf(CabinAction.SetMusicVolume(from = 4, to = 3)),
)

@Preview(name = "F-1 지연 감지 · 휴식 제안", widthDp = 1700, heightDp = 1184)
@Composable
private fun EscortReplanDelayPreview() {
    MosimTheme { EscortReplanContent(proposal = previewProposal) }
}

@Preview(name = "F-1 음성 장소 요청 · 경유 실패", widthDp = 1700, heightDp = 1184)
@Composable
private fun EscortReplanVoicePreview() {
    MosimTheme {
        EscortReplanContent(
            proposal = previewProposal.copy(
                reason = ReplanReason.VoiceRequest("카페 들렀다 가자"),
                message = EscortMessages.voicePlaceProposal(PlaceKind.CAFE, "카페 A"),
                bundledActions = emptyList(),
                errorMessage = "경로에 경유지를 추가하지 못했어요",
            ),
        )
    }
}
