package com.example.mosim.data.mock

import com.example.mosim.data.passenger.EscortTrip
import com.example.mosim.data.passenger.PassengerProfile
import com.example.mosim.platform.pleos.GeoPoint
import java.time.LocalTime

/**
 * Mock이다. 동승자 선택(A-1)과 탑승 감지(D-1)가 아직 없어 에스코트 시작 정보를 고정값으로 둔다.
 * 값은 피그마 E-1 예시("08:29 엄마 탑승", "서울대병원 본관")를 따른다.
 */
object MockEscortTrips {

    val MOTHER = PassengerProfile(
        name = "엄마",
        autoTemperatureRangeCelsius = 22f..25f,
        prefersRestOnLongTrip = true,
        restMusicVolume = 3,
    )

    val MOTHER_TO_HOSPITAL = EscortTrip(
        passenger = MOTHER,
        destinationName = "서울대병원 본관",
        boardedAt = LocalTime.of(8, 29),
        // 카카오 로컬 키워드 검색 "서울대학교병원 본관" 결과의 본관 1층 좌표 (2026-11-12 조회)
        destinationLocation = GeoPoint(latitude = 37.5795590, longitude = 126.9990026),
    )
}
