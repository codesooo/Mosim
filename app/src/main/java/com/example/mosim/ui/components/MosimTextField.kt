package com.example.mosim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

/**
 * 라벨이 안쪽 위에 붙은 회색 입력 칸 (A-2 "Field/이름 · 호칭").
 * Pleos 컴포넌트가 아니라 모심 화면에서 직접 그린 박스다.
 *
 * 주의: 입력 칸이 있는 화면은 키보드가 뜨면 화면이 위로 밀리는 것이 Pleos 사양이다(06 문서 8절).
 * 주행 중에는 키보드 입력이 제한되므로 정차 중 화면(A-2, A-4)에서만 쓴다.
 */
@Composable
fun MosimTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PleosColor.BackgroundPrimary, MosimShape.Field)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = PleosTypography.BodyLarge,
                cursorBrush = SolidColor(PleosColor.TextDarkPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = PleosTypography.BodyLarge, color = PleosColor.TextDarkQuaternary)
                    }
                    innerTextField()
                },
            )
        }
        if (trailing != null) trailing()
    }
}

@Preview(name = "MosimTextField", widthDp = 940, heightDp = 340, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MosimTextFieldPreview() {
    MosimTheme {
        Column(
            modifier = Modifier.padding(40.dp).width(860.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            var name by remember { mutableStateOf("") }
            MosimTextField(label = "이름 · 호칭", value = name, onValueChange = { name = it }, placeholder = "예: 엄마, 김 부장님")
            MosimTextField(
                label = "좌석 위치",
                value = "조수석",
                onValueChange = {},
                trailing = { Text("변경", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary) },
            )
        }
    }
}
