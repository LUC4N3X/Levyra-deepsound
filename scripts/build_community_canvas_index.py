#!/usr/bin/env python3
"""Build a compact hash-sharded lookup index for the community canvas catalog."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import re
import shutil
import unicodedata
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

INDEX_VERSION = 2
MIN_PREFIX_CHARS = 2
MAX_PREFIX_CHARS = 5
DEFAULT_TARGET_SHARD_BYTES = 96 * 1024
DEFAULT_MAX_SHARD_BYTES = 192 * 1024
ISRC_PATTERN = re.compile(r"^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
APOSTROPHES = {"’", "'", "`", "´"}
BRACKETS = set("()[]{}")


class IndexBuildError(RuntimeError):
    pass


def normalize_lookup_text(value: str) -> str:
    output: list[str] = []
    pending_space = False
    for char in value.lower():
        if char in APOSTROPHES:
            continue
        if char in BRACKETS:
            pending_space = bool(output)
            continue
        category = unicodedata.category(char)
        if category.startswith("L") or category.startswith("N"):
            if pending_space and output:
                output.append(" ")
            output.append(char)
            pending_space = False
        else:
            pending_space = bool(output)
    return "".join(output).strip()


def artist_signature(value: str) -> str:
    return normalize_lookup_text(value)


def track_lookup_key(item: dict[str, Any]) -> str:
    return "t|" + "|".join(
        (
            normalize_lookup_text(str(item["song"])),
            artist_signature(str(item["artist"])),
            normalize_lookup_text(str(item["album"])),
        )
    )


def album_lookup_key(item: dict[str, Any]) -> str:
    return "a|" + "|".join(
        (
            artist_signature(str(item["artist"])),
            normalize_lookup_text(str(item["album"])),
        )
    )


def isrc_lookup_key(item: dict[str, Any]) -> str | None:
    isrc = str(item.get("isrc", "")).strip().upper()
    return f"i|{isrc}" if ISRC_PATTERN.fullmatch(isrc) else None


def url_path_scope(raw_url: str) -> str | None:
    path = urlsplit(raw_url).path
    segments = [segment.lower() for segment in path.split("/")[:-1]]
    for segment in reversed(segments):
        if segment in {"album", "song"}:
            return segment
    return None


def build_lookup_rows(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    album_groups: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    for item in items:
        album_groups[
            (
                artist_signature(str(item["artist"])),
                normalize_lookup_text(str(item["album"])),
                str(item["url"]),
            )
        ].append(item)

    rows: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str]] = set()

    def add(key: str, item: dict[str, Any], scope: str, include_isrc: bool = False) -> None:
        digest = hashlib.sha256(key.encode("utf-8")).digest()
        lookup_hash = base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")
        raw_url = str(item["url"])
        dedupe = (lookup_hash, raw_url, scope)
        if dedupe in seen:
            return
        seen.add(dedupe)
        row: dict[str, Any] = {"h": lookup_hash, "u": raw_url, "s": scope[0]}
        if include_isrc and item.get("isrc"):
            row["i"] = item["isrc"]
        if isinstance(item.get("width"), int) and isinstance(item.get("height"), int):
            row["w"] = item["width"]
            row["g"] = item["height"]
        rows.append({"_key": key, **row})

    for group in album_groups.values():
        first = group[0]
        declared_scope = str(first.get("scope", "")).strip().lower()
        path_scope = url_path_scope(str(first["url"])) if not declared_scope else None
        inferred_album = (
            declared_scope == "album"
            or path_scope == "album"
            or len({normalize_lookup_text(str(item["song"])) for item in group}) >= 2
        )

        for item in group:
            item_scope = str(item.get("scope", "")).strip().lower()
            if item_scope != "album":
                track_key = track_lookup_key(item)
                add(track_key, item, "track", include_isrc=True)
                isrc_key = isrc_lookup_key(item)
                if isrc_key is not None:
                    add(isrc_key, item, "track", include_isrc=True)

        if inferred_album:
            add(album_lookup_key(first), first, "album", include_isrc=False)

    rows.sort(key=lambda row: (row["_key"], row["u"], row["s"]))
    return rows


def hash_key(key: str) -> str:
    return hashlib.sha256(key.encode("utf-8")).hexdigest()


def encode_shard_bitmap(prefixes: set[str], prefix_chars: int) -> str:
    bit_count = 16 ** prefix_chars
    bitmap = bytearray(math.ceil(bit_count / 8))
    for prefix in prefixes:
        index = int(prefix, 16)
        bitmap[index // 8] |= 1 << (index % 8)
    return base64.b64encode(bitmap).decode("ascii")


def serialized_shard(rows: list[dict[str, Any]]) -> bytes:
    return (
        json.dumps(
            {"version": INDEX_VERSION, "items": rows},
            ensure_ascii=False,
            separators=(",", ":"),
        )
        + "\n"
    ).encode("utf-8")


def partition_rows(
    rows: list[dict[str, Any]],
    target_bytes: int,
    max_bytes: int,
    requested_prefix_chars: int | None,
) -> tuple[int, dict[str, list[dict[str, Any]]], int]:
    candidates = (
        [requested_prefix_chars]
        if requested_prefix_chars is not None
        else list(range(MIN_PREFIX_CHARS, MAX_PREFIX_CHARS + 1))
    )
    for prefix_chars in candidates:
        if prefix_chars is None or prefix_chars not in range(MIN_PREFIX_CHARS, MAX_PREFIX_CHARS + 1):
            raise IndexBuildError(
                f"prefix chars must be between {MIN_PREFIX_CHARS} and {MAX_PREFIX_CHARS}"
            )
        shards: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for row in rows:
            prefix = hash_key(str(row["_key"]))[:prefix_chars]
            public_row = {key: value for key, value in row.items() if key != "_key"}
            shards[prefix].append(public_row)
        largest = max(
            (len(serialized_shard(shard_rows)) for shard_rows in shards.values()),
            default=0,
        )
        if largest <= max_bytes and (requested_prefix_chars is not None or largest <= target_bytes):
            return prefix_chars, dict(shards), largest
    raise IndexBuildError(
        f"unable to keep every shard below {max_bytes} bytes with at most {MAX_PREFIX_CHARS} prefix chars"
    )


def build_index(
    input_path: Path,
    output_dir: Path,
    target_bytes: int,
    max_bytes: int,
    prefix_chars: int | None,
) -> dict[str, Any]:
    try:
        root = json.loads(input_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise IndexBuildError(f"unable to read normalized catalog: {error}") from error
    if not isinstance(root, dict) or not isinstance(root.get("items"), list):
        raise IndexBuildError("normalized catalog must contain an items array")
    items = [item for item in root["items"] if isinstance(item, dict)]
    rows = build_lookup_rows(items)
    if not rows:
        raise IndexBuildError("normalized catalog produced no lookup rows")

    selected_prefix_chars, shards, largest = partition_rows(
        rows,
        target_bytes=target_bytes,
        max_bytes=max_bytes,
        requested_prefix_chars=prefix_chars,
    )

    if output_dir.exists():
        shutil.rmtree(output_dir)
    shards_dir = output_dir / "v2" / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    for prefix, shard_rows in sorted(shards.items()):
        (shards_dir / f"{prefix}.json").write_bytes(serialized_shard(shard_rows))

    manifest = {
        "version": INDEX_VERSION,
        "generatedAt": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "hash": "sha256",
        "hashEncoding": "base64url",
        "prefixChars": selected_prefix_chars,
        "entryCount": len(items),
        "keyCount": len(rows),
        "shardCount": len(shards),
        "largestShardBytes": largest,
        "shardBitmap": encode_shard_bitmap(set(shards), selected_prefix_chars),
    }
    manifest_path = output_dir / "v2" / "manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    return manifest


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--target-shard-bytes", type=int, default=DEFAULT_TARGET_SHARD_BYTES)
    parser.add_argument("--max-shard-bytes", type=int, default=DEFAULT_MAX_SHARD_BYTES)
    parser.add_argument("--prefix-chars", type=int)
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    if arguments.target_shard_bytes <= 0 or arguments.max_shard_bytes <= 0:
        raise SystemExit("shard byte limits must be positive")
    if arguments.target_shard_bytes > arguments.max_shard_bytes:
        raise SystemExit("target shard size cannot exceed maximum shard size")
    try:
        manifest = build_index(
            input_path=arguments.input,
            output_dir=arguments.output_dir,
            target_bytes=arguments.target_shard_bytes,
            max_bytes=arguments.max_shard_bytes,
            prefix_chars=arguments.prefix_chars,
        )
    except IndexBuildError as error:
        raise SystemExit(f"Community canvas index build failed: {error}") from error
    print(
        "Built community canvas index: "
        f"{manifest['entryCount']} entries, {manifest['keyCount']} lookup rows, "
        f"{manifest['shardCount']} shards, {manifest['prefixChars']} prefix chars, "
        f"largest shard {manifest['largestShardBytes']} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
