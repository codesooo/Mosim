package com.example.mosim.platform.pleos

import com.example.mosim.data.mock.MockCabinControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors

class PleosCabinControlTest {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val climate = FakeClimateApi()

    @After
    fun tearDown() {
        scope.cancel()
        dispatcher.close()
    }

    private fun createControl() = PleosCabinControl(
        scope = scope,
        climate = climate,
        others = MockCabinControl(scope, requestDelayMillis = 0, applyDelayMillis = 0),
        refreshIntervalMillis = 50,
        powerOnTimeoutMillis = 500,
        pollIntervalMillis = 10,
    )

    private fun CabinControl.awaitTemperature(celsius: Float?) =
        runBlocking { withTimeout(2_000) { state.first { it.targetTemperatureCelsius == celsius } } }

    @Test
    fun `온도만 실제로 처리하고 음량과 등받이는 Mock으로 표시한다`() {
        assertEquals(setOf(CabinFeature.MUSIC_VOLUME, CabinFeature.BACKREST), createControl().simulatedFeatures)
    }

    @Test
    fun `동승석 구역의 목표 온도를 상태로 보여 주고 음량은 Mock 값을 쓴다`() {
        val control = createControl()

        val state = control.awaitTemperature(22f)
        assertEquals(4, state.musicVolume)
    }

    @Test
    fun `온도를 바꾸면 동승석 구역에 요청하고 다시 읽은 값이 상태에 오른다`() {
        val control = createControl()
        control.awaitTemperature(22f)

        val result = runBlocking { control.setTargetTemperature(23.5f) }

        assertEquals(SourceResult.Success(Unit), result)
        assertEquals(listOf(CabinSeat.ROW_1_RIGHT to 23.5f), climate.setRequests)
        control.awaitTemperature(23.5f)
    }

    @Test
    fun `공조가 꺼져 있으면 온도를 모름으로 두고 준비 단계에서 켠다`() {
        climate.powerOn = false
        val control = createControl()
        control.awaitTemperature(null)

        val prepared = runBlocking { control.prepareTemperatureControl() }

        assertEquals(SourceResult.Success(true), prepared)
        assertTrue(climate.powerOn)
        assertEquals(22f, control.state.value.targetTemperatureCelsius)
    }

    @Test
    fun `공조가 이미 켜져 있으면 켜지 않았다고 알린다`() {
        val control = createControl()

        assertEquals(SourceResult.Success(false), runBlocking { control.prepareTemperatureControl() })
        assertEquals(0, climate.turnOnRequests)
    }

    @Test
    fun `공조가 끝내 켜지지 않으면 실패로 돌려준다`() {
        climate.powerOn = false
        climate.ignoresPowerOn = true
        val control = createControl()

        val prepared = runBlocking { control.prepareTemperatureControl() }

        assertFalse(prepared is SourceResult.Success)
    }

    @Test
    fun `범위는 차량이 알려 준 동승석 구역 범위를 쓴다`() {
        val capabilities = (runBlocking { createControl().capabilities() } as SourceResult.Success).value

        assertEquals(15f..30f, capabilities.temperatureRangeCelsius)
        assertEquals(0.5f, capabilities.temperatureStepCelsius)
        assertEquals(0..10, capabilities.musicVolumeRange)
    }

    @Test
    fun `SDK 요청이 실패하면 그 이유를 그대로 돌려준다`() {
        climate.setFailure = SourceResult.Failure(FailureReason.PERMISSION_DENIED)
        val control = createControl()

        val result = runBlocking { control.setTargetTemperature(23f) }

        assertEquals(FailureReason.PERMISSION_DENIED, (result as SourceResult.Failure).reason)
    }

    /** 에뮬레이터 검증 결과를 흉내 낸다: 1열 좌우 구역, 15~30℃, 꺼져 있으면 온도 조회 실패. */
    private class FakeClimateApi : ClimateApi {
        val zones = listOf(
            ClimateZone(setOf(CabinSeat.ROW_1_LEFT), 15f..30f),
            ClimateZone(setOf(CabinSeat.ROW_1_RIGHT), 15f..30f),
        )
        val temperatures = mutableMapOf(CabinSeat.ROW_1_LEFT to 24f, CabinSeat.ROW_1_RIGHT to 22f)
        val setRequests = mutableListOf<Pair<CabinSeat, Float>>()
        var powerOn = true
        var ignoresPowerOn = false
        var turnOnRequests = 0
        var setFailure: SourceResult.Failure? = null

        override suspend fun temperatureZones() = SourceResult.Success(ClimateZones(zones, 0.5f))

        override suspend fun occupiedSeats() = SourceResult.Success(setOf(CabinSeat.ROW_1_LEFT, CabinSeat.ROW_1_RIGHT))

        override suspend fun targetTemperature(zone: ClimateZone): SourceResult<Float?> =
            if (powerOn) SourceResult.Success(temperatures[zone.seats.first()])
            else SourceResult.Failure(FailureReason.NOT_SUPPORTED, "PropertyNotAvailableException")

        override suspend fun setTargetTemperature(zone: ClimateZone, celsius: Float): SourceResult<Unit> {
            setFailure?.let { return it }
            setRequests += zone.seats.first() to celsius
            temperatures[zone.seats.first()] = celsius
            return SourceResult.Success(Unit)
        }

        override suspend fun isPowerOn() = SourceResult.Success<Boolean?>(powerOn)

        override suspend fun turnPowerOn(): SourceResult<Unit> {
            turnOnRequests++
            if (!ignoresPowerOn) powerOn = true
            return SourceResult.Success(Unit)
        }
    }
}
