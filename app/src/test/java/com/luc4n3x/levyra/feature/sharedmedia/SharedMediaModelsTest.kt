package com.luc4n3x.levyra.feature.sharedmedia

import org.junit.Assert.assertNotEquals
import org.junit.Test

class SharedMediaModelsTest {
    @Test
    fun `playlist request key uses complete payload identity`() {
        val prefix = "A".repeat(64)
        val first = SharedMediaRequest(
            rawText = "",
            url = "",
            kind = SharedMediaKind.LevyraPlaylist,
            sharedPlaylistPayload = prefix + "first"
        )
        val second = first.copy(sharedPlaylistPayload = prefix + "second")

        assertNotEquals(first.key, second.key)
    }
}
