# MOSIM

차량 동승 서비스 Android 앱이다. Jetpack Compose로 만든다.

와이어프레임과 요구사항은 아직 확정되지 않았다. 지금 들어 있는 화면은 이동 경로를 확인하기 위한 임시 화면이다.

## 확정 버전

버전을 바꾸지 않는다. 바꿔야 하면 팀에 먼저 말한다. 변경 전 상태는 `docs/versions-baseline.md`에 있다.

| 항목 | 버전 | 근거 |
|---|---|---|
| JDK | **17** | AGP 8.13 최소·기본 JDK. Pleos 공식 샘플도 JDK 17 |
| Gradle | **8.13** | AGP 8.13이 요구하는 최소 Gradle |
| AGP | **8.13.0** | 안정판 Android Studio(Narwhal 4 / Otter)가 지원하는 최신 AGP |
| Kotlin | **2.2.20** | AGP 8.13.0 지원 범위. Kotlin 2.3은 AGP 8.13.2부터 |
| Compose Compiler | Kotlin과 동일 | Kotlin 2.0부터 컴파일러 플러그인 버전이 Kotlin 버전과 같다 |
| Compose BOM | **2025.08.00** | Compose 라이브러리 버전은 BOM이 정한다. 개별 버전을 적지 않는다 |
| compileSdk | **36** | AGP 8.13이 지원하는 최대 API. 최신 androidx가 36을 요구한다 |
| targetSdk | **35** | Pleos Vehicle SDK 공식 샘플과 동일 |
| minSdk | **28** | Pleos Playground 배포 요건(서명키 rotation·lineage) |

Compose, Navigation, Lifecycle 버전은 `gradle/libs.versions.toml`에 모아 뒀다.

## 개발 환경 설치

1. **Android Studio 안정판**을 설치한다. Narwhal 4 또는 Otter 안정판이면 된다.
   캐너리는 쓰지 않는다. 팀원 간 AGP 호환이 깨진다.
2. **JDK 17**을 쓴다. Android Studio 번들 JDK가 17이 아니면
   `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`에서 17로 지정한다.
3. **Android SDK**에서 아래를 설치한다. (`Tools > SDK Manager`)
   - SDK Platform **API 36** (compileSdk)
   - SDK Platform **API 34** (Pleos Connect 에뮬레이터용)
   - Android SDK Build-Tools **36.0.0**
   - Android SDK Platform-Tools

4. Pleos Connect 에뮬레이터가 필요하면 `Tools > SDK Manager > SDK Update Sites`에 추가한다.
   당장 화면 개발에는 필요 없다. 일반 에뮬레이터로 충분하다.

   - Name: `Pleos Connect System Image`
   - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
   - Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`

   AVD는 Automotive 기기, API 34, Cold boot, RAM 4GB로 만든다.

## Clone과 빌드

```bash
git clone https://github.com/codesooo/Mosim.git
cd Mosim
```

`local.properties`는 Git에 없다. Android Studio로 프로젝트를 한 번 열면 SDK 경로를 넣어 자동 생성한다.
CLI만 쓸 거면 직접 만든다.

```properties
# local.properties
sdk.dir=C:/Users/<사용자>/AppData/Local/Android/Sdk
```

빌드와 테스트

```bash
./gradlew :app:assembleDebug          # 디버그 APK
./gradlew :app:testDebugUnitTest      # 단위 테스트
./gradlew :app:assembleRelease        # 릴리스 APK
```

Windows PowerShell에서는 `.\gradlew.bat`을 쓴다.

## 실행

Android Studio에서 `app` 구성을 선택하고 에뮬레이터나 기기에서 실행한다.
일반 Android 에뮬레이터(API 28 이상)로도 뜬다. Pleos 에뮬레이터가 없어도 된다.

실행하면 임시 메인 화면이 나오고 이동계획은 ‘A. 이동계획’ 버튼에서 시작한다. 신규 동승자를 등록하면 이동 설정으로 이어지고, 픽업 위치나 목적지의 ‘변경’을 누르면 장소를 검색할 수 있다.

## Mock 개발

차량 데이터는 Mock이다. F-2 음성 요청은 실제 Pleos STT와 Mock을 선택할 수 있다(실제 음성 인식은 미검증).

- `platform/pleos/VehicleStatusSource.kt` — 화면이 보는 인터페이스
- `data/mock/MockVehicleStatusSource.kt` — 현재 쓰는 Mock 구현
- `platform/pleos/PleosVehicleStatusSource.kt` — 실제 SDK 연동 자리. 비어 있다
- `platform/pleos/VehicleStatusSourceProvider.kt` — `USE_MOCK` 한 줄로 교체한다

Vehicle SDK API를 확인하지 않은 상태라 차량 연동 코드는 작성하지 않았다. 순서는 `PleosVehicleStatusSource.kt` 주석에 있다.

## 문서

- `CLAUDE.md` — 공통 개발 규칙, 커밋 규칙, 디렉터리 규칙
- `docs/requirements.md` — 요구사항 (비어 있음, 팀이 채운다)
- `docs/architecture.md` — 구조와 미결정 항목
- `docs/versions-baseline.md` — 버전 변경 전 상태와 변경 이유

## Git

- 저장소: https://github.com/codesooo/Mosim
- `main`에 직접 push하지 않는다. `feature/<이름>` 브랜치 + Pull Request.
- `local.properties`, `.idea/`, 서명키(`*.jks`, `*.keystore`), 빌드 산출물은 커밋하지 않는다.

## F-2 음성 요청 STT

개발자 승인으로 SpeechToText 3.3.1을 연동했다. ON_DEVICE / ko-KR을 사용하며 앱 등록 키는 필요 없다.
일반 에뮬레이터에서는 Mock, Pleos 에뮬레이터에서는 Pleos STT를 선택한다. 실제 SDK 음성 인식 성공은 아직 검증하지 않았다.

Debug 앱 설치 후 `adb shell am start -n com.example.mosim/.feature.voice.VoiceDemoActivity`로 테스트한다.
이 진입점은 release에 포함되지 않는다. 기존 메인 메뉴는 변경하지 않았다.
실행·권한·테스트와 외부 콜백 연결 방법은 [F-2 STT 문서](docs/voice-stt.md)를 참고한다.
