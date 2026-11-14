# MOSIM 구조

> 현재 확정된 것은 패키지 경계와 Pleos SDK 경계뿐이다. 나머지는 팀이 채운다.

## 1. 패키지 구조

```
com.example.mosim
├─ MainActivity.kt          앱 진입점. Compose setContent만 담당
├─ navigation/              화면 경로 상수와 NavHost
├─ ui/                      공통 Compose 요소
│  └─ theme/                색, 타이포
├─ feature/                 화면 단위. 화면 하나당 패키지 하나
│  ├─ home/
│  ├─ passenger/
│  ├─ route/
│  ├─ vehicle/
│  └─ pickup/
├─ data/
│  └─ mock/                 Mock 구현만 모은다
└─ platform/
   └─ pleos/                Pleos SDK 경계. 모델과 인터페이스
```

멀티모듈은 쓰지 않는다. 1주일 일정에서 모듈 분리 비용이 더 크다.

## 2. Pleos SDK 경계

화면은 SDK를 직접 보지 않는다. `platform/pleos`의 인터페이스만 본다.

```
feature/vehicle  →  VehicleStatusSource (interface)
                         ├─ MockVehicleStatusSource      (data/mock, 현재 기본값)
                         └─ PleosVehicleStatusSource     (platform/pleos, 비어 있음)
```

교체 지점은 `VehicleStatusSourceProvider.USE_MOCK` 한 곳이다.

SDK 연동 순서는 `PleosVehicleStatusSource.kt` 주석에 적어 뒀다.

## 3. 아직 정하지 않은 것

- 상태 관리 방식 [작성 필요] — 지금은 화면에서 `produceState`로 직접 읽는다. ViewModel을 둘지 결정 필요
- DI [작성 필요] — 지금은 `object` provider 하나. Hilt 도입 여부 미정
- 서버 통신 계층 [작성 필요] — 서버 API 유무가 정해지면 `data/remote` 추가
- 에러와 로딩 표현 규칙 [작성 필요]

## 4. Pleos 공식 문서에서 확인한 사실

출처: https://document.pleos.ai

### 에뮬레이터

SDK Manager > SDK Update Sites에 아래를 추가하면 Pleos Connect 시스템 이미지를 받을 수 있다.
별도로 URL을 받아야 하는 게 아니다.

- Name: `Pleos Connect System Image`
- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
- Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`

AVD는 Automotive 기기, API 34, Cold boot, RAM 4GB로 만든다. 에뮬레이터 버전은 2.1.0이다.

### SDK 의존성

```kotlin
// settings.gradle.kts
maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }

// app/build.gradle.kts
implementation("ai.pleos.playground:Vehicle:2.3.1")
```

문 상태를 읽으려면 AndroidManifest.xml에 `pleos.car.permission.CAR_DOORS`를 선언한다.

### SDK 접근 권한

- **Vehicle State**: 신청 없이 바로 쓸 수 있다. 차량 상태 읽기는 여기 해당한다
- **Vehicle Control**: "Not Available Yet" 상태다. 신청 자체가 불가능하다. 차량 조작 기능은 지금 만들 수 없다

### 아직 확인되지 않은 것

- `nexus-playground.pleos.ai` Maven 저장소 인증 필요 여부 [확인 필요]
- Vehicle SDK 버전: getting-started는 2.3.1, 샘플 앱 문서는 2.3.2로 적는다 [확인 필요]
- Compose 지원 여부를 Pleos 문서가 언급하지 않는다 [확인 필요]

### 패키지명

Pleos 문서와 Android·Play 공식 문서 어디에도 `com.example` 사용을 막는 규칙은 없다.
다만 배포 후에는 바꿀 수 없다.

> "Once you publish your app, you should never change the application ID."
> — https://developer.android.com/build/configure-app-module

지금 `com.example.mosim`을 쓰고 있다. 배포 전에 정하고 바꾼다. [결정 필요]

## 5. F 화면 경계 (2026-11-13)

F-1은 feature/planrevision, F-3은 feature/waypoint에 둔다.
각 ViewModel이 표시/선택/요청 상태를 관리하며 SDK 타입을 화면에 노출하지 않는다.
AI 분석과 장소 검색은 주입한 suspend 함수/PlaceSearchSource로 분리한다.
NaviHelper 실행은 MosimNavHost.submitWaypoint를 통해 외부로 전달하고 실제 반영 결과만 Applied로 표시한다.
미연결은 NotConnected이며 성공이나 경로 완료를 가정하지 않는다.

F-1 음성은 SpeechSynthesizer 경계에서 PleosTtsAdapter / MockSpeechSynthesizer로 교체한다.
이번 요청에서 개발자가 TTS의 Coming Soon 사전 검증 제한 예외와 v3.3.1 의존성을 명시 승인했다.
docs/context 문서는 수정하지 않는다. ON_DEVICE·한국어, 서버 등록/비밀키 없음.
제안 ID 안내 이력은 프로세스 범위로 유지하고, 오디오 세션은 화면 이탈 시 중지한다.
NavHost가 기본 합성기를 소유하며 화면 ViewModel은 close하지 않는다.
실제 검증 및 오디오 정책의 한계는 docs/f-screens.md에 기록한다.

주행 제한 입력은 interactionAllowed이며 운영 기본값은 false다.
실제 CarUxRestrictions 취득은 통합 담당자가 연결한다. Mock 시연은 명시적으로 허용한다.
기존 공통 디자인 토큰/컴포넌트와 STT SDK 어댑터는 재작성하지 않는다.

## 6. F-2 음성 요청 인식 (2026-11-08 개발자 승인)

F-2는 `feature/voice/VoiceViewModel`에서 상태를 관리하고 `platform/pleos/SpeechRecognizer`에 명령한다.
SDK 콜백은 메인 스레드로 전달한다. 세션 번호로 취소 후 도착하는 결과를 차단하고 최종 결과만 Channel로 전달한다.
기존 다른 화면의 상태 관리 방식과 Vehicle provider는 변경하지 않는다.
실제 ON_DEVICE 어댑터와 비동기 Mock을 provider에서 선택한다. 별도 DI 라이브러리는 추가하지 않는다.
운전 중 본문에는 버튼을 두지 않고 수동 조작은 debug Activity로 분리한다.
수명주기, 이벤트 순서, 실행 방법과 미검증 사항은 `docs/voice-stt.md`에 있다.
