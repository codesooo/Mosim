package com.example.mosim.data.mock

import com.example.mosim.platform.pleos.FailureReason
import com.example.mosim.platform.pleos.SourceResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCabinControlTest {

    @Test
    fun `요청 성공 직후에는 값이 그대로이고 나중에 상태로 반영된다`() = runBlocking {
        val control = MockCabinControl(scope = this, requestDelayMillis = 0, applyDelayMillis = 50)

        val result = control.setTargetTemperature(23f)

        assertEquals(SourceResult.Success(Unit), result)
        assertEquals(24f, control.state.value.targetTemperatureCelsius)
        val applied = withTimeout(1_000) { control.state.first { it.targetTemperatureCelsius == 23f } }
        assertEquals(23f, applied.targetTemperatureCelsius)
    }

    @Test
    fun `온도는 16에서 30도 사이 0점5도 단위만 받는다`() = runBlocking {
        val control = MockCabinControl(scope = this, requestDelayMillis = 0, applyDelayMillis = 0)

        assertInvalid(control.setTargetTemperature(31f))
        assertInvalid(control.setTargetTemperature(15.5f))
        assertInvalid(control.setTargetTemperature(23.3f))
        assertEquals(SourceResult.Success(Unit), control.setTargetTemperature(22.5f))
    }

    @Test
    fun `음량과 등받이도 범위 밖이면 거절한다`() = runBlocking {
        val control = MockCabinControl(scope = this, requestDelayMillis = 0, applyDelayMillis = 0)

        assertInvalid(control.setMusicVolume(11))
        assertInvalid(control.setPassengerBackrestAngle(150))
        assertEquals(SourceResult.Success(Unit), control.setMusicVolume(3))
    }

    @Test
    fun `실패 사유를 주면 모든 요청이 그 사유로 실패하고 값은 바뀌지 않는다`() = runBlocking {
        val control = MockCabinControl(
            scope = this,
            failure = FailureReason.DRIVING_RESTRICTED,
            requestDelayMillis = 0,
            applyDelayMillis = 0,
        )

        assertEquals(SourceResult.Failure(FailureReason.DRIVING_RESTRICTED), control.capabilities())
        assertEquals(SourceResult.Failure(FailureReason.DRIVING_RESTRICTED), control.setMusicVolume(3))
        assertEquals(MockCabinControl.DEFAULT_STATE, control.state.value)
    }

    @Test
    fun `지원하지 않는 기능은 NOT_SUPPORTED로 실패한다`() = runBlocking {
        val control = MockCabinControl(
            scope = this,
            capabilities = MockCabinControl.DEFAULT_CAPABILITIES.copy(backrestAngleRange = null),
            requestDelayMillis = 0,
            applyDelayMillis = 0,
        )

        val result = control.setPassengerBackrestAngle(115)

        assertTrue(result is SourceResult.Failure && result.reason == FailureReason.NOT_SUPPORTED)
    }

    private fun assertInvalid(result: SourceResult<Unit>) {
        assertTrue("$result", result is SourceResult.Failure && result.reason == FailureReason.INVALID_REQUEST)
    }
}
