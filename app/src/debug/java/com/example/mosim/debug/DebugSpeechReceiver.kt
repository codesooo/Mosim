package com.example.mosim.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mosim.feature.escort.EscortDependencies

/**
 * 디버그 빌드 전용. adb로 받은 문장을 Mock 음성 입력에 넣는다.
 *
 * 앱 안에는 음성을 넣을 곳이 없어(STT는 다른 팀원 담당) E-2·F-1을 손으로 볼 방법이 없었다.
 * 에스코트 화면이 열려 있지 않으면 문장은 버려진다. 실제 STT도 듣는 쪽이 없으면 결과를 받지 못한다.
 */
class DebugSpeechReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Log.w(TAG, "문장이 비어 있어 무시한다. --es $EXTRA_TEXT '<문장>'으로 보낸다.")
            return
        }
        Log.i(TAG, "음성 문장 주입: $text")
        EscortDependencies.mockSpeechInput.say(text)
    }

    companion object {
        const val ACTION_SAY = "com.example.mosim.DEBUG_SAY"
        const val EXTRA_TEXT = "text"
        private const val TAG = "DebugSpeechReceiver"
    }
}
