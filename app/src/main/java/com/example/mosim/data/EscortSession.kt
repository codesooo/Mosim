package com.example.mosim.data

import com.example.mosim.data.mock.PassengerProfile
import java.time.LocalTime

/**
 * A~G가 같이 보는 이동 한 건의 정보다.
 *
 * 화면마다 따로 들고 있던 값(A에서 고른 동승자, A-3의 픽업지·목적지, C-2에서 적용한 설정,
 * 에스코트 결과)을 한 곳에 둬서 E와 G가 A의 선택을 그대로 쓰게 한다.
 *
 * 왜 object인가: 이미 쓰고 있는 `MockPassengerProfiles`, `EscortDependencies`와 같은 방식이다.
 * 마감까지 DI 프레임워크를 새로 넣지 않기로 했다(AGENTS.md: 라이브러리 임의 추가 금지).
 *
 * **프로세스 범위다.** 앱이 죽으면 값이 사라지고 각 화면의 Mock 기본값으로 돌아간다.
 * 서버 저장이나 영속화는 하지 않는다.
 */
object EscortSession {

    /** A-1에서 고른 동승자. 고르지 않았으면 null이고 E는 Mock 기본 동승자를 쓴다. */
    var passenger: PassengerProfile? = null

    /** A-3에서 정한 픽업지. */
    var pickup: String? = null

    /** A-3에서 정한 목적지. */
    var destination: String? = null

    /** D-1에서 탑승을 확인한 시각. E-1 부제목에 쓴다. */
    var boardedAt: LocalTime? = null

    /** C-2에서 적용한 실내 설정 요약 한 줄. 없으면 표시하지 않는다. */
    var appliedCabin: String? = null

    /** E가 끝날 때 남기는 요약. G-1이 읽는다. 없으면 G-1은 Mock 값을 보여 준다. */
    var summary: EscortSummary? = null

    /** G-2에서 저장을 누르면 true. 저장 결과를 화면에 알리는 용도이며 실제 영속화는 없다. */
    var profileSaved: Boolean = false

    fun reset() {
        passenger = null
        pickup = null
        destination = null
        boardedAt = null
        appliedCabin = null
        summary = null
        profileSaved = false
    }
}

/**
 * 끝난 에스코트 한 건의 요약이다. 값은 전부 E에서 실제로 일어난 일에서 센다.
 * 이동 시간·거리처럼 계산 근거가 없는 값은 담지 않는다(G-1의 Mock 값을 그대로 쓴다).
 */
data class EscortSummary(
    val passengerName: String,
    val destinationName: String,
    /** 경유지로 추가에 성공한 장소 이름. 실패한 요청은 넣지 않는다. */
    val waypointNames: List<String>,
    /** 실내 설정을 실제로 바꾼 횟수(성공만). */
    val autoAdjustCount: Int,
    /** 동승자 음성 요청을 처리한 횟수. */
    val voiceRequestCount: Int,
    /** 하나라도 Mock 소스로 실행했으면 true. 화면에 Mock임을 표시한다. */
    val isSimulated: Boolean,
)
