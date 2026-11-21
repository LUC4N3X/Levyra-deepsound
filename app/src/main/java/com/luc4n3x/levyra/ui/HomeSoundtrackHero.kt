package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.luc4n3x.levyra.data.AppleArtistArtworkRepository
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.domain.HomeSpotlightCandidate
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import java.util.Locale

internal fun homeSoundtrackPrimaryArtist(rawArtist: String): String {
    var value = rawArtist.trim()
    if (value.isBlank()) return value
    val separators = listOf(",", " & ", " feat. ", " feat ", " featuring ", " x ")
    separators.forEach { separator ->
        val index = value.indexOf(separator, ignoreCase = true)
        if (index > 0) value = value.substring(0, index).trim()
    }
    return value
}

internal data class HomeSpotlightHero(
    val candidate: HomeSpotlightCandidate,
    val artistArtworkUrl: String,
    val artists: List<String>,
    val companionArtworkUrls: List<String>
)

internal fun homeSoundtrackCompanions(
    tracks: List<Track>,
    artistPool: List<Triple<String, String, String>>,
    heroArtistName: String
): List<String> {
    val heroKey = heroArtistName.trim().lowercase(Locale.ROOT)
    return artistPool.asSequence()
        .filter { artist -> homeArtistPortraitUrlEligible(artist.third) }
        .filter { artist -> homeSoundtrackPrimaryArtist(artist.first).lowercase(Locale.ROOT) != heroKey }
        .filter { artist -> tracks.any { track -> homeSoundtrackMatchesArtist(track, artist.first, artist.second) } }
        .distinctBy { artist -> homeSoundtrackPrimaryArtist(artist.first).lowercase(Locale.ROOT) }
        .map { artist -> artist.third.trim() }
        .take(HOME_HERO_COMPANION_LIMIT)
        .toList()
}

internal fun homeSoundtrackArtists(tracks: List<Track>, heroTrack: Track): List<String> {
    return sequenceOf(heroTrack)
        .plus(tracks.asSequence())
        .map { homeSoundtrackPrimaryArtist(it.artist) }
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(3)
        .toList()
}

internal fun homeArtistPortraitUrlEligible(url: String): Boolean {
    val clean = url.trim()
    if (!clean.startsWith("https://", ignoreCase = true)) return false
    if (LevyraPersonalOrbit.isVideoFrameArtworkUrl(clean)) return false
    val lower = clean.lowercase(Locale.ROOT)
    if (lower.contains("i.scdn.co/")) {
        return lower.contains("ab676161")
    }
    return true
}

internal fun homeArtistPortraitSourceRank(url: String): Int {
    val lower = url.trim().lowercase(Locale.ROOT)
    return when {
        lower.contains("mzstatic.com/") -> 0
        lower.contains("i.scdn.co/") && lower.contains("ab676161") -> 1
        lower.contains("googleusercontent.com/") || lower.contains("ggpht.com/") -> 2
        else -> 3
    }
}

internal fun highResolutionHomeArtistArtworkUrl(url: String): String {
    val upgraded = highResolutionPlayerArtworkUrl(url)
    if (!upgraded.contains("i.scdn.co/", ignoreCase = true)) return upgraded
    return upgraded
        .replace("ab67616100005174", "ab6761610000e5eb", ignoreCase = true)
        .replace("ab6761610000f178", "ab6761610000e5eb", ignoreCase = true)
}

@Composable
internal fun rememberHomeHeroPortrait(
    hero: HomeSpotlightHero?,
    searchSongs: suspend (String, Int) -> List<Track>
): HomeSpotlightHero? {
    val context = LocalContext.current
    val spotifyRepository = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    val appleRepository = remember(context) { AppleArtistArtworkRepository.get(context) }
    val languageCode = LocalLevyraStrings.current.code
    val artistName = remember(languageCode) {
        HomeSoundtrackPortraits.artistForDay(languageCode, java.time.LocalDate.now().toEpochDay())
    }
    val qobuzPortraitPage = remember(artistName) { HomeSoundtrackPortraits.qobuzPortraitPage(artistName) }
    val fixedPortrait = remember(artistName) { HomeSoundtrackPortraits.fixedPortrait(artistName) }
    var externalPortraitUrl by rememberSaveable(artistName) {
        mutableStateOf(
            fixedPortrait ?: appleRepository.cachedArtistPortrait(artistName)
                .takeIf(String::isNotBlank)
                ?.let(::highResolutionHomeArtistArtworkUrl)
                .orEmpty()
        )
    }
    var portraitLookupComplete by rememberSaveable(artistName) {
        mutableStateOf(
            artistName.isBlank() || fixedPortrait != null ||
                (externalPortraitUrl.isNotBlank() && qobuzPortraitPage == null)
        )
    }
    LaunchedEffect(artistName, spotifyRepository, appleRepository) {
        if (portraitLookupComplete) return@LaunchedEffect
        val qobuzPortrait = qobuzPortraitPage
            ?.let { page -> com.luc4n3x.levyra.data.qobuz.QobuzEditorialRepository.get(context).artistPortrait(page) }
            .orEmpty()
        val portrait = qobuzPortrait
            .ifBlank { externalPortraitUrl }
            .ifBlank { appleRepository.resolveArtistPortrait(artistName) }
            .ifBlank { spotifyRepository.resolveArtistPortrait(artistName) }
        externalPortraitUrl = portrait.takeIf(String::isNotBlank)
            ?.let(::highResolutionHomeArtistArtworkUrl)
            .orEmpty()
        portraitLookupComplete = true
    }
    val leadArtist = remember(artistName) { homeSoundtrackPrimaryArtist(artistName) }
    var leadTrack by remember(artistName) { mutableStateOf(homeSoundtrackLeadTracks[artistName]) }
    var leadLookupComplete by remember(artistName) { mutableStateOf(artistName.isBlank() || leadTrack != null) }
    LaunchedEffect(artistName) {
        if (leadLookupComplete) return@LaunchedEffect
        leadTrack = searchSongs(artistName, HOME_SOUNDTRACK_LEAD_SEARCH_LIMIT)
            .firstOrNull { track -> homeSoundtrackMatchesArtist(track, leadArtist, "") }
            ?.also { track -> homeSoundtrackLeadTracks[artistName] = track }
        leadLookupComplete = true
    }
    val lead = leadTrack
    return remember(hero, externalPortraitUrl, portraitLookupComplete, lead, leadLookupComplete) {
        when {
            hero == null -> null
            !portraitLookupComplete && externalPortraitUrl.isBlank() || !leadLookupComplete ->
                hero.copy(artistArtworkUrl = "", artists = emptyList())
            externalPortraitUrl.isBlank() || lead == null -> hero
            else -> {
                val heroTrack = hero.candidate.track
                val track = if (homeSoundtrackMatchesArtist(heroTrack, leadArtist, "")) heroTrack else lead
                hero.copy(
                    candidate = hero.candidate.copy(track = track),
                    artistArtworkUrl = externalPortraitUrl,
                    artists = listOf(artistName)
                )
            }
        }
    }
}

private val homeSoundtrackLeadTracks = java.util.concurrent.ConcurrentHashMap<String, Track>()
private const val HOME_SOUNDTRACK_LEAD_SEARCH_LIMIT = 10

internal fun homeSoundtrackQueue(tracks: List<Track>, lead: Track): List<Track> =
    if (tracks.any { it.id == lead.id }) tracks else listOf(lead) + tracks

internal fun homeSoundtrackMatchesArtist(track: Track, artistName: String, browseId: String): Boolean {
    if (artistName.isBlank()) return false
    if (browseId.isNotBlank() && track.artistBrowseIds.any { it.equals(browseId, ignoreCase = true) }) return true
    val primary = homeSoundtrackPrimaryArtist(track.artist)
    return primary.equals(artistName.trim(), ignoreCase = true)
}
