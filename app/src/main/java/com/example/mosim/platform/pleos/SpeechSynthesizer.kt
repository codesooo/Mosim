package com.example.mosim.platform.pleos

/**
 * 음성 안내 경계. F-1의 AI 제안을 읽어 주는 데 쓴다.
 *
 * [SpeechRecognizer]와 같은 모양으로 맞췄다. 명령과 이벤트는 모두 메인 스레드에서 오간다.
 * 한 번에 한 문장만 읽는다. 읽는 중에 [speak]를 다시 부르면 앞의 안내를 끊고 새 문장으로 바꾼다.
 */
interface SpeechSynthesizer {
    fun speak(text: String, onEvent: (SpeechOutputEvent) -> Unit)

    /** 읽는 중이면 멈춘다. 멈추면 [SpeechOutputEvent.Stopped]가 온다. */
    fun stop()

    /** 더 쓰지 않는다. 자원을 돌려준다. */
    fun close()
}

sealed interface SpeechOutputEvent {
    /** 실제로 소리가 나기 시작했다. */
    data object Started : SpeechOutputEvent

    /** 끝까지 읽었다. */
    data object Done : SpeechOutputEvent

    /** [SpeechSynthesizer.stop] 또는 새 문장으로 교체돼 중간에 끊겼다. */
    data object Stopped : SpeechOutputEvent

    data class Failed(val error: SpeechOutputError) : SpeechOutputEvent
}

/**
 * 안내 실패 사유. 화면에는 글로 띄우지 않는다.
 * 음성은 보조 수단이라, 실패해도 화면과 터치는 그대로 쓸 수 있어야 한다.
 */
enum class SpeechOutputError {
    Permission,
    Unsupported,
    Connection,
    Unknown,
}
