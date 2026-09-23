package com.luc4n3x.levyra.domain

import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class ParametricFilterType(val autoEqCode: String) {
    PEAK("PK"),
    LOW_SHELF("LSC"),
    HIGH_SHELF("HSC");

    companion object {
        fun fromAutoEqCode(value: String): ParametricFilterType? =
            entries.firstOrNull { it.autoEqCode.equals(value.trim(), ignoreCase = true) }
    }
}

data class ParametricEqBand(
    val frequencyHz: Float,
    val gainDb: Float,
    val q: Float,
    val filterType: ParametricFilterType,
    val enabled: Boolean = true
) {
    fun normalized(): ParametricEqBand? {
        if (!frequencyHz.isFinite() || !gainDb.isFinite() || !q.isFinite()) return null
        if (frequencyHz !in ParametricEqualizer.MIN_FREQUENCY_HZ..ParametricEqualizer.MAX_FREQUENCY_HZ) return null
        if (q !in ParametricEqualizer.MIN_Q..ParametricEqualizer.MAX_Q) return null
        if (gainDb !in -ParametricEqualizer.MAX_GAIN_DB..ParametricEqualizer.MAX_GAIN_DB) return null
        return this
    }
}

data class ParametricEqProfile(
    val id: String,
    val name: String,
    val preampDb: Float,
    val bands: List<ParametricEqBand>
) {
    fun normalized(): ParametricEqProfile? {
        val cleanId = id.trim().take(ParametricEqualizer.MAX_ID_CHARS)
        val cleanName = name.trim().take(ParametricEqualizer.MAX_NAME_CHARS)
        if (cleanId.isEmpty() || cleanName.isEmpty() || !preampDb.isFinite()) return null
        if (bands.isEmpty() || bands.size > ParametricEqualizer.MAX_BANDS) return null
        val normalizedBands = bands.map { it.normalized() ?: return null }
        return copy(
            id = cleanId,
            name = cleanName,
            preampDb = preampDb.coerceIn(ParametricEqualizer.MIN_PREAMP_DB, ParametricEqualizer.MAX_PREAMP_DB),
            bands = normalizedBands
        )
    }
}

object ParametricEqualizer {
    const val MAX_BANDS = 20
    const val MAX_CUSTOM_PROFILES = 24
    const val MAX_NAME_CHARS = 64
    const val MAX_ID_CHARS = 96
    const val MIN_FREQUENCY_HZ = 10f
    const val MAX_FREQUENCY_HZ = 96_000f
    const val MIN_Q = 0.1f
    const val MAX_Q = 20f
    const val MAX_GAIN_DB = 24f
    const val MIN_PREAMP_DB = -24f
    const val MAX_PREAMP_DB = 6f
    const val CUSTOM_PROFILE_PREFIX = "parametric_custom_"
    const val IMPORTED_PROFILE_PREFIX = "parametric_imported_"

    val defaultProfile = ParametricEqProfile(
        id = "parametric_flat",
        name = "Parametric EQ",
        preampDb = 0f,
        bands = listOf(
            ParametricEqBand(100f, 0f, 0.71f, ParametricFilterType.LOW_SHELF),
            ParametricEqBand(1_000f, 0f, 1f, ParametricFilterType.PEAK),
            ParametricEqBand(10_000f, 0f, 0.71f, ParametricFilterType.HIGH_SHELF)
        )
    )

    fun profileId(prefix: String, name: String, preampDb: Float, bands: List<ParametricEqBand>): String {
        val stable = buildString {
            append(name.trim()).append('|').append(preampDb)
            bands.forEach { band ->
                append('|').append(band.enabled)
                append(':').append(band.filterType.autoEqCode)
                append(':').append(band.frequencyHz)
                append(':').append(band.gainDb)
                append(':').append(band.q)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(16)
        return prefix + digest
    }
}

object ParametricBiquad {
    const val COEFFICIENT_COUNT = 5

    fun design(band: ParametricEqBand, sampleRate: Int, out: DoubleArray): Boolean {
        val frequency = band.frequencyHz.toDouble()
        val q = band.q.toDouble()
        if (sampleRate <= 0 || frequency <= 0.0 || frequency >= sampleRate * 0.5 || q <= 0.0) return false
        val amplitude = 10.0.pow(band.gainDb.toDouble() / 40.0)
        val omega = 2.0 * PI * frequency / sampleRate
        val cosine = cos(omega)
        val alpha = sin(omega) / (2.0 * q)
        when (band.filterType) {
            ParametricFilterType.PEAK -> {
                val a0 = 1.0 + alpha / amplitude
                out[0] = (1.0 + alpha * amplitude) / a0
                out[1] = -2.0 * cosine / a0
                out[2] = (1.0 - alpha * amplitude) / a0
                out[3] = -2.0 * cosine / a0
                out[4] = (1.0 - alpha / amplitude) / a0
            }
            ParametricFilterType.LOW_SHELF, ParametricFilterType.HIGH_SHELF -> {
                val sign = if (band.filterType == ParametricFilterType.HIGH_SHELF) 1.0 else -1.0
                val plus = amplitude + 1.0
                val minus = amplitude - 1.0
                val beta = 2.0 * sqrt(amplitude) * alpha
                val a0 = plus - sign * minus * cosine + beta
                out[0] = amplitude * (plus + sign * minus * cosine + beta) / a0
                out[1] = -sign * 2.0 * amplitude * (minus + sign * plus * cosine) / a0
                out[2] = amplitude * (plus + sign * minus * cosine - beta) / a0
                out[3] = sign * 2.0 * (minus - sign * plus * cosine) / a0
                out[4] = (plus - sign * minus * cosine - beta) / a0
            }
        }
        return out.all { it.isFinite() }
    }

    fun responseDb(profile: ParametricEqProfile, frequenciesHz: FloatArray, sampleRate: Int, out: FloatArray) {
        val coefficients = DoubleArray(COEFFICIENT_COUNT)
        for (index in frequenciesHz.indices) out[index] = profile.preampDb
        profile.bands.forEach { band ->
            if (!band.enabled || !design(band, sampleRate, coefficients)) return@forEach
            for (index in frequenciesHz.indices) {
                out[index] += magnitudeDb(coefficients, frequenciesHz[index].toDouble(), sampleRate).toFloat()
            }
        }
    }

    private fun magnitudeDb(c: DoubleArray, frequencyHz: Double, sampleRate: Int): Double {
        val omega = 2.0 * PI * frequencyHz / sampleRate
        val cos1 = cos(omega)
        val sin1 = sin(omega)
        val cos2 = cos(2.0 * omega)
        val sin2 = sin(2.0 * omega)
        val numeratorReal = c[0] + c[1] * cos1 + c[2] * cos2
        val numeratorImaginary = -(c[1] * sin1 + c[2] * sin2)
        val denominatorReal = 1.0 + c[3] * cos1 + c[4] * cos2
        val denominatorImaginary = -(c[3] * sin1 + c[4] * sin2)
        val numerator = numeratorReal * numeratorReal + numeratorImaginary * numeratorImaginary
        val denominator = denominatorReal * denominatorReal + denominatorImaginary * denominatorImaginary
        if (numerator <= 0.0 || denominator <= 0.0) return 0.0
        return 10.0 * log10(numerator / denominator)
    }
}

object ParametricProfiles {
    const val EDITOR_MIN_FREQUENCY_HZ = 20f
    const val EDITOR_MAX_FREQUENCY_HZ = 20_000f

    fun isCustom(profile: ParametricEqProfile): Boolean =
        profile.id.startsWith(ParametricEqualizer.CUSTOM_PROFILE_PREFIX)

    fun newId(): String = ParametricEqualizer.CUSTOM_PROFILE_PREFIX + UUID.randomUUID().toString().replace("-", "")

    fun create(name: String): ParametricEqProfile =
        ParametricEqualizer.defaultProfile.copy(id = newId(), name = cleanName(name))

    fun duplicate(source: ParametricEqProfile, name: String): ParametricEqProfile =
        source.copy(id = newId(), name = cleanName(name), bands = source.bands.map { it.copy() })

    fun cleanName(name: String): String = name.trim().take(ParametricEqualizer.MAX_NAME_CHARS)

    fun nameTaken(name: String, selfId: String?, profiles: List<ParametricEqProfile>): Boolean {
        val key = cleanName(name).lowercase(Locale.ROOT)
        return key.isNotEmpty() && profiles.any { it.id != selfId && it.name.trim().lowercase(Locale.ROOT) == key }
    }

    fun availableName(base: String, profiles: List<ParametricEqProfile>, variant: (Int) -> String): String {
        if (!nameTaken(base, null, profiles)) return cleanName(base)
        var index = 2
        while (nameTaken(variant(index), null, profiles)) index += 1
        return cleanName(variant(index))
    }

    fun upsert(profiles: List<ParametricEqProfile>, profile: ParametricEqProfile): List<ParametricEqProfile> {
        val index = profiles.indexOfFirst { it.id == profile.id }
        return if (index >= 0) {
            profiles.toMutableList().apply { this[index] = profile }
        } else {
            (profiles + profile).takeLast(ParametricEqualizer.MAX_CUSTOM_PROFILES)
        }
    }

    fun parseDecimal(text: String): Float? {
        val value = text.trim().replace(',', '.').toFloatOrNull() ?: return null
        return value.takeIf { it.isFinite() }
    }

    fun validFrequency(value: Float): Boolean =
        value.isFinite() && value in EDITOR_MIN_FREQUENCY_HZ..EDITOR_MAX_FREQUENCY_HZ

    fun validGain(value: Float): Boolean =
        value.isFinite() && value in -ParametricEqualizer.MAX_GAIN_DB..ParametricEqualizer.MAX_GAIN_DB

    fun validQ(value: Float): Boolean =
        value.isFinite() && value in ParametricEqualizer.MIN_Q..ParametricEqualizer.MAX_Q

    fun validPreamp(value: Float): Boolean =
        value.isFinite() && value in ParametricEqualizer.MIN_PREAMP_DB..ParametricEqualizer.MAX_PREAMP_DB
}
