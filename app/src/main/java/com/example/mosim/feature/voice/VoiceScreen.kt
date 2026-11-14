package com.example.mosim.feature.voice

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
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(model, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.finalResults.collect { text ->
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
    LaunchedEffect(model) { beforeStart(); model.start() }
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

