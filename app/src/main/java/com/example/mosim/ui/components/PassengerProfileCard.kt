package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/** 프로필 카드 아래 태그 하나. 예) ("가족", Neutral), ("개인화 12회", Dark) */
data class ProfileTag(val text: String, val tone: ChipTone = ChipTone.Neutral)

/**
 * 동승자 프로필 카드 (A-1 "passenger/엄마").
 * 선택되면 4dp 진한 테두리가 생긴다. 사진은 아직 없어서 회색 원으로 둔다.
 */
@Composable
fun PassengerProfileCard(
    name: String,
    tags: List<ProfileTag>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MosimShape.Card)
            .background(PleosColor.SurfaceBasic)
            .then(
                if (selected) Modifier.border(4.dp, PleosColor.TextDarkPrimary, MosimShape.Card) else Modifier,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(PleosColor.Basic200, MosimShape.Pill),
        )
        Text(
            text = name,
            style = PleosTypography.TitleSmall.strong(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { StatusChip(text = it.text, tone = it.tone, size = ChipSize.Small) }
        }
    }
}

/**
 * "+ 신규 등록" 점선 카드 (A-1 "add passenger"). 점선 3dp, 길이 12 / 간격 8, 20% 회색.
 */
@Composable
fun AddPassengerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "+ 신규 등록",
    description: String = "기본값으로 시작",
) {
    Column(
        modifier = modifier
            .clip(MosimShape.Card)
            .dashedBorder()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = PleosTypography.BodyLarge.strong())
        Text(text = description, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
    }
}

private fun Modifier.dashedBorder() = drawBehind {
    val strokeWidth = 3.dp.toPx()
    val radius = 32.dp.toPx()
    drawRoundRect(
        color = PleosColor.DividerTertiary,
        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(radius),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())),
        ),
    )
}

@Preview(name = "PassengerProfileCard", widthDp = 1100, heightDp = 400, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun PassengerProfileCardPreview() {
    MosimTheme {
        Row(
            modifier = Modifier.padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // Mock 동승자
            PassengerProfileCard(
                name = "엄마",
                tags = listOf(ProfileTag("가족"), ProfileTag("개인화 12회", ChipTone.Dark)),
                selected = true,
                onClick = {},
                modifier = Modifier.width(317.dp),
            )
            PassengerProfileCard(
                name = "김 부장님",
                tags = listOf(ProfileTag("상사"), ProfileTag("개인화 5회", ChipTone.Dark)),
                selected = false,
                onClick = {},
                modifier = Modifier.width(317.dp),
            )
            AddPassengerCard(
                onClick = {},
                modifier = Modifier
                    .width(317.dp)
                    .height(288.dp),
            )
        }
    }
}
