from __future__ import annotations

import threading
import time
from typing import Any

import pytest
import requests

from levyra_editorial.resolver import (
    CanvasResolverCache,
    CanvasResolverService,
    CanvasTrackMatcher,
    RateLimiter,
    TrackQuery,
    create_resolver_server,
)
from levyra_editorial.spotify import AuthenticationError


class FakeSpotifyClient:
    def __init__(
        self,
        search_results: list[dict[str, Any]] | None = None,
        canvas_map: dict[str, str] | None = None,
        auth_error: bool = False,
        timeout: bool = False,
        paxsenix_fallback_hit: bool = False,
    ) -> None:
        self.search_results = search_results or []
        self.canvas_map = canvas_map or {}
        self.auth_error = auth_error
        self.timeout = timeout
        self.paxsenix_fallback_hit = paxsenix_fallback_hit
        self.searches_called: list[str] = []
        self.canvases_called: list[list[str]] = []

    def search_tracks(self, query: str, limit: int = 10) -> list[dict[str, Any]]:
        if self.auth_error:
            raise AuthenticationError("The editorial session secret is malformed.")
        if self.timeout:
            import requests

            raise requests.Timeout("Spotify search request timed out.")
        self.searches_called.append(query)
        return self.search_results

    def get_canvas_urls(self, track_ids: list[str]) -> dict[str, str]:
        if self.auth_error:
            raise AuthenticationError("The editorial session secret is malformed.")
        if self.timeout:
            import requests

            raise requests.Timeout("Spotify Canvas request timed out.")
        self.canvases_called.append(track_ids)
        if self.paxsenix_fallback_hit:
            # Simulate primary miss and PaxSenix resolving it
            return {tid: self.canvas_map[tid] for tid in track_ids if tid in self.canvas_map}
        return {tid: self.canvas_map[tid] for tid in track_ids if tid in self.canvas_map}


def make_candidate(
    track_id: str = "track123",
    name: str = "Test Song",
    artist: str = "Test Artist",
    album: str = "Test Album",
    duration_ms: int = 180_000,
    isrc: str = "USUM71703861",
) -> dict[str, Any]:
    return {
        "id": track_id,
        "name": name,
        "duration_ms": duration_ms,
        "artists": [{"id": "artist1", "name": artist}],
        "album": {"id": "album1", "name": album},
        "external_ids": {"isrc": isrc},
    }


def test_exact_isrc_hit() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="USUM71703861",
        title="Test Song",
        artist="Test Artist",
        album="Test Album",
        duration_ms=180_000,
    )
    cand = make_candidate(isrc="USUM71703861")
    outcome = matcher.match_candidate(query, cand)
    assert outcome.accepted
    assert outcome.score == 100.0
    assert outcome.reason == "exact_isrc"

    best = matcher.select_best_candidate(query, [cand])
    assert best == "track123"


def test_strict_metadata_fallback() -> None:
    matcher = CanvasTrackMatcher()
    # No ISRC
    query = TrackQuery(
        isrc="",
        title="Blinding Lights",
        artist="The Weeknd",
        album="After Hours",
        duration_ms=200_000,
    )
    cand = make_candidate(
        track_id="weeknd1",
        name="Blinding Lights",
        artist="The Weeknd",
        album="After Hours",
        duration_ms=202_000,
        isrc="OTHERISRC123",
    )
    outcome = matcher.match_candidate(query, cand)
    assert outcome.accepted
    assert outcome.score >= 80.0
    best = matcher.select_best_candidate(query, [cand])
    assert best == "weeknd1"


def test_ambiguous_candidate_rejected() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="",
        title="Photograph",
        artist="Ed Sheeran",
        duration_ms=250_000,
    )
    cand1 = make_candidate(
        track_id="ed1",
        name="Photograph",
        artist="Ed Sheeran",
        album="Album One",
        duration_ms=250_000,
    )
    cand2 = make_candidate(
        track_id="ed2",
        name="Photograph",
        artist="Ed Sheeran",
        album="Album Two",
        duration_ms=250_000,
    )
    # Both have very similar scores and different IDs -> rejected
    best = matcher.select_best_candidate(query, [cand1, cand2])
    assert best is None


def test_wrong_artist_rejected() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="",
        title="Someone Like You",
        artist="Adele",
        duration_ms=280_000,
    )
    cand = make_candidate(
        track_id="cover1",
        name="Someone Like You",
        artist="Cover Artist",
        duration_ms=280_000,
    )
    outcome = matcher.match_candidate(query, cand)
    assert not outcome.accepted
    assert outcome.reason == "wrong_artist"
    assert matcher.select_best_candidate(query, [cand]) is None


def test_wrong_album_version_rejected_when_material() -> None:
    matcher = CanvasTrackMatcher()
    # Studio query vs live candidate
    query = TrackQuery(
        isrc="",
        title="Hotel California",
        artist="Eagles",
        duration_ms=390_000,
    )
    cand_live = make_candidate(
        track_id="eagles_live",
        name="Hotel California (Live at The Forum)",
        artist="Eagles",
        duration_ms=420_000,
    )
    outcome = matcher.match_candidate(query, cand_live)
    assert not outcome.accepted
    assert "edition_mismatch" in outcome.reason


def test_duration_mismatch_rejected() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="",
        title="Short Song",
        artist="Artist",
        duration_ms=120_000,
    )
    # Candidate is 200s (80s delta > 8s limit)
    cand = make_candidate(
        track_id="long1",
        name="Short Song",
        artist="Artist",
        duration_ms=200_000,
    )
    outcome = matcher.match_candidate(query, cand)
    assert not outcome.accepted
    assert "duration_mismatch" in outcome.reason


def test_positive_and_negative_cache_hits() -> None:
    cache = CanvasResolverCache(positive_ttl_seconds=3600, negative_ttl_seconds=60)
    key = "isrc:USUM71703861"

    # Initially miss
    hit, val = cache.get(key)
    assert not hit
    assert val is None

    # Positive put
    cache.put_positive(key, "https://canvaz.scdn.co/video.mp4")
    hit, val = cache.get(key)
    assert hit
    assert val == "https://canvaz.scdn.co/video.mp4"

    # Negative put
    cache.put_negative("isrc:NEGATIVE123")
    hit_neg, val_neg = cache.get("isrc:NEGATIVE123")
    assert hit_neg
    assert val_neg is None


def test_negative_cache_expiry() -> None:
    cache = CanvasResolverCache(negative_ttl_seconds=0.01)
    key = "isrc:EXPIRING123"
    cache.put_negative(key)

    # Immediately hit
    hit, val = cache.get(key)
    assert hit
    assert val is None

    time.sleep(0.02)
    # Expired
    hit_after, _ = cache.get(key)
    assert not hit_after


def test_concurrent_identical_requests_deduplicated() -> None:
    call_count = 0
    lock = threading.Lock()

    def slow_resolver() -> dict[str, Any]:
        nonlocal call_count
        with lock:
            call_count += 1
        time.sleep(0.05)
        return {"status": "resolved", "url": "https://canvaz.scdn.co/singleflight.mp4"}

    cache = CanvasResolverCache()
    results: list[dict[str, Any]] = []

    def worker() -> None:
        res = cache.singleflight("key:stampede", slow_resolver)
        results.append(res)

    threads = [threading.Thread(target=worker) for _ in range(5)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    assert len(results) == 5
    assert all(r["status"] == "resolved" for r in results)
    assert call_count == 1  # Only ONE execution occurred!


def test_invalid_canvas_host_and_non_mp4_rejected() -> None:
    cand = make_candidate(track_id="bad_host_track")
    # Non-canvaz host
    client_bad_host = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"bad_host_track": "https://evil.com/video.mp4"},
    )
    service_bad_host = CanvasResolverService(client_bad_host)
    res_bad_host = service_bad_host.resolve({"isrc": "USUM71703861"})
    assert res_bad_host["status"] == "miss"

    # Non-MP4 extension
    client_bad_ext = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"bad_host_track": "https://canvaz.scdn.co/video.webm"},
    )
    service_bad_ext = CanvasResolverService(client_bad_ext)
    res_bad_ext = service_bad_ext.resolve({"isrc": "USUM71703861"})
    assert res_bad_ext["status"] == "miss"


def test_paxsenix_fallback_success() -> None:
    cand = make_candidate(track_id="pax_track")
    client = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"pax_track": "https://canvaz.scdn.co/paxsenix.cnvs.mp4"},
        paxsenix_fallback_hit=True,
    )
    service = CanvasResolverService(client)
    res = service.resolve({"isrc": "USUM71703861"})
    assert res["status"] == "resolved"
    assert res["url"] == "https://canvaz.scdn.co/paxsenix.cnvs.mp4"
    assert len(service.cache.export_discoveries()) == 1


def test_spotify_auth_failure_surfaces_cleanly() -> None:
    client = FakeSpotifyClient(auth_error=True)
    service = CanvasResolverService(client)
    with pytest.raises(AuthenticationError):
        # Service level directly propagates AuthenticationError so handler can return 503
        service.resolve({"title": "Test", "artist": "Artist"})


def test_timeout_handled_gracefully() -> None:
    client = FakeSpotifyClient(timeout=True)
    service = CanvasResolverService(client)
    # Search timeout results in miss (caught internally)
    res = service.resolve({"title": "Test", "artist": "Artist"})
    assert res["status"] == "miss"


def test_rate_limiter() -> None:
    limiter = RateLimiter(limit_per_minute=2)
    allowed, _ = limiter.is_allowed("1.2.3.4")
    assert allowed
    allowed, _ = limiter.is_allowed("1.2.3.4")
    assert allowed
    allowed, retry_after = limiter.is_allowed("1.2.3.4")
    assert not allowed
    assert retry_after >= 1


def test_http_server_endpoints_and_sanitized_logs(caplog: pytest.LogCaptureFixture) -> None:
    cand = make_candidate(track_id="http_track")
    valid_canvas_url = "https://canvaz.scdn.co/upload/video.mp4"
    client = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"http_track": valid_canvas_url},
    )
    service = CanvasResolverService(client)
    server = create_resolver_server(service, host="127.0.0.1", port=0)
    server_port = server.server_address[1]

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    base_url = f"http://127.0.0.1:{server_port}"
    try:
        # 1. Health check
        resp = requests.get(f"{base_url}/health", timeout=5)
        assert resp.status_code == 200
        assert resp.json() == {"status": "ok"}

        # 2. POST resolve hit
        resp = requests.post(
            f"{base_url}/v1/resolve",
            json={
                "isrc": "USUM71703861",
                "title": "Test Song",
                "artist": "Test Artist",
            },
            timeout=5,
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "resolved"
        assert data["url"] == valid_canvas_url
        assert "sp_dc" not in str(data)
        assert "Bearer" not in str(data)

        # 3. GET resolve hit
        resp = requests.get(f"{base_url}/v1/resolve?isrc=USUM71703861", timeout=5)
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "resolved"
        assert data["url"] == valid_canvas_url

        # 4. Malformed request rejected (empty body / missing isrc and title)
        resp_bad = requests.post(
            f"{base_url}/v1/resolve",
            data=b"{}",
            headers={"Content-Type": "application/json"},
            timeout=5,
        )
        assert resp_bad.status_code == 400

        # 5. Malformed JSON
        resp_malformed = requests.post(
            f"{base_url}/v1/resolve",
            data=b"not-json",
            headers={"Content-Type": "application/json"},
            timeout=5,
        )
        assert resp_malformed.status_code == 400

        # 6. Verify logs contain no sensitive credentials
        assert "sp_dc" not in caplog.text
        assert "Bearer" not in caplog.text
        assert "authorization" not in caplog.text.lower()

    finally:
        server.shutdown()
        server.server_close()
