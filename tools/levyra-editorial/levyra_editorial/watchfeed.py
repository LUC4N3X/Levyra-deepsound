from __future__ import annotations

import hashlib
import json
import logging
import re
from collections.abc import Mapping, Sequence
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
from pathlib import Path
from time import monotonic
from typing import Any

import requests

from .spotify import (
    EditorialSourceError,
    SourceApiError,
    SpotifyWebClient,
    _encode_varint,
    _iter_protobuf_fields,
    _read_bounded_response,
)
from .youtube_music import YoutubeMusicWebClient

LOGGER = logging.getLogger(__name__)

WATCH_FEED_URL = "https://spclient.wg.spotify.com/watch-feed-view/v1/feed"
WATCH_FEED_USER_AGENT = "Spotify/9.1.94.1929 Android/34 (Pixel 8)"
MAX_WATCH_FEED_RESPONSE_BYTES = 4 * 1024 * 1024
MAX_EMBED_RESPONSE_BYTES = 2 * 1024 * 1024
PREVIEW_ENRICHMENT_BUDGET_SECONDS = 45
MAX_PREVIEW_WORKERS = 4
WATCH_FEED_CLIPS_VERSION = 1
CANVAS_CONTENT_TYPE = "spotify.watchfeed.component.content.v1.CanvasContent"
ENTITY_ROW_TYPE = "spotify.watchfeed.component.item.v2.BoxedEntityRowComponentV2"
CANVAS_URL_PATTERN = re.compile(r"^https://canvaz\.scdn\.co/[A-Za-z0-9/_.\-]+\.cnvs\.mp4$")
ARTWORK_URL_PATTERN = re.compile(r"^https://i\.scdn\.co/image/[A-Za-z0-9]+$")
GENRE_ID_PATTERN = re.compile(r"^[a-z0-9-]{2,32}$")
PLAYLIST_ID_PATTERN = re.compile(r"^[A-Za-z0-9]{22}$")
TRACK_URI_PATTERN = re.compile(rb"spotify:track:([A-Za-z0-9]{22})(?![A-Za-z0-9])")
PREVIEW_URL_PATTERN = re.compile(
    r"https://p\.scdn\.co/mp3-preview/[a-f0-9]{40}(?:\.mp3)?(?:\?[A-Za-z0-9%=&._-]+)?"
)
EMBED_DATA_PATTERN = re.compile(
    r"<script\b(?=[^>]*\bid=[\"\']__NEXT_DATA__[\"\'])[^>]*>(.*?)</script\s*>",
    re.S | re.I,
)
LANGUAGE_PATTERN = re.compile(r"^[a-z]{2}$")
MAX_CLIPS_PER_GENRE = 40
MAX_TEXT_LENGTH = 200
FORBIDDEN_KEYS = re.compile(r"sp_dc=|sapisid=|authorization:|bearer |spotify:", re.I)


def encode_watch_feed_request(playlist_id: str) -> bytes:
    def string_field(number: int, value: str) -> bytes:
        data = value.encode("utf-8")
        return _encode_varint((number << 3) | 2) + _encode_varint(len(data)) + data

    return string_field(1, "playlist") + string_field(2, playlist_id)


def _messages(payload: bytes, number: int) -> list[bytes]:
    try:
        fields = _iter_protobuf_fields(payload)
    except SourceApiError:
        return []
    return [
        value
        for field, wire, value in fields
        if field == number and wire == 2 and isinstance(value, bytes)
    ]


def _text(payload: bytes, number: int) -> str:
    for value in _messages(payload, number):
        try:
            return value.decode("utf-8").strip()
        except UnicodeDecodeError:
            return ""
    return ""


def _any_payload(component: bytes, type_suffix: str) -> bytes | None:
    if not _text(component, 1).endswith(type_suffix):
        return None
    values = _messages(component, 2)
    return values[0] if values else None


def parse_watch_feed_clips(payload: bytes) -> list[dict[str, str]]:
    clips: list[dict[str, str]] = []
    for layout in _messages(payload, 2):
        for item in _messages(layout, 2):
            canvas_url = ""
            for content in _messages(item, 2):
                canvas = _any_payload(content, CANVAS_CONTENT_TYPE)
                if canvas is not None:
                    for media in _messages(canvas, 2):
                        canvas_url = _text(media, 1)
            if not CANVAS_URL_PATTERN.match(canvas_url):
                continue
            title = artist = artwork = ""
            for row in _messages(item, 6):
                entity = _any_payload(row, ENTITY_ROW_TYPE)
                if entity is None:
                    continue
                title = _text(entity, 1)
                artist = _text(entity, 2)
                for image in _messages(entity, 3):
                    artwork = _text(image, 1)
            if not title or not artist:
                continue
            clip = {
                "title": title[:MAX_TEXT_LENGTH],
                "artist": artist[:MAX_TEXT_LENGTH],
                "artworkUrl": artwork if ARTWORK_URL_PATTERN.match(artwork) else "",
                "canvasUrl": canvas_url,
            }
            track_ids = set(TRACK_URI_PATTERN.findall(item))
            if len(track_ids) == 1:
                clip["spotifyTrackId"] = track_ids.pop().decode("ascii")
            clips.append(clip)
    return clips


def fetch_spotify_audio_preview(track_id: str, *, deadline: float | None = None) -> str | None:
    if not PLAYLIST_ID_PATTERN.fullmatch(track_id):
        return None
    deadline = min(deadline, monotonic() + 15) if deadline is not None else monotonic() + 15
    remaining = deadline - monotonic()
    if remaining <= 0:
        return None
    try:
        response = requests.get(
            f"https://open.spotify.com/embed/track/{track_id}",
            timeout=remaining,
            stream=True,
            allow_redirects=False,
        )
        try:
            if response.status_code != 200:
                return None
            if int(response.headers.get("Content-Length", "0")) > MAX_EMBED_RESPONSE_BYTES:
                return None
            content = bytearray()
            for chunk in response.iter_content(chunk_size=16 * 1024):
                if monotonic() >= deadline or len(content) + len(chunk) > MAX_EMBED_RESPONSE_BYTES:
                    return None
                content.extend(chunk)
            body = content.decode("utf-8")
        finally:
            response.close()
        script = EMBED_DATA_PATTERN.search(body)
        if script is None:
            return None
        data = json.loads(script.group(1))
        entity = data["props"]["pageProps"]["state"]["data"]["entity"]
        if not isinstance(entity, Mapping) or entity.get("uri") != f"spotify:track:{track_id}":
            return None
        preview = entity.get("audioPreview")
        url = preview.get("url") if isinstance(preview, Mapping) else None
        return url if isinstance(url, str) and PREVIEW_URL_PATTERN.fullmatch(url) else None
    except (requests.RequestException, EditorialSourceError, ValueError, KeyError, TypeError):
        return None


def _enrich_audio_previews(items: Sequence[dict[str, Any]]) -> None:
    candidates = [item for item in items if item.get("spotifyTrackId")]
    if not candidates:
        return
    deadline = monotonic() + PREVIEW_ENRICHMENT_BUDGET_SECONDS
    executor = ThreadPoolExecutor(max_workers=MAX_PREVIEW_WORKERS)
    pending = {
        executor.submit(fetch_spotify_audio_preview, item["spotifyTrackId"], deadline=deadline): item
        for item in candidates
    }
    try:
        while pending:
            remaining = deadline - monotonic()
            if remaining <= 0:
                break
            completed, _ = wait(pending, timeout=remaining, return_when=FIRST_COMPLETED)
            if monotonic() >= deadline:
                break
            for future in completed:
                item = pending.pop(future)
                if preview_url := future.result():
                    item["audioPreviewUrl"] = preview_url
    finally:
        executor.shutdown(wait=False, cancel_futures=True)


def parse_watch_feed_title(payload: bytes) -> str:
    for header in _messages(payload, 1):
        for value in _messages(header, 2):
            title = _text(value, 2)
            if title:
                return title[:MAX_TEXT_LENGTH]
    return ""


def fetch_watch_feed_clips(client: SpotifyWebClient, playlist_id: str) -> tuple[str, list[dict[str, str]]]:
    headers = client._canvas_headers()
    headers.update(
        {
            "App-Platform": "Android",
            "User-Agent": WATCH_FEED_USER_AGENT,
            "Accept": "application/x-protobuf",
            "Content-Type": "application/x-protobuf",
        }
    )
    response = client._session.post(
        WATCH_FEED_URL,
        data=encode_watch_feed_request(playlist_id),
        headers=headers,
        timeout=25,
        stream=True,
    )
    try:
        if response.status_code != 200:
            raise SourceApiError(f"Spotify watch feed returned HTTP {response.status_code}.")
        payload = _read_bounded_response(response, MAX_WATCH_FEED_RESPONSE_BYTES)
    finally:
        response.close()
    return parse_watch_feed_title(payload), parse_watch_feed_clips(payload)


def build_watch_feed_clips(
    client: SpotifyWebClient,
    genres: Sequence[Mapping[str, Any]],
    youtube_music: YoutubeMusicWebClient | None = None,
) -> dict[str, Any]:
    output: list[dict[str, Any]] = []
    published_genres: list[dict[str, Any]] = []
    seen: set[str] = set()
    for genre in genres:
        genre_id = str(genre.get("id") or "").strip()
        playlist_id = str(genre.get("playlistId") or "").strip()
        if not GENRE_ID_PATTERN.match(genre_id) or not PLAYLIST_ID_PATTERN.match(playlist_id):
            raise ValueError("Watch feed genre configuration is invalid.")
        languages = [
            str(language).strip().lower()
            for language in genre.get("languages") or []
            if LANGUAGE_PATTERN.match(str(language).strip().lower())
        ]
        try:
            title, clips = fetch_watch_feed_clips(client, playlist_id)
        except (EditorialSourceError, requests.RequestException) as error:
            LOGGER.warning("Watch feed genre '%s' skipped: %s", genre_id, type(error).__name__)
            continue
        kept = 0
        for clip in clips:
            if kept >= MAX_CLIPS_PER_GENRE or clip["canvasUrl"] in seen:
                continue
            seen.add(clip["canvasUrl"])
            identity = hashlib.sha256(f"{clip['title']}\x1f{clip['artist']}".encode()).hexdigest()[:20]
            item: dict[str, Any] = {"id": f"levyra-{identity}", "genre": genre_id, **clip}
            output.append(item)
            kept += 1
        if kept:
            published_genres.append({"id": genre_id, "title": title or genre_id, "languages": languages})
    _enrich_audio_previews(output)
    payload = {"version": WATCH_FEED_CLIPS_VERSION, "genres": published_genres, "items": output}
    validate_watch_feed_clips(payload)
    return payload


def validate_watch_feed_clips(payload: Mapping[str, Any]) -> None:
    items = payload.get("items")
    if not isinstance(items, list) or not items:
        raise ValueError("Watch feed clips are empty.")
    genres = payload.get("genres")
    if not isinstance(genres, list):
        raise ValueError("Watch feed genres are missing.")
    genre_ids = {str(genre.get("id")) for genre in genres if isinstance(genre, Mapping)}
    allowed = {
        "id",
        "genre",
        "title",
        "artist",
        "artworkUrl",
        "canvasUrl",
        "audioVideoId",
        "spotifyTrackId",
        "audioPreviewUrl",
    }
    for item in items:
        if not isinstance(item, Mapping) or set(item) - allowed:
            raise ValueError("Watch feed clip has unexpected fields.")
        if item.get("genre") not in genre_ids:
            raise ValueError("Watch feed clip references an unknown genre.")
        if not CANVAS_URL_PATTERN.match(str(item.get("canvasUrl") or "")):
            raise ValueError("Watch feed clip has an invalid media destination.")
        artwork = str(item.get("artworkUrl") or "")
        if artwork and not ARTWORK_URL_PATTERN.match(artwork):
            raise ValueError("Watch feed clip has an invalid artwork destination.")
        if "spotifyTrackId" in item:
            track_id = item["spotifyTrackId"]
            if not isinstance(track_id, str) or not PLAYLIST_ID_PATTERN.fullmatch(track_id):
                raise ValueError("Watch feed clip has an invalid track identifier.")
        if "audioPreviewUrl" in item:
            preview = item["audioPreviewUrl"]
            if (
                "spotifyTrackId" not in item
                or not isinstance(preview, str)
                or not PREVIEW_URL_PATTERN.fullmatch(preview)
            ):
                raise ValueError("Watch feed clip has an invalid audio preview destination.")
    if FORBIDDEN_KEYS.search(json.dumps(payload)):
        raise ValueError("Watch feed clips contain credential or source identifiers.")


def write_watch_feed_clips(payload: Mapping[str, Any], output_path: Path) -> None:
    validate_watch_feed_clips(payload)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output_path.with_suffix(output_path.suffix + ".tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    temporary.replace(output_path)
