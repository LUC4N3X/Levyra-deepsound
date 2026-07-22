package com.luc4n3x.levyra.data.lore

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.local.ArtistLoreDao
import com.luc4n3x.levyra.data.local.ArtistLoreEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.artistIdentityKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class ArtistLoreRepository(context: Context?) {
    private val appContext = context?.applicationContext
    private val dao: ArtistLoreDao? = appContext?.let { LevyraDatabase.get(it).artistLoreDao() }
    private val client = LevyraHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val memory = ConcurrentHashMap<String, ArtistLoreEntity>()

    fun observe(
        artistName: String,
        browseId: String,
        languageCode: String
    ): Flow<ArtistBiography> = flow {
        val cleanName = artistName.trim()
        if (cleanName.length < 2) return@flow
        val artistKey = artistIdentityKey(cleanName)
        if (artistKey.isBlank()) return@flow
        val languages = languagePriority(languageCode)
        val now = System.currentTimeMillis()
        var emittedSignature = ""
        var fallbackFresh = false
        val freshLanguages = HashSet<String>()
        val blockedLanguages = HashSet<String>()

        for (language in languages) {
            val cached = readCache(artistKey, browseId, language, now) ?: continue
            if (cached.negative) {
                if (cached.expiresAt > now) blockedLanguages += language
                continue
            }
            if (cached.staleUntil <= now || cached.text.isBlank()) continue
            if (emittedSignature.isBlank()) {
                emittedSignature = cached.signature()
                emit(cached.toBiography(cached = true))
            }
            if (cached.sourceUrl.contains("wikidata.org", ignoreCase = true)) {
                if (cached.expiresAt > now) fallbackFresh = true
            } else if (cached.expiresAt > now) {
                freshLanguages += language
            }
        }

        if (emittedSignature.isBlank()) {
            val fallback = readBestFallbackCache(artistKey, browseId, languages, now)
            if (fallback != null && !fallback.negative && fallback.text.isNotBlank() && fallback.staleUntil > now) {
                emittedSignature = fallback.signature()
                fallbackFresh = fallback.expiresAt > now
                emit(fallback.toBiography(cached = true))
            }
        }

        val preferredLanguage = languages.first()
        if (preferredLanguage in freshLanguages || (preferredLanguage in blockedLanguages && freshLanguages.isNotEmpty())) {
            return@flow
        }

        for (language in languages) {
            if (language in blockedLanguages || language in freshLanguages) continue
            val outcome = resolveNetwork(cleanName, browseId, language)
            val result = outcome.biography
            if (result != null) {
                val entity = persistPositive(artistKey, browseId, result, now)
                if (entity.signature() != emittedSignature) emit(result.copy(cached = false))
                prune(now)
                return@flow
            }
            if (outcome.completedWithoutTransientFailure) {
                persistNegative(artistKey, browseId, language, now)
            }
        }

        if (!fallbackFresh) {
            val fallbackLanguages = crossLanguagePriority(languages)
            if (fallbackLanguages.isNotEmpty()) {
                val fallbackOutcome = resolveFromEntitySearch(
                    artistName = cleanName,
                    browseId = browseId,
                    searchLanguageCode = preferredLanguage,
                    articleLanguages = fallbackLanguages,
                    allowDescriptionFallback = true
                )
                val fallbackBiography = fallbackOutcome.biography
                if (fallbackBiography != null) {
                    val entity = persistPositive(artistKey, browseId, fallbackBiography, now)
                    if (entity.signature() != emittedSignature) emit(fallbackBiography.copy(cached = false))
                }
            }
        }
        prune(now)
    }.flowOn(Dispatchers.IO)

    private suspend fun readCache(
        artistKey: String,
        browseId: String,
        languageCode: String,
        now: Long
    ): ArtistLoreEntity? {
        val key = cacheKey(artistKey, browseId, languageCode)
        memory[key]?.let { cached ->
            if (cached.staleUntil > now) return cached.also { dao?.touch(it.cacheKey, now) }
            memory.remove(key)
        }
        val requestedBrowseId = browseId.trim()
        val stored = dao?.get(key)
            ?: requestedBrowseId.takeIf { it.isNotBlank() }?.let { dao?.findByBrowseId(it, languageCode) }
            ?: (if (allowsArtistKeyFallback(requestedBrowseId)) dao?.findByArtistKey(artistKey, languageCode) else null)
            ?: return null
        if (stored.staleUntil <= now) return null
        memory[key] = stored
        dao?.touch(stored.cacheKey, now)
        return stored
    }

    private suspend fun readBestFallbackCache(
        artistKey: String,
        browseId: String,
        excludedLanguages: List<String>,
        now: Long
    ): ArtistLoreEntity? {
        val excluded = excludedLanguages.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        val requestedBrowseId = browseId.trim()
        val memoryCandidate = memory.values
            .asSequence()
            .filter { !it.negative && it.staleUntil > now && it.languageCode.lowercase(Locale.ROOT) !in excluded }
            .filter {
                if (requestedBrowseId.isNotBlank()) {
                    it.browseId.equals(requestedBrowseId, ignoreCase = true)
                } else {
                    it.browseId.isBlank() && it.artistKey == artistKey
                }
            }
            .sortedWith(compareByDescending<ArtistLoreEntity> { it.confidence }.thenByDescending { it.updatedAt })
            .firstOrNull()
        if (memoryCandidate != null) {
            dao?.touch(memoryCandidate.cacheKey, now)
            return memoryCandidate
        }
        val stored = if (requestedBrowseId.isNotBlank()) {
            dao?.findBestByBrowseId(requestedBrowseId, excludedLanguages)
        } else {
            dao?.findBestByArtistKey(artistKey, excludedLanguages)
        } ?: return null
        if (stored.staleUntil <= now) return null
        memory[stored.cacheKey] = stored
        dao?.touch(stored.cacheKey, now)
        return stored
    }

    private suspend fun resolveNetwork(
        artistName: String,
        browseId: String,
        languageCode: String
    ): LoreNetworkOutcome {
        val pageCollection = collectPageCandidates(artistName, languageCode)
        val pageBiography = resolvePageCandidates(
            browseId = browseId,
            languageCode = languageCode,
            candidates = pageCollection.candidates
        )
        if (pageBiography != null) {
            return LoreNetworkOutcome(pageBiography, pageCollection.completedWithoutTransientFailure)
        }
        val entityOutcome = resolveFromEntitySearch(
            artistName = artistName,
            browseId = browseId,
            searchLanguageCode = languageCode,
            articleLanguages = listOf(languageCode),
            allowDescriptionFallback = false
        )
        return LoreNetworkOutcome(
            biography = entityOutcome.biography,
            completedWithoutTransientFailure = pageCollection.completedWithoutTransientFailure && entityOutcome.completedWithoutTransientFailure
        )
    }

    private suspend fun collectPageCandidates(
        artistName: String,
        languageCode: String
    ): LorePageCollection = coroutineScope {
        val exactJob = async { queryExactCandidates(artistName, languageCode) }
        val prefixJob = async { querySearchCandidates(artistName, languageCode, prefix = true) }
        val initialResults = listOf(exactJob.await(), prefixJob.await())
        val initialCandidates = initialResults.flatMap { result ->
            when (result) {
                is JsonResult.Success -> result.value.toCandidates(artistName, languageCode)
                JsonResult.Failure -> emptyList()
            }
        }
        val bestInitialScore = initialCandidates.maxOfOrNull { it.baseScore } ?: Int.MIN_VALUE
        val fullTextResult = if (bestInitialScore >= STRONG_PREFIX_SCORE) {
            null
        } else {
            querySearchCandidates(artistName, languageCode, prefix = false)
        }
        val allResults = if (fullTextResult == null) initialResults else initialResults + fullTextResult
        val allCandidates = buildList {
            addAll(initialCandidates)
            if (fullTextResult is JsonResult.Success) {
                addAll(fullTextResult.value.toCandidates(artistName, languageCode))
            }
        }.distinctBy { it.pageId.takeIf { id -> id > 0 } ?: normalize(it.pageTitle) }
            .sortedByDescending { it.baseScore }
        LorePageCollection(
            candidates = allCandidates,
            completedWithoutTransientFailure = allResults.none { it is JsonResult.Failure }
        )
    }

    private suspend fun resolvePageCandidates(
        browseId: String,
        languageCode: String,
        candidates: List<LoreCandidate>
    ): ArtistBiography? {
        if (candidates.isEmpty()) return null
        val finalists = candidates.take(MAX_ENTITY_CANDIDATES)
        val signals = fetchEntitySignals(finalists.map { it.entityId }.filter { it.isNotBlank() }.distinct())
        val ranked = finalists.map { candidate ->
            val entitySignals = signals[candidate.entityId]
            candidate.copy(finalScore = candidate.baseScore + entityBonus(entitySignals, browseId))
        }.sortedByDescending { it.finalScore }
        val winner = ranked.firstOrNull { candidate ->
            candidate.finalScore >= ACCEPTED_SCORE
        } ?: return null
        val summary = fetchSummary(winner.pageTitle, languageCode)
        val enriched = if (summary != null) winner.enrich(summary) else winner
        return enriched.toBiography(languageCode, scoreToConfidence(enriched.finalScore))
    }

    private suspend fun resolveFromEntitySearch(
        artistName: String,
        browseId: String,
        searchLanguageCode: String,
        articleLanguages: List<String>,
        allowDescriptionFallback: Boolean
    ): LoreNetworkOutcome = coroutineScope {
        val searchLanguages = linkedSetOf(searchLanguageCode, "en")
        val jobs = searchLanguages.map { language -> async { language to queryEntitySearch(artistName, language) } }
        val results = jobs.map { it.await() }
        val hits = results.flatMap { (language, result) ->
            when (result) {
                is JsonResult.Success -> result.value.toEntitySearchHits(language)
                JsonResult.Failure -> emptyList()
            }
        }.distinctBy { it.entityId }
        if (hits.isEmpty()) {
            return@coroutineScope LoreNetworkOutcome(
                biography = null,
                completedWithoutTransientFailure = results.none { it.second is JsonResult.Failure }
            )
        }
        val recordsResult = fetchEntityRecords(
            entityIds = hits.take(MAX_ENTITY_SEARCH_RESULTS).map { it.entityId },
            languages = linkedSetOf(searchLanguageCode, "en")
        )
        val records = when (recordsResult) {
            is EntityRecordsResult.Success -> recordsResult.records
            EntityRecordsResult.Failure -> return@coroutineScope LoreNetworkOutcome(null, false)
        }
        val hitById = hits.associateBy { it.entityId }
        val ranked = records.map { record ->
            val hit = hitById[record.entityId]
            record to entityRecordScore(artistName, record, hit, browseId)
        }.sortedByDescending { it.second }
        val accepted = ranked.filter { (_, score) ->
            score >= ENTITY_ACCEPTED_SCORE
        }
        var articleTransientFailure = false
        for ((record, score) in accepted.take(MAX_ENTITY_ARTICLE_ATTEMPTS)) {
            for (articleLanguage in articleLanguages.distinct()) {
                val title = record.sitelinks[wikiSiteKey(articleLanguage)]?.trim().orEmpty()
                if (title.isBlank()) continue
                val summary = fetchSummary(title, articleLanguage)
                if (summary != null) {
                    val candidate = LoreCandidate(
                        pageTitle = summary.pageTitle,
                        pageId = summary.pageId,
                        entityId = summary.entityId.ifBlank { record.entityId },
                        description = summary.description.ifBlank { record.descriptionFor(articleLanguage) },
                        extract = summary.extract,
                        thumbnailUrl = summary.thumbnailUrl,
                        originalImageUrl = summary.originalImageUrl,
                        sourceUrl = summary.sourceUrl.ifBlank { wikipediaPageUrl(articleLanguage, summary.pageTitle) },
                        baseScore = score,
                        finalScore = score
                    )
                    val biography = candidate.toBiography(articleLanguage, scoreToConfidence(score))
                    if (biography != null) return@coroutineScope LoreNetworkOutcome(biography, true)
                }
                when (val exact = queryExactCandidates(title, articleLanguage)) {
                    is JsonResult.Success -> {
                        val candidate = exact.value.toCandidates(
                            artistName = artistName,
                            languageCode = articleLanguage,
                            minimumScore = Int.MIN_VALUE
                        ).firstOrNull { page ->
                            page.entityId.isBlank() || page.entityId == record.entityId
                        }
                        if (candidate != null) {
                            val biography = candidate.copy(
                                entityId = candidate.entityId.ifBlank { record.entityId },
                                finalScore = score
                            ).toBiography(articleLanguage, scoreToConfidence(score))
                            if (biography != null) return@coroutineScope LoreNetworkOutcome(biography, true)
                        }
                    }
                    JsonResult.Failure -> articleTransientFailure = true
                }
            }
        }
        if (allowDescriptionFallback) {
            val fallback = accepted.firstNotNullOfOrNull { (record, score) ->
                record.toDescriptionBiography(
                    preferredLanguages = linkedSetOf(searchLanguageCode, "en"),
                    confidence = scoreToConfidence(score).coerceAtMost(84)
                )
            }
            if (fallback != null) return@coroutineScope LoreNetworkOutcome(fallback, true)
        }
        LoreNetworkOutcome(
            biography = null,
            completedWithoutTransientFailure = results.none { it.second is JsonResult.Failure } && !articleTransientFailure
        )
    }

    private suspend fun queryExactCandidates(
        artistName: String,
        languageCode: String
    ): JsonResult {
        val url = pageQueryBase(languageCode)
            .addQueryParameter("titles", artistName)
            .build()
        return getJson(url, languageCode)
    }

    private suspend fun querySearchCandidates(
        artistName: String,
        languageCode: String,
        prefix: Boolean
    ): JsonResult {
        val base = pageQueryBase(languageCode)
        if (prefix) {
            base.addQueryParameter("generator", "prefixsearch")
                .addQueryParameter("gpssearch", artistName)
                .addQueryParameter("gpsnamespace", "0")
                .addQueryParameter("gpslimit", "10")
        } else {
            base.addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", artistName)
                .addQueryParameter("gsrnamespace", "0")
                .addQueryParameter("gsrwhat", "text")
                .addQueryParameter("gsrlimit", "12")
        }
        return getJson(base.build(), languageCode)
    }

    private fun pageQueryBase(languageCode: String): HttpUrl.Builder {
        return "https://$languageCode.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("redirects", "1")
            .addQueryParameter("converttitles", "1")
            .addQueryParameter("prop", "description|pageimages|pageprops|extracts|info")
            .addQueryParameter("ppprop", "disambiguation|wikibase_item")
            .addQueryParameter("piprop", "thumbnail|original")
            .addQueryParameter("pilicense", "any")
            .addQueryParameter("pithumbsize", "640")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("exchars", "3600")
            .addQueryParameter("inprop", "displaytitle")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
    }

    private suspend fun queryEntitySearch(
        artistName: String,
        languageCode: String
    ): JsonResult {
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbsearchentities")
            .addQueryParameter("search", artistName)
            .addQueryParameter("language", languageCode)
            .addQueryParameter("uselang", languageCode)
            .addQueryParameter("type", "item")
            .addQueryParameter("limit", MAX_ENTITY_SEARCH_RESULTS.toString())
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .build()
        return getJson(url, languageCode)
    }

    private suspend fun fetchSummary(pageTitle: String, languageCode: String): LoreSummary? {
        val url = "https://$languageCode.wikipedia.org/api/rest_v1/page/summary/${encodePathSegment(pageTitle)}"
            .toHttpUrl()
        return when (val result = getJson(url, languageCode, SUMMARY_ACCEPT)) {
            is JsonResult.Success -> result.value.toSummary()
            JsonResult.Failure -> null
        }
    }

    private suspend fun fetchEntitySignals(entityIds: List<String>): Map<String, LoreEntitySignals> {
        if (entityIds.isEmpty()) return emptyMap()
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbgetentities")
            .addQueryParameter("ids", entityIds.joinToString("|"))
            .addQueryParameter("props", "claims")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .build()
        val root = when (val result = getJson(url, "en")) {
            is JsonResult.Success -> result.value
            JsonResult.Failure -> return emptyMap()
        }
        val entities = root.optJSONObject("entities") ?: return emptyMap()
        val out = HashMap<String, LoreEntitySignals>()
        entityIds.forEach { entityId ->
            val claims = entities.optJSONObject(entityId)?.optJSONObject("claims") ?: return@forEach
            out[entityId] = claims.toSignals()
        }
        return out
    }

    private suspend fun fetchEntityRecords(
        entityIds: List<String>,
        languages: Set<String>
    ): EntityRecordsResult {
        if (entityIds.isEmpty()) return EntityRecordsResult.Success(emptyList())
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbgetentities")
            .addQueryParameter("ids", entityIds.joinToString("|"))
            .addQueryParameter("props", "claims|sitelinks|labels|descriptions|aliases")
            .addQueryParameter("languages", languages.joinToString("|"))
            .addQueryParameter("languagefallback", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .build()
        val root = when (val result = getJson(url, languages.firstOrNull().orEmpty().ifBlank { "en" })) {
            is JsonResult.Success -> result.value
            JsonResult.Failure -> return EntityRecordsResult.Failure
        }
        val entities = root.optJSONObject("entities") ?: return EntityRecordsResult.Success(emptyList())
        val records = entityIds.mapNotNull { entityId ->
            val entity = entities.optJSONObject(entityId) ?: return@mapNotNull null
            val claims = entity.optJSONObject("claims") ?: JSONObject()
            LoreEntityRecord(
                entityId = entityId,
                labels = entity.localizedTextMap("labels"),
                descriptions = entity.localizedTextMap("descriptions"),
                aliases = entity.localizedAliases(),
                sitelinks = entity.sitelinkMap(),
                signals = claims.toSignals()
            )
        }
        return EntityRecordsResult.Success(records)
    }

    private suspend fun getJson(
        url: HttpUrl,
        languageCode: String,
        accept: String = "application/json"
    ): JsonResult {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", languageCode)
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} (https://github.com/LUC4N3X/Levyra-deepsound) Android")
            .get()
            .build()
        return when (val response = client.await(request)) {
            is HttpResult.Success -> runCatching { JSONObject(response.body) }
                .fold(onSuccess = { JsonResult.Success(it) }, onFailure = { JsonResult.Failure })
            HttpResult.Failure -> JsonResult.Failure
        }
    }

    private suspend fun persistPositive(
        artistKey: String,
        browseId: String,
        biography: ArtistBiography,
        now: Long
    ): ArtistLoreEntity {
        val isDescriptionFallback = biography.sourceUrl.contains("wikidata.org", ignoreCase = true)
        val entity = ArtistLoreEntity(
            cacheKey = cacheKey(artistKey, browseId, biography.languageCode),
            artistKey = artistKey,
            browseId = browseId.trim(),
            languageCode = biography.languageCode,
            text = biography.text,
            description = biography.description,
            pageTitle = biography.pageTitle,
            pageId = biography.pageId,
            entityId = biography.entityId,
            thumbnailUrl = biography.thumbnailUrl,
            originalImageUrl = biography.originalImageUrl,
            sourceUrl = biography.sourceUrl,
            confidence = biography.confidence,
            negative = false,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now,
            expiresAt = now + if (isDescriptionFallback) DESCRIPTION_TTL_MS else POSITIVE_TTL_MS,
            staleUntil = now + if (isDescriptionFallback) DESCRIPTION_STALE_MS else POSITIVE_STALE_MS
        )
        memory[entity.cacheKey] = entity
        dao?.upsert(entity)
        return entity
    }

    private suspend fun persistNegative(
        artistKey: String,
        browseId: String,
        languageCode: String,
        now: Long
    ) {
        val entity = ArtistLoreEntity(
            cacheKey = cacheKey(artistKey, browseId, languageCode),
            artistKey = artistKey,
            browseId = browseId.trim(),
            languageCode = languageCode,
            text = "",
            description = "",
            pageTitle = "",
            pageId = 0,
            entityId = "",
            thumbnailUrl = "",
            originalImageUrl = "",
            sourceUrl = "",
            confidence = 0,
            negative = true,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now,
            expiresAt = now + NEGATIVE_TTL_MS,
            staleUntil = now + NEGATIVE_TTL_MS
        )
        memory[entity.cacheKey] = entity
        dao?.upsert(entity)
    }

    private suspend fun prune(now: Long) {
        val targetDao = dao ?: return
        targetDao.deleteExpired(now)
        val overflow = targetDao.count() - MAX_CACHE_ENTRIES
        if (overflow > 0) targetDao.deleteOldest(overflow)
    }

    private fun cacheKey(artistKey: String, browseId: String, languageCode: String): String {
        val identity = browseId.trim().lowercase(Locale.ROOT).ifBlank { artistKey }
        return "$CACHE_KEY_VERSION|$identity|${languageCode.lowercase(Locale.ROOT)}"
    }

    private fun languagePriority(languageCode: String): List<String> {
        return linkedSetOf(preferredLanguage(languageCode), "en").toList()
    }

    private fun crossLanguagePriority(excludedLanguages: List<String>): List<String> {
        val excluded = excludedLanguages.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        return CROSS_LANGUAGE_FALLBACKS.filterNot { it.lowercase(Locale.ROOT) in excluded }
    }

    private fun JSONArray?.asObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) optJSONObject(index)?.let(::add)
        }
    }

    private fun JSONObject.toCandidates(
        artistName: String,
        languageCode: String,
        minimumScore: Int = MIN_PRELIMINARY_SCORE
    ): List<LoreCandidate> {
        val pages = optJSONObject("query")?.optJSONArray("pages").asObjects()
        return pages.mapNotNull { page -> page.toCandidate(artistName, languageCode, minimumScore) }
            .sortedByDescending { it.baseScore }
    }

    private fun JSONObject.toCandidate(
        artistName: String,
        languageCode: String,
        minimumScore: Int
    ): LoreCandidate? {
        if (has("missing") || has("invalid")) return null
        val title = optString("title").trim()
        val extract = cleanBiography(optString("extract"))
        val description = optString("description").trim()
        val pageProps = optJSONObject("pageprops")
        if (title.isBlank() || pageProps?.has("disambiguation") == true) return null
        if (extract.length < MIN_TEXT_LENGTH) return null
        val score = candidateScore(artistName, title, description, extract)
        if (score < minimumScore) return null
        val thumbnail = optJSONObject("thumbnail")?.optString("source").orEmpty()
        val original = optJSONObject("original")?.optString("source").orEmpty()
        val encodedTitle = encodePathSegment(title.replace(' ', '_'))
        return LoreCandidate(
            pageTitle = title,
            pageId = optInt("pageid", 0),
            entityId = pageProps?.optString("wikibase_item").orEmpty(),
            description = description,
            extract = extract,
            thumbnailUrl = thumbnail,
            originalImageUrl = original,
            sourceUrl = "https://$languageCode.wikipedia.org/wiki/$encodedTitle",
            baseScore = score,
            finalScore = score
        )
    }

    private fun JSONObject.toSummary(): LoreSummary? {
        val title = optString("title").trim()
        val extract = cleanBiography(optString("extract"))
        if (title.isBlank() || extract.length < MIN_TEXT_LENGTH) return null
        return LoreSummary(
            pageTitle = title,
            pageId = optInt("pageid", 0),
            entityId = optString("wikibase_item"),
            description = optString("description").trim(),
            extract = extract,
            thumbnailUrl = optJSONObject("thumbnail")?.optString("source").orEmpty(),
            originalImageUrl = optJSONObject("originalimage")?.optString("source").orEmpty(),
            sourceUrl = optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page").orEmpty()
        )
    }

    private fun JSONObject.toEntitySearchHits(languageCode: String): List<LoreEntitySearchHit> {
        return optJSONArray("search").asObjects().mapNotNull { item ->
            val entityId = item.optString("id").trim()
            if (entityId.isBlank()) return@mapNotNull null
            val aliases = buildList {
                val rawAliases = item.optJSONArray("aliases")
                if (rawAliases != null) {
                    for (index in 0 until rawAliases.length()) {
                        rawAliases.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
            LoreEntitySearchHit(
                entityId = entityId,
                languageCode = languageCode,
                label = item.optString("label").trim(),
                description = item.optString("description").trim(),
                matchText = item.optJSONObject("match")?.optString("text").orEmpty().trim(),
                aliases = aliases
            )
        }
    }

    private fun JSONObject.localizedTextMap(property: String): Map<String, String> {
        val root = optJSONObject(property) ?: return emptyMap()
        return buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = root.optJSONObject(key)?.optString("value").orEmpty().trim()
                if (value.isNotBlank()) put(key, value)
            }
        }
    }

    private fun JSONObject.localizedAliases(): Map<String, List<String>> {
        val root = optJSONObject("aliases") ?: return emptyMap()
        return buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val values = root.optJSONArray(key).asObjects().mapNotNull { alias ->
                    alias.optString("value").trim().takeIf { it.isNotBlank() }
                }
                if (values.isNotEmpty()) put(key, values)
            }
        }
    }

    private fun JSONObject.sitelinkMap(): Map<String, String> {
        val root = optJSONObject("sitelinks") ?: return emptyMap()
        return buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val title = root.optJSONObject(key)?.optString("title").orEmpty().trim()
                if (title.isNotBlank()) put(key, title)
            }
        }
    }

    private fun JSONObject.toSignals(): LoreEntitySignals {
        return LoreEntitySignals(
            instanceOf = qIds("P31"),
            occupations = qIds("P106"),
            youtubeChannelIds = stringValues("P2397")
        )
    }

    private fun JSONObject.qIds(property: String): Set<String> {
        return optJSONArray(property).asObjects().mapNotNullTo(LinkedHashSet()) { claim ->
            claim.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
                ?.optString("id")
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun JSONObject.stringValues(property: String): Set<String> {
        return optJSONArray(property).asObjects().mapNotNullTo(LinkedHashSet()) { claim ->
            claim.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.opt("value")
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
    }

    companion object {
        internal fun preferredLanguage(languageCode: String): String {
            val normalized = LevyraLanguageCatalog.normalize(languageCode).substringBefore('-')
            return when (normalized) {
                "fil" -> "tl"
                "zh", "ar", "pt", "uk", "ru", "tr", "el", "sv", "da", "cs", "pl", "ro", "nl", "de", "fr", "es", "it", "ja", "ko", "hi", "id", "vi", "th", "he" -> normalized
                else -> "en"
            }
        }

        internal fun allowsArtistKeyFallback(browseId: String): Boolean = browseId.trim().isBlank()

        internal fun isExactTitleCandidate(artistName: String, pageTitle: String): Boolean {
            val artist = normalize(artistName)
            val title = normalize(pageTitle)
            val baseTitle = normalize(pageTitle.substringBefore(" (").substringBefore(" ["))
            if (artist.isBlank() || baseTitle.isBlank()) return false
            if (artist == baseTitle || artist == title) return true
            val compactArtist = artist.replace(" ", "")
            val compactTitle = baseTitle.replace(" ", "")
            val distance = editDistance(compactArtist, compactTitle)
            return compactArtist.length >= 3 && distance <= 1
        }

        internal fun candidateScore(
            artistName: String,
            pageTitle: String,
            description: String,
            extract: String
        ): Int {
            val artist = normalize(artistName)
            val title = normalize(pageTitle)
            val baseTitle = normalize(pageTitle.substringBefore(" (").substringBefore(" ["))
            if (artist.isBlank() || title.isBlank()) return Int.MIN_VALUE
            val parenthetical = normalize(pageTitle.substringAfter("(", "").substringBeforeLast(")", ""))
            val blob = normalize("$description ${extract.take(1000)}")
            var score = 0
            val compactArtist = artist.replace(" ", "")
            val compactTitle = baseTitle.replace(" ", "")
            val distance = editDistance(compactArtist, compactTitle)
            when {
                baseTitle == artist -> score += 620
                title == artist -> score += 600
                compactArtist.length >= 3 && distance <= 1 -> score += 440
                compactArtist.length >= 7 && distance <= 2 -> score += 320
                title.startsWith("$artist ") || artist.startsWith("$baseTitle ") -> score += 360
                tokenCoverage(artist, title) >= 0.9 -> score += 280
                tokenCoverage(artist, title) >= 0.7 -> score += 160
                else -> score -= 500
            }
            if (MUSIC_TERMS.any { containsTerm(blob, it) }) score += 300
            if (MUSIC_TERMS.any { containsTerm(parenthetical, it) }) score += 180
            if (GROUP_TERMS.any { containsTerm(blob, it) }) score += 80
            if (NON_ARTIST_TITLE_TERMS.any { containsTerm(parenthetical, it) }) score -= 850
            if (NON_ARTIST_DESCRIPTION_TERMS.any { blob.startsWith(normalize(it)) || containsTerm(blob, it) }) score -= 260
            if (DISAMBIGUATION_TERMS.any { blob.contains(normalize(it)) }) score -= 900
            if (normalize(extract).startsWith(artist)) score += 70
            if (extract.length >= 500) score += 35
            return score
        }

        internal fun entityIdentityScore(
            artistName: String,
            names: Collection<String>,
            description: String
        ): Int {
            val artist = normalize(artistName)
            if (artist.isBlank()) return Int.MIN_VALUE
            val identity = names.asSequence()
                .map(::normalize)
                .filter { it.isNotBlank() }
                .maxOfOrNull { candidate ->
                    val compactArtist = artist.replace(" ", "")
                    val compactCandidate = candidate.replace(" ", "")
                    val distance = editDistance(compactArtist, compactCandidate)
                    when {
                        candidate == artist -> 650
                        compactArtist.length >= 3 && distance <= 1 -> 520
                        compactArtist.length >= 7 && distance <= 2 -> 400
                        candidate.startsWith("$artist ") || artist.startsWith("$candidate ") -> 360
                        tokenCoverage(artist, candidate) >= 0.9 -> 320
                        tokenCoverage(artist, candidate) >= 0.7 -> 180
                        else -> -500
                    }
                } ?: -500
            val normalizedDescription = normalize(description)
            var score = identity
            if (MUSIC_TERMS.any { containsTerm(normalizedDescription, it) }) score += 260
            if (GROUP_TERMS.any { containsTerm(normalizedDescription, it) }) score += 80
            if (NON_ARTIST_DESCRIPTION_TERMS.any { containsTerm(normalizedDescription, it) }) score -= 700
            if (NON_ARTIST_TITLE_TERMS.any { containsTerm(normalizedDescription, it) }) score -= 420
            return score
        }

        private fun entityRecordScore(
            artistName: String,
            record: LoreEntityRecord,
            hit: LoreEntitySearchHit?,
            browseId: String
        ): Int {
            val names = buildList {
                addAll(record.labels.values)
                record.aliases.values.forEach(::addAll)
                hit?.label?.takeIf { it.isNotBlank() }?.let(::add)
                hit?.matchText?.takeIf { it.isNotBlank() }?.let(::add)
                hit?.aliases?.let(::addAll)
            }
            val description = buildString {
                record.descriptions.values.forEach { append(it).append(' ') }
                hit?.description?.let { append(it) }
            }
            var score = entityIdentityScore(artistName, names, description)
            score += entityBonus(record.signals, browseId)
            if (record.sitelinks.keys.any { it.endsWith("wiki") && !it.contains("commons") }) score += 40
            return score
        }

        private fun entityBonus(signals: LoreEntitySignals?, browseId: String): Int {
            signals ?: return 0
            var score = 0
            val hasMusicOccupation = signals.occupations.any(MUSIC_OCCUPATION_IDS::contains)
            val hasMusicGroupType = signals.instanceOf.any(MUSIC_GROUP_INSTANCE_IDS::contains)
            if (hasMusicGroupType) score += 320
            if (hasMusicOccupation) score += 340
            if (signals.instanceOf.any(NON_ARTIST_INSTANCE_IDS::contains)) score -= 1_100
            if ("Q5" in signals.instanceOf && signals.occupations.isNotEmpty() && !hasMusicOccupation) score -= 300
            if (!hasMusicOccupation && !hasMusicGroupType && signals.instanceOf.isNotEmpty()) score -= 180
            score += youtubeChannelScoreAdjustment(browseId, signals.youtubeChannelIds)
            return score
        }

        internal fun youtubeChannelScoreAdjustment(
            browseId: String,
            youtubeChannelIds: Set<String>
        ): Int {
            val channelId = browseId.trim()
            if (!channelId.startsWith("UC", ignoreCase = true) || youtubeChannelIds.isEmpty()) return 0
            return if (youtubeChannelIds.any { it.equals(channelId, ignoreCase = true) }) {
                700
            } else {
                -120
            }
        }

        private fun scoreToConfidence(score: Int): Int {
            return ((score - ACCEPTED_SCORE) / 8 + 72).coerceIn(72, 99)
        }

        private fun cleanBiography(value: String): String {
            val normalized = value
                .replace(Regex("\\s*\\n+\\s*"), " ")
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            if (normalized.length <= MAX_TEXT_LENGTH) return normalized
            val cut = normalized.lastIndexOf('.', MAX_TEXT_LENGTH).takeIf { it >= 1_800 } ?: MAX_TEXT_LENGTH
            return normalized.substring(0, cut + if (cut < normalized.length && normalized[cut] == '.') 1 else 0).trim()
        }

        private fun normalize(value: String): String {
            return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace('&', ' ')
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun containsTerm(value: String, term: String): Boolean {
            val normalizedTerm = normalize(term)
            if (normalizedTerm.isBlank()) return false
            if (normalizedTerm.any(::usesCompactWordBoundaries)) return value.contains(normalizedTerm)
            return " $value ".contains(" $normalizedTerm ")
        }

        private fun usesCompactWordBoundaries(character: Char): Boolean {
            return when (Character.UnicodeScript.of(character.code)) {
                Character.UnicodeScript.HAN,
                Character.UnicodeScript.HANGUL,
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA,
                Character.UnicodeScript.THAI -> true
                else -> false
            }
        }

        private fun editDistance(left: String, right: String): Int {
            if (left == right) return 0
            if (left.isEmpty()) return right.length
            if (right.isEmpty()) return left.length
            var previous = IntArray(right.length + 1) { it }
            var current = IntArray(right.length + 1)
            for (leftIndex in left.indices) {
                current[0] = leftIndex + 1
                for (rightIndex in right.indices) {
                    val cost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                    current[rightIndex + 1] = minOf(
                        current[rightIndex] + 1,
                        previous[rightIndex + 1] + 1,
                        previous[rightIndex] + cost
                    )
                }
                val swap = previous
                previous = current
                current = swap
            }
            return previous[right.length]
        }

        private fun tokenCoverage(expected: String, actual: String): Double {
            val expectedTokens = expected.split(' ').filter { it.length > 1 }
            if (expectedTokens.isEmpty()) return 0.0
            val actualTokens = actual.split(' ').toSet()
            return expectedTokens.count(actualTokens::contains).toDouble() / expectedTokens.size.toDouble()
        }

        private fun encodePathSegment(value: String): String {
            return HttpUrl.Builder()
                .scheme("https")
                .host("localhost")
                .addPathSegment(value)
                .build()
                .encodedPath
                .removePrefix("/")
        }

        private fun wikiSiteKey(languageCode: String): String = "${languageCode.lowercase(Locale.ROOT)}wiki"

        private fun wikipediaPageUrl(languageCode: String, pageTitle: String): String {
            return "https://$languageCode.wikipedia.org/wiki/${encodePathSegment(pageTitle.replace(' ', '_'))}"
        }

        private const val CACHE_KEY_VERSION = "v3"
        private const val MIN_TEXT_LENGTH = 80
        private const val MAX_TEXT_LENGTH = 3_200
        private const val MIN_PRELIMINARY_SCORE = 500
        private const val STRONG_PREFIX_SCORE = 900
        private const val ACCEPTED_SCORE = 760
        private const val ENTITY_ACCEPTED_SCORE = 760
        private const val MAX_ENTITY_CANDIDATES = 8
        private const val MAX_ENTITY_SEARCH_RESULTS = 12
        private const val MAX_ENTITY_ARTICLE_ATTEMPTS = 5
        private const val MAX_CACHE_ENTRIES = 500
        private const val POSITIVE_TTL_MS = 30L * 24L * 60L * 60L * 1_000L
        private const val POSITIVE_STALE_MS = 180L * 24L * 60L * 60L * 1_000L
        private const val DESCRIPTION_TTL_MS = 12L * 60L * 60L * 1_000L
        private const val DESCRIPTION_STALE_MS = 7L * 24L * 60L * 60L * 1_000L
        private const val NEGATIVE_TTL_MS = 2L * 60L * 60L * 1_000L
        private const val SUMMARY_ACCEPT = "application/json; charset=utf-8; profile=\"https://www.mediawiki.org/wiki/Specs/Summary/1.4.2\""

        private val CROSS_LANGUAGE_FALLBACKS = listOf(
            "simple", "it", "es", "fr", "de", "pt", "nl", "pl", "sv", "da", "cs", "ro", "el", "tr", "ru", "uk", "ja", "ko", "zh", "ar", "he", "hi", "id", "vi", "th"
        )
        private val MUSIC_TERMS = setOf(
            "singer", "rapper", "musician", "songwriter", "composer", "record producer", "disc jockey", "dj", "musical artist", "music group", "musical group", "band", "duo", "vocalist",
            "cantante", "musicista", "cantautore", "cantautrice", "compositore", "compositrice", "produttore discografico", "produttrice discografica", "gruppo musicale", "duo musicale",
            "chanteur", "chanteuse", "rappeur", "rappeuse", "musicien", "musicienne", "auteur compositeur", "compositeur", "compositrice", "producteur de musique", "groupe musical",
            "sanger", "sangerin", "musiker", "musikerin", "komponist", "komponistin", "musikproduzent", "musikgruppe",
            "rapero", "rapera", "musico", "musica", "compositor", "compositora", "productor musical", "grupo musical", "banda",
            "cantor", "cantora", "compositor", "produtor musical", "grupo musical",
            "певец", "певица", "рэпер", "музыкант", "композитор", "музыкальная группа",
            "співак", "співачка", "репер", "музикант", "композитор", "музичний гурт",
            "歌手", "音乐家", "音樂家", "说唱歌手", "說唱歌手", "作曲家", "乐队", "樂隊", "音乐组合", "音樂組合", "音楽家", "ラッパー", "音楽グループ", "밴드", "가수", "래퍼", "음악가", "작곡가", "음악 그룹",
            "مغني", "مغنية", "مغن", "رابر", "موسيقي", "ملحن", "فرقة موسيقية",
            "गायक", "गायिका", "रैपर", "संगीतकार", "संगीत समूह",
            "şarkıcı", "rapçi", "müzisyen", "besteci", "müzik grubu",
            "zanger", "zangeres", "rapper", "muzikant", "componist", "muziekgroep",
            "τραγουδιστής", "τραγουδίστρια", "ράπερ", "μουσικός", "συνθέτης", "μουσικό συγκρότημα"
        )
        private val GROUP_TERMS = setOf("band", "group", "duo", "trio", "ensemble", "gruppo", "banda", "groupe", "gruppe", "группа", "гурт", "فرقة", "그룹", "バンド")
        private val NON_ARTIST_TITLE_TERMS = setOf("album", "song", "single", "ep", "film", "soundtrack", "brano", "canzone", "singolo", "disco", "pellicola", "chanson", "titre", "lied", "cancion", "sencillo")
        private val NON_ARTIST_DESCRIPTION_TERMS = setOf("album by", "song by", "single by", "film directed", "album di", "brano di", "singolo di", "canzone di")
        private val DISAMBIGUATION_TERMS = setOf("may refer to", "puo riferirsi", "peut faire reference", "kann sich beziehen", "puede referirse", "曖昧さ回避", "동음이의어", "פירושונים")
        private val MUSIC_GROUP_INSTANCE_IDS = setOf("Q215380", "Q2088357", "Q5741069", "Q10648343", "Q216337")
        private val MUSIC_OCCUPATION_IDS = setOf("Q177220", "Q2252262", "Q639669", "Q753110", "Q36834", "Q183945", "Q130857", "Q488205", "Q855091")
        private val NON_ARTIST_INSTANCE_IDS = setOf("Q482994", "Q7366", "Q134556", "Q11424", "Q5398426", "Q2188189", "Q386724")
    }
}

private data class LoreNetworkOutcome(
    val biography: ArtistBiography?,
    val completedWithoutTransientFailure: Boolean
)

private data class LorePageCollection(
    val candidates: List<LoreCandidate>,
    val completedWithoutTransientFailure: Boolean
)

private data class LoreCandidate(
    val pageTitle: String,
    val pageId: Int,
    val entityId: String,
    val description: String,
    val extract: String,
    val thumbnailUrl: String,
    val originalImageUrl: String,
    val sourceUrl: String,
    val baseScore: Int,
    val finalScore: Int
) {
    fun enrich(summary: LoreSummary): LoreCandidate {
        return copy(
            pageTitle = summary.pageTitle.ifBlank { pageTitle },
            pageId = summary.pageId.takeIf { it > 0 } ?: pageId,
            entityId = summary.entityId.ifBlank { entityId },
            description = summary.description.ifBlank { description },
            extract = summary.extract.ifBlank { extract },
            thumbnailUrl = summary.thumbnailUrl.ifBlank { thumbnailUrl },
            originalImageUrl = summary.originalImageUrl.ifBlank { originalImageUrl },
            sourceUrl = summary.sourceUrl.ifBlank { sourceUrl }
        )
    }

    fun toBiography(languageCode: String, confidence: Int): ArtistBiography? {
        val cleanText = extract.trim()
        if (cleanText.length < 80) return null
        return ArtistBiography(
            text = cleanText,
            description = description.trim(),
            sourceLabel = "Wikipedia",
            sourceUrl = sourceUrl,
            languageCode = languageCode,
            pageTitle = pageTitle,
            pageId = pageId,
            entityId = entityId,
            thumbnailUrl = thumbnailUrl,
            originalImageUrl = originalImageUrl,
            confidence = confidence,
            cached = false
        )
    }
}

private data class LoreSummary(
    val pageTitle: String,
    val pageId: Int,
    val entityId: String,
    val description: String,
    val extract: String,
    val thumbnailUrl: String,
    val originalImageUrl: String,
    val sourceUrl: String
)

private data class LoreEntitySignals(
    val instanceOf: Set<String>,
    val occupations: Set<String>,
    val youtubeChannelIds: Set<String>
)

private data class LoreEntitySearchHit(
    val entityId: String,
    val languageCode: String,
    val label: String,
    val description: String,
    val matchText: String,
    val aliases: List<String>
)

private data class LoreEntityRecord(
    val entityId: String,
    val labels: Map<String, String>,
    val descriptions: Map<String, String>,
    val aliases: Map<String, List<String>>,
    val sitelinks: Map<String, String>,
    val signals: LoreEntitySignals
) {
    fun descriptionFor(languageCode: String): String {
        return descriptions[languageCode].orEmpty().ifBlank { descriptions["en"].orEmpty() }
    }

    fun toDescriptionBiography(
        preferredLanguages: Set<String>,
        confidence: Int
    ): ArtistBiography? {
        val language = preferredLanguages.firstOrNull { descriptions[it].orEmpty().isNotBlank() }
            ?: descriptions.keys.firstOrNull()
            ?: return null
        val description = descriptions[language].orEmpty().trim()
        if (description.length < 12) return null
        val label = labels[language].orEmpty().ifBlank { labels["en"].orEmpty() }
        val sentence = description.replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(Locale.forLanguageTag(language)) else character.toString()
        }.let { value -> if (value.endsWith('.')) value else "$value." }
        return ArtistBiography(
            text = sentence,
            description = "",
            sourceLabel = "Wikidata",
            sourceUrl = "https://www.wikidata.org/wiki/$entityId",
            languageCode = language,
            pageTitle = label,
            pageId = 0,
            entityId = entityId,
            confidence = confidence,
            cached = false
        )
    }
}

private sealed interface EntityRecordsResult {
    data class Success(val records: List<LoreEntityRecord>) : EntityRecordsResult
    data object Failure : EntityRecordsResult
}

private sealed interface JsonResult {
    data class Success(val value: JSONObject) : JsonResult
    data object Failure : JsonResult
}

private sealed interface HttpResult {
    data class Success(val body: String) : HttpResult
    data object Failure : HttpResult
}

private suspend fun okhttp3.OkHttpClient.await(request: Request): HttpResult = suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, error: IOException) {
            if (continuation.isActive) continuation.resume(HttpResult.Failure)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    if (continuation.isActive) continuation.resume(HttpResult.Failure)
                    return
                }
                val body = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
                if (continuation.isActive) continuation.resume(HttpResult.Success(body))
            }
        }
    })
}

private fun ArtistLoreEntity.signature(): String {
    return "$languageCode|$pageId|$entityId|$confidence|${text.hashCode()}|${description.hashCode()}|${sourceUrl.hashCode()}|${originalImageUrl.hashCode()}"
}

private fun ArtistLoreEntity.toBiography(cached: Boolean): ArtistBiography {
    val label = if (sourceUrl.contains("wikidata.org", ignoreCase = true)) "Wikidata" else "Wikipedia"
    return ArtistBiography(
        text = text,
        description = description,
        sourceLabel = label,
        sourceUrl = sourceUrl,
        languageCode = languageCode,
        pageTitle = pageTitle,
        pageId = pageId,
        entityId = entityId,
        thumbnailUrl = thumbnailUrl,
        originalImageUrl = originalImageUrl,
        confidence = confidence,
        cached = cached
    )
}
