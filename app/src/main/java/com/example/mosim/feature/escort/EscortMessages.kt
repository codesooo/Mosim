package com.example.mosim.feature.escort

import com.example.mosim.data.place.PlaceKind
import com.example.mosim.platform.pleos.FailureReason

/**
 * 에스코트 화면에 보여 줄 문구를 만든다.
 * ViewModel에서 판단과 문구 조립을 떼어 두면 문구를 바꿀 때 판단 로직을 건드리지 않는다.
 */
object EscortMessages {

    fun celsius(value: Float): String =
        if (value % 1f == 0f) "${value.toInt()}℃" else "$value℃"

    fun temperatureChanged(from: Float, to: Float): String {
        val verb = if (to < from) "낮췄어요" else "높였어요"
        return "온도를 ${celsius(from)} → ${celsius(to)}로 $verb"
    }

    const val AUTO_ADJUST_DETAIL = "선호 범위 내 자동 조절"

    fun outsidePreferredRange(name: String, range: ClosedFloatingPointRange<Float>?): String =
        if (range == null) {
            "${name}의 자동 조절 동의 범위가 없어 바꾸지 않았어요"
        } else {
            "${name} 선호 범위(${celsius(range.start)}~${celsius(range.endInclusive)}) 밖이라 바꾸지 않았어요"
        }

    fun volumeChanged(from: Int, to: Int): String {
        val verb = if (to < from) "낮췄어요" else "높였어요"
        return "음악 볼륨을 $from → ${to}단계로 $verb"
    }

    fun backrestChanged(from: Int, to: Int): String {
        val verb = if (to > from) "젖혔어요" else "세웠어요"
        return "조수석 등받이를 $from° → $to°로 $verb"
    }

    const val UNKNOWN_VALUE = "현재 값을 알 수 없어 바꾸지 않았어요"
    const val OUT_OF_VEHICLE_RANGE = "차량에서 설정할 수 있는 범위를 벗어나요"
    const val ALREADY_SET = "이미 요청한 값이에요"
    const val NOT_UNDERSTOOD = "다시 말씀해 주세요"
    const val LOCATION_UNAVAILABLE = "현재 위치를 확인하지 못했어요"

    fun placeKindLabel(kind: PlaceKind): String = when (kind) {
        PlaceKind.CAFE -> "카페"
        PlaceKind.RESTROOM -> "화장실"
        PlaceKind.CONVENIENCE_STORE -> "편의점"
        PlaceKind.REST_AREA -> "휴식 장소"
        PlaceKind.EV_CHARGER -> "충전소"
    }

    fun noPlaceFound(kind: PlaceKind): String {
        val label = placeKindLabel(kind)
        return "근처에서 ${label}${objectParticle(label)} 찾지 못했어요"
    }

    fun voicePlaceProposal(kind: PlaceKind, placeName: String): String {
        val label = placeKindLabel(kind)
        return "근처 ${label}${objectParticle(label)} 찾았어요. ${placeName}에 들를까요?"
    }

    /** 반경 검색이라 "경로상"이 아니라 "가까운"이라고 쓴다. 피그마 문구와 다르다(docs/design/escort.md 8장). */
    fun restProposal(name: String): String =
        "${name}${topicParticle(name)} 장거리 이동 시 휴식을 선호해요. 가까운 휴식 장소에 들를까요?"

    fun delayNotice(delayedMinutes: Long): String = "도착이 ${delayedMinutes}분 늦어질 것 같아요"

    fun waypointAdded(placeName: String): String = "${placeName}${objectParticle(placeName)} 경유지로 추가했어요"

    fun failure(reason: FailureReason): String = when (reason) {
        FailureReason.PERMISSION_DENIED -> "권한이 없어 실행하지 못했어요"
        FailureReason.NOT_SUPPORTED -> "이 차량에서 지원하지 않는 기능이에요"
        FailureReason.INVALID_REQUEST -> "요청한 값으로는 바꿀 수 없어요"
        FailureReason.DRIVING_RESTRICTED -> "주행 중에는 실행할 수 없어요"
        FailureReason.CONNECTION_FAILED -> "차량 서비스에 연결하지 못했어요"
        FailureReason.NETWORK -> "네트워크 연결을 확인해 주세요"
        FailureReason.UNKNOWN -> "실행하지 못했어요"
    }

    /** 을/를. 한글이 아닌 글자로 끝나면("카페 A") 받침이 없다고 본다. */
    fun objectParticle(word: String): String = if (endsWithFinalConsonant(word)) "을" else "를"

    /** 은/는 */
    fun topicParticle(word: String): String = if (endsWithFinalConsonant(word)) "은" else "는"

    private fun endsWithFinalConsonant(word: String): Boolean {
        val last = word.trim().lastOrNull() ?: return false
        if (last !in '가'..'힣') return false
        // 한글 음절 코드는 (초성 × 21 + 중성) × 28 + 종성 순서라, 28로 나눈 나머지가 0이면 받침이 없다.
        return (last - '가') % 28 != 0
    }
}
