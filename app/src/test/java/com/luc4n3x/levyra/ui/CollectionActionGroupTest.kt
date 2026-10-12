package com.luc4n3x.levyra.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionActionGroupTest {
    @Test
    fun `short labels share one connected row`() {
        assertFalse(
            collectionActionsStacked(
                availableWidth = 400f,
                playLabelWidth = 90f,
                shuffleLabelWidth = 70f,
                buttonChrome = 62f,
                gap = 4f
            )
        )
    }

    @Test
    fun `a long label stacks the group instead of truncating`() {
        assertTrue(
            collectionActionsStacked(
                availableWidth = 400f,
                playLabelWidth = 130f,
                shuffleLabelWidth = 190f,
                buttonChrome = 62f,
                gap = 4f
            )
        )
    }
}
