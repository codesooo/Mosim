package com.example.mosim.platform.pleos

/**
 * 실제 Pleos Vehicle SDK 연동 자리다.
 *
 * 지금은 비워 둔다. 실제 API 시그니처를 확인하지 않은 상태에서 추측해 채우지 않는다.
 *
 * 연동할 때 해야 할 일:
 * 1. settings.gradle.kts의 Pleos Playground Maven 저장소 주석을 푼다.
 * 2. Vehicle SDK 의존성을 추가한다. (공식 문서 기준 ai.pleos.playground:Vehicle, 버전 [확인 필요])
 * 3. AndroidManifest.xml에 필요한 권한을 선언한다. (문 상태를 읽으려면 pleos.car.permission.CAR_DOORS)
 * 4. 이 클래스의 load()를 SDK 호출로 채운다.
 * 5. VehicleStatusSourceProvider의 USE_MOCK을 false로 바꾼다.
 *
 * 배터리와 주행가능거리에 해당하는 SDK API 이름은 확인되지 않았다. [확인 필요]
 */
class PleosVehicleStatusSource : VehicleStatusSource {
    override suspend fun load(): VehicleStatus {
        error("Pleos Vehicle SDK 연동이 아직 없다. VehicleStatusSourceProvider.USE_MOCK을 true로 두고 개발한다.")
    }
}
