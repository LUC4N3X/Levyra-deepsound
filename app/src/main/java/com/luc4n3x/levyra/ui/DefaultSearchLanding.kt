package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.trackContentDescription

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

private val DefaultRecentArtShape = RoundedCornerShape(24.dp)
private val DefaultBrowseShape = RoundedCornerShape(24.dp)
private val DefaultRecentTileSize = 112.dp
private val DefaultSearchBarHeight = 56.dp
private val DefaultSectionTop = 22.dp
private val DefaultSampleReelWidth = 148.dp
private val DefaultSampleReelShape = RoundedCornerShape(28.dp)
private val DefaultSampleReelScrim = Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.28f), 0.32f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.86f))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSkinSearchHeader(
    query: String, placeholder: String, animatePlaceholder: Boolean, isSearching: Boolean,
    recognitionAvailable: Boolean, recognitionBusy: Boolean, onQuery: (String) -> Unit,
    onClear: () -> Unit, onBack: () -> Unit, launchVoiceSearch: () -> Unit, toggleRecognition: () -> Unit,
    onLyrics: (() -> Unit)?
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val animationsEnabled = LocalAnimationsEnabled.current
    val fieldInteraction = remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val spec = if (animationsEnabled) LevyraMotion.standard<Color>() else snap()
    val container by animateColorAsState(if (focused) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh, spec, label = "default-search-bar")
    val outline by animateColorAsState(if (focused) scheme.primary else Color.Transparent, spec, label = "default-search-outline")
    val leadingTint by animateColorAsState(if (focused || query.isNotEmpty()) scheme.primary else scheme.onSurfaceVariant, spec, label = "default-search-icon")

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevyraBackButton(
                onClick = {
                    if (query.isBlank()) onBack() else onClear()
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                contentDescription = strings.back
            )
            Text(
                text = strings.search,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 14.dp).semantics { heading() }
            )
            if (recognitionAvailable) SearchHeaderRecognitionButton(recognitionBusy, toggleRecognition)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DefaultSearchBarHeight)
                .clip(CircleShape)
                .background(container)
                .border(1.5.dp, outline, CircleShape)
                .padding(start = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = leadingTint, modifier = Modifier.size(22.dp))
            SearchHeaderTextField(query, placeholder, animatePlaceholder, fieldInteraction, onQuery, Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) { text ->
                DefaultSearchPlaceholder(text, animatePlaceholder)
            }
            SearchHeaderClearAction(query, isSearching, onClear)
            if (onLyrics != null && query.isEmpty()) {
                IconButton(onClick = onLyrics) {
                    Icon(Icons.Rounded.FormatQuote, contentDescription = strings.lyrics, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
            IconButton(onClick = launchVoiceSearch) {
                Icon(Icons.Rounded.Mic, contentDescription = strings.voice, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun DefaultSearchPlaceholder(text: String, animate: Boolean) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (animate) {
                slideInVertically(tween(420)) { it / 2 } + fadeIn(tween(420)) togetherWith slideOutVertically(tween(320)) { -it / 2 } + fadeOut(tween(240))
            } else {
                fadeIn(snap()) togetherWith fadeOut(snap())
            }
        },
        label = "default-search-placeholder"
    ) { value ->
        val scheme = MaterialTheme.colorScheme
        val styled = remember(value, scheme.primary) { emphasizedPlaceholder(value, scheme.primary) }
        BasicText(
            styled,
            style = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurfaceVariant, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp)
        )
    }
}

private val PlaceholderQuote = Regex("[\"“«„](.+?)[\"”»“]")

internal fun emphasizedPlaceholder(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    val match = PlaceholderQuote.find(text)
    if (match == null) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, match.range.first))
    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
    append(text.substring(match.range.last + 1))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val actionsLabel = LocalLevyraStrings.current.actions
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(tracks, key = { _, track -> "default-recent-${track.id}" }, contentType = { _, _ -> "DefaultSearchLanding-tracks" }) { _, track ->
                var menuExpanded by remember(track.id) { mutableStateOf(false) }
                val description = trackContentDescription(track)
                Column(
                    Modifier.width(DefaultRecentTileSize)
                        .levyraPressable(
                            onClick = { onTrackClick(track) }, onLongClick = { menuExpanded = true }, onLongClickLabel = actionsLabel,
                            pressedScale = LevyraPressScale.Tile, role = Role.Button
                        )
                        .semantics(mergeDescendants = true) { contentDescription = description }
                ) {
                    Box(Modifier.size(DefaultRecentTileSize).clip(DefaultRecentArtShape).background(scheme.surfaceContainerHighest)) {
                        CoverImage(track, Modifier.fillMaxSize())
                        menu(track, menuExpanded) { menuExpanded = false }
                    }
                    Column(Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp).clearAndSetSemantics { }) {
                        LevyraMediaTitle(track.title, explicit = track.explicit, color = scheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, marqueeEnabled = false)
                        Text(track.artist, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSearchSamplesRail(title: String, samples: List<Track>, previewEnabled: Boolean, onOpen: (Int) -> Unit) {
    if (samples.isEmpty()) return
    val badgeShape = MaterialShapes.SoftBurst.toShape()
    Column(Modifier.padding(top = DefaultSectionTop), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            color = LevyraText,
            style = LevyraType.sectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(samples, key = { _, track -> "default-sample-${track.id}" }, contentType = { _, _ -> "default-sample-reel" }) { index, track ->
                DefaultSampleReelCard(track, index, samples.size, previewEnabled && index == 0, badgeShape) { onOpen(index) }
            }
        }
    }
}

@Composable
private fun DefaultSampleReelCard(track: Track, index: Int, count: Int, preview: Boolean, badgeShape: Shape, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val description = trackContentDescription(track)
    Box(
        Modifier
            .width(DefaultSampleReelWidth)
            .aspectRatio(9f / 16f)
            .clip(DefaultSampleReelShape)
            .background(scheme.surfaceContainerHighest)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics { }, highRes = index < 3)
        if (preview) SpotifySamplePreview(track)
        Box(Modifier.matchParentSize().background(DefaultSampleReelScrim))
        Text(
            "${index + 1}/$count",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.38f))
                .padding(horizontal = 9.dp, vertical = 3.dp)
                .clearAndSetSemantics { }
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 56.dp, bottom = 14.dp).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LevyraMediaTitle(
                track.title, explicit = track.explicit, color = Color.White, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, marqueeEnabled = false
            )
            Text(track.artist, color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp).clip(badgeShape).background(scheme.primary).clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun DefaultBrowseCard(title: String, artworkUrl: String, accent: Pair<Color, Color>, modifier: Modifier, onClick: () -> Unit) {
    var failed by remember(artworkUrl) { mutableStateOf(false) }
    val (start, end) = accent
    Box(
        modifier.clip(DefaultBrowseShape)
            .background(Brush.linearGradient(listOf(lerp(start, Color.Black, 0.18f), lerp(end, Color.Black, 0.38f))))
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 18.dp, y = 10.dp).size(84.dp)
                .graphicsLayer { rotationZ = 24f; shadowElevation = 10.dp.toPx(); shape = RoundedCornerShape(14.dp); clip = true }
                .background(lerp(end, Color.Black, 0.5f))
                .clearAndSetSemantics { }
        ) {
            if (artworkUrl.isNotBlank() && !failed) AsyncImage(artworkUrl, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize())
        }
        Text(
            title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.68f).padding(start = 16.dp, top = 14.dp).clearAndSetSemantics { }
        )
    }
}
