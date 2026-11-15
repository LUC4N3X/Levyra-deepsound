package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicMoodSectionsParserTest {

    private val repository = YoutubeMusicRepository()

    @Test
    fun keepsSectionTitlesAndPageOrder() {
        val sections = repository.parseMoodPlaylistSections(JSONObject(categoryPage))

        assertEquals(listOf("Rap italiano: le playlist", "Playlist popolari Hip Hop"), sections.map { it.title })
        assertEquals(listOf("Ultra", "Street culto"), sections[0].playlists.map { it.title })
        assertEquals(listOf("Hit Rap Italiane"), sections[1].playlists.map { it.title })
    }

    @Test
    fun resolvesPlaylistIdsAndSubtitles() {
        val first = repository.parseMoodPlaylistSections(JSONObject(categoryPage))[0].playlists[0]

        assertEquals("RDCLAK_ultra", first.playlistId)
        assertEquals("VLRDCLAK_ultra", first.browseId)
        assertEquals("Geolier, Sfera Ebbasta, Shiva", first.description)
    }

    @Test
    fun skipsShelvesWithoutTitleOrPlaylists() {
        val sections = repository.parseMoodPlaylistSections(JSONObject(categoryPage))

        assertTrue(sections.none { it.title == "Vuota" })
        assertEquals(2, sections.size)
    }

    @Test
    fun keepsTheSectionMoreLinkOnlyForCategoryPages() {
        val sections = repository.parseMoodPlaylistSections(JSONObject(categoryPage))

        assertEquals(listOf("", "decade-params"), sections.map { it.moreParams })
    }

    @Test
    fun emptyPayloadReturnsNoSections() {
        assertTrue(repository.parseMoodPlaylistSections(JSONObject("""{"contents":{}}""")).isEmpty())
    }

    private fun card(title: String, browseId: String, subtitle: String) = """
        {
          "musicTwoRowItemRenderer": {
            "title": { "runs": [ { "text": "$title" } ] },
            "subtitle": { "runs": [ { "text": "$subtitle" } ] },
            "navigationEndpoint": { "browseEndpoint": { "browseId": "$browseId" } }
          }
        }
    """.trimIndent()

    private val categoryPage = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [ { "tabRenderer": { "content": { "sectionListRenderer": { "contents": [
                {
                  "gridRenderer": {
                    "header": { "gridHeaderRenderer": { "title": { "runs": [ { "text": "Rap italiano: le playlist", "navigationEndpoint": { "browseEndpoint": { "browseId": "VLRDCLAK_other", "params": "ignored" } } } ] } } },
                    "items": [
                      ${card("Ultra", "VLRDCLAK_ultra", "Geolier, Sfera Ebbasta, Shiva")},
                      ${card("Street culto", "VLRDCLAK_street", "Shiva, Nerissima Serpe")}
                    ]
                  }
                },
                {
                  "musicCarouselShelfRenderer": {
                    "header": { "musicCarouselShelfBasicHeaderRenderer": { "title": { "runs": [ { "text": "Vuota" } ] } } },
                    "contents": []
                  }
                },
                {
                  "musicCarouselShelfRenderer": {
                    "contents": [ ${card("Senza titolo", "VLRDCLAK_none", "")} ]
                  }
                },
                {
                  "musicCarouselShelfRenderer": {
                    "header": { "musicCarouselShelfBasicHeaderRenderer": {
                      "title": { "runs": [ { "text": "Playlist popolari Hip Hop", "navigationEndpoint": { "browseEndpoint": { "browseId": "FEmusic_moods_and_genres_category", "params": "decade-params" } } } ] }
                    } },
                    "contents": [ ${card("Hit Rap Italiane", "VLRDCLAK_hit", "Sfera Ebbasta, Guè")} ]
                  }
                }
              ] } } } } ]
            }
          }
        }
    """.trimIndent()
}
