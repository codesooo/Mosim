# Pleos Connect — NaviHelper SDK · FusedLocation SDK

> 출처: Pleos Playground 개발자 문서 (https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro, .../FusedLocation/intro)
> 수집일: 2026-11-07 · 기준 버전: NaviHelper v3.0.5, FusedLocation v1.1.3
> Kotlin 예제만 수록했습니다(원문에는 Java 탭도 있음).
 
---

# Part A. NaviHelper SDK

## A1. Introduction
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/intro -->

NaviHelper SDK는 경로 안내 제어와 내비게이션 정보 조회 API를 제공합니다. 3rd party 앱은 NaviHelper SDK를 통해 Pleos Connect의 **Navi Service**와 연동됩니다. 앱이 목적지·경유지 설정, 경로 변경, 정보 조회, Navigation 화면 표시를 요청하면 Navi Service가 처리하고, 결과와 주행 상태 변화를 이벤트로 전달합니다.

**제공 기능**: 목적지 설정·확인·취소 / 경로 옵션 변경 및 재탐색 / 경유지 추가·조회·제거 / 경유지 포함 경로 요청 / 경로 탐색·주행 상태 이벤트 수신 / 내비게이션 정보 조회 및 Navigation 화면 요청.

요청은 Pleos Connect Navigation 앱에 전달되고, 결과와 주행 중 변경 사항은 `NaviHelperEventListener` 콜백으로 옵니다. **실제로 전달되는 이벤트는 Navigation 앱의 구현과 현재 상태에 따라 달라질 수 있습니다.**

**사용 순서**: `NaviHelper` 생성 → `initialize()` → `addListener()` → 경로 요청/정보 조회 API 호출 → 콜백 처리 → `removeListener()`·`release()`.

## A2. Importing NaviHelper SDK
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk -->
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
    }
}
 
// build.gradle.kts (app)
dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
}
```

## A3. Declaring permissions
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions -->

| Permission | 주요 API |
| --- | --- |
| pleos.car.permission.NAVI_ROUTE | get*Info(), requestTrip() |
| pleos.car.permission.NAVI_ROUTE_SEARCH | requestRoute(), requestRouteWithWaypoints(), cancelRoute(), requestReRoute(), addWaypoint(), removeWaypoint(), changeRouteOption() |
| pleos.car.permission.NAVI_CUSTOM_MAP | requestNavigationScreen() |

API별 정확한 요구 퍼미션은 API Reference의 `@RequiresPermission`에서 확인합니다.
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE" />
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE_SEARCH" />
    <uses-permission android:name="pleos.car.permission.NAVI_CUSTOM_MAP" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```

## A4. Initialization / Release
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release -->

앱 시작/생성 시점에 `NaviHelper` 객체를 만들고 `initialize()`를 호출해야 API를 사용할 수 있습니다.

| Overload | Description |
| --- | --- |
| initialize(onConnected, onFailed) | 서비스 연결 결과를 콜백으로 받음. 최초 연결 성공 시 onConnected, 초기 연결 재시도가 모두 실패하면 onFailed (**권장**) |
| initialize() | 인자 없이 초기화 |

```kotlin
import ai.pleos.playground.navi.helper.NaviHelper
 
val naviHelper = NaviHelper(applicationContext)
naviHelper.initialize(
    onConnected = {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 초기 연결 재시도가 모두 실패했습니다.
    }
)
```
```kotlin
override fun onDestroy() {
    naviHelper.removeListener(naviEventListener)
    naviHelper.release()
    super.onDestroy()
}
```
`release()` 이후 다시 쓰려면 객체 생성과 초기화를 다시 해야 합니다.

## A5. Event listeners
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners -->

| Category | Callback |
| --- | --- |
| 경로 탐색 | onRouteStarted(), onRoutingCompleted(), onRouteCancelled(), onReRouteEnded(), onRouteOptionChanged() |
| 목적지와 경유지 | onDestinationChanged(), onDestinationArrived(), onWaypointChanged(), onWayPointArrived() |
| 정보 조회 | onBookmarkInfo(), onBookMarkChanged(), onRecentDestinationInfo(), onCurrentLocationInfo(), onDestinationInfo(), onWayPointInfo(), onRouteStateInfo(), onTBTInfo(), onChargerOperatorInfo(), onTripInfo(), onNavigationStatus(), onDrivingInfo(), onCurrentRoadSpeedLimit(), onCameraAlert(), onCautionPointInfo() |
| 오류 | onError() |

```kotlin
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
 
private val naviEventListener = object : NaviHelperEventListener {
    override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
        // 경로 탐색 시작을 처리합니다.
    }
    override fun onRoutingCompleted() {
        // 경로 탐색 완료를 처리합니다.
    }
    override fun onError(errorCode: NaviErrorCode) {
        // 내비게이션 오류를 처리합니다.
    }
}
 
naviHelper.addListener(naviEventListener)
// 해제: 등록할 때 사용한 동일 인스턴스
naviHelper.removeListener(naviEventListener)
```

## A6. Destinations
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations -->

`requestRoute()`로 목적지 설정 → `getDestinationInfo()`로 확인 → `cancelRoute()`로 취소. 재설정도 `requestRoute()`.
```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
 
// 목적지 확인
naviHelper.getDestinationInfo()
 
// onDestinationInfo callback으로 데이터 수신
override fun onDestinationInfo(destinationInfo: DestinationInfo) {
    Log.d(logTag, "onDestinationInfo $destinationInfo")
}
 
// 목적지 취소
naviHelper.cancelRoute()
```

| Callback | Description |
| --- | --- |
| onRouteStarted(routeStartInfo) | Navigation 앱이 경로 탐색을 시작하면 호출 |
| onRoutingCompleted() | 경로 탐색 완료 |
| onDestinationChanged() | 목적지 설정/제거 |
| onError(errorCode) | 경로 요청 처리 중 오류 |

> `requestRoute()` 호출이 정상 수행돼도 경로 탐색 완료를 뜻하지 않습니다. 최종 결과는 이벤트 콜백으로 확인합니다. **정보 조회 API(get*)는 값을 직접 반환하지 않고, 대응하는 이벤트 콜백에서 결과를 받습니다.**

## A7. Routes (경로 옵션 변경·재탐색)
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/routes -->

| RouteOption | Description |
| --- | --- |
| RECOMMENDED | 추천 경로 |
| SHORT_DISTANCE | 최단 거리 |
| SHORT_TIME | 최단 시간 |
| FREE | 무료 도로 우선 |
| HIGHWAY | 고속도로 우선 |
| COMFORTABLE | 편안한 경로 |
| SCHOOL_ZONE | 스쿨존 고려 |

처음 설정 시 `RouteInfo.routeOption`으로 지정하거나, 안내 중 `changeRouteOption()`으로 변경합니다.
```kotlin
naviHelper.changeRouteOption(RouteOption.HIGHWAY)
 
override fun onRouteOptionChanged(routeOption: RouteOption) {
    Log.d(logTag, "onRouteOptionChanged $routeOption")
}
 
// 현재 위치·목적지 기준 재탐색
naviHelper.requestReRoute()
```
> 경로 옵션 변경과 재탐색은 **활성 경로가 있는 상태**에서 요청합니다.

## A8. Waypoints
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings -->

경유지는 **최대 두 개**(`WaypointIndex.FIRST`, `WaypointIndex.SECOND`).
```kotlin
// 경유지 추가 (목적지 설정 후)
naviHelper.addWaypoint(
    RequestWaypointInfo(
        latitude = 37.41275833,
        longitude = 127.09481944,
        waypointIndex = WaypointIndex.FIRST,
        poiId = "36khp2hdj2cfcm5nqhe3",
        poiName = "소프트웨어드림센터",
        poiSubId = "0",
        address = "경기도 성남시 수정구 창업로40번길 6"
    )
)
 
override fun onWaypointInfo(waypointInfo: List<WaypointInfo>) {
    Log.d(logTag, "onWaypointInfo $waypointInfo")
}
 
naviHelper.removeWaypoint(WaypointIndex.FIRST)   // 하나 삭제
naviHelper.getWaypointInfo()                     // 목록 요청 → onWayPointInfo()
naviHelper.removeWaypoint(WaypointIndex.ALL)     // 전부 삭제
```
경유지 도착 시 `onWayPointArrived()`가 호출될 수 있습니다.

## A9. Route requests with waypoints
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints -->

최종 목적지(`RouteInfo`)와 하나 이상의 경유지(`RequestWaypointInfo`)를 `RouteWithWaypointsRequest`로 한 번에 요청합니다. 권한 `NAVI_ROUTE_SEARCH` 필요.
```kotlin
naviHelper.requestRouteWithWaypoints(
    RouteWithWaypointsRequest(
        destination = RouteInfo(
            latitude = 37.48544722,
            longitude = 127.03636666,
            poiName = "42dot",
            poiId = "36khp37hh8gqcm5nqhe5",
            address = "서울시 강남구 남부순환로 2621",
            poiSubId = "0"
        ),
        waypoints = listOf(
            RequestWaypointInfo(
                latitude = 37.41275833,
                longitude = 127.09481944,
                poiName = "판교 현대백화점",
                waypointIndex = WaypointIndex.FIRST,
                poiId = "36khp2hdj2cfcm5nqhe3",
                address = "경기도 성남시 수정구 창업로 0번길 6",
                poiSubId = "0"
            )
        )
    )
)
```
결과는 `onRouteStarted`, `onRoutingCompleted`, `onError` 등으로 확인합니다.

## A10. Error handling
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling -->

초기 서비스 연결 실패는 `initialize(onConnected, onFailed)`의 onFailed로, Navigation 앱의 요청 처리 오류는 `NaviHelperEventListener.onError()`로 따로 전달됩니다.
```kotlin
override fun onError(errorCode: NaviErrorCode) {
    when (errorCode) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> { /* 배터리 부족 안내 */ }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> { /* 탐색 가능 거리 초과 */ }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> { /* 목적지가 너무 가까움 */ }
        NaviErrorCode.CAUSE_SEARCH_ROUTE -> { /* 경로 탐색 서버 오류 */ }
    }
}
```
| Error code | Description |
| --- | --- |
| CAUSE_LOW_BATTERY | 배터리 부족 |
| CAUSE_SEARCH_ROUTE_OVER_DISTANCE | 경로 탐색 가능 거리 초과 |
| CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE | 현재 위치와 목적지 거리가 너무 짧음 |
| CAUSE_SEARCH_ROUTE | 경로 탐색 서버 오류 |
| CAUSE_SEARCH_TRIP | (API Reference에 존재, 가이드 표에는 없음) |

오류 시 확인: SDK 연결 완료 후 호출했는지 / 필요한 퍼미션 선언 / 좌표·POI ID·경유지 인덱스 / 현재 Navigation 앱 상태에서 처리 가능한지.

## A11. NaviHelper API 색인 (API Reference 페이지 목록에서 추출)

**`NaviHelper` 메서드**: initialize, release, addListener, removeListener, requestRoute, requestRouteWithWaypoints, requestReRoute, cancelRoute, changeRouteOption, addWaypoint, removeWaypoint, requestTrip, requestNavigationScreen, getBookmarkInfo, getChargerOperatorInfo, getCurrentLocationInfo, getDestinationInfo, getRecentDestinationInfo, getRouteStateInfo, getTBTInfo, getWaypointInfo

**`NaviHelperEventListener` 콜백**: onBookMarkChanged, onBookmarkInfo, onCameraAlert, onCautionPointInfo, onChargerOperatorInfo, onCurrentLocationInfo, onCurrentRoadSpeedLimit, onDestinationArrived, onDestinationChanged, onDestinationInfo, onDrivingInfo, onError, onNavigationStatus, onReRouteEnded, onRecentDestinationInfo, onRouteCancelled, onRouteOptionChanged, onRouteStarted, onRouteStateInfo, onRoutingCompleted, onTBTInfo, onTripInfo, onWayPointArrived, onWayPointInfo, onWaypointChanged

**데이터 모델 (필드)**
- BookmarkInfo: address, category, displayLatitude, displayLongitude, latitude, longitude, name, poiId, poiProvider, poiSubId
- CameraAlert: id, isOverSectionAvrSpeed, isOverSpeed, remainDistance, sectionAvrSpeed, speedLimit, status, type
- CautionPoint: id, remainDistance, type
- ChargerOperator: id, name
- ChargingPlan: duration, endSoc, startSoc
- CurrentLocationInfo: address, currentRouteIndex, heading, latitude, longitude, routeId
- DestinationArrivedInfo: address, distance, duration, name
- DestinationInfo: address, displayLatitude, displayLongitude, latitude, longitude, name, poiId, poiProvider, poiSubId, remainDistance, remainTime, tollFare
- DrivingInfo: destination, waypoints
- DrivingState: chargerCount, chargingPlan, distance, duration, isChargingStation, isRecommendedChargingStation, maxChargingPower, name, remainDestinationChargingValue, targetSoc
- NavigationScreenRequest (요청 가능한 화면): addHomeFavorite, addWorkFavorite, editRoute, favoriteMain, foreground, moveToCurrentLocationOnMap, nearbyChargingStations, overviewRoute, POIDetailPopup, recentDestinations, routeOptionList, searchPlace, settings
- RecentDestinationInfo: address, displayLatitude, displayLongitude, latitude, longitude, name, poiId, poiProvider, poiSubId
- RequestWaypointInfo: address, latitude, longitude, poiId, poiName, poiProvider, poiSubId, waypointIndex
- RouteInfo: address, displayLatitude, displayLongitude, isMobileRequest, latitude, longitude, poiId, poiName, poiProvider, poiSubId, routeOption
- RouteOverviewShowInfo: durationMs
- RoutePointInfo: latitude, longitude, poiId, poiName, poiType, pointType, ratioOnRoute, settingType
- RouteStartInfo: distance, duration, name, remainDestinationChargingValue, tollFare
- RouteStateInfo: isRouting, naviMode, remainDestinationChargingInt, remainWaypointChargingInt, routeOption, termsAgreement, trafficStatus
- RouteWithWaypointsRequest: destination, waypoints
- TBTInfo(턴바이턴): description, distance, type
- TrafficCongestionInfo: avrSpeed, distance, endCrossName, offset, roadName, startCrossName, trafficCongestionTime, trafficCongestionType
- TrafficCongestionSummary: totalCongestionAvrSpeed, totalCongestionDist, totalCongestionTime
- TrafficEventInfo: description, endTime, eventType, latitude, longitude, startTime
- TrafficInfo: congestionType, endRatio, startRatio
- TrafficStatus: congestionItemList, summary, trafficEventList
- **TripInfo: distance, energyConsumption, estimatedSocAtDest, eta, summary** (목적지 도착 예상 SOC 등 — EV 서비스 아이디어에 유용)
- TripRequest: latitude, longitude, poiId, poiName, poiSubId
- TripSummary: routePoints, trafficConditions
- WaypointArrivedInfo: address, distance, duration, name
- WaypointChangedInfo: cmdType, routeStartInfo
- WaypointInfo: address, displayLatitude, displayLongitude, latitude, longitude, name, poiId, poiProvider, poiSubId, remainDistance, remainTime
  **상수(enum)**
- BookmarkType: DEFAULT, EV_CHARGING_STATION, HOME, OFFICE
- CameraType: SECTION_SPEED_ALERT, SPEED_CAMERA_FIX, SPEED_CAMERA_MOV, SPEED_CAMERA_REAR, SPEED_CAMERA_SCHOOL_ZONE, SPEED_CAMERA_SCHOOL_ZONE_TRAFFIC_LIGHT, SPEED_CAMERA_SECTION_START, SPEED_CAMERA_SECTION_END, SPEED_CAMERA_TRAFFIC_LIGHT, VARIABLE_SPEED_CAMERA
- CautionPointType: ACCIDENT_PRONE_AREA, BLOCK_INTERSECTION_ENFORCEMENT, BUS_LANE_*, CUT_IN_VIOLATION_ENFORCEMENT, DOWNHILL_CAUTION_AREA, FALLING_ROCK_AREA, FOG_AREA, HIGHWAY_CONSTRUCTION, LANE_CHANGE_CAMERA_ALERT, NARROW_ROAD, NO_PARKING_ZONE, RAILROAD_CROSSING, ROAD_DAMAGE_ALERT, SCHOOL_ZONE, SIGN_MALFUNCION, SLIPPERY_ROAD_ALERT, SPEED_BUMP, TRAFFIC_INFO_COLLECTION, TRAFFIC_LIGHT_VIOLATION_ENFORCEMENT, WILDLIFE_CROSSING, WRONG_WAY_DRIVING_ALERT
- NaviErrorCode: CAUSE_LOW_BATTERY, CAUSE_SEARCH_ROUTE_OVER_DISTANCE, CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE, CAUSE_SEARCH_ROUTE, CAUSE_SEARCH_TRIP
- NaviMode: NOT_WORKING, ROUTE_GUIDING, SAFE_GUIDING, SIMULATION_GUIDING
- NavigationStatus: FREE_DRIVE, INACTIVE, INITIALIZING, ROUTE_DRIVE
- RouteOption: COMFORTABLE, FREE, HIGHWAY, RECOMMENDED, SCHOOL_ZONE, SHORT_DISTANCE, SHORT_TIME
- RoutePointPoiType: EV_CHARGING, NORMAL / RoutePointSettingType: AUTO, MANUAL / RoutePointType: DESTINATION, ORIGIN, WAYPOINT
- TrafficCongestionType: FAST, HEAVY, MODERATE, SLOW, UNKNOWN / TrafficEventType: ACCIDENT, DISASTER, ETC, EVENT, REPAIRING, ROAD_DAMAGE
- TurnType: TURN_LEFT, TURN_RIGHT, TURN_STRAIGHT, TURN_UTURN, TURN_ARRIVE, TURN_TOLL, TURN_TUNNEL, TURN_RESTAREA, TURN_ENTER_SERVICEAREA, IC·고가·지하차도·회전교차로 진입/진출 등
- WaypointCmdType: ADD, REMOVE / WaypointIndex: ALL, FIRST, SECOND
> v3.0.5부터 데이터 모델 생성자의 좌표 인자 순서가 **(latitude, longitude)**로 바뀌었습니다(01 문서 릴리즈 노트 참고).
 
---

# Part B. FusedLocation SDK

## B1. Introduction
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/intro -->

FusedLocation SDK는 앱이 차량 위치 데이터를 받을 수 있게 하며, Pleos Connect의 FusedLocation Service와 연동됩니다.

**제공 기능**
- 마지막으로 확인된 위치 1회 조회: `getLastKnownLocation()`
- 위치 업데이트 수신/중지: `registerFusedLocationCallback()` / `unregisterFusedLocationCallback()`
- 지원 환경 확인: `isSupported()`
- 위치 접근 차단 시 안내 팝업: `showLocationAccessBlockedPopup()`
- 설정 영역 진입/이탈 알림 등록/해제: `addProximityAlert()` / `removeProximityAlert()` (지오펜스)
  `getLastKnownLocation()`은 새 위치를 계산하지 않고 현재 쓸 수 있는 마지막 위치를 반환합니다. 데이터가 없으면 `null`일 수 있습니다. 사용 전 반드시 `isSupported()`로 지원 여부를 확인하고, 미지원 환경에서는 기능을 제공하지 않도록 처리합니다.

**위치 데이터**: `LocationData`에 위도, 경도, 고도, 진행 방향, 신뢰도, 타임스탬프가 포함됩니다.

## B2. Importing FusedLocation SDK
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup -->
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
// build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
}
```

## B3. Declaring permission
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions -->

| 권한 | 보호 수준 | 사용 API |
| --- | --- | --- |
| pleos.car.permission.FUSED_LOCATION | **dangerous** | getLastKnownLocation, registerFusedLocationCallback, unregisterFusedLocationCallback |

```xml
<manifest ...>
    <uses-permission android:name="pleos.car.permission.FUSED_LOCATION" />
    <application ... />
</manifest>
```
- 권한이 허용되지 않은 상태에서 호출하면 정상 동작하지 않을 수 있으므로, 권한 요청·결과 처리를 앱 흐름에 맞게 구현합니다(dangerous 수준).
- 차량 개인정보 보호 설정으로 위치 접근이 차단된 경우 `showLocationAccessBlockedPopup()`으로 안내할 수 있습니다. 이 API는 접근을 허용해 주지 않습니다. 위치가 오지 않으면 `null`과 `onError(Exception)`을 처리하고 화면·기능을 제한하세요.
- Android의 `ACCESS_COARSE/FINE/BACKGROUND_LOCATION` 동작과 포그라운드·백그라운드 위치 정책은 이 SDK가 보장하지 않습니다. Google Fused Location 설명을 Pleos Connect 동작 보장으로 해석하면 안 됩니다.
## B4. Initialization / Release
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release -->

| 단계 | API | 설명 |
| --- | --- | --- |
| 지원 환경 확인 | isSupported() | 현재 환경 지원 여부 |
| 초기화 | initialize() | 인스턴스 생성 후 초기화 |
| 위치 수신 | getLastKnownLocation(), registerFusedLocationCallback() | initialize() 후 사용 |
| 업데이트 중지 | unregisterFusedLocationCallback() | 리스너 해제 |
| 리소스 해제 | release() | SDK 해제 |

`initialize()`를 호출했다면 끝날 때 반드시 `release()`를 호출하세요. `unregisterFusedLocationCallback()`(리스너 해제)과 `release()`(SDK 해제)는 서로 대체할 수 없습니다.
```kotlin
class MyApplication : Application() {
    lateinit var fusedLocation: FusedLocation
        private set
 
    override fun onCreate() {
        super.onCreate()
        // 1. 인스턴스 생성
        fusedLocation = FusedLocation(this)
        // 2. 지원 여부 확인 후 초기화
        if (fusedLocation.isSupported()) {
            fusedLocation.initialize()
        } else {
            // 위치 관련 UI와 기능을 비활성화합니다.
        }
    }
 
    fun shutdownFusedLocation(listener: FusedLocationListener) {
        // 3. 등록된 위치 업데이트 리스너를 먼저 해제
        fusedLocation.unregisterFusedLocationCallback(listener)
        // 4. SDK 리소스 해제
        fusedLocation.release()
    }
}
```
`release()` 후에는 같은 인스턴스로 API를 호출하지 말고, 다시 쓰려면 새 인스턴스를 만들어 지원 확인·초기화를 반복합니다.

## B5. 위치 데이터 접근
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access -->
```kotlin
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) {
        location ?: return
        // Use the location data.
    }
    override fun onError(exception: Exception) {
        // Handle the error.
    }
}
 
fusedLocation.registerFusedLocationCallback(listener)
// 중지: 등록한 동일 리스너 전달
fusedLocation.unregisterFusedLocationCallback(listener)
```

## B6. 오류 처리
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/FusedLocation/error-handling -->
오류는 `FusedLocationListener.onError(Exception)`으로 전달됩니다. 예외를 기록하고, 위치를 쓸 수 없는 상태를 안내하거나 기능을 제한합니다. `onChanged(LocationData?)`의 인자는 `null`일 수 있으니 **`null`을 유효한 위치로 처리하지 마세요.** 예외 종류나 재시도 가능 여부는 전달된 예외와 실행 환경을 기준으로 판단합니다.
 
