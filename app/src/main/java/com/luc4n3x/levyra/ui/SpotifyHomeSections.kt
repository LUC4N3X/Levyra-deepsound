package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraSectionPlayAll
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

@Composable
internal fun SpotifyRecentShortcuts(tracks: List<Track>, currentId: String?, playing: Boolean, inset: Dp,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    val shortcuts = rememberTrackShelfItems(tracks, currentId, onPlay, onActions)
    SpotifyShortcutGrid(shortcuts, playing, inset, Modifier)
}

internal fun LazyListScope.spotifySpotlight(hero: StudioHero?, playLabel: String, inset: Dp) {
    if (hero == null) return
    item(key = "spotify-picked-for-you", contentType = "home-studio-spotlight") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SpotifySectionTitle(hero.label, inset)
            SpotifyPickedCard(hero, playLabel, inset)
        }
    }
}

internal fun LazyListScope.spotifyPersonalShelves(
    state: LevyraUiState,
    strings: LevyraStrings,
    onPlayFrom: (List<Track>, Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onActions: (Track) -> Unit
) {
    val repeatTracks = LevyraPersonalOrbit.distinctRecordings(state.mostPlayedTracks).take(12)
    val saved = LevyraPersonalOrbit.distinctRecordings(state.favorites).take(12)
    listOf(
        Triple("spotify-on-repeat", strings.collectionRepeat, repeatTracks),
        Triple("spotify-saved-tracks", strings.favoritesPlain, saved)
    ).filter { it.third.isNotEmpty() }.forEach { (key, title, tracks) ->
        item(key = key, contentType = "home-spotify-shelf") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpotifySectionTitle(title, SpotifyDesign.gutter) {
                    LevyraSectionPlayAll(onClick = { onPlayAll(tracks) })
                }
                SpotifyTrackCarousel(tracks, state.currentTrack?.id, state.isPlaying, state.isResolving,
                    { track -> onPlayFrom(tracks, track) }, onActions)
            }
        }
    }
}
