from __future__ import annotations

import json
from typing import Any

import pytest

from levyra_editorial.collector import (
    build_spotify_canvas_catalog,
    validate_spotify_canvas_catalog,
)
from levyra_editorial.models import Album, Artist, Catalog, Collection, Track
from levyra_editorial.spotify import (
    CANVAS_URL,
    CLIENT_TOKEN_URL,
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
    ) -> None:
        self._payload = payload
        self._content = json.dumps(payload).encode() if payload is not None else content
        self.status_code = 200
        self.closed = False
        self.headers = {
            "Content-Type": content_type,
            "Content-Length": str(len(content)),
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
