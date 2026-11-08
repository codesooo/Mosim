package com.example.mosim.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * 모서리 둥글기. 출처: 모심 화면 v2 (B-1 점검 결과) 요소 속성.
 */
object MosimShape {
    // 카드, 판단 배너
    val Card = RoundedCornerShape(32.dp)

    // 상태 칩
    val Chip = RoundedCornerShape(12.dp)

    // Pleos 버튼 Large (buttons/basic/large/radius)
    // [확인 필요] 피그마에 변수명만 보이고 숫자가 안 보인다. 화면에서 잰 값이다. 3단계에서 Components 페이지로 다시 확인.
    val ButtonLarge = RoundedCornerShape(24.dp)

    // 세그먼트 탭, 원형 요소
    val Pill = RoundedCornerShape(percent = 50)
}
