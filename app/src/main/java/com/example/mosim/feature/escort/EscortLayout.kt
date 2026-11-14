package com.example.mosim.feature.escort

/**
 * E-1을 넓은 창(앱 영역 전체)과 좁은 창(내비와 나눈 반쪽) 중 어느 배치로 그릴지 정한다.
 *
 * 기준 크기는 Pleos 에뮬레이터에서 확인했다.
 * - 앱 영역 전체: 1682dp (Connect-S 17″, 왼쪽 Driving view 853px 제외)
 * - 분할 시 반쪽 창: 840dp (웹 콘솔 WindowPolicy의 portrait 테스트 화면)
 * 둘 사이인 1200dp를 경계로 둔다. [확인 필요] 실차 분할 창 크기.
 */
enum class EscortLayout {
    WIDE,
    COMPACT,
    ;

    companion object {
        const val COMPACT_MAX_WIDTH_DP = 1200f

        fun of(widthDp: Float): EscortLayout = if (widthDp < COMPACT_MAX_WIDTH_DP) COMPACT else WIDE
    }
}
