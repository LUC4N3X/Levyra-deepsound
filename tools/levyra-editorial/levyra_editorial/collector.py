from __future__ import annotations

import json
import logging
import re
from collections.abc import Mapping
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, Protocol

from .models import Album, Artist, Catalog, Collection, Track

LOGGER = logging.getLogger(__name__)
CATALOG_SCHEMA_VERSION = 1
CONFIG_SCHEMA_VERSION = 1
COLLECTION_ID_PATTERN = re.compile(r"[a-z0-9][a-z0-9._-]{0,63}")


class EditorialClient(Protocol):
    """Minimum metadata-source interface required by the collector."""

    def get_playlist_metadata(self, playlist_id: str) -> dict[str, Any]:
        """Return public metadata for a playlist."""

    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        """Return ordered playlist items, bounded by limit when it is provided."""

    def resolve_playlist_id(
        self,
        query: str,
        market: str,
        title_hints: list[str],
    ) -> str:
        """Resolve an official editorial playlist from a localized query."""


def utc_now_iso() -> str:
    """Return a stable UTC timestamp suitable for the public catalog."""
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def load_config(path: Path) -> dict[str, Any]:
    """Read and validate the checked-in collector configuration."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Unable to read collector config: {path}") from error
    if not isinstance(payload, dict):
        raise ValueError("Collector config must be a JSON object.")
    if payload.get("schemaVersion") != CONFIG_SCHEMA_VERSION:
        raise ValueError(f"Collector config schemaVersion must be {CONFIG_SCHEMA_VERSION}.")

    collections = payload.get("collections")
    if not isinstance(collections, list) or not collections:
        raise ValueError("Collector config must define at least one collection.")

    seen_ids: set[str] = set()
    for index, item in enumerate(collections):
        if not isinstance(item, dict):
            raise ValueError(f"Collection #{index + 1} must be an object.")
        collection_id = str(item.get("id", "")).strip()
        playlist_id = str(item.get("playlistId", "")).strip()
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
    return payload


def build_catalog(
    config: Mapping[str, Any],
    client: EditorialClient,
    *,
    generated_at: str | None = None,
) -> Catalog:
    """Collect configured playlists and normalize them into Levyra's data contract."""
    output: list[Collection] = []
    raw_collections = config.get("collections")
    if not isinstance(raw_collections, list):
        raise ValueError("Collector config collections are missing.")

    for item in raw_collections:
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
        schema_version=CATALOG_SCHEMA_VERSION,
        generated_at=generated_at or utc_now_iso(),
        collections=output,
    )


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
        for value in (item.get("titleHints") or [])
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
            raw_items = client.iter_playlist_items(playlist_id, limit=item_limit)[:item_limit]
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
    if last_error is not None:
        raise last_error
    raise ValueError(f"Collection '{collection_id}' could not be collected.")


def normalize_playlist_items(items: list[dict[str, Any]]) -> list[Track]:
    """Convert source playlist items to a compact, account-free track model."""
    tracks: list[Track] = []
    for position, item in enumerate(items, start=1):
        raw_track = item.get("track")
        if not isinstance(raw_track, dict):
            continue
        if raw_track.get("type") not in {None, "track"} or raw_track.get("is_local") is True:
            continue

        track_id = _optional_string(raw_track.get("id"))
        uri = _optional_string(raw_track.get("uri"))
        title = _optional_string(raw_track.get("name"))
        duration_ms = _positive_int(raw_track.get("duration_ms"))
        if not track_id or not uri or not title or duration_ms is None:
            continue

        raw_artists = raw_track.get("artists")
        artists = (
            [
                Artist(
                    id=_optional_string(artist.get("id")),
                    name=str(artist.get("name") or "").strip(),
                )
                for artist in raw_artists
                if isinstance(artist, dict) and str(artist.get("name") or "").strip()
            ]
            if isinstance(raw_artists, list)
            else []
        )
        if not artists:
            continue

        raw_album = raw_track.get("album")
        if not isinstance(raw_album, dict):
            raw_album = {}
        album_name = str(raw_album.get("name") or "").strip() or title
        artwork_url = _first_image_url(raw_album.get("images"))

        album = Album(
            id=_optional_string(raw_album.get("id")),
            name=album_name,
            release_date=_optional_string(raw_album.get("release_date")),
            artwork_url=artwork_url,
            external_url=_nested_string(raw_album, "external_urls", "spotify"),
            album_type=_optional_string(raw_album.get("album_type")),
            total_tracks=_positive_int(raw_album.get("total_tracks")),
        )

        tracks.append(
            Track(
                position=position,
                id=track_id,
                uri=uri,
                title=title,
                artists=artists,
                album=album,
                duration_ms=duration_ms,
                explicit=bool(raw_track.get("explicit", False)),
                external_url=_nested_string(raw_track, "external_urls", "spotify"),
                artwork_url=artwork_url,
                isrc=_nested_string(raw_track, "external_ids", "isrc"),
                youtube_music=_safe_youtube_music_match(raw_track.get("youtube_music")),
            )
        )
    return tracks


def validate_catalog_dict(payload: Mapping[str, Any]) -> None:
    """Reject malformed or credential-bearing generated catalogs."""
    if payload.get("schemaVersion") != CATALOG_SCHEMA_VERSION:
        raise ValueError(f"Catalog schemaVersion must be {CATALOG_SCHEMA_VERSION}.")
    generated_at = payload.get("generatedAt")
    if not isinstance(generated_at, str) or not generated_at.endswith("Z"):
        raise ValueError("Catalog generatedAt must be a UTC ISO timestamp.")
    collections = payload.get("collections")
    if not isinstance(collections, list) or not collections:
        raise ValueError("Catalog must contain at least one collection.")

    _assert_safe_keys(payload)

    ids: set[str] = set()
    for collection in collections:
        if not isinstance(collection, dict):
            raise ValueError("Every catalog collection must be an object.")
        collection_id = collection.get("id")
        if not isinstance(collection_id, str) or not COLLECTION_ID_PATTERN.fullmatch(collection_id):
            raise ValueError("Catalog contains an invalid collection id.")
        if collection_id in ids:
            raise ValueError(f"Catalog collection id '{collection_id}' is duplicated.")
        ids.add(collection_id)
        tracks = collection.get("tracks")
        if not isinstance(tracks, list) or not tracks:
            raise ValueError(f"Catalog collection '{collection_id}' has no tracks.")
        for track in tracks:
            if not isinstance(track, dict):
                raise ValueError(f"Catalog collection '{collection_id}' has an invalid track.")
            if not isinstance(track.get("position"), int) or track["position"] <= 0:
                raise ValueError(f"Catalog collection '{collection_id}' has an invalid position.")
            if not str(track.get("id") or "").strip():
                raise ValueError(f"Catalog collection '{collection_id}' has a track without id.")
            if not str(track.get("title") or "").strip():
                raise ValueError(f"Catalog collection '{collection_id}' has a track without title.")
            raw_isrc = str(track.get("isrc") or "").strip()
            if raw_isrc and re.fullmatch(r"[A-Z]{2}[A-Z0-9]{3}[0-9]{7}", raw_isrc) is None:
                raise ValueError(f"Catalog collection '{collection_id}' has an invalid ISRC.")
            youtube_music = track.get("youtubeMusic")
            if youtube_music is not None and _safe_youtube_music_match(youtube_music) != youtube_music:
                raise ValueError(
                    f"Catalog collection '{collection_id}' has an invalid YouTube Music match."
                )


def write_catalog(catalog: Catalog, output_path: Path) -> None:
    """Validate and atomically write a generated catalog."""
    payload = catalog.to_dict()
    validate_catalog_dict(payload)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output_path.with_suffix(output_path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary.replace(output_path)


def validate_catalog_file(path: Path) -> None:
    """Validate an existing generated catalog file."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Unable to read catalog: {path}") from error
    if not isinstance(payload, dict):
        raise ValueError("Catalog root must be an object.")
    validate_catalog_dict(payload)


def _assert_safe_keys(value: Any) -> None:
    forbidden = {"sp_dc", "cookie", "cookies", "access_token", "authorization"}
    if isinstance(value, Mapping):
        for key, item in value.items():
            normalized = str(key).strip().lower()
            if normalized in forbidden:
                raise ValueError("Catalog contains credential-related material.")
            _assert_safe_keys(item)
    elif isinstance(value, list):
        for item in value:
            _assert_safe_keys(item)


def _safe_youtube_music_match(value: Any) -> dict[str, Any] | None:
    if not isinstance(value, Mapping):
        return None

    def safe_video_id(key: str) -> str | None:
        candidate = _optional_string(value.get(key))
        if candidate and re.fullmatch(r"[A-Za-z0-9_-]{11}", candidate):
            return candidate
        return None

    audio_video_id = safe_video_id("audioVideoId")
    official_video_id = safe_video_id("videoId")
    if audio_video_id is None and official_video_id is None:
        return None

    fallback_confidence = value.get("confidence")
    output: dict[str, Any] = {}
    confidence_values: list[int] = []
    for video_key, confidence_key, video_id in (
        ("audioVideoId", "audioConfidence", audio_video_id),
        ("videoId", "videoConfidence", official_video_id),
    ):
        if video_id is None:
            continue
        confidence = value.get(confidence_key, fallback_confidence)
        if not isinstance(confidence, int) or confidence not in range(0, 101):
            return None
        output[video_key] = video_id
        output[confidence_key] = confidence
        confidence_values.append(confidence)
    output["confidence"] = max(confidence_values)

    for source_key, public_key in (
        ("albumBrowseId", "albumBrowseId"),
        ("artistBrowseId", "artistBrowseId"),
    ):
        candidate = _optional_string(value.get(source_key))
        if candidate and len(candidate) <= 128 and re.fullmatch(r"[A-Za-z0-9_-]+", candidate):
            output[public_key] = candidate
    duration_ms = _positive_int(value.get("durationMs"))
    if duration_ms is not None:
        output["durationMs"] = duration_ms
    return output


def _clean_text(value: str) -> str:
    return " ".join(value.split())[:500]


def _first_image_url(value: Any) -> str | None:
    if not isinstance(value, list):
        return None
    for image in value:
        if isinstance(image, dict):
            url = _optional_string(image.get("url"))
            if url and url.startswith("https://"):
                return url
    return None


def _nested_string(value: Mapping[str, Any], *path: str) -> str | None:
    current: Any = value
    for key in path:
        if not isinstance(current, Mapping):
            return None
        current = current.get(key)
    return _optional_string(current)


def _nested_int(
    value: Mapping[str, Any],
    *path: str,
    default: int,
) -> int:
    current: Any = value
    for key in path:
        if not isinstance(current, Mapping):
            return default
        current = current.get(key)
    return current if isinstance(current, int) and current >= 0 else default


def _optional_string(value: Any) -> str | None:
    if not isinstance(value, str):
        return None
    normalized = value.strip()
    return normalized or None


def _positive_int(value: Any) -> int | None:
    return value if isinstance(value, int) and value > 0 else None
