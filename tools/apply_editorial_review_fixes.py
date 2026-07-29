from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content.rstrip() + "\n", encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    content = read(path)
    count = content.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:80]!r}")
    write(path, content.replace(old, new, 1))


def replace_regex(path: str, pattern: str, replacement: str, *, flags: int = 0) -> None:
    content = read(path)
    updated, count = re.subn(pattern, replacement, content, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"Expected one regex match in {path}, found {count}: {pattern!r}")
    write(path, updated)


CONFIG = {
    "schemaVersion": 1,
    "collections": [
        {"id": "top-50-it", "kind": "chart", "market": "IT", "playlistId": "37i9dQZEVXbIQnj7RRhdSX", "title": "Top 50 Italia"},
        {"id": "top-50-us", "kind": "chart", "market": "US", "playlistId": "37i9dQZEVXbLRQDuF5jeBp", "title": "Top 50 USA"},
        {"id": "top-50-gb", "kind": "chart", "market": "GB", "playlistId": "37i9dQZEVXbLnolsZ8PSNw", "title": "Top 50 United Kingdom"},
        {"id": "top-50-es", "kind": "chart", "market": "ES", "playlistId": "37i9dQZEVXbNFJfN1Vw8d9", "title": "Top 50 España"},
        {"id": "top-50-fr", "kind": "chart", "market": "FR", "playlistId": "37i9dQZEVXbIPWwFssbupI", "title": "Top 50 France"},
        {"id": "top-50-de", "kind": "chart", "market": "DE", "playlistId": "37i9dQZEVXbJiZcmkrIHGU", "title": "Top 50 Deutschland"},
        {"id": "top-50-br", "kind": "chart", "market": "BR", "playlistId": "37i9dQZEVXbMXbN3EUUhlg", "title": "Top 50 Brasil"},
        {"id": "top-50-mx", "kind": "chart", "market": "MX", "playlistId": "37i9dQZEVXbO3qyFxbkOE1", "title": "Top 50 México"},
        {"id": "top-50-nl", "kind": "chart", "market": "NL", "playlistId": "37i9dQZEVXbKCF6dqVpDkS", "title": "Top 50 Nederland"},
        {"id": "top-50-pl", "kind": "chart", "market": "PL", "playlistId": "37i9dQZEVXbN6itCcaL3Tt", "title": "Top 50 Polska"},
        {"id": "top-50-ro", "kind": "chart", "market": "RO", "playlistId": "37i9dQZEVXbNZbJ6TZelCq", "title": "Top 50 România"},
        {"id": "top-50-gr", "kind": "chart", "market": "GR", "playlistId": "37i9dQZEVXbJqdarpmTJDL", "title": "Top 50 Ελλάδα"},
        {"id": "top-50-se", "kind": "chart", "market": "SE", "playlistId": "37i9dQZEVXbLoATJ81JYXz", "title": "Top 50 Sverige"},
        {"id": "top-50-dk", "kind": "chart", "market": "DK", "playlistId": "37i9dQZEVXbL3J0k32lWnN", "title": "Top 50 Danmark"},
        {"id": "top-50-cz", "kind": "chart", "market": "CZ", "playlistId": "37i9dQZEVXbIP3c3fqVrJY", "title": "Top 50 Česko"},
        {"id": "top-50-ua", "kind": "chart", "market": "UA", "playlistId": "37i9dQZEVXbKkidEfWYRuD", "title": "Top 50 Україна"},
        {"id": "top-50-ru", "kind": "chart", "market": "RU", "playlistId": "37i9dQZEVXbL8l7ra5vVdB", "title": "Top 50 Россия", "optional": True},
        {"id": "top-50-tr", "kind": "chart", "market": "TR", "playlistId": "37i9dQZEVXbIVYVBNw9D5K", "title": "Top 50 Türkiye"},
        {"id": "top-50-sa", "kind": "chart", "market": "SA", "playlistId": "37i9dQZEVXbLrQBcXqUtaC", "title": "Top 50 السعودية"},
        {"id": "top-50-jp", "kind": "chart", "market": "JP", "playlistId": "37i9dQZEVXbKXQ4mDTEBXq", "title": "Top 50 日本"},
        {"id": "top-50-kr", "kind": "chart", "market": "KR", "playlistId": "37i9dQZEVXbNxXF4SkHj9F", "title": "Top 50 대한민국"},
        {"id": "top-50-in", "kind": "chart", "market": "IN", "playlistId": "37i9dQZEVXbLZ52XmnySJg", "title": "Top 50 भारत"},
        {"id": "top-50-id", "kind": "chart", "market": "ID", "playlistId": "37i9dQZEVXbObFQZ3JLcXt", "title": "Top 50 Indonesia"},
        {"id": "top-50-vn", "kind": "chart", "market": "VN", "playlistId": "37i9dQZEVXbLdGSmz6xilI", "title": "Top 50 Việt Nam"},
        {"id": "top-50-th", "kind": "chart", "market": "TH", "playlistId": "37i9dQZEVXbMnz8KIWsvf9", "title": "Top 50 ประเทศไทย"},
        {"id": "top-50-ph", "kind": "chart", "market": "PH", "playlistId": "37i9dQZEVXbNBz9cRCSFkY", "title": "Top 50 Pilipinas"},
        {"id": "top-50-il", "kind": "chart", "market": "IL", "playlistId": "37i9dQZEVXbJ6IpvItkve3", "title": "Top 50 ישראל"},
    ],
}
write("tools/levyra-editorial/config.json", json.dumps(CONFIG, ensure_ascii=False, indent=2))

write(
    "tools/levyra-editorial/levyra_editorial/models.py",
    r'''from __future__ import annotations

import hashlib
from dataclasses import dataclass
from typing import Any


def _compact(value: Any) -> Any:
    """Recursively remove ``None`` values while preserving empty lists and strings."""
    if isinstance(value, dict):
        return {key: _compact(item) for key, item in value.items() if item is not None}
    if isinstance(value, list):
        return [_compact(item) for item in value]
    return value


def _public_track_id(track: Track) -> str:
    """Create a stable Levyra-owned identity without publishing upstream identifiers."""
    artist_names = "|".join(artist.name.strip().casefold() for artist in track.artists)
    identity = "\u001f".join(
        (
            track.title.strip().casefold(),
            artist_names,
            track.album.name.strip().casefold(),
            str(track.duration_ms),
        )
    )
    digest = hashlib.sha256(identity.encode("utf-8")).hexdigest()[:20]
    return f"levyra-{digest}"


@dataclass(frozen=True)
class Artist:
    id: str | None
    name: str


@dataclass(frozen=True)
class Album:
    id: str | None
    name: str
    release_date: str | None
    artwork_url: str | None
    external_url: str | None


@dataclass(frozen=True)
class Track:
    position: int
    id: str
    uri: str
    title: str
    artists: list[Artist]
    album: Album
    duration_ms: int
    explicit: bool
    external_url: str | None
    artwork_url: str | None

    def to_dict(self) -> dict[str, Any]:
        return _compact(
            {
                "position": self.position,
                "id": _public_track_id(self),
                "title": self.title,
                "artists": [{"name": artist.name} for artist in self.artists],
                "album": {
                    "name": self.album.name,
                    "releaseDate": self.album.release_date,
                },
                "durationMs": self.duration_ms,
                "explicit": self.explicit,
            }
        )


@dataclass(frozen=True)
class Collection:
    id: str
    kind: str
    market: str
    title: str
    description: str
    source_id: str
    source_url: str | None
    artwork_url: str | None
    snapshot_id: str | None
    total_source_items: int
    tracks: list[Track]

    def to_dict(self) -> dict[str, Any]:
        return _compact(
            {
                "id": self.id,
                "kind": self.kind,
                "market": self.market,
                "title": self.title,
                "description": self.description,
                "totalSourceItems": self.total_source_items,
                "tracks": [track.to_dict() for track in self.tracks],
            }
        )


@dataclass(frozen=True)
class Catalog:
    schema_version: int
    generated_at: str
    collections: list[Collection]

    def to_dict(self) -> dict[str, Any]:
        return {
            "schemaVersion": self.schema_version,
            "generatedAt": self.generated_at,
            "collections": [collection.to_dict() for collection in self.collections],
        }
''',
)

write(
    "tools/levyra-editorial/levyra_editorial/resilient.py",
    r'''from __future__ import annotations

import argparse
import logging
import os
import sys
import time
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from .collector import (
    CATALOG_SCHEMA_VERSION,
    EditorialClient,
    build_catalog,
    load_config,
    utc_now_iso,
    validate_catalog_file,
    write_catalog,
)
from .models import Catalog, Collection
from .spotify import EditorialSourceError, SourceApiError, SpotifyWebClient

LOGGER = logging.getLogger(__name__)
COLLECTION_PAUSE_SECONDS = 0.15


def build_resilient_catalog(
    config: Mapping[str, Any],
    client: EditorialClient,
    *,
    generated_at: str | None = None,
    pause_seconds: float = COLLECTION_PAUSE_SECONDS,
) -> Catalog:
    """Collect every required chart and isolate only explicitly optional markets."""
    raw_collections = config.get("collections")
    if not isinstance(raw_collections, list) or not raw_collections:
        raise ValueError("Collector config collections are missing.")

    collected: list[Collection] = []
    skipped_optional: list[str] = []
    required_ids = {
        str(item.get("id") or "").strip()
        for item in raw_collections
        if isinstance(item, dict) and item.get("optional") is not True
    }
    timestamp = generated_at or utc_now_iso()

    for index, item in enumerate(raw_collections):
        if not isinstance(item, dict):
            continue
        collection_id = str(item.get("id") or "").strip()
        optional = item.get("optional") is True
        try:
            single_catalog = build_catalog(
                {"collections": [item]},
                client,
                generated_at=timestamp,
            )
            collected.extend(single_catalog.collections)
        except SourceApiError as error:
            if not optional:
                raise
            skipped_optional.append(collection_id)
            LOGGER.warning(
                "Skipping optional editorial collection %s: %s",
                collection_id,
                error,
            )
        except ValueError as error:
            if not optional or "produced no usable tracks" not in str(error):
                raise
            skipped_optional.append(collection_id)
            LOGGER.warning("Skipping empty optional editorial collection %s.", collection_id)
        finally:
            if pause_seconds > 0 and index + 1 < len(raw_collections):
                time.sleep(pause_seconds)

    collected_ids = {collection.id for collection in collected}
    missing_required = sorted(required_ids - collected_ids)
    if missing_required:
        raise ValueError(
            "Required editorial collections are missing: " + ", ".join(missing_required)
        )
    if not collected:
        raise ValueError("No configured editorial collection produced usable tracks.")

    LOGGER.info(
        "Collected %d editorial collection(s); skipped %d optional collection(s).",
        len(collected),
        len(skipped_optional),
    )
    if skipped_optional:
        LOGGER.info("Unavailable optional collection ids: %s", ", ".join(skipped_optional))

    return Catalog(
        schema_version=CATALOG_SCHEMA_VERSION,
        generated_at=timestamp,
        collections=collected,
    )


def run_collection(config_path: Path, output_path: Path) -> None:
    """Execute one resilient collector run using the repository Actions secret."""
    config = load_config(config_path)
    raw_secret = os.environ.get("LEVYRA_EDITORIAL_SP_DC", "")
    client = SpotifyWebClient(raw_secret)
    try:
        catalog = build_resilient_catalog(config, client)
        write_catalog(catalog, output_path)
    finally:
        client.close()
    LOGGER.info(
        "Editorial catalog written to %s with %d collection(s).",
        output_path,
        len(catalog.collections),
    )


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate Levyra's resilient editorial metadata catalog."
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("tools/levyra-editorial/config.json"),
        help="Path to the checked-in collection configuration.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("build/editorial/catalog.json"),
        help="Path for the generated catalog.",
    )
    parser.add_argument(
        "--validate",
        type=Path,
        help="Validate an existing catalog instead of collecting remote data.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    """Command-line entry point."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    args = _build_parser().parse_args(argv)
    try:
        if args.validate:
            validate_catalog_file(args.validate)
            LOGGER.info("Catalog validation succeeded: %s", args.validate)
        else:
            run_collection(args.config, args.output)
        return 0
    except (EditorialSourceError, OSError, ValueError) as error:
        LOGGER.error("%s", error)
        return 1


if __name__ == "__main__":
    sys.exit(main())
''',
)

# Collector: remove dead CLI, dead market plumbing and the fake ISRC contract.
for import_line in ("import argparse\n", "import os\n", "import sys\n"):
    replace_once("tools/levyra-editorial/levyra_editorial/collector.py", import_line, "")
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "from .spotify import EditorialSourceError, SpotifyWebClient\n",
    "",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "    def get_playlist_metadata(self, playlist_id: str, market: str) -> dict[str, Any]:\n",
    "    def get_playlist_metadata(self, playlist_id: str) -> dict[str, Any]:\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "    def iter_playlist_items(self, playlist_id: str, market: str) -> list[dict[str, Any]]:\n",
    "    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "        metadata = client.get_playlist_metadata(playlist_id, market)\n        raw_items = client.iter_playlist_items(playlist_id, market)\n",
    "        metadata = client.get_playlist_metadata(playlist_id)\n        raw_items = client.iter_playlist_items(playlist_id)\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "                isrc=_nested_string(raw_track, \"external_ids\", \"isrc\"),\n",
    "",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    "        title = item.get(\"title\")\n        if title is not None and (not isinstance(title, str) or not title.strip()):\n            raise ValueError(f\"Collection '{collection_id}' has an invalid title.\")\n",
    "        title = item.get(\"title\")\n        if title is not None and (not isinstance(title, str) or not title.strip()):\n            raise ValueError(f\"Collection '{collection_id}' has an invalid title.\")\n        optional = item.get(\"optional\")\n        if optional is not None and not isinstance(optional, bool):\n            raise ValueError(f\"Collection '{collection_id}' has an invalid optional flag.\")\n",
)
replace_regex(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    r"\n\ndef run_collection\(config_path: Path, output_path: Path\) -> None:.*?\n\n\ndef _assert_safe_keys",
    "\n\ndef _assert_safe_keys",
    flags=re.DOTALL,
)
replace_regex(
    "tools/levyra-editorial/levyra_editorial/collector.py",
    r"\n\ndef _build_parser\(\) -> argparse\.ArgumentParser:.*\Z",
    "",
    flags=re.DOTALL,
)

# Spotify session flow: pin and validate dependencies, regenerate OTP per attempt,
# preserve source positions and retry 429 once even when Retry-After is absent.
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "import os\nimport struct\n",
    "import os\nimport re\nimport struct\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "from typing import Any\n",
    "from typing import Any\nfrom urllib.parse import urlparse\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    'DEFAULT_SECRET_DICT_URL = (\n    "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"\n)\n',
    'DEFAULT_SECRET_DICT_URL = (\n    "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/"\n    "4cd9440671af3a419bad112164a193ea1374e0e1/secrets/secretDict.json"\n)\n',
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "        self._secret_dict_url = (\n            secret_dict_url\n            or os.environ.get(\"LEVYRA_EDITORIAL_TOTP_SECRETS_URL\")\n            or DEFAULT_SECRET_DICT_URL\n        )\n        self._playlist_query_hash = (\n            playlist_query_hash\n            or os.environ.get(\"LEVYRA_EDITORIAL_PLAYLIST_QUERY_HASH\")\n            or DEFAULT_PLAYLIST_QUERY_HASH\n        )\n",
    "        self._secret_dict_url = validate_secret_dict_url(\n            secret_dict_url\n            or os.environ.get(\"LEVYRA_EDITORIAL_TOTP_SECRETS_URL\")\n            or DEFAULT_SECRET_DICT_URL\n        )\n        self._playlist_query_hash = validate_playlist_query_hash(\n            playlist_query_hash\n            or os.environ.get(\"LEVYRA_EDITORIAL_PLAYLIST_QUERY_HASH\")\n            or DEFAULT_PLAYLIST_QUERY_HASH\n        )\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "        server_time = self._fetch_server_time()\n        otp = generate_totp(totp_secret, server_time)\n\n        last_error: Exception | None = None\n",
    "        last_error: Exception | None = None\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "            try:\n                token_data = self._request_access_token(\n",
    "            try:\n                server_time = self._fetch_server_time()\n                otp = generate_totp(totp_secret, server_time)\n                token_data = self._request_access_token(\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "    def get_playlist_metadata(\n        self,\n        playlist_id: str,\n        market: str,\n    ) -> dict[str, Any]:\n",
    "    def get_playlist_metadata(\n        self,\n        playlist_id: str,\n    ) -> dict[str, Any]:\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "    def iter_playlist_items(\n        self,\n        playlist_id: str,\n        market: str,\n    ) -> list[dict[str, Any]]:\n",
    "    def iter_playlist_items(\n        self,\n        playlist_id: str,\n    ) -> list[dict[str, Any]]:\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "            converted = [\n                item\n                for raw_item in raw_items\n                if isinstance(raw_item, Mapping)\n                if (item := _convert_playlist_item(raw_item)) is not None\n            ]\n",
    "            converted = [\n                converted_item if converted_item is not None else {\"track\": None}\n                for raw_item in raw_items\n                for converted_item in [\n                    _convert_playlist_item(raw_item)\n                    if isinstance(raw_item, Mapping)\n                    else None\n                ]\n            ]\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "def _bounded_retry_after(value: str | None) -> int:\n    if not value:\n        return 0\n",
    "def _bounded_retry_after(value: str | None) -> int:\n    if not value:\n        return 1\n",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/spotify.py",
    "def _spotify_id(uri: str | None) -> str | None:\n",
    r'''def validate_secret_dict_url(value: str) -> str:
    """Allow only the pinned third-party dictionary path over HTTPS."""
    normalized = value.strip()
    parsed = urlparse(normalized)
    expected_path = re.fullmatch(
        r"/xyloflake/spot-secrets-go/[0-9a-f]{40}/secrets/secretDict\.json",
        parsed.path,
    )
    if (
        parsed.scheme != "https"
        or parsed.hostname != "raw.githubusercontent.com"
        or parsed.username is not None
        or parsed.password is not None
        or parsed.port is not None
        or parsed.query
        or parsed.fragment
        or expected_path is None
    ):
        raise AuthenticationError("The TOTP dictionary URL is not allowlisted.")
    return normalized


def validate_playlist_query_hash(value: str) -> str:
    normalized = value.strip().lower()
    if re.fullmatch(r"[0-9a-f]{64}", normalized) is None:
        raise AuthenticationError("The playlist query hash is malformed.")
    return normalized


def _spotify_id(uri: str | None) -> str | None:
''',
)

# Android: source catalog is a shared, bounded, age-checked cache. It never ships
# source artwork; chart artwork is resolved by Levyra and then preserved in playback.
write(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    r'''package com.luc4n3x.levyra.data

import android.content.Context
import android.util.AtomicFile
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reads Levyra's public, pre-normalized editorial ranking catalog.
 *
 * The source credential and source artwork never enter the app. A single process-wide instance owns
 * the remote refresh, memory snapshot and AtomicFile cache so Home and Android Auto cannot race.
 */
internal class EditorialChartsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshGuard = Any()
    private val cacheFile = AtomicFile(File(appContext.filesDir, CACHE_RELATIVE_PATH))
    private val httpClient = LevyraHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(BrotliInterceptor)
        .cache(Cache(File(appContext.cacheDir, HTTP_CACHE_DIRECTORY), HTTP_CACHE_BYTES))
        .build()

    @Volatile
    private var memorySnapshot: CatalogSnapshot? = null

    @Volatile
    private var refreshDeferred: Deferred<CatalogSnapshot?>? = null

    @Volatile
    private var lastRefreshFailureAt: Long = 0L

    fun warm() {
        refreshAsync()
    }

    suspend fun cachedTopTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.tracks(country, limit)
    }

    private fun refreshAsync(): Deferred<CatalogSnapshot?> = synchronized(refreshGuard) {
        refreshDeferred?.takeIf { it.isActive }?.let { return@synchronized it }
        val now = System.currentTimeMillis()
        if (now - lastRefreshFailureAt in 0 until REFRESH_RETRY_TTL_MS) {
            return@synchronized CompletableDeferred(usableSnapshot(now))
        }
        scope.async {
            val stored = usableSnapshot(System.currentTimeMillis())
            val remote = fetchRemoteSnapshot()
            if (remote != null) {
                persist(remote.rawJson)
                memorySnapshot = remote
                lastRefreshFailureAt = 0L
                remote
            } else {
                lastRefreshFailureAt = System.currentTimeMillis()
                stored
            }
        }.also { created ->
            refreshDeferred = created
            created.invokeOnCompletion {
                synchronized(refreshGuard) {
                    if (refreshDeferred === created) refreshDeferred = null
                }
            }
        }
    }

    private fun usableSnapshot(now: Long): CatalogSnapshot? {
        memorySnapshot?.let { cached ->
            if (cached.isUsable(now)) return cached
            memorySnapshot = null
        }
        val stored = readStoredSnapshot(now) ?: return null
        memorySnapshot = stored
        return stored
    }

    private suspend fun fetchRemoteSnapshot(): CatalogSnapshot? =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val snapshot = runCatching {
                        response.use { current ->
                            if (!current.isSuccessful) return@use null
                            val body = current.body ?: return@use null
                            val bytes = body.byteStream().readBounded(MAX_CATALOG_BYTES) ?: return@use null
                            if (bytes.isEmpty()) return@use null
                            EditorialCatalogParser.parse(
                                body = bytes.toString(StandardCharsets.UTF_8),
                                loadedAt = System.currentTimeMillis(),
                            )
                        }
                    }.getOrNull()
                    if (continuation.isActive) continuation.resume(snapshot)
                }
            })
        }

    private fun readStoredSnapshot(now: Long): CatalogSnapshot? {
        val bytes = runCatching {
            cacheFile.openRead().use { it.readBounded(MAX_CATALOG_BYTES) }
        }.getOrNull() ?: return null
        val snapshot = EditorialCatalogParser.parse(
            body = bytes.toString(StandardCharsets.UTF_8),
            loadedAt = now,
        ) ?: return null
        return snapshot.takeIf { it.isUsable(now) }
    }

    private fun persist(body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size !in 1..MAX_CATALOG_BYTES) return
        val stream = runCatching { cacheFile.startWrite() }.getOrNull() ?: return
        try {
            stream.write(bytes)
            stream.fd.sync()
            cacheFile.finishWrite(stream)
        } catch (error: Throwable) {
            cacheFile.failWrite(stream)
            Timber.w(error, "Unable to persist editorial chart catalog")
        }
    }

    private fun InputStream.readBounded(maxBytes: Int): ByteArray? {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        @Volatile
        private var instance: EditorialChartsRepository? = null

        fun get(context: Context): EditorialChartsRepository {
            return instance ?: synchronized(this) {
                instance ?: EditorialChartsRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val CACHE_RELATIVE_PATH = "editorial/charts-v2.json"
        private const val HTTP_CACHE_DIRECTORY = "levyra_editorial_http"
        private const val HTTP_CACHE_BYTES = 4L * 1024L * 1024L
        private const val REFRESH_RETRY_TTL_MS = 5L * 60L * 1000L
        private const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
        private const val CATALOG_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json"
    }
}

internal data class CatalogSnapshot(
    val byMarket: Map<String, List<Track>>,
    val generatedAtMs: Long,
    val loadedAt: Long,
    val rawJson: String,
) {
    fun isUsable(now: Long): Boolean {
        val age = now - generatedAtMs
        return age in -MAX_FUTURE_SKEW_MS..MAX_CATALOG_AGE_MS
    }

    fun needsRefresh(now: Long): Boolean = now - generatedAtMs > REFRESH_AFTER_MS

    fun tracks(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        return byMarket[market].orEmpty().take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    private companion object {
        const val DEFAULT_MARKET = "IT"
        const val MAX_TRACKS_PER_MARKET = 100
        const val REFRESH_AFTER_MS = 30L * 60L * 1000L
        const val MAX_CATALOG_AGE_MS = 48L * 60L * 60L * 1000L
        const val MAX_FUTURE_SKEW_MS = 10L * 60L * 1000L
    }
}

internal object EditorialCatalogParser {
    fun parse(body: String, loadedAt: Long): CatalogSnapshot? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("schemaVersion", -1) != SUPPORTED_SCHEMA_VERSION) return null
        val generatedAtMs = parseInstant(root.optString("generatedAt")) ?: return null
        val collections = root.optJSONArray("collections") ?: return null
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
        if (items == null) return emptyList()
        val tracks = ArrayList<Track>(minOf(items.length(), MAX_TRACKS_PER_MARKET))
        for (index in 0 until items.length()) {
            if (tracks.size >= MAX_TRACKS_PER_MARKET) break
            val item = items.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = parseArtists(item.optJSONArray("artists"))
            if (title.isBlank() || artist.isBlank()) continue
            val album = item.optJSONObject("album")
            val releaseDate = album?.optString("releaseDate").orEmpty().trim()
            val identity = chartIdentity("$title|$artist")
            val palette = PALETTES[identity.seed % PALETTES.size]
            tracks += Track(
                id = "chart-${identity.id}",
                title = title,
                artist = artist,
                album = album?.optString("name").orEmpty().trim().ifBlank { EDITORIAL_ALBUM },
                durationMs = item.optLong("durationMs", 0L).coerceAtLeast(0L),
                streamUrl = "",
                videoUrl = "",
                thumbnailUrl = "",
                largeThumbnailUrl = "",
                source = EDITORIAL_SOURCE,
                moodTags = setOf("hit", "chart"),
                energy = 70,
                vocal = 55,
                replayScore = 95,
                cacheScore = 88,
                accentStart = palette.first,
                accentEnd = palette.second,
                releaseDate = releaseDate,
                year = releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
                explicit = item.optBoolean("explicit", false),
                metadataProvider = EDITORIAL_SOURCE,
                metadataConfidence = 94,
            )
        }
        return tracks.distinctBy { it.id }
    }

    private fun parseArtists(items: JSONArray?): String {
        if (items == null) return ""
        return buildList {
            for (index in 0 until items.length()) {
                val name = items.optJSONObject(index)?.optString("name").orEmpty().trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinct().joinToString(", ")
    }

    private fun parseInstant(value: String): Long? {
        return try {
            Instant.parse(value.trim()).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun chartIdentity(value: String): ChartIdentity {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        val seed = digest.take(4).fold(0) { accumulator, byte ->
            (accumulator shl 8) or (byte.toInt() and 0xFF)
        } and Int.MAX_VALUE
        val id = digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
        return ChartIdentity(seed = seed, id = id)
    }

    private data class ChartIdentity(val seed: Int, val id: String)

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MAX_TRACKS_PER_MARKET = 100
    private const val EDITORIAL_SOURCE = "Levyra Editorial"
    private const val EDITORIAL_ALBUM = "Levyra Top 50"

    private val PALETTES = listOf(
        0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
        0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
        0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
        0xFFFFB000.toInt() to 0xFF00E5FF.toInt(),
    )
}
''',
)

# Remove the serial editorial call from the YouTube repository.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "import kotlinx.coroutines.withTimeoutOrNull\n",
    "",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "    private val editorialCharts = EditorialChartsRepository(appContext)\n",
    "",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "        val editorial = withTimeoutOrNull(EDITORIAL_PRIMARY_BUDGET_MS) {\n            editorialCharts.topTracks(request.country, request.limit)\n        }.orEmpty()\n        if (editorial.isNotEmpty()) return@withContext editorial\n\n",
    "",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "        const val EDITORIAL_PRIMARY_BUDGET_MS = 1_500L\n",
    "",
)

# Charts use an instant local editorial snapshot and warm the network independently,
# leaving the existing YouTube/Apple latency budget untouched.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "    private val youtubeMusicCharts = YoutubeMusicChartsRepository(appContext)\n    private val chartArtworkResolver = ChartOfficialArtworkResolver(appContext)\n",
    "    private val youtubeMusicCharts = YoutubeMusicChartsRepository(appContext)\n    private val editorialCharts = EditorialChartsRepository.get(appContext)\n    private val chartArtworkResolver = ChartOfficialArtworkResolver(appContext)\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "    private val inFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()\n\n    fun close() {\n",
    "    private val inFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()\n\n    init {\n        editorialCharts.warm()\n    }\n\n    fun close() {\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> {\n        val ranked = selectTopSongs(country, limit)\n        if (ranked.isEmpty() || ranked.none(::isYoutubeMusicChartTrack)) return ranked\n        return chartArtworkResolver.enrich(ranked, country)\n    }\n",
    "    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> {\n        val ranked = selectTopSongs(country, limit)\n        if (ranked.isEmpty()) return ranked\n        return chartArtworkResolver.enrich(ranked, country)\n    }\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "    private suspend fun selectTopSongs(country: String, limit: Int): List<Track> = coroutineScope {\n        val youtube = async { fetchYoutubeTopSongs(country, limit) }\n",
    "    private suspend fun selectTopSongs(country: String, limit: Int): List<Track> = coroutineScope {\n        val editorial = editorialCharts.cachedTopTracks(country, limit)\n        if (editorial.isNotEmpty()) return@coroutineScope editorial\n        editorialCharts.warm()\n\n        val youtube = async { fetchYoutubeTopSongs(country, limit) }\n",
)

# Preserve the exact cover shown in a Top 50 row after the playable source is resolved.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    "    suspend fun resolve(track: Track, isVideoMode: Boolean = false): Track {\n        return resolveInternal(\n",
    "    suspend fun resolve(track: Track, isVideoMode: Boolean = false): Track {\n        val resolved = resolveInternal(\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    "            reuseProvidedStream = true\n        )\n    }\n\n    suspend fun resolveForOffline",
    "            reuseProvidedStream = true\n        )\n        return preserveEditorialArtwork(track, resolved)\n    }\n\n    suspend fun resolveForOffline",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    "    suspend fun resolveForOffline(track: Track, audioQualityOverride: String? = null): Track {\n        val quality = normalizeAudioQuality(audioQualityOverride ?: selectedAudioQuality)\n        return resolveInternal(\n",
    "    suspend fun resolveForOffline(track: Track, audioQualityOverride: String? = null): Track {\n        val quality = normalizeAudioQuality(audioQualityOverride ?: selectedAudioQuality)\n        val resolved = resolveInternal(\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    "            reuseProvidedStream = audioQualityOverride == null\n        )\n    }\n\n    private suspend fun resolveInternal",
    "            reuseProvidedStream = audioQualityOverride == null\n        )\n        return preserveEditorialArtwork(track, resolved)\n    }\n\n    private suspend fun resolveInternal",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt",
    "class PlaybackResolver private constructor(private val context: Context) {\n",
    "internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {\n    if (!presented.source.equals(\"Levyra Editorial\", ignoreCase = true)) return resolved\n    val artwork = presented.largeThumbnailUrl.trim().ifBlank { presented.thumbnailUrl.trim() }\n    if (artwork.isBlank()) return resolved\n    return resolved.copy(thumbnailUrl = artwork, largeThumbnailUrl = artwork)\n}\n\nclass PlaybackResolver private constructor(private val context: Context) {\n",
)

write(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt",
    r'''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialArtworkContinuityTest {
    @Test
    fun playerKeepsTheArtworkPresentedInTheTop50Row() {
        val presented = track(
            source = "Levyra Editorial",
            thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
        ).copy(id = "abcdefghijk", videoUrl = "https://music.youtube.com/watch?v=abcdefghijk")

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
        assertEquals(resolved.videoUrl, result.videoUrl)
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
        val presented = track(source = "Search", thumbnail = "https://example.test/old.jpg")
        val resolved = track(source = "YouTube Music", thumbnail = "https://example.test/new.jpg")

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

    private fun track(source: String, thumbnail: String): Track = Track(
        id = "chart-id",
        title = "Titolo",
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = thumbnail,
        largeThumbnailUrl = thumbnail,
        source = source,
        moodTags = setOf("chart"),
        energy = 70,
        vocal = 55,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
    )
}
''',
)

write(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    r'''package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditorialCatalogParserTest {
    @Test
    fun parsesCountryChartWithoutSourceArtworkOrFakeIsrc() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = NOW)

        assertNotNull(snapshot)
        val italy = snapshot!!.byMarket.getValue("IT")
        val track = italy.single()
        assertEquals("Prima Canzone", track.title)
        assertEquals("Artista Uno, Artista Due", track.artist)
        assertEquals("Album Uno", track.album)
        assertEquals(181_000L, track.durationMs)
        assertTrue(track.isrc.isBlank())
        assertTrue(track.thumbnailUrl.isBlank())
        assertTrue(track.largeThumbnailUrl.isBlank())
        assertEquals("2026", track.year)
        assertTrue(track.explicit)
        assertEquals("Levyra Editorial", track.source)

        val legacy = ChartFeedParser.modern(legacyFeed, limit = 1).single()
        assertEquals(legacy.id, track.id)
    }

    @Test
    fun onlyConfiguredTwoLetterMarketsAreExposed() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = NOW)!!

        assertEquals(setOf("IT"), snapshot.byMarket.keys)
        assertFalse(snapshot.byMarket.containsKey("GLOBAL"))
    }

    @Test
    fun rejectsWrongSchemaMissingTimestampAndStaleCatalogs() {
        assertNull(EditorialCatalogParser.parse("""{"schemaVersion":2,"collections":[]}""", NOW))
        assertNull(EditorialCatalogParser.parse("""{"schemaVersion":1,"collections":[]}""", NOW))

        val stale = EditorialCatalogParser.parse(
            validCatalog.replace("2026-07-29T18:00:00Z", "2026-07-26T18:00:00Z"),
            loadedAt = NOW,
        )
        assertNotNull(stale)
        assertFalse(stale!!.isUsable(NOW))
    }

    private companion object {
        val NOW: Long = Instant.parse("2026-07-29T20:00:00Z").toEpochMilli()
    }

    private val validCatalog = """
        {
          "schemaVersion": 1,
          "generatedAt": "2026-07-29T18:00:00Z",
          "collections": [
            {
              "id": "top-50-global",
              "kind": "chart",
              "market": "GLOBAL",
              "tracks": [{"position":1,"id":"global","title":"Global","artists":[{"name":"Artist"}]}]
            },
            {
              "id": "top-50-it",
              "kind": "chart",
              "market": "IT",
              "tracks": [
                {
                  "position": 1,
                  "id": "source-track-id",
                  "title": "Prima Canzone",
                  "artists": [
                    {"name": "Artista Uno"},
                    {"name": "Artista Due"}
                  ],
                  "album": {
                    "name": "Album Uno",
                    "releaseDate": "2026-07-01"
                  },
                  "durationMs": 181000,
                  "explicit": true
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private val legacyFeed = """
        {
          "feed": {
            "results": [
              {
                "name": "Prima Canzone",
                "artistName": "Artista Uno, Artista Due",
                "artworkUrl100": "https://image.example/100x100bb.jpg"
              }
            ]
          }
        }
    """.trimIndent()
}
''',
)

# Python tests: production-shaped fixtures, optional-only degradation and URL pinning.
collector_tests = read("tools/levyra-editorial/tests/test_collector.py")
collector_tests = collector_tests.replace("def get_playlist_metadata(self, playlist_id: str, market: str)", "def get_playlist_metadata(self, playlist_id: str)")
collector_tests = collector_tests.replace("def iter_playlist_items(self, playlist_id: str, market: str)", "def iter_playlist_items(self, playlist_id: str)")
collector_tests = collector_tests.replace('                    "external_ids": {"isrc": "ITABC2600001"},\n', '                    "external_ids": {},\n')
collector_tests = collector_tests.replace('    assert track["isrc"] == "ITABC2600001"\n', '    assert "isrc" not in track\n    assert "artworkUrl" not in track\n    assert "artworkUrl" not in track["album"]\n')
collector_tests = collector_tests.replace('    items = FakeClient().iter_playlist_items("playlist12345", "IT")\n', '    items = FakeClient().iter_playlist_items("playlist12345")\n')
collector_tests = collector_tests.replace('    assert "spotify" not in serialized\n', '    assert "open.spotify.com" not in serialized\n    assert "scdn.co" not in serialized\n')
collector_tests = collector_tests.replace(
    "    select_latest_totp_secret,\n)",
    "    select_latest_totp_secret,\n    validate_playlist_query_hash,\n    validate_secret_dict_url,\n)",
)
collector_tests += r'''


def test_secret_dictionary_url_requires_pinned_allowlisted_path() -> None:
    valid = (
        "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/"
        "4cd9440671af3a419bad112164a193ea1374e0e1/secrets/secretDict.json"
    )
    assert validate_secret_dict_url(valid) == valid
    with pytest.raises(AuthenticationError):
        validate_secret_dict_url("https://example.com/secretDict.json")
    with pytest.raises(AuthenticationError):
        validate_secret_dict_url(
            "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"
        )


def test_playlist_query_hash_is_strictly_validated() -> None:
    assert validate_playlist_query_hash("a" * 64) == "a" * 64
    with pytest.raises(AuthenticationError):
        validate_playlist_query_hash("not-a-hash")
'''
write("tools/levyra-editorial/tests/test_collector.py", collector_tests)

pathfinder_tests = read("tools/levyra-editorial/tests/test_pathfinder.py")
pathfinder_tests = pathfinder_tests.replace('client.get_playlist_metadata("playlist12345", "IT")', 'client.get_playlist_metadata("playlist12345")')
pathfinder_tests = pathfinder_tests.replace('client.iter_playlist_items("playlist12345", "IT")', 'client.iter_playlist_items("playlist12345")')
write("tools/levyra-editorial/tests/test_pathfinder.py", pathfinder_tests)

write(
    "tools/levyra-editorial/tests/test_resilient.py",
    r'''from __future__ import annotations

from typing import Any

import pytest

from levyra_editorial.resilient import build_resilient_catalog
from levyra_editorial.spotify import SourceApiError


class PartialClient:
    def __init__(self, unavailable_playlist_ids: set[str]) -> None:
        self.unavailable_playlist_ids = unavailable_playlist_ids

    def get_playlist_metadata(self, playlist_id: str) -> dict[str, Any]:
        if playlist_id in self.unavailable_playlist_ids:
            raise SourceApiError("playlist content is unavailable")
        return {
            "id": playlist_id,
            "name": "Top 50",
            "description": "Daily chart",
            "external_urls": {},
            "images": [],
            "tracks": {"total": 1},
        }

    def iter_playlist_items(self, playlist_id: str) -> list[dict[str, Any]]:
        if playlist_id in self.unavailable_playlist_ids:
            raise SourceApiError("playlist content is unavailable")
        return [
            {
                "track": {
                    "id": f"track{playlist_id[-2:]}",
                    "uri": f"spotify:track:track{playlist_id[-2:]}",
                    "type": "track",
                    "name": "Chart song",
                    "duration_ms": 180_000,
                    "explicit": False,
                    "external_ids": {},
                    "external_urls": {},
                    "artists": [{"id": "artist1", "name": "Chart Artist"}],
                    "album": {"id": "album1", "name": "Chart Album", "images": [], "external_urls": {}},
                }
            }
        ]


def config(*, optional_ru: bool = True) -> dict[str, Any]:
    return {
        "collections": [
            {
                "id": "top-50-it",
                "kind": "chart",
                "market": "IT",
                "playlistId": "playlistitaly123",
                "title": "Top 50 Italia",
            },
            {
                "id": "top-50-ru",
                "kind": "chart",
                "market": "RU",
                "playlistId": "playlistrussia12",
                "title": "Top 50 Russia",
                "optional": optional_ru,
            },
        ],
    }


def test_optional_market_can_be_skipped_without_mutilating_required_catalog() -> None:
    catalog = build_resilient_catalog(
        config(optional_ru=True),
        PartialClient({"playlistrussia12"}),
        generated_at="2026-07-29T18:00:00Z",
        pause_seconds=0,
    )
    assert [collection.id for collection in catalog.collections] == ["top-50-it"]


def test_required_market_failure_blocks_publication() -> None:
    with pytest.raises(SourceApiError, match="unavailable"):
        build_resilient_catalog(
            config(optional_ru=True),
            PartialClient({"playlistitaly123"}),
            generated_at="2026-07-29T18:00:00Z",
            pause_seconds=0,
        )


def test_optional_flag_does_not_make_other_markets_optional() -> None:
    with pytest.raises(SourceApiError, match="unavailable"):
        build_resilient_catalog(
            config(optional_ru=False),
            PartialClient({"playlistrussia12"}),
            generated_at="2026-07-29T18:00:00Z",
            pause_seconds=0,
        )
''',
)

write(
    ".github/workflows/editorial-catalog.yml",
    r'''name: Editorial Catalog

on:
  pull_request:
    paths:
      - "tools/levyra-editorial/**"
      - "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt"
      - "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt"
      - "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt"
      - "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
      - "app/src/test/java/com/luc4n3x/levyra/data/Editorial*.kt"
      - ".github/workflows/editorial-catalog.yml"
  schedule:
    - cron: "17 */6 * * *"
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: editorial-catalog-${{ github.event_name == 'pull_request' && github.ref || 'publisher' }}
  cancel-in-progress: true

jobs:
  verify:
    name: Verify Collector
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - name: Checkout
        uses: actions/checkout@v7
      - name: Setup Python
        uses: actions/setup-python@v6
        with:
          python-version: "3.12"
          cache: pip
          cache-dependency-path: tools/levyra-editorial/pyproject.toml
      - name: Install Collector
        run: python -m pip install --disable-pip-version-check -e "tools/levyra-editorial[dev]"
      - name: Static Checks
        run: |
          set -euo pipefail
          python -m compileall -q tools/levyra-editorial/levyra_editorial
          ruff check tools/levyra-editorial
      - name: Unit Tests
        run: pytest tools/levyra-editorial/tests

  integration:
    name: Test Live Editorial Collection
    if: >-
      github.event_name == 'pull_request' &&
      github.event.pull_request.head.repo.full_name == github.repository &&
      github.actor != 'dependabot[bot]'
    needs: verify
    runs-on: ubuntu-latest
    timeout-minutes: 10
    permissions:
      contents: read
    env:
      LEVYRA_EDITORIAL_SP_DC: ${{ secrets.LEVYRA_EDITORIAL_SP_DC }}
    steps:
      - name: Checkout Pull Request
        uses: actions/checkout@v7
      - name: Setup Python
        uses: actions/setup-python@v6
        with:
          python-version: "3.12"
          cache: pip
          cache-dependency-path: tools/levyra-editorial/pyproject.toml
      - name: Install Collector
        run: python -m pip install --disable-pip-version-check -e "tools/levyra-editorial"
      - name: Require Editorial Secret
        run: |
          set -euo pipefail
          test -n "${LEVYRA_EDITORIAL_SP_DC:-}" || { echo "::error::Missing LEVYRA_EDITORIAL_SP_DC"; exit 1; }
      - name: Generate and Validate Live Catalog
        run: |
          set -euo pipefail
          timeout --foreground 240s levyra-editorial --config tools/levyra-editorial/config.json --output build/editorial/catalog.json
          levyra-editorial --validate build/editorial/catalog.json
          test "$(jq '.collections | length' build/editorial/catalog.json)" -ge 26
          if jq -e '.. | objects | has("artworkUrl") or has("isrc")' build/editorial/catalog.json >/dev/null; then
            echo "::error::Catalog contains source artwork or unsupported ISRC fields"
            exit 1
          fi
          sha256sum build/editorial/catalog.json > build/editorial/catalog.sha256
      - name: Upload Pull Request Diagnostics
        uses: actions/upload-artifact@v7
        with:
          name: editorial-pr-${{ github.run_number }}
          path: |
            build/editorial/catalog.json
            build/editorial/catalog.sha256
          if-no-files-found: error
          retention-days: 7

  collect:
    name: Collect and Publish
    if: >-
      github.event_name != 'pull_request' ||
      (
        github.event.pull_request.head.repo.full_name == github.repository &&
        github.event.pull_request.user.login == github.repository_owner &&
        github.actor != 'dependabot[bot]'
      )
    needs: verify
    runs-on: ubuntu-latest
    timeout-minutes: 12
    permissions:
      contents: write
    env:
      LEVYRA_EDITORIAL_SP_DC: ${{ secrets.LEVYRA_EDITORIAL_SP_DC }}
    steps:
      - name: Checkout
        uses: actions/checkout@v7
      - name: Setup Python
        uses: actions/setup-python@v6
        with:
          python-version: "3.12"
          cache: pip
          cache-dependency-path: tools/levyra-editorial/pyproject.toml
      - name: Install Collector
        run: python -m pip install --disable-pip-version-check -e "tools/levyra-editorial"
      - name: Require Editorial Secret
        run: |
          set -euo pipefail
          test -n "${LEVYRA_EDITORIAL_SP_DC:-}" || { echo "::error::Missing LEVYRA_EDITORIAL_SP_DC"; exit 1; }
      - name: Generate Catalog
        run: |
          set -euo pipefail
          timeout --foreground 360s levyra-editorial --config tools/levyra-editorial/config.json --output build/editorial/catalog.json
          levyra-editorial --validate build/editorial/catalog.json
          test "$(jq '.collections | length' build/editorial/catalog.json)" -ge 26
          sha256sum build/editorial/catalog.json > build/editorial/catalog.sha256
      - name: Upload Diagnostics
        uses: actions/upload-artifact@v7
        with:
          name: editorial-catalog-${{ github.run_number }}
          path: |
            build/editorial/catalog.json
            build/editorial/catalog.sha256
          if-no-files-found: error
          retention-days: 14
      - name: Publish Last Valid Catalog
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          set -euo pipefail
          branch="editorial-data"
          api_path="repos/${GITHUB_REPOSITORY}/contents/catalog/editorial.json"
          output="build/editorial/catalog.json"

          if ! git ls-remote --exit-code origin "refs/heads/${branch}" >/dev/null 2>&1; then
            source_ref="$(git rev-parse HEAD)"
            git config user.name "github-actions[bot]"
            git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
            git checkout --orphan "${branch}"
            git rm -rf .
            mkdir -p catalog
            cp "${output}" catalog/editorial.json
            git add catalog/editorial.json
            git commit -m "chore(editorial): bootstrap data branch"
            git push origin "HEAD:refs/heads/${branch}"
            git checkout --detach "${source_ref}"
          fi

          current_sha="$(gh api "${api_path}?ref=${branch}" --jq .sha 2>/dev/null || true)"
          if [ -n "${current_sha}" ]; then
            gh api -H "Accept: application/vnd.github.raw+json" "${api_path}?ref=${branch}" > /tmp/current-editorial.json
            jq -e . /tmp/current-editorial.json >/dev/null
            jq -S 'del(.generatedAt)' /tmp/current-editorial.json > /tmp/current-editorial-content.json
            jq -S 'del(.generatedAt)' "${output}" > /tmp/new-editorial-content.json
            if cmp -s /tmp/current-editorial-content.json /tmp/new-editorial-content.json; then
              echo "Catalog content is unchanged; publication skipped."
              exit 0
            fi
          fi

          encoded="$(base64 -w 0 "${output}")"
          message="chore(editorial): refresh catalog from run ${GITHUB_RUN_NUMBER}"
          if [ -n "${current_sha}" ]; then
            gh api --method PUT "${api_path}" -f branch="${branch}" -f message="${message}" -f content="${encoded}" -f sha="${current_sha}" >/dev/null
          else
            gh api --method PUT "${api_path}" -f branch="${branch}" -f message="${message}" -f content="${encoded}" >/dev/null
          fi
          echo "Published catalog to ${branch}/catalog/editorial.json"
''',
)

# Documentation is replaced with the actual contract after the review fixes.
write(
    "tools/levyra-editorial/README.md",
    r'''# Levyra editorial collector

This repository-owned Python tool reads configured public country rankings with a dedicated source account and publishes a compact, account-free ranking catalog for Levyra.

## Security and data boundaries

- `LEVYRA_EDITORIAL_SP_DC` exists only as a GitHub Actions repository secret.
- The cookie, TOTP material and short-lived token are never written to source, artifacts, logs, JSON or the APK.
- The TOTP dictionary URL is HTTPS-allowlisted and pinned to an immutable commit.
- The public catalog contains ranking position, title, artist, album, release date, duration and explicit flag only.
- Source artwork, source URLs, source IDs and unsupported ISRC values are deliberately omitted.
- Android obtains artwork independently and keeps the exact same artwork when the selected row opens in the player.
- Required country failures block publication. Only collections explicitly marked `optional` may be skipped.
- A failed or incomplete run never replaces the last valid catalog.

The implementation is original Levyra code. SimpMusic was used only as a behavioral reference for the current `sp_dc` plus TOTP session exchange; no SimpMusic source code was copied.

## Repository secret

Create `LEVYRA_EDITORIAL_SP_DC` in:

```text
Repository Settings → Secrets and variables → Actions → New repository secret
```

Paste only the `sp_dc` value. Same-repository pull requests can run the live read-only integration test; fork and Dependabot pull requests never receive repository secrets.

## Collections

`config.json` maps Levyra's existing country chips to stable public playlist IDs. All configured markets are required unless a collection has `"optional": true`. Russia is currently optional because that public playlist can be unavailable; Levyra transparently keeps its existing YouTube/Apple fallback for any absent market. The unused global collection is not downloaded.

## Publication

The workflow runs every six hours and on manual dispatch. It validates the complete catalog, compares the raw previous JSON without `generatedAt`, and updates the data-only `editorial-data` branch only when substantive content changed.

Public URL:

```text
https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json
```

Android uses a process-wide repository, a separate bounded HTTP cache, an `AtomicFile` disk snapshot, a 30-minute refresh target and a 48-hour maximum catalog age. Network refresh runs independently and never consumes the existing YouTube chart latency budget.
''',
)

print("Editorial review fixes applied.")
