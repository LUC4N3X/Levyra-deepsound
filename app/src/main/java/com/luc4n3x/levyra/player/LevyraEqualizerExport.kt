package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.ParametricBiquad
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricFilterType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

enum class EqualizerExportFormat(val fileSuffix: String) {
    GRAPHIC_EQ("GraphicEQ"),
    PARAMETRIC_EQ("ParametricEQ")
}

object LevyraEqualizerExport {
    const val RESPONSE_SAMPLE_RATE = 48_000
    const val GRAPHIC_POINTS = 127
    private const val GRAPHIC_PROFILE_ID = "levyra_graphic_export"
    private val UNSAFE_FILE_CHARACTERS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
    private val WHITESPACE = Regex("""\s+""")

    val graphicFrequenciesHz: FloatArray = run {
        val ratio = 1_000.0.pow(1.0 / (GRAPHIC_POINTS - 1))
        val rounded = IntArray(GRAPHIC_POINTS) { index -> (20.0 * ratio.pow(index)).roundToInt() }
        for (index in 1 until rounded.size) {
            if (rounded[index] <= rounded[index - 1]) rounded[index] = rounded[index - 1] + 1
        }
        FloatArray(GRAPHIC_POINTS) { rounded[it].toFloat() }
    }

    fun currentProfile(settings: LevyraAudioSettings, name: String): ParametricEqProfile? {
        val normalized = settings.normalized()
        val parametric = normalized.activeParametricProfile
        return when {
            normalized.parametricEqualizerEnabled && parametric != null -> parametric
            normalized.equalizerEnabled -> graphicProfile(normalized.bandLevels, normalized.bassBoost, normalized.preampDb, name)
            else -> null
        }
    }

    fun graphicProfile(levels: List<Int>, bassBoost: Int, preampDb: Float, name: String): ParametricEqProfile {
        val frequencies = LevyraEqualizerAudioProcessor.FREQUENCIES
        val bass = bassBoost.coerceIn(0, 100) / 100f * LevyraEqualizerAudioProcessor.BASS_MAX_DB
        var highestBoost = 0f
        val bands = frequencies.indices.map { index ->
            val userDb = levels.getOrElse(index) { 0 }.coerceIn(-100, 100) / 100f * LevyraEqualizerAudioProcessor.MAX_BAND_DB
            val gainDb = userDb + bass * LevyraEqualizerAudioProcessor.BASS_WEIGHTS[index]
            highestBoost = maxOf(highestBoost, gainDb)
            ParametricEqBand(
                frequencyHz = frequencies[index],
                gainDb = gainDb,
                q = LevyraEqualizerAudioProcessor.Q,
                filterType = ParametricFilterType.PEAK
            )
        }
        return ParametricEqProfile(
            id = GRAPHIC_PROFILE_ID,
            name = name,
            preampDb = equalizerInputGainDb(preampDb, highestBoost),
            bands = bands
        )
    }

    fun render(profile: ParametricEqProfile, format: EqualizerExportFormat): String = when (format) {
        EqualizerExportFormat.GRAPHIC_EQ -> graphicEqText(profile)
        EqualizerExportFormat.PARAMETRIC_EQ -> parametricEqText(profile)
    }

    fun graphicEqText(profile: ParametricEqProfile): String {
        val response = FloatArray(GRAPHIC_POINTS)
        ParametricBiquad.responseDb(profile, graphicFrequenciesHz, RESPONSE_SAMPLE_RATE, response)
        return buildString {
            append("GraphicEQ: ")
            graphicFrequenciesHz.forEachIndexed { index, frequency ->
                if (index > 0) append("; ")
                append(frequency.roundToInt()).append(' ').append(decimal(response[index], 1))
            }
            append('\n')
        }
    }

    fun parametricEqText(profile: ParametricEqProfile): String = buildString {
        append("Preamp: ").append(decimal(profile.preampDb, 1)).append(" dB\n")
        profile.bands.forEachIndexed { index, band ->
            append("Filter ").append(index + 1).append(": ")
            append(if (band.enabled) "ON " else "OFF ")
            append(band.filterType.autoEqCode)
            append(" Fc ").append(frequency(band.frequencyHz)).append(" Hz")
            append(" Gain ").append(decimal(band.gainDb, 1)).append(" dB")
            append(" Q ").append(decimal(band.q, 2)).append('\n')
        }
    }

    fun fileName(profileName: String, format: EqualizerExportFormat): String {
        val safe = profileName.replace(UNSAFE_FILE_CHARACTERS, " ").replace(WHITESPACE, " ").trim().take(64)
            .ifBlank { "Levyra EQ" }
        return "$safe ${format.fileSuffix}.txt"
    }

    private fun frequency(value: Float): String =
        if (abs(value - value.roundToInt()) < 0.005f) value.roundToInt().toString() else decimal(value, 1)

    private fun decimal(value: Float, digits: Int): String {
        val text = String.format(Locale.ROOT, "%.${digits}f", value)
        return if (text.startsWith("-") && text.trimStart('-').all { it == '0' || it == '.' }) text.substring(1) else text
    }
}
