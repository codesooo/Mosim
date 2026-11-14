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

    @Test
    fun 메인에서_출발_전_점검_화면으로_이동한다() {
        composeRule.onNodeWithText("출발 전 점검").performClick()
        composeRule.onNodeWithText("픽업 전 충전이 필요합니다").assertIsDisplayed()
    }

    @Test
    fun 메인에서_AI_제안_화면으로_이동한다() {
        composeRule.onNodeWithText("AI 제안").performClick()
        composeRule.onNodeWithText("이동 계획 수정").assertIsDisplayed()
    }

    @Test
    fun 메인에서_에스코트_모드로_이동하면_Mock_동승자와_도착_시각이_보인다() {
        composeRule.onNodeWithText("에스코트 모드").performClick()
        composeRule.onNodeWithText("08:29 엄마 탑승 · 운전자 조작 최소화").assertIsDisplayed()
        composeRule.onNodeWithText("09:12").assertIsDisplayed()
        composeRule.onNodeWithText("동승자 음성 요청 대기 중").assertIsDisplayed()
    }
}
