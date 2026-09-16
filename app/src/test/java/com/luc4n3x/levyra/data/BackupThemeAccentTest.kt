package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupThemeAccentTest {

    @Test
    fun themeAccentRoundTripsThroughBackupJson() {
        val accent = 0xFF30D158.toInt()
        val json = JSONObject().putBackupThemeAccent(accent)

        assertEquals(accent, backupThemeAccentFromJson(json))
    }

    @Test
    fun missingThemeAccentFallsBackToPresetColor() {
        assertEquals(0, backupThemeAccentFromJson(JSONObject()))
    }
}
