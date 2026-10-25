package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.credits.TrackCreditGroup
import com.luc4n3x.levyra.data.credits.TrackCreditPerson
import com.luc4n3x.levyra.data.credits.TrackCreditSection
import com.luc4n3x.levyra.data.credits.TrackCredits
import com.luc4n3x.levyra.data.credits.TrackCreditsParser
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.levyraShimmer
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TrackCreditsCopy
import com.luc4n3x.levyra.ui.i18n.trackCreditsCopy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val CreditsCardShape = RoundedCornerShape(26.dp)
private val CreditsRowShape = RoundedCornerShape(18.dp)
private val CreditsArtworkSize = 72.dp
private val CreditsIconContainer = 36.dp
private const val CreditsRevealMs = 260

private sealed interface TrackCreditsLoad {
    data object Loading : TrackCreditsLoad
    data class Ready(val credits: TrackCredits) : TrackCreditsLoad
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackCreditsSheet(
    track: Track,
    artworkUrl: String,
    artists: List<ArtistHit>,
    animated: Boolean,
    loadCredits: suspend (Track, Boolean) -> TrackCredits,
    onOpenArtist: (Int) -> Unit,
    onDismiss: () -> Unit,
    onToggleFollow: ((Int) -> Unit)? = null,
    isFollowed: (Int) -> Boolean = { false }
) {
    val copy = LocalLevyraStrings.current.trackCreditsCopy()
    val languageCode = LocalLevyraStrings.current.code
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentLoadCredits by rememberUpdatedState(loadCredits)
    var retryCount by remember(track.id) { mutableIntStateOf(0) }
    var load by remember(track.id) { mutableStateOf<TrackCreditsLoad>(TrackCreditsLoad.Loading) }

    LaunchedEffect(track.id, retryCount) {
        load = TrackCreditsLoad.Loading
        load = TrackCreditsLoad.Ready(currentLoadCredits(track, retryCount > 0))
    }

    if (com.luc4n3x.levyra.ui.theme.LevyraIsSpotify) {
        SpotifyTrackCreditsSheet(track, artists, load, copy, sheetState, onOpenArtist, onDismiss, onToggleFollow, isFollowed) { retryCount++ }
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TrackCreditsHeader(track = track, artworkUrl = artworkUrl, title = copy.title)
            AnimatedContent(
                targetState = load,
                contentKey = { it is TrackCreditsLoad.Ready },
                transitionSpec = {
                    if (animated) {
                        (fadeIn(tween(CreditsRevealMs)) + slideInVertically(tween(CreditsRevealMs)) { it / 12 })
                            .togetherWith(fadeOut(tween(CreditsRevealMs / 2)))
                    } else {
                        EnterTransition.None.togetherWith(ExitTransition.None)
                    }
                },
                label = "track-credits-content"
            ) { state ->
                when (state) {
                    TrackCreditsLoad.Loading -> TrackCreditsSkeleton(copy.loading)
                    is TrackCreditsLoad.Ready -> TrackCreditsContent(
                        track = track,
                        credits = state.credits,
                        artists = artists,
                        copy = copy,
                        languageCode = languageCode,
                        onOpenArtist = onOpenArtist,
                        onRetry = { retryCount++ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackCreditsHeader(track: Track, artworkUrl: String, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SeamlessArtworkImage(
            url = artworkUrl,
            modifier = Modifier
                .size(CreditsArtworkSize)
                .clip(MaterialShapes.Cookie9Sided.toShape())
        ) {
            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrackCreditsContent(
    track: Track,
    credits: TrackCredits,
    artists: List<ArtistHit>,
    copy: TrackCreditsCopy,
    languageCode: String,
    onOpenArtist: (Int) -> Unit,
    onRetry: () -> Unit
) {
    val artistNames = remember(artists, track.artist) {
        artists.map(ArtistHit::name).ifEmpty { listOf(track.artist) }
    }
    val display = remember(credits, artistNames) { TrackCreditsParser.withArtists(credits, artistNames) }
    val releaseRows = remember(credits.release, copy, languageCode) { releaseRows(credits, copy, languageCode) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        display.sections.forEach { section ->
            TrackCreditsSectionCard(
                section = section,
                copy = copy,
                onPersonClick = { person -> person.linkedArtistIndex?.let(onOpenArtist) }
            )
        }
        if (releaseRows.isNotEmpty()) {
            TrackCreditsCard(icon = Icons.Rounded.Album, title = copy.release) {
                releaseRows.forEach { (label, value) -> TrackCreditsFact(label = label, value = value) }
            }
        }
        if (credits.isEmpty || !credits.remoteResolved) {
            TrackCreditsNotice(
                remoteResolved = credits.remoteResolved,
                copy = copy,
                onRetry = onRetry
            )
        }
        if (credits.provider.isNotBlank()) {
            Text(
                text = "${copy.sourcePrefix} · ${credits.provider}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun TrackCreditsSectionCard(
    section: TrackCreditSection,
    copy: TrackCreditsCopy,
    onPersonClick: (TrackCreditPerson) -> Unit
) {
    val (icon, title) = when (section.group) {
        TrackCreditGroup.Artists -> Icons.Rounded.Groups to copy.artists
        TrackCreditGroup.Performance -> Icons.Rounded.Mic to copy.performance
        TrackCreditGroup.Composition -> Icons.Rounded.EditNote to copy.composition
        TrackCreditGroup.Production -> Icons.Rounded.GraphicEq to copy.production
        TrackCreditGroup.Other -> Icons.Rounded.AutoAwesome to section.title.ifBlank { copy.otherCredits }
    }
    TrackCreditsCard(icon = icon, title = title) {
        section.people.forEachIndexed { index, person ->
            val roles = when {
                section.group != TrackCreditGroup.Artists -> person.roles.joinToString(" · ")
                index == 0 -> copy.mainArtist
                else -> copy.participatingArtist
            }
            TrackCreditsPersonRow(
                person = person,
                roles = roles,
                openLabel = copy.openArtist,
                onClick = if (person.linkedArtistIndex != null) {
                    { onPersonClick(person) }
                } else {
                    null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackCreditsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CreditsCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(CreditsIconContainer)
                        .clip(MaterialShapes.Cookie6Sided.toShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}

@Composable
private fun TrackCreditsPersonRow(
    person: TrackCreditPerson,
    roles: String,
    openLabel: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(CreditsRowShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (roles.isNotBlank()) {
                Text(
                    text = roles,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackCreditsFact(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackCreditsNotice(
    remoteResolved: Boolean,
    copy: TrackCreditsCopy,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CreditsCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (remoteResolved) Icons.Rounded.Info else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                text = if (remoteResolved) copy.emptyTitle else copy.unavailableTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (remoteResolved) copy.emptyBody else copy.unavailableBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!remoteResolved) {
                FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
                    Text(copy.retry)
                }
            }
        }
    }
}

@Composable
private fun TrackCreditsSkeleton(description: String) {
    val placeholder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        }
    ) {
        repeat(3) { index ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CreditsCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .levyraShimmer(accent = MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(CreditsIconContainer)
                                .clip(CreditsRowShape)
                                .background(placeholder)
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(14.dp)
                                .clip(CreditsRowShape)
                                .background(placeholder)
                        )
                    }
                    repeat(if (index == 0) 2 else 3) { line ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (line % 2 == 0) 0.72f else 0.54f)
                                .height(12.dp)
                                .clip(CreditsRowShape)
                                .background(placeholder)
                        )
                    }
                }
            }
        }
    }
}

private fun releaseRows(credits: TrackCredits, copy: TrackCreditsCopy, languageCode: String): List<Pair<String, String>> {
    val release = credits.release
    return listOf(
        copy.album to release.album,
        copy.releaseDate to formatReleaseDate(release.releaseDate, languageCode),
        copy.label to release.label,
        copy.phonographicCopyright to release.phonographicCopyright,
        copy.copyright to release.copyright,
        copy.isrc to release.isrc.uppercase(Locale.ROOT),
        copy.upc to release.upc
    ).filter { it.second.isNotBlank() }
}

private fun formatReleaseDate(raw: String, languageCode: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    return runCatching {
        LocalDate.parse(trimmed.take(10))
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.forLanguageTag(languageCode)))
    }.getOrDefault(trimmed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotifyTrackCreditsSheet(
    track: Track,
    artists: List<ArtistHit>,
    load: TrackCreditsLoad,
    copy: TrackCreditsCopy,
    sheetState: androidx.compose.material3.SheetState,
    onOpenArtist: (Int) -> Unit,
    onDismiss: () -> Unit,
    onToggleFollow: ((Int) -> Unit)?,
    isFollowed: (Int) -> Boolean,
    onRetry: () -> Unit
) {
    val surface = androidx.compose.ui.graphics.Color(0xFF212121)
    val subdued = androidx.compose.ui.graphics.Color(0xFFB3B3B3)
    val artistNames = remember(artists, track.artist) { artists.map(ArtistHit::name).ifEmpty { listOf(track.artist) } }
    val followed = remember(track.id) { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        contentColor = androidx.compose.ui.graphics.Color.White,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(androidx.compose.ui.graphics.Color(0xFF7F7F7F)))
        }
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                track.title, color = androidx.compose.ui.graphics.Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp).semantics { heading() }
            )
            Text(
                track.artist, color = subdued, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(androidx.compose.ui.graphics.Color(0xFF3E3E3E)))
            when (load) {
                TrackCreditsLoad.Loading -> TrackCreditsSkeleton(copy.loading)
                is TrackCreditsLoad.Ready -> {
                    val display = remember(load.credits, artistNames) { TrackCreditsParser.withArtists(load.credits, artistNames) }
                    display.sections.forEach { section ->
                        Text(
                            spotifyCreditsSectionTitle(section, copy, LocalLevyraStrings.current.code), color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp).semantics { heading() }
                        )
                        section.people.forEachIndexed { index, person ->
                            val roles = when {
                                section.group != TrackCreditGroup.Artists -> person.roles.joinToString(" \u2022 ")
                                index == 0 -> copy.mainArtist
                                else -> copy.participatingArtist
                            }
                            val linked = person.linkedArtistIndex
                            val followToggle = if (section.group == TrackCreditGroup.Artists && linked != null && linked in artists.indices && onToggleFollow != null) {
                                { followed[linked] = !(followed[linked] ?: isFollowed(linked)); onToggleFollow(linked) }
                            } else null
                            SpotifyCreditsPersonRow(
                                name = person.name,
                                roles = roles,
                                openLabel = copy.openArtist,
                                following = linked?.let { followed[it] ?: isFollowed(it) } ?: false,
                                onOpen = linked?.let { index -> { onOpenArtist(index) } },
                                onToggleFollow = followToggle
                            )
                        }
                    }
                    if (load.credits.isEmpty || !load.credits.remoteResolved) {
                        Box(Modifier.padding(16.dp)) {
                            TrackCreditsNotice(remoteResolved = load.credits.remoteResolved, copy = copy, onRetry = onRetry)
                        }
                    }
                }
            }
        }
    }
}

private fun spotifyCreditsSectionTitle(section: TrackCreditSection, copy: TrackCreditsCopy, code: String): String {
    val language = code.substringBefore('-')
    return when (section.group) {
        TrackCreditGroup.Artists -> when (language) { "it" -> "Artista"; "en" -> "Artist"; else -> copy.artists }
        TrackCreditGroup.Performance -> copy.performance
        TrackCreditGroup.Composition -> when (language) { "it" -> "Composizione e testo"; "en" -> "Composition & Lyrics"; else -> copy.composition }
        TrackCreditGroup.Production -> when (language) { "it" -> "Produzione e ingegnerizzazione"; "en" -> "Production & Engineering"; else -> copy.production }
        TrackCreditGroup.Other -> when (language) { "it" -> "Altri ruoli"; "en" -> "Additional Credits"; else -> section.title.ifBlank { copy.otherCredits } }
    }
}

@Composable
private fun SpotifyCreditsPersonRow(
    name: String,
    roles: String,
    openLabel: String,
    following: Boolean,
    onOpen: (() -> Unit)?,
    onToggleFollow: (() -> Unit)?
) {
    val subdued = androidx.compose.ui.graphics.Color(0xFFB3B3B3)
    Row(
        Modifier.fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen) else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (roles.isNotBlank()) Text(roles, color = subdued, fontSize = 15.sp, lineHeight = 20.sp)
        }
        if (onToggleFollow != null) {
            Box(
                Modifier.padding(start = 12.dp).height(32.dp).clip(RoundedCornerShape(16.dp))
                    .border(1.dp, if (following) androidx.compose.ui.graphics.Color.White else subdued, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onToggleFollow)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (following) LocalLevyraStrings.current.followingArtist else LocalLevyraStrings.current.followArtist,
                    color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        } else if (onOpen != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = subdued, modifier = Modifier.padding(start = 8.dp).size(28.dp))
        }
    }
}
