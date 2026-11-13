package com.example.mosim.feature.planrevision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

/*
 * F-1 화면의 생김새만 모아 둔 파일. 상태 관리와 SDK 호출은 PlanRevisionScreen.kt에 있다.
 *
 * B-1(출발 전 점검)·G-1(도착)과 같은 틀을 쓴다. 그 두 화면이 "판단 배너 → 정보 카드 → 하단 버튼"으로
 * 읽히는데, F-1만 카드 하나에 전부 쌓여 있어 다른 화면처럼 보이지 않았다.
 *  - 판단 한 줄은 JudgmentBanner로 올린다. 음성 다시 듣기는 배너의 동작 버튼 자리에 둔다.
 *  - 장소와 시간은 InfoCard + InfoRow로 나눈다. 라벨과 값이 좌우로 갈려 훑어보기 쉽다.
 *  - 결정 버튼은 MosimAppContainer의 bottomActions로 내린다. 모든 화면에서 같은 자리다.
 */

@Composable
internal fun PlanRevisionLayout(
    state: PlanRevisionUiState,
    onBack: () -> Unit,
    onKeepRoute: () -> Unit,
    onRequestWaypoint: () -> Unit,
    onReplay: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier,
    interactionAllowed: Boolean = true,
) {
    val suggestion = state.suggestion
    val canRequest = interactionAllowed &&
        suggestion.place != null &&
        state.request != WaypointRequestState.Requested &&
        state.request != WaypointRequestState.Applied

    MosimAppContainer(
        title = "이동 계획 수정",
        subtitle = suggestion.passengerName?.let { "$it · AI 제안" } ?: "AI 제안",
        onBack = onBack,
        modifier = modifier,
        bottomActions = {
            PleosButton("기존 경로 유지", onKeepRoute)
            PleosButton(
                text = "경유지 추가",
                onClick = onRequestWaypoint,
                type = PleosButtonType.Filled,
                enabled = canRequest,
                modifier = Modifier.width(MosimDimens.ButtonWideWidth),
            )
        },
    ) {
        AnnouncementBanner(
            suggestion = suggestion,
            announcement = state.announcement,
            onReplay = onReplay,
            onStopSpeaking = onStopSpeaking,
        )
        // 배너와 카드는 읽는 단계가 다르다(판단 → 근거). B-1처럼 한 칸 더 띄운다.
        Spacer(Modifier.height(MosimDimens.SectionGap))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            PlaceCard(
                place = suggestion.place,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ChangeCard(
                state = state,
                interactionAllowed = interactionAllowed,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/**
 * 제안 한 줄과 음성 안내 제어.
 *
 * 상태 문구는 실제 TTS 이벤트([AnnouncementState])를 따른다. 버튼을 눌렀다는 이유로 재생 중이라고 적지 않는다.
 * 읽어 줄 문장이 없으면 동작 버튼도 두지 않는다.
 */
@Composable
private fun AnnouncementBanner(
    suggestion: PlanSuggestion,
    announcement: AnnouncementState,
    onReplay: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speaking = announcement == AnnouncementState.Speaking
    val description = when {
        suggestion.speech == null -> suggestion.reason
        announcement == AnnouncementState.Failed -> "${suggestion.reason} · 음성 안내를 하지 못했어요"
        speaking -> "${suggestion.reason} · 음성으로 안내하고 있어요"
        else -> suggestion.reason
    }
    JudgmentBanner(
        status = MosimStatus.Normal,
        title = suggestion.headline,
        description = description,
        actionText = if (suggestion.speech == null) null else if (speaking) "중지" else "다시 듣기",
        onAction = if (suggestion.speech == null) null else if (speaking) onStopSpeaking else onReplay,
        modifier = modifier,
    )
}

/** 어디를 들르는지. 추천 이유는 배너에서 이미 말했으니 여기서는 이 장소만의 사실을 둔다. */
@Composable
private fun PlaceCard(place: SuggestedPlace?, modifier: Modifier = Modifier) {
    InfoCard(title = "추천 장소", modifier = modifier) {
        if (place == null) {
            InfoRow(
                label = "장소",
                value = "",
                chipText = "추천 장소가 없어요",
                showDivider = false,
                modifier = Modifier.weight(1f),
            )
            return@InfoCard
        }
        InfoRow(
            label = "장소",
            value = place.name,
            icon = painterResource(R.drawable.ic_map_pin),
            modifier = Modifier.weight(1f),
        )
        InfoRow(
            label = "특징",
            value = place.highlight,
            showDivider = false,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 이 제안을 받아들이면 무엇이 달라지는지, 그리고 요청이 어디까지 갔는지.
 *
 * **경로가 바뀌었다고 말하지 않는다.** [WaypointRequestState.Applied]는 NaviHelper가 실제 반영을
 * 알려 줬을 때만 들어온다. 그 전까지는 "요청했다"까지만 말한다.
 */
@Composable
private fun ChangeCard(
    state: PlanRevisionUiState,
    interactionAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val suggestion = state.suggestion
    val notice = requestNotice(state.request, interactionAllowed)
    InfoCard(title = "이동 변화", modifier = modifier) {
        if (suggestion.arrival != null) {
            InfoRow(
                label = "예상 도착",
                value = suggestion.arrival,
                icon = painterResource(R.drawable.ic_clock),
                modifier = Modifier.weight(1f),
            )
        }
        if (suggestion.extra != null) {
            InfoRow(
                label = "추가 소요",
                value = suggestion.extra.text,
                icon = painterResource(R.drawable.ic_clock),
                chipText = suggestion.extra.composition(),
                chipTone = ChipTone.Caution,
                modifier = Modifier.weight(1f),
            )
        }
        InfoRow(
            label = "경유지 요청",
            value = "",
            chipText = notice.first,
            chipTone = notice.second,
            showDivider = false,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun requestNotice(
    request: WaypointRequestState,
    interactionAllowed: Boolean,
): Pair<String, ChipTone> = when {
    // 안전 제한으로 못 누르는 경우는 이유를 짧게 알려 준다.
    !interactionAllowed -> "주행 중에는 선택할 수 없어요" to ChipTone.Caution
    request == WaypointRequestState.Requested -> "요청했어요" to ChipTone.Neutral
    request == WaypointRequestState.NotConnected -> "경유지 추가는 준비 중이에요" to ChipTone.Neutral
    request == WaypointRequestState.Applied -> "경유지가 반영됐어요" to ChipTone.Normal
    request == WaypointRequestState.Failed -> "경유 요청에 실패했어요" to ChipTone.Caution
    else -> "아직 요청하지 않았어요" to ChipTone.Neutral
}
