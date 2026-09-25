package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.SmartOrbitCandidate
import com.luc4n3x.levyra.domain.SmartOrbitEngine
import com.luc4n3x.levyra.domain.SmartOrbitPool
import com.luc4n3x.levyra.domain.Track
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class SmartOrbitStore(context: Context) {
    private val lock = Any()
    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private var cached: SmartOrbitPool? = null

    fun load(nowMs: Long = System.currentTimeMillis()): SmartOrbitPool = synchronized(lock) {
        SmartOrbitEngine.prune(cached ?: read(), nowMs).also { cached = it }
    }

    fun recordRelated(
        seed: Track,
        related: List<Track>,
        nowMs: Long = System.currentTimeMillis()
    ): SmartOrbitPool = synchronized(lock) {
        val current = cached ?: SmartOrbitEngine.prune(read(), nowMs)
        val updated = SmartOrbitEngine.accumulate(current, seed, related, nowMs)
        cached = updated
        if (updated != current) write(updated)
        updated
    }

    private fun read(): SmartOrbitPool {
        if (!file.isFile) return SmartOrbitPool.Empty
        return runCatching { decodeSmartOrbitPool(file.readText()) }
            .onFailure { Timber.w(it, "Smart Orbit restore failed") }
            .getOrDefault(SmartOrbitPool.Empty)
    }

    private fun write(pool: SmartOrbitPool) {
        runCatching {
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(encodeSmartOrbitPool(pool))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }.onFailure { Timber.w(it, "Smart Orbit save failed") }
    }

    private companion object {
        const val FILE_NAME = "levyra_smart_orbit.json"
    }
}

private const val SMART_ORBIT_FORMAT_VERSION = 1

internal fun encodeSmartOrbitPool(pool: SmartOrbitPool): String {
    val candidates = JSONArray()
    pool.candidates.forEach { candidate ->
        candidates.put(
            JSONObject()
                .put("track", TrackPayloadCodec.encode(candidate.track))
                .put("seeds", JSONArray(candidate.seedKeys))
                .put("lastSeenAt", candidate.lastSeenAt)
        )
    }
    return JSONObject()
        .put("version", SMART_ORBIT_FORMAT_VERSION)
        .put("candidates", candidates)
        .toString()
}

internal fun decodeSmartOrbitPool(text: String): SmartOrbitPool {
    val root = JSONObject(text)
    if (root.optInt("version") != SMART_ORBIT_FORMAT_VERSION) return SmartOrbitPool.Empty
    val array = root.optJSONArray("candidates") ?: return SmartOrbitPool.Empty
    val boundedLength = minOf(array.length(), SmartOrbitEngine.MAX_CANDIDATES)
    val candidates = ArrayList<SmartOrbitCandidate>(boundedLength)
    for (index in 0 until boundedLength) {
        val item = array.optJSONObject(index) ?: continue
        val track = TrackPayloadCodec.decode(item.optString("track")) ?: continue
        val seedsArray = item.optJSONArray("seeds") ?: continue
        val seeds = buildList {
            for (seedIndex in 0 until minOf(seedsArray.length(), SmartOrbitEngine.MAX_SEEDS_PER_CANDIDATE)) {
                seedsArray.optString(seedIndex).trim().takeIf(String::isNotEmpty)?.let(::add)
            }
        }.distinct()
        if (seeds.isEmpty()) continue
        candidates += SmartOrbitCandidate(track = track, seedKeys = seeds, lastSeenAt = item.optLong("lastSeenAt"))
    }
    return SmartOrbitPool(candidates.distinctBy { it.key })
}
