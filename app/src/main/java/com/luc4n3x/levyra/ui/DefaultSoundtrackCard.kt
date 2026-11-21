package com.luc4n3x.levyra.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import com.luc4n3x.levyra.ui.components.rememberLevyraHeroPlayShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSoundtrackCard(
    header: (@Composable () -> Unit)?,
    compactStage: Boolean,
    backdropBleed: Dp,
    artworkUrl: String,
    overline: String,
    overlineHighlight: String?,
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
    val body = if (compactStage) {
        Modifier.heightIn(min = levyraCompactLandscapeHeight(272.dp, viewportShare = 0.5f, minimum = 144.dp))
    } else {
        Modifier.height(DefaultSoundtrackBodyHeight)
    }
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
                        .fillMaxWidth(if (compactStage) 0.88f else DefaultSoundtrackPortraitShare)
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
                    .then(body)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = playLabel, onClick = onOpen)
            ) {
                val details: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val locale = LocalConfiguration.current.locales[0]
                        Text(
                            remember(overline, overlineHighlight, locale, action.accent) {
                                soundtrackOverline(overline.uppercase(locale), overlineHighlight?.uppercase(locale), action.accent)
                            },
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                            maxLines = 1
                        )
                        Text(
                            title,
                            color = colors.onSurface,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (companions.isNotEmpty()) HomeSoundtrackCompanionStack(companions, ring = canvas, accent = colors.outlineVariant)
                            Text(lead, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                val inset = if (compactStage) LevyraHomeDesign.HorizontalInset else 20.dp
                details(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            start = inset,
                            end = inset + SoundtrackPlaySize + 12.dp,
                            bottom = (if (compactStage) 12.dp else 24.dp) + SoundtrackPlaySize * SoundtrackPlayOverhang
                        )
                )
                SoundtrackPlayButton(
                    playing = playing,
                    resolving = resolving,
                    label = playLabel,
                    accent = action.accent,
                    onAccent = action.onAccent,
                    onClick = onOpen,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = inset, bottom = 2.dp)
                )
            }
        }
    }
}

private const val DefaultSoundtrackPortraitShare = 0.78f
private val SoundtrackPlaySize = 72.dp
private const val SoundtrackPlayOverhang = 0.3f

@Composable
private fun SoundtrackPlayButton(
    playing: Boolean,
    resolving: Boolean,
    label: String,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated = LocalAnimationsEnabled.current
    val shape = rememberLevyraHeroPlayShape(playing = playing, animated = animated, spinWhilePlaying = true)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = if (animated) spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "soundtrackPlayPress"
    )
    Box(
        modifier = modifier
            .size(SoundtrackPlaySize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                        center = center + Offset(0f, size.height * 0.08f),
                        radius = size.minDimension * 0.72f
                    ),
                    radius = size.minDimension * 0.72f,
                    center = center + Offset(0f, size.height * 0.08f)
                )
            }
            .clip(shape)
            .background(accent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = onAccent),
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (resolving) {
            DiscoveryLoadingIndicator(Modifier.size(30.dp), color = onAccent)
        } else {
            AnimatedContent(
                targetState = playing,
                transitionSpec = {
                    if (animated) {
                        fadeIn() + scaleIn(initialScale = 0.5f) togetherWith fadeOut() + scaleOut(targetScale = 0.5f)
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "soundtrackPlayGlyph"
            ) { isPlaying ->
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = onAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
private val DefaultSoundtrackBodyHeight = 300.dp

private fun soundtrackOverline(text: String, highlight: String?, accent: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    if (highlight.isNullOrBlank()) return@buildAnnotatedString
    val start = text.lastIndexOf(highlight)
    if (start >= 0) addStyle(SpanStyle(color = accent), start, start + highlight.length)
}
