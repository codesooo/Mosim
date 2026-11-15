package com.example.mosim.platform.pleos

/** All commands and events run on the main thread. Each start owns a new session. */
interface SpeechRecognizer {
    fun start(onEvent: (SpeechEvent) -> Unit)
    /** Stop input but accept the final result until the session ends. */
    fun stop()
    /** Discard results and release the session; a later start is allowed. */
    fun cancel()
    fun close()
}

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data object Listening : SpeechEvent
    data class Text(val text: String?, val isFinal: Boolean) : SpeechEvent

    /**
     * 마이크로 들어오는 소리 크기(SpeechToText onUpdatedRms).
     * [확인 필요] SDK가 주는 값의 단위와 범위를 문서에서 찾지 못했다. 화면은 상대 크기로만 쓴다.
     */
    data class Level(val rms: Double) : SpeechEvent
    data object Ended : SpeechEvent
    data class Failed(val error: SpeechError) : SpeechEvent
}

enum class SpeechError(val message: String) {
    Permission("음성 인식 권한을 확인해 주세요"),
    Microphone("마이크와 개인정보 보호 설정을 확인해 주세요"),
    Unsupported("이 환경에서는 음성 인식을 사용할 수 없어요"),
    Connection("음성 서비스에 연결하지 못했어요"),
    Timeout("응답 시간이 초과됐어요. 다시 말씀해 주세요"),
    NoSpeech("음성을 인식하지 못했어요. 다시 말씀해 주세요"),
    Unknown("음성 인식 중 오류가 발생했어요"),
}