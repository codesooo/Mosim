package com.example.mosim.ui.components.pleos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor

private val SwitchWidth = 80.dp
private val SwitchHeight = 48.dp
private val SwitchPadding = 4.dp
private val KnobSize = SwitchHeight - SwitchPadding * 2

/**
 * Pleos 스위치 (ivi_switch, Medium 80×48). G-2 프로필 업데이트 저장 항목에 쓴다.
 *
 * 차량에서 누르기 쉽게 스위치만이 아니라 줄 전체를 누르게 하고 싶으면
 * 이 컴포넌트 대신 줄 쪽에 toggleable을 걸고 여기에는 onCheckedChange = null을 넘긴다.
 */
@Composable
fun PleosSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> PleosColor.SwitchDisabled
            checked -> PleosColor.SwitchOn
            else -> PleosColor.SwitchOff
        },
        label = "trackColor",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) SwitchWidth - SwitchPadding * 2 - KnobSize else 0.dp,
        label = "knobOffset",
    )

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleModifier)
            .size(SwitchWidth, SwitchHeight)
            .background(trackColor, MosimShape.Pill)
            .padding(SwitchPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(KnobSize)
                // Shadow/Light/Level1 근사
                .shadow(elevation = 4.dp, shape = MosimShape.Pill)
                .background(PleosColor.SwitchKnob, MosimShape.Pill),
        )
    }
}

@Preview(name = "PleosSwitch", widthDp = 400, heightDp = 120, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PleosSwitchPreview() {
    MosimTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PleosSwitch(checked = true, onCheckedChange = {})
            PleosSwitch(checked = false, onCheckedChange = {})
            PleosSwitch(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}
