# 모심 통합 계약

> 2026-11-15 실제 코드 기준(통합 작업 후). 코드에 없는 것은 여기 적지 않는다.
> STT 내부 구현은 `voice-stt.md`, F 화면 세부는 `f-screens.md`, 에스코트 설계는 `design/escort.md`를 본다.

## 1. 전체 흐름 (구현됨)

```
A-1 동승자 선택 → A-3 이동 설정 → B-1 출발 전 점검 → B-2 충전소 선택 → B-3 에스코트 계획
→ C-1 픽업지 이동 → C-2 픽업 준비 → D-1 탑승 확인 → E-1 에스코트 모드
→ (F-1 AI 제안 | F-2 음성 요청 → F-3 경유지 후보) → E-1 복귀
→ G-1 도착 → G-2 프로필 업데이트 → HOME
```

- 개발용 HOME 메뉴는 그대로 둔다. 각 화면에 단독으로 들어가 볼 수 있다.
- F는 E에서 갈라지는 분기다. 모든 이동이 F를 거치지는 않는다.
- 화면에 NavController를 넘기지 않는다. 이동은 전부 `MosimNavHost`가 정한다.

### 화면 전환 조건과 Back 정책

| 전환 | 조건 | 스택 처리 |
|---|---|---|
| A-1 → A-3 | 동승자 선택 후 "이동 설정" | 그대로(되돌아갈 수 있음) |
| A-3 → B-1 | "출발 전 점검" | 그대로 |
| B-1 → B-2 / B-3 | "충전소 보기" / "에스코트 계획 보기" | 그대로 |
| B-2 → B-3 | "건너뛰기" 또는 "경유지 추가"(충전소 이름 전달) | 그대로 |
| B-3 → C-1 | "안내 시작" | B 화면들 제거(`popUpTo(VEHICLE, inclusive)`) |
| C-1 → C-2 | Mock 도착 신호 | C-1 제거 |
| C-2 → D-1 | Mock 탑승 신호 | C-2 제거 |
| D-1 → E-1 | "탑승 완료 · 에스코트 시작" | D-1 제거. 이때 `EscortSession.boardedAt` 기록 |
| E-1 → F-1 | `EscortUiState.proposal != null`(지연 감지·음성 장소 요청) | 쌓임. 제안이 사라지면 자동 복귀 |
| E-1 → F-2 | 화면의 "동승자 음성 요청" 탭(주행 제한 시 비활성) | 쌓임 |
| F-2 → F-3 | 최종 인식 문장이 장소 요청으로 분류됨 | F-2 제거 |
| F-2 / F-3 → E-1 | 취소·실패·처리 완료·경유지 추가 성공 | `popBackStack(ESCORT)` |
| E-1 → G-1 | `TripStatus.arrived`(NaviHelper onDestinationArrived) | 에스코트 흐름 전체 제거 |
| G-2 → HOME | "저장 안 함" 또는 "선택 항목 저장" | HOME까지 정리, `EscortSession.reset()` |

## 2. 화면 간 공유 상태 — `data/EscortSession.kt`

이동 한 건의 정보를 담는 `object`다. **프로세스 범위이며 앱이 죽으면 사라진다.** 영속화는 없다.

| 필드 | 쓰는 쪽 | 읽는 쪽 |
|---|---|---|
| `passenger` | A-1 선택 / A-2 등록 | `EscortDependencies.currentTrip()` → E-1·G-1 |
| `pickup`, `destination` | A-3 | 같음(목적지 이름) |
| `boardedAt` | D-1 | E-1 부제목 |
| `appliedCabin` | **아직 아무도 쓰지 않음** [확인 필요] C-2 연결 | — |
| `summary` | E(도착 시) | G-1 "이번 에스코트" |
| `profileSaved` | G-2 | — |

동승자 모델이 둘이라 `EscortDependencies.toEscortPassenger`가 A-1의 `data.mock.PassengerProfile`을
에스코트용 `data.passenger.PassengerProfile`로 바꾼다. A-1에 없는 항목(휴식 선호, 휴식 음량)은
Mock 기본값을 그대로 쓴다 [확인 필요].

## 3. 음성 요청 (E → F-2 → F-3)

```
VoiceScreen → VoiceViewModel → SpeechRecognizer(PleosSpeechRecognizer | MockSpeechRecognizer)
   최종 문장 → EscortViewModel.handleVoiceText(text)
       ├─ VoiceRouting.SearchPlaces(query) → F-3
       └─ VoiceRouting.Handled(message)    → E-1 복귀(결과는 토스트)
```

- **최종 인식 결과가 없으면 화면을 넘기지 않는다.** 타이머로 넘어가는 동작은 없다.
- 의도 해석은 `feature/escort/command/VoiceCommandParser`(키워드 규칙)가 한다. LLM은 쓰지 않는다.
- 해석하지 못한 문장은 성공으로 처리하지 않고 "요청을 이해하지 못했어요" 안내만 남긴다.
- F-2 진입 시 `beforeStart`로 TTS를 먼저 멈춘다. STT와 TTS가 동시에 돌지 않는다.
- 마이크 권한(`RECORD_AUDIO`)은 F-2 진입 때 요청하고, 승인 뒤에 인식을 시작한다.
- `MosimNavHost(analyzeRequest = ...)`를 넘기면 외부 분석기로 바꿀 수 있다(기본은 위 경로).

### F-3 장소 검색·경유지 추가

```
WaypointScreen → PlaceSearchSource(어댑터) → EscortViewModel.searchPlacesFor(query)
                                              → PlaceSearchSource(카카오 또는 Mock).searchNearby
선택 후 확인 → EscortViewModel.addWaypointAt(name, location) → TripSource.addWaypoint
```

- `PlaceUiModel.location: GeoPoint?`이 추가됐다. 좌표가 없으면 경유지로 보내지 않고 `NotConnected`다.
- 성공해야만 `Applied`이고 E-1로 돌아간다. 실패는 `Failed`이며 다시 시도하거나 취소할 수 있다.

## 4. AI 제안 (F-1)

제안은 `EscortViewModel`이 만든다(지연 감지 또는 음성 장소 요청). 화면은 **F-1 구현
(`feature/planrevision`)** 을 쓰고, 경유지 추가는 에스코트의 실제 경로 기능을 쓴다.

```
ReplanProposal ──(MosimNavHost.toSuggestion)──> PlanSuggestion → PlanRevisionScreen
   "경유지 추가" → EscortViewModel.acceptProposalAndWait() → TripSource.addWaypoint
   "기존 경로 유지" / Back → EscortViewModel.keepCurrentRoute()
```

- `ReplanProposal.id`로 제안당 TTS 안내를 한 번만 한다(`AnnouncementHistory`, 프로세스 범위).
- 변경 후 도착 시각·추가 소요시간은 **담지 않는다.** 재탐색 전에는 모르는 값이라 화면이 "—"로 둔다 [확인 필요].
- `feature/escort/EscortReplanScreen`은 쓰지 않지만 Preview·Compose 테스트용으로 남아 있다.

## 5. 외부 경계 요약

| 경계 | 인터페이스 | 실제 구현 | Mock | 상태 |
|---|---|---|---|---|
| 경로·ETA·경유지·현재 위치 | `platform/pleos/TripSource` | `PleosTripSource`(NaviHelper) | `MockTripSource` | 실기에서 경로 요청·경유지 추가 확인(2026-11-15) |
| 도착 | `TripStatus.arrived` ← `NaviApi.arrivals` | `PleosNaviApi.onDestinationArrived` | `MockTripSource.simulateArrival()` | 실기 미검증(에뮬레이터에서 차가 움직이지 않음) |
| 실내 설정 | `CabinControl` | `PleosCabinControl`(온도만) | `MockCabinControl` | 온도 외는 Mock |
| 장소 검색 | `data/place/PlaceSearchSource` | `KakaoPlaceSearchSource` | `MockPlaceSearchSource` | 실기에서 실제 검색 결과 확인 |
| F-3 화면 검색 | `feature/waypoint/PlaceSearchSource` | NavHost 어댑터 → 위 경계 | `MockWaypointSearchSource` | 연결됨 |
| STT | `platform/pleos/SpeechRecognizer` | `PleosSpeechRecognizer` | `MockSpeechRecognizer` | 녹음 시작 확인, **사람 발화 인식 미검증** |
| 음성 입력(E 직접) | `platform/pleos/SpeechInput` | 없음 | `MockSpeechInput` + debug 주입 | 미연결 |
| TTS | `platform/pleos/SpeechSynthesizer` | `PleosTtsAdapter` | `MockSpeechSynthesizer` | F-1에서 재생 확인 |
| 지도 | `ui/components/MosimMap` | 카카오 벡터맵 | `MapPlaceholder` | **x86_64 에뮬레이터에서 렌더 불가**(5절) |

### 키와 권한

- `local.properties`의 `KAKAO_REST_API_KEY`(장소 검색), `KAKAO_NATIVE_APP_KEY`(지도). 저장소에 올리지 않는다.
- 키가 없으면 장소 검색은 Mock, 지도는 자리 표시로 떨어진다. 앱은 죽지 않는다.
- `android.permission.RECORD_AUDIO` — Speech Service가 호출 앱 식별자로 녹음을 집계한다.
  이 선언과 런타임 승인이 없으면 `request()` 경로에 음성이 들어오지 않는다(2026-11-15 실기 확인).
- `POST_NOTIFICATIONS` — E-2·F-1 헤드업 알림.

### 지도 제약 (2026-11-15 확인)

카카오 벡터맵 AAR에는 `arm64-v8a`, `armeabi-v7a` 네이티브 라이브러리만 들어 있다.
**x86_64 Pleos 에뮬레이터에서는 `libK3fAndroid.so`를 찾지 못해 초기화가 실패한다.**
`MosimApplication`이 이 실패를 잡아 지도만 끄고 앱은 계속 돌린다. 지도 렌더 확인은 arm 기기나
arm 시스템 이미지가 필요하다 [확인 필요].

## 6. 디버그 전용 진입점 (release 미포함)

```powershell
# 음성 문장 주입 (E의 음성 명령 경로)
adb shell am broadcast --user 10 -a com.example.mosim.DEBUG_SAY --es text "소리 줄여" -n com.example.mosim/.debug.DebugSpeechReceiver
# 경로 소스를 Mock으로 강제 (에뮬레이터 시연용, 에스코트 재진입 시 적용)
adb shell am broadcast --user 10 -a com.example.mosim.DEBUG_MOCK_TRIP --ez on true -n com.example.mosim/.debug.DebugTripReceiver
# 정체(기본 30분) / 목적지 도착
adb shell am broadcast --user 10 -a com.example.mosim.DEBUG_DELAY --ei minutes 30 -n com.example.mosim/.debug.DebugTripReceiver
adb shell am broadcast --user 10 -a com.example.mosim.DEBUG_ARRIVE -n com.example.mosim/.debug.DebugTripReceiver
```

`FScreenDemoActivity`는 `demoFlow = true`로 Mock STT·Mock TTS를 쓴다. 마이크 없이 F-2 → F-3를 보여 줄 때 쓴다.
