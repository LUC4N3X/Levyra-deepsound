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
MAX_MANIFEST_BYTES = 256 * 1024
MANIFEST_SAFETY_MARGIN_BYTES = 8 * 1024
ISRC_PATTERN = re.compile(r"^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
ARTIST_SEPARATOR_PATTERN = re.compile(
    r"(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+[xX]\s+|\bfeat\.?\b|\bft\.?\b|"
    r"\bfeaturing\b|\bwith\b|\bcon\b)",
    re.IGNORECASE,
)
APOSTROPHES = {"’", "'", "`", "´"}
BRACKETS = set("()[]{}")
CONTENT_DIGEST_PATTERN = re.compile(r"^[0-9a-f]{64}$")


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


def split_artists(value: str) -> list[str]:
    output: list[str] = []
    seen: set[str] = set()
    for candidate in ARTIST_SEPARATOR_PATTERN.split(value):
        candidate = candidate.strip()
        normalized = normalize_lookup_text(candidate)
        if normalized and normalized not in seen:
            seen.add(normalized)
            output.append(candidate)
    return output


def artist_signature(value: str) -> str:
    return normalize_lookup_text(" ".join(split_artists(value)))


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
        declared_album = next(
            (
                item
                for item in group
                if str(item.get("scope", "")).strip().lower() == "album"
            ),
            None,
        )
        path_album = next(
            (
                item
                for item in group
                if not str(item.get("scope", "")).strip()
                and url_path_scope(str(item["url"])) == "album"
            ),
            None,
        )
        distinct_songs = {
            normalize_lookup_text(str(item["song"]))
            for item in group
        }
        inferred_album = (
            declared_album is not None
            or path_album is not None
            or len(distinct_songs) >= 2
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
            representative = declared_album or path_album or group[0]
            add(album_lookup_key(representative), representative, "album", include_isrc=False)

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


def public_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {key: value for key, value in row.items() if key != "_key"}
        for row in rows
    ]


def content_digest(rows: list[dict[str, Any]]) -> str:
    canonical = json.dumps(
        public_rows(rows),
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    digest = hashlib.sha256(canonical).hexdigest()
    if not CONTENT_DIGEST_PATTERN.fullmatch(digest):
        raise IndexBuildError("invalid generated content digest")
    return digest


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
            shards[prefix].append(
                {key: value for key, value in row.items() if key != "_key"}
            )
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
    digest = content_digest(rows)
    generation = digest[:16]
    shard_directory = f"g{generation}/p{selected_prefix_chars}"

    manifest = {
        "version": INDEX_VERSION,
        "generatedAt": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "hash": "sha256",
        "hashEncoding": "base64url",
        "contentDigest": digest,
        "prefixChars": selected_prefix_chars,
        "shardDirectory": shard_directory,
        "entryCount": len(items),
        "keyCount": len(rows),
        "shardCount": len(shards),
        "largestShardBytes": largest,
        "shardBitmap": encode_shard_bitmap(set(shards), selected_prefix_chars),
    }
    manifest_payload = (
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")) + "\n"
    ).encode("utf-8")
    manifest_limit = MAX_MANIFEST_BYTES - MANIFEST_SAFETY_MARGIN_BYTES
    if len(manifest_payload) > manifest_limit:
        raise IndexBuildError(
            f"generated manifest is {len(manifest_payload)} bytes; "
            f"client-safe limit is {manifest_limit} bytes"
        )
    if output_dir.exists():
        shutil.rmtree(output_dir)
    shards_dir = output_dir / "v2" / shard_directory / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    for prefix, shard_rows in sorted(shards.items()):
        (shards_dir / f"{prefix}.json").write_bytes(serialized_shard(shard_rows))

    manifest_path = output_dir / "v2" / "manifest.json"
    manifest_path.write_bytes(manifest_payload)
    return manifest


def verify_compatibility_vectors() -> None:
    vectors = {
        "normalize": normalize_lookup_text("Don't Stop (Live)"),
        "artists": artist_signature("Artist One feat. Artist Two"),
        "track_key": track_lookup_key(
            {
                "song": "Flowers",
                "artist": "Miley Cyrus",
                "album": "Endless Summer Vacation",
            }
        ),
    }
    expected = {
        "normalize": "dont stop live",
        "artists": "artist one artist two",
        "track_key": "t|flowers|miley cyrus|endless summer vacation",
    }
    if vectors != expected:
        raise IndexBuildError(
            f"lookup compatibility vectors changed: expected {expected}, got {vectors}"
        )
    digest = hashlib.sha256(vectors["track_key"].encode("utf-8")).digest()
    encoded = base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")
    if encoded != "-K6RoUVvFyzLwspAVDpboQ9Ad9om6Fpv3P29SRtmU08":
        raise IndexBuildError("Base64 URL-safe lookup digest compatibility changed")
    if digest.hex()[:3] != "f8a":
        raise IndexBuildError("hex shard prefix compatibility changed")

    lookup_rows = build_lookup_rows(
        [
            {
                "song": "Track One",
                "artist": "Exact Artist",
                "album": "Exact Album",
                "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Album/1.m3u8",
                "isrc": "USUM71703861",
            },
            {
                "song": "Track Two",
                "artist": "Exact Artist",
                "album": "Exact Album",
                "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Album/1.m3u8",
            },
        ]
    )
    row_keys = [str(row["_key"]) for row in lookup_rows]
    row_shape = (
        sum(key.startswith("t|") for key in row_keys),
        sum(key.startswith("i|") for key in row_keys),
        sum(key.startswith("a|") for key in row_keys),
    )
    if row_shape != (2, 1, 1):
        raise IndexBuildError(
            f"lookup row generation changed: expected (2 track, 1 ISRC, 1 album), got {row_shape}"
        )


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--target-shard-bytes", type=int, default=DEFAULT_TARGET_SHARD_BYTES)
    parser.add_argument("--max-shard-bytes", type=int, default=DEFAULT_MAX_SHARD_BYTES)
    parser.add_argument("--prefix-chars", type=int)
    parser.add_argument("--self-test", action="store_true")
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    try:
        verify_compatibility_vectors()
    except IndexBuildError as error:
        raise SystemExit(f"Community canvas index self-test failed: {error}") from error
    if arguments.self_test:
        print("Community canvas index compatibility vectors passed")
        return 0
    if arguments.input is None or arguments.output_dir is None:
        raise SystemExit("--input and --output-dir are required unless --self-test is used")
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
        f"{manifest['shardCount']} shards in {manifest['shardDirectory']}, "
        f"largest shard {manifest['largestShardBytes']} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
