package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.BoardingSignalSource
import com.example.mosim.platform.pleos.BoardingSignals
import com.example.mosim.platform.pleos.DoorSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 탑승 신호 Mock. 흉내 내는 실제 API:
 *  - Vehicle SDK Door `registerDoorPosition`, Seat `registerSeatOccupancy` / `registerSeatBeltBuckledState`
 *  - AAOS CarPropertyManager 기어(정차 판단)
 *
 * 실제 콜백처럼 신호가 시간차를 두고 하나씩 들어온다.
 * 순서: 정차 → 도어 열림 → 착석 → 도어 닫힘 → 벨트 착용
 * [확인 필요] 실제 차량에서 신호가 오는 순서와 간격은 에뮬레이터에서 확인한다.
 */
class MockBoardingSignalSource(
    private val scenario: Scenario = Scenario.Normal,
    private val stepDelayMs: Long = 1_200L, // 시연 영상에서 하나씩 바뀌는 게 보이도록 잡은 값
) : BoardingSignalSource {

    enum class Scenario {
        Normal,

        // 탑승 감지 오탐·누락 확인용: 벨트를 매지 않아 신호가 다 모이지 않는다.
        BeltNotBuckled,

        // SDK 연결 실패를 흉내 낸다. 착석 신호 전에 Flow가 예외로 끝난다.
        ConnectionFailed,
    }

    override fun signals(): Flow<BoardingSignals> = flow {
        // 정차 확인 뒤에 D-1에 들어오므로 정차는 처음부터 true다. 나머지는 아직 신호가 없다(null).
        var signals = BoardingSignals(vehicleStopped = true)
        emit(signals)

        delay(stepDelayMs)
        signals = signals.copy(passengerDoor = DoorSignal.Open)
        emit(signals)

        delay(stepDelayMs)
        if (scenario == Scenario.ConnectionFailed) {
            throw IllegalStateException("Mock: 차량 신호 연결 실패")
        }
        signals = signals.copy(passengerSeatOccupied = true)
        emit(signals)

        delay(stepDelayMs)
        signals = signals.copy(passengerDoor = DoorSignal.ClosedAfterOpen)
        emit(signals)

        delay(stepDelayMs)
        signals = signals.copy(passengerSeatBeltBuckled = scenario != Scenario.BeltNotBuckled)
        emit(signals)
    }
}
