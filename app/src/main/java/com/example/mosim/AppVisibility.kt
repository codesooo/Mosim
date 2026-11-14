package com.example.mosim

/**
 * 모심 화면이 지금 보이는지. MainActivity가 onStart/onStop에서 바꾼다.
 *
 * 화면이 보일 때는 화면 안 토스트로, 내비 뒤에 있을 때는 시스템 알림으로 알리려고 둔다(NotificationEscortAlerts).
 * 화면이 하나(MainActivity)뿐이라 값 하나로 충분하다.
 */
object AppVisibility {
    @Volatile
    var isVisible: Boolean = false
}
