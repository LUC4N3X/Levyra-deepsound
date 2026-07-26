package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFileNameTest {
    @Test
    fun longMetadataKeepsDistinctStableSuffixes() {
        val longArtist = "Artist ".repeat(40)
        val longTitle = "Very long track title ".repeat(40)
        val first = OfflineFileName.baseName(
            Track(
                id = "first-unique-video-id",
                title = longTitle,
                artist = longArtist,
                videoUrl = "https://music.youtube.com/watch?v=first-unique-video-id"
            )
        )
        val second = OfflineFileName.baseName(
            Track(
                id = "second-unique-video-id",
                title = longTitle,
                artist = longArtist,
                videoUrl = "https://music.youtube.com/watch?v=second-unique-video-id"
            )
        )

        assertTrue(first.length <= 180)
        assertTrue(second.length <= 180)
        assertTrue(first.matches(Regex(".* \\[[0-9a-f]{16}]$")))
        assertTrue(second.matches(Regex(".* \\[[0-9a-f]{16}]$")))
        assertNotEquals(first, second)
    }
}
