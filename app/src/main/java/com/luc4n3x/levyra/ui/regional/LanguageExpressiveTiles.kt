package com.luc4n3x.levyra.ui.regional

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraLanguageOption
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LevyraLanguagePickerStrings

private const val HERO_SPIN_MS = 28_000

internal fun languageTileIsWide(position: Int): Boolean = position % 5 == 0

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun expressiveBadgeShape(seed: Int, selected: Boolean) = when {
    selected -> MaterialShapes.Cookie12Sided
    else -> when (Math.floorMod(seed, 8)) {
        0 -> MaterialShapes.Cookie4Sided
        1 -> MaterialShapes.Clover4Leaf
        2 -> MaterialShapes.Sunny
        3 -> MaterialShapes.Cookie6Sided
        4 -> MaterialShapes.Flower
        5 -> MaterialShapes.Gem
        6 -> MaterialShapes.SoftBurst
        else -> MaterialShapes.Cookie9Sided
    }
}.toShape()

@Composable
private fun tonalPair(seed: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (Math.floorMod(seed, 4)) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        2 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurface
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressiveLanguageTile(
    language: LevyraLanguageOption,
    position: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (restBackground, restContent) = tonalPair(position)
    val background by animateColorAsState(if (selected) scheme.primary else restBackground, label = "language-tile-color")
    val content = if (selected) scheme.onPrimary else restContent
    val motion = rememberExpressiveTileMotion(pressed, selected)
    val corner by motion.corner
    val scale by motion.scale
    val turn by motion.turn
    val metrics = if (languageTileIsWide(position)) WideTileMetrics else RegularTileMetrics
    val shape = RoundedCornerShape(corner)
    Box(
        Modifier.fillMaxWidth().heightIn(min = metrics.minHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .selectable(selected = selected, interactionSource = interaction, indication = ripple(), role = Role.RadioButton, onClick = onClick)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = metrics.badgeOffsetX, y = metrics.badgeOffsetY).size(metrics.badgeSize)
                .graphicsLayer { rotationZ = turn }
                .background(content.copy(alpha = 0.16f), expressiveBadgeShape(position, selected))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(language.flag, fontSize = metrics.flagSize, modifier = Modifier.graphicsLayer { rotationZ = -turn })
        }
        ExpressiveTileCheck(selected, Modifier.align(Alignment.TopStart).padding(12.dp))
        ExpressiveTileTexts(language, content, metrics, Modifier.align(Alignment.BottomStart))
    }
}

private class ExpressiveTileMotion(val corner: State<Dp>, val scale: State<Float>, val turn: State<Float>)

@Composable
private fun rememberExpressiveTileMotion(pressed: Boolean, selected: Boolean): ExpressiveTileMotion {
    val animations = LocalAnimationsEnabled.current
    val pressing = pressed && animations
    val cornerTarget = when {
        pressing -> 44.dp
        selected -> 40.dp
        else -> 28.dp
    }
    val corner = animateDpAsState(cornerTarget, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "language-tile-corner")
    val scale = animateFloatAsState(if (pressing) 0.95f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "language-tile-scale")
    val turn = animateFloatAsState(if (animations && (selected || pressed)) 72f else 0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "language-tile-turn")
    return ExpressiveTileMotion(corner, scale, turn)
}

private class ExpressiveTileMetrics(
    val minHeight: Dp,
    val badgeSize: Dp,
    val badgeOffsetX: Dp,
    val badgeOffsetY: Dp,
    val flagSize: TextUnit,
    val textWidthFraction: Float,
    val textTopPadding: Dp,
    val greetingSize: TextUnit,
    val greetingLineHeight: TextUnit
)

private val WideTileMetrics = ExpressiveTileMetrics(124.dp, 128.dp, 20.dp, (-14).dp, 46.sp, 0.68f, 40.dp, 28.sp, 32.sp)

private val RegularTileMetrics = ExpressiveTileMetrics(148.dp, 96.dp, 12.dp, (-12).dp, 34.sp, 1f, 72.dp, 22.sp, 26.sp)

@Composable
private fun ExpressiveTileCheck(selected: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = selected, modifier = modifier,
        enter = fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy)), exit = fadeOut() + scaleOut()
    ) {
        Box(Modifier.size(30.dp).background(scheme.onPrimary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ExpressiveTileTexts(language: LevyraLanguageOption, content: Color, metrics: ExpressiveTileMetrics, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth(metrics.textWidthFraction)
            .padding(start = 16.dp, end = 12.dp, top = metrics.textTopPadding, bottom = 14.dp)
    ) {
        Text(
            LevyraLanguagePickerStrings.greeting(language.code), color = content,
            fontSize = metrics.greetingSize, lineHeight = metrics.greetingLineHeight, fontWeight = FontWeight.Black,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            language.nativeName, color = content.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressiveLanguageHero(
    title: String,
    note: String?,
    selected: LevyraLanguageOption?,
    automatic: LanguagePickerAutomatic?
) {
    val scheme = MaterialTheme.colorScheme
    val animations = LocalAnimationsEnabled.current
    val spin = if (animations) {
        rememberInfiniteTransition(label = "language-hero").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(HERO_SPIN_MS, easing = LinearEasing)),
            label = "language-hero-spin"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).heightIn(min = 208.dp)
            .clip(RoundedCornerShape(40.dp)).background(scheme.primaryContainer)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-40).dp).size(232.dp)
                .graphicsLayer { rotationZ = spin.value }
                .background(scheme.primary, MaterialShapes.Cookie12Sided.toShape())
        )
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 150.dp, bottom = 14.dp).size(30.dp)
                .graphicsLayer { rotationZ = -spin.value * 2f }
                .background(scheme.tertiary, MaterialShapes.Clover4Leaf.toShape())
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 34.dp).size(96.dp).clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selected?.flag,
                transitionSpec = { fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy), 0.5f) togetherWith fadeOut() + scaleOut(targetScale = 1.3f) },
                label = "language-hero-flag"
            ) { flag ->
                if (flag != null) Text(flag, fontSize = 62.sp)
                else Icon(Icons.Rounded.GraphicEq, null, tint = scheme.onPrimary, modifier = Modifier.size(64.dp))
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title, color = scheme.onPrimaryContainer.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(0.6f).padding(bottom = 44.dp).semantics { heading() }
            )
            AnimatedContent(
                targetState = selected?.code,
                transitionSpec = { slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut() },
                label = "language-hero-greeting"
            ) { code ->
                Text(
                    code?.let(LevyraLanguagePickerStrings::greeting) ?: automatic?.label.orEmpty(),
                    color = scheme.onPrimaryContainer, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            selected?.let { Text(it.nativeName, color = scheme.onPrimaryContainer, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (note != null) Text(note, color = scheme.onPrimaryContainer.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
