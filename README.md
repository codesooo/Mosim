# 모심 (募心)

> 동승자마다 다른 취향을 기억하고, 출발 전 차량 점검부터 탑승 전 실내 준비, 이동 중 케어와 도착 후 기록까지 운전자를 돕는 동승자 에스코트 서비스, ‘모심’

현대자동차그룹 차량용 앱 개발 공모전 제출작입니다.

**모심**은 동승자를 정성껏 모신다는 의미와 동승자의 취향과 마음을 기록해 모은다는 의미를 담고 있습니다.

**핵심 가치:** 동승자 탑승 후 운전자의 화면 조작 최소화

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 동승자 프로필 관리 | 동승자별 선호 설정 및 이동 계획 관리 |
| 출발 전 차량 점검 | 차량 상태 확인 및 충전소 경유 추천 |
| 픽업 전 실내 준비 | 선호 온도, 좌석, 음악 설정 적용 지원 |
| 에스코트 모드 | 탑승 확인 및 이동 중 맞춤형 케어 |
| 음성 요청 및 안내 | Pleos STT 기반 음성 인식과 TTS 안내 |
| 장소 검색 및 경유 | 주변 장소 검색 및 내비게이션 경유지 추가 |
| 도착 후 기록 | 이동 중 변경된 설정 확인 및 프로필 업데이트 |

일부 차량 상태 및 동승자 설정 기능은 Mock 데이터를 사용합니다.

---

## 실행 방법

Pleos Connect 에뮬레이터 실행 후 APK를 설치합니다.

```bash
adb install -r --user 10 모심_모심_release.apk
adb shell am start --user 10 -n com.example.mosim/.MainActivity
```

Pleos Connect는 Android Automotive 멀티유저 환경이므로 `--user 10` 옵션을 사용합니다. 설치 후 앱 런처에서도 실행할 수 있습니다.

앱 실행 후 **동승자 선택 → 출발 전 점검 → 픽업 및 탑승 → 에스코트 → 도착** 순서로 주요 기능을 확인할 수 있습니다.

### 소스 코드 빌드

```bash
git clone https://github.com/codesooo/Mosim.git
cd Mosim
./gradlew :app:assembleDebug
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

Android SDK 경로와 외부 API 키는 루트 디렉터리의 `local.properties`에서 설정합니다.

```properties
sdk.dir=C:/Users/<사용자>/AppData/Local/Android/Sdk

# 선택 사항
KAKAO_REST_API_KEY=<REST API 키>
KAKAO_NATIVE_APP_KEY=<네이티브 앱 키>
```

API 키가 없으면 장소 검색은 Mock 데이터로, 지도는 대체 화면으로 표시됩니다. 제출용 Release APK는 별도 서명 설정이 필요하므로 아래 다운로드 링크를 이용해 주세요.

---

## 테스트 환경 및 필요한 설정

| 항목 | 환경 |
|---|---|
| 플랫폼 | Pleos Connect / Android Automotive OS |
| 에뮬레이터 | **Pleos Connect v2.1.0** |
| Android 버전 | API 34 |
| 화면 규격 | **2560 × 1440 / 160 dpi** |
| 개발 언어 | Kotlin 2.2.20 |
| UI | Jetpack Compose / Material3 |
| 빌드 환경 | Gradle 8.13 / AGP 8.13.0 |
| JDK | 17 기준 (제출 APK는 JDK 21로 빌드) |

**주요 SDK**

- Pleos Vehicle SDK 2.3.2
- Pleos NaviHelper 3.0.5
- Pleos SpeechToText / TextToSpeech 3.3.1
- Kakao Map SDK 2.15.2 및 Kakao Local API

**에뮬레이터 설치**

Android Studio의 `Tools > SDK Manager > SDK Update Sites`에서 다음 저장소를 추가합니다.

- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
- macOS: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`

음성 인식과 알림 기능 사용 시 마이크 및 알림 권한 승인이 필요합니다.

일부 기능은 에뮬레이터 환경에서 Mock으로 동작하며, 실제 차량 마이크를 통한 STT 인식은 아직 검증되지 않았습니다. 자세한 SDK 연동 및 검증 현황은 [SDK 검증 기록](docs/context/sdk-status.md)을 참고해 주세요.

---

## APK 다운로드

**Release v1.0.0**

- [GitHub Release에서 APK 다운로드](https://github.com/codesooo/Mosim/releases/download/v1.0.0/Mosim_Mosim_release.apk)
- [저장소 내 APK 파일](apk/모심_모심_release.apk)

| 항목 | 정보 |
|---|---|
| 빌드 | Release / 서명 완료 |
| 파일 크기 | 81.9 MB |
| SHA-256 | `65fe131d51beff2178c5fa9ea4f8910b4a205dff26774b08d260e56b8733ffc9` |

---

**상세 기술 문서:** [아키텍처](docs/architecture.md) · [모듈 연동 명세](docs/integration-contracts.md) · [SDK 검증 현황](docs/context/sdk-status.md)
