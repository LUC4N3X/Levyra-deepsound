package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayableMatcherTest {

    private val reference = Track(
        id = "chart-1",
        title = "Notte Fonda",
        artist = "Artista Uno",
        videoUrl = ""
    )

    private fun candidate(id: String, title: String, artist: String, durationMs: Long = 200_000L) = Track(
        id = id,
        title = title,
        artist = artist,
        videoUrl = "https://www.youtube.com/watch?v=$id",
        durationMs = durationMs
    )

    @Test
    fun `exact title and artist wins`() {
        val expected = candidate("a", "Notte Fonda", "Artista Uno")
        val best = PlayableMatcher.best(
            reference,
            listOf(candidate("b", "Notte Fonda Remix", "Altro"), expected)
        )
        assertEquals(expected, best)
    }

    @Test
    fun `official video suffix still matches`() {
        val best = PlayableMatcher.best(
            reference,
            listOf(candidate("a", "Notte Fonda (Official Video)", "Artista Uno"))
        )
        assertEquals("a", best?.id)
    }

    @Test
    fun `unrelated candidates are rejected`() {
        val best = PlayableMatcher.best(
            reference,
            listOf(candidate("a", "Tutt altro brano", "Sconosciuto"))
        )
        assertNull(best)
    }

    @Test
    fun `candidates without playback url are ignored`() {
        val best = PlayableMatcher.best(
            reference,
            listOf(reference.copy(id = "other"))
        )
        assertNull(best)
    }

    @Test
    fun `very long uploads are penalised`() {
        val short = candidate("a", "Notte Fonda", "Artista Uno", durationMs = 210_000L)
        val long = candidate("b", "Notte Fonda", "Artista Uno", durationMs = 3_600_000L)
        assertTrue(PlayableMatcher.score(reference, short) > PlayableMatcher.score(reference, long))
        assertEquals(short, PlayableMatcher.best(reference, listOf(long, short)))
    }
}
