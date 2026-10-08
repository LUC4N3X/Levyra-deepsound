package com.luc4n3x.levyra.ui.theme

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `home reveals personal discovery earlier without collapsing touch targets`() {
        assertEquals(296f, LevyraHomeDesign.HeroHeight.value, 0.001f)
        assertTrue(LevyraHomeDesign.HeroHeight.value >= 240f)
        assertTrue(LevyraHomeDesign.SectionStride > LevyraHomeDesign.SectionGap * 2f)
        assertTrue(LevyraHomeDesign.MoodChipHeight.value >= 48f)
        val source = readUi("HomeExperience.kt")
        assertTrue(source.contains("MaterialTheme.typography.labelLarge"))
        assertTrue(source.contains("onSelect(zone)"))
    }

    @Test
    fun `explore discovery category artwork stays upright on tonal surfaces`() {
        val source = readUi("ExploreDestinationScreens.kt")
        val category = source.substringAfter("private fun ExploreDiscoveryCategoryCard(")
            .substringBefore("private fun BoxScope.ExploreDiscoveryCategoryArtwork(")
        val artwork = source.substringAfter("private fun BoxScope.ExploreDiscoveryCategoryArtwork(")
            .substringBefore("private fun ExploreDiscoveryCategoryArtworkFallback(")
        assertTrue(category.contains("colors.surfaceContainerHigh"))
        assertTrue(category.contains("colors.onSurface"))
        assertTrue(category.contains("LevyraCardDesign.EditorialShape"))
        assertFalse(category.contains("Color.White.copy(alpha = 0.08f)"))
        assertTrue(artwork.contains("LevyraCardDesign.ArtworkShape"))
        assertFalse(artwork.contains(".rotate("))
    }

    @Test
    fun `library collections and mix controls share tonal expressive shapes`() {
        val library = readUi("library/LibraryOverviewComponents.kt")
            .substringAfter("private fun SmartCollectionShortcut(")
            .substringBefore("private val SmartCollectionShortcutHeight")
        val mix = readUi("LevyraMixPanel.kt")
            .substringAfter("private fun MixToolAction(")
            .substringBefore("private fun MixCrest(")
        assertTrue(library.contains("colors.surfaceContainerHigh"))
        assertTrue(library.contains("LevyraCardDesign.EditorialShape"))
        assertTrue(mix.contains("colors.surfaceContainerHigh"))
        assertTrue(mix.contains("heightIn(min = 56.dp)"))
        assertTrue(mix.contains("onClick = onClick"))
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
