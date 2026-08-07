package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.AlbumHit
import org.junit.Assert.assertEquals
import org.junit.Test

class NewReleasesPersonalizationTest {
    @Test
    fun userArtistsMoveUpWithoutDestroyingEditorialOrder() {
        val releases = listOf(
            album("Editoriale 1", "Altro"),
            album("Preferita 1", "Lazza"),
            album("Editoriale 2", "Altro 2"),
            album("Preferita 2", "Lazza")
        )

        val ranked = prioritizeNewReleasesForUser(releases, listOf("Lazza"), 10)

        assertEquals(
            listOf("Preferita 1", "Preferita 2", "Editoriale 1", "Editoriale 2"),
            ranked.map { it.title }
        )
    }

    @Test
    fun emptyPreferencesKeepSpotifyEditorialOrder() {
        val releases = listOf(album("Uno", "A"), album("Due", "B"))

        assertEquals(releases, prioritizeNewReleasesForUser(releases, emptyList(), 10))
    }

    private fun album(title: String, artist: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://levyra.test/$title.jpg",
        query = "$artist $title",
        browseId = "MPRE${title.hashCode().toUInt()}"
    )
}
