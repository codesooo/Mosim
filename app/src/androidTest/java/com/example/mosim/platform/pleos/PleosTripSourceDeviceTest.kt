package com.example.mosim.platform.pleos

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.navi.helper.NaviHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱이 쓰는 PleosTripSource + PleosNaviApi를 Pleos Connect 에뮬레이터에서 그대로 돌려 본다.
 * 끝나면 경로를 지워 다음 실행에 남기지 않는다. Pleos 내비 서비스가 없는 기기에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class PleosTripSourceDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var naviHelper: NaviHelper
    private lateinit var navi: PleosNaviApi

    @Before
    fun setUp() {
        val hasNaviService = context.packageManager
            .queryIntentServices(Intent("ai.pleos.playground.intent.action.NAVI_START"), 0).isNotEmpty()
        assumeTrue("Pleos 내비 서비스가 없는 기기", hasNaviService)
        naviHelper = NaviHelper(context)
        navi = PleosNaviApi(naviHelper)
        navi.connect()
    }

    @After
    fun tearDown() {
        scope.cancel()
        if (::naviHelper.isInitialized) {
            naviHelper.cancelRoute()
            Thread.sleep(1_000)
            navi.release()
        }
    }

    @Test
    fun 경로를_요청하고_도착시각과_경유지를_읽는다(): Unit = runBlocking {
        val source = PleosTripSource(scope, navi)

        val location = source.currentLocation()
        Log.i(LOG_TAG, "currentLocation → $location")

        val route = source.ensureRoute("서울대병원 본관", GeoPoint(37.5795590, 126.9990026))
        Log.i(LOG_TAG, "ensureRoute → $route")
        assertEquals(SourceResult.Success(Unit), route)

        val status = withTimeout(15_000) { source.tripStatus.first { it != null } }!!
        Log.i(LOG_TAG, "tripStatus → $status")

        val waypoint = source.addWaypoint("검증용 경유지", GeoPoint(37.5547, 126.9706))
        Log.i(LOG_TAG, "addWaypoint → $waypoint")
        assertEquals(SourceResult.Success(Unit), waypoint)

        val withWaypoint = withTimeout(15_000) {
            source.tripStatus.first { (it?.waypointCount ?: 0) > status.waypointCount }
        }
        Log.i(LOG_TAG, "tripStatus(after waypoint) → $withWaypoint")
    }

    private companion object {
        const val LOG_TAG = "PleosVerify"
    }
}
