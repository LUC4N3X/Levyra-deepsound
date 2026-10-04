from __future__ import annotations

import argparse
import ipaddress
import json
import logging
import os
import re
import sqlite3
import sys
import threading
import time
from collections import OrderedDict
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Protocol
from urllib.parse import urlparse

import requests

from .collector import _spotify_canvas_url_problem
from .spotify import AuthenticationError, EditorialSourceError, SpotifyWebClient

LOGGER = logging.getLogger(__name__)

ISRC_PATTERN = re.compile(r"^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
NORM_APOSTROPHES = re.compile(r"[’'`´]")
NORM_BRACKETS = re.compile(r"[()\[\]{}]")
NORM_NON_ALPHANUMERIC = re.compile(r"[^\w\s]+", re.UNICODE)
NORM_WHITESPACE = re.compile(r"\s+")
ARTIST_SEPARATORS = re.compile(
    r"(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+[xX]\s+|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b|\bcon\b)",
    re.IGNORECASE,
)

MULTI_WORD_UNSAFE_TERMS = frozenset({
    "sped up",
    "dj mix",
    "set list",
})

SINGLE_WORD_UNSAFE_TERMS = frozenset({
    "karaoke",
    "tribute",
    "cover",
    "nightcore",
    "slowed",
    "instrumental",
    "playlist",
    "essentials",
    "session",
})

UNSAFE_TERMS = MULTI_WORD_UNSAFE_TERMS | SINGLE_WORD_UNSAFE_TERMS

EDITION_TERMS = frozenset({
    "live",
    "remix",
    "acoustic",
    "instrumental",
    "deluxe",
    "remaster",
    "remastered",
    "version",
    "edit",
    "mix",
})

FEATURE_TOKENS = frozenset({"feat", "featuring", "ft", "con", "with", "w", "and", "e"})

DEFAULT_POSITIVE_TTL_SECONDS = 24 * 3600
DEFAULT_NEGATIVE_TTL_SECONDS = 600
MAX_REQUEST_BODY_BYTES = 16 * 1024
DEFAULT_MAX_CACHE_ENTRIES = 50_000
DEFAULT_RATE_LIMIT_PER_MINUTE = 60


def normalize_text(value: str) -> str:
    cleaned = NORM_APOSTROPHES.sub("", value.lower())
    cleaned = NORM_BRACKETS.sub(" ", cleaned)
    cleaned = NORM_NON_ALPHANUMERIC.sub(" ", cleaned)
    return NORM_WHITESPACE.sub(" ", cleaned).strip()


def split_artists(value: str) -> list[str]:
    parts = ARTIST_SEPARATORS.split(value)
    artists: list[str] = []
    seen: set[str] = set()
    for part in parts:
        cleaned = part.strip()
        norm = normalize_text(cleaned)
        if norm and norm not in seen:
            seen.add(norm)
            artists.append(cleaned)
    return artists


def comparison_tokens(value: str) -> set[str]:
    norm = normalize_text(value)
    return {token for token in norm.split() if token and token not in FEATURE_TOKENS}


def text_similarity(first: str, second: str) -> float:
    norm_first = normalize_text(first)
    norm_second = normalize_text(second)
    if not norm_first or not norm_second:
        return 0.0
    if norm_first == norm_second:
        return 1.0
    tokens_first = comparison_tokens(first)
    tokens_second = comparison_tokens(second)
    if not tokens_first or not tokens_second:
        return 0.0
    if tokens_first == tokens_second:
        return 1.0
    intersection = tokens_first & tokens_second
    union = tokens_first | tokens_second
    return len(intersection) / len(union)


def primary_artist_matches(ref_artists: Sequence[str], candidate_artists: Sequence[str]) -> bool:
    if not ref_artists or not candidate_artists:
        return False
    ref_norm_primary = normalize_text(ref_artists[0])
    cand_norms = {normalize_text(artist) for artist in candidate_artists if normalize_text(artist)}
    if ref_norm_primary in cand_norms:
        return True
    ref_combined = normalize_text(" ".join(ref_artists))
    cand_combined = normalize_text(" ".join(candidate_artists))
    return bool(ref_combined and ref_combined == cand_combined)


@dataclass(frozen=True)
class TrackQuery:
    isrc: str
    title: str
    artist: str
    album: str = ""
    duration_ms: int = 0


@dataclass(frozen=True)
class MatchOutcome:
    accepted: bool
    score: float
    reason: str


class SelectedTrack(str):
    candidate: Mapping[str, Any]

    def __new__(cls, track_id: str, candidate: Mapping[str, Any]) -> SelectedTrack:
        obj = super().__new__(cls, track_id)
        obj.candidate = candidate
        return obj


class CanvasTrackMatcher:
    @staticmethod
    def _extract_unsafe_terms(text: str) -> set[str]:
        norm = normalize_text(text)
        padded = f" {norm} "
        found = {term for term in MULTI_WORD_UNSAFE_TERMS if f" {term} " in padded}
        tokens = {token for token in norm.split() if token in SINGLE_WORD_UNSAFE_TERMS}
        return found | tokens

    @staticmethod
    def _check_isrc(
        query: TrackQuery,
        cand_isrc: str,
        cand_artists: list[str],
        cand_title: str,
        cand_album: str = "",
        cand_duration_ms: int = 0,
    ) -> MatchOutcome | None:
        exact_isrc = bool(
            query.isrc and cand_isrc and query.isrc == cand_isrc and ISRC_PATTERN.match(query.isrc)
        )
        if exact_isrc:
            if query.artist and cand_artists:
                ref_artists = split_artists(query.artist)
                if not primary_artist_matches(ref_artists, cand_artists):
                    sim = text_similarity(query.title, cand_title)
                    if sim < 0.5:
                        return MatchOutcome(False, 0.0, "isrc_artist_conflict")
            bonus = 0.0
            if query.album and cand_album:
                bonus += text_similarity(query.album, cand_album) * 5.0
            if (
                query.duration_ms > 0
                and cand_duration_ms > 0
                and abs(query.duration_ms - cand_duration_ms) <= 3_000
            ):
                bonus += 2.0
            return MatchOutcome(True, 100.0 + bonus, "exact_isrc")

        if query.isrc and cand_isrc and query.isrc != cand_isrc:
            return MatchOutcome(False, 0.0, "conflicting_isrc")

        return None

    @staticmethod
    def _check_tokens(query: TrackQuery, cand_title: str, cand_album: str) -> MatchOutcome | None:
        ref_text = f"{query.title} {query.album}"
        cand_text = f"{cand_title} {cand_album}"

        ref_unsafe = CanvasTrackMatcher._extract_unsafe_terms(ref_text)
        cand_unsafe = CanvasTrackMatcher._extract_unsafe_terms(cand_text)
        unsafe_diff = cand_unsafe - ref_unsafe
        if unsafe_diff:
            return MatchOutcome(False, 0.0, f"unsafe_terms_{','.join(sorted(unsafe_diff))}")

        ref_tokens = comparison_tokens(ref_text)
        cand_tokens = comparison_tokens(cand_text)
        edition_diff = (cand_tokens & EDITION_TERMS) - ref_tokens
        if edition_diff:
            return MatchOutcome(False, 0.0, f"edition_mismatch_{','.join(sorted(edition_diff))}")

        return None

    @staticmethod
    def _calculate_score(
        query: TrackQuery,
        cand_album: str,
        cand_duration_ms: int,
        title_sim: float,
    ) -> float:
        score = 40.0 + (title_sim * 40.0)
        if query.album and cand_album:
            album_sim = text_similarity(query.album, cand_album)
            score += album_sim * 15.0
        if query.duration_ms > 0 and cand_duration_ms > 0:
            delta_ms = abs(query.duration_ms - cand_duration_ms)
            if delta_ms <= 3_000:
                score += 5.0
            elif delta_ms <= 6_000:
                score += 2.0
        return score

    @staticmethod
    def match_candidate(query: TrackQuery, candidate: Mapping[str, Any]) -> MatchOutcome:
        cand_title = str(candidate.get("name") or "").strip()
        cand_duration_ms = int(candidate.get("duration_ms") or 0)
        cand_artists = [
            str(a.get("name") or "").strip()
            for a in candidate.get("artists", [])
            if isinstance(a, Mapping) and str(a.get("name") or "").strip()
        ]
        cand_album = str(candidate.get("album", {}).get("name") or "").strip()
        cand_isrc = str(candidate.get("external_ids", {}).get("isrc") or "").strip().upper()

        isrc_outcome = CanvasTrackMatcher._check_isrc(
            query, cand_isrc, cand_artists, cand_title, cand_album, cand_duration_ms
        )
        if isrc_outcome is not None:
            return isrc_outcome

        ref_artists = split_artists(query.artist)
        if not primary_artist_matches(ref_artists, cand_artists):
            return MatchOutcome(False, 0.0, "wrong_artist")

        token_outcome = CanvasTrackMatcher._check_tokens(query, cand_title, cand_album)
        if token_outcome is not None:
            return token_outcome

        if query.duration_ms > 0 and cand_duration_ms > 0:
            delta_ms = abs(query.duration_ms - cand_duration_ms)
            if delta_ms > 8_000:
                return MatchOutcome(False, 0.0, f"duration_mismatch_{delta_ms}ms")

        title_sim = text_similarity(query.title, cand_title)
        if title_sim < 0.82:
            return MatchOutcome(False, 0.0, f"title_similarity_low_{title_sim:.2f}")

        score = CanvasTrackMatcher._calculate_score(
            query, cand_album, cand_duration_ms, title_sim
        )
        return MatchOutcome(True, score, "metadata_matched")

    def select_best_candidate(
        self,
        query: TrackQuery,
        candidates: Sequence[Mapping[str, Any]],
    ) -> SelectedTrack | None:
        accepted: list[tuple[str, float, Mapping[str, Any]]] = []
        for candidate in candidates:
            track_id = str(candidate.get("id") or "").strip()
            if not track_id:
                continue
            outcome = self.match_candidate(query, candidate)
            if outcome.accepted and outcome.score >= 75.0:
                accepted.append((track_id, outcome.score, candidate))

        if not accepted:
            return None

        accepted.sort(key=lambda item: item[1], reverse=True)

        if len(accepted) >= 2:
            top_id, top_score, top_cand = accepted[0]
            second_id, second_score, second_cand = accepted[1]
            top_isrc = str(top_cand.get("external_ids", {}).get("isrc") or "").strip().upper()
            second_isrc = str(second_cand.get("external_ids", {}).get("isrc") or "").strip().upper()
            same_isrc_recording = bool(
                query.isrc and top_isrc == query.isrc and second_isrc == query.isrc
            )
            if top_id != second_id and abs(top_score - second_score) < 5.0 and not same_isrc_recording:
                LOGGER.info(
                    "Aggressively rejected ambiguous match for '%s' (top=%.1f second=%.1f)",
                    query.title,
                    top_score,
                    second_score,
                )
                return None

        return SelectedTrack(accepted[0][0], accepted[0][2])


class CacheStorageBackend(Protocol):
    def get(self, key: str) -> tuple[bool, str | None, float]:
        ...

    def put(self, key: str, url: str | None, ttl_seconds: float) -> None:
        ...

    def save_discovery(self, discovery: Mapping[str, Any]) -> None:
        ...

    def get_discoveries(self) -> list[dict[str, Any]]:
        ...

    def close(self) -> None:
        ...


class SQLiteCacheStorage:
    def __init__(self, db_path: str | Path = ":memory:") -> None:
        self._db_path = str(db_path)
        self._lock = threading.Lock()
        self._writes = 0
        if self._db_path != ":memory:":
            Path(self._db_path).parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(
            self._db_path,
            check_same_thread=False,
            isolation_level=None,
        )
        with self._lock:
            cur = self._conn.cursor()
            cur.execute("PRAGMA journal_mode=WAL")
            cur.execute("PRAGMA synchronous=NORMAL")
            cur.execute(
                """
                CREATE TABLE IF NOT EXISTS cache_entries (
                    key TEXT PRIMARY KEY,
                    url TEXT,
                    expires_at REAL NOT NULL
                )
                """
            )
            cur.execute("CREATE INDEX IF NOT EXISTS idx_cache_expires ON cache_entries(expires_at)")
            cur.execute(
                """
                CREATE TABLE IF NOT EXISTS discoveries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    song TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT NOT NULL,
                    url TEXT NOT NULL,
                    scope TEXT NOT NULL,
                    isrc TEXT NOT NULL,
                    discovered_at REAL NOT NULL,
                    UNIQUE(url, isrc, song, artist)
                )
                """
            )

    def get(self, key: str) -> tuple[bool, str | None, float]:
        now = time.time()
        with self._lock:
            cur = self._conn.cursor()
            cur.execute("SELECT url, expires_at FROM cache_entries WHERE key = ?", (key,))
            row = cur.fetchone()
            if row is None:
                return False, None, 0.0
            url, expires_at = row
            remaining = expires_at - now
            if remaining <= 0:
                cur.execute("DELETE FROM cache_entries WHERE key = ?", (key,))
                return False, None, 0.0
            return True, url, remaining

    def put(self, key: str, url: str | None, ttl_seconds: float) -> None:
        now = time.time()
        expires_at = now + ttl_seconds
        with self._lock:
            cur = self._conn.cursor()
            cur.execute(
                "INSERT OR REPLACE INTO cache_entries (key, url, expires_at) VALUES (?, ?, ?)",
                (key, url, expires_at),
            )
            self._writes += 1
            if self._writes % 500 == 0:
                cur.execute("DELETE FROM cache_entries WHERE expires_at < ?", (now,))

    def save_discovery(self, discovery: Mapping[str, Any]) -> None:
        song = str(discovery.get("song") or "").strip()
        artist = str(discovery.get("artist") or "").strip()
        album = str(discovery.get("album") or "").strip()
        url = str(discovery.get("url") or "").strip()
        scope = str(discovery.get("scope") or "track").strip()
        isrc = str(discovery.get("isrc") or "").strip().upper()
        if not (song and artist and url):
            return
        now = time.time()
        with self._lock:
            cur = self._conn.cursor()
            cur.execute(
                """
                INSERT OR IGNORE INTO discoveries
                (song, artist, album, url, scope, isrc, discovered_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (song, artist, album, url, scope, isrc, now),
            )
            cur.execute(
                """
                DELETE FROM discoveries
                WHERE id NOT IN (SELECT id FROM discoveries ORDER BY id DESC LIMIT 10000)
                """
            )

    def get_discoveries(self) -> list[dict[str, Any]]:
        with self._lock:
            cur = self._conn.cursor()
            cur.execute(
                """
                SELECT song, artist, album, url, scope, isrc
                FROM discoveries
                ORDER BY id ASC
                """
            )
            rows = cur.fetchall()
            return [
                {
                    "song": row[0],
                    "artist": row[1],
                    "album": row[2],
                    "url": row[3],
                    "scope": row[4],
                    "isrc": row[5],
                }
                for row in rows
            ]

    def close(self) -> None:
        with self._lock:
            self._conn.close()


class CanvasResolverCache:
    def __init__(
        self,
        max_entries: int = DEFAULT_MAX_CACHE_ENTRIES,
        positive_ttl_seconds: float = DEFAULT_POSITIVE_TTL_SECONDS,
        negative_ttl_seconds: float = DEFAULT_NEGATIVE_TTL_SECONDS,
        storage_backend: CacheStorageBackend | None = None,
    ) -> None:
        self._max_entries = max_entries
        self._positive_ttl = positive_ttl_seconds
        self._negative_ttl = negative_ttl_seconds
        self._storage = storage_backend
        self._lock = threading.RLock()
        self._entries: OrderedDict[str, tuple[str | None, float]] = OrderedDict()
        self._in_flight: dict[str, threading.Event] = {}
        self._in_flight_results: OrderedDict[str, dict[str, Any]] = OrderedDict()
        self._discoveries: list[dict[str, Any]] = []

    def get(self, key: str) -> tuple[bool, str | None]:
        with self._lock:
            entry = self._entries.get(key)
            if entry is not None:
                url, expires_at = entry
                if time.monotonic() > expires_at:
                    self._entries.pop(key, None)
                else:
                    self._entries.move_to_end(key)
                    return True, url

            if self._storage is not None:
                found, url, remaining = self._storage.get(key)
                if found:
                    self._entries[key] = (url, time.monotonic() + remaining)
                    self._entries.move_to_end(key)
                    return True, url

            return False, None

    def put_positive(
        self,
        key: str,
        url: str,
        discovery_item: dict[str, Any] | None = None,
    ) -> None:
        with self._lock:
            expires_at = time.monotonic() + self._positive_ttl
            self._entries[key] = (url, expires_at)
            self._entries.move_to_end(key)
            while len(self._entries) > self._max_entries:
                self._entries.popitem(last=False)

            if self._storage is not None:
                self._storage.put(key, url, self._positive_ttl)

            if discovery_item is not None:
                self._discoveries.append(discovery_item)
                if len(self._discoveries) > 10_000:
                    self._discoveries.pop(0)
                if self._storage is not None:
                    self._storage.save_discovery(discovery_item)

    def put_negative(self, key: str) -> None:
        with self._lock:
            expires_at = time.monotonic() + self._negative_ttl
            self._entries[key] = (None, expires_at)
            self._entries.move_to_end(key)
            while len(self._entries) > self._max_entries:
                self._entries.popitem(last=False)

            if self._storage is not None:
                self._storage.put(key, None, self._negative_ttl)

    def singleflight(
        self,
        key: str,
        resolver_fn: Callable[[], dict[str, Any]],
    ) -> dict[str, Any]:
        event: threading.Event | None = None
        is_leader = False
        with self._lock:
            hit, cached_url = self.get(key)
            if hit:
                if cached_url:
                    return {"status": "resolved", "url": cached_url, "scope": "track", "cached": True}
                return {"status": "miss", "cached": True}

            if key in self._in_flight:
                event = self._in_flight[key]
            else:
                event = threading.Event()
                self._in_flight[key] = event
                is_leader = True

        if not is_leader and event is not None:
            event.wait(timeout=15.0)
            with self._lock:
                hit, cached_url = self.get(key)
                if hit:
                    if cached_url:
                        return {"status": "resolved", "url": cached_url, "scope": "track", "cached": True}
                    return {"status": "miss", "cached": True}
                return self._in_flight_results.get(key, {"status": "miss", "cached": True})

        try:
            result = resolver_fn()
            with self._lock:
                self._in_flight_results[key] = result
                while len(self._in_flight_results) > 500:
                    self._in_flight_results.popitem(last=False)
            return result
        finally:
            with self._lock:
                if event is not None:
                    event.set()
                self._in_flight.pop(key, None)

    def export_discoveries(self) -> list[dict[str, Any]]:
        with self._lock:
            if self._storage is not None:
                return self._storage.get_discoveries()
            return list(self._discoveries)

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()
            self._in_flight.clear()
            self._in_flight_results.clear()
            self._discoveries.clear()


class RateLimiter:
    def __init__(
        self,
        limit_per_minute: int = DEFAULT_RATE_LIMIT_PER_MINUTE,
        max_tracked_ips: int = 10_000,
    ) -> None:
        self._limit = limit_per_minute
        self._max_tracked_ips = max_tracked_ips
        self._lock = threading.Lock()
        self._requests: OrderedDict[str, list[float]] = OrderedDict()

    def is_allowed(self, client_ip: str) -> tuple[bool, int]:
        now = time.monotonic()
        window_start = now - 60.0
        with self._lock:
            timestamps = self._requests.get(client_ip, [])
            valid = [ts for ts in timestamps if ts > window_start]
            if len(valid) >= self._limit:
                self._requests[client_ip] = valid
                self._requests.move_to_end(client_ip)
                retry_after = max(1, int(valid[0] + 60.0 - now))
                return False, retry_after
            valid.append(now)
            self._requests[client_ip] = valid
            self._requests.move_to_end(client_ip)

            if len(self._requests) > self._max_tracked_ips:
                stale = [
                    ip for ip, ts_list in self._requests.items()
                    if not ts_list or ts_list[-1] <= window_start
                ]
                for ip in stale:
                    self._requests.pop(ip, None)
                while len(self._requests) > self._max_tracked_ips:
                    self._requests.popitem(last=False)
            return True, 0


class CanvasResolverService:
    def __init__(
        self,
        client: SpotifyWebClient,
        cache: CanvasResolverCache | None = None,
        matcher: CanvasTrackMatcher | None = None,
    ) -> None:
        self._client = client
        self._cache = cache or CanvasResolverCache()
        self._matcher = matcher or CanvasTrackMatcher()

    @property
    def cache(self) -> CanvasResolverCache:
        return self._cache

    def _validate_request(self, request_data: Mapping[str, Any]) -> TrackQuery:
        raw_isrc = str(request_data.get("isrc") or "").strip().upper()
        if len(raw_isrc) > 32:
            raise ValueError("ISRC exceeds maximum length (32 chars).")
        isrc = raw_isrc if ISRC_PATTERN.match(raw_isrc) else ""

        title = str(request_data.get("title") or "").strip()
        if len(title) > 500:
            raise ValueError("Title exceeds maximum length (500 chars).")

        artist = str(request_data.get("artist") or "").strip()
        if len(artist) > 500:
            raise ValueError("Artist exceeds maximum length (500 chars).")

        album = str(request_data.get("album") or "").strip()
        if len(album) > 500:
            raise ValueError("Album exceeds maximum length (500 chars).")

        duration_ms = max(0, int(request_data.get("durationMs") or 0))
        if duration_ms > 86_400_000:
            raise ValueError("Duration exceeds maximum value (24h).")

        if not isrc and not (title and artist):
            raise ValueError("Request must contain either a valid ISRC or both title and artist.")

        return TrackQuery(
            isrc=isrc,
            title=title,
            artist=artist,
            album=album,
            duration_ms=duration_ms,
        )

    def _cache_key_for(self, query: TrackQuery) -> str:
        if query.isrc:
            return f"isrc:{query.isrc}"
        return (
            f"meta:{normalize_text(query.title)}|"
            f"{normalize_text(query.artist)}|"
            f"{normalize_text(query.album)}|"
            f"{query.duration_ms // 1000}"
        )

    def _find_by_isrc(self, query: TrackQuery) -> tuple[SelectedTrack | None, bool]:
        if not query.isrc:
            return None, True
        try:
            candidates = self._client.search_tracks(f"isrc:{query.isrc}", limit=5)
            selected = self._matcher.select_best_candidate(query, candidates)
            return selected, True
        except AuthenticationError:
            raise
        except (EditorialSourceError, requests.RequestException) as error:
            LOGGER.warning("ISRC search failed: %s", type(error).__name__)
            return None, False

    def _find_by_metadata(self, query: TrackQuery) -> tuple[SelectedTrack | None, bool]:
        if not (query.title and query.artist):
            return None, True
        try:
            search_query = f"{query.title} {query.artist}"
            candidates = self._client.search_tracks(search_query, limit=10)
            selected = self._matcher.select_best_candidate(query, candidates)
            return selected, True
        except AuthenticationError:
            raise
        except (EditorialSourceError, requests.RequestException) as error:
            LOGGER.warning("Metadata search failed: %s", type(error).__name__)
            return None, False

    def _fetch_canvas_url(self, track_id: str) -> tuple[str | None, bool]:
        try:
            canvas_map = self._client.get_canvas_urls([track_id])
            return canvas_map.get(track_id), True
        except AuthenticationError:
            raise
        except (EditorialSourceError, requests.RequestException) as error:
            LOGGER.warning("Canvas lookup failed for track %s: %s", track_id, type(error).__name__)
            return None, False

    def _build_canonical_discovery(
        self,
        matched_cand: Mapping[str, Any] | None,
        query: TrackQuery,
        canvas_url: str,
    ) -> dict[str, Any]:
        canonical_title = str(matched_cand.get("name") or "").strip() if matched_cand else query.title
        canonical_artists = (
            ", ".join(
                str(a.get("name") or "").strip()
                for a in matched_cand.get("artists", [])
                if isinstance(a, Mapping) and str(a.get("name") or "").strip()
            )
            if matched_cand
            else query.artist
        )
        canonical_album = (
            str(matched_cand.get("album", {}).get("name") or "").strip()
            if matched_cand
            else query.album
        )
        canonical_isrc = (
            str(matched_cand.get("external_ids", {}).get("isrc") or "").strip().upper()
            if matched_cand
            else query.isrc
        )
        return {
            "song": canonical_title or query.title or "Unknown",
            "artist": canonical_artists or query.artist or "Unknown",
            "album": canonical_album or query.album or canonical_title or "Unknown",
            "url": canvas_url,
            "scope": "track",
            "isrc": canonical_isrc or query.isrc,
        }

    def _execute_upstream_resolve(self, query: TrackQuery, key: str) -> dict[str, Any]:
        selected, isrc_ok = self._find_by_isrc(query)
        meta_ok = True
        if selected is None and isrc_ok:
            selected, meta_ok = self._find_by_metadata(query)

        if selected is None:
            if isrc_ok and meta_ok:
                self._cache.put_negative(key)
                return {"status": "miss"}
            return {"status": "unavailable", "message": "Upstream lookup temporarily unavailable"}

        track_id = str(selected)
        matched_cand = getattr(selected, "candidate", None)

        canvas_url, canvas_ok = self._fetch_canvas_url(track_id)
        if not canvas_ok:
            return {"status": "unavailable", "message": "Upstream canvas lookup temporarily unavailable"}

        if not canvas_url:
            self._cache.put_negative(key)
            return {"status": "miss"}

        url_problem = _spotify_canvas_url_problem(canvas_url)
        if url_problem is not None:
            LOGGER.warning("Resolved Canvas URL rejected (%s)", url_problem)
            self._cache.put_negative(key)
            return {"status": "miss"}

        discovery_item = self._build_canonical_discovery(matched_cand, query, canvas_url)
        self._cache.put_positive(key, canvas_url, discovery_item)
        return {
            "status": "resolved",
            "url": canvas_url,
            "scope": "track",
            "isrc": discovery_item["isrc"],
            "trackId": track_id,
        }

    def resolve(self, request_data: Mapping[str, Any]) -> dict[str, Any]:
        query = self._validate_request(request_data)
        key = self._cache_key_for(query)
        return self._cache.singleflight(key, lambda: self._execute_upstream_resolve(query, key))


def extract_client_ip(peer_ip: str, headers: Mapping[str, str]) -> str:
    """Extract client IP under the trusted loopback reverse-proxy contract.

    Forwarded headers are only processed when peer_ip is a trusted loopback
    address (127.0.0.1 or ::1). Direct clients cannot spoof their rate-limit
    identity using X-Forwarded-For or X-Real-IP.

    The trusted reverse proxy (Caddy / Nginx) must strictly overwrite
    X-Forwarded-For / X-Real-IP with the connecting client IP rather than
    appending to client-supplied headers.
    """
    try:
        if ipaddress.ip_address(peer_ip).is_loopback:
            forwarded = headers.get("X-Forwarded-For", "").strip()
            if forwarded:
                candidate = forwarded.split(",")[0].strip()
                ipaddress.ip_address(candidate)
                return candidate
            real_ip = headers.get("X-Real-IP", "").strip()
            if real_ip:
                ipaddress.ip_address(real_ip)
                return real_ip
    except ValueError:
        pass
    return peer_ip


class CanvasHttpHandler(BaseHTTPRequestHandler):
    service: CanvasResolverService
    rate_limiter: RateLimiter
    timeout: float | None = 10.0

    def log_message(self, format: str, *args: Any) -> None:
        message = format % args
        sanitized = message.split("?", 1)[0] if "?" in message else message
        LOGGER.info("HTTP %s", sanitized)

    def do_HEAD(self) -> None:
        if self.path in {"/health", "/v1/health"}:
            self.send_response(HTTPStatus.OK)
            self.end_headers()
        else:
            self.send_response(HTTPStatus.NOT_FOUND)
            self.end_headers()

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path in {"/health", "/v1/health"}:
            self._send_json(HTTPStatus.OK, {"status": "ok"})
            return
        self._send_json(HTTPStatus.NOT_FOUND, {"status": "error", "message": "Not found"})

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path not in {"/resolve", "/v1/resolve"}:
            self._send_json(HTTPStatus.NOT_FOUND, {"status": "error", "message": "Not found"})
            return

        client_ip = extract_client_ip(self.client_address[0], self.headers)
        allowed, retry_after = self.rate_limiter.is_allowed(client_ip)
        if not allowed:
            self.send_response(HTTPStatus.TOO_MANY_REQUESTS)
            self.send_header("Retry-After", str(retry_after))
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"error","message":"Rate limit exceeded"}\n')
            return

        content_length_header = self.headers.get("Content-Length")
        if not content_length_header:
            self._send_json(
                HTTPStatus.LENGTH_REQUIRED,
                {"status": "error", "message": "Content-Length required"},
            )
            return

        try:
            content_length = int(content_length_header)
        except ValueError:
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {"status": "error", "message": "Invalid Content-Length"},
            )
            return

        if content_length < 0:
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {"status": "error", "message": "Invalid Content-Length"},
            )
            return

        if content_length > MAX_REQUEST_BODY_BYTES:
            self._send_json(
                HTTPStatus.REQUEST_ENTITY_TOO_LARGE,
                {"status": "error", "message": "Request body exceeds maximum size"},
            )
            return

        try:
            body = self.rfile.read(content_length)
        except TimeoutError:
            self._send_json(
                HTTPStatus.REQUEST_TIMEOUT,
                {"status": "error", "message": "Request body read timed out"},
            )
            self.close_connection = True
            return

        if len(body) < content_length:
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {"status": "error", "message": "Incomplete request body"},
            )
            self.close_connection = True
            return
        try:
            data = json.loads(body.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {"status": "error", "message": "Malformed JSON body"},
            )
            return

        if not isinstance(data, dict):
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {"status": "error", "message": "JSON body must be an object"},
            )
            return

        self._handle_resolution(data)

    def _handle_resolution(self, request_data: Mapping[str, Any]) -> None:
        try:
            result = self.service.resolve(request_data)
            if result.get("status") == "unavailable":
                self._send_json(HTTPStatus.SERVICE_UNAVAILABLE, result)
            else:
                self._send_json(HTTPStatus.OK, result)
        except ValueError as error:
            self._send_json(HTTPStatus.BAD_REQUEST, {"status": "error", "message": str(error)})
        except AuthenticationError:
            LOGGER.error("Spotify authentication failure during on-demand resolution.")
            self._send_json(
                HTTPStatus.SERVICE_UNAVAILABLE,
                {"status": "error", "message": "Upstream authentication unavailable"},
            )
        except Exception as error:
            LOGGER.error("Unexpected error during on-demand resolution: %s", type(error).__name__)
            self._send_json(
                HTTPStatus.INTERNAL_SERVER_ERROR,
                {"status": "error", "message": "Internal error"},
            )

    def _send_json(self, status: int, payload: Mapping[str, Any]) -> None:
        body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)


def create_resolver_server(
    service: CanvasResolverService,
    host: str = "127.0.0.1",
    port: int = 8080,
    rate_limit: int = DEFAULT_RATE_LIMIT_PER_MINUTE,
) -> ThreadingHTTPServer:
    rate_limiter = RateLimiter(rate_limit)

    class CustomHandler(CanvasHttpHandler):
        pass

    CustomHandler.service = service
    CustomHandler.rate_limiter = rate_limiter

    return ThreadingHTTPServer((host, port), CustomHandler)


def main() -> None:
    parser = argparse.ArgumentParser(description="Levyra on-demand Spotify Canvas resolver")
    parser.add_argument("--host", default=os.environ.get("LEVYRA_RESOLVER_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("LEVYRA_RESOLVER_PORT", "8080")))
    parser.add_argument("--cache-db", default=os.environ.get("LEVYRA_RESOLVER_CACHE_DB", "canvas_cache.db"))
    parser.add_argument(
        "--rate-limit",
        type=int,
        default=int(os.environ.get("LEVYRA_RESOLVER_RATE_LIMIT", str(DEFAULT_RATE_LIMIT_PER_MINUTE))),
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    sp_dc = os.environ.get("LEVYRA_EDITORIAL_SP_DC")
    if not sp_dc:
        LOGGER.error("LEVYRA_EDITORIAL_SP_DC environment variable is required.")
        sys.exit(1)

    storage = SQLiteCacheStorage(args.cache_db)
    cache = CanvasResolverCache(storage_backend=storage)
    client = SpotifyWebClient(sp_dc)
    service = CanvasResolverService(client=client, cache=cache)
    server = create_resolver_server(service, host=args.host, port=args.port, rate_limit=args.rate_limit)

    LOGGER.info("Starting Levyra Canvas resolver on %s:%d (cache=%s)", args.host, args.port, args.cache_db)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        LOGGER.info("Shutting down Canvas resolver.")
    finally:
        server.server_close()
        client.close()
        storage.close()


if __name__ == "__main__":
    main()
