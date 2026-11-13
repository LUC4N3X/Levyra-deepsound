package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.data.apple.AppleCatalogShelf
import com.luc4n3x.levyra.data.apple.AppleCatalogShelfKind
import com.luc4n3x.levyra.data.lastfm.LastFmDiscoveryAlbum
import com.luc4n3x.levyra.data.lastfm.LastFmGenreDiscovery
import com.luc4n3x.levyra.feature.discovery.track
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreDiscoveryDedupTest {

    @Test
    fun youTubeMusicSectionsWinOverLastFm() {
        val editorial = editorial(artists = listOf("Daft Punk", "Lady Gaga"), albums = listOf("Random Access Memories" to "Daft Punk"))
        val discovery = discovery(
            artists = listOf("Daft Punk", "Lady Gaga", "Justice"),
            albums = listOf(album("Random Access Memories", "Daft Punk"), album("The Fame", "Lady Gaga"))
        )

        val result = dedupeGenreDiscovery(editorial, discovery)

        assertEquals(listOf("Justice"), result.lastFm?.artists)
        assertEquals(listOf("The Fame"), result.lastFm?.albums?.map { it.title })
    }

    @Test
    fun appleShelvesOnlyAddWhatIsNewAndEmptyShelvesDisappear() {
        val editorial = editorial(artists = listOf("Daft Punk"), albums = emptyList())
        val discovery = discovery(
            artists = listOf("Justice"),
            albums = listOf(album("Cross", "Justice")),
            apple = listOf(
                shelf("artists", AppleCatalogItemKind.Artist, "daft punk" to "", "Justice" to "", "Kavinsky" to ""),
                shelf("albums", AppleCatalogItemKind.Album, "Cross" to "Justice", "Discovery" to "Daft Punk"),
                shelf("repeat", AppleCatalogItemKind.Artist, "Kavinsky" to "", "Justice" to "")
            )
        )

        val result = dedupeGenreDiscovery(editorial, discovery)

        assertEquals(listOf("artists", "albums"), result.apple.map { it.id })
        assertEquals(listOf("Kavinsky"), result.apple[0].items.map { it.title })
        assertEquals(listOf("Discovery"), result.apple[1].items.map { it.title })
    }

    @Test
    fun featuredAndCommaArtistsCountIndividually() {
        assertEquals(listOf("daft punk", "julian casablancas"), genreArtistNames("Daft Punk, Julian Casablancas"))
        assertEquals(listOf("daft punk", "pharrell williams"), genreArtistNames("Daft Punk feat. Pharrell Williams"))
        assertEquals(listOf("beyonce", "jay z"), genreArtistNames("Beyoncé (feat. JAY-Z)"))
        assertEquals(genreAlbumKey("Discovery", "Daft Punk"), genreAlbumKey("DISCOVERY!", "Daft Punk, Romanthony"))

        val credited = discovery(
            artists = listOf("Daft Punk, Julian Casablancas", "Julian Casablancas", "Daft Punk", "The Weeknd"),
            albums = emptyList(),
            apple = listOf(shelf("artists", AppleCatalogItemKind.Artist, "Julian Casablancas" to "", "Kavinsky" to ""))
        )
        val creditedResult = dedupeGenreDiscovery(editorial(emptyList(), emptyList()), credited)
        assertEquals(listOf("Daft Punk, Julian Casablancas", "The Weeknd"), creditedResult.lastFm?.artists)
        assertEquals(listOf("Kavinsky"), creditedResult.apple.single().items.map { it.title })

        val fromYouTube = discovery(artists = listOf("Julian Casablancas, Daft Punk", "Justice"), albums = emptyList())
        assertEquals(listOf("Justice"), dedupeGenreDiscovery(editorial(listOf("Daft Punk"), emptyList()), fromYouTube).lastFm?.artists)
    }

    @Test
    fun repeatedAppleItemsAcrossShelvesAreShownOnce() {
        val discovery = discovery(
            artists = emptyList(),
            albums = emptyList(),
            apple = listOf(
                shelf("first", AppleCatalogItemKind.Playlist, "Rap Life" to ""),
                shelf("second", AppleCatalogItemKind.Playlist, "Rap Life" to "", "Chill Rap" to "")
            )
        )

        val result = dedupeGenreDiscovery(editorial(emptyList(), emptyList()), discovery)

        assertEquals(listOf("Rap Life"), result.apple[0].items.map { it.title })
        assertEquals(listOf("Chill Rap"), result.apple[1].items.map { it.title })
        assertEquals(result, dedupeGenreDiscovery(editorial(emptyList(), emptyList()), discovery))
    }

    @Test
    fun artistsAndAlbumsMergeIntoOneSectionEachWithYouTubeMusicFirst() {
        val editorial = editorial(artists = listOf("Daft Punk", "Lady Gaga"), albums = listOf("Random Access Memories" to "Daft Punk"))
        val discovery = dedupeGenreDiscovery(
            editorial,
            discovery(
                artists = listOf("Lady Gaga", "Justice", "Daft Punk, Julian Casablancas"),
                albums = listOf(album("Random Access Memories", "Daft Punk"), album("Cross", "Justice"))
            )
        )

        val artists = genreArtistEntries(editorial, discovery)
        val albums = genreAlbumEntries(editorial, discovery)

        assertEquals(listOf("Daft Punk", "Lady Gaga", "Justice"), artists.map { it.name })
        assertEquals(listOf(true, true, false), artists.map { it.track != null })
        assertEquals(listOf("Random Access Memories", "Cross"), albums.map { it.title })
        assertEquals(listOf(true, false), albums.map { it.track != null })
        assertEquals(artists.size, artists.map { it.key }.toSet().size)
        assertEquals(albums.size, albums.map { it.key }.toSet().size)
    }

    @Test
    fun lastFmFillsTheSingleSectionsWhenYouTubeMusicHasNone() {
        val editorial = editorial(emptyList(), emptyList())
        val discovery = dedupeGenreDiscovery(editorial, discovery(artists = listOf("Ludovico Einaudi"), albums = listOf(album("Divenire", "Ludovico Einaudi"))))

        assertEquals(listOf("lastfm:Ludovico Einaudi"), genreArtistEntries(editorial, discovery).map { it.key })
        assertEquals(listOf("https://img/Divenire.png"), genreAlbumEntries(editorial, discovery).map { it.artworkUrl })
        assertTrue(genreArtistEntries(editorial, ExploreGenreDiscoveryState()).isEmpty())
    }

    private fun editorial(artists: List<String>, albums: List<Pair<String, String>>) = ExploreGenreEditorial(
        featured = emptyList(),
        artists = artists.map { name -> ExploreGenreArtistCard(name, name, "", track("Song", name)) },
        albums = albums.map { (title, artist) -> ExploreGenreAlbumCard(title, title, artist, "", track("Song", artist)) },
        essentials = emptyList()
    )

    private fun discovery(
        artists: List<String>,
        albums: List<LastFmDiscoveryAlbum>,
        apple: List<AppleCatalogShelf> = emptyList()
    ) = ExploreGenreDiscoveryState(key = "it:zone:elettronica", apple = apple, lastFm = LastFmGenreDiscovery("", artists, albums))

    private fun album(title: String, artist: String) = LastFmDiscoveryAlbum(title, artist, "https://img/$title.png")

    private fun shelf(id: String, kind: AppleCatalogItemKind, vararg items: Pair<String, String>) = AppleCatalogShelf(
        id = id,
        title = id,
        kind = AppleCatalogShelfKind.Grid,
        items = items.map { (title, subtitle) -> AppleCatalogItem(id = title.lowercase(), kind = kind, title = title, subtitle = subtitle) }
    )
}
