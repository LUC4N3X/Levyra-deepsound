package com.luc4n3x.levyra.ui

import android.media.AudioDeviceInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputState
import com.luc4n3x.levyra.feature.audio.openLevyraSystemOutputSwitcher
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.util.Locale

private val OutputHeroShape = RoundedCornerShape(24.dp)

@Composable
internal fun LevyraOutputHub(
    output: LevyraAudioOutputState,
    track: Track?,
    audioSettings: LevyraAudioSettings
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    val context = LocalContext.current
    val route = output.active
    val stream = track?.playbackManifest?.streams
        ?.firstOrNull { descriptor ->
            descriptor.selected && descriptor.kind in setOf(PlaybackStreamKind.AUDIO, PlaybackStreamKind.MUXED)
        }
    val routeSummary = outputRouteLabel(route, copy)
    val streamDetails = buildStreamDetails(
        stream?.codec,
        stream?.bitrate ?: 0,
        stream?.averageBitrate ?: 0,
        stream?.sampleRate ?: 0
    )
    val dspActive = audioSettings.equalizerEnabled ||
        audioSettings.replayGainEnabled ||
        audioSettings.virtualizer > 0 ||
        audioSettings.preampDb != 0f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = copy.outputTitle.uppercase(Locale.ROOT),
            color = LevyraMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            LevyraCyan.copy(alpha = 0.14f),
                            LevyraViolet.copy(alpha = 0.08f),
                            LevyraCyan.copy(alpha = 0.025f)
                        )
                    ),
                    shape = OutputHeroShape
                )
                .border(BorderStroke(1.dp, LevyraGlassBorder), OutputHeroShape)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = LevyraCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.24f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = outputRouteIcon(route),
                                contentDescription = null,
                                tint = LevyraCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = copy.currentOutput,
                            color = LevyraCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = route?.displayName?.ifBlank { routeSummary } ?: routeSummary,
                            color = LevyraText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$routeSummary · ${output.volumePercent}%",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutputMetric(
                        label = copy.streamQuality,
                        value = streamDetails.ifBlank { track?.source?.ifBlank { "Auto" } ?: "Auto" },
                        modifier = Modifier.weight(1f)
                    )
                    OutputMetric(
                        label = "DSP",
                        value = if (dspActive) copy.dspActive else copy.dspOff,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = copy.systemManaged,
                        color = LevyraMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { openLevyraSystemOutputSwitcher(context) },
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Text(copy.chooseOutput, color = LevyraCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (output.connected.size > 1) {
            Text(
                text = output.connected
                    .filterNot { candidate ->
                        candidate.stableKey != null && candidate.stableKey == route?.stableKey
                    }
                    .take(2)
                    .joinToString(" · ") { it.displayName },
                color = LevyraMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun OutputMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = LevyraText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun outputRouteLabel(
    route: LevyraAudioOutputRoute?,
    copy: com.luc4n3x.levyra.ui.i18n.LevyraSystemPlayerCopy
): String = when {
    route == null -> copy.speaker
    route.bluetooth -> copy.bluetooth
    route.wired -> copy.wired
    route.external -> copy.external
    route.speaker -> copy.speaker
    else -> copy.systemManaged
}

private fun outputRouteIcon(route: LevyraAudioOutputRoute?): ImageVector = when {
    route == null -> Icons.Rounded.PhoneAndroid
    route.bluetooth || route.wired -> Icons.Rounded.Headphones
    route.external || route.type in setOf(
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC
    ) -> Icons.Rounded.Usb
    route.speaker -> Icons.Rounded.Speaker
    else -> Icons.Rounded.PhoneAndroid
}

internal fun buildStreamDetails(codec: String?, bitrate: Int, averageBitrate: Int, sampleRate: Int): String {
    val parts = mutableListOf<String>()
    codec?.trim()?.takeIf(String::isNotBlank)?.let { raw ->
        parts += raw.substringBefore('.').uppercase(Locale.ROOT)
    }
    val effectiveBitrate = averageBitrate.coerceAtLeast(bitrate)
    if (effectiveBitrate > 0) parts += "${effectiveBitrate / 1000} kbps"
    if (sampleRate > 0) {
        val khz = sampleRate / 1000f
        parts += if (sampleRate % 1000 == 0) {
            "${sampleRate / 1000} kHz"
        } else {
            String.format(Locale.ROOT, "%.1f kHz", khz)
        }
    }
    return parts.joinToString(" · ")
}
