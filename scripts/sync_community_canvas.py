#!/usr/bin/env python3
"""Merge and normalize community canvas catalogs into Levyra's mirrored schema."""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

UPSTREAM_URL = "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json"
ALLOWED_HOSTS = ("vivimusicanvas.mkmdevilmi.workers.dev", "vivimusicanvas-mtih.vercel.app")
ALLOWED_EXTENSIONS = (".mp4", ".m3u8")
DECLARED_SCOPES = {"track": "track", "song": "track", "album": "album"}
ISRC_PATTERN = re.compile(r"^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
USER_AGENT = "Levyra-community-canvas-mirror/2.0 (+https://github.com/LUC4N3X/Levyra-deepsound)"
MAX_PAYLOAD_BYTES = 64 * 1024 * 1024
CATALOG_VERSION = 1
SOURCES_VERSION = 1
MAX_REPORTED_REJECTS = 20


class CatalogError(RuntimeError):
    pass


@dataclass(frozen=True)
class SourceSpec:
    name: str
    location: str
    required: bool
    local_path: Path | None = None


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


def read_local_payload(path: Path) -> str:
    try:
        raw = path.read_bytes()
    except OSError as error:
        raise CatalogError(f"Unable to read {path}: {error}") from error
    if len(raw) > MAX_PAYLOAD_BYTES:
        raise CatalogError(f"Catalog at {path} exceeds {MAX_PAYLOAD_BYTES} bytes")
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError as error:
        raise CatalogError(f"Catalog at {path} is not UTF-8: {error}") from error


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
    if ISRC_PATTERN.fullmatch(isrc):
        normalized["isrc"] = isrc
    width = positive_dimension(item, "width")
    height = positive_dimension(item, "height")
    if width is not None and height is not None:
        normalized["width"] = width
        normalized["height"] = height
    return normalized, None


def normalize_catalog(payload: str, source_name: str) -> tuple[list[dict[str, Any]], list[str], list[str]]:
    try:
        root = json.loads(payload)
    except json.JSONDecodeError as error:
        raise CatalogError(f"{source_name} is not valid JSON: {error}") from error
    if not isinstance(root, dict) or not isinstance(root.get("items"), list):
        raise CatalogError(f"{source_name} does not contain an items array")

    items: list[dict[str, Any]] = []
    rejects: list[str] = []
    blocked_hosts: list[str] = []
    seen: set[tuple[str, ...]] = set()
    for index, raw_item in enumerate(root["items"]):
        normalized, problem = normalize_item(raw_item)
        if normalized is None:
            rejects.append(f"{source_name} item {index}: {problem}")
            if problem is not None and problem.startswith("unapproved host"):
                blocked_hosts.append(problem.removeprefix("unapproved host "))
            continue
        key = catalog_entry_key(normalized)
        if key in seen:
            rejects.append(f"{source_name} item {index}: duplicate entry")
            continue
        seen.add(key)
        items.append(normalized)
    return items, rejects, sorted(set(blocked_hosts))


def catalog_entry_key(entry: dict[str, Any]) -> tuple[str, ...]:
    return (
        str(entry["song"]).casefold(),
        str(entry["artist"]).casefold(),
        str(entry["album"]).casefold(),
        str(entry["url"]),
        str(entry.get("scope", "")),
    )


def load_sources_file(path: Path) -> list[SourceSpec]:
    try:
        root = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise CatalogError(f"Unable to read source configuration {path}: {error}") from error
    if not isinstance(root, dict) or root.get("version") != SOURCES_VERSION:
        raise CatalogError(f"Source configuration must declare version {SOURCES_VERSION}")
    raw_sources = root.get("sources")
    if not isinstance(raw_sources, list) or not raw_sources:
        raise CatalogError("Source configuration must contain a non-empty sources array")

    sources: list[SourceSpec] = []
    root_dir = path.parent.parent.resolve()
    for index, raw_source in enumerate(raw_sources):
        if not isinstance(raw_source, dict):
            raise CatalogError(f"Source {index} is not an object")
        name = text_field(raw_source, "name") or f"source-{index + 1}"
        url = text_field(raw_source, "url")
        local = text_field(raw_source, "path")
        if bool(url) == bool(local):
            raise CatalogError(f"Source {name} must declare exactly one of url or path")
        required = raw_source.get("required", True)
        if not isinstance(required, bool):
            raise CatalogError(f"Source {name} required must be boolean")
        if url:
            parts = urlsplit(url)
            if parts.scheme != "https" or not parts.hostname:
                raise CatalogError(f"Source {name} URL must be HTTPS")
            sources.append(SourceSpec(name=name, location=url, required=required))
            continue
        relative_path = Path(local)
        if relative_path.is_absolute() or ".." in relative_path.parts:
            raise CatalogError(f"Source {name} path must stay inside the repository")
        resolved = (root_dir / relative_path).resolve()
        if root_dir not in resolved.parents and resolved != root_dir:
            raise CatalogError(f"Source {name} path escapes the repository")
        sources.append(
            SourceSpec(
                name=name,
                location=relative_path.as_posix(),
                required=required,
                local_path=resolved,
            )
        )
    return sources


def resolve_sources(arguments: argparse.Namespace) -> list[SourceSpec]:
    if arguments.input is not None:
        return [
            SourceSpec(
                name=arguments.input.stem,
                location=arguments.input.as_posix(),
                required=True,
                local_path=arguments.input.resolve(),
            )
        ]
    if arguments.sources_file is not None:
        return load_sources_file(arguments.sources_file)
    source_url = arguments.source_url or UPSTREAM_URL
    return [SourceSpec(name="upstream", location=source_url, required=True)]


def merge_sources(
    sources: list[SourceSpec],
    timeout: float,
) -> tuple[list[dict[str, Any]], list[str], list[str], list[dict[str, Any]]]:
    merged: list[dict[str, Any]] = []
    rejects: list[str] = []
    blocked_hosts: list[str] = []
    metadata: list[dict[str, Any]] = []
    seen: set[tuple[str, ...]] = set()

    for source in sources:
        try:
            payload = (
                read_local_payload(source.local_path)
                if source.local_path is not None
                else fetch_payload(source.location, timeout)
            )
            items, source_rejects, source_blocked_hosts = normalize_catalog(payload, source.name)
        except CatalogError as error:
            if source.required:
                raise
            print(f"Optional community canvas source skipped: {error}", file=sys.stderr)
            metadata.append(
                {
                    "name": source.name,
                    "location": source.location,
                    "required": False,
                    "entries": 0,
                    "status": "unavailable",
                }
            )
            continue

        rejects.extend(source_rejects)
        blocked_hosts.extend(source_blocked_hosts)
        accepted = 0
        for item in items:
            key = catalog_entry_key(item)
            if key in seen:
                rejects.append(f"{source.name}: duplicate across sources")
                continue
            seen.add(key)
            merged.append(item)
            accepted += 1
        metadata.append(
            {
                "name": source.name,
                "location": source.location,
                "required": source.required,
                "entries": accepted,
                "status": "ok",
            }
        )

    merged.sort(
        key=lambda entry: (
            entry["artist"].casefold(),
            entry["album"].casefold(),
            entry["song"].casefold(),
            entry["url"],
        )
    )
    return merged, rejects, sorted(set(blocked_hosts)), metadata


def write_catalog(
    path: Path,
    sources: list[dict[str, Any]],
    items: list[dict[str, Any]],
) -> None:
    document = {
        "version": CATALOG_VERSION,
        "generatedAt": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "sources": sources,
        "items": items,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-url")
    parser.add_argument("--sources-file", type=Path)
    parser.add_argument("--input", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--min-entries", type=int, default=150)
    parser.add_argument("--timeout", type=float, default=30.0)
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    selected_modes = sum(
        value is not None
        for value in (arguments.source_url, arguments.sources_file, arguments.input)
    )
    if selected_modes > 1:
        print("Choose only one of --source-url, --sources-file or --input", file=sys.stderr)
        return 1
    try:
        sources = resolve_sources(arguments)
        items, rejects, blocked_hosts, source_metadata = merge_sources(sources, arguments.timeout)
    except CatalogError as error:
        print(f"Community canvas sync failed: {error}", file=sys.stderr)
        return 1

    for reject in rejects[:MAX_REPORTED_REJECTS]:
        print(f"Rejected {reject}")
    if len(rejects) > MAX_REPORTED_REJECTS:
        print(f"Rejected {len(rejects) - MAX_REPORTED_REJECTS} more entries")

    if blocked_hosts:
        print(
            "Community canvas sync failed: a configured source published media on hosts Levyra "
            "does not allow: " + ", ".join(blocked_hosts),
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

    write_catalog(arguments.output, source_metadata, items)
    album_scoped = sum(1 for entry in items if entry.get("scope") == "album")
    with_isrc = sum(1 for entry in items if "isrc" in entry)
    healthy_sources = sum(1 for source in source_metadata if source["status"] == "ok")
    print(
        f"Normalized {len(items)} entries from {healthy_sources}/{len(source_metadata)} sources "
        f"({album_scoped} album-scoped, {with_isrc} with ISRC, {len(rejects)} rejected) "
        f"into {arguments.output}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
