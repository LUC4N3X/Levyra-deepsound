package com.luc4n3x.levyra.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.dailyDiscoveryCards
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.HomeStyleSpec
import com.luc4n3x.levyra.ui.StudioSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import com.luc4n3x.levyra.viewmodel.DiscoveryUiState

@Composable
internal fun DailyDiscoveryShelf(
    ui: DiscoveryUiState,
    style: HomeStyle,
    spec: HomeStyleSpec,
    copy: DiscoveryCopy,
    handle: DiscoveryHomeHandle
) {
    val hidden = remember(ui.deck.cards) { handle.homeTrackKeys() + ui.teaser.map { it.key } }
    val familiar = remember(ui.deck.cards) { handle.familiarArtistKeys() }
    val cards = remember(ui.deck.cards, hidden, familiar) { dailyDiscoveryCards(ui.deck.cards, hidden, familiar) }
    if (cards.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = spec.sectionGap), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(horizontal = spec.inset)) {
            StudioSectionHeader(style, spec, copy.dailyTitle, false, onPlayAll = { handle.onPlayDaily(cards, cards.first()) })
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = spec.inset),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(cards, key = { _, card -> "daily-${card.key}" }) { index, card ->
                DailyDiscoveryCard(card, dailyDiscoveryReason(card, copy, index), spec) { handle.onPlayDaily(cards, card) }
            }
        }
    }
}

internal fun dailyDiscoveryReason(card: DiscoveryCard, copy: DiscoveryCopy, index: Int): String {
    val artist = primaryArtistName(card.reason.seedArtist)
    return if (index % 3 == 2) copy.reasonForFansOf(artist) else copy.reasonSimilarTo(card.reason.seedTitle, artist)
}

private fun primaryArtistName(artist: String): String =
    artist.split(PrimaryArtistSeparators).first().trim().ifBlank { artist.trim() }

@Composable
private fun DailyDiscoveryCard(card: DiscoveryCard, reason: String, spec: HomeStyleSpec, onPlay: () -> Unit) {
    val shape = RoundedCornerShape(spec.artworkCorner)
    Box(
        Modifier
            .size(DailyCardSize)
            .clip(shape)
            .levyraPressable(onClick = onPlay)
            .semantics { contentDescription = "${card.track.title}, ${card.track.artist}. $reason" }
    ) {
        CoverImage(track = card.track, modifier = Modifier.fillMaxSize(), highRes = true)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to DailyScrim, 0.32f to Color.Transparent, 0.62f to Color.Transparent, 1f to DailyScrim)))
        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
            Text(card.track.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.track.artist, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            reason, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
    }
}

private val PrimaryArtistSeparators = Regex("""\s*(?:,|&|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""", RegexOption.IGNORE_CASE)
private val DailyCardSize = 264.dp
private val DailyScrim = Color.Black.copy(alpha = 0.62f)
