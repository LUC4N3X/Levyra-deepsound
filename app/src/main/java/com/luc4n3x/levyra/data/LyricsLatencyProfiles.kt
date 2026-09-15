package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject

const val MAX_LYRICS_OFFSET_MS = 5_000L
private const val MAX_LYRICS_DEVICE_PROFILES = 32

data class LyricsLatencyProfiles(
    val globalOffsetMs: Long = 0L,
    val deviceOffsetsMs: Map<String, Long> = emptyMap()
) {
    fun resolve(routeKey: String?, bluetooth: Boolean): Long =
        if (bluetooth && !routeKey.isNullOrBlank()) {
            deviceOffsetsMs[routeKey] ?: globalOffsetMs
        } else {
            globalOffsetMs
        }

    fun withGlobalOffset(offsetMs: Long): LyricsLatencyProfiles =
        copy(globalOffsetMs = offsetMs.coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS))

    fun withDeviceOffset(routeKey: String, offsetMs: Long): LyricsLatencyProfiles {
        val key = routeKey.trim().take(MAX_ROUTE_KEY_CHARS)
        if (key.isBlank()) return this
        val updated = LinkedHashMap(deviceOffsetsMs)
        updated.remove(key)
        updated[key] = offsetMs.coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
        while (updated.size > MAX_LYRICS_DEVICE_PROFILES) {
            updated.remove(updated.keys.first())
        }
        return copy(deviceOffsetsMs = updated)
    }

    fun withoutDevice(routeKey: String): LyricsLatencyProfiles =
        copy(deviceOffsetsMs = deviceOffsetsMs - routeKey)

    internal fun encode(): String = JSONObject()
        .put("global", globalOffsetMs)
        .put(
            "devices",
            JSONArray().apply {
                deviceOffsetsMs.forEach { (key, offset) ->
                    put(JSONObject().put("key", key).put("offset", offset))
                }
            }
        )
        .toString()

    companion object {
        private const val MAX_ROUTE_KEY_CHARS = 96

        internal fun decode(raw: String): LyricsLatencyProfiles {
            if (raw.isBlank()) return LyricsLatencyProfiles()
            return runCatching {
                val root = JSONObject(raw)
                val devices = LinkedHashMap<String, Long>()
                val array = root.optJSONArray("devices") ?: JSONArray()
                for (index in 0 until minOf(array.length(), MAX_LYRICS_DEVICE_PROFILES)) {
                    val item = array.optJSONObject(index) ?: continue
                    val key = item.optString("key").trim().take(MAX_ROUTE_KEY_CHARS)
                    if (key.isNotBlank()) {
                        devices[key] = item.optLong("offset").coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
                    }
                }
                LyricsLatencyProfiles(
                    globalOffsetMs = root.optLong("global").coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS),
                    deviceOffsetsMs = devices
                )
            }.getOrDefault(LyricsLatencyProfiles())
        }
    }
}
