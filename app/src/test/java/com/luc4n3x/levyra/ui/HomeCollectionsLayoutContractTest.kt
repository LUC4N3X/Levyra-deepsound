package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCollectionsLayoutContractTest {
    private fun source(file: String): String {
        val path = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/$file"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/$file")
        ).firstOrNull(Files::exists) ?: error("$file not found")
        return Files.readString(path)
    }

    private fun functionBlock(source: String, signature: String): String {
        val start = source.indexOf(signature)
        require(start >= 0) { "$signature not found" }
        val bodyStart = source.indexOf('{', start)
        require(bodyStart > start)
        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        error("$signature body is not balanced")
    }

    @Test
    fun `collections shelf is a single responsive editorial row`() {
        val shelf = functionBlock(source("LevyraApp.kt"), "private fun HomeEditorialCollectionsShelf(")
        assertFalse(shelf.contains(".chunked(2)"))
        assertTrue(shelf.contains("LazyRow("))
        assertTrue(shelf.contains("items = collections"))
        assertTrue(shelf.contains("key = { collection ->"))
        assertTrue(shelf.contains("DiscoveryEditorialCard("))
        assertTrue(shelf.contains("maxWidth - LevyraHomeDesign.EditorialPeek"))
        assertTrue(shelf.contains("coerceAtMost(LevyraHomeDesign.EditorialMaxWidth)"))
    }

    @Test
    fun `editorial collection cards use restrained color instead of grey or opposing gradients`() {
        val card = functionBlock(source("HomeExploreEditorial.kt"), "internal fun DiscoveryEditorialCard(")
        assertTrue(card.contains("levyraTonalRole(accentStart, isLight, maxChroma = 28.0)"))
        assertTrue(card.contains("lerp(colors.surfaceContainerHigh, tonal.container, 0.82f)"))
        assertTrue(card.contains("lerp(colors.surfaceContainerHigh, tonal.container, 0.54f)"))
        assertTrue(card.contains("editorialContrastPalette(tonalStart, tonalEnd)"))
        assertTrue(card.contains(".levyraPressable(onClick = onOpen"))
        assertFalse(card.contains("colors.surfaceContainerHighest"))
        assertFalse(card.contains("accentEnd.copy(alpha"))
        assertFalse(card.contains("Brush.radialGradient("))
    }

    @Test
    fun `collection text wraps and stays separate from artwork on narrow screens`() {
        val editorial = source("HomeExploreEditorial.kt")
        val card = functionBlock(editorial, "internal fun DiscoveryEditorialCard(")
        val text = functionBlock(editorial, "private fun DiscoveryEditorialText(")
        assertTrue(card.contains("maxWidth < 300.dp"))
        assertTrue(card.contains("LocalDensity.current.fontScale > 1.4f"))
        assertTrue(card.contains("if (stacked)"))
        assertTrue(card.contains("Modifier.weight(1f).heightIn(min = LevyraHomeDesign.EditorialThumb)"))
        assertTrue(card.contains("Arrangement.spacedBy(LevyraHomeDesign.EditorialPadding)"))
        assertTrue(text.contains("softWrap = true"))
        assertFalse(text.contains("TextOverflow.Ellipsis"))
        assertFalse(text.contains("maxLines"))
    }
}
