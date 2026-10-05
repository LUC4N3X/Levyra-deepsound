from __future__ import annotations

import hashlib
import logging
import re
from collections.abc import Mapping
from dataclasses import dataclass
from typing import Any

import requests

from .collector import normalize_playlist_items
from .models import Collection
from .spotify import EditorialSourceError, SpotifyWebClient
from .youtube_music import DEFAULT_USER_AGENT, HOME_URL, ORIGIN, YoutubeMusicWebClient

LOGGER = logging.getLogger(__name__)

BROWSE_URL = f"{ORIGIN}/youtubei/v1/browse"
MOODS_BROWSE_ID = "FEmusic_moods_and_genres"
DEFAULT_TRACK_LIMIT = 16
YOUTUBE_MAPPING_LIMIT = 8
BROWSE_PARAMS_PATTERN = re.compile(r"^[A-Za-z0-9_./=+\-]+$")


@dataclass(frozen=True)
class ExploreSeed:
    title: str
    params: str
    section: str
    section_index: int

    @property
    def kind(self) -> str:
        return "mood" if self.section_index == 0 else "genre"


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
    simple = value.get("simpleText")
    if isinstance(simple, str) and simple.strip():
        return simple.strip()
    runs = value.get("runs")
    if not isinstance(runs, list):
        return ""
    return "".join(
        str(run.get("text") or "")
        for run in runs
        if isinstance(run, Mapping)
    ).strip()


def _browse_endpoint(value: Any) -> Mapping[str, Any] | None:
    if not isinstance(value, Mapping):
        return None
    for key in ("clickCommand", "navigationEndpoint"):
        command = value.get(key)
        if not isinstance(command, Mapping):
            continue
        endpoint = command.get("browseEndpoint")
        if isinstance(endpoint, Mapping):
            return endpoint
    return None


def _parse_navigation_button(
    renderer: Mapping[str, Any],
    *,
    section: str,
    section_index: int,
) -> ExploreSeed | None:
    title = _runs_text(renderer.get("buttonText")) or _runs_text(renderer.get("title"))
    endpoint = _browse_endpoint(renderer)
    params = str(endpoint.get("params") or "") if endpoint else ""
    if (
        not title
        or not params
        or len(params) > 1024
        or BROWSE_PARAMS_PATTERN.fullmatch(params) is None
    ):
        return None
    return ExploreSeed(
        title=title,
        params=params,
        section=section,
        section_index=section_index,
    )


def parse_youtube_explore_categories(payload: Mapping[str, Any]) -> list[ExploreSeed]:
    output: dict[str, ExploreSeed] = {}
    grids = [
        node["gridRenderer"]
        for node in _walk(payload)
        if isinstance(node.get("gridRenderer"), Mapping)
    ]
    for section_index, grid in enumerate(grids):
        header = grid.get("header")
        header_renderer = header.get("gridHeaderRenderer") if isinstance(header, Mapping) else None
        section = (
            _runs_text(header_renderer.get("title"))
            if isinstance(header_renderer, Mapping)
            else ""
        )
        items = grid.get("items")
        for node in _walk(items):
            renderer = node.get("musicNavigationButtonRenderer")
            if not isinstance(renderer, Mapping):
                continue
            seed = _parse_navigation_button(
                renderer,
                section=section,
                section_index=section_index,
            )
            if seed is not None:
                output.setdefault(seed.params, seed)

    if output:
        return list(output.values())

    for node in _walk(payload):
        renderer = node.get("musicNavigationButtonRenderer")
        if not isinstance(renderer, Mapping):
            continue
        seed = _parse_navigation_button(renderer, section="", section_index=-1)
        if seed is not None:
            output.setdefault(seed.params, seed)
    return list(output.values())


def discover_youtube_explore_categories(
    *,
    session: requests.Session | None = None,
    timeout_seconds: float = 8.0,
) -> list[ExploreSeed]:
    owned_session = session is None
    client = session or requests.Session()
    client.headers.update(
        {
            "User-Agent": DEFAULT_USER_AGENT,
            "Accept": "application/json,text/html;q=0.9,*/*;q=0.8",
        }
    )
    try:
        response = client.get(HOME_URL, timeout=timeout_seconds)
        response.raise_for_status()
        body = response.text
        innertube_key = re.search(r'"INNERTUBE_API_KEY":"([^"\\]+)"', body)
        version = re.search(r'"INNERTUBE_CLIENT_VERSION":"([^"\\]+)"', body)
        visitor = re.search(r'"VISITOR_DATA":"([^"\\]+)"', body)
        if innertube_key is None or version is None:
            raise RuntimeError("Unable to bootstrap YouTube Music discovery.")

        visitor_data = visitor.group(1) if visitor else ""
        payload = {
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": version.group(1),
                    "hl": "en",
                    "gl": "US",
                    "platform": "DESKTOP",
                    "visitorData": visitor_data,
                }
            },
            "browseId": MOODS_BROWSE_ID,
        }
        headers = {
            "Origin": ORIGIN,
            "Referer": HOME_URL,
            "Content-Type": "application/json",
            "X-Youtube-Client-Name": "67",
            "X-Youtube-Client-Version": version.group(1),
        }
        if visitor_data:
            headers["X-Goog-Visitor-Id"] = visitor_data
        browse = client.post(
            BROWSE_URL,
            params={"key": innertube_key.group(1), "prettyPrint": "false"},
            headers=headers,
            json=payload,
            timeout=timeout_seconds,
        )
        browse.raise_for_status()
        data = browse.json()
        if not isinstance(data, Mapping):
            raise RuntimeError("YouTube Music discovery returned invalid JSON.")
        categories = parse_youtube_explore_categories(data)
        if not categories:
            raise RuntimeError("YouTube Music discovery returned no categories.")
        return categories
    finally:
        if owned_session:
            client.close()


def _first_image_url(value: Any) -> str | None:
    if not isinstance(value, list):
        return None
    for image in value:
        if not isinstance(image, Mapping):
            continue
        url = str(image.get("url") or "").strip()
        if url.startswith("https://"):
            return url
    return None


def _collection_id(seed: ExploreSeed) -> str:
    digest = hashlib.sha256(seed.params.encode("utf-8")).hexdigest()[:16]
    return f"{seed.kind}-{digest}"


def _playlist_total_tracks(metadata: Mapping[str, Any], fallback: int) -> int:
    tracks = metadata.get("tracks")
    if not isinstance(tracks, Mapping):
        return fallback
    total = tracks.get("total")
    if isinstance(total, bool) or not isinstance(total, int) or total < 0:
        return fallback
    return total


def collect_spotify_explore_collections(
    spotify: SpotifyWebClient,
    youtube_music: YoutubeMusicWebClient | None,
    *,
    track_limit: int = DEFAULT_TRACK_LIMIT,
) -> list[Collection]:
    seeds = discover_youtube_explore_categories()
    output: list[Collection] = []
    bounded_limit = max(8, min(track_limit, 24))

    for seed in seeds:
        try:
            playlist_id = spotify.resolve_playlist_id(
                seed.title,
                "US",
                [seed.title],
            )
            metadata = spotify.get_playlist_metadata(playlist_id)
            raw_items = spotify.iter_playlist_items(
                playlist_id,
                limit=bounded_limit,
            )[:bounded_limit]
            raw_items = spotify.enrich_track_metadata(raw_items)
            if youtube_music is not None and raw_items:
                youtube_music.enrich_track_metadata(
                    raw_items[: min(YOUTUBE_MAPPING_LIMIT, len(raw_items))]
                )
            tracks = normalize_playlist_items(raw_items)
            if not tracks:
                continue
            output.append(
                Collection(
                    id=_collection_id(seed),
                    kind=seed.kind,
                    market="GLOBAL",
                    title=seed.title,
                    description=str(metadata.get("description") or "").strip()[:500],
                    source_id=playlist_id,
                    source_url=(
                        metadata.get("external_urls", {}).get("spotify")
                        if isinstance(metadata.get("external_urls"), Mapping)
                        else None
                    ),
                    artwork_url=_first_image_url(metadata.get("images")),
                    snapshot_id=str(metadata.get("snapshot_id") or "").strip() or None,
                    total_source_items=_playlist_total_tracks(metadata, len(raw_items)),
                    tracks=tracks,
                    youtube_params=seed.params,
                    section_index=seed.section_index,
                    section_title=seed.section or None,
                )
            )
        except (EditorialSourceError, requests.RequestException, RuntimeError, ValueError) as error:
            LOGGER.info(
                "Explore category %s will use the YouTube Music fallback: %s",
                seed.title,
                type(error).__name__,
            )

    LOGGER.info(
        "Collected Spotify editorial matches for %d of %d Explore categories.",
        len(output),
        len(seeds),
    )
    return output
