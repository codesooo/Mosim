package com.example.mosim.feature.planrevision

/**
 * F-1이 받는 AI 경로 변경 제안. **외부 AI 모듈이 만들어 넘겨 준다.**
 *
 * 화면은 여기 담긴 값만 그린다. 없는 값은 비워 두고, 임의로 계산하거나 지어내지 않는다.
 * 특히 [arrival]과 [extra]는 실제 경로 계산 결과가 있을 때만 채운다.
 */
data class PlanSuggestion(
    /**
     * 제안 하나를 가리키는 값. 음성 안내를 제안당 한 번만 하기 위한 기준이다.
     * 내용이 같아도 새로 제안한 것이면 새 id를 준다.
     */
    val id: String,
    /** 화면에서 가장 크게 보여 줄 한 줄. 예: "잠시 쉬어갈까요?" */
    val headline: String,
    /** 왜 제안하는지 한 줄. 예: "엄마의 장거리 이동 선호를 반영해 휴식 장소를 추천해요." */
    val reason: String,
    /** 제안하는 경유지. 없으면 장소 카드를 그리지 않는다. */
    val place: SuggestedPlace?,
    /** 경유했을 때의 도착 예정 시각. 계산 결과가 없으면 `null`. */
    val arrival: String? = null,
    /** 경유로 늘어나는 시간. 계산 결과가 없으면 `null`. */
    val extra: ExtraTime? = null,
    /**
     * 음성으로 읽을 문장. 화면 문구와 따로 관리한다.
     * `null`이면 음성 안내를 하지 않는다. 화면에 없는 정보를 넣지 않는다.
     */
    val speech: String? = null,
    val passengerName: String? = null,
    val isMock: Boolean = false,
)

/** F-1이 제안하는 경유지 한 곳. 추천 이유는 [PlanSuggestion.reason]에 있으니 여기서 되풀이하지 않는다. */
data class SuggestedPlace(
    val name: String,
    /** 이 장소라서 좋은 점 한 줄. 일반적인 편의시설 나열은 넣지 않는다. */
    val highlight: String,
    val externalId: String? = null,
)

/**
 * 경유로 늘어나는 시간.
 *
 * 같은 "+30분"이라도 우회만인지 머무는 시간까지인지에 따라 뜻이 달라서, 무엇이 들어갔는지 같이 받는다.
 * 화면은 이 값을 근거로 안내 문구를 고르고, 둘 다 `false`면 구성을 말하지 않는다.
 */
data class ExtraTime(
    /** 표시용 문자열. 예: "+30분" */
    val text: String,
    /** 경로 우회에 드는 시간이 포함됐는가 */
    val includesDetour: Boolean,
    /** 그 장소에 머무는 시간이 포함됐는가 */
    val includesStay: Boolean,
) {
    /** 무엇이 들어간 시간인지 한 줄. 둘 다 아니면 구성을 말하지 않는다. */
    fun composition(): String? = when {
        includesDetour && includesStay -> "우회 + 머무는 시간"
        includesDetour -> "우회만"
        includesStay -> "머무는 시간만"
        else -> null
    }
}

/** 운전자가 F-1에서 내린 결정. */
enum class PlanDecision {
    /** 기존 경로 유지 */
    KeepRoute,

    /** 경유지 추가 요청 */
    AddWaypoint,
}

/**
 * 경유지 추가 요청의 진행 상태.
 *
 * **[Requested]는 "요청을 보냈다"는 뜻이지 "경로가 바뀌었다"는 뜻이 아니다.**
 * 실제 반영 여부는 NaviHelper 응답을 받아야 알 수 있고, 아직 연결 전이다.
 */
enum class WaypointRequestState {
    Idle,
    Requested,
    NotConnected,
    Applied,
    Failed,
}
