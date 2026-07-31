from __future__ import annotations

from pathlib import Path


def insert_before(text: str, marker: str, addition: str) -> str:
    if addition.strip() in text:
        return text
    index = text.find(marker)
    if index < 0:
        raise RuntimeError(f"missing insertion marker: {marker!r}")
    return text[:index] + addition + text[index:]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label} anchor count: {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str) -> str:
    start_index = text.find(start)
    end_index = text.find(end, start_index + len(start))
    if start_index < 0 or end_index < 0:
        raise RuntimeError(f"missing replacement span: {start!r} -> {end!r}")
    return text[:start_index] + replacement + text[end_index:]


def patch_source() -> None:
    path = Path("tools/levyra-editorial/levyra_editorial/youtube_music.py")
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        'SEARCH_URL = f"{ORIGIN}/youtubei/v1/search"\n',
        'SEARCH_URL = f"{ORIGIN}/youtubei/v1/search"\n'
        'YOUTUBE_ORIGIN = "https://www.youtube.com"\n'
        'YOUTUBE_HOME_URL = f"{YOUTUBE_ORIGIN}/"\n'
        'YOUTUBE_SEARCH_URL = f"{YOUTUBE_ORIGIN}/youtubei/v1/search"\n',
        "YouTube Web constants",
    )

    old_runs = '''def _runs_text(value: Any) -> str:
    if not isinstance(value, Mapping):
        return ""
    runs = value.get("runs")
    if not isinstance(runs, list):
        return ""
    return "".join(str(run.get("text") or "") for run in runs if isinstance(run, Mapping)).strip()
'''
    new_runs = '''def _runs_text(value: Any) -> str:
    if not isinstance(value, Mapping):
        return ""
    simple = value.get("simpleText")
    if isinstance(simple, str) and simple.strip():
        return simple.strip()
    runs = value.get("runs")
    if not isinstance(runs, list):
        return ""
    return "".join(str(run.get("text") or "") for run in runs if isinstance(run, Mapping)).strip()
'''
    text = replace_once(text, old_runs, new_runs, "text parser")

    text = replace_once(
        text,
        '''def _duration_score(target_ms: int, candidate_ms: Any, *, official_video: bool) -> int | None:
    if not isinstance(candidate_ms, int) or candidate_ms <= 0 or target_ms <= 0:
        return 8 if official_video else None
''',
        '''def _duration_score(target_ms: int, candidate_ms: Any, *, official_video: bool) -> int | None:
    if not isinstance(candidate_ms, int) or candidate_ms <= 0 or target_ms <= 0:
        return 8
''',
        "missing duration fallback",
    )

    ytm_selector = '''def select_youtube_music_mapping(
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
    audio = _best_unambiguous(audio_ranked, minimum=82)
    if audio is None:
        return None

    audio_score, audio_candidate = audio
    output: dict[str, Any] = {
        "audioVideoId": audio_candidate["videoId"],
        "audioConfidence": audio_score,
        "confidence": audio_score,
    }
    for key in ("albumBrowseId", "artistBrowseId", "durationMs"):
        value = audio_candidate.get(key)
        if value:
            output[key] = value
    return output


'''
    text = replace_between(
        text,
        "def select_youtube_music_mapping(\n",
        "class YoutubeMusicWebClient:",
        ytm_selector,
    )

    web_helpers = r'''
def parse_youtube_web_candidates(payload: Mapping[str, Any]) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    for node in _walk(payload):
        renderer = node.get("videoRenderer")
        if not isinstance(renderer, Mapping):
            continue
        video_id = str(renderer.get("videoId") or "")
        title = _runs_text(renderer.get("title"))
        if not VIDEO_ID.fullmatch(video_id) or not title:
            continue
        owner_text = renderer.get("ownerText")
        owner = _runs_text(owner_text)
        owner_runs = owner_text.get("runs") if isinstance(owner_text, Mapping) else None
        channel_id = ""
        if isinstance(owner_runs, list):
            for run in owner_runs:
                if not isinstance(run, Mapping):
                    continue
                browse_id = _browse_id(run)
                if browse_id.startswith("UC"):
                    channel_id = browse_id
                    break
        badge_styles: set[str] = set()
        badge_tooltips: set[str] = set()
        raw_badges = renderer.get("ownerBadges")
        if isinstance(raw_badges, list):
            for badge in raw_badges:
                if not isinstance(badge, Mapping):
                    continue
                metadata = badge.get("metadataBadgeRenderer")
                if not isinstance(metadata, Mapping):
                    continue
                style = str(metadata.get("style") or "").strip().upper()
                tooltip = str(metadata.get("tooltip") or "").strip().casefold()
                if style:
                    badge_styles.add(style)
                if tooltip:
                    badge_tooltips.add(tooltip)
        output.append(
            {
                "videoId": video_id,
                "title": title,
                "owner": owner,
                "channelId": channel_id,
                "durationMs": _duration_ms(_runs_text(renderer.get("lengthText"))),
                "verifiedArtist": (
                    "BADGE_STYLE_TYPE_VERIFIED_ARTIST" in badge_styles
                    or "official artist channel" in badge_tooltips
                ),
            }
        )
    return output


def _artist_names(value: str) -> list[str]:
    cleaned = re.sub(r"(?i)\b(?:feat(?:uring)?|ft|with)\.?\b", ",", value)
    return [
        key
        for part in re.split(r"\s*(?:,|&|\+|/|;|\band\b)\s*", cleaned)
        if (key := _text_key(part)) and key not in {"and", "more"}
    ]


def _owner_matches_artist(owner: str, artist: str) -> bool:
    owner_key = _text_key(re.sub(r"(?i)\band\s+\d+\s+more\b", "", owner))
    if not owner_key:
        return False
    return any(
        name == owner_key or name in owner_key or owner_key in name
        for name in _artist_names(artist)
    )


def _web_recording_title(value: str, artist: str) -> str:
    cleaned = OFFICIAL_ANNOTATION.sub(" ", value)
    cleaned = TRAILING_MEDIA_ANNOTATION.sub(" ", cleaned).strip()
    for separator in (" - ", " – ", " — ", " | "):
        if separator not in cleaned:
            continue
        prefix, suffix = cleaned.split(separator, 1)
        if _owner_matches_artist(prefix, artist):
            cleaned = suffix
            break
    return _text_key(cleaned)


def _official_web_video_score(
    title: str,
    artist: str,
    duration_ms: int,
    candidate: Mapping[str, Any],
) -> int | None:
    raw_title = str(candidate.get("title") or "")
    title_key = _text_key(raw_title)
    if "official audio" in title_key:
        return None
    if "official video" not in title_key and "official music video" not in title_key:
        return None
    if any(term in title_key for term in ("lyrics", "lyric", "visualizer", "visualiser", "reaction")):
        return None
    if not candidate.get("verifiedArtist"):
        return None
    if not _owner_matches_artist(str(candidate.get("owner") or ""), artist):
        return None
    if _web_recording_title(raw_title, artist) != _recording_title_key(title):
        return None
    candidate_duration = candidate.get("durationMs")
    duration_score = 0
    if isinstance(candidate_duration, int) and candidate_duration > 0 and duration_ms > 0:
        delta = abs(candidate_duration - duration_ms)
        maximum = max(90_000, round(duration_ms * 0.40))
        if delta > maximum:
            return None
        duration_score = max(0, 30 - round(30 * delta / maximum))
    return 220 + duration_score


def select_official_youtube_video(
    title: str,
    artist: str,
    duration_ms: int,
    candidates: list[dict[str, Any]],
) -> dict[str, Any] | None:
    ranked = [
        (score, candidate)
        for candidate in candidates
        if (score := _official_web_video_score(title, artist, duration_ms, candidate)) is not None
    ]
    if not ranked:
        return None
    ranked.sort(key=lambda item: item[0], reverse=True)
    best_score, best = ranked[0]
    if len(ranked) > 1 and best_score - ranked[1][0] < 8:
        return None
    return {
        "videoId": best["videoId"],
        "videoConfidence": min(100, 95 + max(0, best_score - 220) // 6),
    }


'''
    text = insert_before(text, "class YoutubeMusicWebClient:", web_helpers)

    old_fields = '''        self._visitor_data = ""
        self._bootstrap_lock = threading.Lock()
        self._bootstrap_failed = False
'''
    new_fields = '''        self._visitor_data = ""
        self._bootstrap_lock = threading.Lock()
        self._bootstrap_failed = False
        self._web_api_key = ""
        self._web_client_version = ""
        self._web_bootstrap_lock = threading.Lock()
        self._web_bootstrap_failed = False
'''
    text = replace_once(text, old_fields, new_fields, "Web bootstrap fields")

    web_methods = r'''    def _web_bootstrap(self) -> None:
        if self._web_api_key and self._web_client_version:
            return
        if self._web_bootstrap_failed:
            raise YoutubeMusicError("YouTube Web bootstrap previously failed.")
        with self._web_bootstrap_lock:
            if self._web_api_key and self._web_client_version:
                return
            if self._web_bootstrap_failed:
                raise YoutubeMusicError("YouTube Web bootstrap previously failed.")
            try:
                response = self._session.get(
                    YOUTUBE_HOME_URL,
                    headers={
                        "Cookie": self._cookie,
                        "Accept-Language": "en-US,en;q=0.9",
                    },
                    timeout=self._timeout,
                )
                response.raise_for_status()
                key = re.search(r'"INNERTUBE_API_KEY":"([^"\\]+)"', response.text)
                version = re.search(r'"INNERTUBE_CLIENT_VERSION":"([^"\\]+)"', response.text)
                if key is None or version is None:
                    raise YoutubeMusicError("Unable to bootstrap YouTube Web search.")
                self._web_api_key = key.group(1)
                self._web_client_version = version.group(1)
            except (requests.RequestException, YoutubeMusicError):
                self._web_bootstrap_failed = True
                raise

    def _web_search(self, query: str) -> Mapping[str, Any]:
        self._web_bootstrap()
        payload = {
            "context": {
                "client": {
                    "clientName": "WEB",
                    "clientVersion": self._web_client_version,
                    "hl": "en",
                    "gl": "US",
                }
            },
            "query": query,
        }
        response = self._session.post(
            YOUTUBE_SEARCH_URL,
            params={"key": self._web_api_key, "prettyPrint": "false"},
            headers={
                "Cookie": self._cookie,
                "Origin": YOUTUBE_ORIGIN,
                "Referer": YOUTUBE_HOME_URL,
                "Content-Type": "application/json",
                "X-Youtube-Client-Name": "1",
                "X-Youtube-Client-Version": self._web_client_version,
            },
            json=payload,
            timeout=self._timeout,
        )
        response.raise_for_status()
        data = response.json()
        if not isinstance(data, Mapping):
            raise YoutubeMusicError("YouTube Web returned an invalid search response.")
        return data

    def _resolve_official_video(
        self,
        title: str,
        artist: str,
        duration_ms: int,
    ) -> dict[str, Any] | None:
        payload = self._web_search(f"{artist} {title} official video")
        return select_official_youtube_video(
            title,
            artist,
            duration_ms,
            parse_youtube_web_candidates(payload),
        )

'''
    text = insert_before(text, "    def resolve(self, title: str, artist: str, duration_ms: int)", web_methods)

    new_resolve = r'''    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:
        cache_key = f"{_text_key(title)}\x1f{_text_key(artist)}\x1f{duration_ms // 1000}"
        with self._cache_lock:
            if cache_key in self._cache:
                return self._cache[cache_key]
            if self._query_count >= self._max_queries:
                self._cache[cache_key] = None
                return None
            self._query_count += 1

        audio_result: dict[str, Any] | None = None
        try:
            payload = self._search(f"{title} {artist}")
            audio_result = select_youtube_music_mapping(
                title,
                artist,
                duration_ms,
                parse_search_candidates(payload),
            )
        except (requests.RequestException, ValueError, YoutubeMusicError) as error:
            LOGGER.warning("Central YouTube Music audio query skipped: %s", type(error).__name__)

        official_video: dict[str, Any] | None = None
        try:
            official_video = self._resolve_official_video(title, artist, duration_ms)
        except (requests.RequestException, ValueError, YoutubeMusicError) as error:
            LOGGER.warning("Central YouTube official-video query skipped: %s", type(error).__name__)

        result: dict[str, Any] = dict(audio_result or {})
        if official_video:
            result.update(official_video)
        confidence_values = [
            value
            for key in ("audioConfidence", "videoConfidence")
            if isinstance((value := result.get(key)), int)
        ]
        if confidence_values:
            result["confidence"] = max(confidence_values)
        final_result = result or None
        with self._cache_lock:
            self._cache[cache_key] = final_result
        return final_result

'''
    text = replace_between(
        text,
        "    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:\n",
        "    def enrich_track_metadata",
        new_resolve,
    )
    path.write_text(text, encoding="utf-8")


def patch_tests() -> None:
    path = Path("tools/levyra-editorial/tests/test_youtube_music.py")
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    parse_search_candidates,\n",
        "    parse_search_candidates,\n    parse_youtube_web_candidates,\n",
        "Web parser import",
    )
    text = replace_once(
        text,
        "    select_youtube_music_mapping,\n",
        "    select_youtube_music_mapping,\n    select_official_youtube_video,\n",
        "Web selector import",
    )

    old_mapping_test = '''def test_mapping_separates_art_track_from_official_music_video() -> None:
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


'''
    new_mapping_test = '''def test_ytm_mapping_keeps_art_track_and_never_promotes_omv() -> None:
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


'''
    text = replace_once(text, old_mapping_test, new_mapping_test, "old dual mapping test")

    web_tests = r'''
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


'''
    text = insert_before(text, "class BootstrapResponse:", web_tests)
    path.write_text(text, encoding="utf-8")


def main() -> None:
    patch_source()
    patch_tests()


if __name__ == "__main__":
    main()
