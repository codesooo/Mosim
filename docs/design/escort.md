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
- 에스코트 쪽 개발·테스트는 문장을 직접 넣는 `FakeSpeechInput`으로 한다. 팀원 구현이 들어오면 provider에서 바꾼다.
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
  SpeechInput.kt             STT 경계
  CabinControl.kt            실내 설정 경계 (Vehicle Control 🔜)
  TripSource.kt              ETA·남은 거리·현재 위치·경유지 경계 (NaviHelper ✅)
data/
  PlaceSearchSource.kt       장소 검색 경계
  mock/                      FakeSpeechInput, MockCabinControl, MockTripSource, MockPlaceSearchSource
  remote/kakao/
    KakaoPlaceSearchSource.kt  HttpURLConnection + org.json (라이브러리 추가 없음)
ui/components/
  KakaoMapView.kt            AndroidView 래퍼 (SDK 승인 후. 그 전에는 MapPlaceholder)
```

- DI 라이브러리는 쓰지 않는다. ViewModel은 `viewModel { EscortViewModel(...) }` initializer로 만든다.
- 화면에는 NavController를 넘기지 않는다. ViewModel 상태를 보고 NavHost가 이동한다.

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

## 8. 카카오 연동 확인 목록

구현 전에 카카오 공식 문서로 확인한다. 확인 전에는 코드에 쓰지 않는다.

- [ ] 로컬 API: 키워드 / 카테고리 검색 엔드포인트, 반경·좌표 파라미터, 카테고리 코드
- [ ] 키 두 종류(REST API 키, 네이티브 앱 키)를 `local.properties` → `BuildConfig`로 읽기. `buildFeatures.buildConfig = true` 필요
- [ ] 반경 검색은 "현재 위치 기준"이다. 디자인 문구 "경로상"과 맞는지
- [ ] 지도 SDK: Maven 저장소, 버전, AGP 8.13 / compileSdk 36 호환, Automotive arm64 에뮬레이터 동작
- [ ] 카카오 콘솔에 패키지명과 키 해시 등록 (패키지명 `com.example.mosim`은 임시)
- [ ] 매니페스트 `INTERNET` 권한

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
