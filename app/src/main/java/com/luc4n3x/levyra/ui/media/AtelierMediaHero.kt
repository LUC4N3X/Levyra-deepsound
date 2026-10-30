package com.luc4n3x.levyra.ui.media

import androidx.compose.ui.geometry.center
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.atelierUppercaseLabel
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import kotlinx.coroutines.delay

private const val AtelierCoverTintFraction = 0.08f
private const val AtelierSleeveWidthFraction = 0.7f
private val AtelierSleeveMax = 340.dp
private val AtelierStageTop = 18.dp
private const val AtelierRestTiltY = 9f
private const val AtelierRestTiltX = 5f
private const val AtelierCameraDistance = 14f
private const val AtelierParallax = 0.32f
private const val AtelierRecede = 0.08f
private const val AtelierDiscTravel = 0.2f
private const val AtelierDiscRetract = 0.6f
private const val AtelierDiscScale = 0.95f
private const val AtelierDiscDelayMillis = 220L
private const val AtelierDiscRevealMillis = 950
private val AtelierKeyShape = RoundedCornerShape(11.dp)
private val AtelierKeyTop = Color(0xFF2C2C31)
private val AtelierKeyBottom = Color(0xFF141417)
private val AtelierKeyPressedTop = Color(0xFF1E1E22)
private val AtelierKeyPressedBottom = Color(0xFF0E0E10)
private val AtelierHaloFallback = Color(0xFF7A5A3A)
private val AtelierHaloNeutral = Color(0xFF7A7570)
private val AtelierVinylCenter = Color(0xFF1A181C)
private val AtelierVinylMid = Color(0xFF0F0E11)
private val AtelierVinylEdge = Color(0xFF08070A)
private val AtelierGrooveBright = Color.White.copy(alpha = 0.07f)
private val AtelierGrooveDim = Color.White.copy(alpha = 0.03f)
private val AtelierVinylRim = Color.White.copy(alpha = 0.14f)
private val AtelierLabelRing = AtelierDesign.onAccent.copy(alpha = 0.5f)

internal fun atelierMediaColors(cover: Color): ImmersiveMediaColors {
    val field = if (cover.isSpecified) lerp(AtelierDesign.cabinet, cover, AtelierCoverTintFraction) else AtelierDesign.cabinet
    return ImmersiveMediaColors(
        fieldTop = field,
        fieldMid = atelierHaloTone(cover),
        base = AtelierDesign.room,
        content = AtelierDesign.text,
        contentMuted = AtelierDesign.textSecondary,
        accent = AtelierDesign.accent,
        actionStart = AtelierDesign.accent,
        actionEnd = AtelierDesign.accent,
        actionContent = AtelierDesign.onAccent,
        secondaryFill = AtelierDesign.panel,
        hairline = AtelierDesign.hairline
    )
}

private fun atelierHaloTone(cover: Color): Color {
    if (!cover.isSpecified) return AtelierHaloFallback
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(cover.toArgb(), hsl)
    if (hsl[1] < 0.08f) return lerp(AtelierHaloNeutral, AtelierHaloFallback, 0.55f)
    hsl[1] = hsl[1].coerceIn(0.24f, 0.62f)
    hsl[2] = hsl[2].coerceIn(0.40f, 0.56f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun atelierHeroTitleSize(title: String): TextUnit = when {
    title.length <= 14 -> 38.sp
    title.length <= 24 -> 33.sp
    title.length <= 40 -> 28.sp
    else -> 24.sp
}

@Composable
internal fun AtelierMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val animated = LocalAnimationsEnabled.current
    val towardEnd = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val sleeve = min(viewportWidth * AtelierSleeveWidthFraction, AtelierSleeveMax)
    val scrolled = remember { mutableFloatStateOf(0f) }
    val discOut = rememberAtelierDiscReveal(animated)
    val halo = colors.fieldMid
    Column(
        modifier.fillMaxWidth()
            .onGloballyPositioned { scrolled.floatValue = (-it.positionInParent().y).coerceAtLeast(0f) }
            .drawWithCache {
                val sleevePx = sleeve.toPx()
                val center = Offset(size.width / 2f, (topBarHeight + AtelierStageTop).toPx() + sleevePx / 2f)
                val glow = Brush.radialGradient(
                    0f to halo.copy(alpha = 0.62f), 0.32f to halo.copy(alpha = 0.30f),
                    0.62f to halo.copy(alpha = 0.09f), 1f to Color.Transparent,
                    center = center, radius = sleevePx * 1.15f
                )
                val core = Brush.radialGradient(
                    0f to AtelierDesign.accentBright.copy(alpha = 0.10f), 1f to Color.Transparent,
                    center = center, radius = sleevePx * 0.62f
                )
                val floorCenter = Offset(center.x, center.y + sleevePx * 0.58f)
                val floorRadius = sleevePx * 0.6f
                val floor = Brush.radialGradient(
                    0f to Color.Black.copy(alpha = 0.6f), 1f to Color.Transparent,
                    center = floorCenter, radius = floorRadius
                )
                onDrawBehind {
                    val travel = scrolled.floatValue
                    val fade = 1f - (travel / sleevePx).coerceIn(0f, 1f) * 0.55f
                    drawRect(glow, alpha = fade)
                    drawRect(core, alpha = fade)
                    translate(top = if (animated) travel * AtelierParallax else 0f) {
                        scale(1f, 0.16f, floorCenter) { drawCircle(floor, floorRadius, floorCenter, alpha = fade) }
                    }
                }
            }
    ) {
        Box(
            Modifier.fillMaxWidth().padding(top = topBarHeight + AtelierStageTop, bottom = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AtelierSleeveStage(sleeve, towardEnd, animated, scrolled, discOut, colors, artwork)
        }
        AtelierHeroText(title, overline, subtitle, metadata, colors, onSubtitleClick, actions)
    }
}

@Composable
private fun rememberAtelierDiscReveal(animated: Boolean): Animatable<Float, AnimationVector1D> {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (revealed || !animated) 1f else 0f) }
    LaunchedEffect(animated) {
        if (animated && reveal.value < 1f) {
            delay(AtelierDiscDelayMillis)
            reveal.animateTo(1f, tween(AtelierDiscRevealMillis, easing = FastOutSlowInEasing))
        } else {
            reveal.snapTo(1f)
        }
        revealed = true
    }
    return reveal
}

@Composable
private fun AtelierSleeveStage(
    sleeve: Dp, towardEnd: Float, animated: Boolean, scrolled: MutableFloatState,
    discOut: Animatable<Float, AnimationVector1D>, colors: ImmersiveMediaColors, artwork: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier.size(sleeve).graphicsLayer {
            val travel = scrolled.floatValue
            val progress = (travel / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val rest = if (animated) 1f - progress else 1f
            val recede = if (animated) 1f - AtelierRecede * progress else 1f
            transformOrigin = TransformOrigin.Center
            cameraDistance = AtelierCameraDistance * density
            rotationY = AtelierRestTiltY * towardEnd * rest
            rotationX = AtelierRestTiltX * rest
            translationY = if (animated) travel * AtelierParallax else 0f
            scaleX = recede
            scaleY = recede
        }
    ) {
        Spacer(
            Modifier.matchParentSize()
                .graphicsLayer {
                    val progress = (scrolled.floatValue / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val retract = if (animated) 1f - progress * AtelierDiscRetract else 1f
                    translationX = towardEnd * size.width * AtelierDiscTravel * discOut.value * retract
                    scaleX = AtelierDiscScale
                    scaleY = AtelierDiscScale
                }
                .drawWithCache {
                    val r = size.minDimension / 2f
                    val c = size.center
                    val body = Brush.radialGradient(
                        0f to AtelierVinylCenter, 0.7f to AtelierVinylMid, 1f to AtelierVinylEdge, center = c, radius = r
                    )
                    val sheen = Brush.sweepGradient(
                        0f to Color.Transparent, 0.08f to Color.White.copy(alpha = 0.12f), 0.16f to Color.Transparent,
                        0.5f to Color.Transparent, 0.58f to Color.White.copy(alpha = 0.08f), 0.66f to Color.Transparent,
                        1f to Color.Transparent, center = c
                    )
                    val label = Brush.radialGradient(
                        0f to AtelierDesign.accentBright, 1f to AtelierDesign.accent, center = c, radius = r * 0.34f
                    )
                    val groove = Stroke(0.6.dp.toPx())
                    val rim = Stroke(1.dp.toPx())
                    val labelRing = Stroke(0.8.dp.toPx())
                    val rings = FloatArray(22) { index -> r * (0.40f + index * 0.026f) }
                    onDrawBehind {
                        drawCircle(body, r, c)
                        rings.forEachIndexed { index, ring ->
                            drawCircle(if (index % 4 == 0) AtelierGrooveDim else AtelierGrooveBright, ring, c, style = groove)
                        }
                        drawCircle(sheen, r * 0.97f, c)
                        drawCircle(AtelierVinylRim, r * 0.995f, c, style = rim)
                        drawCircle(label, r * 0.34f, c)
                        drawCircle(AtelierLabelRing, r * 0.27f, c, style = labelRing)
                        drawCircle(AtelierDesign.onAccent, r * 0.035f, c)
                    }
                }
        )
        Box(
            Modifier.matchParentSize()
                .shadow(22.dp, AtelierDesign.sleeveShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(AtelierDesign.sleeveShape)
                .background(AtelierDesign.panel)
        ) {
            artwork()
            Spacer(
                Modifier.matchParentSize()
                    .drawWithCache {
                        val gloss = Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.10f), 0.35f to Color.Transparent,
                            0.75f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f),
                            start = Offset.Zero, end = Offset(size.width, size.height)
                        )
                        val spineWidth = size.width * 0.05f
                        val rtl = layoutDirection == LayoutDirection.Rtl
                        val spine = if (rtl) {
                            Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f), startX = size.width - spineWidth, endX = size.width)
                        } else {
                            Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.35f), 1f to Color.Transparent, startX = 0f, endX = spineWidth)
                        }
                        val highlight = Color.White.copy(alpha = 0.22f)
                        onDrawBehind {
                            drawRect(gloss)
                            drawRect(spine)
                            drawRect(highlight, size = Size(size.width, 1.dp.toPx()))
                        }
                    }
                    .border(1.dp, colors.hairline, AtelierDesign.sleeveShape)
            )
        }
    }
}

@Composable
private fun AtelierHeroText(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 30.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (overline.isNotBlank()) {
            Text(
                atelierUppercaseLabel(overline), color = colors.accent,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
        }
        val titleSize = remember(title) { atelierHeroTitleSize(title) }
        Text(
            title, color = colors.content,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = titleSize, lineHeight = titleSize * 1.14f, fontWeight = FontWeight.Light, letterSpacing = (-0.6).sp
            ),
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Box(
                if (onSubtitleClick != null) {
                    Modifier.heightIn(min = 48.dp).clip(AtelierDesign.cardShape)
                        .levyraPressable(onClick = onSubtitleClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
                        .padding(horizontal = 8.dp)
                } else {
                    Modifier.padding(vertical = 4.dp)
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    subtitle, color = colors.content.copy(alpha = 0.82f), style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (metadata.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                metadata, color = AtelierDesign.textFaint,
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.8.sp),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { actions() }
    }
}

@Composable
internal fun AtelierMediaActionRow(
    primary: ImmersiveMediaPrimaryAction,
    shuffleLabel: String,
    downloadLabel: String,
    shuffleEnabled: Boolean,
    downloadEnabled: Boolean,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier,
    secondary: @Composable RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        secondary()
        AtelierQuietAction(Icons.Rounded.Shuffle, shuffleLabel, shuffleEnabled, onShuffle)
        AtelierMediaKey(
            label = primary.label,
            icon = primary.icon,
            enabled = primary.enabled,
            loading = primary.loading,
            onClick = primary.onClick,
            onClickLabel = primary.contentDescription,
            modifier = Modifier.weight(1f, fill = false).widthIn(min = 132.dp, max = 220.dp)
        )
        AtelierQuietAction(Icons.Rounded.Download, downloadLabel, downloadEnabled, onDownload)
    }
}

@Composable
internal fun AtelierMediaKey(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLabel: String = label,
    lit: Boolean = enabled
) {
    val animated = LocalAnimationsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateFloatAsState(
        if (pressed && animated) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 900f), label = "atelier-media-key"
    )
    val led by animateFloatAsState(if (lit) 1f else 0f, if (animated) tween(260) else snap(), label = "atelier-media-key-led")
    val tint = if (enabled) AtelierDesign.text else AtelierDesign.textFaint
    Row(
        modifier.heightIn(min = 56.dp)
            .graphicsLayer { translationY = travel * 1.5.dp.toPx() }
            .clip(AtelierKeyShape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(lerp(AtelierKeyTop, AtelierKeyPressedTop, travel), lerp(AtelierKeyBottom, AtelierKeyPressedBottom, travel))))
                drawRect(Color.White.copy(alpha = 0.16f - travel * 0.1f), size = Size(size.width, 1.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                if (led > 0.01f) {
                    val width = 18.dp.toPx()
                    val left = (size.width - width) / 2f
                    drawRoundRect(
                        AtelierDesign.accent.copy(alpha = 0.22f * led), Offset(left - 4.dp.toPx(), 4.dp.toPx()),
                        Size(width + 8.dp.toPx(), 6.dp.toPx()), CornerRadius(3.dp.toPx())
                    )
                    drawRoundRect(
                        AtelierDesign.accentBright.copy(alpha = led), Offset(left, 6.dp.toPx()),
                        Size(width, 2.dp.toPx()), CornerRadius(1.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interaction, indication = null, enabled = enabled && !loading,
                role = Role.Button, onClickLabel = onClickLabel, onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            LevyraLoadingIndicator(color = tint, modifier = Modifier.size(22.dp))
        } else {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label, color = tint, style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 0.6.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AtelierQuietAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    active: Boolean = false,
    toggleable: Boolean = false
) {
    val ring = when {
        !enabled -> AtelierDesign.hairline
        active -> AtelierDesign.accent.copy(alpha = 0.7f)
        else -> AtelierDesign.hairlineStrong
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, pressedScale = LevyraPressScale.Control, role = Role.Button)
            .border(1.dp, ring, CircleShape)
            .semantics { if (toggleable) toggleableState = ToggleableState(active) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, label,
            tint = when {
                !enabled -> AtelierDesign.textFaint.copy(alpha = 0.5f)
                active -> AtelierDesign.accentBright
                else -> AtelierDesign.text
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
