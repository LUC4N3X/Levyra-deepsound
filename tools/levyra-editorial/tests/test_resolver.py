from __future__ import annotations

import socket
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
    SQLiteCacheStorage,
    TrackQuery,
    create_resolver_server,
    extract_client_ip,
)
from levyra_editorial.spotify import (
    DEFAULT_SEARCH_QUERY_HASH,
    AuthenticationError,
    SourceApiError,
    SpotifyWebClient,
    validate_search_query_hash,
)


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
    assert outcome.score >= 100.0
    assert outcome.reason == "exact_isrc"

    best = matcher.select_best_candidate(query, [cand])
    assert best == "track123"


def test_exact_isrc_multiple_candidates_same_isrc_not_ambiguous() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="USUM71703861",
        title="Test Song",
        artist="Test Artist",
        album="Original Album",
        duration_ms=180_000,
    )
    cand_album = make_candidate(
        track_id="track_album",
        name="Test Song",
        artist="Test Artist",
        album="Original Album",
        isrc="USUM71703861",
    )
    cand_comp = make_candidate(
        track_id="track_compilation",
        name="Test Song",
        artist="Test Artist",
        album="Greatest Hits 2024",
        isrc="USUM71703861",
    )
    best = matcher.select_best_candidate(query, [cand_album, cand_comp])
    assert best is not None
    assert best == "track_album"


def test_ambiguous_candidate_rejected_for_different_recordings() -> None:
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
        isrc="ISRC11111111",
    )
    cand2 = make_candidate(
        track_id="ed2",
        name="Photograph",
        artist="Ed Sheeran",
        album="Album Two",
        duration_ms=250_000,
        isrc="ISRC22222222",
    )
    best = matcher.select_best_candidate(query, [cand1, cand2])
    assert best is None


def test_strict_metadata_fallback() -> None:
    matcher = CanvasTrackMatcher()
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


def test_multi_word_unsafe_terms_rejected() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="",
        title="Original Track",
        artist="Artist",
        duration_ms=180_000,
    )
    for term in ["Sped Up", "DJ Mix", "Set List"]:
        cand = make_candidate(
            track_id="bad_edition",
            name=f"Original Track ({term})",
            artist="Artist",
            duration_ms=180_000,
        )
        outcome = matcher.match_candidate(query, cand)
        assert not outcome.accepted
        assert outcome.reason.startswith("unsafe_terms_")


def test_duration_mismatch_rejected() -> None:
    matcher = CanvasTrackMatcher()
    query = TrackQuery(
        isrc="",
        title="Short Song",
        artist="Artist",
        duration_ms=120_000,
    )
    cand = make_candidate(
        track_id="long1",
        name="Short Song",
        artist="Artist",
        duration_ms=200_000,
    )
    outcome = matcher.match_candidate(query, cand)
    assert not outcome.accepted
    assert "duration_mismatch" in outcome.reason


def test_sqlite_cache_storage_persistence(tmp_path: Any) -> None:
    db_file = str(tmp_path / "canvas_cache.db")
    storage = SQLiteCacheStorage(db_file)
    storage.put("positive_key", "https://canvaz.scdn.co/test.mp4", ttl_seconds=3600)
    storage.put("negative_key", None, ttl_seconds=60)
    storage.save_discovery(
        {
            "song": "Song A",
            "artist": "Artist A",
            "album": "Album A",
            "url": "https://canvaz.scdn.co/test.mp4",
            "scope": "track",
            "isrc": "USUM71703861",
        }
    )

    hit, val, _ = storage.get("positive_key")
    assert hit
    assert val == "https://canvaz.scdn.co/test.mp4"

    hit_neg, val_neg, _ = storage.get("negative_key")
    assert hit_neg
    assert val_neg is None

    storage_reopened = SQLiteCacheStorage(db_file)
    hit_reopened, val_reopened, _ = storage_reopened.get("positive_key")
    assert hit_reopened
    assert val_reopened == "https://canvaz.scdn.co/test.mp4"

    discoveries = storage_reopened.get_discoveries()
    assert len(discoveries) == 1
    assert discoveries[0]["song"] == "Song A"
    assert discoveries[0]["isrc"] == "USUM71703861"
    storage.close()
    storage_reopened.close()


def test_canvas_resolver_cache_with_storage(tmp_path: Any) -> None:
    db_file = str(tmp_path / "cache_l2.db")
    storage = SQLiteCacheStorage(db_file)
    cache = CanvasResolverCache(
        storage_backend=storage,
        positive_ttl_seconds=3600,
        negative_ttl_seconds=60,
    )

    key = "isrc:USUM71703861"
    hit, val = cache.get(key)
    assert not hit
    assert val is None

    cache.put_positive(key, "https://canvaz.scdn.co/video.mp4")
    hit, val = cache.get(key)
    assert hit
    assert val == "https://canvaz.scdn.co/video.mp4"

    storage2 = SQLiteCacheStorage(db_file)
    new_cache = CanvasResolverCache(
        storage_backend=storage2,
        positive_ttl_seconds=3600,
        negative_ttl_seconds=60,
    )
    hit2, val2 = new_cache.get(key)
    assert hit2
    assert val2 == "https://canvaz.scdn.co/video.mp4"
    storage.close()
    storage2.close()


def test_negative_cache_expiry() -> None:
    cache = CanvasResolverCache(negative_ttl_seconds=0.01)
    key = "isrc:EXPIRING123"
    cache.put_negative(key)

    hit, val = cache.get(key)
    assert hit
    assert val is None

    time.sleep(0.02)
    hit_after, _ = cache.get(key)
    assert not hit_after


def test_discovery_row_uses_canonical_spotify_metadata() -> None:
    cand = make_candidate(
        track_id="canon123",
        name="Canonical Song",
        artist="Canonical Artist",
        album="Canonical Studio Album",
        isrc="USUM71703861",
    )
    client = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"canon123": "https://canvaz.scdn.co/canonical.mp4"},
    )
    service = CanvasResolverService(client)
    res = service.resolve(
        {
            "isrc": "USUM71703861",
            "title": "Canonical Song",
            "artist": "Canonical Artist",
            "album": "Messy Untrusted Album Extra",
        }
    )
    assert res["status"] == "resolved"
    discoveries = service.cache.export_discoveries()
    assert len(discoveries) == 1
    row = discoveries[0]
    assert row["song"] == "Canonical Song"
    assert row["artist"] == "Canonical Artist"
    assert row["album"] == "Canonical Studio Album"
    assert row["isrc"] == "USUM71703861"
    assert row["url"] == "https://canvaz.scdn.co/canonical.mp4"


def test_conclusive_miss_negative_cached_transient_error_not_cached() -> None:
    client_empty = FakeSpotifyClient(search_results=[])
    service_empty = CanvasResolverService(client_empty)
    query_payload = {"title": "Unknown Song", "artist": "Unknown Artist"}
    res_miss = service_empty.resolve(query_payload)
    assert res_miss["status"] == "miss"

    key = service_empty._cache_key_for(service_empty._validate_request(query_payload))
    hit, val = service_empty.cache.get(key)
    assert hit
    assert val is None

    client_timeout = FakeSpotifyClient(timeout=True)
    service_timeout = CanvasResolverService(client_timeout)
    res_timeout = service_timeout.resolve(query_payload)
    assert res_timeout["status"] == "unavailable"

    hit_timeout, _ = service_timeout.cache.get(key)
    assert not hit_timeout


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
    assert call_count == 1


def test_invalid_canvas_host_and_non_mp4_rejected() -> None:
    cand = make_candidate(track_id="bad_host_track")
    client_bad_host = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"bad_host_track": "https://evil.com/video.mp4"},
    )
    service_bad_host = CanvasResolverService(client_bad_host)
    res_bad_host = service_bad_host.resolve({"isrc": "USUM71703861"})
    assert res_bad_host["status"] == "miss"

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
        service.resolve({"title": "Test", "artist": "Artist"})


def test_rate_limiter_stale_cleanup_and_capacity_cap() -> None:
    limiter = RateLimiter(limit_per_minute=2, max_tracked_ips=3)
    allowed, _ = limiter.is_allowed("1.1.1.1")
    assert allowed
    allowed, _ = limiter.is_allowed("1.1.1.1")
    assert allowed
    allowed, retry_after = limiter.is_allowed("1.1.1.1")
    assert not allowed
    assert retry_after >= 1

    limiter.is_allowed("2.2.2.2")
    limiter.is_allowed("3.3.3.3")
    limiter.is_allowed("4.4.4.4")
    assert len(limiter._requests) <= 3


def test_http_server_endpoints_and_sanitized_logs(caplog: pytest.LogCaptureFixture) -> None:
    cand = make_candidate(track_id="http_track")
    valid_canvas_url = "https://canvaz.scdn.co/upload/video.mp4"
    client = FakeSpotifyClient(
        search_results=[cand],
        canvas_map={"http_track": valid_canvas_url},
    )
    service = CanvasResolverService(client)
    test_client_key = "test_dummy_key"
    server = create_resolver_server(
        service,
        host="127.0.0.1",
        port=0,
        client_key=test_client_key,
    )
    server_port = server.server_address[1]

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    base_url = f"http://127.0.0.1:{server_port}"
    auth_headers = {"X-Levyra-Key": test_client_key}
    try:
        resp = requests.get(f"{base_url}/health", timeout=5)
        assert resp.status_code == 200
        assert resp.json() == {"status": "ok"}

        resp_v1_health = requests.get(f"{base_url}/v1/health", timeout=5)
        assert resp_v1_health.status_code == 200
        assert resp_v1_health.json() == {"status": "ok"}

        resp_get_resolve = requests.get(f"{base_url}/v1/resolve", timeout=5)
        assert resp_get_resolve.status_code == 404

        resp_unknown = requests.get(f"{base_url}/unknown/path", timeout=5)
        assert resp_unknown.status_code == 404

        resp_no_key = requests.post(
            f"{base_url}/v1/resolve",
            json={
                "isrc": "USUM71703861",
                "title": "Test Song",
                "artist": "Test Artist",
            },
            timeout=5,
        )
        assert resp_no_key.status_code == 401

        resp_wrong_key = requests.post(
            f"{base_url}/v1/resolve",
            json={
                "isrc": "USUM71703861",
                "title": "Test Song",
                "artist": "Test Artist",
            },
            headers={"X-Levyra-Key": "wrong_dummy_key"},
            timeout=5,
        )
        assert resp_wrong_key.status_code == 401

        resp = requests.post(
            f"{base_url}/v1/resolve",
            json={
                "isrc": "USUM71703861",
                "title": "Test Song",
                "artist": "Test Artist",
            },
            headers=auth_headers,
            timeout=5,
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "resolved"
        assert data["url"] == valid_canvas_url
        assert "sp_dc" not in str(data)
        assert "Bearer" not in str(data)

        resp_bad = requests.post(
            f"{base_url}/v1/resolve",
            data=b"{}",
            headers={"Content-Type": "application/json", **auth_headers},
            timeout=5,
        )
        assert resp_bad.status_code == 400

        resp_malformed = requests.post(
            f"{base_url}/v1/resolve",
            data=b"not-json",
            headers={"Content-Type": "application/json", **auth_headers},
            timeout=5,
        )
        assert resp_malformed.status_code == 400

        resp_oversized = requests.post(
            f"{base_url}/v1/resolve",
            data=b"x" * 70_000,
            headers={"Content-Type": "application/json", **auth_headers},
            timeout=5,
        )
        assert resp_oversized.status_code == 413

        assert "sp_dc" not in caplog.text
        assert "Bearer" not in caplog.text
        assert "authorization" not in caplog.text.lower()
        assert "127.0.0.1" not in caplog.text
        assert test_client_key not in caplog.text

    finally:
        server.shutdown()
        server.server_close()


def test_extract_client_ip() -> None:
    headers_xf = {"X-Forwarded-For": "203.0.113.1, 192.168.1.1"}
    assert extract_client_ip("127.0.0.1", headers_xf) == "203.0.113.1"
    headers_real = {"X-Real-IP": "203.0.113.2"}
    assert extract_client_ip("::1", headers_real) == "203.0.113.2"
    assert extract_client_ip("198.51.100.1", headers_xf) == "198.51.100.1"
    assert extract_client_ip("198.51.100.1", headers_real) == "198.51.100.1"
    assert extract_client_ip("10.0.0.5", headers_xf) == "10.0.0.5"
    assert extract_client_ip("127.0.0.1", {"X-Forwarded-For": "not-an-ip"}) == "127.0.0.1"
    assert extract_client_ip("127.0.0.1", {"X-Real-IP": "not-an-ip"}) == "127.0.0.1"
    assert extract_client_ip("127.0.0.1", {}) == "127.0.0.1"


def test_direct_client_spoofed_headers_cannot_bypass_rate_limit() -> None:
    rate_limiter = RateLimiter(limit_per_minute=2)
    direct_peer = "198.51.100.5"

    headers1 = {"X-Forwarded-For": "1.1.1.1", "X-Real-IP": "1.1.1.1"}
    ip1 = extract_client_ip(direct_peer, headers1)
    assert ip1 == direct_peer
    allowed1, _ = rate_limiter.is_allowed(ip1)
    assert allowed1

    headers2 = {"X-Forwarded-For": "2.2.2.2", "X-Real-IP": "2.2.2.2"}
    ip2 = extract_client_ip(direct_peer, headers2)
    assert ip2 == direct_peer
    allowed2, _ = rate_limiter.is_allowed(ip2)
    assert allowed2

    headers3 = {"X-Forwarded-For": "3.3.3.3", "X-Real-IP": "3.3.3.3"}
    ip3 = extract_client_ip(direct_peer, headers3)
    assert ip3 == direct_peer
    allowed3, retry_after3 = rate_limiter.is_allowed(ip3)
    assert not allowed3
    assert retry_after3 > 0


def test_preserve_l2_cache_expiry_across_reload(tmp_path: Any) -> None:
    db_file = str(tmp_path / "expiry_test.db")
    storage = SQLiteCacheStorage(db_file)
    storage.put("isrc:SHORT_TTL", "https://canvaz.scdn.co/short.mp4", ttl_seconds=0.4)
    time.sleep(0.15)

    cache = CanvasResolverCache(storage_backend=storage, positive_ttl_seconds=3600)
    hit, val = cache.get("isrc:SHORT_TTL")
    assert hit
    assert val == "https://canvaz.scdn.co/short.mp4"

    time.sleep(0.3)
    hit_expired, val_expired = cache.get("isrc:SHORT_TTL")
    assert not hit_expired
    assert val_expired is None
    storage.close()


def test_http_server_negative_content_length_and_incomplete_body() -> None:
    service = CanvasResolverService(FakeSpotifyClient())
    server = create_resolver_server(
        service,
        host="127.0.0.1",
        port=0,
        client_key="test_dummy_key",
    )
    server_port = server.server_address[1]
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", server_port))
        sock.sendall(
            b"POST /v1/resolve HTTP/1.1\r\n"
            b"Host: 127.0.0.1\r\n"
            b"X-Levyra-Key: test_dummy_key\r\n"
            b"Content-Length: -5\r\n"
            b"Content-Type: application/json\r\n\r\n"
            b"{}"
        )
        raw_resp = b""
        sock.settimeout(2.0)
        while True:
            chunk = sock.recv(4096)
            if not chunk:
                break
            raw_resp += chunk
            if b"}" in raw_resp:
                break
        sock.close()
        assert b"400 Bad Request" in raw_resp
        assert b"Invalid Content-Length" in raw_resp

        sock2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock2.connect(("127.0.0.1", server_port))
        sock2.sendall(
            b"POST /v1/resolve HTTP/1.1\r\n"
            b"Host: 127.0.0.1\r\n"
            b"X-Levyra-Key: test_dummy_key\r\n"
            b"Content-Length: 50\r\n"
            b"Content-Type: application/json\r\n\r\n"
            b"{\"test\": 1}"
        )
        sock2.shutdown(socket.SHUT_WR)
        raw_resp2 = b""
        sock2.settimeout(2.0)
        while True:
            chunk = sock2.recv(4096)
            if not chunk:
                break
            raw_resp2 += chunk
            if b"}" in raw_resp2:
                break
        sock2.close()
        assert b"400 Bad Request" in raw_resp2
        assert b"Incomplete request body" in raw_resp2
    finally:
        server.shutdown()
        server.server_close()


def test_http_server_read_timeout_behavior() -> None:
    service = CanvasResolverService(FakeSpotifyClient())
    server = create_resolver_server(
        service,
        host="127.0.0.1",
        port=0,
        client_key="test_dummy_key",
    )
    server.RequestHandlerClass.timeout = 0.2
    server_port = server.server_address[1]
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", server_port))
        sock.sendall(
            b"POST /v1/resolve HTTP/1.1\r\n"
            b"Host: 127.0.0.1\r\n"
            b"X-Levyra-Key: test_dummy_key\r\n"
            b"Content-Length: 100\r\n"
            b"Content-Type: application/json\r\n\r\n"
            b"{\"start\":"
        )
        time.sleep(0.3)
        raw_resp = b""
        sock.settimeout(2.0)
        while True:
            try:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                raw_resp += chunk
                if b"}" in raw_resp:
                    break
            except TimeoutError as err:
                raise AssertionError("server did not enforce read timeout") from err
        sock.close()
        assert b"408 Request Timeout" in raw_resp or len(raw_resp) == 0
    finally:
        server.shutdown()
        server.server_close()


def _varint(value: int) -> bytes:
    output = bytearray()
    while value > 0x7F:
        output.append((value & 0x7F) | 0x80)
        value >>= 7
    output.append(value)
    return bytes(output)


def _bytes_field(number: int, value: str | bytes) -> bytes:
    encoded = value.encode() if isinstance(value, str) else value
    return _varint((number << 3) | 2) + _varint(len(encoded)) + encoded


def _canvas_response(track_id: str, url: str) -> bytes:
    canvas = b"".join(
        (
            _bytes_field(1, "canvas-id"),
            _bytes_field(2, url),
            _bytes_field(5, f"spotify:track:{track_id}"),
        )
    )
    return _bytes_field(1, canvas)


class MockCanvasResponse:
    def __init__(
        self,
        *,
        payload: dict[str, Any] | None = None,
        content: bytes = b"",
        content_type: str = "application/json",
        status_code: int = 200,
    ) -> None:
        import json

        self._payload = payload
        self._content = json.dumps(payload).encode() if payload is not None else content
        self.status_code = status_code
        self.closed = False
        self.headers = {
            "Content-Type": content_type,
            "Content-Length": str(len(self._content)),
        }

    def json(self) -> dict[str, Any] | None:
        return self._payload

    def iter_content(self, chunk_size: int) -> list[bytes]:
        return [
            self._content[offset : offset + chunk_size]
            for offset in range(0, len(self._content), chunk_size)
        ]

    def close(self) -> None:
        self.closed = True


def test_concurrent_401_recovery_with_multiple_resolver_threads() -> None:
    client = SpotifyWebClient("sp_dc_mock_session_secret_123456")
    client._access_token = "test-stale-token"
    client._client_id = "test-client-id"
    client._client_token = "test-client-token"
    client._client_token_expires_at = time.monotonic() + 3600

    auth_refresh_count = 0

    def mock_authenticate() -> None:
        nonlocal auth_refresh_count
        with client._lock:
            auth_refresh_count += 1
            time.sleep(0.04)
            client._access_token = "test-fresh-token"
            client._client_id = "test-client-id"

    client.authenticate = mock_authenticate

    def mock_get(url: str, params: Any = None, headers: Any = None, timeout: Any = None) -> Any:
        import json

        auth_hdr = headers.get("Authorization", "") if headers else ""
        if "test-stale-token" in auth_hdr:
            return MockCanvasResponse(status_code=401)
        if "test-fresh-token" in auth_hdr:
            raw_vars = (params or {}).get("variables", "{}")
            q = json.loads(raw_vars).get("searchTerm", "")
            title = q.split()[0] if q else "Song"
            artist = q.split()[1] if len(q.split()) > 1 else "Artist"
            track_id = f"tid{title}{artist}1234567890"
            return MockCanvasResponse(
                status_code=200,
                payload=_pathfinder_search_payload(
                    [
                        _pathfinder_track_node(
                            track_id=track_id,
                            name=title,
                            artist_name=artist,
                            album_name=f"Album{title.removeprefix('Song')}",
                            duration_ms=180_000,
                        )
                    ]
                ),
            )
        return MockCanvasResponse(status_code=403)

    def mock_post(url: str, data: Any = None, headers: Any = None, **kwargs: Any) -> Any:
        import re

        auth_hdr = headers.get("Authorization", "") if headers else ""
        if "test-stale-token" in auth_hdr:
            return MockCanvasResponse(status_code=401)
        if "test-fresh-token" in auth_hdr:
            matches = re.findall(rb"spotify:track:([A-Za-z0-9_]+)", data or b"")
            tid = matches[0].decode() if matches else "track_default"
            content = _canvas_response(tid, f"https://canvaz.scdn.co/{tid}.mp4")
            return MockCanvasResponse(
                status_code=200,
                content=content,
                content_type="application/protobuf",
            )
        return MockCanvasResponse(status_code=403)

    client._session.get = mock_get
    client._session.post = mock_post

    service = CanvasResolverService(client)

    num_threads = 6
    results: list[dict[str, Any] | None] = [None] * num_threads
    errors: list[Exception | None] = [None] * num_threads

    def worker(idx: int) -> None:
        try:
            res = service.resolve(
                {
                    "title": f"Song{idx}",
                    "artist": f"Artist{idx}",
                    "album": f"Album{idx}",
                    "durationMs": 180_000,
                }
            )
            results[idx] = res
        except Exception as ex:
            errors[idx] = ex

    threads = [threading.Thread(target=worker, args=(i,)) for i in range(num_threads)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    assert all(err is None for err in errors)
    assert auth_refresh_count == 1
    assert all(r is not None and r.get("status") == "resolved" for r in results)
    for idx, r in enumerate(results):
        assert r is not None
        assert f"tidSong{idx}Artist{idx}1234567890" in r.get("url", "")


def _pathfinder_track_node(
    track_id: str = "6DCZcSspjsKoFjzjrWoCdn",
    name: str = "HUMBLE.",
    artist_id: str = "2YZyLoL8N0Wb9xBt1NhZWg",
    artist_name: str = "Kendrick Lamar",
    album_id: str = "4eLPsYPBmXABThSJ821sqY",
    album_name: str = "DAMN.",
    duration_ms: int = 177_000,
    album_id_via_uri_only: bool = False,
) -> dict[str, Any]:
    album_node: dict[str, Any] = {
        "name": album_name,
        "uri": f"spotify:album:{album_id}",
        "coverArt": {"sources": []},
    }
    if not album_id_via_uri_only:
        album_node["id"] = album_id
    return {
        "item": {
            "data": {
                "__typename": "Track",
                "id": track_id,
                "uri": f"spotify:track:{track_id}",
                "name": name,
                "duration": {"totalMilliseconds": duration_ms},
                "artists": {
                    "items": [
                        {
                            "uri": f"spotify:artist:{artist_id}",
                            "profile": {"name": artist_name},
                        }
                    ]
                },
                "albumOfTrack": album_node,
            }
        }
    }


def _pathfinder_search_payload(items: list[Any]) -> dict[str, Any]:
    return {
        "data": {
            "searchV2": {
                "tracksV2": {
                    "items": items,
                }
            }
        }
    }


def _authenticated_spotify_client(**kwargs: Any) -> SpotifyWebClient:
    client = SpotifyWebClient("sp_dc_mock_session_secret_123456", **kwargs)
    client._access_token = "test-active-token"
    client._client_id = "test-client-id"
    client._client_token = "test-client-token"
    client._client_token_expires_at = time.monotonic() + 3600
    return client


def test_pathfinder_search_tracks_parsing_and_id_extraction() -> None:
    import json

    client = _authenticated_spotify_client()
    captured: dict[str, Any] = {}

    def mock_get(url: str, params: Any = None, headers: Any = None, timeout: Any = None) -> Any:
        captured["url"] = url
        captured["params"] = params
        captured["headers"] = headers
        return MockCanvasResponse(
            status_code=200,
            payload=_pathfinder_search_payload(
                [
                    _pathfinder_track_node(
                        track_id="trackA123456",
                        name="HUMBLE.",
                        artist_id="artistKDot12",
                        artist_name="Kendrick Lamar",
                        album_id="albumDamn123",
                        album_name="DAMN.",
                        duration_ms=177_000,
                    ),
                    _pathfinder_track_node(
                        track_id="trackB654321",
                        name="DNA.",
                        artist_id="artistKDot12",
                        artist_name="Kendrick Lamar",
                        album_id="albumUriOnly1",
                        album_name="DAMN.",
                        duration_ms=185_000,
                        album_id_via_uri_only=True,
                    ),
                ]
            ),
        )

    client._session.get = mock_get
    results = client.search_tracks("HUMBLE. Kendrick Lamar", limit=10)

    assert captured["url"] == "https://api-partner.spotify.com/pathfinder/v1/query"
    assert "api.spotify.com/v1/search" not in captured["url"]
    assert captured["params"]["operationName"] == "searchTracks"
    variables = json.loads(captured["params"]["variables"])
    assert variables == {
        "searchTerm": "HUMBLE. Kendrick Lamar",
        "offset": 0,
        "limit": 10,
        "numberOfTopResults": 10,
        "includeAudiobooks": True,
        "includePreReleases": False,
    }
    extensions = json.loads(captured["params"]["extensions"])
    assert extensions == {
        "persistedQuery": {
            "version": 1,
            "sha256Hash": DEFAULT_SEARCH_QUERY_HASH,
        }
    }

    assert len(results) == 2
    assert results[0] == {
        "id": "trackA123456",
        "uri": "spotify:track:trackA123456",
        "name": "HUMBLE.",
        "duration_ms": 177_000,
        "artists": [{"id": "artistKDot12", "name": "Kendrick Lamar"}],
        "album": {"id": "albumDamn123", "name": "DAMN."},
        "external_ids": {},
    }
    assert results[1]["album"] == {"id": "albumUriOnly1", "name": "DAMN."}
    assert results[1]["duration_ms"] == 185_000
    assert results[1]["external_ids"] == {}


def test_pathfinder_search_ignores_malformed_items_and_rejects_bad_payload() -> None:
    client = _authenticated_spotify_client()

    def mock_get_partial(
        url: str, params: Any = None, headers: Any = None, timeout: Any = None
    ) -> Any:
        return MockCanvasResponse(
            status_code=200,
            payload=_pathfinder_search_payload(
                [
                    None,
                    {},
                    {"item": "not-a-dict"},
                    {"item": {"data": {"id": "", "name": "Missing ID"}}},
                    {"item": {"data": {"id": "validTrack12", "name": ""}}},
                    _pathfinder_track_node(track_id="validTrack99", name="Valid Track"),
                ]
            ),
        )

    client._session.get = mock_get_partial
    items = client.search_tracks("Valid Track")
    assert len(items) == 1
    assert items[0]["id"] == "validTrack99"

    def mock_get_bad_shape(
        url: str, params: Any = None, headers: Any = None, timeout: Any = None
    ) -> Any:
        return MockCanvasResponse(status_code=200, payload={"data": {"searchV2": {}}})

    client._session.get = mock_get_bad_shape
    with pytest.raises(SourceApiError, match="invalid response shape"):
        client.search_tracks("Valid Track")


def test_pathfinder_search_401_refresh_and_retry() -> None:
    client = _authenticated_spotify_client()
    client._access_token = "expired-tok"
    refreshed = 0

    def mock_auth() -> None:
        nonlocal refreshed
        refreshed += 1
        client._access_token = "renewed-tok"

    client.authenticate = mock_auth

    def mock_get(url: str, params: Any = None, headers: Any = None, timeout: Any = None) -> Any:
        auth_hdr = (headers or {}).get("Authorization", "")
        if "expired-tok" in auth_hdr:
            return MockCanvasResponse(status_code=401)
        return MockCanvasResponse(
            status_code=200,
            payload=_pathfinder_search_payload(
                [_pathfinder_track_node(track_id="after401Track", name="Recovered")]
            ),
        )

    client._session.get = mock_get
    results = client.search_tracks("Recovered")
    assert refreshed == 1
    assert len(results) == 1
    assert results[0]["id"] == "after401Track"


def test_pathfinder_search_429_transient_not_negative_cached() -> None:
    client = _authenticated_spotify_client()
    attempts = 0

    def mock_get(url: str, params: Any = None, headers: Any = None, timeout: Any = None) -> Any:
        nonlocal attempts
        attempts += 1
        resp = MockCanvasResponse(status_code=429)
        resp.headers["Retry-After"] = "0"
        return resp

    client._session.get = mock_get
    service = CanvasResolverService(client)
    query_payload = {"title": "Rate Limited Song", "artist": "Artist"}
    res = service.resolve(query_payload)
    assert res["status"] == "unavailable"
    assert attempts == 2

    key = service._cache_key_for(service._validate_request(query_payload))
    hit, _ = service.cache.get(key)
    assert not hit


def test_pathfinder_search_persisted_query_not_found_and_hash_config(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    custom_hash = "ab" * 32
    monkeypatch.setenv("LEVYRA_EDITORIAL_SEARCH_QUERY_HASH", custom_hash.upper())
    client = _authenticated_spotify_client()
    assert client._search_query_hash == custom_hash

    with pytest.raises(AuthenticationError, match="search query hash is malformed"):
        validate_search_query_hash("invalid-short-hash")

    def mock_get_rotated(
        url: str, params: Any = None, headers: Any = None, timeout: Any = None
    ) -> Any:
        return MockCanvasResponse(
            status_code=200,
            payload={"errors": [{"message": "PersistedQueryNotFound"}]},
        )

    client._session.get = mock_get_rotated
    with pytest.raises(
        SourceApiError,
        match="Update LEVYRA_EDITORIAL_SEARCH_QUERY_HASH",
    ):
        client.search_tracks("Some Track")


def test_pathfinder_candidates_do_not_fabricate_isrc() -> None:
    client = _authenticated_spotify_client()

    def mock_get(url: str, params: Any = None, headers: Any = None, timeout: Any = None) -> Any:
        return MockCanvasResponse(
            status_code=200,
            payload=_pathfinder_search_payload(
                [
                    _pathfinder_track_node(
                        track_id="noIsrcTrack12",
                        name="HUMBLE.",
                        artist_name="Kendrick Lamar",
                        album_name="DAMN.",
                        duration_ms=177_000,
                    )
                ]
            ),
        )

    def mock_post(url: str, data: Any = None, headers: Any = None, **kwargs: Any) -> Any:
        content = _canvas_response(
            "noIsrcTrack12",
            "https://canvaz.scdn.co/upload/artist/kdot/video/humble.cnvs.mp4",
        )
        return MockCanvasResponse(
            status_code=200,
            content=content,
            content_type="application/protobuf",
        )

    client._session.get = mock_get
    client._session.post = mock_post

    matcher = CanvasTrackMatcher()
    candidates = client.search_tracks("HUMBLE. Kendrick Lamar")
    assert candidates[0]["external_ids"] == {}
    outcome = matcher.match_candidate(
        TrackQuery(
            isrc="USUM71703861",
            title="HUMBLE.",
            artist="Kendrick Lamar",
            album="DAMN.",
            duration_ms=177_000,
        ),
        candidates[0],
    )
    assert outcome.accepted
    assert outcome.reason == "metadata_matched"

    service = CanvasResolverService(client)
    res = service.resolve(
        {
            "isrc": "USUM71703861",
            "title": "HUMBLE.",
            "artist": "Kendrick Lamar",
            "album": "DAMN.",
            "durationMs": 177_000,
        }
    )
    assert res["status"] == "resolved"
    assert res["isrc"] == ""
    discoveries = service.cache.export_discoveries()
    assert len(discoveries) == 1
    assert discoveries[0]["isrc"] == ""
