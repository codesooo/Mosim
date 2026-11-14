package com.example.mosim.feature.route

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.*
import com.example.mosim.ui.theme.*

private data class Place(val name: String, val address: String, val distance: String)

// 화면 확인용 예시 주소와 거리.
private val places = listOf(
    Place("강남역 2번 출구", "서울 강남구 강남대로 396", "1.2km"),
    Place("강남역 4번 출구", "서울 강남구 강남대로 지하 396", "1.3km"),
    Place("강남역 11번 출구", "서울 강남구 테헤란로 101", "1.4km"),
    Place("강남역 신분당선 4번 출구", "서울 강남구 강남대로 지하", "1.5km"),
    Place("서울대병원 본관", "서울 종로구 대학로", "8.4 km"),
    Place("서울역", "서울 용산구 한강대로", "6.1 km"),
)

@Composable
fun PlaceSearchScreen(onBack: () -> Unit, onSelect: (String) -> Unit, isPickup: Boolean = true) {
    var query by rememberSaveable(isPickup) { mutableStateOf(if (isPickup) "강남역" else "") }
    val results = places.filter { it.name.contains(query.trim(), true) || it.address.contains(query.trim(), true) }
    MosimAppContainer(title = "장소 검색", subtitle = if (isPickup) "픽업 위치" else "목적지", onBack = onBack) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
            Column(
                Modifier.weight(760f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 100.dp)
                        .background(PleosColor.SurfaceBasic, MosimShape.Field)
                        .border(3.dp, PleosColor.TextDarkPrimary, MosimShape.Field)
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = PleosTypography.BodyLarge.strong(),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "장소명 또는 주소" },
                        decorationBox = { input ->
                            Box {
                                if (query.isEmpty()) Text("장소명 또는 주소", style = PleosTypography.BodyLarge, color = PleosColor.TextDarkTertiary)
                                input()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        Text("지우기", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary,
                            modifier = Modifier.clickable(role = Role.Button) { query = "" }.padding(vertical = 20.dp))
                    }
                }
                Column(Modifier.fillMaxSize().background(PleosColor.SurfaceBasic, MosimShape.Card)
                    .verticalScroll(rememberScrollState()).padding(MosimDimens.CardPadding)) {
                    Text("검색 결과", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
                    if (results.isEmpty()) Text("검색 결과가 없습니다.", modifier = Modifier.padding(vertical = 20.dp))
                    results.forEachIndexed { index, place ->
                        if (index > 0) HorizontalDivider(thickness = MosimDimens.RowDividerThickness, color = MosimColor.RowDivider)
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(place.name) }.padding(vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(place.name, style = PleosTypography.BodyLarge.strong())
                                Text(place.address, style = MosimTypography.Caption, color = PleosColor.TextDarkTertiary)
                            }
                            Text(place.distance, style = PleosTypography.BodySmall)
                        }
                    }
                }
            }
            MapPlaceholder(Modifier.weight(820f).fillMaxHeight(), "지도 · 선택한 장소 위치")
        }
    }
}

@Preview(widthDp = 1700, heightDp = 1184, showBackground = true)
@Composable
private fun PlaceSearchScreenPreview() {
    MosimTheme { PlaceSearchScreen(onBack = {}, onSelect = {}) }
}
