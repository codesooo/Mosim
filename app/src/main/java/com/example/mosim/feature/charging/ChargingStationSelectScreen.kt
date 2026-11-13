package com.example.mosim.feature.charging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.ui.components.ChipSize
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.InfoCard
import com.example.mosim.ui.components.MapPlaceholder
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimColor
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography
import com.example.mosim.ui.theme.strong

/*
 * B-2 충전소 경유 선택 (피그마 모심 화면 v2, B-1 "충전소 보기"에서 진입).
 *
 * 화면에 보이는 값은 전부 아래 Mock 상수다. 충전소 검색, 지도, 경로, 충전 시간·잔량·도착 시각 계산은 하지 않는다.
 * 충전소 데이터 출처(NaviHelper 또는 외부 API)가 정해지면 Mock을 교체한다. [확인 필요]
 */

/**
 * 충전소 후보 하나.
 * mapX/mapY는 Mock 지도 안에서의 상대 위치(0~1)다. 실제 위경도가 아니다.
 */
private data class ChargingStation(
    val name: String,
    val description: String,
    val vacancy: String,
    val mapX: Float,
    val mapY: Float,
)

// Mock: 피그마 B-2 예시 값.
private const val MockSubtitle = "경유지 2개 중 1개 사용 중 (픽업지)"

// Mock: 피그마 B-2 충전소 목록. 실제 충전소·내비 데이터가 아니다.
private val MockStations = listOf(
    ChargingStation(
        name = "EV 스테이션 강남",
        description = "경유 +4분 · 급속 200kW",
        vacancy = "빈 자리 2/4",
        mapX = 0.28f,
        mapY = 0.62f,
    ),
    ChargingStation(
        name = "역삼 공영주차장 충전소",
        description = "경유 +7분 · 급속 100kW",
        vacancy = "빈 자리 1/2",
        mapX = 0.52f,
        mapY = 0.26f,
    ),
    ChargingStation(
        name = "서초 이마트 충전소",
        description = "경유 +11분 · 급속 100kW",
        vacancy = "빈 자리 3/6",
        mapX = 0.72f,
        mapY = 0.74f,
    ),
)

// Mock: 피그마 B-2 요약 카드 값(첫 번째 충전소 기준). 계산 근거가 없어 어느 충전소를 골라도 이 값을 보여 준다.
// 충전 시간·잔량·도착 시각 계산 방식이 정해지면 충전소별 값으로 바꾼다. [확인 필요]
private const val MockSummary = "20분 충전 후 잔량 45% · 픽업 도착 08:27"
private const val MockSummaryChip = "약속 3분 전"
private val MockSummaryChipTone = ChipTone.Normal

// 피그마 B-2는 첫 번째 충전소가 선택된 상태다.
private const val DefaultSelectedIndex = 0

// Mock 지도 위 현재 차량 위치, 픽업지, 경로가 지나는 점 (상대 좌표 0~1).
private val MockCarPosition = Offset(0.14f, 0.88f)
private val MockPickupPosition = Offset(0.84f, 0.14f)
private val MockRoutePoints = listOf(MockCarPosition, Offset(0.30f, 0.66f), Offset(0.40f, 0.30f), MockPickupPosition)

/**
 * 건너뛰기, 경유지 추가는 둘 다 B-3(에스코트 계획)으로 간다. 이동은 호출부(MosimNavHost)가 정한다.
 * 경유지 추가는 고른 충전소 이름을 넘긴다. 경유지로 실제 등록하는 기능(NaviHelper)은 미연동이다.
 */
@Composable
fun ChargingStationSelectScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onAddWaypoint: (stationName: String) -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(DefaultSelectedIndex) }
    val selected = MockStations[selectedIndex]

    MosimAppContainer(
        title = "충전소 경유지 선택",
        subtitle = MockSubtitle,
        onBack = onBack,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // 버튼이 왼쪽 열 안에 있는 화면이라 bottomActions 대신 열 맨 아래에 둔다(ui-components.md 1-6).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
            ) {
                InfoCard(
                    title = "경로상 충전소",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MockStations.forEachIndexed { index, station ->
                        StationRow(
                            station = station,
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                        )
                    }
                }
                SummaryCard(text = MockSummary, chipText = MockSummaryChip, chipTone = MockSummaryChipTone)
                Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
                    PleosButton(text = "건너뛰기", onClick = onSkip, modifier = Modifier.weight(1f))
                    PleosButton(
                        text = "경유지 추가",
                        onClick = { onAddWaypoint(selected.name) },
                        type = PleosButtonType.Filled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            MockStationMap(
                selectedIndex = selectedIndex,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * 선택형 충전소 행. 선택되면 회색 배경과 진한 칩으로 바뀐다.
 * ui-components.md 3장 "선택 상태가 있는 목록 행"이다. 지금은 B-2에서만 써서 화면 안에 둔다.
 */
@Composable
private fun StationRow(
    station: ChargingStation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MosimShape.Field)
            .background(if (selected) PleosColor.SurfaceHigh else PleosColor.SurfaceBasic, MosimShape.Field)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = station.name,
                style = PleosTypography.TitleSmall.strong(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = station.description, style = PleosTypography.LabelMedium, color = PleosColor.TextDarkTertiary)
        }
        StatusChip(
            text = station.vacancy,
            tone = if (selected) ChipTone.Dark else ChipTone.Neutral,
            size = ChipSize.Small,
        )
    }
}

/** 요약 한 줄 카드. CompactInfoCard와 비슷하지만 값 자리에 칩이 들어간다(ui-components.md B-2). */
@Composable
private fun SummaryCard(text: String, chipText: String, chipTone: ChipTone) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PleosColor.SurfaceBasic, MosimShape.Card)
            .padding(horizontal = MosimDimens.CardPadding, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = text,
            style = PleosTypography.BodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StatusChip(text = chipText, tone = chipTone, size = ChipSize.Small)
    }
}

/**
 * Mock 지도. MapPlaceholder 위에 경로선, 현재 차량, 픽업지, 충전소 3곳 점만 그린다.
 * 지도 SDK를 쓰지 않으며 위치는 MockStations의 상대 좌표다. 지도 연동 방식이 정해지면 통째로 교체한다. [확인 필요]
 */
@Composable
private fun MockStationMap(selectedIndex: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        MapPlaceholder(modifier = Modifier.fillMaxSize(), text = "지도 · 경로 + 충전소 3곳")
        Canvas(modifier = Modifier.fillMaxSize()) {
            val route = Path().apply {
                MockRoutePoints.forEachIndexed { i, p ->
                    val o = p.toCanvas(this@Canvas)
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
            }
            drawPath(
                path = route,
                color = PleosColor.InformativePositive,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            MockStations.forEachIndexed { index, station ->
                val center = Offset(station.mapX, station.mapY).toCanvas(this)
                if (index == selectedIndex) {
                    drawCircle(PleosColor.SurfaceBasic, radius = 30.dp.toPx(), center = center)
                    drawCircle(PleosColor.ButtonFilledEnabled, radius = 24.dp.toPx(), center = center)
                } else {
                    drawCircle(PleosColor.SurfaceBasic, radius = 20.dp.toPx(), center = center)
                    drawCircle(PleosColor.Basic400, radius = 15.dp.toPx(), center = center)
                }
            }

            val pickup = MockPickupPosition.toCanvas(this)
            drawCircle(PleosColor.SurfaceBasic, radius = 22.dp.toPx(), center = pickup)
            drawCircle(MosimColor.NormalAccent, radius = 16.dp.toPx(), center = pickup)

            val car = MockCarPosition.toCanvas(this)
            drawCircle(PleosColor.SurfaceBasic, radius = 22.dp.toPx(), center = car)
            drawCircle(PleosColor.InformativePositive, radius = 16.dp.toPx(), center = car)
        }
    }
}

private fun Offset.toCanvas(scope: DrawScope) = Offset(x * scope.size.width, y * scope.size.height)

@Preview(name = "B-2 충전소 경유 선택", widthDp = 1700, heightDp = 1184)
@Composable
private fun ChargingStationSelectScreenPreview() {
    MosimTheme {
        ChargingStationSelectScreen(onBack = {}, onSkip = {}, onAddWaypoint = {})
    }
}
