from pathlib import Path

REPO_PATH = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
DESCRIPTION_PATH = Path("app/src/main/java/com/luc4n3x/levyra/data/AlbumDescriptionRepository.kt")
VIEW_MODEL_PATH = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
TEST_PATH = Path("app/src/test/java/com/luc4n3x/levyra/data/AlbumDescriptionBudgetTest.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


description = DESCRIPTION_PATH.read_text()
repository = REPO_PATH.read_text()
view_model = VIEW_MODEL_PATH.read_text()

description = description.replace("import kotlinx.coroutines.withTimeoutOrNull\n", "")

description = replace_once(
    description,
    '''private fun wikipediaAlbumTitleKey(value: String): String {
    var key = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\\p{M}+"""), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^\\p{L}\\p{N}]+"""), " ")
        .replace(Regex("""\\s+"""), " ")
        .trim()
    val editionSuffixes = listOf(
        " deluxe edition", " deluxe", " expanded edition", " expanded",
        " anniversary edition", " remastered edition", " remastered",
        " special edition", " collector edition", " bonus tracks"
    )
    editionSuffixes.firstOrNull(key::endsWith)?.let { suffix ->
        key = key.removeSuffix(suffix).trim()
    }
    return key
}

internal class AlbumDescriptionRepository(context: Context?) {''',
    '''private fun wikipediaAlbumTitleKey(value: String): String {
    var key = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\\p{M}+"""), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^\\p{L}\\p{N}]+"""), " ")
        .replace(Regex("""\\s+"""), " ")
        .trim()
    val editionSuffixes = listOf(
        " deluxe edition", " deluxe", " expanded edition", " expanded",
        " anniversary edition", " remastered edition", " remastered",
        " special edition", " collector edition", " bonus tracks"
    )
    editionSuffixes.firstOrNull(key::endsWith)?.let { suffix ->
        key = key.removeSuffix(suffix).trim()
    }
    return key
}

internal fun remainingAlbumDescriptionBudgetMillis(
    deadlineNanos: Long,
    nowNanos: Long = System.nanoTime()
): Long {
    val remainingNanos = deadlineNanos - nowNanos
    if (remainingNanos <= 0L) return 0L
    return TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
}

internal class AlbumDescriptionRepository(context: Context?) {''',
    "insert shared elapsed-time helper",
)

old_resolve = '''    suspend fun resolve(
        album: AlbumHit,
        languageCode: String,
        youtubeDescription: String,
        trackCount: Int
    ): String {
        val native = cleanDescription(youtubeDescription)
        if (native.length >= MIN_DESCRIPTION_LENGTH) return native

        val language = LevyraLanguageCatalog.normalize(languageCode)
        val key = "${descriptionKey(album.title)}|${descriptionKey(album.artist)}|$language"
        cache[key]?.let { return it }

        val remote = withTimeoutOrNull(REMOTE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                wikipediaDescription(album, language)
                    ?: wikidataDescription(album, language)
                    ?: spotifyDescription(album.canonicalUrl)
            }
        }
        val resolved = cleanDescription(remote.orEmpty())
            .takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: factualFallback(album, language, trackCount)
        cache[key] = resolved
        return resolved
    }
'''
new_resolve = '''    suspend fun resolve(
        album: AlbumHit,
        languageCode: String,
        youtubeDescription: String,
        trackCount: Int
    ): String {
        val native = cleanDescription(youtubeDescription)
        if (native.length >= MIN_DESCRIPTION_LENGTH) return native

        val language = LevyraLanguageCatalog.normalize(languageCode)
        val key = "${descriptionKey(album.title)}|${descriptionKey(album.artist)}|$language"
        cache[key]?.let { return it }

        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REMOTE_TIMEOUT_MS)
        val remote = withContext(Dispatchers.IO) {
            wikipediaDescription(album, language, deadlineNanos)
                ?: wikidataDescription(album, language, deadlineNanos)
                ?: spotifyDescription(album.canonicalUrl, deadlineNanos)
        }
        val resolved = cleanDescription(remote.orEmpty())
            .takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: factualFallback(album, language, trackCount)
        cache[key] = resolved
        return resolved
    }
'''
description = replace_once(description, old_resolve, new_resolve, "replace description resolver")

description = replace_once(
    description,
    '''    private fun wikipediaDescription(album: AlbumHit, languageCode: String): String? {
        val languages = linkedSetOf(languageCode, "en")
        for (language in languages) {''',
    '''    private fun wikipediaDescription(album: AlbumHit, languageCode: String, deadlineNanos: Long): String? {
        val languages = linkedSetOf(languageCode, "en")
        for (language in languages) {
            if (remainingAlbumDescriptionBudgetMillis(deadlineNanos) <= 0L) break''',
    "bound Wikipedia language loop",
)
description = replace_once(description, "            val pages = requestJson(url)\n", "            val pages = requestJson(url, deadlineNanos)\n", "pass Wikipedia deadline")
description = replace_once(
    description,
    "    private fun wikidataDescription(album: AlbumHit, languageCode: String): String? {",
    "    private fun wikidataDescription(album: AlbumHit, languageCode: String, deadlineNanos: Long): String? {",
    "bound Wikidata lookup",
)
description = replace_once(description, "        val results = requestJson(url)?.optJSONArray(\"search\") ?: return null", "        val results = requestJson(url, deadlineNanos)?.optJSONArray(\"search\") ?: return null", "pass Wikidata deadline")
description = replace_once(
    description,
    "    private fun spotifyDescription(canonicalUrl: String): String? {",
    "    private fun spotifyDescription(canonicalUrl: String, deadlineNanos: Long): String? {",
    "bound Spotify lookup",
)
description = replace_once(description, "        val html = requestText(url, \"text/html\") ?: return null", "        val html = requestText(url, \"text/html\", deadlineNanos) ?: return null", "pass Spotify deadline")

old_requests = '''    private fun requestJson(url: HttpUrl): JSONObject? {
        val body = requestText(url, "application/json") ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun requestText(url: HttpUrl, accept: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", "Levyra/Android album-metadata")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.take(MAX_RESPONSE_CHARS)
            }
        }.getOrNull()
    }
'''
new_requests = '''    private fun requestJson(url: HttpUrl, deadlineNanos: Long): JSONObject? {
        val body = requestText(url, "application/json", deadlineNanos) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun requestText(url: HttpUrl, accept: String, deadlineNanos: Long): String? {
        val remainingMs = remainingAlbumDescriptionBudgetMillis(deadlineNanos)
        if (remainingMs <= 0L) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", "Levyra/Android album-metadata")
            .build()
        val call = client.newCall(request)
        call.timeout().timeout(remainingMs, TimeUnit.MILLISECONDS)
        return runCatching {
            call.execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.take(MAX_RESPONSE_CHARS)
            }
        }.getOrNull()
    }
'''
description = replace_once(description, old_requests, new_requests, "replace bounded HTTP helpers")

old_detail_description = '''        val description = albumDescriptionRepository.resolve(
            album = finalAlbum,
            languageCode = languageCode,
            youtubeDescription = root?.let { parseAlbumDescription(it) }.orEmpty(),
            trackCount = enrichedTracks.size
        )'''
new_detail_description = '''        val description = root?.let { parseAlbumDescription(it) }.orEmpty()'''
repository = replace_once(repository, old_detail_description, new_detail_description, "make album detail non-blocking")

repository = replace_once(
    repository,
    '''        )
    }

    private fun moodCategoryScore(''',
    '''        )
    }

    suspend fun resolveAlbumDescription(
        detail: AlbumDetail,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): String = albumDescriptionRepository.resolve(
        album = detail.album,
        languageCode = languageCode,
        youtubeDescription = detail.description,
        trackCount = detail.trackCount
    )

    private fun moodCategoryScore(''',
    "add asynchronous album description entry point",
)

old_publish = '''            _state.update {
                it.copy(
                    albumLoading = false,
                    albumError = null,
                    albumDetail = detail,
                    tracks = mergeTracks(detail.tracks, it.tracks),
                    searchResults = detail.tracks.take(12),
                    cacheReport = repository.cacheReport()
                )
            }
            recordSmartAlbumOpen(detail.album)'''
new_publish = '''            _state.update {
                it.copy(
                    albumLoading = false,
                    albumError = null,
                    albumDetail = detail,
                    tracks = mergeTracks(detail.tracks, it.tracks),
                    searchResults = detail.tracks.take(12),
                    cacheReport = repository.cacheReport()
                )
            }
            launch {
                val description = runCatching {
                    repository.resolveAlbumDescription(detail, languageCode)
                }.getOrNull()?.trim().orEmpty()
                if (!isActive || description.isBlank() || description == detail.description) return@launch
                _state.update { currentState ->
                    val shownDetail = currentState.albumDetail ?: return@update currentState
                    val sameBrowseId = detail.album.browseId.isNotBlank() &&
                        shownDetail.album.browseId.equals(detail.album.browseId, ignoreCase = true)
                    val sameIdentity = shownDetail.album.title.equals(detail.album.title, ignoreCase = true) &&
                        shownDetail.album.artist.equals(detail.album.artist, ignoreCase = true)
                    if (!currentState.showAlbum || (!sameBrowseId && !sameIdentity)) currentState
                    else currentState.copy(albumDetail = shownDetail.copy(description = description))
                }
            }
            recordSmartAlbumOpen(detail.album)'''
view_model = replace_once(view_model, old_publish, new_publish, "backfill album description asynchronously")

TEST_PATH.write_text(
    '''package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class AlbumDescriptionBudgetTest {
    @Test
    fun providerChainSharesOneElapsedTimeBudget() {
        val deadline = TimeUnit.MILLISECONDS.toNanos(10_000L)

        assertEquals(
            7_000L,
            remainingAlbumDescriptionBudgetMillis(
                deadlineNanos = deadline,
                nowNanos = TimeUnit.MILLISECONDS.toNanos(3_000L)
            )
        )
        assertEquals(0L, remainingAlbumDescriptionBudgetMillis(deadline, deadline))
        assertEquals(0L, remainingAlbumDescriptionBudgetMillis(deadline, deadline + 1L))
    }

    @Test
    fun subMillisecondRemainderStillGetsOneMillisecondCallTimeout() {
        assertEquals(1L, remainingAlbumDescriptionBudgetMillis(2L, 1L))
    }
}
'''
)

DESCRIPTION_PATH.write_text(description)
REPO_PATH.write_text(repository)
VIEW_MODEL_PATH.write_text(view_model)
