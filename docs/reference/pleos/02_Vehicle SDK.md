# Pleos Connect — Vehicle SDK 가이드 + API 목록

> 출처: Pleos Playground 개발자 문서 (https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
> 수집일: 2026-11-07 · 기준 버전: Vehicle SDK 2.3.2 / Vehicle Service 2.3.21~2.3.22
> 원문의 Kotlin/Java 탭 중 **Kotlin 예제만** 수록했습니다. 이미지는 제외했습니다.
 
---

## 1. Introduction
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/intro -->

Vehicle SDK는 차량 정보 조회 및 제어를 위한 API를 제공합니다. 3rd party 앱은 Vehicle SDK를 통해 Pleos Connect의 Vehicle Service에 연결합니다. Vehicle Service는 Android Automotive OS 위에서 동작하며, 3rd party 앱은 Vehicle SDK API로 차량 정보 조회 및 제어를 요청하고 Vehicle Service가 이를 처리합니다.

### 진입점과 도메인 구조

Vehicle SDK API는 도메인별 인터페이스로 제공됩니다. `Door`, `Brake` 같은 도메인 객체는 직접 생성하지 않고, 진입점인 `Vehicle` 인스턴스의 `getDoor()`, `getBrake()` 같은 `getXxx()`로만 얻습니다. 도메인 객체에서 capability·get·set·callback API를 호출합니다.
- Capability(`checkXXXCapabilities`): 기능 지원 여부와 허용 값 범위 확인
- get: 현재 값 조회 / set: 값 설정 / Callback: 리스너 등록으로 상태 변화 구독
  차량 서비스 연결은 `initialize()`로 시작하고, 사용이 끝나면 `release()`로 해제합니다. 차량 통신은 IPC이므로 capability·get·set 결과는 즉시 반환되지 않고 `onComplete`·`onFailed` 비동기 람다로 전달됩니다. **비동기 콜백이 끝나기 전에 `release()`를 호출하지 않도록 주의**하세요.

| 도메인 | 설명 |
| --- | --- |
| Brake | 브레이크 페달, 주차 브레이크 상태 조회 |
| CarInfo | 차량 기본 정보(외부 크기, 제조사, 연식 등) 조회 |
| Display | 인포테인먼트 표시 데이터 단위(온도 단위 등) 조회/설정 |
| Door | 도어 열림/닫힘, 잠금/해제 제어 및 상태 조회 |
| DrivingMode | 드라이브 모드, 크루즈 컨트롤 정보 조회 |
| EvBattery | EV 배터리·충전 상태 및 전비(효율) 조회 |
| Fuel | 연비 등 연료 관련 정보 조회 |
| HVAC | 난방·환기·공조(온도, 풍량, 습도 등) 제어 및 조회 |
| Light | 조명 상태 조회 및 앰비언트 라이트 등 제어 |
| Odometer | 총 주행 거리, 트립 주행 거리/시간, 주행 거리 단위 조회 |
| Safety | 차량 경고(Warning) 정보 조회 |
| Seat | 좌석 위치·각도·기능 제어 및 조회 |
| SideMirror | 사이드미러 열선 상태 조회/설정 |
| Steeringwheel | 핸들(스티어링휠) 상태, 열선 조회/제어 |
| Tire | 타이어 상태 조회 |
| Toll | ETCS(하이패스) 음성 안내 음량 등 조회/설정 |
| TurnSignal | 방향 지시등 상태 조회 |
| Window | 창문 제어 및 상태 조회 |
| Wiper | 와이퍼 제어 및 상태 조회 |

### Emulator setup
Vehicle SDK는 Pleos Connect Emulator 환경에서 개발·검증합니다. 준비 순서: 소프트웨어 개발 환경 요구사항 확인 → Pleos Connect Emulator 설치 → AVD 생성 및 실행 → Pleos Connect Virtual Console 설정 → 마이크 접근 설정 → Cold boot 설정.
 
---

## 2. Importing Vehicle SDK
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup -->

### Maven 저장소 등록 (`settings.gradle`)
```kotlin
dependencyResolutionManagement {
    ...
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
        ...
    }
}
```

### SDK 의존성 추가 (앱 모듈 `build.gradle`)
```kotlin
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}
```
 
---

## 3. Declaring permission
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission -->

Vehicle SDK API는 Android 매니페스트에 Pleos 차량 퍼미션을 선언해야 호출할 수 있습니다. 각 API의 `@RequiresPermission` 어노테이션과 상수 정의는 PleosPermission을 참조하세요. 매니페스트에는 `pleos.car.permission.{상수명}` 형식으로 선언합니다.

| 접두사 | 예시 | 용도 |
| --- | --- | --- |
| CAR_* | CAR_DOORS → pleos.car.permission.CAR_DOORS | 도메인 데이터 조회(get) |
| CONTROL_CAR_* | CONTROL_CAR_DOORS | 도메인 제어(set) |

### 도메인별 대표 퍼미션 (조회용, CAR_*)
세부 API마다 요구 퍼미션이 다를 수 있으므로 호출 전 해당 함수의 `@RequiresPermission`을 확인하세요.

| 도메인 | PleosPermission 상수 | 매니페스트 android:name |
| --- | --- | --- |
| Door | CAR_DOORS | pleos.car.permission.CAR_DOORS |
| Window, Wiper | CAR_WINDOWS | pleos.car.permission.CAR_WINDOWS |
| HVAC | CAR_CLIMATE | pleos.car.permission.CAR_CLIMATE |
| DrivingMode, Brake, Odometer | CAR_DRIVE | pleos.car.permission.CAR_DRIVE |
| EvBattery, Fuel | CAR_ENERGY | pleos.car.permission.CAR_ENERGY |
| CarInfo, Display | CAR_INFO | pleos.car.permission.CAR_INFO |
| Light, TurnSignal | CAR_LIGHTS | pleos.car.permission.CAR_LIGHTS |
| SideMirror | CAR_MIRRORS | pleos.car.permission.CAR_MIRRORS |
| Safety, Tire | CAR_SAFETY | pleos.car.permission.CAR_SAFETY |
| Seat | CAR_SEATS | pleos.car.permission.CAR_SEATS |
| Toll | CAR_TOLL | pleos.car.permission.CAR_TOLL |

### 퍼미션 선언 예시 (`AndroidManifest.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.CAR_DOORS" />
    <uses-permission android:name="pleos.car.permission.CAR_INFO" />
    <!-- 사용하는 도메인에 맞는 퍼미션만 선언 -->
</manifest>
```

### 퍼미션 누락 시
API `onFailed`에 `Exception`이 전달되며, `VehicleException`인 경우 `ERROR_SECURITY_PERMISSION`에 해당할 수 있습니다.

> 설명회 참고: 공모전 개발 단계에서는 별도 승인 없이 매니페스트 선언만으로 에뮬레이터 테스트가 가능하지만, 실제 App Market 배포 시에는 승인된 권한만 허용됩니다.
 
---

## 4. Obtaining a domain object
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object -->

`Vehicle` 인스턴스를 생성하고 `initialize()`로 차량 서비스에 연결한 뒤, `getDoor()`·`getDisplay()` 같은 `getXxx()`로 도메인 객체를 얻습니다. 사용이 끝나면 `release()`로 리소스를 해제합니다.

```kotlin
// Vehicle 인스턴스 생성 후 초기화
val vehicle = Vehicle(context)
vehicle.initialize()
 
// getXxx()로 도메인 객체 획득
val door = vehicle.getDoor()
 
// door 객체에서 Get / Set / Callback / Capability API 호출
// ...
 
// 사용 종료 후 리소스 해제
vehicle.release()
```

### 네 가지 API 타입
| 타입 | 하는 일 | 결과 전달 |
| --- | --- | --- |
| Capability | 기능 지원 여부·허용 범위 확인 | onComplete(config, areaMap) / onFailed |
| Get | 현재 값 조회 | onComplete(값) / onFailed |
| Set | 값 설정 | onFailed(실패 시) |
| Callback | 상태 변화 구독 | 리스너 onXxxUpdated / onFailed |
 
---

## 5. Capability
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability -->

`checkXxxCapabilities`로 기능 지원 여부와 허용 범위를 확인합니다. 반환 형태는 API마다 다르며, 공통 모델은 capability 패키지를 참조하세요.

| 타입 | 설명 |
| --- | --- |
| CapabilityConfig | 기능 단위 접근 수준(AccessType: NONE, READ, WRITE, READ_WRITE) |
| AreaBounds | 영역별 허용 값 범위(minValue, maxValue) |
| Map<Area, AreaBounds> | 영역(예: DoorArea)별 AreaBounds 맵 |

```kotlin
val door = vehicle.getDoor()
door.checkDoorPositionCapabilities(
  onComplete = { config, areaMap ->
    when (config.accessType) {
      AccessType.READ, AccessType.READ_WRITE -> {
        val bounds = areaMap[DoorArea(DoorValue.ROW_1_LEFT)]
        // Use bounds?.minValue / bounds?.maxValue, then call getCurrentDoorPosition
      }
      else -> {
        // NONE or WRITE only — disable UI or use an alternate flow
      }
    }
  },
  onFailed = { e: Exception ->
    if (e is VehicleException) {
      // Capability lookup failed: e.errorCode
    }
  }
)
```
 
---

## 6. Get
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/get-property -->

`getCurrentXxx`로 차량 상태 및 설정 값을 조회합니다. 결과는 `onComplete(값)` / `onFailed`로 비동기 전달됩니다.

```kotlin
val door = vehicle.getDoor()
door.getCurrentDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  onComplete = { pos: Int? ->
    // do something
  },
  onFailed = { e: Exception ->
    // do something
  }
)
 
val display = vehicle.getDisplay()
display.getCurrentTemperatureDisplayUnit(
  onComplete = { unit: TemperatureUnit? ->
    // do something
  },
  onFailed = { e: Exception ->
    // do something
  }
)
```
 
---

## 7. Set
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/set-property -->

`setXxx`로 차량 상태 및 설정 값을 변경합니다. 실패 시 `onFailed`로 전달됩니다.

```kotlin
val door = vehicle.getDoor()
door.setDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  value = 1, // Integer type
  onFailed = { e: Exception ->
    // do something
  }
)
```
 
---

## 8. Callback
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/callback -->

`registerXxx` / `unregisterXxx`로 리스너를 등록·해제해 차량 상태 값의 변화를 구독합니다. 값이 변경되면 리스너의 `onXxxUpdated`로, 실패 시 `onFailed`로 전달됩니다.

```kotlin
// Create callback listener
val callbackListener = object : DoorPositionListener {
  override fun onPositionUpdated(pos: Int?) {
    // do something
  }
 
  override fun onFailed(e: Exception) {
    // do something
  }
}
 
val door = vehicle.getDoor()
 
// register callback
door.registerDoorPosition(
  doorPositionListener = callbackListener
)
// unregister callback
door.unregisterDoorPosition(
  doorPositionListener = callbackListener
)
```
 
---

## 9. Error code
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code -->

`onFailed`로 받은 Exception이 `VehicleException`이면 `errorCode`로 ErrorCode 값을 확인합니다. 모든 onFailed가 VehicleException은 아닐 수 있습니다.
- onFailed 시그니처: Get/Set API, checkXXXCapabilities, VehicleListener 모두 Kotlin 표준 `Exception` 사용. ErrorCode가 콜백 인자로 직접 전달되지는 않음.
- ErrorCode 정의: `ai.pleos.playground.vehicle.constant` 패키지의 enum. 공개 API에서는 `VehicleException.errorCode`로 노출.
- 사전에 checkXXXCapabilities로 미지원 호출을 줄이고, 퍼미션 오류는 Permission을 확인.
```kotlin
door.getCurrentDoorPosition(
  area = DoorArea(DoorValue.ROW_1_LEFT),
  onComplete = { pos -> },
  onFailed = { e ->
    if (e is VehicleException) {
      when (e.errorCode) {
        ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY,
        ErrorCode.ERROR_NOT_SUPPORTED_AREA -> { /* UI 비활성 */ }
        ErrorCode.ERROR_SECURITY_PERMISSION -> { /* 매니페스트 퍼미션 확인 */ }
        ErrorCode.ERROR_SERVICE_CONNECTION_FAILURE,
        ErrorCode.ERROR_INTERNAL_SERVICE -> { /* initialize·재시도 */ }
        else -> Log.e("Vehicle API 오류: ${e.errorCode} — ${e.message}")
      }
    } else {
      Log.e("Vehicle API 오류: ${e.message}")
    }
  }
)
```

| ErrorCode | 설명 | 권장 처리 |
| --- | --- | --- |
| ERROR_NOT_SUPPORTED_PROPERTY | 해당 속성(기능) 미지원 | UI 비활성·대체 흐름 |
| ERROR_NOT_SUPPORTED_AREA | 해당 영역 미지원 | 영역 선택 변경 |
| ERROR_NOT_SUPPORTED_VALUE_TYPE | 지원하지 않는 값 타입 | 파라미터·타입 재확인 |
| ERROR_SECURITY_PERMISSION | 퍼미션 부족 | Permission·@RequiresPermission 확인 |
| ERROR_SERVICE_CONNECTION_FAILURE | Vehicle 서비스 연결 실패 | initialize()·에뮬레이터 상태 확인 |
| ERROR_INTERNAL_SERVICE | 내부 서비스 오류 | 재시도·일시 오류 안내 |
| ERROR_DRIVING_RESTRICTED | 주행 중 기능 제한 | 주행 제한 안내 |
| ERROR_EMPTY_RESULT | 결과 없음 | 빈 상태 UI 처리 |
| ERROR_UNDEFINED | 정의되지 않은 오류 | 로그·재시도 |
 
---

## 10. Vehicle SDK Sample App 사용 가이드 (튜토리얼)
<!-- 원문: /docs/pleos-only/tutorial/vehicle-sdk-sample-app -->

Vehicle SDK Sample App은 차량 기능을 찾아보고 Vehicle SDK 사용 흐름을 익히는 Android 예제 앱입니다. Pleos Connect 전용 AVD 기준입니다.

### 화면 구성
- **진입 화면**: `Vehicle Playground`(제목, 상단에 SDK·Vehicle Service 버전과 초기화 상태), `BROWSE VEHICLE APIS / VEHICLE DOMAINS`(Door, Window 등 분류, 숫자는 기능 수), `Search features`, 기능 카드(예: Door Position).
    - 상단 `SDK initialized`는 초기화 호출이 예외 없이 반환됐다는 뜻일 뿐, Vehicle Service 연결 완료나 개별 기능 사용 가능 여부를 의미하지 않음.
- **기능 상세 화면**
  | 화면 레이블 | 주요 기능 |
  | --- | --- |
  | Vehicle support | 기능 지원 상태와 선택 영역의 값 범위. Check support로 재확인 |
  | Current value | 마지막 조회/변경 알림 값. Area 선택 후 Refresh로 재조회 |
  | Change value | 변경할 값 지정. Apply change를 눌러야 요청 전송 |
  | Live updates | Start updates로 구독, Stop updates로 해제 |
  | API activity | 호출한 API와 최신 결과. Expand/Collapse |
  | 왼쪽 화살표 | 기능 목록으로 복귀(구독 해제 아님) |

기능에 따라 조회·변경·알림 중 일부만 제공되며, 값 형식에 따라 선택 버튼/스위치/숫자 입력/슬라이더가 표시됩니다. `Supported`는 지원 상태, `Reference`는 SDK 직접 호출 기준 예제를 뜻합니다.

### 용어
| 용어 | 의미 |
| --- | --- |
| Domain | Door, Window 등 차량 기능의 분류 |
| Area | ROW_1_LEFT처럼 조회·제어할 대상 영역 |
| Capability | 기능 지원 여부, 읽기·쓰기 접근 방식, 영역별 값 범위 |
| Read / Write | 현재 상태 조회 / 상태 변경 요청 |
| Listener | 차량 상태 변경 알림을 받는 객체 |

### 사용 환경
- 일반 휴대전화나 기본 에뮬레이터가 아니라 **Pleos Vehicle Service가 포함된 Pleos Connect 전용 AVD** 사용. 가이드 화면은 `Pleos_Connect_v2.1` AVD(Android 14) 기준이며, 기능·값 범위는 AVD 지원 상태에 따라 다를 수 있음.
  | 항목 | 기준 |
  | --- | --- |
  | Sample App | 1.0.0-sdk35 |
  | Vehicle SDK | 2.3.2 |
  | Vehicle Service | 2.3.22 |
  | AVD | Pleos_Connect_v2.1, Android 14 |
  | JDK | 17 |
  | Gradle wrapper | 8.10.2 |
  | Android Gradle Plugin | 8.8.2 |
  | Kotlin | 2.0.0 |
  | Android SDK | compileSdk / targetSdk 35 |
  | Sample App minSdk | 23 |

- 소스 빌드: Sample App은 `gradle/libs.versions.toml`에서 Vehicle SDK 버전을 관리. 프로젝트 루트에서 `./gradlew assembleDebug` → `./gradlew installDebug`. APK는 `app/build/outputs/apk/debug/`에 생성.
- SDK 버전을 바꾸면 `PublicApiPolicyTest`의 public API 수 기준이 일치하는지 확인(불일치 시 테스트 실패, 빌드 성공 여부와는 별개).
- **Pleos Playground에 배포할 제품 앱은 signing key rotation·lineage 지원을 위해 Android API 28 이상** 요구사항 적용.
### Door Position으로 사용 흐름 익히기 (Reference 예제, 영역 `ROW_1_LEFT`)
| 작업과 화면 위치 | Door Position typed API |
| --- | --- |
| 지원 범위 확인 — Vehicle support | checkDoorPositionCapabilities |
| 현재 상태 조회 — Current value | getCurrentDoorPosition |
| 변경 알림 구독 — Live updates | registerDoorPosition |
| 상태 변경 요청 — Change value | setDoorPosition |
| 변경 알림 해제 — Live updates | unregisterDoorPosition |

- **지원 범위 확인**: 진입 시 지원 범위를 확인하고 성공하면 현재값을 한 번 자동 조회. 예시 범위는 `0..1`이지만 다른 환경에서 같다고 가정하지 말 것. `Unsupported`에는 실제 미지원뿐 아니라 확인 요청 실패도 포함될 수 있음.
- **현재 상태 조회**: `null`/`unavailable`은 사용할 수 있는 현재값을 받지 못했다는 뜻. 오류 메시지가 있는 경우와 구분.
- **API activity**: 조회·변경의 `Complete`는 결과 콜백 수신, Listener 등록·해제의 `Complete`는 SDK 호출이 예외 없이 반환됐다는 뜻(Service 측 완료 응답 아님).
- **변경 알림 구독**: Start updates → `Receiving vehicle updates`. 실제 알림 수신 시 `Door update: <value>` 표시. 구독 상태 표시와 실제 수신을 구분.
- **상태 변경 요청**: 범위·쓰기 가능 여부 확인 후 값 선택 → Apply change로 전송(확인 창 없음). `setDoorPosition` 결과 값은 요청한 값이며 새로 조회한 상태가 아님.
- **변경 결과 확인**: 요청 완료만으로 실제 값이 바뀌었다고 판단하지 말고, Refresh 재조회나 변경 알림으로 확인.
- **변경 알림 해제**: 기능 확인을 마쳤을 때, 다른 기능/영역으로 바꾸기 전, SDK 사용 화면·객체를 끝낼 때 해제. 화면 이동이나 Area 변경만으로는 해제되지 않으며, 등록과 다른 영역으로 해제하면 오류가 날 수 있음.
### 내 앱에 적용하기
제품 앱에서는 필요한 typed API를 직접 사용하고, **실제 사용하는 API의 최소 권한만 선언**하세요(Sample App의 전체 권한 목록을 그대로 복사하지 말 것). Listener는 등록했던 동일 객체·area로 해제하고, 사용이 끝나면 `Vehicle.release()`로 정리합니다.

### 문제 해결
- 권한은 `app/src/main/AndroidManifest.xml`에 선언. Door Position의 지원 확인·조회·Listener에는 `CAR_DOORS`, 변경 요청에는 `CONTROL_CAR_DOORS` 필요. 실행 중 승인 창을 띄우는 Android 위험 권한 방식이 아님.
  | 증상 | 확인할 내용 |
  | --- | --- |
  | Initialization failed / Service 연결 오류 | 전용 AVD와 Vehicle Service 설치 상태 확인 |
  | Vehicle support가 Unsupported | 실제 미지원 또는 확인 실패. API activity 오류·권한·연결 확인 후 재확인 |
  | 현재값 null/unavailable | 지원 여부·선택 영역 확인 후 재조회(권한 누락으로 단정 금지) |
  | 현재값 조회 Failed | 오류 메시지, 읽기 권한, 선택 영역, Service 상태 확인 |
  | 변경 요청 거부/범위 오류 | 쓰기 권한, 쓰기 접근 가능 여부, 값 범위 확인 |
  | 변경 완료인데 조회값이 다름 | 요청 완료 ≠ 실제 상태. 재조회·변경 알림 확인 |
  | 구독했지만 알림 없음 | 등록 영역과 오류 메시지 확인 |
  | 구독 해제 시 등록 영역 오류 | 등록했던 영역으로 되돌린 뒤 해제 |

- 기능은 Supported인데 특정 영역 조회가 실패하는 경우가 있음(예: Door Lock에서 `area ID: 0x1 not supported`). 기능 전체 지원과 특정 영역 사용 가능 여부를 구분할 것.
### Sample App 전체 public permission (참고)
```xml
<!-- Read / state -->
CAR_CLIMATE, CAR_CONNECT_VENDOR_CONTROL, CAR_DOORS, CAR_DRIVE, CAR_ENERGY, CAR_INFO,
CAR_LIGHTS, CAR_MILEAGE, CAR_MIRRORS, CAR_POWERTRAIN, CAR_SAFETY, CAR_SEATS, CAR_SPEED,
CAR_TOLL, CAR_TIRES, CAR_WINDOWS
<!-- Control -->
CONTROL_CAR_CLIMATE, CONTROL_CAR_DOORS, CONTROL_CAR_DRIVE, CONTROL_CAR_ENERGY, CONTROL_CAR_INFO,
CONTROL_CAR_LIGHTS, CONTROL_CAR_MIRRORS, CONTROL_CAR_SAFETY, CONTROL_CAR_SEATS, CONTROL_CAR_TOLL,
CONTROL_CAR_WINDOWS, READ_CAR_DISPLAY_UNITS, READ_CAR_STEERING, READ_ADAS_STATES,
READ_WINDSHIELD_WIPERS, CONTROL_WINDSHIELD_WIPERS
```
(모두 `pleos.car.permission.` 접두사로 선언. `PRIVATE_*` permission은 예제에 포함하지 않음.)
 
---

## 11. Vehicle API 메서드 목록 (API Reference 색인)

> API Reference(https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/entry) 페이지 목록에서 메서드 이름만 추린 색인입니다. 각 메서드의 파라미터·반환 타입·필요 권한은 원문 API Reference에서 확인하세요.
> 표기 규칙: `unregisterXxx`(register의 짝)와 `checkXxxCapabilities`(기능별 capability 확인)는 생략했습니다. 괄호 안 숫자는 생략분 포함 전체 메서드 수입니다.
> 진입점 `Vehicle`: `initialize`, `release`, `getBrake`, `getCarInfo`, `getDisplay`, `getDoor`, `getDrivingMode`, `getEvBattery`, `getFuel`, `getHVAC`, `getLight`, `getOdometer`, `getSafety`, `getSeat`, `getSideMirror`, `getSteeringWheel`, `getTire`, `getToll`, `getTurnSignal`, `getWindow`, `getWiper`

- **Brake** (4): getCurrentBrakePedalStatus, registerBrakePedalStatus
- **CarInfo** (4): getCRNInfo, getVINInfo
- **Display** (4): getCurrentTemperatureDisplayUnit, registerTemperatureDisplayUnit
- **Door** (15): getCurrentDoorPosition, getCurrentGloveBoxLockState, getDoorLockState, registerDoorLockState, registerDoorPosition, registerGloveBoxLockState, setDoorLock, setDoorPosition, setGloveBoxLock
- **DrivingMode** (12): getCruiseControlState, getCruiseControlTargetSpeed, getCurrentDrivingModeState, registerCruiseControlState, registerCruiseControlTargetSpeed, registerDrivingModeState
- **EvBattery** (30): getCumulativeEvEfficiency, getCurrentEvEfficiency, getEvBatteryStateOfCharge, getEvChargeCurrentDrawLimit, getEvChargeSwitchState, getInstantaneousEvEfficiency, getSinceChargeEvEfficiency, registerCumulativeEvEfficiency, registerCurrentEvEfficiency, registerEvBatteryStateOfCharge, registerInstantaneousEvEfficiency, registerSinceChargeEvEfficiency, setEvChargeCurrentDrawLimit, setEvChargePercentLimit, setEvChargePortOpen, setEvChargeSwitch
- **Fuel** (20): getCumulativeFuelEconomy, getCurrentFuelEconomy, getFuelPercent, getInstantaneousFuelEconomy, getSinceChargeFuelEconomy, registerCumulativeFuelEconomy, registerCurrentFuelEconomy, registerFuelPercent, registerInstantaneousFuelEconomy, registerSinceChargeFuelEconomy
- **HVAC** (67): getHvacAcState, getHvacAirCleanerState, getHvacAutoRecirculationState, getHvacAutoState, getHvacDefrosterState, getHvacDualState, getHvacFanDirection, getHvacFanSpeed, getHvacMaxAcState, getHvacMaxDefrostState, getHvacPowerState, getHvacRecirculationState, getHvacTemperature, getHvacTemperatureCurrent, registerHvacAcState, registerHvacAirCleanerState, registerHvacAutoRecirculationState, registerHvacAutoState, registerHvacDefrosterState, registerHvacDualState, registerHvacFanSpeed, registerHvacMaxAcState, registerHvacMaxDefrostState, registerHvacPowerState, registerHvacRecirculationState, registerHvacTemperature, registerHvacTemperatureCurrent, setHvacAc, setHvacAirCleaner, setHvacAuto, setHvacAutoRecirculation, setHvacDefroster, setHvacDual, setHvacFanDirection, setHvacFanSpeed, setHvacMaxAc, setHvacMaxDefrost, setHvacPower, setHvacRecirculation, setHvacTemperature
- **Light** (71): getAmbientColorAutoByDriveModeEnabled, getAmbientLightBrightness, getAmbientLightBrightnessColor, getAmbientLightState, getAmbientLightSwitch, getCabinLightSwitch, getCurrentCabinLightState, getCurrentFrontFogLightState, getCurrentHighBeamLightState, getCurrentLowBeamLightState, getCurrentReadingLightState, getCurrentRearFogLightState, getCurrentTailLampLightState, getFrontFogLightSwitch, getHighBeamLightSwitch, getLowBeamLightSwitch, getReadingLightSwitch, getRearFogLightSwitch, getTailLampLightSwitch, registerAmbientColorAutoByDriveModeEnabled, registerAmbientLightBrightnessColorListener, registerAmbientLightBrightnessListener, registerAmbientLightStateListener, registerCabinLightState, registerFrontFogLightState, registerHighBeamLightState, registerLowBeamLightState, registerReadingLightState, registerRearFogLightState, registerTailLampLightState, setAmbientColorAutoByDriveModeEnabled, setAmbientLightBrightness, setAmbientLightBrightnessColor, setAmbientLightSwitch, setCabinLightSwitch, setFrontFogLightSwitch, setHighBeamLightSwitch, setLowBeamLightSwitch, setReadingLightSwitch, setRearFogLightSwitch, setTailLampLightSwitch
- **Odometer** (35): getCurrentDistanceDriven, getTripCumulativeDrivingDistance, getTripCumulativeDrivingTime, getTripCurrentDrivingDistance, getTripCurrentDrivingTime, getTripInstantaneousDrivingDistance, getTripSinceRefillDrivingDistance, getTripSinceRefillDrivingTime, registerDistanceDriven, registerTripCumulativeDrivingDistance, registerTripCumulativeDrivingTime, registerTripCurrentDrivingDistance, registerTripCurrentDrivingTime, registerTripInstantaneousDrivingDistance, registerTripSinceRefillDrivingDistance, registerTripSinceRefillDrivingTime, setRangeRemainingDistance
- **Safety** (37): getBrakeFluidLevelState, getEngineOilLevelState, getTelltalesInfo, getTransmissionOilLevelState, hasLowWasherFluidWarning, hasPowerDownWarningIndicator, hasSeatAirbagBeenDeployed, hasSeatAirbagEnabled, hasSeatBeltNotBuckledWarning, registerBrakeFluidLevelState, registerEngineOilLevelState, registerLowWasherFluidWaringState, registerPowerDownWarningIndicator, registerSeatAirbagDeployed, registerSeatAirbagEnabled, registerSeatBeltNotBuckledWarning, registerTelltales, registerTransmissionOilLevelState
- **Seat** (97): getCurrentSeatBackrestAnglePosition, getCurrentSeatBeltBuckledState, getCurrentSeatBeltHeightPosition, getCurrentSeatDepthPosition, getCurrentSeatForeAftPosition, getCurrentSeatHeadrestAnglePosition, getCurrentSeatHeadrestForeAftPosition, getCurrentSeatHeadrestHeightAftPosition, getCurrentSeatHeightPosition, getCurrentSeatLumbarForeAftPosition, getCurrentSeatLumbarSideSupportPosition, getCurrentSeatOccupancy, getCurrentSeatTiltPosition, getHvacSeatTemperature, getHvacSeatVentilationState, moveSeatBackrestAngle, moveSeatBeltHeight, moveSeatDepth, moveSeatForeAft, moveSeatHeadrestAngle, moveSeatHeadrestForeAft, moveSeatHeadrestHeight, moveSeatHeight, moveSeatLumbarForeAft, moveSeatLumbarSideSupport, moveSeatTilt, registerHvacSeatTemperature, registerHvacSeatVentilation, registerSeatBackrestAnglePosition, registerSeatBeltBuckledState, registerSeatBeltHeightPosition, registerSeatDepthPosition, registerSeatForeAftPosition, registerSeatHeadrestAnglePosition, registerSeatHeadrestForeAftPosition, registerSeatHeadrestHeightAftPosition, registerSeatHeightPosition, registerSeatLumbarForeAftPosition, registerSeatLumbarSideSupportPosition, registerSeatOccupancy, registerSeatTiltPosition, selectSeatMemory, setHvacSeatTemperature, setHvacSeatVentilation
- **SideMirror** (5): getHvacSideMirrorHeatState, registerHvacSideMirrorHeatState, setHvacSideMirrorHeat
- **SteeringWheel** (9): getCurrentSteeringWheelAngle, getHvacSteeringWheelHeatState, registerHvacSteeringWheelHeatState, registerSteeringWheelAngle, setHvacSteeringWheelHeat
- **Tire** (3): getLowTirePressure
- **Toll** (5): getCurrentEtcsVoiceGuidanceVolume, registerEtcsVoiceGuidanceVolume, setEtcsVoiceGuidanceVolume
- **TurnSignal** (4): getCurrentTurnSignalState, registerTurnSignalState
- **Window** (29): getCurrentSmartGlassLevel, getCurrentSmartGlassState, getCurrentWindowBlindPosition, getCurrentWindowLockState, getCurrentWindowPosition, getSmartGlassConfig, moveWindowBlind, registerSmartGlassLevel, registerSmartGlassState, registerWindowBlindPosition, registerWindowLockState, registerWindowPosition, setSmartGlassLevel, setSmartGlassState, setSmartGlassZoneState, setWindowLock, setWindowPosition
- **Wiper** (7): getCurrentWiperState, getCurrentWiperSwitch, registerWiperState, setWiperSwitch
### 주요 상수(enum) 일부
- Area: `DoorValue`(HOOD, REAR, ROW_1~3_LEFT/RIGHT), `SeatValue`(ROW_1~3_LEFT/CENTER/RIGHT), `WindowValue`(FRONT/REAR_WINDSHIELD, ROOF_TOP_1/2, ROW_1~2_LEFT/RIGHT), `MirrorValue`, `WheelValue`, `TurnSignalValue`(LEFT, OFF, RIGHT)
- `DrivingModeType`: ECO, NORMAL, SPORT
- `EvChargeState`: CHARGING, ERROR, FULLY_CHARGED, NOT_CHARGING, UNKNOWN
- `CruiseControl`: ACTIVATED, ENABLED, FORCED_DEACTIVATION_WARNING, OTHER, SUSPENDED, USER_OVERRIDE
- `TripStatisticType`: CUMULATIVE, CURRENT, INSTANTANEOUS, SINCE_REFILL
- 단위: `DistanceDisplayUnit`(KILOMETER, MILE), `EvBatteryDisplayUnit`(AMPERE_HOURS, KILOWATT_HOUR, WATT_HOUR), `SpeedDisplayUnit`, `TemperatureUnit`
- Telltale(경고등): `TelltaleData`(color, priority, state, type, warningCount), `TelltaleType`에 LOW_EV_BATTERY, LOW_FUEL, SEAT_BELT, TPMS, OVER_SPEED, PARKING_BRAKE 등 다수
> ⚠️ 정리하면서 확인한 점: 이 색인에는 **차속(speed)이나 기어(gear) 조회 메서드가 없습니다**. SDK 릴리즈 노트상 v2.3.2에서 `VehicleSpeedListener`(onSpeedUpdated)가 삭제됐습니다(01 문서 참고). 기획 가이드의 SDK API 정책에 따르면 **속도·기어·외기 온도·연료 레벨 같은 표준 데이터는 AAOS Car API(`CarPropertyManager`/`VehiclePropertyIds`)로 읽고**, Pleos SDK는 제조사 특화 기능을 담당합니다. 주행/정차에 따른 화면 제어는 `CarUxRestrictions`로 구현합니다. 05 기획 가이드와 06 개발 가이드를 함께 확인하세요.
 
