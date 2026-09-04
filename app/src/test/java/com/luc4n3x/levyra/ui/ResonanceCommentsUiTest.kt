package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ResonanceCommentsUiTest {
    @Test
    fun `comment count keeps the number and removes provider language`() {
        assertEquals("248", youtubeCommentCountBadge("248 Amazwana"))
        assertEquals("1,2K", youtubeCommentCountBadge("1,2K comments"))
    }

    @Test
    fun `blank comment count stays blank`() {
        assertEquals("", youtubeCommentCountBadge("  "))
    }
}
