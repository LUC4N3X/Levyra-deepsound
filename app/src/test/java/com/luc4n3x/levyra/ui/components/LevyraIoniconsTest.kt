package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraIoniconsTest {
    @Test
    fun buildsEveryBundledIcon() {
        val icons = listOf(
            LevyraIonicons.Play,
            LevyraIonicons.Pause,
            LevyraIonicons.SkipPrevious,
            LevyraIonicons.SkipNext,
            LevyraIonicons.Shuffle,
            LevyraIonicons.Repeat,
            LevyraIonicons.ChevronDown,
            LevyraIonicons.MoreVertical,
            LevyraIonicons.AddCircle,
            LevyraIonicons.Heart,
            LevyraIonicons.HeartOutline,
            LevyraIonicons.Download,
            LevyraIonicons.Queue,
            LevyraIonicons.Lyrics,
            LevyraIonicons.Timer,
            LevyraIonicons.Equalizer,
            LevyraIonicons.Device,
            LevyraIonicons.Settings,
            LevyraIonicons.Share
        )

        assertEquals(icons.size, icons.map { it.name }.distinct().size)
        icons.forEach { icon ->
            assertEquals(512f, icon.viewportWidth, 0f)
            assertEquals(512f, icon.viewportHeight, 0f)
            assertEquals(24f, icon.defaultWidth.value, 0f)
            assertEquals(24f, icon.defaultHeight.value, 0f)
            assertTrue(icon.name.startsWith("Ionicons."))
        }
    }
}
