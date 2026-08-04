package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeRefreshStabilityTest {
    @Test
    fun sectionIdentityPreservesNonLatinScripts() {
        assertNotEquals("section", HomeRefreshStability.sectionIdentity("السعودية"))
        assertNotEquals(
            HomeRefreshStability.sectionIdentity("中国"),
            HomeRefreshStability.sectionIdentity("日本")
        )
    }

    @Test
    fun sanitizeSectionsKeepsDistinctNonLatinShelves() {
        val sections = listOf(
            section("موسيقى جديدة", "ar"),
            section("热门歌曲", "zh"),
            section("おすすめ", "ja"),
            section("인기 음악", "ko")
        )

        val sanitized = HomeRefreshStability.sanitizeSections(sections)

        assertEquals(sections.map { it.title }, sanitized.map { it.title })
    }

    @Test
    fun sectionIdentityStillNormalizesWhitespaceAndPunctuation() {
        assertEquals(
            "موسيقى-جديدة",
            HomeRefreshStability.sectionIdentity("  موسيقى جديدة!  ")
        )
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
