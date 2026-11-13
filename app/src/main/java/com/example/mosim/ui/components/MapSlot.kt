package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

/**
 * 지도가 들어갈 자리 (F-1, F-3 오른쪽 열).
 *
 * 지도 렌더링과 NaviHelper는 다른 담당자 몫이라 여기서는 **자리만** 잡는다.
 * [content]가 들어오면 그대로 그리고, 없으면 같은 모양의 빈 판만 둔다.
 *
 * 빈 판에 "연결 예정" 같은 개발용 문구를 쓰지 않는 이유: 운전자에게는 쓸모없는 말이고
 * 시연 화면이 미완성으로 보인다. 연결 여부는 [debugLabel]로만, 그것도 debug 진입점에서만 보여 준다.
 * 가짜 경로나 가짜 지도를 그리지 않는다.
 */
@Composable
fun MapSlot(
    modifier: Modifier = Modifier,
    debugLabel: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier.background(PleosColor.SurfaceHigh, MosimShape.Card),
        contentAlignment = Alignment.Center,
    ) {
        when {
            content != null -> content()
            debugLabel != null -> Text(
                text = debugLabel,
                style = PleosTypography.BodySmall,
                color = PleosColor.TextDarkQuaternary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(MosimDimens.CardPadding),
            )
            // 운영 화면에서는 아무 글자도 두지 않는다.
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

@Preview(name = "MapSlot", widthDp = 700, heightDp = 900, showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun MapSlotPreview() {
    MosimTheme {
        Box(Modifier.padding(24.dp)) { MapSlot(Modifier.fillMaxSize()) }
    }
}
