package com.example.mosim.platform.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Vehicle SDK 공조·착석 API를 suspend 함수로 바꾼다. SDK 타입은 이 파일 밖으로 나가지 않는다.
 * 시그니처 근거: docs/reference/pleos/08 1·4절, Vehicle 2.3.2 라이브러리(javap로 확인).
 *
 * SDK는 콜백으로 답하고, 서비스 연결 전에는 답이 늦거나 실패할 수 있다.
 * 그래서 모든 호출에 시간 제한을 두고 넘으면 CONNECTION_FAILED로 끝낸다(TripSource 주석과 같은 규칙).
 */
class PleosClimateApi(
    vehicle: Vehicle,
    private val timeoutMillis: Long = 5_000,
) : ClimateApi {

    private val hvac = vehicle.getHVAC()
    private val seat = vehicle.getSeat()

    /** setHvacPower / getHvacPowerState에 쓸 area. 지원 여부 조회 결과에서 한 번 받아 둔다. */
    private var powerArea: SeatArea? = null

    override suspend fun temperatureZones(): SourceResult<ClimateZones> = call { done, fail ->
        hvac.checkHvacTemperatureCapabilities({ config, bounds, configArray ->
            val zones = bounds.map { (area, bound) ->
                ClimateZone(area.toCabinSeats(), bound.minValue..bound.maxValue)
            }
            // 쓰기 권한이 없으면 구역이 없는 것과 같다.
            val writable = config.accessType == AccessType.WRITE || config.accessType == AccessType.READ_WRITE
            done(ClimateZones(if (writable) zones else emptyList(), configArray?.incrementCelsius ?: DEFAULT_STEP_CELSIUS))
        }, fail)
    }

    override suspend fun occupiedSeats(): SourceResult<Set<CabinSeat>> {
        val areas = when (val result = call<List<SeatArea>> { done, fail ->
            seat.checkSeatOccupancyCapabilities({ _, areas -> done(areas) }, fail)
        }) {
            is SourceResult.Success -> result.value
            is SourceResult.Failure -> return result
        }
        val occupied = mutableSetOf<CabinSeat>()
        for (area in areas) {
            when (val result = call<Boolean?> { done, fail -> seat.getCurrentSeatOccupancy(area, done, fail) }) {
                is SourceResult.Success -> if (result.value == true) occupied += area.toCabinSeats()
                is SourceResult.Failure -> return result
            }
        }
        return SourceResult.Success(occupied)
    }

    override suspend fun targetTemperature(zone: ClimateZone): SourceResult<Float?> = call { done, fail ->
        hvac.getHvacTemperature(zone.toSeatArea(), TemperatureUnit.CELSIUS, done, fail)
    }

    override suspend fun setTargetTemperature(zone: ClimateZone, celsius: Float): SourceResult<Unit> =
        call { done, fail ->
            hvac.setHvacTemperature(zone.toSeatArea(), celsius, TemperatureUnit.CELSIUS, { _, _ -> done(Unit) }, fail)
        }

    override suspend fun isPowerOn(): SourceResult<Boolean?> {
        val area = powerArea() ?: return SourceResult.Failure(FailureReason.NOT_SUPPORTED, "공조 전원 area 없음")
        return call { done, fail -> hvac.getHvacPowerState(area, done, fail) }
    }

    override suspend fun turnPowerOn(): SourceResult<Unit> {
        val area = powerArea() ?: return SourceResult.Failure(FailureReason.NOT_SUPPORTED, "공조 전원 area 없음")
        return call { done, fail -> hvac.setHvacPower(area, true, { _, _ -> done(Unit) }, fail) }
    }

    private suspend fun powerArea(): SeatArea? {
        powerArea?.let { return it }
        val result = call<List<SeatArea>> { done, fail -> hvac.checkHvacPowerCapabilities({ _, areas -> done(areas) }, fail) }
        return (result as? SourceResult.Success)?.value?.firstOrNull()?.also { powerArea = it }
    }

    /** 콜백 API 하나를 기다린다. 시간 안에 답이 없으면 CONNECTION_FAILED. */
    private suspend fun <T> call(request: (done: (T) -> Unit, fail: (Exception) -> Unit) -> Unit): SourceResult<T> =
        withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                // SDK가 콜백을 두 번 부르거나 취소 뒤에 불러도 한 번만 이어 가게 막는다.
                fun finish(result: SourceResult<T>) {
                    if (continuation.isActive) continuation.resume(result)
                }
                try {
                    request({ finish(SourceResult.Success(it)) }, { finish(it.toFailure()) })
                } catch (exception: Exception) {
                    // 권한 누락 같은 오류는 콜백이 아니라 호출 자리에서 던져진다.
                    finish(exception.toFailure())
                }
            }
        } ?: SourceResult.Failure(FailureReason.CONNECTION_FAILED, "Vehicle 응답 없음(${timeoutMillis}ms)")

    private fun SeatArea.toCabinSeats(): Set<CabinSeat> =
        values.mapNotNull { value -> CabinSeat.entries.firstOrNull { it.name == value.name } }.toSet()

    private fun ClimateZone.toSeatArea(): SeatArea =
        SeatArea(*seats.map { SeatValue.valueOf(it.name) }.toTypedArray())

    private companion object {
        /** HvacTemperatureConfigArray가 없을 때. Virtual Console 기준 0.5℃ 단위다. */
        const val DEFAULT_STEP_CELSIUS = 0.5f
    }
}

/**
 * SDK 예외를 앱의 실패 이유로 바꾼다.
 * android.car 예외 클래스는 compileSdk에 없어 이름으로 구분한다.
 */
internal fun Exception.toFailure(): SourceResult.Failure {
    val reason = when {
        this is SecurityException -> FailureReason.PERMISSION_DENIED
        this is IllegalArgumentException -> FailureReason.INVALID_REQUEST
        javaClass.simpleName == "PropertyNotAvailableException" -> FailureReason.NOT_SUPPORTED
        this is IllegalStateException -> FailureReason.CONNECTION_FAILED
        else -> FailureReason.UNKNOWN
    }
    return SourceResult.Failure(reason, "${javaClass.simpleName}: $message")
}
