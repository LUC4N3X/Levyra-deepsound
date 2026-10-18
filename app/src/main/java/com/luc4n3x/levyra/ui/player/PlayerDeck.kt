package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import java.util.Locale

internal enum class PlayerDeckLayout {
    Levyra,
    Aura,
    Expressive,
    Poster,
    Pulse,
    Halo,
    Folio,
    Flow,
    Sleeve
}

internal val PlayerDeckOrder: List<PlayerVisualMode> = listOf(
    PlayerVisualMode.Flow,
    PlayerVisualMode.CanvasCard,
    PlayerVisualMode.CanvasImmersive,
    PlayerVisualMode.Halo,
    PlayerVisualMode.Folio,
    PlayerVisualMode.Expressive,
    PlayerVisualMode.Artwork,
    PlayerVisualMode.Pulse
)

internal fun PlayerVisualMode.deckLayout(): PlayerDeckLayout = when (this) {
    PlayerVisualMode.CanvasCard -> PlayerDeckLayout.Sleeve
    PlayerVisualMode.CanvasImmersive -> PlayerDeckLayout.Aura
    PlayerVisualMode.Artwork -> PlayerDeckLayout.Poster
    PlayerVisualMode.Expressive -> PlayerDeckLayout.Expressive
    PlayerVisualMode.Pulse -> PlayerDeckLayout.Pulse
    PlayerVisualMode.Halo -> PlayerDeckLayout.Halo
    PlayerVisualMode.Folio -> PlayerDeckLayout.Folio
    PlayerVisualMode.Flow -> PlayerDeckLayout.Flow
}

internal fun PlayerVisualMode.usesAuraPlayPause(): Boolean =
    deckLayout() == PlayerDeckLayout.Aura

internal fun PlayerDeckLayout.showsLyricsFace(): Boolean =
    this != PlayerDeckLayout.Aura && this != PlayerDeckLayout.Pulse

internal fun resolvePlayerDeckLayout(
    mode: PlayerVisualMode,
    isVideoMode: Boolean,
    isLiveRadio: Boolean,
    pane: LevyraPlayerPane,
    hasTrack: Boolean
): PlayerDeckLayout {
    val layout = mode.deckLayout()
    if (layout == PlayerDeckLayout.Levyra) return layout
    val supported = hasTrack && !isVideoMode && !isLiveRadio && pane == LevyraPlayerPane.Stacked
    return if (supported) layout else PlayerDeckLayout.Levyra
}

internal fun resolvePlayerDeckVisualMode(
    mode: PlayerVisualMode,
    layout: PlayerDeckLayout
): PlayerVisualMode = if (layout == mode.deckLayout() || mode == PlayerVisualMode.Artwork) {
    mode
} else {
    PlayerVisualMode.CanvasCard
}

@Immutable
internal data class PlayerDeckQueuePosition(
    val index: Int,
    val total: Int
) {
    val indexLabel: String get() = String.format(Locale.ROOT, "%02d", index + 1)
    val totalLabel: String get() = String.format(Locale.ROOT, "%02d", total)
}

internal fun playerDeckQueuePosition(queue: List<Track>, trackId: String): PlayerDeckQueuePosition? {
    if (queue.size < 2 || trackId.isBlank()) return null
    val index = queue.indexOfFirst { it.id == trackId }
    return if (index < 0) null else PlayerDeckQueuePosition(index, queue.size)
}

internal fun playerDeckAlbumEyebrow(track: Track): String {
    val album = track.album.trim()
    return if (album.isEmpty() || album.equals(track.title.trim(), ignoreCase = true)) "" else album.uppercase()
}

internal fun expressiveTitleSize(title: String, compact: Boolean): TextUnit {
    val length = title.trim().length
    val base = when {
        length <= 12 -> 46f
        length <= 22 -> 40f
        length <= 36 -> 34f
        else -> 29f
    }
    return (if (compact) base - 6f else base).sp
}

@Immutable
internal data class PlayerAudioSpec(
    val codec: String = "",
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
    val mimeType: String = "",
    val codecString: String = ""
) {
    val isEmpty: Boolean
        get() = codec.isEmpty() && bitrateKbps == null && sampleRateHz == null && channels == null &&
            mimeType.isEmpty() && codecString.isEmpty()
}

internal fun playerAudioCodecLabel(mimeType: String?, codecs: String?): String {
    val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
    val codec = codecs.orEmpty().lowercase(Locale.ROOT)
    return when {
        mime == "audio/opus" || codec.startsWith("opus") -> "OPUS"
        mime == "audio/mp4a-latm" || codec.startsWith("mp4a") -> "AAC"
        mime == "audio/mpeg" -> "MP3"
        mime == "audio/mpeg-l2" -> "MP2"
        mime == "audio/flac" || codec == "flac" -> "FLAC"
        mime == "audio/vorbis" || codec == "vorbis" -> "VORBIS"
        mime == "audio/alac" || codec == "alac" -> "ALAC"
        mime == "audio/eac3" || mime == "audio/eac3-joc" -> "E-AC-3"
        mime == "audio/ac3" -> "AC-3"
        mime == "audio/raw" -> "PCM"
        mime.startsWith("audio/") -> mime.removePrefix("audio/").uppercase(Locale.ROOT).take(8)
        else -> ""
    }
}

internal fun playerAudioSpecLabels(spec: PlayerAudioSpec): List<String> = buildList {
    if (spec.codec.isNotEmpty()) add(spec.codec)
    spec.bitrateKbps?.takeIf { it > 0 }?.let { add("$it kbps") }
    spec.sampleRateHz?.takeIf { it > 0 }?.let { add(formatSampleRate(it)) }
    spec.channels?.takeIf { it > 0 }?.let { add(formatChannelLayout(it)) }
}

private fun formatSampleRate(sampleRateHz: Int): String {
    val khz = sampleRateHz / 1000f
    val text = if (sampleRateHz % 1000 == 0) {
        (sampleRateHz / 1000).toString()
    } else {
        String.format(Locale.ROOT, "%.1f", khz)
    }
    return "$text kHz"
}

private fun formatChannelLayout(channels: Int): String = when (channels) {
    1 -> "1.0"
    2 -> "2.0"
    6 -> "5.1"
    8 -> "7.1"
    else -> "${channels}ch"
}

@Immutable
internal class PlayerDeckSlots(
    val header: @Composable () -> Unit,
    val stage: @Composable (Track, Modifier, Dp?) -> Unit,
    val gestures: @Composable (Track, Modifier) -> Unit,
    val controls: @Composable ColumnScope.(Track) -> Unit,
    val controlsWithoutMetadata: @Composable ColumnScope.(Track) -> Unit,
    val engagement: @Composable (Track) -> Unit,
    val progress: @Composable (style: PlayerProgressStyle) -> Unit,
    val dock: @Composable (Track) -> Unit,
    val dockActions: @Composable (Track) -> List<PlayerDockAction>,
    val linerNotes: PlayerLinerNotesState? = null
)

internal val DefaultPlayerVisualMode: PlayerVisualMode = LevyraInterfaceSettings().playerVisualMode

internal val DefaultPlayerBackgroundMode: PlayerBackgroundMode = LevyraInterfaceSettings().playerBackground
