package com.example.mosim.ui.components.pleos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

private val SegmentedHeight = 100.dp
private val SegmentedPadding = 12.dp
private val SeparatorHeight = 32.dp

/**
 * Pleos 세그먼트 컨트롤 (ivi_segmented_control/solid, Large).
 * A-1 동승자 관계 탭(전체/가족/상사/지인/기타), A-2 관계 선택에 쓴다.
 *
 * 칸 너비는 항목 수로 똑같이 나눈다. 전체 너비는 호출하는 쪽이 정한다(A-1은 1176).
 */
@Composable
fun PleosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(SegmentedHeight)
            .background(PleosColor.ControllerTabBackground, MosimShape.SegmentContainer)
            .padding(SegmentedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, label ->
            Segment(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
            // 구분선은 선택되지 않은 두 칸 사이에만 보인다.
            val isLast = index == items.lastIndex
            val touchesSelected = index == selectedIndex || index + 1 == selectedIndex
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(SeparatorHeight)
                        .background(if (touchesSelected) Color.Transparent else PleosColor.DividerSecondary),
                )
            }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedModifier = if (selected) {
        // Shadow/Light/Level2 (y 8, blur 32, 16%)를 Compose 그림자로 근사했다.
        Modifier
            .shadow(elevation = 8.dp, shape = MosimShape.SegmentItem, spotColor = Color.Black.copy(alpha = 0.16f))
            .background(PleosColor.SurfaceBasic, MosimShape.SegmentItem)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(selectedModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = PleosTypography.TitleSmall.strong(),
            color = if (selected) PleosColor.TextDarkPrimary else PleosColor.TextDarkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "PleosSegmentedControl", widthDp = 1240, heightDp = 300, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun PleosSegmentedControlPreview() {
    MosimTheme {
        Column(modifier = Modifier.padding(32.dp)) {
            var selected by remember { mutableIntStateOf(0) }
            PleosSegmentedControl(
                items = listOf("전체", "가족", "상사", "지인", "기타"),
                selectedIndex = selected,
                onSelect = { selected = it },
                modifier = Modifier.width(1176.dp),
            )
            PleosSegmentedControl(
                items = listOf("가족", "상사", "지인", "기타"),
                selectedIndex = 2,
                onSelect = {},
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(0.7f),
            )
        }
    }
}
