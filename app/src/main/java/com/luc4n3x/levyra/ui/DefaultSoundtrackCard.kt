package com.luc4n3x.levyra.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSoundtrackCard(
    header: (@Composable () -> Unit)?,
    backdropBleed: Dp,
    artworkUrl: String,
    overline: String,
    title: String,
    lead: String,
    companions: List<String>,
    playing: Boolean,
    resolving: Boolean,
    playLabel: String,
    onOpen: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val canvas = homeCanvasColor(LevyraIsLight)
    val action = levyraUtilityColors(LevyraUtilitySeed)
    val blur = LocalLevyraVisualCapabilities.current.heavyBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxWidth()) {
        if (artworkUrl.isNotBlank()) {
            Box(
                Modifier
                    .matchParentSize()
                    .homeHeroBleed(backdropBleed)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Black, 0.62f to Color.Black, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                if (blur) {
                    StableRemoteArtwork(
                        url = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.28f }.blur(80.dp, BlurredEdgeTreatment.Rectangle),
                        contentScale = ContentScale.Crop
                    )
                }
                StableRemoteArtwork(
                    url = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(DefaultSoundtrackPortraitShare)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.horizontalGradient(0f to Color.Transparent, 0.42f to Color.Black.copy(alpha = 0.85f), 1f to Color.Black),
                                blendMode = BlendMode.DstIn
                            )
                            drawRect(
                                Brush.verticalGradient(0f to Color.Transparent, 0.18f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.35f),
                    highRes = true
                )
                Box(
                    Modifier.fillMaxSize().drawWithContent {
                        drawRect(Brush.horizontalGradient(0f to canvas.copy(alpha = 0.7f), 0.55f to canvas.copy(alpha = 0.15f), 1f to Color.Transparent))
                    }
                )
            }
        }
        Column(Modifier.fillMaxWidth()) {
            header?.invoke()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(DefaultSoundtrackBodyHeight)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = playLabel, onClick = onOpen)
            ) {
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(0.68f).padding(start = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        overline.uppercase(LocalConfiguration.current.locales[0]),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                        maxLines = 1
                    )
                    Text(
                        title,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (companions.isNotEmpty()) HomeSoundtrackCompanionStack(companions, ring = canvas, accent = colors.outlineVariant)
                        Text(lead, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                FilledIconButton(
                    onClick = onOpen,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(action.accent, action.onAccent),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp).size(64.dp)
                ) {
                    if (resolving) {
                        DiscoveryLoadingIndicator(Modifier.size(24.dp), color = action.onAccent)
                    } else {
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = playLabel,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val DefaultSoundtrackPortraitShare = 0.78f
private val DefaultSoundtrackBodyHeight = 300.dp
