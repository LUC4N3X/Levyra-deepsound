from __future__ import annotations

import pytest

from levyra_editorial.youtube_music import (
    YoutubeMusicError,
    YoutubeMusicWebClient,
    normalize_youtube_music_cookie,
    parse_search_candidates,
    score_candidate,
    select_youtube_music_mapping,
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



def test_parser_finds_video_id_inside_play_button_overlay() -> None:
    payload = {
        "contents": {
            "musicResponsiveListItemRenderer": {
                "flexColumns": [
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {"runs": [{"text": "Nested Song"}]}
                        }
                    },
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {"runs": [{"text": "Nested Artist"}]}
                        }
                    },
                ],
                "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                        "content": {
                            "musicPlayButtonRenderer": {
                                "playNavigationEndpoint": {
                                    "watchEndpoint": {"videoId": "ZyXwVu98765"}
                                }
                            }
                        }
                    }
                },
            }
        }
    }

    candidates = parse_search_candidates(payload)

    assert len(candidates) == 1
    assert candidates[0]["videoId"] == "ZyXwVu98765"



def test_mapping_separates_art_track_from_official_music_video() -> None:
    candidates = [
        {
            "videoId": "Audio123456",
            "title": "Dai Dai",
            "artist": "Shakira, Burna Boy",
            "album": "Dai Dai",
            "durationMs": 223_448,
            "musicVideoType": "MUSIC_VIDEO_TYPE_ATV",
        },
        {
            "videoId": "fcnDmrtj6Sk",
            "title": "Dai Dai (Official Video)",
            "artist": "Shakira, Burna Boy",
            "album": "",
            "durationMs": 226_000,
            "musicVideoType": "MUSIC_VIDEO_TYPE_OMV",
        },
        {
            "videoId": "Wrong123456",
            "title": "Dai Dai",
            "artist": "Shakira, Burna Boy",
            "album": "",
            "durationMs": 223_000,
            "musicVideoType": "MUSIC_VIDEO_TYPE_UGC",
        },
    ]

    mapping = select_youtube_music_mapping("Dai Dai", "Shakira, Burna Boy", 223_448, candidates)

    assert mapping is not None
    assert mapping["audioVideoId"] == "Audio123456"
    assert mapping["videoId"] == "fcnDmrtj6Sk"
    assert mapping["videoId"] != "Wrong123456"


def test_mapping_never_promotes_an_art_track_to_official_video() -> None:
    mapping = select_youtube_music_mapping(
        "Exact Song",
        "Exact Artist",
        180_000,
        [
            {
                "videoId": "OnlyAudio12",
                "title": "Exact Song",
                "artist": "Exact Artist",
                "album": "Exact Song",
                "durationMs": 180_000,
                "musicVideoType": "MUSIC_VIDEO_TYPE_ATV",
            }
        ],
    )

    assert mapping is not None
    assert mapping["audioVideoId"] == "OnlyAudio12"
    assert "videoId" not in mapping


def test_parser_keeps_video_type_attached_to_the_same_video_id() -> None:
    payload = {
        "contents": {
            "musicResponsiveListItemRenderer": {
                "playlistItemData": {"videoId": "Official123"},
                "flexColumns": [
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {"runs": [{"text": "Official Song"}]}
                        }
                    },
                    {
                        "musicResponsiveListItemFlexColumnRenderer": {
                            "text": {"runs": [{"text": "Official Artist"}]}
                        }
                    },
                ],
                "overlay": {
                    "musicPlayButtonRenderer": {
                        "playNavigationEndpoint": {
                            "watchEndpoint": {
                                "videoId": "Official123",
                                "watchEndpointMusicSupportedConfigs": {
                                    "watchEndpointMusicConfig": {
                                        "musicVideoType": "MUSIC_VIDEO_TYPE_OMV"
                                    }
                                },
                            }
                        }
                    }
                },
            }
        }
    }

    candidate = parse_search_candidates(payload)[0]

    assert candidate["videoId"] == "Official123"
    assert candidate["musicVideoType"] == "MUSIC_VIDEO_TYPE_OMV"


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
