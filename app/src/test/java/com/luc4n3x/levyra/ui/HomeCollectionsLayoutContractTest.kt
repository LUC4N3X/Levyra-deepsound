package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCollectionsLayoutContractTest {
    private val source: String by lazy {
        val path = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
        Files.readString(path)
    }

    private fun dpConstant(name: String): Float {
        val match = Regex("private val $name = ([0-9.]+)\\.dp").find(source)
            ?: error("$name not found")
        return match.groupValues[1].toFloat()
    }

    @Test
    fun `collections shelf is a single substantial editorial row`() {
        assertFalse(source.contains(".chunked(2)"))
        assertTrue(source.contains("items = collections"))

        val width = dpConstant("HOME_COLLECTION_CARD_WIDTH")
        val height = dpConstant("HOME_COLLECTION_CARD_HEIGHT")
        val artwork = dpConstant("HOME_COLLECTION_ART_SIZE")

        assertTrue(width >= 170f)
        assertTrue(height >= 150f)
        assertTrue(height / width >= 0.82f)
        assertTrue(artwork >= 72f)
    }
}
