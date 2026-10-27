package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.DockScrollEffect
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.ui.components.DockMorph
import com.luc4n3x.levyra.ui.components.LevyraDockState
import com.luc4n3x.levyra.ui.components.LevyraDockMorphLayout
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.levyraCookieLandingShape
import com.luc4n3x.levyra.ui.i18n.LevyraDockMorphCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.dockMorph

private const val DockAnchorEmphasis = 2.2f
private const val DockBadgeScale = 0.92f
private const val DockBadgeSeed = 0.55f
private const val DockBadgeSpin = -90f
private val DockEffectOuterCorner = 22.dp
private val DockEffectInnerCorner = 6.dp
private val DockEffectSelectedCorner = 28.dp
private val DockEffectMarkSize = 30.dp

@Composable
internal fun rememberDockMorph(active: Boolean, miniVisible: Boolean, compaction: State<Float>): DockMorph? {
    val miniShown = rememberUpdatedState(miniVisible)
    val morph = remember(compaction) { DockMorph { if (miniShown.value) compaction.value else 0f } }
    return morph.takeIf { active }
}

@Composable
internal fun rememberMorphedDockLink(link: DockLink, morph: DockMorph?): DockLink {
    if (morph == null) return link
    return remember(link, morph) {
        DockLink(
            attach = { link.attach() * (1f - morph.fraction) },
            fuse = link.fuse,
            outerCorner = link.outerCorner
        )
    }
}

@Composable
internal fun LevyraDockStack(
    morph: DockMorph?,
    sideBySide: Boolean,
    mini: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    search: DockSearchRoute? = null
) {
    val onSearch = search?.takeIf { it.wanted() }?.let { route ->
        {
            route.dockState.expand()
            route.select(LevyraTab.Search)
        }
    }
    if (morph == null) {
        if (!sideBySide) mini()
        dock()
        return
    }
    CompositionLocalProvider(LocalDockMorph provides morph) {
        LevyraDockMorphLayout(
            morph = morph,
            mini = mini,
            dock = dock,
            modifier = Modifier.fillMaxWidth(),
            trailing = { if (onSearch != null) DockSearchBloom(morph, onSearch) }
        )
    }
}

internal class DockSearchRoute(
    private val skin: LevyraSkin,
    private val navigation: LevyraNavigationStudioSettings,
    private val selected: LevyraTab,
    val dockState: LevyraDockState,
    val select: (LevyraTab) -> Unit
) {
    fun wanted(): Boolean {
        if (skin != LevyraSkin.Default || selected == LevyraTab.Search) return false
        if (navigation.style.separatesSearch) return false
        return navigation.style == NavStyle.Original || LevyraTab.Search in navigation.activeTuning().visibleOrder()
    }
}

@Composable
private fun animateDockEffectCorner(selected: Boolean, edge: Boolean, animated: Boolean, label: String): State<Dp> =
    animateDpAsState(
        targetValue = when {
            selected -> DockEffectSelectedCorner
            edge -> DockEffectOuterCorner
            else -> DockEffectInnerCorner
        },
        animationSpec = if (animated) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else snap(),
        label = label
    )

@Composable
private fun DockEffectMark(selected: Boolean, landing: () -> Float) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(DockEffectMarkSize)
            .graphicsLayer {
                rotationZ = -30f * landing()
                shape = levyraCookieLandingShape(landing())
                clip = true
            }
            .background(if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        val bloom = landing()
                        rotationZ = 30f * bloom
                        scaleX = bloom.coerceIn(0f, 1.2f)
                        scaleY = bloom.coerceIn(0f, 1.2f)
                    }
            )
        }
    }
}

@Composable
internal fun DockScrollEffectSkinRow(
    settings: LevyraInterfaceSettings,
    onSettings: (LevyraInterfaceSettings) -> Unit
) {
    val copy = LocalLevyraStrings.current.dockMorph()
    SettingsChoiceRow(
        icon = Icons.Rounded.Dashboard,
        title = copy.title,
        subtitle = copy.subtitle,
        options = dockScrollEffectOptions(copy),
        selected = settings.dockScrollEffect.storageId,
        onSelect = { onSettings(settings.copy(dockScrollEffect = DockScrollEffect.from(it))) }
    )
}

@Composable
private fun DockSearchBloom(morph: DockMorph, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val entry = rememberLevyraTabEntries().first { it.tab == LevyraTab.Search }
    val container = scheme.secondaryContainer
    Box(
        modifier = Modifier
            .graphicsLayer {
                val bloom = morph.fraction
                alpha = bloom
                scaleX = DockBadgeSeed + (1f - DockBadgeSeed) * bloom
                scaleY = DockBadgeSeed + (1f - DockBadgeSeed) * bloom
            }
            .drawBehind {
                drawDockMorphBadge(
                    center = Offset(size.width / 2f, size.height / 2f),
                    diameter = size.minDimension * DockBadgeScale,
                    fraction = morph.fraction,
                    color = container
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                contentDescription = entry.label
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = entry.selectedIcon,
            contentDescription = null,
            tint = scheme.onSecondaryContainer,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = DockBadgeSpin * (1f - morph.fraction) }
        )
    }
}

internal fun Modifier.dockMorphAnchorBackground(morph: DockMorph?, color: Color, shape: Shape): Modifier =
    if (morph == null) {
        background(color, shape)
    } else {
        drawBehind {
            val fraction = morph.fraction
            if (fraction <= 0f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), color)
                return@drawBehind
            }
            val target = if (morph.rowHeightPx > 0) morph.rowHeightPx * DockBadgeScale else size.height
            val width = size.width + (target - size.width) * fraction
            val height = size.height + (target - size.height) * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset((size.width - width) / 2f, (size.height - height) / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f),
                alpha = 1f - fraction
            )
            drawDockMorphBadge(
                center = Offset(size.width / 2f, size.height / 2f),
                diameter = target,
                fraction = fraction,
                color = color.copy(alpha = (color.alpha * DockAnchorEmphasis).coerceAtMost(1f))
            )
        }
    }

internal fun DrawScope.drawDockMorphBadge(center: Offset, diameter: Float, fraction: Float, color: Color) {
    val bloom = fraction.coerceIn(0f, 1f)
    if (bloom <= 0f || diameter <= 0f) return
    val side = diameter * (DockBadgeSeed + (1f - DockBadgeSeed) * bloom)
    val outline = levyraCookieLandingShape(bloom, DockBadgeSpin * (1f - bloom))
        .createOutline(Size(side, side), layoutDirection, this)
    translate(center.x - side / 2f, center.y - side / 2f) { drawOutline(outline, color, alpha = bloom) }
}

private fun dockEffectLabel(effect: DockScrollEffect, copy: LevyraDockMorphCopy): String = when (effect) {
    DockScrollEffect.Tide -> copy.tide
    DockScrollEffect.Merge -> copy.merge
    DockScrollEffect.Off -> copy.off
}

@Composable
internal fun DockScrollEffectGroup(selected: DockScrollEffect, onSelect: (DockScrollEffect) -> Unit) {
    val copy = LocalLevyraStrings.current.dockMorph()
    MiniStudioGroup(title = copy.title) {
        MiniStudioCaption(copy.subtitle)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val options = DockScrollEffect.entries
            options.forEachIndexed { index, effect ->
                DockScrollEffectOption(
                    label = dockEffectLabel(effect, copy),
                    description = copy.mergeSubtitle.takeIf { effect == DockScrollEffect.Merge },
                    selected = effect == selected,
                    first = index == 0,
                    last = index == options.lastIndex,
                    onClick = { onSelect(effect) }
                )
            }
        }
    }
}

@Composable
private fun DockScrollEffectOption(
    label: String,
    description: String?,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val landing by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "dock-effect-landing"
    )
    val top by animateDockEffectCorner(selected, first, animated, "dock-effect-top")
    val bottom by animateDockEffectCorner(selected, last, animated, "dock-effect-bottom")
    val container by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else scheme.surfaceContainerHighest,
        label = "dock-effect-container"
    )
    val content = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DockEffectMark(selected = selected, landing = { landing })
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                color = content
            )
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.78f))
            }
        }
    }
}

internal fun dockScrollEffectOptions(copy: LevyraDockMorphCopy): List<Pair<String, String>> =
    DockScrollEffect.entries.map { it.storageId to dockEffectLabel(it, copy) }
