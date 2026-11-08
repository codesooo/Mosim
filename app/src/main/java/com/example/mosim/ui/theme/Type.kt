package com.example.mosim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mosim.R

/*
 * 글꼴 토큰.
 *
 * 출처: 피그마 "✦ Foundation > Typography"
 * Connect-S-17 디스플레이가 160 DPI라서 피그마 px 값을 sp로 그대로 쓴다.
 *
 * Pleos 굵기 이름 ↔ Asta Sans 굵기
 *  - Normal       → 400 Regular
 *  - Strong       → 700 Bold
 *  - Extra_strong → 800 ExtraBold
 */
val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_bold, FontWeight.Bold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold),
)

private fun pleosStyle(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = PleosColor.TextDarkPrimary,
)

/** 기본은 Normal 굵기다. 굵게는 `.strong()`, `.extraStrong()`을 붙인다. */
object PleosTypography {
    val HeadlineLarge = pleosStyle(60, 80)
    val HeadlineMedium = pleosStyle(56, 72)
    val HeadlineSmall = pleosStyle(48, 64)

    val TitleLarge = pleosStyle(40, 52)
    val TitleMedium = pleosStyle(36, 44)
    val TitleSmall = pleosStyle(32, 40)

    val BodyLarge = pleosStyle(30, 38)
    val BodyMedium = pleosStyle(28, 36)
    val BodySmall = pleosStyle(26, 34)

    val LabelMedium = pleosStyle(24, 28)
    val LabelSmall = pleosStyle(20, 24)
}

fun TextStyle.strong() = copy(fontWeight = FontWeight.Bold)

fun TextStyle.extraStrong() = copy(fontWeight = FontWeight.ExtraBold)

/** 모심 화면 v2에서 토큰에 없는 크기를 쓴 곳. */
object MosimTypography {
    // B-1 판단 배너 제목 "픽업 전 충전이 필요합니다" (44 Bold, 행간 100%)
    val BannerTitle = TextStyle(
        fontFamily = AstaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        color = PleosColor.TextDarkPrimary,
    )
}

/**
 * Material 컴포넌트(Text 기본값 등)도 Asta Sans를 쓰도록 연결한다.
 * 모심 화면 코드는 MaterialTheme.typography 대신 PleosTypography를 직접 쓴다.
 */
internal val MosimMaterialTypography = Typography(
    headlineLarge = PleosTypography.HeadlineLarge.strong(),
    headlineMedium = PleosTypography.HeadlineMedium.strong(),
    headlineSmall = PleosTypography.HeadlineSmall.strong(),
    titleLarge = PleosTypography.TitleLarge.strong(),
    titleMedium = PleosTypography.TitleMedium.strong(),
    titleSmall = PleosTypography.TitleSmall.strong(),
    bodyLarge = PleosTypography.BodyLarge,
    bodyMedium = PleosTypography.BodyMedium,
    bodySmall = PleosTypography.BodySmall,
    labelLarge = PleosTypography.TitleSmall.strong(),
    labelMedium = PleosTypography.LabelMedium,
    labelSmall = PleosTypography.LabelSmall,
)
