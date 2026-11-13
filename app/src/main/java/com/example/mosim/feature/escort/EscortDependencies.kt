package com.example.mosim.feature.escort

import android.content.Context
import android.content.Intent
import ai.pleos.playground.navi.helper.NaviHelper
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
import com.example.mosim.platform.pleos.PleosNaviApi
import com.example.mosim.platform.pleos.PleosTripSource
import com.example.mosim.platform.pleos.TripSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job

/**
 * EscortViewModel에 넣을 구현을 고르는 한 곳이다. DI 라이브러리 대신 이 object를 쓴다.
 *
 * - SpeechInput: Mock. STT 담당 팀원 구현이 들어오면 바꾼다. 디버그 빌드에서는 adb로 문장을 넣을 수 있다(DebugSpeechReceiver)
 * - TripSource: Pleos 내비 서비스가 있는 기기면 NaviHelper, 없으면 Mock
 * - PlaceSearchSource: local.properties에 카카오 키가 있으면 카카오, 없으면 Mock
 * - EscortAlerts: 시스템 알림. 모심이 내비 뒤에 있을 때 E-2·F-1을 헤드업으로 알린다
 * - CabinControl: Pleos 차량 서비스가 있는 기기면 온도만 실제 SDK, 음량·등받이는 Mock. 없으면 전부 Mock
 */
object EscortDependencies {

    /**
     * 앱 전체에서 하나만 쓴다. 디버그 빌드의 DebugSpeechReceiver가 지금 화면의 ViewModel과
     * 같은 입력에 문장을 넣어야 해서다. STT 구현이 들어오면 이 자리를 바꾼다.
     */
    val mockSpeechInput = MockSpeechInput()

    /** Pleos 차량 서비스가 받는 바인딩 action. Vehicle SDK 매니페스트의 queries와 같다. */
    private const val VEHICLE_SERVICE_ACTION = "ai.pleos.playground.vehicle.VEHICLE_START"

    /** Pleos 내비 서비스가 받는 바인딩 action. NaviHelper SDK 매니페스트의 queries와 같다. */
    private const val NAVI_SERVICE_ACTION = "ai.pleos.playground.intent.action.NAVI_START"

    /**
     * Mock은 반영 지연을 돌릴 범위가 필요하고, ViewModel도 같은 범위를 쓴다.
     * 그래서 범위를 먼저 만들어 둘 다에 넘긴다. ViewModel이 끝날 때 이 범위가 취소된다.
     */
    fun createViewModel(context: Context): EscortViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val appContext = context.applicationContext
        val isNaviAvailable = hasService(appContext, NAVI_SERVICE_ACTION)
        return EscortViewModel(
            scope = scope,
            trip = MockEscortTrips.MOTHER_TO_HOSPITAL,
            speechInput = mockSpeechInput,
            cabinControl = createCabinControl(appContext, scope),
            tripSource = if (isNaviAvailable) createTripSource(appContext, scope) else MockTripSource(scope),
            placeSearch = createPlaceSearch(),
            alerts = createAlerts(appContext, scope),
            // 경유지 추가 같은 경로 행동의 Mock 표시. 장소 검색은 차량을 조작하지 않아 따지지 않는다.
            tripIsSimulated = !isNaviAvailable,
        )
    }

    /**
     * 일반 에뮬레이터·폰에는 Pleos 차량 서비스가 없다. 그때는 지금처럼 전부 Mock으로 데모를 돌린다.
     * Vehicle은 범위가 끝날 때(에스코트 흐름을 벗어날 때) 해제한다.
     */
    private fun createCabinControl(context: Context, scope: CoroutineScope): CabinControl {
        val mock = MockCabinControl(scope)
        if (!hasService(context, VEHICLE_SERVICE_ACTION)) return mock

        val vehicle = Vehicle(context)
        vehicle.initialize()
        scope.coroutineContext.job.invokeOnCompletion { vehicle.release() }
        return PleosCabinControl(scope, PleosClimateApi(vehicle), others = mock)
    }

    /** 에스코트 흐름을 벗어나면 답하지 않은 제안 알림이 남지 않게 지운다. */
    private fun createAlerts(context: Context, scope: CoroutineScope): EscortAlerts {
        val alerts = NotificationEscortAlerts(context)
        scope.coroutineContext.job.invokeOnCompletion { alerts.clearProposal() }
        return alerts
    }

    /** NaviHelper는 범위가 끝날 때 리스너와 연결을 함께 해제한다. */
    private fun createTripSource(context: Context, scope: CoroutineScope): TripSource {
        val navi = PleosNaviApi(NaviHelper(context))
        navi.connect()
        scope.coroutineContext.job.invokeOnCompletion { navi.release() }
        return PleosTripSource(scope, navi)
    }

    private fun hasService(context: Context, action: String): Boolean =
        context.packageManager.queryIntentServices(Intent(action), 0).isNotEmpty()

    /** 키가 없는 팀원도 같은 데모를 돌릴 수 있게 키가 없으면 Mock을 쓴다. */
    private fun createPlaceSearch(): PlaceSearchSource {
        val key = BuildConfig.KAKAO_REST_API_KEY
        return if (key.isBlank()) MockPlaceSearchSource() else KakaoPlaceSearchSource(restApiKey = key)
    }
}
