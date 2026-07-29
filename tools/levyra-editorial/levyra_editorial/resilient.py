from __future__ import annotations

import argparse
import logging
import os
import sys
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from .collector import (
    CATALOG_SCHEMA_VERSION,
    build_catalog,
    load_config,
    utc_now_iso,
    validate_catalog_file,
    write_catalog,
)
from .models import Catalog, Collection
from .spotify import EditorialSourceError, SourceApiError, SpotifyWebClient

LOGGER = logging.getLogger(__name__)


def build_resilient_catalog(
    config: Mapping[str, Any],
    client: SpotifyWebClient,
    *,
    generated_at: str | None = None,
) -> Catalog:
    """Collect every available chart while isolating unavailable country playlists."""
    raw_collections = config.get("collections")
    if not isinstance(raw_collections, list) or not raw_collections:
        raise ValueError("Collector config collections are missing.")

    allow_partial = config.get("allowPartial") is True
    collected: list[Collection] = []
    skipped: list[str] = []
    timestamp = generated_at or utc_now_iso()

    for item in raw_collections:
        if not isinstance(item, dict):
            continue
        collection_id = str(item.get("id") or "").strip()
        single_config = {"collections": [item]}
        try:
            single_catalog = build_catalog(
                single_config,
                client,
                generated_at=timestamp,
            )
        except SourceApiError as error:
            if not allow_partial:
                raise
            skipped.append(collection_id)
            LOGGER.warning(
                "Skipping unavailable editorial collection %s: %s",
                collection_id,
                error,
            )
            continue
        except ValueError as error:
            if not allow_partial or "produced no usable tracks" not in str(error):
                raise
            skipped.append(collection_id)
            LOGGER.warning(
                "Skipping empty editorial collection %s.",
                collection_id,
            )
            continue
        collected.extend(single_catalog.collections)

    if not collected:
        raise ValueError("No configured editorial collection produced usable tracks.")

    LOGGER.info(
        "Collected %d editorial collection(s); skipped %d unavailable collection(s).",
        len(collected),
        len(skipped),
    )
    if skipped:
        LOGGER.info("Unavailable collection ids: %s", ", ".join(skipped))

    return Catalog(
        schema_version=CATALOG_SCHEMA_VERSION,
        generated_at=timestamp,
        collections=collected,
    )


def run_collection(config_path: Path, output_path: Path) -> None:
    """Execute one resilient collector run using the repository Actions secret."""
    config = load_config(config_path)
    raw_secret = os.environ.get("LEVYRA_EDITORIAL_SP_DC", "")
    client = SpotifyWebClient(raw_secret)
    try:
        catalog = build_resilient_catalog(config, client)
        write_catalog(catalog, output_path)
    finally:
        client.close()
    LOGGER.info(
        "Editorial catalog written to %s with %d collection(s).",
        output_path,
        len(catalog.collections),
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
    parser.add_argument(
        "--validate",
        type=Path,
        help="Validate an existing catalog instead of collecting remote data.",
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
        else:
            run_collection(args.config, args.output)
        return 0
    except (EditorialSourceError, OSError, ValueError) as error:
        LOGGER.error("%s", error)
        return 1


if __name__ == "__main__":
    sys.exit(main())
