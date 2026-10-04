from __future__ import annotations

import json
import logging
from dataclasses import replace
from typing import Any

import pytest
import requests

from levyra_editorial.collector import (
    build_spotify_canvas_catalog,
    validate_spotify_canvas_catalog,
)
from levyra_editorial.models import Album, Artist, Catalog, Collection, Track
from levyra_editorial.spotify import (
    CANVAS_URL,
    CLIENT_TOKEN_URL,
    PAXSENIX_USER_AGENT,
    AuthenticationError,
    SourceApiError,
    SpotifyWebClient,
    decode_canvas_response,
    encode_canvas_request,
)


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


class CanvasResponse:
    def __init__(
        self,
        *,
        payload: dict[str, Any] | None = None,
        content: bytes = b"",
        content_type: str = "application/json",
        status_code: int = 200,
    ) -> None:
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


class CanvasSession:
    def __init__(self, track_id: str, url: str) -> None:
        self.track_id = track_id
        self.url = url
        self.requests: list[tuple[str, dict[str, Any]]] = []
        self.cookies = requests.cookies.RequestsCookieJar()
        self.headers: dict[str, str] = {}

    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        self.requests.append((url, kwargs))
        if url == CLIENT_TOKEN_URL:
            return CanvasResponse(
                payload={
                    "granted_token": {
                        "token": "ephemeral-client-token",
                        "expires_after_seconds": 600,
                    }
                }
            )
        assert url == CANVAS_URL
        return CanvasResponse(
            content=_canvas_response(self.track_id, self.url),
            content_type="application/protobuf",
        )

    def close(self) -> None:
        return None


class FallbackCanvasSession(CanvasSession):
    def __init__(self, track_id: str, url: str | None) -> None:
        super().__init__(track_id, url or "")

    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        self.requests.append((url, kwargs))
        if url == CLIENT_TOKEN_URL:
            return CanvasResponse(
                payload={
                    "granted_token": {
                        "token": "ephemeral-client-token",
                        "expires_after_seconds": 600,
                    }
                }
            )
        assert url == CANVAS_URL
        payload = b""
        if "Client-Token" not in kwargs["headers"] and self.url:
            payload = _canvas_response(self.track_id, self.url)
        return CanvasResponse(content=payload, content_type="application/protobuf")


class DuplicateCanvasSession(CanvasSession):
    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        response = super().post(url, **kwargs)
        if url == CANVAS_URL:
            response._content += response._content
            response.headers["Content-Length"] = str(len(response._content))
        return response


class UnauthorizedCanvasSession(CanvasSession):
    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        self.requests.append((url, kwargs))
        if url == CLIENT_TOKEN_URL:
            return CanvasResponse(
                payload={
                    "granted_token": {
                        "token": "ephemeral-client-token",
                        "expires_after_seconds": 600,
                    }
                }
            )
        return CanvasResponse(
            content_type="application/protobuf",
            status_code=401,
        )


class TimeoutCanvasSession(CanvasSession):
    def __init__(self, track_id: str, secret: str) -> None:
        super().__init__(track_id, "")
        self.secret = secret

    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        self.requests.append((url, kwargs))
        raise requests.Timeout(f"request included sp_dc={self.secret}")


class MalformedPrimaryCanvasSession(CanvasSession):
    def post(self, url: str, **kwargs: Any) -> CanvasResponse:
        if url == CLIENT_TOKEN_URL:
            return super().post(url, **kwargs)
        self.requests.append((url, kwargs))
        if "Client-Token" in kwargs["headers"]:
            return CanvasResponse(
                content=b"\x0a\x05bad",
                content_type="application/protobuf",
            )
        raise requests.Timeout("PaxSenix fallback timed out")


def _catalog(track_id: str) -> Catalog:
    track = Track(
        position=1,
        id=track_id,
        uri=f"spotify:track:{track_id}",
        title="Canvas Song",
        artists=[Artist(id="artist-source-id", name="Artist One")],
        album=Album(
            id="album-source-id",
            name="Canvas Album",
            release_date="2026-08-15",
            artwork_url=None,
            external_url=None,
        ),
        duration_ms=180_000,
        explicit=False,
        external_url=None,
        artwork_url=None,
        isrc="ITABC2600001",
    )
    return Catalog(
        schema_version=1,
        generated_at="2026-08-15T12:00:00Z",
        collections=[
            Collection(
                id="editorial",
                kind="editorial",
                market="IT",
                title="Editorial",
                description="",
                source_id="playlist-source-id",
                source_url=None,
                artwork_url=None,
                snapshot_id=None,
                total_source_items=1,
                tracks=[track],
            )
        ],
    )


def test_canvas_request_and_response_use_only_required_protobuf_fields() -> None:
    track_id = "1234567890"
    request = encode_canvas_request([track_id])
    assert request == b"\x0a\x1a\x0a\x18spotify:track:1234567890"

    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    assert decode_canvas_response(_canvas_response(track_id, url)) == [
        (f"spotify:track:{track_id}", url)
    ]


def test_canvas_client_uses_ephemeral_tokens_without_placing_them_in_the_body() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    session = CanvasSession(track_id, url)
    client = SpotifyWebClient("A" * 40, session=session)
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    assert client.get_canvas_urls([track_id]) == {track_id: url}
    client_token_request = session.requests[0][1]
    canvas_request = session.requests[1][1]
    assert client_token_request["json"]["client_data"]["client_id"] == "web-client-id"
    assert canvas_request["headers"]["Client-Token"] == "ephemeral-client-token"
    assert canvas_request["headers"]["Authorization"] == "Bearer ephemeral-access-token"
    assert b"token" not in canvas_request["data"]


def test_paxsenix_fallback_resolves_missing_canvas_without_forwarding_cookie(
    caplog: pytest.LogCaptureFixture,
) -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    secret = "editorial-session-secret-value-123456"
    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    session = FallbackCanvasSession(track_id, None)
    paxsenix_session = FallbackCanvasSession(track_id, url)
    session.cookies.set("sp_dc", secret)
    paxsenix_session.cookies.set("stale", "cookie")
    paxsenix_session.headers["Cookie"] = "stale=cookie"
    client = SpotifyWebClient(
        secret,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    with caplog.at_level(logging.INFO):
        assert client.get_canvas_urls([track_id]) == {track_id: url}

    primary_request = session.requests[1][1]
    paxsenix_request = paxsenix_session.requests[0][1]
    assert primary_request["headers"]["Client-Token"] == "ephemeral-client-token"
    assert paxsenix_request["headers"] == {
        "Accept": "application/protobuf",
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept-Language": "en",
        "User-Agent": PAXSENIX_USER_AGENT,
        "Authorization": "Bearer ephemeral-access-token",
    }
    assert "Cookie" not in paxsenix_request["headers"]
    assert paxsenix_session.cookies.get_dict() == {}
    assert "Cookie" not in paxsenix_session.headers
    assert session.cookies.get_dict() == {"sp_dc": secret}
    assert secret.encode() not in paxsenix_request["data"]
    assert "PaxSenix resolved: 1" in caplog.text
    assert secret not in caplog.text


def test_paxsenix_only_resolver_skips_primary_profile() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    session = FallbackCanvasSession(track_id, None)
    paxsenix_session = FallbackCanvasSession(track_id, url)
    client = SpotifyWebClient(
        "A" * 40,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    assert client.get_paxsenix_canvas_urls([track_id]) == {track_id: url}
    assert session.requests == []
    assert [request_url for request_url, _ in paxsenix_session.requests] == [CANVAS_URL]
    request = paxsenix_session.requests[0][1]
    assert "Client-Token" not in request["headers"]
    assert request["headers"]["User-Agent"] == PAXSENIX_USER_AGENT


def test_paxsenix_fallback_returns_no_canvas_as_a_clean_miss() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    session = FallbackCanvasSession(track_id, None)
    paxsenix_session = FallbackCanvasSession(track_id, None)
    client = SpotifyWebClient(
        "A" * 40,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    assert client.get_canvas_urls([track_id]) == {}


def test_canvas_resolvers_deduplicate_identical_track_results() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    session = DuplicateCanvasSession(track_id, url)
    client = SpotifyWebClient("A" * 40, session=session)
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    assert client.get_canvas_urls([track_id]) == {track_id: url}
    assert [request_url for request_url, _ in session.requests].count(CANVAS_URL) == 1


def test_paxsenix_invalid_url_is_rejected_by_existing_catalog_validation() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    session = FallbackCanvasSession(track_id, None)
    paxsenix_session = FallbackCanvasSession(
        track_id,
        "https://canvaz.scdn.co/upload/artist/video/canvas.webm",
    )
    client = SpotifyWebClient(
        "A" * 40,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    with pytest.raises(ValueError, match="at least one item"):
        build_spotify_canvas_catalog(_catalog(track_id), client)


def test_expired_canvas_auth_fails_closed_without_exposing_credentials(
    caplog: pytest.LogCaptureFixture,
) -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    secret = "expired-editorial-session-secret-123456"
    session = UnauthorizedCanvasSession(track_id, "")
    paxsenix_session = UnauthorizedCanvasSession(track_id, "")
    client = SpotifyWebClient(
        secret,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "expired-access-token"
    client._client_id = "web-client-id"

    def fail_authentication() -> None:
        raise AuthenticationError("The editorial source session could not be authenticated.")

    client.authenticate = fail_authentication
    with caplog.at_level(logging.WARNING), pytest.raises(
        SourceApiError,
        match="All Spotify Canvas resolvers failed",
    ):
        client.get_canvas_urls([track_id])

    assert secret not in caplog.text
    assert "expired-access-token" not in caplog.text


def test_canvas_timeout_is_sanitized_and_does_not_leak_secret(
    caplog: pytest.LogCaptureFixture,
) -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    secret = "timeout-editorial-session-secret-123456"
    session = TimeoutCanvasSession(track_id, secret)
    paxsenix_session = TimeoutCanvasSession(track_id, secret)
    client = SpotifyWebClient(
        secret,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    with caplog.at_level(logging.WARNING), pytest.raises(
        SourceApiError,
        match="All Spotify Canvas resolvers failed",
    ):
        client.get_canvas_urls([track_id])

    assert "request timed out" in caplog.text
    assert secret not in caplog.text


def test_malformed_primary_response_and_failed_fallback_fail_the_batch() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    session = MalformedPrimaryCanvasSession(track_id, "")
    paxsenix_session = TimeoutCanvasSession(track_id, "fallback-secret")
    client = SpotifyWebClient(
        "A" * 40,
        session=session,
        paxsenix_session=paxsenix_session,
    )
    client._access_token = "ephemeral-access-token"
    client._client_id = "web-client-id"

    with pytest.raises(SourceApiError, match="All Spotify Canvas resolvers failed"):
        client.get_canvas_urls([track_id])


def test_public_canvas_catalog_strips_all_spotify_source_identifiers() -> None:
    track_id = "5osCClSjGplWagDsJmyivf"
    url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"

    class Resolver:
        def get_canvas_urls(self, track_ids: list[str]) -> dict[str, str]:
            assert track_ids == [track_id]
            return {track_id: url}

    payload = build_spotify_canvas_catalog(_catalog(track_id), Resolver())
    assert payload == {
        "version": 1,
        "generatedAt": "2026-08-15T12:00:00Z",
        "items": [
            {
                "song": "Canvas Song",
                "artist": "Artist One",
                "album": "Canvas Album",
                "url": url,
                "scope": "track",
                "isrc": "ITABC2600001",
            }
        ],
    }
    serialized = str(payload).casefold()
    assert track_id.casefold() not in serialized
    assert "playlist-source-id" not in serialized
    assert "artist-source-id" not in serialized
    assert "album-source-id" not in serialized


def test_invalid_canvas_row_isolated_from_valid_catalog(
    caplog: pytest.LogCaptureFixture,
) -> None:
    valid_id = "5osCClSjGplWagDsJmyivf"
    invalid_id = "6osCClSjGplWagDsJmyivg"
    valid_url = "https://canvaz.scdn.co/upload/artist/video/canvas.cnvs.mp4"
    catalog = _catalog(valid_id)
    original = catalog.collections[0].tracks[0]
    catalog.collections[0].tracks.append(
        replace(original, id=invalid_id, uri=f"spotify:track:{invalid_id}", title="Other Song")
    )

    class Resolver:
        def get_canvas_urls(self, track_ids: list[str]) -> dict[str, str]:
            assert track_ids == [valid_id, invalid_id]
            return {
                valid_id: valid_url,
                invalid_id: "https://canvaz.scdn.co/upload/artist/video/canvas.webm",
            }

    payload = build_spotify_canvas_catalog(catalog, Resolver())

    assert [item["song"] for item in payload["items"]] == ["Canvas Song"]
    assert "extension=1" in caplog.text
    assert "canvas.webm" not in caplog.text


@pytest.mark.parametrize(
    "url",
    [
        "http://canvaz.scdn.co/upload/canvas.mp4",
        "https://canvaz.scdn.co.evil.example/upload/canvas.mp4",
        "https://canvaz.scdn.co/upload/canvas.m3u8",
        "https://canvaz.scdn.co/upload/canvas.mp4?token=secret",
    ],
)
def test_canvas_catalog_rejects_unapproved_media_urls(url: str) -> None:
    payload = {
        "version": 1,
        "generatedAt": "2026-08-15T12:00:00Z",
        "items": [
            {
                "song": "Canvas Song",
                "artist": "Artist One",
                "album": "Canvas Album",
                "url": url,
                "scope": "track",
            }
        ],
    }
    with pytest.raises(ValueError, match="invalid media URL"):
        validate_spotify_canvas_catalog(payload)
