package com.example.mosim.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mosim.platform.pleos.SpeechError
import com.example.mosim.platform.pleos.SpeechMode
import com.example.mosim.platform.pleos.SpeechRecognizerProvider
import com.example.mosim.ui.components.ChipSize
import com.example.mosim.ui.components.ChipTone
import com.example.mosim.ui.components.MosimAppContainer
import com.example.mosim.ui.components.StatusChip
import com.example.mosim.ui.components.pleos.PleosButton
import com.example.mosim.ui.components.pleos.PleosButtonType
import com.example.mosim.ui.theme.MosimDimens
import com.example.mosim.ui.theme.MosimShape
import com.example.mosim.ui.theme.MosimTheme
import com.example.mosim.ui.theme.MosimTypography
import com.example.mosim.ui.theme.PleosColor
import com.example.mosim.ui.theme.PleosTypography

@Composable
fun VoiceScreen(
    onBack: () -> Unit,
    onSpeechRecognized: (String) -> Unit,
    mode: SpeechMode = SpeechMode.Pleos,
    showDebugControls: Boolean = false,
    /**
     * 최종 텍스트를 넘긴 뒤 외부 AI 분석·장소 검색이 이어지는가.
     * `true`면 결과를 넘긴 직후 "요청을 확인하고 있어요" 상태로 바뀜다.
     */
    awaitsAnalysis: Boolean = false,
    beforeStart: () -> Unit = {},
    analyzeRequest: (suspend (String) -> VoiceAnalysisResult)? = null,
    onAnalysisResult: (VoiceAnalysisResult) -> Unit = {},
    /**
     * 이 시간 안에 최종 인식 결과가 오지 않으면 그때까지의 부분 텍스트(없으면 [fallbackQuery])로
     * SearchPlaces 결과를 만들어 다음 화면으로 넘긴다. 0이면 끈다.
     * 실기 마이크 인식이 아직 미검증이라 시연이 F-2에서 멈추지 않게 두는 임시 장치다.
     * 실제 인식이 검증되면 이 값을 0으로 두고 제거한다.
     */
    autoAdvanceMillis: Long = 0L,
    /** Mock: 자동 진행에서 쓸 기본 검색어. 실제 인식 결과가 아니다. */
    fallbackQuery: String = "근처 카페",
) {
    val context = LocalContext.current.applicationContext
    val model: VoiceViewModel = viewModel(
        key = "voice-$mode",
        factory = viewModelFactory {
            initializer { VoiceViewModel(SpeechRecognizerProvider.create(context, mode)) }
        },
    )
    val state by model.state.collectAsState()
    val latestResult by rememberUpdatedState(onSpeechRecognized)
    val latestAnalyzer by rememberUpdatedState(analyzeRequest)
    val latestAnalysisResult by rememberUpdatedState(onAnalysisResult)
    var analysisMessage by remember { mutableStateOf<String?>(null) }
    // 최종 결과 처리와 자동 진행이 같은 화면에서 두 번 이동하지 않게 한 번만 쓴다.
    val advanced = remember(model) { java.util.concurrent.atomic.AtomicBoolean(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(model, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.finalResults.collect { text ->
                if (!advanced.compareAndSet(false, true)) return@collect
                if (awaitsAnalysis) model.markAnalyzing()
                latestResult(text)
                if (awaitsAnalysis) {
                    val analyzer = latestAnalyzer
                    if (analyzer == null) {
                        analysisMessage = "요청 분석 기능 연결 예정"
                    } else {
                        val result = try {
                            analyzer(text)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            VoiceAnalysisResult.Message("요청을 확인하지 못했어요. 잠시 후 다시 시도해 주세요")
                        }
                        if (result is VoiceAnalysisResult.Message) analysisMessage = result.text
                        latestAnalysisResult(result)
                    }
                }
            }
        }
    }
    DisposableEffect(model, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) model.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            model.cancel()
        }
    }
    // 실제 모드는 마이크 권한이 승인된 뒤에 시작한다. 승인 전에 request()를 부르면 무음만 들어온다.
    var micGranted by remember(mode) {
        mutableStateOf(
            mode != SpeechMode.Pleos || ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val micRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (!granted) analysisMessage = "마이크 권한이 없어 음성을 들을 수 없어요"
    }
    LaunchedEffect(model, micGranted) {
        if (!micGranted) {
            micRequest.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }
        beforeStart()
        model.start()
    }
    // 권한 대화상자가 떠 있는 동안에는 세지 않는다. 승인 전에 시간이 흐르면 F-2를 보지도 못하고 넘어간다.
    LaunchedEffect(model, autoAdvanceMillis, micGranted) {
        if (autoAdvanceMillis <= 0L || !micGranted) return@LaunchedEffect
        delay(autoAdvanceMillis)
        if (!advanced.compareAndSet(false, true)) return@LaunchedEffect
        val query = state.text.trim().ifEmpty { fallbackQuery }
        model.cancel()
        latestAnalysisResult(VoiceAnalysisResult.SearchPlaces(query))
    }
    VoiceContent(
        state = state,
        onBack = { model.cancel(); onBack() },
        isMock = mode != SpeechMode.Pleos,
        showDebugControls = showDebugControls,
        onStart = model::start,
        onStop = model::stop,
        onCancel = model::cancel,
        analysisMessage = analysisMessage,
    )
}

