package com.example.mosim.feature.escort

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.mosim.AppVisibility
import com.example.mosim.MainActivity
import com.example.mosim.R

/**
 * 모심이 화면 뒤에 있을 때 E-2 알림과 F-1 제안을 헤드업 알림으로 올린다.
 *
 * Pleos 에뮬레이터에서 확인한 조건(2026-11-13, escort.md "내비 화면 위 알림"):
 * - 채널 중요도가 HIGH여야 한다.
 * - 알림 종류(category)가 없으면 헤드업으로 뜨지 않는다. CATEGORY_MESSAGE일 때 내비 위에 떴다.
 *   [확인 필요] 조절 결과·경유 제안을 메시지 종류로 올려도 심사에 문제가 없는지, 실차에서도 같은지.
 *
 * 알림 권한이 없으면 시스템이 알림을 버린다. 그때는 화면 안 토스트와 F-1 화면만 남는다.
 */
class NotificationEscortAlerts(
    private val context: Context,
    private val isAppVisible: () -> Boolean = { AppVisibility.isVisible },
) : EscortAlerts {

    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH),
        )
    }

    override fun showNotice(message: String) {
        // 모심 화면이 보이면 화면 안 토스트가 같은 문장을 보여 준다. 두 번 알리지 않는다.
        if (isAppVisible()) return
        val notification = baseBuilder(message)
            .setTimeoutAfter(NOTICE_TIMEOUT_MILLIS)
            .build()
        manager.notify(NOTICE_ID, notification)
    }

    override fun showProposal(message: String) {
        if (isAppVisible()) return
        // 경로 변경은 운전자 승인이 필요하다. 알림을 누르면 모심이 앞으로 오고 F-1에서 답한다.
        manager.notify(PROPOSAL_ID, baseBuilder(message).build())
    }

    override fun clearProposal() {
        manager.cancel(PROPOSAL_ID)
    }

    private fun baseBuilder(message: String): Notification.Builder =
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)

    /**
     * 실행 중인 모심 화면을 그대로 앞으로 가져온다.
     * 플래그 없이 열면 MainActivity가 하나 더 만들어져 임시 메인이 뜨고 진행 중인 에스코트가 가려진다
     * (에뮬레이터에서 확인). SINGLE_TOP + CLEAR_TOP이면 이미 있는 화면을 다시 쓴다.
     */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val CHANNEL_ID = "escort_alerts"
        const val CHANNEL_NAME = "에스코트 알림"
        const val NOTICE_ID = 1
        const val PROPOSAL_ID = 2

        /** 화면 안 토스트(4초)보다 조금 길게 둔다. 헤드업이 내려간 뒤 알림 목록에 쌓이지 않게 지운다. */
        const val NOTICE_TIMEOUT_MILLIS = 8_000L
    }
}
