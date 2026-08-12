from __future__ import annotations

from levyra_editorial.youtube_music import (
    YOUTUBE_MUSIC_SONG_SEARCH_PARAMS,
    YoutubeMusicWebClient,
    select_youtube_music_mapping,
)


def _art_track(video_id: str, album: str, duration_ms: int = 224_000) -> dict:
    return {
        "videoId": video_id,
        "title": "Dai Dai",
        "artist": "Shakira, Burna Boy",
        "album": album,
        "durationMs": duration_ms,
        "musicVideoType": "MUSIC_VIDEO_TYPE_ATV",
    }


def _renderer(video_id: str, album: str) -> dict:
    return {
        "musicResponsiveListItemRenderer": {
            "playlistItemData": {"videoId": video_id},
            "flexColumns": [
                {
                    "musicResponsiveListItemFlexColumnRenderer": {
                        "text": {"runs": [{"text": "Dai Dai"}]}
                    }
                },
                {
                    "musicResponsiveListItemFlexColumnRenderer": {
                        "text": {
                            "runs": [
                                {
                                    "text": "Shakira",
                                    "navigationEndpoint": {
                                        "browseEndpoint": {"browseId": "UCShakira001"}
                                    },
                                },
                                {"text": ", "},
                                {
                                    "text": "Burna Boy",
                                    "navigationEndpoint": {
                                        "browseEndpoint": {"browseId": "UCBurnaBoy01"}
                                    },
                                },
                                {"text": " • "},
                                {
                                    "text": album,
                                    "navigationEndpoint": {
                                        "browseEndpoint": {"browseId": "MPREb_album01"}
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
                        "text": {"runs": [{"text": "3:44"}]}
                    }
                }
            ],
            "overlay": {
                "musicItemThumbnailOverlayRenderer": {
                    "content": {
                        "musicPlayButtonRenderer": {
                            "playNavigationEndpoint": {
                                "watchEndpoint": {
                                    "videoId": video_id,
                                    "watchEndpointMusicSupportedConfigs": {
                                        "watchEndpointMusicConfig": {
                                            "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                                        }
                                    },
                                }
                            }
                        }
                    }
                }
            },
        }
    }


def test_mapping_accepts_equivalent_art_track_publications() -> None:
    mapping = select_youtube_music_mapping(
        "Dai Dai",
        "Shakira, Burna Boy",
        223_448,
        [
            _art_track("lFQdcPTTzSg", "Dai Dai"),
            _art_track("2uT4_w0M_4o", "Official FIFA World Cup 2026 Album (Bonus Edition)"),
        ],
    )

    assert mapping is not None
    assert mapping["audioVideoId"] == "lFQdcPTTzSg"


def test_mapping_keeps_abstaining_when_near_tied_art_tracks_differ_in_duration() -> None:
    mapping = select_youtube_music_mapping(
        "Dai Dai",
        "Shakira, Burna Boy",
        223_448,
        [
            _art_track("lFQdcPTTzSg", "Dai Dai", duration_ms=224_000),
            _art_track("2uT4_w0M_4o", "Another Release", duration_ms=229_000),
        ],
    )

    assert mapping is None


def test_resolve_uses_song_filter_and_keeps_first_equivalent_publication(monkeypatch) -> None:
    client = YoutubeMusicWebClient("SAPISID=abcdefghijklmnopqrstuvwxyz123456", workers=1)
    searches: list[tuple[str, str | None]] = []

    def fake_search(query: str, params: str | None = None):
        searches.append((query, params))
        return {
            "contents": [
                _renderer("lFQdcPTTzSg", "Dai Dai"),
                _renderer("2uT4_w0M_4o", "Official FIFA World Cup 2026 Album (Bonus Edition)"),
            ]
        }

    monkeypatch.setattr(client, "_search", fake_search)
    monkeypatch.setattr(
        client,
        "_resolve_official_video",
        lambda title, artist, duration_ms: {
            "videoId": "fcnDmrtj6Sk",
            "videoConfidence": 99,
        },
    )

    try:
        result = client.resolve("Dai Dai", "Shakira, Burna Boy", 223_448)
    finally:
        client.close()

    assert searches == [
        ("Dai Dai Shakira, Burna Boy", YOUTUBE_MUSIC_SONG_SEARCH_PARAMS)
    ]
    assert result is not None
    assert result["audioVideoId"] == "lFQdcPTTzSg"
    assert result["videoId"] == "fcnDmrtj6Sk"
