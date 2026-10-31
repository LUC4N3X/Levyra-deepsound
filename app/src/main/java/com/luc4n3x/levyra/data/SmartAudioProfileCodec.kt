package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.NightListeningPreset
import com.luc4n3x.levyra.domain.ReplayGainMode
import com.luc4n3x.levyra.domain.SmartAudioDeviceCategory
import com.luc4n3x.levyra.domain.SmartAudioEqMode
import com.luc4n3x.levyra.domain.SmartAudioNightMode
import com.luc4n3x.levyra.domain.SmartAudioProfile
import org.json.JSONArray
import org.json.JSONObject

internal fun smartAudioProfileToJson(profile: SmartAudioProfile): JSONObject = JSONObject()
    .put("deviceKey", profile.deviceKey)
    .put("deviceName", profile.deviceName)
    .put("category", profile.category.storageValue)
    .put("eqMode", profile.eqMode.storageValue)
    .put("presetId", profile.presetId)
    .put("presetLabel", profile.presetLabel)
    .put("bandLevels", JSONArray(profile.bandLevels))
    .put("bassBoost", profile.bassBoost)
    .put("preampDb", profile.preampDb.toDouble())
    .put("parametricProfile", profile.parametricProfile?.let(::parametricProfileToJson))
    .put("overrideReplayGain", profile.overrideReplayGain)
    .put("replayGainMode", profile.replayGainMode.storageValue)
    .put("replayGainPreampDb", profile.replayGainPreampDb.toDouble())
    .put("nightMode", profile.nightMode.storageValue)
    .put("nightPreset", profile.nightPreset.storageValue)

internal fun smartAudioProfileFromJson(json: JSONObject): SmartAudioProfile? {
    val levels = json.optJSONArray("bandLevels")?.let { array ->
        List(array.length()) { index -> array.optInt(index) }
    } ?: LevyraAudioPresets.flatLevels
    return SmartAudioProfile(
        deviceKey = json.optString("deviceKey"),
        deviceName = json.optString("deviceName"),
        category = SmartAudioDeviceCategory.fromStorage(json.optString("category")),
        eqMode = SmartAudioEqMode.fromStorage(json.optString("eqMode")),
        presetId = json.optString("presetId", LevyraAudioPresets.FLAT),
        presetLabel = json.optString("presetLabel"),
        bandLevels = levels,
        bassBoost = json.optInt("bassBoost"),
        preampDb = json.optDouble("preampDb", 0.0).toFloat(),
        parametricProfile = json.optJSONObject("parametricProfile")?.let(::parametricProfileFromJson),
        overrideReplayGain = json.optBoolean("overrideReplayGain"),
        replayGainMode = ReplayGainMode.fromStorage(json.optString("replayGainMode")),
        replayGainPreampDb = json.optDouble("replayGainPreampDb", 0.0).toFloat(),
        nightMode = SmartAudioNightMode.fromStorage(json.optString("nightMode")),
        nightPreset = NightListeningPreset.fromStorage(json.optString("nightPreset"))
    ).normalized()
}

internal fun smartAudioProfilesToJson(profiles: List<SmartAudioProfile>): String =
    JSONArray().apply { profiles.forEach { put(smartAudioProfileToJson(it)) } }.toString()

internal fun smartAudioProfilesFromJson(value: String): List<SmartAudioProfile> = runCatching {
    if (value.isBlank()) return emptyList()
    val array = JSONArray(value)
    (0 until array.length())
        .mapNotNull { index -> array.optJSONObject(index)?.let(::smartAudioProfileFromJson) }
        .distinctBy(SmartAudioProfile::deviceKey)
        .takeLast(SmartAudioProfile.MAX_PROFILES)
}.getOrDefault(emptyList())
