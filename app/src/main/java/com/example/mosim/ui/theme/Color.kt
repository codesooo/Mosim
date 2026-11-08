package com.example.mosim.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 색 토큰.
 *
 * 출처: 피그마 Pleos_Connect_design_assets_215
 *  - PleosColor: "✦ Foundation > Colors" 페이지의 Pleos 공식 토큰 (라이트 모드 값만)
 *  - MosimColor: "모심 화면 v2"에서 토큰 없이 직접 넣은 값
 *
 * 이름은 피그마 변수명을 그대로 옮겼다. 예) text/dark/tertiary → PleosColor.TextDarkTertiary
 * 그래야 피그마에서 요소를 선택했을 때 나오는 이름으로 코드를 바로 찾을 수 있다.
 * 다크 모드는 이번 범위에서 제외했다(팀 결정, 2026-11-08).
 */
object PleosColor {

    // ── Basic Color: Grayscale (라이트 값) ──
    val Basic00 = Color(0xFFFFFFFF)
    val Basic50 = Color(0xFFF7F8FA)
    val Basic100 = Color(0xFFEDEEF2)
    val Basic200 = Color(0xFFE0E1E6)
    val Basic300 = Color(0xFFC7C9CE)
    val Basic400 = Color(0xFFA4A8AE)
    val Basic500 = Color(0xFF686A72)
    val Basic600 = Color(0xFF44464E)
    val Basic700 = Color(0xFF313236)
    val Basic800 = Color(0xFF1D1E21)
    val Basic900 = Color(0xFF131417)

    // Pleos의 반투명·static 색은 모두 이 색에 투명도를 준 것이다.
    private val Ink = Basic900
    private val White = Basic00

    // ── Background ──
    val BackgroundPrimary = Basic50
    val BackgroundSecondary = Basic00
    val BackgroundPopup = Basic00

    // ── Surface (카드 등) ──
    val SurfaceBasic = Basic00
    val SurfaceLow = Basic50
    val SurfaceHigh = Basic100
    val SurfaceAccent = Basic200

    // ── Text ──
    val TextDarkPrimary = Ink
    val TextDarkSecondary = Ink.copy(alpha = 0.84f)
    val TextDarkTertiary = Ink.copy(alpha = 0.64f)
    val TextDarkQuaternary = Ink.copy(alpha = 0.32f)

    // 진한 배경(filled 버튼 등) 위 글자
    val TextLightPrimary = White
    val TextLightSecondary = White.copy(alpha = 0.84f)
    val TextLightTertiary = White.copy(alpha = 0.64f)
    val TextLightQuaternary = White.copy(alpha = 0.32f)

    // ── Divider ──
    val DividerPrimary = Ink.copy(alpha = 0.05f)
    val DividerSecondary = Ink.copy(alpha = 0.10f)
    val DividerTertiary = Ink.copy(alpha = 0.20f)

    // ── Button ──
    // Pleos 이름 기준: basic = 회색 버튼(컴포넌트 Type "Primary"), filled = 진한 버튼(Type "Secondary")
    val ButtonBasicEnabled = Ink.copy(alpha = 0.05f)
    val ButtonBasicPressed = Ink.copy(alpha = 0.10f)

    // [확인 필요] 토큰표에는 alpha_light_200으로만 적혀 있다. 흰색 20%로 해석했다.
    val ButtonBasicDisabled = White.copy(alpha = 0.20f)
    val ButtonFilledEnabled = Basic600
    val ButtonFilledPressed = Basic700
    val ButtonFilledDisabled = Ink.copy(alpha = 0.05f)

    // ── Controller (세그먼트 탭 등) ──
    val ControllerTabBackground = Ink.copy(alpha = 0.05f)

    // ── Switch ──
    val SwitchOn = Color(0xFF02C265)
    val SwitchOff = Ink.copy(alpha = 0.30f)
    val SwitchDisabled = Ink.copy(alpha = 0.10f)
    val SwitchKnob = Basic00

    // ── Informative ──
    val InformativeActive = Color(0xFF02C265)
    val InformativePositive = Color(0xFF0064FF)
    val InformativeNegative = Color(0xFFFE3D16)
    val InformativeAutonomousDriving = Color(0xFF5A46FA)

    // ── Shadow ──
    // Shadow/Light/Level1: x 0, y 6, blur 24, 검정 8%
    val ShadowLevel1 = Color(0xFF000000).copy(alpha = 0.08f)
}

/**
 * 모심 화면 v2에만 있는 색. Pleos 팔레트에 없는 값이라 따로 둔다.
 * 상태 색은 판단 배너와 상태 칩이 같이 쓴다.
 */
object MosimColor {

    // 주의: B-1 "픽업 전 충전이 필요합니다" 배너, "부족" 칩
    val CautionAccent = Color(0xFFFF8A00)
    val CautionContainer = CautionAccent.copy(alpha = 0.12f)
    val CautionChipContainer = CautionAccent.copy(alpha = 0.14f)
    val CautionContent = Color(0xFFC46A00)

    // 정상: B-1 "8분 여유" 칩
    val NormalAccent = Color(0xFF00BA13)
    val NormalContainer = NormalAccent.copy(alpha = 0.12f)
    val NormalContent = Color(0xFF008A0E)

    // 경고: v2 화면에는 아직 예시가 없다.
    // [확인 필요] Pleos informative_negative를 주의 색과 같은 규칙(배경 12%)으로 썼다. 디자인이 나오면 교체.
    val WarningAccent = PleosColor.InformativeNegative
    val WarningContainer = WarningAccent.copy(alpha = 0.12f)
    val WarningContent = Color(0xFFC42E0F)

    // 카드 안 정보 행 사이 구분선 (Pleos divider 토큰보다 옅은 8%)
    val RowDivider = PleosColor.Basic900.copy(alpha = 0.08f)
}
