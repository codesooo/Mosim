package com.example.mosim.feature.escort.command

import com.example.mosim.feature.escort.command.VoiceCommandParser.BACKREST_STEP_DEGREES
import com.example.mosim.feature.escort.command.VoiceCommandParser.DEFAULT_TEMPERATURE_STEP_CELSIUS
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {

    private fun parse(text: String) = VoiceCommandParser.parse(text)

    // 온도

    @Test
    fun `덥다고 하면 기본 폭만큼 온도를 내린다`() {
        val expected = VoiceCommand.AdjustTemperature(-DEFAULT_TEMPERATURE_STEP_CELSIUS)
        assertEquals(expected, parse("좀 덥다"))
        assertEquals(expected, parse("너무 더워"))
        assertEquals(expected, parse("시원하게 해줘"))
    }

    @Test
    fun `춥다고 하면 기본 폭만큼 온도를 올린다`() {
        val expected = VoiceCommand.AdjustTemperature(DEFAULT_TEMPERATURE_STEP_CELSIUS)
        assertEquals(expected, parse("좀 춥네"))
        assertEquals(expected, parse("추워요"))
        assertEquals(expected, parse("따뜻하게 해줘"))
    }

    @Test
    fun `온도라는 말과 올림 내림 표현이 있으면 기본 폭으로 조절한다`() {
        assertEquals(VoiceCommand.AdjustTemperature(-DEFAULT_TEMPERATURE_STEP_CELSIUS), parse("온도 좀 낮춰줘"))
        assertEquals(VoiceCommand.AdjustTemperature(DEFAULT_TEMPERATURE_STEP_CELSIUS), parse("에어컨 온도 올려"))
    }

    @Test
    fun `숫자 뒤에 로가 붙으면 목표 온도다`() {
        assertEquals(VoiceCommand.SetTemperature(23f), parse("23도로 맞춰줘"))
        assertEquals(VoiceCommand.SetTemperature(22.5f), parse("22.5도로 해줘"))
        assertEquals(VoiceCommand.SetTemperature(20f), parse("20 도 으로"))
    }

    @Test
    fun `숫자와 올림 내림 표현이 있으면 그만큼 바꾼다`() {
        assertEquals(VoiceCommand.AdjustTemperature(-2f), parse("2도 낮춰줘"))
        assertEquals(VoiceCommand.AdjustTemperature(1f), parse("1도만 올려줘"))
    }

    @Test
    fun `숫자만 있으면 목표 온도로 본다`() {
        assertEquals(VoiceCommand.SetTemperature(24f), parse("에어컨 24도"))
    }

    @Test
    fun `범위 밖 숫자도 그대로 넘긴다 범위 제한은 실행 정책이 한다`() {
        assertEquals(VoiceCommand.SetTemperature(40f), parse("40도로 맞춰"))
    }

    // 음량

    @Test
    fun `시끄럽거나 줄이라고 하면 음량을 한 단계 낮춘다`() {
        val expected = VoiceCommand.AdjustVolume(-1)
        assertEquals(expected, parse("좀 시끄러워"))
        assertEquals(expected, parse("소리 줄여줘"))
        assertEquals(expected, parse("볼륨 낮춰"))
    }

    @Test
    fun `안 들리거나 키우라고 하면 음량을 한 단계 올린다`() {
        val expected = VoiceCommand.AdjustVolume(1)
        assertEquals(expected, parse("잘 안 들려"))
        assertEquals(expected, parse("소리 키워줘"))
    }

    @Test
    fun `볼륨 올려는 온도가 아니라 음량이다`() {
        assertEquals(VoiceCommand.AdjustVolume(1), parse("볼륨 올려"))
    }

    // 등받이

    @Test
    fun `의자를 젖히라고 하면 등받이를 뒤로 한다`() {
        assertEquals(VoiceCommand.AdjustBackrest(BACKREST_STEP_DEGREES), parse("의자 좀 젖혀줘"))
        assertEquals(VoiceCommand.AdjustBackrest(BACKREST_STEP_DEGREES), parse("등받이 뒤로"))
    }

    @Test
    fun `의자를 세우라고 하면 등받이를 앞으로 한다`() {
        assertEquals(VoiceCommand.AdjustBackrest(-BACKREST_STEP_DEGREES), parse("등받이 세워줘"))
    }

    @Test
    fun `의자라는 말 없이 앞으로만 말하면 등받이 명령이 아니다`() {
        assertEquals(VoiceCommand.Unknown("앞으로 얼마나 남았어"), parse("앞으로 얼마나 남았어"))
    }

    // 장소

    @Test
    fun `장소 종류별 표현을 구분한다`() {
        assertEquals(VoiceCommand.FindPlace(PlaceKind.CAFE), parse("근처 카페 가자"))
        assertEquals(VoiceCommand.FindPlace(PlaceKind.CAFE), parse("커피 마시고 싶어"))
        assertEquals(VoiceCommand.FindPlace(PlaceKind.RESTROOM), parse("화장실 가고 싶어"))
        assertEquals(VoiceCommand.FindPlace(PlaceKind.CONVENIENCE_STORE), parse("편의점 들르자"))
        assertEquals(VoiceCommand.FindPlace(PlaceKind.EV_CHARGER), parse("충전소 찾아줘"))
        assertEquals(VoiceCommand.FindPlace(PlaceKind.REST_AREA), parse("좀 쉬었다 가자"))
    }

    @Test
    fun `두 장소가 같이 나오면 더 구체적인 화장실을 고른다`() {
        assertEquals(VoiceCommand.FindPlace(PlaceKind.RESTROOM), parse("휴게소 화장실 가자"))
    }

    @Test
    fun `장소 요청과 다른 조정이 섞이면 장소를 먼저 본다`() {
        assertEquals(VoiceCommand.FindPlace(PlaceKind.CAFE), parse("더운데 카페 가자"))
    }

    // 미인식

    @Test
    fun `규칙에 없는 문장은 원문을 담아 Unknown으로 돌려준다`() {
        assertEquals(VoiceCommand.Unknown("오늘 날씨 어때"), parse("오늘 날씨 어때"))
    }

    @Test
    fun `빈 문장은 Unknown이다`() {
        assertEquals(VoiceCommand.Unknown("  "), parse("  "))
    }
}
