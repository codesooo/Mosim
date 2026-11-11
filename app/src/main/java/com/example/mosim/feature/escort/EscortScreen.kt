package com.example.mosim.feature.escort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
 * [확인 필요] 에스코트 중 Back을 최상단 규칙(onBack = null, 앱 숨김)으로 할지. 지금은 임시 메인으로 돌아가도록 onBack을 받는다.
 */
@Composable
fun EscortScreen(
    viewModel: EscortViewModel,
    onBack: (() -> Unit)?,
) {
    val state by viewModel.uiState.collectAsState()
    EscortContent(state = state, onBack = onBack)
}

@Composable
fun EscortContent(
    state: EscortUiState,
    onBack: (() -> Unit)? = null,
) {
    MosimAppContainer(
        title = "에스코트 모드",
        subtitle = EscortMessages.escortSubtitle(state.boardedAt, state.passengerName),
        onBack = onBack,
        headerTrailing = {
            // notice가 null이 된 뒤 사라지는 동안에도 마지막 문구를 보여 주려고 기억해 둔다.
            var lastNoticeText by remember { mutableStateOf("") }
            state.notice?.let { lastNoticeText = EscortMessages.noticeLine(it.message, it.detail) }
            AnimatedVisibility(visible = state.notice != null, enter = fadeIn(), exit = fadeOut()) {
                AutoAdjustToast(text = lastNoticeText)
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            EscortHeroPanel(
                passengerName = state.passengerName,
                destination = state.destinationName,
                eta = EscortMessages.clock(state.eta),
                remaining = EscortMessages.remaining(state.remainDistanceMeters, state.remainMinutes),
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
                CompactInfoCard(label = "실내 온도", value = EscortMessages.temperatureValue(state.cabinTemperatureCelsius))
                CompactInfoCard(label = "음악", value = EscortMessages.volumeValue(state.musicVolume))
                CompactInfoCard(label = "좌석", value = EscortMessages.backrestValue(state.passengerBackrestAngle))
                Spacer(modifier = Modifier.weight(1f))
                // [확인 필요] 실제 듣기 상태는 STT 담당과 합의 후 반영한다(docs/design/escort.md 11장 #1).
                StatusPill(text = "동승자 음성 요청 대기 중", modifier = Modifier.fillMaxWidth())
            }
        }
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
