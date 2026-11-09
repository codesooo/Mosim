package com.example.mosim.feature.escort.command

/**
 * 동승자 음성 문장을 해석한 결과다.
 *
 * 값의 범위 제한(16~30°C, 선호 범위 등)은 여기서 하지 않는다.
 * 범위는 차량 Capability와 동승자 프로필에 따라 달라서 실행 정책(EscortViewModel)이 판단한다.
 */
sealed interface VoiceCommand {

    /** 현재 설정 온도에서 [deltaCelsius]만큼 올리거나 내린다. */
    data class AdjustTemperature(val deltaCelsius: Float) : VoiceCommand

    /** 설정 온도를 [targetCelsius]로 맞춘다. */
    data class SetTemperature(val targetCelsius: Float) : VoiceCommand

    /** 음악 음량을 [delta] 단계만큼 바꾼다. */
    data class AdjustVolume(val delta: Int) : VoiceCommand

    /**
     * 조수석 등받이 각도를 [deltaDegrees]만큼 바꾼다. 양수가 젖히는 쪽이다.
     * [확인 필요] 실제 SDK는 목표 각도가 아니라 방향 이동(moveSeatBackrestAngle)이라 실행 방식은 CabinControl에서 정한다.
     */
    data class AdjustBackrest(val deltaDegrees: Int) : VoiceCommand

    /** 근처 [kind] 장소를 찾는다. 경로를 바꾸는 요청이라 자동 실행하지 않고 운전자 승인을 거친다. */
    data class FindPlace(val kind: PlaceKind) : VoiceCommand

    /** 규칙에 맞는 표현이 없다. 원문은 로그와 안내에 쓴다. */
    data class Unknown(val text: String) : VoiceCommand
}

enum class PlaceKind {
    CAFE,
    RESTROOM,
    CONVENIENCE_STORE,
    REST_AREA,
    EV_CHARGER,
}
