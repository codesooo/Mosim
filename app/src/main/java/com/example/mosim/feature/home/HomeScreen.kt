package com.example.mosim.feature.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.navigation.MosimRoute
import com.example.mosim.ui.MosimScreenScaffold
import com.example.mosim.ui.theme.MosimTheme

private val menus = listOf(
    MosimRoute.PASSENGER to "동승자 선택",
    MosimRoute.ROUTE to "경로 추천",
    MosimRoute.VEHICLE to "차량 상태",
    MosimRoute.PICKUP to "픽업 안내",
    // A·B 화면이 없어서 C-1부터 시작하는 임시 입구. A-1~B-3이 연결되면 뺀다.
    MosimRoute.PICKUP_EN_ROUTE to "C-1 픽업지 이동 중",
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    MosimScreenScaffold(title = "MOSIM") {
        Text("개발용 임시 메인 화면이다. 와이어프레임 확정 후 교체한다.")
        Spacer(Modifier.height(16.dp))
        menus.forEach { (route, label) ->
            Button(
                onClick = { onNavigate(route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MosimTheme { HomeScreen(onNavigate = {}) }
}
