package com.example.mosim.data.passenger

/**
 * 에스코트 중에 쓰는 동승자 정보와 자동 조절 동의 범위다.
 *
 * [확인 필요] 동승자 선택(A-1)과 프로필 저장 방식이 아직 없어 필드가 바뀔 수 있다.
 * 지금은 에스코트 판단에 꼭 필요한 값만 둔다.
 */
data class PassengerProfile(
    val name: String,
    /**
     * 이 범위 안의 온도 조절만 운전자 승인 없이 자동 실행한다.
     * null이면 동의 범위가 없다는 뜻이라 자동 온도 조절을 하지 않는다.
     */
    val autoTemperatureRangeCelsius: ClosedFloatingPointRange<Float>?,
    /** 지연 감지 시 휴식 장소를 먼저 제안할지. 제안 문구도 이 값으로 정한다. */
    val prefersRestOnLongTrip: Boolean,
    /**
     * 휴식 장소 경유를 승인하면 함께 맞출 음악 음량. null이면 함께 처리하지 않는다.
     * [확인 필요] 피그마 F-1의 "음악 볼륨 4 → 3"을 따른 규칙이다. 팀 합의 필요.
     */
    val restMusicVolume: Int?,
)

/** 지금 모시고 가는 이동 한 건이다. */
data class EscortTrip(
    val passenger: PassengerProfile,
    val destinationName: String,
    val boardedAt: java.time.LocalTime?,
)
