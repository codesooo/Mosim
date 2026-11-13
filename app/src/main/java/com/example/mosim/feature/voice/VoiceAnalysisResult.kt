package com.example.mosim.feature.voice

/** 외부 의도 분석 담당자가 돌려주는 UI 결과. STT 엔진은 이 타입을 사용하지 않는다. */
sealed interface VoiceAnalysisResult {
    data class SearchPlaces(val query: String) : VoiceAnalysisResult
    data class Message(val text: String) : VoiceAnalysisResult
}
