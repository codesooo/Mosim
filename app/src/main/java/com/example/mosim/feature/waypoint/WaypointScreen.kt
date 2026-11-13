package com.example.mosim.feature.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mosim.data.mock.mockPlaces
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.MapSlot
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.NumberedOptionItem
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

/**
 * F-3 경유지 후보 (피그마 "모심 화면 v2" F-3).
 *
 * F-2가 인식한 [query]로 장소를 찾아 후보를 보여 주고, 운전자가 고른 한 곳을 [onPlaceSelected]로 넘긴다.
 * **경유지 추가(NaviHelper) 호출은 이 화면의 책임이 아니다.** 선택 결과를 넘기는 데서 끝난다.
 */
@Composable
fun WaypointScreen(
    query: String,
    source: PlaceSearchSource,
    onBack: () -> Unit,
    onPlaceSelected: (PlaceUiModel) -> Unit,
    modifier: Modifier = Modifier,
    submitWaypoint: (suspend (PlaceUiModel) -> WaypointRequest)? = null,
    mapContent: (@Composable () -> Unit)? = null,
    interactionAllowed: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val model: WaypointViewModel = viewModel(
        key = "waypoint-$query",
        factory = viewModelFactory { initializer { WaypointViewModel(source, query) } },
    )
    val state by model.state.collectAsState()
    WaypointContent(
        state = state,
        onBack = onBack,
        onSelect = { if (interactionAllowed) model.select(it) },
        onRetry = model::search,
        onConfirm = {
            if (interactionAllowed) model.requestWaypoint()?.let { place ->
                onPlaceSelected(place)
                scope.launch {
                    val result = try {
                        submitWaypoint?.invoke(place) ?: WaypointRequest.NotConnected
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        WaypointRequest.Failed
                    }
                    model.updateRequest(result)
                }
            }
        },
        mapContent = mapContent,
        interactionAllowed = interactionAllowed,
        modifier = modifier,
    )
}

@Composable
fun WaypointContent(
    state: WaypointUiState,
    onBack: () -> Unit,
    onSelect: (PlaceUiModel) -> Unit = {},
    onRetry: () -> Unit = {},
    onConfirm: () -> Unit = {},
    modifier: Modifier = Modifier,
    mapContent: (@Composable () -> Unit)? = null,
    mapDebugLabel: String? = null,
    interactionAllowed: Boolean = true,
) {
    MosimAppContainer(
        title = "경유지 후보",
        subtitle = state.subtitle(),
        onBack = onBack,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
        ) {
            // F-1과 같은 구성이다. 내용과 버튼을 한 카드에 담고 오른쪽에 지도 슬롯을 둔다.
            Column(
                modifier = Modifier
                    .weight(ContentWeight)
                    .fillMaxHeight()
                    .background(PleosColor.SurfaceBasic, MosimShape.Card)
                    .padding(MosimDimens.CardPadding),
            ) {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    when (state) {
                        WaypointUiState.Searching -> CenterMessage("주변 장소를 찾고 있어요")
                        WaypointUiState.Empty -> CenterMessage("조건에 맞는 장소를 찾지 못했어요")
                        is WaypointUiState.Failed -> CenterMessage(state.message)
                        is WaypointUiState.Ready -> PlaceList(state) { if (interactionAllowed) onSelect(it) }
                    }
                }
                RequestNotice(state, interactionAllowed)
                Row(horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap)) {
                    PleosButton("취소", onBack, modifier = Modifier.weight(1f))
                    if (state is WaypointUiState.Failed) {
                        PleosButton(
                            text = "다시 찾기",
                            onClick = onRetry,
                            type = PleosButtonType.Filled,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        val ready = state as? WaypointUiState.Ready
                        PleosButton(
                            text = "경유지 추가",
                            onClick = onConfirm,
                            type = PleosButtonType.Filled,
                            enabled = interactionAllowed && ready != null && ready.request != WaypointRequest.Requested &&
                                ready.request != WaypointRequest.Applied,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            MapSlot(
                modifier = Modifier.weight(MapWeight).fillMaxHeight(),
                debugLabel = mapDebugLabel,
                content = mapContent,
            )
        }
    }
}

@Composable
private fun PlaceList(state: WaypointUiState.Ready, onSelect: (PlaceUiModel) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = MosimDimens.CardGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MosimDimens.CardGap),
    ) {
        Text(
            text = "경유할 장소를 선택해 주세요",
            style = PleosTypography.BodySmall,
            color = PleosColor.TextDarkTertiary,
            modifier = Modifier.weight(1f),
        )
        // 가짜 결과임을 알리는 칩은 두지 않는다(팀 요청). 출처 구분은 [PlaceUiModel.isMock]과 로그로 한다.
    }
    Column {
        state.places.forEachIndexed { index, place ->
            val selected = place.externalId == state.selected.externalId
            NumberedOptionItem(
                number = index + 1,
                title = place.name,
                subtitle = place.detail,
                onClick = { onSelect(place) },
                // 색만으로 구분하지 않도록 고른 줄에는 테두리를 두른다.
                modifier = if (selected) {
                    Modifier.border(
                        width = MosimDimens.RowDividerThickness,
                        color = PleosColor.ButtonFilledEnabled,
                        shape = MosimShape.Field,
                    )
                } else {
                    Modifier
                },
                tag = place.tag,
                selected = selected,
            )
        }
    }
}

/** F-1과 같은 비율로 나눈다. 두 화면이 같은 틀로 보여야 한다. */
private const val ContentWeight = 58f
private const val MapWeight = 42f

/** 경유 요청 상태와 조작 제한 안내. **경로가 바뀜다고 말하지 않는다.** NaviHelper 응답을 받기 전이다. */
@Composable
private fun RequestNotice(
    state: WaypointUiState,
    interactionAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val request = (state as? WaypointUiState.Ready)?.request
    val notice: Pair<String, ChipTone>? = when {
        // 안전 제한으로 못 고르는 경우는 이유를 짧게 알려 준다.
        !interactionAllowed -> "주행 중에는 선택할 수 없어요" to ChipTone.Caution
        request == WaypointRequest.Requested -> "경유지 추가를 요청했어요" to ChipTone.Neutral
        request == WaypointRequest.NotConnected -> "경유지 추가는 준비 중이에요" to ChipTone.Neutral
        request == WaypointRequest.Applied -> "경유지가 반영됐어요" to ChipTone.Normal
        request == WaypointRequest.Failed -> "경유 요청에 실패했어요" to ChipTone.Caution
        else -> null
    }
    if (notice != null) {
        StatusChip(notice.first, notice.second, modifier)
        Spacer(Modifier.height(MosimDimens.CardGap))
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = PleosTypography.BodyLarge,
            color = PleosColor.TextDarkTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun WaypointUiState.subtitle(): String = when (this) {
    WaypointUiState.Searching -> "주변 장소를 찾는 중"
    WaypointUiState.Empty -> "검색 결과 없음"
    is WaypointUiState.Failed -> "검색 실패"
    is WaypointUiState.Ready -> "추천 후보 ${places.size}곳 · 운전자 선택"
}

@Preview(name = "F-3 후보 목록", widthDp = 1700, heightDp = 1184)
@Composable
private fun WaypointReadyPreview() {
    MosimTheme {
        val places = mockPlaces()
        WaypointContent(WaypointUiState.Ready(places, places.first()), onBack = {})
    }
}

@Preview(name = "F-3 검색 중", widthDp = 1700, heightDp = 1184)
@Composable
private fun WaypointSearchingPreview() {
    MosimTheme { WaypointContent(WaypointUiState.Searching, onBack = {}) }
}

@Preview(name = "F-3 결과 없음", widthDp = 1700, heightDp = 1184)
@Composable
private fun WaypointEmptyPreview() {
    MosimTheme { WaypointContent(WaypointUiState.Empty, onBack = {}) }
}

@Preview(name = "F-3 검색 실패", widthDp = 1700, heightDp = 1184)
@Composable
private fun WaypointFailedPreview() {
    MosimTheme {
        WaypointContent(WaypointUiState.Failed("장소를 불러오지 못했어요"), onBack = {})
    }
}
