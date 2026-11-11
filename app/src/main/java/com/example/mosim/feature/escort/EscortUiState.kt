package com.example.mosim.feature.escort

import com.example.mosim.data.place.PlaceCandidate
import java.time.LocalTime

/**
 * E-1 · E-2 · F-1 세 화면이 함께 보는 상태다.
 * 값을 모르면 null이다. null을 0이나 기본값으로 바꿔 보여 주지 않는다(sdk-status.md Mock 작성 기준).
 */
data class EscortUiState(
    val passengerName: String,
    val destinationName: String,
    val boardedAt: LocalTime?,
    val eta: LocalTime?,
    val remainDistanceMeters: Int?,
    val remainMinutes: Int?,
    val cabinTemperatureCelsius: Float?,
    val musicVolume: Int?,
    val passengerBackrestAngle: Int?,
    /** E-2 토스트. 일정 시간 뒤 null로 돌아간다. */
    val notice: EscortNotice? = null,
    /** null이 아니면 F-1을 보여 준다. */
    val proposal: ReplanProposal? = null,
    /** 실행한 행동 기록. G-1 도착 요약("자동 조정 n회")에 쓴다. */
    val history: List<EscortActionRecord> = emptyList(),
)

/**
 * 상단 토스트 한 건. [id]는 새 알림이 이전 알림의 자동 숨김에 지워지지 않게 구분하는 값이다.
 */
data class EscortNotice(
    val id: Long,
    val message: String,
    val detail: String?,
)

data class ReplanProposal(
    val reason: ReplanReason,
    val message: String,
    val place: PlaceCandidate,
    val freeWaypointSlots: Int,
    val bundledActions: List<CabinAction>,
    /** 경유 요청을 보내는 중. 버튼을 막는다. */
    val isSubmitting: Boolean = false,
    /** 경유 요청 실패 안내. F-1에 보여 준다. */
    val errorMessage: String? = null,
) {
    val canAddWaypoint: Boolean get() = freeWaypointSlots > 0 && !isSubmitting
}

sealed interface ReplanReason {
    /** 정체로 도착이 늦어졌다. F-1 상단 비교 카드에 쓴다. */
    data class Delay(
        val plannedEta: LocalTime,
        val delayedEta: LocalTime,
        val plannedMinutes: Int?,
        val delayedMinutes: Int?,
    ) : ReplanReason

    /** 동승자가 음성으로 장소를 요청했다. */
    data class VoiceRequest(val spokenText: String) : ReplanReason
}

/** 제안 승인과 함께 실행할 실내 설정. */
sealed interface CabinAction {
    data class SetMusicVolume(val from: Int, val to: Int) : CabinAction
}

data class EscortActionRecord(
    val description: String,
    val outcome: ActionOutcome,
    /** Mock 소스로 실행했으면 true. 실제 차량에서 한 일과 구분한다(project-status.md 안전 규칙). */
    val isSimulated: Boolean,
)

enum class ActionOutcome { SUCCEEDED, FAILED, SKIPPED }
