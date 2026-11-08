package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.PickupProgress
import com.example.mosim.platform.pleos.PickupProgressSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 픽업지 진행 Mock. 흉내 내는 실제 API: NaviHelper onDestinationInfo(remainDistance), onDestinationArrived.
 *
 * 실제 콜백처럼 남은 거리가 시간차를 두고 줄다가 마지막에 도착 신호가 온다.
 * 피그마 C-1의 "남은 거리 3.1km"에서 시작한다.
 */
class MockPickupProgressSource(
    private val scenario: Scenario = Scenario.Normal,
    private val stepDelayMs: Long = 1_000L, // 시연 영상에서 거리가 줄어드는 게 보이도록 잡은 값
) : PickupProgressSource {

    enum class Scenario {
        Normal,

        // 내비 연결 실패를 흉내 낸다. 거리 두 번을 보낸 뒤 Flow가 예외로 끝나고 도착 신호는 오지 않는다.
        ConnectionFailed,
    }

    override fun progress(): Flow<PickupProgress> = flow {
        MOCK_DISTANCES_M.forEachIndexed { index, meters ->
            if (index > 0) delay(stepDelayMs)
            if (scenario == Scenario.ConnectionFailed && index == FAIL_AFTER_STEPS) {
                throw IllegalStateException("Mock: 내비게이션 연결 실패")
            }
            emit(PickupProgress(remainingDistanceMeters = meters))
        }
        delay(stepDelayMs)
        emit(PickupProgress(remainingDistanceMeters = 0, arrived = true))
    }

    private companion object {
        val MOCK_DISTANCES_M = listOf(3_100, 2_400, 1_600, 900, 300)
        const val FAIL_AFTER_STEPS = 2
    }
}
