package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

@Composable
internal fun SpotifyExploreSamples(state: LevyraUiState, viewModel: ExploreViewModel) {
    if (state.exploreSamples.isEmpty()) return
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            LevyraSpotifySkinStrings.forCode(strings.code).discoverNew,
            color = SpotifyDesign.text,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = SpotifyDesign.gutter).semantics { heading() }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = SpotifyDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(state.exploreSamples.take(12), key = { _, track -> track.id }) { index, track ->
                val tag = spotifyShortTag(track, LevyraSpotifySkinStrings.forCode(strings.code).music)
                SpotifyShortCard(tag, track, preview = index == 0 && !state.isVideoMode && !state.isResolving) {
                    viewModel.playSample(state.exploreSamples, track)
                    viewModel.openExploreSamples(index)
                }
            }
        }
    }
}
