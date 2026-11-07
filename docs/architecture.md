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
