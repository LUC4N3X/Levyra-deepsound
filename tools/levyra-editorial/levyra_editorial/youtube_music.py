from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import threading
import time
from collections.abc import Mapping
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

import requests

LOGGER = logging.getLogger(__name__)
ORIGIN = "https://music.youtube.com"
HOME_URL = f"{ORIGIN}/"
SEARCH_URL = f"{ORIGIN}/youtubei/v1/search"
YOUTUBE_ORIGIN = "https://www.youtube.com"
YOUTUBE_HOME_URL = f"{YOUTUBE_ORIGIN}/"
YOUTUBE_SEARCH_URL = f"{YOUTUBE_ORIGIN}/youtubei/v1/search"
DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
)
COOKIE_NAME = re.compile(r"^[A-Za-z0-9_.-]{1,80}$")
VIDEO_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")
DURATION = re.compile(r"\b(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\b")
PENALTY_TERMS = {
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


class YoutubeMusicError(RuntimeError):
    """Raised when the central YouTube Music session cannot be used safely."""


def normalize_youtube_music_cookie(raw_value: str) -> tuple[str, str]:
    raw = raw_value.strip()
    if not raw or len(raw) > 64 * 1024:
        raise YoutubeMusicError("The central YouTube Music session is empty or too large.")
    cookies: dict[str, str] = {}

    if raw.startswith("{"):
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            payload = None
        if isinstance(payload, Mapping):
            for name, value in payload.items():
                if isinstance(name, str) and isinstance(value, str):
                    cookies[name.strip()] = value.strip()

    for line in raw.splitlines():
        columns = line.strip().split("\t")
        if len(columns) >= 7:
            domain = columns[0].removeprefix("#HttpOnly_").lower()
            if domain in {"youtube.com", ".youtube.com", "music.youtube.com"}:
                cookies[columns[5].strip()] = columns[6].strip()

    header = raw.partition("Cookie:")[2] if "Cookie:" in raw else raw
    for segment in header.replace("\n", ";").replace("\r", ";").split(";"):
        name, separator, value = segment.strip().partition("=")
        if separator:
            cookies[name.strip()] = value.strip()

    safe = {
        name: value
        for name, value in cookies.items()
        if COOKIE_NAME.fullmatch(name)
        and value
        and len(value) <= 8192
        and not any(character.isspace() for character in value)
    }
    sapisid = next(
        (
            safe.get(name)
            for name in ("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
            if safe.get(name)
        ),
        None,
    )
    if sapisid is None:
        raise YoutubeMusicError("The central YouTube Music session lacks SAPISID.")
    return "; ".join(f"{name}={value}" for name, value in sorted(safe.items())), sapisid


def _text_key(value: str) -> str:
    lowered = value.casefold()
    lowered = re.sub(r"[^a-z0-9àèéìòóùçñäöüß]+", " ", lowered)
    return " ".join(lowered.split())


def _tokens(value: str) -> set[str]:
    return {token for token in _text_key(value).split() if len(token) >= 2}


def _duration_ms(value: str) -> int | None:
    match = DURATION.search(value)
    if match is None:
        return None
    hours = int(match.group(1) or 0)
    minutes = int(match.group(2))
    seconds = int(match.group(3))
    return (hours * 3600 + minutes * 60 + seconds) * 1000


def _walk(value: Any):
    if isinstance(value, Mapping):
        yield value
        for child in value.values():
            yield from _walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk(child)


def _runs_text(value: Any) -> str:
    if not isinstance(value, Mapping):
        return ""
    simple = value.get("simpleText")
    if isinstance(simple, str) and simple.strip():
        return simple.strip()
    runs = value.get("runs")
    if not isinstance(runs, list):
        return ""
    return "".join(str(run.get("text") or "") for run in runs if isinstance(run, Mapping)).strip()


def _page_type(run: Mapping[str, Any]) -> str:
    endpoint = run.get("navigationEndpoint")
    if not isinstance(endpoint, Mapping):
        return ""
    browse = endpoint.get("browseEndpoint")
    if not isinstance(browse, Mapping):
        return ""
    configs = browse.get("browseEndpointContextSupportedConfigs")
    if not isinstance(configs, Mapping):
        return ""
    music = configs.get("browseEndpointContextMusicConfig")
    return str(music.get("pageType") or "") if isinstance(music, Mapping) else ""


def _browse_id(run: Mapping[str, Any]) -> str:
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
        video_type = (
    str(config.get("musicVideoType") or "").strip().upper()
    if isinstance(config, Mapping)
    else ""
)
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


def parse_search_candidates(payload: Mapping[str, Any]) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    for node in _walk(payload):
        renderer = node.get("musicResponsiveListItemRenderer")
        if not isinstance(renderer, Mapping):
            continue
        columns = renderer.get("flexColumns")
        if not isinstance(columns, list) or not columns:
            continue
        first = columns[0] if isinstance(columns[0], Mapping) else {}
        first_renderer = first.get("musicResponsiveListItemFlexColumnRenderer")
        if not isinstance(first_renderer, Mapping):
            continue
        title = _runs_text(first_renderer.get("text"))
        if not title:
            continue
        video_id, music_video_type = _playback_identity_from(renderer)
        if not video_id:
            continue

        artists: list[str] = []
        artist_browse_id = ""
        album = ""
        album_browse_id = ""
        metadata_text: list[str] = []
        for column in columns[1:]:
            if not isinstance(column, Mapping):
                continue
            flex = column.get("musicResponsiveListItemFlexColumnRenderer")
            if not isinstance(flex, Mapping):
                continue
            text = flex.get("text")
            if not isinstance(text, Mapping):
                continue
            metadata_text.append(_runs_text(text))
            runs = text.get("runs")
            if not isinstance(runs, list):
                continue
            for run in runs:
                if not isinstance(run, Mapping):
                    continue
                label = str(run.get("text") or "").strip()
                browse_id = _browse_id(run)
                page_type = _page_type(run)
                if "ARTIST" in page_type or browse_id.startswith("UC"):
                    if label and label not in artists:
                        artists.append(label)
                    if not artist_browse_id:
                        artist_browse_id = browse_id
                elif "ALBUM" in page_type or browse_id.startswith("MPRE"):
                    if label and not album:
                        album = label
                    if not album_browse_id:
                        album_browse_id = browse_id

        duration = None
        fixed = renderer.get("fixedColumns")
        if isinstance(fixed, list):
            for column in fixed:
                if not isinstance(column, Mapping):
                    continue
                fixed_renderer = column.get("musicResponsiveListItemFixedColumnRenderer")
                if isinstance(fixed_renderer, Mapping):
                    duration = _duration_ms(_runs_text(fixed_renderer.get("text"))) or duration
        if duration is None:
            duration = _duration_ms(" ".join(metadata_text))
        output.append(
            {
                "videoId": video_id,
                "title": title,
                "artist": ", ".join(artists),
                "album": album,
                "artistBrowseId": artist_browse_id,
                "albumBrowseId": album_browse_id,
                "durationMs": duration,
                "musicVideoType": music_video_type,
            }
        )
    return output


def score_candidate(
    title: str,
    artist: str,
    duration_ms: int,
    candidate: Mapping[str, Any],
) -> int:
    target_title = _text_key(title)
    actual_title = _text_key(str(candidate.get("title") or ""))
    target_artist = _text_key(artist)
    actual_artist = _text_key(str(candidate.get("artist") or ""))
    if not target_title or not actual_title:
        return -100
    score = 0
    if target_title == actual_title:
        score += 48
    elif target_title in actual_title or actual_title in target_title:
        score += 32
    else:
        title_tokens = _tokens(target_title)
        actual_tokens = _tokens(actual_title)
        if title_tokens:
            score += round(30 * len(title_tokens & actual_tokens) / len(title_tokens))

    if target_artist and target_artist == actual_artist:
        score += 32
    elif target_artist and (target_artist in actual_artist or actual_artist in target_artist):
        score += 24
    else:
        artist_tokens = _tokens(target_artist)
        actual_tokens = _tokens(actual_artist)
        if artist_tokens:
            score += round(22 * len(artist_tokens & actual_tokens) / len(artist_tokens))

    candidate_duration = candidate.get("durationMs")
    if isinstance(candidate_duration, int) and duration_ms > 0:
        delta = abs(candidate_duration - duration_ms)
        if delta <= 3_000:
            score += 20
        elif delta <= 10_000:
            score += 12
        elif delta > 35_000:
            score -= 20

    blob = f"{actual_title} {_text_key(str(candidate.get('album') or ''))}"
    target_blob = target_title
    if any(term in blob for term in PENALTY_TERMS) and not any(term in target_blob for term in PENALTY_TERMS):
        score -= 28
    return score



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
        return 8
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


def combine_verified_youtube_mapping(
    audio_result: Mapping[str, Any] | None,
    official_video: Mapping[str, Any] | None,
) -> dict[str, Any] | None:
    """Publish an official video only behind a verified Art Track identity."""
    if not isinstance(audio_result, Mapping):
        return None
    audio_id = str(audio_result.get("audioVideoId") or "").strip()
    audio_confidence = audio_result.get("audioConfidence")
    if not VIDEO_ID.fullmatch(audio_id) or not isinstance(audio_confidence, int) or audio_confidence < 82:
        return None

    result = dict(audio_result)
    result["audioVideoId"] = audio_id
    result.pop("videoId", None)
    result.pop("videoConfidence", None)

    if isinstance(official_video, Mapping):
        video_id = str(official_video.get("videoId") or "").strip()
        video_confidence = official_video.get("videoConfidence")
        if (
            VIDEO_ID.fullmatch(video_id)
            and video_id != audio_id
            and isinstance(video_confidence, int)
            and video_confidence >= 90
        ):
            result["videoId"] = video_id
            result["videoConfidence"] = video_confidence

    confidence_values = [
        value
        for key in ("audioConfidence", "videoConfidence")
        if isinstance((value := result.get(key)), int)
    ]
    result["confidence"] = max(confidence_values)
    return result


class YoutubeMusicWebClient:
    """Resolve public recording identities with one repository-owned session."""

    def __init__(
        self,
        cookie: str,
        *,
        session: requests.Session | None = None,
        timeout_seconds: float = 8.0,
        workers: int = 6,
    ) -> None:
        self._cookie, self._sapisid = normalize_youtube_music_cookie(cookie)
        self._session = session or requests.Session()
        self._session.headers.update({"User-Agent": DEFAULT_USER_AGENT, "Accept": "application/json"})
        self._timeout = timeout_seconds
        self._workers = max(1, min(workers, 10))
        self._api_key = ""
        self._client_version = ""
        self._visitor_data = ""
        self._bootstrap_lock = threading.Lock()
        self._bootstrap_failed = False
        self._web_api_key = ""
        self._web_client_version = ""
        self._web_bootstrap_lock = threading.Lock()
        self._web_bootstrap_failed = False
        self._cache_lock = threading.Lock()
        self._cache: dict[str, dict[str, Any] | None] = {}
        self._query_count = 0
        self._max_queries = max(1, int(os.environ.get("LEVYRA_EDITORIAL_YTM_MAX_QUERIES", "700")))

    def close(self) -> None:
        self._session.close()

    def _authorization(self) -> str:
        timestamp = int(time.time())
        digest = hashlib.sha1(f"{timestamp} {self._sapisid} {ORIGIN}".encode()).hexdigest()
        return f"SAPISIDHASH {timestamp}_{digest}"

    def _bootstrap(self) -> None:
        if self._api_key and self._client_version:
            return
        if self._bootstrap_failed:
            raise YoutubeMusicError("Central YouTube Music session bootstrap previously failed.")
        with self._bootstrap_lock:
            if self._api_key and self._client_version:
                return
            if self._bootstrap_failed:
                raise YoutubeMusicError("Central YouTube Music session bootstrap previously failed.")
            try:
                response = self._session.get(
                    HOME_URL,
                    headers={"Cookie": self._cookie, "Origin": ORIGIN, "Referer": HOME_URL},
                    timeout=self._timeout,
                )
                response.raise_for_status()
                body = response.text
                key = re.search(r'"INNERTUBE_API_KEY":"([^"\\]+)"', body)
                version = re.search(r'"INNERTUBE_CLIENT_VERSION":"([^"\\]+)"', body)
                visitor = re.search(r'"VISITOR_DATA":"([^"\\]+)"', body)
                if key is None or version is None:
                    raise YoutubeMusicError("Unable to bootstrap the central YouTube Music session.")
                self._api_key = key.group(1)
                self._client_version = version.group(1)
                self._visitor_data = visitor.group(1) if visitor else ""
            except (requests.RequestException, YoutubeMusicError):
                self._bootstrap_failed = True
                raise

    def _search(self, query: str) -> Mapping[str, Any]:
        self._bootstrap()
        headers = {
            "Cookie": self._cookie,
            "Authorization": self._authorization(),
            "Origin": ORIGIN,
            "Referer": HOME_URL,
            "Content-Type": "application/json",
            "X-Goog-AuthUser": "0",
            "X-Youtube-Client-Name": "67",
            "X-Youtube-Client-Version": self._client_version,
        }
        if self._visitor_data:
            headers["X-Goog-Visitor-Id"] = self._visitor_data
        payload = {
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": self._client_version,
                    "hl": "en",
                    "gl": "US",
                    "platform": "DESKTOP",
                    "visitorData": self._visitor_data,
                }
            },
            "query": query,
        }
        response = self._session.post(
            SEARCH_URL,
            params={"key": self._api_key, "prettyPrint": "false"},
            headers=headers,
            json=payload,
            timeout=self._timeout,
        )
        response.raise_for_status()
        data = response.json()
        if not isinstance(data, Mapping):
            raise YoutubeMusicError("YouTube Music returned an invalid search response.")
        return data

    def _web_bootstrap(self) -> None:
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

    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:
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

        verified_audio = combine_verified_youtube_mapping(audio_result, None)
        official_video: dict[str, Any] | None = None
        if verified_audio is not None:
            try:
                official_video = self._resolve_official_video(title, artist, duration_ms)
            except (requests.RequestException, ValueError, YoutubeMusicError) as error:
                LOGGER.warning("Central YouTube official-video query skipped: %s", type(error).__name__)

        final_result = combine_verified_youtube_mapping(verified_audio, official_video)
        with self._cache_lock:
            self._cache[cache_key] = final_result
        return final_result

    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        targets: dict[str, list[dict[str, Any]]] = {}
        metadata: dict[str, tuple[str, str, int]] = {}
        for item in items:
            track = item.get("track")
            if not isinstance(track, dict):
                continue
            title = str(track.get("name") or "").strip()
            raw_artists = track.get("artists")
            artists = [
                str(value.get("name") or "").strip()
                for value in raw_artists
                if isinstance(value, Mapping) and str(value.get("name") or "").strip()
            ] if isinstance(raw_artists, list) else []
            duration_ms = track.get("duration_ms")
            if not title or not artists or not isinstance(duration_ms, int) or duration_ms <= 0:
                continue
            key = f"{_text_key(title)}\x1f{_text_key(', '.join(artists))}\x1f{duration_ms // 1000}"
            targets.setdefault(key, []).append(track)
            metadata[key] = (title, ", ".join(artists), duration_ms)

        results: dict[str, dict[str, Any] | None] = {}
        with ThreadPoolExecutor(max_workers=self._workers) as executor:
            futures = {
                executor.submit(self.resolve, title, artist, duration_ms): key
                for key, (title, artist, duration_ms) in metadata.items()
            }
            for future in as_completed(futures):
                key = futures[future]
                try:
                    results[key] = future.result()
                except Exception:
                    results[key] = None
        matched = 0
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
