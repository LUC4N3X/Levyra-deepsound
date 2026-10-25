from __future__ import annotations

import json
import threading
import time
from types import SimpleNamespace

import pytest
import requests

from levyra_editorial import watchfeed
from levyra_editorial.spotify import _encode_varint

TRACK_ID = "7lQ8MOhq6IN2w8EYcFNSUk"
OTHER_ID = "1234567890123456789012"
PREVIEW = "https://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba"
CANVAS = "https://canvaz.scdn.co/upload/test.cnvs.mp4"


def field(number: int, value: bytes | str) -> bytes:
    data = value.encode() if isinstance(value, str) else value
    return _encode_varint(number << 3 | 2) + _encode_varint(len(data)) + data


def feed(*track_ids: str) -> bytes:
    canvas = field(1, watchfeed.CANVAS_CONTENT_TYPE) + field(2, field(2, field(1, CANVAS)))
    row = field(1, watchfeed.ENTITY_ROW_TYPE) + field(2, field(1, "Without Me") + field(2, "Eminem"))
    identity = field(19, field(7, b"".join(field(4, f"spotify:track:{value}") for value in track_ids)))
    return field(2, field(2, field(2, canvas) + field(6, row) + identity))


@pytest.mark.parametrize("ids", [(TRACK_ID,), (TRACK_ID, TRACK_ID)])
def test_parser_preserves_unique_track_identity_in_nested_item(ids):
    clip = watchfeed.parse_watch_feed_clips(feed(*ids))[0]
    assert clip["spotifyTrackId"] == TRACK_ID
    assert "spotify:" not in json.dumps(clip)


@pytest.mark.parametrize("ids", [(), (TRACK_ID, OTHER_ID), (TRACK_ID + "a",)])
def test_parser_does_not_guess_absent_ambiguous_or_invalid_identity(ids):
    assert "spotifyTrackId" not in watchfeed.parse_watch_feed_clips(feed(*ids))[0]


class Response:
    status_code = 200

    def __init__(self, entity=None, body=None):
        data = {"props": {"pageProps": {"state": {"data": {"entity": entity}}}}}
        self.body = (
            body
            if body is not None
            else (
                '<html><script type="application/json" id="__NEXT_DATA__">'
                + json.dumps(data)
                + "</script></html>"
            ).encode()
        )
        self.headers = {"Content-Length": str(len(self.body))}
        self.closed = False

    def iter_content(self, chunk_size):
        yield self.body

    def close(self):
        self.closed = True


def mock_embed(monkeypatch, response):
    def get(url, **kwargs):
        assert url == f"https://open.spotify.com/embed/track/{TRACK_ID}"
        assert kwargs["stream"] is True
        assert kwargs["allow_redirects"] is False
        assert 0 < kwargs["timeout"] <= 25
        assert "Authorization" not in kwargs.get("headers", {})
        return response

    monkeypatch.setattr(watchfeed.requests, "get", get)


@pytest.mark.parametrize("url", [PREVIEW, PREVIEW + ".mp3?cid=public"])
def test_fetches_original_preview_for_exact_embed_identity(monkeypatch, url):
    response = Response({"uri": f"spotify:track:{TRACK_ID}", "audioPreview": {"url": url}})
    mock_embed(monkeypatch, response)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) == url
    assert response.closed


@pytest.mark.parametrize(
    "url",
    [
        "https://evil.example/mp3-preview/" + "a" * 40,
        PREVIEW.replace("p.scdn.co", "p.scdn.co.evil.example"),
        PREVIEW.replace("https:", "http:"),
        PREVIEW + "/../other",
        PREVIEW + "#fragment",
        PREVIEW[:-1],
        PREVIEW + "?value=+bad",
        PREVIEW + "?",
    ],
)
def test_rejects_preview_destinations(monkeypatch, url):
    mock_embed(monkeypatch, Response({"uri": f"spotify:track:{TRACK_ID}", "audioPreview": {"url": url}}))
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) is None
    payload = catalog({"spotifyTrackId": TRACK_ID, "audioPreviewUrl": url})
    with pytest.raises(ValueError):
        watchfeed.validate_watch_feed_clips(payload)


@pytest.mark.parametrize(
    "entity",
    [
        {"uri": f"spotify:track:{OTHER_ID}", "audioPreview": {"url": PREVIEW}},
        {"audioPreview": {"url": PREVIEW}},
        {"uri": f"spotify:track:{TRACK_ID}"},
        {"uri": f"spotify:track:{TRACK_ID}", "audioPreview": None},
        None,
    ],
)
def test_missing_or_mismatched_preview_is_not_published(monkeypatch, entity):
    mock_embed(monkeypatch, Response(entity))
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) is None


def test_network_error_is_optional_preview(monkeypatch):
    def fail(*args, **kwargs):
        raise requests.Timeout()

    monkeypatch.setattr(watchfeed.requests, "get", fail)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) is None


@pytest.mark.parametrize(
    "body",
    [b"not html", b"<script id='__NEXT_DATA__'>{bad}</script>", b"x" * 2_100_000],
    ids=["no-state", "malformed-json", "oversize"],
)
def test_malformed_or_oversize_embed_is_optional_preview(monkeypatch, body):
    response = Response(body=body)
    mock_embed(monkeypatch, response)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) is None
    assert response.closed


def catalog(extra):
    return {
        "version": 1,
        "genres": [{"id": "rap"}],
        "items": [
            {
                "id": "levyra-test",
                "genre": "rap",
                "title": "Without Me",
                "artist": "Eminem",
                "artworkUrl": "",
                "canvasUrl": CANVAS,
                **extra,
            }
        ],
    }


def test_validation_accepts_optional_exact_identity_and_preview():
    watchfeed.validate_watch_feed_clips(catalog({"spotifyTrackId": TRACK_ID, "audioPreviewUrl": PREVIEW}))
    watchfeed.validate_watch_feed_clips(catalog({}))


@pytest.mark.parametrize(
    "extra",
    [
        {"spotifyTrackId": f"spotify:track:{TRACK_ID}"},
        {"spotifyTrackId": 1},
        {"spotifyTrackId": ""},
        {"audioPreviewUrl": PREVIEW},
    ],
)
def test_validation_requires_valid_exact_identity_for_preview(extra):
    with pytest.raises(ValueError):
        watchfeed.validate_watch_feed_clips(catalog(extra))


def test_builder_publishes_original_preview_without_youtube_match(monkeypatch):
    monkeypatch.setattr(
        watchfeed,
        "fetch_watch_feed_clips",
        lambda *args: (
            "Rap",
            watchfeed.parse_watch_feed_clips(feed(TRACK_ID)),
        ),
    )
    mock_embed(monkeypatch, Response({"uri": f"spotify:track:{TRACK_ID}", "audioPreview": {"url": PREVIEW}}))

    def no_match(*args):
        pytest.fail("WatchFeed audio must not be approximated by a YouTube match")

    payload = watchfeed.build_watch_feed_clips(
        object(),
        [{"id": "rap", "playlistId": OTHER_ID}],
        SimpleNamespace(resolve=no_match),
    )
    assert payload["items"][0]["audioPreviewUrl"] == PREVIEW
    assert "audioVideoId" not in payload["items"][0]
    assert "spotify:" not in json.dumps(payload)


@pytest.mark.parametrize("status", [302, 404, 500])
def test_embed_http_failure_does_not_publish_preview(monkeypatch, status):
    response = Response({"uri": f"spotify:track:{TRACK_ID}", "audioPreview": {"url": PREVIEW}})
    response.status_code = status
    mock_embed(monkeypatch, response)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID) is None
    assert response.closed


@pytest.mark.parametrize("track_ids", [(), (TRACK_ID,)])
def test_builder_keeps_canvas_without_preview_and_never_approximates_audio(monkeypatch, track_ids):
    monkeypatch.setattr(
        watchfeed,
        "fetch_watch_feed_clips",
        lambda *args: (
            "Rap",
            watchfeed.parse_watch_feed_clips(feed(*track_ids)),
        ),
    )

    def fail(*args, **kwargs):
        if not track_ids:
            pytest.fail("No track identity must mean no network lookup")
        raise requests.Timeout()

    def no_match(*args):
        pytest.fail("Missing Spotify preview must not fall back to a YouTube match")

    monkeypatch.setattr(watchfeed.requests, "get", fail)
    payload = watchfeed.build_watch_feed_clips(
        object(),
        [{"id": "rap", "playlistId": OTHER_ID}],
        SimpleNamespace(resolve=no_match),
    )
    assert payload["items"][0]["canvasUrl"] == CANVAS
    assert "audioPreviewUrl" not in payload["items"][0]
    assert "audioVideoId" not in payload["items"][0]


def test_preview_budget_returns_without_waiting_for_running_requests(monkeypatch):
    release = threading.Event()
    lock = threading.Lock()
    started = 0

    def blocked(*args, **kwargs):
        nonlocal started
        with lock:
            started += 1
        release.wait(2)
        return PREVIEW

    monkeypatch.setattr(watchfeed, "fetch_spotify_audio_preview", blocked)
    monkeypatch.setattr(watchfeed, "PREVIEW_ENRICHMENT_BUDGET_SECONDS", 0.05, raising=False)
    items = [{"spotifyTrackId": TRACK_ID} for _ in range(20)]
    began = time.monotonic()
    try:
        watchfeed._enrich_audio_previews(items)
        assert time.monotonic() - began < 0.5
        assert 1 <= started <= 4
        assert all("audioPreviewUrl" not in item for item in items)
        assert all(item["spotifyTrackId"] == TRACK_ID for item in items)
    finally:
        release.set()


def test_preview_enrichment_uses_bounded_parallel_requests(monkeypatch):
    lock = threading.Lock()
    parallel = threading.Event()
    active = peak = 0

    def preview(*args, **kwargs):
        nonlocal active, peak
        with lock:
            active += 1
            peak = max(peak, active)
            if active == 4:
                parallel.set()
        try:
            assert parallel.wait(1)
            return PREVIEW
        finally:
            with lock:
                active -= 1

    monkeypatch.setattr(watchfeed, "fetch_spotify_audio_preview", preview)
    items = [{"spotifyTrackId": TRACK_ID} for _ in range(12)]
    watchfeed._enrich_audio_previews(items)
    assert peak == 4
    assert all(item["audioPreviewUrl"] == PREVIEW for item in items)


def test_expired_preview_deadline_does_not_start_network_request(monkeypatch):
    def forbidden(*args, **kwargs):
        pytest.fail("Expired preview enrichment must not start another request")

    monkeypatch.setattr(watchfeed.requests, "get", forbidden)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID, deadline=0) is None


def test_preview_deadline_closes_response_during_streaming(monkeypatch):
    now = [0.0]
    response = Response({"uri": f"spotify:track:{TRACK_ID}", "audioPreview": {"url": PREVIEW}})

    def expired_stream(chunk_size):
        now[0] = 2.0
        yield response.body

    response.iter_content = expired_stream
    monkeypatch.setattr(watchfeed, "monotonic", lambda: now[0])
    mock_embed(monkeypatch, response)
    assert watchfeed.fetch_spotify_audio_preview(TRACK_ID, deadline=1.0) is None
    assert response.closed
