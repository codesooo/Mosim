# Pleos Connect — TextToSpeech SDK · SpeechToText SDK

> 출처: Pleos Playground 개발자 문서 (https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro, .../SpeechToText/intro)
> 수집일: 2026-11-07 · 기준 버전: TextToSpeech v3.3.1, SpeechToText v3.3.1
> Kotlin 예제만 수록했습니다.
> ℹ️ **LLM SDK는 2026-10-19부로 제공이 종료**됐습니다. Gleo AI 연동이 필요하면 Gleo Interface를 이용하라고 안내돼 있습니다(01 문서 릴리즈 노트).

두 SDK는 구조가 거의 같습니다: `Mode`(HYBRID/SERVER/ON_DEVICE) 선택 → `initialize()` → 리스너 등록 → (서버 모드면) `registerApp()` → 요청 → `stop()` → 리스너 제거·`release()`.
 
---

# Part A. TextToSpeech (TTS)

## A1. Introduction
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro -->

텍스트를 음성으로 합성해 **차량 오디오 시스템으로 재생**하는 API입니다. 3rd party 앱은 Pleos Connect의 Speech Service와 연동됩니다.

### 음성 합성 모드
- `HYBRID`(기본): 서버 기반 streaming TTS 우선, 네트워크 실패 시 On-device 엔진으로 자동 전환. 서버 합성을 쓰려면 My Project 자격 증명으로 앱 등록 필요.
- `SERVER`: 서버 기반 streaming TTS만 사용. 네트워크·앱 등록 필요, 실패해도 On-device로 전환하지 않고 오류 이벤트 전달.
- `ON_DEVICE`: 차량 On-device 엔진. 네트워크·앱 등록 불필요.
### 진입점
`ai.pleos.playground.tts.TextToSpeech`. 생성자에 `Context`와 `Mode` 전달(생략 시 HYBRID).
```kotlin
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
 
val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID)
```
사용 순서: 객체 생성 → `initialize()` → `addEventListener()` → (필요 시) `setLocale()` → (HYBRID/SERVER) 앱 등록 → `onConnected()` 이후 `speak()` → `stop()` → `removeEventListener()` 후 `release()`.

## A2. Importing
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk -->
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
}
```

## A3. Declaring permissions
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions -->
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```
초기화, 리스너 등록, 앱 등록, 합성 요청 등 TTS 서비스 접근 API에 필요합니다. 미선언 시 초기화·호출이 실패할 수 있고, `PERMISSION_DENIED`가 오면 manifest와 실행 환경의 권한을 확인하세요.

## A4. Initialization / Release
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release -->
```kotlin
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.constant.ErrorType
 
textToSpeech.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // TextToSpeech API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})
// 결과 콜백이 필요 없으면: textToSpeech.initialize()
```
```kotlin
override fun onDestroy() {
    textToSpeech.removeEventListener(eventListener)
    textToSpeech.release()
    super.onDestroy()
}
```
객체는 컴포넌트 수명주기에 맞춰 재사용하고, `release()` 후 다시 쓰려면 새 객체를 만들어 초기화합니다.

## A5. App registration (서버 기반 합성)
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration -->
My Project 생성 시 발급받은 `Client ID`·`Client Secret`으로 등록합니다. 초기화 완료 후 호출. SERVER 모드는 필수, HYBRID도 서버 합성을 쓰려면 필요, ON_DEVICE만 쓰면 불필요.
```kotlin
textToSpeech.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 합성을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```

## A6. Event listeners
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners -->
| Callback | Description |
| --- | --- |
| onReady() | speak() 요청 처리 준비 완료 |
| onStart() | 음성 재생 시작 |
| onUpdatedRms(rms) | 재생 중 RMS 값 갱신 |
| onDone() | 재생 정상 종료 |
| onStop() | 진행 중 재생 중지 |
| onError(errorType) | 처리 중 오류 |
```kotlin
private val eventListener = object : EventListener {
    override fun onReady() = Unit
    override fun onStart() = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = Unit
    override fun onStop() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}
textToSpeech.addEventListener(eventListener)
textToSpeech.removeEventListener(eventListener)
```

## A7. Set locale
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale -->
`speak()` 전에 `setLocale()`로 설정하면 해당 요청부터 적용됩니다. 지원: `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`. 호출하지 않으면 같은 앱에서 이전에 설정한 값, 없으면 시스템 locale을 사용합니다.
```kotlin
import java.util.Locale
textToSpeech.setLocale(Locale.KOREA)
```

## A8. Start and stop synthesis
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop -->
```kotlin
textToSpeech.speak("안녕하세요. 무엇을 도와드릴까요?")   // isFinal 기본값 true
 
// 여러 조각을 하나의 발화로: 마지막 조각만 isFinal = true
textToSpeech.speak("안녕하세요. ", isFinal = false)
textToSpeech.speak("TextToSpeech SDK입니다.", isFinal = true)
 
textToSpeech.stop()   // 완료되면 onStop() 호출
```
앱 등록이 필요한 모드에서는 `onConnected()` 이후에 요청하세요.

## A8-1. AudioAttributes API 확인 (2026-11-13)

공식 API Reference에 setAudioAttributes가 열거되어 있다.
현재 웹 본문에는 타입 상세가 나오지 않아, Maven v3.3.1 AAR의 classes.jar를 javap로 확인했다.

```kotlin
// 확인한 실제 시그니처: public final void setAudioAttributes(android.media.AudioAttributes)
textToSpeech.setAudioAttributes(audioAttributes)
```

출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/set-audio-attributes

## A9. Error code
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code -->
오류는 `OnInitializeListener`, `OnServerConnectionListener`, `EventListener`에서 `ErrorType`으로 받습니다.

| ErrorType | Description | Recommended action |
| --- | --- | --- |
| AUDIO_ERROR | 오디오 출력/재생 처리 실패 | 오디오 설정·출력 장치 확인 후 재요청 |
| INVALID_REQUEST_ERROR | 요청 값/상태가 올바르지 않음 | 텍스트·호출 순서 확인 |
| MIC_ERROR | 오디오 입력 장치 처리 오류 | 오디오 장치 상태 확인 |
| NETWORK_ERROR | 네트워크 통신 실패 | 연결 확인 후 재요청 |
| NOT_SUPPORTED_API | 현재 모드/환경에서 미지원 API | Mode·플랫폼 지원 범위 확인 |
| ON_DEVICE_ENGINE_ERROR | On-device 엔진 오류 | 엔진 상태 확인 또는 HYBRID/SERVER 검토 |
| PERMISSION_DENIED | TTS 서비스 접근 권한 없음 | manifest의 TTS_SERVICE 확인 |
| SERVER_INTERNAL_ERROR | Speech Service 내부 오류 | 잠시 후 재요청, 반복 시 문의 |
| TIMEOUT | 제한 시간 초과 | 네트워크·서버 확인 후 재요청 |
| UNAUTHENTICATED | 앱 인증 실패 | My Project 자격 증명 또는 UA 앱 정보 확인 |
| UNAVAILABLE_LANGUAGE | 요청 언어 사용 불가 | 지원 Locale로 변경 |
| UNKNOWN | 분류되지 않은 오류 | 호출 시점·환경 정보 수집 후 문의 |

`ErrorType`은 늘어날 수 있으니 `when`에 기본 분기를 두세요.
```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkTtsPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```
 
---

# Part B. SpeechToText (STT)

## B1. Introduction
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/intro -->

음성을 텍스트로 변환하는 API입니다. Pleos Connect의 Speech Service와 연동됩니다.

> ⚠️ **음성 명령 기능은 제공하지 않습니다.** STT는 발화를 `"음악 틀어줘"` 같은 텍스트로 돌려줄 뿐 실제 동작은 실행하지 않습니다. 인식한 명령으로 앱 기능을 실행하려면 **Gleo Interface**를 참고하세요.

### 음성 인식 모드
- `HYBRID`(기본): 서버 인식 우선, 네트워크 실패 시 On-device로 자동 전환. 서버 인식은 앱 등록 필요.
- `SERVER`: 서버 인식만. 네트워크·앱 등록 필요, 실패해도 On-device로 전환하지 않음.
- `ON_DEVICE`: 차량 On-device 엔진. 네트워크·앱 등록 불필요.
### 진입점
```kotlin
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
 
val speechToText = SpeechToText(applicationContext, Mode.HYBRID)
```
사용 순서: 생성 → `initialize()` → `addListener(ResultListener)` → (필요 시) `setLocale()` → (HYBRID/SERVER) 앱 등록 → `request()` → `stop()` → `removeListener()` 후 `release()`.

## B2. Importing
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk -->
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
```

## B3. Declaring permissions
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions -->
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.STT_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```
초기화, 결과 리스너 등록, 앱 등록, 인식 요청, 오디오 전송 등에 필요합니다. `PERMISSION_DENIED`가 오면 manifest와 실행 환경 권한을 확인하세요.

## B4. Initialization / Release
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release -->
```kotlin
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener
 
speechToText.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // SpeechToText API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})
// 또는 speechToText.initialize()
 
override fun onDestroy() {
    speechToText.removeListener(resultListener)
    speechToText.release()
    super.onDestroy()
}
```

## B5. App registration (서버 기반 인식)
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/app-registration -->
```kotlin
speechToText.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 인식을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```
SERVER는 필수, HYBRID도 서버 인식을 쓰려면 필요, ON_DEVICE만 쓰면 불필요. `onConnected()` 이후 서버 요청을 시작하세요.

## B6. Result listeners
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners -->
| Callback | Description |
| --- | --- |
| onReady() | 인식 요청 처리 준비 완료 |
| onStartedRecognition() | 인식 시작 |
| onUpdated(stt, completed) | 인식 텍스트 갱신. `completed == true`면 해당 결과 완료(최종) |
| onUpdatedEpdData(on, off) | EPD(End Point Detection)의 음성 시작/종료 위치(입력 오디오 버퍼 byte offset) |
| onUpdatedRms(rms) | 입력 신호 RMS 값(dB 아님) |
| onEndedRecognition() | 인식 종료 |
| onError(errorType) | 오류 |
```kotlin
private val resultListener = object : ResultListener {
    override fun onReady() = Unit
    override fun onStartedRecognition() = Unit
    override fun onUpdated(stt: String, completed: Boolean) {
        // 중간 또는 최종 인식 결과를 처리합니다.
    }
    override fun onUpdatedEpdData(on: Long, off: Long) = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onEndedRecognition() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}
speechToText.addListener(resultListener)
speechToText.removeListener(resultListener)
```
이벤트를 놓치지 않도록 `request()`/`sendAudio()` **전에** 리스너를 등록하세요.

## B7. Set locale
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale -->
`request()` 전에 설정한 locale이 해당 세션 기준. 지원: `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`. 미설정 시 이전 설정값 → 시스템 locale 순.
```kotlin
speechToText.setLocale(Locale.KOREA)
```

## B8. Start and stop recognition
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop -->
`request()`를 호출하면 SDK가 **차량 마이크를 활성화**하고 인식을 시작합니다. 호출 전에 초기화·리스너 등록(서버 모드면 앱 등록 또는 UA 인증)을 마치세요.
```kotlin
speechToText.addListener(resultListener)
speechToText.request()
 
speechToText.stop()   // 인식 종료 시 onEndedRecognition()
```

## B9. Sending audio data (앱이 가진 PCM 인식)
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data -->
차량 마이크 대신 앱의 PCM 오디오를 `sendAudio(ByteArray)`로 전달합니다. **ON_DEVICE 모드에서만** 사용 가능. 예시의 3,200-byte 청크·100ms 간격은 16 kHz, 16-bit, mono PCM 실시간 전송 기준입니다.
```kotlin
// 전체 버퍼 한 번에
val pcmData = applicationContext.assets
    .open("test_audio.pcm")
    .use { it.readBytes() }
speechToText.sendAudio(pcmData)
speechToText.completeAudioSend()
 
// 청크 스트리밍
private const val BYTES_PER_100_MS = 3_200
pcmData.asList()
    .chunked(BYTES_PER_100_MS)
    .forEach { chunk ->
        speechToText.sendAudio(chunk.toByteArray())
        Thread.sleep(100L)
    }
speechToText.completeAudioSend()
```
마지막 데이터 후 반드시 `completeAudioSend()`로 입력 종료를 알리세요.

## B10. Error code
<!-- 원문: /docs/pleos-only/connect-sdk-pleos/SpeechToText/error-code -->
| ErrorType | Description | Recommended action |
| --- | --- | --- |
| AUDIO_ERROR | 입력 오디오 데이터 처리 실패 | 입력 데이터·오디오 형식 확인 |
| INVALID_REQUEST_ERROR | 요청 값/상태 오류 | 호출 순서·중복 요청·입력 값 확인 |
| MIC_ERROR | 마이크 입력 시작/처리 오류 | 마이크 상태와 **센서 개인정보 보호 설정** 확인 |
| NETWORK_ERROR | 네트워크 실패 | 연결 확인 후 재요청 |
| NOT_SUPPORTED_API | 현재 모드/환경 미지원 | Mode·플랫폼 지원 범위 확인 |
| ON_DEVICE_ENGINE_ERROR | On-device 엔진 오류 | 엔진 상태 확인 또는 HYBRID/SERVER 검토 |
| PERMISSION_DENIED | STT 서비스 접근 권한 없음 | manifest의 STT_SERVICE 확인 |
| SERVER_INTERNAL_ERROR | Speech Service 내부 오류 | 잠시 후 재요청 |
| TIMEOUT | 제한 시간 초과 | 네트워크·서버 확인 |
| UNAUTHENTICATED | 앱 인증 실패 | My Project 자격 증명 또는 UA 앱 정보 확인 |
| UNAVAILABLE_LANGUAGE | 요청 언어 사용 불가 | 지원 Locale로 변경 |
| UNKNOWN | 분류되지 않은 오류 | 환경 정보 수집 후 문의 |
```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkSttPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

> 에뮬레이터에서 STT를 테스트하려면 Emulator setup의 "마이크 접근 설정" 단계를 먼저 완료하세요.
 
