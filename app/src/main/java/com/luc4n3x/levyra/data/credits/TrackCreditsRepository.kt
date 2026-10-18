package com.luc4n3x.levyra.data.credits

import android.content.Context
import com.luc4n3x.levyra.data.apple.AppleMetadataEnricher
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class TrackCreditsRepository private constructor(context: Context) {
    private val enricher = AppleMetadataEnricher(context.applicationContext)
    private val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIRECTORY)
    private val memoryGuard = Any()
    private val memory = object : LinkedHashMap<String, CachedCredits>(MEMORY_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedCredits>): Boolean =
            size > MEMORY_ENTRIES
    }
    private val keyLocks = Array(8) { Mutex() }

    suspend fun load(track: Track, retry: Boolean = false): TrackCredits = withContext(Dispatchers.IO) {
        val local = TrackCreditsParser.localFallback(track)
        val key = cacheKey(track)
        val lock = keyLocks[(key.hashCode() and Int.MAX_VALUE) % keyLocks.size]
        val remote = lock.withLock {
            val now = System.currentTimeMillis()
            readMemory(key, now)?.takeUnless { retry && it.credits == null }?.let { return@withLock it.credits }
            readDisk(key, now)?.let { cached ->
                writeMemory(key, CachedCredits(cached, now + REMOTE_TTL_MS))
                return@withLock cached
            }
            val resolved = withTimeoutOrNull(REMOTE_TIMEOUT_MS) { resolveRemote(track) }
            when {
                resolved != null -> {
                    writeMemory(key, CachedCredits(resolved, now + REMOTE_TTL_MS))
                    writeDisk(key, resolved)
                }
                else -> writeMemory(key, CachedCredits(null, now + MISS_TTL_MS))
            }
            resolved
        }
        if (remote == null) local else TrackCreditsParser.merge(remote, local)
    }

    private suspend fun resolveRemote(track: Track): TrackCredits? = try {
        val match = enricher.matchMetadata(track, timeoutMs = MATCH_TIMEOUT_MS)
        val payload = match?.songId?.takeIf(String::isNotBlank)?.let { enricher.fetchSongCredits(it) }
        payload?.let { TrackCreditsParser.parseApple(it.song, it.credits, match.isReleaseMatch) }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.d(error, "Track credits lookup failed")
        null
    }

    private fun readMemory(key: String, now: Long): CachedCredits? = synchronized(memoryGuard) {
        val cached = memory[key] ?: return@synchronized null
        if (cached.expiresAt > now) cached else {
            memory.remove(key)
            null
        }
    }

    private fun writeMemory(key: String, value: CachedCredits) = synchronized(memoryGuard) {
        memory[key] = value
    }

    private fun readDisk(key: String, now: Long): TrackCredits? {
        val file = File(cacheDir, fileName(key))
        if (!file.isFile) return null
        if (now - file.lastModified() > REMOTE_TTL_MS) {
            file.delete()
            return null
        }
        return runCatching { decode(JSONObject(file.readText())) }.getOrElse {
            file.delete()
            null
        }
    }

    private fun writeDisk(key: String, credits: TrackCredits) {
        runCatching {
            if (!cacheDir.isDirectory) cacheDir.mkdirs()
            val target = File(cacheDir, fileName(key))
            val temp = File(cacheDir, target.name + ".tmp")
            temp.writeText(encode(credits).toString())
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
            pruneDisk()
        }
    }

    private fun pruneDisk() {
        val files = cacheDir.listFiles { file -> file.isFile && file.name.endsWith(CACHE_EXTENSION) } ?: return
        if (files.size <= DISK_ENTRIES) return
        files.sortedBy(File::lastModified).take(files.size - DISK_ENTRIES).forEach(File::delete)
    }

    private fun encode(credits: TrackCredits): JSONObject = JSONObject()
        .put("version", CACHE_VERSION)
        .put("provider", credits.provider)
        .put("sections", JSONArray().apply {
            credits.sections.forEach { section ->
                put(
                    JSONObject()
                        .put("group", section.group.name)
                        .put("title", section.title)
                        .put("people", JSONArray().apply {
                            section.people.forEach { person ->
                                put(JSONObject().put("name", person.name).put("roles", JSONArray(person.roles)))
                            }
                        })
                )
            }
        })
        .put("release", JSONObject()
            .put("album", credits.release.album)
            .put("releaseDate", credits.release.releaseDate)
            .put("label", credits.release.label)
            .put("phonographicCopyright", credits.release.phonographicCopyright)
            .put("copyright", credits.release.copyright)
            .put("isrc", credits.release.isrc)
            .put("upc", credits.release.upc))

    private fun decode(json: JSONObject): TrackCredits? {
        if (json.optInt("version") != CACHE_VERSION) return null
        val sectionsJson = json.optJSONArray("sections") ?: JSONArray()
        val sections = (0 until sectionsJson.length()).mapNotNull { index ->
            val section = sectionsJson.optJSONObject(index) ?: return@mapNotNull null
            val group = TrackCreditGroup.entries.firstOrNull { it.name == section.optString("group") }
                ?: return@mapNotNull null
            val peopleJson = section.optJSONArray("people") ?: JSONArray()
            val people = (0 until peopleJson.length()).mapNotNull { personIndex ->
                val person = peopleJson.optJSONObject(personIndex) ?: return@mapNotNull null
                val name = person.optString("name").trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
                val rolesJson = person.optJSONArray("roles") ?: JSONArray()
                TrackCreditPerson(
                    name = name,
                    roles = (0 until rolesJson.length()).mapNotNull { rolesJson.optString(it).trim().takeIf(String::isNotBlank) }
                )
            }
            if (people.isEmpty()) null else TrackCreditSection(group, section.optString("title"), people)
        }
        val release = json.optJSONObject("release") ?: JSONObject()
        return TrackCredits(
            sections = sections,
            release = TrackReleaseCredits(
                album = release.optString("album"),
                releaseDate = release.optString("releaseDate"),
                label = release.optString("label"),
                phonographicCopyright = release.optString("phonographicCopyright"),
                copyright = release.optString("copyright"),
                isrc = release.optString("isrc"),
                upc = release.optString("upc")
            ),
            provider = json.optString("provider"),
            remoteResolved = true
        )
    }

    private fun cacheKey(track: Track): String = listOf(
        track.title.trim().lowercase(Locale.ROOT),
        track.artist.trim().lowercase(Locale.ROOT),
        track.album.trim().lowercase(Locale.ROOT),
        track.isrc.trim().lowercase(Locale.ROOT),
        (track.durationMs / 5_000L).toString(),
        Locale.getDefault().country.uppercase(Locale.ROOT)
    ).joinToString("|")

    private fun fileName(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + CACHE_EXTENSION
    }

    private class CachedCredits(val credits: TrackCredits?, val expiresAt: Long)

    companion object {
        private const val CACHE_DIRECTORY = "track-credits"
        private const val CACHE_EXTENSION = ".json"
        private const val CACHE_VERSION = 1
        private const val MEMORY_ENTRIES = 48
        private const val DISK_ENTRIES = 300
        private const val REMOTE_TIMEOUT_MS = 12_000L
        private const val MATCH_TIMEOUT_MS = 8_000L
        private const val REMOTE_TTL_MS = 14L * 24 * 60 * 60 * 1000
        private const val MISS_TTL_MS = 5L * 60 * 1000

        @Volatile
        private var instance: TrackCreditsRepository? = null

        fun get(context: Context): TrackCreditsRepository =
            instance ?: synchronized(this) {
                instance ?: TrackCreditsRepository(context.applicationContext).also { instance = it }
            }
    }
}
