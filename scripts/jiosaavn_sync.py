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


def parse_media_location(
    raw_url: str,
    *,
    override_open_host: str | None = None,
    authorized: bool = False,
) -> MediaLocation | None:
    parsed = urllib.parse.urlsplit(raw_url.strip())
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
    segments = [seg for seg in parsed.path.split("/") if seg]
    if len(segments) < 2:
        return None
    match = MEDIA_FILE_RE.match(segments[-1])
    if not match:
        return None
    folders = segments[:-1]
    if not all(PATH_SEGMENT_RE.match(seg) for seg in folders):
        return None
    query = urllib.parse.parse_qs(parsed.query)
    open_host = override_open_host or host
    return MediaLocation(
        open_host=open_host,
        directory="".join(f"/{seg}" for seg in folders),
        stem=match.group(1),
        extension=match.group(3).lower(),
        authorized_tier=int(match.group(2)) if authorized else None,
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


def _inspect_song_schema(songs: list[dict[str, Any]]) -> dict[str, Any]:
    explicit_values: set[str] = set()
    offers320_values: set[str] = set()
    more_info_keys: set[str] = set()
    artist_map_keys: set[str] = set()
    playable_count = 0
    lyrics_capable_id = ""
    first_playable: dict[str, Any] | None = None

    for item in songs:
        exp = item.get("explicit_content")
        if exp is not None:
            explicit_values.add(str(exp).strip().lower())
        info = item.get("more_info")
        if isinstance(info, dict):
            more_info_keys.update(str(k) for k in info.keys())
            flag320 = info.get("320kbps")
            if flag320 is not None:
                offers320_values.add(str(flag320).strip().lower())
            artist_map = info.get("artistMap")
            if isinstance(artist_map, dict):
                artist_map_keys.update(str(k) for k in artist_map.keys())
            encrypted = str(info.get("encrypted_media_url") or "").strip()
            song_id = str(item.get("id") or "").strip()
            if song_id and encrypted:
                playable_count += 1
                if first_playable is None:
                    first_playable = item
                if not lyrics_capable_id and str(info.get("has_lyrics") or "").lower() == "true":
                    lyrics_capable_id = song_id

    return {
        "playable_count": playable_count,
        "has_explicit": "1" in explicit_values or "true" in explicit_values,
        "has_clean": "0" in explicit_values or "false" in explicit_values,
        "explicit_values": sorted(explicit_values),
        "offers320_values": sorted(offers320_values),
        "more_info_has_required": {
            "encrypted_media_url": "encrypted_media_url" in more_info_keys,
            "duration": "duration" in more_info_keys,
            "album": "album" in more_info_keys,
            "artistMap": "artistMap" in more_info_keys,
        },
        "artist_map_keys": sorted(artist_map_keys),
        "_first_playable": first_playable,
        "_lyrics_id": lyrics_capable_id,
    }


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

    envelope_kind = "unrecognized"
    entry: dict[str, Any] | None = None
    if isinstance(payload.get("songs"), list):
        envelope_kind = "songs_array"
        for item in payload["songs"]:
            if isinstance(item, dict) and str(item.get("id") or "") == song_id:
                entry = item
                break
    elif isinstance(payload.get(song_id), dict):
        envelope_kind = "id_keyed_object"
        entry = payload[song_id]

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
    location = (
        parse_media_location(
            decoded,
            override_open_host=None,
            authorized=False,
        )
        if decoded
        else None
    )

    auth_payload, auth_meta = _api_call(
        api_cfg,
        endpoints.get("authorize_media", "song.generateAuthToken"),
        {"url": encrypted_payload, "bitrate": "320"},
        host_override=host_override,
        context_override=context_override,
        max_rotations=max_rotations,
    )
    auth_status = ""
    auth_loc: MediaLocation | None = None
    if isinstance(auth_payload, dict):
        auth_status = str(auth_payload.get("status") or "").lower()
        raw_auth_url = auth_payload.get("auth_url")
        if auth_status == "success" and isinstance(raw_auth_url, str):
            auth_loc = parse_media_location(
                raw_auth_url,
                override_open_host=open_media_host_override
                or str(api_cfg.get("open_media_host") or "aac.saavncdn.com"),
                authorized=True,
            )

    profile = build_india_profile()
    media_headers = profile.media_headers()
    tier_results: dict[str, Any] = {}
    verified_tiers: set[int] = set()
    candidates = [loc for loc in (location, auth_loc) if loc is not None]
    saw_transport_attempt = False
    all_transport_blocked = True

    for tier in quality_tiers:
        chosen_validation: dict[str, Any] | None = None
        for idx, loc in enumerate(candidates):
            saw_transport_attempt = True
            url = loc.open_url(tier)
            validation = _probe_single_media_url(url, media_headers, tier, duration_seconds)
            if chosen_validation is None or validation.get("valid"):
                chosen_validation = validation
            if validation.get("valid"):
                all_transport_blocked = False
                break
            if validation.get("status") != "transport_error" and validation.get("http_status") not in (
                403,
                429,
                500,
                502,
                503,
            ):
                if idx == len(candidates) - 1:
                    all_transport_blocked = False
        if chosen_validation is not None:
            tier_results[str(tier)] = chosen_validation
            measured_tier = chosen_validation.get("verified_tier")
            if chosen_validation.get("valid") and isinstance(measured_tier, int):
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
    required = bool(sentinel.get("required", True))
    expect_explicit_and_clean = bool(sentinel.get("expect_explicit_and_clean", False))
    expect_lyrics = bool(sentinel.get("expect_lyrics", False))

    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        search_payload, search_meta = _api_call(
            api_cfg,
            endpoints.get("search_songs", "search.getResults"),
            {"q": query, "n": 10, "p": 1},
            host_override=host_override,
            context_override=context_override,
            max_rotations=max_rotations,
        )
        if not search_meta.get("ok"):
            last_result = {
                "ok": False,
                "access_blocked": bool(search_meta.get("access_blocked")),
                "error": f"search_failed:{search_meta.get('error')}",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        songs, search_summary = _extract_song_list(search_payload)
        schema_info = _inspect_song_schema(songs)
        first_song = schema_info.pop("_first_playable", None)
        lyrics_id = schema_info.pop("_lyrics_id", "")

        if not first_song:
            last_result = {
                "ok": False,
                "access_blocked": False,
                "search": search_summary,
                "schema": schema_info,
                "error": "no_playable_song_in_search",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        first_info = first_song.get("more_info") or {}
        song_id = str(first_song.get("id") or "")
        duration_sec = _parse_duration_seconds(first_info.get("duration"))
        if duration_sec is None:
            last_result = {
                "ok": False,
                "access_blocked": False,
                "search": search_summary,
                "schema": schema_info,
                "error": "invalid_duration_schema",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        encrypted_payload = str(first_info.get("encrypted_media_url") or "").strip()

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
            encrypted_payload,
            duration_sec,
            quality_tiers,
            host_override=host_override,
            context_override=context_override,
            open_media_host_override=open_media_host_override,
            max_rotations=max_rotations,
        )

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
        last_result = {
            "ok": all_ok,
            "access_blocked": access_blocked,
            "search": search_summary,
            "schema": schema_info,
            "explicit_and_clean_ok": explicit_ok,
            "details": details,
            "lyrics": lyrics,
            "streams": streams,
        }
        if all_ok:
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)

    return {
        "name": name,
        "required": required,
        "expect_explicit_and_clean": expect_explicit_and_clean,
        "expect_lyrics": expect_lyrics,
        "ok": bool(last_result.get("ok")),
        "observation": last_result,
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
    name = str(sentinel.get("name") or sentinel.get("query") or "")
    query = str(sentinel.get("query") or "")
    expected_name = str(sentinel.get("expected_name") or "").lower()
    required = bool(sentinel.get("required", True))

    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        search_payload, search_meta = _api_call(
            api_cfg,
            endpoints.get("search_artists", "search.getArtistResults"),
            {"q": query, "n": 5, "p": 1},
            host_override=host_override,
            context_override=context_override,
        )
        if not search_meta.get("ok"):
            last_result = {
                "ok": False,
                "access_blocked": bool(search_meta.get("access_blocked")),
                "error": f"artist_search_failed:{search_meta.get('error')}",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        results = search_payload.get("results") if isinstance(search_payload, dict) else None
        if not isinstance(results, list) or not results:
            last_result = {
                "ok": False,
                "access_blocked": False,
                "error": "artist_search_empty_or_malformed",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        matched = next(
            (
                item
                for item in results
                if isinstance(item, dict)
                and expected_name
                and expected_name in str(item.get("name") or item.get("title") or "").lower()
            ),
            results[0] if isinstance(results[0], dict) else None,
        )
        artist_id = str((matched or {}).get("id") or "").strip()
        if not artist_id:
            last_result = {"ok": False, "access_blocked": False, "error": "artist_id_missing"}
            continue

        details_payload, details_meta = _api_call(
            api_cfg,
            endpoints.get("artist_details", "artist.getArtistPageDetails"),
            {
                "artistId": artist_id,
                "n_song": 5,
                "n_album": 5,
                "page": 0,
                "sort_order": "desc",
                "category": "popularity",
            },
            host_override=host_override,
            context_override=context_override,
        )
        if not details_meta.get("ok") or not isinstance(details_payload, dict):
            last_result = {
                "ok": False,
                "access_blocked": bool(details_meta.get("access_blocked")),
                "error": f"artist_details_failed:{details_meta.get('error', 'non_object')}",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        top_songs = details_payload.get("topSongs")
        top_albums = details_payload.get("topAlbums")
        has_name = bool(str(details_payload.get("name") or "").strip())
        has_songs = isinstance(top_songs, list) and len(top_songs) > 0
        has_albums = isinstance(top_albums, list) and len(top_albums) > 0
        ok = has_name and has_songs and has_albums
        last_result = {
            "ok": ok,
            "access_blocked": False,
            "search_count": len(results),
            "has_artist_id": bool(details_payload.get("artistId")),
            "has_name": has_name,
            "top_songs_count": len(top_songs) if isinstance(top_songs, list) else 0,
            "top_albums_count": len(top_albums) if isinstance(top_albums, list) else 0,
            "detail_keys": sorted(str(k) for k in details_payload.keys()),
        }
        if ok:
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)

    return {
        "name": name,
        "required": required,
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
    name = str(sentinel.get("name") or sentinel.get("query") or "")
    query = str(sentinel.get("query") or "")
    expected_title = str(sentinel.get("expected_title_contains") or "").lower()
    required = bool(sentinel.get("required", True))

    last_result: dict[str, Any] = {}
    for attempt in range(max(1, attempts)):
        search_payload, search_meta = _api_call(
            api_cfg,
            endpoints.get("search_albums", "search.getAlbumResults"),
            {"q": query, "n": 5, "p": 1},
            host_override=host_override,
            context_override=context_override,
        )
        if not search_meta.get("ok"):
            last_result = {
                "ok": False,
                "access_blocked": bool(search_meta.get("access_blocked")),
                "error": f"album_search_failed:{search_meta.get('error')}",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        results = search_payload.get("results") if isinstance(search_payload, dict) else None
        if not isinstance(results, list) or not results:
            last_result = {
                "ok": False,
                "access_blocked": False,
                "error": "album_search_empty_or_malformed",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        matched = next(
            (
                item
                for item in results
                if isinstance(item, dict)
                and expected_title
                and expected_title in str(item.get("title") or "").lower()
            ),
            results[0] if isinstance(results[0], dict) else None,
        )
        album_id = str((matched or {}).get("id") or "").strip()
        if not album_id:
            last_result = {"ok": False, "access_blocked": False, "error": "album_id_missing"}
            continue

        details_payload, details_meta = _api_call(
            api_cfg,
            endpoints.get("album_details", "content.getAlbumDetails"),
            {"albumid": album_id},
            host_override=host_override,
            context_override=context_override,
        )
        if not details_meta.get("ok") or not isinstance(details_payload, dict):
            last_result = {
                "ok": False,
                "access_blocked": bool(details_meta.get("access_blocked")),
                "error": f"album_details_failed:{details_meta.get('error', 'non_object')}",
            }
            if attempt + 1 < attempts:
                time.sleep(retry_delay_seconds)
            continue

        songs = details_payload.get("list")
        has_title = bool(str(details_payload.get("title") or "").strip())
        has_songs = isinstance(songs, list) and len(songs) > 0
        songs_have_more_info = bool(
            has_songs
            and all(isinstance(s, dict) and isinstance(s.get("more_info"), dict) for s in songs[:3])
        )
        ok = has_title and has_songs and songs_have_more_info
        last_result = {
            "ok": ok,
            "access_blocked": False,
            "search_count": len(results),
            "has_title": has_title,
            "list_count": len(songs) if isinstance(songs, list) else 0,
            "songs_have_more_info": songs_have_more_info,
            "has_more_info": isinstance(details_payload.get("more_info"), dict),
            "detail_keys": sorted(str(k) for k in details_payload.keys()),
        }
        if ok:
            break
        if attempt + 1 < attempts:
            time.sleep(retry_delay_seconds)

    return {
        "name": name,
        "required": required,
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
    needs_repair = (provisional_decision or {}).get("decision") == "repair"
    all_sentinels_ok = (
        all(t.get("ok") for t in tracks)
        and all(a.get("ok") for a in artists)
        and all(b.get("ok") for b in albums)
    )
    if all_sentinels_ok and not needs_repair:
        return []
    all_items = tracks + artists + albums
    failed_items = [item for item in all_items if not item.get("ok")]
    if failed_items and not needs_repair and all(_is_access_blocked(item) for item in failed_items):
        # All failures look like geo/rate-limit/transport outages; avoid burning CI budget.
        return []

    api_cfg = config.get("api") if isinstance(config.get("api"), dict) else {}
    primary_host = str(api_cfg.get("host") or "www.jiosaavn.com")
    primary_ctx = str(api_cfg.get("client_context") or "web6dot0")
    primary_media = str(api_cfg.get("open_media_host") or "aac.saavncdn.com")
    quality_tiers = [int(t) for t in (config.get("quality_tiers") or QUALITY_TIERS)]
    track_sentinels = [s for s in (config.get("track_sentinels") or []) if isinstance(s, dict)]
    if not track_sentinels:
        return []
    artist_sentinels = [s for s in (config.get("artist_sentinels") or []) if isinstance(s, dict)]
    album_sentinels = [s for s in (config.get("album_sentinels") or []) if isinstance(s, dict)]

    budget_seconds = float(config.get("alternative_discovery_budget_seconds") or 90.0)
    deadline = time.monotonic() + max(5.0, budget_seconds)

    candidates: list[dict[str, str]] = []
    seen_candidates: set[tuple[str, str, str]] = {(primary_host, primary_ctx, primary_media)}
    candidate_hosts = [primary_host] + [
        h for h in (api_cfg.get("fallback_hosts") or []) if h in ALLOWLISTED_API_HOSTS
    ]
    candidate_ctxs = [primary_ctx] + [
        c for c in (api_cfg.get("fallback_contexts") or []) if c in ALLOWLISTED_CONTEXTS
    ]
    candidate_media = [primary_media] + [
        m for m in (api_cfg.get("fallback_media_hosts") or []) if m in ALLOWLISTED_OPEN_MEDIA_HOSTS
    ]
    for host in candidate_hosts:
        for ctx in candidate_ctxs:
            triple = (host, ctx, primary_media)
            if triple not in seen_candidates:
                seen_candidates.add(triple)
                candidates.append(
                    {"api_host": host, "client_context": ctx, "open_media_host": primary_media}
                )
    for media_host in candidate_media:
        triple = (primary_host, primary_ctx, media_host)
        if triple not in seen_candidates:
            seen_candidates.add(triple)
            candidates.append(
                {"api_host": primary_host, "client_context": primary_ctx, "open_media_host": media_host}
            )

    results: list[dict[str, Any]] = []
    for candidate in candidates:
        if time.monotonic() >= deadline:
            break
        # Canary-first: probe only the first track sentinel before expanding to full suite.
        canary_track = _probe_track_sentinel(
            track_sentinels[0],
            api_cfg,
            quality_tiers,
            attempts=1,
            retry_delay_seconds=0.0,
            host_override=candidate["api_host"],
            context_override=candidate["client_context"],
            open_media_host_override=candidate["open_media_host"],
            max_rotations=1,
        )
        if not canary_track.get("ok"):
            results.append(
                {
                    **candidate,
                    "all_passed": False,
                    "tracks_ok": 0,
                    "artists_ok": 0,
                    "albums_ok": 0,
                }
            )
            continue

        alt_tracks = [canary_track]
        for s in track_sentinels[1:]:
            if time.monotonic() >= deadline:
                break
            alt_tracks.append(
                _probe_track_sentinel(
                    s,
                    api_cfg,
                    quality_tiers,
                    attempts=1,
                    retry_delay_seconds=0.0,
                    host_override=candidate["api_host"],
                    context_override=candidate["client_context"],
                    open_media_host_override=candidate["open_media_host"],
                    max_rotations=1,
                )
            )
        alt_artists: list[dict[str, Any]] = []
        for s in artist_sentinels:
            if time.monotonic() >= deadline:
                break
            alt_artists.append(
                _probe_artist_sentinel(
                    s,
                    api_cfg,
                    attempts=1,
                    retry_delay_seconds=0.0,
                    host_override=candidate["api_host"],
                    context_override=candidate["client_context"],
                )
            )
        alt_albums: list[dict[str, Any]] = []
        for s in album_sentinels:
            if time.monotonic() >= deadline:
                break
            alt_albums.append(
                _probe_album_sentinel(
                    s,
                    api_cfg,
                    attempts=1,
                    retry_delay_seconds=0.0,
                    host_override=candidate["api_host"],
                    context_override=candidate["client_context"],
                )
            )
        all_passed = (
            len(alt_tracks) == len(track_sentinels)
            and len(alt_artists) == len(artist_sentinels)
            and len(alt_albums) == len(album_sentinels)
            and all(item.get("ok") for item in alt_tracks)
            and all(item.get("ok") for item in alt_artists)
            and all(item.get("ok") for item in alt_albums)
        )
        if all_passed and isinstance(baseline, dict) and isinstance(baseline.get("observation"), dict):
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
            if alt_decision.get("decision") == "repair":
                all_passed = False
        results.append(
            {
                **candidate,
                "all_passed": all_passed,
                "tracks_ok": sum(1 for item in alt_tracks if item.get("ok")),
                "artists_ok": sum(1 for item in alt_artists if item.get("ok")),
                "albums_ok": sum(1 for item in alt_albums if item.get("ok")),
            }
        )
    return results


def _current_observation(
    config: dict[str, Any],
    *,
    baseline: dict[str, Any] | None = None,
) -> dict[str, Any]:
    api_cfg = config.get("api") if isinstance(config.get("api"), dict) else {}
    attempts = int(config.get("probe_attempts") or 2)
    retry_delay = float(config.get("retry_delay_seconds") or 2.0)
    quality_tiers = [int(t) for t in (config.get("quality_tiers") or QUALITY_TIERS)]

    track_sentinels = [s for s in (config.get("track_sentinels") or []) if isinstance(s, dict)]
    if not track_sentinels:
        return []
    artist_sentinels = [s for s in (config.get("artist_sentinels") or []) if isinstance(s, dict)]
    album_sentinels = [s for s in (config.get("album_sentinels") or []) if isinstance(s, dict)]
    if not track_sentinels:
        raise SyncError("config requires at least one track sentinel")

    tracks = [
        _probe_track_sentinel(
            s, api_cfg, quality_tiers, attempts=attempts, retry_delay_seconds=retry_delay
        )
        for s in track_sentinels
    ]
    artists = [
        _probe_artist_sentinel(
            s, api_cfg, attempts=attempts, retry_delay_seconds=retry_delay
        )
        for s in artist_sentinels
    ]
    albums = [
        _probe_album_sentinel(
            s, api_cfg, attempts=attempts, retry_delay_seconds=retry_delay
        )
        for s in album_sentinels
    ]
    observation = {
        "schema": SCHEMA_VERSION,
        "observed_at_epoch": int(time.time()),
        "api_contract": {
            "host": str(api_cfg.get("host") or "www.jiosaavn.com"),
            "api_version": str(api_cfg.get("api_version") or "4"),
            "client_context": str(api_cfg.get("client_context") or "web6dot0"),
            "open_media_host": str(api_cfg.get("open_media_host") or "aac.saavncdn.com"),
        },
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

    upstreams_cfg = config.get("upstreams") if isinstance(config.get("upstreams"), list) else []
    observation["upstreams"] = [
        _fetch_upstream(str(item.get("repo") or ""), str(item.get("branch") or "main"))
        for item in upstreams_cfg
        if isinstance(item, dict)
    ]
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


def _classify(
    baseline: dict[str, Any],
    observation: dict[str, Any],
    config: dict[str, Any],
) -> dict[str, Any]:
    accepted = baseline.get("observation")
    baseline_schema = baseline.get("schema")
    if baseline_schema is None and isinstance(accepted, dict):
        baseline_schema = accepted.get("schema")
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

    req_tracks = [item for item in tracks_after.values() if bool(item.get("required", True))]
    req_artists = [item for item in artists_after.values() if bool(item.get("required", True))]
    req_albums = [item for item in albums_after.values() if bool(item.get("required", True))]
    all_required = req_tracks + req_artists + req_albums
    failed_required = [item for item in all_required if not item.get("ok")]

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
        if failed_required:
            blocked_only = all(_is_access_blocked(item) for item in failed_required)
            return {
                "decision": "blocked" if blocked_only else "repair",
                "severity": "warning" if blocked_only else "major",
                "material_changes": [] if blocked_only else [
                    f"Bootstrap failed on non-blocked required sentinels: {', '.join(i['name'] for i in failed_required)}"
                ],
                "informational_changes": [
                    f"Baseline not seeded; failing sentinels: {', '.join(i['name'] for i in failed_required)}"
                ] if blocked_only else [],
            }
        return {
            "decision": "bootstrap",
            "severity": "info",
            "material_changes": ["No accepted JioSaavn baseline exists yet."],
            "informational_changes": [],
        }

    tracks_before = _entity_map(accepted.get("tracks"))
    artists_before = _entity_map(accepted.get("artists"))
    albums_before = _entity_map(accepted.get("albums"))

    material: list[str] = []
    info: list[str] = []
    track_regressions: set[str] = set()
    entity_regressions: set[str] = set()

    for name, current in tracks_after.items():
        previous = tracks_before.get(name)
        if not previous:
            info.append(f"track/{name}: new sentinel")
            continue
        required = bool(current.get("required", True))
        blocked = _is_access_blocked(current)
        old_obs = previous.get("observation") if isinstance(previous.get("observation"), dict) else {}
        new_obs = current.get("observation") if isinstance(current.get("observation"), dict) else {}

        if required and previous.get("ok") and not current.get("ok"):
            if blocked:
                info.append(f"track/{name}: probe access-blocked (geo/rate-limit/transient)")
                continue
            track_regressions.add(name)
            material.append(f"track/{name}: required probe regressed ({new_obs.get('error') or 'contract mismatch'})")

        if blocked:
            continue

        if previous.get("expect_explicit_and_clean") and old_obs.get("explicit_and_clean_ok") and not new_obs.get("explicit_and_clean_ok"):
            track_regressions.add(name)
            material.append(f"track/{name}: explicit/clean coexistence metadata missing")

        old_details = old_obs.get("details") if isinstance(old_obs.get("details"), dict) else {}
        new_details = new_obs.get("details") if isinstance(new_obs.get("details"), dict) else {}
        if old_details.get("ok") and not new_details.get("ok"):
            track_regressions.add(name)
            material.append(f"track/{name}: song.getDetails failed (envelope={new_details.get('envelope_kind')})")

        old_lyrics = old_obs.get("lyrics") if isinstance(old_obs.get("lyrics"), dict) else {}
        new_lyrics = new_obs.get("lyrics") if isinstance(new_obs.get("lyrics"), dict) else {}
        if current.get("expect_lyrics") and old_lyrics.get("ok") and not new_lyrics.get("ok"):
            track_regressions.add(name)
            material.append(f"track/{name}: lyrics.getLyrics stopped returning lyrics")

        old_streams = old_obs.get("streams") if isinstance(old_obs.get("streams"), dict) else {}
        new_streams = new_obs.get("streams") if isinstance(new_obs.get("streams"), dict) else {}
        if old_streams.get("playable_any_tier") and not new_streams.get("playable_any_tier"):
            track_regressions.add(name)
            material.append(f"track/{name}: all audio tiers (96/160/320) became unplayable")
        elif old_streams.get("decrypt_ok") and not new_streams.get("decrypt_ok"):
            if not new_streams.get("auth_url_valid"):
                track_regressions.add(name)
                material.append(f"track/{name}: both DES decryption and song.generateAuthToken failed")
            else:
                info.append(f"track/{name}: local DES decryption failed while auth token still works")

        old_tiers = set(old_streams.get("verified_tiers") or [])
        new_tiers = set(new_streams.get("verified_tiers") or [])
        lost_tiers = sorted(old_tiers - new_tiers)
        if lost_tiers and new_streams.get("playable_any_tier"):
            info.append(f"track/{name}: quality tier(s) {lost_tiers} unavailable while {sorted(new_tiers)} playable")

    for kind, before_map, after_map in (
        ("artist", artists_before, artists_after),
        ("album", albums_before, albums_after),
    ):
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
                    entity_regressions.add(f"{kind}/{name}")
                    err = (current.get("observation") or {}).get("error") or "schema mismatch"
                    material.append(f"{kind}/{name}: failed ({err})")

    thresholds = config.get("thresholds") if isinstance(config.get("thresholds"), dict) else {}
    track_threshold = int(thresholds.get("required_track_regressions_for_repair") or 2)
    entity_threshold = int(thresholds.get("required_entity_regressions_for_repair") or 2)

    # Check global 320 kbps tier loss across comparable non-blocked tracks only
    previously_320 = [
        name
        for name, prev in tracks_before.items()
        if 320 in ((prev.get("observation") or {}).get("streams") or {}).get("verified_tiers", [])
    ]
    comparable_non_blocked = [
        name
        for name in previously_320
        if name in tracks_after and not _is_access_blocked(tracks_after[name])
    ]
    now_320 = [
        name
        for name in comparable_non_blocked
        if 320 in (((tracks_after[name].get("observation") or {}).get("streams") or {}).get("verified_tiers", []))
    ]
    if (
        len(previously_320) >= track_threshold
        and len(comparable_non_blocked) >= track_threshold
        and not now_320
    ):
        material.append("global_quality: 320 kbps tier disappeared across all non-blocked 320-capable tracks")

    if track_regressions and len(track_regressions) < track_threshold:
        filtered = [m for m in material if not m.startswith("track/")]
        downgraded = [m for m in material if m.startswith("track/")]
        material = filtered
        info.extend(downgraded)
        info.append(
            f"Track regression count {len(track_regressions)} below repair threshold {track_threshold}"
        )

    if entity_regressions and len(entity_regressions) < entity_threshold:
        filtered = [m for m in material if not m.startswith(("artist/", "album/"))]
        downgraded = [m for m in material if m.startswith(("artist/", "album/"))]
        material = filtered
        info.extend(downgraded)
        info.append(
            f"Artist/album regression count {len(entity_regressions)} below repair threshold {entity_threshold}"
        )

    decision = "repair" if material else "none"
    severity = "major" if material else "info"
    return {
        "decision": decision,
        "severity": severity,
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


def _render_report(observation: dict[str, Any], decision: dict[str, Any]) -> str:
    lines = [
        "# Levyra JioSaavn Sync",
        "",
        f"- Decision: **{decision['decision']}**",
        f"- Severity: **{decision['severity']}**",
        "",
        "## Material changes",
    ]
    lines.extend([f"- {item}" for item in (decision.get("material_changes") or [])] or ["- None"])
    lines += ["", "## Informational changes"]
    lines.extend([f"- {item}" for item in (decision.get("informational_changes") or [])] or ["- None"])
    lines += [
        "",
        "## Track, metadata & stream quality status",
        "",
        "| Sentinel | OK | Details | Explicit+Clean | Lyrics | DES | AuthToken | 96 | 160 | 320 |",
        "|---|---:|---|---:|---:|---:|---:|---|---|---|",
    ]
    for item in observation.get("tracks") or []:
        if not isinstance(item, dict):
            continue
        obs = item.get("observation") if isinstance(item.get("observation"), dict) else {}
        details = obs.get("details") if isinstance(obs.get("details"), dict) else {}
        lyrics = obs.get("lyrics") if isinstance(obs.get("lyrics"), dict) else {}
        streams = obs.get("streams") if isinstance(obs.get("streams"), dict) else {}
        tiers = streams.get("tiers") if isinstance(streams.get("tiers"), dict) else {}

        def tier_cell(t: int) -> str:
            info = tiers.get(str(t))
            if not isinstance(info, dict):
                return "n/a"
            if info.get("valid"):
                return f"ok ({info.get('estimated_kbps')}k)"
            return str(info.get("status") or "fail")

        lyr_cell = "n/a" if lyrics.get("skipped") else ("ok" if lyrics.get("ok") else "fail")
        lines.append(
            "| {name} | {ok} | {det} | {exp} | {lyr} | {des} | {auth} | {t96} | {t160} | {t320} |".format(
                name=item.get("name", ""),
                ok="yes" if item.get("ok") else "no",
                det=details.get("envelope_kind") or obs.get("error") or "-",
                exp="yes" if obs.get("explicit_and_clean_ok") else "no",
                lyr=lyr_cell,
                des="yes" if streams.get("decrypt_ok") else "no",
                auth="yes" if streams.get("auth_url_valid") else "no",
                t96=tier_cell(96),
                t160=tier_cell(160),
                t320=tier_cell(320),
            )
        )

    lines += ["", "## Artist & album endpoints", "", "| Kind | Sentinel | OK | Summary |", "|---|---|---:|---|"]
    for kind, key in (("artist", "artists"), ("album", "albums")):
        for item in observation.get(key) or []:
            if not isinstance(item, dict):
                continue
            obs = item.get("observation") if isinstance(item.get("observation"), dict) else {}
            if kind == "artist":
                summary = f"songs={obs.get('top_songs_count', 0)}, albums={obs.get('top_albums_count', 0)}"
            else:
                summary = f"tracks={obs.get('list_count', 0)}"
            if obs.get("error"):
                summary = str(obs.get("error"))[:80]
            lines.append(
                f"| {kind} | {item.get('name', '')} | {'yes' if item.get('ok') else 'no'} | {summary} |"
            )

    alternatives = observation.get("protocol_alternatives") or []
    if alternatives:
        lines += ["", "## Protocol fallback diagnostics", ""]
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

    lines += [
        "",
        "## Upstream radar (untrusted evidence only)",
        "",
    ]
    for up in observation.get("upstreams") or []:
        if not isinstance(up, dict):
            continue
        lines.append(f"- **{up.get('repo')}** ({up.get('branch')}):")
        if up.get("error"):
            lines.append(f"  - unavailable: {up.get('error')}")
            continue
        commits = up.get("relevant_commits") or []
        if not commits:
            lines.append("  - no recent JioSaavn-matching commits")
        for c in commits:
            lines.append(f"  - `{c.get('sha', '')}` {c.get('message', '')}")

    lines += [
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


def apply_deterministic_protocol_fix(
    observation: dict[str, Any],
    repo_root: Path,
) -> dict[str, Any]:
    """Apply an allowlisted host/context/media-host fix only when proven healthy."""
    alternatives = [
        alt
        for alt in (observation.get("protocol_alternatives") or [])
        if isinstance(alt, dict) and alt.get("all_passed") is True
    ]
    if not alternatives:
        return {"applied": False, "reason": "no passing allowlisted protocol alternative found"}

    chosen = alternatives[0]
    new_host = str(chosen.get("api_host") or "")
    new_ctx = str(chosen.get("client_context") or "")
    new_media = str(chosen.get("open_media_host") or "")
    if (
        new_host not in ALLOWLISTED_API_HOSTS
        or new_ctx not in ALLOWLISTED_CONTEXTS
        or new_media not in ALLOWLISTED_OPEN_MEDIA_HOSTS
    ):
        return {"applied": False, "reason": "candidate outside protocol allowlist"}

    endpoints_path = repo_root / "app/src/main/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpoints.kt"
    endpoints_test_path = repo_root / "app/src/test/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpointsTest.kt"
    config_path = repo_root / "jiosaavn-sync/config.json"

    if not endpoints_path.is_file():
        return {"applied": False, "reason": "JioSaavnEndpoints.kt not found"}

    text = endpoints_path.read_text(encoding="utf-8")
    updated = re.sub(
        r'(const val API_HOST = ")[^"]+(")',
        rf"\g<1>{new_host}\g<2>",
        text,
        count=1,
    )
    updated = re.sub(
        r'(const val OPEN_MEDIA_HOST = ")[^"]+(")',
        rf"\g<1>{new_media}\g<2>",
        updated,
        count=1,
    )
    updated = re.sub(
        r'(private const val CLIENT_CONTEXT = ")[^"]+(")',
        rf"\g<1>{new_ctx}\g<2>",
        updated,
        count=1,
    )

    if updated == text:
        return {"applied": False, "reason": "endpoints already match chosen alternative"}

    endpoints_path.write_text(updated, encoding="utf-8")

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
