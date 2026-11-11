package com.example.mosim.feature.escort

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mosim.data.place.PlaceCandidate
import com.example.mosim.platform.pleos.GeoPoint
import com.example.mosim.ui.theme.MosimTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class EscortReplanContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val delayProposal = ReplanProposal(
        reason = ReplanReason.Delay(LocalTime.of(9, 12), LocalTime.of(9, 42), 30, 60),
        message = EscortMessages.restProposal("엄마"),
        place = PlaceCandidate("rest", "휴식 장소 A", "화장실 · 카페", null, GeoPoint(37.58, 126.99), 1_800),
        freeWaypointSlots = 1,
        bundledActions = listOf(CabinAction.SetMusicVolume(4, 3)),
    )

    @Test
    fun 지연_제안은_전후_비교와_함께_처리할_일을_보여_준다() {
        composeRule.setContent { MosimTheme { EscortReplanContent(proposal = delayProposal) } }

        composeRule.onNodeWithText("30분").assertIsDisplayed()
        composeRule.onNodeWithText("60분").assertIsDisplayed()
        composeRule.onNodeWithText("09:42").assertIsDisplayed()
        composeRule.onNodeWithText("경유지 1/2 여유").assertIsDisplayed()
        composeRule.onNodeWithText("함께 처리: 음악 볼륨 4 → 3").assertIsDisplayed()
        composeRule.onNodeWithText("휴식 장소 A 경유").assertIsEnabled()
    }

    @Test
    fun 버튼을_누르면_각각의_콜백을_부른다() {
        var kept = 0
        var added = 0
        composeRule.setContent {
            MosimTheme {
                EscortReplanContent(proposal = delayProposal, onKeepRoute = { kept++ }, onAddWaypoint = { added++ })
            }
        }

        composeRule.onNodeWithText("기존 경로 유지").performClick()
        composeRule.onNodeWithText("휴식 장소 A 경유").performClick()

        assertEquals(1, kept)
        assertEquals(1, added)
    }

    @Test
    fun 경유지_자리가_없으면_경유_버튼이_꺼진다() {
        composeRule.setContent { MosimTheme { EscortReplanContent(proposal = delayProposal.copy(freeWaypointSlots = 0)) } }

        composeRule.onNodeWithText("경유지 자리 없음").assertIsDisplayed()
        composeRule.onNodeWithText("휴식 장소 A 경유").assertIsNotEnabled()
    }

    @Test
    fun 음성_요청은_동승자_말과_실패_안내를_보여_준다() {
        val voice = delayProposal.copy(
            reason = ReplanReason.VoiceRequest("카페 들렀다 가자"),
            bundledActions = emptyList(),
            errorMessage = "경로에 경유지를 추가하지 못했어요",
        )
        composeRule.setContent { MosimTheme { EscortReplanContent(proposal = voice) } }

        composeRule.onNodeWithText("“카페 들렀다 가자”").assertIsDisplayed()
        composeRule.onNodeWithText("경로에 경유지를 추가하지 못했어요").assertIsDisplayed()
    }
}
