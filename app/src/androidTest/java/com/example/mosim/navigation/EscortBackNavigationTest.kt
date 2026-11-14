package com.example.mosim.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mosim.ui.theme.MosimTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 에스코트 중 Back이 임시 메인으로 돌아가지 않고 앱 숨김(최상단 규칙)으로 가는지 본다.
 *
 * 숨김이 화면에 보이는 모습은 환경마다 다르다. 같은 Pleos 에뮬레이터라도 창 없이 띄우면 앱이 내려가고,
 * -gpu host로 띄우면 덮을 앱이 없어 RESUMED로 남았다(2026-11-12 확인). 그래서 숨김 여부 대신
 * "다시 앞으로 불러오면 같은 에스코트 화면이다"를 본다. 실차 CCP Back 동작은 [확인 필요].
 */
@RunWith(AndroidJUnit4::class)
class EscortBackNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun 에스코트_중_Back으로_숨겼다가_다시_열면_같은_에스코트_화면이다() {
        composeRule.setContent { MosimTheme { MosimNavHost() } }
        composeRule.onNodeWithText("에스코트 모드").performClick()
        composeRule.onNodeWithText("운전자 조작 최소화", substring = true).assertIsDisplayed()

        // Pleos 에뮬레이터는 adb·Espresso의 시스템 Back 키를 앱이 아닌 Driving view로 보낸다.
        // 그래서 CCP Back이 도착하는 곳과 같은 Back 디스패처에 직접 넣는다.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertFalse(composeRule.activity.isFinishing)

        bringActivityToFront()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("운전자 조작 최소화", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("운전자 조작 최소화", substring = true).assertIsDisplayed()
    }

    /** 숨기지 않은 환경에서는 아무 일도 일어나지 않는다. 숨긴 환경에서는 같은 활동이 앞으로 온다. */
    private fun bringActivityToFront() {
        val activity = composeRule.activity
        val intent = Intent(activity, activity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.applicationContext.startActivity(intent)
    }
}
