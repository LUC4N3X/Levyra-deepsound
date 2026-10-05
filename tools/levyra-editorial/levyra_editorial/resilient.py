from __future__ import annotations

import argparse
import logging
import os
import sys
import time
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from .collector import (
    CATALOG_SCHEMA_VERSION,
    EditorialClient,
    build_catalog,
    build_spotify_canvas_catalog,
    load_config,
    utc_now_iso,
    validate_catalog_file,
    validate_spotify_canvas_file,
    write_catalog,
    write_spotify_canvas_catalog,
)
from .explore import collect_spotify_explore_collections
from .models import Catalog, Collection
from .spotify import EditorialSourceError, SourceApiError, SpotifyWebClient
from .youtube_music import YoutubeMusicError, YoutubeMusicWebClient

LOGGER = logging.getLogger(__name__)
COLLECTION_PAUSE_SECONDS = 0.15


class CentralEditorialClient:
    def __init__(self, spotify: SpotifyWebClient, youtube_music: YoutubeMusicWebClient | None) -> None:
        self._spotify = spotify
        self._youtube_music = youtube_music

    def get_playlist_metadata(self, playlist_id: str) -> dict[str, Any]:
        return self._spotify.get_playlist_metadata(playlist_id)

    def iter_playlist_items(
        self,
        playlist_id: str,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        return self._spotify.iter_playlist_items(playlist_id, limit=limit)

    def resolve_playlist_id(
        self,
        query: str,
        market: str,
        title_hints: list[str],
    ) -> str:
        return self._spotify.resolve_playlist_id(query, market, title_hints)

    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        enriched = self._spotify.enrich_track_metadata(items)
        if self._youtube_music is not None:
            enriched = self._youtube_music.enrich_track_metadata(enriched)
        return enriched

    def close(self) -> None:
        self._spotify.close()
        if self._youtube_music is not None:
            self._youtube_music.close()


class PaxSenixCanvasClient:
    def __init__(self, spotify: SpotifyWebClient) -> None:
        self._spotify = spotify

    def get_canvas_urls(self, track_ids: list[str]) -> dict[str, str]:
        return self._spotify.get_paxsenix_canvas_urls(track_ids)


def build_resilient_catalog(
    config: Mapping[str, Any],
    client: EditorialClient,
    *,
    generated_at: str | None = None,
    pause_seconds: float = COLLECTION_PAUSE_SECONDS,
) -> Catalog:
    """Collect every required chart and isolate only explicitly optional markets."""
    raw_collections = config.get("collections")
    if not isinstance(raw_collections, list) or not raw_collections:
        raise ValueError("Collector config collections are missing.")

    collected: list[Collection] = []
    skipped_optional: list[str] = []
    required_ids = {
        str(item.get("id") or "").strip()
        for item in raw_collections
        if isinstance(item, dict) and item.get("optional") is not True
    }
    timestamp = generated_at or utc_now_iso()

    for index, item in enumerate(raw_collections):
        if not isinstance(item, dict):
            continue
        collection_id = str(item.get("id") or "").strip()
        optional = item.get("optional") is True
        try:
            single_catalog = build_catalog(
                {"collections": [item]},
                client,
                generated_at=timestamp,
            )
            collected.extend(single_catalog.collections)
        except SourceApiError as error:
            if not optional:
                raise
            skipped_optional.append(collection_id)
            LOGGER.warning(
                "Skipping optional editorial collection %s: %s",
                collection_id,
                error,
            )
        except ValueError as error:
            if not optional or "produced no usable tracks" not in str(error):
                raise
            skipped_optional.append(collection_id)
            LOGGER.warning("Skipping empty optional editorial collection %s.", collection_id)
        finally:
            if pause_seconds > 0 and index + 1 < len(raw_collections):
                time.sleep(pause_seconds)

    collected_ids = {collection.id for collection in collected}
    missing_required = sorted(required_ids - collected_ids)
    if missing_required:
        raise ValueError(
            "Required editorial collections are missing: " + ", ".join(missing_required)
        )
    if not collected:
        raise ValueError("No configured editorial collection produced usable tracks.")

    LOGGER.info(
        "Collected %d editorial collection(s); skipped %d optional collection(s).",
        len(collected),
        len(skipped_optional),
    )
    if skipped_optional:
        LOGGER.info("Unavailable optional collection ids: %s", ", ".join(skipped_optional))

    return Catalog(
        schema_version=CATALOG_SCHEMA_VERSION,
        generated_at=timestamp,
        collections=collected,
    )


def run_collection(
    config_path: Path,
    output_path: Path,
    canvas_output_path: Path | None = None,
    require_canvas: bool = False,
    paxsenix_canvas_only: bool = False,
) -> None:
    """Execute one resilient collector run using the repository Actions secret."""
    config = load_config(config_path)
    raw_secret = os.environ.get("LEVYRA_EDITORIAL_SP_DC", "")
    spotify = SpotifyWebClient(raw_secret)
    youtube_cookie = os.environ.get("LEVYRA_EDITORIAL_YTM_COOKIE", "").strip()
    youtube_music: YoutubeMusicWebClient | None = None
    if youtube_cookie:
        try:
            youtube_music = YoutubeMusicWebClient(youtube_cookie)
        except (YoutubeMusicError, ValueError) as error:
            LOGGER.warning("Central YouTube Music enrichment disabled: %s", error)
    else:
        LOGGER.warning("LEVYRA_EDITORIAL_YTM_COOKIE is not configured; publishing Spotify-only metadata.")
    client = CentralEditorialClient(spotify, youtube_music)
    canvas_count = 0
    try:
        catalog = build_resilient_catalog(config, client)
        explore_config = config.get("exploreDiscovery")
        explore_enabled = (
            isinstance(explore_config, Mapping)
            and explore_config.get("enabled") is True
        )
        explore_collections: list[Collection] = []
        if explore_enabled:
            track_limit = int(explore_config.get("trackLimit") or 16)
            try:
                explore_collections = collect_spotify_explore_collections(
                    spotify,
                    youtube_music,
                    track_limit=track_limit,
                )
            except (EditorialSourceError, OSError, RuntimeError, ValueError) as error:
                LOGGER.warning(
                    "Spotify-first Explore collection skipped; runtime YouTube Music fallback remains active: %s",
                    type(error).__name__,
                )
        if explore_collections:
            catalog = Catalog(
                schema_version=catalog.schema_version,
                generated_at=catalog.generated_at,
                collections=[*catalog.collections, *explore_collections],
            )
        write_catalog(catalog, output_path)
        if canvas_output_path is not None:
            try:
                canvas_client = PaxSenixCanvasClient(spotify) if paxsenix_canvas_only else spotify
                canvas_catalog = build_spotify_canvas_catalog(catalog, canvas_client)
                write_spotify_canvas_catalog(canvas_catalog, canvas_output_path)
                canvas_count = len(canvas_catalog["items"])
            except (EditorialSourceError, OSError, ValueError) as error:
                if require_canvas:
                    raise
                LOGGER.warning(
                    "Spotify Canvas refresh skipped; the last published source remains active: %s",
                    type(error).__name__,
                )
    finally:
        client.close()
    LOGGER.info(
        "Editorial catalog written to %s with %d collection(s).",
        output_path,
        len(catalog.collections),
    )
    if canvas_output_path is not None and canvas_count:
        LOGGER.info(
            "Spotify Canvas catalog written to %s with %d item(s).",
            canvas_output_path,
            canvas_count,
        )


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate Levyra's resilient editorial metadata catalog."
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("tools/levyra-editorial/config.json"),
        help="Path to the checked-in collection configuration.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("build/editorial/catalog.json"),
        help="Path for the generated catalog.",
    )
    validation = parser.add_mutually_exclusive_group()
    validation.add_argument(
        "--validate",
        type=Path,
        help="Validate an existing catalog instead of collecting remote data.",
    )
    validation.add_argument(
        "--validate-canvas",
        type=Path,
        help="Validate an existing sanitized Spotify Canvas catalog.",
    )
    parser.add_argument(
        "--canvas-output",
        type=Path,
        help="Optional path for the best-effort sanitized Spotify Canvas catalog.",
    )
    parser.add_argument(
        "--require-canvas",
        action="store_true",
        help="Fail when live Canvas verification cannot produce a sanitized catalog.",
    )
    parser.add_argument(
        "--paxsenix-canvas-only",
        action="store_true",
        help="Use only the PaxSenix-compatible resolver for live Canvas diagnostics.",
    )
    parser.add_argument(
        "--serve",
        action="store_true",
        help="Start the on-demand Canvas resolver HTTP service.",
    )
    parser.add_argument(
        "--host",
        type=str,
        default="127.0.0.1",
        help="Host to bind the on-demand Canvas resolver HTTP service.",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8080,
        help="Port to bind the on-demand Canvas resolver HTTP service.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    """Command-line entry point."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    args = _build_parser().parse_args(argv)
    try:
        if args.validate:
            validate_catalog_file(args.validate)
            LOGGER.info("Catalog validation succeeded: %s", args.validate)
        elif args.validate_canvas:
            validate_spotify_canvas_file(args.validate_canvas)
            LOGGER.info("Spotify Canvas validation succeeded: %s", args.validate_canvas)
        elif args.serve:
            from .resolver import CanvasResolverService, create_resolver_server

            raw_secret = os.environ.get("LEVYRA_EDITORIAL_SP_DC", "")
            spotify = SpotifyWebClient(raw_secret)
            service = CanvasResolverService(spotify)
            server = create_resolver_server(service, host=args.host, port=args.port)
            LOGGER.info("Starting on-demand Canvas resolver on http://%s:%d", args.host, args.port)
            try:
                server.serve_forever()
            except KeyboardInterrupt:
                LOGGER.info("Stopping on-demand Canvas resolver.")
            finally:
                server.server_close()
                spotify.close()
        else:
            if args.require_canvas and args.canvas_output is None:
                raise ValueError("--require-canvas requires --canvas-output.")
            if args.paxsenix_canvas_only and args.canvas_output is None:
                raise ValueError("--paxsenix-canvas-only requires --canvas-output.")
            run_collection(
                config_path=args.config,
                output_path=args.output,
                canvas_output_path=args.canvas_output,
                require_canvas=args.require_canvas,
                paxsenix_canvas_only=args.paxsenix_canvas_only,
            )
        return 0
    except (EditorialSourceError, OSError, ValueError) as error:
        LOGGER.error("%s", error)
        return 1


if __name__ == "__main__":
    sys.exit(main())
