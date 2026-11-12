# 에스코트 모드 설계 (E-1 · E-2 · F-1)

> 작성: 2026-11-09 · 상태: **설계 초안, 구현 전**
> 대상 프레임: `ui-components.md`의 E-1 에스코트 모드, E-2 자동 조절 알림, F-1 지연 감지 · 휴식 제안
> `requirements.md`가 비어 있어 `ui-components.md`와 `project-status.md` 7단계 흐름(05·06단계)을 기준으로 삼았다.
> 끝의 "팀 합의 필요" 항목은 합의 전까지 이 문서의 가정대로 구현하고, 합의 결과에 따라 고친다.

## 1. 범위

| 포함 | 제외 (담당 또는 사유) |
|---|---|
| 음성 문장 → 명령 해석 (키워드 규칙) | STT 연동, 음성 인식 화면 F-2 — 다른 팀원 담당 |
| 실내 설정 조정: 온도, 음악 음량, 조수석 등받이 | 실제 차량 제어 — Vehicle Control 🔜, Mock만 |
| 근처 장소 검색 (카카오 로컬 REST) | 경유지 후보 목록 화면 F-3 — F-1 카드로 대신 |
| 경유지 추가 제안과 운전자 승인 | LLM 호출 — 팀 결정 전 금지 |
| 지도 표시 (카카오맵 SDK, 팀 승인 후) | |

## 2. 전체 흐름

```
SpeechInput (STT 팀원 구현)  ──문장──▶  VoiceCommandParser  ──VoiceCommand──▶  EscortViewModel
                                                                              │
                     ┌───────────────────────────┬────────────────────────────┤
                     ▼                           ▼                            ▼
               CabinControl                PlaceSearchSource             TripSource
          온도·등받이·음량 (Mock)        카카오 로컬 REST / Mock       ETA·위치·경유지 (Mock → NaviHelper)
                     │                           │                            │
                     └───────────────▶  EscortUiState  ◀──────────────────────┘
                                          │          │
                                   E-1 / E-2 화면   F-1 화면
```

- E-1과 E-2는 화면 하나다. 자동 조절이 일어나면 `notice`를 채워 토스트를 몇 초 보여 준다.
- `proposal`이 생기면 F-1로 이동하고, 운전자가 고르면 `proposal`을 지우고 E-1로 돌아온다.

## 3. STT 경계 (안 A: 문장을 넘겨받는다)

```kotlin
// platform/pleos/SpeechInput.kt
interface SpeechInput {
    /** 최종 인식 결과(completed = true)만 내보낸다. */
    val recognizedTexts: Flow<String>
}
```

- STT 쪽은 문장만 넘긴다. 명령 해석과 실행 정책은 에스코트 쪽이 맡는다.
  이유: STT SDK는 텍스트만 돌려주고, 해석은 "자동 실행인지 승인이 필요한지"와 붙어 있어 같은 곳에 두는 편이 설명하기 쉽다.
- 에스코트 쪽 개발·테스트는 문장을 직접 넣는 `MockSpeechInput.say()`로 한다. 팀원 구현이 들어오면 provider에서 바꾼다.
- 가정: 인식 실패는 STT 쪽에서 처리하고, 에스코트 쪽은 성공한 문장만 받는다. (합의 필요 1)

## 4. 패키지와 파일

```
navigation/
  MosimRoute.kt              + ESCORT, ESCORT_REPLAN
  MosimNavHost.kt            EscortViewModel을 NavHost 수준에서 하나 만들어 두 화면에 넘긴다
feature/escort/
  EscortScreen.kt            E-1 + E-2
  EscortReplanScreen.kt      F-1
  EscortViewModel.kt         상태 보관, 명령 실행, 정책 판단
  EscortUiState.kt
  EscortDependencies.kt      소스 구현을 고르는 object (VehicleStatusSourceProvider와 같은 방식)
  command/
    VoiceCommand.kt          sealed class
    VoiceCommandParser.kt    키워드 규칙. Android 의존성 없음
platform/pleos/
  SourceResult.kt            호출 결과(Success / Failure + FailureReason). SDK onFailed를 값으로 바꾼다
  GeoPoint.kt                좌표 (latitude, longitude)
  SpeechInput.kt             STT 경계
  CabinControl.kt            실내 설정 경계. simulatedFeatures로 기능별 Mock 여부를 알린다
  ClimateZone.kt             좌석·공조 구역 모델, 착석 기준 구역 선택 규칙(ClimateZoneRule)
  ClimateApi.kt              공조·착석 SDK 호출 경계 (실제: PleosClimateApi)
  PleosCabinControl.kt       온도만 Vehicle SDK, 음량·등받이는 Mock에 맡김
  TripSource.kt              ETA·남은 거리·현재 위치·경유지 경계 (NaviHelper ✅)
data/
  place/PlaceSearch.kt       PlaceKind, PlaceCandidate, PlaceSearchSource
  mock/                      MockSpeechInput, MockCabinControl, MockTripSource, MockPlaceSearchSource
  remote/kakao/
    KakaoPlaceSearchSource.kt  HttpURLConnection + org.json (라이브러리 추가 없음)
ui/components/
  MosimMap.kt                AndroidView로 감싼 카카오 지도. 키가 없거나 오류면 MapPlaceholder
```

- DI 라이브러리는 쓰지 않는다. ViewModel은 `viewModel { EscortViewModel(...) }` initializer로 만든다.
- 화면에는 NavController를 넘기지 않는다. ViewModel 상태를 보고 NavHost가 이동한다.

### 경계 규칙 (2단계에서 정함)

- 실패는 예외가 아니라 `SourceResult.Failure(reason)`로 돌려준다. 사유는 앱이 다르게 처리할 단위로만 묶었다:
  권한 없음, 미지원, 잘못된 요청(범위 밖·경유지 초과), 주행 중 제한, 연결 실패, 네트워크, 기타.
- set 요청의 Success는 "요청이 받아들여짐"이다. 실제 값은 `StateFlow`로 나중에 바뀐다(실제 SDK와 같게).
- `CabinControl.setPassengerBackrestAngle`은 목표 각도를 받는다. 실제 SDK의 방향 이동 방식은 구현체가 감춘다.

### 공조 실제 연동 규칙 (팀 결정 2026-11-12)

- Pleos 차량 서비스가 있는 기기(Pleos 에뮬레이터·실차)에서는 온도를 Vehicle SDK로 바꾼다. 없으면 전부 Mock이다(`EscortDependencies`).
- 바꿀 구역: 착석 감지로 사람이 앉은 좌석 중 운전석을 뺀 좌석을 덮는 구역. 그 좌석을 덮는 구역이 없으면(뒷좌석 구역이 없는 차) 동승석 구역, 착석을 모르면 동승석 구역.
  `[확인 필요]` 운전석이 ROW_1_LEFT(좌핸들)인지.
- 공조가 꺼져 있으면 온도 조회가 실패한다. 동승자가 온도를 요청하면 공조를 켜고(최대 15초 대기) 설정하며, "공조 켜기"도 실행 기록에 남긴다.
- 실행 기록의 Mock 표시는 기능별이다. 온도는 실제, 음량·등받이는 Mock으로 표시된다.
- 화면의 실내 온도는 5초마다 다시 읽는다. `[확인 필요]` Virtual Console·차량 공조 바에서 바꾼 값이 앱에 오는지는 확인하지 못했다(콘솔 버튼이 반응하지 않음).
- Mock은 생성자로 실패 사유와 지연을 바꿀 수 있다. `MockTripSource.simulateTrafficDelay()`는 지연 감지 시연용 Mock 전용 조작이다.
- Mock 범위 `[확인 필요]`: 음량 0~10, 등받이 90~135°. 온도는 Virtual Console 기준 16~30°C, 0.5°C 단위.
- 장소 검색 결과가 없으면 실패가 아니라 빈 목록이다. 경유 시 늘어나는 시간은 장소 API가 주지 않는다 `[확인 필요]`.

### ViewModel 규칙 (3단계에서 정함)

- `EscortViewModel(scope, ...)`는 lifecycle 2.9.1의 `ViewModel(CoroutineScope)` 생성자를 쓴다(라이브러리에서 시그니처 확인).
  테스트가 메인 스레드 없이 범위를 넘길 수 있어 `kotlinx-coroutines-test`를 추가하지 않아도 된다.
- `EscortDependencies.createViewModel()`이 범위를 먼저 만들고 Mock과 ViewModel에 같이 넘긴다.
- 음성 명령은 들어온 순서대로 하나씩 처리한다.
- 기록(`history`)과 안내(`notice`)는 한 번의 상태 갱신으로 반영한다. 중간 상태가 화면에 보이지 않게 하기 위해서다.
- 조절 범위 조회(Capability)가 실패하면 그 자리에서 실패로 기록·안내하고 멈춘다. 다른 문구로 덮어쓰지 않는다.
- 동승자 정보는 `data/passenger/PassengerProfile`, 고정값은 `MockEscortTrips`(A-1·D-1이 아직 없어서).
- 문구는 `EscortMessages`에 모았다. 받침에 따라 을/를, 은/는을 고른다.
- `[확인 필요]` 값: 토스트 4초, 지연 기준 15분, 검색 반경 2km, 휴식 경유 시 음량 3단계(`restMusicVolume`).

### 화면 규칙 (4단계에서 정함)

- E-1과 F-1은 중첩 그래프 `escort_flow`로 묶고, `EscortViewModel`은 그래프 backstack entry에 붙인다.
  두 화면이 같은 상태를 보고, 흐름을 벗어나면 ViewModel과 Mock이 함께 정리된다.
- E-2 토스트는 `MosimAppContainer`에 새로 추가한 `headerTrailing` 자리에 띄운다. 이 화면에서만 써서 화면 파일 안에 둔다.
- 값을 모르면 "—"로 보여 준다(`EscortMessages.UNKNOWN_DISPLAY`).
- 음악 카드는 피그마의 "조용한 음악 · 4" 대신 "볼륨 4"만 보여 준다. 재생 정보 출처가 없다 `[확인 필요]`.
- 탑승 감지(D-1)가 없어 임시 메인에 "에스코트 모드" 버튼을 두었다. Back은 임시 메인으로 돌아간다 `[확인 필요]` 최상단 규칙 적용 여부.
- 앱 안에서는 음성 문장을 넣을 방법이 없다(STT 팀원 담당). E-2는 계측 테스트(`EscortContentTest`)와 Preview로 확인한다.

## 5. 상태 모델

```kotlin
data class EscortUiState(
    val passengerName: String,
    val destinationName: String,
    val boardedAt: LocalTime?,
    val eta: LocalTime?,               // null = 모름
    val remainDistanceKm: Double?,
    val remainMinutes: Int?,
    val cabinTemperature: Float?,
    val musicVolume: Int?,
    val seatBackrestAngle: Int?,
    val notice: AutoAdjustNotice?,     // E-2 토스트
    val proposal: ReplanProposal?,     // null 아니면 F-1
)

data class ReplanProposal(
    val reason: ReplanReason,          // Delay(before, after) | VoiceRequest(text)
    val message: String,
    val place: PlaceCandidate,         // 이름, 설명, 거리, 우회 시간, 좌표
    val freeWaypointSlots: Int,        // NaviHelper 경유지 최대 2개
    val bundledActions: List<CabinAction>, // 예: 음악 볼륨 4 → 3
)
```

값을 모르면 `null`로 둔다. `null`을 0이나 기본값으로 바꾸지 않는다(`sdk-status.md` Mock 작성 기준).

## 6. 명령 해석 규칙

| 문장 예 | VoiceCommand | 실행 정책 |
|---|---|---|
| 덥다, 더워 | `AdjustTemperature(-1.0)` | 선호 범위 안이면 자동 + 토스트 |
| 춥다, 추워 | `AdjustTemperature(+1.0)` | 같음 |
| 23도로 맞춰 | `SetTemperature(23.0)` | 16~30°C, Capability 범위로 제한 |
| 시끄러워, 소리 줄여 | `AdjustVolume(-1)` | 자동 + 토스트 |
| 소리 키워 | `AdjustVolume(+1)` | 자동 + 토스트 |
| 의자 젖혀 / 세워 | `AdjustBackrest(±5)` | 자동 + 토스트 `[확인 필요]` 좌석은 방향 이동 API |
| 카페, 화장실, 편의점, 쉬었다 가자, 충전소 | `FindPlace(PlaceKind)` | **자동 실행 안 함** → 검색 → F-1 → 승인 |
| 그 외 | `Unknown(text)` | 토스트 "다시 말씀해 주세요" |

- 공백을 지우고 비교한다. 검사 순서는 장소 → 등받이 → 음량 → 온도다("더운데 카페 가자"는 장소, "볼륨 올려"는 음량).
- 숫자 없는 온도 요청 폭 1°C, 등받이 폭 5°는 `[확인 필요]` 값이다(`VoiceCommandParser` 상수).
- 파서는 값을 해석만 하고 범위를 자르지 않는다. "40도로"도 `SetTemperature(40)`으로 넘기고 실행 정책이 막는다.
- 부정 표현("카페 말고")은 처리하지 않는다. 필요하면 규칙을 추가한다.
- 선호 범위는 동승자 프로필 Mock에서 읽는다. 범위를 벗어나는 조정은 실행하지 않고 토스트로 알린다. `[확인 필요]` 범위 밖일 때 승인 화면을 띄울지.
- 실행 결과는 성공 / 실패 / Mock 실행으로 나눠 기록한다(`project-status.md` 안전 규칙).

## 7. F-1이 뜨는 경우

1. **지연 감지**: ETA가 출발 시 계획보다 15분 이상 늘면 휴식 장소를 검색해 제안한다. `[확인 필요]` 기준 시간.
2. **음성 장소 요청**: `FindPlace` 종류로 검색해 첫 후보를 제안한다.

- "기존 경로 유지": 제안을 지우고 E-1로 돌아간다.
- "○○ 경유": `TripSource.addWaypoint` → 성공하면 `bundledActions` 실행 → E-1. 실패하면 F-1에 오류를 보여 준다.
- 경유지 빈 자리가 없으면 경유 버튼을 비활성화한다.
- 제안 문구는 프로필 규칙으로 만든 고정 문장이다. LLM을 쓰지 않는다.
- 휴식을 원하지 않는 동승자(`prefersRestOnLongTrip = false`)는 경유를 묻지 않고 "도착이 n분 늦어질 것 같아요"만 알린다.
- 제안에 답하면(승인·거절) 그때 도착 시각을 새 기준으로 잡는다. 같은 지연을 두 번 묻지 않기 위해서다.
- 휴식 제안 문구는 "경로상" 대신 "가까운"을 쓴다. 반경 검색이라 경로 위인지 보장하지 못한다.

### 휴식 장소 기준 (`RestPlaceRule`, 팀 결정 2026-11-12)

| 도로 | 찾는 곳 | 반경 |
|---|---|---|
| 고속도로 | 휴게소만 | 20km (카카오 최대값) |
| 국도 | 휴게소 → 공원 → 카페 순, 반경 안에 있는 첫 종류의 가장 가까운 곳 | 2km |
| 모름 | 국도 규칙 | 2km |

- 음성 "쉬었다 가자"(`FindPlace(REST_AREA)`)도 같은 기준을 쓴다.
- 우선순위를 둔 이유: 도심·국도에서는 카페가 거의 항상 가장 가까워 거리만 보면 늘 카페가 된다.
- `[확인 필요]` 도로 종류(`TripStatus.roadType`)를 NaviHelper에서 직접 주지 않는다. 후보 신호는 `TurnType.TURN_RESTAREA`·`TURN_ENTER_SERVICEAREA`, `onCurrentRoadSpeedLimit`, `TrafficCongestionInfo.roadName`. 지금은 Mock에서 null(모름)이다.
- `[확인 필요]` 고속도로 반경 검색은 진행 방향 반대편이나 지나온 휴게소도 돌려준다(예: "안성휴게소 부산방향/서울방향").

## 8. 카카오 연동 확인 목록

카카오 개발자 문서 "카카오맵 REST API"로 확인했다(2026-11-12).

- [x] 로컬 API: `GET https://dapi.kakao.com/v2/local/search/{category|keyword}.json`, 헤더 `Authorization: KakaoAK {REST API 키}`.
  `x`=경도, `y`=위도, `radius` 0~20000m, `size` 1~15, `sort=distance`는 x·y 필요. 응답 값은 전부 문자열이고 `distance`는 x·y를 보낼 때만 채워진다.
- [x] 종류 대응: 카페 `CE7`, 편의점 `CS2`는 카테고리 검색. 충전소("전기차충전소"), 화장실("공중화장실"), 휴게소("휴게소"), 공원("공원")은 키워드 검색이고, 분류에 기대한 단어가 없는 결과는 뺀다.
  `OL7`은 주유소가 섞여 쓰지 않는다. `[확인 필요]` 도심 2km 안에서 "휴게소" 결과가 나오는지, 휴식 장소를 무엇으로 볼지.
- [x] REST 키는 `local.properties`의 `KAKAO_REST_API_KEY` → `BuildConfig`. 키가 없으면 Mock 장소 검색을 쓴다(`EscortDependencies`).
- [x] 실패 대응: 401·403 → `PERMISSION_DENIED`, 400(인자 오류, 쿼터 -10) → `INVALID_REQUEST`, 429(무료 제공량 소진) → `UNKNOWN`, 5xx → `CONNECTION_FAILED`, 연결 오류 → `NETWORK`.
- [x] 매니페스트 `INTERNET` 권한.
- [ ] `[확인 필요]` 2026-07-21부터 카카오맵 API 무료 쿼터 방식이 바뀌었다. 무료 제공량은 계정에서 처음 활성화한 앱에만 준다. 팀 계정과 앱을 정해야 한다.
- [ ] 반경 검색은 "현재 위치 기준"이다. 디자인 문구 "경로상"과 맞지 않아 "가까운"으로 썼다.
- [x] 지도 SDK(7단계, 팀 승인 2026-11-12): `com.kakao.maps.open:android:2.15.2`, 저장소 `devrepo.kakao.com/nexus/repository/kakaomap-releases/`.
  arm64-v8a / armeabi-v7a만 지원한다. AGP 8.13 / compileSdk 36에서 빌드되고 arm64 에뮬레이터(API 34)에서 지도가 뜬다.
- [x] 네이티브 앱 키는 `local.properties`의 `KAKAO_NATIVE_APP_KEY` → `BuildConfig`. 없으면 `MosimMap`이 `MapPlaceholder`를 그린다.
- [x] 카카오 콘솔 [앱] > [플랫폼 키] > [네이티브 앱 키]에 패키지명과 키 해시 등록. 등록 전에는 지도 인증이 401이다.
  `[확인 필요]` 팀원마다 디버그 키 해시가 달라 각자 등록해야 한다. 패키지명 `com.example.mosim`은 임시다.
- `[확인 필요]` 카카오맵 SDK가 logcat에 네이티브 앱 키를 출력한다. 시연 녹화에 logcat이 나오지 않게 한다.
- 응답 파싱 단위 테스트를 위해 `testImplementation(org.json:json)`을 추가했다. 앱은 안드로이드 내장 org.json을 쓴다. `[확인 필요]` 팀 공유.

## 9. 테스트

`kotlinx-coroutines-test`는 라이브러리 추가라서 쓰지 않는다. 기존 테스트처럼 `runBlocking`을 쓴다.

- `VoiceCommandParserTest`: 문장별 명령, 숫자 추출, 검사 순서, 미인식 (1단계 완료, 18개)
  - 범위 제한은 파서가 아니라 ViewModel 테스트에서 본다. 범위가 차량 Capability와 프로필에 따라 달라서다.
- `EscortViewModelTest` (Mock 주입)
  - 온도 명령 → 온도 변경 + 토스트
  - 선호 범위 밖 → 실행 안 함
  - 장소 명령 → 제안 생성
  - 승인 → 경유지 추가 + 함께 처리 실행 + 제안 해제
  - 경유지 가득 참 → 경유 불가
  - ETA 지연 기준 초과 → 제안 생성

## 10. 구현 순서

1. 명령 해석기 + 테스트
2. 경계 인터페이스 + Mock
3. ViewModel + 상태 + 테스트
4. E-1 / E-2 화면
5. F-1 화면 (지도 자리는 `MapPlaceholder`)
6. 카카오 로컬 REST 연동
7. 카카오맵 SDK 연동 (팀 승인 후)

## 11. 팀 합의 필요

| # | 항목 | 지금 가정 |
|---|---|---|
| 1 | STT 경계: 인터페이스 파일 소유, 듣기 시작 시점, 실패 전달 방식 | 문장만 넘김, 실패는 STT 쪽 처리 |
| 2 | 키워드 명령 해석이 "의도 파악 보류"(2026-11-08) 결정과 충돌 | LLM 없이 키워드 규칙만 |
| 3 | 장소 API를 카카오로 정하기 (`project-status.md` 미선정 항목) | 카카오 로컬 REST |
| 4 | 카카오맵 SDK 의존성 추가 (라이브러리 임의 추가 금지 규칙) | 승인 전 `MapPlaceholder` |
| 5 | F-1은 주행 중 화면인데 승인 버튼이 있다. 안전 규칙은 "승인은 출발 전이나 정차 중에" | 디자인대로 버튼을 두고 `CarUxRestrictions` 대응은 `[확인 필요]` |
| 6 | 디자인의 "AI 제안" 라벨. 실제로는 규칙 기반 문장 | 라벨 유지, PT 설명 방식 결정 필요 |
| 7 | 상태 관리를 ViewModel로 정하기 (`architecture.md` 미정 항목) | ViewModel 하나, DI 없음 |
| 8 | 지연 감지 기준 시간, 선호 범위 밖 조정 처리 | 15분, 실행 안 함 |
