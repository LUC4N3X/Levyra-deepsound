package com.luc4n3x.levyra.data

import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesAllLanguagesPolicyTest {
    @Test
    fun everySupportedLanguageProducesLocalizedShortQueries() {
        val representativeTokens = mapOf(
            "it" to "italiana",
            "nl" to "Nederland",
            "pl" to "polsk",
            "el" to "ελλην",
            "uk" to "україн",
            "ar" to "عرب",
            "zh" to "华语",
            "hi" to "हिंदी",
            "fil" to "Pilipinas",
            "he" to "ישראל"
        )

        representativeTokens.forEach { (language, token) ->
            val queries = youtubeShortQueries(emptyList(), emptyList(), language)
            assertTrue("Missing local query for $language", queries.any { it.contains(token, ignoreCase = true) })
        }
    }

    @Test
    fun personalizedNewPipeQueriesNeverRemoveTheLocalMarket() {
        val queries = youtubeShortQueries(
            seeds = List(8) { index -> sample("Song $index", "Artist $index") },
            preferredArtists = List(8) { index -> "Followed $index" },
            languageCode = "it"
        )

        assertTrue(queries.any { it.contains("italiana", ignoreCase = true) })
        assertTrue(queries.any { it.contains("Followed 0", ignoreCase = true) })
    }

    private fun sample(title: String, artist: String) = com.luc4n3x.levyra.domain.Track(
        id = (title + artist).hashCode().toUInt().toString().padStart(11, '0').take(11),
        title = title,
        artist = artist,
        album = title,
        durationMs = 60_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://levyra.test/sample.jpg",
        largeThumbnailUrl = "https://levyra.test/sample-large.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0
    )
}
