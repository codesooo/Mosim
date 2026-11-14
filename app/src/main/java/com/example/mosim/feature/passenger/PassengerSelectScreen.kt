package com.example.mosim.feature.passenger

import com.example.mosim.data.mock.MockPassengerProfiles
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.ui.components.AddPassengerCard
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.PassengerProfileCard
import com.example.mosim.ui.components.ProfileTag
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.components.pleos.PleosSegmentedControl
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

private val relationships = listOf("전체") + MockPassengerProfiles.relationships

@Composable
fun PassengerSelectScreen(
    onBack: () -> Unit,
    onAddPassenger: () -> Unit,
    onMoveSettings: (String) -> Unit,
    registeredPassengerId: String? = null,
) {
    var relationship by rememberSaveable { mutableStateOf("전체") }
    var selectedId by rememberSaveable { mutableStateOf("mom") }
    val mockPassengers = MockPassengerProfiles.passengers
    LaunchedEffect(registeredPassengerId) {
        if (registeredPassengerId != null && mockPassengers.any { it.id == registeredPassengerId }) {
            relationship = "전체"
            selectedId = registeredPassengerId
        }
    }
    val passengers = mockPassengers.filter { relationship == "전체" || it.relationship == relationship }
    val passenger = passengers.find { it.id == selectedId }

    MosimAppContainer(
        title = "누구를 모시나요?",
        subtitle = "동승자를 선택하면 저장된 선호도로 에스코트를 준비합니다",
        onBack = onBack,
    ) {
        PleosSegmentedControl(
            items = relationships,
            selectedIndex = relationships.indexOf(relationship),
            onSelect = { index ->
                relationship = relationships[index]
                // 필터 밖의 숨은 동승자로 진행하지 않도록 현재 목록 안에서만 선택한다.
                if (mockPassengers.none { it.id == selectedId &&
                        (relationship == "전체" || it.relationship == relationship) }) {
                    selectedId = mockPassengers.firstOrNull {
                        relationship == "전체" || it.relationship == relationship
                    }?.id.orEmpty()
                }
            },
            modifier = Modifier.fillMaxWidth(0.73f),
        )
        Spacer(Modifier.height(MosimDimens.SectionGap))
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            Column(modifier = Modifier.weight(1.8f).fillMaxHeight()) {
                if (passengers.isEmpty()) {
                    Text(
                        text = "등록된 동승자가 없습니다. 신규 등록으로 시작하세요.",
                        style = PleosTypography.BodySmall,
                        color = PleosColor.TextDarkTertiary,
                        modifier = Modifier.padding(bottom = MosimDimens.CardGap),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                    verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                ) {
                    items(passengers, key = { it.id }) { item ->
                        PassengerProfileCard(
                            name = item.name,
                            tags = listOf(
                                ProfileTag(item.relationship),
                                ProfileTag(item.personalization,
                                    if (item.personalization == "Default") ChipTone.Neutral else ChipTone.Dark),
                            ),
                            selected = item == passenger,
                            onClick = { selectedId = item.id },
                            modifier = Modifier.fillMaxWidth().semantics { selected = item == passenger },
                        )
                    }
                    item(key = "add") {
                        AddPassengerCard(
                            onClick = onAddPassenger,
                            modifier = Modifier.fillMaxWidth().heightIn(
                                min = MosimDimens.ButtonLargeHeight * 3,
                            ),
                        )
                    }
                }
            }
            InfoCard(
                title = passenger?.let { "${it.name}의 저장된 선호도" } ?: "저장된 선호도",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (passenger?.temperature != null) {
                        PreferenceRow("선호 온도", passenger.temperature)
                        PreferenceRow("음악", passenger.music.orEmpty())
                        PreferenceRow("좌석", passenger.seat.orEmpty())
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = MosimDimens.RowPaddingVertical),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("자동 적용 동의", style = PleosTypography.LabelMedium,
                                color = PleosColor.TextDarkTertiary)
                            StatusChip(if (passenger.consent) "동의함" else "미동의",
                                tone = if (passenger.consent) ChipTone.Normal else ChipTone.Neutral)
                        }
                    } else {
                        passenger?.seat?.let { PreferenceRow("좌석", it) }
                        if (passenger?.seat != null) {
                            PreferenceRow("자동 적용 동의", if (passenger.consent) "동의함" else "미동의")
                        }
                        Text(
                            text = if (passenger == null) "동승자를 선택해주세요."
                            else "표시할 선호도 데이터가 없습니다.",
                            style = PleosTypography.BodySmall,
                            color = PleosColor.TextDarkTertiary,
                            modifier = Modifier.padding(top = MosimDimens.CardGap),
                        )
                    }
                }
                Spacer(Modifier.height(MosimDimens.CardGap))
                PleosButton(
                    text = "이동 설정",
                    onClick = { passenger?.let { onMoveSettings(it.id) } },
                    enabled = passenger != null,
                    type = PleosButtonType.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// A-1의 긴 선호도 값은 LabelMedium 크기다. 다른 화면의 InfoRow(36sp)는 변경하지 않는다.
@Composable
private fun PreferenceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MosimDimens.RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.IconTextGap),
    ) {
        Text(label, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
        Text(value, style = PleosTypography.LabelMedium.strong(), textAlign = TextAlign.End,
            modifier = Modifier.weight(1f))
    }
    HorizontalDivider(thickness = MosimDimens.RowDividerThickness, color = MosimColor.RowDivider)
}

@Preview(name = "A-1 동승자 선택", widthDp = 1700, heightDp = 1184, showBackground = true)
@Composable
private fun PassengerSelectScreenPreview() {
    MosimTheme { PassengerSelectScreen(onBack = {}, onAddPassenger = {}, onMoveSettings = {}) }
}
