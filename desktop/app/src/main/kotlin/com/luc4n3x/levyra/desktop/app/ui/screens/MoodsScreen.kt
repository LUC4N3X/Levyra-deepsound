package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.state.MoodsUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.ExpressiveSectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.horizontalShelfBleed
import com.luc4n3x.levyra.desktop.app.ui.components.rememberArtworkRequest
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.moods.MoodCategory
import com.luc4n3x.levyra.desktop.core.moods.MoodPlaylist
import com.luc4n3x.levyra.ui.i18n.searchBrowseCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun MoodsScreen(
    state: MoodsUiState,
    contentCountry: String,
    newReleasesArtwork: String,
    localWaveArtwork: String,
    onLoad: (language: String, country: String, force: Boolean) -> Unit,
    onRequestArtwork: (String) -> Unit,
    onOpenCategory: (MoodCategory) -> Unit,
    onCloseCategory: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onOpenLocalWave: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val code = LocalStrings.current.languageCode
    val shared = remember(code) { SharedStrings.forCode(code) }
    LaunchedEffect(code, contentCountry) { onLoad(code, contentCountry, false) }
    val open = state.open
    if (open != null) {
        MoodCategoryPage(open, state, shared, onCloseCategory, onOpenCollection, modifier)
        return
    }
    val tiles = remember(state.categories, state.artwork, shared, newReleasesArtwork, localWaveArtwork) {
        listOf(
            BrowseTile("new-releases", shared.exploreNewReleases, newReleasesArtwork, null),
            BrowseTile("local-wave", shared.localWaveName, localWaveArtwork, null)
        ) + state.categories.map { BrowseTile(it.params, it.title, state.artwork[it.params].orEmpty(), it) }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = ((maxWidth - Gutter * 2 + TileGap) / (TileMinWidth + TileGap)).toInt().coerceIn(2, MAX_COLUMNS)
        val palettes = remember(tiles, columns) { browsePaletteIndices(tiles.map { it.identity }, BrowsePalettes.size, columns) }
        val rows = remember(tiles, columns) { tiles.withIndex().chunked(columns) }
        ScrollableColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(TileGap)
        ) {
            item(key = "moods-header") {
                ExpressiveSectionHeader(GridIcon, SectionTints.Amber, remember(code) { searchBrowseCopy(code).browseTitle })
            }
            items(rows, key = { row -> "moods-row-${row.first().value.identity}" }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.forEach { (index, tile) ->
                        val category = tile.category
                        if (category != null) LaunchedEffect(category.params) { onRequestArtwork(category.params) }
                        BrowseCard(
                            title = tile.title,
                            artworkUrl = tile.artworkUrl,
                            accent = BrowsePalettes[palettes[index]],
                            modifier = Modifier.weight(1f).height(TileHeight),
                            onClick = {
                                when {
                                    category != null -> onOpenCategory(category)
                                    tile.identity == "new-releases" -> onOpenNewReleases()
                                    else -> onOpenLocalWave()
                                }
                            }
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (state.categories.isEmpty()) {
                item(key = "moods-status") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        if (state.loading || !state.failed) {
                            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(shared.exploreEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { onLoad(code, contentCountry, true) }) { Text(shared.exploreSamplesRetry) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCategoryPage(
    category: MoodCategory,
    state: MoodsUiState,
    shared: SharedStrings,
    onBack: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    modifier: Modifier
) {
    val accent = BrowsePalettes[(category.params.hashCode() and Int.MAX_VALUE) % BrowsePalettes.size]
    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "mood-hero") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(role = Role.Button, onClickLabel = shared.back, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(LevyraIcons.ChevronLeft, contentDescription = shared.back, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                BrowseHero(category.title, state.artwork[category.params] ?: category.artworkUrl, accent, Modifier.weight(1f).height(HeroHeight))
            }
        }
        if (state.shelves.isEmpty()) {
            item(key = "mood-status") {
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    if (state.shelvesLoading) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                    else Text(shared.exploreEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(state.shelves, key = { shelf -> "mood-shelf-${shelf.title}" }) { shelf ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    shelf.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shelf.playlists, key = { "${shelf.title}-${it.id}" }) { playlist ->
                        PlaylistCard(playlist, onClick = { onOpenCollection(playlist.toCollectionRef()) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseCard(title: String, artworkUrl: String, accent: Pair<Color, Color>, modifier: Modifier, onClick: () -> Unit) {
    val (start, end) = accent
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateFloatAsState(if (hovered) 1f else 0f, label = "moods-tile-hover")
    Box(
        modifier
            .graphicsLayer {
                val scale = 1f + 0.02f * lift
                scaleX = scale
                scaleY = scale
            }
            .clip(TileShape)
            .background(Brush.linearGradient(listOf(lerp(start, Color.Black, 0.18f), lerp(end, Color.Black, 0.38f))))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        TiltedCover(
            artworkUrl,
            lerp(end, Color.Black, 0.5f),
            Modifier.align(Alignment.BottomEnd).offset(x = 18.dp, y = 10.dp).size(CoverSize),
            rotation = 24f - 6f * lift
        )
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.68f).padding(start = 16.dp, top = 14.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun BrowseHero(title: String, artworkUrl: String, accent: Pair<Color, Color>, modifier: Modifier) {
    val (start, end) = accent
    Box(
        modifier.clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(lerp(start, Color.Black, 0.18f), lerp(end, Color.Black, 0.38f))))
    ) {
        TiltedCover(
            artworkUrl,
            lerp(end, Color.Black, 0.5f),
            Modifier.align(Alignment.BottomEnd).offset(x = 28.dp, y = 22.dp).size(HeroCoverSize),
            rotation = 24f
        )
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.7f).padding(start = 24.dp).semantics { heading() }
        )
    }
}

@Composable
private fun TiltedCover(artworkUrl: String, fallback: Color, modifier: Modifier, rotation: Float) {
    val request = rememberArtworkRequest(artworkUrl)
    var failed by remember(artworkUrl) { mutableStateOf(false) }
    Box(
        modifier
            .graphicsLayer {
                rotationZ = rotation
                shadowElevation = 10.dp.toPx()
                shape = CoverShape
                clip = true
            }
            .background(fallback)
            .clearAndSetSemantics { }
    ) {
        if (request != null && !failed) {
            AsyncImage(request, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlaylistCard(playlist: MoodPlaylist, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .width(PlaylistSize)
            .hoverable(interaction)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Artwork(
            playlist.artworkUrl,
            Modifier.size(PlaylistSize).graphicsLayer {
                val scale = if (hovered) 1.03f else 1f
                scaleX = scale
                scaleY = scale
            },
            cornerRadius = 20.dp
        )
        Text(playlist.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        if (playlist.subtitle.isNotBlank()) {
            Text(playlist.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private class BrowseTile(val identity: String, val title: String, val artworkUrl: String, val category: MoodCategory?)

internal fun MoodPlaylist.toCollectionRef() = CollectionRef(
    id = id,
    title = title,
    subtitle = subtitle,
    artworkUrl = artworkUrl,
    url = "https://www.youtube.com/playlist?list=$id",
    kind = CollectionKind.PLAYLIST
)

private fun browsePaletteIndices(identities: List<String>, paletteSize: Int, columns: Int): List<Int> {
    val chosen = ArrayList<Int>(identities.size)
    identities.forEachIndexed { index, identity ->
        val blocked = buildSet {
            if (index % columns != 0) add(chosen[index - 1])
            if (index >= columns) add(chosen[index - columns])
        }
        val preferred = (identity.hashCode() and Int.MAX_VALUE) % paletteSize
        chosen += (0 until paletteSize).map { (preferred + it) % paletteSize }.first { it !in blocked }
    }
    return chosen
}

private val BrowsePalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF7C3AED) to Color(0xFF4C1D95),
    Color(0xFFD81B60) to Color(0xFF7A1538),
    Color(0xFF0F8A78) to Color(0xFF075E54),
    Color(0xFF246BCE) to Color(0xFF173F7A),
    Color(0xFFB96A16) to Color(0xFF70400F),
    Color(0xFF238636) to Color(0xFF145325),
    Color(0xFFB335B5) to Color(0xFF64236C),
    Color(0xFFD9571C) to Color(0xFF7B3215)
)

private fun PathBuilder.roundedSquare(left: Float, top: Float, size: Float, radius: Float) {
    val right = left + size
    val bottom = top + size
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    quadTo(right, top, right, top + radius)
    lineTo(right, bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    lineTo(left + radius, bottom)
    quadTo(left, bottom, left, bottom - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}

private val GridIcon: ImageVector by lazy {
    ImageVector.Builder("LevyraBrowseGrid", 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathData {
            roundedSquare(3f, 3f, 8f, 2f)
            roundedSquare(13f, 3f, 8f, 2f)
            roundedSquare(3f, 13f, 8f, 2f)
            roundedSquare(13f, 13f, 8f, 2f)
        },
        fill = SolidColor(Color.Black)
    ).build()
}

private val Gutter: Dp = 32.dp
private val TileGap: Dp = 12.dp
private val TileMinWidth: Dp = 220.dp
private val TileHeight: Dp = 76.dp
private val CoverSize: Dp = 84.dp
private val HeroHeight: Dp = 148.dp
private val HeroCoverSize: Dp = 156.dp
private val PlaylistSize: Dp = 180.dp
private val TileShape = RoundedCornerShape(24.dp)
private val CoverShape = RoundedCornerShape(14.dp)
private const val MAX_COLUMNS = 6
