package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.IvoryCornerShape
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.TorqueStripShape

private enum class SkinGenreRail(val gap: Dp, val edgeScale: Float, val edgeFade: Float) {
    Nocturne(16.dp, 0.025f, 0.45f),
    Prism(14.dp, 0.07f, 0.26f),
    Torque(10.dp, 0.035f, 0.42f),
    Ivory(14.dp, 0f, 0f)
}

private class GenreChartTile(
    val genre: String,
    val kicker: String,
    val tone: Color,
    val artwork: String?,
    val artists: String,
    val rank: Int,
    val total: Int
)

private val TorqueGenreSide = 164.dp
private val IvoryGenreSide = 176.dp
private val PrismGenreWidth = 184.dp
private val PrismGenreHeight = 232.dp
private val NocturneGenreSide = 152.dp
private val NocturnePlaylistSide = 148.dp
private val AtelierGenreSleeve = 160.dp
private val AtelierVinylPeek = 30.dp
private const val AtelierRailScale = 0.05f
private const val AtelierRailFade = 0.3f
private val AtelierSleeveInk = Color(0xFF1E1A15)
private val AtelierSleeveInkSoft = Color(0xFF6B5638)
private val GenreCoverShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 10f)
private val TorqueGenreGrade = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
private val NocturneGenreGrade = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun skinRailMotionEnabled(): Boolean =
    LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion

internal fun LazyListScope.skinGenreChartsSection(
    spec: HomeStyleSpec,
    showCharts: Boolean,
    deferredReady: Boolean,
    onOpenChart: (ExploreZone) -> Unit,
    onOpenPlaylist: (PlaylistHit) -> Unit
) {
    if (!deferredReady) return
    val skin = when {
        LevyraIsNocturne -> SkinGenreRail.Nocturne
        LevyraIsPrismOnly -> SkinGenreRail.Prism
        LevyraIsTorque -> SkinGenreRail.Torque
        LevyraIsIvory -> SkinGenreRail.Ivory
        else -> return
    }
    if (showCharts) {
        item(key = "skin-genre-charts", contentType = "skin-genre-charts") { SkinGenreChartShelf(skin, spec, onOpenChart) }
    }
    if (skin == SkinGenreRail.Nocturne) {
        item(key = "nocturne-curated-collections", contentType = "nocturne-curated-collections") { NocturneCuratedCollections(spec, onOpenPlaylist) }
    }
}

internal fun LazyListScope.atelierGenreChartsSection(visible: Boolean, onOpen: (ExploreZone) -> Unit) {
    if (!visible) return
    item(key = "atelier-genre-charts", contentType = "atelier-genre-charts") { AtelierGenreChartShelf(onOpen) }
}

@Composable
private fun rememberGenreChartZones(): List<ExploreZone> {
    val strings = LocalLevyraStrings.current
    return remember(strings) { exploreGenreChartZones(ExploreCatalog.getZones(strings), strings.code) }
}

@Composable
private fun rememberGenreChartTile(zone: ExploreZone, kicker: String, rank: Int, total: Int): GenreChartTile {
    val cover = rememberGenreChartCover(zone)
    return remember(zone.id, zone.label, kicker, cover, rank, total) {
        GenreChartTile(
            genre = zone.label,
            kicker = kicker,
            tone = genreChartCoverTone(zone),
            artwork = cover.artworkUrl.trim().takeIf(String::isNotEmpty),
            artists = cover.artists.joinToString(", "),
            rank = rank,
            total = total
        )
    }
}

@Composable
private fun Modifier.genreChartPress(tile: GenreChartTile, onClick: () -> Unit): Modifier =
    levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = "${tile.kicker} ${tile.genre}")
        .semantics(mergeDescendants = true) {
            contentDescription = listOf("${tile.kicker} ${tile.genre}", tile.artists).filter(String::isNotBlank).joinToString(", ")
        }

@Composable
private fun SkinGenreChartShelf(skin: SkinGenreRail, spec: HomeStyleSpec, onOpen: (ExploreZone) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { genreChartCopy(strings.code) }
    val zones = rememberGenreChartZones()
    if (zones.isEmpty()) return
    val inset = spec.inset
    Column(Modifier.fillMaxWidth()) {
        when (skin) {
            SkinGenreRail.Nocturne -> {
                StudioSectionHeader(HomeStyle.Minimal, spec, copy.shelfTitle, featured = false, onPlayAll = null)
                Spacer(Modifier.height(8.dp))
            }
            SkinGenreRail.Prism -> PrismShelfHeader(copy.shelfTitle, inset)
            SkinGenreRail.Torque, SkinGenreRail.Ivory -> SkinSectionTitle(copy.shelfTitle, inset)
        }
        val rowState = rememberLazyListState()
        val motion = if (skin == SkinGenreRail.Ivory) ivoryRailMotionEnabled() else skinRailMotionEnabled()
        LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(skin.gap)) {
            itemsIndexed(zones, key = { _, zone -> "skin-genre-chart-${zone.id}" }, contentType = { _, _ -> "skin-genre-chart" }) { index, zone ->
                val itemKey = "skin-genre-chart-${zone.id}"
                val tile = rememberGenreChartTile(zone, copy.title, index + 1, zones.size)
                val open = { onOpen(zone) }
                Box(skinRailItemMotion(skin, motion, rowState, itemKey)) {
                    when (skin) {
                        SkinGenreRail.Nocturne -> NocturneGenreChartCard(tile, open)
                        SkinGenreRail.Prism -> PrismGenreChartCard(tile, open)
                        SkinGenreRail.Torque -> TorqueGenreChartCard(tile, open)
                        SkinGenreRail.Ivory -> IvoryGenreChartCard(tile, open)
                    }
                }
            }
        }
    }
}

private fun skinRailItemMotion(skin: SkinGenreRail, enabled: Boolean, state: LazyListState, key: Any): Modifier =
    if (skin == SkinGenreRail.Ivory) Modifier.ivoryRailItemMotion(enabled, state, key)
    else Modifier.railEdgeMotion(enabled, state, key, skin.edgeScale, skin.edgeFade)

@Composable
private fun TorqueGenreChartCard(tile: GenreChartTile, onOpen: () -> Unit) {
    val accent = LevyraActivePalette.blue
    Column(Modifier.width(TorqueGenreSide).genreChartPress(tile, onOpen)) {
        Box(Modifier.size(TorqueGenreSide).background(LevyraPanel).clearAndSetSemantics { }) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(tile.tone, LevyraBlack))))
            tile.artwork?.let { url ->
                AsyncImage(url, null, contentScale = ContentScale.Crop, colorFilter = TorqueGenreGrade, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
            Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(tile.tone))
            Text(
                tile.kicker.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(TorqueTagShape).background(accent).padding(horizontal = 12.dp, vertical = 5.dp)
            )
            Text(
                tile.rank.toString().padStart(2, '0') + "/" + tile.total.toString().padStart(2, '0'),
                color = Color.White.copy(alpha = 0.86f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 10.dp)
            )
            Text(
                tile.genre.uppercase(), color = Color.White, fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(shadow = GenreCoverShadow),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
        Text(
            tile.artists, color = LevyraMuted, fontSize = 12.sp, lineHeight = 16.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun IvoryGenreChartCard(tile: GenreChartTile, onOpen: () -> Unit) {
    val wash = remember(tile.tone) { lerp(tile.tone, Color.Black, 0.35f) }
    Column(Modifier.width(IvoryGenreSide).genreChartPress(tile, onOpen)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(wash).clearAndSetSemantics { }) {
            tile.artwork?.let { url -> AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to wash.copy(alpha = 0.92f))))
            Text(
                tile.kicker, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 12.dp, vertical = 5.dp)
            )
            Text(
                tile.genre, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(shadow = GenreCoverShadow), fontSize = 22.sp, lineHeight = 26.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 56.dp, bottom = 16.dp)
            )
            Box(Modifier.align(Alignment.BottomEnd).size(48.dp).clip(IvoryCornerShape).background(IvoryDesign.ink)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward, null, tint = IvoryDesign.onInk,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp).size(18.dp)
                )
            }
        }
        Text(
            tile.artists, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun PrismGenreChartCard(tile: GenreChartTile, onOpen: () -> Unit) {
    val shape = PrismDesign.cut(22.dp)
    val rim = remember(tile.tone) { Brush.sweepGradient(listOf(LevyraCyan, LevyraPink, tile.tone, LevyraCyan)) }
    Column(Modifier.width(PrismGenreWidth).genreChartPress(tile, onOpen)) {
        Box(
            Modifier.fillMaxWidth().height(PrismGenreHeight).clip(shape).background(LevyraBlack)
                .border(1.dp, rim, shape).clearAndSetSemantics { }
        ) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(tile.tone, LevyraBlack))))
            tile.artwork?.let { url -> PrismSplitArtwork(url, Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.38f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f))))
            Text(
                tile.rank.toString().padStart(2, '0'), color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    tile.kicker, maxLines = 1,
                    style = MaterialTheme.typography.labelLarge.copy(brush = PrismAccent.gradient(false), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Text(
                    tile.genre, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, shadow = GenreCoverShadow),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            tile.artists, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun NocturneGenreChartCard(tile: GenreChartTile, onOpen: () -> Unit) {
    val p = Nocturne78Palette
    val shape = RoundedCornerShape(Nocturne78Design.cardCorner)
    Column(Modifier.width(NocturneGenreSide).genreChartPress(tile, onOpen)) {
        Column(Modifier.fillMaxWidth().clip(shape).background(p.panel).border(1.dp, p.outline, shape).clearAndSetSemantics { }) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(lerp(tile.tone, p.black, 0.7f))) {
                tile.artwork?.let { url ->
                    AsyncImage(url, null, contentScale = ContentScale.Crop, colorFilter = NocturneGenreGrade, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(tile.tone.copy(alpha = 0.16f)))
                Text(
                    tile.kicker.uppercase(), color = p.cyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(Nocturne78Design.controlCorner))
                        .background(p.black.copy(alpha = 0.72f)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.cyan))
            Text(
                tile.genre, color = p.text, fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        Text(
            tile.artists, color = p.muted, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun NocturneCuratedCollections(spec: HomeStyleSpec, onOpen: (PlaylistHit) -> Unit) {
    val strings = LocalLevyraStrings.current
    val curated = rememberCuratedPlaylistShelves(strings.code)
    val shelves = remember(curated) { curated.filter { shelf -> shelf.items.any { it.playlist != null } }.distinctBy { it.key } }
    if (shelves.isEmpty()) return
    val motion = skinRailMotionEnabled()
    val openAll = LocalCuratedPlaylistsOpener.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spec.sectionGap)) {
        shelves.forEachIndexed { index, shelf ->
            key(shelf.key) {
                val tiles = remember(shelf.items) { shelf.items.filter { it.playlist != null }.distinctBy { it.key } }
                if (tiles.isNotEmpty()) Column(Modifier.fillMaxWidth()) {
                    if (index == 0 && openAll != null) {
                        Row(Modifier.fillMaxWidth().padding(end = spec.inset), verticalAlignment = Alignment.CenterVertically) {
                            StudioSectionHeader(HomeStyle.Minimal, spec, shelf.title, featured = false, onPlayAll = null, modifier = Modifier.weight(1f))
                            LevyraSectionShowAll(onClick = openAll)
                        }
                    } else {
                        StudioSectionHeader(HomeStyle.Minimal, spec, shelf.title, featured = false, onPlayAll = null)
                    }
                    Spacer(Modifier.height(8.dp))
                    val rowState = rememberLazyListState()
                    LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = spec.inset), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(tiles, key = { it.key }, contentType = { "nocturne-collection" }) { tile ->
                            Box(Modifier.railEdgeMotion(motion, rowState, tile.key, SkinGenreRail.Nocturne.edgeScale, SkinGenreRail.Nocturne.edgeFade)) {
                                NocturnePlaylistCard(tile.title, monoWithoutYoutube(tile.subtitle), tile.thumbnailUrl) { tile.playlist?.let(onOpen) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NocturnePlaylistCard(
    title: String,
    subtitle: String,
    artwork: String,
    modifier: Modifier = Modifier.width(NocturnePlaylistSide),
    onOpen: () -> Unit
) {
    val p = Nocturne78Palette
    val shape = RoundedCornerShape(Nocturne78Design.artworkCorner)
    Column(
        modifier
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = title)
            .semantics(mergeDescendants = true) { contentDescription = listOf(title, subtitle).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(p.panel).border(1.dp, p.outline, shape).clearAndSetSemantics { }) {
            AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(
            title, color = p.text, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
        Text(
            subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
private fun AtelierGenreChartShelf(onOpen: (ExploreZone) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { genreChartCopy(strings.code) }
    val zones = rememberGenreChartZones()
    if (zones.isEmpty()) return
    val rowState = rememberLazyListState()
    val motion = skinRailMotionEnabled()
    Column(Modifier.fillMaxWidth()) {
        AtelierSectionLabel(copy.shelfTitle)
        LazyRow(state = rowState, contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
            itemsIndexed(zones, key = { _, zone -> "atelier-genre-chart-${zone.id}" }, contentType = { _, _ -> "atelier-genre-chart" }) { index, zone ->
                val itemKey = "atelier-genre-chart-${zone.id}"
                val tile = rememberGenreChartTile(zone, copy.title, index + 1, zones.size)
                Box(Modifier.railEdgeMotion(motion, rowState, itemKey, AtelierRailScale, AtelierRailFade)) {
                    AtelierGenreChartSleeve(tile) { onOpen(zone) }
                }
            }
        }
    }
}

@Composable
private fun AtelierGenreChartSleeve(tile: GenreChartTile, onOpen: () -> Unit) {
    Column(Modifier.width(AtelierGenreSleeve + AtelierVinylPeek).genreChartPress(tile, onOpen)) {
        Box(Modifier.fillMaxWidth().height(AtelierGenreSleeve).clearAndSetSemantics { }) {
            AtelierVinyl(null, Modifier.align(Alignment.CenterEnd).size(AtelierGenreSleeve - 12.dp))
            Box(
                Modifier.size(AtelierGenreSleeve).align(Alignment.CenterStart)
                    .graphicsLayer {
                        shadowElevation = 12.dp.toPx()
                        shape = AtelierDesign.sleeveShape
                        clip = true
                    }
                    .background(AtelierPaper)
                    .padding(5.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(lerp(tile.tone, AtelierSleeveInk, 0.4f))) {
                        tile.artwork?.let { url -> AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        Text(
                            "№ " + tile.rank.toString().padStart(2, '0'), color = AtelierPaper, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(AtelierSleeveInk.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 1.dp)) {
                        Text(
                            tile.genre, color = AtelierSleeveInk, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            tile.kicker, color = AtelierSleeveInkSoft, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 12.sp, lineHeight = 15.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Text(
            tile.artists, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, minLines = 2, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp, end = AtelierVinylPeek)
        )
    }
}
