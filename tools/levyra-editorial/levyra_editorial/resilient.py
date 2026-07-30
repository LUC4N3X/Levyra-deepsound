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
    load_config,
    utc_now_iso,
    validate_catalog_file,
    write_catalog,
)
from .models import Catalog, Collection
from .spotify import EditorialSourceError, SourceApiError, SpotifyWebClient

LOGGER = logging.getLogger(__name__)
COLLECTION_PAUSE_SECONDS = 0.15


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
