package com.luc4n3x.levyra.data.hqaudio

import android.content.Context
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import org.json.JSONObject

interface HighQualityMappingStorage {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
    fun keys(): Set<String>
}

internal class SharedPreferencesMappingStorage(context: Context) : HighQualityMappingStorage {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun keys(): Set<String> = preferences.all.keys.toSet()

    companion object {
        const val PREFERENCES_NAME = "levyra_hq_audio_mappings"
    }
}

data class StoredAlternativeMapping(
    val providerId: String,
    val providerTrackId: String,
    val queryFingerprint: String,
    val candidateFingerprint: String,
    val verdict: AlternativeMatchVerdict,
    val confidence: Int,
    val storedAtMs: Long
)

class HighQualityMappingStore(
    private val storage: HighQualityMappingStorage,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private val lock = Any()

    fun load(identityKey: String, providerId: String, queryFingerprint: String): StoredAlternativeMapping? =
        synchronized(lock) {
            val key = storageKey(identityKey, providerId)
            val raw = storage.read(key) ?: return migrateLegacy(identityKey, providerId, queryFingerprint)
            val mapping = decode(raw)
            if (mapping == null || mapping.providerId != providerId || !usable(mapping, queryFingerprint)) {
                storage.remove(key)
                return null
            }
            mapping
        }

    fun save(identityKey: String, mapping: StoredAlternativeMapping): Boolean = synchronized(lock) {
        if (!isPersistable(mapping)) return false
        val key = storageKey(identityKey, mapping.providerId)
        evictBeforeInsert(key)
        storage.write(key, encode(mapping))
        true
    }

    fun remove(identityKey: String, providerId: String) {
        synchronized(lock) {
            storage.remove(storageKey(identityKey, providerId))
            val legacyKey = legacyStorageKey(identityKey)
            if (storage.read(legacyKey)?.let(::decode)?.providerId == providerId) storage.remove(legacyKey)
        }
    }

    private fun migrateLegacy(identityKey: String, providerId: String, queryFingerprint: String): StoredAlternativeMapping? {
        val legacyKey = legacyStorageKey(identityKey)
        val raw = storage.read(legacyKey) ?: return null
        val mapping = decode(raw)
        if (mapping == null || !usable(mapping, queryFingerprint)) {
            storage.remove(legacyKey)
            return null
        }
        if (mapping.providerId != providerId) return null
        storage.remove(legacyKey)
        storage.write(storageKey(identityKey, providerId), encode(mapping))
        return mapping
    }

    private fun usable(mapping: StoredAlternativeMapping, queryFingerprint: String): Boolean {
        val now = clock()
        return mapping.queryFingerprint == queryFingerprint &&
            now - mapping.storedAtMs <= ttlMs &&
            mapping.storedAtMs <= now + CLOCK_SKEW_TOLERANCE_MS &&
            isPersistable(mapping)
    }

    private fun evictBeforeInsert(incomingKey: String) {
        val keys = storage.keys()
        if (incomingKey in keys || keys.size < maxEntries) return
        keys
            .map { key -> key to (storage.read(key)?.let(::decode)?.storedAtMs ?: Long.MIN_VALUE) }
            .sortedBy { it.second }
            .take(keys.size - maxEntries + 1)
            .forEach { storage.remove(it.first) }
    }

    private fun encode(mapping: StoredAlternativeMapping): String = JSONObject()
        .put("schema", SCHEMA_VERSION)
        .put("providerId", mapping.providerId)
        .put("providerTrackId", mapping.providerTrackId)
        .put("queryFingerprint", mapping.queryFingerprint)
        .put("candidateFingerprint", mapping.candidateFingerprint)
        .put("verdict", mapping.verdict.name)
        .put("confidence", mapping.confidence)
        .put("storedAtMs", mapping.storedAtMs)
        .toString()

    private fun decode(raw: String): StoredAlternativeMapping? = runCatching {
        val json = JSONObject(raw)
        if (json.optInt("schema", -1) !in SUPPORTED_SCHEMAS) return@runCatching null
        val verdict = AlternativeMatchVerdict.entries.firstOrNull { it.name == json.optString("verdict") }
            ?: return@runCatching null
        StoredAlternativeMapping(
            providerId = json.optString("providerId"),
            providerTrackId = json.optString("providerTrackId"),
            queryFingerprint = json.optString("queryFingerprint"),
            candidateFingerprint = json.optString("candidateFingerprint"),
            verdict = verdict,
            confidence = json.optInt("confidence", 0),
            storedAtMs = json.optLong("storedAtMs", 0L)
        ).takeIf { it.providerId.isNotBlank() && it.providerTrackId.isNotBlank() }
    }.getOrNull()

    private fun storageKey(identityKey: String, providerId: String): String =
        "hq-v2:${providerKeySegment(providerId)}:${sha256(identityKey).take(40)}"

    private fun legacyStorageKey(identityKey: String): String = "hq-v1:${sha256(identityKey).take(40)}"

    private fun providerKeySegment(providerId: String): String =
        providerId.lowercase(Locale.ROOT).filter { it in 'a'..'z' || it in '0'..'9' || it == '-' }.take(24)

    companion object {
        const val DEFAULT_MAX_ENTRIES = 400
        const val DEFAULT_TTL_MS = 30L * 24L * 60L * 60L * 1_000L
        private const val CLOCK_SKEW_TOLERANCE_MS = 5L * 60L * 1_000L
        private const val SCHEMA_VERSION = 2
        private val SUPPORTED_SCHEMAS = setOf(1, SCHEMA_VERSION)

        fun isPersistable(mapping: StoredAlternativeMapping): Boolean =
            mapping.verdict == AlternativeMatchVerdict.EXACT ||
                (mapping.verdict == AlternativeMatchVerdict.HIGH &&
                    mapping.confidence >= AlternativeTrackMatcher.PERSISTABLE_CONFIDENCE)
    }
}

internal object AlternativeTrackFingerprint {
    fun of(query: AlternativeTrackQuery): String {
        val title = AlternativeTrackText.title(query.title)
        return sha256(
            listOf(
                "query-v1",
                title.core,
                title.versionSignature.sorted().joinToString(","),
                AlternativeTrackText.artistCredit(query.artist).primary,
                AlternativeTrackText.album(query.album).core,
                (query.durationMs / 1_000L).toString(),
                query.explicit?.toString() ?: "unknown",
                query.isrc.trim().uppercase()
            ).joinToString("|")
        )
    }

    fun of(candidate: AlternativeTrackCandidate): String = sha256(
        listOf(
            "candidate-v1",
            candidate.providerId,
            candidate.providerTrackId,
            AlternativeTrackText.title(candidate.title).fullNormalized,
            candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).joinToString(","),
            AlternativeTrackText.normalize(candidate.album),
            candidate.durationSeconds.toString(),
            candidate.explicit?.toString() ?: "unknown"
        ).joinToString("|")
    )
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
