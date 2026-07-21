package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.json.JSONArray
import org.json.JSONObject

object PlaybackManifestCodec {
    fun encode(manifest: ResolvedPlaybackManifest): String {
        val streams = JSONArray()
        manifest.compact().streams.forEach { stream ->
            streams.put(
                JSONObject()
                    .put("url", stream.url)
                    .put("kind", stream.kind.name)
                    .put("deliveryMethod", stream.deliveryMethod.name)
                    .put("container", stream.container)
                    .put("mimeType", stream.mimeType)
                    .put("codec", stream.codec)
                    .put("bitrate", stream.bitrate)
                    .put("averageBitrate", stream.averageBitrate)
                    .put("sampleRate", stream.sampleRate)
                    .put("bitDepth", stream.bitDepth)
                    .put("width", stream.width)
                    .put("height", stream.height)
                    .put("fps", stream.fps)
                    .put("itag", stream.itag)
                    .put("qualityLabel", stream.qualityLabel)
                    .put("expiresAtMs", stream.expiresAtMs)
                    .put("selected", stream.selected)
            )
        }
        return JSONObject()
            .put("schemaVersion", 1)
            .put("sourceVideoId", manifest.sourceVideoId)
            .put("provider", manifest.provider)
            .put("resolvedAtMs", manifest.resolvedAtMs)
            .put("expiresAtMs", manifest.expiresAtMs)
            .put("durationMs", manifest.durationMs)
            .put("selectedAudioUrl", manifest.selectedAudioUrl)
            .put("selectedVideoUrl", manifest.selectedVideoUrl)
            .put("loudnessDb", manifest.loudnessDb)
            .put("perceptualLoudnessDb", manifest.perceptualLoudnessDb)
            .put("streams", streams)
            .toString()
    }

    fun decode(raw: String): ResolvedPlaybackManifest? = runCatching {
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion", 0) != 1) return@runCatching null
        val streamArray = root.optJSONArray("streams") ?: JSONArray()
        val streams = buildList {
            for (index in 0 until streamArray.length()) {
                val json = streamArray.optJSONObject(index) ?: continue
                val url = json.optString("url")
                if (url.isBlank()) continue
                add(
                    PlaybackStreamDescriptor(
                        url = url,
                        kind = json.optEnum("kind", PlaybackStreamKind.AUDIO),
                        deliveryMethod = json.optEnum("deliveryMethod", PlaybackDeliveryMethod.UNKNOWN),
                        container = json.optString("container"),
                        mimeType = json.optString("mimeType"),
                        codec = json.optString("codec"),
                        bitrate = json.optInt("bitrate", 0),
                        averageBitrate = json.optInt("averageBitrate", 0),
                        sampleRate = json.optInt("sampleRate", 0),
                        bitDepth = json.optInt("bitDepth", 0),
                        width = json.optInt("width", 0),
                        height = json.optInt("height", 0),
                        fps = json.optInt("fps", 0),
                        itag = json.optInt("itag", -1),
                        qualityLabel = json.optString("qualityLabel"),
                        expiresAtMs = json.optLong("expiresAtMs", 0L),
                        selected = json.optBoolean("selected", false)
                    )
                )
            }
        }
        val selectedAudioUrl = root.optString("selectedAudioUrl")
        if (selectedAudioUrl.isBlank()) return@runCatching null
        ResolvedPlaybackManifest(
            sourceVideoId = root.optString("sourceVideoId"),
            provider = root.optString("provider"),
            resolvedAtMs = root.optLong("resolvedAtMs", 0L),
            expiresAtMs = root.optLong("expiresAtMs", 0L),
            durationMs = root.optLong("durationMs", 0L),
            selectedAudioUrl = selectedAudioUrl,
            selectedVideoUrl = root.optString("selectedVideoUrl"),
            streams = streams,
            loudnessDb = root.optNullableFloat("loudnessDb"),
            perceptualLoudnessDb = root.optNullableFloat("perceptualLoudnessDb")
        )
    }.getOrNull()
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
    return enumValues<T>().firstOrNull { it.name == optString(key) } ?: fallback
}

private fun JSONObject.optNullableFloat(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key, Double.NaN).takeIf { it.isFinite() }?.toFloat()
}
