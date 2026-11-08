package com.example.mosim.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/**
 * 모든 모심 화면의 바깥 틀 (피그마 mosim_app_content).
 *
 * 구성: 제목·부제 → 본문(남은 높이를 채움) → 오른쪽 정렬 하단 버튼.
 * 피그마 v2에는 뒤로가기 버튼이 있는 앱 바가 없다. 뒤로가기는 CCP Back 하드키(= Android Back)로만 처리한다.
 *
 * Back 규칙 (Pleos 06 문서 "CCP Back 하드키 → 앱 숨김 처리", 심사 필수)
 *  - onBack == null : 최상단 화면이다. 앱을 끝내지 않고 moveTaskToBack(true)로 숨긴다.
 *  - onBack != null : 하위 화면이다. 이전 화면으로 간다.
 */
@Composable
fun MosimAppContainer(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    bottomActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    BackHandler {
        if (onBack != null) onBack() else context.findActivity()?.moveTaskToBack(true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PleosColor.BackgroundPrimary)
            .padding(MosimDimens.AppPadding),
        verticalArrangement = Arrangement.spacedBy(MosimDimens.SectionGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = PleosTypography.TitleLarge.strong())
            if (subtitle != null) {
                Text(text = subtitle, style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content,
        )

        if (bottomActions != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap, alignment = Alignment.End),
                content = bottomActions,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "MosimAppContainer", widthDp = 1700, heightDp = 1184)
@Composable
private fun MosimAppContainerPreview() {
    MosimTheme {
        MosimAppContainer(
            title = "출발 전 점검",
            subtitle = "엄마 · 강남역 2번 출구 08:30 → 서울대병원",
            bottomActions = {
                PleosButton(text = "다시 점검", onClick = {}, modifier = Modifier.width(280.dp))
                PleosButton(
                    text = "에스코트 계획 보기",
                    onClick = {},
                    type = PleosButtonType.Filled,
                    modifier = Modifier.width(400.dp),
                )
            },
        ) {
            Text("본문 영역", style = PleosTypography.BodyLarge)
        }
    }
}
