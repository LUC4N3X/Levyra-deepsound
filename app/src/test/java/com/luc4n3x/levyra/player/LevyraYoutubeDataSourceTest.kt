package com.luc4n3x.levyra.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraYoutubeDataSourceTest {
    @Test
    fun signedGoogleVideoUrlIsNeverRewritten() {
        val source = Files.readString(sourceFile())

        assertTrue(source.contains(".withAdditionalHeaders"))
        assertFalse(source.contains("withUri("))
        assertFalse(source.contains("appendQueryParameter"))
        assertFalse(source.contains("range="))
    }

    private fun sourceFile(): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/LevyraYoutubeDataSource.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/LevyraYoutubeDataSource.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraYoutubeDataSource.kt not found")
    }
}
