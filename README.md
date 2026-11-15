# 모심 (募心)

> 동승자마다 다른 취향을 기억하고, 출발 전 차량 점검부터 탑승 전 실내 준비, 이동 중 케어와 도착 후 기록까지 운전자를 돕는 동승자 에스코트 서비스, ‘모심’

현대자동차그룹 차량용 앱 개발 공모전 제출작입니다. Pleos Connect(Android Automotive OS) 환경에서 동작합니다.

핵심 설계 원칙은 **동승자 탑승 후 운전자의 화면 조작 최소화**입니다. 동승자가 타기 전에는 정보를 모아 판단을 돕고, 탄 뒤에는 큰 글씨와 음성 위주로만 동작합니다.

- 저장소: https://github.com/codesooo/Mosim
- 상세 설계와 검증 기록: [`docs/`](docs/)

---

## 주요 기능

| 기능 | 화면 | 데이터 출처 |
|---|---|---|
| 동승자 프로필 선택과 이동 설정 | A-1~A-3 | Mock 프로필 + 카카오 로컬 |
| 출발 전 차량 점검과 충전소 경유 판단 | B-1~B-3 | Mock 차량 상태 + NaviHelper |
| 픽업 전 실내 환경 준비 | C-1, C-2 | **Vehicle SDK**(공조 온도) + Mock(좌석·음악) |
| 탑승 확인 후 에스코트 모드 전환 | D-1, E-1 | Mock 탑승 신호 + NaviHelper |
| 음성 요청 처리 | F-2 | **Pleos SpeechToText** |
| 경유 제안과 음성 안내 | F-1 | 앱 내 규칙 판단 + **Pleos TextToSpeech** |
| 주변 장소 검색과 경유지 추가 | F-3 | 카카오 로컬 API + NaviHelper `addWaypoint` |
| 도착 후 프로필 업데이트 | G-1, G-2 | NaviHelper 도착 이벤트 |

전체 흐름은 `A-1 → A-3 → B-1 → B-3 → C → D-1 → E-1 → (F-1 | F-2 → F-3) → E-1 → G-1 → G-2`입니다. 화면 전환 조건은 [`docs/integration-contracts.md`](docs/integration-contracts.md)에 정리돼 있습니다.

음성 의도 해석은 LLM이 아니라 키워드 규칙(`VoiceCommandParser`)으로 처리합니다. Pleos LLM SDK가 종료됐고, 판단 근거를 설명할 수 있는 방식을 택했습니다.

---

## 실행 방법

Pleos Connect 에뮬레이터를 먼저 실행한 뒤 설치합니다.

```bash
adb install -r --user 10 모심_모심_release.apk
adb shell am start --user 10 -n com.example.mosim/.MainActivity
```

Pleos Connect는 Android Automotive 멀티유저 환경이라 앱이 **user 10**에 설치됩니다. `--user 10`을 빼면 설치는 되지만 런처에 보이지 않을 수 있습니다. 설치 후 런처에서 **모심** 아이콘을 눌러도 실행됩니다.

| 항목 | 값 |
|---|---|
| applicationId | `com.example.mosim` |
| 메인 Activity | `com.example.mosim.MainActivity` |
| 빌드 타입 | release (서명됨, APK Signature Scheme v2) |

HOME 화면의 개발용 메뉴로 각 화면에 단독 진입할 수 있고, ‘동승자 선택’부터 누르면 A→G 전체 흐름이 이어집니다. 차량 상태는 에뮬레이터의 **Virtual Console(8088 포트)** 에서 바꿔 가며 시연합니다.

### 소스에서 빌드

```bash
git clone https://github.com/codesooo/Mosim.git
cd Mosim
./gradlew :app:assembleDebug        # 디버그 APK (Windows: .\gradlew.bat)
./gradlew :app:testDebugUnitTest    # 단위 테스트
```

`local.properties`는 저장소에 없습니다. Android Studio로 프로젝트를 한 번 열면 SDK 경로가 담긴 파일이 자동 생성되고, CLI만 쓴다면 직접 만듭니다.

```properties
sdk.dir=C:/Users/<사용자>/AppData/Local/Android/Sdk

# 선택 항목입니다. 없으면 장소 검색은 Mock, 지도는 자리 표시로 동작합니다.
KAKAO_REST_API_KEY=<카카오 REST API 키>
KAKAO_NATIVE_APP_KEY=<카카오 네이티브 앱 키>
```

`assembleRelease` 서명은 저장소에 포함되지 않는 `keystore.properties`에서 읽습니다. 이 파일이 없으면 release는 서명 없이 빌드되므로, 제출용 서명 APK는 아래 다운로드 경로에서 받으시면 됩니다.

---

## 테스트 환경 및 설정

### 에뮬레이터와 화면 규격

| 항목 | 값 |
|---|---|
| Pleos Connect 에뮬레이터 | **v2.1.0** (`android-automotive`, API 34) |
| 화면 규격 | **2560 × 1440**, density 160 (mdpi) |
| AVD 설정 | Automotive 기기, API 34, Cold boot, RAM 4GB 권장 |
| API Level | compileSdk 36 / targetSdk 35 / minSdk 28 |
| JDK · Gradle · AGP | 17 · 8.13 · 8.13.0 (제출 APK는 JDK 21로 빌드) |
| Kotlin · Compose BOM | 2.2.20 · 2025.08.00 (Material3) |

에뮬레이터 시스템 이미지는 `Tools > SDK Manager > SDK Update Sites`에 아래 주소를 추가해 설치합니다.

- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
- Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`

사용 SDK는 Pleos Vehicle 2.3.2, NaviHelper 3.0.5, SpeechToText 3.3.1, TextToSpeech 3.3.1, 카카오맵 Android SDK 2.15.2입니다.

### 권한

| 권한 | 쓰는 곳 |
|---|---|
| `INTERNET` | 카카오 로컬 장소 검색, 지도 타일 |
| `pleos.car.permission.CAR_CLIMATE` / `CONTROL_CAR_CLIMATE` | 공조 온도 조회·설정 |
| `pleos.car.permission.CAR_SEATS` / `CONTROL_CAR_SEATS` | 동승석 등받이 |
| `pleos.car.permission.NAVI_ROUTE` / `NAVI_ROUTE_SEARCH` | 경로·ETA 조회, 경유지 추가 |
| `pleos.car.permission.STT_SERVICE` / `TTS_SERVICE` | 음성 인식·안내 |
| `android.permission.RECORD_AUDIO` | STT `request()` 경로 음성 입력 |
| `android.permission.POST_NOTIFICATIONS` | 헤드업 알림 |

`RECORD_AUDIO`와 `POST_NOTIFICATIONS`는 런타임 승인이 필요하며, F-2 진입과 에스코트 진입 시 요청합니다.

키나 네트워크가 없어도 앱은 종료되지 않습니다. `KAKAO_REST_API_KEY`가 없으면 장소 검색이 Mock으로, `KAKAO_NATIVE_APP_KEY`가 없으면 지도가 자리 표시로 동작합니다.

---

## APK 다운로드

| 경로 | 링크 | 파일명 |
|---|---|---|
| **저장소 내 경로** (제출 기준) | [`apk/모심_모심_release.apk`](apk/모심_모심_release.apk) | `모심_모심_release.apk` |
| GitHub Release | [v1.0.0 · 바로 받기](https://github.com/codesooo/Mosim/releases/download/v1.0.0/Mosim_Mosim_release.apk) | `Mosim_Mosim_release.apk` |

두 파일은 같은 빌드 산출물입니다(SHA-256 일치). GitHub Release가 첨부파일 이름에 한글을 허용하지 않아 ASCII 이름으로 올렸고, 제출 규정의 `팀명_앱이름_release.apk` 형식은 저장소 `apk/` 경로가 따릅니다.

| 항목 | 값 |
|---|---|
| 빌드 | release, 서명됨(APK Signature Scheme v2) |
| 크기 | 85,844,099 bytes (81.9 MB) |
| SHA-256 | `65fe131d51beff2178c5fa9ea4f8910b4a205dff26774b08d260e56b8733ffc9` |

---

## 검증 범위 및 제한사항

검증하지 못한 항목은 검증하지 못했다고 적었습니다. 날짜별 상세 기록은 [`docs/context/sdk-status.md`](docs/context/sdk-status.md)에 있습니다.

**실제 SDK 연동 + 에뮬레이터 검증 완료**

- 공조 목표 온도 조회·설정 (Vehicle SDK)
- 경로 요청, 남은 거리, 현재 위치, 경유지 추가 (NaviHelper)
- 한국어 음성 안내 재생 (TextToSpeech, ON_DEVICE · ko-KR)
- 주변 장소 검색 (카카오 로컬 API)

**부분 검증**

- **음성 인식(STT)**: 초기화·권한·마이크 점유와 PCM 주입 인식까지 확인했으나, 사람이 차량 마이크에 말해 인식되는 경로는 미검증입니다.
- **좌석 등받이 제어**: 호출은 성공하지만 Virtual Console에 좌석 위치 항목이 없어 실제 이동을 확인할 수 없었습니다.
- **도착 감지**: 에뮬레이터에서 차량이 이동하지 않아 실제 도착 이벤트는 미검증입니다.

**Mock으로 동작하는 범위** — 탑승·하차 감지 신호(착석·벨트·도어), 배터리 잔량과 주행 가능 거리, 좌석·음악 적용 상태, 날씨와 주차 정보, 동승자 프로필 저장소입니다. 인터페이스와 Mock까지만 구현했고 추측에 기반한 SDK 호출은 작성하지 않았습니다.

**release APK 실행 검증 (2026-11-15)** — Pleos Connect v2.1.0 x86_64 에뮬레이터에서 설치·실행에 성공했고 크래시는 없었습니다. A-1부터 E-1 에스코트 모드 진입까지 화면 전환을 확인했습니다. 다만 해당 인스턴스에서 내비 게이트웨이 DNS 해석과 차량 HAL 연결이 실패해 **F·G 화면은 이번 실행에서 확인하지 못했습니다.** 두 경우 모두 앱은 종료되지 않고 안내 문구로 대체 동작했습니다. 위 SDK 검증 기록은 arm64 환경 기준입니다.

**알려진 제한사항**

- 영속 저장이 없습니다. 동승자 프로필과 이동 기록은 앱을 종료하면 사라집니다.
- 카카오 벡터맵 AAR에 x86_64 네이티브 라이브러리가 없어, x86_64 에뮬레이터에서는 WebView 지도(Leaflet + OpenStreetMap)로 대체 렌더링합니다. arm 기기의 네이티브 지도 렌더링은 미확인입니다.
- 패키지명 `com.example.mosim`과 추천 온도 보정 계수는 아직 확정 전입니다.

---

## 문서

| 문서 | 내용 |
|---|---|
| [`AGENTS.md`](AGENTS.md) | 개발 규칙, 커밋 규칙, 디렉터리 규칙 |
| [`docs/architecture.md`](docs/architecture.md) | 구조와 미결정 항목 |
| [`docs/integration-contracts.md`](docs/integration-contracts.md) | 화면 전환 조건, 공유 상태, 외부 경계 |
| [`docs/context/project-status.md`](docs/context/project-status.md) | 현재 상태와 결정 사항 |
| [`docs/context/sdk-status.md`](docs/context/sdk-status.md) | SDK별 상태와 에뮬레이터 검증 기록 |
| [`docs/versions-baseline.md`](docs/versions-baseline.md) | 확정 버전과 선정 근거 |
