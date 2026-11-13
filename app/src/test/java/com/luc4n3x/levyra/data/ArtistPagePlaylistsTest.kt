package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaylistHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPagePlaylistsTest {

    private fun playlist(id: String, author: String, thumbnail: String = "https://i.ytimg.com/$id.jpg") =
        PlaylistHit(title = "Playlist $id", author = author, thumbnailUrl = thumbnail, playlistId = id)

    @Test
    fun playlistsWithoutArtworkAreNotShownOnTheArtistPage() {
        val playlists = listOf(
            playlist("PL_broken", "Some Listener", thumbnail = ""),
            playlist("PL_ok", "Some Listener")
        )

        val split = splitArtistPagePlaylists(playlists, "Yngwie Malmsteen", limit = 12)

        assertEquals(listOf("PL_ok"), split.featuredOn.map { it.playlistId })
        assertTrue(split.byArtist.isEmpty())
    }

    @Test
    fun playlistsWithoutAnyIdAreDroppedButBrowseIdOnlyIsKept() {
        val noId = PlaylistHit(title = "x", author = "Fan", thumbnailUrl = "https://i.ytimg.com/x.jpg", playlistId = "")
        val browseOnly = PlaylistHit(title = "y", author = "Fan", thumbnailUrl = "https://i.ytimg.com/y.jpg", playlistId = "", browseId = "VLPLy")

        val split = splitArtistPagePlaylists(listOf(noId, browseOnly), "Yngwie Malmsteen", limit = 12)

        assertEquals(listOf("VLPLy"), split.featuredOn.map { it.browseId })
        assertEquals(listOf("PLy"), split.featuredOn.map { it.playlistId })
    }

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
