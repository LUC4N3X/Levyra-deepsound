package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.atelier.AtelierAmbientBackdrop
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private const val AtelierQuickPicksKey = "quick-picks"
private const val AtelierOrbitKey = "orbit"
private const val AtelierForgottenKey = "forgotten"
private const val AtelierRankedMax = 50
private val AtelierWideSleeve = 264.dp
private val AtelierLinerNote = 288.dp
private val AtelierDiscoveryVinyl = 96.dp

internal class AtelierComposedKit(
    private val data: AppleMusicHomeData,
    private val actions: AppleMusicHomeActions,
    private val pairWidth: Dp
) : SkinHomeKit {

    @Composable
    override fun Songs(
        sectionKey: String,
        title: String,
        tracks: List<Track>,
        ranked: Boolean,
        currentId: String?,
        onPlay: (Track) -> Unit,
        onPlayAll: () -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        val strings = LocalLevyraStrings.current
        val unique = remember(tracks) { tracks.distinctBy(Track::id).take(AtelierRankedMax) }
        if (unique.isEmpty()) return
        Column(Modifier.fillMaxWidth()) {
            AtelierSectionLabel(title) {
                if (ranked && data.countryLabel.isNotBlank()) {
                    AtelierTextAction(data.countryLabel, actions.pickCountry, onClickLabel = strings.chartMarketChange)
                }
                AtelierIconAction(Icons.Rounded.PlayArrow, strings.playAll, onPlayAll, AtelierDesign.accent)
            }
            when {
                ranked -> AtelierComposedChart(sectionKey, unique, currentId, strings.songOptions, onPlay, onTrackActions)
                sectionKey == AtelierQuickPicksKey -> AtelierComposedPicks(sectionKey, unique, currentId, strings.songOptions, onPlay, onTrackActions)
                sectionKey == AtelierOrbitKey -> AtelierStandingShelf(unique, currentId, strings.songOptions, onPlay, onTrackActions)
                sectionKey == AtelierForgottenKey -> AtelierTrackStrip(unique, AtelierSmallSleeve, currentId, strings.songOptions, onPlay, onTrackActions)
                else -> AtelierTrackStrip(unique, AtelierDesign.albumCard, currentId, strings.songOptions, onPlay, onTrackActions)
            }
        }
    }

    @Composable
    private fun AtelierComposedChart(
        sectionKey: String,
        tracks: List<Track>,
        currentId: String?,
        actionLabel: String,
        onPlay: (Track) -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        var expanded by rememberSaveable(sectionKey) { mutableStateOf(false) }
        val visible = if (expanded) tracks else tracks.take(AtelierCollapsedCharts)
        visible.forEachIndexed { index, track ->
            key(track.id) {
                AtelierTrackLine(
                    track = track,
                    rank = index + 1,
                    isCurrent = track.id == currentId,
                    actionIcon = Icons.Rounded.MoreVert,
                    actionLabel = actionLabel,
                    onPlay = { onPlay(track) },
                    onAction = { onTrackActions(track) }
                )
            }
        }
        AtelierComposedToggle(tracks.size > AtelierCollapsedCharts, expanded) { expanded = !expanded }
    }

    @Composable
    private fun AtelierComposedPicks(
        sectionKey: String,
        tracks: List<Track>,
        currentId: String?,
        actionLabel: String,
        onPlay: (Track) -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        var expanded by rememberSaveable(sectionKey) { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 4.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tracks.take(AtelierPickPairCount).forEachIndexed { index, track ->
                AtelierPickSleeve(
                    track = track,
                    width = pairWidth,
                    isCurrent = track.id == currentId,
                    actionsLabel = actionLabel,
                    modifier = if (index == 0) Modifier else Modifier.padding(top = 40.dp),
                    onPlay = { onPlay(track) },
                    onActions = { onTrackActions(track) }
                )
            }
        }
        val compact = tracks.drop(AtelierPickPairCount)
        val visible = if (expanded) compact else compact.take(AtelierCollapsedPicks - AtelierPickPairCount)
        visible.forEach { track ->
            key(track.id) {
                AtelierCompactRow(
                    track = track,
                    isCurrent = track.id == currentId,
                    actionsLabel = actionLabel,
                    onPlay = { onPlay(track) },
                    onActions = { onTrackActions(track) }
                )
            }
        }
        AtelierComposedToggle(tracks.size > AtelierCollapsedPicks, expanded) { expanded = !expanded }
    }

    @Composable
    private fun AtelierComposedToggle(visible: Boolean, expanded: Boolean, onToggle: () -> Unit) {
        if (!visible) return
        val strings = LocalLevyraStrings.current
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            AtelierTextAction(if (expanded) strings.showLess else strings.showMore, onToggle)
        }
    }

    @Composable
    override fun Cards(title: String, cards: List<ComposedCard>) {
        val unique = remember(cards) { cards.distinctBy(ComposedCard::key) }
        if (unique.isEmpty()) return
        Column(Modifier.fillMaxWidth()) {
            AtelierSectionLabel(title)
            AtelierComposedCardRow(unique)
        }
    }

    @Composable
    private fun AtelierComposedCardRow(cards: List<ComposedCard>) {
        LazyRow(
            contentPadding = AtelierRowPadding,
            horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap),
            verticalAlignment = Alignment.Top
        ) {
            itemsIndexed(cards, key = { _, card -> card.key }, contentType = { _, card -> if (card.circular) "atelier-composed-artist" else "atelier-composed-sleeve" }) { index, card ->
                if (card.circular) {
                    AtelierComposedPortrait(card, AtelierArtistSizes[index % AtelierArtistSizes.size])
                } else {
                    AtelierComposedSleeve(card)
                }
            }
        }
    }

    @Composable
    private fun AtelierComposedSleeve(card: ComposedCard) {
        val strings = LocalLevyraStrings.current
        val width = if (card.wide) AtelierWideSleeve else AtelierDesign.albumCard
        Column(
            Modifier.width(width).levyraPressable(
                onClick = card.onClick,
                role = Role.Button,
                pressedScale = LevyraPressScale.Tile,
                onLongClick = card.onLongClick,
                onLongClickLabel = card.onLongClick?.let { strings.songOptions }
            )
        ) {
            AtelierSleeveFrame(Modifier.fillMaxWidth().aspectRatio(if (card.wide) 16f / 9f else 1f)) {
                AtelierComposedArtwork(card)
            }
            AtelierTileText(card.title, card.subtitle, highlighted = false)
        }
    }

    @Composable
    private fun AtelierComposedPortrait(card: ComposedCard, portrait: Dp) {
        Column(
            Modifier.width(maxOf(portrait, 88.dp)).levyraPressable(
                onClick = card.onClick,
                role = Role.Button,
                pressedScale = LevyraPressScale.Tile
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height((AtelierArtistMax - portrait) / 2))
            Box(
                Modifier.size(portrait).clip(CircleShape).background(AtelierDesign.panel)
                    .border(1.dp, AtelierDesign.hairlineStrong, CircleShape)
            ) { AtelierComposedArtwork(card) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = AtelierDesign.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun AtelierComposedArtwork(card: ComposedCard) {
        val track = card.track
        if (track != null) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        } else {
            AtelierArtwork(card.artworkUrl)
        }
    }

    @Composable
    override fun Chips(labels: List<String>, onSelect: (Int) -> Unit) {
        LazyRow(
            modifier = Modifier.padding(top = AtelierDesign.sectionGap),
            contentPadding = AtelierRowPadding,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(labels, key = { index, label -> "$index-$label" }, contentType = { _, _ -> "atelier-composed-key" }) { index, label ->
                AtelierHardwareKey(label = label, lit = false, onClick = { onSelect(index) }) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = AtelierDesign.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    override fun Hero(hero: StudioHero) {
        val track = hero.track
        val strings = LocalLevyraStrings.current
        val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
        val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
        val cover = remember(track.largeThumbnailUrl, track.thumbnailUrl) { atelierCoverUrl(track) }
        val active = hero.isCurrent && hero.isPlaying
        val spotlightTitle = hero.label.ifBlank { copy.featureTitle }
        Box(Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap).clipToBounds()) {
            AtelierAmbientBackdrop(cover, Modifier.matchParentSize(), strength = 1.15f)
            Box(
                Modifier.matchParentSize().drawWithCache {
                    val fade = Brush.verticalGradient(
                        0f to AtelierDesign.room,
                        0.2f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to AtelierDesign.room
                    )
                    onDrawBehind { drawRect(fade) }
                }
            )
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = atelierUppercase(spotlightTitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = AtelierDesign.accentBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {
                            heading()
                            contentDescription = spotlightTitle
                        }
                    )
                    Text(
                        text = copy.sideA,
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierDesign.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 12.dp).clearAndSetSemantics {}
                    )
                }
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    val sleeve = (maxWidth * 0.52f).coerceAtMost(280.dp)
                    Box(Modifier.fillMaxWidth().height(sleeve).padding(start = AtelierDesign.gutter)) {
                        AtelierVinyl(
                            track,
                            Modifier.align(Alignment.CenterStart).offset(x = sleeve * 0.32f).size(sleeve * 0.94f)
                        )
                        AtelierPerspectiveSleeve(
                            track = track,
                            side = sleeve,
                            turn = -9f * direction,
                            elevation = 28.dp,
                            highRes = true,
                            modifier = Modifier.align(Alignment.CenterStart).levyraPressable(
                                onClick = hero.onOpen,
                                role = Role.Button,
                                onClickLabel = strings.play,
                                pressedScale = LevyraPressScale.Surface
                            )
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 18.dp)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                        color = if (hero.isCurrent) AtelierDesign.accentBright else AtelierDesign.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        color = AtelierDesign.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (hero.lead.isNotBlank()) {
                        Text(
                            text = hero.lead,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtelierDesign.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    AtelierHardwareKey(
                        label = if (active) strings.pause else strings.play,
                        lit = active,
                        busy = hero.isCurrent && hero.isResolving,
                        onClick = hero.onOpen,
                        modifier = Modifier.padding(top = 16.dp).width(92.dp).height(58.dp)
                    ) { AtelierPlayGlyph(active, 28.dp) }
                }
            }
        }
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy(ComposedQuote::key) }
        if (unique.isEmpty()) return
        Column(Modifier.fillMaxWidth()) {
            AtelierSectionLabel(title)
            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(unique, key = { it.key }, contentType = { "atelier-composed-liner-note" }) { quote ->
                    AtelierComposedLinerNote(quote)
                }
            }
        }
    }

    @Composable
    private fun AtelierComposedLinerNote(quote: ComposedQuote) {
        val strings = LocalLevyraStrings.current
        Column(
            Modifier.width(AtelierLinerNote).heightIn(min = 200.dp)
                .levyraPressable(onClick = quote.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
                .clip(AtelierDesign.panelShape)
                .background(Brush.verticalGradient(listOf(AtelierDesign.panelRaised, AtelierDesign.cabinet)))
                .border(1.dp, AtelierDesign.hairline, AtelierDesign.panelShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(Modifier.width(28.dp).height(2.dp).background(AtelierDesign.accent))
                Spacer(Modifier.height(12.dp))
                if (quote.quote != quote.track.artist) {
                    Text(
                        text = quote.quote,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
                        color = AtelierDesign.text,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (quote.meta.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = atelierUppercase(quote.meta),
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierDesign.textFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AtelierSleeveFrame(Modifier.size(AtelierDesign.rowArtwork)) {
                    CoverImage(quote.track, Modifier.fillMaxSize().clearAndSetSemantics {})
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = quote.track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = AtelierDesign.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = quote.track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierDesign.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AtelierHardwareKey(
                    label = strings.play,
                    lit = false,
                    onClick = quote.onPlay,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { AtelierPlayGlyph(false, 22.dp) }
            }
        }
    }

    @Composable
    override fun Discovery(model: ComposedDiscovery) {
        val unique = remember(model.cards) { model.cards.distinctBy(ComposedCard::key) }
        Column(Modifier.fillMaxWidth()) {
            AtelierSectionLabel(model.title)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)
                    .clip(AtelierDesign.panelShape)
                    .background(Brush.verticalGradient(listOf(AtelierDesign.panelRaised, AtelierDesign.cabinet)))
                    .border(1.dp, AtelierDesign.hairline, AtelierDesign.panelShape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtelierVinyl(unique.firstOrNull()?.track, Modifier.size(AtelierDiscoveryVinyl))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = model.lead,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                        color = AtelierDesign.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    AtelierHardwareKey(label = model.startLabel, lit = false, onClick = model.onOpen) {
                        Text(
                            text = model.startLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = AtelierDesign.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            when {
                unique.isNotEmpty() -> {
                    Spacer(Modifier.height(AtelierDesign.headerToContent))
                    AtelierComposedCardRow(unique)
                }
                model.loading -> AtelierShelfSkeleton()
            }
        }
    }

    @Composable
    override fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = AtelierDesign.sectionGap)) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                color = AtelierDesign.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(8.dp))
                AtelierTextAction(actionLabel, onAction)
            }
        }
    }

    @Composable
    override fun LoadingShelf(title: String) {
        Column(Modifier.fillMaxWidth()) {
            AtelierSectionLabel(title)
            Row(
                Modifier.fillMaxWidth().padding(AtelierRowPadding),
                horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
            ) {
                repeat(3) {
                    Box(Modifier.size(AtelierDesign.albumCard).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel))
                }
            }
        }
    }
}
