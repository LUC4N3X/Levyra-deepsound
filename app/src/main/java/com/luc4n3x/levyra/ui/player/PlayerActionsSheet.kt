package com.luc4n3x.levyra.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Immutable
internal data class PlayerSheetAction(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val value: String? = null,
    val active: Boolean = false,
    val toggle: Boolean = false,
    val busy: Boolean = false,
    val enabled: Boolean = true,
    val keepsSheetOpen: Boolean = false
)

private const val SheetEnterMs = 280
private const val SheetExitMs = 200
private const val SheetColumns = 3
private const val SheetSurfaceTint = 0.93f
private val SheetShape = RoundedCornerShape(
    topStart = LevyraPlayerDesign.DockTrayCorner,
    topEnd = LevyraPlayerDesign.DockTrayCorner
)
private val SheetDismissDistance = 104.dp
private val SheetTileHeight = 88.dp
private val SheetArtwork = 52.dp

@Composable
internal fun PlayerActionsSheet(
    track: Track,
    artworkUrl: String,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    actions: List<PlayerSheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    engagementContent: (@Composable () -> Unit)? = null,
    discoverContent: (@Composable () -> Unit)? = null
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragY by remember { mutableFloatStateOf(0f) }
    val settleAnim = remember { Animatable(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val dismissDistancePx = with(density) { SheetDismissDistance.toPx() }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(closing) {
        if (closing) {
            if (animated) delay(SheetExitMs.toLong())
            onDismiss()
        }
    }
    val dismiss: () -> Unit = {
        if (!closing) {
            visible = false
            closing = true
        }
    }
    BackHandler(enabled = !closing, onBack = dismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(80f)
    ) {
        PlayerSheetScrim(visible = visible, animated = animated, onClick = dismiss)
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding(),
            enter = sheetBodyEnter(animated),
            exit = sheetBodyExit(animated)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, (if (isDragging) dragY else settleAnim.value).roundToInt()) }
                    .clip(SheetShape)
                    .background(playerSheetColor(surfaces))
                    .border(LevyraPlayerDesign.Hairline, playerSheetOutline(surfaces), SheetShape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding()
            ) {
                PlayerSheetDragHandle(
                    onDragStart = {
                        settleJob?.cancel()
                        settleJob = null
                        isDragging = true
                        dragY = settleAnim.value
                    },
                    onDragDelta = { delta ->
                        dragY = (dragY + delta).coerceAtLeast(0f)
                    },
                    onDragFinish = {
                        val currentOffset = dragY
                        if (shouldDismissSheetOnDragEnd(currentOffset, dismissDistancePx)) {
                            settleJob?.cancel()
                            settleJob = scope.launch {
                                settleAnim.snapTo(currentOffset)
                                isDragging = false
                                dismiss()
                            }
                        } else {
                            settleJob?.cancel()
                            settleJob = scope.launch {
                                settleAnim.snapTo(currentOffset)
                                isDragging = false
                                if (animated) {
                                    settleAnim.animateTo(0f, LevyraPlayerDesign.smoothSpring())
                                } else {
                                    settleAnim.snapTo(0f)
                                }
                                dragY = 0f
                            }
                        }
                    },
                    onDragCancel = {
                        val currentOffset = dragY
                        settleJob?.cancel()
                        settleJob = scope.launch {
                            settleAnim.snapTo(currentOffset)
                            isDragging = false
                            if (animated) {
                                settleAnim.animateTo(0f, LevyraPlayerDesign.smoothSpring())
                            } else {
                                settleAnim.snapTo(0f)
                            }
                            dragY = 0f
                        }
                    },
                    surfaces = surfaces
                )
                PlayerSheetScrollBody(
                    track = track,
                    artworkUrl = artworkUrl,
                    surfaces = surfaces,
                    animated = animated,
                    actions = actions,
                    engagementContent = engagementContent,
                    discoverContent = discoverContent,
                    onAction = { action ->
                        action.onClick()
                        if (!action.keepsSheetOpen) dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerSheetScrim(
    visible: Boolean,
    animated: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = sheetScrimEnter(animated),
        exit = sheetScrimExit(animated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
}

private fun sheetScrimEnter(animated: Boolean): EnterTransition =
    if (animated) fadeIn(LevyraPlayerDesign.standardTween(SheetEnterMs)) else EnterTransition.None

private fun sheetScrimExit(animated: Boolean): ExitTransition =
    if (animated) fadeOut(LevyraPlayerDesign.standardTween(SheetExitMs)) else ExitTransition.None

private fun sheetBodyEnter(animated: Boolean): EnterTransition =
    if (animated) {
        slideInVertically(LevyraPlayerDesign.expandSpring()) { it } +
            fadeIn(LevyraPlayerDesign.standardTween(SheetEnterMs))
    } else {
        EnterTransition.None
    }

private fun sheetBodyExit(animated: Boolean): ExitTransition =
    if (animated) {
        slideOutVertically(LevyraPlayerDesign.emphasizedTween(SheetExitMs)) { it } +
            fadeOut(LevyraPlayerDesign.standardTween(SheetExitMs))
    } else {
        ExitTransition.None
    }

private fun playerSheetColor(surfaces: PlayerSurfaceTokens): Color =
    if (surfaces.amoled) {
        Color.Black
    } else {
        surfaces.glow.playerMix(PlayerDarkSurface, SheetSurfaceTint)
    }

private fun playerSheetOutline(surfaces: PlayerSurfaceTokens): Color =
    if (surfaces.amoled) surfaces.outline else Color.White.copy(alpha = 0.08f)

internal fun shouldDismissSheetOnDragEnd(
    dragOffsetPx: Float,
    dismissThresholdPx: Float
): Boolean = dragOffsetPx > dismissThresholdPx

@Composable
private fun PlayerSheetDragHandle(
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragFinish: () -> Unit,
    onDragCancel: () -> Unit,
    surfaces: PlayerSurfaceTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragFinish() },
                    onDragCancel = { onDragCancel() }
                ) { change, delta ->
                    change.consume()
                    onDragDelta(delta)
                }
            }
            .padding(top = LevyraPlayerDesign.SpaceMd, bottom = LevyraPlayerDesign.SpaceSm),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(surfaces.contentFaint)
        )
    }
}

@Composable
private fun PlayerSheetScrollBody(
    track: Track,
    artworkUrl: String,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    actions: List<PlayerSheetAction>,
    engagementContent: (@Composable () -> Unit)?,
    discoverContent: (@Composable () -> Unit)?,
    onAction: (PlayerSheetAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LevyraPlayerDesign.Gutter)
            .padding(bottom = LevyraPlayerDesign.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
    ) {
        PlayerSheetHeader(track = track, artworkUrl = artworkUrl, surfaces = surfaces)
        engagementContent?.invoke()
        PlayerSheetGrid(
            actions = actions,
            surfaces = surfaces,
            animated = animated,
            onPerform = onAction
        )
        discoverContent?.invoke()
    }
}

@Composable
private fun PlayerSheetHeader(
    track: Track,
    artworkUrl: String,
    surfaces: PlayerSurfaceTokens
) {
    val artworkShape = RoundedCornerShape(LevyraPlayerDesign.CornerXs)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
    ) {
        SeamlessArtworkImage(
            url = artworkUrl,
            modifier = Modifier
                .size(SheetArtwork)
                .clip(artworkShape)
        ) {
            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = surfaces.content,
                fontSize = 17.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = track.artist,
                color = surfaces.contentMuted,
                fontSize = 14.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(14.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerSheetGrid(
    actions: List<PlayerSheetAction>,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onPerform: (PlayerSheetAction) -> Unit
) {
    val outline = if (surfaces.amoled) surfaces.outline else Color.Transparent
    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
        actions.chunked(SheetColumns).forEach { rowActions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                rowActions.forEach { action ->
                    key(action.key) {
                        val tint by animateColorAsState(
                            targetValue = if (action.active) surfaces.activeContent else surfaces.content,
                            animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
                            label = "player-sheet-tint"
                        )
                        PlayerSegmentButton(
                            position = LevyraSegment.Middle,
                            weight = 1f,
                            container = if (action.active) surfaces.active else surfaces.controlQuiet,
                            innerCorner = LevyraPlayerDesign.CornerMd,
                            contentDescription = listOfNotNull(action.label, action.value).joinToString(", "),
                            animated = animated,
                            enabled = action.enabled,
                            toggleState = if (action.toggle) ToggleableState(action.active) else null,
                            outline = outline,
                            modifier = Modifier.heightIn(min = SheetTileHeight),
                            onClick = { if (!action.busy) onPerform(action) }
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = LevyraPlayerDesign.SpaceSm,
                                    vertical = LevyraPlayerDesign.SpaceMd
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (action.busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = tint
                                    )
                                } else {
                                    PlayerIcon(action.icon, tint, Modifier.size(LevyraPlayerDesign.DockGlyph))
                                }
                                Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceSm))
                                Text(
                                    text = action.label,
                                    color = tint,
                                    fontSize = 12.sp,
                                    lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                action.value?.let { value ->
                                    Text(
                                        text = value,
                                        color = if (action.active) tint else surfaces.contentFaint,
                                        fontSize = 11.sp,
                                        lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(SheetColumns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
