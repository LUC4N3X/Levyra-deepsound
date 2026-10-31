package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private const val QobuzQuickPicksKey = "quick-picks"
private const val QobuzPageSize = 3
private const val QobuzRankedMax = 50
private const val QobuzRowMax = 20
private const val QobuzRowVisible = 4
private const val QobuzTwoRowThreshold = 6
private val QobuzQuoteCard = 288.dp

internal class QobuzComposedKit(
    private val copy: QobuzSkinCopy,
    private val data: AppleMusicHomeData,
    private val actions: AppleMusicHomeActions,
    private val onSeeAllTracks: (String, List<Track>) -> Unit
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
        val unique = remember(tracks) { tracks.distinctBy(Track::id) }
        if (unique.isEmpty()) return
        val paged = ranked || sectionKey == QobuzQuickPicksKey
        val hasMore = unique.size > if (paged) QobuzPageSize else QobuzRowVisible
        QobuzComposedSection {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QobuzSectionHeader(
                    title = title,
                    baseline = null,
                    seeAllLabel = copy.seeAll,
                    onSeeAll = { onSeeAllTracks(title, unique) }.takeIf { hasMore },
                    modifier = Modifier.weight(1f)
                )
                QobuzIconAction(Icons.Rounded.PlayArrow, LocalLevyraStrings.current.playAll, onPlayAll)
            }
            if (ranked && data.countryLabel.isNotBlank()) {
                Box(
                    Modifier.padding(horizontal = QobuzDesign.gutter).heightIn(min = 48.dp).clip(QobuzDesign.buttonShape)
                        .clickable(role = Role.Button, onClick = actions.pickCountry),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        data.countryLabel, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textAccent,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            if (paged) {
                QobuzComposedRankedPages(sectionKey, unique, ranked, currentId, onPlay, onTrackActions)
            } else {
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(unique.take(QobuzRowMax), key = { it.id }, contentType = { "qobuz-composed-track" }) { track ->
                        QobuzTrackCard(track, QobuzDesign.gridCard, { onPlay(track) }, { onTrackActions(track) })
                    }
                }
            }
        }
    }

    @Composable
    private fun QobuzComposedRankedPages(
        sectionKey: String,
        tracks: List<Track>,
        numbered: Boolean,
        currentId: String?,
        onPlay: (Track) -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val pageWidth = maxWidth - QobuzDesign.gutter * 2 - 40.dp
            val pages = remember(tracks) { tracks.take(QobuzRankedMax).withIndex().toList().chunked(QobuzPageSize) }
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pages, key = { page -> "$sectionKey-${page.first().value.id}" }, contentType = { "qobuz-composed-page" }) { page ->
                    Column(Modifier.width(pageWidth)) {
                        page.forEach { (index, track) ->
                            QobuzRankedRow(
                                if (numbered) index + 1 else null, track, copy, track.id == currentId,
                                { onPlay(track) }, { onTrackActions(track) }, Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Cards(title: String, cards: List<ComposedCard>) {
        val columns = remember(cards) {
            val unique = cards.distinctBy(ComposedCard::key)
            val twoRows = unique.size >= QobuzTwoRowThreshold && unique.none { it.wide || it.circular }
            if (twoRows) unique.chunked(2) else unique.map { listOf(it) }
        }
        if (columns.isEmpty()) return
        QobuzComposedSection {
            QobuzSectionHeader(title, null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            QobuzComposedCardRow(columns, reasons = false)
        }
    }

    @Composable
    private fun QobuzComposedCardRow(columns: List<List<ComposedCard>>, reasons: Boolean) {
        LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
            items(columns, key = { it.first().key }, contentType = { "qobuz-composed-card" }) { column ->
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    column.forEach { card -> QobuzComposedCardTile(card, reasons) }
                }
            }
        }
    }

    @Composable
    private fun QobuzComposedCardTile(card: ComposedCard, reason: Boolean) {
        val width = when {
            card.circular -> QobuzDesign.artistCircle
            card.wide -> QobuzDesign.wideCard
            else -> QobuzDesign.gridCard
        }
        Column(
            Modifier.width(width).combinedClickable(role = Role.Button, onLongClick = card.onLongClick, onClick = card.onClick),
            horizontalAlignment = if (card.circular) Alignment.CenterHorizontally else Alignment.Start
        ) {
            QobuzComposedCardArtwork(card)
            Spacer(Modifier.height(8.dp))
            Text(
                card.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = if (card.circular) TextAlign.Center else TextAlign.Start
            )
            if (card.subtitle.isNotBlank() || card.explicit) QobuzComposedCardSubtitle(card, reason)
        }
    }

    @Composable
    private fun QobuzComposedCardArtwork(card: ComposedCard) {
        val shape = if (card.circular) CircleShape else QobuzDesign.cardShape
        val artwork = Modifier.fillMaxWidth().aspectRatio(if (card.wide) 16f / 9f else 1f)
        val track = card.track
        if (track != null) {
            Box(artwork.clip(shape).background(QobuzDesign.placeholder)) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
            }
        } else {
            QobuzArtworkBox(card.artworkUrl, artwork, shape)
        }
    }

    @Composable
    private fun QobuzComposedCardSubtitle(card: ComposedCard, reason: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (card.explicit) {
                QobuzExplicitBadge(copy.explicit)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                card.subtitle, style = if (reason) QobuzText.category else QobuzText.cardSubtitle,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    override fun Chips(labels: List<String>, onSelect: (Int) -> Unit) {
        LazyRow(
            modifier = Modifier.padding(top = QobuzDesign.sectionGap),
            contentPadding = PaddingValues(horizontal = QobuzDesign.gutter, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(QobuzDesign.chipGap)
        ) {
            itemsIndexed(labels, key = { index, label -> "$index-$label" }, contentType = { _, _ -> "qobuz-composed-chip" }) { index, label ->
                QobuzChip(label, false, { onSelect(index) })
            }
        }
    }

    @Composable
    override fun Hero(hero: StudioHero) {
        QobuzComposedSection {
            QobuzSectionHeader(hero.label.ifBlank { copy.welcomeTitle }, null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            QobuzComposedSpotlightCard(hero)
        }
    }

    @Composable
    private fun QobuzComposedSpotlightCard(hero: StudioHero) {
        val shape = RoundedCornerShape(QobuzDesign.welcomeCorner)
        Row(
            Modifier.padding(horizontal = QobuzDesign.contentPadding).fillMaxWidth().clip(shape)
                .background(QobuzDesign.surfaceSecondary).border(1.dp, QobuzDesign.border, shape)
                .clickable(role = Role.Button, onClick = hero.onOpen).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                Text(
                    hero.track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = if (hero.isCurrent) QobuzDesign.textAccent else QobuzDesign.textPrimary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    hero.track.artist, style = MaterialTheme.typography.bodyLarge, color = QobuzDesign.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (hero.lead.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hero.lead, style = MaterialTheme.typography.bodyMedium, color = QobuzDesign.textTertiary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(14.dp))
                QobuzAvatarStack(hero, data.personalTracks, data.artists)
            }
            Box(Modifier.size(QobuzDesign.welcomeArtwork).clip(RoundedCornerShape(6.dp)).background(QobuzDesign.placeholder)) {
                CoverImage(hero.track, Modifier.fillMaxSize(), highRes = true)
                QobuzPlayCircle(copy.play, 48.dp, hero.onOpen, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy(ComposedQuote::key) }
        if (unique.isEmpty()) return
        QobuzComposedSection {
            QobuzSectionHeader(title, null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                items(unique, key = { it.key }, contentType = { "qobuz-composed-quote" }) { quote ->
                    QobuzComposedQuoteCard(quote)
                }
            }
        }
    }

    @Composable
    private fun QobuzComposedQuoteCard(quote: ComposedQuote) {
        val shape = RoundedCornerShape(QobuzDesign.welcomeCorner)
        Column(
            Modifier.width(QobuzQuoteCard).heightIn(min = 200.dp).clip(shape).background(QobuzDesign.surfaceSecondary)
                .border(1.dp, QobuzDesign.border, shape).clickable(role = Role.Button, onClick = quote.onOpen).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (quote.quote != quote.track.artist) {
                    Text(
                        quote.quote, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        color = QobuzDesign.textPrimary, maxLines = 4, overflow = TextOverflow.Ellipsis
                    )
                }
                if (quote.meta.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(quote.meta, style = QobuzText.category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(QobuzDesign.miniArtwork).clip(QobuzDesign.artworkShape).background(QobuzDesign.placeholder)) {
                    CoverImage(quote.track, Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(quote.track.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(quote.track.artist, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                QobuzPlayCircle(copy.play, 48.dp, quote.onPlay)
            }
        }
    }

    @Composable
    override fun Discovery(model: ComposedDiscovery) {
        val columns = remember(model.cards) { model.cards.distinctBy(ComposedCard::key).map { listOf(it) } }
        if (columns.isEmpty() && model.loading) {
            LoadingShelf(model.title)
            return
        }
        QobuzComposedSection {
            QobuzSectionHeader(model.title, model.lead, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            if (columns.isNotEmpty()) {
                QobuzComposedCardRow(columns, reasons = true)
                Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            }
            QobuzComposedAction(model.startLabel, primary = true, onClick = model.onOpen)
        }
    }

    @Composable
    override fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        QobuzComposedSection {
            Text(
                message, style = QobuzText.baseline, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = QobuzDesign.gutter)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                QobuzComposedAction(actionLabel, primary = false, onClick = onAction)
            }
        }
    }

    @Composable
    override fun LoadingShelf(title: String) {
        QobuzComposedSection { QobuzShelfSkeleton(title, "", rows = 1) }
    }
}

@Composable
private fun QobuzComposedSection(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = QobuzDesign.sectionGap), content = content)
}

@Composable
private fun QobuzComposedAction(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = QobuzDesign.gutter).heightIn(min = 48.dp).clip(QobuzDesign.buttonShape)
            .background(if (primary) QobuzDesign.buttonPrimary else QobuzDesign.surfaceSecondary)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            color = if (primary) QobuzDesign.buttonPrimaryContent else QobuzDesign.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
