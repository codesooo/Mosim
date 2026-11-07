package com.example.mosim.feature.pickup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.ui.MosimScreenScaffold
import com.example.mosim.ui.theme.MosimTheme

@Composable
fun PickupGuideScreen(onBack: () -> Unit) {
    MosimScreenScaffold(title = "픽업 안내", onBack = onBack) {
        Text("픽업 지점과 도착 예정 정보를 보여주는 화면이다. 데이터 출처는 미정이다.")
    }
}

@Preview(showBackground = true)
@Composable
private fun PickupGuideScreenPreview() {
    MosimTheme { PickupGuideScreen(onBack = {}) }
}
