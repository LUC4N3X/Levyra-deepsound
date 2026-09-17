package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private val EditorialRailWidth = 40.dp
private val EditorialArtworkCorner = 6.dp
private val EditorialScrollingStage = 280.dp
private val EditorialNumeralStyle = TextStyle(fontFeatureSettings = "tnum, lnum")

@Composable
internal fun PlayerEditorialDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    favoriteIds: Set<String>,
    queuePosition: PlayerDeckQueuePosition?,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    onArtistClick: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (scrollable) {
                        Modifier.height(EditorialScrollingStage)
                    } else {
                        Modifier.weight(1f)
                    }
                )
                .padding(
                    top = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd,
                    bottom = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceLg
                )
        ) {
            EditorialRail(
                queuePosition = queuePosition,
                surfaces = surfaces,
                accent = accent,
                modifier = Modifier.fillMaxHeight()
            )
            slots.stage(
                track,
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                EditorialArtworkCorner
            )
        }
        EditorialHeadline(
            track = track,
            surfaces = surfaces,
            accent = accent,
            favoriteIds = favoriteIds,
            animated = animated,
            compact = compact,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite
        )
        Box(
            modifier = Modifier
                .padding(vertical = if (compact) LevyraPlayerDesign.SpaceXs else LevyraPlayerDesign.SpaceSm)
                .fillMaxWidth()
                .height(LevyraPlayerDesign.Hairline)
                .background(surfaces.contentFaint.copy(alpha = 0.32f))
        )
        slots.controlsWithoutMetadata(this, track)
    }
}

@Composable
private fun EditorialRail(
    queuePosition: PlayerDeckQueuePosition?,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(EditorialRailWidth),
        horizontalAlignment = Alignment.Start
    ) {
        if (queuePosition != null) {
            Text(
                text = queuePosition.indexLabel,
                color = accent,
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.8).sp,
                style = EditorialNumeralStyle,
                maxLines = 1
            )
            Text(
                text = "/ ${queuePosition.totalLabel}",
                color = surfaces.contentFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                style = EditorialNumeralStyle,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceMd))
        }
        Box(
            modifier = Modifier
                .padding(start = LevyraPlayerDesign.SpaceXs)
                .weight(1f)
                .width(LevyraPlayerDesign.Hairline)
                .background(surfaces.contentFaint.copy(alpha = 0.36f))
        )
    }
}

@Composable
private fun EditorialHeadline(
    track: Track,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    favoriteIds: Set<String>,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        transitionSpec = { LevyraMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-editorial-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            val deckline = editorialDeckline(shown)
            if (deckline.isNotEmpty()) {
                Text(
                    text = deckline,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceXs))
            }
            val titleSize = editorialTitleSize(shown.title, compact)
            Text(
                text = shown.title,
                color = surfaces.content,
                fontSize = titleSize,
                lineHeight = titleSize * 1.02f,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                        .clip(LevyraPlayerDesign.ShapeXxs)
                        .clickable(onClickLabel = strings.openArtist) { onArtistClick(shown) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = shown.artist,
                        color = surfaces.contentMuted,
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PlayerFavoriteButton(
                    trackId = shown.id,
                    isFavorite = shown.id in favoriteIds,
                    surfaces = surfaces,
                    label = strings.favoritesPlain,
                    animated = animated,
                    onToggle = { onToggleFavorite(shown) }
                )
            }
        }
    }
}
