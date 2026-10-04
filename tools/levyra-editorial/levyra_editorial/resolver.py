from __future__ import annotations

import json
import logging
import re
import threading
import time
from collections import OrderedDict
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import parse_qs, urlparse

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

UNSAFE_TERMS = frozenset({
    "karaoke",
    "tribute",
    "cover",
    "nightcore",
    "sped up",
    "slowed",
    "instrumental",
    "dj mix",
    "playlist",
    "essentials",
    "set list",
    "session",
})

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

DEFAULT_POSITIVE_TTL_SECONDS = 24 * 3600  # 24 hours
DEFAULT_NEGATIVE_TTL_SECONDS = 600  # 10 minutes
MAX_REQUEST_BODY_BYTES = 16 * 1024  # 16 KB
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


class CanvasTrackMatcher:
    """Conservative Spotify track candidate matcher."""

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

        # 1. Exact ISRC check
        exact_isrc = bool(
            query.isrc and cand_isrc and query.isrc == cand_isrc and ISRC_PATTERN.match(query.isrc)
        )
        if exact_isrc:
            # Safeguard: verify artist compatibility or moderate title similarity
            # to prevent corrupted catalog hits.
            if query.artist and cand_artists:
                ref_artists = split_artists(query.artist)
                if not primary_artist_matches(ref_artists, cand_artists):
                    sim = text_similarity(query.title, cand_title)
                    if sim < 0.5:
                        return MatchOutcome(False, 0.0, "isrc_artist_conflict")
            return MatchOutcome(True, 100.0, "exact_isrc")

        # Conflicting ISRC
        if query.isrc and cand_isrc and query.isrc != cand_isrc:
            return MatchOutcome(False, 0.0, "conflicting_isrc")

        # 2. Artist check
        ref_artists = split_artists(query.artist)
        if not primary_artist_matches(ref_artists, cand_artists):
            return MatchOutcome(False, 0.0, "wrong_artist")

        # 3. Unsafe terms check
        ref_tokens = comparison_tokens(f"{query.title} {query.album}")
        cand_tokens = comparison_tokens(f"{cand_title} {cand_album}")
        unsafe_diff = (cand_tokens & UNSAFE_TERMS) - ref_tokens
        if unsafe_diff:
            return MatchOutcome(False, 0.0, f"unsafe_terms_{','.join(sorted(unsafe_diff))}")

        # 4. Edition / version check
        edition_diff = (cand_tokens & EDITION_TERMS) - ref_tokens
        if edition_diff:
            return MatchOutcome(False, 0.0, f"edition_mismatch_{','.join(sorted(edition_diff))}")

        # 5. Duration mismatch check
        if query.duration_ms > 0 and cand_duration_ms > 0:
            delta_ms = abs(query.duration_ms - cand_duration_ms)
            if delta_ms > 8_000:
                return MatchOutcome(False, 0.0, f"duration_mismatch_{delta_ms}ms")

        # 6. Title similarity check
        title_sim = text_similarity(query.title, cand_title)
        if title_sim < 0.82:
            return MatchOutcome(False, 0.0, f"title_similarity_low_{title_sim:.2f}")

        # 7. Score calculation
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

        return MatchOutcome(True, score, "metadata_matched")

    def select_best_candidate(
        self,
        query: TrackQuery,
        candidates: Sequence[Mapping[str, Any]],
    ) -> str | None:
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

        # Sort by score descending
        accepted.sort(key=lambda item: item[1], reverse=True)

        # Ambiguity check: if top 2 candidates have distinct track IDs with very close scores, reject!
        if len(accepted) >= 2:
            top_id, top_score, _ = accepted[0]
            second_id, second_score, _ = accepted[1]
            if top_id != second_id and abs(top_score - second_score) < 5.0:
                LOGGER.info(
                    "Aggressively rejected ambiguous match for '%s' (top=%.1f second=%.1f)",
                    query.title,
                    top_score,
                    second_score,
                )
                return None

        return accepted[0][0]


class CanvasResolverCache:
    """Bounded, thread-safe cache with singleflight request deduplication and discovery export."""

    def __init__(
        self,
        max_entries: int = DEFAULT_MAX_CACHE_ENTRIES,
        positive_ttl_seconds: float = DEFAULT_POSITIVE_TTL_SECONDS,
        negative_ttl_seconds: float = DEFAULT_NEGATIVE_TTL_SECONDS,
    ) -> None:
        self._max_entries = max_entries
        self._positive_ttl = positive_ttl_seconds
        self._negative_ttl = negative_ttl_seconds
        self._lock = threading.RLock()
        self._entries: OrderedDict[str, tuple[str | None, float]] = OrderedDict()
        self._in_flight: dict[str, threading.Event] = {}
        self._in_flight_results: OrderedDict[str, dict[str, Any]] = OrderedDict()
        self._discoveries: list[dict[str, Any]] = []

    def get(self, key: str) -> tuple[bool, str | None]:
        with self._lock:
            entry = self._entries.get(key)
            if entry is None:
                return False, None
            url, expires_at = entry
            if time.monotonic() > expires_at:
                self._entries.pop(key, None)
                return False, None
            # Move to end (LRU)
            self._entries.move_to_end(key)
            return True, url

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
            if discovery_item is not None:
                self._discoveries.append(discovery_item)
                if len(self._discoveries) > 10_000:
                    self._discoveries.pop(0)

    def put_negative(self, key: str) -> None:
        with self._lock:
            expires_at = time.monotonic() + self._negative_ttl
            self._entries[key] = (None, expires_at)
            self._entries.move_to_end(key)
            while len(self._entries) > self._max_entries:
                self._entries.popitem(last=False)

    def singleflight(
        self,
        key: str,
        resolver_fn: Callable[[], dict[str, Any]],
    ) -> dict[str, Any]:
        """Deduplicate concurrent identical lookups to prevent stampedes."""
        event: threading.Event | None = None
        is_leader = False
        with self._lock:
            # Check cache first
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
            return list(self._discoveries)

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()
            self._in_flight.clear()
            self._in_flight_results.clear()
            self._discoveries.clear()


class RateLimiter:
    """Sliding-window per-IP rate limiter."""

    def __init__(self, limit_per_minute: int = DEFAULT_RATE_LIMIT_PER_MINUTE) -> None:
        self._limit = limit_per_minute
        self._lock = threading.Lock()
        self._requests: dict[str, list[float]] = {}

    def is_allowed(self, client_ip: str) -> tuple[bool, int]:
        now = time.monotonic()
        window_start = now - 60.0
        with self._lock:
            timestamps = self._requests.setdefault(client_ip, [])
            # Prune stale timestamps
            valid = [ts for ts in timestamps if ts > window_start]
            self._requests[client_ip] = valid
            if len(valid) >= self._limit:
                retry_after = max(1, int(valid[0] + 60.0 - now))
                return False, retry_after
            valid.append(now)
            # Periodic cleanup of empty keys
            if len(self._requests) > 5_000:
                self._requests = {k: v for k, v in self._requests.items() if v}
            return True, 0


class CanvasResolverService:
    """Repository-owned on-demand Spotify Canvas resolver service."""

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

    def resolve(self, request_data: Mapping[str, Any]) -> dict[str, Any]:
        raw_isrc = str(request_data.get("isrc") or "").strip().upper()
        isrc = raw_isrc if ISRC_PATTERN.match(raw_isrc) else ""
        title = str(request_data.get("title") or "").strip()
        artist = str(request_data.get("artist") or "").strip()
        album = str(request_data.get("album") or "").strip()
        duration_ms = max(0, int(request_data.get("durationMs") or 0))

        if not isrc and not (title and artist):
            raise ValueError("Request must contain either a valid ISRC or both title and artist.")

        query = TrackQuery(
            isrc=isrc,
            title=title,
            artist=artist,
            album=album,
            duration_ms=duration_ms,
        )

        key = (
            f"isrc:{isrc}"
            if isrc
            else f"meta:{normalize_text(title)}|{normalize_text(artist)}|{duration_ms // 1000}"
        )

        def _do_resolve() -> dict[str, Any]:
            track_id: str | None = None
            # 1. Prefer ISRC matching
            if isrc:
                try:
                    candidates = self._client.search_tracks(f"isrc:{isrc}", limit=5)
                    track_id = self._matcher.select_best_candidate(query, candidates)
                except AuthenticationError:
                    raise
                except (EditorialSourceError, requests.RequestException) as error:
                    LOGGER.warning("ISRC search failed: %s", type(error).__name__)

            # 2. Conservative metadata search fallback
            if not track_id and title and artist:
                try:
                    search_query = f"{title} {artist}"
                    candidates = self._client.search_tracks(search_query, limit=10)
                    track_id = self._matcher.select_best_candidate(query, candidates)
                except AuthenticationError:
                    raise
                except (EditorialSourceError, requests.RequestException) as error:
                    LOGGER.warning("Metadata search failed: %s", type(error).__name__)

            if not track_id:
                self._cache.put_negative(key)
                return {"status": "miss"}

            # 3. Canvas lookup using PR #833 resolver (primary -> PaxSenix fallback)
            try:
                canvas_map = self._client.get_canvas_urls([track_id])
            except AuthenticationError:
                raise
            except (EditorialSourceError, requests.RequestException) as error:
                LOGGER.warning("Canvas lookup failed for track %s: %s", track_id, type(error).__name__)
                self._cache.put_negative(key)
                return {"status": "miss"}

            canvas_url = canvas_map.get(track_id)
            if not canvas_url:
                self._cache.put_negative(key)
                return {"status": "miss"}

            # 4. Strict URL validation
            url_problem = _spotify_canvas_url_problem(canvas_url)
            if url_problem is not None:
                LOGGER.warning("Resolved Canvas URL rejected (%s)", url_problem)
                self._cache.put_negative(key)
                return {"status": "miss"}

            discovery_item = {
                "song": title or "Unknown",
                "artist": artist or "Unknown",
                "album": album or title or "Unknown",
                "url": canvas_url,
                "scope": "track",
                "isrc": isrc,
            }
            self._cache.put_positive(key, canvas_url, discovery_item)
            return {
                "status": "resolved",
                "url": canvas_url,
                "scope": "track",
                "isrc": isrc,
                "trackId": track_id,
            }

        return self._cache.singleflight(key, _do_resolve)


class CanvasHttpHandler(BaseHTTPRequestHandler):
    """HTTP handler for the on-demand Canvas resolver."""

    service: CanvasResolverService
    rate_limiter: RateLimiter

    def log_message(self, format: str, *args: Any) -> None:
        # Sanitize logs: never log sensitive headers, cookies or tokens
        LOGGER.info("%s - %s", self.client_address[0], format % args)

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

        if parsed.path in {"/resolve", "/v1/resolve"}:
            client_ip = self.client_address[0]
            allowed, retry_after = self.rate_limiter.is_allowed(client_ip)
            if not allowed:
                self.send_response(HTTPStatus.TOO_MANY_REQUESTS)
                self.send_header("Retry-After", str(retry_after))
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(b'{"status":"error","message":"Rate limit exceeded"}\n')
                return

            params = parse_qs(parsed.query)
            request_data = {
                "isrc": params.get("isrc", [""])[0],
                "title": params.get("title", [""])[0],
                "artist": params.get("artist", [""])[0],
                "album": params.get("album", [""])[0],
                "durationMs": params.get("durationMs", ["0"])[0],
            }
            self._handle_resolution(request_data)
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"status": "error", "message": "Not found"})

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path not in {"/resolve", "/v1/resolve"}:
            self._send_json(HTTPStatus.NOT_FOUND, {"status": "error", "message": "Not found"})
            return

        client_ip = self.client_address[0]
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

        if content_length > MAX_REQUEST_BODY_BYTES:
            self._send_json(
                HTTPStatus.REQUEST_ENTITY_TOO_LARGE,
                {"status": "error", "message": "Request body exceeds maximum size"},
            )
            return

        body = self.rfile.read(content_length)
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
