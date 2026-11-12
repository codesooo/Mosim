package com.example.mosim.platform.pleos

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.vehicle.Vehicle
import com.example.mosim.data.mock.MockCabinControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱이 쓰는 PleosCabinControl + PleosClimateApi를 Pleos Connect 에뮬레이터에서 그대로 돌려 본다.
 * Pleos 차량 서비스가 없는 기기에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class PleosCabinControlDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var vehicle: Vehicle

    @Before
    fun setUp() {
        val hasVehicleService = context.packageManager
            .queryIntentServices(Intent("ai.pleos.playground.vehicle.VEHICLE_START"), 0).isNotEmpty()
        assumeTrue("Pleos 차량 서비스가 없는 기기", hasVehicleService)
        vehicle = Vehicle(context)
        vehicle.initialize()
    }

    @After
    fun tearDown() {
        scope.cancel()
        if (::vehicle.isInitialized) vehicle.release()
    }

    @Test
    fun 공조를_준비하고_동승석_목표_온도를_바꾸면_상태에_반영된다(): Unit = runBlocking {
        // 에뮬레이터에서 공조가 켜지기까지 8초를 넘긴 적이 있어 여기서는 넉넉히 기다리고 걸린 시간을 남긴다.
        val control = PleosCabinControl(
            scope,
            PleosClimateApi(vehicle),
            others = MockCabinControl(scope),
            powerOnTimeoutMillis = POWER_ON_TIMEOUT_MILLIS,
        )
        val startedAt = System.currentTimeMillis()
        // 서비스 연결과 첫 주기 조회를 기다리지 않아도 되는지 보려고 바로 부른다. 실패하면 한 번 더 기다린다.
        val prepared = control.prepareTemperatureControl().takeIf { it is SourceResult.Success }
            ?: run { Thread.sleep(2_000); control.prepareTemperatureControl() }
        Log.i(LOG_TAG, "prepareTemperatureControl → $prepared (${System.currentTimeMillis() - startedAt}ms)")
        assertTrue(prepared is SourceResult.Success)

        val before = control.state.value.targetTemperatureCelsius
        Log.i(LOG_TAG, "before → $before")
        val target = if ((before ?: 22f) >= 24f) 21.5f else 26f

        val result = control.setTargetTemperature(target)
        Log.i(LOG_TAG, "setTargetTemperature($target) → $result")
        assertEquals(SourceResult.Success(Unit), result)

        val after = withTimeout(5_000) { control.state.first { it.targetTemperatureCelsius == target } }
        Log.i(LOG_TAG, "after → ${after.targetTemperatureCelsius}, simulated=${control.simulatedFeatures}")
    }

    private companion object {
        const val LOG_TAG = "PleosVerify"
        const val POWER_ON_TIMEOUT_MILLIS = 30_000L
    }
}
