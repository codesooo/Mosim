package com.example.mosim.feature.escort

import android.content.Context
import android.content.Intent
import ai.pleos.playground.vehicle.Vehicle
import com.example.mosim.BuildConfig
import com.example.mosim.data.mock.MockCabinControl
import com.example.mosim.data.mock.MockEscortTrips
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechInput
import com.example.mosim.data.mock.MockTripSource
import com.example.mosim.data.place.PlaceSearchSource
import com.example.mosim.data.remote.kakao.KakaoPlaceSearchSource
import com.example.mosim.platform.pleos.CabinControl
import com.example.mosim.platform.pleos.PleosCabinControl
import com.example.mosim.platform.pleos.PleosClimateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job

/**
 * EscortViewModel에 넣을 구현을 고르는 한 곳이다. DI 라이브러리 대신 이 object를 쓴다.
 *
 * - SpeechInput: Mock. STT 담당 팀원 구현이 들어오면 바꾼다
 * - TripSource: Mock. NaviHelper 연동 후 바꾼다
 * - PlaceSearchSource: local.properties에 카카오 키가 있으면 카카오, 없으면 Mock
 * - CabinControl: Pleos 차량 서비스가 있는 기기면 온도만 실제 SDK, 음량·등받이는 Mock. 없으면 전부 Mock
 */
object EscortDependencies {

    /** 경로·장소 쪽은 아직 전부 Mock이다. NaviHelper가 들어오면 false로 바꾼다. */
    private const val TRIP_IS_SIMULATED = true

    /** Pleos 차량 서비스가 받는 바인딩 action. Vehicle SDK 매니페스트의 queries와 같다. */
    private const val VEHICLE_SERVICE_ACTION = "ai.pleos.playground.vehicle.VEHICLE_START"

    /**
     * Mock은 반영 지연을 돌릴 범위가 필요하고, ViewModel도 같은 범위를 쓴다.
     * 그래서 범위를 먼저 만들어 둘 다에 넘긴다. ViewModel이 끝날 때 이 범위가 취소된다.
     */
    fun createViewModel(context: Context): EscortViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return EscortViewModel(
            scope = scope,
            trip = MockEscortTrips.MOTHER_TO_HOSPITAL,
            speechInput = MockSpeechInput(),
            cabinControl = createCabinControl(context.applicationContext, scope),
            tripSource = MockTripSource(scope),
            placeSearch = createPlaceSearch(),
            tripIsSimulated = TRIP_IS_SIMULATED,
        )
    }

    /**
     * 일반 에뮬레이터·폰에는 Pleos 차량 서비스가 없다. 그때는 지금처럼 전부 Mock으로 데모를 돌린다.
     * Vehicle은 범위가 끝날 때(에스코트 흐름을 벗어날 때) 해제한다.
     */
    private fun createCabinControl(context: Context, scope: CoroutineScope): CabinControl {
        val mock = MockCabinControl(scope)
        if (!hasVehicleService(context)) return mock

        val vehicle = Vehicle(context)
        vehicle.initialize()
        scope.coroutineContext.job.invokeOnCompletion { vehicle.release() }
        return PleosCabinControl(scope, PleosClimateApi(vehicle), others = mock)
    }

    private fun hasVehicleService(context: Context): Boolean =
        context.packageManager.queryIntentServices(Intent(VEHICLE_SERVICE_ACTION), 0).isNotEmpty()

    /** 키가 없는 팀원도 같은 데모를 돌릴 수 있게 키가 없으면 Mock을 쓴다. */
    private fun createPlaceSearch(): PlaceSearchSource {
        val key = BuildConfig.KAKAO_REST_API_KEY
        return if (key.isBlank()) MockPlaceSearchSource() else KakaoPlaceSearchSource(restApiKey = key)
    }
}
