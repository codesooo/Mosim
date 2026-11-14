package com.example.mosim

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.mosim.data.mock.mockPlanSuggestion
import com.example.mosim.data.mock.mockPlanSuggestionWithoutTimes
import com.example.mosim.data.mock.mockPlaces
import com.example.mosim.feature.planrevision.*
import com.example.mosim.feature.voice.VoiceContent
import com.example.mosim.feature.voice.VoicePhase
import com.example.mosim.feature.voice.VoiceUiState
import com.example.mosim.feature.waypoint.*
import com.example.mosim.ui.theme.MosimTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun proposalShowsProvidedDataAndEmitsDecisions() {
        var keep = 0
        var request = 0
        compose.setContent {
            MosimTheme {
                PlanRevisionContent(
                    PlanRevisionUiState(mockPlanSuggestion()), {},
                    onKeepRoute = { keep++ }, onRequestWaypoint = { request++ },
                )
            }
        }
        compose.onNodeWithText("잠시 쉬어갈까요?").assertIsDisplayed()
        compose.onNodeWithText("09:42").assertIsDisplayed()
        compose.onNodeWithText("기존 경로 유지").performClick()
        compose.onNodeWithText("경유지 추가").performClick()
        assertEquals(1, keep)
        assertEquals(1, request)
    }

    @Test fun missingTimesAreOmittedAndSpeechFailurePreservesActions() {
        compose.setContent {
            MosimTheme {
                PlanRevisionContent(
                    PlanRevisionUiState(
                        mockPlanSuggestionWithoutTimes(), announcement = AnnouncementState.Failed,
                    ), {},
                )
            }
        }
        compose.onNodeWithText("예상 도착").assertDoesNotExist()
        compose.onNodeWithText("추가 소요").assertDoesNotExist()
        compose.onNodeWithText("음성 안내를 하지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("경유지 추가").assertIsEnabled()
    }

    @Test fun selectedPlaceHasSelectionSemanticsAndConfirmCallback() {
        val places = mockPlaces()
        val state = mutableStateOf(WaypointUiState.Ready(places, places.first()))
        var confirmed = ""
        var backs = 0
        compose.setContent {
            MosimTheme {
                WaypointContent(
                    state.value, { backs++ },
                    onSelect = { state.value = state.value.copy(selected = it) },
                    onConfirm = { confirmed = state.value.selected.externalId },
                )
            }
        }
        compose.onNodeWithText("카페 B").performClick()
        compose.onNode(isSelected()).assertExists()
        compose.onNodeWithText("경유지 추가").performClick()
        assertEquals("mock-cafe-b", confirmed)
        compose.onNodeWithText("취소").performClick()
        assertEquals(1, backs)
    }

    @Test fun searchLoadingHasNoConfirmation() {
        compose.setContent { MosimTheme { WaypointContent(WaypointUiState.Searching, {}) } }
        compose.onNodeWithText("주변 장소를 찾고 있어요").assertIsDisplayed()
        compose.onNodeWithText("경유지 추가").assertIsNotEnabled()
    }

    @Test fun emptySearchHasNoConfirmation() {
        compose.setContent { MosimTheme { WaypointContent(WaypointUiState.Empty, {}) } }
        compose.onNodeWithText("조건에 맞는 장소를 찾지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("경유지 추가").assertIsNotEnabled()
    }

    @Test fun searchFailureOffersRetry() {
        var retries = 0
        compose.setContent {
            MosimTheme { WaypointContent(WaypointUiState.Failed("검색 실패"), {}, onRetry = { retries++ }) }
        }
        compose.onNodeWithText("다시 찾기").performClick()
        assertEquals(1, retries)
    }

    @Test fun analysisWaitIsDistinctFromListening() {
        compose.setContent { MosimTheme { VoiceContent(VoiceUiState(VoicePhase.Analyzing, "커피"), {}) } }
        // 부제는 고정 문구로 바뀔다. 상태 문구는 본문에만 한 번 나온다.
        compose.onNodeWithText("요청을 확인하고 있어요").assertIsDisplayed()
        compose.onNodeWithText("듣고 있어요").assertDoesNotExist()
    }

    @Test fun independentMockFlowsReachTheirOwnRequestStates() {
        compose.setContent { MosimTheme { com.example.mosim.navigation.MosimNavHost(demoFlow = true) } }
        compose.onNodeWithText("AI 제안").performClick()
        compose.onNodeWithText("잠시 쉬어갈까요?").assertIsDisplayed()
        compose.onNodeWithText("기존 경로 유지").performClick()
        compose.onNodeWithText("음성 요청").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("카페 A").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("카페 B").performClick()
        compose.onNodeWithText("경유지 추가").performClick()
        compose.onNodeWithText("경유지 추가는 준비 중이에요").assertIsDisplayed()
    }

    @Test fun restrictedInteractionDisablesWaypointSubmission() {
        compose.setContent {
            MosimTheme {
                PlanRevisionContent(PlanRevisionUiState(mockPlanSuggestion()), {}, interactionAllowed = false)
            }
        }
        compose.onNodeWithText("경유지 추가").assertIsNotEnabled()
        compose.onNodeWithText("기존 경로 유지").assertIsEnabled()
        compose.onNodeWithText("주행 중에는 선택할 수 없어요").assertIsDisplayed()
    }
}
