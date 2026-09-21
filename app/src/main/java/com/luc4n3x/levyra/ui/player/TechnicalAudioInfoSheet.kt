@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputState
import com.luc4n3x.levyra.feature.audio.rememberLevyraAudioOutputState
import com.luc4n3x.levyra.feature.cast.RemotePlaybackState
import com.luc4n3x.levyra.player.NativeAudioIntegration
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TechnicalAudioInfoCopy
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.i18n.technicalAudioInfoCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.util.Locale

private val TechnicalAudioCardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TechnicalAudioInfoSheet(
    track: Track,
    audioSettings: LevyraAudioSettings,
    audioNormalization: Boolean,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.technicalAudioInfoCopy()
    val systemCopy = strings.systemPlayerCopy()
    val runtime by rememberPlayerAudioSpec()
    val player by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val remotePlayback by PlaybackService.remotePlaybackStateFlow.collectAsStateWithLifecycle()
    val output = rememberLevyraAudioOutputState()
    val effectiveRuntime = remember(runtime, remotePlayback.connected) {
        technicalRuntimeSpec(runtime, remotePlayback.connected)
    }
    val source = remember(track.playbackManifest) { selectedAudioDescriptor(track) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val runtimeRows = remember(effectiveRuntime, player?.audioSessionId, copy, remotePlayback.connected) {
        if (remotePlayback.connected) {
            emptyList()
        } else {
            buildList {
                add(copy.codec to effectiveRuntime.codec.ifBlank { copy.unavailable })
                effectiveRuntime.bitrateKbps?.let { add(copy.bitrate to "$it kbps") }
                effectiveRuntime.sampleRateHz?.let { add(copy.sampleRate to formatTechnicalSampleRate(it)) }
                effectiveRuntime.channels?.let { add(copy.channels to formatTechnicalChannels(it)) }
                effectiveRuntime.mimeType.takeIf(String::isNotBlank)?.let { add(copy.mime to it) }
                effectiveRuntime.codecString.takeIf(String::isNotBlank)?.let { add(copy.codecId to it) }
                player?.audioSessionId?.takeIf { it > 0 }?.let { add(copy.audioSession to it.toString()) }
            }
        }
    }

    val sourceRows = remember(track.playbackManifest, track.source, copy) {
        buildSourceRows(track, source, copy)
    }

    val outputRows = remember(
        output,
        audioSettings,
        audioNormalization,
        copy,
        systemCopy,
        remotePlayback
    ) {
        buildTechnicalOutputRows(
            output = output,
            settings = audioSettings,
            audioNormalization = audioNormalization,
            copy = copy,
            systemCopy = systemCopy,
            remotePlayback = remotePlayback
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LevyraPanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
        ) {
            Text(
                text = copy.title,
                color = LevyraText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = copy.subtitle,
                color = LevyraMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            TechnicalAudioHero(
                codec = effectiveRuntime.codec.ifBlank { technicalSourceCodec(source).orEmpty().ifBlank { copy.unavailable } },
                quality = effectiveRuntime.bitrateKbps?.let { "$it kbps" }
                    ?: technicalSourceBitrateKbps(source)?.let { "$it kbps" }
                    ?: track.playbackManifest?.alternativeSource?.bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" }
                    ?: source?.qualityLabel?.takeIf(String::isNotBlank)
                    ?: copy.unavailable,
                sampleRate = effectiveRuntime.sampleRateHz?.let(::formatTechnicalSampleRate)
                    ?: source?.sampleRate?.takeIf { it > 0 }?.let(::formatTechnicalSampleRate)
                    ?: copy.unavailable
            )

            TechnicalAudioSection(copy.runtime, runtimeRows)
            TechnicalAudioSection(copy.source, sourceRows)
            TechnicalAudioSection(copy.outputAndDsp, outputRows)
        }
    }
}

@Composable
private fun TechnicalAudioHero(codec: String, quality: String, sampleRate: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(22.dp),
        color = LevyraCyan.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TechnicalHeroMetric(codec, Modifier.weight(1f))
            TechnicalHeroMetric(quality, Modifier.weight(1f))
            TechnicalHeroMetric(sampleRate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TechnicalHeroMetric(value: String, modifier: Modifier = Modifier) {
    Text(
        text = value,
        color = LevyraText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun TechnicalAudioSection(title: String, rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Text(
        text = title,
        color = LevyraCyan,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.25.sp,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = TechnicalAudioCardShape,
        color = LevyraText.copy(alpha = 0.025f),
        border = BorderStroke(1.dp, LevyraGlassBorder.copy(alpha = 0.7f))
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                TechnicalAudioRow(row.first, row.second)
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        color = LevyraGlassBorder.copy(alpha = 0.55f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnicalAudioRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            color = LevyraText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f)
        )
    }
}

internal fun technicalRuntimeSpec(runtime: PlayerAudioSpec, remoteConnected: Boolean): PlayerAudioSpec =
    if (remoteConnected) PlayerAudioSpec() else runtime

internal fun buildTechnicalOutputRows(
    output: LevyraAudioOutputState,
    settings: LevyraAudioSettings,
    audioNormalization: Boolean,
    copy: TechnicalAudioInfoCopy,
    systemCopy: com.luc4n3x.levyra.ui.i18n.LevyraSystemPlayerCopy,
    remotePlayback: RemotePlaybackState
): List<Pair<String, String>> {
    if (remotePlayback.connected) {
        val device = remotePlayback.deviceName?.trim()?.takeIf(String::isNotBlank) ?: copy.remotePlayback
        return listOf(
            copy.output to device,
            copy.route to copy.remotePlayback,
            copy.engine to "Google Cast",
            copy.processing to copy.receiverManaged
        )
    }

    val route = output.active
    val routeType = technicalRouteLabel(route, systemCopy)
    return listOf(
        copy.output to (route?.displayName?.ifBlank { routeType } ?: routeType),
        copy.route to routeType,
        copy.volume to "${output.volumePercent}%",
        copy.engine to "Media3 · ExoPlayer",
        copy.path to technicalOutputPath(settings, copy),
        copy.processing to buildProcessingLabel(settings, audioNormalization, copy)
    )
}

internal fun selectedAudioDescriptor(track: Track): PlaybackStreamDescriptor? {
    val streams = track.playbackManifest?.streams.orEmpty()
    return streams.firstOrNull { it.selected && it.kind == PlaybackStreamKind.AUDIO }
        ?: streams.firstOrNull { it.selected && it.kind == PlaybackStreamKind.MUXED }
        ?: streams.firstOrNull { it.selected && it.kind == PlaybackStreamKind.HLS }
}

internal fun buildSourceRows(
    track: Track,
    stream: PlaybackStreamDescriptor?,
    copy: TechnicalAudioInfoCopy
): List<Pair<String, String>> {
    val manifest = track.playbackManifest
    val alternative = manifest?.alternativeSource
    return buildList {
        val provider = alternative?.providerId?.takeIf(String::isNotBlank)
            ?: manifest?.provider?.takeIf(String::isNotBlank)
            ?: track.source.takeIf(String::isNotBlank)
        provider?.let { add(copy.provider to it) }
        stream?.deliveryMethod?.name?.let { add(copy.delivery to it) }
        stream?.container?.takeIf(String::isNotBlank)?.uppercase(Locale.ROOT)?.let { add(copy.container to it) }
        technicalSourceCodec(stream)?.let { add(copy.codec to it) }
        technicalSourceBitrateKbps(stream)?.let { add(copy.bitrate to "$it kbps") }
        stream?.sampleRate?.takeIf { it > 0 }?.let { add(copy.sampleRate to formatTechnicalSampleRate(it)) }
        stream?.bitDepth?.takeIf { it > 0 }?.let { add(copy.bitDepth to "$it-bit") }
        stream?.qualityLabel?.takeIf(String::isNotBlank)?.let { add(copy.quality to it) }
        stream?.itag?.takeIf { it >= 0 }?.let { add(copy.streamId to "itag $it") }
        manifest?.loudnessDb?.takeIf { it.isFinite() }?.let {
            add(copy.loudness to String.format(Locale.ROOT, "%+.1f dB", it))
        }
        alternative?.let {
            add(copy.verifiedSource to "${it.providerId} · ${it.bitrateKbps} kbps · ${it.verdict.name}")
            add(copy.confidence to "${it.confidence.coerceIn(0, 100)}%")
        }
    }
}

internal fun buildProcessingLabel(
    settings: LevyraAudioSettings,
    audioNormalization: Boolean,
    copy: TechnicalAudioInfoCopy
): String = buildList {
    val equalizerActive = settings.equalizerEnabled
    val replayGainActive = settings.replayGainActive
    val virtualizerActive = equalizerActive && settings.virtualizer > 0
    val preampActive = equalizerActive && settings.preampDb != 0f
    val limiterActive = settings.limiterEnabled &&
        (equalizerActive || virtualizerActive || replayGainActive || audioNormalization)

    if (audioNormalization) add(copy.normalization)
    if (equalizerActive) add(copy.equalizer)
    if (replayGainActive) add("ReplayGain ${settings.effectiveReplayGainMode.name}")
    if (limiterActive) add(copy.limiter)
    if (virtualizerActive) add("${copy.virtualizer} ${settings.virtualizer}%")
    if (preampActive) {
        add("${copy.preamp} ${String.format(Locale.ROOT, "%+.1f dB", settings.preampDb)}")
    }
}.ifEmpty { listOf(copy.none) }.joinToString(" · ")

internal fun technicalOutputPath(
    settings: LevyraAudioSettings,
    copy: TechnicalAudioInfoCopy,
    nativeSupported: Boolean = NativeAudioIntegration.isAaudioOutputSupported()
): String = when {
    settings.aaudioOutputEnabled && nativeSupported -> "AAudio / Oboe · ${copy.requested}"
    settings.aaudioOutputEnabled -> "AudioTrack · ${copy.fallback}"
    else -> "AudioTrack"
}

private fun technicalRouteLabel(
    route: LevyraAudioOutputRoute?,
    copy: com.luc4n3x.levyra.ui.i18n.LevyraSystemPlayerCopy
): String = when {
    route == null -> copy.systemManaged
    route.bluetooth -> copy.bluetooth
    route.wired -> copy.wired
    route.external -> copy.external
    route.speaker -> copy.speaker
    else -> copy.systemManaged
}

private fun PlaybackStreamDescriptor.effectiveBitrateKbps(): Int? =
    averageBitrate.takeIf { it > 0 }?.div(1_000)
        ?: bitrate.takeIf { it > 0 }?.div(1_000)

internal fun technicalSourceBitrateKbps(stream: PlaybackStreamDescriptor?): Int? =
    stream
        ?.takeUnless { it.kind == PlaybackStreamKind.MUXED || it.kind == PlaybackStreamKind.VIDEO }
        ?.effectiveBitrateKbps()

internal fun technicalSourceCodec(stream: PlaybackStreamDescriptor?): String? {
    val codec = stream?.codec?.trim()?.takeIf(String::isNotBlank) ?: return null
    if (stream.kind != PlaybackStreamKind.MUXED) return codec
    return codec.split(',')
        .asSequence()
        .map(String::trim)
        .firstOrNull { value ->
            val normalized = value.lowercase(Locale.ROOT)
            AUDIO_CODEC_PREFIXES.any(normalized::startsWith)
        }
}

private val AUDIO_CODEC_PREFIXES = listOf(
    "mp4a",
    "aac",
    "opus",
    "vorbis",
    "flac",
    "alac",
    "ac-3",
    "ec-3",
    "mp3"
)

internal fun formatTechnicalSampleRate(sampleRateHz: Int): String {
    if (sampleRateHz <= 0) return ""
    return if (sampleRateHz % 1_000 == 0) {
        "${sampleRateHz / 1_000} kHz"
    } else {
        String.format(Locale.ROOT, "%.1f kHz", sampleRateHz / 1_000f)
    }
}

internal fun formatTechnicalChannels(channels: Int): String = when (channels) {
    1 -> "1.0"
    2 -> "2.0"
    6 -> "5.1"
    8 -> "7.1"
    else -> "${channels.coerceAtLeast(0)} ch"
}
