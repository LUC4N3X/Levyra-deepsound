from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor in {path}, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def append_once(path: str, marker: str, addition: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if addition.strip() in text:
        return
    count = text.count(marker)
    if count != 1:
        raise RuntimeError(f"{label}: expected one marker in {path}, found {count}")
    target.write_text(text.replace(marker, marker + addition, 1), encoding="utf-8")


# The top-level candidate builder needs the provider id for verified candidates.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasProvider.kt",
    "    private companion object {\n",
    "    companion object {\n",
    "community provider id visibility",
)

# Keep the existing SSRF allowlist model and add only the two curated canvas media hosts.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/feature/motion/MotionArtworkUrlVerifier.kt",
    '''            "tidal-video-cover" -> host == TIDAL_MEDIA_HOST
            else -> false
''',
    '''            "tidal-video-cover" -> host == TIDAL_MEDIA_HOST
            "community-canvas" -> host in COMMUNITY_MEDIA_HOSTS
            else -> false
''',
    "community canvas destination allowlist",
)

replace_once(
    "app/src/test/java/com/luc4n3x/levyra/feature/motion/MotionArtworkDestinationPolicyTest.kt",
    '''        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "apple-motion",
                "https://video-ssl.itunes.apple.com/itunes-assets/master.m3u8".toHttpUrl()
            )
        )
''',
    '''        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "apple-motion",
                "https://video-ssl.itunes.apple.com/itunes-assets/master.m3u8".toHttpUrl()
            )
        )
        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "community-canvas",
                "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4".toHttpUrl()
            )
        )
''',
    "community canvas allowed host regression",
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/feature/motion/MotionArtworkDestinationPolicyTest.kt",
    '''        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "tidal-video-cover",
                "https://example.com/video.mp4".toHttpUrl()
            )
        )
''',
    '''        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "tidal-video-cover",
                "https://example.com/video.mp4".toHttpUrl()
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "community-canvas",
                "https://example.com/Song/1.mp4".toHttpUrl()
            )
        )
''',
    "community canvas rejected host regression",
)

# Video mode gets the same square footprint as album artwork and uses the spare screen width.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "        val playerHorizontalPadding = if (compactPlayer) 18.dp else 20.dp\n",
    '''        val playerHorizontalPadding = when {
            state.isVideoMode -> 8.dp
            compactPlayer -> 18.dp
            else -> 20.dp
        }
''',
    "video player horizontal footprint",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    '''                    val mediaHeight = if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                        artworkSize * 0.5625f
                    } else {
                        artworkSize
                    }
''',
    '''                    val mediaHeight = artworkSize
''',
    "square video stage",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT\n",
    '''                    view.resizeMode = if (pictureInPicture) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
''',
    "cinematic video zoom",
)

# Alternate mode resolution already runs off the playback path. Resolve immediately and prime
# the Media3 cache so tapping Video normally joins a warm descriptor with initial bytes present.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    '''    private fun prefetchAlternateMode(track: Track, activeVideoMode: Boolean) {
        alternateModePrefetchJob?.cancel()
        if (track.id.isBlank() || track.videoUrl.isBlank() || track.source.equals("Offline", true)) return
        alternateModePrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            val cleanTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
            resolver.prefetch(cleanTrack, !activeVideoMode)
        }
    }
''',
    '''    private fun prefetchAlternateMode(track: Track, activeVideoMode: Boolean) {
        alternateModePrefetchJob?.cancel()
        if (track.id.isBlank() || track.videoUrl.isBlank() || track.source.equals("Offline", true)) return
        alternateModePrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val targetVideoMode = !activeVideoMode
            val cleanTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
            val resolved = resolver.prefetch(cleanTrack, targetVideoMode) ?: return@launch
            if (targetVideoMode) {
                runCatching { playbackWarmup.primeVideo(resolved) }
                    .onFailure { Timber.d(it, "alternate video warmup skipped") }
            } else {
                runCatching { playbackWarmup.prime(resolved) }
                    .onFailure { Timber.d(it, "alternate audio warmup skipped") }
            }
        }
    }
''',
    "immediate alternate mode warmup",
)

# YouTube Music frequently nests the playable endpoint under the overlay play button rather than
# exposing playlistItemData on the list row. Walk only endpoint-bearing structures recursively.
replace_once(
    "tools/levyra-editorial/levyra_editorial/youtube_music.py",
    '''def _browse_id(run: Mapping[str, Any]) -> str:
    endpoint = run.get("navigationEndpoint")
    if not isinstance(endpoint, Mapping):
        return ""
    browse = endpoint.get("browseEndpoint")
    return str(browse.get("browseId") or "") if isinstance(browse, Mapping) else ""


''',
    '''def _browse_id(run: Mapping[str, Any]) -> str:
    endpoint = run.get("navigationEndpoint")
    if not isinstance(endpoint, Mapping):
        return ""
    browse = endpoint.get("browseEndpoint")
    return str(browse.get("browseId") or "") if isinstance(browse, Mapping) else ""


def _video_id_from(value: Any) -> str:
    if isinstance(value, Mapping):
        playlist_data = value.get("playlistItemData")
        if isinstance(playlist_data, Mapping):
            candidate = str(playlist_data.get("videoId") or "")
            if VIDEO_ID.fullmatch(candidate):
                return candidate
        watch = value.get("watchEndpoint")
        if isinstance(watch, Mapping):
            candidate = str(watch.get("videoId") or "")
            if VIDEO_ID.fullmatch(candidate):
                return candidate
        for child in value.values():
            candidate = _video_id_from(child)
            if candidate:
                return candidate
    elif isinstance(value, list):
        for child in value:
            candidate = _video_id_from(child)
            if candidate:
                return candidate
    return ""


''',
    "nested YouTube Music video id extractor",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/youtube_music.py",
    '''        video_id = ""
        playlist_data = renderer.get("playlistItemData")
        if isinstance(playlist_data, Mapping):
            video_id = str(playlist_data.get("videoId") or "")
        if not video_id:
            endpoint = renderer.get("navigationEndpoint")
            if isinstance(endpoint, Mapping):
                watch = endpoint.get("watchEndpoint")
                if isinstance(watch, Mapping):
                    video_id = str(watch.get("videoId") or "")
        if VIDEO_ID.fullmatch(video_id) is None:
            continue
''',
    '''        video_id = _video_id_from(renderer)
        if not video_id:
            continue
''',
    "use nested YouTube Music video id extractor",
)
replace_once(
    "tools/levyra-editorial/levyra_editorial/youtube_music.py",
    '''        for key, tracks in targets.items():
            result = results.get(key)
            if result is None:
                continue
            for track in tracks:
                track["youtube_music"] = dict(result)
        return items
''',
    '''        matched = 0
        for key, tracks in targets.items():
            result = results.get(key)
            if result is None:
                continue
            matched += 1
            for track in tracks:
                track["youtube_music"] = dict(result)
        LOGGER.info(
            "Central YouTube Music enrichment matched %d of %d unique recording(s).",
            matched,
            len(metadata),
        )
        return items
''',
    "YouTube Music match telemetry",
)

append_once(
    "tools/levyra-editorial/tests/test_youtube_music.py",
    '''    assert score_candidate("Perfect Song", "Exact Artist", 210_000, candidate) >= 90
''',
    '''


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
''',
    "nested YouTube Music parser regression",
)
