# F 화면 구현·검증

기준: 2026-11-13. F-1/F-3 기존 작업 초안을 재사용해 마무리했다.
공통 UI/토큰, STT 어댑터/provider, 다른 담당 화면, Vehicle provider는 이번 작업에서 변경하지 않았다.
작업 전부터 여러 수정·미추적 파일이 존재했으며 이를 유지했다. 커밋/푸시는 수행하지 않았다.

## 구현과 UX

- F-1: 제안 → 장소의 고유 장점 → 변경 후 도착/추가 시간 → 하단 유지/경유 승인.
  이전 시간 비교·경유지 개수·중복 설명을 표시하지 않는다. 없는 시간은 숨긴다.
  추가 시간의 우회/체류 포함 여부는 외부 데이터에서 받아 표시한다.
- F-3: 검색 중/빈 결과/실패/선택/요청 중/미연결/실패/외부 반영 확인.
  최대 3후보, 선택 테두리·RadioButton semantics, 하단 취소/확인.
  고정 거리 범위·경유지 여유 수·미구현 “1번으로 가줘” 안내를 제거했다.
- 지도 데이터가 없을 때 콘텐츠:보조 영역을 3:1로 조정하고 큰 회색 지도 판을 제거했다.
  mapContent 슬롯으로 추후 지도 컴포넌트를 주입할 수 있다.
- Asta Sans, PleosButton, MosimAppContainer, StatusChip, 카드·간격·색 토큰을 재사용했다.
  왼쪽 내비게이션, Pleos 상단 상태바, 하단 시스템 UI는 플랫폼 영역 그대로다.
- 원본 Figma 링크/수치가 이번 입력에 없으므로 docs/ui-components.md와 기존 코드 기준이다.
  픽셀 단위 Figma 일치는 검증하지 않았다.
- 운영 NavHost의 interactionAllowed=false가 기본이다.
  실제 정차/주행 제한 상태를 받기 전까지 장소 선택·경유 승인을 막는다.
  Mock 진입점은 true로 재현한다. 실제 CarUxRestrictions 연결은 미구현이다.
- E 에스코트 화면은 저장소에 없어 수정하지 않았다. 개발용 Home에 있는 독립 F 진입점을 재사용했다.

## TTS

이번 대화에서 개발자가 TTS에 한해 Coming Soon 사전 에뮬레이터 검증 제한 예외를 승인했다.
TextToSpeech:3.3.1과 TTS_SERVICE를 추가했다. context 문서의 SDK 상태를 바꾸지 않았다.

공식 Introduction, Programming Guide(초기화/리스너/권한/로케일/시작·중단), API Reference를 확인했다.
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/entry

ON_DEVICE·Locale.KOREA이며 서버 등록·비밀키를 사용하지 않는다.
발화 데이터 speech는 UI 문구와 분리된 입력이다. 외부 모듈은 실제 전달한 장소·시간만 넣어야 한다.
null/공백이면 말하지 않으며 시간 추정은 하지 않는다.

제안 ID 이력은 프로세스 범위다. 재구성·재진입·A→B→A에서도 자동 재생은 각 ID당 한 번이다.
다시 듣기는 명시적으로 재생한다. 실패도 자동 재시도하지 않는다.
준비 중·재생 중 모두 중단 가능하며 이전 세션의 지연 콜백은 무시한다.
화면 이탈/ON_STOP에서 중지하고 자원을 반납한다.
NavHost가 합성기를 소유하고, ViewModel은 구성 변경 후 새 합성기를 다시 연결한다.

오디오 정책:
- USAGE_ASSISTANT / CONTENT_TYPE_SPEECH로 일시 포커스를 요청한다.
- 실제 SDK에도 동일 AudioAttributes를 지정한다.
- 포커스 거절은 실패로 처리하고, 포커스를 잃으면 중단한다. 자동으로 재개하지 않는다.
- 완료·중단·실패 시 포커스 반납, 리스너 제거, SDK release.
- 앱 정책 60초 타임아웃으로 콜백 유실 시 무기한 점유를 막는다. SDK 보장값은 아니다.
- F-2 시작 전에 TTS를 중지하며 STT 최종 결과에서는 기존 마이크 해제 정책을 유지한다.
- [확인 필요] 실제 내비 음성/통화/Gleo와 함께 실행했을 때 차량 오디오 매트릭스와
  Speech Service의 자체 포커스 처리. 현재 설정만으로 무충돌을 보장하지 않는다.
- [확인 필요] SDK 내부 선행 묵음 150~200ms 지원 여부. 결과 PCM을 앱이 받지 않아
  임의 지연/PCM 후처리는 넣지 않았다.

## 실행

Android Studio에서 프로젝트의 Gradle JDK를 호환 JDK로 지정한다.
이번 셸 기본 JDK 11은 AGP 실행에 실패했고, IDE JBR 25도 Kotlin 구성에서 실패했다.
설치된 JBR 21로 아래 작업을 실행했다. 저장소 JDK/버전 설정은 변경하지 않았다.

```powershell
$env:JAVA_HOME='C:\Users\USER\.jdks\jbr-21.0.11'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --console=plain
```

debug 전용 FScreenDemoActivity:
```powershell
adb shell am start --user 10 -S -n com.example.mosim/.feature.planrevision.FScreenDemoActivity
adb shell am start --user 10 -S -n com.example.mosim/.feature.planrevision.FScreenDemoActivity --ez real_tts true
```

첫 명령은 두 흐름의 Mock 시연이다. 둘째는 실제 TTS 확인용이며 Home의 F-1으로 진입한다.
실제 TTS 이벤트만 MOSIM_TTS_CHECK로 기록하고 발화/개인정보는 로그에 남기지 않는다.
real_tts 진입점의 조작 허용은 debug 테스트 전용이다.

Pleos는 현재 사용자 10이므로 테스트 APK 설치 후 해당 사용자에서 실행했다.
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument --user 10 -w com.example.mosim.test/androidx.test.runner.AndroidJUnitRunner
```

## 검증 기록

- APK 빌드 성공, Android 테스트 APK 빌드 성공, Gradle sync 성공.
- JVM 30개 성공: 기존 STT 8, F-1 8, F-3 8, 차량 Mock 1, F 회귀 5.
- Pleos Connect v2.1.0(API 34), 사용자 10에서 Compose 테스트 13개 성공:
  FScreenTest 9, VoiceScreenTest 3, MosimNavigationTest 1.
- FScreenTest: 제안 데이터/시간 누락, 유지/요청 콜백, TTS 실패 시 버튼 유지,
  장소 선택·확인·취소, 검색 중/없음/실패/재시도, 분석 중, 조작 제한,
  두 Mock 흐름의 독립 진입과 미연결 요청 표시.
- 회귀 단위 테스트: 제안 재진입/A→B→A 안내 1회, 준비 중 중단·지연 콜백 차단,
  외부 결과 전 Applied 금지, 검색 예외와 후보 수 제한, 분석 중 STT 재시작 방지·취소.
- 실제 TTS: 에뮬레이터에서 F-1 진입 후 Started→Done 이벤트 수신.
  다시 듣기에서도 Started→Done 이벤트 수신. UI의 읽는 중/다시 듣기 상태를 확인했다.
- 실제 PleosTtsDeviceTest 2개 별도 실행 성공(건너뛰기 없음): ON_DEVICE 한국어 Started→Done,
  Started 직후 stop→Stopped. 4.483초, OK (2 tests). 소리 청취를 판정하는 테스트는 아니다.
- 2026-11-13 재확인(SDK 로그 전체 추적). `MainActivity` → F-1 진입 시 실제 SDK 호출 순서가
  로그에 그대로 찍혔다: `initialize - ON_DEVICE with listener` → `registerCallback` → `addEventListener`
  → `setAudioAttributes(usage=USAGE_ASSISTANT, content=CONTENT_TYPE_SPEECH)` → `updateLocale ko_KR`
  → `generate(... speaker=seoyoon locale=ko_KR)` → AudioTrack에 346,624바이트(173,312프레임) 기록
  → `onDone` → `stop` → `removeEventListener` → `release` → `unregisterCallback`.
  **ON_DEVICE 한국어 합성과 해제까지 실기에서 동작한다.** 오디오 속성을 보조 음성(ASSISTANT)으로 주어
  내비 안내와 경합하지 않게 했다.
- 소리 청취·발음 품질·첫 음절 손실·실차 음성 출력은 미검증이다.
- Lint 성공(오류 0, 경고 있음). 버전/targetSdk 권고, 기존 리소스/offset,
  ModifierParameter, 직접 의존성 표기, SDK 전이 JNA의 16KB 정렬 경고가 있다.
  고정 버전/공통 컴포넌트/기존 시그니처를 임의 변경하지 않았다.
- IDE Preview 렌더 도구는 CompilationAborted로 실패했다.
  별도의 Gradle 컴파일·기기 실행·Compose 테스트는 성공했다.
- F 화면에 NaviHelper 초기화/의존성/경로 API 직접 호출이 없음을 소스 검색으로 확인했다.

## 이번 작업의 생성·수정 파일

main Kotlin 경로는 app/src/main/java/com/example/mosim 기준이다.

| 파일 | 변경 이유 |
|---|---|
| feature/planrevision/PlanSuggestion.kt | 동승자/Mock/장소 식별자, 외부 경유 결과 상태 |
| feature/planrevision/AnnouncementHistory.kt (신규) | 프로세스 범위 제안 ID 안내 이력 |
| feature/planrevision/PlanRevisionViewModel.kt | 대기 중 중단·지연 이벤트 차단, 합성기 소유권/재연결, 외부 결과 |
| feature/planrevision/PlanRevisionScreen.kt | 미연결·승인 상태, 조작 제한, 지도 슬롯, Mock 표기 |
| feature/waypoint/WaypointContracts.kt | 선택적 장소 부가정보와 Mock 표기 |
| feature/waypoint/WaypointViewModel.kt | 예외/취소 구분, 최대 3후보, 외부 반영 상태 |
| feature/waypoint/WaypointScreen.kt | 허구 정보 제거, 선택/요청 결과, 지도 슬롯, 조작 제한 |
| feature/voice/VoiceScreen.kt | 기존 STT 콜백 유지, 분석 대기·결과 연결, TTS 선행 중단 |
| feature/voice/VoiceAnalysisResult.kt (신규) | 외부 분석의 검색/안내 결과 계약 |
| platform/pleos/PleosTtsAdapter.kt (신규) | 실제 ON_DEVICE TTS, 포커스와 SDK 자원 관리 |
| data/mock/MockPlanRevision.kt | Mock 제안·시간임을 명시 |
| data/mock/MockPlaceSearchSource.kt | 모의 거리/시간 표기 |
| navigation/MosimNavHost.kt | 독립 흐름과 외부 입력/결과 주입, 운영/Mock 분리 |
| app/build.gradle.kts | 승인된 TTS v3.3.1 의존성 |
| app/src/main/AndroidManifest.xml | TTS_SERVICE 최소 권한 |
| app/src/debug/AndroidManifest.xml | F 시연 Activity 등록 |
| app/src/debug/java/com/example/mosim/feature/planrevision/FScreenDemoActivity.kt (신규) | Mock/실제 TTS 확인 진입점 |
| app/src/test/java/com/example/mosim/PlanRevisionViewModelTest.kt | JUnit void 반환형 수정 |
| app/src/test/java/com/example/mosim/WaypointViewModelTest.kt | JUnit void 반환형 수정 |
| app/src/test/java/com/example/mosim/FScreenRegressionTest.kt (신규) | 상태/생명주기/검색 실패 회귀 검증 |
| app/src/androidTest/java/com/example/mosim/FScreenTest.kt (신규) | F UI와 두 Mock 흐름 검증 |
| app/src/androidTest/java/com/example/mosim/PleosTtsDeviceTest.kt (신규) | 실제 TTS 완료·중단 이벤트 검증 |
| docs/integration-contracts.md | 실제 F 입력/출력/외부 담당 경계 |
| docs/voice-stt.md | 분석 상태와 최신 진입점 변경 기록 |
| docs/architecture.md | F ViewModel/외부 기능/합성기 소유권 경계 |
| docs/reference/pleos/04_TTS STT SDK.md | 공식 API·AAR로 확인한 AudioAttributes 시그니처 |
| docs/f-screens.md (신규) | 구현·시연·검증·미결 기록 |

README, Home, MosimRoute, 공통 UI, 버전 카탈로그, settings.gradle.kts,
docs/context/sdk-status.md의 작업 전 변경은 이번 작업 결과 목록에 포함하지 않았다.

## 연동 및 남은 확인

실제 코드의 입력/출력 상세는 integration-contracts.md를 본다.
AI·장소 검색·NaviHelper 실제 구현과 E 화면 트리거는 연결되지 않았다.
submitWaypoint가 없으면 연결 예정이며 실제 경로 변경을 주장하지 않는다.

새 [확인 필요]:
1. 내비/통화/Gleo와 TTS 포커스 충돌, 덕킹/중단 및 복귀 정책.
2. TTS 실제 청취·발음·선행 묵음·실차 출력.
3. CarUxRestrictions 기반 interactionAllowed 연결 및 주행 상태 전환 검증.
4. 원본 Figma와 최종 배치 대조.
5. 장소 externalId로 좌표를 조회하는 팀 공통 계약과 E 화면의 실제 진입 이벤트.
6. SDK 전이 JNA의 16KB 페이지 크기 지원(버전 변경은 팀 결정 필요).
