package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistNameScriptMatchTest {
    @Test
    fun pinnedArtistNameMatchesTheHeaderInAnotherScriptOrSpelling() {
        assertTrue(artistNameMatches("Okean Elzy", "Океан Ельзи"))
        assertTrue(artistNameMatches("Океан Эльзы", "Океан Ельзи"))
        assertTrue(artistNameMatches("Kino", "Кино"))
        assertTrue(artistNameMatches("Океан Ельзи", "Океан Ельзи"))
    }

    @Test
    fun differentArtistsStillDoNotMatch() {
        assertFalse(artistNameMatches("Okean Elzy", "Кино"))
        assertFalse(artistNameMatches("Земфира", "Zemlyane"))
        assertFalse(artistNameMatches("Gold", "Hold"))
        assertFalse(artistNameMatches("Yngwie Malmsteen", "Yngwie J. Malmsteen's Rising Force"))
        assertFalse(artistNameMatches("J Balvin", "Balvin"))
    }

    @Test
    fun middleInitialDoesNotSplitOneArtist() {
        assertTrue(artistNameMatches("Yngwie J Malmsteen", "Yngwie Malmsteen"))
        assertTrue(artistNameMatches("Yngwie Malmsteen", "Yngwie J. Malmsteen"))
        assertFalse(artistNameMatches("John A Smith", "John B Smith"))
    }

    @Test
    fun scriptsAreComparedByLetters() {
        assertTrue(artistNamesShareScript("Yngwie Malmsteen", "Steve Vai"))
        assertFalse(artistNamesShareScript("Yngwie Malmsteen", "イングヴェイ・マルムスティーン"))
        assertFalse(artistNamesShareScript("Кино", "Kino"))
        assertTrue(artistNamesShareScript("123", "Kino"))
    }

    @Test
    fun localizedHeaderResolvesThroughCreditsLinkedToTheSameArtist() {
        val page = artistPage(
            selfId = "UC-self",
            credits = listOf(
                "Yngwie Malmsteen" to "UC-self",
                "Yngwie Malmsteen" to "UC-self",
                "Ингви Мальмстин" to "UC-self",
                "Ингви Мальмстин" to "UC-self",
                "Ингви Мальмстин" to "UC-self",
                "Ronnie James Dio" to "UC-self",
                "Steve Vai" to "UC-other",
                "Steve Vai" to "UC-other"
            )
        )
        val credits = artistPageCreditNames(page, "uc-self")

        assertEquals(listOf("Ингви Мальмстин", "Yngwie Malmsteen"), credits)
        assertEquals("Yngwie Malmsteen", resolveArtistPageName("Yngwie Malmsteen", "イングヴェイ・マルムスティーン") { credits })
        assertEquals("Yngwie Malmsteen", resolveArtistPageName("Yngwie J Malmsteen", "イングヴェイ・マルムスティーン") { credits })
        assertNull(resolveArtistPageName("Ronnie James Dio", "イングヴェイ・マルムスティーン") { credits })
        assertNull(resolveArtistPageName("Steve Vai", "イングヴェイ・マルムスティーン") { credits })
    }

    @Test
    fun matchingHeaderKeepsItsOwnName() {
        assertEquals("Кино", resolveArtistPageName("Kino", "Кино") { emptyList() })
    }

    private fun artistPage(selfId: String, credits: List<Pair<String, String>>): JSONObject {
        val items = JSONArray()
        credits.forEachIndexed { index, (name, browseId) ->
            val creditRun = JSONObject()
                .put("text", name)
                .put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", browseId)))
            val renderer = if (index % 2 == 0) {
                JSONObject().put(
                    "musicResponsiveListItemRenderer",
                    JSONObject().put(
                        "flexColumns",
                        JSONArray()
                            .put(flexColumn(JSONArray().put(JSONObject().put("text", "Song $index"))))
                            .put(flexColumn(JSONArray().put(creditRun)))
                    )
                )
            } else {
                JSONObject().put(
                    "musicTwoRowItemRenderer",
                    JSONObject().put(
                        "subtitle",
                        JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Album • ")).put(creditRun))
                    )
                )
            }
            items.put(renderer)
        }
        val sectionTitleLinkingToSelf = JSONObject().put(
            "runs",
            JSONArray().put(
                JSONObject()
                    .put("text", "Live performances")
                    .put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", selfId)))
            )
        )
        return JSONObject().put(
            "contents",
            JSONArray()
                .put(JSONObject().put("title", sectionTitleLinkingToSelf))
                .put(JSONObject().put("title", sectionTitleLinkingToSelf))
                .put(JSONObject().put("contents", items))
        )
    }

    private fun flexColumn(runs: JSONArray): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", runs))
    )
}
