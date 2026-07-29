from __future__ import annotations

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
