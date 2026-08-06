from __future__ import annotations

import json
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Spotify editorial client: resolve the official local New Music Friday list.
# ---------------------------------------------------------------------------
path = Path("tools/levyra-editorial/levyra_editorial/spotify.py")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "\n\nclass SpotifyWebClient:\n",
    '''

def _normalize_playlist_title(value: str) -> str:
    return " ".join(str(value or "").casefold().split())


def select_official_spotify_playlist_id(
    items: Sequence[Mapping[str, Any]],
    query: str,
    title_hints: Sequence[str] = (),
) -> str | None:
    """Pick the best Spotify-owned editorial playlist from search results."""
    query_key = _normalize_playlist_title(query)
    hints = [
        normalized
        for value in title_hints
        for normalized in [_normalize_playlist_title(value)]
        if normalized
    ]
    if query_key and query_key not in hints:
        hints.append(query_key)

    best_id: str | None = None
    best_score = -1
    for position, item in enumerate(items):
        if not isinstance(item, Mapping):
            continue
        owner = item.get("owner")
        owner_id = ""
        owner_name = ""
        if isinstance(owner, Mapping):
            owner_id = str(owner.get("id") or "").strip().casefold()
            owner_name = str(owner.get("display_name") or "").strip().casefold()
        if owner_id != "spotify" and owner_name != "spotify":
            continue

        playlist_id = str(item.get("id") or "").strip()
        name_key = _normalize_playlist_title(str(item.get("name") or ""))
        if not playlist_id.isalnum() or len(playlist_id) not in range(10, 80) or not name_key:
            continue

        score = 0
        if name_key in hints:
            score += 20_000
        for hint_index, hint in enumerate(hints):
            if not hint:
                continue
            if name_key.startswith(hint):
                score += 12_000 - hint_index * 80
            elif hint in name_key:
                score += 8_000 - hint_index * 80
            hint_tokens = {token for token in hint.split() if len(token) >= 3}
            name_tokens = set(name_key.split())
            score += len(hint_tokens & name_tokens) * 350
        if "new music friday" in name_key:
            score += 4_000
        if "novedades viernes" in name_key or "lancamentos da semana" in name_key:
            score += 3_800
        score -= position
        if score > best_score:
            best_score = score
            best_id = playlist_id

    return best_id if best_score > 0 else None


class SpotifyWebClient:
''',
    "Spotify playlist selector",
)
text = replace_once(
    text,
    '''    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
''',
    '''    def resolve_playlist_id(
        self,
        query: str,
        market: str,
        title_hints: Sequence[str] = (),
    ) -> str:
        """Resolve one Spotify-owned editorial playlist through web-player search."""
        normalized_query = str(query or "").strip()
        normalized_market = str(market or "").strip().upper()
        if len(normalized_query) < 3:
            raise SourceApiError("The editorial playlist search query is too short.")
        if normalized_market not in {"GLOBAL", "WORLD"} and not re.fullmatch(
            r"[A-Z]{2}", normalized_market
        ):
            raise SourceApiError("The editorial playlist market is invalid.")
        if self._access_token is None:
            self.authenticate()

        params = {
            "q": normalized_query,
            "type": "playlist",
            "limit": 20,
        }
        if normalized_market not in {"GLOBAL", "WORLD"}:
            params["market"] = normalized_market

        def request_search() -> requests.Response:
            return self._session.get(
                f"{API_BASE_URL}/search",
                params=params,
                headers=self._api_headers(),
                timeout=self._timeout,
            )

        response = request_search()
        if response.status_code == 401:
            self.authenticate()
            response = request_search()
        if response.status_code == 429:
            delay = _bounded_retry_after(response.headers.get("Retry-After"))
            if delay > 0:
                time.sleep(delay)
            response = request_search()
            if response.status_code == 401:
                self.authenticate()
                response = request_search()
        if response.status_code >= 400:
            raise SourceApiError(
                f"Spotify editorial playlist search failed with HTTP {response.status_code}."
            )
        try:
            payload = response.json()
        except ValueError as error:
            raise SourceApiError("Spotify playlist search returned invalid JSON.") from error
        playlists = payload.get("playlists") if isinstance(payload, Mapping) else None
        items = playlists.get("items") if isinstance(playlists, Mapping) else None
        if not isinstance(items, list):
            raise SourceApiError("Spotify playlist search returned no usable result list.")
        selected = select_official_spotify_playlist_id(
            [item for item in items if isinstance(item, Mapping)],
            normalized_query,
            title_hints,
        )
        if selected is None:
            raise SourceApiError(
                f"No Spotify-owned editorial playlist matched '{normalized_query}'."
            )
        return selected

    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
''',
    "Spotify playlist search method",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Collector: allow query-resolved playlists, per-collection limits and fallback.
# ---------------------------------------------------------------------------
path = Path("tools/levyra-editorial/levyra_editorial/collector.py")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:
        """Return all ordered playlist items."""
''',
    '''    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:
        """Return all ordered playlist items."""

    def resolve_playlist_id(
        self,
        query: str,
        market: str,
        title_hints: list[str],
    ) -> str:
        """Resolve an official editorial playlist from a localized query."""
''',
    "collector protocol resolver",
)
old_validation = '''        playlist_id = str(item.get("playlistId", "")).strip()
        market = str(item.get("market", "")).strip().upper()
        kind = str(item.get("kind", "")).strip().lower()
        if not COLLECTION_ID_PATTERN.fullmatch(collection_id):
            raise ValueError(f"Collection #{index + 1} has an invalid id.")
        if collection_id in seen_ids:
            raise ValueError(f"Collection id '{collection_id}' is duplicated.")
        seen_ids.add(collection_id)
        if len(playlist_id) not in range(10, 80) or not playlist_id.isalnum():
            raise ValueError(f"Collection '{collection_id}' has an invalid playlistId.")
        if market not in {"GLOBAL", "WORLD"} and not re.fullmatch(r"[A-Z]{2}", market):
            raise ValueError(f"Collection '{collection_id}' has an invalid market.")
        if kind not in {"chart", "editorial", "release"}:
            raise ValueError(f"Collection '{collection_id}' has an unsupported kind.")
        title = item.get("title")
        if title is not None and (not isinstance(title, str) or not title.strip()):
            raise ValueError(f"Collection '{collection_id}' has an invalid title.")
        optional = item.get("optional")
        if optional is not None and not isinstance(optional, bool):
            raise ValueError(f"Collection '{collection_id}' has an invalid optional flag.")
'''
new_validation = '''        playlist_id = str(item.get("playlistId", "")).strip()
        playlist_query = str(item.get("playlistQuery", "")).strip()
        fallback_playlist_id = str(item.get("fallbackPlaylistId", "")).strip()
        market = str(item.get("market", "")).strip().upper()
        kind = str(item.get("kind", "")).strip().lower()
        if not COLLECTION_ID_PATTERN.fullmatch(collection_id):
            raise ValueError(f"Collection #{index + 1} has an invalid id.")
        if collection_id in seen_ids:
            raise ValueError(f"Collection id '{collection_id}' is duplicated.")
        seen_ids.add(collection_id)
        if bool(playlist_id) == bool(playlist_query):
            raise ValueError(
                f"Collection '{collection_id}' must define exactly one of playlistId or playlistQuery."
            )
        for field_name, candidate in (
            ("playlistId", playlist_id),
            ("fallbackPlaylistId", fallback_playlist_id),
        ):
            if candidate and (len(candidate) not in range(10, 80) or not candidate.isalnum()):
                raise ValueError(f"Collection '{collection_id}' has an invalid {field_name}.")
        if playlist_query and len(playlist_query) not in range(3, 160):
            raise ValueError(f"Collection '{collection_id}' has an invalid playlistQuery.")
        if market not in {"GLOBAL", "WORLD"} and not re.fullmatch(r"[A-Z]{2}", market):
            raise ValueError(f"Collection '{collection_id}' has an invalid market.")
        if kind not in {"chart", "editorial", "release"}:
            raise ValueError(f"Collection '{collection_id}' has an unsupported kind.")
        title = item.get("title")
        if title is not None and (not isinstance(title, str) or not title.strip()):
            raise ValueError(f"Collection '{collection_id}' has an invalid title.")
        title_hints = item.get("titleHints")
        if title_hints is not None and (
            not isinstance(title_hints, list)
            or any(not isinstance(value, str) or not value.strip() for value in title_hints)
        ):
            raise ValueError(f"Collection '{collection_id}' has invalid titleHints.")
        item_limit = item.get("limit")
        if item_limit is not None and (
            not isinstance(item_limit, int) or isinstance(item_limit, bool) or item_limit not in range(1, 101)
        ):
            raise ValueError(f"Collection '{collection_id}' has an invalid limit.")
        optional = item.get("optional")
        if optional is not None and not isinstance(optional, bool):
            raise ValueError(f"Collection '{collection_id}' has an invalid optional flag.")
'''
text = replace_once(text, old_validation, new_validation, "collector config validation")
old_loop = '''    for item in raw_collections:
        if not isinstance(item, dict):
            continue
        collection_id = str(item["id"])
        playlist_id = str(item["playlistId"])
        market = str(item["market"]).upper()
        LOGGER.info("Collecting %s (%s)", collection_id, market)

        metadata = client.get_playlist_metadata(playlist_id)
        raw_items = client.iter_playlist_items(playlist_id)
        enricher = getattr(client, "enrich_track_metadata", None)
        if callable(enricher):
            try:
                raw_items = enricher(raw_items)
            except Exception as error:
                LOGGER.warning("Optional track metadata enrichment skipped: %s", type(error).__name__)
        tracks = normalize_playlist_items(raw_items)
        if not tracks:
            raise ValueError(f"Collection '{collection_id}' produced no usable tracks.")

        output.append(
            Collection(
                id=collection_id,
                kind=str(item["kind"]).lower(),
                market=market,
                title=str(item.get("title") or metadata.get("name") or collection_id).strip(),
                description=_clean_text(str(metadata.get("description") or "")),
                source_id=playlist_id,
                source_url=_nested_string(metadata, "external_urls", "spotify"),
                artwork_url=_first_image_url(metadata.get("images")),
                snapshot_id=_optional_string(metadata.get("snapshot_id")),
                total_source_items=_nested_int(metadata, "tracks", "total", default=len(raw_items)),
                tracks=tracks,
            )
        )

    return Catalog(
'''
new_loop = '''    for item in raw_collections:
        if not isinstance(item, dict):
            continue
        collection_id = str(item["id"])
        market = str(item["market"]).upper()
        LOGGER.info("Collecting %s (%s)", collection_id, market)
        try:
            output.append(_collect_configured_collection(item, client))
        except Exception as error:
            if item.get("optional") is True:
                LOGGER.warning(
                    "Optional collection %s skipped after %s.",
                    collection_id,
                    type(error).__name__,
                )
                continue
            raise

    return Catalog(
'''
text = replace_once(text, old_loop, new_loop, "collector build loop")
text = replace_once(
    text,
    '''
def normalize_playlist_items(items: list[dict[str, Any]]) -> list[Track]:
''',
    '''
def _collect_configured_collection(
    item: Mapping[str, Any],
    client: EditorialClient,
) -> Collection:
    collection_id = str(item["id"])
    market = str(item["market"]).upper()
    configured_id = str(item.get("playlistId") or "").strip()
    fallback_id = str(item.get("fallbackPlaylistId") or "").strip()
    title_hints = [
        str(value).strip()
        for value in item.get("titleHints", [])
        if isinstance(value, str) and value.strip()
    ]

    candidates: list[str] = []
    if configured_id:
        candidates.append(configured_id)
    else:
        resolver = getattr(client, "resolve_playlist_id", None)
        if callable(resolver):
            try:
                resolved = str(
                    resolver(
                        str(item.get("playlistQuery") or "").strip(),
                        market,
                        title_hints,
                    )
                    or ""
                ).strip()
                if resolved:
                    candidates.append(resolved)
            except Exception as error:
                LOGGER.warning(
                    "Localized playlist resolution failed for %s: %s.",
                    collection_id,
                    type(error).__name__,
                )
    if fallback_id and fallback_id not in candidates:
        candidates.append(fallback_id)
    if not candidates:
        raise ValueError(f"Collection '{collection_id}' has no resolvable playlist.")

    last_error: Exception | None = None
    item_limit = int(item.get("limit") or 100)
    for playlist_id in candidates:
        try:
            metadata = client.get_playlist_metadata(playlist_id)
            raw_items = client.iter_playlist_items(playlist_id)[:item_limit]
            enricher = getattr(client, "enrich_track_metadata", None)
            if callable(enricher):
                try:
                    raw_items = enricher(raw_items)
                except Exception as error:
                    LOGGER.warning(
                        "Optional track metadata enrichment skipped: %s",
                        type(error).__name__,
                    )
            tracks = normalize_playlist_items(raw_items)[:item_limit]
            if not tracks:
                raise ValueError(f"Collection '{collection_id}' produced no usable tracks.")
            return Collection(
                id=collection_id,
                kind=str(item["kind"]).lower(),
                market=market,
                title=str(item.get("title") or metadata.get("name") or collection_id).strip(),
                description=_clean_text(str(metadata.get("description") or "")),
                source_id=playlist_id,
                source_url=_nested_string(metadata, "external_urls", "spotify"),
                artwork_url=_first_image_url(metadata.get("images")),
                snapshot_id=_optional_string(metadata.get("snapshot_id")),
                total_source_items=_nested_int(
                    metadata,
                    "tracks",
                    "total",
                    default=len(raw_items),
                ),
                tracks=tracks,
            )
        except Exception as error:
            last_error = error
            LOGGER.warning(
                "Playlist candidate for %s failed: %s.",
                collection_id,
                type(error).__name__,
            )
    raise ValueError(f"Collection '{collection_id}' could not be collected.") from last_error


def normalize_playlist_items(items: list[dict[str, Any]]) -> list[Track]:
''',
    "collector configured collection helper",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Add one official release feed per supported app language/market.
# ---------------------------------------------------------------------------
path = Path("tools/levyra-editorial/config.json")
config = json.loads(path.read_text(encoding="utf-8"))
collections = [
    item
    for item in config["collections"]
    if not str(item.get("id", "")).startswith("new-releases-")
]
GLOBAL_NEW_MUSIC_FRIDAY = "37i9dQZF1DX4JAvHpjipBk"
release_specs = [
    ("us", "US", None, ["New Music Friday"], GLOBAL_NEW_MUSIC_FRIDAY),
    ("it", "IT", None, ["New Music Friday Italia"], "37i9dQZF1DWVKDF4ycOESi"),
    ("es", "ES", "Novedades Viernes España", ["Novedades Viernes España", "New Music Friday Spain"], None),
    ("fr", "FR", "New Music Friday France", ["New Music Friday France"], None),
    ("de", "DE", "New Music Friday Deutschland", ["New Music Friday Deutschland", "New Music Friday Germany"], None),
    ("br", "BR", "Lançamentos da Semana", ["Lançamentos da Semana", "New Music Friday Brasil"], None),
    ("nl", "NL", "New Music Friday NL", ["New Music Friday NL", "New Music Friday Netherlands"], None),
    ("pl", "PL", "New Music Friday Polska", ["New Music Friday Polska", "New Music Friday Poland"], None),
    ("ro", "RO", "New Music Friday Romania", ["New Music Friday Romania"], None),
    ("gr", "GR", "New Music Friday Greece", ["New Music Friday Greece"], None),
    ("se", "SE", "New Music Friday Sweden", ["New Music Friday Sweden"], None),
    ("dk", "DK", "New Music Friday Denmark", ["New Music Friday Denmark"], None),
    ("cz", "CZ", "New Music Friday Czech Republic", ["New Music Friday Czech Republic", "New Music Friday Czechia"], None),
    ("ua", "UA", "New Music Friday Ukraine", ["New Music Friday Ukraine"], None),
    ("ru", "RU", "New Music Friday Russia", ["New Music Friday Russia"], None),
    ("tr", "TR", "New Music Friday Türkiye", ["New Music Friday Türkiye", "New Music Friday Turkey"], None),
    ("sa", "SA", "New Music Friday Arabic", ["New Music Friday Arabic", "New Music Friday Arabia"], None),
    ("cn", "CN", None, ["New Music Friday"], GLOBAL_NEW_MUSIC_FRIDAY),
    ("jp", "JP", "New Music Friday Japan", ["New Music Friday Japan"], None),
    ("kr", "KR", "New Music Friday Korea", ["New Music Friday Korea", "New Music Friday South Korea"], None),
    ("in", "IN", "New Music Friday India", ["New Music Friday India"], None),
    ("id", "ID", "New Music Friday Indonesia", ["New Music Friday Indonesia"], None),
    ("vn", "VN", "New Music Friday Vietnam", ["New Music Friday Vietnam"], None),
    ("th", "TH", "New Music Friday Thailand", ["New Music Friday Thailand"], None),
    ("ph", "PH", "New Music Friday Philippines", ["New Music Friday Philippines"], None),
    ("il", "IL", "New Music Friday Israel", ["New Music Friday Israel"], None),
]
for suffix, market, query, hints, fixed_id in release_specs:
    item = {
        "id": f"new-releases-{suffix}",
        "kind": "release",
        "market": market,
        "title": "New releases",
        "limit": 20,
        "optional": True,
    }
    if fixed_id:
        item["playlistId"] = fixed_id
    else:
        item["playlistQuery"] = query
        item["titleHints"] = hints
        item["fallbackPlaylistId"] = GLOBAL_NEW_MUSIC_FRIDAY
    collections.append(item)
config["collections"] = collections
path.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# Android catalog parser: retain release collections separately from charts.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    suspend fun cachedAllMarkets(limit: Int): Map<String, List<Track>> = withContext(Dispatchers.IO) {
''',
    '''    suspend fun cachedNewReleaseTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.newReleases(country, limit)
    }

    suspend fun cachedAllMarkets(limit: Int): Map<String, List<Track>> = withContext(Dispatchers.IO) {
''',
    "editorial release cache method",
)
text = replace_once(
    text,
    '''internal data class CatalogSnapshot(
    val byMarket: Map<String, List<Track>>,
    val generatedAtMs: Long,
''',
    '''internal data class CatalogSnapshot(
    val byMarket: Map<String, List<Track>>,
    val releaseByMarket: Map<String, List<Track>>,
    val generatedAtMs: Long,
''',
    "catalog release map",
)
text = replace_once(
    text,
    '''    fun tracks(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        return byMarket[market].orEmpty().take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    private companion object {
''',
    '''    fun tracks(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        return byMarket[market].orEmpty().take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    fun newReleases(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        val localized = releaseByMarket[market].orEmpty()
        val fallback = releaseByMarket["US"].orEmpty()
        return localized.ifEmpty { fallback }.take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    private companion object {
''',
    "catalog release accessor",
)
old_parse = '''        val collections = root.optJSONArray("collections") ?: return null
        val byMarket = LinkedHashMap<String, List<Track>>()
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            if (!collection.optString("kind").equals("chart", ignoreCase = true)) continue
            val market = collection.optString("market")
                .trim()
                .uppercase(Locale.ROOT)
                .takeIf { it.length == 2 }
                ?: continue
            val tracks = parseTracks(collection.optJSONArray("tracks"))
            if (tracks.isNotEmpty()) byMarket[market] = tracks
        }
        if (byMarket.isEmpty()) return null
        return CatalogSnapshot(
            byMarket = byMarket,
            generatedAtMs = generatedAtMs,
            loadedAt = loadedAt,
            rawJson = body,
        )
    }

    private fun parseTracks(items: JSONArray?): List<Track> {
'''
new_parse = '''        val collections = root.optJSONArray("collections") ?: return null
        val byMarket = LinkedHashMap<String, List<Track>>()
        val releaseByMarket = LinkedHashMap<String, List<Track>>()
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            val kind = collection.optString("kind").trim().lowercase(Locale.ROOT)
            if (kind != "chart" && kind != "release") continue
            val market = collection.optString("market")
                .trim()
                .uppercase(Locale.ROOT)
                .takeIf { it.length == 2 }
                ?: continue
            val tracks = parseTracks(collection.optJSONArray("tracks"), kind)
            if (tracks.isEmpty()) continue
            if (kind == "release") releaseByMarket[market] = tracks else byMarket[market] = tracks
        }
        if (byMarket.isEmpty()) return null
        return CatalogSnapshot(
            byMarket = byMarket,
            releaseByMarket = releaseByMarket,
            generatedAtMs = generatedAtMs,
            loadedAt = loadedAt,
            rawJson = body,
        )
    }

    private fun parseTracks(items: JSONArray?, kind: String): List<Track> {
'''
text = replace_once(text, old_parse, new_parse, "editorial parser release collections")
text = replace_once(
    text,
    '''        val tracks = ArrayList<Track>(minOf(items.length(), MAX_TRACKS_PER_MARKET))
        for (index in 0 until items.length()) {
''',
    '''        val tracks = ArrayList<Track>(minOf(items.length(), MAX_TRACKS_PER_MARKET))
        val releaseCollection = kind.equals("release", ignoreCase = true)
        for (index in 0 until items.length()) {
''',
    "release parse flag",
)
text = replace_once(
    text,
    '''                album = album?.optString("name").orEmpty().trim().ifBlank { EDITORIAL_ALBUM },
''',
    '''                album = album?.optString("name").orEmpty().trim().ifBlank {
                    if (releaseCollection) title else EDITORIAL_ALBUM
                },
''',
    "release album fallback",
)
text = replace_once(
    text,
    '''                source = EDITORIAL_SOURCE,
                moodTags = setOf("hit", "chart"),
                energy = 70,
                vocal = 55,
                replayScore = 95,
                cacheScore = 88,
''',
    '''                source = if (releaseCollection) EDITORIAL_RELEASE_SOURCE else EDITORIAL_SOURCE,
                moodTags = if (releaseCollection) {
                    setOf("new-release", "editorial")
                } else {
                    setOf("hit", "chart")
                },
                energy = 70,
                vocal = 55,
                replayScore = if (releaseCollection) 90 else 95,
                cacheScore = 88,
''',
    "release track identity",
)
text = replace_once(
    text,
    '''                metadataProvider = if (youtubePlaybackId.isBlank()) EDITORIAL_SOURCE else "$EDITORIAL_SOURCE + YouTube Music",
''',
    '''                metadataProvider = when {
                    releaseCollection && youtubePlaybackId.isNotBlank() -> "$EDITORIAL_RELEASE_SOURCE + YouTube Music"
                    releaseCollection -> EDITORIAL_RELEASE_SOURCE
                    youtubePlaybackId.isNotBlank() -> "$EDITORIAL_SOURCE + YouTube Music"
                    else -> EDITORIAL_SOURCE
                },
''',
    "release metadata provider",
)
text = replace_once(
    text,
    '''    private const val EDITORIAL_SOURCE = "Levyra Editorial"
    private const val EDITORIAL_ALBUM = "Levyra Top 50"
''',
    '''    private const val EDITORIAL_SOURCE = "Levyra Editorial"
    private const val EDITORIAL_RELEASE_SOURCE = "Levyra Editorial Releases"
    private const val EDITORIAL_ALBUM = "Levyra Top 50"
''',
    "release source constant",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# ChartsRepository exposes localized official Spotify releases as AlbumHit.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''import com.luc4n3x.levyra.domain.Track
''',
    '''import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
''',
    "ChartsRepository release imports",
)
text = replace_once(
    text,
    '''    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
''',
    '''    suspend fun newReleaseAlbums(country: String, limit: Int = 40): List<AlbumHit> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 80)
        val tracks = editorialCharts.cachedNewReleaseTracks(country, safeLimit * 3)
        tracks.asSequence()
            .filter { track ->
                track.title.isNotBlank() &&
                    track.artist.isNotBlank() &&
                    track.album.isNotBlank() &&
                    (track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank())
            }
            .map { track ->
                val albumTitle = track.album.ifBlank { track.title }
                AlbumHit(
                    title = albumTitle,
                    artist = track.artist,
                    year = track.year,
                    thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    query = "${track.artist} $albumTitle".trim(),
                    browseId = track.albumBrowseId,
                    artistBrowseId = track.artistBrowseIds.firstOrNull().orEmpty(),
                    explicit = track.explicit,
                    releaseDate = track.releaseDate,
                    metadataProvider = track.metadataProvider,
                    metadataConfidence = track.metadataConfidence,
                    releaseType = if (albumTitle.equals(track.title, ignoreCase = true)) {
                        ReleaseType.Single
                    } else {
                        ReleaseType.Unknown
                    }
                )
            }
            .distinctBy { release ->
                release.browseId.ifBlank {
                    "${release.artist.trim().lowercase()}|${release.title.trim().lowercase()}"
                }
            }
            .take(safeLimit)
            .toList()
    }

    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
''',
    "ChartsRepository release albums",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# ViewModel: Spotify local playlist first, strict YouTube Music fallback second.
# ---------------------------------------------------------------------------
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''            val releases = try {
                repository.newReleases(
                    languageCode = languageCode,
                    limit = 48,
                    preferredArtists = preferredArtists
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Personalized new releases failed for %s", languageCode)
                emptyList()
            }
''',
    '''            val market = ChartsCatalog.defaultRegionForLanguage(languageCode).country
            val editorialReleases = try {
                chartsRepository.newReleaseAlbums(country = market, limit = 48)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Localized editorial releases failed for %s", market)
                emptyList()
            }
            val releases = prioritizeNewReleasesForUser(
                releases = editorialReleases,
                preferredArtists = preferredArtists,
                limit = 48
            ).ifEmpty {
                try {
                    repository.newReleases(
                        languageCode = languageCode,
                        limit = 48,
                        preferredArtists = preferredArtists
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.w(error, "Personalized new releases fallback failed for %s", languageCode)
                    emptyList()
                }
            }
''',
    "ViewModel Spotify release priority",
)
insert_marker = '''internal fun selectYoutubeShortSample(list: List<Track>, requested: Track): Track? {
'''
helper = '''internal fun prioritizeNewReleasesForUser(
    releases: List<AlbumHit>,
    preferredArtists: List<String>,
    limit: Int
): List<AlbumHit> {
    if (limit <= 0) return emptyList()
    val preferences = preferredArtists
        .map { artist -> artist.trim().lowercase(java.util.Locale.ROOT) }
        .filter(String::isNotBlank)
        .distinct()
    if (preferences.isEmpty()) return releases.take(limit)
    val (matched, remaining) = releases.partition { release ->
        val artist = release.artist.trim().lowercase(java.util.Locale.ROOT)
        preferences.any { preferred ->
            artist == preferred || artist.contains(preferred) || preferred.contains(artist)
        }
    }
    return (matched + remaining)
        .distinctBy { release ->
            release.browseId.ifBlank {
                "${release.artist.lowercase(java.util.Locale.ROOT)}|${release.title.lowercase(java.util.Locale.ROOT)}"
            }
        }
        .take(limit)
}

internal fun selectYoutubeShortSample(list: List<Track>, requested: Track): Track? {
'''
text = replace_once(text, insert_marker, helper, "release personalization helper")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Android and Python regression coverage.
# ---------------------------------------------------------------------------
Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/NewReleasesPersonalizationTest.kt").write_text(
    '''package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.AlbumHit
import org.junit.Assert.assertEquals
import org.junit.Test

class NewReleasesPersonalizationTest {
    @Test
    fun userArtistsMoveUpWithoutDestroyingEditorialOrder() {
        val releases = listOf(
            album("Editoriale 1", "Altro"),
            album("Preferita 1", "Lazza"),
            album("Editoriale 2", "Altro 2"),
            album("Preferita 2", "Lazza")
        )

        val ranked = prioritizeNewReleasesForUser(releases, listOf("Lazza"), 10)

        assertEquals(
            listOf("Preferita 1", "Preferita 2", "Editoriale 1", "Editoriale 2"),
            ranked.map { it.title }
        )
    }

    @Test
    fun emptyPreferencesKeepSpotifyEditorialOrder() {
        val releases = listOf(album("Uno", "A"), album("Due", "B"))

        assertEquals(releases, prioritizeNewReleasesForUser(releases, emptyList(), 10))
    }

    private fun album(title: String, artist: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://levyra.test/$title.jpg",
        query = "$artist $title",
        browseId = "MPRE${title.hashCode().toUInt()}"
    )
}
''',
    encoding="utf-8",
)

# Add a release collection parser test alongside the existing parser tests.
parser_test = Path("app/src/test/java/com/luc4n3x/levyra/data/EditorialChartsRepositoryTest.kt")
if parser_test.exists():
    text = parser_test.read_text(encoding="utf-8")
    if "releaseCollectionsStaySeparateFromCharts" not in text:
        insertion = '''

    @Test
    fun releaseCollectionsStaySeparateFromCharts() {
        val body = """{
            "schemaVersion": 1,
            "generatedAt": "2026-08-06T12:00:00Z",
            "collections": [
                {
                    "id": "top-50-it",
                    "kind": "chart",
                    "market": "IT",
                    "tracks": [${catalogTrackJson("chart", "Chart Song", "Chart Artist")}]
                },
                {
                    "id": "new-releases-it",
                    "kind": "release",
                    "market": "IT",
                    "tracks": [${catalogTrackJson("release", "New Song", "New Artist")}]
                }
            ]
        }""".trimIndent()

        val snapshot = requireNotNull(EditorialCatalogParser.parse(body, loadedAt = 1L))

        assertEquals(listOf("Chart Song"), snapshot.tracks("it", 10).map { it.title })
        assertEquals(listOf("New Song"), snapshot.newReleases("it", 10).map { it.title })
    }
'''
        closing = text.rfind("}\n")
        if closing < 0:
            raise RuntimeError("EditorialChartsRepositoryTest has no class closing brace")
        text = text[:closing] + insertion + text[closing:]
        parser_test.write_text(text, encoding="utf-8")

# Spotify playlist selector unit tests.
spotify_test = Path("tools/levyra-editorial/tests/test_spotify_release_search.py")
spotify_test.write_text(
    '''from levyra_editorial.spotify import select_official_spotify_playlist_id


def playlist(identifier: str, name: str, owner: str = "spotify") -> dict:
    return {
        "id": identifier,
        "name": name,
        "owner": {"id": owner, "display_name": owner.title()},
    }


def test_selector_prefers_exact_local_spotify_playlist() -> None:
    items = [
        playlist("userplaylist000000000001", "New Music Friday Italia", owner="someone"),
        playlist("globalplaylist0000000001", "New Music Friday"),
        playlist("italiaplaylist0000000001", "New Music Friday Italia"),
    ]

    assert (
        select_official_spotify_playlist_id(
            items,
            "New Music Friday Italia",
            ["New Music Friday Italia"],
        )
        == "italiaplaylist0000000001"
    )


def test_selector_rejects_non_spotify_owner() -> None:
    assert (
        select_official_spotify_playlist_id(
            [playlist("userplaylist000000000001", "New Music Friday Italia", owner="user")],
            "New Music Friday Italia",
        )
        is None
    )
''',
    encoding="utf-8",
)

# Update the Samples assertion changed by the first patch's stricter localized query.
samples_test = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicSamplesPolicyTest.kt")
if samples_test.exists():
    text = samples_test.read_text(encoding="utf-8")
    text = text.replace(
        'assertTrue(queries.contains("nuovi video musicali"))',
        'assertTrue(queries.contains("nuovi video musicali italiani"))',
    )
    samples_test.write_text(text, encoding="utf-8")

print("Applied localized Spotify New Music Friday feeds for every supported language")
