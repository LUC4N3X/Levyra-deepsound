package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMusicVideoFilterTest {

    private fun video(title: String, artist: String) = Track(
        id = "$title|$artist", title = title, artist = artist, album = "", durationMs = 200_000L, streamUrl = "",
        videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test", moodTags = emptySet(),
        energy = 50, vocal = 50, replayScore = 50, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    @Test
    fun `italian listener drops videos clearly in another language or script`() {
        val videos = listOf(
            video("Escuta os Conselhos dos Cotas Que Vais Longe | Kizomba 2026", "Luis Nildo"),
            video("Piquito", "LOS DOS DE TAMAULIPAS e Grupo Frontera"),
            video("メタセコイア - Metasequoia", "blend house e sucola"),
            video("Japan", "Fontaines D.C."),
            video("Kekkina - Pulecenè", "Kekkina"),
            video("Los Angeles Nights", "Some Band")
        )

        val kept = LocalMusicVideoFilter.localFirst(videos, "it").map { it.title }

        assertEquals(setOf("Japan", "Kekkina - Pulecenè", "Los Angeles Nights"), kept.toSet())
    }

    @Test
    fun `a japanese listener keeps japanese script`() {
        val kept = LocalMusicVideoFilter.localFirst(listOf(video("メタセコイア - Metasequoia", "blend house e sucola")), "ja")

        assertEquals(1, kept.size)
    }
}
