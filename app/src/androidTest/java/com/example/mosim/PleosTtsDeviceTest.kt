package com.example.mosim

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mosim.platform.pleos.PleosTtsAdapter
import com.example.mosim.platform.pleos.SpeechOutputEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** 실제 Pleos 서비스 권한이 있는 기기에서만 실행한다. 소리 청취 성공을 판정하는 테스트는 아니다. */
class PleosTtsDeviceTest {
    @Test fun onDeviceKoreanCompletes() = verify(stopOnStart = false)

    @Test fun stopAfterStartReleasesPlayback() = verify(stopOnStart = true)

    private fun verify(stopOnStart: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(
            context.checkSelfPermission("pleos.car.permission.TTS_SERVICE") == PackageManager.PERMISSION_GRANTED,
        )
        val terminal = CountDownLatch(1)
        val started = CountDownLatch(1)
        val result = AtomicReference<SpeechOutputEvent>()
        lateinit var speech: PleosTtsAdapter
        instrumentation.runOnMainSync {
            speech = PleosTtsAdapter(context)
            // Mock 문장: 사용자 발화/개인정보를 테스트 입력이나 로그에 기록하지 않는다.
            speech.speak("음성 안내 테스트입니다. 잠시 쉬어가는 건 어떨까요?") { event ->
                if (event == SpeechOutputEvent.Started) {
                    started.countDown()
                    if (stopOnStart) speech.stop()
                } else if (event == SpeechOutputEvent.Done || event == SpeechOutputEvent.Stopped ||
                    event is SpeechOutputEvent.Failed) {
                    result.set(event)
                    terminal.countDown()
                }
            }
        }
        try {
            assertTrue("TTS terminal callback timeout", terminal.await(30, TimeUnit.SECONDS))
            assertEquals(0L, started.count)
            assertEquals(
                if (stopOnStart) SpeechOutputEvent.Stopped else SpeechOutputEvent.Done,
                result.get(),
            )
        } finally {
            instrumentation.runOnMainSync { speech.close() }
        }
    }
}
