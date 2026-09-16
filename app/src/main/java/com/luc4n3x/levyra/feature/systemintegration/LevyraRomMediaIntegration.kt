package com.luc4n3x.levyra.feature.systemintegration

import android.os.Build
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONObject

internal const val OPLUS_LYRIC_INFO_KEY = "lyricInfo"
private const val MAX_SYSTEM_LYRICS_BYTES = 384 * 1024

internal enum class LevyraRomFamily {
    OPLUS,
    GENERIC
}

internal data class LevyraRomMediaCapabilities(
    val family: LevyraRomFamily,
    val timedLyricsMetadata: Boolean
)

internal fun detectLevyraRomMediaCapabilities(
    manufacturer: String = Build.MANUFACTURER.orEmpty(),
    brand: String = Build.BRAND.orEmpty()
): LevyraRomMediaCapabilities {
    val identity = "$manufacturer $brand".lowercase(Locale.ROOT)
    val oplus = listOf("oppo", "oneplus", "realme").any(identity::contains)
    return LevyraRomMediaCapabilities(
        family = if (oplus) LevyraRomFamily.OPLUS else LevyraRomFamily.GENERIC,
        timedLyricsMetadata = oplus
    )
}

internal fun buildOPlusLyricsPayload(
    track: Track,
    lines: List<LyricLine>,
    synced: Boolean,
    provider: String,
    packageName: String,
    generation: Long
): String? {
    if (!synced || generation <= 0L) return null
    val timedLines = lines
        .asSequence()
        .filterNot { it.isMetadata || it.isInstrumental }
        .filter { it.startMs >= 0L && it.text.isNotBlank() }
        .sortedBy(LyricLine::startMs)
        .toList()
    if (timedLines.isEmpty()) return null

    val lyric = timedLines.joinToString(separator = "\n", postfix = "\n") { line ->
        "${lrcTimestamp(line.startMs)}${cleanSystemLyricText(line.text)}"
    }
    val rawLyric = buildWordTimedLane(timedLines)
    val translation = timedLines
        .filter { it.translated.isNotBlank() && it.translated.trim() != it.text.trim() }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n", postfix = "\n") { line ->
            "${lrcTimestamp(line.startMs)}${cleanSystemLyricText(line.translated)}"
        }

    val mediaId = track.id.ifBlank {
        track.videoUrl.ifBlank { "${track.artist}-${track.title}" }
    }
    val payload = JSONObject()
        .put("songName", track.title)
        .put("artist", track.artist)
        .put("songId", mediaId)
        .put("lyricType", 0)
        .put("lyric", lyric)
        .put("provider", packageName)
        .put("source", "levyra:${provider.ifBlank { "automatic" }}")
        .put(
            "trackKey",
            listOf(
                mediaId,
                track.title.trim().lowercase(Locale.ROOT),
                track.artist.trim().lowercase(Locale.ROOT),
                (track.durationMs.coerceAtLeast(0L) / 1_000L).toString()
            ).joinToString("|")
        )
        .put("sessionGeneration", generation)
        .put("noLyric", false)
        .apply {
            track.album.trim().takeIf(String::isNotBlank)?.let { put("album", it) }
            rawLyric?.let { put("rawLyric", it) }
            translation?.let { put("translationLyric", it) }
        }
        .toString()

    return payload.takeIf {
        it.toByteArray(StandardCharsets.UTF_8).size <= MAX_SYSTEM_LYRICS_BYTES
    }
}

private fun buildWordTimedLane(lines: List<LyricLine>): String? {
    val wordTimed = lines.mapNotNull { line ->
        val words = line.words
            .filter { it.startMs >= line.startMs && it.endMs >= it.startMs && it.text.isNotBlank() }
            .sortedBy { it.startMs }
        if (words.isEmpty()) return@mapNotNull null
        var previous = line.startMs
        val body = buildString {
            append(lrcTimestamp(line.startMs))
            words.forEach { word ->
                val start = word.startMs.coerceAtLeast(previous)
                append(wordTimestamp(start))
                append(cleanSystemLyricText(word.text))
                previous = start
            }
            val terminal = words.last().endMs.coerceAtLeast(previous)
            if (terminal > previous) append(wordTimestamp(terminal))
        }
        body
    }
    return wordTimed.takeIf(List<String>::isNotEmpty)?.joinToString(separator = "\n", postfix = "\n")
}

private fun lrcTimestamp(positionMs: Long): String {
    val safe = positionMs.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val millis = safe % 1_000L
    return "[${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}]"
}

private fun wordTimestamp(positionMs: Long): String =
    lrcTimestamp(positionMs).replace('[', '<').replace(']', '>')

private fun cleanSystemLyricText(value: String): String =
    value.replace('\n', ' ').replace('\r', ' ').trim()
