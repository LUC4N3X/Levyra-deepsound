package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleTrendingQuickPicksTest {

    private fun page(sections: String): AppleCatalogPage? = AppleCatalogPageParser.parse(
        """<html><body><script type="application/json" id="serialized-server-data">{"data":[{"data":{"sections":[$sections]}}]}</script></body></html>""",
        "it",
        0L
    )

    private fun songs(id: String, vararg songs: Pair<String, String>): String {
        val items = songs.mapIndexed { index, (title, artist) ->
            """{"id":"$id-$index","title":"$title","subtitleLinks":[{"title":"$artist"}],"contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"${index + 1}"}}}"""
        }.joinToString(",")
        return """{"id":"$id","itemKind":"trackLockup","header":{"item":{"titleLink":{"title":"$id"}}},"items":[$items]}"""
    }

    private val albums = """{"id":"week","itemKind":"squareLockup","header":{"item":{"titleLink":{"title":"week"}}},"items":[
        {"id":"a1","titleLinks":[{"title":"Album"}],"subtitleLinks":[{"title":"Artist"}],"contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"9"}}}]}"""

    private fun youtube(id: String, title: String, artist: String) = Track(
        id = id, title = title, artist = artist, album = "", durationMs = 180_000L,
        streamUrl = "", videoUrl = "https://www.youtube.com/watch?v=$id", thumbnailUrl = "", largeThumbnailUrl = "",
        source = "youtube", moodTags = emptySet(), energy = 50, vocal = 50, replayScore = 70, cacheScore = 70,
        accentStart = 0, accentEnd = 0, videoType = "MUSIC_VIDEO_TYPE_ATV"
    )

    @Test
    fun `trending is the second song shelf whatever its localized title`() {
        val parsed = page(listOf(songs("best", "New" to "Someone"), albums, songs("tendenza", "Housewife" to "Tate McRae")).joinToString(","))

        val trending = appleTrendingSongs(parsed)

        assertEquals(listOf("Housewife"), trending.map { it.title })
        assertEquals("1", trending.single().appleSongId)
    }

    @Test
    fun `storefront without a trending shelf yields nothing`() {
        assertTrue(appleTrendingSongs(page(songs("best", "New" to "Someone"))).isEmpty())
        assertTrue(appleTrendingSongs(null).isEmpty())
    }

    @Test
    fun `only the official audio with the same title and artist is accepted`() = runBlocking {
        val apple = appleTrendingSongs(page(listOf(songs("best", "New" to "Someone"), songs("tendenza", "Housewife" to "Tate McRae", "Ti ricordi" to "Sfera Ebbasta")).joinToString(",")))
        val results = mapOf(
            "Housewife" to listOf(youtube("covervid001", "Housewife (Cover)", "Some Singer"), youtube("housewife01", "Housewife", "Tate McRae")),
            "Ti ricordi" to listOf(youtube("otherartist", "Ti ricordi", "Another Artist"))
        )

        val matched = AppleTrendingQuickPicks.match(apple) { query -> results.entries.firstOrNull { query.contains(it.key, ignoreCase = true) }?.value.orEmpty() }

        assertEquals(listOf("housewife01"), matched.map { it.id })
        assertEquals(apple.first().appleSongId, matched.single().appleSongId)
    }

    @Test
    fun `music videos are never accepted as trending picks`() = runBlocking {
        val apple = appleTrendingSongs(page(listOf(songs("best", "New" to "Someone"), songs("tendenza", "Housewife" to "Tate McRae")).joinToString(",")))
        val video = youtube("housewifemv", "Housewife", "Tate McRae").copy(videoType = "MUSIC_VIDEO_TYPE_OMV")

        assertTrue(AppleTrendingQuickPicks.match(apple) { listOf(video) }.isEmpty())
    }

    @Test
    fun `apple explicit flag survives the youtube match`() = runBlocking {
        val apple = appleTrendingSongs(page(listOf(songs("best", "New" to "Someone"), songs("tendenza", "Ti ricordi" to "Sfera Ebbasta")).joinToString(",")))
            .map { it.copy(explicit = true) }

        val matched = AppleTrendingQuickPicks.match(apple) { listOf(youtube("tiricordi01", "Ti ricordi", "Sfera Ebbasta")) }

        assertTrue(matched.single().explicit)
    }

    @Test
    fun `catalog duration rejects a longer version with the same title`() = runBlocking {
        val apple = withAppleSongDetails(
            appleTrendingSongs(page(listOf(songs("best", "New" to "Someone"), songs("tendenza", "Housewife" to "Tate McRae")).joinToString(","))),
            org.json.JSONArray("""[{"id":"1","attributes":{"durationInMillis":180000,"isrc":"USRC12500001","albumName":"Housewife - Single"}}]""")
        )
        assertEquals(180_000L, apple.single().durationMs)
        assertEquals("USRC12500001", apple.single().isrc)

        val extended = youtube("extended001", "Housewife", "Tate McRae").copy(durationMs = 300_000L)
        val exact = youtube("housewife01", "Housewife", "Tate McRae").copy(durationMs = 181_000L)

        assertTrue(AppleTrendingQuickPicks.match(apple) { listOf(extended) }.isEmpty())
        assertEquals(listOf("housewife01"), AppleTrendingQuickPicks.match(apple) { listOf(extended, exact) }.map { it.id })
    }
}
