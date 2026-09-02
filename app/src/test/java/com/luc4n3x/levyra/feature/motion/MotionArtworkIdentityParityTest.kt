package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MotionArtworkIdentityParityTest {
    @Test
    fun `same recording keeps motion key when playback provider changes id`() {
        val catalog = track(
            id = "catalog-entry",
            album = "YouTube Music",
            durationMs = 0L
        )
        val resolvedPlayback = track(
            id = "dQw4w9WgXcQ",
            album = "Official Album",
            durationMs = 213_000L
        )

        assertEquals(
            MotionArtworkIdentityKey.create(catalog),
            MotionArtworkIdentityKey.create(resolvedPlayback)
        )
    }

    @Test
    fun `different recording still gets a different motion key`() {
        val first = track(id = "one", title = "First Song")
        val second = track(id = "two", title = "Second Song")

        assertNotEquals(
            MotionArtworkIdentityKey.create(first),
            MotionArtworkIdentityKey.create(second)
        )
    }

    private fun track(
        id: String,
        title: String = "Same Song",
        album: String = "Album",
        durationMs: Long = 210_000L
    ) = Track(
        id = id,
        title = title,
        artist = "Same Artist",
        album = album,
        durationMs = durationMs,
        streamUrl = "",
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
