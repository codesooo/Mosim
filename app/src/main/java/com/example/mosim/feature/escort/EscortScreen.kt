package com.example.mosim.feature.escort

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.CompactInfoCard
import com.example.mosim.ui.components.EscortHeroPanel
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.StatusPill
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong
import java.time.LocalTime

/**
 * E-1 에스코트 모드 + E-2 자동 조절 알림.
 *
 * 주행 중 화면이라 버튼을 두지 않는다(ui-components.md E·F 안내). 조작은 동승자 음성으로만 들어온다.
 * E-2는 화면을 따로 두지 않고 상태의 notice가 있을 때 제목 옆에 토스트를 띄운다.
 *
 * 에스코트 중에는 이 화면이 최상단이다. Back은 흐름을 끝내지 않고 앱을 숨긴다(Pleos 06 CCP Back 규칙).
 * 운전 중 Back 한 번에 에스코트가 끊기지 않게 하려는 것이다. 다시 앱을 열면 같은 에스코트로 돌아온다.
 */
@Composable
fun EscortScreen(
    viewModel: EscortViewModel,
    onProposal: () -> Unit,
    onVoiceRequest: () -> Unit = {},
    onArrived: () -> Unit = {},
    interactionAllowed: Boolean = false,
) {
    val state by viewModel.uiState.collectAsState()
    RequestNotificationPermission()
    // 목적지 도착은 내비가 알려 준다. 도착하면 G-1로 넘긴다. 하차 감지는 아직 없다 [확인 필요].
    LaunchedEffect(state.arrived) { if (state.arrived) onArrived() }
    // 제안은 지연 감지나 음성 요청으로 화면 밖에서 생긴다. 생기면 F-1로 넘긴다.
    // E-1로 돌아왔을 때 제안이 남아 있으면 다시 넘긴다(키가 같아도 화면에 다시 들어오면 효과가 새로 돈다).
    val hasProposal = state.proposal != null
    LaunchedEffect(hasProposal) {
        if (hasProposal) onProposal()
    }
    EscortContent(
        state = state,
        // 주행 중 조작 제한이 걸려 있으면 음성 요청도 시작하지 않는다(탭 한 번도 조작이다).
        onVoiceRequest = if (interactionAllowed) onVoiceRequest else null,
    )
}

/**
 * 모심이 내비 뒤에 있을 때 E-2·F-1을 시스템 알림으로 올리려면 알림 권한이 필요하다(Android 13부터).
 * 에스코트에 들어올 때 한 번 묻는다. 거절해도 에스코트는 그대로 동작하고 화면 밖 알림만 빠진다.
 */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

/**
 * 창 너비에 따라 배치를 바꾼다. Pleos에서는 내비와 화면을 나누면 창이 반쪽(약 840dp)이 된다.
 * 좁은 창에서는 내비를 옆에 두고 흘끗 보는 용도라, 같은 정보를 세로로 쌓아 한 열로 보여 준다.
 */
@Composable
fun EscortContent(state: EscortUiState, onVoiceRequest: (() -> Unit)? = null) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        when (EscortLayout.of(maxWidth.value)) {
            EscortLayout.WIDE -> WideEscortContent(state = state, onVoiceRequest = onVoiceRequest)
            EscortLayout.COMPACT -> CompactEscortContent(state = state, onVoiceRequest = onVoiceRequest)
        }
    }
}

@Composable
private fun WideEscortContent(state: EscortUiState, onVoiceRequest: (() -> Unit)?) {
    MosimAppContainer(
        title = "에스코트 모드",
        subtitle = EscortMessages.escortSubtitle(state.boardedAt, state.passengerName),
        headerTrailing = { NoticeToast(notice = state.notice) },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            EscortHero(
                state = state,
                // 피그마 E-1 비율(왼쪽 1060 : 오른쪽 400)
                modifier = Modifier
                    .weight(HERO_WEIGHT)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(SIDE_WEIGHT)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                CabinCards(state = state)
                Spacer(modifier = Modifier.weight(1f))
                VoiceStandbyPill(onVoiceRequest)
            }
        }
    }
}

/** 제목 줄에 토스트를 붙이면 좁은 창에서 제목이 밀려 잘린다. 그래서 토스트를 제목 아래 한 줄로 내린다. */
@Composable
private fun CompactEscortContent(state: EscortUiState, onVoiceRequest: (() -> Unit)?) {
    MosimAppContainer(
        title = "에스코트 모드",
        subtitle = EscortMessages.escortSubtitle(state.boardedAt, state.passengerName),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            NoticeToast(notice = state.notice, modifier = Modifier.fillMaxWidth())
            EscortHero(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            // 세로 카드 세 장을 쌓으면 히어로가 눌려 "남은 거리" 줄이 잘렸다(840×1184에서 확인). 그래서 한 줄에 둔다.
            Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
                cabinItems(state).forEach { (label, value) ->
                    CabinTile(label = label, value = value, modifier = Modifier.weight(1f))
                }
            }
            VoiceStandbyPill(onVoiceRequest)
        }
    }
}

@Composable
private fun EscortHero(state: EscortUiState, modifier: Modifier) {
    EscortHeroPanel(
        passengerName = state.passengerName,
        destination = state.destinationName,
        eta = EscortMessages.clock(state.eta),
        remaining = EscortMessages.remaining(state.remainDistanceMeters, state.remainMinutes),
        modifier = modifier,
    )
}

/** 넓은 배치와 좁은 배치가 같은 항목을 같은 순서로 보여 주도록 한 곳에서 만든다. */
private fun cabinItems(state: EscortUiState): List<Pair<String, String>> = listOf(
    "실내 온도" to EscortMessages.temperatureValue(state.cabinTemperatureCelsius),
    "음악" to EscortMessages.volumeValue(state.musicVolume),
    "좌석" to EscortMessages.backrestValue(state.passengerBackrestAngle),
)

@Composable
private fun CabinCards(state: EscortUiState) {
    cabinItems(state).forEach { (label, value) -> CompactInfoCard(label = label, value = value) }
}

/** 좁은 배치용 카드. 너비가 1/3이라 라벨과 값을 좌우가 아니라 위아래로 둔다. */
@Composable
private fun CabinTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary, maxLines = 1)
        Text(
            text = value,
            style = PleosTypography.TitleMedium.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 동승자 음성 요청을 시작하는 자리다.
 *
 * 호출어로 늘 듣고 있지 않는 이유: 상시 인식 허용 여부가 확인되지 않았고(docs/design/escort.md 11장 #1),
 * 마이크를 계속 점유하게 된다. 그래서 탭 한 번으로 F-2를 연다. 주행 제한이 걸리면 누를 수 없다.
 */
@Composable
private fun VoiceStandbyPill(onVoiceRequest: (() -> Unit)?) {
    val base = Modifier.fillMaxWidth()
    // 문구는 상태와 상관없이 하나로 둔다. 좁은 창에서 긴 문구가 잘려 "눌러…"로 보였다.
    StatusPill(
        text = "동승자 음성 요청 대기 중",
        modifier = if (onVoiceRequest == null) base else base.clickable(onClick = onVoiceRequest),
    )
}

/** notice가 null이 된 뒤 사라지는 동안에도 마지막 문구를 보여 주려고 기억해 둔다. */
@Composable
private fun NoticeToast(notice: EscortNotice?, modifier: Modifier = Modifier) {
    var lastNoticeText by remember { mutableStateOf("") }
    notice?.let { lastNoticeText = EscortMessages.noticeLine(it.message, it.detail) }
    AnimatedVisibility(visible = notice != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        AutoAdjustToast(text = lastNoticeText)
    }
}

/** E-2 제목 옆 진한 토스트 (피그마 toast/auto adjust). 이 화면에서만 써서 여기 둔다. */
@Composable
private fun AutoAdjustToast(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PleosTypography.BodySmall.strong(),
        color = PleosColor.TextLightPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(PleosColor.ButtonFilledEnabled, MosimShape.Field)
            .padding(horizontal = 32.dp, vertical = 20.dp),
    )
}

private const val HERO_WEIGHT = 1060f
private const val SIDE_WEIGHT = 400f

// Mock: 피그마 E-1 / E-2 예시 값
private val previewState = EscortUiState(
    passengerName = "엄마",
    destinationName = "서울대병원 본관",
    boardedAt = LocalTime.of(8, 29),
    eta = LocalTime.of(9, 12),
    remainDistanceMeters = 14_600,
    remainMinutes = 43,
    cabinTemperatureCelsius = 24f,
    musicVolume = 4,
    passengerBackrestAngle = 110,
)

@Preview(name = "E-1 에스코트 모드", widthDp = 1700, heightDp = 1184)
@Composable
private fun EscortContentPreview() {
    MosimTheme { EscortContent(state = previewState) }
}

@Preview(name = "E-2 자동 조절 알림", widthDp = 1700, heightDp = 1184)
@Composable
private fun EscortContentNoticePreview() {
    MosimTheme {
        EscortContent(
            state = previewState.copy(
                cabinTemperatureCelsius = 23f,
                notice = EscortNotice(
                    id = 1,
                    message = "온도를 24℃ → 23℃로 낮췄어요",
                    detail = EscortMessages.AUTO_ADJUST_DETAIL,
                ),
            ),
        )
    }
}

@Preview(name = "E-1 좁은 창 (내비와 분할)", widthDp = 840, heightDp = 1184)
@Composable
private fun EscortContentCompactPreview() {
    MosimTheme {
        EscortContent(
            state = previewState.copy(
                notice = EscortNotice(
                    id = 1,
                    message = "온도를 24℃ → 23℃로 낮췄어요",
                    detail = EscortMessages.AUTO_ADJUST_DETAIL,
                ),
            ),
        )
    }
}
