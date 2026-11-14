# SDK·API 현황

> 출처: Pleos Playground 개발자 문서(수집 2026-11-07, SDK 릴리즈 2026-10-19 기준), 설명회 내용, 모심 서비스 기획.
> 메서드 시그니처는 이 문서에 없다. 시그니처가 필요하면 `docs/reference/pleos/`를 본다.
> 에뮬레이터에서 직접 확인한 사람은 표의 "에뮬레이터 검증" 칸을 PR로 갱신한다.

## 1. 상태 정의와 코딩 규칙

| 상태 | 뜻 | 코딩 규칙 |
|---|---|---|
| ✅ 사용 가능 | 문서·라이브러리 공개, 포털에서 신청 없이 사용 | `docs/reference/pleos/`에서 시그니처를 확인한 뒤 `platform/pleos/`에 구현한다 |
| 🔜 Coming Soon | 포털 SDK 신청 화면에 "준비 중(Comming Soon)" 배지. 문서·라이브러리는 공개돼 있고, 설명회에서는 개발 단계에 매니페스트 선언만으로 에뮬레이터 테스트가 된다고 안내됨 | **인터페이스 + Mock까지만** 만든다. 에뮬레이터 검증 칸이 "됨"으로 바뀐 뒤에 실제 호출 코드를 쓴다 |
| ⛔ 종료 | 제공 종료 | 쓰지 않는다 |
| 🅰️ AAOS 표준 | Pleos SDK가 아닌 Android Automotive 표준 API | Pleos SDK에서 찾지 않는다. 코드 위치는 `docs/architecture.md`에서 정한다 `[확인 필요]` |
| 🌐 외부 API | Pleos 밖의 서비스. 공모전에서 사용 허용됨 | 인터페이스 + Mock. 서비스가 정해지면 구현한다. 키는 `local.properties` |
| ❓ 미확인 | 정보 부족 | 쓰지 않는다. 필요하면 팀에 알린다 |

## 2. 전체 목록

공통 Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`
좌표 형식: `ai.pleos.playground:<SDK>:<버전>`. 권한은 모두 `pleos.car.permission.` 접두사.

| 이름 | 상태 | 버전 | 의존성 | 권한 | 모심에서 쓰는 곳 | 에뮬레이터 검증 |
|---|---|---|---|---|---|---|
| Vehicle SDK – 조회(State) | ✅ | 2.3.2 | `Vehicle:2.3.2` | `CAR_*` (도메인별) | 착석·벨트·도어 감지, 배터리·연료, 실내 온도 조회 | 일부 됨: 공조 목표 온도 조회 (2026-11-12, 아래 검증 기록) |
| Vehicle SDK – 제어(Control) | 🔜 | 2.3.2 (같은 라이브러리) | `Vehicle:2.3.2` | `CONTROL_CAR_*` | 공조 온도, 좌석 위치·등받이, 시트 열선·통풍 적용 | 일부 됨: 공조 목표 온도 설정 됨. 좌석 등받이는 호출만 성공, 위치 변화 확인 불가 (2026-11-12, 아래 검증 기록) |
| NaviHelper | ✅ | 3.0.5 | `NaviHelper:3.0.5` | `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` | 경로·ETA, 주유소·충전소 경유지, 도착 감지, 도착 예상 SOC | 됨: 경로 요청, 남은 거리·시간, 현재 위치, 경유지 추가 (2026-11-12, 아래 검증 기록) |
| FusedLocation | 🔜 | 1.1.3 | `FusedLocation:1.1.3` | `FUSED_LOCATION` (**dangerous**) | 현재 위치, 픽업지 접근 감지(지오펜스) | 안 됨 |
| TextToSpeech | 🔜 | 3.3.1 | `TextToSpeech:3.3.1` | `TTS_SERVICE` | F-1 AI 제안 음성 안내 (개발자 승인 예외, 3.4 참고) | 됨 (3.4 참고) |
| SpeechToText | 🔜 | 3.3.1 | `SpeechToText:3.3.1` | `STT_SERVICE` | 동승자 음성 요청 (개발자 승인 예외, 아래 참고) | 부분 (3.4 참고) |
| LLM SDK | ⛔ | — | — | — | 쓰지 않음 (2026-10-19 종료) | — |
| Gleo Interface | ❓ | — | — | — | 포털이 AI 음성 연동용으로 안내. 내용 미확인 | — |

### 에뮬레이터 검증 기록

**2026-11-12 · Pleos Connect v2.1.0 arm64 에뮬레이터(API 34) · Vehicle 2.3.2**
근거: `app/src/androidTest/.../platform/pleos/PleosVehicleControlVerificationTest.kt` (2/2 통과), Virtual Console(에뮬레이터 안 8088 포트) 화면 확인.

- Maven 저장소는 인증 없이 받아진다.
- 공조 목표 온도
  - `checkHvacTemperatureCapabilities`: READ_WRITE, ROW_1_LEFT·ROW_1_RIGHT, **15.0~30.0℃** (Virtual Console 카드 설명의 16~30℃와 다르다)
  - `setHvacTemperature(ROW_1_LEFT, 25.5)` → Virtual Console 운전석이 25.5℃로 바뀜. 이어서 24.5로 설정 후 `getHvacTemperature`가 24.5를 돌려줌
  - **공조 전원이 꺼져 있으면 `getHvacTemperature`가 `PropertyNotAvailableException`으로 실패한다.** 설정은 전원과 상관없이 반영됐다
  - `setHvacPower(true)`는 바로 응답하지만 `getHvacPowerState`가 true가 되기까지 3초 넘게 걸렸다
  - 공조 전원 area는 하나다(1열 좌우 + 2열 전체 묶음)
- 좌석 등받이
  - `checkMoveSeatBackrestAngleCapabilities`: READ_WRITE, 1·2열 좌우 4곳
  - `checkSeatBackrestAnglePositionCapabilities`: 위치 범위 **0~100** (각도가 아니다)
  - `moveSeatBackrestAngle(ROW_1_RIGHT, FORWARD)` 3초 → `STOP`: 둘 다 성공 응답. 위치는 0 → 0으로 변화 없음
  - Virtual Console에 좌석 위치 카드가 없다. 실제 이동은 에뮬레이터로 확인할 수 없다
  - 추가 확인: Emulation Controls 48개 항목에도 좌석 위치·등받이 항목이 없다(좌석은 Seat Temperature, Seat Occupancy만)
  - 추가 확인: FORWARD·BACKWARD를 각 10초 움직여도 위치 0. `registerSeatBackrestAnglePosition` 구독은 처음 0 한 번만 오고 이동 중 갱신 없음
  - `checkSeatMemoryCapabilities`: 운전석(ROW_1_LEFT)만, WRITE, 슬롯 1~2. 동승석 등받이를 메모리로 맞추는 방법은 없다

**2026-11-12 · Pleos Connect v2.1.0 arm64 에뮬레이터(API 34) · NaviHelper 3.0.5**
근거: `PleosNaviHelperVerificationTest`, `PleosTripSourceDeviceTest` (통과), 내비 화면 확인.

- `initialize(onConnected, onFailed)`: onConnected가 온다
- `getCurrentLocationInfo` → 37.4127212, 127.094681 ("수정구 시흥동", 에뮬레이터 기본 위치)
- `requestRoute`: `onRouteStarted(duration=3780, distance=23000)`가 온다. **`onRoutingCompleted`는 30초 기다려도 오지 않았다**
  - 요청하면 **Pleos 내비 화면이 앞으로 나온다**
- 단위: `DestinationInfo.remainDistance`는 m, `remainTime`은 초. `TripInfo.eta`도 **시각이 아니라 남은 초**(3939)였다
- 경로가 없을 때 `onDestinationInfo`는 이름이 비고 remainDistance=0인 값을 준다
- `addWaypoint(FIRST)` → `onWaypointChanged(index=FIRST, cmdType=ADD)`. 남은 시간 3780 → 5640초
- `onWayPointInfo`: 이름·남은 거리는 오지만 **좌표는 0.0**으로 온다
- **`requestTrip`을 부른 직후 `onRouteCancelled`가 와서 안내 중이던 경로가 취소됐다.** 원인 미확인 `[확인 필요]`. 안내 중에는 쓰지 않는다
- `onCurrentRoadSpeedLimit`은 정차 중 0만 왔다
| AAOS `CarPropertyManager` | 🅰️ | — | Android Car API | AAOS 권한 `[확인 필요]` | 차속, 기어(정차 판단), 외기 온도 | 안 됨 |
| AAOS `CarUxRestrictions` | 🅰️ | — | Android Car API | — | 주행/정차에 따른 화면 제한 | 안 됨 |
| 장소 검색 API | 🌐 미선정 | — | — | — | "근처 카페" 같은 요청 | — |
| 주차 정보 API | 🌐 미선정 | — | — | — | 목적지 주차 가능 여부 | — |
| 날씨 API | 🌐 미선정 | — | — | — | 출발 전 날씨 점검, 외기 보정 | — |
| 에이전트용 LLM API | 🌐 미정 | — | — | — | Agentic AI 오케스트레이션 검토 중(`project-status.md` 참고) | — |
| 음악·음량 제어 | ❓ | — | — | — | Vehicle SDK에 미디어 도메인 없음 | — |

## 3. SDK별 핵심 규칙

### 3.1 Vehicle SDK (2.3.2)

**사용 흐름**: `Vehicle(context)` 생성 → `initialize()` → `getSeat()`, `getHVAC()` 같은 `getXxx()`로 도메인 객체를 얻음 → API 호출 → `release()`

- 모든 결과는 **비동기**로 온다(`onComplete` / `onFailed`). 값을 바로 반환한다고 가정하지 않는다.
- 비동기 콜백이 끝나기 전에 `release()`를 부르지 않는다. 보통 `onDestroy`에서 부른다.
- API 종류는 네 가지다: Capability(`checkXxxCapabilities`, 지원 여부와 값 범위), Get(`getCurrentXxx`), Set(`setXxx`, `moveXxx`), Callback(`registerXxx`/`unregisterXxx`).
- 쓰기 전에 Capability로 지원 여부를 확인하고, 미지원이면 UI를 비활성화한다.
- Set이 성공했다고 실제 값이 바뀐 것은 아니다. 다시 조회하거나 Callback으로 확인한다.
- 리스너는 등록할 때 쓴 **같은 객체, 같은 Area**로 해제한다.
- 실패는 `VehicleException.errorCode`로 구분한다. 특히 `ERROR_SECURITY_PERMISSION`(권한), `ERROR_NOT_SUPPORTED_PROPERTY`/`ERROR_NOT_SUPPORTED_AREA`(미지원), `ERROR_DRIVING_RESTRICTED`(주행 중 제한)는 꼭 처리한다.

**모심에서 쓸 도메인과 메서드 이름** (시그니처는 reference에서 확인)

| 기능 | 도메인 | 메서드 | 권한 |
|---|---|---|---|
| 착석 감지 | Seat | `getCurrentSeatOccupancy`, `registerSeatOccupancy` | `CAR_SEATS` |
| 벨트 감지 | Seat | `getCurrentSeatBeltBuckledState`, `registerSeatBeltBuckledState` | `CAR_SEATS` |
| 도어 감지 | Door | `getCurrentDoorPosition`, `registerDoorPosition` | `CAR_DOORS` |
| 실내 온도 조회 | HVAC | `getHvacTemperature`, `getHvacTemperatureCurrent`, `registerHvacTemperature` | `CAR_CLIMATE` |
| 실내 온도 설정 🔜 | HVAC | `setHvacTemperature` | `CONTROL_CAR_CLIMATE` |
| 좌석 위치·등받이 🔜 | Seat | `moveSeatForeAft`, `moveSeatBackrestAngle` — 목표값이 아니라 FORWARD/BACKWARD/STOP 방향 이동. 구현 방법은 `docs/reference/pleos/08_Vehicle_SDK_API_상세_모심용.md` 5절 | `CONTROL_CAR_SEATS` |
| 시트 열선·통풍 🔜 | Seat | `setHvacSeatTemperature`, `setHvacSeatVentilation` (조회는 `getHvacSeatTemperature`, `getHvacSeatVentilationState`) | `CONTROL_CAR_SEATS` / `CAR_SEATS` |
| EV 배터리 | EvBattery | `getEvBatteryStateOfCharge`, `registerEvBatteryStateOfCharge` | `CAR_ENERGY` |
| 연료량 | Fuel | `getFuelPercent`, `registerFuelPercent` | `CAR_ENERGY` |
| 주행가능거리 | 없음 | Vehicle SDK로 조회 불가. `setRangeRemainingDistance`는 사용 중단(Deprecated). 대안은 `docs/reference/pleos/08_Vehicle_SDK_API_상세_모심용.md` 9절 | |

Area 예: 운전석 `SeatValue.ROW_1_LEFT`, 동승석 `SeatValue.ROW_1_RIGHT`. `[확인 필요]` 동승석이 RIGHT인지는 에뮬레이터에서 확인한다.

**함정 (2.3.2 변경 사항)**

- `VehicleSpeedListener`는 **삭제됐다.** 차속은 Vehicle SDK로 받을 수 없다 → AAOS `CarPropertyManager`.
- 기어 조회 메서드도 Vehicle SDK에 없다 → AAOS.
- 연료량은 Vehicle `Fuel.getFuelPercent`가 2.3.2에 새로 생겼다. 기획 가이드는 연료 레벨을 AAOS로 읽으라고 한다. 어느 쪽을 쓸지 `[확인 필요]`.
- `FanDirectionType` 값과 순서가 바뀌었다. ordinal을 저장하지 않는다.
- Area 클래스들(`SeatArea`, `DoorArea` 등)은 이제 내용으로 `equals`를 비교한다.
- 매니페스트에는 실제로 쓰는 API의 **최소 권한만** 선언한다. Sample App의 권한 목록을 통째로 복사하지 않는다.

### 3.2 NaviHelper (3.0.5)

**사용 흐름**: `NaviHelper(applicationContext)` → `initialize(onConnected, onFailed)` (권장 형태) → `addListener(NaviHelperEventListener)` → 요청·조회 → `removeListener()` → `release()`

- **조회 API(`get*Info`)는 값을 반환하지 않는다.** 결과는 대응하는 리스너 콜백으로 온다. 예: `getDestinationInfo()` → `onDestinationInfo()`.
- `requestRoute()`가 성공해도 경로 탐색이 끝난 것이 아니다. `onRoutingCompleted()`나 `onError()`로 확인한다.
- 3.0.5부터 데이터 모델 생성자의 좌표 순서는 **(latitude, longitude)**다.
- 경유지는 **최대 2개**(`WaypointIndex.FIRST`, `SECOND`).
- 경로 옵션 변경(`changeRouteOption`)과 재탐색(`requestReRoute`)은 활성 경로가 있을 때만 된다.
- 실제로 오는 이벤트는 Navigation 앱의 구현과 상태에 따라 다를 수 있다. 콜백이 안 와도 앱이 멈추지 않게 만든다.

**모심에서 쓸 것**

| 기능 | API / 콜백 | 권한 |
|---|---|---|
| 목적지 설정 | `requestRoute(RouteInfo)` | `NAVI_ROUTE_SEARCH` |
| 경유지 포함 경로(주유소·충전소) | `requestRouteWithWaypoints`, `addWaypoint` | `NAVI_ROUTE_SEARCH` |
| 편안한 경로 | `RouteOption.COMFORTABLE` | `NAVI_ROUTE_SEARCH` |
| 예상 거리·ETA·도착 예상 SOC | `requestTrip` → `onTripInfo` (`TripInfo.distance`, `eta`, `estimatedSocAtDest`) | `NAVI_ROUTE` |
| 남은 거리·시간 | `getDestinationInfo` → `DestinationInfo.remainDistance`, `remainTime` | `NAVI_ROUTE` |
| 도착 감지 | `onDestinationArrived` | |
| 근처 충전소 화면 | `requestNavigationScreen` (`nearbyChargingStations`) | `NAVI_CUSTOM_MAP` |

### 3.3 FusedLocation (1.1.3) 🔜

- 먼저 `isSupported()`로 확인하고, 미지원이면 위치 기능을 끈다.
- 흐름: 생성 → `initialize()` → `getLastKnownLocation()` 또는 `registerFusedLocationCallback()` → `unregisterFusedLocationCallback()` → `release()`. unregister와 release는 서로 대체할 수 없다.
- `onChanged(LocationData?)`의 값은 `null`일 수 있다. **`null`을 유효한 위치로 쓰지 않는다.**
- `onError(Exception)`은 반드시 구현한다.
- 권한이 **dangerous** 수준이라 런타임 권한 요청 흐름이 필요하다.
- 픽업지 접근 감지는 `addProximityAlert()` / `removeProximityAlert()`. 모델과 리스너 이름은 reference에서 확인한다.
- Android 표준 `ACCESS_FINE_LOCATION` 동작을 이 SDK가 보장하지 않는다.

### 3.4 TextToSpeech / SpeechToText (3.3.1) 🔜

> 2026-11-08 개발자 명시 승인: 이번 F-2 작업의 SpeechToText v3.3.1에 한해 실제 의존성·권한·호출 구현 및 에뮬레이터 사전 검증 제한의 예외를 허용한다. 설명회의 개발 단계 신청 불필요 안내와 공식 ON_DEVICE 앱 등록 불필요 안내가 근거다. 상태는 Coming Soon으로 유지한다.
> ~~TTS와 다른 SDK에는 적용하지 않는다.~~ → TTS 부분은 아래 2026-11-13 승인으로 대체됐다.
> 그 밖의 SDK에는 여전히 적용하지 않는다.

> 2026-11-13 개발자 명시 승인(TextToSpeech): F-1 AI 제안 음성 안내를 위해 TextToSpeech v3.3.1의
> 실제 의존성·권한·호출 구현을 허용한다. 2026-11-08 메모의 "TTS에는 적용하지 않는다"를 이 항목이 대체한다.
> 근거는 ON_DEVICE가 앱 등록 없이 쓰인다는 공식 안내다. 상태는 Coming Soon으로 유지한다.
>
> **에뮬레이터 검증: 됨 (2026-11-13).** Pleos Connect v2.1.0에서 전체 수명주기를 확인했다.
> `initialize - ON_DEVICE` → `addEventListener` → `setAudioAttributes(USAGE_ASSISTANT)` → `updateLocale ko_KR`
> → `generate(speaker=seoyoon, locale=ko_KR)` → AudioTrack 346,624바이트 재생 → `onDone`
> → `stop` → `removeEventListener` → `release`. **한국어 합성과 해제까지 실기에서 동작한다.**
> 다만 소리 청취·발음 품질·실차 음성 출력은 미검증이다. 로그와 재현 방법은 `docs/f-screens.md`에 있다.

> 2026-11-09 에뮬레이터 검증 결과(**부분**). Pleos Connect v2.1.0에서 확인한 범위는 아래와 같다.
> 상세한 로그와 재현 방법은 `docs/voice-stt.md`에 있다.
> - **된 것**: 의존성 해석, `STT_SERVICE` 권한 부여, `Mode.ON_DEVICE` 초기화, 리스너 등록,
>   `Locale.KOREA` 적용, `request()`로 마이크 점유, `stop`→`removeListener`→`release` 해제.
>   **한국어 인식 자체도 성공했다.** `sendAudio()`로 PCM을 넣어 부분 결과와 최종 결과를 받았고,
>   최종 텍스트가 입력 문장과 정확히 일치했다.
> - **안 된 것**: 사람이 차량 마이크에 말해서 인식되는 경로. 에뮬레이터 마이크는 무음만 들어왔다.
>   운영 화면이 쓰는 것은 `sendAudio()`가 아니라 `request()`라, 이 구간은 여전히 미검증이다.

- 모드: `HYBRID`(기본, 서버 우선 후 On-device로 전환), `SERVER`, `ON_DEVICE`.
- `HYBRID`/`SERVER`에서 서버 기능을 쓰려면 My Project의 Client ID/Secret으로 `registerApp()`을 해야 한다.
  **`ON_DEVICE`는 등록이 필요 없다.** 데모 안정성을 위해 `ON_DEVICE`부터 검증하는 것을 권장한다. `[확인 필요]` 에뮬레이터 동작.
- Client ID/Secret은 코드에 쓰지 않는다. `local.properties`에서 읽는다.
- 한국어는 `Locale.KOREA`(ko-KR). `speak()`나 `request()` 전에 `setLocale()`.
- TTS는 `onConnected()` 이후에 `speak()`를 부른다(등록이 필요한 모드일 때).
- STT는 **텍스트만 돌려준다.** 명령 실행이나 의도 파악은 하지 않는다. 의도 파악은 앱이 해야 한다(현재 보류).
- STT 리스너는 `request()` **전에** 등록한다. 최종 결과는 `onUpdated(stt, completed = true)`.
- 에뮬레이터에서 STT를 쓰려면 마이크 접근 설정이 필요하다.
- 오류는 `ErrorType`으로 온다. 값이 늘어날 수 있으니 `when`에 `else` 분기를 둔다.

### 3.5 AAOS 표준 API 🅰️

- 차속, 기어, 외기 온도 같은 표준 데이터는 `CarPropertyManager`로 읽는다(예: `PERF_VEHICLE_SPEED`).
- 주행/정차에 따른 UI 전환은 `CarUxRestrictions`로 처리한다.
- Pleos 정책상 같은 기능이 AAOS에 있으면 AAOS를 따른다.
- 일반 Android 에뮬레이터에는 Car API가 없을 수 있다. 화면 개발은 Mock으로 하고, 실제 동작은 Pleos 에뮬레이터에서 확인한다.

## 4. Mock 작성 기준

Coming Soon이나 미선정 API는 Mock으로 먼저 만든다. 나중에 실제 SDK로 바꿀 때 화면 코드가 안 바뀌도록 실제 SDK의 성질을 흉내 낸다.

- **비동기**로 만든다(`suspend` 또는 Flow). 즉시 값을 돌려주는 Mock은 실제 SDK로 바꿀 때 화면을 깨뜨린다.
- `null`과 실패를 돌려주는 경우를 만든다. 실패 사유는 SDK 오류 분류(권한 없음, 미지원, 주행 중 제한, 연결 실패)를 흉내 낸다.
- 값의 범위는 Virtual Console 기준에 맞춘다.

| 값 | 범위 |
|---|---|
| 목표 온도 | 16 ~ 30°C, 0.5°C 단위 |
| 시트 열선·통풍 | 통풍 3단계, Off, 열선 3단계 |
| 풍량 | 0 ~ 8단계 |
| 배터리 SOC | 0 ~ 100%, 1% 단위 |
| 주행가능거리 | 0 ~ 1,000,000m, 1,000m 단위. Vehicle SDK로는 조회 불가라 Mock 출처는 `[확인 필요]` — 후보는 08 문서 9절 참고 |
| 기어 | P, R, N, D |
| 좌석 탑승 | 운전석·동승석·뒷좌석 좌우 Empty / Occupied |
| 도어 | 앞·뒷좌석 좌우 열림 / 닫힘 |

- 차량을 바꾸는 Mock(온도·좌석 설정)은 바로 반영하지 말고, 설정 후 상태 스트림으로 바뀐 값을 내보낸다.
  실제 SDK도 Set 성공과 실제 반영이 다르기 때문이다.
- Mock 파일과 클래스 이름에 `Mock`을 붙이고, 상단 주석에 어떤 실제 API를 흉내 내는지 적는다.
