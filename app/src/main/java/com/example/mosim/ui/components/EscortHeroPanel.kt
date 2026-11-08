package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * 주행 중(에스코트 모드)에 쓰는 컴포넌트. 운전자가 한눈에 읽어야 해서 글자가 크고 버튼이 없다.
 * 설계 원칙: 탑승 이후 운전자 화면 조작 최소화.
 */

/**
 * E-1 왼쪽 큰 패널 "escort main": 누구를 어디로 모시는 중인지 + 예상 도착.
 */
@Composable
fun EscortHeroPanel(
    passengerName: String,
    destination: String,
    eta: String,
    remaining: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroLabel("모시는 중")
        Text(text = passengerName, style = MosimTypography.HeroName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = destination, style = MosimTypography.HeroSub, color = PleosColor.TextDarkTertiary, maxLines = 1)

        Spacer(modifier = Modifier.weight(1f))

        HeroLabel("예상 도착")
        Text(text = eta, style = MosimTypography.HeroEta, maxLines = 1)
        Text(text = remaining, style = MosimTypography.HeroSub, color = PleosColor.TextDarkTertiary, maxLines = 1)
        if (footer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            footer()
        }
    }
}

@Composable
private fun HeroLabel(text: String) {
    Text(text = text, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
}

/**
 * 진한 알약 모양 상태 표시 (E-1 "voice standby": ● 동승자 음성 요청 대기 중).
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = MosimColor.NormalAccent,
) {
    Row(
        modifier = modifier
            .background(PleosColor.ButtonFilledEnabled, MosimShape.Card)
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(dotColor, MosimShape.Pill),
        )
        Text(
            text = text,
            style = PleosTypography.BodyMedium.strong(),
            color = PleosColor.TextLightPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 지도 자리 (F-3 오른쪽 "Placeholder"). 지도 연동 전까지 회색 판과 설명 문구만 보여 준다.
 */
@Composable
fun MapPlaceholder(
    modifier: Modifier = Modifier,
    text: String = "지도 영역",
) {
    Box(
        modifier = modifier.background(PleosColor.SurfaceHigh, MosimShape.Card),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
    }
}

@Preview(name = "Escort", widthDp = 1700, heightDp = 1080, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun EscortPreview() {
    MosimTheme {
        Row(
            modifier = Modifier.padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // Mock: E-1 예시 값
            EscortHeroPanel(
                passengerName = "엄마",
                destination = "서울대병원 본관",
                eta = "09:12",
                remaining = "남은 거리 14.6km · 43분",
                modifier = Modifier
                    .width(1060.dp)
                    .height(976.dp),
            )
            Column(
                modifier = Modifier.width(520.dp).height(976.dp),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                CompactInfoCard(label = "실내 온도", value = "24℃")
                MapPlaceholder(modifier = Modifier.fillMaxWidth().weight(1f), text = "지도 · 현재 위치")
                StatusPill(text = "동승자 음성 요청 대기 중", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
