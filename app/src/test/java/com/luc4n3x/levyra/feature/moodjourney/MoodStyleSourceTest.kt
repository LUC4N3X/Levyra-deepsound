package com.luc4n3x.levyra.feature.moodjourney

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.MoodCategoryKind
import com.luc4n3x.levyra.domain.MoodStyle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.MoodStyleCopy
import com.luc4n3x.levyra.ui.i18n.moodStyleCopy
import com.luc4n3x.levyra.ui.i18n.moodStyleLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.moodStyleRawEntries
import com.luc4n3x.levyra.ui.i18n.smartMusicLocalizationCodes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodStyleSourceTest {
    private fun track(title: String, artist: String) = Track(
        id = "$artist-$title", title = title, artist = artist, album = "", durationMs = 200_000L,
        streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test",
        moodTags = emptySet(), energy = 50, vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0
    )

    private class Calls {
        var categories = 0
        val searched = mutableListOf<String>()
        val read = mutableListOf<String>()
        val appleRead = mutableListOf<String>()
        val appleGenres = mutableListOf<String>()
    }

    private fun source(
        calls: Calls,
        editorial: List<EditorialPlaylistRef>,
        apple: Map<String, List<EditorialPlaylistRef>> = emptyMap(),
        now: () -> Long = { 0L }
    ) = MoodStyleSource(
        MoodStyleGateway(
            applePlaylists = { genre -> calls.appleGenres += genre; apple[genre].orEmpty() },
            appleTracks = { id -> calls.appleRead += id; List(2) { track("$id apple $it", "$id apple artist") } },
            categories = { calls.categories++; listOf(ExploreCategory("Rock", "rock-params"), ExploreCategory("K-Pop", "kpop")) },
            playlists = { params -> if (params == "rock-params") editorial else emptyList() },
            searchPlaylists = { query -> calls.searched += query; listOf(EditorialPlaylistRef("search-$query", "$query mix")) },
            tracks = { id -> calls.read += id; List(3) { track("$id song $it", "$id artist") } }
        ),
        now
    )

    @Test
    fun genreCategoryIsPreferredAndPlaylistsSpreadAcrossMoods() = runBlocking {
        val editorial = listOf(
            EditorialPlaylistRef("w1", "Rock Workout"), EditorialPlaylistRef("w2", "Classic Rock Workout"),
            EditorialPlaylistRef("w3", "Running Rock"), EditorialPlaylistRef("c1", "Chill Rock"),
            EditorialPlaylistRef("s1", "Rock Ballads"), EditorialPlaylistRef("n1", "'90s Rock"),
            EditorialPlaylistRef("f1", "Feel-Good Rock Hits")
        )
        val calls = Calls()
        val shelf = source(calls, editorial).load(MoodStyle.ROCK)
        assertTrue(calls.searched.isEmpty())
        assertEquals(editorial.map { it.playlistId }.toSet(), calls.read.toSet())
        assertEquals(editorial.size, shelf.playlistsRead)
        assertEquals(MoodCategoryKind.CHILL, shelf.entries.first { it.track.title.startsWith("c1") }.mood)
        assertEquals(null, shelf.entries.first { it.track.title.startsWith("n1") }.mood)
    }

    @Test
    fun spreadTakesOneOfEachMoodBeforeRepeatingAndUnlabelledLast() {
        val picked = source(Calls(), emptyList()).spreadAcrossMoods(
            listOf(
                EditorialPlaylistRef("n1", "'90s Rock"), EditorialPlaylistRef("w1", "Rock Workout"),
                EditorialPlaylistRef("w2", "Cardio Rock"), EditorialPlaylistRef("c1", "Chill Rock"),
                EditorialPlaylistRef("", "Broken"), EditorialPlaylistRef("w1", "Rock Workout")
            )
        ).map { it.playlistId }
        assertEquals(listOf("w1", "c1", "w2", "n1"), picked)
    }

    @Test
    fun stylesWithoutGenreCategoryUseSearchAndResultsAreCached() = runBlocking {
        val calls = Calls()
        var now = 0L
        val source = source(calls, emptyList(), now = { now })
        val first = source.load(MoodStyle.DISCO)
        assertEquals(MoodStyle.DISCO.searchQueries, calls.searched)
        assertTrue(first.entries.isNotEmpty())
        source.load(MoodStyle.DISCO)
        assertEquals(1, calls.categories)
        now += MoodStyleSource.TTL_MS + 1
        source.load(MoodStyle.DISCO)
        assertEquals(2, calls.categories)
    }

    @Test
    fun evidencePropagatesStylesToTheSameArtist() {
        val shelf = MoodStyleShelf(MoodStyle.REGGAE, listOf(MoodStyleEntry(track("One Love", "Bob Marley, The Wailers"), MoodCategoryKind.FEEL_GOOD)), 1)
        val evidence = MoodStyleEvidence(listOf(shelf))
        assertEquals(setOf(MoodStyle.REGGAE), evidence.stylesFor(track("Jamming", "Bob Marley")))
        assertEquals(emptySet<MoodStyle>(), evidence.stylesFor(track("Song", "Someone Else")))
        assertEquals(setOf(MoodCategoryKind.FEEL_GOOD), evidence.labelsFor(track("One Love", "Bob Marley, The Wailers")))
    }

    @Test
    fun appleGenrePlaylistsComeFirstAndUseTheAppleCatalog() = runBlocking {
        val calls = Calls()
        val apple = mapOf("Metal" to emptyList(), "Hard Rock" to List(6) { EditorialPlaylistRef("pl.$it", if (it == 0) "Metal Workout" else "Hard Rock $it") })
        val shelf = source(calls, emptyList(), apple).load(MoodStyle.METAL)
        assertEquals(listOf("Metal", "Hard Rock"), calls.appleGenres)
        assertTrue(calls.searched.isEmpty())
        assertEquals((0 until 6).map { "pl.$it" }.toSet(), calls.appleRead.toSet())
        assertTrue(calls.read.isEmpty())
        assertEquals(MoodCategoryKind.WORKOUT, shelf.entries.first { it.track.title.startsWith("pl.0") }.mood)
    }

    @Test
    fun moodStyleCopyIsCompleteInEveryLanguage() {
        assertEquals(smartMusicLocalizationCodes(), moodStyleLocalizationCodes())
        val expected = MoodStyleCopy.FIXED_ENTRIES + MoodStyle.entries.size
        moodStyleLocalizationCodes().forEach { code ->
            val entries = moodStyleRawEntries(code)
            assertEquals(code, expected, entries.size)
            entries.forEachIndexed { index, value -> assertTrue("$code $index blank", value.isNotBlank()) }
            val point = moodStyleCopy(code).pointDescription
            listOf("{n}", "{e}", "{b}").forEach { assertTrue("$code point $it", point.contains(it)) }
        }
        assertEquals("Нью-эйдж", moodStyleCopy("ru").name(MoodStyle.NEW_AGE))
        assertEquals("Disco", moodStyleCopy("en").name(MoodStyle.DISCO))
        assertTrue(moodStyleCopy("pt-BR").title.isNotBlank())
    }
}
