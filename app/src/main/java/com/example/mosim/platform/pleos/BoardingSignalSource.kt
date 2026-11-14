package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.Flow

/**
 * 탑승 감지에 쓰는 차량 신호 묶음 (D-1 "감지된 신호").
 *
 * 실제 출처 (sdk-status.md 3.1, 3.5. 에뮬레이터 검증 전이라 아직 호출 코드는 없다)
 *  - 조수석 도어: Vehicle SDK Door `registerDoorPosition`
 *  - 조수석 착석: Vehicle SDK Seat `registerSeatOccupancy`
 *  - 안전벨트: Vehicle SDK Seat `registerSeatBeltBuckledState`
 *  - 정차: Vehicle SDK에 기어·차속이 없어서 AAOS CarPropertyManager
 *
 * 값이 null이면 아직 신호를 받지 못한 것이다(첫 콜백 전, 조회 실패, 미지원).
 */
data class BoardingSignals(
    val passengerDoor: DoorSignal? = null,
    val passengerSeatOccupied: Boolean? = null,
    val passengerSeatBeltBuckled: Boolean? = null,
    val vehicleStopped: Boolean? = null,
) {
    /** 네 신호가 다 모였는지. 모여도 누가 탔는지는 모르므로 운전자 확인은 따로 받는다. */
    val allDetected: Boolean
        get() = passengerDoor == DoorSignal.ClosedAfterOpen &&
            passengerSeatOccupied == true &&
            passengerSeatBeltBuckled == true &&
            vehicleStopped == true
}

/** 도어는 지금 상태만으로는 부족하다. "열렸다가 닫혔다"를 봐야 누군가 탔다고 본다. */
enum class DoorSignal { Closed, Open, ClosedAfterOpen }

/** 화면은 이 인터페이스만 본다. 실제 SDK 구현은 에뮬레이터 검증 후에 만든다. */
interface BoardingSignalSource {
    fun signals(): Flow<BoardingSignals>
}
