package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private val PosterStageCorner = 0.dp
private val PosterFade = Brush.verticalGradient(
    0.00f to Color.Black,
    0.58f to Color.Black,
    0.86f to Color.Black.copy(alpha = 0.42f),
    1.00f to Color.Transparent
)

@Composable
internal fun PlayerPosterDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            slots.stage(
                track,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = PosterFade, blendMode = BlendMode.DstIn)
                    }
                    .graphicsLayer(),
                PosterStageCorner
            )
            slots.gestures(track, Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = gutter, end = gutter, top = LevyraPlayerDesign.SpaceXs)
            ) {
                slots.header()
            }
        }
        if (!scrollable) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = gutter,
                    end = gutter,
                    bottom = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
                )
        ) {
            PosterHeadline(
                linerNotes = slots.linerNotes,
                track = track,
                surfaces = surfaces,
                isFavorite = isFavorite,
                animated = animated,
                compact = compact,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                modifier = headlineModifier
            )
            slots.controlsWithoutMetadata(this, track)
        }
    }
}

@Composable
private fun PosterHeadline(
    linerNotes: PlayerLinerNotesState?,
    track: Track,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = { LevyraMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-poster-headline"
    ) { shown ->
        val isCurrent = shown.id == track.id
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleSize = if (compact) 26.sp else 32.sp
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.06f,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.0).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = surfaces.contentMuted,
                    style = TextStyle(
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.1).sp
                    ),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = isCurrent
                )
                PlayerLinerNotes(notes = linerNotes, track = shown, color = surfaces.contentMuted)
            }
            if (isCurrent) {
                PlayerFavoriteButton(
                    trackId = shown.id,
                    isFavorite = isFavorite,
                    surfaces = surfaces,
                    label = strings.favoritesPlain,
                    animated = animated,
                    onToggle = onToggleFavorite
                )
            } else {
                Box(modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget))
            }
        }
    }
}
