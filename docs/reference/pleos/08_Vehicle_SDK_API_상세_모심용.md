# Vehicle SDK API 상세 — 모심에서 쓰는 메서드

> 출처: Pleos Playground Vehicle SDK API Reference (Dokka 생성 문서)
> `https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/<패키지>/<클래스>/<메서드>`
> 수집일: 2026-11-08 · 기준 버전: Vehicle SDK 2.3.2
> **"시그니처" 블록은 API Reference 원문 그대로다.** "사용 예" 블록은 시그니처를 바탕으로 정리한 것이며 공식 예제가 아니다.
> 최종 확인은 Gradle에 받아진 SDK 라이브러리에서 한다. 문서와 라이브러리가 다르면 라이브러리를 따른다.

---

## 0. 공통 규칙 (모든 메서드에 해당)

### 진입점 `ai.pleos.playground.vehicle.Vehicle`

```kotlin
constructor(context: Context)   // vehicle 서비스에 바인딩할 Context
fun initialize()                // 앱 생성 시 호출
fun release()                   // 앱 종료 시 호출
fun getSeat(): Seat             // getDoor(), getHVAC(), getEvBattery(), getFuel() 도 같은 형태
```

### 리스너 공통 부모 `VehicleListener`

```kotlin
sealed interface VehicleListener {
    abstract fun onFailed(e: Exception)   // 모든 리스너가 반드시 구현
}
```

모든 `XxxListener`는 `VehicleListener`를 상속한다. 즉 **리스너를 만들 때 `onXxxUpdated`와 `onFailed` 두 개를 구현**해야 한다.

### Capability 모델

```kotlin
data class CapabilityConfig(val accessType: AccessType = AccessType.NONE)
enum AccessType { NONE, READ, WRITE, READ_WRITE }
data class AreaBounds<T>(val minValue: T, val maxValue: T)
```

`CapabilityConfig`에는 접근 수준만 있다. **값 범위(`AreaBounds`)는 콜백의 두 번째 인자로 따로 온다.**

### Area

```kotlin
class SeatArea(vararg values: SeatValue) : Area   // val values: Array<out SeatValue>, equals/hashCode 내용 비교
class DoorArea(vararg values: DoorValue) : Area   // val values: Array<out DoorValue>, equals/hashCode 내용 비교

enum SeatValue { ROW_1_LEFT, ROW_1_CENTER, ROW_1_RIGHT, ROW_2_LEFT, ROW_2_CENTER, ROW_2_RIGHT,
                 ROW_3_LEFT, ROW_3_CENTER, ROW_3_RIGHT }
enum DoorValue { ROW_1_LEFT, ROW_1_RIGHT, ROW_2_LEFT, ROW_2_RIGHT, ROW_3_LEFT, ROW_3_RIGHT, HOOD, REAR }
```

`[확인 필요]` 동승석이 `ROW_1_RIGHT`인지(좌핸들 기준)는 에뮬레이터에서 확인한다.

### 기타 enum

```kotlin
enum MoveControl { BACKWARD, STOP, FORWARD }            // 좌석 이동 방향
enum TemperatureUnit { CELSIUS, FAHRENHEIT }
```

### 반환 형태 요약

| 종류 | 형태 |
|---|---|
| Get | `onComplete: (T?) -> Unit, onFailed: (Exception) -> Unit` — 값은 **nullable** |
| Set / Move | `onComplete: (Area, T?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {}` — 둘 다 기본값이 있어 생략 가능 |
| Register / Unregister | `(area, listener)` 또는 `(listener)` — 등록과 같은 객체·area로 해제 |
| Capability | `onComplete: (CapabilityConfig, List<Area>)` 또는 `(CapabilityConfig, Map<Area, AreaBounds<T>>)` |

Capability API는 `CapabilityConfig`가 없는 옛 오버로드가 **Deprecated**다. 항상 `CapabilityConfig`를 받는 오버로드를 쓴다.

---

## 1. Seat — 착석 감지 (탑승 감지 핵심)

권한: `pleos.car.permission.CAR_SEATS`

### 시그니처

```kotlin
// Seat
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkSeatOccupancyCapabilities(
    onComplete: (CapabilityConfig, List<SeatArea>) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getCurrentSeatOccupancy(
    area: SeatArea,
    onComplete: (Boolean?) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun registerSeatOccupancy(area: SeatArea, listener: SeatOccupancyListener)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun unregisterSeatOccupancy(area: SeatArea, listener: SeatOccupancyListener)

// Listener
interface SeatOccupancyListener : VehicleListener {
    abstract fun onSeatOccupancyUpdated(occupancy: Boolean?)
}
```

### 사용 예 (정리본)

```kotlin
val seat = vehicle.getSeat()
val passengerSeat = SeatArea(SeatValue.ROW_1_RIGHT) // [확인 필요] 동승석 위치

seat.checkSeatOccupancyCapabilities(
    onComplete = { config, areas ->
        val readable = config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE
        if (!readable || passengerSeat !in areas) {
            // 착석 감지 미지원 → 대체 흐름
        }
    },
    onFailed = { e -> /* 로그 */ }
)

seat.getCurrentSeatOccupancy(
    area = passengerSeat,
    onComplete = { occupied: Boolean? -> /* null 이면 값 없음 */ },
    onFailed = { e -> }
)

val occupancyListener = object : SeatOccupancyListener {
    override fun onSeatOccupancyUpdated(occupancy: Boolean?) { /* 착석 변화 */ }
    override fun onFailed(e: Exception) { }
}
seat.registerSeatOccupancy(passengerSeat, occupancyListener)
// 해제는 같은 area, 같은 객체로
seat.unregisterSeatOccupancy(passengerSeat, occupancyListener)
```

---

## 2. Seat — 안전벨트 체결

권한: `pleos.car.permission.CAR_SEATS`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkSeatBeltBuckledCapabilities(
    onComplete: (CapabilityConfig, List<SeatArea>) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getCurrentSeatBeltBuckledState(
    area: SeatArea,
    onComplete: (Boolean?) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun registerSeatBeltBuckledState(area: SeatArea, listener: SeatBeltBuckledStateListener)

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun unregisterSeatBeltBuckledState(area: SeatArea, listener: SeatBeltBuckledStateListener)

interface SeatBeltBuckledStateListener : VehicleListener {
    abstract fun onSeatBeltBuckledStateUpdated(state: Boolean?)
}
```

사용 형태는 착석 감지와 같다. `true`가 체결로 보이지만 `[확인 필요]` 에뮬레이터에서 값 의미를 확인한다.
참고: Virtual Console에는 벨트 전용 카드가 없고, Safety > Telltales에 안전벨트 경고등만 있다. `[확인 필요]` 벨트 상태를 에뮬레이터에서 바꿀 수 있는지.

---

## 3. Door — 도어 열림/닫힘

권한: `pleos.car.permission.CAR_DOORS`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_DOORS")
abstract fun checkDoorPositionCapabilities(
    onComplete: (CapabilityConfig, Map<DoorArea, AreaBounds<Int>>) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_DOORS")
abstract fun getCurrentDoorPosition(
    area: DoorArea,
    onComplete: (Int?) -> Unit,
    onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_DOORS")
abstract fun registerDoorPosition(area: DoorArea, listener: DoorPositionListener)

interface DoorPositionListener : VehicleListener {
    abstract fun onPositionUpdated(pos: Int?)
}
```

값은 `Int`이고, 범위는 Capability의 `AreaBounds`로 확인한다. Sample App 가이드 예시 범위는 `0..1`(닫힘/열림 추정)이지만 다른 환경에서 같다고 가정하지 않는다.

---

## 4. HVAC — 실내 온도 (조회·설정)

권한: 조회 `CAR_CLIMATE`, 설정 `CONTROL_CAR_CLIMATE`

### 시그니처

```kotlin
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun checkHvacTemperatureCapabilities(
    onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?) -> Unit,
    onFailed: (Exception) -> Unit)
// 범위는 °C. HvacTemperatureConfigArray 가 null 이면 일반 변환 공식 사용

@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun checkHvacTemperatureCurrentCapabilities(
    onComplete: (CapabilityConfig, List<SeatArea>) -> Unit,
    onFailed: (Exception) -> Unit)

// 설정 온도(목표 온도) 조회
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun getHvacTemperature(
    area: SeatArea, unit: TemperatureUnit,
    onComplete: (Float?) -> Unit,
    onFailed: (Exception) -> Unit)

// 현재 실내 온도 조회 (°C)
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun getHvacTemperatureCurrent(
    area: SeatArea,
    onComplete: (Float?) -> Unit,
    onFailed: (Exception) -> Unit)
// 단위 지정 오버로드도 있음: getHvacTemperatureCurrent(area, unit, onComplete, onFailed)

// 목표 온도 변경 구독
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun registerHvacTemperature(area: SeatArea, listener: HvacTemperatureListener)       // °C만
abstract fun registerHvacTemperature(area: SeatArea, listener: HvacTemperatureValueListener)  // °C + °F

// 현재 실내 온도 구독
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun registerHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureListener)
abstract fun registerHvacTemperatureCurrent(area: SeatArea, listener: HvacTemperatureValueListener)

// 목표 온도 설정
@RequiresPermission("pleos.car.permission.CONTROL_CAR_CLIMATE")
abstract fun setHvacTemperature(
    area: SeatArea, value: Float, unit: TemperatureUnit,
    onComplete: (SeatArea, Float?) -> Unit = { _, _ -> },
    onFailed: (Exception) -> Unit = {})

// 공조 전원
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun checkHvacPowerCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CAR_CLIMATE")
abstract fun getHvacPowerState(area: SeatArea, onComplete: (Boolean?) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CONTROL_CAR_CLIMATE")
abstract fun setHvacPower(area: SeatArea, value: Boolean,
    onComplete: (SeatArea, Boolean?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})

// Listener
interface HvacTemperatureListener : VehicleListener {
    abstract fun onHvacTemperatureUpdated(temperature: Float?)            // °C
}
interface HvacTemperatureValueListener : VehicleListener {
    abstract fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?)
}

data class HvacTemperatureConfigArray(
    val minCelsius: Float, val maxCelsius: Float, val incrementCelsius: Float,
    val minFahrenheit: Float, val maxFahrenheit: Float, val incrementFahrenheit: Float,
    val raw: List<Int>)   // VHAL 원본 정수값(×10)
```

### 주의

- **설정 가능 범위는 차량마다 다르다.** `setHvacTemperature` 전에 `checkHvacTemperatureCapabilities`의 두 번째 인자(`Map<SeatArea, AreaBounds<Float>>`)에서 `minValue`/`maxValue`를 읽는다. `CapabilityConfig`에는 범위가 없다.
- `onComplete`의 값은 **요청한 값**이다. 실제 반영은 `registerHvacTemperature`나 재조회로 확인한다.
- 모심의 "설정 온도 + 외기 온도 쌍 저장"에서 설정 온도는 `getHvacTemperature`(목표 온도), 실내 온도는 `getHvacTemperatureCurrent`다. 외기 온도는 Vehicle SDK에 없다(AAOS).
- Virtual Console 목표 온도 범위: 16~30°C, 0.5°C 단위.

### 사용 예 (정리본)

```kotlin
val hvac = vehicle.getHVAC()
val driver = SeatArea(SeatValue.ROW_1_LEFT)

hvac.checkHvacTemperatureCapabilities(
    onComplete = { config, bounds, _ ->
        val range = bounds[driver]                // AreaBounds<Float>? (°C)
        val target = 21.5f.coerceIn(range?.minValue ?: 16f, range?.maxValue ?: 30f)
        if (config.accessType == AccessType.WRITE || config.accessType == AccessType.READ_WRITE) {
            hvac.setHvacTemperature(
                area = driver, value = target, unit = TemperatureUnit.CELSIUS,
                onComplete = { area, requested -> /* 요청 완료. 실제 값은 리스너로 확인 */ },
                onFailed = { e -> }
            )
        }
    },
    onFailed = { e -> }
)
```

---

## 5. Seat — 좌석 위치·등받이 (제어)

권한: 조회 `CAR_SEATS`, 이동 `CONTROL_CAR_SEATS`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkMoveSeatForeAftCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CONTROL_CAR_SEATS")
abstract fun moveSeatForeAft(area: SeatArea, value: MoveControl,
    onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getCurrentSeatForeAftPosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)
interface SeatForeAftPositionListener : VehicleListener { abstract fun onSeatForeAftPositionUpdated(position: Int?) }

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkMoveSeatBackrestAngleCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CONTROL_CAR_SEATS")
abstract fun moveSeatBackrestAngle(area: SeatArea, value: MoveControl,
    onComplete: (SeatArea, MoveControl?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getCurrentSeatBackrestAnglePosition(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)
interface SeatBackrestAnglePositionListener : VehicleListener { abstract fun onSeatBackrestAnglePositionUpdated(position: Int?) }
```

### 중요: 좌석은 "위치값 설정"이 아니라 "방향 이동"이다

`moveSeatForeAft`와 `moveSeatBackrestAngle`은 목표 위치(숫자)를 받지 않는다. `MoveControl.FORWARD / BACKWARD / STOP`으로 **움직이기 시작하고 멈추는** 방식이다.
그래서 "저장된 좌석 위치로 맞추기"는 이렇게 구현해야 한다.

1. `registerSeatForeAftPosition`으로 현재 위치를 구독한다.
2. 목표보다 뒤에 있으면 `FORWARD`, 앞에 있으면 `BACKWARD`로 이동을 시작한다.
3. 위치가 목표에 도달하면 `STOP`을 보낸다.
4. 일정 시간 안에 도달하지 못하면 `STOP`을 보내고 실패로 처리한다(무한 이동 방지).

`[확인 필요]` 위치값의 범위와 단위, 이동 속도, Virtual Console에 좌석 위치 카드가 없어서 에뮬레이터에서 동작하는지.
`selectSeatMemory`(시트 메모리 호출) API도 있다. 위치 복원은 이쪽이 더 단순할 수 있다. `[확인 필요]`

---

## 6. Seat — 시트 열선·통풍

권한: 조회 `CAR_SEATS`, 설정 `CONTROL_CAR_SEATS`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkSeatTemperatureCapabilities(
    onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getHvacSeatTemperature(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CONTROL_CAR_SEATS")
abstract fun setHvacSeatTemperature(area: SeatArea, value: Int,
    onComplete: (SeatArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})
interface HvacSeatTemperatureListener : VehicleListener { abstract fun onHvacSeatTemperatureUpdated(temperature: Int?) }

@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun checkSeatVentilationCapabilities(
    onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CAR_SEATS")
abstract fun getHvacSeatVentilationState(area: SeatArea, onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)
@RequiresPermission("pleos.car.permission.CONTROL_CAR_SEATS")
abstract fun setHvacSeatVentilation(area: SeatArea, value: Int,
    onComplete: (SeatArea, Int?) -> Unit = { _, _ -> }, onFailed: (Exception) -> Unit = {})
interface HvacSeatVentilationListener : VehicleListener { abstract fun onHvacSeatVentilationUpdated(ventilation: Int?) }
```

값은 **단계(step) 정수**다. 범위는 Capability의 `AreaBounds<Int>`로 확인한다.
`[확인 필요]` Virtual Console의 Seat Temperature 카드는 "통풍 3단계, Off, 열선 3단계"를 한 카드로 다룬다. 열선이 양수, 통풍이 음수인 한 값인지, 열선·통풍이 별도 값인지 에뮬레이터에서 확인한다.

---

## 7. EvBattery — 배터리 잔량(SOC)

권한: `pleos.car.permission.CAR_ENERGY`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun checkEvBatteryStateOfChargeCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun getEvBatteryStateOfCharge(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)   // %

@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun registerEvBatteryStateOfCharge(listener: EvBatteryStateOfChargeListener)

interface EvBatteryStateOfChargeListener : VehicleListener {
    abstract fun onEvBatteryStateOfChargeUpdated(value: Int?)   // %
}
```

Area 인자가 없다. 값은 퍼센트(%) 정수.

---

## 8. Fuel — 연료량

권한: `pleos.car.permission.CAR_ENERGY`

```kotlin
@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun checkFuelPercentCapabilities(onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit)

@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun getFuelPercent(onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit)   // %

@RequiresPermission("pleos.car.permission.CAR_ENERGY")
abstract fun registerFuelPercent(listener: FuelPercentListener)

interface FuelPercentListener : VehicleListener {
    abstract fun onFuelPercentUpdated(value: Int?)   // %
}
```

연료량은 Vehicle SDK의 이 API로 읽을 수 있다(2.3.2 신규). 퍼센트라서 "남은 주행거리(km)"로 바꾸려면 탱크 용량과 연비가 따로 필요하다.

---

## 9. 주행가능거리 — Vehicle SDK로는 읽을 수 없음

- Odometer 도메인에 있는 것은 `setRangeRemainingDistance(value: Float, ...)`(권한 `CONTROL_CAR_DRIVE`) 하나뿐이고, **Deprecated (with error)** 상태다. 쓰면 안 된다.
- 조회용 메서드는 API Reference에 없다. `RangeRemainingListener`는 문서에서 숨겨졌다.
- 대안 `[확인 필요]`
  - AAOS `CarPropertyManager`의 `RANGE_REMAINING` 프로퍼티
  - EV는 NaviHelper `TripInfo.estimatedSocAtDest`(도착 예상 SOC)로 "도착까지 충분한가"를 직접 판단
  - 연료차는 `getFuelPercent` + 평균 연비(`Fuel.getCurrentFuelEconomy` 등)로 추정

---

## 10. 탑승 감지 조합 (모심 04단계)

모심 기획의 탑승 감지 조건과 API 대응.

| 조건 | API | 값 |
|---|---|---|
| 동승석 착석 | `Seat.registerSeatOccupancy(SeatArea(ROW_1_RIGHT), …)` | `Boolean?` |
| 안전벨트 체결 | `Seat.registerSeatBeltBuckledState(SeatArea(ROW_1_RIGHT), …)` | `Boolean?` |
| 도어 열림 → 닫힘 | `Door.registerDoorPosition(DoorArea(DoorValue.ROW_1_RIGHT), …)` | `Int?` (범위는 Capability) |
| 정차 상태 | Vehicle SDK에 없음 → AAOS `CarPropertyManager`(기어·차속) | — |

- 세 리스너 모두 `onFailed`를 구현한다. 하나라도 미지원이면 탑승 감지를 수동 확인 버튼으로 대체할 수 있게 둔다.
- 값이 `null`이면 "모름"으로 처리한다. `false`로 취급하지 않는다.
- 리스너는 화면이 아니라 앱 수준 객체에서 등록·해제한다. 화면 이동만으로는 해제되지 않는다.

---

## 11. 문서에서 발견한 주의점

- `Vehicle` 클래스 페이지의 Callback 예제는 `vehicle.getGear()`와 `GearSelectionListener`를 쓴다. **2.3.2에는 Gear 도메인이 없다.** 예전 버전 예제로 보이니 따라 하지 않는다.
- Door 페이지의 `checkDoorPositionCapabilities` 파라미터 설명에는 `List<DoorArea>`라고 적혀 있지만 시그니처는 `Map<DoorArea, AreaBounds<Int>>`다. 시그니처를 따른다.
