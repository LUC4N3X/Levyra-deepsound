from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


def _compact(value: Any) -> Any:
    """Recursively remove ``None`` values while preserving empty lists and strings."""
    if isinstance(value, dict):
        return {key: _compact(item) for key, item in value.items() if item is not None}
    if isinstance(value, list):
        return [_compact(item) for item in value]
    return value


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
    isrc: str | None
    external_url: str | None
    artwork_url: str | None

    def to_dict(self) -> dict[str, Any]:
        return _compact(
            {
                "position": self.position,
                "id": self.id,
                "uri": self.uri,
                "title": self.title,
                "artists": [asdict(artist) for artist in self.artists],
                "album": {
                    "id": self.album.id,
                    "name": self.album.name,
                    "releaseDate": self.album.release_date,
                    "artworkUrl": self.album.artwork_url,
                    "externalUrl": self.album.external_url,
                },
                "durationMs": self.duration_ms,
                "explicit": self.explicit,
                "isrc": self.isrc,
                "externalUrl": self.external_url,
                "artworkUrl": self.artwork_url,
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
                "sourceId": self.source_id,
                "sourceUrl": self.source_url,
                "artworkUrl": self.artwork_url,
                "snapshotId": self.snapshot_id,
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
