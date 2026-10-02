package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistSongAlbumTest {

    @Test
    fun `popular song row takes its album from the album link, not the play count column`() {
        val row = songRow(
            column(run("Don't Stop Me Now", watch = "HgzGwKwLmgM")),
            column(run("Queen", browseId = "UCiMhD4jzUqG-IgPzUmmytRQ", pageType = "MUSIC_PAGE_TYPE_ARTIST")),
            column(run("1.1B plays")),
            column(run("Jazz (Deluxe Edition)", browseId = "MPREb_2nSfvOdSg5s", pageType = "MUSIC_PAGE_TYPE_ALBUM"))
        )

        assertEquals(ArtistSongAlbum(title = "Jazz (Deluxe Edition)", browseId = "MPREb_2nSfvOdSg5s"), artistSongAlbum(row))
    }

    @Test
    fun `album browse id prefix is recognised without page type metadata`() {
        val row = songRow(
            column(run("Kashmir")),
            column(run("Physical Graffiti", browseId = "MPREb_physical"))
        )

        assertEquals("Physical Graffiti", artistSongAlbum(row)?.title)
    }

    @Test
    fun `row without an album link has no album instead of a guessed column`() {
        val row = songRow(
            column(run("Bohemian Rhapsody")),
            column(run("Queen", browseId = "UCiMhD4jzUqG-IgPzUmmytRQ", pageType = "MUSIC_PAGE_TYPE_ARTIST")),
            column(run("2.7B plays"))
        )

        assertNull(artistSongAlbum(row))
    }

    private fun songRow(vararg columns: JSONObject) = JSONObject().put("flexColumns", JSONArray(columns.toList()))

    private fun column(vararg runs: JSONObject) = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", JSONArray(runs.toList())))
    )

    private fun run(text: String, browseId: String = "", pageType: String = "", watch: String = ""): JSONObject {
        val run = JSONObject().put("text", text)
        if (browseId.isNotBlank()) {
            val endpoint = JSONObject().put("browseId", browseId)
            if (pageType.isNotBlank()) {
                endpoint.put(
                    "browseEndpointContextSupportedConfigs",
                    JSONObject().put("browseEndpointContextMusicConfig", JSONObject().put("pageType", pageType))
                )
            }
            run.put("navigationEndpoint", JSONObject().put("browseEndpoint", endpoint))
        }
        if (watch.isNotBlank()) {
            run.put("navigationEndpoint", JSONObject().put("watchEndpoint", JSONObject().put("videoId", watch)))
        }
        return run
    }
}
