package com.example.mosim.platform.pleos

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.MoveControl
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 🔜 Coming Soon인 Vehicle Control을 Pleos Connect 에뮬레이터에서 실제로 불러 보는 검증 테스트다.
 * sdk-status.md "에뮬레이터 검증" 칸을 갱신할 근거로 쓴다. 결과는 logcat 태그 PleosVerify로도 남긴다.
 *
 * Pleos 차량 서비스가 없는 기기(일반 에뮬레이터)에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class PleosVehicleControlVerificationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var vehicle: Vehicle

    @Before
    fun setUp() {
        val serviceIntent = Intent("ai.pleos.playground.vehicle.VEHICLE_START")
        val hasVehicleService = context.packageManager.queryIntentServices(serviceIntent, 0).isNotEmpty()
        assumeTrue("Pleos 차량 서비스가 없는 기기", hasVehicleService)
        vehicle = Vehicle(context)
        vehicle.initialize()
        // 서비스 바인딩이 비동기라 첫 호출 전에 잠시 기다린다. [확인 필요] 바인딩 완료 콜백 유무
        Thread.sleep(BIND_WAIT_MILLIS)
    }

    @After
    fun tearDown() {
        if (::vehicle.isInitialized) vehicle.release()
    }

    @Test
    fun 공조_목표_온도를_바꾸면_다시_조회한_값이_바뀐다() {
        val hvac = vehicle.getHVAC()

        // 공조는 좌석 area 묶음이 차량마다 달라서, 지원 여부 결과에 나온 area를 그대로 쓴다.
        val bounds = await<Map<SeatArea, *>>("HVAC capability") { done, fail ->
            hvac.checkHvacTemperatureCapabilities({ config, areaBounds ->
                report("checkHvacTemperatureCapabilities", "${config.accessType} ${areaBounds.describe()}")
                done(areaBounds)
            }, fail)
        }
        val area = bounds.keys.first()

        val powerAreas = await<List<SeatArea>>("HVAC power capability") { done, fail ->
            hvac.checkHvacPowerCapabilities({ config, areas ->
                report("checkHvacPowerCapabilities", "${config.accessType} ${areas.map { it.describe() }}")
                done(areas)
            }, fail)
        }
        val powerArea = powerAreas.firstOrNull() ?: area
        val powerBefore = await<Boolean?>("getHvacPowerState") { done, fail ->
            hvac.getHvacPowerState(powerArea, { done(it) }, fail)
        }
        report("getHvacPowerState(${powerArea.describe()})", powerBefore)
        if (powerBefore != true) {
            // 공조가 꺼져 있으면 온도 조회가 PropertyNotAvailableException으로 실패한다(sdk-status.md 검증 기록). 켜고 기다린다.
            val turnedOn = await<Boolean?>("setHvacPower") { done, fail ->
                hvac.setHvacPower(powerArea, true, { _, value -> done(value) }, fail)
            }
            report("setHvacPower(true)", turnedOn)
            // 요청 응답은 요청값이라, 실제로 켜졌는지 다시 읽어 본다.
            repeat(POWER_POLL_COUNT) { attempt ->
                Thread.sleep(APPLY_WAIT_MILLIS)
                val powerNow = await<Boolean?>("getHvacPowerState") { done, fail ->
                    hvac.getHvacPowerState(powerArea, { done(it) }, fail)
                }
                report("getHvacPowerState(after on, ${attempt + 1})", powerNow)
            }
        }
        val before = await<Float?>("getHvacTemperature") { done, fail ->
            hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, { done(it) }, fail)
        }
        report("getHvacTemperature(${area.describe()}, before)", before)
        assertNotNull(before)

        val target = if (before!! >= 24f) before - 1f else before + 1f
        val requested = await<Float?>("setHvacTemperature") { done, fail ->
            hvac.setHvacTemperature(area, target, TemperatureUnit.CELSIUS, { _, value -> done(value) }, fail)
        }
        report("setHvacTemperature($target)", requested)

        Thread.sleep(APPLY_WAIT_MILLIS)
        val after = await<Float?>("getHvacTemperature") { done, fail ->
            hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, { done(it) }, fail)
        }
        report("getHvacTemperature(after)", after)
        assertEquals(target, after)

        // 다음 검증에 영향을 주지 않도록 원래 상태로 돌려 둔다.
        await<Float?>("restore temperature") { done, fail ->
            hvac.setHvacTemperature(area, before, TemperatureUnit.CELSIUS, { _, v -> done(v) }, fail)
        }
        if (powerBefore == false) {
            await<Boolean?>("restore power") { done, fail -> hvac.setHvacPower(powerArea, false, { _, v -> done(v) }, fail) }
        }
    }

    @Test
    fun 동승석_등받이를_움직였다가_멈추고_위치를_읽는다() {
        val seat = vehicle.getSeat()
        val area = SeatArea(SeatValue.ROW_1_RIGHT)

        val moveCapability = await("move capability") { done, fail ->
            seat.checkMoveSeatBackrestAngleCapabilities({ config, areas ->
                done("${config.accessType} ${areas.map { it.describe() }}")
            }, fail)
        }
        report("checkMoveSeatBackrestAngleCapabilities", moveCapability)

        val positionCapability = await("position capability") { done, fail ->
            seat.checkSeatBackrestAnglePositionCapabilities({ config, bounds ->
                done("${config.accessType} ${bounds.describe()}")
            }, fail)
        }
        report("checkSeatBackrestAnglePositionCapabilities", positionCapability)

        val before = await("position before") { done, fail -> seat.getCurrentSeatBackrestAnglePosition(area, { done(it) }, fail) }
        report("getCurrentSeatBackrestAnglePosition(before)", before)

        val moving = await("move") { done, fail -> seat.moveSeatBackrestAngle(area, MoveControl.FORWARD, { _, v -> done(v) }, fail) }
        // 첫 실행에서 위치가 최솟값 0이라 BACKWARD로는 움직일 수 없었다. FORWARD로 확인한다.
        report("moveSeatBackrestAngle(FORWARD)", moving)
        Thread.sleep(SEAT_MOVE_MILLIS)
        val stopped = await("stop") { done, fail -> seat.moveSeatBackrestAngle(area, MoveControl.STOP, { _, v -> done(v) }, fail) }
        report("moveSeatBackrestAngle(STOP)", stopped)

        Thread.sleep(APPLY_WAIT_MILLIS)
        val after = await("position after") { done, fail -> seat.getCurrentSeatBackrestAnglePosition(area, { done(it) }, fail) }
        report("getCurrentSeatBackrestAnglePosition(after)", after)
        // 위치가 바뀌는지는 Virtual Console 지원 여부에 달려 있어 단정하지 않고 기록만 한다. [확인 필요]
    }

    /** 콜백 API를 기다린다. 실패 콜백이 오면 그 예외로 테스트를 실패시킨다. */
    private fun <T> await(
        name: String,
        call: (done: (T) -> Unit, fail: (Exception) -> Unit) -> Unit,
    ): T {
        val latch = CountDownLatch(1)
        var value: Result<T>? = null
        call({ value = Result.success(it); latch.countDown() }, { value = Result.failure(it); latch.countDown() })
        check(latch.await(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "$name 응답 없음(${CALL_TIMEOUT_SECONDS}초)" }
        return value!!.onFailure { report("$name 실패", "${it.javaClass.simpleName}: ${it.message}") }.getOrThrow()
    }

    private fun SeatArea.describe(): String = values.joinToString("+")

    private fun Map<SeatArea, *>.describe(): String = entries.joinToString { (area, bound) -> "${area.describe()}=$bound" }

    private fun report(step: String, value: Any?) {
        Log.i(LOG_TAG, "$step → $value")
    }

    private companion object {
        const val LOG_TAG = "PleosVerify"
        const val BIND_WAIT_MILLIS = 2_000L
        const val APPLY_WAIT_MILLIS = 1_000L
        const val SEAT_MOVE_MILLIS = 3_000L
        const val CALL_TIMEOUT_SECONDS = 10L
        const val POWER_POLL_COUNT = 3
    }
}
