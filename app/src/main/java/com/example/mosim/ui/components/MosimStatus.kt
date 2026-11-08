package com.example.mosim.ui.components

import androidx.compose.ui.graphics.Color
import com.example.mosim.ui.theme.MosimColor

/**
 * 판단 결과 단계. 판단 배너와 상태 칩이 같은 색 규칙을 쓴다.
 *
 * 예) 주행 가능 거리 < 예정 이동 거리 → Caution "픽업 전 충전이 필요합니다"
 */
enum class MosimStatus(
    val accent: Color,
    val container: Color,
    val content: Color,
) {
    Normal(MosimColor.NormalAccent, MosimColor.NormalContainer, MosimColor.NormalContent),
    Caution(MosimColor.CautionAccent, MosimColor.CautionContainer, MosimColor.CautionContent),
    Warning(MosimColor.WarningAccent, MosimColor.WarningContainer, MosimColor.WarningContent),
}
