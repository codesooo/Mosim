package com.example.mosim.data.mock

import androidx.compose.runtime.mutableStateListOf
import java.util.UUID

data class PassengerProfile(
    val id: String,
    val name: String,
    val relationship: String,
    val personalization: String = "Default",
    val temperature: String? = null,
    val music: String? = null,
    val seat: String? = null,
    val consent: Boolean = false,
    val savePreferences: Boolean = false,
    val adjustDuringTrip: Boolean = false,
)

// Mock: 앱 프로세스 안에서만 유지한다. 서버 저장과 실제 차량 제어는 하지 않는다.
object MockPassengerProfiles {
    val relationships = listOf("가족", "상사", "지인", "기타")
    val passengers = mutableStateListOf(
        PassengerProfile(
            id = "mom", name = "엄마", relationship = "가족", personalization = "개인화 12회",
            temperature = "24°C (외부 5°C 기준)", music = "조용한 음악 · 낮은 볼륨",
            seat = "조수석 · 110°", consent = true,
        ),
        PassengerProfile("dad", "아빠", "가족", "개인화 3회"),
        PassengerProfile("manager", "김 부장님", "상사", "개인화 5회"),
        PassengerProfile("director", "이 대표님", "기타"),
    )

    fun createProfile(
        name: String,
        relationship: String,
        seat: String,
        savePreferences: Boolean,
        applyBeforeBoarding: Boolean,
        adjustDuringTrip: Boolean,
    ): PassengerProfile {
        require(name.isNotBlank()) { "이름 · 호칭을 입력해주세요." }
        require(relationship in relationships) { "관계를 선택해주세요." }
        require(seat.isNotBlank()) { "좌석 위치를 입력해주세요." }
        return PassengerProfile(
            id = UUID.randomUUID().toString(), name = name.trim(), relationship = relationship,
            seat = seat.trim(), consent = applyBeforeBoarding,
            savePreferences = savePreferences, adjustDuringTrip = adjustDuringTrip,
        )
    }
}
