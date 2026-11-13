package com.example.mosim.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.navigation.MosimRoute
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme

/*
 * 화면 진입점 모음.
 *
 * 이동 중(F)이 둘로 나뉘는 이유: 시작 조건이 서로 다르고 중간에 만나지 않는다.
 *  - AI 제안  : 차가 먼저 말을 건다. F-1에서 시작해 기존 경로 유지 / 경유 요청으로 끝난다.
 *  - 음성 요청: 동승자가 먼저 말을 건다. F-2 인식 → 외부 분석 → F-3 후보·선택으로 이어진다.
 * 버튼 하나로 합치면 둘 중 하나를 먼저 열 수밖에 없어서, 나머지 하나는 들어갈 길이 없어진다.
 * (F-3은 F-2가 결과를 넘겨줘야 의미가 있으므로 메뉴에 두지 않는다.)
 */
private val menus = listOf(
    MosimRoute.PASSENGER to "동승자 선택",
    MosimRoute.ROUTE to "경로 추천",
    MosimRoute.VEHICLE to "출발 전 점검",
    MosimRoute.PICKUP to "픽업 안내",
    MosimRoute.ARRIVAL to "도착",
    MosimRoute.PLAN_REVISION to "AI 제안",
    MosimRoute.VOICE to "음성 요청",
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    // 최상단 화면이라 onBack을 주지 않는다. CCP Back은 앱 숨김으로 처리된다(MosimAppContainer).
    MosimAppContainer(title = "모심", subtitle = "이동 중 동승자 케어") {
        // 차량 디스플레이는 가로가 넓다. 세로로 쌓지 않고 두 칸씩 채운다.
        Column(verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
            menus.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                ) {
                    row.forEach { (route, label) ->
                        PleosButton(
                            text = label,
                            onClick = { onNavigate(route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // 홀수 개여도 왼쪽 칸 너비가 흔들리지 않게 빈 칸을 채운다.
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(name = "홈", widthDp = 1700, heightDp = 1184)
@Composable
private fun HomeScreenPreview() {
    MosimTheme { HomeScreen(onNavigate = {}) }
}
