package com.luc4n3x.levyra.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.MixChapter
import com.luc4n3x.levyra.domain.MixChapterList
import com.luc4n3x.levyra.domain.MixChapterSource
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartAudioCopy
import com.luc4n3x.levyra.ui.i18n.SmartAudioText
import com.luc4n3x.levyra.ui.i18n.smartAudioCopy
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

private const val MAX_DRAWN_SEGMENTS = 120

internal fun formatMixChapterTime(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

@Composable
internal fun MixChapterStrip(
    chapters: MixChapterList,
    positionMs: Long,
    activeColor: Color,
    inactiveColor: Color,
    contentColor: Color,
    animated: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val index = chapters.indexAt(positionMs)
    val chapter = chapters.chapters.getOrNull(index) ?: return
    val remaining = (chapters.endMs(index) - positionMs).coerceAtLeast(0L)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ListAlt, copy[SmartAudioText.ChaptersOpen], tint = activeColor, modifier = Modifier.size(18.dp))
            AnimatedContent(
                targetState = chapter.title,
                transitionSpec = {
                    if (animated) {
                        (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
                    } else {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    }
                },
                label = "mix-chapter-title",
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            ) { title ->
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                copy[SmartAudioText.ChaptersLeft].replace("{time}", formatMixChapterTime(remaining)),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        MixChapterSegments(chapters, positionMs, activeColor, inactiveColor, Modifier.fillMaxWidth().height(4.dp))
    }
}

@Composable
private fun MixChapterSegments(
    chapters: MixChapterList,
    positionMs: Long,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val duration = chapters.durationMs.coerceAtLeast(1L).toFloat()
    val starts = remember(chapters) {
        val all = chapters.chapters
        if (all.size <= MAX_DRAWN_SEGMENTS) all.map { it.startMs } else {
            val stride = all.size.toFloat() / MAX_DRAWN_SEGMENTS
            List(MAX_DRAWN_SEGMENTS) { all[(it * stride).toInt()].startMs }
        }
    }
    Canvas(modifier) {
        val gap = if (starts.size > 60) 1.dp.toPx() else 2.dp.toPx()
        val radius = CornerRadius(size.height / 2f)
        val playhead = positionMs / duration * size.width
        starts.forEachIndexed { index, start ->
            val left = start / duration * size.width
            val endMs = starts.getOrNull(index + 1)?.toFloat() ?: duration
            val right = endMs / duration * size.width - gap
            val width = (right - left).coerceAtLeast(1f)
            drawRoundRect(inactiveColor, Offset(left, 0f), Size(width, size.height), radius)
            val filled = (playhead - left).coerceIn(0f, width)
            if (filled > 0f) {
                drawRoundRect(activeColor, Offset(left, 0f), Size(filled, size.height), radius)
            }
        }
    }
}

@Composable
internal fun MixChaptersEntry(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainer)
            .clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.ListAlt, null, tint = scheme.primary)
        Text(
            copy[SmartAudioText.ChaptersOpen],
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MixChaptersSheet(
    chapters: MixChapterList,
    positionMs: Long,
    animated: Boolean,
    onSeek: (MixChapter) -> Unit,
    onDismiss: () -> Unit
) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = landscape)
    val currentIndex = chapters.indexAt(positionMs)
    var follow by rememberSaveable { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = scheme.surfaceContainerLow) {
        if (landscape) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(0.42f)) {
                    MixChaptersHeader(chapters, positionMs, currentIndex, copy, follow, { follow = it })
                }
                Box(Modifier.weight(0.58f)) {
                    MixChapterTimeline(chapters, positionMs, currentIndex, follow, animated, onSeek)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                MixChaptersHeader(chapters, positionMs, currentIndex, copy, follow, { follow = it })
                MixChapterTimeline(chapters, positionMs, currentIndex, follow, animated, onSeek)
            }
        }
    }
}

@Composable
private fun MixChaptersHeader(
    chapters: MixChapterList,
    positionMs: Long,
    currentIndex: Int,
    copy: SmartAudioCopy,
    follow: Boolean,
    onFollow: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        Text(copy[SmartAudioText.ChaptersTitle], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = scheme.onSurface)
        val source = if (chapters.source == MixChapterSource.STRUCTURED) SmartAudioText.ChaptersFromVideo else SmartAudioText.ChaptersFromDescription
        Text(
            "${copy[SmartAudioText.ChaptersCount].replace("{n}", chapters.chapters.size.toString())} · ${copy[source]}",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        MixChapterSegments(chapters, positionMs, scheme.primary, scheme.surfaceContainerHighest, Modifier.fillMaxWidth().height(6.dp))
        chapters.chapters.getOrNull(currentIndex)?.let { chapter ->
            val remaining = (chapters.endMs(currentIndex) - positionMs).coerceAtLeast(0L)
            Text(
                "${chapter.title} · ${copy[SmartAudioText.ChaptersLeft].replace("{time}", formatMixChapterTime(remaining))}",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(copy[SmartAudioText.ChaptersFollow], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = follow, onCheckedChange = onFollow)
        }
    }
}

@Composable
private fun MixChapterTimeline(
    chapters: MixChapterList,
    positionMs: Long,
    currentIndex: Int,
    follow: Boolean,
    animated: Boolean,
    onSeek: (MixChapter) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
    val latestIndex by rememberUpdatedState(currentIndex)
    LaunchedEffect(follow, listState) {
        if (!follow) return@LaunchedEffect
        snapshotFlow { latestIndex }.distinctUntilChanged().collect { index ->
            val target = (index - 1).coerceAtLeast(0)
            if (animated) listState.animateScrollToItem(target) else listState.scrollToItem(target)
        }
    }
    val progressInCurrent = chapters.chapters.getOrNull(currentIndex)?.let { chapter ->
        val span = (chapters.endMs(currentIndex) - chapter.startMs).coerceAtLeast(1L).toFloat()
        ((positionMs - chapter.startMs) / span).coerceIn(0f, 1f)
    } ?: 0f
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 28.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
    ) {
        itemsIndexed(chapters.chapters, key = { _, chapter -> chapter.startMs }) { index, chapter ->
            MixChapterRow(
                chapter = chapter,
                durationMs = chapters.endMs(index) - chapter.startMs,
                state = when {
                    index < currentIndex -> MixChapterRowState.PAST
                    index == currentIndex -> MixChapterRowState.CURRENT
                    else -> MixChapterRowState.UPCOMING
                },
                progress = if (index == currentIndex) progressInCurrent else 0f,
                isFirst = index == 0,
                isLast = index == chapters.chapters.lastIndex,
                animated = animated,
                onClick = { onSeek(chapter) }
            )
        }
    }
}

private enum class MixChapterRowState { PAST, CURRENT, UPCOMING }

@Composable
private fun MixChapterRow(
    chapter: MixChapter,
    durationMs: Long,
    state: MixChapterRowState,
    progress: Float,
    isFirst: Boolean,
    isLast: Boolean,
    animated: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val current = state == MixChapterRowState.CURRENT
    val spec = if (animated) spring<androidx.compose.ui.unit.Dp>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) else snap()
    val corner by animateDpAsState(if (current) 26.dp else 14.dp, spec, label = "mix-chapter-corner")
    val nodeSize by animateDpAsState(if (current) 18.dp else 10.dp, spec, label = "mix-chapter-node")
    val container by animateColorAsState(
        if (current) scheme.primaryContainer else Color.Transparent,
        label = "mix-chapter-container"
    )
    val played = scheme.primary
    val pending = scheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(30.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxHeight().width(30.dp)) {
                val x = size.width / 2f
                val middle = size.height / 2f
                val stroke = 3.dp.toPx()
                if (!isFirst) {
                    drawLine(if (state == MixChapterRowState.UPCOMING) pending else played, Offset(x, 0f), Offset(x, middle), stroke, StrokeCap.Round)
                }
                if (!isLast) {
                    drawLine(if (state == MixChapterRowState.PAST) played else pending, Offset(x, middle), Offset(x, size.height), stroke, StrokeCap.Round)
                    if (current) {
                        drawLine(played, Offset(x, middle), Offset(x, middle + (size.height - middle) * progress), stroke, StrokeCap.Round)
                    }
                }
            }
            Box(
                Modifier
                    .size(nodeSize)
                    .clip(CircleShape)
                    .background(if (state == MixChapterRowState.UPCOMING) pending else played)
            )
        }
        Surface(
            color = container,
            shape = RoundedCornerShape(corner),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 3.dp)
                .semantics(mergeDescendants = true) { selected = current; }
                .clip(RoundedCornerShape(corner))
                .clickable(role = Role.Button, onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        chapter.title,
                        style = if (current) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        color = when (state) {
                            MixChapterRowState.CURRENT -> scheme.onPrimaryContainer
                            MixChapterRowState.PAST -> scheme.onSurfaceVariant
                            MixChapterRowState.UPCOMING -> scheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatMixChapterTime(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (current) scheme.onPrimaryContainer.copy(alpha = 0.72f) else scheme.onSurfaceVariant
                    )
                }
                Text(
                    formatMixChapterTime(chapter.startMs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (current) scheme.onPrimaryContainer else scheme.primary,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}
