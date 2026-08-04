package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRefreshStabilityTest {
    @Test
    fun sectionTitleIdentityPreservesNonLatinScripts() {
        assertNotEquals("section", HomeRefreshStability.sectionTitleIdentity("السعودية"))
        assertNotEquals(
            HomeRefreshStability.sectionTitleIdentity("中国"),
            HomeRefreshStability.sectionTitleIdentity("日本")
        )
        assertEquals("भारत", HomeRefreshStability.sectionTitleIdentity("भारत"))
        assertEquals("ประเทศไทย", HomeRefreshStability.sectionTitleIdentity("ประเทศไทย"))
    }

    @Test
    fun sectionTitleIdentityPreservesExistingLatinAccentFolding() {
        assertEquals("cafe-del-mar", HomeRefreshStability.sectionTitleIdentity("Café del Mar"))
    }

    @Test
    fun sectionIdentityIgnoresLocalizedDisplayTitle() {
        val english = section("Made for you", "stable")
        val italian = english.copy(title = "Creato per te")

        assertEquals(
            HomeRefreshStability.sectionIdentity(english),
            HomeRefreshStability.sectionIdentity(italian)
        )
    }

    @Test
    fun sanitizeSectionsKeepsDistinctNonLatinShelves() {
        val sections = listOf(
            section("موسيقى جديدة", "ar"),
            section("热门歌曲", "zh"),
            section("おすすめ", "ja"),
            section("인기 음악", "ko"),
            section("नया संगीत", "hi"),
            section("เพลงยอดนิยม", "th")
        )

        val sanitized = HomeRefreshStability.sanitizeSections(sections)

        assertEquals(sections.map { it.title }, sanitized.map { it.title })
    }

    @Test
    fun sectionTitleIdentityStillNormalizesWhitespaceAndPunctuation() {
        assertEquals(
            "موسيقى-جديدة",
            HomeRefreshStability.sectionTitleIdentity("  موسيقى جديدة!  ")
        )
    }

    @Test
    fun localizedTitleChangeDoesNotBecomeStructuralWhileFrozen() {
        val previous = section("Made for you", "stable")
        val incoming = previous.copy(title = "Creato per te")

        val result = HomeRefreshStability.mergeSections(
            previous = listOf(previous),
            incoming = listOf(incoming),
            allowStructuralChanges = false
        )

        assertEquals("Creato per te", result.visible.single().title)
        assertNull(result.deferredStructural)
        assertTrue(result.changed)
    }

    @Test
    fun partialTrackRefreshKeepsTheExistingSectionKey() {
        val previous = HomeSection(
            title = "Made for you",
            tracks = listOf(track("shared-1"), track("shared-2"), track("old-3"))
        )
        val incoming = HomeSection(
            title = "Creato per te",
            tracks = listOf(track("shared-1"), track("shared-2"), track("new-3"))
        )

        val result = HomeRefreshStability.mergeSections(
            previous = listOf(previous),
            incoming = listOf(incoming),
            allowStructuralChanges = false
        )

        assertEquals(listOf("shared-1", "shared-2", "new-3"), result.visible.single().tracks.map { it.id })
        assertNull(result.deferredStructural)
    }

    @Test
    fun sameTitleCompleteRefreshKeepsTheExistingSectionKey() {
        val previous = section("Made for you", "old")
        val incoming = section("Made for you", "new")

        val result = HomeRefreshStability.mergeSections(
            previous = listOf(previous),
            incoming = listOf(incoming),
            allowStructuralChanges = false
        )

        assertEquals(incoming.tracks.map { it.id }, result.visible.single().tracks.map { it.id })
        assertNull(result.deferredStructural)
    }

    @Test
    fun unrelatedSectionReplacementRemainsStructural() {
        val previous = section("Made for you", "old")
        val incoming = section("New shelf", "new")

        val result = HomeRefreshStability.mergeSections(
            previous = listOf(previous),
            incoming = listOf(incoming),
            allowStructuralChanges = false
        )

        assertEquals(previous, result.visible.single())
        assertEquals(listOf(incoming), result.deferredStructural)
    }

    private fun section(title: String, prefix: String): HomeSection = HomeSection(
        title = title,
        tracks = List(3) { index -> track("$prefix-$index") }
    )

    private fun track(id: String): Track = Track(
        id = id,
        title = "Track $id",
        artist = "Artist $id",
        album = "",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = setOf("music"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}
