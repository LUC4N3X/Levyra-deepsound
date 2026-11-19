package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import kotlinx.coroutines.delay

private const val HomeEntranceStepMillis = 35L
private const val HomeEntranceMaxDelayMillis = 280L
private const val HomeEntranceBurstGapNanos = 500_000_000L
private const val HomeEntranceDamping = 0.8f
private const val HomeEntranceStiffness = 520f
private val HomeEntranceRise = 48.dp
private const val HomeEntranceScale = 0.94f

private object HomeEntranceSequence {
    private var lastStartNanos = 0L
    private var count = 0

    fun nextDelayMillis(nowNanos: Long): Long {
        if (nowNanos - lastStartNanos > HomeEntranceBurstGapNanos) count = 0
        lastStartNanos = nowNanos
        return (count++ * HomeEntranceStepMillis).coerceAtMost(HomeEntranceMaxDelayMillis)
    }
}

internal fun homeEntrance(scope: LazyListScope): LazyListScope =
    if (LevyraIsDefaultSkin) HomeEntranceLazyListScope(scope) else scope

private class HomeEntranceLazyListScope(private val delegate: LazyListScope) : LazyListScope by delegate {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        delegate.item(key, contentType) { HomeItemEntrance(Modifier.animateItem(fadeInSpec = null)) { content() } }
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        delegate.items(count, key, contentType) { index -> HomeItemEntrance(Modifier.animateItem(fadeInSpec = null)) { itemContent(index) } }
    }
}

@Composable
private fun HomeItemEntrance(itemMotion: Modifier, content: @Composable () -> Unit) {
    if (!LocalAnimationsEnabled.current) {
        content()
        return
    }
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        delay(HomeEntranceSequence.nextDelayMillis(System.nanoTime()))
        progress.animateTo(1f, spring(HomeEntranceDamping, HomeEntranceStiffness))
        played = true
    }
    Box(
        itemMotion.graphicsLayer {
            val value = progress.value
            translationY = (1f - value) * HomeEntranceRise.toPx()
            val scale = HomeEntranceScale + (1f - HomeEntranceScale) * value
            scaleX = scale
            scaleY = scale
            alpha = value.coerceIn(0f, 1f)
        }
    ) { content() }
}

@Composable
internal fun discussableResonanceTracks(
    tracks: List<com.luc4n3x.levyra.domain.Track>,
    comments: Map<String, com.luc4n3x.levyra.domain.ResonanceCommentSnippet>
): List<com.luc4n3x.levyra.domain.Track> = remember(tracks, comments) {
    val visible = tracks.filterNot { comments[it.id]?.disabled == true }
    if (visible.size == tracks.size) tracks else visible
}
