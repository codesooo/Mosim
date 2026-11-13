package com.example.mosim

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MosimNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // 메인 메뉴는 F 화면 진입점만 둔다(팀 요청). 다른 담당자 화면은 route로만 들어간다.
    @Test
    fun 메인에서_AI_제안_화면으로_이동한다() {
        composeRule.onNodeWithText("AI 제안").performClick()
        composeRule.onNodeWithText("이동 계획 수정").assertIsDisplayed()
    }
}
