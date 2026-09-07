package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.ui.components.PlayerAccentColors
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerTransportControls
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Composable
internal fun PlayerTransportBar(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    accents: PlayerAccentColors,
    compact: Boolean,
    animated: Boolean,
    labels: PlayerControlLabels,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerTransportControls(
        isPlaying = isPlaying,
        isResolving = isResolving,
        shuffleOn = shuffleOn,
        repeatOn = repeatMode != RepeatMode.Off,
        repeatOne = repeatMode == RepeatMode.One,
        accents = accents,
        compact = compact,
        animated = animated,
        labels = labels,
        onShuffle = onShuffle,
        onPrevious = onPrevious,
        onToggle = onTogglePlay,
        onNext = onNext,
        onRepeat = onRepeat,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 0.dp else LevyraPlayerDesign.SpaceXs,
                vertical = if (compact) LevyraPlayerDesign.SpaceXxs else LevyraPlayerDesign.SpaceXs
            )
    )
}
