from __future__ import annotations

import hashlib
import json
import logging
import re
from collections.abc import Mapping, Sequence
from pathlib import Path
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
from .youtube_music import YoutubeMusicError, YoutubeMusicWebClient

LOGGER = logging.getLogger(__name__)

WATCH_FEED_URL = "https://spclient.wg.spotify.com/watch-feed-view/v1/feed"
WATCH_FEED_USER_AGENT = "Spotify/9.1.94.1929 Android/34 (Pixel 8)"
MAX_WATCH_FEED_RESPONSE_BYTES = 4 * 1024 * 1024
WATCH_FEED_CLIPS_VERSION = 1
CANVAS_CONTENT_TYPE = "spotify.watchfeed.component.content.v1.CanvasContent"
ENTITY_ROW_TYPE = "spotify.watchfeed.component.item.v2.BoxedEntityRowComponentV2"
CANVAS_URL_PATTERN = re.compile(r"^https://canvaz\.scdn\.co/[A-Za-z0-9/_.\-]+\.cnvs\.mp4$")
ARTWORK_URL_PATTERN = re.compile(r"^https://i\.scdn\.co/image/[A-Za-z0-9]+$")
GENRE_ID_PATTERN = re.compile(r"^[a-z0-9-]{2,32}$")
PLAYLIST_ID_PATTERN = re.compile(r"^[A-Za-z0-9]{22}$")
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
    return [value for field, wire, value in fields if field == number and wire == 2 and isinstance(value, bytes)]


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
            clips.append(
                {
                    "title": title[:MAX_TEXT_LENGTH],
                    "artist": artist[:MAX_TEXT_LENGTH],
                    "artworkUrl": artwork if ARTWORK_URL_PATTERN.match(artwork) else "",
                    "canvasUrl": canvas_url,
                }
            )
    return clips


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
            if youtube_music is not None:
                try:
                    match = youtube_music.resolve(clip["title"], clip["artist"].split(",")[0], 0)
                except (YoutubeMusicError, requests.RequestException, ValueError):
                    match = None
                video_id = (match or {}).get("audioVideoId")
                if isinstance(video_id, str) and re.fullmatch(r"[A-Za-z0-9_-]{11}", video_id):
                    item["audioVideoId"] = video_id
            output.append(item)
            kept += 1
        if kept:
            published_genres.append({"id": genre_id, "title": title or genre_id, "languages": languages})
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
    allowed = {"id", "genre", "title", "artist", "artworkUrl", "canvasUrl", "audioVideoId"}
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
    if FORBIDDEN_KEYS.search(json.dumps(payload)):
        raise ValueError("Watch feed clips contain credential or source identifiers.")


def write_watch_feed_clips(payload: Mapping[str, Any], output_path: Path) -> None:
    validate_watch_feed_clips(payload)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output_path.with_suffix(output_path.suffix + ".tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    temporary.replace(output_path)
