package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.theme.SkinManagedNotice

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.HomeDensity
import com.luc4n3x.levyra.domain.HomeMotion
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeSectionLayout
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.HomeStyleLayout
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.HomeStudioCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.i18n.homeVideoShelfTitle
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

internal fun homeStyleName(style: HomeStyle): String = when (style) {
    HomeStyle.Original -> "Original"
    HomeStyle.ExpressiveMax -> "Expressive MAX"
    HomeStyle.Flow -> "Flow"
    HomeStyle.Editorial -> "Editorial"
    HomeStyle.Minimal -> "Minimal"
    HomeStyle.Immersive -> "Immersive"
    HomeStyle.MaterialExpressive -> "Material Expressive"
}

internal fun homeStyleDescription(style: HomeStyle, copy: HomeStudioCopy): String = when (style) {
    HomeStyle.Original -> copy.descOriginal
    HomeStyle.ExpressiveMax -> copy.descExpressive
    HomeStyle.Flow -> copy.descFlow
    HomeStyle.Editorial -> copy.descEditorial
    HomeStyle.Minimal -> copy.descMinimal
    HomeStyle.Immersive -> copy.descImmersive
    HomeStyle.MaterialExpressive -> copy.descMaterial
}

internal fun homeSectionTitle(id: HomeSectionId, strings: LevyraStrings): String {
    val copy = strings.homeStudioCopy
    return when (id) {
        HomeSectionId.GenreChips -> copy.sectionGenres
        HomeSectionId.SpeedDial -> strings.speedDialCopy().title
        HomeSectionId.Spotlight -> copy.sectionSpotlight
        HomeSectionId.PersonalOrbit -> strings.yourOrbitSetting
        HomeSectionId.FavoriteRadio -> com.luc4n3x.levyra.ui.i18n.radioLibraryCopy(strings.code).favorites
        HomeSectionId.EditorialCollections -> copy.sectionCollections
        HomeSectionId.QuickPicks -> strings.quickPicks
        HomeSectionId.ForgottenFavorites -> strings.forgottenFavorites
        HomeSectionId.AlbumsForYou -> strings.albumsForYou
        HomeSectionId.TrendingArtists -> strings.trendingArtists
        HomeSectionId.NewReleases -> strings.newReleases
        HomeSectionId.MusicVideos -> strings.homeVideoShelfTitle
        HomeSectionId.ReleaseRadar -> strings.releaseRadar
        HomeSectionId.SimilarArtists -> strings.similarToFollowed
        HomeSectionId.MoreSections -> copy.sectionMore
        HomeSectionId.Resonance -> strings.voicesSetting
        HomeSectionId.Charts -> strings.top50Charts
    }
}

internal fun homeSectionGloballyEnabled(id: HomeSectionId, settings: LevyraInterfaceSettings): Boolean = when (id) {
    HomeSectionId.PersonalOrbit -> settings.showPersonalOrbit
    HomeSectionId.Resonance -> settings.showResonance
    HomeSectionId.NewReleases, HomeSectionId.ReleaseRadar -> settings.showNewReleases
    HomeSectionId.AlbumsForYou -> settings.showAlbumsForYou
    HomeSectionId.TrendingArtists, HomeSectionId.SimilarArtists -> settings.showTrendingArtists
    HomeSectionId.Charts -> settings.showCharts
    else -> true
}

internal fun LevyraInterfaceSettings.withHomeSectionEnabled(id: HomeSectionId): LevyraInterfaceSettings = when (id) {
    HomeSectionId.PersonalOrbit -> copy(showPersonalOrbit = true)
    HomeSectionId.Resonance -> copy(showResonance = true)
    HomeSectionId.NewReleases, HomeSectionId.ReleaseRadar -> copy(showNewReleases = true)
    HomeSectionId.AlbumsForYou -> copy(showAlbumsForYou = true)
    HomeSectionId.TrendingArtists, HomeSectionId.SimilarArtists -> copy(showTrendingArtists = true)
    HomeSectionId.Charts -> copy(showCharts = true)
    else -> this
}

private fun layoutLabel(layout: HomeSectionLayout, copy: HomeStudioCopy): String = when (layout) {
    HomeSectionLayout.Auto -> copy.layoutAuto
    HomeSectionLayout.Carousel -> copy.layoutCarousel
    HomeSectionLayout.Grid -> copy.layoutGrid
    HomeSectionLayout.List -> copy.layoutList
    HomeSectionLayout.Compact -> copy.layoutCompact
    HomeSectionLayout.Large -> copy.layoutLarge
}

@Composable
internal fun HomeStudioOverlay(
    settings: LevyraHomeStudioSettings,
    interfaceSettings: LevyraInterfaceSettings,
    onUpdate: (LevyraHomeStudioSettings) -> Unit,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.homeStudioCopy
    val haptics = LocalLevyraHaptics.current
    val latestSettings by rememberUpdatedState(settings)
    val update: (LevyraHomeStudioSettings) -> Unit = { next -> onUpdate(next) }
    val style = settings.style
    val layout = settings.activeLayout()
    var previewStyle by remember { mutableStateOf<HomeStyle?>(null) }
    var confirmResetAll by remember { mutableStateOf(false) }

    var dragging by remember { mutableStateOf<HomeSectionId?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var workingOrder by remember(style, layout.order) { mutableStateOf(layout.order) }
    val rowHeights = remember { mutableStateMapOf<HomeSectionId, Int>() }

    fun commitOrder(order: List<HomeSectionId>) {
        val current = latestSettings
        update(current.updateLayout(current.style) { it.copy(order = order) })
    }

    fun moveBy(id: HomeSectionId, delta: Int): Boolean {
        val index = workingOrder.indexOf(id)
        val target = index + delta
        if (index < 0 || target !in workingOrder.indices) return false
        val next = workingOrder.toMutableList().apply { add(target, removeAt(index)) }
        workingOrder = next
        commitOrder(next)
        haptics.perform(LevyraHapticAction.Reorder)
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "studio-top", contentType = "studio-top") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevyraBackButton(onClick = onClose, contentDescription = strings.back)
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = copy.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = LevyraText,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(text = copy.subtitle, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
                    }
                }
            }
            homeStudioAppearanceItems(style, copy, interfaceSettings, onInterfaceSettings) { previewStyle = it }
            item(key = "studio-sections-label", contentType = "studio-label") {
                Column {
                    HomeStudioLabel("${copy.sections} · ${homeStudioAppearanceName(style)}")
                    Text(text = copy.sectionsHint, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
                    Text(text = copy.featuredHint, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
                }
            }
            items(workingOrder, key = { "section-${it.storageId}" }, contentType = { "studio-section-row" }) { id ->
                val isDragging = dragging == id
                val visible = !layout.isHidden(id) && homeSectionGloballyEnabled(id, interfaceSettings)
                val index = workingOrder.indexOf(id)
                HomeStudioSectionRow(
                    title = homeSectionTitle(id, strings),
                    id = id,
                    visible = visible,
                    featured = id in layout.featured,
                    layout = layout.layoutFor(id),
                    copy = copy,
                    dragging = isDragging,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            val lifted = if (isDragging) 1.02f else 1f
                            scaleX = lifted
                            scaleY = lifted
                        }
                        .then(
                            if (isDragging) {
                                Modifier
                            } else {
                                Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        )
                        .onSizeChanged { rowHeights[id] = it.height }
                        .semantics {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(strings.speedDialCopy().moveEarlier) { moveBy(id, -1) }
                                    .takeIf { index > 0 },
                                CustomAccessibilityAction(strings.speedDialCopy().moveLater) { moveBy(id, 1) }
                                    .takeIf { index < workingOrder.lastIndex }
                            )
                        },
                    dragHandleModifier = Modifier.pointerInput(id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = id
                                dragOffset = 0f
                                haptics.perform(LevyraHapticAction.Confirm)
                            },
                            onDragEnd = {
                                dragging = null
                                dragOffset = 0f
                                commitOrder(workingOrder)
                            },
                            onDragCancel = {
                                dragging = null
                                dragOffset = 0f
                                commitOrder(workingOrder)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                reorderStep(workingOrder, id, dragOffset, rowHeights)?.let { (order, offset) ->
                                    workingOrder = order
                                    dragOffset = offset
                                    haptics.perform(LevyraHapticAction.Reorder)
                                }
                            }
                        )
                    },
                    onVisible = { show ->
                        val current = latestSettings
                        update(current.updateLayout(current.style) { it.withVisibility(id, show) })
                        if (show && !homeSectionGloballyEnabled(id, interfaceSettings)) {
                            onInterfaceSettings(interfaceSettings.withHomeSectionEnabled(id))
                        }
                    },
                    onFeatured = { on ->
                        val current = latestSettings
                        update(current.updateLayout(current.style) { it.withFeatured(id, on) })
                    },
                    onLayout = { chosen ->
                        val current = latestSettings
                        update(current.updateLayout(current.style) { it.withLayout(id, chosen) })
                    }
                )
            }
            homeStudioAdvancedItems(settings, style, layout, copy, update)
            item(key = "studio-reset", contentType = "studio-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { update(settings.resetStyle(style)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copy.resetStyle)
                    }
                    FilledTonalButton(
                        onClick = { confirmResetAll = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(copy.resetAll)
                    }
                }
            }
        }
    }

    previewStyle?.let { target ->
        HomeStylePreviewDialog(
            style = target,
            copy = copy,
            inUse = target == style,
            closeLabel = strings.back,
            onApply = {
                update(latestSettings.withStyle(target))
                haptics.perform(LevyraHapticAction.Confirm)
                previewStyle = null
            },
            onDismiss = { previewStyle = null }
        )
    }

    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text(copy.resetAll) },
            text = { Text(copy.resetAllConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    update(LevyraHomeStudioSettings())
                    confirmResetAll = false
                }) { Text(copy.reset) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetAll = false }) { Text(strings.cancel) }
            }
        )
    }
}

private fun LazyListScope.homeStudioAdvancedItems(
    settings: LevyraHomeStudioSettings,
    style: HomeStyle,
    layout: HomeStyleLayout,
    copy: HomeStudioCopy,
    update: (LevyraHomeStudioSettings) -> Unit
) {
    item(key = "studio-advanced-label", contentType = "studio-label") {
        HomeStudioLabel(copy.advanced)
    }
    item(key = "studio-density", contentType = "studio-choice") {
        HomeStudioChoice(
            title = copy.density,
            options = HomeDensity.entries,
            selected = layout.density,
            label = { density ->
                when (density) {
                    HomeDensity.Compact -> copy.densityCompact
                    HomeDensity.Balanced -> copy.densityBalanced
                    HomeDensity.Spacious -> copy.densitySpacious
                }
            },
            onSelect = { density -> update(settings.updateLayout(style) { it.copy(density = density) }) }
        )
    }
    item(key = "studio-motion", contentType = "studio-choice") {
        val motionOptions = if (style == HomeStyle.Original) {
            listOf(HomeMotion.Reduced, HomeMotion.Normal)
        } else {
            HomeMotion.entries
        }
        HomeStudioChoice(
            title = copy.motion,
            options = motionOptions,
            selected = layout.motion,
            label = { motion ->
                when (motion) {
                    HomeMotion.Reduced -> copy.motionReduced
                    HomeMotion.Normal -> copy.motionNormal
                    HomeMotion.Expressive -> copy.motionExpressive
                }
            },
            onSelect = { motion -> update(settings.updateLayout(style) { it.copy(motion = motion) }) }
        )
    }
    if (!LevyraSkinManagesLayout) item(key = "studio-palette", contentType = "studio-switch") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(copy.dynamicPalette, style = MaterialTheme.typography.titleSmall, color = LevyraText)
                Text(copy.dynamicPaletteSubtitle, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
            }
            Switch(
                checked = layout.dynamicPalette,
                onCheckedChange = { checked ->
                    update(settings.updateLayout(style) { it.copy(dynamicPalette = checked) })
                },
                modifier = Modifier.semantics { contentDescription = copy.dynamicPalette }
            )
        }
    }
}

internal fun reorderStep(
    order: List<HomeSectionId>,
    id: HomeSectionId,
    offset: Float,
    heights: Map<HomeSectionId, Int>
): Pair<List<HomeSectionId>, Float>? {
    val position = order.indexOf(id)
    if (position < 0) return null
    val nextHeight = order.getOrNull(position + 1)?.let(heights::get)?.toFloat()
    val previousHeight = order.getOrNull(position - 1)?.let(heights::get)?.toFloat()
    return when {
        nextHeight != null && offset > nextHeight / 2f ->
            order.toMutableList().apply { add(position + 1, removeAt(position)) } to offset - nextHeight
        previousHeight != null && offset < -previousHeight / 2f ->
            order.toMutableList().apply { add(position - 1, removeAt(position)) } to offset + previousHeight
        else -> null
    }
}

@Composable
private fun HomeStudioLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = LevyraText,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() }
    )
}

@Composable
private fun HomeStyleCard(style: HomeStyle, selected: Boolean, inUseLabel: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .width(128.dp)
            .levyraPressable(onClick = onClick, onClickLabel = homeStyleName(style), role = Role.Button)
            .semantics { this.selected = selected },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            HomeStylePreviewCanvas(style = style, modifier = Modifier.fillMaxSize())
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = inUseLabel,
                    tint = primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
        Text(
            text = homeStyleName(style),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) primary else LevyraText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeStylePreviewDialog(
    style: HomeStyle,
    copy: HomeStudioCopy,
    inUse: Boolean,
    closeLabel: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = copy.preview,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = homeStyleName(style),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = LevyraText,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = LevyraText)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .align(Alignment.CenterHorizontally)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                HomeStylePreviewCanvas(style = style, modifier = Modifier.fillMaxSize())
            }
            Text(
                text = homeStyleDescription(style, copy),
                style = MaterialTheme.typography.bodyLarge,
                color = LevyraMuted
            )
            Button(
                onClick = onApply,
                enabled = !inUse,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (inUse) copy.inUse else copy.apply)
            }
        }
    }
}

@Composable
private fun HomeStudioSectionRow(
    title: String,
    id: HomeSectionId,
    visible: Boolean,
    featured: Boolean,
    layout: HomeSectionLayout,
    copy: HomeStudioCopy,
    dragging: Boolean,
    modifier: Modifier,
    dragHandleModifier: Modifier,
    onVisible: (Boolean) -> Unit,
    onFeatured: (Boolean) -> Unit,
    onLayout: (HomeSectionLayout) -> Unit
) {
    var layoutMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (dragging) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = dragHandleModifier
                .size(48.dp)
                .semantics { contentDescription = copy.dragHandle },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = LevyraMuted)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (visible) LevyraText else LevyraMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (id.supportsLayouts && !LevyraSkinManagesLayout) {
                Box {
                    TextButton(
                        onClick = { layoutMenu = true },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "${copy.layout}: ${layoutLabel(layout, copy)}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(expanded = layoutMenu, onDismissRequest = { layoutMenu = false }) {
                        HomeSectionLayout.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(layoutLabel(option, copy)) },
                                leadingIcon = if (option == layout) {
                                    { Icon(Icons.Rounded.CheckCircle, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    layoutMenu = false
                                    onLayout(option)
                                }
                            )
                        }
                    }
                }
            }
        }
        IconToggleButton(checked = featured, onCheckedChange = onFeatured) {
            Icon(
                imageVector = if (featured) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "${copy.featured}: $title",
                tint = if (featured) MaterialTheme.colorScheme.primary else LevyraMuted
            )
        }
        Switch(
            checked = visible,
            onCheckedChange = onVisible,
            modifier = Modifier.semantics { contentDescription = "${copy.showSection}: $title" }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> HomeStudioChoice(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LevyraText)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun HomeStylePreviewCanvas(style: HomeStyle, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val canvas = homeCanvasColor(LevyraIsLight)
    val accent = scheme.primary
    val ink = LevyraText
    val tile = scheme.surfaceContainerHighest
    val tertiary = scheme.tertiary
    val secondary = scheme.secondary
    Canvas(modifier = modifier.background(canvas)) {
        val w = size.width
        val h = size.height
        val pad = w * 0.08f
        val inner = w - pad * 2
        fun bar(y: Float, width: Float, height: Float, color: Color, x: Float = pad) =
            drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(height / 2f))
        fun box(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float) =
            drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(radius))
        when (style) {
            HomeStyle.Original -> {
                bar(h * 0.05f, inner * 0.55f, h * 0.025f, ink.copy(alpha = 0.8f))
                repeat(3) { i -> bar(h * 0.1f, inner * 0.26f, h * 0.03f, tile, pad + i * inner * 0.3f) }
                box(pad, h * 0.16f, inner, h * 0.24f, accent.copy(alpha = 0.55f), w * 0.06f)
                repeat(2) { row ->
                    bar(h * (0.44f + row * 0.2f), inner * 0.4f, h * 0.02f, ink.copy(alpha = 0.6f))
                    repeat(3) { i ->
                        box(pad + i * inner * 0.35f, h * (0.48f + row * 0.2f), inner * 0.3f, inner * 0.3f, tile, w * 0.03f)
                    }
                }
            }
            HomeStyle.ExpressiveMax -> {
                bar(h * 0.06f, inner * 0.3f, h * 0.022f, accent)
                bar(h * 0.1f, inner * 0.75f, h * 0.045f, ink)
                drawExpressiveHero(pad, h * 0.18f, inner, h * 0.2f, scheme.surfaceContainerHigh, accent)
                val big = inner * 0.6f
                drawRoundRect(tertiary, Offset(pad, h * 0.42f), Size(big, big), CornerRadius(w * 0.12f, w * 0.12f))
                drawCircle(secondary, inner * 0.17f, Offset(pad + big + inner * 0.2f, h * 0.42f + inner * 0.16f))
                box(pad + big + inner * 0.04f, h * 0.42f + inner * 0.34f, inner * 0.36f, inner * 0.26f, accent.copy(alpha = 0.7f), w * 0.1f)
                repeat(3) { i ->
                    box(pad + i * inner * 0.34f, h * 0.42f + big + h * 0.03f, inner * 0.3f, inner * 0.3f, tile, w * (0.04f + i * 0.04f))
                }
            }
            HomeStyle.Flow -> {
                bar(h * 0.05f, inner * 0.5f, h * 0.028f, ink)
                repeat(3) { row ->
                    repeat(2) { col ->
                        val x = pad + col * inner * 0.51f
                        val y = h * (0.1f + row * 0.065f)
                        box(x, y, inner * 0.49f, h * 0.055f, tile, w * 0.015f)
                        box(x, y, h * 0.055f, h * 0.055f, accent.copy(alpha = 0.5f + row * 0.15f), w * 0.015f)
                    }
                }
                repeat(3) { section ->
                    val y = h * (0.33f + section * 0.21f)
                    bar(y, inner * 0.45f, h * 0.022f, ink.copy(alpha = 0.7f))
                    repeat(4) { i -> box(pad + i * inner * 0.28f, y + h * 0.035f, inner * 0.25f, inner * 0.25f, tile, w * 0.015f) }
                }
            }
            HomeStyle.Editorial -> {
                bar(h * 0.05f, inner * 0.35f, h * 0.016f, ink.copy(alpha = 0.5f))
                bar(h * 0.08f, inner * 0.8f, h * 0.035f, ink)
                drawRoundRect(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f), tertiary.copy(alpha = 0.9f))),
                    Offset(pad, h * 0.15f),
                    Size(inner, inner * 0.69f),
                    CornerRadius(w * 0.05f)
                )
                val start = h * 0.15f + inner * 0.69f + h * 0.04f
                drawLine(ink.copy(alpha = 0.25f), Offset(pad, start), Offset(pad + inner, start), strokeWidth = 2f)
                bar(start + h * 0.02f, inner * 0.55f, h * 0.026f, ink.copy(alpha = 0.85f))
                repeat(4) { i ->
                    val y = start + h * (0.07f + i * 0.065f)
                    bar(y, inner * 0.08f, h * 0.022f, accent)
                    box(pad + inner * 0.14f, y - h * 0.012f, h * 0.045f, h * 0.045f, tile, w * 0.02f)
                    bar(y, inner * 0.5f, h * 0.018f, ink.copy(alpha = 0.6f), pad + inner * 0.34f)
                }
            }
            HomeStyle.Minimal -> {
                bar(h * 0.05f, inner * 0.4f, h * 0.02f, ink.copy(alpha = 0.85f))
                repeat(6) { i ->
                    val y = h * (0.12f + i * 0.07f)
                    box(pad, y, h * 0.045f, h * 0.045f, tile, w * 0.008f)
                    bar(y + h * 0.008f, inner * 0.55f, h * 0.014f, ink.copy(alpha = 0.7f), pad + inner * 0.2f)
                    bar(y + h * 0.028f, inner * 0.32f, h * 0.01f, ink.copy(alpha = 0.35f), pad + inner * 0.2f)
                }
                bar(h * 0.58f, inner * 0.3f, h * 0.016f, ink.copy(alpha = 0.6f))
                repeat(2) { row ->
                    repeat(3) { i ->
                        box(pad + i * inner * 0.35f, h * (0.62f + row * 0.17f), inner * 0.3f, inner * 0.3f, tile, w * 0.008f)
                    }
                }
            }
            HomeStyle.MaterialExpressive -> {
                drawRoundRect(scheme.primaryContainer, Offset(pad, h * 0.04f), Size(inner, h * 0.12f), CornerRadius(w * 0.1f))
                bar(h * 0.08f, inner * 0.5f, h * 0.03f, scheme.onPrimaryContainer, pad * 1.8f)
                drawCircle(scheme.primary, h * 0.025f, Offset(pad + inner * 0.88f, h * 0.1f))
                drawRoundRect(tile, Offset(pad - w * 0.03f, h * 0.2f), Size(w * 0.14f, h * 0.2f), CornerRadius(w * 0.05f))
                drawRoundRect(accent.copy(alpha = 0.8f), Offset(pad + inner * 0.16f, h * 0.19f), Size(inner * 0.68f, h * 0.22f), CornerRadius(w * 0.08f))
                drawRoundRect(tile, Offset(pad + inner * 0.88f, h * 0.2f), Size(w * 0.14f, h * 0.2f), CornerRadius(w * 0.05f))
                repeat(3) { i ->
                    drawRoundRect(
                        if (i == 0) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                        Offset(pad, h * (0.45f + i * 0.055f)),
                        Size(inner, h * 0.05f),
                        CornerRadius(if (i == 1) w * 0.015f else w * 0.06f)
                    )
                }
                drawCircle(tertiary, inner * 0.13f, Offset(pad + inner * 0.15f, h * 0.73f))
                drawCircle(secondary, inner * 0.13f, Offset(pad + inner * 0.5f, h * 0.73f))
                drawRoundRect(accent.copy(alpha = 0.6f), Offset(pad + inner * 0.72f, h * 0.73f - inner * 0.13f), Size(inner * 0.26f, inner * 0.26f), CornerRadius(w * 0.03f))
                drawRoundRect(scheme.tertiaryContainer, Offset(pad, h * 0.83f), Size(inner * 0.48f, h * 0.12f), CornerRadius(w * 0.06f))
                drawRoundRect(scheme.primaryContainer, Offset(pad + inner * 0.52f, h * 0.83f), Size(inner * 0.48f, h * 0.12f), CornerRadius(w * 0.06f))
            }
            HomeStyle.Immersive -> {
                drawRect(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.85f), tertiary.copy(alpha = 0.4f), canvas)),
                    size = Size(w, h * 0.55f)
                )
                bar(h * 0.05f, inner * 0.6f, h * 0.03f, Color.White.copy(alpha = 0.9f))
                drawRoundRect(
                    Brush.verticalGradient(listOf(secondary.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.7f))),
                    Offset(pad, h * 0.12f),
                    Size(inner, h * 0.42f),
                    CornerRadius(w * 0.08f)
                )
                bar(h * 0.44f, inner * 0.7f, h * 0.035f, Color.White, pad * 1.6f)
                bar(h * 0.49f, inner * 0.3f, h * 0.03f, Color.White.copy(alpha = 0.9f), pad * 1.6f)
                repeat(2) { i ->
                    box(pad + i * inner * 0.55f, h * 0.6f, inner * 0.5f, inner * 0.62f, tile, w * 0.06f)
                }
                repeat(3) { i -> box(pad + i * inner * 0.36f, h * 0.84f, inner * 0.3f, inner * 0.3f, tile, w * 0.05f) }
            }
        }
    }
}

private fun DrawScope.drawExpressiveHero(x: Float, y: Float, width: Float, height: Float, surface: Color, accent: Color) {
    drawRoundRect(surface, Offset(x, y), Size(width, height), CornerRadius(height * 0.35f))
    drawCircle(accent, height * 0.38f, Offset(x + height * 0.5f, y + height * 0.5f))
    drawRoundRect(accent.copy(alpha = 0.9f), Offset(x + width * 0.55f, y + height * 0.62f), Size(height * 0.28f, height * 0.28f), CornerRadius(height * 0.1f))
    drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(x + width * 0.55f, y + height * 0.2f), Size(width * 0.36f, height * 0.12f), CornerRadius(height * 0.06f))
}

private fun homeStudioAppearanceName(style: HomeStyle): String =
    if (LevyraSkinManagesLayout) LevyraSkinLabel else homeStyleName(style)

private fun LazyListScope.homeStudioAppearanceItems(
    style: HomeStyle,
    copy: HomeStudioCopy,
    interfaceSettings: LevyraInterfaceSettings,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit,
    onStyle: (HomeStyle) -> Unit
) {
    if (LevyraSkinManagesLayout) {
        item(key = "studio-skin-managed") {
            Text(LevyraSkinLabel, color = LevyraText, style = MaterialTheme.typography.titleMedium)
            SkinManagedNotice { onInterfaceSettings(interfaceSettings.copy(skin = LevyraSkin.Default)) }
        }
    } else {
        item(key = "studio-style-label", contentType = "studio-label") {
            HomeStudioLabel(copy.styleSection)
        }
        item(key = "studio-styles", contentType = "studio-styles") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(HomeStyle.entries, key = { it.storageId }) { option ->
                    HomeStyleCard(
                        style = option,
                        selected = option == style,
                        inUseLabel = copy.inUse,
                        onClick = { onStyle(option) }
                    )
                }
            }
        }
        item(key = "studio-style-description", contentType = "studio-text") {
            Text(
                text = "${homeStyleName(style)} · ${homeStyleDescription(style, copy)}",
                style = MaterialTheme.typography.bodyMedium,
                color = LevyraMuted
            )
        }
    }

}
