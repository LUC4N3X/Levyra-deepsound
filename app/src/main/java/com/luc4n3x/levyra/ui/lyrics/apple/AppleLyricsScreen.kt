package com.luc4n3x.levyra.ui.lyrics.apple

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFontFamilyResolver
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.player.AppleMusicSeekbar
import com.luc4n3x.levyra.ui.theme.levyraFontFamilyFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
internal class AppleLyricsMotion {
    private class Shift(val distance: Float, val startMs: Long, val durationMs: Long, val focus: Int)

    private val shifts = ArrayList<Shift>(4)
    private val frame = mutableLongStateOf(0L)
    val active: Boolean get() = shifts.isNotEmpty()

    fun push(distance: Float, durationMs: Long, focus: Int) {
        val now = SystemClock.uptimeMillis()
        shifts += Shift(distance, now, durationMs.coerceAtLeast(1L), focus)
        frame.longValue = now
    }

    fun tick() {
        val now = SystemClock.uptimeMillis()
        shifts.removeAll { now - it.startMs >= it.durationMs + appleLyricsStaggerDelayMs(Int.MAX_VALUE, it.focus) }
        frame.longValue = now
    }

    fun clear() {
        shifts.clear()
        frame.longValue = SystemClock.uptimeMillis()
    }

    fun translation(index: Int): Float {
        val now = frame.longValue
        if (shifts.isEmpty()) return 0f
        var total = 0f
        for (i in shifts.indices) {
            val shift = shifts[i]
            val delay = appleLyricsStaggerDelayMs(index, shift.focus)
            val raw = ((now - shift.startMs - delay).toFloat() / shift.durationMs).coerceIn(0f, 1f)
            total += shift.distance * (1f - AppleLyricsMoveEase.transform(raw))
        }
        return total
    }
}

@Stable
internal class AppleLyricsActions(
    val onLineClick: (Int, LyricLine) -> Unit,
    val onLineLongClick: (Int, LyricLine) -> Unit,
    val onSeekFraction: (Float) -> Unit,
    val onPrevious: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onToggleTranslation: () -> Unit,
    val onOptions: () -> Unit,
    val onClose: () -> Unit
)

@Stable
internal class AppleLyricsLabels(
    val unavailable: String,
    val searching: String,
    val options: String,
    val close: String,
    val translation: String,
    val previous: String,
    val play: String,
    val pause: String,
    val next: String
)

@Stable
internal class AppleLyricsTextKit(
    val main: TextStyle,
    val background: TextStyle,
    val secondary: TextStyle,
    val static: TextStyle,
    val measurer: androidx.compose.ui.text.TextMeasurer,
    val fontFamily: androidx.compose.ui.text.font.FontFamily,
    val fontKey: Any?,
    val karaoke: Boolean,
    val animations: Boolean,
    val showTranslation: Boolean,
    val romanization: Boolean,
    val showOriginal: Boolean
)

@Composable
private fun rememberAppleLyricsTextKit(
    settings: LevyraLyricsStudioSettings,
    showTranslation: Boolean,
    animations: Boolean
): AppleLyricsTextKit {
    val fontFamily = remember { levyraFontFamilyFor(LevyraFontPreset.Inter) }
    val fontKey = LocalFontFamilyResolver.current.resolve(fontFamily, FontWeight.Bold).value
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val main = remember(fontFamily) {
        TextStyle(
            fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = AppleLyricsTokens.lineTextSize,
            letterSpacing = AppleLyricsTokens.LETTER_SPACING_EM.em, textDirection = TextDirection.Content
        )
    }
    val static = remember(fontFamily) {
        TextStyle(
            fontFamily = fontFamily, fontSize = AppleLyricsTokens.staticTextSize,
            lineHeight = AppleLyricsTokens.staticLineHeight, color = AppleLyricsTokens.primary,
            textDirection = TextDirection.Content
        )
    }
    return remember(main, static, measurer, fontKey, settings, showTranslation, animations) {
        AppleLyricsTextKit(
            main = main,
            background = main.copy(fontSize = AppleLyricsTokens.backgroundTextSize),
            secondary = main.copy(fontSize = AppleLyricsTokens.secondaryTextSize, letterSpacing = 0.em),
            static = static,
            measurer = measurer,
            fontFamily = fontFamily,
            fontKey = fontKey,
            karaoke = settings.progressiveKaraokeEnabled,
            animations = animations,
            showTranslation = showTranslation,
            romanization = settings.romanizationEnabled,
            showOriginal = settings.showOriginalLyrics
        )
    }
}

@Stable
private class AppleMotionRunner(private val motion: AppleLyricsMotion) {
    private var job: Job? = null

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive && motion.active) {
                androidx.compose.runtime.withFrameMillis { motion.tick() }
            }
            motion.tick()
        }
    }
}

private fun appleUserScrollConnection(manual: MutableState<Boolean>, motion: AppleLyricsMotion) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0f) {
            manual.value = true
            motion.clear()
        }
        return Offset.Zero
    }
}

private suspend fun appleFollowFocus(
    focus: Int,
    rows: List<AppleLyricsRow>,
    leads: LongArray,
    listState: LazyListState,
    motion: AppleLyricsMotion,
    autoScroll: Boolean,
    animations: Boolean,
    manual: MutableState<Boolean>,
    positioned: MutableState<Boolean>,
    startMotion: () -> Unit
) {
    if (focus < 0 || focus >= rows.size) return
    if (!autoScroll && positioned.value) return
    val info = listState.layoutInfo
    val target = info.visibleItemsInfo.firstOrNull { it.index == focus }
    if (manual.value) {
        val visible = target != null && target.offset >= 0 && target.offset + target.size <= info.viewportEndOffset
        if (listState.isScrollInProgress || !visible) return
        manual.value = false
    }
    if (target == null || !positioned.value || !animations) {
        motion.clear()
        listState.scrollToItem(focus, 0)
        positioned.value = true
        return
    }
    var consumed = 0f
    listState.scroll { consumed = scrollBy(target.offset.toFloat()) }
    if (abs(consumed) > 0.5f) {
        motion.push(consumed, leads[focus].takeIf { it > 0L } ?: APPLE_LYRICS_MOVE_MAX_MS, focus)
        startMotion()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppleMusicLyricsScreen(
    track: Track?,
    lines: List<LyricLine>,
    synced: Boolean,
    loading: Boolean,
    position: () -> Long,
    reportedPositionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    verifiedKbps: Int?,
    playing: Boolean,
    resolving: Boolean,
    settings: LevyraLyricsStudioSettings,
    showTranslation: Boolean,
    translationAvailable: Boolean,
    showSecondaryVoices: Boolean,
    animations: Boolean,
    heavyBlur: Boolean,
    selectionMode: Boolean,
    isSelected: (Int) -> Boolean,
    labels: AppleLyricsLabels,
    actions: AppleLyricsActions,
    modifier: Modifier = Modifier
) {
    val trackKey = track?.id.orEmpty()
    val rows = remember(lines, synced, showSecondaryVoices) { appleLyricsRows(lines, synced, showSecondaryVoices) }
    val leads = remember(rows) { appleLyricsLeads(rows) }
    val currentPosition by rememberUpdatedState(position)
    val focusState = remember(rows, leads, synced) {
        derivedStateOf { if (synced) appleLyricsFocusIndex(rows, leads, currentPosition()) else -1 }
    }
    val concurrentState = remember(rows, synced) {
        derivedStateOf { if (synced) appleLyricsConcurrentIndices(rows, focusState.value, currentPosition()) else IntRange.EMPTY }
    }
    val listState = remember(trackKey) { LazyListState() }
    val motion = remember(trackKey) { AppleLyricsMotion() }
    val manual = remember(trackKey) { mutableStateOf(false) }
    val positioned = remember(trackKey, rows) { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val runner = remember(motion) { AppleMotionRunner(motion) }
    val autoScroll = settings.autoScrollEnabled
    val kit = rememberAppleLyricsTextKit(settings, showTranslation, animations)
    val blurInactive = settings.appleBlurEnabled && heavyBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val stanzas = remember(rows, synced, showTranslation, kit.romanization, kit.showOriginal) {
        if (synced) emptyList() else appleStaticStanzas(rows, showTranslation, kit.romanization, kit.showOriginal)
    }
    val controlsProgress = animateFloatAsState(
        if (controlsVisible) 1f else 0f, tween(AppleLyricsTokens.CONTROLS_FADE_MS), label = "apple-lyrics-controls-fade"
    )
    LaunchedEffect(touches, playing) {
        controlsVisible = true
        if (playing) {
            delay(AppleLyricsTokens.CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }
    val userScroll = remember(trackKey) { appleUserScrollConnection(manual, motion) }
    LaunchedEffect(rows, autoScroll, animations) {
        snapshotFlow { focusState.value }.collect { focus ->
            appleFollowFocus(focus, rows, leads, listState, motion, autoScroll, animations, manual, positioned) { runner.start(scope) }
        }
    }
    val list = AppleLyricsListModel(
        rows = rows, stanzas = stanzas, synced = synced, loading = loading, listState = listState, userScroll = userScroll,
        motion = motion, focus = focusState, concurrent = concurrentState, manual = manual, blur = blurInactive,
        position = position, kit = kit, isSelected = isSelected, labels = labels, actions = actions
    )
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touches++
                }
            }
    ) {
        AppleLyricsBackdrop(
            artworkUrl = track?.let { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } },
            animate = animations && playing
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppleLyricsHeader(track, labels, actions, kit.fontFamily)
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val controlsHeight = maxHeight * AppleLyricsTokens.CONTROLS_FRACTION
                AppleLyricsBody(list, maxHeight, controlsHeight, controlsProgress)
                AppleLyricsControls(
                    visible = controlsVisible,
                    height = controlsHeight,
                    reportedPositionMs = reportedPositionMs,
                    bufferedMs = bufferedMs,
                    durationMs = durationMs,
                    verifiedKbps = verifiedKbps,
                    playing = playing,
                    resolving = resolving,
                    translationAvailable = translationAvailable,
                    showTranslation = showTranslation,
                    labels = labels,
                    actions = actions,
                    onReveal = { touches++ },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Stable
private class AppleLyricsListModel(
    val rows: List<AppleLyricsRow>,
    val stanzas: List<AnnotatedString>,
    val synced: Boolean,
    val loading: Boolean,
    val listState: LazyListState,
    val userScroll: NestedScrollConnection,
    val motion: AppleLyricsMotion,
    val focus: androidx.compose.runtime.State<Int>,
    val concurrent: androidx.compose.runtime.State<IntRange>,
    val manual: MutableState<Boolean>,
    val blur: Boolean,
    val position: () -> Long,
    val kit: AppleLyricsTextKit,
    val isSelected: (Int) -> Boolean,
    val labels: AppleLyricsLabels,
    val actions: AppleLyricsActions
)

private fun Modifier.appleLyricsEdgeFade(restingFadeEnd: Float, controlsProgress: androidx.compose.runtime.State<Float>): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val fadeTop = AppleLyricsTokens.TOP_FADE_FRACTION
            val fadeEnd = restingFadeEnd + (1f - restingFadeEnd) * (1f - controlsProgress.value)
            val fadeStart = (fadeEnd - AppleLyricsTokens.BOTTOM_FADE_FRACTION).coerceIn(fadeTop + 0.01f, fadeEnd - 0.001f)
            val mask = Brush.verticalGradient(
                0f to Color.Transparent,
                fadeTop to Color.Black,
                fadeStart to Color.Black,
                fadeEnd to Color.Transparent,
                1f to Color.Transparent
            )
            onDrawWithContent {
                drawContent()
                drawRect(mask, blendMode = BlendMode.DstIn)
            }
        }

@Composable
private fun AppleLyricsBody(
    list: AppleLyricsListModel,
    areaHeight: androidx.compose.ui.unit.Dp,
    controlsHeight: androidx.compose.ui.unit.Dp,
    controlsProgress: androidx.compose.runtime.State<Float>
) {
    if (list.rows.isEmpty()) {
        AppleLyricsPlaceholder(list.loading, list.labels.unavailable, list.kit.main, controlsHeight)
        return
    }
    val anchor = (areaHeight * AppleLyricsTokens.ANCHOR_FRACTION - AppleLyricsTokens.textPaddingTop).coerceAtLeast(0.dp)
    val bottomPad = controlsHeight + areaHeight * AppleLyricsTokens.BOTTOM_FADE_FRACTION
    val restingFadeEnd = (1f - controlsHeight / areaHeight).coerceIn(0.2f, 1f)
    val topPad = if (list.synced) anchor else AppleLyricsTokens.staticTop
    LazyColumn(
        state = list.listState,
        modifier = Modifier.fillMaxSize().nestedScroll(list.userScroll).appleLyricsEdgeFade(restingFadeEnd, controlsProgress),
        contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(if (list.synced) AppleLyricsTokens.rowSpacing else AppleLyricsTokens.staticSpacing)
    ) {
        if (list.synced) {
            itemsIndexed(list.rows, key = { _, row -> row.key }) { index, row -> AppleSyncedItem(list, index, row) }
        } else {
            itemsIndexed(list.stanzas, key = { index, _ -> "stanza-$index" }) { _, stanza ->
                Text(stanza, style = list.kit.static, modifier = Modifier.fillMaxWidth().padding(horizontal = AppleLyricsTokens.staticPadding))
            }
        }
    }
}

@Composable
private fun AppleLyricsPlaceholder(loading: Boolean, unavailable: String, style: TextStyle, controlsHeight: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxSize().padding(bottom = controlsHeight), contentAlignment = Alignment.Center) {
        if (loading) {
            LevyraLoadingIndicator(color = AppleLyricsTokens.loading, modifier = Modifier.size(36.dp))
        } else {
            Text(
                unavailable,
                style = style.copy(color = Color.White.copy(alpha = 0.45f), textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = AppleLyricsTokens.staticPadding)
            )
        }
    }
}

@Composable
private fun AppleSyncedItem(list: AppleLyricsListModel, index: Int, row: AppleLyricsRow) {
    val focusIndex = list.focus.value
    val focused = focusIndex == index || index in list.concurrent.value
    val distance = if (focusIndex < 0) 0 else abs(index - focusIndex)
    val motion = list.motion
    val rowModifier = Modifier.graphicsLayer { translationY = motion.translation(index) }
    when (row) {
        is AppleLyricsRow.Instrumental -> AppleInstrumentalRow(row, focused, list.position, list.kit.animations, rowModifier)
        is AppleLyricsRow.Vocal -> AppleVocalRow(
            row = row, focused = focused, manual = list.manual.value, distance = distance,
            blur = list.blur && !list.manual.value, position = list.position, kit = list.kit,
            selected = list.isSelected(row.sourceIndex),
            onClick = {
                list.manual.value = false
                list.actions.onLineClick(row.sourceIndex, row.main)
            },
            onLongClick = { list.actions.onLineLongClick(row.sourceIndex, row.main) },
            modifier = rowModifier
        )
    }
}

private fun appleStaticStanzas(
    rows: List<AppleLyricsRow>,
    translation: Boolean,
    romanization: Boolean,
    original: Boolean
): List<AnnotatedString> {
    val stanzas = ArrayList<AnnotatedString>()
    var builder = AnnotatedString.Builder()
    var empty = true
    fun flush() {
        if (!empty) stanzas += builder.toAnnotatedString()
        builder = AnnotatedString.Builder()
        empty = true
    }
    rows.forEach { row ->
        val line = (row as? AppleLyricsRow.Vocal)?.main ?: return@forEach
        if (line.text.isBlank()) {
            flush()
            return@forEach
        }
        if (!empty) builder.append('\n')
        val primary = if (!original && line.translated.isNotBlank()) line.translated else line.text
        builder.append(primary)
        if (romanization && line.romanized.isNotBlank()) {
            builder.append('\n')
            builder.withStyle(SpanStyle(color = AppleLyricsTokens.secondary)) { append(line.romanized) }
        }
        if (translation && original && line.translated.isNotBlank()) {
            builder.append('\n')
            builder.withStyle(SpanStyle(color = AppleLyricsTokens.secondary)) { append(line.translated) }
        }
        empty = false
    }
    flush()
    return stanzas
}

@Composable
private fun AppleLyricsHeader(track: Track?, labels: AppleLyricsLabels, actions: AppleLyricsActions, fontFamily: androidx.compose.ui.text.font.FontFamily) {
    Row(
        Modifier.fillMaxWidth().padding(top = AppleLyricsTokens.headerTop, start = AppleLyricsTokens.headerStart, end = AppleLyricsTokens.headerEnd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = RoundedCornerShape(AppleLyricsTokens.thumbnailCorner)
        Box(
            Modifier.size(AppleLyricsTokens.thumbnail).clip(shape).background(AppleLyricsTokens.quaternary)
                .border(0.5.dp, AppleLyricsTokens.thumbnailStroke, shape)
                .clickable(role = Role.Button, onClick = actions.onClose)
                .semantics { contentDescription = labels.close }
        ) {
            if (track != null) {
                AsyncImage(
                    model = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = AppleLyricsTokens.thumbnailGap)) {
            Text(
                track?.title.orEmpty(), color = AppleLyricsTokens.primary, fontSize = AppleLyricsTokens.headerTextSize,
                fontFamily = fontFamily, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.artist.orEmpty(), color = AppleLyricsTokens.secondary, fontSize = AppleLyricsTokens.headerTextSize,
                fontFamily = fontFamily,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier.size(AppleLyricsTokens.headerIcon + AppleLyricsTokens.headerIconPadding * 2).clip(CircleShape)
                .clickable(role = Role.Button, onClick = actions.onOptions)
                .semantics { contentDescription = labels.options },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(AppleLyricsTokens.headerIcon).clip(CircleShape).background(AppleLyricsTokens.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreHoriz, null, tint = AppleLyricsTokens.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Immutable
private class AppleVocalContent(
    val mainText: String,
    val mainWords: List<com.luc4n3x.levyra.domain.LyricWord>,
    val romanized: String?,
    val translated: String?,
    val description: String
)

private fun appleVocalContent(main: LyricLine, romanization: Boolean, showOriginal: Boolean): AppleVocalContent {
    val translated = main.translated.takeIf(String::isNotBlank)
    val replaceOriginal = !showOriginal && translated != null
    return AppleVocalContent(
        mainText = if (replaceOriginal) translated.orEmpty() else main.text,
        mainWords = if (replaceOriginal) emptyList() else main.words,
        romanized = main.romanized.takeIf { romanization && it.isNotBlank() && !replaceOriginal },
        translated = translated.takeIf { !replaceOriginal },
        description = listOfNotNull(main.text, translated).joinToString("\n")
    )
}

private fun appleBlurTarget(blur: Boolean, focused: Boolean, distance: Int): Float =
    if (blur && !focused) distance.coerceAtMost(4) * 1.5f else 0f

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleVocalRow(
    row: AppleLyricsRow.Vocal,
    focused: Boolean,
    manual: Boolean,
    distance: Int,
    blur: Boolean,
    position: () -> Long,
    kit: AppleLyricsTextKit,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier
) {
    val spec = if (kit.animations) tween<Float>(AppleLyricsTokens.FOCUS_MS, easing = AppleLyricsEase) else snap()
    val focusState = animateFloatAsState(if (focused) 1f else 0f, spec, label = "apple-lyrics-focus")
    val manualState = animateFloatAsState(if (manual) 1f else 0f, spec, label = "apple-lyrics-manual")
    val blurState = animateFloatAsState(appleBlurTarget(blur, focused, distance), spec, label = "apple-lyrics-blur")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val end = row.side == AppleLyricsSide.END
    val pivotX = if (end != rtl) 1f else 0f
    val content = remember(row.main, kit.romanization, kit.showOriginal) { appleVocalContent(row.main, kit.romanization, kit.showOriginal) }
    val text = AppleRowTextScope(
        kit = kit, align = if (end) TextAlign.End else TextAlign.Start, focused = focused, position = position,
        focus = { focusState.value },
        inactive = { lerp(AppleLyricsTokens.tertiary, AppleLyricsTokens.secondary, manualState.value) }
    )
    Box(modifier.fillMaxWidth().padding(horizontal = AppleLyricsTokens.rowMargin), contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .fillMaxWidth(if (row.duet) AppleLyricsTokens.DUET_WIDTH else 1f)
                .clip(RoundedCornerShape(AppleLyricsTokens.rippleCorner))
                .background(if (selected) AppleLyricsTokens.quaternary else Color.Transparent)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .semantics(mergeDescendants = true) { contentDescription = content.description }
                .padding(
                    start = AppleLyricsTokens.textPaddingHorizontal, end = AppleLyricsTokens.textPaddingHorizontal,
                    top = AppleLyricsTokens.textPaddingTop, bottom = AppleLyricsTokens.textPaddingBottom
                )
                .graphicsLayer {
                    val scale = AppleLyricsTokens.INACTIVE_SCALE + (1f - AppleLyricsTokens.INACTIVE_SCALE) * focusState.value
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(pivotX, 0.5f)
                    val radius = blurState.value.dp.toPx()
                    renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
                },
            horizontalAlignment = if (end) Alignment.End else Alignment.Start
        ) {
            AppleVocalLines(row, content, text)
        }
    }
}

@Stable
private class AppleRowTextScope(
    val kit: AppleLyricsTextKit,
    val align: TextAlign,
    val focused: Boolean,
    val position: () -> Long,
    val focus: () -> Float,
    val inactive: () -> Color
)

@Composable
private fun AppleRowText(
    scope: AppleRowTextScope,
    text: String,
    words: List<com.luc4n3x.levyra.domain.LyricWord>,
    style: TextStyle,
    colors: AppleLineColors,
    modifier: Modifier = Modifier
) {
    val kit = scope.kit
    AppleLyricText(
        text = text, words = words, style = style, align = scope.align, measurer = kit.measurer, fontKey = kit.fontKey,
        colors = colors, karaoke = kit.karaoke && words.isNotEmpty(), focused = scope.focused, animations = kit.animations,
        focus = scope.focus, inactive = scope.inactive, position = scope.position, modifier = modifier
    )
}

@Composable
private fun AppleVocalLines(row: AppleLyricsRow.Vocal, content: AppleVocalContent, scope: AppleRowTextScope) {
    val kit = scope.kit
    AppleRowText(scope, content.mainText, content.mainWords, kit.main, AppleMainColors)
    content.romanized?.let { AppleRowText(scope, it, emptyList(), kit.secondary, AppleSecondaryColors) }
    if (kit.showTranslation && content.translated != null) {
        AppleRowText(scope, content.translated, emptyList(), kit.secondary, AppleSecondaryColors, Modifier.padding(top = AppleLyricsTokens.translationTop))
    }
    row.background.forEach { background ->
        AppleRowText(scope, background.text, background.words, kit.background, AppleBackgroundColors, Modifier.padding(top = AppleLyricsTokens.backgroundTop))
        if (kit.showTranslation && background.translated.isNotBlank()) {
            Text(
                background.translated, style = kit.secondary.copy(fontSize = 14.sp, color = AppleLyricsTokens.secondary, textAlign = scope.align),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
    }
}

private val AppleMainColors = AppleLineColors(AppleLyricsTokens.primary, AppleLyricsTokens.tertiary)
private val AppleSecondaryColors = AppleLineColors(AppleLyricsTokens.secondary, AppleLyricsTokens.tertiary)
private val AppleBackgroundColors = AppleLineColors(AppleLyricsTokens.backgroundActive, AppleLyricsTokens.tertiary)

@Composable
private fun AppleInstrumentalRow(row: AppleLyricsRow.Instrumental, focused: Boolean, position: () -> Long, animations: Boolean, modifier: Modifier) {
    val exit = remember(row.key) { Animatable(1f) }
    val appear = remember(row.key) { Animatable(0f) }
    LaunchedEffect(focused, animations) {
        if (focused) {
            exit.snapTo(0f)
            if (animations) {
                appear.snapTo(0f)
                appear.animateTo(1f, tween(AppleLyricsTokens.DOT_APPEAR_MS + AppleLyricsTokens.DOT_STAGGER_MS * 2, easing = LinearEasing))
            } else {
                appear.snapTo(1f)
            }
        } else if (exit.value < 1f) {
            if (animations) {
                exit.animateTo(1f, tween(AppleLyricsTokens.DOT_GROW_MS + AppleLyricsTokens.DOT_SHRINK_MS, easing = LinearEasing))
            } else {
                exit.snapTo(1f)
            }
        }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier.fillMaxWidth()
            .padding(horizontal = AppleLyricsTokens.rowMargin)
            .padding(
                start = AppleLyricsTokens.textPaddingHorizontal, end = AppleLyricsTokens.textPaddingHorizontal,
                top = AppleLyricsTokens.textPaddingTop, bottom = AppleLyricsTokens.textPaddingBottom
            )
            .height(AppleLyricsTokens.dotSize + 16.dp)
            .graphicsLayer {
                val now = position()
                val gap = (row.endMs - row.startMs).coerceAtLeast(1L)
                val cycle = appleLyricsBreathingCycleMs(gap)
                val phase = ((now - row.startMs).coerceAtLeast(0L) % cycle).toFloat() / cycle
                val breathingEase = phase * phase * (3f - 2f * phase)
                val breathe = if (animations) {
                    if (breathingEase < 0.5f) 1f + (AppleLyricsTokens.BREATHING_SCALE - 1f) * breathingEase * 2f
                    else AppleLyricsTokens.BREATHING_SCALE - (AppleLyricsTokens.BREATHING_SCALE - 1f) * (breathingEase - 0.5f) * 2f
                } else 1f
                val exitMs = exit.value * (AppleLyricsTokens.DOT_GROW_MS + AppleLyricsTokens.DOT_SHRINK_MS)
                val (scale, alpha) = when {
                    exit.value <= 0f -> breathe to 1f
                    exitMs < AppleLyricsTokens.DOT_GROW_MS -> {
                        val t = AppleLyricsEase.transform(exitMs / AppleLyricsTokens.DOT_GROW_MS)
                        breathe + (AppleLyricsTokens.BREATHING_SCALE - breathe) * t to 1f
                    }
                    exit.value >= 1f -> AppleLyricsTokens.EXIT_SCALE to 0f
                    else -> {
                        val t = AppleLyricsExitEase.transform((exitMs - AppleLyricsTokens.DOT_GROW_MS) / AppleLyricsTokens.DOT_SHRINK_MS)
                        AppleLyricsTokens.BREATHING_SCALE + (AppleLyricsTokens.EXIT_SCALE - AppleLyricsTokens.BREATHING_SCALE) * t to 1f - t
                    }
                }
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            },
        horizontalArrangement = Arrangement.spacedBy(AppleLyricsTokens.dotGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { dot ->
            Box(
                Modifier.padding(start = if (dot == 0) AppleLyricsTokens.dotGap else 0.dp).size(AppleLyricsTokens.dotSize)
                    .graphicsLayer {
                        val stagger = AppleLyricsTokens.DOT_STAGGER_MS * dot
                        val total = AppleLyricsTokens.DOT_APPEAR_MS + AppleLyricsTokens.DOT_STAGGER_MS * 2f
                        val local = ((appear.value * total - stagger) / AppleLyricsTokens.DOT_APPEAR_MS).coerceIn(0f, 1f)
                        alpha = AppleLyricsAccelerate.transform(local)
                    }
                    .drawWithCache {
                        onDrawBehind {
                            val fill = AppleLyricsDotEase.transform(appleLyricsDotFill(position(), row, dot))
                            drawCircle(lerp(AppleLyricsTokens.tertiary, AppleLyricsTokens.primary, fill))
                        }
                    }
            )
        }
    }
}

@Composable
private fun AppleLyricsControls(
    visible: Boolean,
    height: androidx.compose.ui.unit.Dp,
    reportedPositionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    verifiedKbps: Int?,
    playing: Boolean,
    resolving: Boolean,
    translationAvailable: Boolean,
    showTranslation: Boolean,
    labels: AppleLyricsLabels,
    actions: AppleLyricsActions,
    onReveal: () -> Unit,
    modifier: Modifier
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(AppleLyricsTokens.CONTROLS_FADE_MS), label = "apple-lyrics-controls")
    Box(modifier.fillMaxWidth().height(height)) {
        Column(
            Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.navigationBarsPadding(),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (translationAvailable) {
                Box(
                    Modifier.padding(start = AppleLyricsTokens.seekbarInset - 10.dp, bottom = 4.dp)
                        .size(48.dp).clip(CircleShape)
                        .clickable(enabled = visible, role = Role.Switch, onClick = actions.onToggleTranslation)
                        .semantics { contentDescription = labels.translation },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Translate, null,
                        tint = if (showTranslation) AppleLyricsTokens.primary else AppleLyricsTokens.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            AppleMusicSeekbar(
                positionMs = reportedPositionMs,
                bufferedMs = bufferedMs,
                durationMs = durationMs,
                onSeek = actions.onSeekFraction,
                modifier = Modifier.padding(horizontal = AppleLyricsTokens.seekbarInset),
                verifiedKbps = verifiedKbps
            )
            Row(
                Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(AppleLyricsTokens.transportGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = actions.onPrevious, enabled = visible, modifier = Modifier.size(AppleLyricsTokens.skipIcon + AppleLyricsTokens.controlPadding * 2)) {
                    Icon(Icons.Rounded.FastRewind, labels.previous, tint = AppleLyricsTokens.primary, modifier = Modifier.size(AppleLyricsTokens.skipIcon))
                }
                Box(
                    Modifier.size(AppleLyricsTokens.playIcon + AppleLyricsTokens.controlPadding * 2).clip(CircleShape)
                        .clickable(enabled = visible, role = Role.Button, onClick = actions.onTogglePlay)
                        .semantics { contentDescription = if (playing) labels.pause else labels.play },
                    contentAlignment = Alignment.Center
                ) {
                    if (resolving) LevyraLoadingIndicator(color = AppleLyricsTokens.primary, modifier = Modifier.size(30.dp))
                    else Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null,
                        tint = AppleLyricsTokens.primary, modifier = Modifier.size(AppleLyricsTokens.playIcon + 10.dp)
                    )
                }
                IconButton(onClick = actions.onNext, enabled = visible, modifier = Modifier.size(AppleLyricsTokens.skipIcon + AppleLyricsTokens.controlPadding * 2)) {
                    Icon(Icons.Rounded.FastForward, labels.next, tint = AppleLyricsTokens.primary, modifier = Modifier.size(AppleLyricsTokens.skipIcon))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = AppleLyricsTokens.seekbarInset - AppleLyricsTokens.dockPadding, vertical = 0.dp).padding(bottom = AppleLyricsTokens.dockBottom)) {
                Box(
                    Modifier.size(AppleLyricsTokens.dockIcon + AppleLyricsTokens.dockPadding * 2)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(enabled = visible, role = Role.Button, onClick = actions.onClose)
                        .semantics { contentDescription = labels.close },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(AppleLyricsTokens.dockIcon + 8.dp).clip(RoundedCornerShape(7.dp)).background(AppleLyricsTokens.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FormatQuote, null, tint = Color.Black.copy(alpha = 0.78f), modifier = Modifier.size(AppleLyricsTokens.dockIcon - 4.dp))
                    }
                }
            }
        }
        if (!visible) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onReveal))
        }
    }
}
