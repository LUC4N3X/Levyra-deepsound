package com.luc4n3x.levyra.feature.systemintegration

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.Track
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraRomMediaIntegrationTest {
    @Test
    fun oplusFamiliesExposeTimedLyricsCapability() {
        listOf(
            "OPPO" to "OPPO",
            "OnePlus" to "OnePlus",
            "realme" to "realme"
        ).forEach { (manufacturer, brand) ->
            val capabilities = detectLevyraRomMediaCapabilities(manufacturer, brand)
            assertEquals(LevyraRomFamily.OPLUS, capabilities.family)
            assertTrue(capabilities.timedLyricsMetadata)
        }
    }

    @Test
    fun unrelatedRomStaysGeneric() {
        val capabilities = detectLevyraRomMediaCapabilities("samsung", "samsung")
        assertEquals(LevyraRomFamily.GENERIC, capabilities.family)
        assertFalse(capabilities.timedLyricsMetadata)
    }

    @Test
    fun unsyncedLyricsAreNotPublished() {
        assertNull(
            buildOPlusLyricsPayload(
                track = track(),
                lines = listOf(LyricLine(0L, 1_000L, "Hello")),
                synced = false,
                provider = "test",
                packageName = "com.luc4n3x.levyra",
                generation = 1L
            )
        )
    }

    @Test
    fun payloadCarriesLineWordAndTranslationTiming() {
        val payload = buildOPlusLyricsPayload(
            track = track(),
            lines = listOf(
                LyricLine(
                    startMs = 10_000L,
                    endMs = 12_800L,
                    text = "Hello world",
                    translated = "Ciao mondo",
                    words = listOf(
                        LyricWord(10_000L, 10_700L, "Hello "),
                        LyricWord(10_700L, 12_800L, "world")
                    )
                ),
                LyricLine(
                    startMs = 14_500L,
                    endMs = 17_000L,
                    text = "Second line"
                )
            ),
            synced = true,
            provider = "LRCLIB",
            packageName = "com.luc4n3x.levyra",
            generation = 7L
        )

        val root = JSONObject(requireNotNull(payload))
        assertEquals("song-1", root.getString("songId"))
        assertEquals(7L, root.getLong("sessionGeneration"))
        assertEquals(
            "[00:10.000]Hello world\n[00:14.500]Second line\n",
            root.getString("lyric")
        )
        assertEquals(
            "[00:10.000]Ciao mondo\n",
            root.getString("translationLyric")
        )
        assertEquals(
            "[00:10.000]<00:10.000>Hello <00:10.700>world<00:12.800>\n",
            root.getString("rawLyric")
        )
    }

    @Test
    fun metadataAndInstrumentalRowsDoNotLeakIntoSystemLyrics() {
        val payload = buildOPlusLyricsPayload(
            track = track(),
            lines = listOf(
                LyricLine(0L, 500L, "[by:someone]", isMetadata = true),
                LyricLine(500L, 1_000L, "♪", isInstrumental = true),
                LyricLine(1_000L, 2_000L, "Real lyric")
            ),
            synced = true,
            provider = "test",
            packageName = "com.luc4n3x.levyra",
            generation = 2L
        )

        val lyric = JSONObject(requireNotNull(payload)).getString("lyric")
        assertEquals("[00:01.000]Real lyric\n", lyric)
    }

    private fun track() = Track(
        id = "song-1",
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "https://example.test/audio",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
