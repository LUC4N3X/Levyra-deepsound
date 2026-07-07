package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

class LevyraHomeSnapshotCache(context: Context) {
    private val root = File(context.applicationContext.filesDir, "levyra_home_snapshots").apply { mkdirs() }

    fun load(languageCode: String): LevyraHomeSnapshot? {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        val file = fileFor(normalized)
        if (!file.isFile || file.length() <= 0L) return null
        return runCatching {
            val rootJson = JSONObject(file.readText())
            if (rootJson.optInt("schema") != SCHEMA) return null
            val storedLanguage = LevyraLanguageCatalog.normalize(rootJson.optString("languageCode"))
            if (storedLanguage != normalized) return null
            val createdAt = rootJson.optLong("createdAt", 0L)
            val homeSections = parseHomeSections(rootJson.optJSONArray("homeSections") ?: JSONArray())
            val charts = parseTracks(rootJson.optJSONArray("charts") ?: JSONArray())
            val personalOrbit = parseTracks(rootJson.optJSONArray("personalOrbit") ?: JSONArray())
            if (homeSections.isEmpty() && charts.isEmpty() && personalOrbit.isEmpty()) return null
            LevyraHomeSnapshot(storedLanguage, createdAt, homeSections, charts, personalOrbit)
        }.onFailure { Timber.w(it, "Home snapshot restore failed") }.getOrNull()
    }

    fun save(languageCode: String, homeSections: List<HomeSection>, charts: List<Track>, personalOrbit: List<Track>) {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        if (homeSections.isEmpty() && charts.isEmpty() && personalOrbit.isEmpty()) return
        runCatching {
            val json = JSONObject()
                .put("schema", SCHEMA)
                .put("languageCode", normalized)
                .put("createdAt", System.currentTimeMillis())
                .put("homeSections", homeSectionsToJson(homeSections.take(12)))
                .put("charts", tracksToJson(charts.take(60)))
                .put("personalOrbit", tracksToJson(personalOrbit.take(40)))
            val target = fileFor(normalized)
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(json.toString())
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            cleanup(normalized)
        }.onFailure { Timber.w(it, "Home snapshot save failed") }
    }

    private fun fileFor(languageCode: String): File = File(root, "home_${LevyraLanguageCatalog.normalize(languageCode)}.json")

    private fun cleanup(activeLanguage: String) {
        root.listFiles()?.forEach { file ->
            if (!file.name.endsWith(".json") || file.name == "home_${activeLanguage}.json") return@forEach
            if (System.currentTimeMillis() - file.lastModified() > MAX_STALE_MS) runCatching { file.delete() }
        }
    }

    private fun homeSectionsToJson(sections: List<HomeSection>): JSONArray {
        val array = JSONArray()
        sections.forEach { section ->
            val tracks = tracksToJson(section.tracks.take(24))
            if (tracks.length() > 0) array.put(JSONObject().put("title", section.title).put("tracks", tracks))
        }
        return array
    }

    private fun tracksToJson(tracks: List<Track>): JSONArray {
        val array = JSONArray()
        tracks.filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() || it.title.isNotBlank() }.distinctBy { snapshotKey(it) }.forEach { track ->
            array.put(TrackJson.toJson(track.copy(streamUrl = "", videoStreamUrl = "")))
        }
        return array
    }

    private fun parseHomeSections(array: JSONArray): List<HomeSection> {
        val out = ArrayList<HomeSection>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val title = item.optString("title").ifBlank { "Per te" }
            val tracks = parseTracks(item.optJSONArray("tracks") ?: JSONArray())
            if (tracks.isNotEmpty()) out += HomeSection(title, tracks)
        }
        return out
    }

    private fun parseTracks(array: JSONArray): List<Track> {
        val out = LinkedHashMap<String, Track>()
        for (i in 0 until array.length()) {
            val track = runCatching { array.optJSONObject(i)?.let(TrackJson::fromJson) }.getOrNull() ?: continue
            val key = snapshotKey(track)
            if (key.isNotBlank() && !out.containsKey(key)) out[key] = track
        }
        return out.values.toList()
    }

    private fun snapshotKey(track: Track): String = track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}|${track.title}" } }.trim().lowercase()

    private companion object {
        const val SCHEMA = 2
        const val MAX_STALE_MS = 21L * 24L * 60L * 60L * 1000L
    }
}

data class LevyraHomeSnapshot(
    val languageCode: String,
    val createdAt: Long,
    val homeSections: List<HomeSection>,
    val charts: List<Track>,
    val personalOrbit: List<Track>
)
