package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject

const val MAX_LYRICS_OFFSET_MS = 5_000L
const val MAX_LYRICS_TRACK_OFFSET_MS = 30_000L
private const val MAX_LYRICS_DEVICE_PROFILES = 32
internal const val MAX_LYRICS_TRACK_PROFILES = 500

data class LyricsLatencyProfiles(
    val globalOffsetMs: Long = 0L,
    val deviceOffsetsMs: Map<String, Long> = emptyMap(),
    val trackOffsetsMs: Map<String, Long> = emptyMap()
) {
    fun resolve(routeKey: String?, bluetooth: Boolean): Long =
        if (bluetooth && !routeKey.isNullOrBlank()) {
            deviceOffsetsMs[routeKey] ?: globalOffsetMs
        } else {
            globalOffsetMs
        }

    fun trackOffsetMs(trackId: String?): Long =
        trackId?.let(::trackKey)?.let(trackOffsetsMs::get) ?: 0L

    fun effectiveOffsetMs(trackId: String?, routeKey: String?, bluetooth: Boolean): Long =
        resolve(routeKey, bluetooth) + trackOffsetMs(trackId)

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

    fun withLatencyOffset(routeKey: String?, bluetooth: Boolean, offsetMs: Long): LyricsLatencyProfiles =
        if (bluetooth && !routeKey.isNullOrBlank()) {
            withDeviceOffset(routeKey, offsetMs)
        } else {
            withGlobalOffset(offsetMs)
        }

    fun withTrackOffset(trackId: String, offsetMs: Long): LyricsLatencyProfiles {
        val key = trackKey(trackId) ?: return this
        val clamped = offsetMs.coerceIn(-MAX_LYRICS_TRACK_OFFSET_MS, MAX_LYRICS_TRACK_OFFSET_MS)
        if (trackOffsetsMs[key] == clamped.takeIf { it != 0L }) return this
        val updated = LinkedHashMap(trackOffsetsMs)
        updated.remove(key)
        if (clamped != 0L) updated[key] = clamped
        while (updated.size > MAX_LYRICS_TRACK_PROFILES) {
            updated.remove(updated.keys.first())
        }
        return copy(trackOffsetsMs = updated)
    }

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
        .put(
            "tracks",
            JSONArray().apply {
                trackOffsetsMs.forEach { (key, offset) ->
                    put(JSONObject().put("key", key).put("offset", offset))
                }
            }
        )
        .toString()

    companion object {
        private const val MAX_ROUTE_KEY_CHARS = 96
        private const val MAX_TRACK_KEY_CHARS = 160

        private fun trackKey(trackId: String): String? =
            trackId.trim().take(MAX_TRACK_KEY_CHARS).takeIf(String::isNotBlank)

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
                val tracks = LinkedHashMap<String, Long>()
                val trackArray = root.optJSONArray("tracks") ?: JSONArray()
                val firstTrack = (trackArray.length() - MAX_LYRICS_TRACK_PROFILES).coerceAtLeast(0)
                for (index in firstTrack until trackArray.length()) {
                    val item = trackArray.optJSONObject(index) ?: continue
                    val key = trackKey(item.optString("key")) ?: continue
                    val offset = item.optLong("offset")
                        .coerceIn(-MAX_LYRICS_TRACK_OFFSET_MS, MAX_LYRICS_TRACK_OFFSET_MS)
                    if (offset != 0L) tracks[key] = offset
                }
                LyricsLatencyProfiles(
                    globalOffsetMs = root.optLong("global").coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS),
                    deviceOffsetsMs = devices,
                    trackOffsetsMs = tracks
                )
            }.getOrDefault(LyricsLatencyProfiles())
        }
    }
}
