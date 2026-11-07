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
    fun 메인에서_차량_상태_화면으로_이동한다() {
        composeRule.onNodeWithText("차량 상태").performClick()
        composeRule.onNodeWithText("Mock 데이터다. 실제 차량 값이 아니다.").assertIsDisplayed()
    }
}
