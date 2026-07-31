from __future__ import annotations

import pytest

from levyra_editorial.youtube_music import (
    YoutubeMusicError,
    YoutubeMusicWebClient,
    combine_verified_youtube_mapping,
    normalize_youtube_music_cookie,
    parse_search_candidates,
    parse_youtube_web_candidates,
    score_candidate,
    select_official_youtube_video,
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



def test_ytm_mapping_keeps_art_track_and_never_promotes_omv() -> None:
    candidates = [
        {
            "videoId": "Audio123456",
            "title": "Dai Dai",
            "artist": "Shakira, Burna Boy",
            "album": "Dai Dai",
            "durationMs": None,
            "musicVideoType": "MUSIC_VIDEO_TYPE_ATV",
        },
        {
            "videoId": "Wrong123456",
            "title": "Dai Dai",
            "artist": "Shakira, Burna Boy",
            "album": "",
            "durationMs": None,
            "musicVideoType": "MUSIC_VIDEO_TYPE_OMV",
        },
    ]

    mapping = select_youtube_music_mapping("Dai Dai", "Shakira, Burna Boy", 223_448, candidates)

    assert mapping is not None
    assert mapping["audioVideoId"] == "Audio123456"
    assert "videoId" not in mapping


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



def test_web_parser_marks_only_official_artist_channel_badge() -> None:
    payload = {
        "contents": {
            "videoRenderer": {
                "videoId": "fcnDmrtj6Sk",
                "title": {"runs": [{"text": "Shakira, Burna Boy - Dai Dai (Official Video)"}]},
                "ownerText": {"runs": [{"text": "Shakira and 2 more"}]},
                "lengthText": {"simpleText": "4:01"},
                "ownerBadges": [
                    {
                        "metadataBadgeRenderer": {
                            "style": "BADGE_STYLE_TYPE_VERIFIED_ARTIST",
                            "tooltip": "Official Artist Channel",
                        }
                    }
                ],
            }
        }
    }

    candidate = parse_youtube_web_candidates(payload)[0]

    assert candidate["videoId"] == "fcnDmrtj6Sk"
    assert candidate["verifiedArtist"] is True
    assert candidate["durationMs"] == 241_000


def test_web_selector_rejects_fake_official_titles_and_picks_oac() -> None:
    candidates = [
        {
            "videoId": "fcnDmrtj6Sk",
            "title": "Shakira, Burna Boy - Dai Dai (Official Video)",
            "owner": "Shakira and 2 more",
            "channelId": "",
            "durationMs": 241_000,
            "verifiedArtist": True,
        },
        {
            "videoId": "Ni6F5qdCpEY",
            "title": "Shakira & Burna Boy – Dai Dai (Official Music Video)",
            "owner": "Ayan.zehen.official",
            "channelId": "UCSe2JMN9viN7XGgfDTJ3a9w",
            "durationMs": 300_000,
            "verifiedArtist": False,
        },
        {
            "videoId": "NWU1m16yzAY",
            "title": "Dai dai",
            "owner": "Shakira",
            "channelId": "",
            "durationMs": 223_000,
            "verifiedArtist": True,
        },
        {
            "videoId": "X9CsK_nuqdE",
            "title": "Shakira, Burna Boy - Dai Dai (Official Audio)",
            "owner": "Shakira",
            "channelId": "UCYLNGLIzMhRTi6ZOLjAPSmw",
            "durationMs": 225_000,
            "verifiedArtist": True,
        },
    ]

    mapping = select_official_youtube_video(
        "Dai Dai",
        "Shakira, Burna Boy",
        223_448,
        candidates,
    )

    assert mapping == {"videoId": "fcnDmrtj6Sk", "videoConfidence": 99}


def test_web_selector_abstains_without_official_artist_channel() -> None:
    mapping = select_official_youtube_video(
        "Exact Song",
        "Exact Artist",
        180_000,
        [
            {
                "videoId": "Fake1234567",
                "title": "Exact Artist - Exact Song (Official Video)",
                "owner": "Fan Uploads",
                "channelId": "UCFake123456",
                "durationMs": 180_000,
                "verifiedArtist": False,
            }
        ],
    )

    assert mapping is None


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



def test_combined_mapping_requires_verified_audio_identity() -> None:
    official = {"videoId": "fcnDmrtj6Sk", "videoConfidence": 99}

    assert combine_verified_youtube_mapping(None, official) is None
    assert combine_verified_youtube_mapping(
        {"audioVideoId": "invalid", "audioConfidence": 99},
        official,
    ) is None

    mapping = combine_verified_youtube_mapping(
        {"audioVideoId": "lFQdcPTTzSg", "audioConfidence": 99},
        official,
    )
    assert mapping is not None
    assert mapping["audioVideoId"] == "lFQdcPTTzSg"
    assert mapping["videoId"] == "fcnDmrtj6Sk"
    assert mapping["confidence"] == 99


def test_resolve_skips_web_video_query_when_audio_identity_is_missing(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = YoutubeMusicWebClient("SAPISID=abcdefghijklmnopqrstuvwxyz123456")
    monkeypatch.setattr(client, "_search", lambda _query: {})
    calls = 0

    def official_video(_title: str, _artist: str, _duration_ms: int) -> dict[str, object]:
        nonlocal calls
        calls += 1
        return {"videoId": "fcnDmrtj6Sk", "videoConfidence": 99}

    monkeypatch.setattr(client, "_resolve_official_video", official_video)
    try:
        assert client.resolve("Missing", "Artist", 180_000) is None
        assert calls == 0
    finally:
        client.close()
