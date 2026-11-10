package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.SpeechInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Mock이다. 에스코트 쪽 개발·테스트용으로 인식 문장을 직접 넣는다.
 * 흉내 내는 API: SpeechToText ResultListener.onUpdated(stt, completed = true)
 *
 * STT 담당 팀원의 구현이 들어오면 EscortDependencies에서 교체한다.
 * 듣는 쪽이 없을 때 넣은 문장은 버려진다. 실제 STT도 리스너 등록 전 결과는 받지 못한다.
 */
class MockSpeechInput : SpeechInput {

    // 화면 이동 중에 연달아 말해도 놓치지 않도록 버퍼를 조금 둔다.
    private val texts = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override val recognizedTexts: Flow<String> = texts.asSharedFlow()

    fun say(text: String) {
        texts.tryEmit(text)
    }
}
