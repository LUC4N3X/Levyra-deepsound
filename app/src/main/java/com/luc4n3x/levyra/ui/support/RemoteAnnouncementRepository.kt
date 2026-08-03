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

internal const val BUILT_IN_SUPPORT_ANNOUNCEMENT_ID = "github-star-2026-08"
internal const val LEVYRA_REPOSITORY_URL = "https://github.com/LUC4N3X/Levyra-deepsound"
internal const val BUNDLED_ANNOUNCEMENTS_ASSET = "config/announcements.json"
internal const val REMOTE_ANNOUNCEMENTS_URL =
    "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/main/app/src/main/assets/config/announcements.json"

private const val REMOTE_SCHEMA_VERSION = 2
private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1_000L
private const val STORE_NAME = "levyra_remote_announcements"
private const val KEY_CACHED_JSON = "cached_json"
private const val KEY_ETAG = "etag"
private const val KEY_LAST_SUCCESSFUL_CHECK = "last_successful_check"
private const val KEY_COMPLETED_IDS = "dismissed_ids"
private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
private const val KEY_LAST_APP_LAUNCH_AT = "last_app_launch_at"
private const val KEY_SNOOZED_UNTIL_PREFIX = "snoozed_until_"
private const val MAX_ANNOUNCEMENTS = 20
private const val MAX_COMPLETED_IDS = 200
private const val MAX_RECORDED_LAUNCHES = 10_000
private const val APP_LAUNCH_SESSION_GAP_MS = 30L * 60L * 1_000L
private val supportedLanguageCodes = LevyraLanguageCatalog.languages.mapTo(mutableSetOf()) { it.code }

internal object RemoteAnnouncementPromptPolicy {
    const val MINIMUM_APP_LAUNCHES = 3
    const val MINIMUM_RECENT_LISTENS = 3
    const val MINIMUM_CURRENT_LISTEN_MS = 90_000L
    const val PASSIVE_SNOOZE_MS = 3L * 24L * 60L * 60L * 1_000L
    const val LATER_SNOOZE_MS = 10L * 24L * 60L * 60L * 1_000L

    fun hasPositiveListeningMoment(recentListenCount: Int, listenedPlaybackMs: Long): Boolean {
        return recentListenCount >= MINIMUM_RECENT_LISTENS ||
            listenedPlaybackMs >= MINIMUM_CURRENT_LISTEN_MS
    }

    fun isEligible(
        launchCount: Int,
        hasPositiveListeningMoment: Boolean,
        snoozedUntilMs: Long,
        nowMs: Long
    ): Boolean {
        return launchCount >= MINIMUM_APP_LAUNCHES &&
            hasPositiveListeningMoment &&
            snoozedUntilMs <= nowMs
    }
}

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

internal data class OpenSourceSupportCopy(
    val badge: String,
    val title: String,
    val body: String,
    val starAction: String,
    val laterAction: String,
    val settingsTitle: String,
    val settingsSubtitle: String
)

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
        completedIds: Set<String>,
        nowMs: Long,
        isAnnouncementEligible: (RemoteAnnouncement) -> Boolean = { true }
    ): RemoteAnnouncementPresentation? {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        return catalog.announcements
            .asSequence()
            .filter { it.enabled }
            .filter { it.id !in completedIds }
            .filter(isAnnouncementEligible)
            .filter { versionCode >= it.minimumVersionCode }
            .filter { it.maximumVersionCode == null || versionCode <= it.maximumVersionCode }
            .filter { it.startAtMs == null || nowMs >= it.startAtMs }
            .filter { it.endAtMs == null || nowMs <= it.endAtMs }
            .sortedWith(
                compareByDescending<RemoteAnnouncement> { it.priority }
                    .thenByDescending { it.startAtMs ?: Long.MIN_VALUE }
                    .thenBy { it.id }
            )
            .mapNotNull { announcement -> presentationFor(announcement, normalizedLanguage) }
            .firstOrNull()
    }

    fun presentationFor(
        announcement: RemoteAnnouncement,
        languageCode: String
    ): RemoteAnnouncementPresentation? {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        val copy = announcement.translations[normalizedLanguage]
            ?: announcement.translations["en"]
            ?: return null
        return RemoteAnnouncementPresentation(
            id = announcement.id,
            style = announcement.style,
            copy = copy,
            actionUrl = announcement.actionUrl
        )
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
        val translations = parseTranslations(item, actionUrl != null)
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

    private fun parseTranslations(
        item: JSONObject,
        actionRequired: Boolean
    ): Map<String, OpenSourceSupportCopy> {
        val translationsObject = item.optJSONObject("translations") ?: return emptyMap()
        return buildMap {
            val keys = translationsObject.keys()
            while (keys.hasNext()) {
                val sourceCode = keys.next()
                val normalized = LevyraLanguageCatalog.normalize(sourceCode)
                if (normalized !in supportedLanguageCodes) continue
                val copyObject = translationsObject.optJSONObject(sourceCode) ?: continue
                parseCopy(copyObject, actionRequired)?.let { put(normalized, it) }
            }
        }
    }

    private fun parseCopy(item: JSONObject, actionRequired: Boolean): OpenSourceSupportCopy? {
        val badge = item.optString("badge").trim()
        val title = item.optString("title").trim()
        val body = item.optString("body").trim()
        val action = item.optString("action").trim()
        val later = item.optString("dismiss").trim()
        val settingsTitle = item.optString("settingsTitle").trim().ifBlank { action.ifBlank { title } }
        val settingsSubtitle = item.optString("settingsSubtitle").trim().ifBlank { body }
        if (badge.isBlank() || badge.length > 60) return null
        if (title.isBlank() || title.length > 140) return null
        if (body.length !in 20..1_200) return null
        if (actionRequired && action.isBlank()) return null
        if (action.length > 100) return null
        if (later.isBlank() || later.length > 80) return null
        if (settingsTitle.isBlank() || settingsTitle.length > 120) return null
        if (settingsSubtitle.isBlank() || settingsSubtitle.length > 1_200) return null
        return OpenSourceSupportCopy(
            badge = badge,
            title = title,
            body = body,
            starAction = action,
            laterAction = later,
            settingsTitle = settingsTitle,
            settingsSubtitle = settingsSubtitle
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
    private val bundledCatalog: RemoteAnnouncementCatalog? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        readBundledCatalog()
    }

    fun recordAppLaunch(nowMs: Long = System.currentTimeMillis()): Int = store.recordAppLaunch(nowMs)

    suspend fun resolveForPrompt(
        languageCode: String,
        launchCount: Int,
        hasPositiveListeningMoment: Boolean,
        versionCode: Int = BuildConfig.VERSION_CODE,
        nowMs: Long = System.currentTimeMillis()
    ): RemoteAnnouncementPresentation? = withContext(Dispatchers.IO) {
        resolveAvailable(
            languageCode = languageCode,
            versionCode = versionCode,
            nowMs = nowMs
        ) { announcement ->
            val snoozeExpired = store.snoozedUntil(announcement.id) <= nowMs
            val engagementEligible = announcement.style != AnnouncementStyle.OPEN_SOURCE ||
                RemoteAnnouncementPromptPolicy.isEligible(
                    launchCount = launchCount,
                    hasPositiveListeningMoment = hasPositiveListeningMoment,
                    snoozedUntilMs = 0L,
                    nowMs = nowMs
                )
            snoozeExpired && engagementEligible
        }
    }

    fun bundledSupportPresentation(languageCode: String): RemoteAnnouncementPresentation? {
        val announcement = bundledCatalog
            ?.announcements
            ?.firstOrNull { it.id == BUILT_IN_SUPPORT_ANNOUNCEMENT_ID }
            ?: return null
        return RemoteAnnouncementRules.presentationFor(announcement, languageCode)
    }

    fun snooze(id: String, durationMs: Long, nowMs: Long = System.currentTimeMillis()) {
        store.snooze(id, nowMs + durationMs.coerceAtLeast(0L))
    }

    fun markCompleted(id: String) {
        store.markCompleted(id)
    }

    private fun resolveAvailable(
        languageCode: String,
        versionCode: Int,
        nowMs: Long,
        isAnnouncementEligible: (RemoteAnnouncement) -> Boolean
    ): RemoteAnnouncementPresentation? {
        val cached = store.cachedCatalog()
        val remoteState = if (cached != null && !store.shouldRefresh(nowMs)) {
            CatalogState(cached, available = true)
        } else {
            refresh(cached, nowMs)
        }
        val completedIds = store.completedIds()
        fun selectFrom(catalog: RemoteAnnouncementCatalog): RemoteAnnouncementPresentation? {
            return RemoteAnnouncementRules.select(
                catalog = catalog,
                languageCode = languageCode,
                versionCode = versionCode,
                completedIds = completedIds,
                nowMs = nowMs,
                isAnnouncementEligible = isAnnouncementEligible
            )
        }
        val selected = remoteState.catalog?.let(::selectFrom)
        return selected ?: if (remoteState.available) {
            null
        } else {
            bundledCatalog?.let(::selectFrom)
        }
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

    private fun readBundledCatalog(): RemoteAnnouncementCatalog? = runCatching {
        appContext.assets.open(BUNDLED_ANNOUNCEMENTS_ASSET).bufferedReader().use { reader ->
            RemoteAnnouncementParser.parse(reader.readText())
        }
    }.getOrNull()

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

    fun recordAppLaunch(nowMs: Long): Int = synchronized(this) {
        val current = preferences.getInt(KEY_APP_LAUNCH_COUNT, 0)
        val lastRecordedAt = preferences.getLong(KEY_LAST_APP_LAUNCH_AT, 0L)
        val elapsed = nowMs - lastRecordedAt
        if (lastRecordedAt > 0L && elapsed in 0L until APP_LAUNCH_SESSION_GAP_MS) {
            return@synchronized current
        }
        val next = (current + 1).coerceAtMost(MAX_RECORDED_LAUNCHES)
        preferences.edit()
            .putInt(KEY_APP_LAUNCH_COUNT, next)
            .putLong(KEY_LAST_APP_LAUNCH_AT, nowMs)
            .apply()
        next
    }

    fun completedIds(): Set<String> =
        preferences.getStringSet(KEY_COMPLETED_IDS, emptySet()).orEmpty().toSet()

    fun snoozedUntil(id: String): Long {
        if (!RemoteAnnouncementRules.isValidId(id)) return Long.MAX_VALUE
        return preferences.getLong(snoozeKey(id), 0L)
    }

    fun snooze(id: String, untilMs: Long) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        preferences.edit().putLong(snoozeKey(id), untilMs.coerceAtLeast(0L)).apply()
    }

    fun markCompleted(id: String) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        synchronized(this) {
            val updated = completedIds().toMutableSet()
            if (updated.size >= MAX_COMPLETED_IDS && id !in updated) {
                updated.firstOrNull()?.let(updated::remove)
            }
            updated += id
            preferences.edit()
                .putStringSet(KEY_COMPLETED_IDS, updated)
                .remove(snoozeKey(id))
                .apply()
        }
    }

    private fun snoozeKey(id: String): String = KEY_SNOOZED_UNTIL_PREFIX + id
}
