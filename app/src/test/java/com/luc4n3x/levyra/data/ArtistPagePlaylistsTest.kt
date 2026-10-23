package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaylistHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPagePlaylistsTest {

    private fun playlist(id: String, author: String) = PlaylistHit(title = "Playlist $id", author = author, thumbnailUrl = "", playlistId = id)

    @Test
    fun officialPlaylistsAreSeparatedFromPlaylistsThatFeatureTheArtist() {
        val playlists = listOf(
            playlist("RDCLAK_featured", "YouTube Music"),
            playlist("PL_official", "Dua Lipa"),
            playlist("PL_fan", "Some Listener")
        )

        val split = splitArtistPagePlaylists(playlists, "Dua Lipa", limit = 12)

        assertEquals(listOf("PL_official"), split.byArtist.map { it.playlistId })
        assertEquals(listOf("RDCLAK_featured", "PL_fan"), split.featuredOn.map { it.playlistId })
    }

    @Test
    fun featuredShelfNoLongerCrowdsOutTheArtistsOwnPlaylists() {
        val featured = (1..10).map { playlist("RDCLAK_$it", "YouTube Music") }
        val official = (1..10).map { playlist("PL_$it", "Dua Lipa") }

        val split = splitArtistPagePlaylists(featured + official, "Dua Lipa", limit = 12)

        assertEquals(10, split.byArtist.size)
        assertEquals(10, split.featuredOn.size)
    }

    @Test
    fun unknownAuthorOrArtistIsNeverTreatedAsOfficial() {
        val split = splitArtistPagePlaylists(listOf(playlist("PL_blank", "")), "Dua Lipa", limit = 12)
        val unnamed = splitArtistPagePlaylists(listOf(playlist("PL_named", "Dua Lipa")), "", limit = 12)

        assertTrue(split.byArtist.isEmpty())
        assertTrue(unnamed.byArtist.isEmpty())
    }

    @Test
    fun eachShelfRespectsItsOwnLimit() {
        val split = splitArtistPagePlaylists((1..20).map { playlist("PL_$it", "Dua Lipa") }, "Dua Lipa", limit = 12)

        assertEquals(12, split.byArtist.size)
        assertTrue(split.featuredOn.isEmpty())
    }
}
