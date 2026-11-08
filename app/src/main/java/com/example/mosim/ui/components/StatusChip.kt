package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** 칩 색 종류. 상태(Normal/Caution/Warning)와 정보용(Neutral/Dark)을 같이 둔다. */
enum class ChipTone(val container: Color, val content: Color) {
    Neutral(PleosColor.ButtonBasicEnabled, PleosColor.TextDarkPrimary), // A-1 "가족", "Default"
    Dark(PleosColor.ButtonFilledEnabled, PleosColor.TextLightPrimary), // A-1 "개인화 12회", G-2 "반복 · 3회째"
    Normal(MosimColor.NormalContainer, MosimColor.NormalContent), // B-1 "8분 여유"
    Caution(MosimColor.CautionChipContainer, MosimColor.CautionContent), // B-1 "부족", "짐 있음 추정"
    Warning(MosimColor.WarningContainer, MosimColor.WarningContent),
}

/** 칩 크기. Medium = 카드 안 값 옆(26), Small = 프로필·목록 안 태그(22). */
enum class ChipSize { Medium, Small }

@Composable
fun StatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.Medium,
) {
    Text(
        text = text,
        style = if (size == ChipSize.Medium) PleosTypography.BodySmall.strong() else MosimTypography.CaptionStrong,
        color = tone.content,
        maxLines = 1,
        modifier = modifier
            .background(tone.container, MosimShape.Chip)
            .padding(
                horizontal = MosimDimens.ChipPaddingHorizontal,
                vertical = MosimDimens.ChipPaddingVertical,
            ),
    )
}

/** MosimStatus로 바로 칩을 고를 때 쓴다. */
fun MosimStatus.toChipTone(): ChipTone = when (this) {
    MosimStatus.Normal -> ChipTone.Normal
    MosimStatus.Caution -> ChipTone.Caution
    MosimStatus.Warning -> ChipTone.Warning
}

@Preview(name = "StatusChip", widthDp = 900, heightDp = 200, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StatusChipPreview() {
    MosimTheme {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusChip("부족", ChipTone.Caution)
                StatusChip("8분 여유", ChipTone.Normal)
                StatusChip("짐 있음 추정", ChipTone.Caution)
                StatusChip("경고", ChipTone.Warning)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("가족", ChipTone.Neutral, size = ChipSize.Small)
                StatusChip("개인화 12회", ChipTone.Dark, size = ChipSize.Small)
                StatusChip("Default", ChipTone.Neutral, size = ChipSize.Small)
            }
        }
    }
}
