from __future__ import annotations

import hashlib
from dataclasses import dataclass
from typing import Any
from urllib.parse import urlparse


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
    if parsed.scheme != "https" or parsed.username or parsed.password or parsed.port:
        return None
    if host == "i.scdn.co" or host.endswith(".scdn.co") or host == "image-cdn-ak.spotifycdn.com":
        return normalized
    return None


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
    album_type: str | None = None
    total_tracks: int | None = None


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
    isrc: str | None = None
    youtube_music: dict[str, Any] | None = None

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
                    "type": self.album.album_type,
                    "totalTracks": self.album.total_tracks,
                },
                "durationMs": self.duration_ms,
                "explicit": self.explicit,
                "isrc": self.isrc,
                "youtubeMusic": self.youtube_music,
                "artworkUrl": _public_artwork_url(self.artwork_url),
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
