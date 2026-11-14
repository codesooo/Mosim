package com.example.mosim.feature.escort.command

import com.example.mosim.data.place.PlaceKind

/**
 * STT가 넘긴 문장을 [VoiceCommand]로 바꾼다.
 *
 * LLM 없이 키워드 규칙만 쓴다. 팀 결정 전까지 LLM 호출 코드를 쓰지 않기로 했고(project-status.md),
 * 규칙이면 PT에서 "이 단어가 있으면 이 명령"으로 바로 설명할 수 있다.
 * 의도 파악 보류 결정과의 충돌은 docs/design/escort.md 11장 #2에서 팀 합의를 기다린다.
 *
 * 검사 순서가 결과를 정한다. 장소 → 등받이 → 음량 → 온도 순으로 본다.
 * 장소 요청은 경로를 바꾸는 요청이라 다른 조정과 섞였을 때 놓치지 않도록 먼저 본다.
 * 음량을 온도보다 먼저 보는 이유는 "볼륨 올려"의 "올려"가 온도 규칙에 걸리지 않게 하기 위해서다.
 */
object VoiceCommandParser {

    /** 숫자 없이 "더워", "추워"라고 했을 때 바꾸는 폭. [확인 필요] 팀과 기본 폭 합의 */
    const val DEFAULT_TEMPERATURE_STEP_CELSIUS = 1.0f

    /** 등받이 한 번 요청에 바꾸는 각도. [확인 필요] 실제 좌석 이동 속도와 위치 단위를 에뮬레이터에서 확인 */
    const val BACKREST_STEP_DEGREES = 5

    // 같은 문장에 두 장소가 있으면 위쪽이 이긴다. "휴게소 화장실"은 화장실이 더 구체적인 요청이라 먼저 둔다.
    private val placeKeywords: List<Pair<PlaceKind, List<String>>> = listOf(
        PlaceKind.RESTROOM to listOf("화장실"),
        PlaceKind.CAFE to listOf("카페", "커피"),
        PlaceKind.CONVENIENCE_STORE to listOf("편의점"),
        PlaceKind.EV_CHARGER to listOf("충전소", "충전"),
        PlaceKind.REST_AREA to listOf("휴게소", "쉬었다", "쉬어", "휴식"),
    )

    private val backrestTargets = listOf("의자", "등받이", "시트")
    private val backrestRecline = listOf("젖혀", "눕혀", "뒤로")
    private val backrestUpright = listOf("세워", "세우", "앞으로")

    private val volumeTargets = listOf("소리", "볼륨", "음량")
    private val volumeUp = listOf("키워", "올려", "크게", "높여")
    private val volumeDown = listOf("줄여", "낮춰", "내려", "작게")
    private val tooLoud = listOf("시끄러", "조용히")
    private val tooQuiet = listOf("안들려")

    private val feelsHot = listOf("더워", "덥", "더운", "시원하게")
    private val feelsCold = listOf("추워", "춥", "추운", "쌀쌀", "따뜻하게")
    private val temperatureTargets = listOf("온도", "에어컨", "히터")
    private val temperatureUp = listOf("올려", "높여", "올리")
    private val temperatureDown = listOf("낮춰", "내려", "내리")

    // "23도", "23.5도". 숫자 바로 뒤의 "도"만 온도로 본다.
    private val degreeNumberPattern = Regex("""(\d+(?:\.\d+)?)도(으로|로)?""")

    fun parse(text: String): VoiceCommand {
        // STT 결과는 띄어쓰기가 일정하지 않다("소리 줄여" / "소리줄여"). 공백을 지우고 비교한다.
        val compact = text.replace(Regex("""\s+"""), "")
        if (compact.isEmpty()) return VoiceCommand.Unknown(text)

        return parsePlace(compact)
            ?: parseBackrest(compact)
            ?: parseVolume(compact)
            ?: parseTemperature(compact)
            ?: VoiceCommand.Unknown(text)
    }

    private fun parsePlace(compact: String): VoiceCommand? {
        val kind = placeKeywords.firstOrNull { (_, words) -> compact.containsAny(words) }?.first
        return kind?.let { VoiceCommand.FindPlace(it) }
    }

    private fun parseBackrest(compact: String): VoiceCommand? {
        if (!compact.containsAny(backrestTargets)) return null
        return when {
            compact.containsAny(backrestRecline) -> VoiceCommand.AdjustBackrest(BACKREST_STEP_DEGREES)
            compact.containsAny(backrestUpright) -> VoiceCommand.AdjustBackrest(-BACKREST_STEP_DEGREES)
            else -> null
        }
    }

    private fun parseVolume(compact: String): VoiceCommand? {
        if (compact.containsAny(tooLoud)) return VoiceCommand.AdjustVolume(-1)
        if (compact.containsAny(tooQuiet)) return VoiceCommand.AdjustVolume(1)
        if (!compact.containsAny(volumeTargets)) return null
        return when {
            compact.containsAny(volumeUp) -> VoiceCommand.AdjustVolume(1)
            compact.containsAny(volumeDown) -> VoiceCommand.AdjustVolume(-1)
            else -> null
        }
    }

    private fun parseTemperature(compact: String): VoiceCommand? {
        parseTemperatureWithNumber(compact)?.let { return it }

        if (compact.containsAny(feelsHot)) return VoiceCommand.AdjustTemperature(-DEFAULT_TEMPERATURE_STEP_CELSIUS)
        if (compact.containsAny(feelsCold)) return VoiceCommand.AdjustTemperature(DEFAULT_TEMPERATURE_STEP_CELSIUS)
        if (!compact.containsAny(temperatureTargets)) return null
        return when {
            compact.containsAny(temperatureUp) -> VoiceCommand.AdjustTemperature(DEFAULT_TEMPERATURE_STEP_CELSIUS)
            compact.containsAny(temperatureDown) -> VoiceCommand.AdjustTemperature(-DEFAULT_TEMPERATURE_STEP_CELSIUS)
            else -> null
        }
    }

    /**
     * "23도로 맞춰"는 목표값, "2도 낮춰"는 변화량이다.
     * "로/으로"가 붙으면 목표값으로 보고, 아니면 올림·내림 표현이 있을 때만 변화량으로 본다.
     */
    private fun parseTemperatureWithNumber(compact: String): VoiceCommand? {
        val match = degreeNumberPattern.find(compact) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        val hasTargetSuffix = match.groupValues[2].isNotEmpty()

        return when {
            hasTargetSuffix -> VoiceCommand.SetTemperature(value)
            compact.containsAny(temperatureDown) -> VoiceCommand.AdjustTemperature(-value)
            compact.containsAny(temperatureUp) -> VoiceCommand.AdjustTemperature(value)
            else -> VoiceCommand.SetTemperature(value)
        }
    }

    private fun String.containsAny(words: List<String>): Boolean = words.any { contains(it) }
}
