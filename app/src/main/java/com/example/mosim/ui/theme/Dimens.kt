package com.example.mosim.ui.theme

import androidx.compose.ui.unit.dp

/*
 * 간격·크기 토큰. 출처: 모심 화면 v2 요소 속성 (피그마 px = dp, 160 DPI 기준).
 */
object MosimDimens {

    // ── 앱 영역 (mosim_app_content) ──
    // Connect-S-17 landscape 기준 크기. Preview 크기로만 쓰고 실제 레이아웃은 화면을 채운다.
    val AppContentWidth = 1700.dp
    val AppContentHeight = 1184.dp
    val AppPadding = 48.dp
    val SectionGap = 32.dp

    // ── 카드 ──
    val CardPadding = 40.dp
    val CardGap = 24.dp

    // ── 판단 배너 ──
    val BannerPaddingHorizontal = 40.dp
    val BannerPaddingVertical = 32.dp

    // ── 정보 행 (카드 안 라벨 / 값) ──
    val RowPaddingVertical = 22.dp
    val RowDividerThickness = 2.dp
    val IconSize = 28.dp
    val IconTextGap = 16.dp

    // ── 상태 칩 ──
    val ChipPaddingHorizontal = 16.dp
    val ChipPaddingVertical = 8.dp

    // ── 버튼 (Pleos Large) ──
    val ButtonLargeHeight = 100.dp

    // 넓은 버튼 너비. 피그마 모심 화면 v2 기준 400 (B-1 "에스코트 계획 보기").
    val ButtonWideWidth = 400.dp
}
