package com.luc4n3x.levyra.data.credits

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackCreditsParserTest {
    private fun creditSection(id: String, title: String, vararg people: Pair<String, List<String>>): JSONObject =
        JSONObject()
            .put("id", id)
            .put("attributes", JSONObject().put("title", title))
            .put(
                "relationships",
                JSONObject().put(
                    "credit-artists",
                    JSONObject().put("data", JSONArray().apply {
                        people.forEach { (name, roles) ->
                            put(JSONObject().put("attributes", JSONObject().put("name", name).put("roleNames", JSONArray(roles))))
                        }
                    })
                )
            )

    private fun song(composer: String, copyright: String, label: String): JSONObject = JSONObject().put(
        "data",
        JSONArray().put(
            JSONObject()
                .put("attributes", JSONObject().put("composerName", composer).put("isrc", "USABC2400001").put("releaseDate", "2024-03-08"))
                .put(
                    "relationships",
                    JSONObject().put(
                        "albums",
                        JSONObject().put(
                            "data",
                            JSONArray().put(
                                JSONObject().put(
                                    "attributes",
                                    JSONObject().put("name", "Album").put("copyright", copyright).put("recordLabel", label)
                                )
                            )
                        )
                    )
                )
        )
    )

    @Test
    fun groupsAppleSectionsAndMergesDuplicatePeople() {
        val credits = JSONObject().put(
            "data",
            JSONArray()
                .put(creditSection("performer", "Performers", "Ada" to listOf("Vocals"), "Ada" to listOf("Guitar")))
                .put(creditSection("composer", "Composition & Lyrics", "Bea" to listOf("Composer", "Lyricist")))
                .put(creditSection("producer", "Production & Engineering", "Cy" to listOf("Producer")))
        )
        val parsed = TrackCreditsParser.parseApple(song("", "℗ 2024 Label", "Label"), credits, releaseMatch = true)

        assertEquals(
            listOf(TrackCreditGroup.Performance, TrackCreditGroup.Composition, TrackCreditGroup.Production),
            parsed.sections.map { it.group }
        )
        assertEquals(listOf("Vocals", "Guitar"), parsed.sections.first().people.single().roles)
        assertEquals("℗ 2024 Label", parsed.release.phonographicCopyright)
        assertEquals("", parsed.release.copyright)
        assertEquals("Label", parsed.release.label)
    }

    @Test
    fun composerFallbackSplitsNamesWithoutInventingRoles() {
        val parsed = TrackCreditsParser.parseApple(song("Ann Lee, Bo Ray & Cal", "", ""), null, releaseMatch = true)
        val composition = parsed.sections.single()

        assertEquals(TrackCreditGroup.Composition, composition.group)
        assertEquals(listOf("Ann Lee", "Bo Ray", "Cal"), composition.people.map { it.name })
        assertTrue(composition.people.all { it.roles.isEmpty() })
    }

    @Test
    fun releaseFieldsAreDroppedWhenOnlyTheRecordingMatched() {
        val parsed = TrackCreditsParser.parseApple(song("", "℗ 2010 Compilation", "Other"), null, releaseMatch = false)

        assertEquals("", parsed.release.label)
        assertEquals("", parsed.release.album)
        assertEquals("", parsed.release.phonographicCopyright)
        assertEquals("USABC2400001", parsed.release.isrc)
    }

    @Test
    fun withArtistsKeepsComposersAndLinksMatchingNames() {
        val base = TrackCredits(
            sections = listOf(
                TrackCreditSection(TrackCreditGroup.Performance, "", listOf(TrackCreditPerson("Ada", listOf("Primary Artist")))),
                TrackCreditSection(TrackCreditGroup.Composition, "", listOf(TrackCreditPerson("Ada"), TrackCreditPerson("Bea")))
            )
        )
        val display = TrackCreditsParser.withArtists(base, listOf("Ada"))

        assertEquals(listOf(TrackCreditGroup.Artists, TrackCreditGroup.Composition), display.sections.map { it.group })
        val composers = display.sections.last().people
        assertEquals(0, composers.first { it.name == "Ada" }.linkedArtistIndex)
        assertNull(composers.first { it.name == "Bea" }.linkedArtistIndex)
    }
}
