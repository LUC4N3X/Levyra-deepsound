package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicSearchRendererTest {
    private val repository = YoutubeMusicRepository()

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
        assertEquals("YouTube Music", track.artist)
        assertEquals("YouTube Music", track.album)
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

    private fun renderer(vararg flexLines: JSONObject): JSONObject = JSONObject()
        .put("playlistItemData", JSONObject().put("videoId", "2nd73lyvq4w"))
        .put("flexColumns", JSONArray(flexLines.toList()))

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
            JSONObject().put(
                "browseEndpoint",
                JSONObject()
                    .put("browseId", browseId)
                    .put(
                        "browseEndpointContextSupportedConfigs",
                        JSONObject().put(
                            "browseEndpointContextMusicConfig",
                            JSONObject().put("pageType", "MUSIC_PAGE_TYPE_ARTIST")
                        )
                    )
            )
        )

    private fun flexColumn(runs: JSONArray): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", runs))
    )
}
