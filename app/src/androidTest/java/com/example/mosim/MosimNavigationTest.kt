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
}
