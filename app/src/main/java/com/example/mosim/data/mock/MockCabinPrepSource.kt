package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.CabinPrepSource
import com.example.mosim.platform.pleos.PrepApplyState
import com.example.mosim.platform.pleos.PrepItemStatus
import com.example.mosim.platform.pleos.PrepKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 실내 준비 적용 Mock. 흉내 내는 실제 API: Vehicle SDK HVAC·Seat의 Capability 확인 → Set → 상태 콜백.
 *
 * 항목을 하나씩 "적용 중"으로 바꾼 뒤, 잠시 후 "적용"으로 바꾼다.
 * 실제 SDK도 Set 성공과 실제 반영 사이에 시간이 있어서 바로 "적용"으로 보내지 않는다(sdk-status.md 4장).
 */
class MockCabinPrepSource(
    private val scenario: Scenario = Scenario.Normal,
    private val stepDelayMs: Long = 700L, // 시연 영상에서 하나씩 바뀌는 게 보이도록 잡은 값
) : CabinPrepSource {

    enum class Scenario {
        Normal,

        // 웰컴 조명이 없는 차량 (ERROR_NOT_SUPPORTED_PROPERTY에 해당)
        LightingUnsupported,

        // 좌석 조정이 실패한 경우 (권한·주행 중 제한 등)
        SeatFailed,
    }

    override fun apply(items: List<PrepKind>): Flow<List<PrepItemStatus>> = flow {
        val states = items.associateWith { PrepApplyState.Pending }.toMutableMap()
        fun snapshot() = items.map { PrepItemStatus(it, states.getValue(it)) }

        emit(snapshot())
        for (kind in items) {
            if (scenario == Scenario.LightingUnsupported && kind == PrepKind.Lighting) {
                // 미지원은 Capability 확인에서 바로 알 수 있어서 "적용 중"을 거치지 않는다.
                states[kind] = PrepApplyState.Unsupported
                emit(snapshot())
                continue
            }
            delay(stepDelayMs)
            states[kind] = PrepApplyState.Applying
            emit(snapshot())

            delay(stepDelayMs)
            states[kind] = if (scenario == Scenario.SeatFailed && kind == PrepKind.Seat) {
                PrepApplyState.Failed
            } else {
                PrepApplyState.Applied
            }
            emit(snapshot())
        }
    }
}
