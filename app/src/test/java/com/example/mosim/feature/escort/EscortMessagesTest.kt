package com.example.mosim.feature.escort

import com.example.mosim.feature.escort.EscortMessages.UNKNOWN_DISPLAY
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class EscortMessagesTest {

    @Test
    fun `E-1 표시 값은 피그마 예시와 같은 형식이다`() {
        assertEquals("09:12", EscortMessages.clock(LocalTime.of(9, 12)))
        assertEquals("남은 거리 14.6km · 43분", EscortMessages.remaining(14_600, 43))
        assertEquals("24℃", EscortMessages.temperatureValue(24f))
        assertEquals("22.5℃", EscortMessages.temperatureValue(22.5f))
        assertEquals("조수석 110°", EscortMessages.backrestValue(110))
        assertEquals("08:29 엄마 탑승 · 운전자 조작 최소화", EscortMessages.escortSubtitle(LocalTime.of(8, 29), "엄마"))
    }

    @Test
    fun `값을 모르면 0이 아니라 모름 표시를 쓴다`() {
        assertEquals(UNKNOWN_DISPLAY, EscortMessages.clock(null))
        assertEquals(UNKNOWN_DISPLAY, EscortMessages.temperatureValue(null))
        assertEquals(UNKNOWN_DISPLAY, EscortMessages.volumeValue(null))
        assertEquals(UNKNOWN_DISPLAY, EscortMessages.backrestValue(null))
        assertEquals("남은 거리 $UNKNOWN_DISPLAY", EscortMessages.remaining(null, null))
        assertEquals("엄마 탑승 · 운전자 조작 최소화", EscortMessages.escortSubtitle(null, "엄마"))
    }

    @Test
    fun `남은 거리와 시간 중 아는 것만 보여 준다`() {
        assertEquals("43분", EscortMessages.remaining(null, 43))
        assertEquals("남은 거리 1.0km", EscortMessages.remaining(1_000, null))
    }

    @Test
    fun `토스트는 안내와 부연을 한 줄로 잇는다`() {
        assertEquals(
            "온도를 24℃ → 23℃로 낮췄어요 · 선호 범위 내 자동 조절",
            EscortMessages.noticeLine("온도를 24℃ → 23℃로 낮췄어요", EscortMessages.AUTO_ADJUST_DETAIL),
        )
        assertEquals("다시 말씀해 주세요", EscortMessages.noticeLine("다시 말씀해 주세요", null))
    }

    @Test
    fun `F-1 표시 값은 피그마 예시와 같은 형식이다`() {
        assertEquals("30분", EscortMessages.minutesValue(30))
        assertEquals(UNKNOWN_DISPLAY, EscortMessages.minutesValue(null))
        assertEquals("경유지 1/2 여유", EscortMessages.waypointSlots(1, 2))
        assertEquals("함께 처리: 음악 볼륨 4 → 3", EscortMessages.bundledActions(listOf(CabinAction.SetMusicVolume(4, 3))))
        assertEquals("휴식 장소 A 경유", EscortMessages.addWaypointButton("휴식 장소 A"))
    }

    @Test
    fun `경유지 자리가 없거나 함께 할 일이 없으면 그렇게 보여 준다`() {
        assertEquals("경유지 자리 없음", EscortMessages.waypointSlots(0, 2))
        assertEquals(null, EscortMessages.bundledActions(emptyList()))
    }

    @Test
    fun `장소 설명은 아는 값만 잇고 1km 미만은 m로 쓴다`() {
        assertEquals("휴게소 · 1.8km", EscortMessages.placeSummary("휴게소", 1_800))
        assertEquals("400m", EscortMessages.placeSummary(null, 400))
        assertEquals(null, EscortMessages.placeSummary(null, null))
    }

    @Test
    fun `받침에 따라 조사를 고른다`() {
        assertEquals("을", EscortMessages.objectParticle("편의점"))
        assertEquals("를", EscortMessages.objectParticle("카페"))
        assertEquals("를", EscortMessages.objectParticle("카페 A"))
        assertEquals("는", EscortMessages.topicParticle("엄마"))
        assertEquals("은", EscortMessages.topicParticle("김 부장님"))
    }
}
