#!/usr/bin/env python3
"""Levyra JioSaavn Sync observer, drift detector, and deterministic safe fixer.

Records only sanitized protocol and schema metadata. Never persists raw IP
addresses, media tokens, encrypted URLs, auth URLs, or signed query strings.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import random
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

SCHEMA_VERSION = 1
API_MAX_BYTES = 1_048_576
MEDIA_PROBE_MAX_BYTES = 16_384
MEDIA_PROBE_RANGE_END = 8_191
GITHUB_JSON_MAX_BYTES = 2_097_152
DEFAULT_TIMEOUT_SECONDS = 12.0
MAX_TOKEN_CHARS = 512
DES_KEY_BYTES = b"38346591"

MOBILE_USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/139.0.0.0 Mobile Safari/537.36"
)
RADAR_USER_AGENT = "Levyra-JioSaavn-Sync/1.0"

EXACT_API_HOSTS = {
    "www.jiosaavn.com",
    "api.jiosaavn.com",
    "jiosaavn.com",
    "api.github.com",
}
ALLOWED_CDN_SUFFIX = ".saavncdn.com"
HOST_LABEL_RE = re.compile(r"^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$")
PATH_SEGMENT_RE = re.compile(r"^[A-Za-z0-9_-]+$")
MEDIA_FILE_RE = re.compile(r"^([A-Za-z0-9_-]+)_(\d{2,3})\.(mp4|m4a)$", re.IGNORECASE)
CONTENT_RANGE_RE = re.compile(r"bytes\s+\d+-\d+/(\d+)", re.IGNORECASE)
HTML_BREAK_RE = re.compile(r"(?i)<br\s*/?>|</?(?:p|div)(?:\s[^>]*)?>")
IPV4_RE = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
SENSITIVE_QUERY_RE = re.compile(
    r"(?i)(Signature|Key-Pair-Id|Expires|auth_url|encrypted_media_url|token)=([^&\s]+)"
)

ACCEPTED_AUDIO_MIME_TYPES = {
    "audio/mp4",
    "audio/x-m4a",
    "audio/m4a",
    "audio/aac",
    "audio/mp4a-latm",
    "video/mp4",
    "application/octet-stream",
}

QUALITY_TIERS = (96, 160, 320)
LOWER_BITRATE_RATIO = 0.85
UPPER_BITRATE_RATIO = 1.15
UPPER_BITRATE_SLACK_KBPS = 8

INDIAN_ADDRESS_BLOCKS: tuple[tuple[str, int, str], ...] = (
    ("49.32.0.0", 12, "Reliance Jio"),
    ("152.56.0.0", 14, "Reliance Jio"),
    ("157.48.0.0", 16, "Reliance Jio"),
    ("117.96.0.0", 14, "Bharti Airtel"),
)

UPSTREAM_KEYWORDS = (
    "jiosaavn",
    "saavn",
    "api.php",
    "endpoint",
    "search",
    "song",
    "album",
    "artist",
    "lyrics",
    "decrypt",
    "encrypted_media_url",
    "320",
    "bitrate",
    "authtoken",
    "web6dot0",
    "android",
)

ALLOWLISTED_API_HOSTS = ("www.jiosaavn.com", "api.jiosaavn.com")
ALLOWLISTED_CONTEXTS = ("web6dot0", "android")
ALLOWLISTED_OPEN_MEDIA_HOSTS = ("aac.saavncdn.com", "c.saavncdn.com", "h.saavncdn.com")

_DES_IP = (
    58, 50, 42, 34, 26, 18, 10, 2,
    60, 52, 44, 36, 28, 20, 12, 4,
    62, 54, 46, 38, 30, 22, 14, 6,
    64, 56, 48, 40, 32, 24, 16, 8,
    57, 49, 41, 33, 25, 17, 9, 1,
    59, 51, 43, 35, 27, 19, 11, 3,
    61, 53, 45, 37, 29, 21, 13, 5,
    63, 55, 47, 39, 31, 23, 15, 7,
)
_DES_FP = (
    40, 8, 48, 16, 56, 24, 64, 32,
    39, 7, 47, 15, 55, 23, 63, 31,
    38, 6, 46, 14, 54, 22, 62, 30,
    37, 5, 45, 13, 53, 21, 61, 29,
    36, 4, 44, 12, 52, 20, 60, 28,
    35, 3, 43, 11, 51, 19, 59, 27,
    34, 2, 42, 10, 50, 18, 58, 26,
    33, 1, 41, 9, 49, 17, 57, 25,
)
_DES_E = (
    32, 1, 2, 3, 4, 5,
    4, 5, 6, 7, 8, 9,
    8, 9, 10, 11, 12, 13,
    12, 13, 14, 15, 16, 17,
    16, 17, 18, 19, 20, 21,
    20, 21, 22, 23, 24, 25,
    24, 25, 26, 27, 28, 29,
    28, 29, 30, 31, 32, 1,
)
_DES_P = (
    16, 7, 20, 21, 29, 12, 28, 17,
    1, 15, 23, 26, 5, 18, 31, 10,
    2, 8, 24, 14, 32, 27, 3, 9,
    19, 13, 30, 6, 22, 11, 4, 25,
)
_DES_PC1 = (
    57, 49, 41, 33, 25, 17, 9,
    1, 58, 50, 42, 34, 26, 18,
    10, 2, 59, 51, 43, 35, 27,
    19, 11, 3, 60, 52, 44, 36,
    63, 55, 47, 39, 31, 23, 15,
    7, 62, 54, 46, 38, 30, 22,
    14, 6, 61, 53, 45, 37, 29,
    21, 13, 5, 28, 20, 12, 4,
)
_DES_PC2 = (
    14, 17, 11, 24, 1, 5,
    3, 28, 15, 6, 21, 10,
    23, 19, 12, 4, 26, 8,
    16, 7, 27, 20, 13, 2,
    41, 52, 31, 37, 47, 55,
    30, 40, 51, 45, 33, 48,
    44, 49, 39, 56, 34, 53,
    46, 42, 50, 36, 29, 32,
)
_DES_SHIFTS = (1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
_DES_SBOX = (
    (
        (14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7),
        (0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8),
        (4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0),
        (15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13),
    ),
    (
        (15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10),
        (3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5),
        (0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15),
        (13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9),
    ),
    (
        (10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8),
        (13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1),
        (13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7),
        (1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12),
    ),
    (
        (7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15),
        (13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9),
        (10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4),
        (3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14),
    ),
    (
        (2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9),
        (14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6),
        (4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14),
        (11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3),
    ),
    (
        (12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11),
        (10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8),
        (9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6),
        (4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13),
    ),
    (
        (4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1),
        (13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6),
        (1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2),
        (6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12),
    ),
    (
        (13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7),
        (1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2),
        (7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8),
        (2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11),
    ),
)


def _permute(value: int, width: int, table: tuple[int, ...]) -> int:
    out = 0
    for pos in table:
        out = (out << 1) | ((value >> (width - pos)) & 1)
    return out


def _des_subkeys(key_bytes: bytes) -> list[int]:
    if len(key_bytes) != 8:
        raise ValueError("DES key must be 8 bytes")
    key_int = int.from_bytes(key_bytes, "big")
    perm = _permute(key_int, 64, _DES_PC1)
    left = (perm >> 28) & 0xFFFFFFF
    right = perm & 0xFFFFFFF
    keys: list[int] = []
    for shift in _DES_SHIFTS:
        left = ((left << shift) | (left >> (28 - shift))) & 0xFFFFFFF
        right = ((right << shift) | (right >> (28 - shift))) & 0xFFFFFFF
        keys.append(_permute((left << 28) | right, 56, _DES_PC2))
    return keys


def _des_feistel(right: int, subkey: int) -> int:
    expanded = _permute(right, 32, _DES_E) ^ subkey
    s_out = 0
    for box_idx in range(8):
        chunk = (expanded >> (42 - box_idx * 6)) & 0x3F
        row = ((chunk & 0x20) >> 4) | (chunk & 1)
        col = (chunk >> 1) & 0x0F
        s_out = (s_out << 4) | _DES_SBOX[box_idx][row][col]
    return _permute(s_out, 32, _DES_P)


def _des_decrypt_block(block8: bytes, subkeys: list[int]) -> bytes:
    state = _permute(int.from_bytes(block8, "big"), 64, _DES_IP)
    left = (state >> 32) & 0xFFFFFFFF
    right = state & 0xFFFFFFFF
    for subkey in reversed(subkeys):
        left, right = right, left ^ _des_feistel(right, subkey)
    combined = (right << 32) | left
    return _permute(combined, 64, _DES_FP).to_bytes(8, "big")


def des_ecb_decrypt(ciphertext: bytes, key_bytes: bytes = DES_KEY_BYTES) -> bytes | None:
    if not ciphertext or len(ciphertext) % 8 != 0:
        return None
    subkeys = _des_subkeys(key_bytes)
    plaintext = b"".join(
        _des_decrypt_block(ciphertext[i : i + 8], subkeys)
        for i in range(0, len(ciphertext), 8)
    )
    pad = plaintext[-1]
    if pad < 1 or pad > 8 or plaintext[-pad:] != bytes([pad]) * pad:
        return None
    return plaintext[:-pad]


def decode_media_token(raw_payload: str) -> str | None:
    trimmed = raw_payload.strip()
    if not trimmed or len(trimmed) > MAX_TOKEN_CHARS:
        return None
    try:
        raw_bytes = base64.b64decode(trimmed, validate=True)
    except ValueError:
        return None
    plain = des_ecb_decrypt(raw_bytes)
    if not plain:
        return None
    try:
        decoded = plain.decode("utf-8")
    except UnicodeDecodeError:
        return None
    if decoded.startswith(("https://", "http://")):
        return decoded
    return None


class SyncError(RuntimeError):
    def __init__(self, message: str, *, status: int | None = None) -> None:
        super().__init__(message)
        self.status = status


@dataclass(frozen=True)
class HttpResult:
    status: int
    headers: dict[str, str]
    body: bytes


def sanitize_text(text: str) -> str:
    cleaned = SENSITIVE_QUERY_RE.sub(r"\1=<redacted>", text)
    cleaned = IPV4_RE.sub("<redacted-ip>", cleaned)
    cleaned = re.sub(
        r"https?://([a-z0-9.-]+\.saavncdn\.com)(?::\d+)?/[^\s\"'<>]*",
        r"https://\1/<redacted-path>",
        cleaned,
        flags=re.IGNORECASE,
    )
    return cleaned[:240]


def _safe_host(url: str, *, media: bool = False) -> str:
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != "https":
        raise SyncError(f"non-HTTPS URL rejected: {parsed.scheme or '<missing>'}")
    if parsed.username or parsed.password:
        raise SyncError("URL user-info rejected")
    if parsed.port not in (None, 443):
        raise SyncError(f"non-standard HTTPS port rejected: {parsed.port}")
    host = (parsed.hostname or "").lower().rstrip(".")
    if not host:
        raise SyncError("URL host missing")
    if media:
        if host.endswith(ALLOWED_CDN_SUFFIX) and all(
            HOST_LABEL_RE.match(label) for label in host.split(".")
        ):
            return host
        raise SyncError(f"unexpected media host: {host}")
    if host not in EXACT_API_HOSTS:
        raise SyncError(f"unexpected host: {host}")
    return host


class _SafeRedirectHandler(urllib.request.HTTPRedirectHandler):
    def __init__(self, *, media: bool) -> None:
        super().__init__()
        self._media = media
        self._redirects = 0

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        self._redirects += 1
        if self._redirects > 5:
            raise SyncError("too many redirects")
        _safe_host(newurl, media=self._media)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def _bounded_request(
    url: str,
    *,
    headers: dict[str, str] | None = None,
    max_bytes: int,
    timeout: float = DEFAULT_TIMEOUT_SECONDS,
    media: bool = False,
) -> HttpResult:
    _safe_host(url, media=media)
    request = urllib.request.Request(url, headers=headers or {}, method="GET")
    opener = urllib.request.build_opener(_SafeRedirectHandler(media=media))
    try:
        with opener.open(request, timeout=timeout) as response:
            length = response.headers.get("Content-Length")
            if length and not media:
                try:
                    if int(length) > max_bytes:
                        raise SyncError(f"response too large: {length} > {max_bytes}")
                except ValueError:
                    pass
            body = response.read(max_bytes + 1)
            if len(body) > max_bytes:
                raise SyncError(f"response exceeded {max_bytes} bytes")
            return HttpResult(
                status=getattr(response, "status", response.getcode()),
                headers={k.lower(): v for k, v in response.headers.items()},
                body=body,
            )
    except urllib.error.HTTPError as error:
        body = error.read(min(max_bytes, MEDIA_PROBE_MAX_BYTES))
        return HttpResult(
            status=error.code,
            headers={k.lower(): v for k, v in error.headers.items()},
            body=body,
        )
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        raise SyncError(
            f"request failed: {type(error).__name__}: {sanitize_text(str(error))}"
        ) from error


def _ipv4_to_int(address: str) -> int:
    parts = [int(p) for p in address.split(".")]
    return (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | parts[3]


def _int_to_ipv4(value: int) -> str:
    return ".".join(str((value >> shift) & 0xFF) for shift in (24, 16, 8, 0))


@dataclass(frozen=True)
class IndiaProfile:
    forwarded_address: str
    operator: str
    block_index: int

    def api_headers(self) -> dict[str, str]:
        return {
            "User-Agent": MOBILE_USER_AGENT,
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "en-IN,en;q=0.9",
            "X-Forwarded-For": self.forwarded_address,
            "X-Real-IP": self.forwarded_address,
            "Referer": "https://www.jiosaavn.com/",
        }

    def media_headers(self) -> dict[str, str]:
        return {
            "User-Agent": MOBILE_USER_AGENT,
            "Accept": "*/*",
            "Accept-Encoding": "identity",
            "Range": f"bytes=0-{MEDIA_PROBE_RANGE_END}",
        }


def build_india_profile(
    *,
    excluded_indices: set[int] | None = None,
    rng: random.Random | None = None,
) -> IndiaProfile:
    generator = rng or random.SystemRandom()
    excluded = excluded_indices or set()
    eligible = [
        (idx, block)
        for idx, block in enumerate(INDIAN_ADDRESS_BLOCKS)
        if idx not in excluded
    ]
    if not eligible:
        eligible = list(enumerate(INDIAN_ADDRESS_BLOCKS))
    idx, (network, prefix_len, operator) = generator.choice(eligible)
    first_addr = _ipv4_to_int(network)
    size = 1 << (32 - prefix_len)
    chosen = first_addr + generator.randrange(size)
    host_octet = 11 + generator.randrange(240)
    address = _int_to_ipv4((chosen & 0xFFFFFF00) | host_octet)
    return IndiaProfile(forwarded_address=address, operator=operator, block_index=idx)


@dataclass(frozen=True)
class MediaLocation:
    open_host: str
    directory: str
    stem: str
    extension: str
    authorized_tier: int | None = None
    has_expires: bool = False

    def open_url(self, tier_kbps: int) -> str:
        return f"https://{self.open_host}{self.directory}/{self.stem}_{tier_kbps}.{self.extension}"


def _validate_media_host(parsed: urllib.parse.SplitResult, authorized: bool) -> str | None:
    if parsed.scheme not in (("https",) if authorized else ("https", "http")):
        return None
    if parsed.username or parsed.password:
        return None
    default_port = 443 if parsed.scheme == "https" else 80
    if parsed.port not in (None, default_port):
        return None
    host = (parsed.hostname or "").lower().rstrip(".")
    if not host.endswith(ALLOWED_CDN_SUFFIX):
        return None
    if not all(HOST_LABEL_RE.match(label) for label in host.split(".")):
        return None
    return host


def _parse_media_path(path: str) -> tuple[str, str, int, str] | None:
    segments = [seg for seg in path.split("/") if seg]
    if len(segments) < 2:
        return None
    match = MEDIA_FILE_RE.match(segments[-1])
    folders = segments[:-1]
    if not match or not all(PATH_SEGMENT_RE.match(seg) for seg in folders):
        return None
    return "".join(f"/{seg}" for seg in folders), match.group(1), int(match.group(2)), match.group(3).lower()


def parse_media_location(
    raw_url: str,
    *,
    override_open_host: str | None = None,
    authorized: bool = False,
) -> MediaLocation | None:
    parsed = urllib.parse.urlsplit(raw_url.strip())
    host = _validate_media_host(parsed, authorized)
    path_parts = _parse_media_path(parsed.path) if host else None
    if not host or not path_parts:
        return None
    directory, stem, parsed_tier, extension = path_parts
    query = urllib.parse.parse_qs(parsed.query)
    return MediaLocation(
        open_host=override_open_host or host,
        directory=directory,
        stem=stem,
        extension=extension,
        authorized_tier=parsed_tier if authorized else None,
        has_expires=bool(query.get("Expires")) if authorized else False,
    )


def bitrate_matches(estimated_kbps: int, tier_kbps: int) -> bool:
    return (
        estimated_kbps >= tier_kbps * LOWER_BITRATE_RATIO
        and estimated_kbps <= tier_kbps * UPPER_BITRATE_RATIO + UPPER_BITRATE_SLACK_KBPS
    )


def verified_tier(estimated_kbps: int, requested_kbps: int) -> int | None:
    if bitrate_matches(estimated_kbps, requested_kbps):
        return requested_kbps
    ordered = sorted(QUALITY_TIERS, reverse=True)
    idx = ordered.index(requested_kbps) if requested_kbps in ordered else -1
    if idx < 0 or idx + 1 >= len(ordered):
        return None
    lower = ordered[idx + 1]
    upper_of_lower = lower * UPPER_BITRATE_RATIO + UPPER_BITRATE_SLACK_KBPS
    lower_of_requested = requested_kbps * LOWER_BITRATE_RATIO
    if upper_of_lower < estimated_kbps < lower_of_requested:
        return lower
    return None


def validate_media_probe(
    result: HttpResult,
    tier_kbps: int,
    duration_seconds: int,
) -> dict[str, Any]:
    if result.status not in (200, 206):
        return {
            "status": f"http_{result.status}",
            "valid": False,
            "http_status": result.status,
        }
    if not result.body:
        return {"status": "empty_body", "valid": False, "http_status": result.status}
    content_type = result.headers.get("content-type", "").split(";", 1)[0].strip().lower()
    if content_type not in ACCEPTED_AUDIO_MIME_TYPES:
        return {
            "status": "not_audio",
            "valid": False,
            "http_status": result.status,
            "content_type": content_type[:48],
        }
    if len(result.body) < 8 or result.body[4:8] != b"ftyp":
        return {
            "status": "unsupported_container",
            "valid": False,
            "http_status": result.status,
        }
    total_bytes: int | None = None
    cr = result.headers.get("content-range", "")
    cr_match = CONTENT_RANGE_RE.search(cr)
    if cr_match:
        total_bytes = int(cr_match.group(1))
    elif result.status == 200:
        cl = result.headers.get("content-length", "").strip()
        if cl.isdigit():
            total_bytes = int(cl)
    if not total_bytes or total_bytes <= 0 or duration_seconds <= 0:
        return {
            "status": "unknown_length",
            "valid": False,
            "http_status": result.status,
        }
    estimated_kbps = round(total_bytes * 8.0 / duration_seconds / 1000.0)
    matched_tier = verified_tier(estimated_kbps, tier_kbps)
    has_mp4a = b"mp4a" in result.body
    if matched_tier is None:
        return {
            "status": "bitrate_mismatch",
            "valid": False,
            "http_status": result.status,
            "estimated_kbps": estimated_kbps,
            "has_mp4a": has_mp4a,
        }
    return {
        "status": "valid",
        "valid": True,
        "http_status": result.status,
        "verified_tier": matched_tier,
        "estimated_kbps": estimated_kbps,
        "has_mp4a": has_mp4a,
    }


def _parse_api_response(response: BoundedResponse) -> tuple[dict[str, Any] | list[Any] | None, dict[str, Any]]:
    if response.status != 200:
        return None, {
            "ok": False,
            "http_status": response.status,
            "error": f"http_{response.status}",
            "access_blocked": response.status >= 500,
        }
    body_text = response.body.decode("utf-8", errors="replace").strip()
    if not body_text or body_text.startswith("<"):
        return None, {
            "ok": False,
            "http_status": response.status,
            "error": "non_json_html_response",
            "access_blocked": True,
        }
    try:
        parsed = json.loads(body_text)
    except json.JSONDecodeError as exc:
        return None, {
            "ok": False,
            "http_status": response.status,
            "error": f"json_decode_error: {exc.msg}",
            "access_blocked": False,
        }
    if isinstance(parsed, dict) and isinstance(parsed.get("error"), dict):
        code = str(parsed["error"].get("code") or "API_ERROR")[:48]
        return parsed, {
            "ok": False,
            "http_status": response.status,
            "error": f"api_error:{code}",
            "access_blocked": False,
        }
    return parsed, {"ok": True, "http_status": response.status, "access_blocked": False}


def _api_call(
    api_cfg: dict[str, Any],
    call_name: str,
    params: dict[str, Any],
    *,
    host_override: str | None = None,
    context_override: str | None = None,
    version_override: str | None = None,
    max_rotations: int = 2,
) -> tuple[dict[str, Any] | list[Any] | None, dict[str, Any]]:
    host = host_override or str(api_cfg.get("host") or "www.jiosaavn.com")
    api_path = str(api_cfg.get("api_path") or "/api.php")
    api_version = version_override or str(api_cfg.get("api_version") or "4")
    ctx = context_override or str(api_cfg.get("client_context") or "web6dot0")
    query_items = {
        "__call": call_name,
        "_format": "json",
        "_marker": "0",
        "api_version": api_version,
        "ctx": ctx,
        **params,
    }
    url = f"https://{host}{api_path}?{urllib.parse.urlencode(query_items)}"
    excluded_blocks: set[int] = set()
    last_meta: dict[str, Any] = {"ok": False, "http_status": 0, "error": "unknown"}

    for attempt in range(max_rotations):
        profile = build_india_profile(excluded_indices=excluded_blocks)
        try:
            response = _bounded_request(
                url,
                headers=profile.api_headers(),
                max_bytes=API_MAX_BYTES,
            )
        except SyncError as exc:
            last_meta = {
                "ok": False,
                "http_status": exc.status or 0,
                "error": sanitize_text(str(exc)),
                "access_blocked": True,
            }
            if attempt + 1 < max_rotations:
                continue
            return None, last_meta

        if response.status in (403, 429):
            excluded_blocks.add(profile.block_index)
            last_meta = {
                "ok": False,
                "http_status": response.status,
                "error": f"http_{response.status}",
                "access_blocked": True,
            }
            continue

        return _parse_api_response(response)

    return None, last_meta


def _extract_song_list(search_payload: Any) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    if not isinstance(search_payload, dict):
        return [], {"envelope_ok": False, "top_keys": [], "has_results_list": False}
    top_keys = sorted(str(k) for k in search_payload.keys())
    results = search_payload.get("results")
    if not isinstance(results, list):
        return [], {
            "envelope_ok": False,
            "top_keys": top_keys,
            "has_results_list": False,
        }
    songs = [
        item
        for item in results
        if isinstance(item, dict) and item.get("type", "") in ("", "song")
    ]
    return songs, {
        "envelope_ok": True,
        "top_keys": top_keys,
        "has_results_list": True,
        "has_total": "total" in search_payload,
        "has_start": "start" in search_payload,
        "total_results": len(results),
        "song_count": len(songs),
    }


def _record_song_more_info(
    item: dict[str, Any],
    info: dict[str, Any],
    acc: dict[str, Any],
) -> None:
    acc["more_info_keys"].update(str(k) for k in info.keys())
    flag320 = info.get("320kbps")
    if flag320 is not None:
        acc["offers320_values"].add(str(flag320).strip().lower())
    artist_map = info.get("artistMap")
    if isinstance(artist_map, dict):
        acc["artist_map_keys"].update(str(k) for k in artist_map.keys())
    encrypted = str(info.get("encrypted_media_url") or "").strip()
    song_id = str(item.get("id") or "").strip()
    if song_id and encrypted:
        acc["playable_count"] += 1
        if acc["first_playable"] is None:
            acc["first_playable"] = item
        if not acc["lyrics_capable_id"] and str(info.get("has_lyrics") or "").lower() == "true":
            acc["lyrics_capable_id"] = song_id


def _inspect_song_schema(songs: list[dict[str, Any]]) -> dict[str, Any]:
    acc: dict[str, Any] = {
        "explicit_values": set(),
        "offers320_values": set(),
        "more_info_keys": set(),
        "artist_map_keys": set(),
        "playable_count": 0,
        "lyrics_capable_id": "",
        "first_playable": None,
    }
    for item in songs:
        exp = item.get("explicit_content")
        if exp is not None:
            acc["explicit_values"].add(str(exp).strip().lower())
        info = item.get("more_info")
        if isinstance(info, dict):
            _record_song_more_info(item, info, acc)

    explicit_values = acc["explicit_values"]
    more_info_keys = acc["more_info_keys"]
    return {
        "playable_count": acc["playable_count"],
        "has_explicit": "1" in explicit_values or "true" in explicit_values,
        "has_clean": "0" in explicit_values or "false" in explicit_values,
        "explicit_values": sorted(explicit_values),
        "offers320_values": sorted(acc["offers320_values"]),
        "more_info_has_required": {
            "encrypted_media_url": "encrypted_media_url" in more_info_keys,
            "duration": "duration" in more_info_keys,
            "album": "album" in more_info_keys,
            "artistMap": "artistMap" in more_info_keys,
        },
        "artist_map_keys": sorted(acc["artist_map_keys"]),
        "_first_playable": acc["first_playable"],
        "_lyrics_id": acc["lyrics_capable_id"],
    }


def _extract_song_details_entry(
    payload: dict[str, Any],
    song_id: str,
) -> tuple[str, dict[str, Any] | None]:
    if isinstance(payload.get("songs"), list):
        for item in payload["songs"]:
            if isinstance(item, dict) and str(item.get("id") or "") == song_id:
                return "songs_array", item
        return "songs_array", None
    if isinstance(payload.get(song_id), dict):
        return "id_keyed_object", payload[song_id]
    return "unrecognized", None


def _probe_song_details(
    api_cfg: dict[str, Any],
    endpoints: dict[str, str],
    song_id: str,
    *,
    host_override: str | None = None,
    context_override: str | None = None,
) -> dict[str, Any]:
    payload, meta = _api_call(
        api_cfg,
        endpoints.get("song_details", "song.getDetails"),
        {"pids": song_id},
        host_override=host_override,
        context_override=context_override,
    )
    if not meta.get("ok"):
        return {
            "ok": False,
            "envelope_kind": "error",
            "access_blocked": bool(meta.get("access_blocked")),
            "error": meta.get("error", ""),
        }
    if isinstance(payload, list):
        return {
            "ok": False,
            "envelope_kind": "empty_array" if not payload else "unexpected_array",
            "access_blocked": False,
        }
    if not isinstance(payload, dict):
        return {"ok": False, "envelope_kind": "non_object", "access_blocked": False}

    envelope_kind, entry = _extract_song_details_entry(payload, song_id)
    if not entry:
        return {"ok": False, "envelope_kind": envelope_kind, "access_blocked": False}

    info = entry.get("more_info") if isinstance(entry.get("more_info"), dict) else {}
    has_media = bool(str(info.get("encrypted_media_url") or entry.get("encrypted_media_url") or "").strip())
    has_duration = str(info.get("duration") or entry.get("duration") or "").strip().isdigit()
    return {
        "ok": has_media and has_duration,
        "envelope_kind": envelope_kind,
        "has_media_payload": has_media,
        "has_duration": has_duration,
        "has_artist_map": isinstance(info.get("artistMap"), dict),
        "access_blocked": False,
    }


def _probe_lyrics(
    api_cfg: dict[str, Any],
    endpoints: dict[str, str],
    lyrics_id: str,
    *,
    host_override: str | None = None,
    context_override: str | None = None,
) -> dict[str, Any]:
    payload, meta = _api_call(
        api_cfg,
        endpoints.get("lyrics", "lyrics.getLyrics"),
        {"lyrics_id": lyrics_id},
        host_override=host_override,
        context_override=context_override,
    )
    if not meta.get("ok"):
        return {
            "ok": False,
            "access_blocked": bool(meta.get("access_blocked")),
            "error": meta.get("error", ""),
        }
    if not isinstance(payload, dict):
        return {"ok": False, "access_blocked": False, "error": "non_object"}
    raw = payload.get("lyrics")
    if not isinstance(raw, str) or not raw.strip():
        return {"ok": False, "has_lyrics_field": isinstance(raw, str), "access_blocked": False}
    lines = [line.strip() for line in HTML_BREAK_RE.sub("\n", raw).splitlines() if line.strip()]
    return {
        "ok": len(lines) > 0,
        "has_lyrics_field": True,
        "has_html_breaks": bool(HTML_BREAK_RE.search(raw)),
        "line_count": len(lines),
        "access_blocked": False,
    }


def _parse_duration_seconds(raw: Any) -> int | None:
    text = str(raw or "").strip()
    if not text or not text.isdigit():
        return None
    seconds = int(text)
    return seconds if seconds > 0 else None


def _probe_single_media_url(
    url: str,
    headers: dict[str, str],
    tier: int,
    duration_seconds: int,
) -> dict[str, Any]:
    try:
        res = _bounded_request(
            url,
            headers=headers,
            max_bytes=MEDIA_PROBE_MAX_BYTES,
            media=True,
        )
        return validate_media_probe(res, tier, duration_seconds)
    except SyncError as exc:
        return {
            "status": "transport_error",
            "valid": False,
            "error": sanitize_text(str(exc)),
        }


def _authorize_media_location(
    api_cfg: dict[str, Any],
    endpoints: dict[str, str],
    encrypted_payload: str,
    *,
    host_override: str | None,
    context_override: str | None,
    open_media_host_override: str | None,
    max_rotations: int,
) -> tuple[MediaLocation | None, str, dict[str, Any]]:
    auth_payload, auth_meta = _api_call(
        api_cfg,
        endpoints.get("authorize_media", "song.generateAuthToken"),
        {"url": encrypted_payload, "bitrate": "320"},
        host_override=host_override,
        context_override=context_override,
        max_rotations=max_rotations,
    )
    if not isinstance(auth_payload, dict):
        return None, "", auth_meta
    auth_status = str(auth_payload.get("status") or "").lower()
    raw_auth_url = auth_payload.get("auth_url")
    if auth_status != "success" or not isinstance(raw_auth_url, str):
        return None, auth_status, auth_meta
    auth_loc = parse_media_location(
        raw_auth_url,
        override_open_host=open_media_host_override or str(api_cfg.get("open_media_host") or "aac.saavncdn.com"),
        authorized=True,
    )
    return auth_loc, auth_status, auth_meta


def _probe_tier_across_candidates(
    candidates: list[MediaLocation],
    media_headers: dict[str, str],
    tier: int,
    duration_seconds: int,
) -> tuple[dict[str, Any] | None, bool, bool]:
    chosen: dict[str, Any] | None = None
    saw_attempt = False
    for idx, loc in enumerate(candidates):
        saw_attempt = True
        validation = _probe_single_media_url(loc.open_url(tier), media_headers, tier, duration_seconds)
        if chosen is None or validation.get("valid"):
            chosen = validation
        if validation.get("valid"):
            return chosen, saw_attempt, False
        non_blocked = (
            validation.get("status") != "transport_error"
            and validation.get("http_status") not in (403, 429, 500, 502, 503)
        )
        if non_blocked and idx == len(candidates) - 1:
            return chosen, saw_attempt, False
    return chosen, saw_attempt, True


def _probe_streams(
    api_cfg: dict[str, Any],
    endpoints: dict[str, str],
    encrypted_payload: str,
    duration_seconds: int,
    quality_tiers: list[int],
    *,
    host_override: str | None = None,
    context_override: str | None = None,
    open_media_host_override: str | None = None,
    max_rotations: int = 2,
) -> dict[str, Any]:
    decoded = decode_media_token(encrypted_payload)
    # Parity with JioSaavnMediaLocation.fromMediaToken: preserve decrypted CDN host.
    location = parse_media_location(decoded, override_open_host=None, authorized=False) if decoded else None
    auth_loc, auth_status, auth_meta = _authorize_media_location(
        api_cfg,
        endpoints,
        encrypted_payload,
        host_override=host_override,
        context_override=context_override,
        open_media_host_override=open_media_host_override,
        max_rotations=max_rotations,
    )

    media_headers = build_india_profile().media_headers()
    candidates = [loc for loc in (location, auth_loc) if loc is not None]
    tier_results: dict[str, Any] = {}
    verified_tiers: set[int] = set()
    saw_transport_attempt = False
    all_transport_blocked = True

    for tier in quality_tiers:
        chosen, attempt_seen, blocked = _probe_tier_across_candidates(
            candidates, media_headers, tier, duration_seconds
        )
        saw_transport_attempt = saw_transport_attempt or attempt_seen
        all_transport_blocked = all_transport_blocked and blocked
        if chosen is not None:
            tier_results[str(tier)] = chosen
            measured_tier = chosen.get("verified_tier")
            if chosen.get("valid") and isinstance(measured_tier, int):
                verified_tiers.add(measured_tier)

    return {
        "decrypt_ok": location is not None,
        "decrypted_cdn_host": location.open_host if location else "",
        "decrypted_extension": location.extension if location else "",
        "auth_api_ok": bool(auth_meta.get("ok")),
        "auth_api_blocked": bool(auth_meta.get("access_blocked")),
        "auth_status": auth_status,
        "auth_url_valid": auth_loc is not None,
        "auth_has_expires": bool(auth_loc and auth_loc.has_expires),
        "tiers": tier_results,
        "verified_tiers": sorted(verified_tiers),
        "playable_any_tier": len(verified_tiers) > 0,
        "cdn_access_blocked": bool(saw_transport_attempt and all_transport_blocked and not verified_tiers),
    }


def _evaluate_track_attempt(
    search_summary: dict[str, Any],
    schema_info: dict[str, Any],
    details: dict[str, Any],
    lyrics: dict[str, Any],
    streams: dict[str, Any],
    *,
    expect_explicit_and_clean: bool,
    expect_lyrics: bool,
) -> dict[str, Any]:
    explicit_ok = (
        (schema_info["has_explicit"] and schema_info["has_clean"])
        if expect_explicit_and_clean
        else True
    )
    lyrics_ok = bool(lyrics.get("ok")) if expect_lyrics else True
    all_ok = (
        search_summary.get("envelope_ok", False)
        and details.get("ok", False)
        and lyrics_ok
        and streams.get("playable_any_tier", False)
        and explicit_ok
    )
    access_blocked = bool(
        details.get("access_blocked")
        or (expect_lyrics and lyrics.get("access_blocked"))
        or streams.get("cdn_access_blocked")
        or (not streams.get("playable_any_tier") and streams.get("auth_api_blocked"))
    )
    return {
        "ok": all_ok,
        "access_blocked": access_blocked,
        "search": search_summary,
        "schema": schema_info,
        "explicit_and_clean_ok": explicit_ok,
        "details": details,
        "lyrics": lyrics,
        "streams": streams,
    }


def _probe_track_attempt(
    query: str,
    api_cfg: dict[str, Any],
    endpoints: dict[str, str],
    quality_tiers: list[int],
    *,
    expect_explicit_and_clean: bool,
    expect_lyrics: bool,
    host_override: str | None = None,
    context_override: str | None = None,
    open_media_host_override: str | None = None,
    max_rotations: int = 2,
) -> dict[str, Any]:
    search_payload, search_meta = _api_call(
        api_cfg,
        endpoints.get("search_songs", "search.getResults"),
        {"q": query, "n": 10, "p": 1},
        host_override=host_override,
        context_override=context_override,
        max_rotations=max_rotations,
    )
    if not search_meta.get("ok"):
        return {
            "ok": False,
            "access_blocked": bool(search_meta.get("access_blocked")),
            "error": f"search_failed:{search_meta.get('error')}",
        }

    songs, search_summary = _extract_song_list(search_payload)
    schema_info = _inspect_song_schema(songs)
    first_song = schema_info.pop("_first_playable", None)
    lyrics_id = schema_info.pop("_lyrics_id", "")
    if not first_song:
        return {
            "ok": False,
            "access_blocked": False,
            "search": search_summary,
            "schema": schema_info,
            "error": "no_playable_song_in_search",
        }

    first_info = first_song.get("more_info") or {}
    song_id = str(first_song.get("id") or "")
    duration_sec = _parse_duration_seconds(first_info.get("duration"))
    if duration_sec is None:
        return {
            "ok": False,
            "access_blocked": False,
            "search": search_summary,
            "schema": schema_info,
            "error": "invalid_duration_schema",
        }

    details = _probe_song_details(
        api_cfg,
        endpoints,
        song_id,
        host_override=host_override,
        context_override=context_override,
    )
    lyrics = (
        _probe_lyrics(
            api_cfg,
            endpoints,
            lyrics_id or song_id,
            host_override=host_override,
            context_override=context_override,
        )
        if expect_lyrics
        else {"skipped": True}
    )
    streams = _probe_streams(
        api_cfg,
        endpoints,
        str(first_info.get("encrypted_media_url") or "").strip(),
        duration_sec,
        quality_tiers,
        host_override=host_override,
        context_override=context_override,
        open_media_host_override=open_media_host_override,
        max_rotations=max_rotations,
    )
    return _evaluate_track_attempt(
        search_summary,
        schema_info,
        details,
        lyrics,
        streams,
        expect_explicit_and_clean=expect_explicit_and_clean,
        expect_lyrics=expect_lyrics,
    )


def _probe_track_sentinel(
    sentinel: dict[str, Any],
    api_cfg: dict[str, Any],
    quality_tiers: list[int],
    *,
    attempts: int,
    retry_delay_seconds: float,
    host_override: str | None = None,
    context_override: str | None = None,
    open_media_host_override: str | None = None,
    max_rotations: int = 2,
) -> dict[str, Any]:
    endpoints = api_cfg.get("endpoints") if isinstance(api_cfg.get("endpoints"), dict) else {}
    name = str(sentinel.get("name") or sentinel.get("query") or "")
    query = str(sentinel.get("query") or "")
    expect_explicit_and_clean = bool(sentinel.get("expect_explicit_and_clean", False))
    expect_lyrics = bool(sentinel.get("expect_lyrics", False))

    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        last_result = _probe_track_attempt(
            query,
            api_cfg,
            endpoints,
            quality_tiers,
            expect_explicit_and_clean=expect_explicit_and_clean,
            expect_lyrics=expect_lyrics,
            host_override=host_override,
            context_override=context_override,
            open_media_host_override=open_media_host_override,
            max_rotations=max_rotations,
        )
        if last_result.get("ok"):
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)

    return {
        "name": name,
        "required": bool(sentinel.get("required", True)),
        "expect_explicit_and_clean": expect_explicit_and_clean,
        "expect_lyrics": expect_lyrics,
        "ok": bool(last_result.get("ok")),
        "observation": last_result,
    }


def _match_entity_id(
    results: list[Any],
    match_keys: tuple[str, ...],
    expected_text: str,
) -> str:
    dict_items = [item for item in results if isinstance(item, dict)]
    if not dict_items:
        return ""
    if expected_text:
        for item in dict_items:
            candidate = " ".join(str(item.get(k) or "") for k in match_keys).lower()
            if expected_text in candidate:
                return str(item.get("id") or "").strip()
    return str(dict_items[0].get("id") or "").strip()


def _probe_entity_once(
    api_cfg: dict[str, Any],
    kind: str,
    search_call: str,
    query: str,
    match_keys: tuple[str, ...],
    expected_text: str,
    details_call: str,
    details_params_fn: Any,
    summarize_fn: Any,
    *,
    host_override: str | None,
    context_override: str | None,
) -> dict[str, Any]:
    search_payload, search_meta = _api_call(
        api_cfg, search_call, {"q": query, "n": 5, "p": 1},
        host_override=host_override, context_override=context_override,
    )
    if not search_meta.get("ok"):
        return {
            "ok": False,
            "access_blocked": bool(search_meta.get("access_blocked")),
            "error": f"{kind}_search_failed:{search_meta.get('error')}",
        }
    results = search_payload.get("results") if isinstance(search_payload, dict) else None
    if not isinstance(results, list) or not results:
        return {"ok": False, "access_blocked": False, "error": f"{kind}_search_empty_or_malformed"}
    entity_id = _match_entity_id(results, match_keys, expected_text)
    if not entity_id:
        return {"ok": False, "access_blocked": False, "error": f"{kind}_id_missing"}
    details_payload, details_meta = _api_call(
        api_cfg, details_call, details_params_fn(entity_id),
        host_override=host_override, context_override=context_override,
    )
    if not details_meta.get("ok") or not isinstance(details_payload, dict):
        return {
            "ok": False,
            "access_blocked": bool(details_meta.get("access_blocked")),
            "error": f"{kind}_details_failed:{details_meta.get('error', 'non_object')}",
        }
    return summarize_fn(details_payload, len(results))


def _summarize_artist_details(payload: dict[str, Any], search_count: int) -> dict[str, Any]:
    top_songs = payload.get("topSongs")
    top_albums = payload.get("topAlbums")
    has_name = bool(str(payload.get("name") or "").strip())
    has_songs = isinstance(top_songs, list) and len(top_songs) > 0
    has_albums = isinstance(top_albums, list) and len(top_albums) > 0
    return {
        "ok": has_name and has_songs and has_albums,
        "access_blocked": False,
        "search_count": search_count,
        "has_artist_id": bool(payload.get("artistId")),
        "has_name": has_name,
        "top_songs_count": len(top_songs) if isinstance(top_songs, list) else 0,
        "top_albums_count": len(top_albums) if isinstance(top_albums, list) else 0,
        "detail_keys": sorted(str(k) for k in payload.keys()),
    }


def _summarize_album_details(payload: dict[str, Any], search_count: int) -> dict[str, Any]:
    songs = payload.get("list")
    has_title = bool(str(payload.get("title") or "").strip())
    has_songs = isinstance(songs, list) and len(songs) > 0
    songs_have_more_info = bool(
        has_songs and all(isinstance(s, dict) and isinstance(s.get("more_info"), dict) for s in songs[:3])
    )
    return {
        "ok": has_title and has_songs and songs_have_more_info,
        "access_blocked": False,
        "search_count": search_count,
        "has_title": has_title,
        "list_count": len(songs) if isinstance(songs, list) else 0,
        "songs_have_more_info": songs_have_more_info,
        "has_more_info": isinstance(payload.get("more_info"), dict),
        "detail_keys": sorted(str(k) for k in payload.keys()),
    }


def _probe_artist_sentinel(
    sentinel: dict[str, Any],
    api_cfg: dict[str, Any],
    *,
    attempts: int,
    retry_delay_seconds: float,
    host_override: str | None = None,
    context_override: str | None = None,
) -> dict[str, Any]:
    endpoints = api_cfg.get("endpoints") if isinstance(api_cfg.get("endpoints"), dict) else {}
    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        last_result = _probe_entity_once(
            api_cfg,
            "artist",
            endpoints.get("search_artists", "search.getArtistResults"),
            str(sentinel.get("query") or ""),
            ("name", "title"),
            str(sentinel.get("expected_name") or "").lower(),
            endpoints.get("artist_details", "artist.getArtistPageDetails"),
            lambda aid: {"artistId": aid, "n_song": 5, "n_album": 5, "page": 0, "sort_order": "desc", "category": "popularity"},
            _summarize_artist_details,
            host_override=host_override,
            context_override=context_override,
        )
        if last_result.get("ok"):
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)
    return {
        "name": str(sentinel.get("name") or sentinel.get("query") or ""),
        "required": bool(sentinel.get("required", True)),
        "ok": bool(last_result.get("ok")),
        "observation": last_result,
    }


def _probe_album_sentinel(
    sentinel: dict[str, Any],
    api_cfg: dict[str, Any],
    *,
    attempts: int,
    retry_delay_seconds: float,
    host_override: str | None = None,
    context_override: str | None = None,
) -> dict[str, Any]:
    endpoints = api_cfg.get("endpoints") if isinstance(api_cfg.get("endpoints"), dict) else {}
    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        last_result = _probe_entity_once(
            api_cfg,
            "album",
            endpoints.get("search_albums", "search.getAlbumResults"),
            str(sentinel.get("query") or ""),
            ("title",),
            str(sentinel.get("expected_title_contains") or "").lower(),
            endpoints.get("album_details", "content.getAlbumDetails"),
            lambda aid: {"albumid": aid},
            _summarize_album_details,
            host_override=host_override,
            context_override=context_override,
        )
        if last_result.get("ok"):
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)
    return {
        "name": str(sentinel.get("name") or sentinel.get("query") or ""),
        "required": bool(sentinel.get("required", True)),
        "ok": bool(last_result.get("ok")),
        "observation": last_result,
    }


def _fetch_upstream(repo: str, branch: str) -> dict[str, Any]:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        return {"repo": repo, "branch": branch, "error": "invalid repository identifier"}
    url = (
        f"https://api.github.com/repos/{urllib.parse.quote(repo)}/commits"
        f"?sha={urllib.parse.quote(branch)}&per_page=10"
    )
    try:
        res = _bounded_request(
            url,
            headers={"Accept": "application/vnd.github+json", "User-Agent": RADAR_USER_AGENT},
            max_bytes=GITHUB_JSON_MAX_BYTES,
        )
    except SyncError as exc:
        return {"repo": repo, "branch": branch, "error": sanitize_text(str(exc))}
    if res.status != 200:
        return {"repo": repo, "branch": branch, "error": f"GitHub API HTTP {res.status}"}
    try:
        commits = json.loads(res.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        return {"repo": repo, "branch": branch, "error": f"invalid JSON: {exc}"}
    if not isinstance(commits, list):
        return {"repo": repo, "branch": branch, "error": "unexpected response shape"}
    relevant: list[dict[str, str]] = []
    for entry in commits:
        if not isinstance(entry, dict):
            continue
        commit = entry.get("commit") if isinstance(entry.get("commit"), dict) else {}
        message = str(commit.get("message") or "").splitlines()[0].strip()
        if any(kw in message.lower() for kw in UPSTREAM_KEYWORDS):
            relevant.append({"sha": str(entry.get("sha") or "")[:12], "message": message[:160]})
    return {"repo": repo, "branch": branch, "relevant_commits": relevant[:5]}


def _allowlisted_candidates(
    api_cfg: dict[str, Any],
    key: str,
    primary: str,
    allowlist: Any,
) -> list[str]:
    extras = api_cfg.get(key)
    if not isinstance(extras, list):
        return [primary]
    return [primary] + [str(item) for item in extras if item in allowlist]


def _append_candidate(
    candidates: list[dict[str, str]],
    seen: set[tuple[str, str, str]],
    host: str,
    ctx: str,
    media_host: str,
) -> None:
    triple = (host, ctx, media_host)
    if triple not in seen:
        seen.add(triple)
        candidates.append({"api_host": host, "client_context": ctx, "open_media_host": media_host})


def _build_protocol_candidates(api_cfg: dict[str, Any]) -> list[dict[str, str]]:
    primary_host = str(api_cfg.get("host") or "www.jiosaavn.com")
    primary_ctx = str(api_cfg.get("client_context") or "web6dot0")
    primary_media = str(api_cfg.get("open_media_host") or "aac.saavncdn.com")

    candidate_hosts = _allowlisted_candidates(api_cfg, "fallback_hosts", primary_host, ALLOWLISTED_API_HOSTS)
    candidate_ctxs = _allowlisted_candidates(api_cfg, "fallback_contexts", primary_ctx, ALLOWLISTED_CONTEXTS)
    candidate_media = _allowlisted_candidates(api_cfg, "fallback_media_hosts", primary_media, ALLOWLISTED_OPEN_MEDIA_HOSTS)

    candidates: list[dict[str, str]] = []
    seen: set[tuple[str, str, str]] = {(primary_host, primary_ctx, primary_media)}
    for host in candidate_hosts:
        for ctx in candidate_ctxs:
            _append_candidate(candidates, seen, host, ctx, primary_media)
    for media_host in candidate_media:
        _append_candidate(candidates, seen, primary_host, primary_ctx, media_host)
    return candidates


def _sentinel_list(config: dict[str, Any], key: str) -> list[dict[str, Any]]:
    raw = config.get(key)
    if not isinstance(raw, list):
        return []
    return [item for item in raw if isinstance(item, dict)]


def _probe_candidate_sentinels(
    sentinels: list[dict[str, Any]],
    deadline: float,
    probe_fn: Any,
) -> list[dict[str, Any]]:
    collected: list[dict[str, Any]] = []
    for sentinel in sentinels:
        if time.monotonic() >= deadline:
            break
        collected.append(probe_fn(sentinel))
    return collected


def _candidate_all_passed(
    alt_tracks: list[dict[str, Any]],
    alt_artists: list[dict[str, Any]],
    alt_albums: list[dict[str, Any]],
    expected_counts: tuple[int, int, int],
    baseline: dict[str, Any] | None,
    config: dict[str, Any],
) -> bool:
    t_count, a_count, al_count = expected_counts
    if (
        len(alt_tracks) != t_count
        or len(alt_artists) != a_count
        or len(alt_albums) != al_count
    ):
        return False
    if not all(item.get("ok") for item in alt_tracks + alt_artists + alt_albums):
        return False
    if not isinstance(baseline, dict) or not isinstance(baseline.get("observation"), dict):
        return True
    alt_decision = _classify(
        baseline,
        {
            "schema": SCHEMA_VERSION,
            "tracks": alt_tracks,
            "artists": alt_artists,
            "albums": alt_albums,
        },
        config,
    )
    return alt_decision.get("decision") != "repair"


def _evaluate_protocol_candidate(
    candidate: dict[str, str],
    config: dict[str, Any],
    api_cfg: dict[str, Any],
    quality_tiers: list[int],
    baseline: dict[str, Any] | None,
    deadline: float,
) -> dict[str, Any]:
    track_sentinels = _sentinel_list(config, "track_sentinels")
    artist_sentinels = _sentinel_list(config, "artist_sentinels")
    album_sentinels = _sentinel_list(config, "album_sentinels")

    def run_track(sentinel: dict[str, Any]) -> dict[str, Any]:
        return _probe_track_sentinel(
            sentinel,
            api_cfg,
            quality_tiers,
            attempts=1,
            retry_delay_seconds=0.0,
            host_override=candidate["api_host"],
            context_override=candidate["client_context"],
            open_media_host_override=candidate["open_media_host"],
            max_rotations=1,
        )

    canary_track = run_track(track_sentinels[0])
    if not canary_track.get("ok"):
        return {**candidate, "all_passed": False, "tracks_ok": 0, "artists_ok": 0, "albums_ok": 0}

    alt_tracks = [canary_track] + _probe_candidate_sentinels(track_sentinels[1:], deadline, run_track)
    alt_artists = _probe_candidate_sentinels(
        artist_sentinels,
        deadline,
        lambda s: _probe_artist_sentinel(
            s,
            api_cfg,
            attempts=1,
            retry_delay_seconds=0.0,
            host_override=candidate["api_host"],
            context_override=candidate["client_context"],
        ),
    )
    alt_albums = _probe_candidate_sentinels(
        album_sentinels,
        deadline,
        lambda s: _probe_album_sentinel(
            s,
            api_cfg,
            attempts=1,
            retry_delay_seconds=0.0,
            host_override=candidate["api_host"],
            context_override=candidate["client_context"],
        ),
    )
    all_passed = _candidate_all_passed(
        alt_tracks,
        alt_artists,
        alt_albums,
        (len(track_sentinels), len(artist_sentinels), len(album_sentinels)),
        baseline,
        config,
    )
    return {
        **candidate,
        "all_passed": all_passed,
        "tracks_ok": sum(1 for item in alt_tracks if item.get("ok")),
        "artists_ok": sum(1 for item in alt_artists if item.get("ok")),
        "albums_ok": sum(1 for item in alt_albums if item.get("ok")),
    }


def _should_probe_alternatives(
    all_items: list[dict[str, Any]],
    provisional_decision: dict[str, Any] | None,
) -> bool:
    needs_repair = (provisional_decision or {}).get("decision") == "repair"
    if all(item.get("ok") for item in all_items) and not needs_repair:
        return False
    failed_items = [item for item in all_items if not item.get("ok")]
    if failed_items and not needs_repair and all(_is_access_blocked(item) for item in failed_items):
        return False
    return True


def _probe_protocol_alternatives(
    config: dict[str, Any],
    tracks: list[dict[str, Any]],
    artists: list[dict[str, Any]],
    albums: list[dict[str, Any]],
    *,
    baseline: dict[str, Any] | None = None,
    provisional_decision: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    """Probe allowlisted host/context/media-host fallbacks within a bounded budget."""
    if not _should_probe_alternatives(tracks + artists + albums, provisional_decision):
        return []

    track_sentinels = _sentinel_list(config, "track_sentinels")
    if not track_sentinels:
        return []

    api_cfg = config.get("api") if isinstance(config.get("api"), dict) else {}
    quality_tiers = [int(t) for t in (config.get("quality_tiers") or QUALITY_TIERS)]
    budget_seconds = float(config.get("alternative_discovery_budget_seconds") or 90.0)
    deadline = time.monotonic() + max(5.0, budget_seconds)

    results: list[dict[str, Any]] = []
    for candidate in _build_protocol_candidates(api_cfg):
        if time.monotonic() >= deadline:
            break
        results.append(
            _evaluate_protocol_candidate(
                candidate, config, api_cfg, quality_tiers, baseline, deadline
            )
        )
    return results


def _api_contract_snapshot(api_cfg: dict[str, Any]) -> dict[str, str]:
    return {
        "host": str(api_cfg.get("host") or "www.jiosaavn.com"),
        "api_version": str(api_cfg.get("api_version") or "4"),
        "client_context": str(api_cfg.get("client_context") or "web6dot0"),
        "open_media_host": str(api_cfg.get("open_media_host") or "aac.saavncdn.com"),
    }


def _collect_upstreams(config: dict[str, Any]) -> list[dict[str, Any]]:
    upstreams_cfg = config.get("upstreams")
    if not isinstance(upstreams_cfg, list):
        return []
    return [
        _fetch_upstream(str(item.get("repo") or ""), str(item.get("branch") or "main"))
        for item in upstreams_cfg
        if isinstance(item, dict)
    ]


def _current_observation(
    config: dict[str, Any],
    *,
    baseline: dict[str, Any] | None = None,
) -> dict[str, Any]:
    api_cfg = config.get("api") if isinstance(config.get("api"), dict) else {}
    attempts = int(config.get("probe_attempts") or 2)
    retry_delay = float(config.get("retry_delay_seconds") or 2.0)
    quality_tiers = [int(t) for t in (config.get("quality_tiers") or QUALITY_TIERS)]

    track_sentinels = _sentinel_list(config, "track_sentinels")
    if not track_sentinels:
        raise SyncError("config requires at least one track sentinel")

    tracks = [
        _probe_track_sentinel(
            s, api_cfg, quality_tiers, attempts=attempts, retry_delay_seconds=retry_delay
        )
        for s in track_sentinels
    ]
    artists = [
        _probe_artist_sentinel(s, api_cfg, attempts=attempts, retry_delay_seconds=retry_delay)
        for s in _sentinel_list(config, "artist_sentinels")
    ]
    albums = [
        _probe_album_sentinel(s, api_cfg, attempts=attempts, retry_delay_seconds=retry_delay)
        for s in _sentinel_list(config, "album_sentinels")
    ]
    observation = {
        "schema": SCHEMA_VERSION,
        "observed_at_epoch": int(time.time()),
        "api_contract": _api_contract_snapshot(api_cfg),
        "tracks": tracks,
        "artists": artists,
        "albums": albums,
        "protocol_alternatives": [],
        "upstreams": [],
    }
    provisional_decision = (
        _classify(baseline, observation, config) if isinstance(baseline, dict) else None
    )
    observation["protocol_alternatives"] = _probe_protocol_alternatives(
        config,
        tracks,
        artists,
        albums,
        baseline=baseline,
        provisional_decision=provisional_decision,
    )
    observation["upstreams"] = _collect_upstreams(config)
    return observation


def _entity_map(items: Any) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    if isinstance(items, list):
        for item in items:
            if isinstance(item, dict):
                result[str(item.get("name") or "")] = item
    return result


def _is_access_blocked(entry: dict[str, Any]) -> bool:
    obs = entry.get("observation") if isinstance(entry.get("observation"), dict) else {}
    return bool(obs.get("access_blocked"))


def _classify_without_baseline(failed_required: list[dict[str, Any]]) -> dict[str, Any]:
    if not failed_required:
        return {
            "decision": "bootstrap",
            "severity": "info",
            "material_changes": ["No accepted JioSaavn baseline exists yet."],
            "informational_changes": [],
        }
    blocked_only = all(_is_access_blocked(item) for item in failed_required)
    failing_names = ", ".join(item["name"] for item in failed_required)
    return {
        "decision": "blocked" if blocked_only else "repair",
        "severity": "warning" if blocked_only else "major",
        "material_changes": [] if blocked_only else [
            f"Bootstrap failed on non-blocked required sentinels: {failing_names}"
        ],
        "informational_changes": [
            f"Baseline not seeded; failing sentinels: {failing_names}"
        ] if blocked_only else [],
    }


def _diff_track_streams(
    name: str,
    old_streams: dict[str, Any],
    new_streams: dict[str, Any],
) -> tuple[list[str], list[str], bool]:
    material: list[str] = []
    info: list[str] = []
    regressed = False
    if old_streams.get("playable_any_tier") and not new_streams.get("playable_any_tier"):
        regressed = True
        material.append(f"track/{name}: all audio tiers (96/160/320) became unplayable")
    elif old_streams.get("decrypt_ok") and not new_streams.get("decrypt_ok"):
        if not new_streams.get("auth_url_valid"):
            regressed = True
            material.append(f"track/{name}: both DES decryption and song.generateAuthToken failed")
        else:
            info.append(f"track/{name}: local DES decryption failed while auth token still works")

    old_tiers = set(old_streams.get("verified_tiers") or [])
    new_tiers = set(new_streams.get("verified_tiers") or [])
    lost_tiers = sorted(old_tiers - new_tiers)
    if lost_tiers and new_streams.get("playable_any_tier"):
        info.append(f"track/{name}: quality tier(s) {lost_tiers} unavailable while {sorted(new_tiers)} playable")
    return material, info, regressed


def _check_track_metadata_regressions(
    name: str,
    previous: dict[str, Any],
    current: dict[str, Any],
    old_obs: dict[str, Any],
    new_obs: dict[str, Any],
) -> tuple[list[str], bool]:
    material: list[str] = []
    if previous.get("expect_explicit_and_clean") and old_obs.get("explicit_and_clean_ok") and not new_obs.get("explicit_and_clean_ok"):
        material.append(f"track/{name}: explicit/clean coexistence metadata missing")

    old_details = old_obs.get("details") if isinstance(old_obs.get("details"), dict) else {}
    new_details = new_obs.get("details") if isinstance(new_obs.get("details"), dict) else {}
    if old_details.get("ok") and not new_details.get("ok"):
        material.append(f"track/{name}: song.getDetails failed (envelope={new_details.get('envelope_kind')})")

    old_lyrics = old_obs.get("lyrics") if isinstance(old_obs.get("lyrics"), dict) else {}
    new_lyrics = new_obs.get("lyrics") if isinstance(new_obs.get("lyrics"), dict) else {}
    if current.get("expect_lyrics") and old_lyrics.get("ok") and not new_lyrics.get("ok"):
        material.append(f"track/{name}: lyrics.getLyrics stopped returning lyrics")

    return material, bool(material)


def _diff_single_track(
    name: str,
    previous: dict[str, Any],
    current: dict[str, Any],
) -> tuple[list[str], list[str], bool]:
    material: list[str] = []
    info: list[str] = []
    regressed = False
    old_obs = previous.get("observation") if isinstance(previous.get("observation"), dict) else {}
    new_obs = current.get("observation") if isinstance(current.get("observation"), dict) else {}

    if bool(current.get("required", True)) and previous.get("ok") and not current.get("ok"):
        if _is_access_blocked(current):
            return [], [f"track/{name}: probe access-blocked (geo/rate-limit/transient)"], False
        regressed = True
        material.append(f"track/{name}: required probe regressed ({new_obs.get('error') or 'contract mismatch'})")

    if _is_access_blocked(current):
        return material, info, regressed

    m_meta, r_meta = _check_track_metadata_regressions(name, previous, current, old_obs, new_obs)
    material.extend(m_meta)

    s_mat, s_info, s_reg = _diff_track_streams(
        name,
        old_obs.get("streams") if isinstance(old_obs.get("streams"), dict) else {},
        new_obs.get("streams") if isinstance(new_obs.get("streams"), dict) else {},
    )
    material.extend(s_mat)
    info.extend(s_info)
    return material, info, regressed or r_meta or s_reg


def _diff_all_tracks(
    tracks_before: dict[str, dict[str, Any]],
    tracks_after: dict[str, dict[str, Any]],
) -> tuple[list[str], list[str], set[str]]:
    material: list[str] = []
    info: list[str] = []
    regressions: set[str] = set()
    for name, current in tracks_after.items():
        previous = tracks_before.get(name)
        if not previous:
            info.append(f"track/{name}: new sentinel")
            continue
        t_mat, t_info, regressed = _diff_single_track(name, previous, current)
        material.extend(t_mat)
        info.extend(t_info)
        if regressed:
            regressions.add(name)
    return material, info, regressions


def _diff_entity_sentinels(
    kind: str,
    before_map: dict[str, dict[str, Any]],
    after_map: dict[str, dict[str, Any]],
) -> tuple[list[str], list[str], set[str]]:
    material: list[str] = []
    info: list[str] = []
    regressions: set[str] = set()
    for name, current in after_map.items():
        previous = before_map.get(name)
        if not previous:
            info.append(f"{kind}/{name}: new sentinel")
            continue
        if not bool(current.get("required", True)):
            continue
        if previous.get("ok") and not current.get("ok"):
            if _is_access_blocked(current):
                info.append(f"{kind}/{name}: access-blocked (geo/rate-limit/transient)")
            else:
                regressions.add(f"{kind}/{name}")
                obs = current.get("observation") if isinstance(current.get("observation"), dict) else {}
                err = obs.get("error") or "schema mismatch"
                material.append(f"{kind}/{name}: failed ({err})")
    return material, info, regressions


def _collect_entity_regressions(
    accepted: dict[str, Any],
    artists_after: dict[str, dict[str, Any]],
    albums_after: dict[str, dict[str, Any]],
) -> tuple[list[str], list[str], set[str]]:
    material: list[str] = []
    info: list[str] = []
    regressions: set[str] = set()
    for kind, before_key, after_map in (
        ("artist", "artists", artists_after),
        ("album", "albums", albums_after),
    ):
        e_mat, e_info, e_regs = _diff_entity_sentinels(kind, _entity_map(accepted.get(before_key)), after_map)
        material.extend(e_mat)
        info.extend(e_info)
        regressions.update(e_regs)
    return material, info, regressions


def _baseline_schema_version(baseline: dict[str, Any], accepted: Any) -> Any:
    schema = baseline.get("schema")
    if schema is None and isinstance(accepted, dict):
        return accepted.get("schema")
    return schema


def _failed_required_probes(
    *groups: dict[str, dict[str, Any]],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    all_required = [
        item
        for group in groups
        for item in group.values()
        if bool(item.get("required", True))
    ]
    failed_required = [item for item in all_required if not item.get("ok")]
    return all_required, failed_required


def _verified_tiers_for(entry: dict[str, Any]) -> list[Any]:
    obs = entry.get("observation") if isinstance(entry.get("observation"), dict) else {}
    streams = obs.get("streams") if isinstance(obs.get("streams"), dict) else {}
    tiers = streams.get("verified_tiers")
    return tiers if isinstance(tiers, list) else []


def _has_global_320_loss(
    tracks_before: dict[str, dict[str, Any]],
    tracks_after: dict[str, dict[str, Any]],
    track_threshold: int,
) -> bool:
    previously_320 = [
        name for name, prev in tracks_before.items() if 320 in _verified_tiers_for(prev)
    ]
    comparable_non_blocked = [
        name
        for name in previously_320
        if name in tracks_after and not _is_access_blocked(tracks_after[name])
    ]
    now_320 = [
        name for name in comparable_non_blocked if 320 in _verified_tiers_for(tracks_after[name])
    ]
    return (
        len(previously_320) >= track_threshold
        and len(comparable_non_blocked) >= track_threshold
        and not now_320
    )


def _apply_regression_thresholds(
    material: list[str],
    info: list[str],
    regression_count: int,
    threshold: int,
    prefixes: tuple[str, ...],
    label: str,
) -> list[str]:
    if regression_count and regression_count < threshold:
        filtered = [m for m in material if not m.startswith(prefixes)]
        downgraded = [m for m in material if m.startswith(prefixes)]
        info.extend(downgraded)
        info.append(
            f"{label} regression count {regression_count} below repair threshold {threshold}"
        )
        return filtered
    return material


def _classify(
    baseline: dict[str, Any],
    observation: dict[str, Any],
    config: dict[str, Any],
) -> dict[str, Any]:
    accepted = baseline.get("observation")
    baseline_schema = _baseline_schema_version(baseline, accepted)
    if isinstance(accepted, dict) and baseline_schema != SCHEMA_VERSION:
        return {
            "decision": "blocked",
            "severity": "warning",
            "material_changes": [],
            "informational_changes": [
                f"Baseline schema {baseline_schema!r} differs from {SCHEMA_VERSION}; re-seed baseline."
            ],
        }

    tracks_after = _entity_map(observation.get("tracks"))
    artists_after = _entity_map(observation.get("artists"))
    albums_after = _entity_map(observation.get("albums"))
    all_required, failed_required = _failed_required_probes(tracks_after, artists_after, albums_after)

    if failed_required and len(failed_required) == len(all_required) and all(
        _is_access_blocked(item) for item in failed_required
    ):
        return {
            "decision": "blocked",
            "severity": "warning",
            "material_changes": [],
            "informational_changes": [
                "All required JioSaavn probes were blocked at the network/geo/rate-limit layer; no repair attempted."
            ],
        }

    if not isinstance(accepted, dict):
        return _classify_without_baseline(failed_required)

    tracks_before = _entity_map(accepted.get("tracks"))
    material, info, track_regressions = _diff_all_tracks(tracks_before, tracks_after)
    e_mat, e_info, entity_regressions = _collect_entity_regressions(accepted, artists_after, albums_after)
    material.extend(e_mat)
    info.extend(e_info)

    thresholds = config.get("thresholds") if isinstance(config.get("thresholds"), dict) else {}
    track_threshold = int(thresholds.get("required_track_regressions_for_repair") or 2)
    entity_threshold = int(thresholds.get("required_entity_regressions_for_repair") or 2)

    if _has_global_320_loss(tracks_before, tracks_after, track_threshold):
        material.append("global_quality: 320 kbps tier disappeared across all non-blocked 320-capable tracks")

    material = _apply_regression_thresholds(
        material, info, len(track_regressions), track_threshold, ("track/",), "Track"
    )
    material = _apply_regression_thresholds(
        material, info, len(entity_regressions), entity_threshold, ("artist/", "album/"), "Artist/album"
    )

    return {
        "decision": "repair" if material else "none",
        "severity": "major" if material else "info",
        "material_changes": material,
        "informational_changes": info,
    }


def _sanitize_for_baseline(observation: dict[str, Any]) -> dict[str, Any]:
    safe = {
        "schema": SCHEMA_VERSION,
        "observed_at_epoch": observation.get("observed_at_epoch"),
        "api_contract": observation.get("api_contract"),
        "tracks": observation.get("tracks"),
        "artists": observation.get("artists"),
        "albums": observation.get("albums"),
        "upstreams": observation.get("upstreams"),
    }
    serialized = json.dumps(safe)
    return json.loads(serialized)


def _format_tier_cell(tiers: dict[str, Any], tier: int) -> str:
    info = tiers.get(str(tier))
    if not isinstance(info, dict):
        return "n/a"
    if info.get("valid"):
        return f"ok ({info.get('estimated_kbps')}k)"
    return str(info.get("status") or "fail")


def _format_track_row(item: dict[str, Any]) -> str:
    obs = item.get("observation") if isinstance(item.get("observation"), dict) else {}
    details = obs.get("details") if isinstance(obs.get("details"), dict) else {}
    lyrics = obs.get("lyrics") if isinstance(obs.get("lyrics"), dict) else {}
    streams = obs.get("streams") if isinstance(obs.get("streams"), dict) else {}
    tiers = streams.get("tiers") if isinstance(streams.get("tiers"), dict) else {}
    lyr_cell = "n/a" if lyrics.get("skipped") else ("ok" if lyrics.get("ok") else "fail")
    return (
        "| {name} | {ok} | {det} | {exp} | {lyr} | {des} | {auth} | {t96} | {t160} | {t320} |".format(
            name=item.get("name", ""),
            ok="yes" if item.get("ok") else "no",
            det=details.get("envelope_kind") or obs.get("error") or "-",
            exp="yes" if obs.get("explicit_and_clean_ok") else "no",
            lyr=lyr_cell,
            des="yes" if streams.get("decrypt_ok") else "no",
            auth="yes" if streams.get("auth_url_valid") else "no",
            t96=_format_tier_cell(tiers, 96),
            t160=_format_tier_cell(tiers, 160),
            t320=_format_tier_cell(tiers, 320),
        )
    )


def _render_track_rows(tracks: Any) -> list[str]:
    return [_format_track_row(item) for item in (tracks or []) if isinstance(item, dict)]


def _render_entity_rows(observation: dict[str, Any]) -> list[str]:
    rows: list[str] = []
    for kind, key in (("artist", "artists"), ("album", "albums")):
        for item in observation.get(key) or []:
            if not isinstance(item, dict):
                continue
            obs = item.get("observation") if isinstance(item.get("observation"), dict) else {}
            if obs.get("error"):
                summary = str(obs.get("error"))[:80]
            elif kind == "artist":
                summary = f"songs={obs.get('top_songs_count', 0)}, albums={obs.get('top_albums_count', 0)}"
            else:
                summary = f"tracks={obs.get('list_count', 0)}"
            rows.append(f"| {kind} | {item.get('name', '')} | {'yes' if item.get('ok') else 'no'} | {summary} |")
    return rows


def _render_alternative_rows(alternatives: Any) -> list[str]:
    if not alternatives:
        return []
    lines = ["", "## Protocol fallback diagnostics", ""]
    for alt in alternatives:
        lines.append(
            "- host=`{host}` ctx=`{ctx}` media=`{media}` passed={passed} "
            "(tracks={tracks}, artists={artists}, albums={albums})".format(
                host=alt.get("api_host"),
                ctx=alt.get("client_context"),
                media=alt.get("open_media_host"),
                passed="yes" if alt.get("all_passed") else "no",
                tracks=alt.get("tracks_ok"),
                artists=alt.get("artists_ok"),
                albums=alt.get("albums_ok"),
            )
        )
    return lines


def _render_upstream_rows(upstreams: Any) -> list[str]:
    lines = ["", "## Upstream radar (untrusted evidence only)", ""]
    for up in upstreams or []:
        if not isinstance(up, dict):
            continue
        lines.append(f"- **{up.get('repo')}** ({up.get('branch')}):")
        if up.get("error"):
            lines.append(f"  - unavailable: {up.get('error')}")
            continue
        commits = up.get("relevant_commits") or []
        if not commits:
            lines.append("  - no recent JioSaavn-matching commits")
        lines.extend(f"  - `{c.get('sha', '')}` {c.get('message', '')}" for c in commits)
    return lines


def _render_report(observation: dict[str, Any], decision: dict[str, Any]) -> str:
    lines = [
        "# Levyra JioSaavn Sync",
        "",
        f"- Decision: **{decision['decision']}**",
        f"- Severity: **{decision['severity']}**",
        "",
        "## Material changes",
        *([f"- {item}" for item in (decision.get("material_changes") or [])] or ["- None"]),
        "",
        "## Informational changes",
        *([f"- {item}" for item in (decision.get("informational_changes") or [])] or ["- None"]),
        "",
        "## Track, metadata & stream quality status",
        "",
        "| Sentinel | OK | Details | Explicit+Clean | Lyrics | DES | AuthToken | 96 | 160 | 320 |",
        "|---|---:|---|---:|---:|---:|---:|---|---|---|",
        *_render_track_rows(observation.get("tracks")),
        "",
        "## Artist & album endpoints",
        "",
        "| Kind | Sentinel | OK | Summary |",
        "|---|---|---:|---|",
        *_render_entity_rows(observation),
        *_render_alternative_rows(observation.get("protocol_alternatives")),
        *_render_upstream_rows(observation.get("upstreams")),
        "",
        "## Privacy & security",
        "",
        "- No credentials, encrypted media payloads, auth URLs, signed query strings, or raw IP addresses are persisted.",
        "- All media probes are bounded `Range: bytes=0-8191` requests to HTTPS `*.saavncdn.com`.",
        "",
    ]
    return "\n".join(lines)


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _select_passing_alternative(
    observation: dict[str, Any],
) -> tuple[tuple[str, str, str] | None, str]:
    alternatives = [
        alt
        for alt in (observation.get("protocol_alternatives") or [])
        if isinstance(alt, dict) and alt.get("all_passed") is True
    ]
    if not alternatives:
        return None, "no passing allowlisted protocol alternative found"
    chosen = alternatives[0]
    new_host = str(chosen.get("api_host") or "")
    new_ctx = str(chosen.get("client_context") or "")
    new_media = str(chosen.get("open_media_host") or "")
    if (
        new_host not in ALLOWLISTED_API_HOSTS
        or new_ctx not in ALLOWLISTED_CONTEXTS
        or new_media not in ALLOWLISTED_OPEN_MEDIA_HOSTS
    ):
        return None, "candidate outside protocol allowlist"
    return (new_host, new_ctx, new_media), ""


def _sync_protocol_mirrors(
    endpoints_test_path: Path,
    config_path: Path,
    new_host: str,
    new_ctx: str,
    new_media: str,
) -> None:
    if endpoints_test_path.is_file():
        test_text = endpoints_test_path.read_text(encoding="utf-8")
        test_updated = re.sub(
            r'assertEquals\("(?:web6dot0|android|iphone)",\s*url\.queryParameter\("ctx"\)\)',
            f'assertEquals("{new_ctx}", url.queryParameter("ctx"))',
            test_text,
        )
        test_updated = re.sub(
            r'"https://(?:aac|c|h)\.saavncdn\.com(/820/5ddb9a79a5218f85ca9bef170f3a461d_160\.mp4")',
            f'"https://{new_media}\\g<1>',
            test_updated,
        )
        if test_updated != test_text:
            endpoints_test_path.write_text(test_updated, encoding="utf-8")

    if config_path.is_file():
        cfg = json.loads(config_path.read_text(encoding="utf-8"))
        cfg.setdefault("api", {})
        cfg["api"]["host"] = new_host
        cfg["api"]["client_context"] = new_ctx
        cfg["api"]["open_media_host"] = new_media
        _write_json(config_path, cfg)


def apply_deterministic_protocol_fix(
    observation: dict[str, Any],
    repo_root: Path,
) -> dict[str, Any]:
    """Apply an allowlisted host/context/media-host fix only when proven healthy."""
    selected, error_reason = _select_passing_alternative(observation)
    if selected is None:
        return {"applied": False, "reason": error_reason}

    new_host, new_ctx, new_media = selected
    endpoints_path = repo_root / "app/src/main/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpoints.kt"
    if not endpoints_path.is_file():
        return {"applied": False, "reason": "JioSaavnEndpoints.kt not found"}

    text = endpoints_path.read_text(encoding="utf-8")
    updated = re.sub(r'(const val API_HOST = ")[^"]+(")', rf"\g<1>{new_host}\g<2>", text, count=1)
    updated = re.sub(r'(const val OPEN_MEDIA_HOST = ")[^"]+(")', rf"\g<1>{new_media}\g<2>", updated, count=1)
    updated = re.sub(r'(private const val CLIENT_CONTEXT = ")[^"]+(")', rf"\g<1>{new_ctx}\g<2>", updated, count=1)
    if updated == text:
        return {"applied": False, "reason": "endpoints already match chosen alternative"}

    endpoints_path.write_text(updated, encoding="utf-8")
    _sync_protocol_mirrors(
        repo_root / "app/src/test/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpointsTest.kt",
        repo_root / "jiosaavn-sync/config.json",
        new_host,
        new_ctx,
        new_media,
    )
    return {
        "applied": True,
        "api_host": new_host,
        "client_context": new_ctx,
        "open_media_host": new_media,
    }


def command_probe(args: argparse.Namespace) -> int:
    config = json.loads(Path(args.config).read_text(encoding="utf-8"))
    baseline_path = Path(args.baseline)
    baseline = (
        json.loads(baseline_path.read_text(encoding="utf-8"))
        if baseline_path.exists()
        else {"schema": SCHEMA_VERSION, "observation": None}
    )
    observation = _current_observation(config, baseline=baseline)
    decision = _classify(baseline, observation, config)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    _write_json(out_dir / "observation.json", observation)
    _write_json(out_dir / "decision.json", decision)
    (out_dir / "report.md").write_text(_render_report(observation, decision), encoding="utf-8")
    print(json.dumps(decision, sort_keys=True))
    if args.github_output:
        with Path(args.github_output).open("a", encoding="utf-8") as handle:
            handle.write(f"decision={decision['decision']}\n")
            handle.write(f"severity={decision['severity']}\n")
    return 0


def command_accept(args: argparse.Namespace) -> int:
    observation = json.loads(Path(args.observation).read_text(encoding="utf-8"))
    baseline = {
        "schema": SCHEMA_VERSION,
        "accepted_at_epoch": int(time.time()),
        "accepted_reason": str(args.reason or "validated JioSaavn sync observation")[:240],
        "observation": _sanitize_for_baseline(observation),
    }
    _write_json(Path(args.baseline), baseline)
    return 0


def command_deterministic_fix(args: argparse.Namespace) -> int:
    observation = json.loads(Path(args.observation).read_text(encoding="utf-8"))
    outcome = apply_deterministic_protocol_fix(observation, Path(args.repo_root))
    print(json.dumps(outcome, sort_keys=True))
    if args.github_output:
        with Path(args.github_output).open("a", encoding="utf-8") as handle:
            handle.write(f"applied={'true' if outcome.get('applied') else 'false'}\n")
    return 0


def command_incident(args: argparse.Namespace) -> int:
    report = Path(args.report).read_text(encoding="utf-8")
    destination = Path(args.destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        "<!-- Auto-generated by Levyra JioSaavn Sync. Sanitized metadata only. -->\n\n" + report,
        encoding="utf-8",
    )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    probe = subparsers.add_parser("probe")
    probe.add_argument("--config", required=True)
    probe.add_argument("--baseline", required=True)
    probe.add_argument("--out-dir", required=True)
    probe.add_argument("--github-output", default="")
    probe.set_defaults(func=command_probe)

    accept = subparsers.add_parser("accept")
    accept.add_argument("--observation", required=True)
    accept.add_argument("--baseline", required=True)
    accept.add_argument("--reason", default="")
    accept.set_defaults(func=command_accept)

    det_fix = subparsers.add_parser("deterministic-fix")
    det_fix.add_argument("--observation", required=True)
    det_fix.add_argument("--repo-root", default=".")
    det_fix.add_argument("--github-output", default="")
    det_fix.set_defaults(func=command_deterministic_fix)

    incident = subparsers.add_parser("incident")
    incident.add_argument("--report", required=True)
    incident.add_argument("--destination", required=True)
    incident.set_defaults(func=command_incident)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    try:
        return int(args.func(args))
    except (SyncError, OSError, ValueError, json.JSONDecodeError) as error:
        print(f"JioSaavn sync failed: {sanitize_text(str(error))}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
