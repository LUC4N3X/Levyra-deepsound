from __future__ import annotations

import hashlib
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any, Iterable, Mapping
from urllib.parse import urlparse


PUBLIC_ARTWORK_HOSTS = frozenset(
    {
        "i.scdn.co",
        "image-cdn-ak.spotifycdn.com",
    }
)
PUBLIC_ARTWORK_HOST_SUFFIX = ".scdn.co"


def generated_timestamp() -> str:
    return datetime.now(tz=UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def stable_id(*parts: str) -> str:
    normalized = "\x1f".join(part.strip().casefold() for part in parts)
    digest = hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:24]
    return f"levyra-{digest}"


def _public_artwork_url(value: str | None) -> str | None:
    """Keep only HTTPS cover artwork hosted by the source's public image CDN.

    Album covers are the one source-hosted asset the catalog publishes: the chart rows need a real
    cover for every entry, and on-device lookups cannot match every track. Everything else about the
    source (page URLs, ids and credentials) stays out. ISRC is a public recording identity. The
    allowlist keeps a tampered or unexpected payload from pointing the app's image loader at an
    arbitrary host.
    """
    normalized = str(value or "").strip()
    if not normalized:
        return None
    parsed = urlparse(normalized)
    host = (parsed.hostname or "").lower()
    if parsed.scheme != "https" or parsed.username or parsed.password or parsed.port not in (None, 443):
        return None
    if host not in PUBLIC_ARTWORK_HOSTS and not host.endswith(PUBLIC_ARTWORK_HOST_SUFFIX):
        return None
    return normalized


@dataclass(frozen=True)
class Artist:
    name: str
    external_url: str | None


@dataclass(frozen=True)
class Album:
    name: str
    release_date: str | None
    artwork_url: str | None
    external_url: str | None
    album_type: str | None = None
    total_tracks: int | None = None


@dataclass(frozen=True)
class Track:
    source_id: str
    source_uri: str
    external_url: str
    name: str
    duration_ms: int
    explicit: bool
    artists: tuple[Artist, ...]
    album: Album
    artwork_url: str | None
    isrc: str | None = None

    def to_dict(self, rank: int) -> dict[str, Any]:
        artist_names = [artist.name for artist in self.artists]
        return {
            "rank": rank,
            "id": stable_id(self.name, "|".join(artist_names), self.album.name),
            "title": self.name,
            "artists": artist_names,
            "album": {
                "name": self.album.name,
                "releaseDate": self.album.release_date,
                "type": self.album.album_type,
                "totalTracks": self.album.total_tracks,
            },
            "durationMs": self.duration_ms,
            "explicit": self.explicit,
            "isrc": self.isrc,
            "artworkUrl": _public_artwork_url(self.artwork_url or self.album.artwork_url),
        }


@dataclass(frozen=True)
class Collection:
    id: str
    name: str
    market: str
    playlist_id: str
    optional: bool
    tracks: tuple[Track, ...]

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "market": self.market,
            "optional": self.optional,
            "tracks": [track.to_dict(rank=index) for index, track in enumerate(self.tracks, start=1)],
        }


@dataclass(frozen=True)
class Catalog:
    schema: int
    generated_at: str
    collections: tuple[Collection, ...]

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema": self.schema,
            "generatedAt": self.generated_at,
            "collections": [collection.to_dict() for collection in self.collections],
        }


def catalog_from_dict(payload: Mapping[str, Any]) -> Catalog:
    collections: list[Collection] = []
    for raw_collection in payload.get("collections", []):
        tracks: list[Track] = []
        for raw_track in raw_collection.get("tracks", []):
            raw_album = raw_track.get("album") or {}
            artists = tuple(
                Artist(name=str(name), external_url=None) for name in raw_track.get("artists", [])
            )
            tracks.append(
                Track(
                    source_id=str(raw_track.get("id") or ""),
                    source_uri="",
                    external_url="",
                    name=str(raw_track.get("title") or ""),
                    duration_ms=int(raw_track.get("durationMs") or 0),
                    explicit=bool(raw_track.get("explicit", False)),
                    artists=artists,
                    album=Album(
                        name=str(raw_album.get("name") or ""),
                        release_date=_optional_string(raw_album.get("releaseDate")),
                        artwork_url=_optional_string(raw_track.get("artworkUrl")),
                        external_url=None,
                        album_type=_optional_string(raw_album.get("type")),
                        total_tracks=_positive_int(raw_album.get("totalTracks")),
                    ),
                    artwork_url=_optional_string(raw_track.get("artworkUrl")),
                    isrc=_optional_string(raw_track.get("isrc")),
                )
            )
        collections.append(
            Collection(
                id=str(raw_collection.get("id") or ""),
                name=str(raw_collection.get("name") or ""),
                market=str(raw_collection.get("market") or ""),
                playlist_id="",
                optional=bool(raw_collection.get("optional", False)),
                tracks=tuple(tracks),
            )
        )
    return Catalog(
        schema=int(payload.get("schema") or 1),
        generated_at=str(payload.get("generatedAt") or ""),
        collections=tuple(collections),
    )


def _optional_string(value: object) -> str | None:
    normalized = str(value or "").strip()
    return normalized or None


def _positive_int(value: object) -> int | None:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        return None
    return parsed if parsed > 0 else None


def flatten_tracks(collections: Iterable[Collection]) -> tuple[Track, ...]:
    return tuple(track for collection in collections for track in collection.tracks)
