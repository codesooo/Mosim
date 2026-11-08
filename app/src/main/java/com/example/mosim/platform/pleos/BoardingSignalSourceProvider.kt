package com.example.mosim.platform.pleos

import com.example.mosim.data.mock.MockBoardingSignalSource

/**
 * 탑승 신호 소스를 고르는 곳.
 * 실제 구현(Vehicle SDK Door·Seat + AAOS 기어)은 에뮬레이터 검증 전이라 아직 없다. 지금은 Mock만 돌려준다.
 * 실제 구현이 생기면 VehicleStatusSourceProvider처럼 USE_MOCK으로 고르게 바꾼다.
 */
object BoardingSignalSourceProvider {
    val source: BoardingSignalSource by lazy { MockBoardingSignalSource() }
}
