package com.example.mosim.platform.pleos

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NaviHelper(✅)를 Pleos Connect 에뮬레이터에서 실제로 불러 보는 검증 테스트다.
 * 목적: 문서에 없는 값의 단위(eta, remainTime)와 실제로 오는 콜백을 확인한다. 결과는 logcat 태그 PleosVerify.
 * Pleos 내비 서비스가 없는 기기에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class PleosNaviHelperVerificationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var naviHelper: NaviHelper

    /** 콜백 이름별로 처음 온 시각을 남겨, 기다릴 콜백이 왔는지 본다. */
    private val events = java.util.concurrent.ConcurrentHashMap<String, CountDownLatch>()

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) = event("onRouteStarted", routeStartInfo)
        override fun onRoutingCompleted() = event("onRoutingCompleted", "")
        override fun onRouteCancelled() = event("onRouteCancelled", "")
        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) =
            event("onWaypointChanged", waypointChangedInfo)
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) =
            event("onDestinationArrived", destinationArrivedInfo)
        override fun onDestinationChanged() = event("onDestinationChanged", "")
        override fun onError(errorCode: NaviErrorCode) = event("onError", errorCode)
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) =
            event("onCurrentLocationInfo", currentLocationInfo)
        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) = event("onRouteStateInfo", routeStateInfo)
        override fun onDestinationInfo(destinationInfo: DestinationInfo) = event("onDestinationInfo", destinationInfo)
        override fun onWayPointInfo(waypointInfo: List<WaypointInfo>) = event("onWayPointInfo", waypointInfo)
        override fun onCurrentRoadSpeedLimit(speedLimit: Int) = event("onCurrentRoadSpeedLimit", speedLimit)
        override fun onNavigationStatus(navigationStatus: NavigationStatus) = event("onNavigationStatus", navigationStatus)
        override fun onTBTInfo(tbtInfo: List<TBTInfo>) = event("onTBTInfo", tbtInfo)
        override fun onTripInfo(tripInfo: TripInfo) = event("onTripInfo", tripInfo)
    }

    @Before
    fun setUp() {
        val hasNaviService = context.packageManager
            .queryIntentServices(Intent("ai.pleos.playground.intent.action.NAVI_START"), 0).isNotEmpty()
        assumeTrue("Pleos 내비 서비스가 없는 기기", hasNaviService)

        naviHelper = NaviHelper(context)
        val connected = CountDownLatch(1)
        naviHelper.initialize(
            onConnected = { report("initialize", "connected"); connected.countDown() },
            onFailed = { report("initialize 실패", "${it.javaClass.simpleName}: ${it.message}"); connected.countDown() },
        )
        assertTrue("NaviHelper 연결 응답 없음", connected.await(15, TimeUnit.SECONDS))
        naviHelper.addListener(listener)
    }

    @After
    fun tearDown() {
        if (::naviHelper.isInitialized) {
            naviHelper.removeListener(listener)
            naviHelper.release()
        }
    }

    @Test
    fun 목적지를_설정하고_ETA_위치_경유지_콜백을_확인한다() {
        // 경로가 없을 때 무엇이 오는지 먼저 본다.
        naviHelper.getRouteStateInfo()
        naviHelper.getCurrentLocationInfo()
        await("onCurrentLocationInfo", 5)
        report("now(epoch s)", System.currentTimeMillis() / 1000)

        // 서울대병원 본관 근처 좌표. [확인 필요] 정확한 POI 좌표는 아니다.
        naviHelper.requestRoute(
            RouteInfo(
                latitude = HOSPITAL_LATITUDE,
                longitude = HOSPITAL_LONGITUDE,
                displayLatitude = null,
                displayLongitude = null,
                poiName = "서울대학교병원 본관",
                poiId = "",
                address = "서울 종로구 대학로 101",
                poiSubId = "0",
            ),
        )
        await("onRoutingCompleted", 30)

        naviHelper.getDestinationInfo()
        await("onDestinationInfo", 5)
        naviHelper.requestTrip(TripRequest(HOSPITAL_LATITUDE, HOSPITAL_LONGITUDE, "", "서울대학교병원 본관", "0"))
        await("onTripInfo", 15)
        report("now(epoch s)", System.currentTimeMillis() / 1000)

        // 경유지 추가: 서울역 근처 임의 좌표
        naviHelper.addWaypoint(
            RequestWaypointInfo(
                latitude = 37.5547,
                longitude = 126.9706,
                poiName = "검증용 경유지",
                waypointIndex = WaypointIndex.FIRST,
                poiId = "",
                address = "",
                poiSubId = "0",
            ),
        )
        await("onWaypointChanged", 20)
        naviHelper.getWaypointInfo()
        await("onWayPointInfo", 5)
        naviHelper.getDestinationInfo()
        Thread.sleep(2_000)
        naviHelper.getTBTInfo()
        naviHelper.getRouteStateInfo()
        Thread.sleep(3_000)

        // 다음 검증에 남지 않도록 경로를 지운다.
        naviHelper.cancelRoute()
        await("onRouteCancelled", 10)
    }

    private fun event(name: String, value: Any) {
        report(name, value)
        events.getOrPut(name) { CountDownLatch(1) }.countDown()
    }

    private fun await(name: String, seconds: Long) {
        val arrived = events.getOrPut(name) { CountDownLatch(1) }.await(seconds, TimeUnit.SECONDS)
        if (!arrived) report("$name 기다림", "${seconds}초 안에 안 옴")
    }

    private fun report(step: String, value: Any?) {
        Log.i(LOG_TAG, "$step → $value")
    }

    private companion object {
        const val LOG_TAG = "PleosVerify"
        const val HOSPITAL_LATITUDE = 37.5796
        const val HOSPITAL_LONGITUDE = 126.9990
    }
}
