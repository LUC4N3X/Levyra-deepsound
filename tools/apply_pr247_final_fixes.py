from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != expected:
        raise SystemExit(f"{path}: expected {expected} occurrence(s), found {actual}")
    target.write_text(text.replace(old, new), encoding="utf-8")


def replace_in_section(path: str, start: str, end: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    start_index = text.index(start)
    end_index = text.index(end, start_index)
    section = text[start_index:end_index]
    actual = section.count(old)
    if actual != 1:
        raise SystemExit(f"{path}: expected one section occurrence, found {actual}")
    updated = section.replace(old, new, 1)
    target.write_text(text[:start_index] + updated + text[end_index:], encoding="utf-8")


WORKFLOW = ".github/workflows/editorial-catalog.yml"
RESOLVER = "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt"
VIEW_MODEL = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
APP_UI = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"

# artworkUrl is now part of the intended public catalog contract. Keep rejecting
# identifiers and source-page URLs that must never be published.
replace_exact(
    WORKFLOW,
    'has("artworkUrl") or has("isrc")',
    'has("isrc") or has("sourceId") or has("sourceUrl")',
    expected=2,
)
replace_exact(
    WORKFLOW,
    "Catalog contains source artwork or unsupported ISRC fields",
    "Catalog contains unsupported source identifiers or URLs",
    expected=2,
)

# Only editorial rows need immutable row-to-player artwork continuity.
replace_exact(
    RESOLVER,
    """                canonicalAlbumUrl = canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                moodTags = track.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
""",
    """                canonicalAlbumUrl = canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                moodTags = if (track.source.equals("Levyra Editorial", ignoreCase = true)) {
                    track.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
                } else {
                    track.moodTags
                }
""",
)

# Warmed persisted/editorial charts are already usable. Do not refetch them, and
# honour the declared prime bound.
replace_exact(
    VIEW_MODEL,
    "            ChartsCatalog.regions.map { region ->\n",
    "            ChartsCatalog.regions.take(CHART_PRIME_REGION_COUNT).map { region ->\n",
)
replace_exact(
    VIEW_MODEL,
    "                        if (isChartCacheFresh(cacheKey)) return@withPermit\n",
    "                        if (isChartCacheFresh(cacheKey) || chartsByRegion[cacheKey].orEmpty().isNotEmpty()) return@withPermit\n",
)

old_search = """    private suspend fun runSearch(clean: String) {
        moveToTab(LevyraTab.Search, rememberCurrent = true)
        _state.update { it.copy(isSearching = true, searchError = null, searchSuggestions = emptyList(), searchFilter = SearchFilter.All) }
        val result = runCatching {
            val raw = providerRouter.searchEverything(clean, _state.value.languageCode)
            raw.copy(artists = artistRepository.officialArtistHits(raw.artists))
        }
        result.onSuccess { data ->
            val tracks = data.songs
            val mood = _state.value.selectedMood
            val queue = moodEngine.buildQueue(mood, tracks.ifEmpty { repository.cachedTracks() })
            _state.update {
                it.copy(
                    tracks = mergeTracks(it.tracks, tracks),
                    searchResults = tracks,
                    searchData = data,
                    cacheReport = repository.cacheReport(),
                    smartScore = calculateSmartScore(queue),
                    isSearching = false,
                    searchError = if (data.isEmpty) "Nessun risultato trovato per $clean" else null
                )
            }
            val startupPlan = adaptivePlaybackPolicy.current(videoMode = false)
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, tracks, if (startupPlan.lowRam) 8 else 18)
            prefetchTop(tracks, if (startupPlan.lowRam) 3 else 8)
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
    }

"""
new_search = """    private suspend fun runSearch(clean: String) {
        moveToTab(LevyraTab.Search, rememberCurrent = true)
        _state.update { it.copy(isSearching = true, searchError = null, searchSuggestions = emptyList(), searchFilter = SearchFilter.All) }
        val result = runCatching {
            val raw = providerRouter.searchEverything(clean, _state.value.languageCode)
            val officialArtists = artistRepository.officialArtistHits(raw.artists)
            raw.copy(
                artists = officialArtists,
                albums = searchAlbumsForArtistQuery(clean, raw, officialArtists)
            )
        }
        result.onSuccess { data ->
            val tracks = data.songs
            val mood = _state.value.selectedMood
            val queue = moodEngine.buildQueue(mood, tracks.ifEmpty { repository.cachedTracks() })
            _state.update {
                it.copy(
                    tracks = mergeTracks(it.tracks, tracks),
                    searchResults = tracks,
                    searchData = data,
                    cacheReport = repository.cacheReport(),
                    smartScore = calculateSmartScore(queue),
                    isSearching = false,
                    searchError = if (data.isEmpty) "Nessun risultato trovato per $clean" else null
                )
            }
            val startupPlan = adaptivePlaybackPolicy.current(videoMode = false)
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, tracks, if (startupPlan.lowRam) 8 else 18)
            prefetchTop(tracks, if (startupPlan.lowRam) 3 else 8)
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
    }

    private suspend fun searchAlbumsForArtistQuery(
        query: String,
        raw: SearchResults,
        artists: List<ArtistHit>
    ): List<AlbumHit> {
        val queryKey = artistIdentityKey(query)
        val exactArtist = artists.firstOrNull { artistIdentityKey(it.name) == queryKey } ?: return raw.albums
        val profile = runCatching {
            artistRepository.profile(exactArtist.browseId, exactArtist.name)
        }.getOrNull()
        val officialArtistName = profile?.name.orEmpty().ifBlank { exactArtist.name }
        val officialArtistBrowseId = profile?.browseId.orEmpty().ifBlank { exactArtist.browseId }
        val officialAlbums = profile?.albums.orEmpty()
            .asSequence()
            .filter { release -> release.title.isNotBlank() && release.browseId.isNotBlank() }
            .map { release ->
                AlbumHit(
                    title = release.title,
                    artist = officialArtistName,
                    year = release.year,
                    thumbnailUrl = release.thumbnailUrl,
                    query = listOf(release.title, officialArtistName, "album")
                        .filter(String::isNotBlank)
                        .joinToString(" "),
                    browseId = release.browseId,
                    artistBrowseId = officialArtistBrowseId,
                    audioPlaylistId = release.playlistId,
                    explicit = release.explicit
                )
            }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .take(10)
            .toList()
        if (officialAlbums.isNotEmpty()) return officialAlbums

        val songTitles = raw.songs
            .asSequence()
            .map { track -> albumRecommendationTextKey(track.title) }
            .filter(String::isNotBlank)
            .toSet()
        return raw.albums
            .filterNot { album -> albumRecommendationTextKey(album.title) in songTitles }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .take(10)
    }

"""
replace_exact(VIEW_MODEL, old_search, new_search)

# Match the passive overlay root used by Home/Artist so vertical drags belong to
# the LazyColumn rather than an outer pointer consumer.
replace_in_section(
    APP_UI,
    "private fun AlbumOverlay(",
    "private fun AlbumLoadingCard()",
    """            .fillMaxSize()
            .background(LevyraBlack)
            .consumeOverlayTouches()
""",
    """            .fillMaxSize()
            .background(LevyraBlack)
""",
)
replace_in_section(
    APP_UI,
    "private fun SearchScreen(",
    "private fun SearchHeader(",
    "item { SectionTitle(strings.albumsAndSingles) }",
    "item { SectionTitle(strings.albumsPlain) }",
)

print("PR #247 fixes applied successfully")
