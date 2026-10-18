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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
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
import com.luc4n3x.levyra.ui.components.dockLerpSize
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.abs
import kotlin.math.roundToInt

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

private val SplitDockJoinedGap = 4.dp
private val SplitDockDetachedGap = 10.dp
private val SplitDockInnerCorner = 8.dp
private val SplitOrbMarkInset = 5.dp
private val SplitOrbRingStroke = 2.dp
private const val SplitOrbTilt = -14f
private val DockLinkSeamCorner = 10.dp
private val DockLinkFusedCorner = 4.dp
private val ConnectedPreviewHeight = 64.dp

@Stable
internal class DockLink(
    val attach: () -> Float,
    val fuse: () -> Float,
    val outerCorner: Dp
)

@Composable
internal fun rememberConnectedDockLink(
    navigation: LevyraNavigationStudioSettings,
    attached: Boolean,
    compaction: State<Float>,
    animated: Boolean
): DockLink {
    val attach = animateFloatAsState(
        targetValue = if (attached) 1f else 0f,
        animationSpec = LevyraMotion.physics(animated, LevyraMotion.spatial),
        label = "connected-dock-attach"
    )
    val outer = remember(navigation) {
        val tuning = navigation.tuningFor(NavStyle.ConnectedDock)
        minOf(tuning.cornerDp, tuning.heightDp / 2).dp
    }
    return remember(attach, compaction, outer) {
        DockLink(attach = { attach.value }, fuse = { compaction.value }, outerCorner = outer)
    }
}

internal fun dockLinkInnerCorner(outer: Dp, attach: Float, fuse: Float): Dp {
    val joined = lerp(DockLinkSeamCorner, DockLinkFusedCorner, fuse.coerceIn(0f, 1f))
    return lerp(outer, joined, attach.coerceIn(0f, 1f))
}

private data class DockBlendCorner(val rest: Dp, val target: Dp, val fraction: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        val half = shapeSize.minDimension / 2f
        val from = with(density) { rest.toPx() }
        val to = if (target.isSpecified) with(density) { target.toPx() } else half
        return (from + (to - from) * fraction.coerceIn(0f, 1f)).coerceIn(0f, half)
    }
}

internal fun Modifier.dockLinkedClip(elevation: Dp, rim: Color, shape: () -> Shape): Modifier =
    graphicsLayer {
        this.shape = shape()
        clip = true
        shadowElevation = elevation.toPx()
        ambientShadowColor = DockLinkAmbientShadow
        spotShadowColor = DockLinkSpotShadow
    }.drawWithContent {
        drawContent()
        if (rim.alpha > 0f) {
            drawOutline(
                outline = shape().createOutline(size, layoutDirection, this),
                color = rim,
                style = Stroke(width = LevyraPlayerDesign.Hairline.toPx() * 2f)
            )
        }
    }

private val DockLinkAmbientShadow = Color.Black.copy(alpha = 0.22f)
private val DockLinkSpotShadow = Color.Black.copy(alpha = 0.32f)

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
    val seed = if (tuning.colorSource == NavColorSource.Artwork) rememberAnimatedNavArtworkSeed() else null
    return remember(scheme, style.floating, tuning.colorSource, tuning.accent, tuning.surfaceTone, tuning.indicatorTone, tuning.inactiveAlpha, seed) {
        buildNavColors(scheme, style.floating, tuning, seed)
    }
}

@Composable
private fun rememberAnimatedNavArtworkSeed(): NavArtworkSeed? {
    val target = LocalNavArtworkSeed.current ?: return null
    val spec = LevyraPlayerDesign.motion(LocalAnimationsEnabled.current, LevyraPlayerDesign.paletteTween<Color>())
    val primary by animateColorAsState(target.primary, spec, label = "nav-artwork-primary")
    val secondary by animateColorAsState(target.secondary, spec, label = "nav-artwork-secondary")
    return NavArtworkSeed(primary, secondary)
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

private fun NavPalette.tintedBy(tint: Color): NavPalette {
    val amount = if (isLight) 0.05f else 0.08f
    val tintedSurface = surface.playerMix(tint, amount)
    return NavPalette(
        surface = tintedSurface,
        content = content,
        variant = variant,
        raised = raised.playerMix(tint, amount),
        isLight = tintedSurface.luminance() > 0.5f
    )
}

private fun buildNavColors(scheme: ColorScheme, capsule: Boolean, tuning: NavTuning, seed: NavArtworkSeed?): NavColors {
    val artwork = seed.takeIf { tuning.colorSource == NavColorSource.Artwork }
    val basePalette = navPalette(scheme, capsule, tuning.surfaceTone)
    val palette = if (artwork != null && tuning.surfaceTone != NavSurfaceTone.Amoled) basePalette.tintedBy(artwork.secondary) else basePalette
    val surface = palette.surface
    val isLight = palette.isLight
    val custom = tuning.colorSource == NavColorSource.Custom
    val accent = when {
        custom -> Color(tuning.accent)
        artwork != null -> artwork.primary
        else -> scheme.primary
    }
    val themedTonal = tuning.surfaceTone == NavSurfaceTone.Auto && !custom && artwork == null
    val (indicator, onIndicatorSeed) = navIndicatorPair(tuning.indicatorTone, scheme, palette, accent, themedTonal)
    val mark = accent.navReadableOn(surface, NavIconContrast)
    val inactiveBase = if (tuning.indicator == NavIndicator.None) palette.content else palette.variant
    val inactive = inactiveBase.navAlphaReadableOn(surface, tuning.inactiveAlpha / 100f, NavIconContrast)
    return NavColors(
        surface = surface,
        raised = palette.raised,
        rim = when {
            artwork != null -> artwork.primary.copy(alpha = if (isLight) 0.16f else 0.20f)
            isLight -> Color.Black.copy(alpha = 0.08f)
            else -> Color.White.copy(alpha = 0.10f)
        },
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
            .then(if (style == NavStyle.YtMusic) Modifier else Modifier.border(LevyraPlayerDesign.Hairline, colors.rim, shape)),
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
            refractionShape = shape,
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
    NavLabelMode.Never, NavLabelMode.Expanding -> 0.dp
}

private fun navIconTop(height: Dp, labelRoom: Dp): Dp =
    ((height - StyledNavIconBox - labelRoom) / 2).coerceAtLeast(4.dp)

private fun navLabelShown(mode: NavLabelMode, selected: Boolean): Boolean = when (mode) {
    NavLabelMode.Always, NavLabelMode.Compact -> true
    NavLabelMode.ActiveOnly, NavLabelMode.Expanding -> selected
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

private fun navDockGeometry(floating: Boolean, collapsible: Boolean, height: Dp, labels: NavLabelMode): NavDockGeometry {
    val compactHeight = if (!collapsible) {
        height
    } else if (floating) {
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
    depth: Float,
    link: DockLink?,
    outer: Dp
): Modifier = when {
    style == NavStyle.LiquidGlass -> Modifier.navLiquidGlass(colors, opacity, glass, shape, depth)
    style == NavStyle.ConnectedDock -> Modifier
        .dockLinkedClip((6f + 10f * depth).dp, colors.rim) { connectedNavShape(outer, link) }
        .navBackground(background, colors, opacity, glass)
    style.floating -> Modifier.navCapsule(colors, background, opacity, glass, shape, depth)
    else -> Modifier
}

private fun connectedNavShape(outer: Dp, link: DockLink?): Shape {
    val inner = dockLinkInnerCorner(outer, link?.attach?.invoke() ?: 0f, link?.fuse?.invoke() ?: 0f)
    return RoundedCornerShape(topStart = inner, topEnd = inner, bottomEnd = outer, bottomStart = outer)
}

private fun splitCapsuleShape(outer: Dp, detach: Float): Shape {
    val seam = DockBlendCorner(SplitDockInnerCorner, outer, detach)
    return RoundedCornerShape(
        topStart = CornerSize(outer),
        topEnd = seam,
        bottomEnd = seam,
        bottomStart = CornerSize(outer)
    )
}

private fun splitOrbShape(detach: Float): Shape {
    val seam = DockBlendCorner(SplitDockInnerCorner, Dp.Unspecified, detach)
    return RoundedCornerShape(
        topStart = seam,
        topEnd = CornerSize(50),
        bottomEnd = CornerSize(50),
        bottomStart = seam
    )
}

private fun Modifier.navDockSizing(floating: Boolean, compaction: () -> Float, geometry: NavDockGeometry): Modifier =
    if (floating) {
        dockLerpHeight(compaction, geometry.height, geometry.compactHeight)
    } else {
        dockClipHeight(compaction, geometry.height - geometry.compactHeight).height(geometry.height)
    }

private fun Modifier.dockGapWidth(gap: () -> Dp): Modifier = layout { measurable, _ ->
    val width = gap().roundToPx().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(width, 0))
    layout(width, 0) { placeable.place(0, 0) }
}

@Composable
internal fun StyledBottomTabs(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    compaction: () -> Float,
    onSelect: (LevyraTab) -> Unit,
    glass: GlassBackdropState?,
    applySystemInset: Boolean = true,
    link: DockLink? = null
) {
    val allEntries = rememberLevyraTabEntries()
    val visibleEntries = remember(allEntries, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> allEntries.firstOrNull { it.tab == tab } }
    }
    val searchEntry = if (style == NavStyle.SplitDock) visibleEntries.firstOrNull { it.tab == LevyraTab.Search } else null
    val entries = remember(visibleEntries, searchEntry) {
        if (searchEntry == null) visibleEntries else visibleEntries.filterNot { it.tab == LevyraTab.Search }
    }
    if (entries.isEmpty()) return
    val colors = rememberNavColors(style, tuning)
    val animationsEnabled = LocalAnimationsEnabled.current
    val motion = remember(tuning.motion, tuning.intensity, animationsEnabled) {
        navMotionSpec(tuning.motion, tuning.intensity, animationsEnabled)
    }
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    var parkedIndex by remember { mutableIntStateOf(0) }
    if (selectedIndex >= 0) {
        SideEffect { parkedIndex = selectedIndex }
    }
    val restingIndex = if (selectedIndex >= 0) selectedIndex else parkedIndex.coerceIn(0, entries.lastIndex)
    val indicatorPosition = animateFloatAsState(
        targetValue = restingIndex.toFloat(),
        animationSpec = motion.position,
        label = "styled-nav-indicator"
    )
    val floating = style.floating
    val collapsible = style != NavStyle.YtMusic
    val geometry = remember(floating, collapsible, tuning.heightDp, tuning.labels) {
        navDockGeometry(floating, collapsible, tuning.heightDp.dp, tuning.labels)
    }
    val depth = tuning.intensity / 100f
    val outer = minOf(tuning.cornerDp.dp, geometry.height / 2)
    val expansion = remember(style, tuning.labels) {
        if (tuning.labels == NavLabelMode.Expanding) navExpansion(style) else null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (searchEntry != null) {
            SplitDockRow(
                entries = entries,
                searchEntry = searchEntry,
                selected = selected,
                restingIndex = restingIndex,
                position = { indicatorPosition.value },
                tuning = tuning,
                colors = colors,
                motion = motion,
                geometry = geometry,
                compaction = compaction,
                glass = glass,
                depth = depth,
                outer = outer,
                expansion = expansion,
                onSelect = onSelect
            )
        } else {
            val shape = remember(outer) { RoundedCornerShape(outer) }
            val surface = remember(style, colors, tuning.background, tuning.opacity, glass, shape, depth, link, outer) {
                navDockSurface(style, colors, tuning.background, tuning.opacity, glass, shape, depth, link, outer)
            }
            StyledTabCapsule(
                modifier = Modifier
                    .navFloatingInset(floating)
                    .fillMaxWidth()
                    .navDockSizing(floating, compaction, geometry)
                    .then(surface),
                entries = entries,
                selected = selected,
                indicatorIndex = selectedIndex,
                position = { indicatorPosition.value },
                indicatorAlpha = null,
                tuning = tuning,
                colors = colors,
                motion = motion,
                geometry = geometry,
                floating = floating,
                compaction = compaction,
                expansion = expansion,
                onSelect = onSelect
            )
        }
        if (applySystemInset) {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StyledTabCapsule(
    modifier: Modifier,
    entries: List<LevyraTabEntry>,
    selected: LevyraTab,
    indicatorIndex: Int,
    position: () -> Float,
    indicatorAlpha: (() -> Float)?,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    geometry: NavDockGeometry,
    floating: Boolean,
    compaction: () -> Float,
    expansion: NavExpansion?,
    onSelect: (LevyraTab) -> Unit
) {
    val padding = tuning.paddingDp.dp
    val spacing = tuning.spacingDp.dp
    BoxWithConstraints(modifier = modifier) {
        val slotWidth = ((maxWidth - padding * 2 - spacing * (entries.size - 1)) / entries.size).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(geometry.height)
                .graphicsLayer { translationY = -geometry.lift.toPx() * compaction() }
        ) {
            if (expansion != null) {
                ExpandingTabRow(
                    entries = entries,
                    selected = selected,
                    tuning = tuning,
                    colors = colors,
                    motion = motion,
                    geometry = geometry,
                    expansion = expansion,
                    floating = floating,
                    compaction = compaction,
                    indicatorAlpha = indicatorAlpha,
                    onSelect = onSelect
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (indicatorAlpha != null) Modifier.graphicsLayer { alpha = indicatorAlpha() } else Modifier)
                        .drawBehind {
                            if (indicatorIndex < 0) return@drawBehind
                            val fold = compaction().coerceIn(0f, 1f)
                            drawNavIndicator(
                                tuning = tuning,
                                floating = floating,
                                startPx = padding.toPx(),
                                slotPx = slotWidth.toPx(),
                                gapPx = spacing.toPx(),
                                position = position(),
                                selectedIndex = indicatorIndex,
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
    }
}

@Composable
private fun SplitDockRow(
    entries: List<LevyraTabEntry>,
    searchEntry: LevyraTabEntry,
    selected: LevyraTab,
    restingIndex: Int,
    position: () -> Float,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    geometry: NavDockGeometry,
    compaction: () -> Float,
    glass: GlassBackdropState?,
    depth: Float,
    outer: Dp,
    expansion: NavExpansion?,
    onSelect: (LevyraTab) -> Unit
) {
    val searchSelected = selected == searchEntry.tab
    val detach = animateFloatAsState(
        targetValue = if (searchSelected) 1f else 0f,
        animationSpec = motion.position,
        label = "split-dock-detach"
    )
    val capsuleSurface = remember(colors, tuning.background, tuning.opacity, glass, depth, outer, detach) {
        Modifier
            .dockLinkedClip((6f + 12f * depth).dp, colors.rim) { splitCapsuleShape(outer, detach.value) }
            .navBackground(tuning.background, colors, tuning.opacity, glass)
    }
    Row(
        modifier = Modifier
            .navFloatingInset(true)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledTabCapsule(
            modifier = Modifier
                .weight(1f)
                .navDockSizing(true, compaction, geometry)
                .then(capsuleSurface),
            entries = entries,
            selected = selected,
            indicatorIndex = restingIndex,
            position = position,
            indicatorAlpha = { 1f - detach.value.coerceIn(0f, 1f) },
            tuning = tuning,
            colors = colors,
            motion = motion,
            geometry = geometry,
            floating = true,
            compaction = compaction,
            expansion = expansion,
            onSelect = onSelect
        )
        Spacer(
            modifier = Modifier.dockGapWidth {
                lerp(SplitDockJoinedGap, SplitDockDetachedGap, detach.value.coerceIn(0f, 1f))
            }
        )
        SplitSearchOrb(
            entry = searchEntry,
            isSelected = searchSelected,
            detach = detach,
            tuning = tuning,
            colors = colors,
            motion = motion,
            geometry = geometry,
            compaction = compaction,
            glass = glass,
            depth = depth,
            onSelect = onSelect
        )
    }
}

@Composable
private fun SplitSearchOrb(
    entry: LevyraTabEntry,
    isSelected: Boolean,
    detach: State<Float>,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    geometry: NavDockGeometry,
    compaction: () -> Float,
    glass: GlassBackdropState?,
    depth: Float,
    onSelect: (LevyraTab) -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint by animateColorAsState(
        targetValue = navTabIconTint(tuning, colors, isSelected),
        animationSpec = motion.tint,
        label = "split-orb-icon"
    )
    val scale by animateFloatAsState(
        targetValue = navTabScale(pressed, isSelected, motion),
        animationSpec = motion.scale,
        label = "split-orb-scale"
    )
    val tilt by animateFloatAsState(
        targetValue = if (isSelected && motion.animated) SplitOrbTilt else 0f,
        animationSpec = motion.scale,
        label = "split-orb-tilt"
    )
    val bloom by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = motion.scale,
        label = "split-orb-bloom"
    )
    val surface = remember(colors, tuning.background, tuning.opacity, glass, depth, detach) {
        Modifier
            .dockLinkedClip((6f + 12f * depth).dp, colors.rim) { splitOrbShape(detach.value) }
            .navBackground(tuning.background, colors, tuning.opacity, glass)
    }
    val filled = tuning.indicator.filled
    Box(
        modifier = Modifier
            .dockLerpSize(compaction, geometry.height, geometry.compactHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = entry.label
            }
            .then(surface)
            .drawBehind { drawSplitOrbMark(bloom, filled, colors) }
            .levyraPressable(
                onClick = {
                    if (!isSelected && tuning.haptics) {
                        haptics.perform(if (motion.pop > 1f) LevyraHapticAction.Confirm else LevyraHapticAction.SeekSnap)
                    }
                    onSelect(entry.tab)
                },
                interactionSource = interactionSource,
                pressedScale = if (motion.animated) 0.94f else 1f
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) entry.selectedIcon else entry.unselectedIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(tuning.iconDp.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt
                }
        )
    }
}

private fun DrawScope.drawSplitOrbMark(bloom: Float, filled: Boolean, colors: NavColors) {
    val presence = bloom.coerceIn(0f, 1f)
    val radius = size.minDimension / 2f - SplitOrbMarkInset.toPx()
    if (presence <= 0f || radius <= 0f) return
    if (filled) {
        val markRadius = radius * (0.55f + 0.45f * bloom.coerceIn(0f, 1.08f))
        drawCircle(color = colors.indicator, radius = markRadius, alpha = presence)
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = if (colors.isLight) 0.30f else 0.10f), Color.Transparent),
                startY = center.y - markRadius,
                endY = center.y + markRadius * 0.2f
            ),
            radius = markRadius,
            alpha = presence
        )
    } else {
        val stroke = SplitOrbRingStroke.toPx()
        drawCircle(
            color = colors.mark,
            radius = radius - stroke / 2f,
            alpha = presence,
            style = Stroke(width = stroke)
        )
    }
}

@Composable
internal fun ConnectedDockPreviewSlab(tuning: NavTuning) {
    val colors = rememberNavColors(NavStyle.ConnectedDock, tuning)
    val outer = minOf(tuning.cornerDp.dp, ConnectedPreviewHeight / 2)
    val shape = remember(outer) {
        RoundedCornerShape(topStart = outer, topEnd = outer, bottomEnd = DockLinkSeamCorner, bottomStart = DockLinkSeamCorner)
    }
    Row(
        modifier = Modifier
            .padding(start = StyledNavFloatingGutter, end = StyledNavFloatingGutter, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth()
            .height(ConnectedPreviewHeight)
            .clip(shape)
            .background(colors.raised)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.indicator)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(colors.inactive.copy(alpha = 0.55f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colors.inactive.copy(alpha = 0.32f))
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.indicator)
        )
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
    val bend = travel * stretch
    when (tuning.indicator) {
        NavIndicator.Pill -> drawNavPill(centerX, iconCenterY, slotPx, iconBox, scale, 0f, colors.indicator)
        NavIndicator.Morph -> drawNavPill(centerX, iconCenterY, slotPx, iconBox, scale, bend, colors.indicator)
        NavIndicator.Circle -> drawCircle(
            color = colors.indicator,
            radius = 20.dp.toPx() * scale * (1f - 0.12f * bend),
            center = Offset(centerX, iconCenterY)
        )
        NavIndicator.Floating -> {
            val inset = if (floating) 5.dp.toPx() else 8.dp.toPx()
            val width = (slotPx - 6.dp.toPx()).coerceAtLeast(iconBox) * (0.88f + 0.12f * scale) + slotPx * 0.25f * bend
            val pillHeight = (visibleHeightPx - inset * 2f).coerceAtLeast(iconBox)
            val radius = minOf(tuning.cornerDp.dp.toPx() - inset, pillHeight / 2f).coerceAtLeast(10.dp.toPx())
            drawNavFloatingPill(
                topLeft = Offset(centerX - width / 2f, visibleTopPx + inset),
                pillSize = Size(width, pillHeight),
                radius = radius,
                shadowDrop = 2.dp.toPx() * scale,
                colors = colors
            )
        }
        NavIndicator.Dot -> drawCircle(
            color = colors.mark,
            radius = 2.5.dp.toPx() * scale,
            center = Offset(centerX, iconTopPx + iconBox + labelRoomPx + 5.dp.toPx())
        )
        NavIndicator.Line -> {
            val width = (22.dp.toPx() + 18.dp.toPx() * bend) * scale
            val lineHeight = 3.dp.toPx()
            val bottomGap = if (floating) 5.dp.toPx() else 6.dp.toPx()
            drawRoundRect(
                color = colors.mark,
                topLeft = Offset(centerX - width / 2f, visibleTopPx + visibleHeightPx - lineHeight - bottomGap),
                size = Size(width, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
        NavIndicator.None -> Unit
    }
}

private fun DrawScope.drawNavPill(
    centerX: Float,
    centerY: Float,
    slotPx: Float,
    iconBox: Float,
    scale: Float,
    morph: Float,
    color: Color
) {
    val base = (slotPx - 20.dp.toPx()).coerceIn(48.dp.toPx(), 64.dp.toPx()) * scale
    val width = base + slotPx * morph
    val pillHeight = iconBox * scale * (1f - 0.18f * morph)
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - width / 2f, centerY - pillHeight / 2f),
        size = Size(width, pillHeight),
        cornerRadius = CornerRadius(pillHeight / 2f * (1f - 0.42f * morph))
    )
}

private fun DrawScope.drawNavFloatingPill(
    topLeft: Offset,
    pillSize: Size,
    radius: Float,
    shadowDrop: Float,
    colors: NavColors,
    alpha: Float = 1f
) {
    val corner = CornerRadius(radius)
    drawRoundRect(
        color = Color.Black.copy(alpha = if (colors.isLight) 0.08f else 0.22f),
        topLeft = topLeft + Offset(0f, shadowDrop),
        size = pillSize,
        cornerRadius = corner,
        alpha = alpha
    )
    drawRoundRect(color = colors.indicator, topLeft = topLeft, size = pillSize, cornerRadius = corner, alpha = alpha)
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (colors.isLight) 0.30f else 0.10f), Color.Transparent),
            startY = topLeft.y,
            endY = topLeft.y + pillSize.height * 0.6f
        ),
        topLeft = topLeft,
        size = pillSize,
        cornerRadius = corner,
        alpha = alpha
    )
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
            StyledTabIcon(
                entry = entry,
                selected = isSelected,
                pressed = pressed,
                tuning = tuning,
                colors = colors,
                motion = motion
            )
            if (tuning.labels != NavLabelMode.Never) {
                StyledTabLabel(
                    text = entry.label,
                    selected = isSelected,
                    tuning = tuning,
                    colors = colors,
                    motion = motion,
                    foldLabel = foldLabel
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

private fun navTabIconTint(tuning: NavTuning, colors: NavColors, selected: Boolean): Color = when {
    !selected -> colors.inactive
    tuning.indicator.filled -> colors.onIndicator
    else -> colors.mark
}

private fun navTabTextTint(tuning: NavTuning, colors: NavColors, selected: Boolean): Color = when {
    !selected -> colors.inactiveText
    tuning.indicator == NavIndicator.Floating -> colors.onIndicator
    else -> colors.markText
}

@Composable
private fun StyledTabIcon(
    entry: LevyraTabEntry,
    selected: Boolean,
    pressed: Boolean,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec
) {
    val tint by animateColorAsState(
        targetValue = navTabIconTint(tuning, colors, selected),
        animationSpec = motion.tint,
        label = "styled-tab-icon"
    )
    val scale by animateFloatAsState(
        targetValue = navTabScale(pressed, selected, motion),
        animationSpec = motion.scale,
        label = "styled-tab-scale"
    )
    Box(
        modifier = Modifier.height(StyledNavIconBox),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(tuning.iconDp.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun StyledTabLabel(
    text: String,
    selected: Boolean,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    foldLabel: (() -> Float)?
) {
    val color by animateColorAsState(
        targetValue = navTabTextTint(tuning, colors, selected),
        animationSpec = motion.tint,
        label = "styled-tab-text"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (navLabelShown(tuning.labels, selected)) 1f else 0f,
        animationSpec = motion.fade,
        label = "styled-tab-label"
    )
    val compact = tuning.labels == NavLabelMode.Compact
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
            .graphicsLayer {
                val fold = foldLabel?.invoke()?.coerceIn(0f, 1f) ?: 0f
                alpha = labelAlpha * (1f - fold)
            }
    )
}

@Immutable
internal data class NavExpansion(
    val grow: Float,
    val labelSize: TextUnit,
    val gap: Dp,
    val pillHeight: Dp,
    val inset: Dp,
    val morph: Boolean,
    val glass: Boolean
)

internal fun navExpansion(style: NavStyle): NavExpansion = when (style) {
    NavStyle.SuperExpressive -> NavExpansion(1.25f, 12.sp, 8.dp, 44.dp, 4.dp, morph = true, glass = false)
    NavStyle.FloatingDock -> NavExpansion(1.10f, 12.sp, 6.dp, 44.dp, 4.dp, morph = false, glass = false)
    NavStyle.SplitDock -> NavExpansion(1.00f, 12.sp, 6.dp, 44.dp, 4.dp, morph = false, glass = false)
    NavStyle.ConnectedDock -> NavExpansion(0.95f, 12.sp, 6.dp, 40.dp, 4.dp, morph = false, glass = false)
    NavStyle.LiquidGlass -> NavExpansion(1.00f, 12.sp, 6.dp, 44.dp, 4.dp, morph = false, glass = true)
    NavStyle.Minimal -> NavExpansion(0.70f, 11.sp, 6.dp, 36.dp, 6.dp, morph = false, glass = false)
    NavStyle.ExpressiveCompact -> NavExpansion(0.60f, 11.sp, 4.dp, 34.dp, 3.dp, morph = true, glass = false)
    NavStyle.Original, NavStyle.YtMusic -> NavExpansion(0.80f, 12.sp, 6.dp, 40.dp, 4.dp, morph = false, glass = false)
}

@Composable
private fun ExpandingTabRow(
    entries: List<LevyraTabEntry>,
    selected: LevyraTab,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    geometry: NavDockGeometry,
    expansion: NavExpansion,
    floating: Boolean,
    compaction: () -> Float,
    indicatorAlpha: (() -> Float)?,
    onSelect: (LevyraTab) -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val fractions = entries.map { entry ->
        key(entry.tab) {
            animateFloatAsState(
                targetValue = if (entry.tab == selected) 1f else 0f,
                animationSpec = motion.position,
                label = "nav-expand"
            )
        }
    }
    val gap = tuning.spacingDp.dp
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = tuning.paddingDp.dp)
            .selectableGroup(),
        content = {
            entries.forEachIndexed { index, entry ->
                key(entry.tab) {
                    val isSelected = entry.tab == selected
                    val fraction = fractions[index]
                    ExpandingTabButton(
                        entry = entry,
                        isSelected = isSelected,
                        fraction = { fraction.value },
                        tuning = tuning,
                        colors = colors,
                        motion = motion,
                        geometry = geometry,
                        expansion = expansion,
                        floating = floating,
                        compaction = compaction,
                        indicatorAlpha = indicatorAlpha,
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
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val available = (width - gapPx * (measurables.size - 1)).coerceAtLeast(0)
        val weights = fractions.map { 1f + expansion.grow * it.value.coerceIn(0f, 1.2f) }
        val total = weights.sum().coerceAtLeast(1f)
        var assigned = 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val slot = if (index == measurables.lastIndex) {
                available - assigned
            } else {
                (available * weights[index] / total).roundToInt()
            }.coerceAtLeast(0)
            assigned += slot
            measurable.measure(Constraints.fixed(slot, constraints.maxHeight))
        }
        layout(width, constraints.maxHeight) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += placeable.width + gapPx
            }
        }
    }
}

@Composable
private fun ExpandingTabButton(
    entry: LevyraTabEntry,
    isSelected: Boolean,
    fraction: () -> Float,
    tuning: NavTuning,
    colors: NavColors,
    motion: NavMotionSpec,
    geometry: NavDockGeometry,
    expansion: NavExpansion,
    floating: Boolean,
    compaction: () -> Float,
    indicatorAlpha: (() -> Float)?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val textColor by animateColorAsState(
        targetValue = navExpandingTextTint(tuning, colors, isSelected),
        animationSpec = motion.tint,
        label = "nav-expand-text"
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = entry.label
            }
            .drawBehind {
                val fold = compaction().coerceIn(0f, 1f)
                drawExpandingIndicator(
                    tuning = tuning,
                    colors = colors,
                    expansion = expansion,
                    floating = floating,
                    fraction = fraction().coerceIn(0f, 1.2f),
                    iconCenterY = geometry.iconTop.toPx() + StyledNavIconBox.toPx() / 2f,
                    visibleTop = geometry.lift.toPx() * fold,
                    visibleHeight = size.height - (geometry.height - geometry.compactHeight).toPx() * fold,
                    alpha = indicatorAlpha?.invoke()?.coerceIn(0f, 1f) ?: 1f
                )
            }
            .levyraPressable(
                onClick = onClick,
                interactionSource = interactionSource,
                pressedScale = if (motion.animated) 0.96f else 1f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .padding(top = geometry.iconTop, start = expansion.inset + 6.dp, end = expansion.inset + 6.dp)
                .height(StyledNavIconBox),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledTabIcon(
                entry = entry,
                selected = isSelected,
                pressed = pressed,
                tuning = tuning,
                colors = colors,
                motion = motion
            )
            Spacer(modifier = Modifier.dockGapWidth { expansion.gap * fraction().coerceIn(0f, 1f) })
            Text(
                text = entry.label,
                color = textColor,
                fontSize = expansion.labelSize,
                lineHeight = LevyraTypeRhythm.lineHeight(expansion.labelSize),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0))
                        val revealed = (placeable.width * fraction().coerceIn(0f, 1f)).roundToInt()
                        layout(revealed, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .clipToBounds()
                    .graphicsLayer {
                        val reveal = fraction().coerceIn(0f, 1f)
                        alpha = reveal * reveal
                    }
            )
        }
    }
}

private fun navExpandingTextTint(tuning: NavTuning, colors: NavColors, selected: Boolean): Color = when {
    !selected -> colors.inactiveText
    tuning.indicator.filled -> colors.onIndicator
    else -> colors.markText
}

private fun DrawScope.drawExpandingIndicator(
    tuning: NavTuning,
    colors: NavColors,
    expansion: NavExpansion,
    floating: Boolean,
    fraction: Float,
    iconCenterY: Float,
    visibleTop: Float,
    visibleHeight: Float,
    alpha: Float
) {
    val presence = (fraction * 1.6f).coerceIn(0f, 1f) * alpha
    if (presence <= 0f) return
    val grown = fraction.coerceIn(0f, 1f)
    val inset = expansion.inset.toPx()
    val iconBox = StyledNavIconBox.toPx()
    val scale = tuning.indicatorScale / 100f
    val centerX = size.width / 2f
    val fullWidth = (size.width - inset * 2f).coerceAtLeast(iconBox)
    val restWidth = minOf(iconBox * 1.5f, fullWidth)
    val width = restWidth + (fullWidth - restWidth) * grown
    when (tuning.indicator) {
        NavIndicator.Pill, NavIndicator.Morph, NavIndicator.Circle -> {
            val height = minOf(expansion.pillHeight.toPx() * scale, visibleHeight - 8.dp.toPx()).coerceAtLeast(iconBox)
            val bend = if (tuning.indicator == NavIndicator.Morph || expansion.morph) grown else 0f
            drawRoundRect(
                color = colors.indicator,
                topLeft = Offset(centerX - width / 2f, iconCenterY - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f * (1f - 0.36f * bend)),
                alpha = presence
            )
        }
        NavIndicator.Floating -> {
            val edge = if (floating) 5.dp.toPx() else 8.dp.toPx()
            val height = (visibleHeight - edge * 2f).coerceAtLeast(iconBox)
            val radius = minOf(tuning.cornerDp.dp.toPx() - edge, height / 2f).coerceAtLeast(10.dp.toPx())
            val topLeft = Offset(centerX - width / 2f, visibleTop + edge)
            if (expansion.glass) {
                drawNavFloatingPill(topLeft, Size(width, height), radius, 2.dp.toPx() * scale, colors, presence)
            } else {
                drawRoundRect(
                    color = colors.indicator,
                    topLeft = topLeft,
                    size = Size(width, height),
                    cornerRadius = CornerRadius(radius),
                    alpha = presence
                )
            }
        }
        NavIndicator.Dot, NavIndicator.Line -> {
            val line = tuning.indicator == NavIndicator.Line
            val markHeight = if (line) 3.dp.toPx() else 4.dp.toPx() * scale
            val markWidth = markHeight + (width * 0.42f - markHeight).coerceAtLeast(0f) * grown
            val y = if (line) {
                visibleTop + visibleHeight - markHeight - if (floating) 5.dp.toPx() else 6.dp.toPx()
            } else {
                iconCenterY + iconBox / 2f + 3.dp.toPx()
            }
            drawRoundRect(
                color = colors.mark,
                topLeft = Offset(centerX - markWidth / 2f, y),
                size = Size(markWidth, markHeight),
                cornerRadius = CornerRadius(markHeight / 2f),
                alpha = presence
            )
        }
        NavIndicator.None -> Unit
    }
}
