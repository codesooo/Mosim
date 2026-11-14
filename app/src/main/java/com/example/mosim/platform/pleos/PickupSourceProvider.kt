package com.example.mosim.platform.pleos

import com.example.mosim.data.mock.MockBoardingSignalSource
import com.example.mosim.data.mock.MockCabinPrepSource
import com.example.mosim.data.mock.MockPickupProgressSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * C·D 픽업 · 탑승 단계에서 쓰는 차량 데이터 소스를 고르는 곳.
 *
 * 실제 구현(NaviHelper, Vehicle SDK, AAOS)은 에뮬레이터 검증 전이라 아직 없다. 지금은 Mock만 돌려준다.
 * 실제 구현이 생기면 VehicleStatusSourceProvider처럼 USE_MOCK으로 고르게 바꾼다.
 */
object PickupSourceProvider {

    val pickupProgress: PickupProgressSource by lazy { MockPickupProgressSource() }

    val cabinPrep: CabinPrepSource by lazy { MockCabinPrepSource() }

    private val boardingSource: BoardingSignalSource by lazy { MockBoardingSignalSource() }

    // 화면보다 오래 사는 범위. 탑승 신호를 C-2와 D-1이 나눠 보려고 쓴다.
    private val sharingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 탑승 신호는 C-2와 D-1 두 화면이 이어서 본다. 화면마다 따로 구독하면 D-1에서 처음부터 다시 시작해
     * "도어 열림 → 닫힘" 순서를 놓친다. 그래서 하나의 스트림을 나눠 쓴다.
     *
     * - 화면 전환 사이에 구독이 잠깐 끊겨도 5초 안이면 이어서 받는다.
     * - 두 화면을 다 떠나고 5초가 지나면 멈추고 값을 처음 상태로 되돌린다. 다음 픽업 때 처음부터 다시 받는다.
     * - 실패하면 마지막 값을 그대로 둔다. 예외를 여기서 막지 않으면 앱이 종료된다.
     */
    val boardingSignals: StateFlow<BoardingSignals> by lazy {
        boardingSource.signals()
            .catch { }
            .stateIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = SHARE_STOP_TIMEOUT_MS,
                    replayExpirationMillis = 0,
                ),
                initialValue = BoardingSignals(),
            )
    }

    private const val SHARE_STOP_TIMEOUT_MS = 5_000L
}
