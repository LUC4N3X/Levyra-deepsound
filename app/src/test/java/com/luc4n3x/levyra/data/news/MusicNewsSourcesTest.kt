package com.luc4n3x.levyra.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNewsSourcesTest {
    @Test
    fun everyFeedIsHttpsAndEveryLanguageHasFeeds() {
        MusicNewsSources.languages.forEach { language ->
            val sources = MusicNewsSources.forLanguage(language)
            assertTrue(language, sources.isNotEmpty())
            sources.forEach { assertTrue(it.url, it.url.startsWith("https://")) }
            assertEquals(language, sources.size, sources.map { it.url }.distinct().size)
        }
    }

    @Test
    fun unknownLanguageFallsBackToEnglish() {
        assertEquals(MusicNewsSources.forLanguage("en"), MusicNewsSources.forLanguage("ja"))
        assertEquals(MusicNewsSources.forLanguage("en"), MusicNewsSources.forLanguage(""))
        assertEquals("en", MusicNewsSources.key("pt-BR"))
    }

    @Test
    fun regionalCodesUseTheirLanguage() {
        assertEquals("it", MusicNewsSources.key("it-IT"))
        assertEquals("ru", MusicNewsSources.key("RU"))
    }

    @Test
    fun nonEnglishListsIncludeAnImageRichFeed() {
        MusicNewsSources.languages.filter { it != "en" }.forEach { language ->
            assertTrue(language, MusicNewsSources.forLanguage(language).any { it.url.startsWith("https://pitchfork.com/") })
        }
    }

    @Test
    fun mergeDedupesByUrlAndTitleThenSortsNewestFirst() {
        val merged = MusicNewsRepository.merge(
            listOf(
                MusicNewsStory("https://a.example/1", "New album out", "A", "", 10L),
                MusicNewsStory("https://a.example/1", "Duplicate url", "A", "", 50L),
                MusicNewsStory("https://b.example/9", "New Album: Out!", "B", "", 40L),
                MusicNewsStory("https://b.example/2", "Tour dates", "B", "", 30L),
                MusicNewsStory("https://b.example/3", "Undated", "B", "", 0L)
            )
        )
        assertEquals(listOf("https://b.example/2", "https://a.example/1", "https://b.example/3"), merged.map { it.url })
    }
}
