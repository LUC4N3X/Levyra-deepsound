from __future__ import annotations

from pathlib import Path


def insert_before(text: str, marker: str, addition: str) -> str:
    if addition.strip() in text:
        return text
    index = text.find(marker)
    if index < 0:
        raise RuntimeError(f"missing insertion marker: {marker!r}")
    return text[:index] + addition + text[index:]


def replace_between(text: str, start: str, end: str, replacement: str) -> str:
    start_index = text.find(start)
    end_index = text.find(end, start_index + len(start))
    if start_index < 0 or end_index < 0:
        raise RuntimeError(f"missing replacement span: {start!r} -> {end!r}")
    return text[:start_index] + replacement + text[end_index:]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label} anchor count: {count}")
    return text.replace(old, new, 1)


def patch_youtube_music() -> None:
    path = Path("tools/levyra-editorial/levyra_editorial/youtube_music.py")
    text = path.read_text(encoding="utf-8")

    identity_helper = r'''
def _playback_identity_from(value: Any) -> tuple[str, str]:
    playlist_id = ""
    typed: list[tuple[str, str]] = []
    untyped: list[str] = []
    for node in _walk(value):
        playlist_data = node.get("playlistItemData")
        if isinstance(playlist_data, Mapping):
            candidate = str(playlist_data.get("videoId") or "")
            if VIDEO_ID.fullmatch(candidate) and not playlist_id:
                playlist_id = candidate
        watch = node.get("watchEndpoint")
        if not isinstance(watch, Mapping):
            continue
        candidate = str(watch.get("videoId") or "")
        if not VIDEO_ID.fullmatch(candidate):
            continue
        supported = watch.get("watchEndpointMusicSupportedConfigs")
        config = supported.get("watchEndpointMusicConfig") if isinstance(supported, Mapping) else None
        video_type = str(config.get("musicVideoType") or "").strip().upper() if isinstance(config, Mapping) else ""
        if video_type:
            typed.append((candidate, video_type))
        else:
            untyped.append(candidate)
    if playlist_id:
        matching = next((item for item in typed if item[0] == playlist_id), None)
        if matching is not None:
            return matching
        return playlist_id, ""
    if typed:
        return typed[0]
    if untyped:
        return untyped[0], ""
    return "", ""


'''
    text = insert_before(text, "def parse_search_candidates", identity_helper)
    old_video = '''        video_id = _video_id_from(renderer)
        if not video_id:
            continue
'''
    new_video = '''        video_id, music_video_type = _playback_identity_from(renderer)
        if not video_id:
            continue
'''
    text = replace_once(text, old_video, new_video, "candidate playback identity")
    old_duration = '                "durationMs": duration,\n'
    new_duration = (
        '                "durationMs": duration,\n'
        '                "musicVideoType": music_video_type,\n'
    )
    text = replace_once(text, old_duration, new_duration, "candidate video type")

    selector = r'''
OFFICIAL_ANNOTATION = re.compile(
    r"(?i)\s*[\[(](?:official\s+)?(?:music\s+)?(?:video|audio|visuali[sz]er|lyrics?(?:\s+video)?)[^\])]*[\])]\s*"
)
TRAILING_MEDIA_ANNOTATION = re.compile(
    r"(?i)\s*(?:[-–—|:]\s*)?(?:official\s+)?(?:music\s+)?(?:video|audio|visuali[sz]er|lyrics?(?:\s+video)?)\s*$"
)
HARD_VARIANT_TERMS = {
    "karaoke",
    "cover",
    "reaction",
    "sped up",
    "slowed",
    "instrumental",
    "nightcore",
    "live",
    "remix",
}
NON_OFFICIAL_VIDEO_TERMS = {"lyrics", "lyric", "visualizer", "visualiser", "audio"}


def _recording_title_key(value: str) -> str:
    cleaned = OFFICIAL_ANNOTATION.sub(" ", value)
    cleaned = TRAILING_MEDIA_ANNOTATION.sub(" ", cleaned)
    return _text_key(cleaned)


def _artist_similarity(target: str, actual: str) -> float:
    target_key = _text_key(target)
    actual_key = _text_key(actual)
    if not target_key or not actual_key:
        return 0.0
    if target_key == actual_key:
        return 1.0
    target_tokens = _tokens(target_key)
    actual_tokens = _tokens(actual_key)
    if not target_tokens or not actual_tokens:
        return 0.0
    if actual_tokens <= target_tokens:
        return 0.90
    if target_tokens <= actual_tokens:
        return 0.95
    overlap = len(target_tokens & actual_tokens)
    if overlap == 0:
        return 0.0
    precision = overlap / len(actual_tokens)
    recall = overlap / len(target_tokens)
    return 2 * precision * recall / (precision + recall)


def _duration_score(target_ms: int, candidate_ms: Any, *, official_video: bool) -> int | None:
    if not isinstance(candidate_ms, int) or candidate_ms <= 0 or target_ms <= 0:
        return 8 if official_video else None
    delta = abs(candidate_ms - target_ms)
    maximum = max(75_000, round(target_ms * 0.30)) if official_video else 12_000
    if delta > maximum:
        return None
    if delta <= 3_000:
        return 22
    if delta <= 10_000:
        return 18
    if official_video and delta <= 30_000:
        return 12
    return 6


def _contains_unrequested_variant(target_title: str, candidate: Mapping[str, Any]) -> bool:
    target_blob = _text_key(target_title)
    candidate_blob = _text_key(f"{candidate.get('title') or ''} {candidate.get('album') or ''}")
    return any(term in candidate_blob and term not in target_blob for term in HARD_VARIANT_TERMS)


def _audio_candidate_score(
    title: str,
    artist: str,
    duration_ms: int,
    candidate: Mapping[str, Any],
) -> int | None:
    kind = str(candidate.get("musicVideoType") or "").upper()
    if kind.endswith("_OMV") or kind.endswith("_UGC"):
        return None
    if kind and not kind.endswith("_ATV"):
        return None
    if _contains_unrequested_variant(title, candidate):
        return None
    if _recording_title_key(title) != _recording_title_key(str(candidate.get("title") or "")):
        return None
    artist_score = _artist_similarity(artist, str(candidate.get("artist") or ""))
    if artist_score < 0.72:
        return None
    duration_score = _duration_score(duration_ms, candidate.get("durationMs"), official_video=False)
    if duration_score is None:
        return None
    kind_score = 48 if kind.endswith("_ATV") else 12
    return min(100, round(35 + artist_score * 25 + duration_score + kind_score))


def _official_video_candidate_score(
    title: str,
    artist: str,
    duration_ms: int,
    candidate: Mapping[str, Any],
) -> int | None:
    kind = str(candidate.get("musicVideoType") or "").upper()
    raw_title = str(candidate.get("title") or "")
    title_blob = _text_key(raw_title)
    explicitly_official = "official video" in title_blob or "official music video" in title_blob
    if kind.endswith("_ATV") or kind.endswith("_UGC"):
        return None
    if not kind.endswith("_OMV") and not explicitly_official:
        return None
    if _contains_unrequested_variant(title, candidate):
        return None
    if any(term in title_blob for term in NON_OFFICIAL_VIDEO_TERMS) and not explicitly_official:
        return None
    if _recording_title_key(title) != _recording_title_key(raw_title):
        return None
    artist_score = _artist_similarity(artist, str(candidate.get("artist") or ""))
    if artist_score < 0.72:
        return None
    duration_score = _duration_score(duration_ms, candidate.get("durationMs"), official_video=True)
    if duration_score is None:
        return None
    kind_score = 55 if kind.endswith("_OMV") else 36
    official_title_score = 8 if explicitly_official else 0
    return min(100, round(35 + artist_score * 25 + duration_score + kind_score + official_title_score))


def _best_unambiguous(
    ranked: list[tuple[int, Mapping[str, Any]]],
    *,
    minimum: int,
) -> tuple[int, Mapping[str, Any]] | None:
    if not ranked:
        return None
    ranked.sort(key=lambda item: item[0], reverse=True)
    best = ranked[0]
    if best[0] < minimum:
        return None
    if len(ranked) > 1 and best[0] - ranked[1][0] < 7:
        best_kind = str(best[1].get("musicVideoType") or "").upper()
        second_kind = str(ranked[1][1].get("musicVideoType") or "").upper()
        if not (best_kind.endswith("_OMV") and not second_kind.endswith("_OMV")):
            return None
    return best


def select_youtube_music_mapping(
    title: str,
    artist: str,
    duration_ms: int,
    candidates: list[dict[str, Any]],
) -> dict[str, Any] | None:
    unique = {
        str(candidate.get("videoId") or ""): candidate
        for candidate in candidates
        if VIDEO_ID.fullmatch(str(candidate.get("videoId") or ""))
    }
    audio_ranked = [
        (score, candidate)
        for candidate in unique.values()
        if (score := _audio_candidate_score(title, artist, duration_ms, candidate)) is not None
    ]
    video_ranked = [
        (score, candidate)
        for candidate in unique.values()
        if (score := _official_video_candidate_score(title, artist, duration_ms, candidate)) is not None
    ]
    audio = _best_unambiguous(audio_ranked, minimum=82)
    video = _best_unambiguous(video_ranked, minimum=90)
    if audio is None and video is None:
        return None

    output: dict[str, Any] = {}
    selected: list[Mapping[str, Any]] = []
    if audio is not None:
        audio_score, audio_candidate = audio
        output["audioVideoId"] = audio_candidate["videoId"]
        output["audioConfidence"] = audio_score
        selected.append(audio_candidate)
    if video is not None:
        video_score, video_candidate = video
        output["videoId"] = video_candidate["videoId"]
        output["videoConfidence"] = video_score
        selected.insert(0, video_candidate)
    output["confidence"] = max(
        int(output.get("audioConfidence") or 0),
        int(output.get("videoConfidence") or 0),
    )
    for key in ("albumBrowseId", "artistBrowseId", "durationMs"):
        value = next((candidate.get(key) for candidate in selected if candidate.get(key)), None)
        if value:
            output[key] = value
    return output


'''
    text = insert_before(text, "class YoutubeMusicWebClient:", selector)

    new_resolve = r'''    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:
        cache_key = f"{_text_key(title)}\x1f{_text_key(artist)}\x1f{duration_ms // 1000}"
        with self._cache_lock:
            if cache_key in self._cache:
                return self._cache[cache_key]
            if self._query_count >= self._max_queries:
                self._cache[cache_key] = None
                return None
            self._query_count += 1

        candidates_by_id: dict[str, dict[str, Any]] = {}
        result: dict[str, Any] | None = None
        queries = (
            f"{artist} {title} official video",
            f"{title} {artist}",
        )
        for query in dict.fromkeys(queries):
            try:
                payload = self._search(query)
            except (requests.RequestException, ValueError, YoutubeMusicError) as error:
                LOGGER.warning("Central YouTube Music resolution query skipped: %s", type(error).__name__)
                continue
            for candidate in parse_search_candidates(payload):
                video_id = str(candidate.get("videoId") or "")
                if VIDEO_ID.fullmatch(video_id):
                    candidates_by_id[video_id] = candidate
            result = select_youtube_music_mapping(
                title,
                artist,
                duration_ms,
                list(candidates_by_id.values()),
            )
            if result and result.get("audioVideoId") and result.get("videoId"):
                break

        with self._cache_lock:
            self._cache[cache_key] = result
        return result

'''
    text = replace_between(
        text,
        "    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:\n",
        "    def enrich_track_metadata",
        new_resolve,
    )
    path.write_text(text, encoding="utf-8")


def patch_collector() -> None:
    path = Path("tools/levyra-editorial/levyra_editorial/collector.py")
    text = path.read_text(encoding="utf-8")
    replacement = r'''def _safe_youtube_music_match(value: Any) -> dict[str, Any] | None:
    if not isinstance(value, Mapping):
        return None

    def safe_video_id(key: str) -> str | None:
        candidate = _optional_string(value.get(key))
        if candidate and re.fullmatch(r"[A-Za-z0-9_-]{11}", candidate):
            return candidate
        return None

    audio_video_id = safe_video_id("audioVideoId")
    official_video_id = safe_video_id("videoId")
    if audio_video_id is None and official_video_id is None:
        return None

    fallback_confidence = value.get("confidence")
    output: dict[str, Any] = {}
    confidence_values: list[int] = []
    for video_key, confidence_key, video_id in (
        ("audioVideoId", "audioConfidence", audio_video_id),
        ("videoId", "videoConfidence", official_video_id),
    ):
        if video_id is None:
            continue
        confidence = value.get(confidence_key, fallback_confidence)
        if not isinstance(confidence, int) or confidence not in range(0, 101):
            return None
        output[video_key] = video_id
        output[confidence_key] = confidence
        confidence_values.append(confidence)
    output["confidence"] = max(confidence_values)

    for source_key, public_key in (
        ("albumBrowseId", "albumBrowseId"),
        ("artistBrowseId", "artistBrowseId"),
    ):
        candidate = _optional_string(value.get(source_key))
        if candidate and len(candidate) <= 128 and re.fullmatch(r"[A-Za-z0-9_-]+", candidate):
            output[public_key] = candidate
    duration_ms = _positive_int(value.get("durationMs"))
    if duration_ms is not None:
        output["durationMs"] = duration_ms
    return output


'''
    text = replace_between(
        text,
        "def _safe_youtube_music_match(value: Any) -> dict[str, Any] | None:\n",
        "def _clean_text",
        replacement,
    )
    path.write_text(text, encoding="utf-8")


def patch_android() -> None:
    path = Path("app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt")
    text = path.read_text(encoding="utf-8")
    old = '''            val youtubeMusic = item.optJSONObject("youtubeMusic")
            val youtubeVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("videoId"))
            val albumBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("albumBrowseId"))
            val artistBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("artistBrowseId"))
            val youtubeConfidence = youtubeMusic?.optInt("confidence", 0)?.coerceIn(0, 100) ?: 0
'''
    new = '''            val youtubeMusic = item.optJSONObject("youtubeMusic")
            val youtubeAudioVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("audioVideoId"))
            val youtubeOfficialVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("videoId"))
            val youtubePlaybackId = youtubeAudioVideoId.ifBlank { youtubeOfficialVideoId }
            val albumBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("albumBrowseId"))
            val artistBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("artistBrowseId"))
            val youtubeConfidence = maxOf(
                youtubeMusic?.optInt("confidence", 0) ?: 0,
                youtubeMusic?.optInt("audioConfidence", 0) ?: 0,
                youtubeMusic?.optInt("videoConfidence", 0) ?: 0,
            ).coerceIn(0, 100)
'''
    text = replace_once(text, old, new, "Android YouTube metadata")
    text = replace_once(
        text,
        '                id = youtubeVideoId.ifBlank { "chart-${identity.id}" },',
        '                id = youtubePlaybackId.ifBlank { "chart-${identity.id}" },',
        "Android playback id",
    )
    old_url = '''                videoUrl = youtubeVideoId.takeIf(String::isNotBlank)
                    ?.let { "https://www.youtube.com/watch?v=$it" }
                    .orEmpty(),
'''
    text = replace_once(text, old_url, '                videoUrl = "",\n', "Android audio URL")
    text = replace_once(
        text,
        '                counterpartVideoId = youtubeVideoId,',
        '''                counterpartVideoId = youtubeOfficialVideoId,
                videoType = when {
                    youtubeOfficialVideoId.isNotBlank() -> "MUSIC_VIDEO_TYPE_OMV"
                    youtubeAudioVideoId.isNotBlank() -> "MUSIC_VIDEO_TYPE_ATV"
                    else -> ""
                },''',
        "Android official counterpart",
    )
    text = replace_once(
        text,
        '                metadataProvider = if (youtubeVideoId.isBlank()) EDITORIAL_SOURCE else "$EDITORIAL_SOURCE + YouTube Music",',
        '                metadataProvider = if (youtubePlaybackId.isBlank()) EDITORIAL_SOURCE else "$EDITORIAL_SOURCE + YouTube Music",',
        "Android metadata provider",
    )
    text = replace_once(
        text,
        '                metadataConfidence = if (youtubeVideoId.isBlank()) 94 else youtubeConfidence,',
        '                metadataConfidence = if (youtubePlaybackId.isBlank()) 94 else youtubeConfidence,',
        "Android metadata confidence",
    )
    path.write_text(text, encoding="utf-8")


def patch_tests() -> None:
    path = Path("tools/levyra-editorial/tests/test_youtube_music.py")
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    score_candidate,\n",
        "    score_candidate,\n    select_youtube_music_mapping,\n",
        "mapping import",
    )
    addition = r'''
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


'''
    text = insert_before(text, "class BootstrapResponse:", addition)
    path.write_text(text, encoding="utf-8")

    collector_path = Path("tools/levyra-editorial/tests/test_collector.py")
    collector = collector_path.read_text(encoding="utf-8")
    collector_addition = r'''
def test_catalog_keeps_separate_audio_and_official_video_ids() -> None:
    item = FakeClient().iter_playlist_items("playlist12345")[0]
    item["track"]["youtube_music"] = {
        "audioVideoId": "Audio123456",
        "audioConfidence": 99,
        "videoId": "Official123",
        "videoConfidence": 97,
        "confidence": 99,
    }

    public = normalize_playlist_items([item])[0].to_dict()

    assert public["youtubeMusic"]["audioVideoId"] == "Audio123456"
    assert public["youtubeMusic"]["videoId"] == "Official123"
    assert public["youtubeMusic"]["audioConfidence"] == 99
    assert public["youtubeMusic"]["videoConfidence"] == 97


'''
    collector = insert_before(
        collector,
        "def test_secret_dictionary_url_requires_pinned_allowlisted_path",
        collector_addition,
    )
    collector_path.write_text(collector, encoding="utf-8")


def main() -> None:
    patch_youtube_music()
    patch_collector()
    patch_android()
    patch_tests()


if __name__ == "__main__":
    main()
