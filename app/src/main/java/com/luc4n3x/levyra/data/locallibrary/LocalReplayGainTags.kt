package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.domain.ReplayGainMetadata
import java.util.Locale

internal fun parseLocalReplayGainTags(raw: String): ReplayGainMetadata {
    if (raw.isBlank()) return ReplayGainMetadata()
    val tags = buildMap {
        raw.lineSequence().forEach { line ->
            val separator = line.indexOf('=')
            if (separator <= 0 || separator >= line.lastIndex) return@forEach
            val key = line.substring(0, separator).trim().uppercase(Locale.ROOT)
            val value = line.substring(separator + 1).trim()
            if (key.isNotBlank() && value.isNotBlank()) put(key, value)
        }
    }

    fun gain(key: String): Float? = tags[key]
        ?.removeSuffix("dB")
        ?.removeSuffix("DB")
        ?.trim()
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?.takeIf { it.isFinite() }
        ?.coerceIn(-48f, 24f)

    fun peak(key: String): Float? = tags[key]
        ?.trim()
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?.takeIf { it.isFinite() && it > 0f }
        ?.coerceAtMost(16f)

    return ReplayGainMetadata(
        trackGainDb = gain("REPLAYGAIN_TRACK_GAIN"),
        albumGainDb = gain("REPLAYGAIN_ALBUM_GAIN"),
        trackPeak = peak("REPLAYGAIN_TRACK_PEAK"),
        albumPeak = peak("REPLAYGAIN_ALBUM_PEAK")
    )
}
