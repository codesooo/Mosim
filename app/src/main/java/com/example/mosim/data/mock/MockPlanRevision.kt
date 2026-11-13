package com.example.mosim.data.mock

import com.example.mosim.feature.planrevision.ExtraTime
import com.example.mosim.feature.planrevision.PlanSuggestion
import com.example.mosim.feature.planrevision.SuggestedPlace

/**
 * Mock: F-1 AI 제안. 외부 AI 모듈이 붙기 전까지 쓰는 고정값이다.
 *
 * 기획 시나리오(엄마 · 장거리 이동 중 휴식 선호)를 그대로 옮겼다.
 * 시간 값은 Mock이라 실제 경로 계산 결과가 아니다.
 */
fun mockPlanSuggestion() = PlanSuggestion(
    id = "mock-rest-stop-1",
    passengerName = "엄마",
    isMock = true,
    headline = "잠시 쉬어갈까요?",
    reason = "엄마의 장거리 이동 선호를 반영해 휴식 장소를 추천해요.",
    place = SuggestedPlace(
        name = "덕평 휴게소",
        highlight = "지금 경로에서 거의 벗어나지 않아요",
    ),
    arrival = "09:42",
    extra = ExtraTime(text = "+30분", includesDetour = true, includesStay = true),
    speech = "장거리 이동 중 잠시 쉬어가는 건 어떨까요? " +
        "덕평 휴게소를 경유하면 도착이 9시 42분, 약 30분이 추가됩니다.",
)

/** Mock: 시간 계산 결과가 아직 없을 때. 화면이 시간 칸을 어떻게 비우는지 보려고 쓴다. */
fun mockPlanSuggestionWithoutTimes() = mockPlanSuggestion().copy(
    id = "mock-rest-stop-no-times",
    arrival = null,
    extra = null,
    speech = "장거리 이동 중 잠시 쉬어가는 건 어떨까요? 덕평 휴게소를 경유할 수 있어요.",
)
