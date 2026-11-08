package com.example.mosim.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * 라이트 테마만 둔다(팀 결정, 2026-11-08). 시스템 다크 모드여도 라이트로 그린다.
 *
 * Material 색 슬롯은 Material 기본 컴포넌트가 엉뚱한 보라색으로 나오지 않게 하려는 최소 연결이다.
 * 모심 화면 코드는 MaterialTheme.colorScheme 대신 PleosColor / MosimColor를 직접 쓴다.
 */
private val MosimLightColors = lightColorScheme(
    primary = PleosColor.ButtonFilledEnabled,
    onPrimary = PleosColor.TextLightPrimary,
    secondary = PleosColor.ButtonFilledEnabled,
    onSecondary = PleosColor.TextLightPrimary,
    background = PleosColor.BackgroundPrimary,
    onBackground = PleosColor.TextDarkPrimary,
    surface = PleosColor.SurfaceBasic,
    onSurface = PleosColor.TextDarkPrimary,
    onSurfaceVariant = PleosColor.TextDarkTertiary,
    outline = PleosColor.DividerTertiary,
    outlineVariant = PleosColor.DividerSecondary,
    error = PleosColor.InformativeNegative,
)

@Composable
fun MosimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MosimLightColors,
        typography = MosimMaterialTypography,
        content = content,
    )
}

// ── 토큰 확인용 Preview. 피그마 Foundation과 눈으로 비교할 때 쓴다. ──

@Preview(name = "Theme tokens", widthDp = 1200, heightDp = 900, showBackground = true)
@Composable
private fun ThemeTokensPreview() {
    MosimTheme {
        Column(
            modifier = Modifier
                .background(PleosColor.BackgroundPrimary)
                .padding(MosimDimens.AppPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Title Large Strong 40/52", style = PleosTypography.TitleLarge.strong())
            Text("Title Medium Strong 36/44", style = PleosTypography.TitleMedium.strong())
            Text("Body Large Strong 30/38", style = PleosTypography.BodyLarge.strong())
            Text("Body Small 26/34 · 엄마 · 강남역 2번 출구", style = PleosTypography.BodySmall)
            Text("픽업 전 충전이 필요합니다", style = MosimTypography.BannerTitle)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    PleosColor.BackgroundPrimary,
                    PleosColor.SurfaceBasic,
                    PleosColor.ButtonBasicEnabled,
                    PleosColor.ButtonFilledEnabled,
                    PleosColor.TextDarkPrimary,
                    PleosColor.TextDarkTertiary,
                    PleosColor.SwitchOn,
                    MosimColor.CautionAccent,
                    MosimColor.NormalAccent,
                    MosimColor.WarningAccent,
                ).forEach { ColorSwatch(it) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(PleosColor.SurfaceBasic, MosimShape.Chip)
            .padding(4.dp)
            .background(color, MosimShape.Chip),
    )
}
