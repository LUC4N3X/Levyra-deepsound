package com.luc4n3x.levyra.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareResourceTest {
    private val released = mutableListOf<String>()
    private fun resource() = LyricsShareResource<String> { released += it }

    @Test
    fun closeReleasesAnIdleValueExactlyOnce() {
        val resource = resource()
        resource.set("artwork")
        resource.close()
        resource.close()
        assertEquals(listOf("artwork"), released)
    }

    @Test
    fun closeWhileInUseDefersReleaseUntilTheUserFinishes() {
        val resource = resource()
        resource.set("artwork")
        resource.use { value ->
            assertEquals("artwork", value)
            resource.close()
            assertTrue(released.isEmpty())
        }
        assertEquals(listOf("artwork"), released)
    }

    @Test
    fun useAfterCloseSeesNoValue() {
        val resource = resource()
        resource.set("artwork")
        resource.close()
        assertNull(resource.use { it })
    }

    @Test
    fun setAfterCloseReleasesTheLateValueImmediately() {
        val resource = resource()
        resource.close()
        resource.set("late")
        assertEquals(listOf("late"), released)
        assertNull(resource.use { it })
    }

    @Test
    fun secondSetIsRejectedAndReleased() {
        val resource = resource()
        resource.set("first")
        resource.set("second")
        assertEquals(listOf("second"), released)
        assertEquals("first", resource.use { it })
    }

    @Test
    fun releaseStillHappensWhenTheUserThrows() {
        val resource = resource()
        resource.set("artwork")
        try {
            resource.use { error("render failed") }
        } catch (_: IllegalStateException) {
        }
        assertTrue(released.isEmpty())
        resource.close()
        assertEquals(listOf("artwork"), released)
    }

    @Test
    fun emptyResourceCloseReleasesNothing() {
        val resource = resource()
        resource.close()
        assertFalse(released.any())
    }

    private val first = LyricsSharePreviewKey(LyricsShareCardContent("T", "A", listOf("one")), LyricsShareCardStyle.ARTWORK)
    private val otherStyle = first.copy(style = LyricsShareCardStyle.MINIMAL)
    private val otherText = first.copy(content = first.content.copy(lyrics = listOf("translated")))

    @Test
    fun readyPreviewIsOnlyShownForItsOwnKey() {
        val state: LyricsSharePreviewState<String> = LyricsSharePreviewState.Ready(first, "card")
        assertEquals("card", state.readyFor(first))
        assertNull(state.readyFor(otherStyle))
        assertNull(state.readyFor(otherText))
    }

    @Test
    fun failedPreviewNeverLeavesStaleContentVisible() {
        val ready: LyricsSharePreviewState<String> = LyricsSharePreviewState.Ready(first, "card")
        val failed: LyricsSharePreviewState<String> = LyricsSharePreviewState.Failed(otherStyle)
        assertNull(failed.readyFor(first))
        assertNull(failed.readyFor(otherStyle))
        assertTrue(failed.failedFor(otherStyle))
        assertFalse(failed.failedFor(first))
        assertFalse(ready.failedFor(first))
        assertNull((null as LyricsSharePreviewState<String>?).readyFor(first))
    }
}
