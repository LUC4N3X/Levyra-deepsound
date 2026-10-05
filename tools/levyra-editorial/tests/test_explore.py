from __future__ import annotations

import json

import pytest

import levyra_editorial.explore as explore_module
from levyra_editorial.collector import validate_catalog_dict
from levyra_editorial.explore import (
    ExploreSeed,
    collect_spotify_explore_collections,
    parse_youtube_explore_categories,
)


def _button(title: str, params: str, *, navigation: bool = False) -> dict:
    endpoint_key = "navigationEndpoint" if navigation else "clickCommand"
    return {
        "musicNavigationButtonRenderer": {
            "buttonText": {"runs": [{"text": title}]},
            endpoint_key: {"browseEndpoint": {"params": params}},
        }
    }


def test_youtube_taxonomy_preserves_sections_order_and_opaque_params() -> None:
    payload = {
        "contents": {
            "gridRenderer": {
                "header": {"gridHeaderRenderer": {"title": {"runs": [{"text": "Moods & moments"}]}}},
                "items": [
                    _button("Focus", "ggM8SgQIBxAB/+=_"),
                    _button("Workout", "workoutParams", navigation=True),
                ],
            }
        },
        "continuation": {
            "gridRenderer": {
                "header": {"gridHeaderRenderer": {"title": {"runs": [{"text": "Genres"}]}}},
                "items": [
                    _button("Dance & Electronic", "danceParams"),
                    _button("Focus duplicate", "ggM8SgQIBxAB/+=_"),
                    _button("Unsafe", "bad?token"),
                ],
            }
        },
    }

    categories = parse_youtube_explore_categories(payload)

    assert [(item.title, item.section, item.section_index, item.kind) for item in categories] == [
        ("Focus", "Moods & moments", 0, "mood"),
        ("Workout", "Moods & moments", 0, "mood"),
        ("Dance & Electronic", "Genres", 1, "genre"),
    ]
    assert categories[0].params == "ggM8SgQIBxAB/+=_"


class FakeSpotify:
    def __init__(self) -> None:
        self.queries: list[str] = []

    def resolve_playlist_id(self, query: str, market: str, title_hints: list[str]) -> str:
        self.queries.append(query)
        assert market == "US"
        assert title_hints == [query]
        return "37i9dQZF1DWZtZ8vUCzche"

    def get_playlist_metadata(self, playlist_id: str) -> dict:
        return {
            "name": "Spotify Editorial",
            "description": "Official Spotify selection",
            "images": [{"url": "https://i.scdn.co/image/category-cover"}],
            "snapshot_id": "private-snapshot",
            "tracks": {"total": 123},
            "external_urls": {"spotify": f"https://open.spotify.com/playlist/{playlist_id}"},
        }

    def iter_playlist_items(self, playlist_id: str, limit: int | None = None) -> list[dict]:
        assert limit == 16
        return [
            {
                "track": {
                    "id": "spotifyTrack123",
                    "uri": "spotify:track:spotifyTrack123",
                    "type": "track",
                    "name": "Midnight Drive",
                    "duration_ms": 180_000,
                    "explicit": False,
                    "external_ids": {"isrc": "ITABC2600001"},
                    "external_urls": {"spotify": "https://open.spotify.com/track/spotifyTrack123"},
                    "artists": [{"id": "artistPrivate", "name": "Test Artist"}],
                    "album": {
                        "id": "albumPrivate",
                        "name": "Real Album",
                        "release_date": "2026-01-02",
                        "images": [{"url": "https://i.scdn.co/image/real-album"}],
                        "external_urls": {"spotify": "https://open.spotify.com/album/albumPrivate"},
                    },
                }
            }
        ]

    def enrich_track_metadata(self, items: list[dict]) -> list[dict]:
        return items


def test_spotify_explore_collection_is_sanitized_and_keyed_by_youtube_params(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    seed = ExploreSeed("Focus", "opaque/+=_", "Moods & moments", 0)
    monkeypatch.setattr(explore_module, "discover_youtube_explore_categories", lambda: [seed])

    collections = collect_spotify_explore_collections(FakeSpotify(), None)
    assert len(collections) == 1

    public = collections[0].to_dict()
    assert public["kind"] == "mood"
    assert public["youtubeParams"] == "opaque/+=_"
    assert public["sectionIndex"] == 0
    assert public["artworkUrl"] == "https://i.scdn.co/image/category-cover"
    assert public["totalSourceItems"] == 123
    assert public["tracks"][0]["album"]["name"] == "Real Album"
    assert public["tracks"][0]["artworkUrl"] == "https://i.scdn.co/image/real-album"

    serialized = json.dumps(public).lower()
    assert "open.spotify.com" not in serialized
    assert "spotify:track" not in serialized
    assert "privatesnapshot" not in serialized
    assert "artistprivate" not in serialized
    assert "albumprivate" not in serialized


def test_catalog_validation_accepts_opaque_explore_params_and_rejects_bad_values() -> None:
    payload = {
        "schemaVersion": 1,
        "generatedAt": "2026-10-05T08:00:00Z",
        "collections": [
            {
                "id": "mood-focus",
                "kind": "mood",
                "market": "GLOBAL",
                "title": "Focus",
                "youtubeParams": "ggM8SgQIBxAB/+=_",
                "sectionIndex": 0,
                "tracks": [{"position": 1, "id": "levyra-a", "title": "Song"}],
            }
        ],
    }
    validate_catalog_dict(payload)

    payload["collections"][0]["youtubeParams"] = "bad?token"
    with pytest.raises(ValueError, match="invalid YouTube browse params"):
        validate_catalog_dict(payload)
