package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialExpressiveContractTest {

    @Test
    fun `theme keeps expressive motion with a reduced motion fallback`() {
        val theme = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/theme/LevyraTheme.kt",
            "src/main/java/com/luc4n3x/levyra/ui/theme/LevyraTheme.kt"
        )

        assertTrue(theme.contains("MaterialExpressiveTheme("))
        assertTrue(theme.contains("MotionScheme.expressive()"))
        assertTrue(theme.contains("LevyraStaticMotionScheme"))
        assertTrue(theme.contains("snap()"))
        assertTrue(theme.contains("largeIncreased = RoundedCornerShape(28.dp)"))
        assertTrue(theme.contains("extraLargeIncreased = RoundedCornerShape(36.dp)"))
        assertTrue(theme.contains("extraExtraLarge = RoundedCornerShape(44.dp)"))
    }

    @Test
    fun `shared icon controls use native material shape morphing`() {
        val expressive = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/components/LevyraExpressive.kt",
            "src/main/java/com/luc4n3x/levyra/ui/components/LevyraExpressive.kt"
        )

        assertTrue(expressive.contains("LevyraExpressiveIconButton("))
        assertTrue(expressive.contains("IconButtonDefaults.shapes("))
        assertTrue(expressive.contains("shape = MaterialTheme.shapes.extraLarge"))
        assertTrue(expressive.contains("pressedShape = MaterialTheme.shapes.medium"))
    }

    @Test
    fun `high visibility action controls share the expressive icon primitive`() {
        val discovery = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/HomeExploreEditorial.kt",
            "src/main/java/com/luc4n3x/levyra/ui/HomeExploreEditorial.kt"
        )
        val library = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryPlaylistShelf.kt",
            "src/main/java/com/luc4n3x/levyra/ui/library/LibraryPlaylistShelf.kt"
        )
        val selection = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/selection/TrackSelectionBar.kt",
            "src/main/java/com/luc4n3x/levyra/ui/selection/TrackSelectionBar.kt"
        )
        val matching = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/playlistimport/PlaylistChangeMatchSheet.kt",
            "src/main/java/com/luc4n3x/levyra/ui/playlistimport/PlaylistChangeMatchSheet.kt"
        )

        assertTrue(discovery.contains("LevyraExpressiveIconButton("))
        assertTrue(library.contains("LevyraExpressiveIconButton("))
        assertTrue(selection.contains("LevyraExpressiveIconButton("))
        assertTrue(matching.contains("LevyraExpressiveIconButton("))
        assertTrue(matching.contains("LevyraLoadingIndicator("))
    }

    @Test
    fun `home and library filters use expressive shape changes with accessible targets`() {
        val home = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/HomeExperience.kt",
            "src/main/java/com/luc4n3x/levyra/ui/HomeExperience.kt"
        )
        val localLibrary = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/library/LocalLibraryPane.kt",
            "src/main/java/com/luc4n3x/levyra/ui/library/LocalLibraryPane.kt"
        )
        val matching = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/playlistimport/PlaylistChangeMatchSheet.kt",
            "src/main/java/com/luc4n3x/levyra/ui/playlistimport/PlaylistChangeMatchSheet.kt"
        )

        assertTrue(home.contains("levyraExpressiveCorner("))
        assertTrue(home.contains(".heightIn(min = 48.dp)"))
        assertTrue(localLibrary.contains("FilterChipDefaults.shapes("))
        assertTrue(localLibrary.contains("selectedShape = MaterialTheme.shapes.extraLarge"))
        assertTrue(localLibrary.contains("Modifier.heightIn(min = 48.dp)"))
        assertTrue(matching.contains("FilterChipDefaults.shapes("))
    }

    @Test
    fun `mix slider follows the stateful material3 beta api`() {
        val mix = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/LevyraMixPanel.kt",
            "src/main/java/com/luc4n3x/levyra/ui/LevyraMixPanel.kt"
        )

        assertTrue(mix.contains("rememberSliderState("))
        assertTrue(mix.contains("state = sliderState"))
        assertTrue(mix.contains("sliderState.value = value"))
    }

    @Test
    fun `explore screen components preserve expressive styling contracts`() {
        val mix = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/LevyraMixPanel.kt",
            "src/main/java/com/luc4n3x/levyra/ui/LevyraMixPanel.kt"
        )
        val fresh = source(
            "app/src/main/java/com/luc4n3x/levyra/ui/ExploreFreshCurrents.kt",
            "src/main/java/com/luc4n3x/levyra/ui/ExploreFreshCurrents.kt"
        )

        assertTrue(mix.contains("SonicReactorSpectrum("))
        assertTrue(mix.contains("MixPresetChip("))
        assertTrue(fresh.contains("FreshArtworkCorner = 36.dp"))
    }

    @Test
    fun `material3 expressive uses the current beta api surface`() {
        val versions = source(
            "gradle/libs.versions.toml",
            "../gradle/libs.versions.toml"
        )

        assertTrue(versions.contains("material3Expressive = \"1.5.0-beta01\""))
    }

    private fun source(vararg candidates: String): String {
        val path = candidates.asSequence()
            .map(Path::of)
            .firstOrNull(Files::exists)
            ?: error("Source not found: ${candidates.joinToString()}")
        return Files.readString(path)
    }
}
