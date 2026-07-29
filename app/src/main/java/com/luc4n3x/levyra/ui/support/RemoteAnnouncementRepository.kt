package com.luc4n3x.levyra.ui.support

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.net.URI
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

internal const val BUILT_IN_SUPPORT_ANNOUNCEMENT_ID = "github-star-2026-07"
internal const val LEVYRA_REPOSITORY_URL = "https://github.com/LUC4N3X/Levyra-deepsound"
internal const val REMOTE_ANNOUNCEMENTS_URL =
    "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/main/config/announcements.json"

private const val REMOTE_SCHEMA_VERSION = 1
private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1_000L
private const val STORE_NAME = "levyra_remote_announcements"
private const val KEY_CACHED_JSON = "cached_json"
private const val KEY_ETAG = "etag"
private const val KEY_LAST_SUCCESSFUL_CHECK = "last_successful_check"
private const val KEY_DISMISSED_IDS = "dismissed_ids"
private const val MAX_ANNOUNCEMENTS = 20
private const val MAX_DISMISSED_IDS = 200

internal enum class AnnouncementStyle {
    OPEN_SOURCE,
    INFO,
    UPDATE;

    companion object {
        fun from(value: String): AnnouncementStyle = when (value.trim().lowercase()) {
            "open_source", "opensource", "support" -> OPEN_SOURCE
            "update", "release" -> UPDATE
            else -> INFO
        }
    }
}

internal data class RemoteAnnouncement(
    val id: String,
    val enabled: Boolean,
    val priority: Int,
    val style: AnnouncementStyle,
    val minimumVersionCode: Int,
    val maximumVersionCode: Int?,
    val startAtMs: Long?,
    val endAtMs: Long?,
    val actionUrl: String?,
    val translations: Map<String, OpenSourceSupportCopy>
)

internal data class RemoteAnnouncementCatalog(
    val schemaVersion: Int,
    val announcements: List<RemoteAnnouncement>
)

internal data class RemoteAnnouncementPresentation(
    val id: String,
    val style: AnnouncementStyle,
    val copy: OpenSourceSupportCopy,
    val actionUrl: String?
)

internal object RemoteAnnouncementRules {
    private val safeActionHosts = setOf("github.com", "www.github.com")
    private val idPattern = Regex("[a-z0-9][a-z0-9._-]{0,63}")

    fun isValidId(id: String): Boolean = idPattern.matches(id)

    fun isSafeActionUrl(value: String): Boolean {
        return runCatching {
            val uri = URI(value)
            val host = uri.host.orEmpty().lowercase()
            uri.scheme.equals("https", ignoreCase = true) &&
                host in safeActionHosts &&
                uri.userInfo == null &&
                uri.fragment == null &&
                uri.path.orEmpty().startsWith("/LUC4N3X/")
        }.getOrDefault(false)
    }

    fun select(
        catalog: RemoteAnnouncementCatalog,
        languageCode: String,
        versionCode: Int,
        dismissedIds: Set<String>,
        nowMs: Long
    ): RemoteAnnouncementPresentation? {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        return catalog.announcements
            .asSequence()
            .filter { it.enabled }
            .filter { it.id !in dismissedIds }
            .filter { versionCode >= it.minimumVersionCode }
            .filter { it.maximumVersionCode == null || versionCode <= it.maximumVersionCode }
            .filter { it.startAtMs == null || nowMs >= it.startAtMs }
            .filter { it.endAtMs == null || nowMs <= it.endAtMs }
            .sortedWith(
                compareByDescending<RemoteAnnouncement> { it.priority }
                    .thenByDescending { it.startAtMs ?: Long.MIN_VALUE }
                    .thenBy { it.id }
            )
            .mapNotNull { announcement ->
                val copy = announcement.translations[normalizedLanguage]
                    ?: announcement.translations["en"]
                    ?: return@mapNotNull null
                RemoteAnnouncementPresentation(
                    id = announcement.id,
                    style = announcement.style,
                    copy = copy,
                    actionUrl = announcement.actionUrl
                )
            }
            .firstOrNull()
    }
}

internal object RemoteAnnouncementParser {
    fun parse(raw: String): RemoteAnnouncementCatalog? = runCatching {
        val root = JSONObject(raw)
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != REMOTE_SCHEMA_VERSION) return@runCatching null
        val source = root.optJSONArray("announcements") ?: return@runCatching null
        if (source.length() > MAX_ANNOUNCEMENTS) return@runCatching null
        val announcements = buildList {
            for (index in 0 until source.length()) {
                val item = source.optJSONObject(index) ?: continue
                parseAnnouncement(item)?.let(::add)
            }
        }
        RemoteAnnouncementCatalog(schemaVersion, announcements)
    }.getOrNull()

    private fun parseAnnouncement(item: JSONObject): RemoteAnnouncement? = runCatching {
        val id = item.optString("id").trim()
        if (!RemoteAnnouncementRules.isValidId(id)) return@runCatching null
        val enabled = item.optBoolean("enabled", false)
        val priority = item.optInt("priority", 0).coerceIn(0, 100)
        val style = AnnouncementStyle.from(item.optString("style"))
        val minimumVersionCode = item.optInt("minimumVersionCode", 1).coerceAtLeast(1)
        val maximumVersionCode = optionalPositiveInt(item, "maximumVersionCode")
        if (maximumVersionCode != null && maximumVersionCode < minimumVersionCode) return@runCatching null
        val startAtMs = optionalInstant(item, "startAt")
        val endAtMs = optionalInstant(item, "endAt")
        if (startAtMs != null && endAtMs != null && endAtMs < startAtMs) return@runCatching null
        val actionUrl = item.optString("actionUrl").trim().ifBlank { null }
        if (actionUrl != null && !RemoteAnnouncementRules.isSafeActionUrl(actionUrl)) return@runCatching null
        val translationsObject = item.optJSONObject("translations") ?: return@runCatching null
        val translations = linkedMapOf<String, OpenSourceSupportCopy>()
        val keys = translationsObject.keys()
        while (keys.hasNext()) {
            val sourceCode = keys.next()
            val normalized = LevyraLanguageCatalog.normalize(sourceCode)
            if (normalized !in LevyraLanguageCatalog.languages.map { it.code }.toSet()) continue
            val copyObject = translationsObject.optJSONObject(sourceCode) ?: continue
            parseCopy(copyObject, actionUrl != null)?.let { translations[normalized] = it }
        }
        if (translations["en"] == null) return@runCatching null
        RemoteAnnouncement(
            id = id,
            enabled = enabled,
            priority = priority,
            style = style,
            minimumVersionCode = minimumVersionCode,
            maximumVersionCode = maximumVersionCode,
            startAtMs = startAtMs,
            endAtMs = endAtMs,
            actionUrl = actionUrl,
            translations = translations
        )
    }.getOrNull()

    private fun parseCopy(item: JSONObject, actionRequired: Boolean): OpenSourceSupportCopy? {
        val badge = item.optString("badge").trim()
        val title = item.optString("title").trim()
        val body = item.optString("body").trim()
        val action = item.optString("action").trim()
        val dismiss = item.optString("dismiss").trim()
        if (badge.isBlank() || badge.length > 60) return null
        if (title.isBlank() || title.length > 140) return null
        if (body.length !in 20..1_200) return null
        if (actionRequired && action.isBlank()) return null
        if (action.length > 100) return null
        if (dismiss.isBlank() || dismiss.length > 80) return null
        return OpenSourceSupportCopy(
            badge = badge,
            title = title,
            body = body,
            starAction = action,
            continueAction = dismiss
        )
    }

    private fun optionalPositiveInt(item: JSONObject, key: String): Int? {
        if (!item.has(key) || item.isNull(key)) return null
        return item.optInt(key, -1).takeIf { it > 0 }
    }

    private fun optionalInstant(item: JSONObject, key: String): Long? {
        val value = item.optString(key).trim()
        if (value.isBlank()) return null
        return Instant.parse(value).toEpochMilli()
    }
}

internal class RemoteAnnouncementRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = RemoteAnnouncementStore(appContext)
    private val client = LevyraHttpClientFactory.feeds(appContext).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun resolve(
        languageCode: String,
        versionCode: Int = BuildConfig.VERSION_CODE,
        nowMs: Long = System.currentTimeMillis()
    ): RemoteAnnouncementPresentation? = withContext(Dispatchers.IO) {
        val cached = store.cachedCatalog()
        val remoteState = if (cached != null && !store.shouldRefresh(nowMs)) {
            CatalogState(cached, available = true)
        } else {
            refresh(cached, nowMs)
        }
        val dismissedIds = store.dismissedIds()
        val selected = remoteState.catalog?.let {
            RemoteAnnouncementRules.select(it, languageCode, versionCode, dismissedIds, nowMs)
        }
        selected ?: if (remoteState.available) null else builtInFallback(languageCode, dismissedIds)
    }

    fun markDismissed(id: String) {
        store.markDismissed(id)
    }

    private fun refresh(cached: RemoteAnnouncementCatalog?, nowMs: Long): CatalogState {
        val request = Request.Builder()
            .url(REMOTE_ANNOUNCEMENTS_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
            .apply { store.etag()?.takeIf { it.isNotBlank() }?.let { header("If-None-Match", it) } }
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 304 && cached != null -> {
                        store.markSuccessfullyChecked(nowMs)
                        CatalogState(cached, available = true)
                    }
                    response.isSuccessful -> {
                        val raw = response.body.string()
                        val parsed = RemoteAnnouncementParser.parse(raw)
                            ?: return@use CatalogState(cached, available = cached != null)
                        store.saveCatalog(raw, response.header("ETag"), nowMs)
                        CatalogState(parsed, available = true)
                    }
                    else -> CatalogState(cached, available = cached != null)
                }
            }
        }.getOrElse { CatalogState(cached, available = cached != null) }
    }

    private fun builtInFallback(
        languageCode: String,
        dismissedIds: Set<String>
    ): RemoteAnnouncementPresentation? {
        if (BUILT_IN_SUPPORT_ANNOUNCEMENT_ID in dismissedIds) return null
        return RemoteAnnouncementPresentation(
            id = BUILT_IN_SUPPORT_ANNOUNCEMENT_ID,
            style = AnnouncementStyle.OPEN_SOURCE,
            copy = OpenSourceSupportStrings.forCode(languageCode),
            actionUrl = LEVYRA_REPOSITORY_URL
        )
    }

    private data class CatalogState(
        val catalog: RemoteAnnouncementCatalog?,
        val available: Boolean
    )
}

private class RemoteAnnouncementStore(context: Context) {
    private val preferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    fun cachedCatalog(): RemoteAnnouncementCatalog? {
        val raw = preferences.getString(KEY_CACHED_JSON, null).orEmpty()
        return raw.takeIf { it.isNotBlank() }?.let(RemoteAnnouncementParser::parse)
    }

    fun etag(): String? = preferences.getString(KEY_ETAG, null)

    fun shouldRefresh(nowMs: Long): Boolean {
        val lastCheck = preferences.getLong(KEY_LAST_SUCCESSFUL_CHECK, 0L)
        return lastCheck <= 0L || nowMs - lastCheck >= CHECK_INTERVAL_MS
    }

    fun saveCatalog(raw: String, etag: String?, nowMs: Long) {
        preferences.edit()
            .putString(KEY_CACHED_JSON, raw)
            .putString(KEY_ETAG, etag.orEmpty())
            .putLong(KEY_LAST_SUCCESSFUL_CHECK, nowMs)
            .apply()
    }

    fun markSuccessfullyChecked(nowMs: Long) {
        preferences.edit().putLong(KEY_LAST_SUCCESSFUL_CHECK, nowMs).apply()
    }

    fun dismissedIds(): Set<String> =
        preferences.getStringSet(KEY_DISMISSED_IDS, emptySet()).orEmpty().toSet()

    fun markDismissed(id: String) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        val updated = dismissedIds().toMutableSet()
        if (updated.size >= MAX_DISMISSED_IDS && id !in updated) {
            updated.firstOrNull()?.let(updated::remove)
        }
        updated += id
        preferences.edit().putStringSet(KEY_DISMISSED_IDS, updated).apply()
    }
}
