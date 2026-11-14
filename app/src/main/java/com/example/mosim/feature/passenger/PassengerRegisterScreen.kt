package com.example.mosim.feature.passenger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.mosim.data.mock.MockPassengerProfiles
import com.example.mosim.data.mock.PassengerProfile
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.MosimTextField
import com.example.mosim.ui.components.ToggleRow
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.components.pleos.PleosSegmentedControl
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

@Composable
fun PassengerRegisterScreen(
    onBack: () -> Unit,
    onRegister: (PassengerProfile) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var relationshipIndex by rememberSaveable { mutableStateOf(0) }
    var seat by rememberSaveable { mutableStateOf("조수석") }
    // Mock: Figma의 초기 스위치 상태. 등록을 눌러도 실제 차량 설정은 바꾸지 않는다.
    var savePreferences by rememberSaveable { mutableStateOf(true) }
    var applyBeforeBoarding by rememberSaveable { mutableStateOf(true) }
    var adjustDuringTrip by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val seatFocusRequester = remember { FocusRequester() }

    MosimAppContainer(
        title = "신규 동승자 등록",
        subtitle = "처음에는 기본값으로 시작하고, 이동할수록 개인화됩니다",
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            InfoCard("기본 정보", modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                ) {
                    Spacer(Modifier.height(MosimDimens.CardGap))
                    MosimTextField(
                        label = "이름 · 호칭 (필수)", value = name, onValueChange = { name = it },
                        placeholder = "예: 엄마, 김 부장님",
                    )
                    Text("관계", style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
                    PleosSegmentedControl(
                        items = MockPassengerProfiles.relationships,
                        selectedIndex = relationshipIndex,
                        onSelect = { relationshipIndex = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MosimTextField(
                        label = "좌석 위치 (필수)", value = seat, onValueChange = { seat = it },
                        modifier = Modifier.focusRequester(seatFocusRequester),
                        trailing = {
                            Text(
                                "변경", style = PleosTypography.BodySmall,
                                color = PleosColor.TextDarkTertiary,
                                modifier = Modifier.clickable(role = Role.Button) { seatFocusRequester.requestFocus() },
                            )
                        },
                    )
                }
            }
            InfoCard("동의 설정", modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ToggleRow(
                        "선호도 저장", "이동 중 바뀐 설정을 프로필에 기록",
                        savePreferences, { savePreferences = it },
                    )
                    ToggleRow(
                        "탑승 전 자동 적용", "픽업지 정차 후 공조·좌석·음악 준비",
                        applyBeforeBoarding, { applyBeforeBoarding = it },
                    )
                    ToggleRow(
                        "이동 중 자동 조절", "허용 범위 안에서 AI가 직접 조정",
                        adjustDuringTrip, { adjustDuringTrip = it },
                    )
                }
                Spacer(Modifier.height(MosimDimens.CardGap))
                Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
                    PleosButton("취소", onClick = onBack, modifier = Modifier.weight(1f))
                    PleosButton(
                        "등록",
                        onClick = {
                            if (!submitted && name.isNotBlank() && seat.isNotBlank()) {
                                submitted = true
                                onRegister(MockPassengerProfiles.createProfile(
                                    name, MockPassengerProfiles.relationships[relationshipIndex], seat,
                                    savePreferences, applyBeforeBoarding, adjustDuringTrip,
                                ))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        type = PleosButtonType.Filled,
                        enabled = name.isNotBlank() && seat.isNotBlank() && !submitted,
                    )
                }
            }
        }
    }
}

@Preview(name = "A-2 신규 동승자 등록", widthDp = 1700, heightDp = 1184, showBackground = true)
@Composable
private fun PassengerRegisterScreenPreview() {
    MosimTheme { PassengerRegisterScreen(onBack = {}, onRegister = {}) }
}
