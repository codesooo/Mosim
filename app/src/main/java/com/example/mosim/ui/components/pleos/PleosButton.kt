package com.example.mosim.ui.components.pleos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/**
 * Pleos 버튼 종류.
 *
 * 피그마 컴포넌트 속성 Type과 이름이 반대처럼 느껴져서 색 토큰 이름을 따랐다.
 *  - Basic  = 피그마 Type "Primary"   (회색, button/basic/…)   예) "다시 점검", "취소"
 *  - Filled = 피그마 Type "Secondary" (진한 회색, button/filled/…) 예) "에스코트 계획 보기"
 */
enum class PleosButtonType { Basic, Filled }

/**
 * Pleos 버튼 Large (높이 100).
 * 너비는 화면마다 다르다(280, 320, 400). 호출하는 쪽에서 Modifier.width나 weight로 정한다.
 *
 * 눌림 표현은 Material 물결 대신 Pleos 토큰의 pressed 색으로 바꾼다.
 */
@Composable
fun PleosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: PleosButtonType = PleosButtonType.Basic,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val containerColor = when (type) {
        PleosButtonType.Basic -> when {
            !enabled -> PleosColor.ButtonBasicDisabled
            pressed -> PleosColor.ButtonBasicPressed
            else -> PleosColor.ButtonBasicEnabled
        }
        PleosButtonType.Filled -> when {
            !enabled -> PleosColor.ButtonFilledDisabled
            pressed -> PleosColor.ButtonFilledPressed
            else -> PleosColor.ButtonFilledEnabled
        }
    }
    val contentColor = when {
        !enabled -> PleosColor.TextDarkQuaternary
        type == PleosButtonType.Filled -> PleosColor.TextLightPrimary
        else -> PleosColor.TextDarkPrimary
    }

    Box(
        modifier = modifier
            .height(MosimDimens.ButtonLargeHeight)
            // [확인 필요] buttons/basic/large/min_width 숫자가 피그마에서 안 보인다. 가장 좁은 버튼(280)에 맞췄다.
            .defaultMinSize(minWidth = 280.dp)
            .background(containerColor, MosimShape.ButtonLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PleosTypography.TitleSmall.strong(),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "PleosButton", widthDp = 1000, heightDp = 300, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PleosButtonPreview() {
    MosimTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PleosButton(text = "다시 점검", onClick = {}, modifier = Modifier.width(280.dp))
                PleosButton(
                    text = "에스코트 계획 보기",
                    onClick = {},
                    type = PleosButtonType.Filled,
                    modifier = Modifier.width(400.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PleosButton(text = "비활성", onClick = {}, enabled = false, modifier = Modifier.width(280.dp))
                PleosButton(
                    text = "비활성",
                    onClick = {},
                    type = PleosButtonType.Filled,
                    enabled = false,
                    modifier = Modifier.width(280.dp),
                )
            }
        }
    }
}
