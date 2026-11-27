package com.luc4n3x.levyra.desktop.core.model

import java.util.Locale
import org.junit.Test
import org.junit.Assert.assertEquals

class EqualizerExportTest {
    @Test
    fun graphicEqListsEveryBandWithPreampApplied() {
        val settings = EqualizerSettings(enabled = true, preamp = -3f, amps = listOf(6f, 4.25f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 3f))
        assertEquals(
            "GraphicEQ: 60 3.0; 170 1.3; 310 -3.0; 600 -3.0; 1000 -3.0; 3000 -3.0; 6000 -3.0; 12000 -3.0; 14000 -3.0; 16000 0.0\n",
            settings.toGraphicEq()
        )
    }

    @Test
    fun graphicEqIgnoresDeviceLocaleAndNegativeZero() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val text = EqualizerSettings(preamp = -0.01f).toGraphicEq()
            assertEquals(
                "GraphicEQ: 60 0.0; 170 0.0; 310 0.0; 600 0.0; 1000 0.0; 3000 0.0; 6000 0.0; 12000 0.0; 14000 0.0; 16000 0.0\n",
                text
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun outputLatencyOffsetIsBoundedAndDefaultsToZero() {
        assertEquals(0, DesktopSettings().outputLatencyOffsetMs)
        assertEquals(5_000, DesktopSettings(outputLatencyOffsetMs = 9_000).sanitized().outputLatencyOffsetMs)
        assertEquals(-5_000, DesktopSettings(outputLatencyOffsetMs = -9_000).sanitized().outputLatencyOffsetMs)
        assertEquals(120, DesktopSettings(outputLatencyOffsetMs = 120).sanitized().outputLatencyOffsetMs)
    }
}
