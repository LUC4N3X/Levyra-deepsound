package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.SponsorSegment
import com.luc4n3x.levyra.feature.radio.MemoryRadioPreferences
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.discoveryCopyLanguages
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverySessionTest {
    private val cards = (1..5).map { DiscoveryCard(track("Song $it", "Artist $it"), DiscoveryReason(DiscoveryReasonKind.SimilarToTrack, "Seed")) }

    @Test
    fun saveDismissAndUndoRestoreTheExactCardAndDecision() {
        val deck = DiscoveryDeck(cards)
            .decide(DiscoveryDecision.Saved, "levyra-discovery-2026-w43", "yt-1")
            .decide(DiscoveryDecision.Dismissed)
        assertEquals("Song 3", deck.current?.track?.title)
        val (afterUndo, step) = deck.undo()!!
        assertEquals(DiscoveryDecision.Dismissed, step.decision)
        assertEquals("Song 2", afterUndo.current?.track?.title)
        val (twice, saved) = afterUndo.undo()!!
        assertEquals(DiscoveryDecision.Saved, saved.decision)
        assertEquals("yt-1", saved.savedTrackId)
        assertEquals("levyra-discovery-2026-w43", saved.playlistId)
        assertEquals("Song 1", twice.current?.track?.title)
        assertNull(twice.undo())
    }

    @Test
    fun appendingSkipsCardsAlreadyInTheDeck() {
        val deck = DiscoveryDeck(cards.take(3)).appended(cards.drop(1))
        assertEquals(5, deck.cards.size)
        assertEquals(cards.map { it.key }, deck.cards.map { it.key })
        assertEquals(3, deck.upcoming.size)
    }

    @Test
    fun historyIsBounded() {
        var deck = DiscoveryDeck((1..60).map { DiscoveryCard(track("T$it", "A$it"), DiscoveryReason(DiscoveryReasonKind.Trending)) })
        repeat(50) { deck = deck.decide(DiscoveryDecision.Dismissed) }
        assertEquals(DiscoveryDeck.MAX_HISTORY, deck.history.size)
    }

    @Test
    fun weeklyPlaylistIdIsStableWithinAnIsoWeekAndChangesNextWeek() {
        val monday = DiscoveryWeek.of(LocalDate.of(2026, 10, 19))
        val sunday = DiscoveryWeek.of(LocalDate.of(2026, 10, 25))
        val next = DiscoveryWeek.of(LocalDate.of(2026, 10, 26))
        assertEquals(monday.id, sunday.id)
        assertEquals(LocalDate.of(2026, 10, 19), sunday.monday)
        assertNotEquals(monday.id, next.id)
        assertEquals("levyra-discovery-2026-w01", DiscoveryWeek.of(LocalDate.of(2025, 12, 29)).id)
    }

    @Test
    fun interactionsPersistAcrossStoreInstancesAndUndoReverts() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val store = DiscoveryInteractionStore(preferences)
        store.record(cards[0], DiscoveryDecision.Dismissed, 10L)
        store.record(cards[1], DiscoveryDecision.Saved, 20L)
        val reloaded = DiscoveryInteractionStore(preferences).load()
        assertTrue(reloaded.seen(cards[0].key))
        assertTrue(reloaded.seen(cards[1].key))
        assertEquals(1, reloaded.artistDismissals["artist 1"])
        val reverted = DiscoveryInteractionStore(preferences).revert(DiscoveryStep(cards[0], DiscoveryDecision.Dismissed))
        assertFalse(reverted.seen(cards[0].key))
        assertNull(reverted.artistDismissals["artist 1"])
    }

    @Test
    fun interactionCodecIsBoundedAndToleratesCorruptData() {
        val many = DiscoveryInteractions(dismissed = (1..900).associate { "k$it|a" to it.toLong() })
        val decoded = DiscoveryInteractionCodec.decode(DiscoveryInteractionCodec.encode(many))
        assertEquals(DiscoveryInteractionCodec.MAX_DISMISSED, decoded.dismissed.size)
        assertTrue("k900|a" in decoded.dismissed)
        assertFalse("k1|a" in decoded.dismissed)
        assertEquals(DiscoveryInteractions(), DiscoveryInteractionCodec.decode("{not json"))
        assertEquals(DiscoveryInteractions(), DiscoveryInteractionCodec.decode(null))
    }

    @Test
    fun previewWindowIsThirtySecondsAndUsesOnlyReliableHighlights() {
        val plain = track("A", "B", durationMs = 200_000L)
        assertEquals(DiscoveryPreviewWindow(0L, 30_000L), DiscoveryPreviewWindows.of(plain))
        val highlighted = plain.copy(sponsorSegments = listOf(SponsorSegment(72_000L, 72_000L, "poi_highlight")))
        assertEquals(DiscoveryPreviewWindow(72_000L, 102_000L), DiscoveryPreviewWindows.of(highlighted))
        val intro = plain.copy(sponsorSegments = listOf(SponsorSegment(0L, 14_000L, "music_offtopic")))
        assertEquals(14_000L, DiscoveryPreviewWindows.of(intro).startMs)
        val lateHighlight = plain.copy(sponsorSegments = listOf(SponsorSegment(195_000L, 195_000L, "poi_highlight")))
        assertEquals(170_000L, DiscoveryPreviewWindows.of(lateHighlight).startMs)
        val short = track("Short", "B", durationMs = 35_000L)
        assertEquals(DiscoveryPreviewWindow(0L, 35_000L), DiscoveryPreviewWindows.of(short))
    }

    @Test
    fun discoverySitsDirectlyBelowMySoundtrackInEveryDefaultHomeStyle() {
        HomeStyle.entries.forEach { style ->
            val order = LevyraHomeStudioSettings.defaultLayout(style).order
            assertTrue("$style misses discovery", HomeSectionId.Discovery in order)
            when (style) {
                HomeStyle.MaterialExpressive -> assertEquals(HomeSectionId.Discovery, order.first())
                HomeStyle.Minimal -> assertEquals(HomeSectionId.QuickPicks, order[order.indexOf(HomeSectionId.Discovery) - 1])
                else -> assertEquals("$style", HomeSectionId.Spotlight, order[order.indexOf(HomeSectionId.Discovery) - 1])
            }
        }
    }

    @Test
    fun savedLayoutsGainDiscoveryWithoutLosingCustomOrderAndCanHideIt() {
        HomeStyle.entries.forEach { style ->
            val custom = LevyraHomeStudioSettings(style = style).updateLayout(style) { layout ->
                layout.copy(order = layout.order.filterNot { it == HomeSectionId.Discovery }.reversed())
            }
            val legacy = custom.encode().replace("\"discovery\",", "").replace(",\"discovery\"", "")
            val upgraded = LevyraHomeStudioSettings.decode(legacy).activeLayout().order
            assertTrue(HomeSectionId.Discovery in upgraded)
            assertEquals(custom.activeLayout().order.filterNot { it == HomeSectionId.Discovery }, upgraded.filterNot { it == HomeSectionId.Discovery })
            val hidden = LevyraHomeStudioSettings(style = style).updateLayout(style) { it.withVisibility(HomeSectionId.Discovery, false) }
            assertTrue(LevyraHomeStudioSettings.decode(hidden.encode()).activeLayout().isHidden(HomeSectionId.Discovery))
        }
    }

    @Test
    fun defaultOriginalLayoutStillUsesTheClassicHome() {
        assertTrue(LevyraHomeStudioSettings().usesClassicOriginalHome())
        assertTrue(LevyraHomeStudioSettings.decode(LevyraHomeStudioSettings().encode()).usesClassicOriginalHome())
    }

    @Test
    fun everySupportedLanguageHasCompleteDiscoveryCopyWithPlaceholders() {
        assertEquals(LevyraLanguageCatalog.languages.map { it.code }.toSet(), discoveryCopyLanguages())
        LevyraLanguageCatalog.languages.forEach { language ->
            val copy = discoveryCopy(language.code)
            assertTrue(language.code, copy.reasonBecause("XYZ").contains("XYZ"))
            val description = copy.cardDescription("TTT", "AAA")
            assertTrue(language.code, description.contains("TTT") && description.contains("AAA"))
            assertFalse(language.code, copy.reasonBecause("XYZ").contains("{track}"))
        }
        assertEquals("Scopri per te", discoveryCopy("it").title)
        assertEquals("Scoperte della settimana", discoveryCopy("it").weeklyPlaylist)
    }
}
