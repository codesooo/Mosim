package com.example.mosim.feature.route

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.example.mosim.ui.components.pleos.PleosButtonType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MapPlaceholder
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.theme.*

@Composable
fun RouteRecommendScreen(
    onBack: () -> Unit,
    subtitle: String? = null,
    pickup: String = "강남역 2번 출구",
    destination: String = "서울대병원 본관",
    onEditPickup: () -> Unit = {},
    onEditDestination: () -> Unit = {},
    /** 출발 전 점검(B-1)으로 간다. 연결되지 않은 Preview에서는 null이라 안내만 띄운다. */
    onStartCheck: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    fun showNotice(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    MosimAppContainer(title = "이동 설정", subtitle = subtitle, onBack = onBack) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
            InfoCard(
                title = "어디서 어디로 모시나요?",
                modifier = Modifier.weight(700f).fillMaxHeight(),
            ) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SettingRow("픽업 위치", pickup, onEditPickup)
                    SettingRow("목적지", destination, onEditDestination)
                    SettingRow("약속 시간", "오늘 08:30") { showNotice("약속 시간 변경은 준비 중입니다.") }
                }
                Spacer(Modifier.height(24.dp))
                PleosButton(
                    text = "출발 전 점검",
                    onClick = onStartCheck ?: { showNotice("출발 전 점검 화면은 준비 중입니다.") },
                    type = PleosButtonType.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MapPlaceholder(Modifier.weight(880f).fillMaxHeight(), "경로 미리보기 · 현재 위치 → 픽업 → 목적지")
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onEdit: () -> Unit) {
    Spacer(Modifier.height(MosimDimens.CardGap))
    Row(
        Modifier.fillMaxWidth().background(PleosColor.BackgroundPrimary, MosimShape.Field)
            .clickable(role = Role.Button, onClick = onEdit)
            .heightIn(min = 116.dp).padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
            Text(value, style = PleosTypography.BodyLarge.strong())
        }
        Text("변경", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
    }
}

@Preview(widthDp = 1700, heightDp = 1184, showBackground = true)
@Composable
private fun RouteRecommendScreenPreview() {
    MosimTheme { RouteRecommendScreen(onBack = {}, subtitle = "엄마 · 개인화 12회") }
}
