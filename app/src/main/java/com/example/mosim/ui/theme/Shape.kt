package com.example.mosim.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * 모서리 둥글기. 출처: 모심 화면 v2 요소 속성.
 * Pleos 컴포넌트는 변수명만 보이고 숫자가 안 보이는 경우가 있다. 그런 값은 피그마 100% 화면에서 잰 값이다.
 */
object MosimShape {
    // 카드, 판단 배너
    val Card = RoundedCornerShape(32.dp)

    // 입력 필드, 카드 안 회색 박스 (A-2 Field/이름 · 호칭)
    val Field = RoundedCornerShape(24.dp)

    // 상태 칩
    val Chip = RoundedCornerShape(12.dp)

    // Pleos 버튼 Large. 피그마 100% 화면에서 잰 값(약 24).
    val ButtonLarge = RoundedCornerShape(24.dp)

    // Pleos 세그먼트 컨트롤 (ivi_segmented_control/solid, Large)
    // [확인 필요] 변수명만 보여서 화면에서 잰 값이다.
    val SegmentContainer = RoundedCornerShape(24.dp)
    val SegmentItem = RoundedCornerShape(18.dp)

    // 스위치, 원형 요소
    val Pill = RoundedCornerShape(percent = 50)
}
