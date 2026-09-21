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
import com.luc4n3x.levyra.feature.audio.rememberLevyraAudioOutputState
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
    val output = rememberLevyraAudioOutputState()
    val source = remember(track.playbackManifest) { selectedAudioDescriptor(track) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val runtimeRows = remember(runtime, player?.audioSessionId, copy) {
        buildList {
            add(copy.codec to runtime.codec.ifBlank { copy.unavailable })
            runtime.bitrateKbps?.let { add(copy.bitrate to "$it kbps") }
            runtime.sampleRateHz?.let { add(copy.sampleRate to formatTechnicalSampleRate(it)) }
            runtime.channels?.let { add(copy.channels to formatTechnicalChannels(it)) }
            runtime.mimeType.takeIf(String::isNotBlank)?.let { add(copy.mime to it) }
            runtime.codecString.takeIf(String::isNotBlank)?.let { add("Codec ID" to it) }
            player?.audioSessionId?.takeIf { it > 0 }?.let { add(copy.audioSession to it.toString()) }
        }
    }

    val sourceRows = remember(track.playbackManifest, track.source, copy) {
        buildSourceRows(track, source, copy)
    }

    val processing = remember(audioSettings, audioNormalization, copy) {
        buildProcessingLabel(audioSettings, audioNormalization, copy)
    }
    val route = output.active
    val routeType = remember(route, systemCopy) {
        technicalRouteLabel(route, systemCopy)
    }
    val outputRows = buildList {
        add(copy.output to (route?.displayName?.ifBlank { routeType } ?: routeType))
        add(copy.route to routeType)
        add(copy.volume to "${output.volumePercent}%")
        add(copy.engine to "Media3 · ExoPlayer")
        add(copy.path to technicalOutputPath(audioSettings, copy))
        add(copy.processing to processing)
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
                codec = runtime.codec.ifBlank { source?.codec.orEmpty().ifBlank { copy.unavailable } },
                quality = runtime.bitrateKbps?.let { "$it kbps" }
                    ?: source?.effectiveBitrateKbps()?.let { "$it kbps" }
                    ?: source?.qualityLabel?.takeIf(String::isNotBlank)
                    ?: copy.unavailable,
                sampleRate = runtime.sampleRateHz?.let(::formatTechnicalSampleRate)
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
        stream?.codec?.takeIf(String::isNotBlank)?.let { add(copy.codec to it) }
        stream?.effectiveBitrateKbps()?.let { add(copy.bitrate to "$it kbps") }
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
