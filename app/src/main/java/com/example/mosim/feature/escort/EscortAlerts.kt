package com.example.mosim.feature.escort

/**
 * 에스코트 화면 밖에서 운전자에게 알리는 통로다.
 *
 * 주행 중에는 운전자가 대부분 내비 화면을 본다. 그때 E-2 알림과 F-1 제안은 모심 화면 안에만 떠서 놓친다.
 * 그래서 같은 문장을 시스템 알림으로도 올린다. 구현은 [NotificationEscortAlerts]다.
 * ViewModel이 Android 알림 API를 모르게 하려고 인터페이스로 뗐다(단위 테스트에서 가짜로 바꾼다).
 */
interface EscortAlerts {

    /** E-2: 자동 조절 결과나 안내 문장. 잠깐 보였다 사라진다. */
    fun showNotice(message: String)

    /** F-1: 운전자 승인이 필요한 경유 제안. 답하기 전까지 남는다. */
    fun showProposal(message: String)

    /** 제안에 답했거나 에스코트가 끝났을 때 남은 제안 알림을 지운다. */
    fun clearProposal()
}
