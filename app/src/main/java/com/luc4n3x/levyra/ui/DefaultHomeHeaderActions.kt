package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.graphics.shapes.Morph
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

@Composable
internal fun DefaultHomeBrowseHeader(
    zones: List<ExploreZone>,
    chipState: LazyListState,
    loading: Boolean,
    onSelect: (ExploreZone) -> Unit,
    onSettings: () -> Unit,
    moodsSheet: @Composable (onDismiss: () -> Unit) -> Unit = {}
) {
    var actionsOpen by rememberSaveable { mutableStateOf(false) }
    var moodsOpen by rememberSaveable { mutableStateOf(false) }
    if (moodsOpen) moodsSheet { moodsOpen = false }
    BackHandler(enabled = actionsOpen) { actionsOpen = false }
    val logoHalf = LevyraHomeDesign.SettingsControlHeight / 2
    val animated = LocalAnimationsEnabled.current
    val chipLead by animateDpAsState(
        targetValue = if (actionsOpen) DefaultLogoPillOpenWidth - logoHalf + 10.dp else logoHalf + 8.dp,
        animationSpec = if (animated) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "defaultChipLead"
    )
    Box(Modifier.fillMaxWidth()) {
        HomeGenreChips(
            zones = zones,
            contentPadding = PaddingValues(start = chipLead),
            onSelect = { zone -> if (zone.id == DEFAULT_MOODS_CHIP) moodsOpen = true else onSelect(zone) },
            modifier = Modifier.padding(start = logoHalf),
            compactPills = true,
            state = chipState
        )
        if (actionsOpen) {
            Box(
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics { }
                    .clickable(interactionSource = null, indication = null) { actionsOpen = false }
            )
        }
        DefaultHomeLogoPill(
            open = actionsOpen,
            loading = loading,
            onToggle = { actionsOpen = !actionsOpen },
            onSettings = {
                actionsOpen = false
                onSettings()
            },
            onClose = { actionsOpen = false },
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun DefaultHomeLogoPill(
    open: Boolean,
    loading: Boolean,
    onToggle: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val navigation = LocalMusicExperiences.current
    val animated = LocalAnimationsEnabled.current
    val light = LevyraIsLight
    val pillAlpha by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (animated) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "defaultLogoPill"
    )
    val scheme = MaterialTheme.colorScheme
    val pillFill = lerp(scheme.surfaceContainerHigh, scheme.primary, 0.05f).copy(alpha = if (light) 0.97f else 0.95f)
    val pillEdge = scheme.outlineVariant.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .background(pillFill.copy(alpha = pillFill.alpha * pillAlpha), CircleShape)
            .border(1.dp, pillEdge.copy(alpha = pillEdge.alpha * pillAlpha), CircleShape)
            .padding(end = if (open) DefaultLogoPillEndPadding else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DefaultLogoPillGap)
    ) {
        DefaultHomeLogo(open = open, loading = loading, actionLabel = strings.options, onClick = onToggle)
        AnimatedVisibility(
            visible = open,
            enter = if (animated) {
                expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow), expandFrom = Alignment.Start) +
                    fadeIn() + scaleIn(initialScale = 0.6f)
            } else EnterTransition.None,
            exit = if (animated) {
                shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium), shrinkTowards = Alignment.Start) +
                    fadeOut() + scaleOut(targetScale = 0.6f)
            } else ExitTransition.None
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DefaultLogoPillActionGap)) {
                DefaultLogoPillAction(
                    Icons.Rounded.Replay,
                    "Levyra Rewind",
                    {
                        onClose()
                        navigation.rewind()
                    },
                    Modifier.testTag("open-rewind")
                )
                DefaultLogoPillAction(Icons.Rounded.Settings, strings.settings, onSettings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultHomeLogo(open: Boolean, loading: Boolean, actionLabel: String, onClick: () -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val turn by animateFloatAsState(
        targetValue = if (open) 360f else 0f,
        animationSpec = if (animated) spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessVeryLow) else snap(),
        label = "defaultLogoTurn"
    )
    val morph by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (animated) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "defaultLogoMorph"
    )
    val interaction = remember { MutableInteractionSource() }
    val press = rememberLogoPressScale(interaction, animated)
    val rotation = turn + rememberLogoLoadingSpin(loading && animated && LocalLevyraVisualCapabilities.current.decorativeMotion)
    val shape = remember(morph, rotation) { LevyraMorphShape(DefaultLogoMorph, morph, rotation) }
    val appName = stringResource(R.string.app_name)
    Box(
        modifier = Modifier
            .size(LevyraHomeDesign.SettingsControlHeight)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(shape)
            .vinylDisc(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.colorScheme.primary)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = actionLabel,
                onClick = onClick
            )
            .semantics { contentDescription = appName },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.levyra_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.18f
                    scaleY = 1.18f
                    rotationZ = rotation
                }
        )
    }
}

@Composable
private fun rememberLogoPressScale(interaction: MutableInteractionSource, animated: Boolean): Float {
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = if (animated) spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "defaultLogoPress"
    )
    return press
}

@Composable
private fun rememberLogoLoadingSpin(active: Boolean): Float {
    if (!active) return 0f
    val angle by rememberInfiniteTransition(label = "defaultLogoSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(DefaultLogoSpinMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "defaultLogoSpinAngle"
    )
    return angle
}

internal fun Modifier.vinylDisc(base: Color, tint: Color): Modifier = drawWithCache {
    val radius = size.minDimension / 2f
    val groove = Stroke(width = 0.6.dp.toPx())
    val disc = lerp(base, tint, 0.07f)
    val sheen = Brush.sweepGradient(
        0f to Color.Transparent,
        0.08f to Color.White.copy(alpha = 0.16f),
        0.18f to Color.Transparent,
        0.58f to Color.Transparent,
        0.66f to Color.White.copy(alpha = 0.07f),
        0.74f to Color.Transparent,
        1f to Color.Transparent,
        center = Offset(size.width / 2f, size.height / 2f)
    )
    val lift = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent))
    onDrawWithContent {
        drawRect(disc)
        DefaultLogoGrooves.forEachIndexed { index, share ->
            drawCircle(tint.copy(alpha = if (index % 2 == 0) 0.24f else 0.12f), radius * share, style = groove)
        }
        drawCircle(lift, radius)
        drawContent()
        drawCircle(sheen, radius)
    }
}

@Composable
internal fun DefaultHomeHeaderActions(loading: Boolean, onSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val navigation = LocalMusicExperiences.current
    val container = if (LevyraIsLight) Color.White.copy(alpha = 0.86f) else Color.Black.copy(alpha = 0.42f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HeaderToolbarButton(Icons.Rounded.Replay, "Levyra Rewind", navigation.rewind, Modifier.testTag("open-rewind").size(46.dp), container)
        HeaderToolbarButton(Icons.Rounded.Settings, strings.settings, onSettings, Modifier.size(46.dp), container, loading)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeaderToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = if (LevyraIsLight) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.10f),
    loading: Boolean = false
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            pressedShape = MaterialShapes.Cookie9Sided.toShape()
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = container,
            contentColor = LevyraText.copy(alpha = 0.94f)
        ),
        modifier = Modifier.size(HeaderToolbarButtonSize).then(modifier)
    ) {
        if (loading) {
            LevyraLoadingIndicator(modifier = Modifier.size(24.dp).semantics { contentDescription = label }, color = LevyraText)
        } else {
            Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLogoPillAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.iconButtonColors(contentColor = LevyraText.copy(alpha = 0.94f)),
        modifier = modifier.size(DefaultLogoPillActionSize)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val DefaultLogoMorph: Morph by lazy {
    Morph(MaterialShapes.Cookie12Sided.normalized(), MaterialShapes.Circle.normalized())
}
private val DefaultLogoGrooves = listOf(0.86f, 0.78f, 0.70f, 0.62f)
private const val DefaultLogoSpinMillis = 2_400
private val HeaderToolbarButtonSize = 42.dp
private val DefaultLogoPillGap = 2.dp
private val DefaultLogoPillActionGap = 0.dp
private val DefaultLogoPillActionSize = 48.dp
private val DefaultLogoPillEndPadding = 2.dp
private val DefaultLogoPillOpenWidth =
    LevyraHomeDesign.SettingsControlHeight + DefaultLogoPillGap + DefaultLogoPillActionSize * 2 + DefaultLogoPillActionGap + DefaultLogoPillEndPadding
