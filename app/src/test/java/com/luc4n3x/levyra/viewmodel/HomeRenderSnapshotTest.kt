package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeRenderSnapshotTest {
    @Test
    fun keepsBaseChartsAndDerivedChunksFromTheSameState() {
        val charts = listOf(track("aaaaaaaaaaa"), track("bbbbbbbbbbb"), track("ccccccccccc"), track("ddddddddddd"), track("eeeeeeeeeee"))
        val state = LevyraUiState(charts = charts)

        val snapshot = buildHomeRenderSnapshot(state)

        assertSame(state, snapshot.state)
        assertEquals(snapshot.state.charts, snapshot.derived.chartChunks.flatten())
    }

    private fun track(id: String): Track {
        return Track(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "https://music.youtube.com/watch?v=$id",
            thumbnailUrl = "https://example.com/$id.jpg",
            largeThumbnailUrl = "https://example.com/$id.jpg",
            source = "YouTube Music",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
    }
}
