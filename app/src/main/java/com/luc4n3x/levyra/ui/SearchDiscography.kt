package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.SearchAlbumKind
import com.luc4n3x.levyra.domain.SearchArtistDiscography
import com.luc4n3x.levyra.domain.SearchDiscographyShelf
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import java.util.Locale

internal data class SearchCardModel(
    val key: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val artworkUrl: String,
    val playlist: Boolean
)

internal data class SearchCardAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

internal enum class SearchCardStyle { Expressive, Prism, Torque, Nocturne, Ivory, Spotify }

internal val currentSearchCardStyle: SearchCardStyle
    get() = when {
        LevyraIsTorque -> SearchCardStyle.Torque
        LevyraIsSpotify -> SearchCardStyle.Spotify
        LevyraIsIvory -> SearchCardStyle.Ivory
        LevyraIsPrism -> SearchCardStyle.Prism
        LevyraIsNocturne -> SearchCardStyle.Nocturne
        else -> SearchCardStyle.Expressive
    }

internal fun AlbumHit.searchCardModel(albumLabel: String, singleLabel: String): SearchCardModel {
    val badge = when (releaseType) {
        ReleaseType.Album, ReleaseType.Compilation -> albumLabel
        ReleaseType.Single -> singleLabel
        ReleaseType.Ep -> "EP"
        ReleaseType.Unknown -> ""
    }
    return SearchCardModel(
        key = browseId.ifBlank { "${title.trim().lowercase(Locale.ROOT)}|${artist.trim().lowercase(Locale.ROOT)}" },
        title = title,
        subtitle = year.ifBlank { artist },
        badge = badge,
        artworkUrl = thumbnailUrl,
        playlist = false
    )
}

internal fun PlaylistHit.searchCardModel(): SearchCardModel = SearchCardModel(
    key = playlistId.ifBlank { browseId.ifBlank { title.trim().lowercase(Locale.ROOT) } },
    title = title,
    subtitle = listOf(author, trackCountLabel).filter(String::isNotBlank).joinToString(" · "),
    badge = "",
    artworkUrl = thumbnailUrl,
    playlist = true
)

@Composable
internal fun SearchDiscographySection(
    discography: SearchArtistDiscography,
    onOpenRelease: (AlbumHit) -> Unit,
    onOpenPlaylist: (PlaylistHit) -> Unit,
    onOpenArtist: (ArtistHit) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = searchDiscographyCopy(strings.code)
    val shelves = discography.shelves
    if (shelves.isEmpty()) return
    var selectedName by rememberSaveable(discography.artist.browseId) { mutableStateOf(shelves.first().name) }
    val selected = shelves.firstOrNull { it.name == selectedName } ?: shelves.first()
    val animationsEnabled = LocalAnimationsEnabled.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchDiscographyHeader(
            artist = discography.artist,
            overline = copy.discography,
            onOpenArtist = { onOpenArtist(discography.artist) }
        )
        if (shelves.size > 1) {
            Row(
                modifier = Modifier
                    .recentSearchesBleed(RecentSearchGutter)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = RecentSearchGutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shelves.forEach { shelf ->
                    LevyraFilterPill(
                        label = searchSkinLabel(shelf.label(copy.featuredOn, strings.albumsPlain, strings.singlesAndEps, strings.compilations)),
                        selected = shelf == selected,
                        onClick = { selectedName = shelf.name }
                    )
                }
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(LevyraMotion.standard()) togetherWith fadeOut(LevyraMotion.standard())
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            label = "search-discography-shelf"
        ) { shelf ->
            val cards = if (shelf == SearchDiscographyShelf.Playlists) {
                discography.playlists.map { it.searchCardModel() }
            } else {
                discography.releases(shelf).map { release ->
                    release.searchCardModel(copy.album, copy.single)
                        .let { card -> if (shelf == SearchDiscographyShelf.Singles) card else card.copy(badge = "") }
                }
            }
            LazyRow(
                modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
                contentPadding = PaddingValues(horizontal = RecentSearchGutter),
                horizontalArrangement = Arrangement.spacedBy(searchCardSpacing())
            ) {
                itemsIndexed(
                    cards,
                    key = { index, card -> "discography-${shelf.name}-$index-${card.key}" },
                    contentType = { _, _ -> "search-card" }
                ) { index, card ->
                    SearchCard(
                        card = card,
                        modifier = Modifier.width(SEARCH_SHELF_CARD_WIDTH),
                        onClick = {
                            if (shelf == SearchDiscographyShelf.Playlists) {
                                discography.playlists.getOrNull(index)?.let(onOpenPlaylist)
                            } else {
                                discography.releases(shelf).getOrNull(index)?.let(onOpenRelease)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun SearchDiscographyShelf.label(featuredOn: String, albums: String, singles: String, compilations: String): String =
    when (this) {
        SearchDiscographyShelf.Albums -> albums
        SearchDiscographyShelf.Singles -> singles
        SearchDiscographyShelf.Compilations -> compilations
        SearchDiscographyShelf.Playlists -> featuredOn
    }

@Composable
private fun searchSkinLabel(text: String): String =
    if (LevyraIsTorque || LevyraIsNocturne) text.uppercase(LocalLocale.current.platformLocale) else text

@Composable
private fun SearchDiscographyHeader(artist: ArtistHit, overline: String, onOpenArtist: () -> Unit) {
    val style = currentSearchCardStyle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (style == SearchCardStyle.Torque) {
            Box(Modifier.width(4.dp).height(36.dp).background(LevyraActivePalette.blue))
        }
        SearchDiscographyAvatar(artist, style)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = searchSkinLabel(overline),
                color = if (style == SearchCardStyle.Expressive) MaterialTheme.colorScheme.primary else LevyraCyan,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = if (style == SearchCardStyle.Torque || style == SearchCardStyle.Nocturne) 1.6.sp else 0.4.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (style == SearchCardStyle.Torque) artist.name.uppercase(LocalLocale.current.platformLocale) else artist.name,
                color = LevyraText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = when (style) {
                    SearchCardStyle.Expressive -> FontWeight.Black
                    SearchCardStyle.Torque -> FontWeight.Bold
                    else -> FontWeight.Normal
                },
                letterSpacing = if (style == SearchCardStyle.Torque) 1.4.sp else (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        LevyraSectionShowAll(onClick = onOpenArtist)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchDiscographyAvatar(artist: ArtistHit, style: SearchCardStyle) {
    val shape: Shape = when (style) {
        SearchCardStyle.Expressive -> MaterialShapes.Cookie9Sided.toShape()
        SearchCardStyle.Torque -> RoundedCornerShape(2.dp)
        else -> CircleShape
    }
    val ring = if (style == SearchCardStyle.Prism) {
        Modifier.clip(CircleShape).background(PrismAccent.gradient()).padding(2.dp)
    } else {
        Modifier
    }
    Box(modifier = ring) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (artist.thumbnailUrl.isNotBlank()) {
                StableRemoteArtwork(
                    url = artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

internal fun searchCardSpacing(): Dp = when (currentSearchCardStyle) {
    SearchCardStyle.Torque -> 10.dp
    SearchCardStyle.Spotify -> SpotifyDesign.gutter
    else -> 14.dp
}

@Composable
internal fun SearchCard(
    card: SearchCardModel,
    modifier: Modifier = Modifier,
    actions: List<SearchCardAction> = emptyList(),
    onClick: () -> Unit
) {
    when (currentSearchCardStyle) {
        SearchCardStyle.Expressive -> ExpressiveSearchCard(card, actions, modifier, onClick)
        SearchCardStyle.Prism -> PrismSearchCard(card, actions, modifier, onClick)
        SearchCardStyle.Torque -> TorqueSearchCard(card, actions, modifier, onClick)
        SearchCardStyle.Nocturne -> NocturneSearchCard(card, actions, modifier, onClick)
        SearchCardStyle.Ivory -> IvorySearchCard(card, actions, modifier, onClick)
        SearchCardStyle.Spotify -> SpotifySearchCard(card, actions, modifier, onClick)
    }
}

@Composable
private fun SearchCardActions(actions: List<SearchCardAction>, modifier: Modifier) {
    if (actions.isEmpty()) return
    val shape = if (currentSearchCardStyle == SearchCardStyle.Torque) RoundedCornerShape(2.dp) else CircleShape
    Row(modifier = modifier.padding(4.dp)) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClickLabel = action.label, role = Role.Button, onClick = action.onClick)
                    .semantics { contentDescription = action.label },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(shape).background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchCardArtwork(card: SearchCardModel, modifier: Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (card.artworkUrl.isNotBlank()) {
            StableRemoteArtwork(
                url = card.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                highRes = true
            )
        } else {
            Icon(
                imageVector = if (card.playlist) Icons.AutoMirrored.Rounded.PlaylistPlay else Icons.Rounded.Album,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            SearchCardArtwork(card, Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
            if (card.badge.isNotBlank()) {
                Text(
                    text = card.badge,
                    color = scheme.onTertiaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(MaterialShapes.Pill.toShape())
                        .background(scheme.tertiaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        SearchCardTitle(card.title, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        SearchCardSubtitle(card.subtitle)
    }
}

@Composable
private fun PrismSearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.86f)
                    .height(18.dp)
                    .offset(y = 8.dp)
                    .background(PrismAccent.gradient(), RoundedCornerShape(PrismDesign.artworkCorner))
            )
            SearchCardArtwork(card, Modifier.fillMaxSize().clip(RoundedCornerShape(PrismDesign.artworkCorner)))
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
            if (card.badge.isNotBlank()) {
                Text(
                    text = card.badge,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(PrismAccent.gradient())
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        SearchCardTitle(card.title, MaterialTheme.typography.titleSmall)
        SearchCardSubtitle(card.subtitle)
    }
}

@Composable
private fun IvorySearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape)) {
            SearchCardArtwork(card, Modifier.fillMaxSize())
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
            if (card.badge.isNotBlank()) {
                Text(
                    text = card.badge, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            IvoryCornerBadge(48.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = IvoryDesign.onInk, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        SearchCardTitle(card.title, MaterialTheme.typography.titleSmall)
        SearchCardSubtitle(card.subtitle)
    }
}

@Composable
private fun SpotifySearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    val round = !card.playlist && card.badge.isBlank() && card.subtitle.isBlank()
    Column(modifier = modifier.levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(if (round) CircleShape else SpotifyDesign.coverShape)) {
            SearchCardArtwork(card, Modifier.fillMaxSize())
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(8.dp))
        Text(card.title, color = SpotifyDesign.text, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val detail = listOf(card.badge, card.subtitle).filter { it.isNotBlank() }.joinToString(" • ")
        if (detail.isNotBlank()) Text(detail, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TorqueSearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(LevyraPanel)
            .levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            SearchCardArtwork(card, Modifier.fillMaxSize())
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
            if (card.badge.isNotBlank()) {
                Text(
                    text = card.badge.uppercase(LocalLocale.current.platformLocale),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(TorqueTagShape)
                        .background(LevyraActivePalette.blue)
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(LevyraActivePalette.blue))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                card.title.uppercase(LocalLocale.current.platformLocale),
                color = LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            SearchCardSubtitle(card.subtitle)
        }
    }
}

@Composable
private fun NocturneSearchCard(card: SearchCardModel, actions: List<SearchCardAction>, modifier: Modifier, onClick: () -> Unit) {
    val artworkShape = RoundedCornerShape(Nocturne78Design.artworkCorner)
    Column(
        modifier = modifier.levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = card.title),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            SearchCardArtwork(
                card,
                Modifier
                    .fillMaxSize()
                    .clip(artworkShape)
                    .border(1.dp, LevyraActivePalette.outline, artworkShape)
            )
            SearchCardActions(actions, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(6.dp))
        if (card.badge.isNotBlank()) {
            Text(
                text = card.badge.uppercase(LocalLocale.current.platformLocale),
                color = LevyraCyan,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        SearchCardTitle(card.title, MaterialTheme.typography.titleMedium)
        SearchCardSubtitle(card.subtitle)
    }
}

@Composable
private fun SearchCardTitle(title: String, style: TextStyle) {
    Text(
        text = title,
        color = LevyraText,
        style = style,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun SearchCardSubtitle(subtitle: String) {
    if (subtitle.isBlank()) return
    Text(
        text = subtitle,
        color = LevyraMuted,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
internal fun SearchCardGridRow(
    cards: List<SearchCardModel>,
    firstIndex: Int,
    columns: Int,
    actionsFor: (Int) -> List<SearchCardAction> = { emptyList() },
    onClick: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(searchCardSpacing()), modifier = Modifier.fillMaxWidth()) {
        repeat(columns) { column ->
            val card = cards.getOrNull(column)
            if (card == null) {
                Spacer(Modifier.weight(1f))
            } else {
                SearchCard(
                    card = card,
                    modifier = Modifier.weight(1f),
                    actions = actionsFor(firstIndex + column),
                    onClick = { onClick(firstIndex + column) }
                )
            }
        }
    }
}

internal fun searchGridColumns(maxWidthDp: Int): Int = (maxWidthDp / SEARCH_GRID_MIN_CELL_DP).coerceIn(2, 4)

@Composable
internal fun SearchAlbumKindChips(
    kinds: List<SearchAlbumKind>,
    selected: SearchAlbumKind,
    onSelect: (SearchAlbumKind) -> Unit
) {
    if (kinds.isEmpty()) return
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .recentSearchesBleed(RecentSearchGutter)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = RecentSearchGutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        kinds.forEach { kind ->
            val label = when (kind) {
                SearchAlbumKind.All -> strings.all
                SearchAlbumKind.Albums -> strings.albumsPlain
                SearchAlbumKind.Singles -> strings.singlesAndEps
            }
            LevyraFilterPill(label = searchSkinLabel(label), selected = kind == selected, onClick = { onSelect(kind) })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchArtistListRow(artist: ArtistHit, onClick: () -> Unit) {
    val style = currentSearchCardStyle
    val avatarShape: Shape = when (style) {
        SearchCardStyle.Expressive -> MaterialShapes.Cookie9Sided.toShape()
        SearchCardStyle.Torque -> RoundedCornerShape(2.dp)
        else -> CircleShape
    }
    val container = when (style) {
        SearchCardStyle.Expressive -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f)
        SearchCardStyle.Prism -> Color.Transparent
        SearchCardStyle.Torque -> LevyraPanel
        SearchCardStyle.Nocturne -> LevyraPanelSoft
        SearchCardStyle.Ivory, SearchCardStyle.Spotify -> Color.Transparent
    }
    val rowShape: Shape = when (style) {
        SearchCardStyle.Expressive -> RoundedCornerShape(24.dp)
        SearchCardStyle.Prism -> RoundedCornerShape(PrismDesign.surfaceCorner)
        SearchCardStyle.Torque -> RoundedCornerShape(2.dp)
        SearchCardStyle.Nocturne -> RoundedCornerShape(Nocturne78Design.cardCorner)
        SearchCardStyle.Ivory -> RoundedCornerShape(PrismDesign.surfaceCorner)
        SearchCardStyle.Spotify -> RoundedCornerShape(SpotifyDesign.cornerSmall)
    }
    val avatarRing = if (style == SearchCardStyle.Prism) {
        Modifier.clip(CircleShape).background(PrismAccent.gradient()).padding(2.dp)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(container)
            .levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = artist.name)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = avatarRing) {
            Box(
                modifier = Modifier.size(56.dp).clip(avatarShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (artist.thumbnailUrl.isNotBlank()) {
                    StableRemoteArtwork(
                        url = artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(26.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (style == SearchCardStyle.Torque) artist.name.uppercase(LocalLocale.current.platformLocale) else artist.name,
                color = LevyraText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (style == SearchCardStyle.Expressive || style == SearchCardStyle.Torque) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (artist.subscribers.isNotBlank()) {
                Text(
                    text = artist.subscribers,
                    color = LevyraMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = if (style == SearchCardStyle.Torque) LevyraActivePalette.blue else LevyraMuted
        )
    }
}

private val SEARCH_SHELF_CARD_WIDTH = 156.dp
private const val SEARCH_GRID_MIN_CELL_DP = 168
