from __future__ import annotations

import pytest

from levyra_editorial.youtube_music import (
    YoutubeMusicError,
    YoutubeMusicWebClient,
    normalize_youtube_music_cookie,
    parse_search_candidates,
    score_candidate,
)


def test_cookie_parser_accepts_header_without_exposing_it() -> None:
    header, sapisid = normalize_youtube_music_cookie(
        "SAPISID=abcdefghijklmnopqrstuvwxyz123456; __Secure-3PAPISID=secondary"
    )
    assert "SAPISID=" in header
    assert sapisid == "abcdefghijklmnopqrstuvwxyz123456"


def test_cookie_parser_rejects_session_without_sapisid() -> None:
    with pytest.raises(YoutubeMusicError):
        normalize_youtube_music_cookie("SID=only-a-sid-cookie")


def test_parser_and_score_prefer_exact_studio_recording() -> None:
    payload = {
        "contents": {
            "musicResponsiveListItemRenderer": {
                "playlistItemData": {"videoId": "AbCdEf12345"},
                "flexColumns": [
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {"runs": [{"text": "Perfect Song"}]}
                        }
                    },
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {
                                "runs": [
                                    {
                                        "text": "Exact Artist",
                                        "navigationEndpoint": {
                                            "browseEndpoint": {
                                                "browseId": "UC123456789",
                                                "browseEndpointContextSupportedConfigs": {
                                                    "browseEndpointContextMusicConfig": {
                                                        "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                                    }
                                                },
                                            }
                                        },
                                    },
                                    {"text": " • "},
                                    {
                                        "text": "Perfect Album",
                                        "navigationEndpoint": {
                                            "browseEndpoint": {
                                                "browseId": "MPREb_123456",
                                                "browseEndpointContextSupportedConfigs": {
                                                    "browseEndpointContextMusicConfig": {
                                                        "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                                                    }
                                                },
                                            }
                                        },
                                    },
                                ]
                            }
                        }
                    },
                ],
                "fixedColumns": [
                    {
                        "musicResponsiveListItemFixedColumnRenderer": {
                            "text": {"runs": [{"text": "3:30"}]}
                        }
                    }
                ],
            }
        }
    }
    candidate = parse_search_candidates(payload)[0]
    assert candidate["videoId"] == "AbCdEf12345"
    assert candidate["albumBrowseId"] == "MPREb_123456"
    assert score_candidate("Perfect Song", "Exact Artist", 210_000, candidate) >= 90


class BootstrapResponse:
    text = "<html>missing innertube configuration</html>"

    def raise_for_status(self) -> None:
        return None


class BootstrapSession:
    def __init__(self) -> None:
        self.headers: dict[str, str] = {}
        self.get_calls = 0

    def get(self, *_args: object, **_kwargs: object) -> BootstrapResponse:
        self.get_calls += 1
        return BootstrapResponse()

    def close(self) -> None:
        return None


def test_bootstrap_failure_is_cached_after_first_attempt() -> None:
    session = BootstrapSession()
    client = YoutubeMusicWebClient(
        "SAPISID=abcdefghijklmnopqrstuvwxyz123456",
        session=session,
    )

    with pytest.raises(YoutubeMusicError, match="bootstrap"):
        client._bootstrap()
    with pytest.raises(YoutubeMusicError, match="previously failed"):
        client._bootstrap()

    assert session.get_calls == 1
