package com.luc4n3x.levyra.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavBackground
import com.luc4n3x.levyra.domain.NavColorSource
import com.luc4n3x.levyra.domain.NavIndicator
import com.luc4n3x.levyra.domain.NavIndicatorTone
import com.luc4n3x.levyra.domain.NavLabelMode
import com.luc4n3x.levyra.domain.NavMotion
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.NavSurfaceTone
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.components.dockClipHeight
import com.luc4n3x.levyra.ui.components.dockLerpHeight
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.abs

private val StyledNavTrayShrink = 22.dp
private val StyledNavMinTouch = 48.dp
private val StyledNavCompactFootprint = 54.dp
private val StyledNavIconBox = 32.dp
private val StyledNavFloatingGutter = 16.dp
private val StyledNavFloatingBottom = 6.dp
private val StyledNavOriginalFootprint = 76.dp
private val NavLightSurface = Color(0xFFF7F5FB)
private val NavDarkSurface = Color(0xFF1D1B22)
private val NavLightContent = Color(0xFF1C1B1F)
private val NavDarkContent = Color(0xFFE6E1E9)
private val NavLightVariant = Color(0xFF49454F)
private val NavDarkVariant = Color(0xFFCAC4D0)
private const val NavIconContrast = 3f
private const val NavTextContrast = 4.5f

internal val LocalNavDockExtraClearance = compositionLocalOf { 0.dp }

internal fun navDockExtraClearance(style: NavStyle, tuning: NavTuning): Dp {
    if (style == NavStyle.Original) return 0.dp
    val footprint = tuning.heightDp.dp + if (style.floating) StyledNavFloatingBottom else 0.dp
    return (footprint - StyledNavOriginalFootprint).coerceAtLeast(0.dp)
}

@Immutable
internal data class NavColors(
    val surface: Color,
    val raised: Color,
    val rim: Color,
    val indicator: Color,
    val onIndicator: Color,
    val mark: Color,
    val markText: Color,
    val inactive: Color,
    val inactiveText: Color,
    val accentWash: Color,
    val isLight: Boolean
)

@Immutable
private data class NavMotionSpec(
    val position: AnimationSpec<Float>,
    val tint: AnimationSpec<Color>,
    val fade: AnimationSpec<Float>,
    val scale: AnimationSpec<Float>,
    val pop: Float,
    val stretch: Float,
    val animated: Boolean
)

private val NavStaticMotion = NavMotionSpec(snap(), snap(), snap(), snap(), pop = 1f, stretch = 0f, animated = false)

@Composable
internal fun rememberNavColors(style: NavStyle, tuning: NavTuning): NavColors {
    val theme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val themeLight = LevyraActivePalette.isLight
    val dynamic = remember(tuning.colorSource, themeLight, context) {
        if (tuning.colorSource == NavColorSource.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (themeLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        } else {
            null
        }
    }
    val scheme = dynamic ?: theme
    return remember(scheme, style.floating, tuning.colorSource, tuning.accent, tuning.surfaceTone, tuning.indicatorTone, tuning.inactiveAlpha) {
        buildNavColors(scheme, style.floating, tuning)
    }
}

private class NavPalette(
    val surface: Color,
    val content: Color,
    val variant: Color,
    val raised: Color,
    val isLight: Boolean
)

private fun navPalette(scheme: ColorScheme, capsule: Boolean, tone: NavSurfaceTone): NavPalette {
    if (tone == NavSurfaceTone.Auto) {
        val surface = if (capsule) scheme.surfaceContainerHigh else scheme.surfaceContainer
        return NavPalette(
            surface = surface,
            content = scheme.onSurface,
            variant = scheme.onSurfaceVariant,
            raised = if (capsule) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh,
            isLight = surface.luminance() > 0.5f
        )
    }
    val surface = when (tone) {
        NavSurfaceTone.Light -> NavLightSurface
        NavSurfaceTone.Dark -> NavDarkSurface
        else -> Color.Black
    }
    val isLight = surface.luminance() > 0.5f
    val content = if (isLight) NavLightContent else NavDarkContent
    return NavPalette(
        surface = surface,
        content = content,
        variant = if (isLight) NavLightVariant else NavDarkVariant,
        raised = surface.playerMix(content, 0.06f),
        isLight = isLight
    )
}

private fun navIndicatorPair(
    tone: NavIndicatorTone,
    scheme: ColorScheme,
    palette: NavPalette,
    accent: Color,
    themedTonal: Boolean
): Pair<Color, Color> = when (tone) {
    NavIndicatorTone.Tonal -> if (themedTonal) {
        scheme.secondaryContainer to scheme.onSecondaryContainer
    } else {
        palette.surface.playerMix(accent, if (palette.isLight) 0.24f else 0.32f) to accent.playerMix(palette.content, 0.55f)
    }
    NavIndicatorTone.Vivid -> accent to accent
    NavIndicatorTone.Neutral -> palette.surface.playerMix(palette.content, if (palette.isLight) 0.10f else 0.16f) to palette.content
}

private fun buildNavColors(scheme: ColorScheme, capsule: Boolean, tuning: NavTuning): NavColors {
    val palette = navPalette(scheme, capsule, tuning.surfaceTone)
    val surface = palette.surface
    val isLight = palette.isLight
    val custom = tuning.colorSource == NavColorSource.Custom
    val accent = if (custom) Color(tuning.accent) else scheme.primary
    val themedTonal = tuning.surfaceTone == NavSurfaceTone.Auto && !custom
    val (indicator, onIndicatorSeed) = navIndicatorPair(tuning.indicatorTone, scheme, palette, accent, themedTonal)
    val mark = accent.navReadableOn(surface, NavIconContrast)
    val inactive = palette.variant.navAlphaReadableOn(surface, tuning.inactiveAlpha / 100f, NavIconContrast)
    return NavColors(
        surface = surface,
        raised = palette.raised,
        rim = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f),
        indicator = indicator,
        onIndicator = onIndicatorSeed.navReadableOn(indicator, NavTextContrast),
        mark = mark,
        markText = mark.navReadableOn(surface, NavTextContrast),
        inactive = inactive,
        inactiveText = inactive.navAlphaReadableOn(surface, inactive.alpha, NavTextContrast),
        accentWash = accent.copy(alpha = if (isLight) 0.10f else 0.14f),
        isLight = isLight
    )
}

private fun Color.navReadableOn(background: Color, minimum: Float): Color {
    val target = if (playerContrastRatio(Color.White, background) >= playerContrastRatio(Color.Black, background)) Color.White else Color.Black
    return playerAdjustForegroundToward(target, listOf(background), minimum).color
}

private fun Color.navAlphaReadableOn(background: Color, alpha: Float, minimum: Float): Color {
    var level = alpha.coerceIn(0f, 1f)
    while (level < 1f && playerContrastRatio(copy(alpha = level), background) < minimum) {
        level = (level + 0.05f).coerceAtMost(1f)
    }
    val candidate = copy(alpha = level)
    return if (playerContrastRatio(candidate, background) >= minimum) candidate else navReadableOn(background, minimum)
}

private fun navMotionSpec(motion: NavMotion, intensity: Int, animationsEnabled: Boolean): NavMotionSpec {
    val k = intensity.coerceIn(0, 100) / 100f
    if (!animationsEnabled) return NavStaticMotion
    return when (motion) {
        NavMotion.Off -> NavStaticMotion
        NavMotion.Minimal -> NavMotionSpec(
            position = tween(160, easing = FastOutSlowInEasing),
            tint = tween(160),
            fade = tween(160),
            scale = snap(),
            pop = 1f,
            stretch = 0f,
            animated = true
        )
        NavMotion.Standard -> NavMotionSpec(
            position = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            tint = tween(200),
            fade = tween(200),
            scale = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            pop = 1f + 0.04f * k,
            stretch = 0.30f * k,
            animated = true
        )
        NavMotion.Expressive -> NavMotionSpec(
            position = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            tint = tween(220),
            fade = spring(stiffness = Spring.StiffnessMediumLow),
            scale = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
            pop = 1f + 0.10f * k,
            stretch = 0.55f * k,
            animated = true
        )
        NavMotion.Spring -> NavMotionSpec(
            position = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
            tint = tween(240),
            fade = spring(stiffness = Spring.StiffnessLow),
            scale = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow),
            pop = 1f + 0.14f * k,
            stretch = 0.75f * k,
            animated = true
        )
    }
}

@Composable
internal fun StyledDockSurface(
    style: NavStyle,
    tuning: NavTuning,
    glass: GlassBackdropState?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (style.floating) {
        Column(modifier = modifier, content = content)
        return
    }
    val colors = rememberNavColors(style, tuning)
    val shape = remember(tuning.cornerDp) {
        RoundedCornerShape(topStart = tuning.cornerDp.dp, topEnd = tuning.cornerDp.dp)
    }
    val wash = if (style == NavStyle.SuperExpressive) colors.accentWash else Color.Transparent
    Column(
        modifier = modifier
            .clip(shape)
            .navBackground(tuning.background, colors, tuning.opacity, glass)
            .drawBehind {
                if (wash.alpha > 0f) drawRect(Brush.verticalGradient(0f to wash, 0.5f to Color.Transparent))
            }
            .border(LevyraPlayerDesign.Hairline, colors.rim, shape),
        content = content
    )
}

private fun Modifier.navBackground(
    background: NavBackground,
    colors: NavColors,
    opacity: Int,
    glass: GlassBackdropState?
): Modifier = when (background) {
    NavBackground.Solid -> background(colors.surface)
    NavBackground.Gradient -> background(Brush.verticalGradient(listOf(colors.raised, colors.surface)))
    NavBackground.Translucent -> {
        val frosted = colors.surface.copy(alpha = opacity / 100f * 0.82f)
        val fallback = colors.surface.copy(alpha = maxOf(opacity / 100f, 0.92f))
        if (glass != null) {
            glassFrost(
                state = glass,
                blurRadius = 24.dp,
                onDrawFrosted = { drawRect(frosted) },
                onDrawFallback = { drawRect(fallback) }
            )
        } else {
            background(fallback)
        }
    }
}

private fun Modifier.navLiquidGlass(
    colors: NavColors,
    opacity: Int,
    glass: GlassBackdropState?,
    shape: Shape,
    depth: Float
): Modifier {
    val frosted = colors.surface.copy(alpha = opacity / 100f * 0.72f)
    val fallback = colors.surface.copy(alpha = maxOf(opacity / 100f, 0.92f))
    val sheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (colors.isLight) 0.42f else 0.13f),
        0.42f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (colors.isLight) 0.03f else 0.10f)
    )
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (colors.isLight) 0.70f else 0.30f),
        1f to Color.White.copy(alpha = if (colors.isLight) 0.18f else 0.05f)
    )
    val material = if (glass != null) {
        glassFrost(
            state = glass,
            blurRadius = 32.dp,
            onDrawFrosted = {
                drawRect(frosted)
                drawRect(sheen)
            },
            onDrawFallback = {
                drawRect(fallback)
                drawRect(sheen)
            }
        )
    } else {
        background(fallback).drawBehind { drawRect(sheen) }
    }
    return shadow(
        elevation = (8f + 10f * depth).dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.18f),
        spotColor = Color.Black.copy(alpha = 0.28f)
    )
        .clip(shape)
        .then(material)
        .border(1.dp, rim, shape)
}

private fun Modifier.navCapsule(
    colors: NavColors,
    background: NavBackground,
    opacity: Int,
    glass: GlassBackdropState?,
    shape: Shape,
    depth: Float
): Modifier = shadow(
    elevation = (6f + 12f * depth).dp,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.22f),
    spotColor = Color.Black.copy(alpha = 0.32f)
)
    .clip(shape)
    .navBackground(background, colors, opacity, glass)
    .border(LevyraPlayerDesign.Hairline, colors.rim, shape)

private fun navLabelRoom(mode: NavLabelMode): Dp = when (mode) {
    NavLabelMode.Always, NavLabelMode.ActiveOnly -> 16.dp
    NavLabelMode.Compact -> 12.dp
    NavLabelMode.Never -> 0.dp
}

private fun navIconTop(height: Dp, labelRoom: Dp): Dp =
    ((height - StyledNavIconBox - labelRoom) / 2).coerceAtLeast(4.dp)

private fun navLabelShown(mode: NavLabelMode, selected: Boolean): Boolean = when (mode) {
    NavLabelMode.Always, NavLabelMode.Compact -> true
    NavLabelMode.ActiveOnly -> selected
    NavLabelMode.Never -> false
}

private fun Modifier.navFloatingInset(floating: Boolean): Modifier = if (floating) {
    padding(start = StyledNavFloatingGutter, end = StyledNavFloatingGutter, bottom = StyledNavFloatingBottom)
} else {
    this
}

private class NavDockGeometry(
    val height: Dp,
    val compactHeight: Dp,
    val labelRoom: Dp,
    val iconTop: Dp,
    val lift: Dp
)

private fun navDockGeometry(floating: Boolean, height: Dp, labels: NavLabelMode): NavDockGeometry {
    val compactHeight = if (floating) {
        StyledNavCompactFootprint - StyledNavFloatingBottom
    } else {
        (height - StyledNavTrayShrink).coerceIn(StyledNavMinTouch, StyledNavCompactFootprint)
    }.coerceAtMost(height)
    val labelRoom = navLabelRoom(labels)
    val iconTop = navIconTop(height, labelRoom)
    val lift = if (floating) (iconTop - navIconTop(compactHeight, 0.dp)).coerceAtLeast(0.dp) else 0.dp
    return NavDockGeometry(height, compactHeight, labelRoom, iconTop, lift)
}

private fun navDockSurface(
    style: NavStyle,
    colors: NavColors,
    background: NavBackground,
    opacity: Int,
    glass: GlassBackdropState?,
    shape: Shape,
    depth: Float
): Modifier = when {
    style == NavStyle.LiquidGlass -> Modifier.navLiquidGlass(colors, opacity, glass, shape, depth)
    style.floating -> Modifier.navCapsule(colors, background, opacity, glass, shape, depth)
    else -> Modifier
}

private fun Modifier.navDockSizing(floating: Boolean, compaction: () -> Float, geometry: NavDockGeometry): Modifier =
    if (floating) {
        dockLerpHeight(compaction, geometry.height, geometry.compactHeight)
    } else {
        dockClipHeight(compaction, geometry.height - geometry.compactHeight).height(geometry.height)
    }

@Composable
internal fun StyledBottomTabs(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    compaction: () -> Float,
    onSelect: (LevyraTab) -> Unit,
    glass: GlassBackdropState?,
    applySystemInset: Boolean = true
) {
    val allEntries = rememberLevyraTabEntries()
    val entries = remember(allEntries, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> allEntries.firstOrNull { it.tab == tab } }
    }
    if (entries.isEmpty()) return
    val colors = rememberNavColors(style, tuning)
    val animationsEnabled = LocalAnimationsEnabled.current
    val motion = remember(tuning.motion, tuning.intensity, animationsEnabled) {
        navMotionSpec(tuning.motion, tuning.intensity, animationsEnabled)
    }
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = motion.position,
        label = "styled-nav-indicator"
    )
    val floating = style.floating
    val geometry = remember(floating, tuning.heightDp, tuning.labels) {
        navDockGeometry(floating, tuning.heightDp.dp, tuning.labels)
    }
    val depth = tuning.intensity / 100f
    val padding = tuning.paddingDp.dp
    val spacing = tuning.spacingDp.dp
    val shape = remember(tuning.cornerDp, geometry.height) {
        RoundedCornerShape(minOf(tuning.cornerDp.dp, geometry.height / 2))
    }
    val surface = remember(style, colors, tuning.background, tuning.opacity, glass, shape, depth) {
        navDockSurface(style, colors, tuning.background, tuning.opacity, glass, shape, depth)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .navFloatingInset(floating)
                .fillMaxWidth()
                .navDockSizing(floating, compaction, geometry)
                .then(surface)
        ) {
            val slotWidth = ((maxWidth - padding * 2 - spacing * (entries.size - 1)) / entries.size).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .height(geometry.height)
                    .graphicsLayer { translationY = -geometry.lift.toPx() * compaction() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            if (selectedIndex < 0) return@drawBehind
                            val fold = compaction().coerceIn(0f, 1f)
                            drawNavIndicator(
                                tuning = tuning,
                                floating = floating,
                                startPx = padding.toPx(),
                                slotPx = slotWidth.toPx(),
                                gapPx = spacing.toPx(),
                                position = indicatorPosition,
                                selectedIndex = selectedIndex,
                                lastIndex = entries.lastIndex,
                                iconTopPx = geometry.iconTop.toPx(),
                                labelRoomPx = geometry.labelRoom.toPx(),
                                visibleTopPx = geometry.lift.toPx() * fold,
                                visibleHeightPx = size.height - (geometry.height - geometry.compactHeight).toPx() * fold,
                                stretch = motion.stretch,
                                colors = colors
                            )
                        }
                )
                StyledTabRow(
                    entries = entries,
                    selected = selected,
                    tuning = tuning,
                    colors = colors,
                    iconTop = geometry.iconTop,
                    motion = motion,
                    foldLabel = if (floating) compaction else null,
                    onSelect = onSelect
                )
            }
        }
        if (applySystemInset) {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StyledTabRow(
    entries: List<LevyraTabEntry>,
    selected: LevyraTab,
    tuning: NavTuning,
    colors: NavColors,
    iconTop: Dp,
    motion: NavMotionSpec,
    foldLabel: (() -> Float)?,
    onSelect: (LevyraTab) -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = tuning.paddingDp.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(tuning.spacingDp.dp),
        verticalAlignment = Alignment.Top
    ) {
        entries.forEach { entry ->
            val isSelected = entry.tab == selected
            StyledTabButton(
                entry = entry,
                isSelected = isSelected,
                tuning = tuning,
                colors = colors,
                iconTop = iconTop,
                motion = motion,
                foldLabel = foldLabel,
                onClick = {
                    if (!isSelected && tuning.haptics) {
                        haptics.perform(if (motion.pop > 1f) LevyraHapticAction.Confirm else LevyraHapticAction.SeekSnap)
                    }
                    onSelect(entry.tab)
                }
            )
        }
    }
}

private fun DrawScope.drawNavIndicator(
    tuning: NavTuning,
    floating: Boolean,
    startPx: Float,
    slotPx: Float,
    gapPx: Float,
    position: Float,
    selectedIndex: Int,
    lastIndex: Int,
    iconTopPx: Float,
    labelRoomPx: Float,
    visibleTopPx: Float,
    visibleHeightPx: Float,
    stretch: Float,
    colors: NavColors
) {
    val travel = abs(position - selectedIndex).coerceAtMost(1f)
    val visual = if (layoutDirection == LayoutDirection.Rtl) lastIndex - position else position
    val centerX = startPx + visual * (slotPx + gapPx) + slotPx / 2f
    val scale = tuning.indicatorScale / 100f
    val iconBox = StyledNavIconBox.toPx()
    val iconCenterY = iconTopPx + iconBox / 2f
    when (tuning.indicator) {
        NavIndicator.Pill, NavIndicator.Morph -> {
            val morph = if (tuning.indicator == NavIndicator.Morph) travel * stretch else 0f
            val base = (slotPx - 20.dp.toPx()).coerceIn(48.dp.toPx(), 64.dp.toPx()) * scale
            val width = base + slotPx * morph
            val pillHeight = iconBox * scale * (1f - 0.18f * morph)
            drawRoundRect(
                color = colors.indicator,
                topLeft = Offset(centerX - width / 2f, iconCenterY - pillHeight / 2f),
                size = Size(width, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f * (1f - 0.42f * morph))
            )
        }
        NavIndicator.Circle -> drawCircle(
            color = colors.indicator,
            radius = 20.dp.toPx() * scale * (1f - 0.12f * travel * stretch),
            center = Offset(centerX, iconCenterY)
        )
        NavIndicator.Floating -> {
            val inset = if (floating) 5.dp.toPx() else 8.dp.toPx()
            val width = (slotPx - 6.dp.toPx()).coerceAtLeast(iconBox) * (0.88f + 0.12f * scale) + slotPx * 0.25f * travel * stretch
            val pillHeight = (visibleHeightPx - inset * 2f).coerceAtLeast(iconBox)
            val radius = minOf(tuning.cornerDp.dp.toPx() - inset, pillHeight / 2f).coerceAtLeast(10.dp.toPx())
            val topLeft = Offset(centerX - width / 2f, visibleTopPx + inset)
            val pillSize = Size(width, pillHeight)
            drawRoundRect(
                color = Color.Black.copy(alpha = if (colors.isLight) 0.08f else 0.22f),
                topLeft = topLeft + Offset(0f, 2.dp.toPx() * scale),
                size = pillSize,
                cornerRadius = CornerRadius(radius)
            )
            drawRoundRect(color = colors.indicator, topLeft = topLeft, size = pillSize, cornerRadius = CornerRadius(radius))
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (colors.isLight) 0.30f else 0.10f), Color.Transparent),
                    startY = topLeft.y,
                    endY = topLeft.y + pillHeight * 0.6f
                ),
                topLeft = topLeft,
                size = pillSize,
                cornerRadius = CornerRadius(radius)
            )
        }
        NavIndicator.Dot -> drawCircle(
            color = colors.mark,
            radius = 2.5.dp.toPx() * scale,
            center = Offset(centerX, iconTopPx + iconBox + labelRoomPx + 5.dp.toPx())
        )
        NavIndicator.Line -> {
            val width = (22.dp.toPx() + 18.dp.toPx() * travel * stretch) * scale
            val lineHeight = 3.dp.toPx()
            val bottomGap = if (floating) 5.dp.toPx() else 6.dp.toPx()
            drawRoundRect(
                color = colors.mark,
                topLeft = Offset(centerX - width / 2f, visibleTopPx + visibleHeightPx - lineHeight - bottomGap),
                size = Size(width, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
    }
}

@Composable
private fun RowScope.StyledTabButton(
    entry: LevyraTabEntry,
    isSelected: Boolean,
    tuning: NavTuning,
    colors: NavColors,
    iconTop: Dp,
    motion: NavMotionSpec,
    foldLabel: (() -> Float)?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selectedIcon = if (tuning.indicator.filled) colors.onIndicator else colors.mark
    val selectedText = if (tuning.indicator == NavIndicator.Floating) colors.onIndicator else colors.markText
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) selectedIcon else colors.inactive,
        animationSpec = motion.tint,
        label = "styled-tab-icon"
    )
    val textTint by animateColorAsState(
        targetValue = if (isSelected) selectedText else colors.inactiveText,
        animationSpec = motion.tint,
        label = "styled-tab-text"
    )
    val scale by animateFloatAsState(
        targetValue = navTabScale(pressed, isSelected, motion),
        animationSpec = motion.scale,
        label = "styled-tab-scale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (navLabelShown(tuning.labels, isSelected)) 1f else 0f,
        animationSpec = motion.fade,
        label = "styled-tab-label"
    )
    val compact = tuning.labels == NavLabelMode.Compact
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = entry.label
            }
            .levyraPressable(
                onClick = onClick,
                interactionSource = interactionSource,
                pressedScale = if (motion.animated) 0.96f else 1f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = iconTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 2.dp)
        ) {
            Box(
                modifier = Modifier.height(StyledNavIconBox),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) entry.selectedIcon else entry.unselectedIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(tuning.iconDp.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            if (tuning.labels != NavLabelMode.Never) {
                StyledTabLabel(
                    text = entry.label,
                    color = textTint,
                    selected = isSelected,
                    compact = compact,
                    alpha = {
                        val fold = foldLabel?.invoke()?.coerceIn(0f, 1f) ?: 0f
                        labelAlpha * (1f - fold)
                    }
                )
            }
        }
    }
}

private fun navTabScale(pressed: Boolean, selected: Boolean, motion: NavMotionSpec): Float = when {
    pressed && motion.animated -> 0.88f
    selected -> motion.pop
    else -> 1f
}

@Composable
private fun StyledTabLabel(
    text: String,
    color: Color,
    selected: Boolean,
    compact: Boolean,
    alpha: () -> Float
) {
    val labelSize: TextUnit = if (compact) 10.sp else 11.sp
    Text(
        text = text,
        color = color,
        fontSize = labelSize,
        lineHeight = LevyraTypeRhythm.lineHeight(labelSize),
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        letterSpacing = if (compact) 0.sp else 0.1.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .height(if (compact) 12.dp else 14.dp)
            .graphicsLayer { this.alpha = alpha() }
    )
}
