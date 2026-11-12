package com.luc4n3x.levyra.diagnostics

import com.luc4n3x.levyra.data.backupInterfaceSettingsFromJson
import com.luc4n3x.levyra.data.backupInterfaceSettingsToJson
import com.luc4n3x.levyra.domain.FloatingNavSide
import com.luc4n3x.levyra.domain.LevyraFloatingNavSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavSettingsTest {
    @Test fun defaultsAreOffOnTheRightAtTheDefaultAnchor() {
        val defaults = LevyraInterfaceSettings().floatingNav
        assertFalse(defaults.enabled)
        assertEquals(FloatingNavSide.Right, defaults.side)
        assertEquals(0.42f, defaults.anchor, 0f)
    }

    @Test fun normalizedClampsAnchorAndReplacesNonFiniteValues() {
        assertEquals(1f, LevyraFloatingNavSettings(anchor = 1.7f).normalized().anchor, 0f)
        assertEquals(0f, LevyraFloatingNavSettings(anchor = -0.3f).normalized().anchor, 0f)
        assertEquals(LevyraFloatingNavSettings.DEFAULT_ANCHOR, LevyraFloatingNavSettings(anchor = Float.NaN).normalized().anchor, 0f)
        assertEquals(
            1f,
            LevyraInterfaceSettings(floatingNav = LevyraFloatingNavSettings(anchor = 1.7f)).normalized().floatingNav.anchor,
            0f
        )
    }

    @Test fun sideParsesStorageIdsAndFallsBackToRight() {
        assertEquals(FloatingNavSide.Left, FloatingNavSide.from("left"))
        assertEquals(FloatingNavSide.Right, FloatingNavSide.from("right"))
        assertEquals(FloatingNavSide.Right, FloatingNavSide.from("middle"))
        assertEquals(FloatingNavSide.Right, FloatingNavSide.from(null))
    }

    @Test fun backupRoundTripKeepsFloatingNavAndLegacyBackupsUseDefaults() {
        val original = LevyraInterfaceSettings(
            floatingNav = LevyraFloatingNavSettings(enabled = true, side = FloatingNavSide.Left, anchor = 0.8f)
        )
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original)).floatingNav
        assertTrue(restored.enabled)
        assertEquals(FloatingNavSide.Left, restored.side)
        assertEquals(0.8f, restored.anchor, 0.0001f)
        assertEquals(LevyraFloatingNavSettings(), backupInterfaceSettingsFromJson(JSONObject()).floatingNav)
    }
}
