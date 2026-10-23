package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.apple.AppleArtistPage
import com.luc4n3x.levyra.data.apple.AppleArtistShelfStyle
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.artistBiographyEditorial
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.appleArtistInfoCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.*

internal fun LazyListScope.artistReleaseItems(artist: ArtistProfile, strings: LevyraStrings, onOpenRelease: (ArtistRelease, String) -> Unit) {
    if (LevyraIsSpotify) {
        item(key = "spotify-artist-releases", contentType = "spotify-artist-releases") {
            SpotifyArtistReleases(artist.albums + artist.compilations, artist.singles, artist.name, onOpenRelease, Modifier.padding(top = 12.dp))
        }
        return
    }
    artistReleaseSection("artist-albums", strings.albumsPlain, artist.albums, artist.name, onOpenRelease)
    artistReleaseSection("artist-singles", strings.singlesAndEps, artist.singles, artist.name, onOpenRelease)
    artistReleaseSection("artist-compilations", strings.compilations, artist.compilations, artist.name, onOpenRelease)
}

internal fun LazyListScope.artistPlaylistItems(artist: ArtistProfile, strings: LevyraStrings, onOpenPlaylist: (PlaylistHit) -> Unit) {
    artistPlaylistSection("artist-playlists", strings.playlists, artist.artistPlaylists, onOpenPlaylist)
    artistPlaylistSection("artist-featured-on", searchDiscographyCopy(strings.code).featuredOn, artist.featuredPlaylists, onOpenPlaylist)
}

internal fun LazyListScope.artistReleaseSection(
    key: String,
    title: String,
    releases: List<ArtistRelease>,
    artistName: String,
    onOpenRelease: (ArtistRelease, String) -> Unit
) {
    if (releases.isEmpty()) return
    item(key = "$key-title", contentType = "artist-section-title") { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(title) } }
    item(key = key, contentType = "artist-release-row") { Box(modifier = Modifier.padding(start = 20.dp)) { ArtistReleaseRow(releases, artistName, onOpenRelease) } }
}

internal fun LazyListScope.artistPlaylistSection(key: String, title: String, playlists: List<PlaylistHit>, onOpenPlaylist: (PlaylistHit) -> Unit) {
    if (playlists.isEmpty()) return
    item(key = "$key-title", contentType = "artist-section-title") { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(title) } }
    item(key = key, contentType = "artist-playlist-row") { Box(modifier = Modifier.padding(start = 20.dp)) { ArtistPlaylistRow(playlists, onOpenPlaylist) } }
}

internal fun LazyListScope.appleMusicArtistClosingItems(
    artist: ArtistProfile,
    appleArtist: AppleArtistPage?,
    strings: LevyraStrings,
    onOpenArtistByName: (String) -> Unit,
    bottomPadding: Dp
) {
    val info = appleArtist?.info
    val youtubeBio = artist.biography?.takeIf { artist.hasBio }
    val hasAbout = info?.bio?.isNotBlank() == true || youtubeBio != null
    val appleSimilar = appleArtist?.shelves?.firstOrNull { it.style == AppleArtistShelfStyle.Artists }
    val bubbles = appleSimilar?.items?.map { AppleArtistBubble(it.title, it.artworkUrl(AppleBubbleArtworkPx)) }
        ?: artist.relatedArtists.map { AppleArtistBubble(it.name, it.thumbnailUrl) }
    if (!hasAbout && bubbles.isEmpty()) {
        item(key = "apple-artist-bottom", contentType = "apple-artist-bottom") { Spacer(Modifier.height(bottomPadding)) }
        return
    }
    item(key = "apple-artist-closing", contentType = "apple-artist-closing") {
        Column(modifier = Modifier.fillMaxWidth().background(AppleMusicDesign.groupedBackground)) {
            if (hasAbout) {
                val copy = remember(strings.code) { appleArtistInfoCopy(strings.code) }
                val body = remember(info, youtubeBio) {
                    info?.bio?.takeIf(String::isNotBlank)?.let(::appleBioText) ?: AnnotatedString(youtubeBio?.let(::artistBiographyBody).orEmpty())
                }
                val details = remember(info, copy) {
                    buildList {
                        info?.origin?.takeIf(String::isNotBlank)?.let { add(copy.hometown to it) }
                        info?.bornOrFormed?.takeIf(String::isNotBlank)?.let { add((if (info.isGroup) copy.formed else copy.born) to it) }
                        info?.genres?.takeIf { it.isNotEmpty() }?.let { add(copy.genre to it.joinToString(", ")) }
                    }
                }
                AppleMusicArtistAbout(title = copy.about, body = body, details = details)
            }
            if (bubbles.isNotEmpty()) {
                if (!hasAbout) Spacer(Modifier.height(AppleMusicDesign.sectionGap))
                AppleMusicArtistShelfTitle(appleSimilar?.title?.takeIf(String::isNotBlank) ?: strings.similarArtists)
                Spacer(Modifier.height(AppleSimilarTitleGap))
                AppleMusicArtistBubbles(bubbles, onOpenArtistByName)
            }
            Spacer(Modifier.height(AppleMusicDesign.sectionGap + bottomPadding))
        }
    }
}

@Composable
internal fun ArtistOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isExcluded: Boolean,
    onToggleExclude: () -> Unit,
    isPinnedToHome: Boolean,
    homePinsFull: Boolean,
    onTogglePinToHome: () -> Unit,
    onShuffle: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val speedDialCopy = remember(strings) { strings.speedDialCopy() }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (onShuffle != null) {
            DropdownMenuItem(
                text = { Text(strings.shuffle) },
                leadingIcon = { Icon(Icons.Rounded.Shuffle, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onShuffle()
                }
            )
        }
        DropdownMenuItem(
            text = { Text(if (isExcluded) strings.includeArtist else strings.excludeArtist) },
            leadingIcon = {
                Icon(
                    if (isExcluded) Icons.Rounded.Recommend else Icons.Rounded.DoNotDisturbOn,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onToggleExclude()
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    when {
                        isPinnedToHome -> speedDialCopy.removeFromHome
                        homePinsFull -> speedDialCopy.homeFull
                        else -> speedDialCopy.addToHome
                    }
                )
            },
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
            enabled = isPinnedToHome || !homePinsFull,
            onClick = {
                onDismiss()
                onTogglePinToHome()
            }
        )
    }
}

private fun artistBiographyBody(biography: ArtistBiography): String {
    val editorial = artistBiographyEditorial(
        text = biography.text,
        description = biography.description,
        languageCode = biography.languageCode
    )
    val presentation = buildArtistBiographyPresentation(biography, editorial.paragraphs, editorial.summary)
    return presentation.paragraphs.filter { it.isNotBlank() }.joinToString("\n\n").ifBlank { presentation.previewText }
}

@Composable
private fun ArtistReleaseRow(releases: List<ArtistRelease>, artistName: String, onOpen: (ArtistRelease, String) -> Unit) {
    val width = artistShelfCardWidth()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(releases, key = { "rel-${it.browseId.ifBlank { it.title }}" }) { release ->
            ArtistShelfCard(
                title = release.title,
                subtitle = release.year.ifBlank { release.subtitle },
                imageUrl = release.thumbnailUrl,
                placeholder = Icons.Rounded.Album,
                width = width,
                onClick = { onOpen(release, artistName) }
            )
        }
    }
}

@Composable
private fun ArtistPlaylistRow(playlists: List<PlaylistHit>, onOpen: (PlaylistHit) -> Unit) {
    val width = artistShelfCardWidth()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(playlists, key = { "playlist-${it.playlistId.ifBlank { it.browseId.ifBlank { it.title } }}" }) { playlist ->
            ArtistShelfCard(
                title = playlist.title,
                subtitle = listOf(playlist.author, playlist.trackCountLabel).filter(String::isNotBlank).joinToString(" • "),
                imageUrl = playlist.thumbnailUrl,
                placeholder = Icons.AutoMirrored.Rounded.PlaylistPlay,
                width = width,
                onClick = { onOpen(playlist) }
            )
        }
    }
}

@Composable
private fun artistShelfCardWidth(): Dp =
    if (LevyraIsAppleMusic) AppleMusicDesign.cardWidth(LocalConfiguration.current.screenWidthDp.dp) else ArtistShelfCardWidth

@Composable
private fun ArtistShelfCard(title: String, subtitle: String, imageUrl: String, placeholder: ImageVector, width: Dp, onClick: () -> Unit) {
    val apple = LevyraIsAppleMusic
    Column(
        modifier = Modifier.width(width).pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width)
                .clip(if (apple) AppleMusicDesign.artworkShape else LevyraCardDesign.ArtworkShape)
                .background(if (apple) AppleMusicDesign.placeholder else LevyraPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(placeholder, null, tint = LevyraMuted, modifier = Modifier.size(40.dp))
            }
        }
        if (apple) {
            Text(title, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            Text(title, color = LevyraText, style = LevyraType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = LevyraMuted, style = LevyraType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val ArtistShelfCardWidth = 140.dp
internal const val AppleHeroArtworkMaxPx = 1600
internal const val AppleHeroWaitMs = 4_000L
internal const val AppleReleaseArtworkPx = 400
private const val AppleBubbleArtworkPx = 300
private val AppleSimilarTitleGap = 14.dp
