# F-2 음성 요청 인식

> 다른 기능과 연결하려면 `docs/integration-contracts.md`의 STT 절을 먼저 본다.
> 이 문서는 내부 구현·생명주기·검증 기록을 다룬다.

## 범위와 구조

F-2의 STT 엔진은 텍스트만 반환한다. AI 의도 분류·장소 검색·경로·TTS 호출은 엔진에 포함하지 않는다.
2026-11-13: 화면에 별도의 Analyzing 상태와 외부 analyzeRequest 연결점을 추가했다.
최종 텍스트 콜백 계약과 SDK 어댑터/provider는 유지한다.
NavHost는 인식 시작 전에 TTS를 중지하며, 분석 결과가 SearchPlaces일 때만 F-3을 연다.
분석 함수가 없으면 연결 예정, Message 결과/분석 예외는 F-2 내부 안내로 표시한다.
분석 중 ON_STOP/이탈은 coroutine 취소 및 기존 취소 정책을 따른다.
최신 진입점과 시그니처는 integration-contracts.md를 기준으로 한다.
기존 아래 검증 기록은 당시 STT 작업 범위이며 이번 F 흐름 테스트는 f-screens.md에 별도 기록한다.

`VoiceScreen → VoiceViewModel → SpeechRecognizer → PleosSpeechRecognizer / MockSpeechRecognizer`

- SDK 타입은 `platform/pleos/PleosSpeechRecognizer.kt`에만 있다.
- `SpeechRecognizerProvider.create(context, mode)`로 Pleos / Mock / MockFailure를 명시적으로 선택한다.
- 실제 모드는 `SpeechToText(applicationContext, Mode.ON_DEVICE)`와 `Locale.KOREA`를 사용한다.
- 서버 앱 등록이나 Client ID/Secret은 사용하지 않는다.
- 일반 Android 기기에서는 Mock을 선택한다. 실제 서비스가 실패해도 가짜 인식 성공으로 바꾸지 않는다.
- VehicleStatusSourceProvider와 다른 기능 화면은 변경하지 않았다.

## 결과와 생명주기

`VoiceScreen(onBack, onSpeechRecognized = { text -> ... })`를 사용한다.
NavHost의 `onSpeechRecognized` 인자로도 외부 결과 처리기를 연결할 수 있다. 현재 기본 처리기는 비어 있다.
현재 개발용 Home에 F-1/F-2 독립 진입 메뉴가 있다. 실제 E 화면의 진입 트리거는 통합 담당자가 연결한다.

- 대기 → 준비 → 인식 중 → 완료 / 오류 / 취소. 중지 중은 최종 결과를 기다리는 별도 상태다.
- 부분 결과는 표시만 하고, 비어 있지 않은 최종 결과만 세션당 한 번 전달한다.
- 초기화부터 20초간 결과가 없으면 타임아웃 처리한다. 이 값은 앱의 임시 정책이며 SDK 보장값이 아니다.
- 종료 이벤트 또는 수동 중지 후 1.5초까지 최종 결과를 기다린다. 부분 결과를 최종 결과로 승격하지 않는다.
- 중복 시작은 무시하고, 취소·실패·완료 시 세션 번호를 변경해 과거 콜백을 차단한다.
- 최종 결과는 Channel로 전달한다. 화면 재구성으로 같은 결과를 다시 전달하지 않는다. 프로세스 종료 후 복원은 지원하지 않는다.
- 화면이 멈추거나 구성에서 빠지면 취소하고 SDK를 해제한다. ViewModel 제거 시 close한다.
- SDK에는 별도 cancel 호출이 안내되어 있지 않다. 취소는 콜백 차단 → stop → 같은 Listener 제거 → release로 구현했다.
- SDK 초기화 실패, 권한 없음, 마이크 오류, 미지원, 연결 실패와 타임아웃을 UI 오류로 바꾼다.

## 권한

공식 가이드가 지정한 `pleos.car.permission.STT_SERVICE`를 앱 Manifest에 선언한다.
차량 마이크는 Speech Service가 사용하며, 이번 구현은 AudioRecord로 앱에서 직접 녹음하지 않는다.
공식 가이드와 SDK AAR 매니페스트에는 앱의 RECORD_AUDIO 런타임 요청이 명시되지 않아 임의로 추가하지 않았다.
`PERMISSION_DENIED`와 `MIC_ERROR`는 각각 권한 및 마이크/개인정보 설정 안내로 표시한다.
[확인 필요] Pleos 에뮬레이터의 서비스 권한 부여와 마이크 접근 설정, ON_DEVICE 엔진의 한국어 인식 성공.
SDK AAR에는 INTERNET 권한이 포함되어 최종 Manifest에 병합된다. ON_DEVICE에서 앱 등록은 하지 않는다.

## UI와 디자인 근거

제공 이미지의 네 번째 프레임과 `docs/ui-components.md`의 F-2를 기준으로 작성했다.
MosimAppContainer, StatusChip, PleosButton 및 기존 색·글자·모양·간격 토큰을 재사용한다.
왼쪽 내비게이션, 상태 표시줄과 하단 시스템 UI는 그리지 않는다.
의도 칩은 외부 분석 결과가 없으므로 표시하지 않는다. Mock 모드에서는 Mock 표시만 추가한다.
주행용 본문에는 버튼을 두지 않고 화면 진입 시 인식을 시작한다. CCP Back은 취소 후 이전 화면으로 돌아간다.
중지·재시도·취소 버튼은 별도의 debug 테스트 진입점에서만 제공한다.

[확인 필요] 원본 Figma 수치 없이 추정한 부분:
- 파형은 정적 5개 막대. 너비·간격은 ChipPaddingVertical, 최대 높이는 AppPadding을 재사용했다.
- 막대 상대 높이는 0.3 / 0.65 / 1 / 0.65 / 0.3이다. 실제 음량을 나타내지 않는다.
- 중앙 콘텐츠 간격 CardGap, 카드 안 여백 CardPadding은 공통 토큰을 재사용했다.
- 인식 상태별 오류·취소·완료 문구와 debug 조작부는 기존 토큰으로 구성했다.

## 실행과 시연

Android Studio에서 debug 앱을 설치한 후 다음 Activity를 실행한다.

```powershell
adb shell am start -n com.example.mosim/.feature.voice.VoiceDemoActivity
```

이 Activity는 `src/debug`에만 있으며 release에는 포함되지 않는다.

1. 일반 에뮬레이터: `Mock 성공` → 준비·부분 인식·최종 인식 확인 → Back → 전달된 최종 결과 확인.
2. `Mock 오류` → 타임아웃 오류 안내 → 다시 듣기. 이 모드는 계속 실패하는 테스트 시나리오다.
3. 인식 중 `취소` → 결과 전달 없음 → 다시 듣기.
4. `인식 중지` → 비동기 최종 결과 수신 또는 오류 확인.
5. Pleos 에뮬레이터: 마이크 접근 설정 후 `Pleos STT` → 한국어 발화 → 텍스트와 최종 콜백 확인.
6. Pleos 실패 시 오류를 확인하고 Back → `Mock 성공`으로 독립적인 시연 가능.

재시도·중지·취소의 외부 제어가 필요하면 통합 담당자가 VoiceViewModel 명령에 연결한다.
실제 주행 UI 제한에 대한 플랫폼 심사·기기 검증은 별도 작업이다.

## 검증

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

- JVM 테스트: Mock 비동기·부분·최종 이벤트, 실패/빈 결과, 중복 시작/결과, 이전 세션 결과 차단, 중지, 취소, 재시도, 타임아웃과 해제.
- Compose 기기 테스트: 부분 텍스트 및 수동 버튼 미표시, debug 재시도, Mock 최종 결과/콜백.
- 실제 SDK 검증: **안 됨**. 빌드 성공이나 Mock 성공을 실제 SDK 성공으로 간주하지 않는다.

## 공식 문서

- Introduction: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro
- Programming Guide: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk
- 초기화: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release
- 리스너: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners
- 언어: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale
- 권한: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions
- 시작·중지: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop
- API Reference: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/entry

API Reference의 클래스·메서드 페이지와 가이드를 확인했다. Reference 본문이 텍스트 추출에 나타나지 않는 부분은 가이드의 Kotlin 예제와 실제 3.3.1 라이브러리 컴파일로 확인한다.

## 이번 작업의 검증 결과

- `:app:assembleDebug :app:testDebugUnitTest`: 성공. STT 테스트 8개와 기존 차량 테스트 1개, 총 9개 성공.
- `:app:assembleDebugAndroidTest`: 성공. Compose 테스트 3개를 포함한 테스트 APK 컴파일 완료.
- `:app:lintDebug`: 성공, 오류 0 / 경고 21. 경고에는 고정 버전 갱신 안내, 기존 리소스·Switch 코드 및 JNA 네이티브 라이브러리 16KB 정렬 문제가 있다.
- [확인 필요] SDK와 함께 들어온 JNA 5.13.0의 x86_64 `libjnidispatch.so`가 16KB 정렬을 만족하지 않는다. 16KB 페이지 환경은 별도 호환성 확인이 필요하며 SDK 버전/전이 의존성을 임의로 바꾸지 않았다.
- 로컬 JDK 17이 없어 설치된 JBR 21.0.11로 Gradle을 실행했다. 프로젝트의 Java/Kotlin 바이트코드 타깃 17 및 기존 버전은 유지했다. 팀 기준 JDK 17 환경의 재확인은 남아 있다.
- `git diff --check`: 성공.

## 2026-11-09 Pleos 에뮬레이터 실행 확인

Pleos Connect v2.1.0 에뮬레이터(API 34, `crp_avd_x86_64`)에 debug APK를 설치하고 `VoiceDemoActivity`의
`Pleos STT`로 실제 SDK를 호출했다. 아래는 logcat에서 직접 확인한 내용이다.

- `pleos.car.permission.STT_SERVICE`: 설치 시 `granted=true`. 런타임 권한 요청은 필요하지 않았다.
- 호출 순서가 의도대로 나갔다: `initialize with listener` → `createStt ON_DEVICE` →
  `bindOnDeviceSttService` / `onServiceConnected` → `addListener` → `updateLocale ko_KR` → `request`.
- 서비스 쪽에서 `setLocale ko-KR`, `getSpeechToText currentLocale ko_KR`로 한국어가 적용됐다.
  ON_DEVICE 엔진은 `speechSdkVersion=1.17.1`, variant `gleoAiCoral`이다.
- `onReady`(ReadyToStartRecognition)가 올라왔고, 서비스가 16kHz `AudioRecord`로 녹음을 시작했다.
  시스템 상태 표시줄에 마이크 사용 표시가 떴다. 앱이 직접 녹음하지 않는다는 전제가 맞았다.
- 앱 타임아웃 20초가 동작해 오류 문구와 `다시 듣기`가 표시됐고, 해제 경로가
  `stop` → `removeListener` → `release` 순으로 나간 뒤 서비스가 `AudioRecord`를 해제했다.
- 즉 **의존성·권한·초기화·리스너·로케일·시작·해제까지는 실제 SDK에서 동작한다.**

### 한국어 인식 검증 (sendAudio 경로) — 성공

에뮬레이터 마이크는 무음만 들어와 `request()` 경로로는 한국어 인식을 확인할 수 없었다.
공식 문서가 ON_DEVICE 전용으로 안내하는 `sendAudio()`로 PCM을 직접 넣어 검증했다.

- 검증 도구: `app/src/debug/.../SttAudioCheckActivity.kt` (debug 전용, release 미포함).
  `assets/ko.pcm`을 3,200바이트(100ms)씩 `sendAudio()`로 보내고 `completeAudioSend()`로 끝낸다.
- 음원: Windows SAPI `Microsoft Heami`(ko-KR)로 합성한 16kHz / 16-bit / mono PCM, 4.44초.
  문장은 `커피 마시고 싶은데 근처에 카페 있을까?`다.
- 결과: 부분 결과가 `커피` → `커피 마시고` → ... 순으로 `completed=false`로 올라왔고,
  `onEndedRecognition` 뒤에 `completed=true`로 **`커피 마시고 싶은데 근처에 카페 있을까?`** 가 왔다.
  입력 문장과 정확히 일치한다. `onUpdatedEpdData(on=0, off=128000)`도 함께 왔다.
- 즉 **ON_DEVICE 한국어 인식 엔진과 `Locale.KOREA` 설정은 에뮬레이터에서 실제로 동작한다.**

재현 방법:

```powershell
adb shell am start --user 10 -S -n com.example.mosim/.feature.voice.SttAudioCheckActivity
adb logcat -d -s STTCHECK:*
```

### 아직 검증하지 못한 것

- **사람이 차량 마이크에 말해서 인식되는 경로는 여전히 미검증이다.**
  위 검증은 `sendAudio()`로 음원을 주입한 것이고, 운영 화면(`PleosSpeechRecognizer`)은
  `request()`로 마이크를 쓴다. 엔진과 한국어는 검증됐으나 마이크 입력 구간은 남아 있다.
- AVD의 `mic\available`를 `true`로 바꾸고 `-allow-host-audio`로 재기동해 두었다.
  마이크 사용 표시까지는 뜨지만, 실제 발화 확인은 사람이 해야 한다.
  절차: `VoiceDemoActivity` → `Pleos STT` → 한국어 발화 → 화면 텍스트와 최종 콜백 확인.
- Compose 기기 테스트(`connectedDebugAndroidTest`)는 이번에도 실행하지 않았다.
