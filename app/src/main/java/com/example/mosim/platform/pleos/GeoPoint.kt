package com.example.mosim.platform.pleos

/**
 * 좌표다. NaviHelper 3.0.5부터 생성자 순서가 (latitude, longitude)라서 같은 순서로 맞췄다.
 * 카카오 로컬 API는 x = 경도, y = 위도로 주므로 변환할 때 순서에 주의한다.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
