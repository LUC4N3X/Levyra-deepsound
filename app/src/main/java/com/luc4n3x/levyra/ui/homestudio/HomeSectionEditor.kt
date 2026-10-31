package com.luc4n3x.levyra.ui.homestudio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.HomeCompositionMode
import com.luc4n3x.levyra.domain.LevyraHomeCatalog
import com.luc4n3x.levyra.domain.LevyraHomeCompositionSettings
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.homeSectionGloballyEnabled
import com.luc4n3x.levyra.ui.homeSectionTitle
import com.luc4n3x.levyra.ui.i18n.HomeCompositionCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.sectionMeaning
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.reorderStep
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.SkinChrome
import com.luc4n3x.levyra.ui.theme.title
import com.luc4n3x.levyra.ui.withHomeSectionEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal val HomeEditorItemGap = 10.dp
private val AutoScrollEdge = 96.dp
private val AutoScrollStep = 9.dp
private const val JustAddedHighlightMillis = 2_800L

internal fun homeBlockKey(block: HomeBlock): String = "composer-${block.storageId}"

internal fun homeBlockEnabled(block: HomeBlock, settings: LevyraInterfaceSettings): Boolean =
    block !is HomeBlock.Section || homeSectionGloballyEnabled(block.id, settings)

internal fun homeEditorBlocks(
    skin: LevyraSkin,
    composition: LevyraHomeCompositionSettings,
    studio: LevyraHomeStudioSettings,
    settings: LevyraInterfaceSettings
): List<HomeBlock> = composition
    .withMode(skin, HomeCompositionMode.Custom)
    .resolvedBlocks(skin, studio)
    .orEmpty()
    .filter { homeBlockEnabled(it, settings) }

internal fun homeEditorDefaultBlocks(skin: LevyraSkin, studio: LevyraHomeStudioSettings): List<HomeBlock> =
    LevyraHomeCatalog.originalBlocks(skin, studio).ifEmpty { LevyraHomeCatalog.levyraBlocks(skin) }

@Stable
internal class HomeSectionEditor(
    initial: List<HomeBlock>,
    private val scope: CoroutineScope,
    private val haptic: (LevyraHapticAction) -> Unit,
    private val commit: (List<HomeBlock>) -> Unit,
    private val enable: (HomeBlock) -> Unit
) {
    var working by mutableStateOf(initial)
    var dragging by mutableStateOf<HomeBlock?>(null)
        private set
    var holding by mutableStateOf(false)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    var justAdded by mutableStateOf<HomeBlock?>(null)

    private val heights = mutableMapOf<HomeBlock, Int>()
    private var settleJob: Job? = null

    val canRemove: Boolean get() = working.size > 1

    fun recordHeight(block: HomeBlock, height: Int) {
        heights[block] = height
    }

    fun moveBy(block: HomeBlock, delta: Int): Boolean {
        val index = working.indexOf(block)
        val target = index + delta
        if (index < 0 || target !in working.indices) return false
        publish(working.toMutableList().apply { add(target, removeAt(index)) })
        haptic(LevyraHapticAction.Reorder)
        return true
    }

    fun remove(block: HomeBlock): Boolean {
        if (!canRemove || block !in working) return false
        if (justAdded == block) justAdded = null
        publish(working - block)
        haptic(LevyraHapticAction.Confirm)
        return true
    }

    fun add(block: HomeBlock) {
        if (block in working) return
        enable(block)
        publish(working + block)
        justAdded = block
        haptic(LevyraHapticAction.Confirm)
    }

    fun startDrag(block: HomeBlock) {
        settleJob?.cancel()
        dragging = block
        holding = true
        dragOffset = 0f
        haptic(LevyraHapticAction.Confirm)
    }

    fun dragBy(delta: Float) {
        dragOffset += delta
        stepDrag()
    }

    fun onAutoScroll(consumed: Float) {
        dragOffset += consumed
        stepDrag()
    }

    fun endDrag(animated: Boolean) {
        if (dragging == null) return
        holding = false
        commit(working)
        val start = dragOffset
        if (!animated || start == 0f) {
            dragging = null
            dragOffset = 0f
            return
        }
        settleJob = scope.launch {
            animate(start, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                dragOffset = value
            }
            dragging = null
        }
    }

    private fun stepDrag() {
        val block = dragging ?: return
        reorderStep(working, block, dragOffset, heights)?.let { (order, offset) ->
            working = order
            dragOffset = offset
            haptic(LevyraHapticAction.Reorder)
        }
    }

    private fun publish(next: List<HomeBlock>) {
        working = next
        commit(next)
    }
}

@Composable
internal fun rememberHomeSectionEditor(
    skin: LevyraSkin,
    composition: LevyraHomeCompositionSettings,
    studio: LevyraHomeStudioSettings,
    interfaceSettings: LevyraInterfaceSettings,
    onComposition: (LevyraHomeCompositionSettings) -> Unit,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit
): HomeSectionEditor {
    val source = remember(skin, composition, studio, interfaceSettings) {
        homeEditorBlocks(skin, composition, studio, interfaceSettings)
    }
    val scope = rememberCoroutineScope()
    val haptics by rememberUpdatedState(LocalLevyraHaptics.current)
    val latestComposition by rememberUpdatedState(composition)
    val latestSettings by rememberUpdatedState(interfaceSettings)
    val latestOnComposition by rememberUpdatedState(onComposition)
    val latestOnInterface by rememberUpdatedState(onInterfaceSettings)
    val editor = remember(skin) {
        HomeSectionEditor(
            initial = source,
            scope = scope,
            haptic = { action -> haptics.perform(action) },
            commit = { next -> latestOnComposition(latestComposition.withCustomBlocks(skin, next)) },
            enable = { block ->
                val settings = latestSettings
                if (block is HomeBlock.Section && !homeBlockEnabled(block, settings)) {
                    latestOnInterface(settings.withHomeSectionEnabled(block.id))
                }
            }
        )
    }
    LaunchedEffect(editor, source) {
        if (editor.dragging == null) editor.working = source
    }
    return editor
}

@Composable
internal fun HomeSectionEditorEffects(editor: HomeSectionEditor, listState: LazyListState, firstBlockIndex: Int) {
    val density = LocalDensity.current
    val edge = with(density) { AutoScrollEdge.toPx() }
    val step = with(density) { AutoScrollStep.toPx() }
    val animated = LocalAnimationsEnabled.current
    val holding = editor.holding
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        val key = editor.dragging?.let(::homeBlockKey) ?: return@LaunchedEffect
        while (isActive) {
            withFrameNanos { }
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: continue
            val top = item.offset + editor.dragOffset
            val bottom = top + item.size
            val request = when {
                bottom > info.viewportEndOffset - edge -> step
                top < info.viewportStartOffset + edge -> -step
                else -> 0f
            }
            if (request != 0f) editor.onAutoScroll(listState.scrollBy(request))
        }
    }
    val added = editor.justAdded
    LaunchedEffect(added) {
        val block = added ?: return@LaunchedEffect
        withFrameNanos { }
        val index = firstBlockIndex + editor.working.indexOf(block)
        val info = listState.layoutInfo
        val landed = info.visibleItemsInfo.firstOrNull { it.key == homeBlockKey(block) }
        val inView = landed != null &&
            landed.offset >= info.viewportStartOffset &&
            landed.offset + landed.size <= info.viewportEndOffset - edge.toInt()
        if (!inView) {
            val centered = -info.viewportSize.height / 3
            if (animated) listState.animateScrollToItem(index, centered) else listState.scrollToItem(index, centered)
        }
        delay(JustAddedHighlightMillis)
        if (editor.justAdded == block) editor.justAdded = null
    }
}

internal fun LazyListScope.homeSectionEditorItems(
    editor: HomeSectionEditor,
    skin: LevyraSkin,
    strings: LevyraStrings,
    copy: HomeCompositionCopy,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones,
    canResetOrder: Boolean,
    onResetOrder: () -> Unit
) {
    val working = editor.working
    val toAdd = LevyraHomeCatalog.availableBlocks(skin).filterNot { it in working }
    item(key = "composer-home-zone", contentType = "composer-zone") {
        HomeEditorZoneHeader(copy.inYourHome, if (editor.canRemove) copy.dragHint else copy.lastBlockHint)
    }
    items(working, key = ::homeBlockKey, contentType = { "composer-block" }) { block ->
        HomeEditorCard(
            block = block,
            index = working.indexOf(block),
            lastIndex = working.lastIndex,
            editor = editor,
            skin = skin,
            strings = strings,
            copy = copy,
            tokens = tokens,
            tones = tones
        )
    }
    item(key = "composer-reset-order", contentType = "composer-action") {
        val locale = rememberHomeStudioLocale()
        OutlinedButton(
            onClick = onResetOrder,
            enabled = canResetOrder,
            shape = tokens.controlShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tokens.title(copy.resetOrder, locale))
        }
    }
    item(key = "composer-add-zone", contentType = "composer-zone") {
        HomeEditorZoneHeader(copy.toAdd, if (toAdd.isEmpty()) copy.allAdded else null)
    }
    items(toAdd, key = ::homeBlockKey, contentType = { "composer-block" }) { block ->
        HomeEditorAddTile(block, editor, skin, strings, copy, tokens, tones)
    }
}

@Composable
private fun LazyItemScope.homeEditorItemModifier(dragging: Boolean): Modifier {
    val animated = LocalAnimationsEnabled.current
    return Modifier.animateItem(
        fadeInSpec = if (animated) tween(180) else null,
        placementSpec = if (animated && !dragging) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        } else {
            null
        },
        fadeOutSpec = if (animated) tween(120) else null
    )
}

@Composable
private fun HomeEditorZoneHeader(title: String, hint: String?) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = SkinChrome.label(title, rememberHomeStudioLocale()),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LevyraText,
            modifier = Modifier.semantics { heading() }
        )
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        }
    }
}

private fun homeBlockTitle(block: HomeBlock, skin: LevyraSkin, strings: LevyraStrings, copy: HomeCompositionCopy): String = when (block) {
    is HomeBlock.Section -> homeSectionTitle(block.id, strings)
    HomeBlock.SkinFeed -> copy.skinFeedName.replace("{skin}", skin.title())
}

private fun homeBlockMeaning(block: HomeBlock, copy: HomeCompositionCopy): String = when (block) {
    is HomeBlock.Section -> copy.sectionMeaning(block.id)
    HomeBlock.SkinFeed -> copy.skinFeedBody
}

private fun homeEditorActions(
    block: HomeBlock,
    index: Int,
    lastIndex: Int,
    editor: HomeSectionEditor,
    earlierLabel: String,
    laterLabel: String,
    removeLabel: String
): List<CustomAccessibilityAction> = buildList {
    if (index > 0) add(CustomAccessibilityAction(earlierLabel) { editor.moveBy(block, -1) })
    if (index < lastIndex) add(CustomAccessibilityAction(laterLabel) { editor.moveBy(block, 1) })
    if (editor.canRemove) add(CustomAccessibilityAction(removeLabel) { editor.remove(block) })
}

@Composable
private fun HomeEditorDragHandle(block: HomeBlock, editor: HomeSectionEditor, dragHint: String, animated: () -> Boolean) {
    Box(
        modifier = Modifier
            .pointerInput(block) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { editor.startDrag(block) },
                    onDragEnd = { editor.endDrag(animated()) },
                    onDragCancel = { editor.endDrag(animated()) },
                    onDrag = { change, amount ->
                        change.consume()
                        editor.dragBy(amount.y)
                    }
                )
            }
            .size(48.dp)
            .semantics { contentDescription = dragHint },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = LevyraMuted)
    }
}

@Composable
private fun HomeEditorCardText(
    title: String,
    meaning: String,
    justAddedLabel: String,
    highlighted: Boolean,
    animated: Boolean,
    tokens: HomeStudioSkinTokens,
    modifier: Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.merge(tokens.display).copy(fontWeight = FontWeight.SemiBold),
            color = LevyraText
        )
        Text(text = meaning, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        AnimatedVisibility(
            visible = highlighted,
            enter = if (animated) fadeIn(tween(160)) + expandVertically(tween(200)) else EnterTransition.None,
            exit = if (animated) fadeOut(tween(120)) + shrinkVertically(tween(160)) else ExitTransition.None
        ) {
            Text(
                text = justAddedLabel,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.accent,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun LazyItemScope.HomeEditorCard(
    block: HomeBlock,
    index: Int,
    lastIndex: Int,
    editor: HomeSectionEditor,
    skin: LevyraSkin,
    strings: LevyraStrings,
    copy: HomeCompositionCopy,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones
) {
    val animated = LocalAnimationsEnabled.current
    val latestAnimated by rememberUpdatedState(animated)
    val isDragging = editor.dragging == block
    val highlighted = editor.justAdded == block
    val gap = with(LocalDensity.current) { HomeEditorItemGap.roundToPx() }
    val locale = rememberHomeStudioLocale()
    val title = homeBlockTitle(block, skin, strings, copy)
    val shape = tokens.cardShape
    val moves = strings.speedDialCopy()
    Box(
        homeEditorItemModifier(isDragging)
            .zIndex(if (isDragging) 1f else 0f)
            .fillMaxWidth()
            .onSizeChanged { editor.recordHeight(block, it.height + gap) }
    ) {
        if (isDragging) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(tokens.accent.copy(alpha = 0.1f))
                    .border(1.dp, tokens.accent.copy(alpha = 0.34f), shape)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = if (isDragging) editor.dragOffset else 0f
                    val lifted = if (isDragging) 1.03f else 1f
                    scaleX = lifted
                    scaleY = lifted
                    shadowElevation = if (isDragging) 14.dp.toPx() else 0f
                    this.shape = shape
                }
                .clip(shape)
                .background(if (isDragging) tokens.cardLifted else tokens.card)
                .then(if (highlighted) Modifier.border(2.dp, tokens.accent, shape) else Modifier)
                .semantics(mergeDescendants = true) {
                    customActions = homeEditorActions(block, index, lastIndex, editor, moves.moveEarlier, moves.moveLater, copy.remove)
                }
                .heightIn(min = 72.dp)
                .padding(start = 2.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeEditorDragHandle(block, editor, copy.dragHint) { latestAnimated }
            HomeSectionThumbnail(block, tokens, tones, Modifier.size(52.dp))
            HomeEditorCardText(
                title = tokens.title(title, locale),
                meaning = homeBlockMeaning(block, copy),
                justAddedLabel = copy.justAdded,
                highlighted = highlighted,
                animated = animated,
                tokens = tokens,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            IconButton(onClick = { editor.remove(block) }, enabled = editor.canRemove) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "${copy.remove}: $title",
                    tint = if (editor.canRemove) LevyraMuted else LevyraMuted.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.HomeEditorAddTile(
    block: HomeBlock,
    editor: HomeSectionEditor,
    skin: LevyraSkin,
    strings: LevyraStrings,
    copy: HomeCompositionCopy,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones
) {
    val locale = rememberHomeStudioLocale()
    val shape = tokens.cardShape
    Row(
        modifier = homeEditorItemModifier(false)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, tokens.line, shape)
            .levyraPressable(
                onClick = { editor.add(block) },
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = copy.add
            )
            .heightIn(min = 68.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSectionThumbnail(block, tokens, tones, Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tokens.title(homeBlockTitle(block, skin, strings, copy), locale),
                style = MaterialTheme.typography.titleSmall.merge(tokens.display).copy(fontWeight = FontWeight.SemiBold),
                color = LevyraText
            )
            Text(text = homeBlockMeaning(block, copy), style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        }
        Box(
            Modifier
                .size(40.dp)
                .background(tokens.accent.copy(alpha = 0.16f), tokens.chipShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.accent)
        }
    }
}
