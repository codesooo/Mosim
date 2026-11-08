# Pleos Connect — 앱 개발 가이드 (Vehicle App Developer Guide)

> 출처: Pleos Playground 개발자 문서 (https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro)
> 수집일: 2026-11-07. Pleos Connect 앱이 **반드시 준수해야 할 정책과 구현 방법**을 다룹니다.
> 구성: 주행 규제 / 오디오 포커스 / Suspend 모드 오디오 / CCP(물리 컨트롤러) / Maps·Navigation 앱 / Music·Audio 앱 / TTS 선행 묵음 / IME window resize / FAQ(실행 환경) / FAQ(ADB)
 
---

## 1. 주행 규제 개발 가이드 ⭐
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide -->

앱은 플랫폼이 제공하는 **`CarUxRestrictions`** 상태를 기준으로 주행 중 UX 제한을 적용해야 합니다. (지역·기능별 제한 범위는 05 기획 가이드 2절)

### CarUxRestrictions 연동
- AAOS `CarUxRestrictionsManager`로 `CarUxRestrictions`를 얻어 주행 규제 상태를 확인합니다.
- `CarUxRestrictionsManager.registerListener()`로 등록하면 `onUxRestrictionsChanged()` 콜백으로 변경된 값을 받습니다.
- DO(Distraction Optimization)를 선언한 앱도 주행 규제가 활성화되면 `CarUxRestrictions`와 `ActiveRestrictions`를 확인해 **기능별 UX 제한을 직접 적용**해야 합니다.
### 샘플 코드 — 동영상 재생 제한
```java
if (mCurrentCarUxRestrictions.isRequiresDistractionOptimization()
        && (mCurrentCarUxRestrictions.getActiveRestrictions()
            & CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0) {
    // 동영상 재생 중지/차단
}
```
> 수집 시 원문 코드 끝부분 일부가 누락되어, 앞부분(원문)과 이어지는 표준 AAOS 비트 플래그 검사 형태로 보완했습니다. 정확한 원문은 링크에서 확인하세요.

### UX 규제 항목과 CarUxRestrictions 매핑
- 기능별 플래그는 `onUxRestrictionsChanged()`로 받은 `CarUxRestrictions`의 `ActiveRestrictions` 기준으로 적용합니다. 예: 동영상(DMB)은 `UX_RESTRICTIONS_NO_VIDEO`로 판단.
- UX 규제 피그마 사양서는 Playground에 공개하지 않습니다.
- **플랫폼이 지역별 규제 사양에 맞는 `ActiveRestrictions`를 내려주므로, 앱은 notify로 받은 `CarUxRestrictions`만 반영하면 됩니다.**
### 테스트 방법 (ADB)
```bash
adb shell cmd hmg_car_service --service drivingstate driving_regulation on    # 주행 규제 ON
adb shell cmd hmg_car_service --service drivingstate driving_regulation off   # 해제
```
Virtual Console의 Gear Selection(P/R/N/D)으로도 상태 전환을 확인할 수 있습니다.
 
---

## 2. 오디오 포커스 개발 가이드
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development -->

차 안에서는 음악, 내비, 통화, 안전·긴급 안내 등이 경쟁적으로 재생됩니다. AAOS 오디오 포커스를 바탕으로 하되 **Pleos Connect 오디오 포커스 정책(Interaction Matrix v1.0, 2026-01-28)**을 반드시 따릅니다.

### AAOS 기본 개념
- 요청의 `CarAudioContext`와 현재 보유자의 `CarAudioContext` 간 사전 정의된 Interaction으로 처리.
- **R(Reject)**: 새 요청 거부 / **E(Exclusive)**: 새 요청이 획득, 기존 보유자 상실 / **C(Concurrent)**: 조건 충족 시 동시 유지(보통 duck 기반).
- 포커스는 오디오 제어의 유일 수단이 아니라 충돌 방지·간접 제어 수단.
- C를 의도하면 요청은 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 사용(기존 보유자가 duck 시 pause하도록 설정돼 있으면 C가 깨질 수 있음).
- 보유자가 여럿이면 각 결과 중 가장 보수적인 것 적용: **R > E > C**.
- (Optional) 통화 중 내비 C→R 사용자 설정, 지연 가능한 포커스.
### Pleos Connect Interaction Matrix
- 행: 현재 보유자 컨텍스트 / 열: 새 요청 컨텍스트 / 값: E·C·R
- 컨텍스트: MUSIC, NAVIGATION, VOICE_COMMAND, CALL_RING, CALL, ALARM, NOTIFICATION, SYSTEM_SOUND, EMERGENCY, SAFETY, VEHICLE_STATUS, ANNOUNCEMENT
- (매트릭스 표 자체는 원문 이미지/표 참조)
### 개발사가 구현할 규칙
- **요청**: 재생 전 `AudioAttributes` + `AudioFocusRequest`로 포커스 요청. C를 기대하는 경우(예: NAVIGATION이 MUSIC과 동시) `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`. E가 필요한 경우(CALL, EMERGENCY 등)는 목적에 맞는 gain.
- **포커스 변경 이벤트 처리(필수)**: AAOS상 필수는 아니지만 자동차 UX/정책 준수를 위해 구현.
    - `AUDIOFOCUS_GAIN` → 정상 재생
    - `AUDIOFOCUS_LOSS` → 즉시 중단/정지
    - `AUDIOFOCUS_LOSS_TRANSIENT` → 일시정지 후 복귀 가능
    - `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` → 볼륨 감쇄
- **Duck**: `LOSS_TRANSIENT_CAN_DUCK` 수신 시 볼륨 감쇄, 해제 조건에서 원복. duck 중 pause하면 C가 깨질 수 있음.
- **다중 홀더**: result = C로 시작 → 모든 currentHolder에 대해 `cell = MATRIX[currentHolder][incoming]`, 가장 강한 결과(R 최강) 선택 → grant/steal/fail 처리.
### 컨텍스트별 가이드
- **EMERGENCY**: current일 때 대부분 incoming은 R. incoming EMERGENCY는 대부분 E로 선점. EMERGENCY-EMERGENCY는 C.
- **SAFETY**: MUSIC/NAVIGATION/알림과 대체로 C/E. EMERGENCY가 current면 SAFETY incoming은 R.
- **CALL / CALL_RING**: CALL 중 MUSIC incoming은 R(통화 중 음악 금지), NAVIGATION incoming은 C(통화 중 내비 허용; 플래그로 C→R 가능).
- **NAVIGATION ↔ MUSIC**: C로 공존 기본. NAVIGATION은 MAY_DUCK 요청 사용.
- **VOICE_COMMAND(Gleo)**: 일반 오디오보다 우선하되 EMERGENCY, CALL 등 Reject 컨텍스트보다는 아님. Gleo 호출로 `AUDIOFOCUS_LOSS_TRANSIENT`를 받으면 즉시 재생 중단, 필요 시 포커스 반납.
### 권장 사항
- 매트릭스는 하드코딩하지 말고 `interaction_matrix.json`/csv로 탑재 후 로딩(키: `current_context`, `incoming_context` → E|C|R).
- 로그: incoming context, gain type, client id, current holders, holder별 결과, 최종 결과·처리, duck 적용/해제 이벤트.
### 검증 체크리스트
- MUSIC 재생 중 NAVIGATION 요청 → C (뮤직 duck + 내비 재생)
- CALL 중 MUSIC 요청 → R (실패)
- MUSIC 중 CALL 요청 → E (통화가 선점)
- EMERGENCY 중 어떤 요청 → R
- MUSIC+NAVIGATION 동시 홀더 상태에서 VOICE_COMMAND 요청 → 보수적 결과(R/E 우선) 적용 확인
### CarAudioContext ↔ AttributeUsages
| CarAudioContext | AttributeUsages |
| --- | --- |
| MUSIC | MUSIC, UNKNOWN, GAME, MEDIA |
| NAVIGATION | NAVIGATION, ASSISTANCE_NAVIGATION_GUIDANCE |
| VOICE_COMMAND | VOICE_COMMAND, ASSISTANT |
| CALL_RING | CALL_RING, NOTIFICATION_RINGTONE |
| CALL | CALL, VOICE_COMMUNICATION, VOICE_COMMUNICATION_SIGNALING |
| ALARM | ALARM |
| NOTIFICATION | NOTIFICATION, NOTIFICATION_* |
| SYSTEM_SOUND | SYSTEM_SOUND, ASSISTANCE_SONIFICATION, ASSISTANCE_ACCESSIBILITY |
| EMERGENCY | EMERGENCY |
| SAFETY | SAFETY |
| VEHICLE_STATUS | VEHICLE_STATUS |
| ANNOUNCEMENT | ANNOUNCEMENT |
(시스템 컨텍스트는 Android 11 이상)
 
---

## 3. Suspend 모드 — 오디오 포커스
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus -->

Suspend 모드: 시동(IGN) OFF 후 완전 종료가 아닌 **저전력 유지 상태**. UI·오디오·미디어 중단, 일부 시스템 작업(Garage mode 등)만 제한적 수행, 빠른 Resume을 위해 상태 유지. 앱은 "언제든 중단되고 다시 시작될 수 있는 상태"로 간주해야 합니다.

**원칙(위험 등급)**: Suspend 진입 시 모든 일반 오디오 세션은 출력되지 않아야 하며, **각 앱은 보유한 포커스를 반드시 반납**해야 합니다. Resume 후 포커스는 자동 복원되지 않고, 이전 오디오 상태를 그대로 복원하지 않습니다.

**진입 시 처리 순서**: 진행 중 오디오 즉시 중단 → 포커스 반납(abandon) → 재생 상태 저장(재생 여부, 위치) → player·track 등 리소스 해제. 이 과정에서 새 재생·포커스 요청 금지.
**예외**: 전화·긴급 통신, 안전 경고음, 시스템 필수 음성 안내(최소 범위).
**Resume**: 현재 상태 재평가 후 필요 시 포커스 재요청. 자동 재생 여부는 OEM/사용자 정책.
**준수**: Suspend 중 포커스 유지 금지, 백그라운드 재생 금지, Resume 대비 포커스 선점·유지 금지.

```kotlin
class PlayerActivity : AppCompatActivity() {
    private lateinit var audioManager: AudioManager
    private var isPlaying = false
    private var lastPlaybackPositionMs = 0L
    private var hasAudioFocus = false
 
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausePlayback()
                abandonAudioFocus()
            }
        }
    }
 
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
 
    fun startPlayback() {
        if (!requestAudioFocus()) return
        // TODO: 실제 플레이어 start
        isPlaying = true
    }
 
    fun onSuspendEntering() {
        pausePlayback()            // 진행 중 작업 중단
        savePlaybackState()        // 상태 저장
        abandonAudioFocus()        // 포커스 반납
        releaseTransientResources()// 네트워크/타이머/센서 정리
    }
 
    fun onResumeFromSuspend() {
        restorePlaybackState()
        val shouldAutoResume = false // OEM/앱 정책에 따라 결정
        if (shouldAutoResume) startPlayback()
    }
 
    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }
 
    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
    }
    // pausePlayback, savePlaybackState, restorePlaybackState, releaseTransientResources ... (원문 참고)
}
```
 
---

## 4. CCP(Center Control Panel) 개발 가이드
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/ccp-rotary-controller-development -->

CCP = 차량 물리 버튼·하드키 입력 장치(Rotary Controller, 다이얼, 물리 버튼). CCP Back 하드키는 이전 단계 이동 또는 앱/시스템 Back 동작.

### 앱 분류별 정책
- **User-installed 앱**(App Market으로 사용자 설치 — 공모전 앱은 여기 해당): CCP 지원은 **선택**. 미지원이어도 등록·배포 제한 없음. 단, **필수 조작을 터치만으로 완결**할 수 있어야 함.
- **Pre-installed 앱**(기본 탑재): CCP 지원 필수(정적 콘텐츠, 브라우저/WebView, 게임 등 예외 가능, 계약서 명시). 회전→스크롤, 클릭→선택, 좌우→포커스 이동 등. 등록 전 CCP 심사.
- **System 앱**: 플랫폼/OEM 제공.
### CCP 미지원 선언
```xml
<application ...>
    <meta-data
        android:name="com.hmg.rotary.ROTARY_SUPPORTED"
        android:value="false" />
</application>
```

### CCP Back 하드키 → 앱 숨김 처리 (필수, 모든 앱)
- 앱 최상단 화면(더 이상 이전 화면 없음)에서 CCP Back 입력 시 **앱을 종료하지 말고 숨김(Hide)** 처리. 미디어 등 백그라운드 기능은 계속 동작.
- 하위 화면/팝업이 있으면 기존 Back 정책(이전 화면/팝업 닫기).
- 구현: CCP Back이 Android 표준 Back 이벤트로 오는지 확인 → `OnBackInvokedDispatcher` 등 사용 → 최상위 화면에서만 **`moveTaskToBack(true)`** 호출. `finish()`·`finishAffinity()` 사용 금지. 프로세스가 종료될 수 있으므로 상태 저장·복원.
- **최상단에서 Back 시 앱이 종료되거나 `moveTaskToBack(true)` 숨김이 구현되지 않으면 심사 반려 대상.**
---

## 5. Maps/Navigation 앱 개발 가이드
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide -->

**기본 정책**: 운전자 안전 최우선 / 단일 경로·단일 내비게이션 / 주행 중 조작 최소화 / 카테고리 기반 기능 제한 / Pleos·AAOS 표준 API와 UX 가이드 준수.

**카테고리 선언** (내비컨테이너 사용을 위해 필수):
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.APP_MAPS" />
</intent-filter>
```
App Market 등록 시에도 동일 카테고리. 선언과 실제 기능 불일치 시 즉시 반려, 심사 중 기능 변경 시 재심사.

**단일 경로 정책**: AAOS에서는 하나의 Navigation 앱만 Active Route를 가질 수 있음.
| 상태 | 설명 | 허용 범위 |
| --- | --- | --- |
| Active Route | 경로 안내 | 음성·TBT 허용 |
| Inactive Route | 지도 탐색 | 안내 금지 |
| Background | 백그라운드 | 모든 안내 금지 |
Phone Projection과의 관계에도 적용. `CarAppFocusManager.requestAppFocus`, `OnAppFocusChangedListener` 사용. 경로 전환은 사용자의 명시적 선택 또는 기존 앱 오류/종료 시에만, 사용자에게 안내하고 동의 없이 자동 전환 금지.

**주행 중**: 금지 — 3 depth 초과 메뉴, 긴 리스트 스크롤, 영상·애니메이션·광고 / 허용 — 음성 입력, 단일 터치 주요 기능, 즐겨찾기 목적지 선택.
**UX 심사**: 플랫폼·AAOS UX 가이드, 터치 영역 최소 크기, 주행 핵심 정보 상시 노출, 주행 중 전체 화면 팝업 금지.
**권한**: 위치 필수, 마이크는 음성 입력 시에만, 불필요한 권한은 반려. 비공식 센서/API 금지.
**성능**: 실행·전환 지연 최소, 장시간 안정, Crash/freeze 시 반려.
**보안**: 위치 데이터 암호화, HTTPS, 개인정보 처리방침 제공, 불필요한 위치 저장 금지.
**즉시 반려**: 주행 중 광고, 영상·게임 요소, 복수 내비 동시 경로 안내, 카테고리 위반, OEM/AAOS 정책 위반.
 
---

## 6. Music/Audio 앱 개발 가이드
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/music-audio-app-developement-guide -->

(App Market 등록 시 심사 기준으로 적용)
- **카테고리**: 음악 스트리밍·재생, 팟캐스트·오디오북·라디오, 차량 내 미디어 재생 오디오 플랫폼. **영상이 주 기능이면 제외.** 오디오 재생이 주 기능이어야 하며, 음악을 보조로만 제공하면 즉시 반려.
- **하위 카테고리 필수**: 음악 / 팟캐스트 / 오디오북 / 라디오. 불명확하면 반려 가능. 단일 리스트 혼합 노출 제한.
- **매니페스트**:
```xml
<application ... android:appCategory="audio">
    <meta-data android:name="androidx.car.app.launchable" android:value="true" />
</application>
```
- **필수**: `MediaBrowserService`(MBS) 구현 + `MediaSession`으로 재생 상태·제어 제공. 미구현 시 배포 제외 가능. **미디어 센터**(MBS 기반 템플릿) 사용 권장.
  | 기능 | 미디어 센터 사용 | 미사용 |
  | --- | --- | --- |
  | 스티어링 휠 키 | 기본 제공 | 별도 구현 필요(미구현 시 반려 가능) |
  | 미디어 앱 라이브러리 등록 | 지원 | 미지원 |
  | Tune Knob(다음/이전 곡) | 지원 | 미지원 |
  | 차량 Media 버튼 | 지원 | 미지원 |
  | 차량 Mode 버튼 | 미지원 | 미지원 |

- **스티어링 휠 키(필수)**: `MediaSessionCompat` + `Callback` — 재생, 일시정지, 정지, 미디어 ID 기반 재생, 검색 기반 재생(가능하면 이전/다음 트랙).
- **자동 재생 금지**: 앱 연결·프로필 변경 시에도 자동 재생 금지.
- **포커스 상실**: 게임 앱 실행 등으로 `AUDIOFOCUS_LOSS` 시 stop + `MediaSession` 비활성화.
```kotlin
private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focus ->
    when (focus) {
        AudioManager.AUDIOFOCUS_LOSS -> {
            stopPlayback()
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pausePlayback()
        AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
    }
}
```
- 서비스 생명주기: `onCreate()`에서 `startService()`로 Started 상태 유지, `onStartCommand()`·재생 시 `startForeground()`, `onTaskRemoved()`에서 `stopMedia()` → `stopSelf()` → `super.onTaskRemoved()` 순서. 쓰리핑거 동작 시 `onStop()` → `ExoPlayer.stop()`.
- **Gleo AI 연동**: "멜론 앱에서 아이유 노래 틀어줘" 같은 곡 검색·재생 + 핵심 기능 3~5개 음성 기능 제공 가능. 방법: ① **Gleo Interface**(앱이 function 구현 + schema 등록) ② Android 표준 미디어 API(Media3 `MediaSession`/`MediaLibraryService`; 검색 음성 기능은 `MediaLibraryService` 검색 구현 필수, 스키마 없는 방식은 향후 업데이트 예정).
- **위반 조치**: 카테고리 불일치 → 수정 요청/반려, 하위 카테고리 미정의 → 보완 요청, 반복 위반 → 노출 제한/등록 중단.
---

## 7. TTS / 음성 안내 선행 묵음 가이드
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide -->

첫 음절 손실·시작부 클리핑·출력 경로 초기화 지연을 막기 위해 모든 음성 안내 재생 전 **선행 묵음 150~200 ms** 권장(최소 150 ms, 200 ms 초과는 체감 지연).
- 적용 위치 우선순위: PCM/WAV 앞단 silence padding → TTS 결과 후처리로 묵음 추가 → 플레이어 재생 지연(스케줄링 편차가 커 비권장).
- 하나의 발화로 이어지는 연속 음성에는 추가 묵음 불필요, 새 세션 시작 시 재적용.
- 출력 경로 초기화, 포커스/세션 확보, 디코더·플레이어 준비도 함께 확인. 선행 묵음은 초기화 문제 자체를 대체하지 않음.
---

## 8. IME 동작에 따른 window resize 처리
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/ime-window-resize-handling-guide -->

| 구분 | 사양 |
| --- | --- |
| EditText/TextField가 있는 window | IME 표시 시 view가 위로 밀려 올라가는 것이 사양 |
| EditText/TextField가 없는 window | IME 표시 시 view가 앵커링되지 않아야 함 → 앱 수정 필요 |

Dialog의 insets 적용 대상을 `statusBars`·`navigationBars`로 한정하면 IME insets 영향을 받지 않습니다.
```kotlin
// System Dialog
val params = WindowManager.LayoutParams(...)
val typesToFit = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
params.setFitInsetsTypes(typesToFit)
windowManager.addView(overlay, params)
 
// App Dialog
val dialog = Dialog(this)
dialog.window?.let { win ->
    val params = win.attributes
    params.setFitInsetsTypes(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    win.attributes = params
}
dialog.setContentView(R.layout...)
dialog.show()
```
IME 외 SystemBars insets 대응은 `View.OnApplyWindowInsetsListener`에서 개별 처리. Activity 유형은 Google "Handle input method visibility" 가이드 기준.
 
---

## 9. FAQ — 앱 실행 환경 및 플랫폼 리소스 ⭐
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq -->

- **실행 플랫폼**: `android.os.Build.DEVICE`(`ro.product.device`) → `CONNECT_S`, `CONNECT_L`, `CONNECT_W`. **실차에서만 유효**, 에뮬레이터에서는 구분 용도로 사용 불가.
- **지역 코드**: `product.sw.target_region` → `KOR`, `EUR`, `USA`, `CAN`.
- **내비게이션 탑재 여부**: 확인 방법 비공개(partnership@pleos.ai 문의).
- **클러스터 속도(차속) 조회**: 클러스터는 Google **`PERF_VEHICLE_SPEED`** 프로퍼티를 사용. 같은 속도를 쓰려면 AAOS **`CarPropertyManager`로 `PERF_VEHICLE_SPEED`**를 조회(권한 선언 후 현재 값 조회 또는 변경 이벤트 구독). GPS 계산 속도는 클러스터와 다를 수 있음.
- **배터리 정보**: Vehicle SDK의 `getEvBatteryStateOfCharge`.
- **미디어 코덱**: 플랫폼별로 다르며 에뮬레이터 결과를 실차에 그대로 적용하지 말 것.
    - Connect-S/L 오디오: FLAC, AC3+, ALAC, APE, PCM, TTA, AIFF, WMA, OGG, DSD, DFF/DSF, DTS, WAV, MP2, MP3, BSAC, ADPCM, HE-AAC(+), AAC, EAC3(+JOC), OPUS, AMR 등 / 비디오: MPEG-1/2, VC-1, WMV, H.263, MP42/43, MPEG4 Part2, Xvid, VP6~9, H.264, HEVC(main 10), x265, AV1 등
    - Connect-W 오디오: PCM, AAC, MPEG, AMR, WMA, Opus, Vorbis, FLAC, ALAC, APE, Dolby, DTS, ADPCM, AIFF, TTA, BSAC / 비디오: MPEG-1/2, MPEG-4 Part 2, H.263, H.264, H.265, AV1, VP8, VP9
    - Emulator 디코더: AMR-NB/WB, FLAC, G.711 A/μ-law, AAC, MP3, Opus, PCM Raw, Vorbis / H.263, AV1, AVC, HEVC, MPEG-4 Part 2, VP8, VP9. 인코더: AMR-NB/WB, FLAC, AAC, Opus / H.263, AV1, AVC, HEVC, MPEG-4 Part 2, VP8, VP9
---

## 10. FAQ — 디버깅용 ADB 명령
<!-- 원문: /docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide -->

- **설치**: Mac `brew install android-platform-tools` / Windows `choco install adb` 또는 `winget install Google.AndroidSDKPlatformTools` / Ubuntu `sudo apt install android-tools-adb android-tools-fastboot` → `adb version`
- **기기 확인**: `adb devices` (예: `emulator-5554 device`)
- **CCS 연결**(블루링크 등): CCS 실차 전용. Devbox·Emulator에서는 사용하지 말 것.
- **화면 캡처**: `adb exec-out screencap -p > ~/Desktop/screen_$(date +"%Y%m%d_%H%M%S").png`
- **화면 녹화**: `adb exec-out screenrecord --output-format=h264 - > ~/Desktop/screen.h264` → `Ctrl+C` 종료 → `ffmpeg -framerate 60 -i ~/Desktop/screen.h264 -c copy ~/Desktop/screen.mp4` (공모전 시연 영상 제작에 유용)
- **앱 설치**: `adb install -d -r app.apk` (-d 다운그레이드 허용). 실패 시 `adb push app.apk /data/local/tmp/` → `adb shell pm install -d -r /data/local/tmp/app.apk`
- **앱 실행**: `adb shell pm list packages | grep <일부>` → `adb shell monkey -p <패키지> -c android.intent.category.LAUNCHER 1` 또는 `adb shell am start -n <패키지>/<Activity>`
- **삭제/데이터 비우기**: `adb uninstall <패키지>` / `adb shell pm clear <패키지>`
- **강제 종료**: `adb shell am force-stop <패키지>`
- **텍스트 입력**: `adb shell input text "test"`
- **기어 변경**: Virtual Console 사용
- **주행 규제 ON/OFF**(모든 차종 공통): `adb shell cmd hmg_car_service --service drivingstate driving_regulation on|off`
- **유틸리티 모드**: 차속 0 / 주차 브레이크 On / 기어 P 상태에서 원문의 `vcs_simulator_rx` 명령 실행(원문 참고)
 
