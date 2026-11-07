package com.example.mosim.feature.passenger

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.ui.MosimScreenScaffold
import com.example.mosim.ui.theme.MosimTheme

@Composable
fun PassengerSelectScreen(onBack: () -> Unit) {
    MosimScreenScaffold(title = "동승자 선택", onBack = onBack) {
        Text("함께 탈 동승자를 고르는 화면이다. 후보 목록과 선택 규칙은 미정이다.")
    }
}

@Preview(showBackground = true)
@Composable
private fun PassengerSelectScreenPreview() {
    MosimTheme { PassengerSelectScreen(onBack = {}) }
}
