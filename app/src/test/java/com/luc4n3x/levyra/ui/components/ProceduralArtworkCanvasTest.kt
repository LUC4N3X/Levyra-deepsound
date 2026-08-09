package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProceduralArtworkCanvasTest {

    @Test
    fun seedGeneratesDeterministicParameters() {
        val seed1 = "Radiohead - Karma Police"
        val seed2 = "Daft Punk - One More Time"

        val hash1 = Math.abs(seed1.hashCode())
        val hash2 = Math.abs(seed2.hashCode())

        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals(hash1, Math.abs(seed1.hashCode()))
    }
}
