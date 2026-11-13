package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.pleos.PleosSwitch
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * 목록 한 줄짜리 컴포넌트 두 개. 글자 구성(제목 32 Bold + 태그 + 설명 24)이 같아서 한 파일에 뒀다.
 *  - NumberedOptionItem: F-3 경유지 후보 "1 카페 A [드라이브스루] / 0.4km · 경유 +3분"
 *  - ToggleRow:          G-2 프로필 업데이트 "실내 온도 [반복 · 3회째] / 24℃ → 23℃ … [스위치]"
 */

/**
 * 번호가 붙은 선택지. 동승자가 "1번으로 가줘"처럼 번호로 말할 수 있게 번호를 크게 보여 준다.
 *
 * F-3은 후보 중 하나를 골라 두고 버튼으로 승인하는 흐름이라 [selected]로 골라진 줄을 표시한다.
 * 피그마 F-3에서 고른 번호만 진한 원이고 나머지는 연한 원이다.
 */
@Composable
fun NumberedOptionItem(
    number: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    selected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .rowDivider()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (selected) PleosColor.ButtonFilledEnabled else PleosColor.Basic400,
                    MosimShape.Pill,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = PleosTypography.BodySmall.strong(), color = PleosColor.TextLightPrimary)
        }
        OptionTexts(title = title, tag = tag, subtitle = subtitle, modifier = Modifier.weight(1f))
    }
}

/**
 * 스위치가 붙은 한 줄. 차량에서 누르기 쉽게 스위치만이 아니라 줄 전체를 누를 수 있다.
 */
@Composable
fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .rowDivider()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OptionTexts(title = title, tag = tag, subtitle = description, modifier = Modifier.weight(1f))
        PleosSwitch(checked = checked, onCheckedChange = null)
    }
}

// [확인 필요] G-2 글자 크기는 직접 재지 않았다. 모양이 같은 F-3 값(32 / 22 / 24)을 썼다.
@Composable
private fun OptionTexts(
    title: String,
    tag: String?,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = PleosTypography.TitleSmall.strong(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tag != null) {
                StatusChip(text = tag, tone = ChipTone.Dark, size = ChipSize.Small)
            }
        }
        Text(text = subtitle, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
    }
}

private fun Modifier.rowDivider() = drawBehind {
    val stroke = MosimDimens.RowDividerThickness.toPx()
    val y = size.height - stroke / 2
    drawLine(MosimColor.RowDivider, Offset(0f, y), Offset(size.width, y), stroke)
}

@Preview(name = "OptionRows", widthDp = 800, heightDp = 720, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun OptionRowsPreview() {
    MosimTheme {
        Column(modifier = Modifier.padding(40.dp).width(700.dp)) {
            // Mock: F-3 경유지 후보
            NumberedOptionItem(1, "카페 A", "0.4km · 경유 +3분", onClick = {}, tag = "드라이브스루")
            NumberedOptionItem(2, "카페 B", "0.7km · 경유 +5분", onClick = {}, tag = "주차 가능")
            NumberedOptionItem(3, "카페 C", "0.9km · 경유 +6분", onClick = {})

            // Mock: G-2 프로필 업데이트
            var temp by remember { mutableStateOf(true) }
            var seat by remember { mutableStateOf(false) }
            ToggleRow("실내 온도", "24℃ → 23℃ (외부 7℃)", temp, { temp = it }, tag = "반복 · 3회째")
            ToggleRow("시트 각도", "110° → 112°", seat, { seat = it }, tag = "처음 변경")
        }
    }
}
