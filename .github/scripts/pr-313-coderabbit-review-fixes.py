from __future__ import annotations

import json
import re
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_region(text: str, start: str, end: str, replacement: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise RuntimeError(f"{label}: start marker not found")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise RuntimeError(f"{label}: end marker not found")
    return text[:start_index] + replacement + text[end_index:]


# ---------------------------------------------------------------------------
# YouTube Music: preserve cancellation, classify video renderers, and avoid
# false preference matches for very short artist names.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "import kotlinx.coroutines.Dispatchers\n",
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.Dispatchers\n",
    "CancellationException import",
)
text = replace_once(
    text,
    "import org.json.JSONObject\n",
    "import org.json.JSONObject\nimport timber.log.Timber\n",
    "Timber import",
)
text = replace_once(
    text,
    '''private fun matchingArtistSignalIndex(artist: String, signals: List<String>): Int {
    if (artist.length < 2) return -1
    return signals.indexOfFirst { signal ->
        signal.length >= 2 && (artist == signal || artist.contains(signal) || signal.contains(artist))
    }
}
''',
    '''private fun matchingArtistSignalIndex(artist: String, signals: List<String>): Int {
    if (artist.length < 2) return -1
    return signals.indexOfFirst { signal ->
        when {
            signal.length < 2 -> false
            artist == signal -> true
            artist.length >= 4 && signal.length >= 4 ->
                artist.contains(signal) || signal.contains(artist)
            else -> false
        }
    }
}
''',
    "short artist preference matching",
)
text = replace_once(
    text,
    '''        val nativeExplore = runCatching { explore(languageCode) }.getOrDefault(YoutubeMusicExplore())
''',
    '''        val nativeExplore = try {
            explore(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Music Explore fallback failed for %s", languageCode)
            YoutubeMusicExplore()
        }
''',
    "new releases cancellation",
)
text = replace_once(
    text,
    '''                        runCatching {
                            searchMusicVideoSamples(
                                query = query,
                                languageCode = languageCode,
                                limit = YOUTUBE_MUSIC_SAMPLE_RESULTS_PER_QUERY
                            )
                        }.getOrDefault(emptyList())
''',
    '''                        try {
                            searchMusicVideoSamples(
                                query = query,
                                languageCode = languageCode,
                                limit = YOUTUBE_MUSIC_SAMPLE_RESULTS_PER_QUERY
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Timber.w(error, "YouTube Music sample query failed: %s", query)
                            emptyList()
                        }
''',
    "sample query cancellation",
)
text = replace_once(
    text,
    '''        val nativeVideos = runCatching { explore(languageCode).newVideos }
            .getOrDefault(emptyList())
            .mapNotNull(::asYoutubeMusicSample)
''',
    '''        val nativeVideos = try {
            explore(languageCode).newVideos
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Music native video shelf failed for %s", languageCode)
            emptyList()
        }.mapNotNull(::asYoutubeMusicSample)
''',
    "native sample shelf cancellation",
)
text = replace_once(
    text,
    '''            source = "YouTube Music",
            artistBrowseIds = artistReferences.map { it.browseId }
        )
''',
    '''            source = "YouTube Music",
            artistBrowseIds = artistReferences.map { it.browseId },
            videoType = findStringUnderKey(renderer, "musicVideoType").orEmpty()
        )
''',
    "music renderer video type",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Shorts cache: one bounded entry per language, with profile validation inside
# the payload rather than one SharedPreferences key for every profile mutation.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsCache.kt")
path.write_text(
    '''package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale

private const val SHORTS_CACHE_NAME = "levyra_shorts_cache"
private const val SHORTS_CACHE_LIMIT = 24
private const val SHORTS_CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1_000L

internal data class YoutubeShortsCacheSnapshot(
    val tracks: List<Track>,
    val savedAtMs: Long
) {
    fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean =
        tracks.isNotEmpty() && savedAtMs > 0L && nowMs - savedAtMs <= SHORTS_CACHE_MAX_AGE_MS
}

/** Keeps the last verified Shorts feed available synchronously while the network refreshes. */
internal class YoutubeShortsCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        SHORTS_CACHE_NAME,
        Context.MODE_PRIVATE
    )

    fun load(languageCode: String, profileSignature: String = ""): YoutubeShortsCacheSnapshot {
        val raw = preferences.getString(cacheKey(languageCode), null).orEmpty()
        if (raw.isBlank()) return YoutubeShortsCacheSnapshot(emptyList(), 0L)
        return runCatching {
            val root = JSONObject(raw)
            val requestedProfile = normalizedProfileSignature(profileSignature)
            val storedProfile = normalizedProfileSignature(root.optString("profileSignature"))
            if (requestedProfile != storedProfile) {
                return@runCatching YoutubeShortsCacheSnapshot(emptyList(), 0L)
            }
            val items = root.optJSONArray("tracks") ?: JSONArray()
            val tracks = buildList {
                for (index in 0 until items.length()) {
                    items.optJSONObject(index)
                        ?.let(TrackJson::fromJson)
                        ?.takeIf(::isYoutubeShortTrack)
                        ?.let(::add)
                }
            }
                .distinctBy { track -> track.id }
                .take(SHORTS_CACHE_LIMIT)
            YoutubeShortsCacheSnapshot(
                tracks = tracks,
                savedAtMs = root.optLong("savedAtMs", 0L)
            )
        }.getOrElse { error ->
            Timber.w(error, "Unable to read Shorts cache for %s", languageCode)
            YoutubeShortsCacheSnapshot(emptyList(), 0L)
        }
    }

    fun save(
        languageCode: String,
        tracks: List<Track>,
        savedAtMs: Long = System.currentTimeMillis(),
        profileSignature: String = ""
    ) {
        val verified = tracks
            .asSequence()
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(SHORTS_CACHE_LIMIT)
            .toList()
        if (verified.isEmpty()) return
        val array = JSONArray().apply {
            verified.forEach { track -> put(TrackJson.toJson(track)) }
        }
        val root = JSONObject()
            .put("savedAtMs", savedAtMs.coerceAtLeast(0L))
            .put("profileSignature", normalizedProfileSignature(profileSignature))
            .put("tracks", array)
        preferences.edit().putString(cacheKey(languageCode), root.toString()).apply()
    }

    private fun cacheKey(languageCode: String): String =
        "shorts_${LevyraLanguageCatalog.normalize(languageCode)}"

    private fun normalizedProfileSignature(value: String): String =
        value.trim().lowercase(Locale.ROOT)
}
''',
    encoding="utf-8",
)


# ---------------------------------------------------------------------------
# NewPipe channel URLs: parse and allowlist real HTTPS YouTube origins.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "import org.schabi.newpipe.extractor.stream.StreamInfoItem\n",
    "import okhttp3.HttpUrl.Companion.toHttpUrlOrNull\nimport org.schabi.newpipe.extractor.stream.StreamInfoItem\n",
    "HttpUrl import",
)
text = replace_once(
    text,
    '''private const val YOUTUBE_FRONTEND = "https://www.youtube.com"
''',
    '''private const val YOUTUBE_FRONTEND = "https://www.youtube.com"
private val YOUTUBE_CHANNEL_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com")
''',
    "YouTube host allowlist",
)
old_channel_url = '''internal fun canonicalYoutubeChannelUrl(value: String): String? {
    val candidate = value.trim()
    if (candidate.isBlank()) return null
    if (candidate.startsWith("UC") && candidate.length >= 20 && '/' !in candidate) {
        return "$YOUTUBE_FRONTEND/channel/$candidate"
    }
    val absolute = when {
        candidate.startsWith("https://", ignoreCase = true) ||
            candidate.startsWith("http://", ignoreCase = true) -> candidate
        candidate.startsWith("/") -> "$YOUTUBE_FRONTEND$candidate"
        else -> return null
    }
    val normalized = absolute.substringBefore('?').substringBefore('#').trimEnd('/')
    return normalized.takeIf { url ->
        url.contains("youtube.com/channel/", ignoreCase = true) ||
            url.contains("youtube.com/@", ignoreCase = true) ||
            url.contains("youtube.com/c/", ignoreCase = true) ||
            url.contains("youtube.com/user/", ignoreCase = true)
    }
}
'''
new_channel_url = '''internal fun canonicalYoutubeChannelUrl(value: String): String? {
    val candidate = value.trim()
    if (candidate.isBlank()) return null
    if (candidate.startsWith("UC") && candidate.length >= 20 && '/' !in candidate) {
        return "$YOUTUBE_FRONTEND/channel/$candidate"
    }
    val absolute = when {
        candidate.startsWith("https://", ignoreCase = true) -> candidate
        candidate.startsWith("/") -> "$YOUTUBE_FRONTEND$candidate"
        else -> return null
    }
    val parsed = absolute.toHttpUrlOrNull() ?: return null
    if (
        parsed.scheme != "https" ||
        parsed.port != 443 ||
        parsed.username.isNotEmpty() ||
        parsed.password.isNotEmpty() ||
        parsed.host.lowercase(Locale.ROOT) !in YOUTUBE_CHANNEL_HOSTS
    ) return null

    val segments = parsed.pathSegments.filter(String::isNotBlank)
    val validPath = when {
        segments.size == 1 && segments.first().startsWith("@") ->
            segments.first().length > 1
        segments.size == 2 && segments.first() == "channel" ->
            segments.last().startsWith("UC") && segments.last().length >= 20
        segments.size == 2 && segments.first() in setOf("c", "user") ->
            segments.last().isNotBlank()
        else -> false
    }
    if (!validPath) return null
    return "$YOUTUBE_FRONTEND/${segments.joinToString("/")}"
}
'''
text = replace_once(text, old_channel_url, new_channel_url, "safe YouTube channel URL")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Explore destinations: use RTL-aware forward icon and include the status-bar
# inset when positioning scrollable content under the fixed header.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreDestinationScreens.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "import androidx.compose.foundation.layout.Spacer\n",
    "import androidx.compose.foundation.layout.Spacer\nimport androidx.compose.foundation.layout.WindowInsets\nimport androidx.compose.foundation.layout.asPaddingValues\n",
    "WindowInsets imports",
)
text = replace_once(
    text,
    "import androidx.compose.foundation.layout.statusBarsPadding\n",
    "import androidx.compose.foundation.layout.statusBars\nimport androidx.compose.foundation.layout.statusBarsPadding\n",
    "statusBars import",
)
text = replace_once(
    text,
    "import androidx.compose.material.icons.automirrored.rounded.ArrowBack\n",
    "import androidx.compose.material.icons.automirrored.rounded.ArrowBack\nimport androidx.compose.material.icons.automirrored.rounded.ArrowForward\n",
    "ArrowForward import",
)
text = text.replace("import androidx.compose.ui.graphics.graphicsLayer\n", "")
text = replace_once(
    text,
    '''        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            tint = LevyraText,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
        )
''',
    '''        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = LevyraText,
            modifier = Modifier.size(20.dp)
        )
''',
    "RTL-aware release arrow",
)
text = replace_once(
    text,
    '''    Box(
        modifier = Modifier
''',
    '''    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    Box(
        modifier = Modifier
''',
    "destination status-bar measurement",
)
text = replace_once(
    text,
    '''        content(PaddingValues(top = 94.dp))
''',
    '''        content(PaddingValues(top = statusBarTop + 94.dp))
''',
    "destination content inset",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# ViewModel: move SharedPreferences reads off the main thread and split the
# Samples pipeline into small cancellation-safe helpers.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''private data class SamplesPlaybackSession(
    val queue: PlaybackQueueSnapshot,
    val currentTrack: Track?,
    val videoMode: Boolean,
    val loopOnCompletion: Boolean,
    val wasPlaying: Boolean,
    val positionMs: Long
)
''',
    '''private data class SamplesPlaybackSession(
    val queue: PlaybackQueueSnapshot,
    val currentTrack: Track?,
    val videoMode: Boolean,
    val loopOnCompletion: Boolean,
    val wasPlaying: Boolean,
    val positionMs: Long
)

private data class SamplesDiscoveryInput(
    val seeds: List<Track>,
    val preferredArtists: List<String>,
    val preferredChannelIds: List<String>
)

private data class SamplesFeedLoadResult(
    val tracks: List<Track>,
    val conclusive: Boolean
)
''',
    "Samples helper data classes",
)
new_samples_pipeline = '''    private fun ensureMusicVideosLoaded() {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        val profileSignature = samplesDiscoveryProfileSignature(snapshot)
        if (musicVideosJob?.isActive == true) return
        if (
            musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature
        ) return

        val hasVerifiedShorts = snapshot.exploreVideos.isNotEmpty() &&
            snapshot.exploreVideos.all(::isYoutubeShortTrack)
        if (!hasVerifiedShorts && snapshot.exploreVideos.isNotEmpty()) {
            _state.update { current -> current.copy(exploreVideos = emptyList()) }
        }
        musicVideosJob = viewModelScope.launch {
            refreshSamplesFeed(languageCode, profileSignature)
        }
    }

    private suspend fun refreshSamplesFeed(languageCode: String, profileSignature: String) {
        val cached = withContext(Dispatchers.IO) {
            shortsCache.load(languageCode, profileSignature)
        }
        if (_state.value.languageCode != languageCode) return
        if (cached.tracks.isNotEmpty()) {
            publishCachedSamples(languageCode, cached.tracks)
            if (cached.isFresh()) {
                markSamplesLoaded(languageCode, profileSignature)
                return
            }
        }
        if (!samplesRetryAllowed(languageCode)) return

        publishSamplesLoading(languageCode)
        val input = buildSamplesDiscoveryInput(_state.value)
        val result = loadSamplesFeed(languageCode, input)
        if (_state.value.languageCode != languageCode) return
        if (!result.conclusive || result.tracks.isEmpty()) {
            publishSamplesFailure(languageCode)
            registerShortsFeedFailure(languageCode)
            return
        }

        markSamplesLoaded(languageCode, profileSignature)
        publishSamplesSuccess(languageCode, result.tracks)
        withContext(Dispatchers.IO) {
            shortsCache.save(
                languageCode = languageCode,
                tracks = result.tracks,
                profileSignature = profileSignature
            )
        }
    }

    private fun samplesRetryAllowed(languageCode: String): Boolean {
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
        }
        return System.currentTimeMillis() >= musicVideosRetryAfterMs
    }

    private fun buildSamplesDiscoveryInput(snapshot: LevyraUiState): SamplesDiscoveryInput {
        val seeds = buildList {
            snapshot.currentTrack?.let(::add)
            addAll(snapshot.recentListens)
            addAll(snapshot.favorites)
            addAll(snapshot.personalOrbitTracks)
            addAll(snapshot.homeResonanceTracks)
            addAll(snapshot.exploreTracks)
            addAll(snapshot.charts)
            snapshot.homeSections.forEach { section -> addAll(section.tracks) }
            addAll(snapshot.tracks)
        }
            .distinctBy { track -> track.id }
            .take(48)
        val preferredArtists = discoveryPreferredArtists(snapshot, 16)
        val preferredChannelIds = buildList {
            addAll(snapshot.followedArtists.map { artist -> artist.browseId })
            seeds.forEach { track -> addAll(track.artistBrowseIds) }
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(20)
        return SamplesDiscoveryInput(seeds, preferredArtists, preferredChannelIds)
    }

    private suspend fun loadSamplesFeed(
        languageCode: String,
        input: SamplesDiscoveryInput
    ): SamplesFeedLoadResult {
        val youtubeMusicSamples = try {
            repository.musicSamples(
                seeds = input.seeds,
                preferredArtists = input.preferredArtists,
                languageCode = languageCode,
                limit = EXPLORE_SHORTS_FEED_LIMIT
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Music Samples feed failed for %s", languageCode)
            emptyList()
        }
        if (youtubeMusicSamples.isNotEmpty()) {
            return SamplesFeedLoadResult(youtubeMusicSamples, conclusive = true)
        }

        val fallback = try {
            shortsRepository.feed(
                seeds = input.seeds,
                languageCode = languageCode,
                preferredArtists = input.preferredArtists,
                preferredChannelIds = input.preferredChannelIds,
                limit = EXPLORE_SHORTS_FEED_LIMIT
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "NewPipe Shorts fallback failed for %s", languageCode)
            null
        }
        return SamplesFeedLoadResult(
            tracks = fallback?.tracks.orEmpty(),
            conclusive = fallback?.isConclusive == true
        )
    }

    private fun publishCachedSamples(languageCode: String, tracks: List<Track>) {
        _state.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(
                    exploreVideos = tracks,
                    isSamplesLoading = false,
                    samplesLoadFailed = false
                )
            } else {
                current
            }
        }
    }

    private fun publishSamplesLoading(languageCode: String) {
        _state.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(isSamplesLoading = true, samplesLoadFailed = false)
            } else {
                current
            }
        }
    }

    private fun publishSamplesFailure(languageCode: String) {
        _state.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(isSamplesLoading = false, samplesLoadFailed = true)
            } else {
                current
            }
        }
    }

    private fun publishSamplesSuccess(languageCode: String, tracks: List<Track>) {
        _state.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(
                    exploreVideos = tracks,
                    isSamplesLoading = false,
                    samplesLoadFailed = false
                )
            } else {
                current
            }
        }
    }

    private fun markSamplesLoaded(languageCode: String, profileSignature: String) {
        musicVideosLoadedLanguage = languageCode
        musicVideosLoadedProfileSignature = profileSignature
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
    }

'''
text = replace_region(
    text,
    "    private fun ensureMusicVideosLoaded() {\n",
    "    private fun registerShortsFeedFailure(languageCode: String) {\n",
    new_samples_pipeline,
    "Samples ViewModel pipeline",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Editorial collector: tolerate null hints, pass configured limits down to the
# source, and keep unavailable RU/CN release feeds optional.
# ---------------------------------------------------------------------------
path = Path("tools/levyra-editorial/levyra_editorial/collector.py")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:
        """Return all ordered playlist items."""
''',
    '''    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        """Return ordered playlist items, optionally bounded by ``limit``."""
''',
    "collector protocol item limit",
)
text = replace_once(
    text,
    '''        for value in item.get("titleHints", [])
''',
    '''        for value in item.get("titleHints") or []
''',
    "nullable title hints",
)
text = replace_once(
    text,
    '''            metadata = client.get_playlist_metadata(playlist_id)
            raw_items = client.iter_playlist_items(playlist_id)[:item_limit]
''',
    '''            raw_items = client.iter_playlist_items(playlist_id, limit=item_limit)
            metadata = client.get_playlist_metadata(playlist_id)
''',
    "bounded source collection",
)
path.write_text(text, encoding="utf-8")

path = Path("tools/levyra-editorial/levyra_editorial/resilient.py")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:
        return self._spotify.iter_playlist_items(playlist_id)
''',
    '''    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        return self._spotify.iter_playlist_items(playlist_id, limit=limit)
''',
    "central client bounded items",
)
path.write_text(text, encoding="utf-8")

path = Path("tools/levyra-editorial/levyra_editorial/spotify.py")
text = path.read_text(encoding="utf-8")
old_iter = '''    def iter_playlist_items(
        self,
        playlist_id: str,
    ) -> list[dict[str, Any]]:
        """Fetch every track item from one configured playlist, preserving order."""
        normalized_id = self._validate_playlist_id(playlist_id)
        offset = 0
        output: list[dict[str, Any]] = []
        total: int | None = None

        while total is None or offset < total:
            page = self._get_playlist_page(normalized_id, offset=offset)
            playlist = _playlist_union(page)
            content = _mapping(playlist.get("content"))
            if content is None:
                raise SourceApiError(
                    "The editorial playlist response is missing its content page."
                )
            raw_items = content.get("items")
            if not isinstance(raw_items, list):
                raise SourceApiError(
                    "The editorial playlist response has an invalid item list."
                )
            if total is None:
                total = _non_negative_int(content.get("totalCount"))
            converted = [
                converted_item if converted_item is not None else {"track": None}
                for raw_item in raw_items
                for converted_item in [
                    _convert_playlist_item(raw_item)
                    if isinstance(raw_item, Mapping)
                    else None
                ]
            ]
            output.extend(converted)

            consumed = len(raw_items)
            if consumed == 0:
                break
            offset += consumed
            if total is None or total <= offset:
                break

        return output
'''
new_iter = '''    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        """Fetch ordered playlist items without materializing beyond ``limit``."""
        if limit is not None and (
            not isinstance(limit, int) or isinstance(limit, bool) or limit <= 0
        ):
            raise ValueError("Playlist item limit must be a positive integer.")
        normalized_id = self._validate_playlist_id(playlist_id)
        offset = 0
        output: list[dict[str, Any]] = []
        total: int | None = None

        while (total is None or offset < total) and (
            limit is None or len(output) < limit
        ):
            remaining = None if limit is None else limit - len(output)
            page_limit = 100 if remaining is None else min(100, remaining)
            page = self._get_playlist_page(
                normalized_id,
                offset=offset,
                limit=page_limit,
            )
            playlist = _playlist_union(page)
            content = _mapping(playlist.get("content"))
            if content is None:
                raise SourceApiError(
                    "The editorial playlist response is missing its content page."
                )
            raw_items = content.get("items")
            if not isinstance(raw_items, list):
                raise SourceApiError(
                    "The editorial playlist response has an invalid item list."
                )
            if total is None:
                total = _non_negative_int(content.get("totalCount"))
            converted = [
                converted_item if converted_item is not None else {"track": None}
                for raw_item in raw_items
                for converted_item in [
                    _convert_playlist_item(raw_item)
                    if isinstance(raw_item, Mapping)
                    else None
                ]
            ]
            if remaining is not None:
                converted = converted[:remaining]
            output.extend(converted)

            consumed = len(raw_items)
            if consumed == 0:
                break
            offset += consumed
            if total is None or total <= offset:
                break

        return output
'''
text = replace_once(text, old_iter, new_iter, "bounded Spotify playlist iteration")
path.write_text(text, encoding="utf-8")

config_path = Path("tools/levyra-editorial/config.json")
config = json.loads(config_path.read_text(encoding="utf-8"))
for collection in config.get("collections", []):
    if collection.get("id") in {"new-releases-ru", "new-releases-cn"}:
        collection["optional"] = True
config_path.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# Regression tests for every behavior changed above.
# ---------------------------------------------------------------------------
path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicNewReleasesPolicyTest.kt")
text = path.read_text(encoding="utf-8")
insert = '''
    @Test
    fun shortArtistSignalsRequireAnExactMatch() {
        val exact = release(
            artist = "Io",
            title = "Release esatta",
            id = "exact-short",
            type = ReleaseType.Single,
            artistBrowseId = ""
        )
        val falsePositive = release(
            artist = "Dio",
            title = "Non correlata",
            id = "false-positive",
            type = ReleaseType.Unknown,
            artistBrowseId = ""
        )

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(falsePositive, exact),
            preferredArtists = listOf("Io"),
            popularArtists = emptyList(),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("MPREexact-short"), ranked.map { it.browseId })
    }
'''
text = replace_once(
    text,
    '''    private fun release(
''',
    insert + '''
    private fun release(
''',
    "short artist regression test",
)
path.write_text(text, encoding="utf-8")

path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeShortsRepositoryTest.kt")
text = path.read_text(encoding="utf-8")
insert = '''
    @Test
    fun channelUrlsRejectLookalikeHostsAndUnsafeOrigins() {
        assertEquals(null, canonicalYoutubeChannelUrl("https://youtube.com.evil.test/@artist"))
        assertEquals(null, canonicalYoutubeChannelUrl("http://www.youtube.com/@artist"))
        assertEquals(null, canonicalYoutubeChannelUrl("https://user@www.youtube.com/@artist"))
        assertEquals(null, canonicalYoutubeChannelUrl("https://www.youtube.com:444/@artist"))
        assertEquals(null, canonicalYoutubeChannelUrl("https://www.youtube.com/watch?v=abcdefghijk"))
        assertEquals(
            "https://www.youtube.com/@artist",
            canonicalYoutubeChannelUrl("https://m.youtube.com/@artist?feature=shared")
        )
    }
'''
text = replace_once(
    text,
    '''    private fun track(
''',
    insert + '''
    private fun track(
''',
    "safe channel URL tests",
)
path.write_text(text, encoding="utf-8")

# Update Python doubles to accept the optional source limit.
for test_path in Path("tools/levyra-editorial/tests").glob("test_*.py"):
    test_text = test_path.read_text(encoding="utf-8")
    test_text = re.sub(
        r"def iter_playlist_items\(self, playlist_id: str\)",
        "def iter_playlist_items(self, playlist_id: str, limit: int | None = None)",
        test_text,
    )
    test_text = re.sub(
        r"def iter_playlist_items\(\n(\s+)self,\n\1playlist_id: str,\n\s*\)",
        lambda match: (
            "def iter_playlist_items(\n"
            f"{match.group(1)}self,\n"
            f"{match.group(1)}playlist_id: str,\n"
            f"{match.group(1)}limit: int | None = None,\n"
            "    )"
        ),
        test_text,
    )
    test_path.write_text(test_text, encoding="utf-8")

path = Path("tools/levyra-editorial/tests/test_collector.py")
text = path.read_text(encoding="utf-8")
insert = '''

class LimitRecordingClient(FakeClient):
    def __init__(self) -> None:
        self.requested_limit: int | None = None

    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict]:
        self.requested_limit = limit
        items = super().iter_playlist_items(playlist_id, limit=limit)
        return items if limit is None else items[:limit]


def test_config_accepts_null_title_hints(tmp_path: Path) -> None:
    config_path = tmp_path / "config.json"
    config_path.write_text(
        json.dumps(
            {
                "schemaVersion": 1,
                "collections": [
                    {
                        "id": "new-releases-it",
                        "kind": "release",
                        "market": "IT",
                        "playlistQuery": "New Music Friday Italia",
                        "titleHints": None,
                        "fallbackPlaylistId": "37i9dQZF1DWVKDF4ycOESi",
                    }
                ],
            }
        ),
        encoding="utf-8",
    )

    config = load_config(config_path)

    assert config["collections"][0]["titleHints"] is None


def test_collection_limit_is_forwarded_to_source() -> None:
    client = LimitRecordingClient()
    config = {
        "collections": [
            {
                "id": "new-releases-it",
                "kind": "release",
                "market": "IT",
                "playlistId": "37i9dQZF1DWVKDF4ycOESi",
                "limit": 1,
            }
        ]
    }

    catalog = build_catalog(config, client, generated_at="2026-08-06T12:00:00Z")

    assert client.requested_limit == 1
    assert len(catalog.collections[0].tracks) == 1
'''
text += insert
path.write_text(text, encoding="utf-8")

path = Path("tools/levyra-editorial/tests/test_pathfinder.py")
text = path.read_text(encoding="utf-8")
text += '''


def test_pathfinder_playlist_limit_reaches_request_variables() -> None:
    session = PathfinderSession(FakeResponse(playlist_payload()))
    client = authenticated_client(session)

    items = client.iter_playlist_items("playlist12345", limit=1)

    assert len(items) == 1
    _, kwargs = session.calls[0]
    variables = json.loads(kwargs["params"]["variables"])
    assert variables["limit"] == 1
'''
text = replace_once(
    text,
    "from __future__ import annotations\n\n",
    "from __future__ import annotations\n\nimport json\n",
    "pathfinder json import",
)
path.write_text(text, encoding="utf-8")

# The Room schema export is unrelated to this Explore PR: database version 14
# and MIGRATION_13_14 already existed on main before the branch was created.
schema_path = Path(
    "app/schemas/com.luc4n3x.levyra.data.local.LevyraDatabase/14.json"
)
if schema_path.exists():
    schema_path.unlink()

print("Applied all valid CodeRabbit review fixes for PR #313")
