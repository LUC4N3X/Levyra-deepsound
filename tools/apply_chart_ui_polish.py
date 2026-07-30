from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    (ROOT / path).write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    content = read(path)
    count = content.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one occurrence, found {count}: {old[:100]!r}")
    write(path, content.replace(old, new, 1))


def replace_regex(path: str, pattern: str, replacement: str, *, flags: int = 0) -> None:
    content = read(path)
    updated, count = re.subn(pattern, replacement, content, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"{path}: regex expected one occurrence, found {count}: {pattern[:100]!r}")
    write(path, updated)


# Let child scrollables consume gestures first. The final-pass blocker still prevents taps from
# reaching screens underneath a full-screen overlay.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "import androidx.compose.ui.input.pointer.pointerInput\n",
    "import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.input.pointer.PointerEventPass\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    '''private fun Modifier.consumeOverlayTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { change ->
                if (!change.isConsumed) change.consume()
            }
        }
    }
}
''',
    '''private fun Modifier.consumeOverlayTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.forEach { change ->
                if (!change.isConsumed) change.consume()
            }
        }
    }
}
''',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = if (state.currentTrack != null) 232.dp else 112.dp),",
    "contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 232.dp),",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "contentPadding = PaddingValues(top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),",
    "contentPadding = PaddingValues(top = 8.dp, bottom = 188.dp),",
)

# Make the whole country catalog available to the ViewModel in one background snapshot.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    '''    suspend fun cachedTopTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.tracks(country, limit)
    }
''',
    '''    suspend fun cachedTopTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.tracks(country, limit)
    }

    suspend fun cachedAllMarkets(limit: Int): Map<String, List<Track>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now) ?: refreshAsync().await() ?: return@withContext emptyMap()
        if (snapshot.needsRefresh(now)) warm()
        val safeLimit = limit.coerceIn(1, 100)
        snapshot.byMarket
            .mapValues { (_, tracks) -> tracks.take(safeLimit) }
            .filterValues { it.isNotEmpty() }
    }
''',
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    '''    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
''',
    '''    suspend fun cachedCountryCharts(limit: Int = 50): Map<String, List<Track>> = withContext(Dispatchers.IO) {
        editorialCharts.cachedAllMarkets(limit)
            .mapKeys { (market, _) -> market.lowercase() }
    }

    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
''',
)

# Add a YouTube Music artwork fallback when the official release lookup has no usable image.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    "import com.luc4n3x.levyra.domain.Track\n",
    "import com.luc4n3x.levyra.domain.LevyraPersonalOrbit\nimport com.luc4n3x.levyra.domain.Track\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    "    private val repositories = Array(PROVIDER_LANES) { OfficialArtworkRepository(appContext) }\n",
    "    private val repositories = Array(PROVIDER_LANES) { OfficialArtworkRepository(appContext) }\n    private val youtubeMusicRepository = YoutubeMusicRepository(appContext)\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    '''            if (official == null) {
                store.edit().putLong(missKey(key, country), System.currentTimeMillis()).apply()
                return@withLock null
            }
            val artwork = CachedArtwork.from(official)
''',
    '''            val artwork = if (official != null) {
                CachedArtwork.from(official)
            } else {
                findYoutubeFallback(track)
            }
            if (artwork == null) {
                store.edit().putLong(missKey(key, country), System.currentTimeMillis()).apply()
                return@withLock null
            }
''',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    '''    private fun repositoryFor(key: String): OfficialArtworkRepository {
''',
    '''    private suspend fun findYoutubeFallback(track: Track): CachedArtwork? {
        val candidates = runCatching {
            youtubeMusicRepository.search("${track.title} ${track.artist}", 8)
        }.getOrDefault(emptyList())
        val best = candidates
            .asSequence()
            .map { candidate -> candidate to youtubeMatchScore(track, candidate) }
            .filter { (_, score) -> score >= MIN_YOUTUBE_MATCH_SCORE }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: return null
        val prepared = LevyraPersonalOrbit.withoutVideoArtwork(best)
        val artwork = prepared.largeThumbnailUrl.trim()
            .ifBlank { prepared.thumbnailUrl.trim() }
            .ifBlank { best.largeThumbnailUrl.trim() }
            .ifBlank { best.thumbnailUrl.trim() }
        if (artwork.isBlank()) return null
        return CachedArtwork.fromYoutube(prepared.copy(thumbnailUrl = artwork, largeThumbnailUrl = artwork))
    }

    private fun youtubeMatchScore(target: Track, candidate: Track): Int {
        val targetTitle = ChartFeedParser.normalizeMusicText(target.title)
        val targetArtist = ChartFeedParser.normalizeMusicText(target.artist)
        val candidateTitle = ChartFeedParser.normalizeMusicText(candidate.title)
        val candidateArtist = ChartFeedParser.normalizeMusicText(candidate.artist)
        var score = when {
            candidateTitle == targetTitle -> 140
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 90
            else -> 0
        }
        score += when {
            candidateArtist == targetArtist -> 90
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 52
            else -> 0
        }
        if (target.durationMs > 0L && candidate.durationMs > 0L) {
            val delta = kotlin.math.abs(target.durationMs - candidate.durationMs)
            if (delta <= 6_000L) score += 30 else if (delta > 40_000L) score -= 30
        }
        val searchable = "${candidate.title} ${candidate.artist} ${candidate.album}".lowercase()
        if (listOf("karaoke", "cover", "reaction", "nightcore", "sped up", "slowed").any(searchable::contains)) score -= 70
        return score
    }

    private fun repositoryFor(key: String): OfficialArtworkRepository {
''',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    '''                canonicalAlbumUrl = canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl }
            )
''',
    '''                canonicalAlbumUrl = canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                moodTags = track.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
            )
''',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    '''            fun from(json: JSONObject): CachedArtwork = CachedArtwork(
''',
    '''            fun fromYoutube(value: Track): CachedArtwork = CachedArtwork(
                thumbnailUrl = value.thumbnailUrl,
                largeThumbnailUrl = value.largeThumbnailUrl.ifBlank { value.thumbnailUrl },
                album = value.album,
                provider = value.metadataProvider.ifBlank { value.source.ifBlank { "YouTube Music" } },
                canonicalAlbumUrl = value.canonicalAlbumUrl,
                releaseDate = value.releaseDate,
                year = value.year,
                trackNumber = value.trackNumber,
                discNumber = value.discNumber,
                explicit = value.explicit,
                isrc = value.isrc,
                upc = value.upc,
                score = MIN_YOUTUBE_MATCH_SCORE,
                cachedAt = System.currentTimeMillis()
            )

            fun from(json: JSONObject): CachedArtwork = CachedArtwork(
''',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    "        const val MISS_TTL_MS = 12L * 60L * 60L * 1000L\n",
    "        const val MISS_TTL_MS = 12L * 60L * 60L * 1000L\n        const val MIN_YOUTUBE_MATCH_SCORE = 150\n",
)

# Keep the row artwork as a persistent presentation lock through cache hits, playback resolution,
# mode switches and recovery.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    '''internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {
    if (!presented.source.equals("Levyra Editorial", ignoreCase = true)) return resolved
    val artwork = presented.largeThumbnailUrl.trim().ifBlank { presented.thumbnailUrl.trim() }
    if (artwork.isBlank()) return resolved
    return resolved.copy(thumbnailUrl = artwork, largeThumbnailUrl = artwork)
}
''',
    '''internal const val EDITORIAL_ARTWORK_LOCK_TAG = "editorial-artwork-lock"

internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {
    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags
    if (!artworkLocked) return resolved
    val artwork = presented.largeThumbnailUrl.trim().ifBlank { presented.thumbnailUrl.trim() }
    if (artwork.isBlank()) return resolved
    return resolved.copy(
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        moodTags = resolved.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
    )
}
''',
)

# Fill every country from the single catalog snapshot immediately, then enrich all countries in the
# background. Playback always reapplies the image selected for the chart row.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "import com.luc4n3x.levyra.data.PlaybackResolver\n",
    "import com.luc4n3x.levyra.data.PlaybackResolver\nimport com.luc4n3x.levyra.data.preserveEditorialArtwork\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "    private var chartMemoryWarmJob: Job? = null\n",
    "    private var chartMemoryWarmJob: Job? = null\n    private var chartCatalogPrimeJob: Job? = null\n",
)
replace_regex(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    r'''    /\*\*
     \* Loads the persisted chart regions.*?^    private fun prefetchChartRegions''',
    '''    /**
     * Loads every persisted and editorial country into memory. All chips therefore render a chart
     * immediately; slower artwork/playback enrichment continues independently in the background.
     */
    private fun warmChartRegionMemoryCache() {
        chartMemoryWarmJob?.cancel()
        chartCatalogPrimeJob?.cancel()
        chartMemoryWarmJob = viewModelScope.launch(Dispatchers.IO) {
            val languageCode = _state.value.languageCode
            val regionIds = ChartsCatalog.regions.map { it.id }
            val stored = preferences.loadChartTracksByRegion(languageCode, regionIds)
            stored.forEach { (regionId, tracks) ->
                val repaired = LevyraStartupCatalog.repairTracks(tracks, languageCode)
                if (repaired.isNotEmpty()) chartsByRegion[chartsCacheKey(languageCode, regionId)] = repaired
            }

            val editorial = runCatching { chartsRepository.cachedCountryCharts(50) }.getOrDefault(emptyMap())
            editorial.forEach { (regionId, tracks) ->
                if (tracks.isNotEmpty()) {
                    chartsByRegion.putIfAbsent(chartsCacheKey(languageCode, regionId), tracks)
                }
            }
            if (!isActive || _state.value.languageCode != languageCode) return@launch

            val selectedId = _state.value.selectedChartId
            val selected = chartsByRegion[chartsCacheKey(languageCode, selectedId)].orEmpty()
            if (selected.isNotEmpty()) publishPrefetchedCharts(languageCode, selectedId, selected)
            primeAllChartRegions(languageCode)
        }
    }

    private fun primeAllChartRegions(languageCode: String) {
        chartCatalogPrimeJob?.cancel()
        val appContext = getApplication<Application>().applicationContext
        val concurrency = if (adaptivePlaybackPolicy.current(videoMode = false).lowRam) 1 else 2
        chartCatalogPrimeJob = viewModelScope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(concurrency)
            ChartsCatalog.regions.map { region ->
                async {
                    semaphore.withPermit {
                        if (!isActive || _state.value.languageCode != languageCode) return@withPermit
                        val cacheKey = chartsCacheKey(languageCode, region.id)
                        if (isChartCacheFresh(cacheKey)) return@withPermit
                        val result = runCatching { chartsRepository.topSongs(region.country) }.getOrDefault(emptyList())
                        if (result.isEmpty() || !isActive || _state.value.languageCode != languageCode) return@withPermit
                        chartsByRegion[cacheKey] = result
                        chartsFreshAt[cacheKey] = System.currentTimeMillis()
                        preferences.saveChartTracks(result, languageCode, region.id)
                        LevyraArtworkCache.preloadHome(appContext, result, 8)
                        publishPrefetchedCharts(languageCode, region.id, result)
                    }
                }
            }.awaitAll()
        }
    }

    private fun prefetchChartRegions''',
    flags=re.DOTALL | re.MULTILINE,
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "            startPlayback(instant)\n",
    "            startPlayback(preserveEditorialArtwork(track, instant))\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "        youtubePlayableTrack(track)?.let { return resolvePlayableTrack(it) }\n",
    "        youtubePlayableTrack(track)?.let { return preserveEditorialArtwork(track, resolvePlayableTrack(it)) }\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "            resolved.onSuccess { return it }\n",
    "            resolved.onSuccess { return preserveEditorialArtwork(track, it) }\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "        chartPrefetchJob?.cancel()\n        homeSnapshotJob?.cancel()\n",
    "        chartPrefetchJob?.cancel()\n        chartMemoryWarmJob?.cancel()\n        chartCatalogPrimeJob?.cancel()\n        homeSnapshotJob?.cancel()\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "        private const val CHART_WARM_REGION_LIMIT = 14\n        private const val CHART_WARM_REGION_LIMIT_LOW_RAM = 6\n",
    "        private const val CHART_PRIME_REGION_COUNT = 28\n",
)

# Strengthen the continuity test so a subsequent playback resolve cannot lose the lock.
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt",
    '''        assertEquals(resolved.videoUrl, result.videoUrl)
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
''',
    '''        assertEquals(resolved.videoUrl, result.videoUrl)

        val recovered = preserveEditorialArtwork(
            result,
            resolved.copy(thumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg")
        )
        assertEquals(presented.thumbnailUrl, recovered.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, recovered.largeThumbnailUrl)
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
''',
)

print("Applied chart immediacy, artwork continuity, stable Home position and album scrolling fixes.")
