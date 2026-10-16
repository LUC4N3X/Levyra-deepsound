package com.luc4n3x.levyra.feature.systemintegration

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.player.playerLyricsCardLines
import kotlin.math.ceil
import org.json.JSONObject

private const val ISLAND_TEXT_MAX_CHARS = 42
private const val ISLAND_AOD_MAX_CHARS = 20
private const val ISLAND_BOUNDARY_SLACK_MS = 24L
private const val ISLAND_MIN_TICK_MS = 80L
private const val FOCUS_PROGRESS_COLOR = "#FF757575"
private const val FOCUS_PROGRESS_TRACK_COLOR = "#333333"
private const val ISLAND_PLACEHOLDER = "♪"
private const val VIVO_CHRONOMETER_TOLERANCE_MS = 2_000L

internal data class LevyraIslandLyrics(
    val mediaId: String,
    val trackId: String,
    val lines: List<LyricLine>
)

internal data class LevyraIslandLyricFrame(
    val lineIndex: Int,
    val current: String,
    val next: String,
    val secondary: String,
    val nextBoundaryMs: Long?
)

internal data class LevyraIslandSnapshot(
    val mediaId: String,
    val title: String,
    val artist: String,
    val active: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val speed: Float,
    val lyric: LevyraIslandLyricFrame?
) {
    val progressPercent: Int
        get() = if (durationMs > 0L) {
            (positionMs.coerceIn(0L, durationMs) * 100L / durationMs).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val songLabel: String
        get() = listOf(title.trim(), artist.trim()).filter(String::isNotBlank).joinToString(" · ")
}

internal fun levyraIslandLyrics(
    mediaId: String,
    trackId: String,
    lines: List<LyricLine>,
    synced: Boolean
): LevyraIslandLyrics? {
    if (!synced || mediaId.isBlank()) return null
    val timed = playerLyricsCardLines(lines)
        .filter { it.startMs >= 0L && !it.isInstrumental }
        .sortedBy(LyricLine::startMs)
    return timed.takeIf(List<LyricLine>::isNotEmpty)?.let { LevyraIslandLyrics(mediaId, trackId, it) }
}

internal fun levyraIslandLyricFrame(lines: List<LyricLine>, lyricsPositionMs: Long): LevyraIslandLyricFrame? {
    if (lines.isEmpty()) return null
    val index = activeLyricIndex(lyricsPositionMs, lines)
    val line = lines.getOrNull(index)
    val nextLine = lines.getOrNull(index + 1)
    val boundary = lines.firstOrNull { it.startMs > lyricsPositionMs }?.startMs
    return LevyraIslandLyricFrame(
        lineIndex = index,
        current = line?.text?.let(::cleanLyricText).orEmpty(),
        next = nextLine?.text?.let(::cleanLyricText).orEmpty(),
        secondary = line?.let(::secondaryLyricText).orEmpty(),
        nextBoundaryMs = boundary
    )
}

internal fun levyraIslandTickDelayMs(lyricsPositionMs: Long, boundaryMs: Long?, speed: Float): Long? {
    if (boundaryMs == null || boundaryMs <= lyricsPositionMs) return null
    val safeSpeed = speed.takeIf { it.isFinite() && it > 0f } ?: 1f
    val realMs = ceil((boundaryMs - lyricsPositionMs) / safeSpeed.toDouble()).toLong()
    return (realMs + ISLAND_BOUNDARY_SLACK_MS).coerceAtLeast(ISLAND_MIN_TICK_MS)
}

internal data class HyperOsSuperIslandContent(
    val primary: String,
    val upcoming: String,
    val detail: String
)

internal fun hyperOsSuperIslandContent(snapshot: LevyraIslandSnapshot): HyperOsSuperIslandContent {
    val lyric = snapshot.lyric
    val title = snapshot.title.trim()
    val artist = snapshot.artist.trim()
    val current = lyric?.current.orEmpty()
    val primary = current.ifBlank { title }.ifBlank { ISLAND_PLACEHOLDER }
    val upcoming = if (current.isNotBlank()) {
        lyric?.next.orEmpty().ifBlank { title }
    } else {
        lyric?.next.orEmpty().ifBlank { artist }
    }.ifBlank { primary }
    val detail = if (current.isNotBlank()) {
        lyric?.secondary.orEmpty().ifBlank { snapshot.songLabel }
    } else {
        artist
    }.ifBlank { ISLAND_PLACEHOLDER }
    return HyperOsSuperIslandContent(primary, upcoming, detail)
}

internal fun hyperOsSuperIslandFocusParam(content: HyperOsSuperIslandContent, progress: Int, packageName: String): String {
    val current = content.primary
    val upcoming = content.upcoming
    val detail = content.detail
    val param = JSONObject()
        .put("ticker", current)
        .put("updatable", true)
        .put("enableFloat", false)
        .put("aodTitle", current.take(ISLAND_AOD_MAX_CHARS))
        .put("isShowNotification", true)
        .put("islandFirstFloat", false)
        .put("business", "lyric_display")
        .put(
            "chatInfo",
            JSONObject()
                .put("title", current)
                .put("content", detail)
                .put("appIconPkg", packageName)
        )
        .put(
            "progressInfo",
            JSONObject()
                .put("progress", progress)
                .put("colorProgress", FOCUS_PROGRESS_COLOR)
                .put("colorProgressEnd", FOCUS_PROGRESS_COLOR)
        )
        .put(
            "param_island",
            JSONObject()
                .put("islandProperty", 1)
                .put(
                    "bigIslandArea",
                    JSONObject()
                        .put(
                            "imageTextInfoLeft",
                            JSONObject()
                                .put("type", 1)
                                .put("textInfo", focusText(current.take(ISLAND_TEXT_MAX_CHARS)))
                        )
                        .put("textInfo", focusText(upcoming.take(ISLAND_TEXT_MAX_CHARS)))
                )
                .put(
                    "smallIslandArea",
                    JSONObject().put(
                        "combinePicInfo",
                        JSONObject().put(
                            "progressInfo",
                            JSONObject()
                                .put("progress", progress)
                                .put("colorReach", FOCUS_PROGRESS_COLOR)
                                .put("colorUnReach", FOCUS_PROGRESS_TRACK_COLOR)
                        )
                    )
                )
        )
    return JSONObject().put("param_v2", param).toString()
}

internal fun hyperOsSuperIslandKey(snapshot: LevyraIslandSnapshot, content: HyperOsSuperIslandContent): String =
    listOf(
        snapshot.mediaId,
        snapshot.active.toString(),
        content.primary,
        content.upcoming,
        content.detail,
        (snapshot.progressPercent / 5).toString()
    ).joinToString("\u0000")

internal enum class VivoAtomicIslandOperation(val code: Int) {
    ADD(0),
    UPDATE(1),
    REMOVE(2)
}

internal data class VivoAtomicIslandContent(
    val operation: VivoAtomicIslandOperation,
    val title: String,
    val content: String,
    val playing: Boolean,
    val elapsedMs: Long,
    val chronometer: Boolean = false
)

internal fun vivoAtomicIslandKey(snapshot: LevyraIslandSnapshot, nowMs: Long): String {
    val anchor = if (vivoUsesChronometer(snapshot)) {
        (nowMs - snapshot.positionMs) / VIVO_CHRONOMETER_TOLERANCE_MS
    } else {
        snapshot.positionMs
    }
    return listOf(snapshot.mediaId, snapshot.title, snapshot.artist, snapshot.active, anchor).joinToString("\u0000")
}

private fun vivoUsesChronometer(snapshot: LevyraIslandSnapshot): Boolean =
    snapshot.active && snapshot.speed == 1f

internal fun vivoAtomicIslandContent(
    snapshot: LevyraIslandSnapshot,
    activeMediaId: String?
): VivoAtomicIslandContent = VivoAtomicIslandContent(
    operation = if (activeMediaId == snapshot.mediaId) VivoAtomicIslandOperation.UPDATE else VivoAtomicIslandOperation.ADD,
    title = snapshot.title.trim().ifBlank { ISLAND_PLACEHOLDER },
    content = snapshot.artist.trim().ifBlank { ISLAND_PLACEHOLDER },
    playing = snapshot.active,
    elapsedMs = snapshot.positionMs.coerceAtLeast(0L),
    chronometer = vivoUsesChronometer(snapshot)
)

private fun focusText(title: String): JSONObject = JSONObject()
    .put("title", title)
    .put("showHighlightColor", false)
    .put("narrowFont", false)

private fun secondaryLyricText(line: LyricLine): String {
    val primary = line.text.trim()
    return listOf(line.translated, line.romanized)
        .map(String::trim)
        .firstOrNull { it.isNotBlank() && it != primary }
        ?.let(::cleanLyricText)
        .orEmpty()
}

private fun cleanLyricText(value: String): String =
    value.replace('\n', ' ').replace('\r', ' ').trim()
