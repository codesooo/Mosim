package com.example.mosim.feature.vehicle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mosim.platform.pleos.VehicleStatus
import com.example.mosim.platform.pleos.VehicleStatusSourceProvider
import com.example.mosim.ui.MosimScreenScaffold
import com.example.mosim.ui.theme.MosimTheme

@Composable
fun VehicleStatusScreen(onBack: () -> Unit) {
    val source = VehicleStatusSourceProvider.source
    val status by produceState<VehicleStatus?>(initialValue = null, source) {
        value = source.load()
    }

    MosimScreenScaffold(title = "차량 상태", onBack = onBack) {
        val current = status
        if (current == null) {
            Text("불러오는 중")
        } else {
            VehicleStatusBody(current)
        }
    }
}

@Composable
private fun VehicleStatusBody(status: VehicleStatus) {
    Text("배터리 ${status.batteryPercent}%")
    Spacer(Modifier.height(8.dp))
    Text("주행 가능 ${status.rangeKm}km")
    Spacer(Modifier.height(8.dp))
    Text(if (status.doorsLocked) "문 잠김" else "문 열림")
    Spacer(Modifier.height(8.dp))
    Text("남은 좌석 ${status.seatsAvailable}석")
    Spacer(Modifier.height(16.dp))
    Text("Mock 데이터다. 실제 차량 값이 아니다.")
}

@Preview(showBackground = true)
@Composable
private fun VehicleStatusBodyPreview() {
    MosimTheme {
        MosimScreenScaffold(title = "차량 상태") {
            VehicleStatusBody(VehicleStatus(72, 310, true, 3))
        }
    }
}
