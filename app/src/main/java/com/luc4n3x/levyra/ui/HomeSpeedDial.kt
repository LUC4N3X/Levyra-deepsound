package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.SpeedDialKind
import com.luc4n3x.levyra.domain.SpeedDialPin
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SpeedDialCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.abs

private val SpeedDialTileSize = 64.dp
private val SpeedDialTileGap = 12.dp
private val SpeedDialTileShape = RoundedCornerShape(14.dp)
private val SpeedDialAutoScrollEdge = 40.dp
private val SpeedDialAutoScrollStep = 14.dp
private const val SpeedDialLiftScale = 1.08f
private const val SpeedDialPressScale = 0.95f

@Composable
internal fun HomeSpeedDialStrip(
    pins: List<SpeedDialPin>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    onOpen: (SpeedDialPin) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (pins.isEmpty()) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings) { strings.speedDialCopy() }
    val haptics = LocalLevyraHaptics.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val listState = rememberLazyListState()
    val pinKeys = remember(pins) { pins.map { it.key } }
    val pinsByKey = remember(pins) { pins.associateBy { it.key } }
    val reorder = remember(listState) { SpeedDialReorderState(listState) }
    LaunchedEffect(pinKeys) { reorder.reconcile() }
    val displayedPins = remember(reorder.localOrder, pinKeys, pinsByKey) {
        reorder.displayedKeys(pinKeys).mapNotNull(pinsByKey::get)
    }
    val latestOnReorder by rememberUpdatedState(onReorder)
    val edgePx = with(density) { SpeedDialAutoScrollEdge.toPx() }
    val autoScrollStepPx = with(density) { SpeedDialAutoScrollStep.toPx() }
    var menuKey by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = null,
                tint = LevyraMuted.copy(alpha = 0.8f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = copy.title,
                color = LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
            horizontalArrangement = Arrangement.spacedBy(SpeedDialTileGap),
            userScrollEnabled = reorder.draggingKey == null
        ) {
            items(displayedPins, key = { it.key }, contentType = { "speed-dial-tile" }) { pin ->
                val dragging = reorder.draggingKey == pin.key
                val index = displayedPins.indexOf(pin)
                val moveBy: (Int) -> Unit = { step ->
                    val keys = displayedPins.map { it.key }.toMutableList()
                    val target = (index + step).coerceIn(0, keys.lastIndex)
                    if (index >= 0 && target != index) {
                        keys.add(target, keys.removeAt(index))
                        latestOnReorder(keys)
                    }
                }
                Box(
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .animateItem(
                            fadeInSpec = if (animationsEnabled) spring(stiffness = 600f) else null,
                            placementSpec = if (animationsEnabled && !dragging) {
                                spring(dampingRatio = 0.86f, stiffness = 520f)
                            } else {
                                null
                            },
                            fadeOutSpec = if (animationsEnabled) spring(stiffness = 600f) else null
                        )
                ) {
                    SpeedDialTile(
                        pin = pin,
                        copy = copy,
                        isCurrent = pin.kind == SpeedDialKind.SONG && pin.targetId == currentTrackId,
                        isPlaying = isPlaying,
                        isResolving = isResolving,
                        dragging = dragging,
                        dragOffset = {
                            val logical = if (reorder.draggingKey == pin.key) reorder.dragOffset else 0f
                            if (rtl) -logical else logical
                        },
                        animationsEnabled = animationsEnabled,
                        canMoveEarlier = index > 0,
                        canMoveLater = index < displayedPins.lastIndex,
                        onOpen = { onOpen(pin) },
                        onMoveEarlier = { moveBy(-1) },
                        onMoveLater = { moveBy(1) },
                        onRemove = { onRemove(pin.key) },
                        onLift = {
                            haptics.perform(LevyraHapticAction.Reorder)
                            reorder.start(pin.key, displayedPins.map { it.key })
                        },
                        onDrag = { dx ->
                            val logicalDx = if (rtl) -dx else dx
                            if (reorder.drag(logicalDx, edgePx, autoScrollStepPx)) haptics.perform(LevyraHapticAction.Reorder)
                        },
                        onDrop = { moved, completed ->
                            val finalOrder = reorder.finish(pin.key, completed)
                            when {
                                !completed -> Unit
                                moved -> if (finalOrder != null && finalOrder != pinKeys) latestOnReorder(finalOrder)
                                else -> menuKey = pin.key
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = menuKey == pin.key,
                        onDismissRequest = { menuKey = null }
                    ) {
                        pin.track?.let { track ->
                            DropdownMenuItem(
                                text = { Text(strings.songOptions) },
                                leadingIcon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
                                onClick = {
                                    menuKey = null
                                    onTrackActions(track)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(copy.removeFromHome) },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                            onClick = {
                                menuKey = null
                                onRemove(pin.key)
                            }
                        )
                    }
                }
            }
        }
    }
}

private class SpeedDialReorderState(private val listState: LazyListState) {
    var localOrder by mutableStateOf<List<String>?>(null)
        private set
    var draggingKey by mutableStateOf<String?>(null)
        private set
    private val offsetState = mutableFloatStateOf(0f)
    val dragOffset: Float
        get() = offsetState.floatValue

    fun displayedKeys(pinKeys: List<String>): List<String> {
        val local = localOrder ?: return pinKeys
        return if (sameMembers(local, pinKeys)) local else pinKeys
    }

    fun reconcile() {
        if (draggingKey == null) localOrder = null
    }

    fun start(key: String, keys: List<String>) {
        localOrder = keys
        draggingKey = key
        offsetState.floatValue = 0f
    }

    fun drag(deltaX: Float, edgePx: Float, stepPx: Float): Boolean {
        val key = draggingKey ?: return false
        offsetState.floatValue += deltaX
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
        val left = item.offset + offsetState.floatValue
        val center = left + item.size / 2f
        val target = layout.visibleItemsInfo.firstOrNull {
            it.key != key && center >= it.offset && center <= it.offset + it.size
        }
        var swapped = false
        val keys = localOrder
        if (target != null && keys != null) {
            val from = keys.indexOf(key)
            val to = keys.indexOf(target.key)
            if (from >= 0 && to >= 0) {
                if (from == listState.firstVisibleItemIndex || to == listState.firstVisibleItemIndex) {
                    listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                }
                localOrder = keys.toMutableList().apply { add(to, removeAt(from)) }
                offsetState.floatValue += item.offset - target.offset
                swapped = true
            }
        }
        val right = left + item.size
        val scroll = when {
            right > layout.viewportEndOffset - edgePx -> stepPx
            left < layout.viewportStartOffset + edgePx -> -stepPx
            else -> 0f
        }
        if (scroll != 0f) offsetState.floatValue += listState.dispatchRawDelta(scroll)
        return swapped
    }

    fun finish(key: String, completed: Boolean): List<String>? {
        if (draggingKey != key) return null
        draggingKey = null
        offsetState.floatValue = 0f
        val result = localOrder
        if (!completed) localOrder = null
        return result
    }

    private fun sameMembers(first: List<String>, second: List<String>): Boolean =
        first.size == second.size && first.toSet() == second.toSet()
}

@Composable
private fun SpeedDialTile(
    pin: SpeedDialPin,
    copy: SpeedDialCopy,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    dragging: Boolean,
    dragOffset: () -> Float,
    animationsEnabled: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onOpen: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
    onLift: () -> Unit,
    onDrag: (Float) -> Unit,
    onDrop: (moved: Boolean, completed: Boolean) -> Unit
) {
    val shape: Shape = if (pin.kind == SpeedDialKind.ARTIST) CircleShape else SpeedDialTileShape
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            dragging -> SpeedDialLiftScale
            pressed -> SpeedDialPressScale
            else -> 1f
        },
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.7f, stiffness = 700f) else snap(),
        label = "speed-dial-tile-scale"
    )
    val latestOpen by rememberUpdatedState(onOpen)
    val latestLift by rememberUpdatedState(onLift)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDrop by rememberUpdatedState(onDrop)
    val kindLabel = when (pin.kind) {
        SpeedDialKind.SONG -> copy.song
        SpeedDialKind.ALBUM -> copy.album
        SpeedDialKind.ARTIST -> copy.artist
        SpeedDialKind.PLAYLIST -> copy.playlist
    }
    val actions = buildList {
        if (canMoveEarlier) add(CustomAccessibilityAction(copy.moveEarlier) { onMoveEarlier(); true })
        if (canMoveLater) add(CustomAccessibilityAction(copy.moveLater) { onMoveLater(); true })
        add(CustomAccessibilityAction(copy.removeFromHome) { onRemove(); true })
    }

    Column(
        modifier = Modifier
            .width(SpeedDialTileSize)
            .graphicsLayer { translationX = dragOffset() }
            .semantics(mergeDescendants = true) {
                contentDescription = "${pin.title}, $kindLabel"
                onClick { latestOpen(); true }
                onLongClick(label = copy.reorderHint, action = null)
                customActions = actions
            }
            .pointerInput(pin.key) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        pressed = false
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && up.changedToUp() && !up.isConsumed) {
                            up.consume()
                            latestOpen()
                        }
                        return@awaitEachGesture
                    }
                    latestLift()
                    var travel = 0f
                    var completed = false
                    try {
                        completed = drag(longPress.id) { change ->
                            val dx = change.positionChange().x
                            travel += abs(dx)
                            latestDrag(dx)
                            change.consume()
                        }
                    } finally {
                        pressed = false
                        latestDrop(travel > viewConfiguration.touchSlop, completed)
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(SpeedDialTileSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.shape = shape
                    clip = true
                    shadowElevation = if (dragging) 10.dp.toPx() else 0f
                }
                .background(LevyraAdaptiveChip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = pin.kind.fallbackIcon(),
                contentDescription = null,
                tint = LevyraMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            val track = pin.track
            if (track != null) {
                CoverImage(track, Modifier.fillMaxSize())
            } else if (pin.artworkUrl.isNotBlank()) {
                StableRemoteArtwork(
                    url = pin.artworkUrl,
                    contentDescription = pin.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(shape)
                )
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.62f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ActiveTrackEqualizer(
                        color = LevyraCyan,
                        isPlaying = isPlaying && !isResolving,
                        width = 10.dp,
                        height = 8.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = pin.title,
            color = if (isCurrent) LevyraCyan else LevyraText.copy(alpha = 0.86f),
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun SpeedDialKind.fallbackIcon(): ImageVector = when (this) {
    SpeedDialKind.SONG -> Icons.Rounded.MusicNote
    SpeedDialKind.ALBUM -> Icons.Rounded.Album
    SpeedDialKind.ARTIST -> Icons.Rounded.Person
    SpeedDialKind.PLAYLIST -> Icons.AutoMirrored.Rounded.QueueMusic
}
