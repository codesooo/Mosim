package com.example.mosim.platform.pleos

/**
 * 차량 상태를 읽는 통로다. 구현은 두 개다.
 *
 * - [com.example.mosim.data.mock.MockVehicleStatusSource]: SDK 없이 개발할 때 쓴다. 현재 기본값.
 * - [PleosVehicleStatusSource]: 실제 Pleos Vehicle SDK 연동 자리. 아직 비어 있다.
 *
 * 화면과 ViewModel은 이 인터페이스만 본다.
 */
interface VehicleStatusSource {
    suspend fun load(): VehicleStatus
}
