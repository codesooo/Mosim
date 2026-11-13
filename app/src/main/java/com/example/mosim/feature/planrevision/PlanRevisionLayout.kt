package com.example.mosim.feature.planrevision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mosim.R
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.MapSlot
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * F-1 화면의 생김새만 모아 둔 파일. 상태 관리와 SDK 호출은 PlanRevisionScreen.kt에 있다.
 *
 * 레이아웃 원칙
 *  - 카드 하나에 다 담는다. 버튼까지 카드 안에 넣어 "제안 → 결정"이 한 덩어리로 읽히게 했다.
 *  - 위에서부터 중요도 순으로 쌓고, 남는 높이는 장소·시간 블록과 버튼 사이에서만 흡수한다.
 *    글자가 늘어나면 그 여백이 먼저 줄어들어 버튼과 겹치지 않는다.
 *  - 왼쪽 내용과 오른쪽 지도는 가중치로 나눈다. 폼팩터마다 앱 영역 크기가 달라 고정 너비를 쓰지 않는다.
 */

/** 왼쪽 내용 : 오른쪽 지도 = 약 58 : 42. 지도가 비어 있어도 허전해 보이지 않는 선에서 잡았다. */
private const val ContentWeight = 58f
private const val MapWeight = 42f

@Composable
internal fun PlanRevisionLayout(
    state: PlanRevisionUiState,
    onBack: () -> Unit,
    onKeepRoute: () -> Unit,
    onRequestWaypoint: () -> Unit,
    onReplay: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable () -> Unit)? = null,
    mapDebugLabel: String? = null,
    interactionAllowed: Boolean = true,
) {
    MosimAppContainer(
        title = "이동 계획 수정",
        subtitle = "AI 제안",
        onBack = onBack,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            SuggestionCard(
                state = state,
                onKeepRoute = onKeepRoute,
                onRequestWaypoint = onRequestWaypoint,
                onReplay = onReplay,
                onStopSpeaking = onStopSpeaking,
                interactionAllowed = interactionAllowed,
                modifier = Modifier.weight(ContentWeight).fillMaxHeight(),
            )
            MapSlot(
                modifier = Modifier.weight(MapWeight).fillMaxHeight(),
                debugLabel = mapDebugLabel,
                content = mapContent,
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    state: PlanRevisionUiState,
    onKeepRoute: () -> Unit,
    onRequestWaypoint: () -> Unit,
    onReplay: () -> Unit,
    onStopSpeaking: () -> Unit,
    interactionAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val suggestion = state.suggestion
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
    ) {
        StatusChip("AI 추천", ChipTone.Neutral)
        Spacer(Modifier.height(MosimDimens.CardGap))

        // 1순위. 운전자가 이 한 줄만 보고도 결정할 수 있어야 한다.
        Text(suggestion.headline, style = PleosTypography.HeadlineSmall.strong())
        Spacer(Modifier.height(12.dp))
        Text(suggestion.reason, style = PleosTypography.BodyLarge, color = PleosColor.TextDarkSecondary)

        Spacer(Modifier.height(MosimDimens.SectionGap))

        // 2순위. 어디를 들르는지.
        if (suggestion.place != null) {
            PlaceCard(suggestion.place)
            Spacer(Modifier.height(MosimDimens.CardGap))
        }

        // 3순위. 언제 도착하고 얼마나 늘어나는지.
        if (suggestion.arrival != null || suggestion.extra != null) {
            TimeRow(suggestion)
        }

        // 4순위. 음성 안내 제어. 제안 내용에 따라붙는 것이라 위 덩어리에 붙여 둔다.
        if (suggestion.speech != null) {
            Spacer(Modifier.height(MosimDimens.SectionGap))
            AnnouncementControl(state.announcement, onReplay, onStopSpeaking)
        }

        // 내용이 짧을 때 남는 높이는 여기서만 먹는다.
        // 제안 덩어리와 결정 버튼 사이 한 군데만 비워, 글자가 늘면 이 여백부터 줄어든다.
        Spacer(Modifier.weight(1f))

        RequestNotice(state.request, interactionAllowed)

        // 5순위. 결정.
        Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
            PleosButton("기존 경로 유지", onKeepRoute, modifier = Modifier.weight(1f))
            PleosButton(
                text = "경유지 추가",
                onClick = onRequestWaypoint,
                type = PleosButtonType.Filled,
                enabled = interactionAllowed &&
                    suggestion.place != null &&
                    state.request != WaypointRequestState.Requested &&
                    state.request != WaypointRequestState.Applied,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 추천 장소. 추천 이유는 위에서 이미 말했으니 여기서는 이 장소만의 장점을 짧게 둔다. */
@Composable
private fun PlaceCard(place: SuggestedPlace, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceHigh, MosimShape.Field)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(place.name, style = PleosTypography.TitleSmall.strong())
        Text(place.highlight, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
    }
}

/**
 * 도착 시각과 늘어나는 시간. 계산 결과가 없는 값은 아예 그리지 않는다.
 * 숫자를 크게 둬서 한눈에 비교되게 했다.
 */
@Composable
private fun TimeRow(suggestion: PlanSuggestion, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.SectionGap * 2),
    ) {
        if (suggestion.arrival != null) {
            LabeledValue("예상 도착", suggestion.arrival, null)
        }
        if (suggestion.extra != null) {
            LabeledValue("추가 소요", suggestion.extra.text, suggestion.extra.composition())
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, note: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Text(value, style = PleosTypography.TitleLarge.strong())
        if (note != null) {
            Text(note, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
        }
    }
}

/**
 * 요청 상태와 조작 제한 안내.
 *
 * **경로가 바뀌었다고 말하지 않는다.** [WaypointRequestState.Applied]는 NaviHelper가 실제 반영을
 * 알려 줬을 때만 들어온다. 그 전까지는 "요청했다"까지만 말한다.
 */
@Composable
private fun RequestNotice(
    request: WaypointRequestState,
    interactionAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val notice: Pair<String, ChipTone>? = when {
        // 안전 제한으로 못 누르는 경우는 이유를 짧게 알려 준다.
        !interactionAllowed -> "주행 중에는 선택할 수 없어요" to ChipTone.Caution
        request == WaypointRequestState.Requested -> "경유지 추가를 요청했어요" to ChipTone.Neutral
        request == WaypointRequestState.NotConnected -> "경유지 추가는 준비 중이에요" to ChipTone.Neutral
        request == WaypointRequestState.Applied -> "경유지가 반영됐어요" to ChipTone.Normal
        request == WaypointRequestState.Failed -> "경유 요청에 실패했어요" to ChipTone.Caution
        else -> null
    }
    if (notice != null) {
        StatusChip(notice.first, notice.second, modifier)
        Spacer(Modifier.height(MosimDimens.CardGap))
    }
}

/**
 * 음성 안내 제어.
 *
 * 스피커 아이콘을 붙여 평범한 회색 버튼이 아니라 음성 기능임을 드러낸다.
 * 상태 문구는 실제 TTS 이벤트([AnnouncementState])를 따른다. 버튼을 눌렀다는 이유로 재생 중이라고 적지 않는다.
 */
@Composable
private fun AnnouncementControl(
    announcement: AnnouncementState,
    onReplay: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speaking = announcement == AnnouncementState.Speaking
    val failed = announcement == AnnouncementState.Failed
    val message = when {
        speaking -> "음성으로 안내하고 있어요"
        failed -> "음성 안내를 하지 못했어요"
        else -> "음성으로 다시 들을 수 있어요"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.IconTextGap),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume),
            contentDescription = null,
            tint = if (speaking) PleosColor.TextDarkPrimary else PleosColor.TextDarkTertiary,
            modifier = Modifier.size(MosimDimens.IconSize),
        )
        Text(
            text = message,
            style = PleosTypography.BodySmall,
            color = if (failed) PleosColor.TextDarkTertiary else PleosColor.TextDarkSecondary,
            modifier = Modifier.weight(1f),
        )
        if (speaking) {
            PleosButton("중지", onStopSpeaking)
        } else {
            PleosButton("다시 듣기", onReplay)
        }
    }
}
