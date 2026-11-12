package com.example.mosim.feature.escort

import com.example.mosim.BuildConfig
import com.example.mosim.data.mock.MockCabinControl
import com.example.mosim.data.mock.MockEscortTrips
import com.example.mosim.data.mock.MockPlaceSearchSource
import com.example.mosim.data.mock.MockSpeechInput
import com.example.mosim.data.mock.MockTripSource
import com.example.mosim.data.place.PlaceSearchSource
import com.example.mosim.data.remote.kakao.KakaoPlaceSearchSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * EscortViewModel에 넣을 구현을 고르는 한 곳이다. DI 라이브러리 대신 이 object를 쓴다.
 *
 * 지금은 전부 Mock이다. 실제 구현으로 바꿀 때 여기만 고친다.
 * - SpeechInput: STT 담당 팀원 구현
 * - TripSource: NaviHelper 연동
 * - PlaceSearchSource: 카카오 로컬 REST. local.properties에 키가 있으면 카카오, 없으면 Mock
 * - CabinControl: Vehicle Control 제공 후
 */
object EscortDependencies {

    /** 실제 구현이 하나라도 들어오면 소스별로 나눠 판단하도록 바꾼다. */
    private const val SOURCES_ARE_SIMULATED = true

    /**
     * Mock은 반영 지연을 돌릴 범위가 필요하고, ViewModel도 같은 범위를 쓴다.
     * 그래서 범위를 먼저 만들어 둘 다에 넘긴다. ViewModel이 끝날 때 이 범위가 취소된다.
     */
    fun createViewModel(): EscortViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return EscortViewModel(
            scope = scope,
            trip = MockEscortTrips.MOTHER_TO_HOSPITAL,
            speechInput = MockSpeechInput(),
            cabinControl = MockCabinControl(scope),
            tripSource = MockTripSource(scope),
            placeSearch = createPlaceSearch(),
            sourcesAreSimulated = SOURCES_ARE_SIMULATED,
        )
    }

    /**
     * 키가 없는 팀원도 같은 데모를 돌릴 수 있게 키가 없으면 Mock을 쓴다.
     * 장소 검색은 차량을 조작하지 않아 Mock 실행 표시(SOURCES_ARE_SIMULATED)와 상관이 없다.
     */
    private fun createPlaceSearch(): PlaceSearchSource {
        val key = BuildConfig.KAKAO_REST_API_KEY
        return if (key.isBlank()) MockPlaceSearchSource() else KakaoPlaceSearchSource(restApiKey = key)
    }
}
