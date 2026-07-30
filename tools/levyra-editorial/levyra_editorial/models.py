from __future__ import annotations

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
