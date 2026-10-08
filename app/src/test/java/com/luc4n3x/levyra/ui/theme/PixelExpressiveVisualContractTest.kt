package com.luc4n3x.levyra.ui.theme

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelExpressiveVisualContractTest {
    @Test
    fun `surfaces follow an intentional expressive radius scale`() {
        assertEquals(12f, LevyraCardDesign.ThumbCorner.value, 0.001f)
        assertEquals(16f, LevyraCardDesign.ArtworkCorner.value, 0.001f)
        assertEquals(24f, LevyraCardDesign.EditorialCorner.value, 0.001f)
        assertEquals(28f, LevyraCardDesign.SurfaceCorner.value, 0.001f)
        assertEquals(LevyraCardDesign.ArtworkCorner, LevyraHomeDesign.ArtworkCorner)
        assertTrue(LevyraHomeDesign.HeroCorner > LevyraHomeDesign.ShelfCorner)
    }

    @Test
    fun `media titles remain visually stronger than metadata`() {
        assertTrue(LevyraCardDesign.CardTitleSize > LevyraCardDesign.CardSubtitleSize)
        assertTrue(LevyraCardDesign.RowTitleSize > LevyraCardDesign.RowSubtitleSize)
        assertTrue(LevyraCardDesign.RowHeight > LevyraCardDesign.RowThumb)
    }

    @Test
    fun `home chips keep accessible targets and use themed surfaces`() {
        val source = readUi("HomeExperience.kt")
        assertTrue(source.contains(".heightIn(min = 48.dp)"))
        assertTrue(source.contains(".background(MaterialTheme.colorScheme.surfaceContainer)"))
        assertTrue(source.contains("pressed = LevyraCardDesign.SurfaceCorner"))
        assertTrue(source.contains("onSelect(zone)"))
    }

    @Test
    fun `library chip selection is tonal without losing tab semantics`() {
        val source = readUi("library/LibraryOverviewComponents.kt")
        val chip = source.substringAfter("internal fun LibraryCategoryChip(").substringBefore("internal fun LibraryToolbar(")
        assertTrue(chip.contains("colors.secondaryContainer"))
        assertTrue(chip.contains("colors.onSecondaryContainer"))
        assertTrue(chip.contains("LibraryPillShape"))
        assertTrue(chip.contains("role = Role.Tab"))
        assertTrue(chip.contains(".heightIn(min = 48.dp)"))
    }

    @Test
    fun `dock material retains glass fallback and pure black support`() {
        val dock = readUi("components/LevyraAdaptiveDock.kt")
        assertTrue(dock.contains("colors.surfaceContainerHigh"))
        assertTrue(dock.contains("colors.outlineVariant"))
        assertTrue(dock.contains("pureBlack -> DockMaterial("))
        assertTrue(dock.contains(".glassFrost("))
        assertTrue(dock.contains("onDrawFallback = { drawRect(solidBrush) }"))
    }

    private fun readUi(relativePath: String): String {
        val candidates = listOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui", relativePath),
            Path.of("src/main/java/com/luc4n3x/levyra/ui", relativePath)
        )
        val path = candidates.firstOrNull(Files::exists)
            ?: error("Missing UI source: $relativePath")
        return Files.readString(path)
    }
}
