# F 화면 통합 계약

> 2026-11-13 실제 코드 기준. AI 분석·장소 검색·NaviHelper 구현은 포함하지 않는다.
> STT 내부 검증 이력은 voice-stt.md, F 화면 실행·검증은 f-screens.md를 본다.

## 연결 구조 (구현됨)

- 흐름 A: 외부 제안 → F-1 → 유지/취소 또는 외부 경유 요청.
- 흐름 B: F-2 → 최종 텍스트 → 외부 분석 → F-3 검색 → 선택 → 외부 경유 요청.
- MosimNavHost의 demoFlow 기본값은 false다. true는 Mock STT·분석 지연·장소 검색·TTS 상태만 실행한다.
- E 에스코트 화면은 이 저장소에 없다. 현재 개발용 Home에서 F-1/F-2에 독립 진입한다.
- suggestion이 들어오면 Home에서 F-1으로 진입한다. F-2/F-3 진행 중 제안은 Home 복귀까지 대기한다.
- 화면에 NavController를 넘기지 않는다. 지도/왼쪽 내비게이션/시스템 바는 구현하지 않는다.

## F-1 입력 (구현됨)

feature/planrevision/PlanSuggestion.kt:

| 필드 | 의미 |
|---|---|
| id: String | 새 제안마다 고유 ID. 같은 ID의 내용 변경은 새 제안으로 처리하지 않는다 |
| passengerName: String? | 동승자 표시 정보. 추천 이유는 reason에 완성된 문장으로 전달 |
| headline, reason: String | UI 제안 제목·이유 |
| place: SuggestedPlace? | name, highlight, externalId(선택). 장소가 없으면 요청 불가 |
| arrival: String? | 변경 후 예상 도착. 외부 계산 결과만 전달 |
| extra: ExtraTime? | 표시용 text와 includesDetour/includesStay. 우회·체류 포함 여부를 명시 |
| speech: String? | UI와 분리된 완성 발화문. null/공백이면 안내하지 않는다 |
| isMock: Boolean | true이면 실제 경로 정보가 아님을 표시 |

speech의 장소·시간은 전달한 데이터에만 근거해야 한다. UI가 ETA나 추가 시간을 계산하지 않는다.
두 시간 포함 플래그가 false이면 구성은 미상이며 합계 시간을 임의로 분해하지 않는다.

PlanRevisionScreen은 suggestion, synthesizer, onBack, onKeepRoute,
onRequestWaypoint: (SuggestedPlace?) -> Unit을 받는다.
추가 입력은 submitWaypoint: (suspend (SuggestedPlace) -> WaypointRequestState)?,
mapContent: (@Composable () -> Unit)?, interactionAllowed: Boolean이다.

## F-1 출력 및 TTS (구현됨)

- onKeepRoute: 기존 경로 유지. 외부 경로 호출 없음.
- onBack: 제안 취소. MosimAppContainer의 CCP Back을 재사용하며 음성을 중지한다.
- onRequestWaypoint: 선택한 장소를 알리는 관찰 콜백. 이 콜백만으로 성공/연결을 판단하지 않는다.
- submitWaypoint: 실제 외부 작업 경계. 없으면 NotConnected, 진행 중 Requested, 예외/외부 실패는 Failed.
- Applied는 외부에서 실제 반영을 확인한 뒤에만 반환한다.
- NavHost의 onSuggestionKept(id), onSuggestionCancelled(id)로 유지/취소를 외부에 알린다.
- SpeechSynthesizer → PleosTtsAdapter / MockSpeechSynthesizer. 실제는 ON_DEVICE, Locale.KOREA.
- 앱 프로세스 동안 AnnouncementHistory.session이 안내 ID를 기억한다. 화면 재진입·재구성으로 재생하지 않는다.
  프로세스 종료 후 이력은 유지하지 않는다. 다시 듣기는 명시적 재생이다.
- 다른 제안/화면 이탈/ON_STOP/사용자 중단은 준비 중 발화도 중지한다.
- 합성기는 주입한 호출자가 소유한다. NavHost 기본 합성기는 구성에서 빠질 때 close한다.
- 실제 SDK 실패를 Mock 성공으로 바꾸지 않는다. 검증 상태는 f-screens.md에 별도 기록한다.

## F-2 STT 및 분석 (구현됨)

기존 onSpeechRecognized: (String) -> Unit 계약은 유지한다.
trim된 비어 있지 않은 최종 텍스트만 세션당 한 번 전달한다.
부분 결과, 오류, 취소, 타임아웃은 전달하지 않는다.
실제 기본 모드는 SpeechMode.Pleos, ON_DEVICE 한국어이며 Mock은 명시적으로 고른다.
STT 엔진·provider·오류 분류·세션 해제 로직은 변경하지 않았다.

VoiceScreen 추가 입력:

- awaitsAnalysis: Boolean = false. true이면 최종 결과 후 Analyzing(“요청을 확인하고 있어요”).
- beforeStart: () -> Unit. NavHost는 TTS stop을 연결하고 그 뒤 STT를 시작한다.
- analyzeRequest: (suspend (String) -> VoiceAnalysisResult)?.
- onAnalysisResult: (VoiceAnalysisResult) -> Unit.
- VoiceAnalysisResult.SearchPlaces(query): 외부 분석에서 검색으로 판단한 경우만 F-3 진입.
- VoiceAnalysisResult.Message(text): 분석 오류/지원하지 않는 요청/안내를 F-2 내부에 표시.
- 분석 미연결은 “요청 분석 기능 연결 예정”. 실제 의도 분석은 구현하지 않았다.
- 분석은 STARTED 생명주기에서 실행하고 이탈/중단 시 취소한다. 완료 결과를 자동 재전달하지 않는다.

## F-3 입력·출력 (구현됨)

PlaceSearchSource.suspend search(query: String): PlaceSearchResult:
Found(List<PlaceUiModel>), Empty, Failed(message).
검색 요청 query는 외부 분석이 결정한 검색어이며 원래 STT 문장과 같을 필요는 없다.

PlaceUiModel: externalId, name, detail(기본 빈 문자열), tag(선택), isMock.
detail에는 외부에서 확인한 거리/추가 이동시간만 전달한다. 평점·영업 여부는 만들지 않는다.
검색 담당자가 추천 순서를 정하며 F-3은 앞의 최대 3곳만 표시한다. 첫 후보를 기본 선택한다.
원본 좌표·검색 결과는 검색 담당자가 externalId로 조회할 수 있어야 한다.

WaypointScreen:
query, source, onBack, onPlaceSelected: (PlaceUiModel) -> Unit,
submitWaypoint: (suspend (PlaceUiModel) -> WaypointRequest)?,
mapContent: (@Composable () -> Unit)?, interactionAllowed: Boolean.

- Searching / Ready / Empty / Failed를 화면 안에서 표시한다.
- Ready는 selected와 Idle / Requested / NotConnected / Applied / Failed 요청 상태를 가진다.
- 선택은 테두리와 RadioButton 선택 semantics로 표시한다.
- onPlaceSelected는 확인 버튼에서 한 번 호출된다. 목록을 누르는 것은 선택만 바꾼다.
- onBack은 취소/이전 화면 복귀. 실제 경로 변경을 취소한다는 의미는 아니다.
- submitWaypoint가 없으면 “경유 기능 연결 예정”. 완료를 가정하지 않는다.
- 요청 중에는 선택 변경/중복 요청을 막는다. 실패하면 다시 요청할 수 있다.
- 검색 예외는 실패 화면, coroutine 취소는 정상 전파한다.

## 외부 담당자가 연결할 것

MosimNavHost의 실제 입력:

| 담당 | 연결점 |
|---|---|
| AI 선제 제안 | suggestion: PlanSuggestion? |
| 의도 분석 | analyzeRequest: (suspend (String) -> VoiceAnalysisResult)? |
| 장소 검색 | placeSearch: PlaceSearchSource? |
| NaviHelper | submitWaypoint: (suspend (WaypointRequestSource) -> WaypointSubmissionResult)? |
| 경유 요청 관찰 | onWaypointRequested: (WaypointRequestSource) -> Unit |
| 주행 제한 정책 | interactionAllowed: Boolean |

WaypointRequestSource.AiSuggestion(place) / DriverPick(place)로 출처를 구분한다.
WaypointSubmissionResult는 NotConnected / Applied / Failed다.
관찰 콜백과 submitWaypoint 양쪽에서 경유를 실행하면 안 된다. 실행은 submitWaypoint 한 곳에서 한다.
외부 함수는 메인 스레드에서 수집되므로 내부 블로킹 I/O는 적절한 dispatcher로 옮겨야 한다.
외부 함수는 자체 타임아웃과 coroutine 취소, 원본 장소 식별, 실제 반영 확인을 책임진다.

운영 NavHost의 interactionAllowed 기본값은 false다.
정차 또는 플랫폼에서 허용한 조작 상태를 통합 담당자가 전달해야 경유 승인/장소 선택이 가능하다.
독립 화면·Preview·Mock 시연은 true로 확인할 수 있다.
실제 CarUxRestrictions 연결, E 진입 트리거, 좌표 계약은 담당자와 연결해야 하는 미구현 영역이다.
NaviHelper 초기화·의존성·지도·경로 요청 호출은 F 화면에 없다.
