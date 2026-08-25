package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class YoutubeMusicSearchRendererTest {
    private val repository = YoutubeMusicRepository()

    @Test
    fun videoDiscoveryUsesYoutubeMusicVideoFilter() {
        assertEquals("EgWKAQIQAWoMEA4QChADEAQQCRAF", YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS)
    }

    @Test
    fun `play count is not used as artist or album`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                line("27M plays")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("", track.artist)
        assertEquals("YouTube Music", track.album)
    }

    @Test
    fun `provider name is never surfaced as artist credit`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                line("27M plays")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertNotEquals("YouTube Music", track.artist)
        assertNotEquals("YouTube", track.artist)
    }

    @Test
    fun `artist reference wins when metrics precede it`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                line("27M plays"),
                artistLine("Coldplay", "UCsFc1cQ7K09jEUpVgJ7OrhQ")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals("YouTube Music", track.album)
        assertEquals(listOf("UCsFc1cQ7K09jEUpVgJ7OrhQ"), track.artistBrowseIds)
    }

    @Test
    fun `real album label remains available`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                artistLine("Coldplay", "UCsFc1cQ7K09jEUpVgJ7OrhQ"),
                line("Ghost Stories"),
                line("27M plays")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals("Ghost Stories", track.album)
    }

    @Test
    fun `artist and album packed in single flex column`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                packedLine(
                    artistRun("Coldplay", "UCsFc1cQ7K09jEUpVgJ7OrhQ"),
                    textRun(" • "),
                    textRun("Ghost Stories")
                )
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals("Ghost Stories", track.album)
    }

    @Test
    fun `collaborating artist is not used as album`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                packedLine(
                    artistRun("Coldplay", "UC_COLDPLAY"),
                    textRun(" & "),
                    artistRun("BTS", "UC_BTS")
                ),
                line("My Universe")
            ),
            query = "Coldplay BTS"
        )

        requireNotNull(track)
        assertEquals("Coldplay, BTS", track.artist)
        assertEquals("My Universe", track.album)
        assertEquals(listOf("UC_COLDPLAY", "UC_BTS"), track.artistBrowseIds)
    }

    @Test
    fun `structured album reference wins and browse id is preserved`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                packedLine(
                    artistRun("Coldplay", "UC_COLDPLAY"),
                    textRun(" • "),
                    albumRun("Ghost Stories", "MPRE_GHOST_STORIES")
                ),
                line("27M plays")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals("Ghost Stories", track.album)
        assertEquals("MPRE_GHOST_STORIES", track.albumBrowseId)
    }

    @Test
    fun `localized play count is ignored`() {
        val track = repository.parseMusicRenderer(
            renderer(
                line("O"),
                line("27 M reproducciones"),
                artistLine("Coldplay", "UC_COLDPLAY")
            ),
            query = "Coldplay"
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals("YouTube Music", track.album)
    }

    @Test
    fun `two row carousel prefers artist reference over localized metric`() {
        val track = repository.parseCarouselItem(
            carouselItem(
                textRun("27 M reproducciones"),
                textRun(" • "),
                artistRun("Coldplay", "UC_COLDPLAY")
            )
        )

        requireNotNull(track)
        assertEquals("Coldplay", track.artist)
        assertEquals(listOf("UC_COLDPLAY"), track.artistBrowseIds)
    }

    @Test
    fun `carousel item keeps every collaborating artist`() {
        val track = repository.parseCarouselItem(
            carouselItem(
                artistRun("Coldplay", "UC_COLDPLAY"),
                textRun(" & "),
                artistRun("BTS", "UC_BTS")
            )
        )

        requireNotNull(track)
        assertEquals("Coldplay, BTS", track.artist)
        assertEquals(listOf("UC_COLDPLAY", "UC_BTS"), track.artistBrowseIds)
    }

    @Test
    fun `album track keeps every collaborating artist`() {
        val track = repository.parseAlbumTrackRenderer(
            renderer(
                line("O"),
                packedLine(
                    artistRun("Coldplay", "UC_COLDPLAY"),
                    textRun(" & "),
                    artistRun("BTS", "UC_BTS")
                )
            ),
            album = AlbumHit(
                title = "My Universe",
                artist = "Coldplay",
                year = "2021",
                thumbnailUrl = "https://levyra.test/album.jpg",
                query = "Coldplay My Universe"
            )
        )

        requireNotNull(track)
        assertEquals("Coldplay, BTS", track.artist)
        assertEquals(listOf("UC_COLDPLAY", "UC_BTS"), track.artistBrowseIds)
    }

    @Test
    fun `album parser ignores related rows outside track shelf`() {
        val album = AlbumHit(
            title = "Exact Album",
            artist = "Exact Artist",
            year = "2026",
            thumbnailUrl = "https://levyra.test/exact-album.jpg",
            query = "Exact Album Exact Artist",
            browseId = "MPRE_EXACT_ALBUM"
        )
        val albumRenderer = renderer(
            line("First Song"),
            artistLine("Exact Artist", "UC_EXACT")
        )
        val unrelatedRenderer = renderer(
            line("Unrelated Result"),
            artistLine("Other Artist", "UC_OTHER")
        ).put("playlistItemData", JSONObject().put("videoId", "different01"))
        val root = JSONObject()
            .put(
                "primary",
                JSONObject().put(
                    "musicShelfRenderer",
                    JSONObject().put(
                        "contents",
                        JSONArray().put(JSONObject().put("musicResponsiveListItemRenderer", albumRenderer))
                    )
                )
            )
            .put(
                "related",
                JSONObject().put(
                    "musicCarouselShelfRenderer",
                    JSONObject().put(
                        "contents",
                        JSONArray().put(JSONObject().put("musicResponsiveListItemRenderer", unrelatedRenderer))
                    )
                )
            )

        val tracks = repository.parseAlbumTracks(root, album)

        assertEquals(listOf("First Song"), tracks.map { it.title })
    }

    private fun renderer(vararg flexLines: JSONObject): JSONObject = JSONObject()
        .put("playlistItemData", JSONObject().put("videoId", "2nd73lyvq4w"))
        .put("flexColumns", JSONArray(flexLines.toList()))

    private fun carouselItem(vararg subtitleRuns: JSONObject): JSONObject {
        val runs = JSONArray()
        subtitleRuns.forEach { runs.put(it) }
        val two = JSONObject()
            .put("title", JSONObject().put("runs", JSONArray().put(textRun("O"))))
            .put("subtitle", JSONObject().put("runs", runs))
            .put(
                "navigationEndpoint",
                JSONObject().put("watchEndpoint", JSONObject().put("videoId", "2nd73lyvq4w"))
            )
        return JSONObject().put("musicTwoRowItemRenderer", two)
    }

    private fun line(text: String): JSONObject = flexColumn(
        JSONArray().put(JSONObject().put("text", text))
    )

    private fun artistLine(name: String, browseId: String): JSONObject =
        flexColumn(JSONArray().put(artistRun(name, browseId)))

    private fun packedLine(vararg runs: JSONObject): JSONObject {
        val runsArray = JSONArray()
        runs.forEach { runsArray.put(it) }
        return flexColumn(runsArray)
    }

    private fun textRun(text: String): JSONObject = JSONObject().put("text", text)

    private fun artistRun(name: String, browseId: String): JSONObject = JSONObject()
        .put("text", name)
        .put(
            "navigationEndpoint",
            browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ARTIST")
        )

    private fun albumRun(name: String, browseId: String): JSONObject = JSONObject()
        .put("text", name)
        .put(
            "navigationEndpoint",
            browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ALBUM")
        )

    private fun browseEndpoint(browseId: String, pageType: String): JSONObject = JSONObject().put(
        "browseEndpoint",
        JSONObject()
            .put("browseId", browseId)
            .put(
                "browseEndpointContextSupportedConfigs",
                JSONObject().put(
                    "browseEndpointContextMusicConfig",
                    JSONObject().put("pageType", pageType)
                )
            )
    )

    private fun flexColumn(runs: JSONArray): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", runs))
    )
}
