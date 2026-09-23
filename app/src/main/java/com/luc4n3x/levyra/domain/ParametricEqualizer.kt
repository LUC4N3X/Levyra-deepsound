package com.luc4n3x.levyra.domain

import java.security.MessageDigest

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
