from __future__ import annotations

from typing import Any

import pytest

from levyra_editorial.resilient import build_resilient_catalog
from levyra_editorial.spotify import SourceApiError


class PartialClient:
    """Return one valid chart and one unavailable chart."""

    def __init__(self, unavailable_playlist_ids: set[str]) -> None:
        self.unavailable_playlist_ids = unavailable_playlist_ids

    def get_playlist_metadata(self, playlist_id: str, market: str) -> dict[str, Any]:
        if playlist_id in self.unavailable_playlist_ids:
            raise SourceApiError("playlist content is unavailable")
        return {
            "id": playlist_id,
            "name": f"Top 50 {market}",
            "description": "Daily chart",
            "external_urls": {
                "spotify": f"https://open.spotify.com/playlist/{playlist_id}"
            },
            "images": [{"url": "https://image.example/chart.jpg"}],
            "tracks": {"total": 1},
        }

    def iter_playlist_items(self, playlist_id: str, market: str) -> list[dict[str, Any]]:
        if playlist_id in self.unavailable_playlist_ids:
            raise SourceApiError("playlist content is unavailable")
        return [
            {
                "track": {
                    "id": f"track{market.lower()}",
                    "uri": f"spotify:track:track{market.lower()}",
                    "type": "track",
                    "name": f"Chart song {market}",
                    "duration_ms": 180_000,
                    "explicit": False,
                    "external_ids": {},
                    "external_urls": {},
                    "artists": [{"id": "artist1", "name": "Chart Artist"}],
                    "album": {
                        "id": "album1",
                        "name": "Chart Album",
                        "images": [{"url": "https://image.example/album.jpg"}],
                        "external_urls": {},
                    },
                }
            }
        ]


def config(*, allow_partial: bool) -> dict[str, Any]:
    return {
        "allowPartial": allow_partial,
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
            },
        ],
    }


def test_partial_mode_keeps_available_countries() -> None:
    catalog = build_resilient_catalog(
        config(allow_partial=True),
        PartialClient({"playlistrussia12"}),
        generated_at="2026-07-29T18:00:00Z",
    )

    assert [collection.id for collection in catalog.collections] == ["top-50-it"]
    assert catalog.collections[0].tracks[0].title == "Chart song IT"


def test_strict_mode_still_fails_on_unavailable_country() -> None:
    with pytest.raises(SourceApiError, match="unavailable"):
        build_resilient_catalog(
            config(allow_partial=False),
            PartialClient({"playlistrussia12"}),
            generated_at="2026-07-29T18:00:00Z",
        )


def test_partial_mode_fails_when_every_country_is_unavailable() -> None:
    with pytest.raises(ValueError, match="No configured editorial collection"):
        build_resilient_catalog(
            config(allow_partial=True),
            PartialClient({"playlistitaly123", "playlistrussia12"}),
            generated_at="2026-07-29T18:00:00Z",
        )
