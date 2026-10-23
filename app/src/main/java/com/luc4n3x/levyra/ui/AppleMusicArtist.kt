package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.apple.AppleArtistPage
import com.luc4n3x.levyra.data.apple.AppleArtistPageParser
import com.luc4n3x.levyra.data.apple.AppleArtistShelf
import com.luc4n3x.levyra.data.apple.AppleArtistShelfStyle
import com.luc4n3x.levyra.data.apple.AppleCatalogArtwork
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.data.apple.AppleCatalogTracks
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.appleArtistInfoCopy
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.viewmodel.AppleMusicEditorialViewModel
import com.luc4n3x.levyra.viewmodel.appleArtistKey

internal fun appleMusicLatestRelease(artist: ArtistProfile): ArtistRelease? =
    (artist.singles + artist.albums).withIndex()
        .maxWithOrNull(compareBy<IndexedValue<ArtistRelease>> { it.value.year.toIntOrNull() ?: 0 }.thenByDescending { it.index })
        ?.value

@Composable
internal fun AppleMusicArtistHeroPlay(onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = modifier
            .size(AppleMusicArtistPlaySize)
            .clip(CircleShape)
            .background(AppleMusicDesign.accent)
            .levyraPressable(onClick = onPlay, pressedScale = LevyraPressScale.Control, role = Role.Button)
            .semantics { contentDescription = strings.play },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
internal fun AppleMusicArtistTopActions(
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onMore: () -> Unit,
    menu: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box {
        Row(
            modifier = Modifier
                .height(AppleMusicArtistChromeHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppleMusicArtistChromeHeight)
                    .levyraPressable(onClick = onToggleFollow, pressedScale = LevyraPressScale.Control, role = Role.Switch)
                    .semantics {
                        contentDescription = strings.followArtist
                        stateDescription = if (isFollowed) strings.followingArtist else strings.followArtist
                        role = Role.Switch
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (isFollowed) AppleMusicDesign.accent else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(AppleMusicArtistChromeHeight)
                    .levyraPressable(onClick = onMore, pressedScale = LevyraPressScale.Control, role = Role.Button)
                    .semantics { contentDescription = strings.options },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
            }
        }
        menu()
    }
}

@Composable
internal fun AppleMusicArtistLatestRelease(
    caption: String,
    title: String,
    detail: String,
    artworkUrl: String,
    explicit: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .levyraPressable(onClick = onOpen, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .padding(horizontal = AppleMusicDesign.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppleMusicArtwork(artworkUrl, Icons.Rounded.Album, Modifier.size(AppleMusicArtistReleaseArtwork))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (caption.isNotBlank()) {
                Text(caption.uppercase(), color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AppleMusicTitle(title, explicit, MaterialTheme.typography.bodyLarge, maxLines = 2)
            if (detail.isNotBlank()) {
                Text(detail, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun ArtistRelease.appleLatestCaption(): String = subtitle.ifBlank { year }

@Composable
private fun AppleMusicArtwork(url: String, placeholder: ImageVector, modifier: Modifier, shape: Shape = AppleMusicDesign.artworkShape) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppleMusicDesign.placeholder)
            .border(0.5.dp, AppleMusicDesign.artworkStroke, shape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(placeholder, contentDescription = null, tint = AppleMusicDesign.tertiaryLabel, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun AppleMusicTitle(title: String, explicit: Boolean, style: androidx.compose.ui.text.TextStyle, maxLines: Int = 1) {
    val strings = LocalLevyraStrings.current
    val explicitLabel = remember(strings.code) { LevyraAppleMusicStrings.forCode(strings.code).explicit }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = AppleMusicDesign.label,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (explicit) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppleMusicDesign.secondaryLabel)
                    .semantics { contentDescription = explicitLabel },
                contentAlignment = Alignment.Center
            ) {
                Text("E", color = AppleMusicDesign.background, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

internal val LevyraUsesAppleArtistHero: Boolean get() = LevyraIsAppleMusic || (!LevyraIsPrism && !LevyraIsNocturne)

internal class AppleArtistActions(
    val openAlbum: (AppleCatalogItem) -> Unit,
    val openPlaylist: (AppleCatalogItem) -> Unit,
    val playVideo: (AppleCatalogItem) -> Unit,
    val openArtist: (String) -> Unit
)

@Composable
internal fun rememberAppleArtistPage(artistName: String?, storefront: String, language: String): AppleArtistPage? {
    val viewModel = viewModel<AppleMusicEditorialViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(artistName, storefront, language) {
        artistName?.let { viewModel.loadArtist(it, storefront, language) }
    }
    val key = artistName?.let { appleArtistKey(it, storefront, language) }.orEmpty()
    return state.artist?.takeIf { key.isNotBlank() && state.artistKey == key }
}

private val appleReleaseSuffix = Regex("\\s+-\\s+(EP|Single)$", RegexOption.IGNORE_CASE)
private val appleYear = Regex("^(19|20)\\d{2}$")

internal fun AppleCatalogItem.toArtistAlbumHit(artistName: String): AlbumHit {
    val credit = subtitle.trim().takeUnless { it.isBlank() || appleYear.matches(it) } ?: artistName
    val cleanTitle = title.replace(appleReleaseSuffix, "").trim().ifBlank { title }
    return AlbumHit(
        title = cleanTitle,
        artist = credit,
        year = subtitle.trim().takeIf(appleYear::matches).orEmpty(),
        thumbnailUrl = artworkUrl(AppleAlbumArtworkPx),
        query = "$cleanTitle $credit".trim(),
        explicit = explicit,
        canonicalUrl = url,
        metadataProvider = AppleCatalogTracks.SOURCE
    )
}

private val appleVideoAnnotation = Regex("[(\\[][^)\\]]*[)\\]]")
private val appleVideoMarks = Regex("\\p{M}+")
private val appleVideoNonWord = Regex("[^\\p{L}\\p{N}]+")

private fun appleVideoKey(value: String): String =
    java.text.Normalizer.normalize(value.replace(appleVideoAnnotation, " "), java.text.Normalizer.Form.NFD)
        .replace(appleVideoMarks, "")
        .lowercase(java.util.Locale.ROOT)
        .replace(appleVideoNonWord, " ")
        .trim()

internal fun appleVideoTitleMatches(youtubeTitle: String, appleTitle: String): Boolean {
    val apple = appleVideoKey(appleTitle)
    return apple.isNotBlank() && appleVideoKey(youtubeTitle) == apple
}

internal fun AppleCatalogItem.toApplePlaylistHit(): PlaylistHit =
    PlaylistHit(title = title, author = subtitle, thumbnailUrl = artworkUrl(AppleAlbumArtworkPx), playlistId = id)

@Composable
internal fun PrefetchAppleArtistHero(artistName: String?, storefront: String) {
    val name = artistName?.trim().orEmpty()
    if (name.length < 2) return
    val viewModel = viewModel<AppleMusicEditorialViewModel>()
    val context = LocalContext.current
    val language = LocalLevyraStrings.current.code
    val size = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }.coerceAtMost(AppleHeroArtworkMaxPx)
    LaunchedEffect(name, storefront, language, size) {
        val header = viewModel.prefetchArtistHeader(name, storefront, language) ?: return@LaunchedEffect
        val url = header.heroArtworkUrl(size).takeIf(String::isNotBlank) ?: return@LaunchedEffect
        SingletonImageLoader.get(context).enqueue(ImageRequest.Builder(context).data(url).build())
    }
}

internal fun AppleArtistPage.heroMotion(): MotionArtwork? = squareMotion?.let { motion ->
    val now = System.currentTimeMillis()
    MotionArtwork(
        identityKey = "apple-artist:$id",
        provider = AppleArtistMotionProvider,
        url = motion.videoUrl,
        mimeType = AppleArtistMotionMime,
        width = motion.width.takeIf { it > 0 },
        height = motion.height.takeIf { it > 0 },
        confidence = 100,
        expiresAtMs = now + AppleArtistMotionTtlMs,
        lastVerifiedAtMs = now,
        configEpoch = 0L
    )
}

internal fun AppleArtistPage.heroArtworkUrl(size: Int): String =
    squareMotion?.previewTemplate?.takeIf(String::isNotBlank)?.let { AppleCatalogArtwork.url(it, size, size) }
        ?: AppleCatalogArtwork.url(artworkTemplate, size, size)

internal fun LazyListScope.appleMusicArtistShelves(page: AppleArtistPage, actions: AppleArtistActions) {
    page.shelves.filter { it.style != AppleArtistShelfStyle.Artists }.forEach { shelf ->
        item(key = "apple-shelf-title-${shelf.id}", contentType = "apple-artist-shelf-title") {
            val code = LocalLevyraStrings.current.code
            val title = if (shelf.style == AppleArtistShelfStyle.Video) remember(code) { appleArtistInfoCopy(code).topVideos } else shelf.title
            AppleMusicArtistShelfTitle(title)
        }
        item(key = "apple-shelf-${shelf.id}", contentType = "apple-artist-shelf-${shelf.style.name}") { AppleMusicArtistShelfRow(shelf, actions) }
    }
}

@Composable
internal fun AppleMusicArtistShelfTitle(title: String) {
    Text(
        title,
        color = AppleMusicDesign.label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = AppleMusicDesign.gutter).semantics { heading() }
    )
}

@Composable
private fun AppleMusicArtistShelfRow(shelf: AppleArtistShelf, actions: AppleArtistActions) {
    val viewport = LocalConfiguration.current.screenWidthDp.dp
    val squareWidth = AppleMusicDesign.cardWidth(viewport)
    val wideWidth = viewport - AppleMusicDesign.gutter * 2 - AppleMusicDesign.heroPeek
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(shelf.items, key = { "${shelf.id}-${it.kind}-${it.id}" }) { item ->
            val onClick = {
                when (item.kind) {
                    AppleCatalogItemKind.Album -> actions.openAlbum(item)
                    AppleCatalogItemKind.Playlist -> actions.openPlaylist(item)
                    AppleCatalogItemKind.Video -> actions.playVideo(item)
                    AppleCatalogItemKind.Artist -> actions.openArtist(item.title)
                    else -> Unit
                }
            }
            when (shelf.style) {
                AppleArtistShelfStyle.Feature -> AppleMusicArtistCard(item, wideWidth, 1f, subtitleLines = 2, onClick = onClick)
                AppleArtistShelfStyle.Video -> AppleMusicArtistCard(item, wideWidth, AppleMusicVideoAspect, subtitleLines = 1, onClick = onClick)
                else -> AppleMusicArtistCard(item, squareWidth, 1f, subtitleLines = 1, onClick = onClick)
            }
        }
    }
}

@Composable
private fun AppleMusicArtistCard(item: AppleCatalogItem, width: Dp, aspect: Float, subtitleLines: Int, onClick: () -> Unit) {
    val density = LocalDensity.current
    val imageWidth = with(density) { width.roundToPx() }
    val imageHeight = (imageWidth / aspect).toInt()
    val artwork = remember(item.artworkTemplate, item.coverArtworkTemplate, imageWidth, imageHeight) {
        AppleCatalogArtwork.url(item.coverArtworkTemplate.ifBlank { item.artworkTemplate }, imageWidth, imageHeight)
    }
    Column(
        modifier = Modifier
            .width(width)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AppleMusicArtwork(
            artwork,
            if (item.kind == AppleCatalogItemKind.Video) Icons.Rounded.PlayArrow else Icons.Rounded.Album,
            Modifier.fillMaxWidth().aspectRatio(aspect)
        )
        Spacer(Modifier.height(4.dp))
        AppleMusicTitle(item.title, item.explicit, MaterialTheme.typography.bodyMedium)
        val secondary = if (item.kind == AppleCatalogItemKind.Video) {
            listOf(item.subtitle, item.heading).filter(String::isNotBlank).joinToString(" • ")
        } else {
            item.subtitle
        }
        if (secondary.isNotBlank()) {
            Text(secondary, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = subtitleLines, overflow = TextOverflow.Ellipsis)
        }
        if (item.kind == AppleCatalogItemKind.Video && item.description.isNotBlank()) {
            Text(item.description, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal class AppleArtistBubble(val name: String, val imageUrl: String)

@Composable
internal fun AppleMusicArtistBubbles(artists: List<AppleArtistBubble>, onOpen: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(artists, key = { "apple-bubble-${it.name}" }) { artist ->
            Column(
                modifier = Modifier
                    .width(AppleMusicArtistBubbleSize)
                    .levyraPressable(onClick = { onOpen(artist.name) }, pressedScale = LevyraPressScale.Tile, role = Role.Button),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppleMusicArtwork(artist.imageUrl, Icons.Rounded.Person, Modifier.size(AppleMusicArtistBubbleSize), CircleShape)
                Text(
                    artist.name,
                    color = AppleMusicDesign.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun AppleMusicArtistAbout(title: String, body: AnnotatedString, modifier: Modifier = Modifier, details: List<Pair<String, String>> = emptyList()) {
    val strings = LocalLevyraStrings.current
    var expanded by rememberSaveable(body.text) { mutableStateOf(false) }
    var overflowing by remember(body.text) { mutableStateOf(false) }
    val more = strings.showMore.uppercase()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppleMusicDesign.groupedBackground)
            .animateContentSize()
            .padding(horizontal = AppleMusicDesign.gutter, vertical = AppleMusicDesign.sectionGap),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            color = AppleMusicDesign.label,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 4.dp).semantics { heading() }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .levyraPressable(enabled = overflowing && !expanded, onClick = { expanded = true }, pressedScale = LevyraPressScale.Control, role = Role.Button)
        ) {
            Text(
                body,
                color = AppleMusicDesign.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else AppleMusicArtistAboutLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout -> if (!expanded) overflowing = layout.hasVisualOverflow }
            )
            if (overflowing && !expanded) {
                Text(
                    more,
                    color = AppleMusicDesign.accent,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            Brush.horizontalGradient(
                                0f to AppleMusicDesign.groupedBackground.copy(alpha = 0f),
                                0.25f to AppleMusicDesign.groupedBackground
                            )
                        )
                        .padding(start = 28.dp)
                )
            }
        }
        details.forEach { (label, value) ->
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Text(label.uppercase(), color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyLarge)
                Text(value, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

internal fun appleBioText(raw: String): AnnotatedString = buildAnnotatedString {
    AppleArtistPageParser.styledText(raw).forEach { (text, italic) ->
        if (italic) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text) } else append(text)
    }
}

private const val AppleAlbumArtworkPx = 600
private val AppleMusicArtistBubbleSize = 112.dp
private const val AppleMusicVideoAspect = 16f / 9f
private const val AppleArtistMotionProvider = "apple-motion"
private const val AppleArtistMotionMime = "application/x-mpegURL"
private const val AppleArtistMotionTtlMs = 6L * 60L * 60L * 1000L
private val AppleMusicArtistPlaySize = 44.dp
private val AppleMusicArtistChromeHeight = 44.dp
private val AppleMusicArtistReleaseArtwork = 104.dp
private const val AppleMusicArtistAboutLines = 6
