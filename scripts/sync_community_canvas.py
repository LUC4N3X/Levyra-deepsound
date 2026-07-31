#!/usr/bin/env python3
"""Normalize the upstream community canvas catalog into Levyra's mirrored schema."""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

UPSTREAM_URL = "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json"
ALLOWED_HOSTS = ("vivimusicanvas.mkmdevilmi.workers.dev", "vivimusicanvas-mtih.vercel.app")
ALLOWED_EXTENSIONS = (".mp4", ".m3u8")
DECLARED_SCOPES = {"track": "track", "song": "track", "album": "album"}
ISRC_PATTERN = re.compile(r"^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
USER_AGENT = "Levyra-community-canvas-mirror/1.0 (+https://github.com/LUC4N3X/Levyra-deepsound)"
MAX_PAYLOAD_BYTES = 1024 * 1024
CATALOG_VERSION = 1
MAX_REPORTED_REJECTS = 20


class CatalogError(RuntimeError):
    pass


def fetch_payload(url: str, timeout: float) -> str:
    request = urllib.request.Request(
        url,
        headers={"Accept": "application/json", "User-Agent": USER_AGENT},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read(MAX_PAYLOAD_BYTES + 1)
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        raise CatalogError(f"Unable to download {url}: {error}") from error
    if len(raw) > MAX_PAYLOAD_BYTES:
        raise CatalogError(f"Catalog at {url} exceeds {MAX_PAYLOAD_BYTES} bytes")
    return raw.decode("utf-8")


def text_field(item: dict[str, Any], key: str) -> str:
    value = item.get(key)
    return value.strip() if isinstance(value, str) else ""


def positive_dimension(item: dict[str, Any], key: str) -> int | None:
    value = item.get(key)
    if isinstance(value, bool) or not isinstance(value, int):
        return None
    return value if value > 0 else None


def media_url_problem(raw_url: str) -> str | None:
    try:
        parts = urlsplit(raw_url)
        port = parts.port
    except ValueError:
        return "malformed url"
    if parts.scheme != "https":
        return "non-https url"
    if parts.username or parts.password:
        return "url with credentials"
    if port not in (None, 443):
        return "non-standard port"
    host = (parts.hostname or "").lower()
    if not host:
        return "missing host"
    if host not in ALLOWED_HOSTS:
        return f"unapproved host {host}"
    if not parts.path.lower().endswith(ALLOWED_EXTENSIONS):
        return "unsupported media type"
    return None


def normalize_item(item: Any) -> tuple[dict[str, Any] | None, str | None]:
    if not isinstance(item, dict):
        return None, "entry is not an object"
    song = text_field(item, "song")
    artist = text_field(item, "artist")
    album = text_field(item, "album")
    raw_url = text_field(item, "url")
    if not song or not artist or not album or not raw_url:
        return None, "missing required field"
    problem = media_url_problem(raw_url)
    if problem is not None:
        return None, problem

    normalized: dict[str, Any] = {
        "song": song,
        "artist": artist,
        "album": album,
        "url": raw_url,
    }
    scope = DECLARED_SCOPES.get(text_field(item, "scope").lower())
    if scope is not None:
        normalized["scope"] = scope
    isrc = text_field(item, "isrc").upper()
    if ISRC_PATTERN.match(isrc):
        normalized["isrc"] = isrc
    width = positive_dimension(item, "width")
    height = positive_dimension(item, "height")
    if width is not None and height is not None:
        normalized["width"] = width
        normalized["height"] = height
    return normalized, None


def normalize_catalog(payload: str) -> tuple[list[dict[str, Any]], list[str], list[str]]:
    try:
        root = json.loads(payload)
    except json.JSONDecodeError as error:
        raise CatalogError(f"Upstream catalog is not valid JSON: {error}") from error
    if not isinstance(root, dict) or not isinstance(root.get("items"), list):
        raise CatalogError("Upstream catalog does not contain an items array")

    items: list[dict[str, Any]] = []
    rejects: list[str] = []
    blocked_hosts: list[str] = []
    seen: set[tuple[str, ...]] = set()
    for index, raw_item in enumerate(root["items"]):
        normalized, problem = normalize_item(raw_item)
        if normalized is None:
            rejects.append(f"item {index}: {problem}")
            if problem is not None and problem.startswith("unapproved host"):
                blocked_hosts.append(problem.removeprefix("unapproved host "))
            continue
        key = (
            normalized["song"].casefold(),
            normalized["artist"].casefold(),
            normalized["album"].casefold(),
            normalized["url"],
            normalized.get("scope", ""),
        )
        if key in seen:
            rejects.append(f"item {index}: duplicate entry")
            continue
        seen.add(key)
        items.append(normalized)

    items.sort(key=lambda entry: (
        entry["artist"].casefold(),
        entry["album"].casefold(),
        entry["song"].casefold(),
        entry["url"],
    ))
    return items, rejects, sorted(set(blocked_hosts))


def write_catalog(path: Path, source: str, items: list[dict[str, Any]]) -> None:
    document = {
        "version": CATALOG_VERSION,
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source": source,
        "items": items,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-url", default=UPSTREAM_URL)
    parser.add_argument("--input", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--min-entries", type=int, default=150)
    parser.add_argument("--timeout", type=float, default=30.0)
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    try:
        payload = (
            arguments.input.read_text(encoding="utf-8")
            if arguments.input is not None
            else fetch_payload(arguments.source_url, arguments.timeout)
        )
        items, rejects, blocked_hosts = normalize_catalog(payload)
    except (CatalogError, OSError, UnicodeDecodeError) as error:
        print(f"Community canvas sync failed: {error}", file=sys.stderr)
        return 1

    for reject in rejects[:MAX_REPORTED_REJECTS]:
        print(f"Rejected {reject}")
    if len(rejects) > MAX_REPORTED_REJECTS:
        print(f"Rejected {len(rejects) - MAX_REPORTED_REJECTS} more entries")

    if blocked_hosts:
        print(
            "Community canvas sync failed: upstream published media on hosts Levyra does not allow: "
            + ", ".join(blocked_hosts),
            file=sys.stderr,
        )
        return 1

    if len(items) < arguments.min_entries:
        print(
            f"Community canvas sync failed: only {len(items)} usable entries, "
            f"expected at least {arguments.min_entries}",
            file=sys.stderr,
        )
        return 1

    source = arguments.source_url if arguments.input is None else arguments.input.as_posix()
    write_catalog(arguments.output, source, items)
    album_scoped = sum(1 for entry in items if entry.get("scope") == "album")
    with_isrc = sum(1 for entry in items if "isrc" in entry)
    print(
        f"Normalized {len(items)} entries "
        f"({album_scoped} album-scoped, {with_isrc} with ISRC, {len(rejects)} rejected) "
        f"into {arguments.output}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
