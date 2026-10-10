package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupAudioQualityTest {
    @Test
    fun backupWithoutAudioQualityGetsTheHighDefault() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject()))
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", JSONObject.NULL)))
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "")))
    }

    @Test
    fun explicitBackupChoicesAreRestoredUnchanged() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "High")))
        assertEquals("Auto", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Auto")))
        assertEquals("Low", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Low")))
    }

    @Test
    fun corruptBackupValueFallsBackToTheHighDefault() {
        assertEquals("High", backupAudioQualityFromJson(JSONObject().put("audioQuality", "Ultra")))
    }
}
