package com.luc4n3x.levyra.domain

import java.security.MessageDigest
import kotlin.math.log10
import kotlin.math.roundToInt

object AutoEqImporter {

    const val MAX_INPUT_CHARS = 16_000
    const val MAX_POINTS = 200
    const val MAX_FREQUENCY_HZ = 96_000.0
    const val MIN_POINTS = 2

    val levyraBandFrequenciesHz = intArrayOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

    data class ImportedProfile(
        val name: String?,
        val bandLevels: List<Int>,
        val bandGainDb: List<Float>,
        val preampDb: Float,
        val clamped: Boolean,
        val interpolated: Boolean,
        val skippedPoints: Int,
        val pointCount: Int
    )

    sealed interface ParseResult {
        data class Success(val profile: ImportedProfile) : ParseResult
        data class Error(val error: ParseError) : ParseResult
    }

    sealed interface ParametricParseResult {
        data class Success(val profile: ParametricEqProfile) : ParametricParseResult
        data class Error(val error: ParametricParseError, val lineNumber: Int? = null) : ParametricParseResult
    }

    enum class ParseError {
        EMPTY,
        TOO_LARGE,
        NO_GRAPHIC_EQ,
        INVALID_POINT,
        INVALID_PREAMP,
        NON_FINITE_VALUE,
        TOO_MANY_POINTS,
        INSUFFICIENT_POINTS
    }

    enum class ParametricParseError {
        EMPTY,
        TOO_LARGE,
        NO_PARAMETRIC_EQ,
        INVALID_PREAMP,
        INVALID_FILTER,
        UNSUPPORTED_FILTER,
        DUPLICATE_FILTER,
        TOO_MANY_FILTERS,
        INVALID_FREQUENCY,
        INVALID_GAIN,
        INVALID_Q,
        NON_FINITE_VALUE
    }

    fun parse(text: String): ParseResult {
        if (text.isBlank()) return ParseResult.Error(ParseError.EMPTY)
        if (text.length > MAX_INPUT_CHARS) return ParseResult.Error(ParseError.TOO_LARGE)

        var preampDb = 0f
        var preampClamped = false
        var points: List<Point>? = null
        var name: String? = null
        var skipped = 0

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("Preamp", ignoreCase = true)) {
                val match = PREAMP_LINE.matchEntire(line)
                    ?: return ParseResult.Error(ParseError.INVALID_PREAMP)
                val raw = match.groupValues[1].toDoubleOrNull()
                    ?: return ParseResult.Error(ParseError.INVALID_PREAMP)
                if (!raw.isFinite()) return ParseResult.Error(ParseError.NON_FINITE_VALUE)
                val clampedValue = raw.coerceIn(MIN_PREAMP_DB.toDouble(), MAX_PREAMP_DB.toDouble()).toFloat()
                if (raw < MIN_PREAMP_DB || raw > MAX_PREAMP_DB) preampClamped = true
                preampDb = clampedValue
                continue
            }

            if (line.startsWith("GraphicEQ", ignoreCase = true)) {
                when (val outcome = parseGraphicEqLine(line)) {
                    is GraphicEqOutcome.Ok -> {
                        if (outcome.points.size > MAX_POINTS) return ParseResult.Error(ParseError.TOO_MANY_POINTS)
                        if (outcome.points.any { it.frequency <= 0.0 }) return ParseResult.Error(ParseError.INVALID_POINT)
                        val usable = outcome.points.filter { it.frequency <= MAX_FREQUENCY_HZ }
                        skipped += outcome.points.size - usable.size
                        points = usable
                    }
                    GraphicEqOutcome.NonFinite -> return ParseResult.Error(ParseError.NON_FINITE_VALUE)
                    GraphicEqOutcome.Malformed -> return ParseResult.Error(ParseError.INVALID_POINT)
                }
                continue
            }

            if (line.startsWith("#")) {
                if (name == null) {
                    NAME_LINE.matchEntire(line)?.let { match ->
                        name = match.groupValues[1].trim().takeIf { it.isNotBlank() && it.length <= 48 }
                    }
                }
                continue
            }

            skipped += 1
        }

        val rawPoints = points ?: return ParseResult.Error(ParseError.NO_GRAPHIC_EQ)
        val deduped = rawPoints.associateBy { it.frequency }.values.sortedBy { it.frequency }
        if (deduped.size < MIN_POINTS) return ParseResult.Error(ParseError.INSUFFICIENT_POINTS)

        var clamped = preampClamped
        var interpolated = false
        val bandLevels = ArrayList<Int>(LevyraAudioPresets.bandCount)
        val bandGainDb = ArrayList<Float>(LevyraAudioPresets.bandCount)
        for (bandHz in levyraBandFrequenciesHz) {
            val frequency = bandHz.toDouble()
            val exact = deduped.any { it.frequency == frequency }
            if (!exact) interpolated = true
            val gain = interpolateGain(frequency, deduped)
            if (gain > LevyraAudioPresets.maxBandDb || gain < -LevyraAudioPresets.maxBandDb) clamped = true
            val level = (gain / LevyraAudioPresets.maxBandDb * 100.0).roundToInt().coerceIn(-100, 100)
            bandLevels += level
            bandGainDb += level / 100f * LevyraAudioPresets.maxBandDb
        }

        return ParseResult.Success(
            ImportedProfile(
                name = name,
                bandLevels = bandLevels,
                bandGainDb = bandGainDb,
                preampDb = preampDb,
                clamped = clamped,
                interpolated = interpolated,
                skippedPoints = skipped,
                pointCount = deduped.size
            )
        )
    }

    fun parseParametric(
        text: String,
        fallbackName: String = "Imported Parametric EQ"
    ): ParametricParseResult {
        if (text.isBlank()) return ParametricParseResult.Error(ParametricParseError.EMPTY)
        if (text.length > MAX_INPUT_CHARS) return ParametricParseResult.Error(ParametricParseError.TOO_LARGE)

        var preampDb = 0f
        var hasPreamp = false
        var name: String? = null
        val filterNumbers = HashSet<Int>(ParametricEqualizer.MAX_BANDS)
        val bands = ArrayList<ParametricEqBand>(ParametricEqualizer.MAX_BANDS)

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) return@forEachIndexed
            if (line.startsWith("#")) {
                if (name == null) {
                    NAME_LINE.matchEntire(line)?.let { match ->
                        name = match.groupValues[1].trim().takeIf { it.isNotBlank() }
                    }
                }
                return@forEachIndexed
            }
            if (line.startsWith("Preamp", ignoreCase = true)) {
                if (hasPreamp) {
                    return ParametricParseResult.Error(ParametricParseError.INVALID_PREAMP, lineNumber)
                }
                when (val parsed = parseParametricPreamp(line)) {
                    is ParametricPreampResult.Error -> {
                        return ParametricParseResult.Error(parsed.error, lineNumber)
                    }
                    is ParametricPreampResult.Success -> preampDb = parsed.value
                }
                hasPreamp = true
                return@forEachIndexed
            }
            if (line.startsWith("GraphicEQ", ignoreCase = true)) {
                return@forEachIndexed
            }
            if (!line.startsWith("Filter", ignoreCase = true)) {
                return ParametricParseResult.Error(ParametricParseError.INVALID_FILTER, lineNumber)
            }
            if (bands.size >= ParametricEqualizer.MAX_BANDS) {
                return ParametricParseResult.Error(ParametricParseError.TOO_MANY_FILTERS, lineNumber)
            }
            when (val parsed = parseParametricFilter(line)) {
                is ParametricFilterResult.Error -> {
                    return ParametricParseResult.Error(parsed.error, lineNumber)
                }
                is ParametricFilterResult.Success -> {
                    if (!filterNumbers.add(parsed.number)) {
                        return ParametricParseResult.Error(ParametricParseError.DUPLICATE_FILTER, lineNumber)
                    }
                    bands += parsed.band
                }
            }
        }

        if (bands.isEmpty()) return ParametricParseResult.Error(ParametricParseError.NO_PARAMETRIC_EQ)
        val profileName = name?.take(ParametricEqualizer.MAX_NAME_CHARS)
            ?: fallbackName.trim().take(ParametricEqualizer.MAX_NAME_CHARS).ifEmpty { "Imported Parametric EQ" }
        return ParametricParseResult.Success(
            ParametricEqProfile(
                id = ParametricEqualizer.profileId(
                    prefix = ParametricEqualizer.IMPORTED_PROFILE_PREFIX,
                    name = profileName,
                    preampDb = preampDb,
                    bands = bands
                ),
                name = profileName,
                preampDb = preampDb,
                bands = bands
            )
        )
    }

    private sealed interface ParametricPreampResult {
        data class Success(val value: Float) : ParametricPreampResult
        data class Error(val error: ParametricParseError) : ParametricPreampResult
    }

    private sealed interface ParametricFilterResult {
        data class Success(val number: Int, val band: ParametricEqBand) : ParametricFilterResult
        data class Error(val error: ParametricParseError) : ParametricFilterResult
    }

    private fun parseParametricPreamp(line: String): ParametricPreampResult {
        val match = PARAMETRIC_PREAMP_LINE.matchEntire(line)
            ?: return ParametricPreampResult.Error(ParametricParseError.INVALID_PREAMP)
        val value = match.groupValues[1].toFloatOrNull()
            ?: return ParametricPreampResult.Error(ParametricParseError.INVALID_PREAMP)
        if (!value.isFinite()) return ParametricPreampResult.Error(ParametricParseError.NON_FINITE_VALUE)
        if (value !in ParametricEqualizer.MIN_PREAMP_DB..ParametricEqualizer.MAX_PREAMP_DB) {
            return ParametricPreampResult.Error(ParametricParseError.INVALID_PREAMP)
        }
        return ParametricPreampResult.Success(value)
    }

    private fun parseParametricFilter(line: String): ParametricFilterResult {
        val match = PARAMETRIC_FILTER_LINE.matchEntire(line)
            ?: return ParametricFilterResult.Error(ParametricParseError.INVALID_FILTER)
        val number = match.groupValues[1].toIntOrNull()
            ?: return ParametricFilterResult.Error(ParametricParseError.INVALID_FILTER)
        if (number <= 0) return ParametricFilterResult.Error(ParametricParseError.INVALID_FILTER)
        val filterType = ParametricFilterType.fromAutoEqCode(match.groupValues[3])
            ?: return ParametricFilterResult.Error(ParametricParseError.UNSUPPORTED_FILTER)
        val frequency = match.groupValues[4].toFloatOrNull()
            ?: return ParametricFilterResult.Error(ParametricParseError.INVALID_FREQUENCY)
        val gain = match.groupValues[5].toFloatOrNull()
            ?: return ParametricFilterResult.Error(ParametricParseError.INVALID_GAIN)
        val q = match.groupValues[6].toFloatOrNull()
            ?: return ParametricFilterResult.Error(ParametricParseError.INVALID_Q)
        validateParametricFilterValues(frequency, gain, q)?.let { error ->
            return ParametricFilterResult.Error(error)
        }
        return ParametricFilterResult.Success(
            number = number,
            band = ParametricEqBand(
                frequencyHz = frequency,
                gainDb = gain,
                q = q,
                filterType = filterType,
                enabled = match.groupValues[2].equals("ON", ignoreCase = true)
            )
        )
    }

    private fun validateParametricFilterValues(
        frequency: Float,
        gain: Float,
        q: Float
    ): ParametricParseError? = when {
        !frequency.isFinite() || !gain.isFinite() || !q.isFinite() -> ParametricParseError.NON_FINITE_VALUE
        frequency !in ParametricEqualizer.MIN_FREQUENCY_HZ..ParametricEqualizer.MAX_FREQUENCY_HZ ->
            ParametricParseError.INVALID_FREQUENCY
        gain !in -ParametricEqualizer.MAX_GAIN_DB..ParametricEqualizer.MAX_GAIN_DB -> ParametricParseError.INVALID_GAIN
        q !in ParametricEqualizer.MIN_Q..ParametricEqualizer.MAX_Q -> ParametricParseError.INVALID_Q
        else -> null
    }

    fun customPresetId(name: String, profile: ImportedProfile): String {
        val stable = "$name|${profile.bandLevels.joinToString(",")}|${profile.preampDb}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(16)
        return LevyraAudioPresets.CUSTOM_PRESET_PREFIX + digest
    }

    private fun interpolateGain(frequency: Double, points: List<Point>): Double {
        points.firstOrNull { it.frequency == frequency }?.let { return it.gain }
        val lower = points.lastOrNull { it.frequency < frequency }
        val upper = points.firstOrNull { it.frequency > frequency }
        if (lower == null) return upper?.gain ?: 0.0
        if (upper == null) return lower.gain
        val logFrequency = log10(frequency)
        val logLower = log10(lower.frequency)
        val logUpper = log10(upper.frequency)
        val fraction = (logFrequency - logLower) / (logUpper - logLower)
        return lower.gain + (upper.gain - lower.gain) * fraction
    }

    private fun parseGraphicEqLine(line: String): GraphicEqOutcome {
        val colon = line.indexOf(':')
        if (colon < 0) return GraphicEqOutcome.Malformed
        val body = line.substring(colon + 1).trim()
        if (body.isEmpty()) return GraphicEqOutcome.Malformed
        val parsed = ArrayList<Point>()
        for (segment in body.split(';')) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            val tokens = trimmed.split(WHITESPACE)
            if (tokens.size != 2) return GraphicEqOutcome.Malformed
            val frequency = tokens[0].toDoubleOrNull() ?: return GraphicEqOutcome.Malformed
            val gain = tokens[1].toDoubleOrNull() ?: return GraphicEqOutcome.Malformed
            if (!frequency.isFinite() || !gain.isFinite()) return GraphicEqOutcome.NonFinite
            parsed += Point(frequency, gain)
        }
        return GraphicEqOutcome.Ok(parsed)
    }

    private data class Point(val frequency: Double, val gain: Double)

    private sealed interface GraphicEqOutcome {
        data class Ok(val points: List<Point>) : GraphicEqOutcome
        data object NonFinite : GraphicEqOutcome
        data object Malformed : GraphicEqOutcome
    }

    private val WHITESPACE = Regex("""\s+""")
    private val PREAMP_LINE = Regex(
        """^Preamp\s*:\s*([+-]?(?:\d+(?:\.\d*)?|\.\d+))\s*(?:dB)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val NAME_LINE = Regex(
        """^#\s*(?:Name|Profile)\s*:\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private const val PARAMETRIC_NUMBER = "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?"
    private val PARAMETRIC_PREAMP_LINE = Regex(
        "^Preamp\\s*:\\s*($PARAMETRIC_NUMBER)\\s*(?:dB)?\\s*$",
        RegexOption.IGNORE_CASE
    )
    private val PARAMETRIC_FILTER_LINE = Regex(
        "^Filter\\s+(\\d+)\\s*:\\s*(ON|OFF)\\s+([A-Za-z0-9]+)\\s+" +
            "Fc\\s+($PARAMETRIC_NUMBER)\\s*Hz\\s+Gain\\s+($PARAMETRIC_NUMBER)\\s*dB\\s+" +
            "Q\\s+($PARAMETRIC_NUMBER)\\s*$",
        RegexOption.IGNORE_CASE
    )

    private const val MIN_PREAMP_DB = -12f
    private const val MAX_PREAMP_DB = 3f
}
