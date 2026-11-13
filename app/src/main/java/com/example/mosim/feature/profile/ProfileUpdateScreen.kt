package com.example.mosim.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.ToggleRow
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * G-2 프로필 업데이트 (design/screen-G2.png, 이용 흐름 07단계). G-1 "프로필 업데이트 확인"에서 진입한다.
 *
 * 화면에 보이는 값은 전부 아래 Mock 상수다. 바뀐 설정 수집, AI 추천, 프로필 저장은 하지 않는다.
 * 스위치는 화면 안에서만 켜고 끈다. 저장 방식과 저장 위치가 정해지면 교체한다. [확인 필요]
 */

/** 저장 후보 한 줄. recommended는 디자인에서 처음에 켜져 있는 항목이다. */
private data class ProfileChange(
    val title: String,
    val tag: String,
    val tagTone: ChipTone,
    val description: String,
    val recommended: Boolean,
)

// Mock: screen-G2 design example values. 실제 동승자 프로필·차량 설정 기록이 아니다.
private const val MockSubtitle = "엄마 · 이번 이동에서 바뀐 설정"

// Mock: screen-G2 문구. 추천 개수는 스위치 상태로 세지 않고 디자인 값을 그대로 쓴다.
private const val MockRecommendationCount = "AI 추천 2개"

// Mock: screen-G2 저장 후보. 반복 횟수·추천 여부를 판단하지 않고 디자인 값을 그대로 쓴다.
private val MockChanges = listOf(
    ProfileChange("실내 온도", "반복 · 3회째", ChipTone.Dark, "24°C → 23°C (외부 7°C)", recommended = true),
    ProfileChange("음악 볼륨", "반복 · 2회째", ChipTone.Dark, "4 → 3", recommended = true),
    ProfileChange("시트 각도", "처음 변경", ChipTone.Neutral, "110° → 112°", recommended = false),
    ProfileChange("카페 경유", "일회성 요청", ChipTone.Neutral, "카페 A", recommended = false),
)

// Mock: screen-G2 저장 기준 안내 문구.
private const val MockCriteriaTitle = "반복된 변경만 추천하고, 일회성 요청은 기본으로 저장하지 않아요"
private const val MockCriteriaDescription = "온도는 외부 기온과 함께 저장해 계절이 바뀌어도 다시 산출합니다"

/**
 * 선택 항목 저장은 "저장되었습니다." 확인 팝업만 띄운다. 실제 저장은 하지 않는다(Mock).
 * 저장 안 함, 선택 항목 저장 이후 화면과 저장 기능은 정해지지 않아 콜백만 둔다. [확인 필요]
 */
@Composable
fun ProfileUpdateScreen(
    onBack: () -> Unit,
    onSkipSave: () -> Unit,
    onSaveSelected: () -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(MockChanges.map { it.recommended }) }
    var showSavedDialog by rememberSaveable { mutableStateOf(false) }

    // 팝업은 화면 위에 덮어 그린다. 뒤의 G-2 화면은 그대로 보이고 반투명 막으로 구분한다.
    Box(modifier = Modifier.fillMaxSize()) {
        MosimAppContainer(
            title = "프로필 업데이트",
            subtitle = MockSubtitle,
            onBack = onBack,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                // 열 너비 비율은 screen-G2에서 잰 값(약 1140 : 440)이다.
                // InfoCard는 제목 오른쪽에 글자를 둘 수 없어서 같은 배경·여백으로 직접 그린다.
                Column(
                    modifier = Modifier
                        .weight(1140f)
                        .fillMaxHeight()
                        .background(PleosColor.SurfaceBasic, MosimShape.Card)
                        .padding(MosimDimens.CardPadding),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "저장할 항목을 선택하세요",
                            style = PleosTypography.BodyLarge.strong(),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = MockRecommendationCount,
                            style = PleosTypography.LabelMedium.strong(),
                            color = PleosColor.TextDarkTertiary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MockChanges.forEachIndexed { index, change ->
                        ToggleRow(
                            title = change.title,
                            description = change.description,
                            checked = checked[index],
                            onCheckedChange = { value ->
                                checked = checked.toMutableList().also { it[index] = value }
                            },
                            tag = change.tag,
                            tagTone = change.tagTone,
                            showDivider = index != MockChanges.lastIndex,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(440f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
                ) {
                    CriteriaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    PleosButton(text = "저장 안 함", onClick = onSkipSave, modifier = Modifier.fillMaxWidth())
                    PleosButton(
                        text = "선택 항목 저장",
                        onClick = {
                            showSavedDialog = true
                            onSaveSelected()
                        },
                        type = PleosButtonType.Filled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (showSavedDialog) {
            SavedPopup(onDismiss = { showSavedDialog = false })
        }
    }
}

/**
 * 오른쪽 "저장 기준" 메모 카드. 흐린 라벨 + 굵은 제목 + 흐린 설명.
 * B-3 오른쪽 카드(EscortPlanScreen의 LabeledCard)와 틀이 같다. 공통 컴포넌트로 옮길지는 팀 결정이 필요하다. [확인 필요]
 */
@Composable
private fun CriteriaCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(MosimDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "저장 기준", style = PleosTypography.BodySmall, color = PleosColor.TextDarkTertiary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = MockCriteriaTitle, style = PleosTypography.BodySmall.strong())
            Text(
                text = MockCriteriaDescription,
                style = PleosTypography.LabelMedium,
                color = PleosColor.TextDarkTertiary,
            )
        }
    }
}

/**
 * 선택 항목 저장 확인 팝업. 화면 전체를 반투명 막으로 덮고 가운데에 흰 카드를 띄운다.
 * 별도 Dialog 창 대신 화면 위에 그려서, 시스템 dim 설정과 상관없이 배경과 팝업이 구분된다.
 * 바깥을 눌러도 닫히지 않고(뒤 화면 터치도 막는다) "확인"과 Back으로만 닫힌다(Pleos 06: 팝업이 있으면 Back으로 닫기).
 * 피그마 v2에 팝업 디자인이 없어 크기·글자는 기존 토큰으로 정한 임시 값이다. [확인 필요]
 */
@Composable
private fun SavedPopup(onDismiss: () -> Unit) {
    // MosimAppContainer의 BackHandler보다 나중에 등록돼서, 팝업이 떠 있는 동안 Back은 팝업만 닫는다.
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MosimColor.Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(PleosColor.BackgroundPopup, MosimShape.Card)
                .padding(MosimDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MosimDimens.SectionGap),
        ) {
            Text(text = "저장되었습니다.", style = PleosTypography.TitleSmall.strong())
            PleosButton(
                text = "확인",
                onClick = onDismiss,
                type = PleosButtonType.Filled,
                modifier = Modifier.width(MosimDimens.ButtonWideWidth),
            )
        }
    }
}

@Preview(name = "G-2 프로필 업데이트", widthDp = 1700, heightDp = 1184)
@Composable
private fun ProfileUpdateScreenPreview() {
    MosimTheme {
        ProfileUpdateScreen(onBack = {}, onSkipSave = {}, onSaveSelected = {})
    }
}
