package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Composable
internal fun PlayerQuickActions(
    visualMode: PlayerVisualMode,
    motionCanvasAvailable: Boolean,
    showLyrics: Boolean,
    isDownloaded: Boolean,
    isExporting: Boolean,
    equalizerActive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    compact: Boolean,
    queueLabel: String,
    lyricsLabel: String,
    visualModeLabel: String,
    downloadLabel: String,
    equalizerLabel: String,
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onCycleVisualMode: () -> Unit,
    onDownloadClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualModeIcon: ImageVector = when (visualMode) {
        PlayerVisualMode.Artwork -> Icons.Rounded.Image
        PlayerVisualMode.CanvasCard -> Icons.Rounded.AutoAwesome
        PlayerVisualMode.CanvasImmersive -> Icons.Rounded.Fullscreen
    }
    val visualModeActive = visualMode != PlayerVisualMode.Artwork

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LevyraPlayerDesign.MinimumTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LevyraPlayerDesign.MinimumTouchTarget)
                .padding(horizontal = if (compact) 8.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerQuickActionButton(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = queueLabel,
                tint = Color.White.copy(alpha = 0.82f),
                active = false,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onQueueClick
            )
            PlayerQuickActionButton(
                icon = Icons.AutoMirrored.Rounded.Subject,
                contentDescription = lyricsLabel,
                tint = if (showLyrics) primaryColor else Color.White.copy(alpha = 0.82f),
                active = showLyrics,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onLyricsClick
            )
            if (motionCanvasAvailable) {
                PlayerQuickActionButton(
                    icon = visualModeIcon,
                    contentDescription = visualModeLabel,
                    tint = if (visualModeActive) primaryColor else Color.White.copy(alpha = 0.75f),
                    active = visualModeActive,
                    toggleableState = ToggleableState(visualModeActive),
                    compact = compact,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = onCycleVisualMode
                )
            }
            PlayerQuickActionButton(
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                contentDescription = downloadLabel,
                tint = if (isExporting || isDownloaded) secondaryColor else Color.White.copy(alpha = 0.82f),
                active = isExporting || isDownloaded,
                busy = isExporting,
                enabled = !isExporting,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onDownloadClick
            )
            PlayerQuickActionButton(
                icon = Icons.Rounded.Equalizer,
                contentDescription = equalizerLabel,
                tint = if (equalizerActive) primaryColor else Color.White.copy(alpha = 0.82f),
                active = equalizerActive,
                toggleableState = ToggleableState(equalizerActive),
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onEqualizerClick
            )
        }
    }
}

@Composable
private fun PlayerQuickActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    active: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    toggleableState: ToggleableState? = null,
    busy: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val containerAlpha by animateFloatAsState(
        targetValue = if (active) 0.20f else 0.06f,
        animationSpec = if (animationsEnabled) LevyraPlayerDesign.standardTween(170) else snap(),
        label = "player-quick-container"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0.40f else 0.10f,
        animationSpec = if (animationsEnabled) LevyraPlayerDesign.standardTween(170) else snap(),
        label = "player-quick-border"
    )

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = LevyraPlayerDesign.MinimumTouchTarget,
                minHeight = LevyraPlayerDesign.MinimumTouchTarget
            )
            .semantics {
                this.contentDescription = contentDescription
                if (toggleableState != null) {
                    this.toggleableState = toggleableState
                }
            }
            .levyraPressable(
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val pillSize = if (compact) 38.dp else 42.dp
        val iconSize = if (compact) 20.dp else 22.dp
        Box(
            modifier = Modifier
                .size(pillSize)
                .background(
                    if (active) tint.copy(alpha = containerAlpha) else Color.White.copy(alpha = containerAlpha),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (active) tint.copy(alpha = borderAlpha) else Color.White.copy(alpha = borderAlpha),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                    strokeWidth = 2.dp,
                    color = tint
                )
            } else {
                PlayerIcon(
                    icon = icon,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
