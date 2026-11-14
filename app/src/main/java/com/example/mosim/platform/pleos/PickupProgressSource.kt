package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.Flow

/**
 * 픽업지로 가는 동안의 진행 상황 (C-1).
 *
 * 실제 출처 (sdk-status.md 3.2. NaviHelper는 ✅이지만 에뮬레이터 검증 전이라 아직 호출 코드는 없다)
 *  - 남은 거리: NaviHelper getDestinationInfo → onDestinationInfo의 DestinationInfo.remainDistance
 *  - 도착: NaviHelper onDestinationArrived
 * [확인 필요] remainDistance 단위가 reference 문서에 없다. 미터로 가정했다.
 *
 * remainingDistanceMeters가 null이면 아직 값을 받지 못한 것이다.
 */
data class PickupProgress(
    val remainingDistanceMeters: Int? = null,
    val arrived: Boolean = false,
)

interface PickupProgressSource {
    fun progress(): Flow<PickupProgress>
}
