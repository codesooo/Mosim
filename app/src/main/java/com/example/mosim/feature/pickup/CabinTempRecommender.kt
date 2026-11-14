package com.example.mosim.feature.pickup

import kotlin.math.roundToLong

/*
 * 픽업 전 실내 온도 추천 (C-2 "적용 온도").
 *
 * 공식(project-status.md 2장, 초안): 추천온도 = 과거 설정온도 − k × (현재 외기 − 과거 외기)
 * 밖이 더 따뜻해지면 조금 낮추고, 더 추워지면 조금 올린다.
 * 설정 온도를 저장할 때 당시 외기를 같이 저장하는 이유가 이 계산 때문이다.
 */
object CabinTempRule {
    // [확인 필요] k값은 미결(project-status.md 6장). 초안 값 0.1을 쓴다.
    const val K = 0.1

    // 공식 초안의 제한 범위
    const val MIN_C = 18.0
    const val MAX_C = 28.0

    // Vehicle SDK 목표 온도 단위(sdk-status.md 4장 Virtual Console: 0.5°C 단위)
    const val STEP_C = 0.5
}

/** 추천 결과. [adjusted]가 false면 저장값을 그대로 쓴 것이다. */
data class CabinTempRecommendation(
    val temp: Double,
    val adjusted: Boolean,
)

fun recommendCabinTemp(
    savedSetTemp: Double,
    savedOutsideTemp: Double,
    currentOutsideTemp: Double,
): CabinTempRecommendation {
    val raw = savedSetTemp - CabinTempRule.K * (currentOutsideTemp - savedOutsideTemp)
    // 차량에 0.5 단위로만 설정할 수 있어서 반올림한 뒤 범위를 자른다.
    val stepped = (raw / CabinTempRule.STEP_C).roundToLong() * CabinTempRule.STEP_C
    val temp = stepped.coerceIn(CabinTempRule.MIN_C, CabinTempRule.MAX_C)
    return CabinTempRecommendation(temp = temp, adjusted = temp != savedSetTemp)
}

/** 24.0 → "24℃", 23.5 → "23.5℃" */
fun formatTemp(celsius: Double): String =
    if (celsius % 1.0 == 0.0) "${celsius.toLong()}℃" else "${celsius}℃"
