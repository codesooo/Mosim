package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

/**
 * 앱의 판단 결과를 한 줄로 알려 주는 배너 (B-1 "judgement banner").
 * 예) "픽업 전 충전이 필요합니다" + [충전소 보기]
 */
@Composable
fun JudgmentBanner(
    status: MosimStatus,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(status.container, MosimShape.Card)
            .padding(
                horizontal = MosimDimens.BannerPaddingHorizontal,
                vertical = MosimDimens.BannerPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // [확인 필요] 점 크기는 E-1 상태 알약의 점(24)과 같다고 보았다.
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(status.accent, MosimShape.Pill),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MosimTypography.BannerTitle)
            if (description != null) {
                Text(text = description, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
            }
        }
        if (actionText != null && onAction != null) {
            PleosButton(
                text = actionText,
                onClick = onAction,
                type = PleosButtonType.Filled,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}

@Preview(name = "JudgmentBanner", widthDp = 1700, heightDp = 640, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun JudgmentBannerPreview() {
    MosimTheme {
        Column(
            modifier = Modifier.padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            JudgmentBanner(
                status = MosimStatus.Caution,
                title = "픽업 전 충전이 필요합니다",
                description = "목적지 도착 예상 잔량 4% · 기준 15% 미만 (전체 이동 22.8km)",
                actionText = "충전소 보기",
                onAction = {},
            )
            JudgmentBanner(
                status = MosimStatus.Normal,
                title = "바로 출발해도 됩니다",
                description = "Mock 예시 문구",
            )
            JudgmentBanner(
                status = MosimStatus.Warning,
                title = "차량 경고등을 확인하세요",
                description = "Mock 예시 문구 · 경고 색은 확인 필요",
            )
        }
    }
}
