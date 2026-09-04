package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraHomeSnapshotCacheTest {
    @Test
    fun resonanceSnapshotAfterCompleteReplacementContainsOnlyCurrentIds() {
        val oldTrack = track("aaaaaaaaaaa")
        val firstCurrent = track("bbbbbbbbbbb")
        val disabledCurrent = track("ccccccccccc")
        val comments = linkedMapOf(
            oldTrack.id to ResonanceCommentSnippet(videoId = oldTrack.id, text = "Old"),
            disabledCurrent.id to ResonanceCommentSnippet(
                videoId = disabledCurrent.id,
                disabled = true,
                updatedAtMs = 20L
            ),
            firstCurrent.id to ResonanceCommentSnippet(
                videoId = firstCurrent.id,
                text = "Current",
                updatedAtMs = 10L
            )
        )

        val json = resonanceCommentsToJson(listOf(firstCurrent, disabledCurrent), comments)

        assertEquals(setOf(firstCurrent.id, disabledCurrent.id), json.keys().asSequence().toSet())
        assertEquals("Current", json.getJSONObject(firstCurrent.id).getString("text"))
        assertTrue(json.getJSONObject(disabledCurrent.id).getBoolean("disabled"))
    }

    @Test
    fun successfulEmptyCommentSnapshotKeepsFreshness() {
        val track = track("bbbbbbbbbbb")

        val json = resonanceCommentsToJson(
            listOf(track),
            mapOf(track.id to ResonanceCommentSnippet(videoId = track.id, updatedAtMs = 42L))
        )

        assertEquals(42L, json.getJSONObject(track.id).getLong("updatedAtMs"))
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://music.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}
