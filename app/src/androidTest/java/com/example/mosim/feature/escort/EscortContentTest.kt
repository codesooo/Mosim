package com.example.mosim.feature.escort

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mosim.ui.theme.MosimTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class EscortContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = EscortUiState(
        passengerName = "엄마",
        destinationName = "서울대병원 본관",
        boardedAt = LocalTime.of(8, 29),
        eta = LocalTime.of(9, 12),
        remainDistanceMeters = 14_600,
        remainMinutes = 43,
        cabinTemperatureCelsius = 23f,
        musicVolume = 4,
        passengerBackrestAngle = 110,
    )

    @Test
    fun 알림이_있으면_제목_옆에_토스트를_한_줄로_보여_준다() {
        val notice = EscortNotice(1, "온도를 24℃ → 23℃로 낮췄어요", EscortMessages.AUTO_ADJUST_DETAIL)
        composeRule.setContent { MosimTheme { EscortContent(state = state.copy(notice = notice)) } }

        composeRule.onNodeWithText("온도를 24℃ → 23℃로 낮췄어요 · 선호 범위 내 자동 조절").assertIsDisplayed()
        composeRule.onNodeWithText("23℃").assertIsDisplayed()
    }

    @Test
    fun 알림이_없으면_토스트가_없다() {
        composeRule.setContent { MosimTheme { EscortContent(state = state) } }

        composeRule.onNodeWithText("09:12").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("선호 범위 내 자동 조절", substring = true).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun 값을_모르면_모름_표시를_쓴다() {
        val unknown = state.copy(eta = null, cabinTemperatureCelsius = null, musicVolume = null)
        composeRule.setContent { MosimTheme { EscortContent(state = unknown) } }

        assertEquals(3, composeRule.onAllNodesWithText(EscortMessages.UNKNOWN_DISPLAY).fetchSemanticsNodes().size)
    }
}
