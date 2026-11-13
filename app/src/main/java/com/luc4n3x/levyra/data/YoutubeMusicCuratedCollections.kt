package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

internal enum class CuratedCollection(val cacheName: String) { Decades("decades"), Everyday("everyday") }

internal data class CuratedCollectionCard(
    val title: String,
    val subtitle: String,
    val playlistId: String,
    val thumbnailUrl: String
)

internal data class CuratedCollectionSnapshot(val savedAtMs: Long, val cards: List<CuratedCollectionCard>) {
    fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean =
        cards.isNotEmpty() && savedAtMs > 0L && nowMs - savedAtMs in 0 until CURATED_COLLECTION_TTL_MS
}

internal object YoutubeMusicDecades {
    const val PARAMS = "ggMPOg1uX1V4cTVVT0JjY0xI"
    private const val FIRST_DECADE = 1960
    private const val LAST_DECADE = 2010

    fun decadeOf(title: String): Int? {
        val runs = mutableListOf<String>()
        val digits = StringBuilder()
        for (char in title) {
            val digit = Character.digit(char, 10)
            if (digit >= 0) {
                digits.append(digit)
            } else if (digits.isNotEmpty()) {
                runs += digits.toString()
                digits.clear()
            }
        }
        if (digits.isNotEmpty()) runs += digits.toString()
        runs.firstOrNull { it.length == 4 && it.endsWith('0') }?.let { year ->
            return year.toInt().takeIf { it in 1900..2090 }
        }
        val short = runs.lastOrNull { it.length == 2 && it.endsWith('0') }?.toInt() ?: return null
        return if (short >= 30) 1900 + short else 2000 + short
    }

    fun cards(sections: List<YoutubeMusicMoodSection>): List<CuratedCollectionCard> {
        val byDecade = sortedMapOf<Int, CuratedCollectionCard>()
        val used = HashSet<String>()
        sections.forEach { section ->
            val decade = decadeOf(section.title)?.takeIf { it in FIRST_DECADE..LAST_DECADE } ?: return@forEach
            if (decade in byDecade) return@forEach
            val playlist = section.playlists.firstOrNull { it.isCardReady() && used.add(it.playlistId) } ?: return@forEach
            byDecade[decade] = playlist.toCard(section.title.trim())
        }
        return byDecade.values.toList()
    }
}

internal object YoutubeMusicEverydayMoods {
    val params = listOf(
        "ggMPOg1uXzIxYkNac21YZ2Z0",
        "ggMPOg1uX0NvNGNhWThMYWRh",
        "ggMPOg1uX1JOQWZFeDByc2Jm",
        "ggMPOg1uX1MxaFQ3Z0JMZkN4",
        "ggMPOg1uX2w1aW1CRDFTSUNo",
        "ggMPOg1uX044Z2o5WERLckpU",
        "ggMPOg1uXzZQbDB5eThLRTQ3"
    )

    fun available(categories: List<ExploreCategory>): Map<String, String> {
        val titles = categories.filter { it.title.isNotBlank() }.associate { it.params to it.title.trim() }
        return params.mapNotNull { param -> titles[param]?.let { param to it } }.toMap()
    }

    fun cards(titles: Map<String, String>, playlists: Map<String, List<YoutubeMusicMoodPlaylist>>): List<CuratedCollectionCard> {
        val used = HashSet<String>()
        return params.mapNotNull { param ->
            val title = titles[param] ?: return@mapNotNull null
            playlists[param].orEmpty().firstOrNull { it.isCardReady() && used.add(it.playlistId) }?.toCard(title)
        }
    }
}

private fun YoutubeMusicMoodPlaylist.isCardReady(): Boolean =
    playlistId.isNotBlank() && title.isNotBlank() && thumbnailUrl.isNotBlank()

private fun YoutubeMusicMoodPlaylist.toCard(subtitle: String) =
    CuratedCollectionCard(title = title.trim(), subtitle = subtitle, playlistId = playlistId, thumbnailUrl = thumbnailUrl)

internal fun encodeCuratedCollection(snapshot: CuratedCollectionSnapshot): String =
    JSONObject()
        .put("schema", CURATED_COLLECTION_SCHEMA)
        .put("savedAtMs", snapshot.savedAtMs)
        .put("cards", JSONArray().apply {
            snapshot.cards.forEach { card ->
                put(
                    JSONObject()
                        .put("title", card.title)
                        .put("subtitle", card.subtitle)
                        .put("playlistId", card.playlistId)
                        .put("thumbnailUrl", card.thumbnailUrl)
                )
            }
        })
        .toString()

internal fun decodeCuratedCollection(raw: String): CuratedCollectionSnapshot? = runCatching {
    val root = JSONObject(raw)
    if (root.optInt("schema") != CURATED_COLLECTION_SCHEMA) return null
    val items = root.optJSONArray("cards") ?: return null
    val cards = (0 until minOf(items.length(), CURATED_COLLECTION_MAX_CARDS)).mapNotNull { index ->
        val item = items.optJSONObject(index) ?: return@mapNotNull null
        CuratedCollectionCard(
            title = item.optString("title").trim(),
            subtitle = item.optString("subtitle").trim(),
            playlistId = item.optString("playlistId").trim(),
            thumbnailUrl = item.optString("thumbnailUrl").trim()
        ).takeIf { it.title.isNotBlank() && it.playlistId.isNotBlank() && it.thumbnailUrl.startsWith("https://") }
    }
    CuratedCollectionSnapshot(root.optLong("savedAtMs", 0L), cards).takeIf { it.cards.isNotEmpty() }
}.getOrNull()

internal class YoutubeMusicCuratedCollectionRepository(context: Context, private val youtube: YoutubeMusicRepository) {
    private val cacheDir: File by lazy { File(context.applicationContext.cacheDir, CACHE_DIRECTORY) }

    suspend fun cached(collection: CuratedCollection, languageCode: String): CuratedCollectionSnapshot? =
        withContext(Dispatchers.IO) { read(cacheKey(collection, languageCode)) }

    suspend fun refresh(collection: CuratedCollection, languageCode: String): List<CuratedCollectionCard>? = withContext(Dispatchers.IO) {
        val key = cacheKey(collection, languageCode)
        val cards = try {
            when (collection) {
                CuratedCollection.Decades -> YoutubeMusicDecades.cards(youtube.moodPlaylistSections(YoutubeMusicDecades.PARAMS, languageCode))
                CuratedCollection.Everyday -> everyday(languageCode)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Curated collection refresh failed")
            return@withContext null
        }
        if (cards.isEmpty()) return@withContext null
        write(key, CuratedCollectionSnapshot(System.currentTimeMillis(), cards))
        cards
    }

    private suspend fun everyday(languageCode: String): List<CuratedCollectionCard> {
        val titles = YoutubeMusicEverydayMoods.available(youtube.moodCategories(languageCode))
        if (titles.isEmpty()) return emptyList()
        val playlists = coroutineScope {
            titles.keys.map { param -> async { param to youtube.moodPlaylists(param, languageCode, EVERYDAY_PLAYLISTS_PER_MOOD) } }.awaitAll()
        }.toMap()
        return YoutubeMusicEverydayMoods.cards(titles, playlists)
    }

    private fun cacheKey(collection: CuratedCollection, languageCode: String): String =
        collection.cacheName + "-" + LevyraLanguageCatalog.normalize(languageCode).replace(Regex("[^A-Za-z0-9-]"), "_")

    private fun read(key: String): CuratedCollectionSnapshot? {
        memory[key]?.let { return it }
        val file = File(cacheDir, "$key.json")
        if (!file.isFile || file.length() > MAX_CACHE_BYTES) return null
        return runCatching { decodeCuratedCollection(file.readText()) }.getOrNull()?.also { memory[key] = it }
    }

    private fun write(key: String, snapshot: CuratedCollectionSnapshot) {
        memory[key] = snapshot
        runCatching {
            cacheDir.mkdirs()
            val target = File(cacheDir, "$key.json")
            val temporary = File(cacheDir, "$key.json.tmp")
            temporary.writeText(encodeCuratedCollection(snapshot))
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }.onFailure { error -> Timber.w(error, "Curated collection cache write failed") }
    }

    private companion object {
        const val CACHE_DIRECTORY = "ytm-curated-collections"
        const val EVERYDAY_PLAYLISTS_PER_MOOD = 8
        const val MAX_CACHE_BYTES = 256L * 1024L
        val memory = ConcurrentHashMap<String, CuratedCollectionSnapshot>()
    }
}

internal const val CURATED_COLLECTION_TTL_MS = 24L * 60L * 60L * 1000L
private const val CURATED_COLLECTION_SCHEMA = 1
private const val CURATED_COLLECTION_MAX_CARDS = 24
