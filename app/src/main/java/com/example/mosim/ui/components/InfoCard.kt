package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.R
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/**
 * 제목이 있는 흰 카드 (B-1 "차량 상태", "이동 상태", "외부 환경").
 * 안에는 보통 InfoRow를 넣는다.
 */
@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
    ) {
        Text(text = title, style = PleosTypography.BodyLarge.strong())
        content()
    }
}

/**
 * 카드 안 한 줄: 아이콘·라벨은 왼쪽, (칩)·값은 오른쪽 (B-1 "Row/픽업까지").
 * 아이콘은 res/drawable의 ic_* (Lucide)를 painterResource(R.drawable.ic_battery)처럼 넘긴다.
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    chipText: String? = null,
    chipTone: ChipTone = ChipTone.Neutral,
    showDivider: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.bottomDivider() else Modifier)
            .padding(vertical = MosimDimens.RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.IconTextGap),
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = PleosColor.TextDarkTertiary,
                    modifier = Modifier.size(MosimDimens.IconSize),
                )
            }
            Text(
                text = label,
                style = PleosTypography.BodySmall,
                color = PleosColor.TextDarkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (chipText != null) {
                StatusChip(text = chipText, tone = chipTone)
            }
            Text(text = value, style = PleosTypography.TitleMedium.strong(), maxLines = 1)
        }
    }
}

/**
 * 라벨과 값만 있는 작은 카드 (E-1 오른쪽 "실내 온도 24℃", "음악", "좌석").
 */
@Composable
fun CompactInfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(horizontal = MosimDimens.CardPadding, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Text(text = value, style = PleosTypography.TitleMedium.strong(), maxLines = 1)
    }
}

private fun Modifier.bottomDivider() = drawBehind {
    val stroke = MosimDimens.RowDividerThickness.toPx()
    val y = size.height - stroke / 2
    drawLine(
        color = MosimColor.RowDivider,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = stroke,
    )
}

@Preview(name = "InfoCard", widthDp = 1200, heightDp = 760, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun InfoCardPreview() {
    MosimTheme {
        Row(
            modifier = Modifier.padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // Mock 값: 피그마 B-1 예시를 그대로 옮겼다.
            InfoCard(title = "차량 상태", modifier = Modifier.width(484.dp).height(648.dp)) {
                InfoRow(
                    label = "배터리",
                    value = "12%",
                    icon = painterResource(R.drawable.ic_battery),
                    chipText = "부족",
                    chipTone = ChipTone.Caution,
                )
                InfoRow(label = "주행 가능", value = "30km", icon = painterResource(R.drawable.ic_map_pin))
                InfoRow(label = "실내 온도", value = "16℃", icon = painterResource(R.drawable.ic_thermometer))
                InfoRow(
                    label = "동승석",
                    value = "",
                    icon = painterResource(R.drawable.ic_users),
                    chipText = "짐 있음 추정",
                    chipTone = ChipTone.Caution,
                )
            }
            Column(
                modifier = Modifier.width(520.dp),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                CompactInfoCard(label = "실내 온도", value = "24℃")
                CompactInfoCard(label = "음악", value = "조용한 음악 · 4")
                CompactInfoCard(label = "좌석", value = "조수석 110°")
            }
        }
    }
}
