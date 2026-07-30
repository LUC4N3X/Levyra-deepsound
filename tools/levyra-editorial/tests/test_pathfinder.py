from __future__ import annotations

from typing import Any

import pytest

from levyra_editorial.spotify import (
    API_BASE_URL,
    PATHFINDER_URL,
    SourceApiError,
    SpotifyWebClient,
)


class FakeResponse:
    def __init__(
        self,
        payload: object,
        *,
        status_code: int = 200,
        headers: dict[str, str] | None = None,
    ) -> None:
        self._payload = payload
        self.status_code = status_code
        self.headers = headers or {}

    def json(self) -> object:
        return self._payload


class PathfinderSession:
    def __init__(self, response: FakeResponse) -> None:
        self.response = response
        self.calls: list[tuple[str, dict[str, Any]]] = []

    def get(self, url: str, **kwargs: Any) -> FakeResponse:
        self.calls.append((url, kwargs))
        return self.response

    def close(self) -> None:
        return None


def playlist_payload() -> dict[str, Any]:
    return {
        "data": {
            "playlistV2": {
                "uri": "spotify:playlist:playlist12345",
                "name": "Top 50 Italia",
                "description": "Daily chart",
                "images": {
                    "items": [
                        {
                            "sources": [
                                {
                                    "url": "https://image.example/chart.jpg",
                                    "width": 640,
                                    "height": 640,
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "totalCount": 1,
                    "items": [
                        {
                            "itemV2": {
                                "data": {
                                    "__typename": "Track",
                                    "uri": "spotify:track:track12345",
                                    "name": "Chart song",
                                    "trackDuration": {"totalMilliseconds": 181_000},
                                    "contentRating": {"label": "EXPLICIT"},
                                    "artists": {
                                        "items": [
                                            {
                                                "uri": "spotify:artist:artist12345",
                                                "profile": {"name": "Chart Artist"},
                                            }
                                        ]
                                    },
                                    "albumOfTrack": {
                                        "uri": "spotify:album:album12345",
                                        "name": "Chart Album",
                                        "coverArt": {
                                            "sources": [
                                                {
                                                    "url": "https://image.example/album.jpg",
                                                    "width": 640,
                                                    "height": 640,
                                                }
                                            ]
                                        },
                                    },
                                }
                            },
                            "itemV3": {
                                "data": {
                                    "identityTrait": {
                                        "contentHierarchyParent": {
                                            "publishingMetadataTrait": {
                                                "firstPublishedAt": {
                                                    "isoString": "2026-07-01"
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        }
                    ],
                },
            }
        }
    }


def authenticated_client(session: PathfinderSession) -> SpotifyWebClient:
    client = SpotifyWebClient("A" * 40, session=session)
    client._access_token = "temporary-token"
    client._client_id = "web-client"
    return client


def test_pathfinder_collects_metadata_and_tracks_without_developer_api() -> None:
    session = PathfinderSession(FakeResponse(playlist_payload()))
    client = authenticated_client(session)

    metadata = client.get_playlist_metadata("playlist12345")
    items = client.iter_playlist_items("playlist12345")

    assert metadata["name"] == "Top 50 Italia"
    assert metadata["tracks"]["total"] == 1
    assert metadata["images"][0]["url"] == "https://image.example/chart.jpg"
    assert items[0]["track"]["id"] == "track12345"
    assert items[0]["track"]["artists"][0]["name"] == "Chart Artist"
    assert items[0]["track"]["album"]["release_date"] == "2026-07-01"
    assert items[0]["track"]["explicit"] is True
    assert [url for url, _ in session.calls] == [PATHFINDER_URL]
    assert all(not url.startswith(API_BASE_URL) for url, _ in session.calls)

    _, kwargs = session.calls[0]
    assert kwargs["params"]["operationName"] == "fetchPlaylist"
    assert kwargs["headers"]["Authorization"] == "Bearer temporary-token"
    assert kwargs["headers"]["App-Platform"] == "WebPlayer"


def test_pathfinder_reports_rotated_persisted_query_hash() -> None:
    session = PathfinderSession(
        FakeResponse({"errors": [{"message": "PersistedQueryNotFound"}]})
    )
    client = authenticated_client(session)

    with pytest.raises(SourceApiError, match="rotated"):
        client.get_playlist_metadata("playlist12345")


def test_pathfinder_rejects_missing_playlist_union() -> None:
    session = PathfinderSession(FakeResponse({"data": {"playlistV2": None}}))
    client = authenticated_client(session)

    with pytest.raises(SourceApiError, match="playlistV2"):
        client.get_playlist_metadata("playlist12345")
