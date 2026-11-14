package com.example.mosim.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mosim.feature.escort.EscortDependencies

/**
 * 디버그 빌드 전용. Mock 경로 소스에 "도착"과 "정체"를 손으로 넣는다.
 *
 * 왜 필요한가: 도착(G-1 진입)과 지연 감지(F-1 제안)는 실제로는 내비가 알려 주는 사건이다.
 * 에뮬레이터에서는 차가 움직이지 않아 기다려도 오지 않는다. 시연과 손 테스트를 위해 둔다.
 *
 * 실제 NaviHelper를 쓰는 기기에서는 Mock 경로 소스가 없어 아무 일도 하지 않는다.
 * 즉 **이 신호로 실제 차량의 도착을 흉내 내지 않는다.**
 */
class DebugTripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MOCK_TRIP) {
            val on = intent.getBooleanExtra(EXTRA_ON, true)
            EscortDependencies.forceMockTrip = on
            Log.i(TAG, "Mock 경로 강제: $on (에스코트 화면에 다시 들어가야 적용된다)")
            return
        }
        val mock = EscortDependencies.mockTripSource
        if (mock == null) {
            Log.w(TAG, "Mock 경로 소스가 없다. 에스코트 화면에 들어간 뒤, Mock 경로일 때만 동작한다.")
            return
        }
        when (intent.action) {
            ACTION_ARRIVE -> {
                Log.i(TAG, "목적지 도착 신호 주입")
                mock.simulateArrival()
            }
            ACTION_DELAY -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, DEFAULT_DELAY_MINUTES).toLong()
                Log.i(TAG, "정체 ${minutes}분 주입")
                mock.simulateTrafficDelay(minutes)
            }
            else -> Log.w(TAG, "모르는 action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_ARRIVE = "com.example.mosim.DEBUG_ARRIVE"
        const val ACTION_MOCK_TRIP = "com.example.mosim.DEBUG_MOCK_TRIP"
        const val EXTRA_ON = "on"
        const val ACTION_DELAY = "com.example.mosim.DEBUG_DELAY"
        const val EXTRA_MINUTES = "minutes"

        /** EscortViewModel의 지연 기준(15분)을 넘겨 제안이 뜨게 하는 기본값이다. */
        private const val DEFAULT_DELAY_MINUTES = 30
        private const val TAG = "DebugTripReceiver"
    }
}
