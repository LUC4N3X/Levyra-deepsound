package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.youtubeMusicHomeCopy
import com.luc4n3x.levyra.ui.i18n.youtubeMusicHomeLocalizationCodes
import com.luc4n3x.levyra.ui.youtubemusic.entryFor
import com.luc4n3x.levyra.ui.youtubemusic.youtubeMusicShelfAlbums
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicLibraryShelfTest {
    private fun card(browseId: String, menuIcons: List<String>, libraryToggle: Boolean = false, authorChannel: String? = null): JSONObject {
        val menuItems = menuIcons.map { icon ->
            JSONObject().put("menuNavigationItemRenderer", JSONObject().put("icon", JSONObject().put("iconType", icon)))
        }.toMutableList()
        if (libraryToggle) {
            menuItems += JSONObject().put(
                "toggleMenuServiceItemRenderer",
                JSONObject()
                    .put("defaultIcon", JSONObject().put("iconType", "LIBRARY_SAVED"))
                    .put("toggledIcon", JSONObject().put("iconType", "LIBRARY_ADD"))
            )
        }
        val subtitleRuns = JSONArray().put(
            JSONObject().put("text", "Author").apply {
                authorChannel?.let { put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", it))) }
            }
        )
        return JSONObject().put(
            "musicTwoRowItemRenderer",
            JSONObject()
                .put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", browseId)))
                .put("subtitle", JSONObject().put("runs", subtitleRuns))
                .put("menu", JSONObject().put("menuRenderer", JSONObject().put("items", JSONArray(menuItems))))
        )
    }

    private fun entry(browseId: String, owned: Boolean = false, collection: YoutubeMusicCollection = YoutubeMusicCollection.Playlists) =
        YoutubeMusicLibraryEntry(browseId, "Title $browseId", "Author", "", collection, owned)

    @Test
    fun `created playlists are told apart from saved ones and albums`() {
        val ownMenu = listOf("MUSIC_SHUFFLE", "MIX", "QUEUE_PLAY_NEXT", "ADD_TO_REMOTE_QUEUE", "ADD_TO_PLAYLIST", "SHARE")
        val content = JSONObject().put(
            "items",
            JSONArray(
                listOf(
                    card("VLPLmine", ownMenu),
                    card("VLPLedit", ownMenu + "EDIT", authorChannel = "UCme"),
                    card("VLPLsaved", ownMenu, libraryToggle = true, authorChannel = "UCother"),
                    card("VLPLlinked", ownMenu, authorChannel = "UCother"),
                    card("MPREalbum", ownMenu)
                )
            )
        )

        assertEquals(setOf("VLPLmine", "VLPLedit"), YoutubeMusicAccountParser.ownedPlaylistBrowseIds(content))
    }

    @Test
    fun `shelf hides system playlists and lists created playlists first`() {
        val shelf = youtubeMusicShelfPlaylists(
            listOf(entry("VLLM"), entry("VLPLsaved"), entry("VLSE"), entry("VLPLmine", owned = true))
        )

        assertEquals(listOf("VLPLmine", "VLPLsaved"), shelf.map { it.browseId })
    }

    @Test
    fun `shelf cards label created playlists and keep albums as albums`() {
        val library = YoutubeMusicLibraryShelf(
            accountId = "account",
            languageCode = "it",
            playlists = listOf(entry("VLPLmine", owned = true), entry("VLPLsaved")),
            albums = listOf(entry("MPREalbum", collection = YoutubeMusicCollection.Albums))
        )

        val cards = youtubeMusicShelfAlbums(library, "Creata da te")

        assertEquals(listOf("Creata da te", "Author", "Author"), cards.map { it.artist })
        assertEquals(ReleaseType.Album, cards.last().releaseType)
        assertEquals(YoutubeMusicCollection.Albums, library.entryFor(cards.last())?.collection)
        assertNull(library.entryFor(cards.first().copy(browseId = "VLunknown")))
    }

    @Test
    fun `every supported language ships the home library copy`() {
        assertEquals(LevyraLanguageCatalog.languages.map { it.code }.toSet(), youtubeMusicHomeLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = youtubeMusicHomeCopy(strings.code)
            val values = listOf(copy.title, copy.source, copy.createdByYou, copy.refresh, copy.loadFailed, copy.retry, copy.sessionExpired, copy.reconnect)
            assertTrue(strings.code, values.all(String::isNotBlank))
        }
    }
}
