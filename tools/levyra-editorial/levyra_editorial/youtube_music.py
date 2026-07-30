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
        video_id = ""
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
        with self._bootstrap_lock:
            if self._api_key and self._client_version:
                return
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

    def resolve(self, title: str, artist: str, duration_ms: int) -> dict[str, Any] | None:
        cache_key = f"{_text_key(title)}\x1f{_text_key(artist)}\x1f{duration_ms // 1000}"
        with self._cache_lock:
            if cache_key in self._cache:
                return self._cache[cache_key]
            if self._query_count >= self._max_queries:
                self._cache[cache_key] = None
                return None
            self._query_count += 1
        try:
            payload = self._search(f"{title} {artist}")
            candidates = parse_search_candidates(payload)
            ranked = sorted(
                (
            (score_candidate(title, artist, duration_ms, candidate), candidate)
            for candidate in candidates
        ),
                key=lambda item: item[0],
                reverse=True,
            )
            if not ranked or ranked[0][0] < 70:
                result = None
            else:
                score, candidate = ranked[0]
                result = {
                    "videoId": candidate["videoId"],
                    "confidence": min(100, max(0, score)),
                }
                for key in ("albumBrowseId", "artistBrowseId", "durationMs"):
                    value = candidate.get(key)
                    if value:
                        result[key] = value
        except (requests.RequestException, ValueError, YoutubeMusicError) as error:
            LOGGER.warning("Central YouTube Music resolution skipped: %s", type(error).__name__)
            result = None
        with self._cache_lock:
            self._cache[cache_key] = result
        return result

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
        for key, tracks in targets.items():
            result = results.get(key)
            if result is None:
                continue
            for track in tracks:
                track["youtube_music"] = dict(result)
        return items
