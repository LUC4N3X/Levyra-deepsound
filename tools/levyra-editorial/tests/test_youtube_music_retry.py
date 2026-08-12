from __future__ import annotations

from levyra_editorial.youtube_music import YoutubeMusicWebClient


def _dai_dai_art_track_payload() -> dict:
    artist_endpoint = {
        "browseEndpoint": {
            "browseId": "UCo6JijJGA3IvIiPsawDK3Ww",
            "browseEndpointContextSupportedConfigs": {
                "browseEndpointContextMusicConfig": {
                    "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                }
            },
        }
    }
    return {
        "contents": {
            "musicResponsiveListItemRenderer": {
                "playlistItemData": {"videoId": "lFQdcPTTzSg"},
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
                                        "navigationEndpoint": artist_endpoint,
                                    },
                                    {"text": ", "},
                                    {
                                        "text": "Burna Boy",
                                        "navigationEndpoint": artist_endpoint,
                                    },
                                ]
                            }
                        }
                    },
                ],
                "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                        "content": {
                            "musicPlayButtonRenderer": {
                                "playNavigationEndpoint": {
                                    "watchEndpoint": {
                                        "videoId": "lFQdcPTTzSg",
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
    }


def test_resolve_retries_punctuation_free_audio_query_before_official_video(monkeypatch) -> None:
    client = YoutubeMusicWebClient("SAPISID=abcdefghijklmnopqrstuvwxyz123456", workers=1)
    queries: list[str] = []

    def fake_search(query: str):
        queries.append(query)
        return {} if len(queries) == 1 else _dai_dai_art_track_payload()

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

    assert queries == ["Dai Dai Shakira, Burna Boy", "Dai Dai Shakira Burna Boy"]
    assert result is not None
    assert result["audioVideoId"] == "lFQdcPTTzSg"
    assert result["videoId"] == "fcnDmrtj6Sk"
