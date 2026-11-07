package com.example.mosim.feature.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.ui.MosimScreenScaffold
import com.example.mosim.ui.theme.MosimTheme

@Composable
fun RouteRecommendScreen(onBack: () -> Unit) {
    MosimScreenScaffold(title = "경로 추천", onBack = onBack) {
        Text("출발지와 목적지를 받아 경로를 제안하는 화면이다. 추천 기준은 미정이다.")
    }
}

@Preview(showBackground = true)
@Composable
private fun RouteRecommendScreenPreview() {
    MosimTheme { RouteRecommendScreen(onBack = {}) }
}
