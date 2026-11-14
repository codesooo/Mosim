package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.Flow

/**
 * 픽업지에서 동승자 맞이 준비를 차량에 적용한다 (C-2 "준비 항목", 이용 흐름 03).
 *
 * 실제 출처 (sdk-status.md)
 *  - 공조: Vehicle SDK HVAC `setHvacTemperature` 🔜
 *  - 좌석: Vehicle SDK Seat `moveSeatBackrestAngle`, `setHvacSeatTemperature` 🔜
 *  - 음악: ❓ Vehicle SDK에 미디어 도메인이 없다
 *  - 조명: ❓ sdk-status.md 목록에 없다
 * 전부 사용 가능(✅)이 아니어서 인터페이스와 Mock까지만 만든다.
 *
 * Set이 성공해도 실제로 바뀐 것은 아니다(sdk-status.md 3.1). 그래서 결과를 한 번에 돌려주지 않고
 * 항목마다 상태가 바뀌는 스트림으로 내보낸다.
 * 항목 하나가 실패해도 Flow를 예외로 끝내지 않고 그 항목을 Failed로 보낸다. 나머지 항목은 계속 적용한다.
 *
 * [확인 필요] 실제 구현에는 프로필의 값(온도, 좌석 각도 등)이 필요하다. 프로필 모델이 정해지면 인자로 넘긴다.
 */
interface CabinPrepSource {
    fun apply(items: List<PrepKind>): Flow<List<PrepItemStatus>>
}

enum class PrepKind { Climate, Seat, Music, Lighting }

enum class PrepApplyState {
    Pending, // 아직 차례가 안 됨
    Applying, // Set 요청을 보냈고 실제 반영을 기다리는 중
    Applied, // 다시 조회하거나 콜백으로 바뀐 값을 확인함
    Unsupported, // Capability 확인 결과 이 차량에 없는 기능
    Failed, // 권한 없음, 주행 중 제한, 연결 실패 등
}

data class PrepItemStatus(
    val kind: PrepKind,
    val state: PrepApplyState,
)
