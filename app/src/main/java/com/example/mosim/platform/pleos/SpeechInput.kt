package com.example.mosim.platform.pleos

import kotlinx.coroutines.flow.Flow

/**
 * STT 경계다. 실제 구현(Pleos SpeechToText 🔜)은 다른 팀원이 맡는다.
 *
 * 문장만 넘기고 명령 해석은 에스코트 쪽(VoiceCommandParser)이 한다. 근거는 docs/design/escort.md 3장.
 * [확인 필요] 듣기 시작 시점과 인식 실패 전달 방식은 STT 담당과 합의 전이다(11장 #1).
 * 지금은 "실패는 STT 쪽이 처리하고, 여기로는 성공한 문장만 온다"고 가정한다.
 */
interface SpeechInput {
    /** 최종 인식 문장(SpeechToText onUpdated의 completed = true)만 내보낸다. */
    val recognizedTexts: Flow<String>
}
