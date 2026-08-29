#!/usr/bin/env python3
"""Levyra YouTube protocol canary.

The canary records only sanitized protocol metadata. It never persists API keys,
visitor data, cookies, media URLs, or signed query parameters.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

SCHEMA_VERSION = 3
WATCH_MAX_BYTES = 5 * 1024 * 1024
PLAYER_JS_MAX_BYTES = 10 * 1024 * 1024
PLAYER_JSON_MAX_BYTES = 8 * 1024 * 1024
MEDIA_PROBE_MAX_BYTES = 64 * 1024
GITHUB_JSON_MAX_BYTES = 2 * 1024 * 1024
DEFAULT_TIMEOUT_SECONDS = 15.0
USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
)
VIDEO_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")

# Mirrors the clients Levyra's playback compatibility policy actually uses. ANDROID_VR stays out
# because the shipped policy disables it; the canary must not imply support Levyra does not ship.
LEVYRA_CLIENT_MATRIX: tuple[dict[str, Any], ...] = (
    {
        "name": "VISIONOS",
        "client": {
            "clientName": "VISIONOS",
            "clientVersion": "1.04",
            "deviceMake": "Apple",
            "deviceModel": "RealityDevice14,1",
            "osName": "visionOS",
            "osVersion": "1.0.3.21O566",
        },
        "user_agent": "com.google.ios.youtube/1.04 (RealityDevice14,1; U; CPU visionOS 1_0_3 like Mac OS X)",
    },
    {
        "name": "ANDROID_MUSIC",
        "client": {
            "clientName": "ANDROID_MUSIC",
            "clientVersion": "8.10.52",
            "androidSdkVersion": 34,
            "osName": "Android",
            "osVersion": "15",
        },
        "user_agent": "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15) gzip",
    },
    {
        "name": "ANDROID",
        "client": {
            "clientName": "ANDROID",
            "clientVersion": "19.44.38",
            "androidSdkVersion": 34,
            "osName": "Android",
            "osVersion": "15",
        },
        "user_agent": "com.google.android.youtube/19.44.38 (Linux; U; Android 15) gzip",
    },
    {
        "name": "IOS",
        "client": {
            "clientName": "IOS",
            "clientVersion": "20.10.4",
            "deviceMake": "Apple",
            "deviceModel": "iPhone16,2",
            "osName": "iPhone",
            "osVersion": "18.3.0.22D63",
        },
        "user_agent": "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)",
    },
    {"name": "WEB_REMIX", "client": {"clientName": "WEB_REMIX", "clientVersion": "1.20260804.16.00"}},
    {"name": "WEB", "client": {"clientName": "WEB", "clientVersion": ""}},
    {
        "name": "WEB_EMBEDDED_PLAYER",
        "client": {"clientName": "WEB_EMBEDDED_PLAYER", "clientVersion": "1.20260423.01.00"},
    },
)

DELIVERY_DIRECT_HEALTHY = "direct_healthy"
DELIVERY_DIRECT_DEGRADED = "direct_degraded"
DELIVERY_DIRECT_UNAVAILABLE = "direct_unavailable"
DELIVERY_SABR_ONLY = "sabr_only"
DELIVERY_SECURITY_FAILURE = "security_failure"
DELIVERY_CLIENT_FAILURE = "client_failure"
DELIVERY_TRANSPORT_FAILURE = "transport_failure"
KEYWORDS = ("youtube", "player", "cipher", "sabr", "innertube", "stream", "signature", "visionos", "reel")

EXACT_HTTPS_HOSTS = {
    "www.youtube.com",
    "youtube.com",
    "youtubei.googleapis.com",
    "api.github.com",
    "github.com",
    "raw.githubusercontent.com",
    "patch-diff.githubusercontent.com",
}


class CanaryError(RuntimeError):
    def __init__(self, message: str, *, status: int | None = None) -> None:
        super().__init__(message)
        self.status = status


class _SafeRedirectHandler(urllib.request.HTTPRedirectHandler):
    def __init__(self, *, media: bool) -> None:
        super().__init__()
        self._media = media
        self._redirects = 0

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        self._redirects += 1
        if self._redirects > 5:
            raise CanaryError("too many redirects")
        _safe_host(newurl, media=self._media)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


@dataclass(frozen=True)
class HttpResult:
    status: int
    headers: dict[str, str]
    body: bytes


def _safe_host(url: str, *, media: bool = False) -> str:
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != "https":
        raise CanaryError(f"non-HTTPS URL rejected: {parsed.scheme or '<missing>'}")
    if parsed.username or parsed.password:
        raise CanaryError("URL user-info rejected")
    if parsed.port not in (None, 443):
        raise CanaryError(f"non-standard HTTPS port rejected: {parsed.port}")
    host = (parsed.hostname or "").lower().rstrip(".")
    if not host:
        raise CanaryError("URL host missing")
    if media:
        if host == "googlevideo.com" or host.endswith(".googlevideo.com"):
            return host
        raise CanaryError(f"unexpected media host: {host}")
    if host not in EXACT_HTTPS_HOSTS:
        raise CanaryError(f"unexpected host: {host}")
    return host


def _bounded_request(
    url: str,
    *,
    data: bytes | None = None,
    headers: dict[str, str] | None = None,
    max_bytes: int,
    timeout: float = DEFAULT_TIMEOUT_SECONDS,
    media: bool = False,
) -> HttpResult:
    _safe_host(url, media=media)
    request = urllib.request.Request(
        url,
        data=data,
        headers=headers or {},
        method="POST" if data is not None else "GET",
    )
    opener = urllib.request.build_opener(_SafeRedirectHandler(media=media))
    try:
        with opener.open(request, timeout=timeout) as response:
            length = response.headers.get("Content-Length")
            if length:
                try:
                    if int(length) > max_bytes:
                        raise CanaryError(
                            f"response too large: declared {length} bytes > {max_bytes}"
                        )
                except ValueError:
                    pass
            body = response.read(max_bytes + 1)
            if len(body) > max_bytes:
                raise CanaryError(f"response exceeded {max_bytes} bytes")
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
        raise CanaryError(f"request failed: {type(error).__name__}: {error}") from error


def _extract_balanced_json(text: str, start_at: int) -> dict[str, Any]:
    object_start = text.find("{", start_at)
    if object_start < 0:
        raise CanaryError("JSON object start not found")

    depth = 0
    in_string = False
    escaped = False
    for index in range(object_start, len(text)):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue

        if char == '"':
            in_string = True
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                try:
                    value = json.loads(text[object_start : index + 1])
                except json.JSONDecodeError as error:
                    raise CanaryError(f"invalid embedded JSON: {error}") from error
                if not isinstance(value, dict):
                    raise CanaryError("embedded JSON is not an object")
                return value
    raise CanaryError("unterminated embedded JSON object")


def _extract_ytcfg(html: str) -> dict[str, Any]:
    merged: dict[str, Any] = {}
    offset = 0
    marker = "ytcfg.set("
    while True:
        index = html.find(marker, offset)
        if index < 0:
            break
        try:
            merged.update(_extract_balanced_json(html, index + len(marker)))
        except CanaryError:
            pass
        offset = index + len(marker)

    def fallback_string(key: str) -> str:
        pattern = re.compile(rf'"{re.escape(key)}"\s*:\s*"((?:\\.|[^"\\])*)"')
        match = pattern.search(html)
        if not match:
            return ""
        try:
            return json.loads(f'"{match.group(1)}"')
        except json.JSONDecodeError:
            return ""

    for key in (
        "INNERTUBE_API_KEY",
        "INNERTUBE_CLIENT_VERSION",
        "VISITOR_DATA",
        "PLAYER_JS_URL",
    ):
        if not merged.get(key):
            value = fallback_string(key)
            if value:
                merged[key] = value
    return merged


def _extract_initial_player_response(html: str) -> dict[str, Any] | None:
    for marker in (
        "ytInitialPlayerResponse =",
        "var ytInitialPlayerResponse =",
        'window["ytInitialPlayerResponse"] =',
    ):
        index = html.find(marker)
        if index >= 0:
            try:
                return _extract_balanced_json(html, index + len(marker))
            except CanaryError:
                continue
    return None


def _resolve_player_js_url(html: str, ytcfg: dict[str, Any]) -> str:
    raw = str(ytcfg.get("PLAYER_JS_URL") or "").strip()
    if not raw:
        patterns = (
            r'"jsUrl"\s*:\s*"((?:\\.|[^"\\])*)"',
            r'"js"\s*:\s*"((?:\\.|[^"\\])*?/base\.js)"',
        )
        for pattern in patterns:
            match = re.search(pattern, html)
            if match:
                try:
                    raw = json.loads(f'"{match.group(1)}"')
                except json.JSONDecodeError:
                    raw = match.group(1).replace("\\/", "/")
                break
    if not raw:
        raise CanaryError("player JS URL not found")
    return urllib.parse.urljoin("https://www.youtube.com", raw)


def _extract_signature_timestamp(player_js: str) -> int | None:
    patterns = (
        r"signatureTimestamp\s*[:=]\s*(\d{3,})",
        r"\bsts\s*[:=]\s*(\d{3,})",
    )
    for pattern in patterns:
        match = re.search(pattern, player_js)
        if match:
            return int(match.group(1))
    return None


def _player_api_request(
    *,
    video_id: str,
    innertube_query_value: str,
    client_version: str,
    visitor_data: str,
    hl: str,
    gl: str,
    client: dict[str, Any] | None = None,
    user_agent: str = USER_AGENT,
) -> dict[str, Any]:
    if not innertube_query_value or not client_version:
        raise CanaryError("watch page did not expose InnerTube API key/client version")
    context_client: dict[str, Any] = dict(client or {"clientName": "WEB"})
    context_client.update(
        {
            "clientVersion": client_version,
            "hl": hl,
            "gl": gl,
            "utcOffsetMinutes": 0,
        }
    )
    body: dict[str, Any] = {
        "context": {"client": context_client},
        "videoId": video_id,
        "contentCheckOk": True,
        "racyCheckOk": True,
    }
    if visitor_data:
        body["context"]["client"]["visitorData"] = visitor_data

    endpoint = (
        "https://www.youtube.com/youtubei/v1/player?"
        + urllib.parse.urlencode({"key": innertube_query_value, "prettyPrint": "false"})
    )
    result = _bounded_request(
        endpoint,
        data=json.dumps(body, separators=(",", ":")).encode("utf-8"),
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Origin": "https://www.youtube.com",
            "Referer": f"https://www.youtube.com/watch?v={video_id}",
            "User-Agent": user_agent,
            "X-YouTube-Client-Name": "1",
            "X-YouTube-Client-Version": client_version,
        },
        max_bytes=PLAYER_JSON_MAX_BYTES,
    )
    if result.status < 200 or result.status >= 300:
        raise CanaryError(f"player endpoint HTTP {result.status}", status=result.status)
    try:
        parsed = json.loads(result.body.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise CanaryError(f"invalid player JSON: {error}") from error
    if not isinstance(parsed, dict):
        raise CanaryError("player response is not an object")
    return parsed


def _format_url_metadata(format_json: dict[str, Any]) -> tuple[str, bool, bool, bool]:
    direct_url = str(format_json.get("url") or "")
    cipher_text = str(format_json.get("signatureCipher") or format_json.get("cipher") or "")
    candidate_url = direct_url
    if not candidate_url and cipher_text:
        cipher = urllib.parse.parse_qs(cipher_text, keep_blank_values=True)
        candidate_url = (cipher.get("url") or [""])[0]
    host = ""
    has_n = False
    if candidate_url:
        try:
            parsed = urllib.parse.urlsplit(candidate_url)
            host = (parsed.hostname or "").lower()
            has_n = "n" in urllib.parse.parse_qs(parsed.query, keep_blank_values=True)
        except ValueError:
            pass
    return host, bool(direct_url), bool(cipher_text), has_n


def _summarize_player_response(player: dict[str, Any]) -> dict[str, Any]:
    playability = player.get("playabilityStatus")
    if not isinstance(playability, dict):
        playability = {}
    streaming = player.get("streamingData")
    if not isinstance(streaming, dict):
        streaming = {}

    formats_raw = streaming.get("formats")
    adaptive_raw = streaming.get("adaptiveFormats")
    formats = [item for item in formats_raw if isinstance(item, dict)] if isinstance(formats_raw, list) else []
    adaptive = [item for item in adaptive_raw if isinstance(item, dict)] if isinstance(adaptive_raw, list) else []
    all_formats = formats + adaptive

    direct_count = 0
    cipher_count = 0
    n_count = 0
    googlevideo_count = 0
    muxed_video_count = 0
    adaptive_video_count = 0
    adaptive_audio_count = 0
    max_adaptive_video_height = 0
    direct_probe_url = ""
    for item in all_formats:
        host, is_direct, is_cipher, has_n = _format_url_metadata(item)
        direct_count += int(is_direct)
        cipher_count += int(is_cipher)
        n_count += int(has_n)
        if host == "googlevideo.com" or host.endswith(".googlevideo.com"):
            googlevideo_count += 1
            if is_direct and not direct_probe_url:
                direct_probe_url = str(item.get("url") or "")

    for item in formats:
        if str(item.get("mimeType") or "").lower().startswith("video/"):
            muxed_video_count += 1
    for item in adaptive:
        mime_type = str(item.get("mimeType") or "").lower()
        if mime_type.startswith("video/"):
            adaptive_video_count += 1
            try:
                max_adaptive_video_height = max(
                    max_adaptive_video_height, int(item.get("height") or 0)
                )
            except (TypeError, ValueError):
                pass
        elif mime_type.startswith("audio/"):
            adaptive_audio_count += 1

    player_config = player.get("playerConfig")
    media_common = player_config.get("mediaCommonConfig") if isinstance(player_config, dict) else None
    ustreamer = media_common.get("mediaUstreamerRequestConfig") if isinstance(media_common, dict) else None
    ustreamer_config = ""
    use_ump = False
    if isinstance(ustreamer, dict):
        ustreamer_config = str(ustreamer.get("videoPlaybackUstreamerConfig") or "")
        use_ump = bool(ustreamer.get("videoPlaybackUseUmp"))

    sabr_url = str(streaming.get("serverAbrStreamingUrl") or "")
    sabr_networks = _media_network_count(sabr_url)
    direct_networks = _media_network_count(direct_probe_url)

    return {
        "playability_status": str(playability.get("status") or ""),
        "playability_reason": str(playability.get("reason") or "")[:240],
        "has_streaming_data": bool(streaming),
        "streaming_keys": sorted(str(key) for key in streaming.keys()),
        "has_server_abr_streaming_url": bool(sabr_url),
        "server_abr_host_is_googlevideo": _is_googlevideo(sabr_url),
        "server_abr_media_networks": sabr_networks,
        "has_ustreamer_config": bool(ustreamer_config),
        "ustreamer_config_bytes": len(ustreamer_config),
        "video_playback_use_ump": use_ump,
        "direct_media_networks": direct_networks,
        "formats": len(formats),
        "adaptive_formats": len(adaptive),
        "total_formats": len(all_formats),
        "muxed_video_formats": muxed_video_count,
        "adaptive_video_formats": adaptive_video_count,
        "adaptive_audio_formats": adaptive_audio_count,
        "max_adaptive_video_height": max_adaptive_video_height,
        "direct_urls": direct_count,
        "cipher_urls": cipher_count,
        "n_parameter_urls": n_count,
        "googlevideo_candidates": googlevideo_count,
        "has_hls_manifest": bool(streaming.get("hlsManifestUrl")),
        "has_dash_manifest": bool(streaming.get("dashManifestUrl")),
        "top_level_keys": sorted(str(key) for key in player.keys()),
        "_probe_url": direct_probe_url,
    }


def _is_googlevideo(url: str) -> bool:
    if not url:
        return False
    try:
        host = (urllib.parse.urlsplit(url).hostname or "").lower()
    except ValueError:
        return False
    return host == "googlevideo.com" or host.endswith(".googlevideo.com")


def _media_network_count(url: str) -> int:
    """Number of equivalent Googlevideo media networks the signed URL advertises. Host names and
    signed parameters are never recorded, only how many alternatives exist."""
    if not url:
        return 0
    try:
        query = urllib.parse.parse_qs(urllib.parse.urlsplit(url).query, keep_blank_values=False)
    except ValueError:
        return 0
    declared = (query.get("mn") or [""])[0]
    return len({item for item in declared.split(",") if item.strip()})


def _classify_delivery(summary: dict[str, Any]) -> str:
    """Where playback for one client currently stands. The canary is a sensor: it reports what the
    protocol offers an unauthenticated prober, never what Levyra can reach with its own tokens."""
    status = str(summary.get("playability_status") or "")
    reason = str(summary.get("playability_reason") or "").lower()
    if status in ("LOGIN_REQUIRED", "AGE_VERIFICATION_REQUIRED") or "bot" in reason or "sign in" in reason:
        return DELIVERY_SECURITY_FAILURE
    if status and status != "OK":
        return DELIVERY_CLIENT_FAILURE
    if not summary.get("has_streaming_data"):
        return DELIVERY_CLIENT_FAILURE

    direct = int(summary.get("direct_urls") or 0)
    cipher = int(summary.get("cipher_urls") or 0)
    adaptive_audio = int(summary.get("adaptive_audio_formats") or 0)
    adaptive_video = int(summary.get("adaptive_video_formats") or 0)
    sabr = bool(summary.get("has_server_abr_streaming_url"))

    if direct == 0 and cipher == 0:
        return DELIVERY_SABR_ONLY if sabr else DELIVERY_DIRECT_UNAVAILABLE
    if adaptive_audio == 0 or adaptive_video == 0:
        return DELIVERY_DIRECT_DEGRADED
    return DELIVERY_DIRECT_HEALTHY


def _probe_client(
    entry: dict[str, Any],
    *,
    video_id: str,
    innertube_query_value: str,
    web_client_version: str,
    visitor_data: str,
    hl: str,
    gl: str,
) -> dict[str, Any]:
    name = str(entry.get("name") or "")
    client = dict(entry.get("client") or {})
    if not client.get("clientVersion"):
        client["clientVersion"] = web_client_version
    started = time.monotonic()
    try:
        player = _player_api_request(
            video_id=video_id,
            innertube_query_value=innertube_query_value,
            client_version=str(client.get("clientVersion") or ""),
            visitor_data=visitor_data,
            hl=hl,
            gl=gl,
            client=client,
            user_agent=str(entry.get("user_agent") or USER_AGENT),
        )
    except CanaryError as error:
        status = getattr(error, "status", None)
        if status is None:
            delivery = DELIVERY_TRANSPORT_FAILURE
        elif status in (401, 403):
            delivery = DELIVERY_SECURITY_FAILURE
        else:
            delivery = DELIVERY_CLIENT_FAILURE
        return {
            "client": name,
            "latency_ms": int((time.monotonic() - started) * 1000),
            "delivery": delivery,
            "error": str(error)[:240],
        }
    summary = _summarize_player_response(player)
    summary.pop("_probe_url", "")
    summary.pop("top_level_keys", None)
    summary.pop("streaming_keys", None)
    return {
        "client": name,
        "latency_ms": int((time.monotonic() - started) * 1000),
        "delivery": _classify_delivery(summary),
        "player": summary,
    }


def _probe_client_matrix(
    *,
    video_id: str,
    innertube_query_value: str,
    web_client_version: str,
    visitor_data: str,
    hl: str,
    gl: str,
) -> list[dict[str, Any]]:
    if not innertube_query_value:
        return []
    return [
        _probe_client(
            entry,
            video_id=video_id,
            innertube_query_value=innertube_query_value,
            web_client_version=web_client_version,
            visitor_data=visitor_data,
            hl=hl,
            gl=gl,
        )
        for entry in LEVYRA_CLIENT_MATRIX
    ]


def _probe_media_url(url: str) -> dict[str, Any]:
    if not url:
        return {"attempted": False, "initial_ok": False, "continuation_ok": False}
    try:
        host = _safe_host(url, media=True)
    except CanaryError as error:
        return {
            "attempted": False,
            "initial_ok": False,
            "continuation_ok": False,
            "error": str(error),
        }

    def one_range(start: int) -> tuple[bool, int, str, str]:
        request = urllib.request.Request(
            url,
            headers={
                "Range": f"bytes={start}-{start + 1023}",
                "User-Agent": USER_AGENT,
                "Referer": "https://www.youtube.com/",
            },
        )
        try:
            opener = urllib.request.build_opener(_SafeRedirectHandler(media=True))
            with opener.open(request, timeout=DEFAULT_TIMEOUT_SECONDS) as response:
                body = response.read(MEDIA_PROBE_MAX_BYTES + 1)
                status = getattr(response, "status", response.getcode())
                content_type = response.headers.get("Content-Type", "")
                content_range = response.headers.get("Content-Range", "")
                ok = status in (200, 206) and 0 < len(body) <= MEDIA_PROBE_MAX_BYTES
                return ok, status, content_type[:120], content_range[:160]
        except urllib.error.HTTPError as error:
            return False, error.code, error.headers.get("Content-Type", "")[:120], ""
        except (urllib.error.URLError, TimeoutError, OSError):
            return False, 0, "", ""

    initial_ok, initial_status, content_type, content_range = one_range(0)
    continuation_ok = False
    continuation_status = 0
    if initial_ok:
        continuation_ok, continuation_status, _, _ = one_range(65536)
    return {
        "attempted": True,
        "host": host,
        "initial_ok": initial_ok,
        "initial_status": initial_status,
        "continuation_ok": continuation_ok,
        "continuation_status": continuation_status,
        "content_type": content_type,
        "content_range": content_range,
    }


def _probe_sentinel(
    sentinel: dict[str, Any],
    *,
    hl: str,
    gl: str,
    attempts: int,
    retry_delay_seconds: float,
    private_evidence_dir: Path | None = None,
) -> dict[str, Any]:
    video_id = str(sentinel.get("video_id") or "")
    if not VIDEO_ID_RE.fullmatch(video_id):
        raise CanaryError(f"invalid configured video id: {video_id!r}")

    result: dict[str, Any] = {
        "name": str(sentinel.get("name") or video_id),
        "video_id": video_id,
        "required": bool(sentinel.get("required", True)),
        "attempts": [],
    }

    matrix_inputs: dict[str, str] | None = None
    for attempt in range(1, max(1, attempts) + 1):
        attempt_result: dict[str, Any] = {"attempt": attempt}
        try:
            watch_url = "https://www.youtube.com/watch?" + urllib.parse.urlencode(
                {
                    "v": video_id,
                    "hl": hl,
                    "gl": gl,
                    "bpctr": "9999999999",
                    "has_verified": "1",
                }
            )
            watch = _bounded_request(
                watch_url,
                headers={"Accept": "text/html", "User-Agent": USER_AGENT},
                max_bytes=WATCH_MAX_BYTES,
            )
            if watch.status < 200 or watch.status >= 300:
                raise CanaryError(f"watch page HTTP {watch.status}")
            html = watch.body.decode("utf-8", errors="replace")
            ytcfg = _extract_ytcfg(html)
            initial = _extract_initial_player_response(html)
            js_url = _resolve_player_js_url(html, ytcfg)
            js_result = _bounded_request(
                js_url,
                headers={"Accept": "*/*", "User-Agent": USER_AGENT},
                max_bytes=PLAYER_JS_MAX_BYTES,
            )
            if js_result.status < 200 or js_result.status >= 300:
                raise CanaryError(f"player JS HTTP {js_result.status}")
            js_text = js_result.body.decode("utf-8", errors="replace")
            js_sha256 = hashlib.sha256(js_result.body).hexdigest()
            if private_evidence_dir is not None:
                private_evidence_dir.mkdir(parents=True, exist_ok=True)
                private_js = private_evidence_dir / f"player-{js_sha256[:16]}.js"
                if not private_js.exists():
                    private_js.write_bytes(js_result.body)
            matrix_inputs = {
                "innertube_query_value": str(ytcfg.get("INNERTUBE_API_KEY") or ""),
                "web_client_version": str(ytcfg.get("INNERTUBE_CLIENT_VERSION") or ""),
                "visitor_data": str(ytcfg.get("VISITOR_DATA") or ""),
            }
            player = _player_api_request(
                video_id=video_id,
                innertube_query_value=matrix_inputs["innertube_query_value"],
                client_version=matrix_inputs["web_client_version"],
                visitor_data=matrix_inputs["visitor_data"],
                hl=hl,
                gl=gl,
            )
            summary = _summarize_player_response(player)
            probe_url = summary.pop("_probe_url", "")
            media = _probe_media_url(probe_url)
            initial_status = ""
            if isinstance(initial, dict):
                initial_playability = initial.get("playabilityStatus")
                if isinstance(initial_playability, dict):
                    initial_status = str(initial_playability.get("status") or "")
            expects_adaptive_video = bool(sentinel.get("expect_adaptive_video", False))
            adaptive_video_ok = (
                not expects_adaptive_video or int(summary.get("adaptive_video_formats") or 0) > 0
            )
            attempt_result.update(
                {
                    "ok": (
                        summary["playability_status"] == "OK"
                        and summary["has_streaming_data"]
                        and adaptive_video_ok
                    ),
                    "adaptive_video_ok": adaptive_video_ok,
                    "player_js": {
                        "host": _safe_host(js_url),
                        "sha256": js_sha256,
                        "bytes": len(js_result.body),
                        "signature_timestamp": _extract_signature_timestamp(js_text),
                    },
                    "web_client_version": str(ytcfg.get("INNERTUBE_CLIENT_VERSION") or ""),
                    "watch_initial_playability_status": initial_status,
                    "player": summary,
                    "media_probe": media,
                }
            )
        except CanaryError as error:
            attempt_result.update({"ok": False, "error": str(error)[:400]})
        result["attempts"].append(attempt_result)
        if attempt_result.get("ok"):
            break
        if attempt < attempts:
            time.sleep(max(0.0, retry_delay_seconds))

    successful = next((item for item in result["attempts"] if item.get("ok")), None)
    chosen = successful or result["attempts"][-1]
    clients = (
        _probe_client_matrix(
            video_id=video_id,
            hl=hl,
            gl=gl,
            **matrix_inputs,
        )
        if matrix_inputs
        else []
    )
    chosen["clients"] = clients
    chosen["delivery_summary"] = _summarize_delivery(clients)
    result["observation"] = chosen
    result["ok"] = bool(chosen.get("ok"))
    return result


def _summarize_delivery(clients: list[dict[str, Any]]) -> dict[str, Any]:
    deliveries = [str(item.get("delivery") or "") for item in clients]
    direct_capable = [
        item
        for item in clients
        if item.get("delivery") in (DELIVERY_DIRECT_HEALTHY, DELIVERY_DIRECT_DEGRADED)
    ]
    sabr_capable = [
        item
        for item in clients
        if isinstance(item.get("player"), dict)
        and item["player"].get("has_server_abr_streaming_url")
    ]
    playable = [
        item
        for item in clients
        if item.get("delivery")
        in (DELIVERY_DIRECT_HEALTHY, DELIVERY_DIRECT_DEGRADED, DELIVERY_SABR_ONLY)
    ]
    return {
        "clients_probed": len(clients),
        "clients_playable": len(playable),
        "clients_direct_capable": len(direct_capable),
        "clients_sabr_capable": len(sabr_capable),
        "clients_sabr_only": deliveries.count(DELIVERY_SABR_ONLY),
        "clients_security_failure": deliveries.count(DELIVERY_SECURITY_FAILURE),
        "clients_client_failure": deliveries.count(DELIVERY_CLIENT_FAILURE),
        "clients_transport_failure": deliveries.count(DELIVERY_TRANSPORT_FAILURE),
        "sabr_enforced": bool(playable) and not direct_capable and bool(sabr_capable),
        "by_client": {
            str(item.get("client") or ""): str(item.get("delivery") or "") for item in clients
        },
    }


def _fetch_upstream(repo: str, branch: str) -> dict[str, Any]:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        return {"repo": repo, "branch": branch, "error": "invalid repository name"}
    endpoint = (
        f"https://api.github.com/repos/{repo}/commits?"
        + urllib.parse.urlencode({"sha": branch, "per_page": "8"})
    )
    try:
        response = _bounded_request(
            endpoint,
            headers={"Accept": "application/vnd.github+json", "User-Agent": "Levyra-YouTube-Canary"},
            max_bytes=GITHUB_JSON_MAX_BYTES,
        )
        if response.status < 200 or response.status >= 300:
            return {"repo": repo, "branch": branch, "error": f"HTTP {response.status}"}
        payload = json.loads(response.body.decode("utf-8"))
        commits = []
        for entry in payload if isinstance(payload, list) else []:
            if not isinstance(entry, dict):
                continue
            commit = entry.get("commit")
            if not isinstance(commit, dict):
                continue
            message = str(commit.get("message") or "").splitlines()[0][:240]
            if not any(keyword in message.lower() for keyword in KEYWORDS):
                continue
            author = commit.get("author") if isinstance(commit.get("author"), dict) else {}
            commits.append(
                {
                    "sha": str(entry.get("sha") or "")[:40],
                    "message": message,
                    "date": str(author.get("date") or ""),
                    "url": str(entry.get("html_url") or ""),
                }
            )
        return {"repo": repo, "branch": branch, "relevant_commits": commits[:5]}
    except (CanaryError, json.JSONDecodeError) as error:
        return {"repo": repo, "branch": branch, "error": str(error)[:300]}


def _current_observation(config: dict[str, Any], private_evidence_dir: Path | None = None) -> dict[str, Any]:
    sentinels = config.get("sentinels")
    if not isinstance(sentinels, list) or not sentinels:
        raise CanaryError("canary config requires at least one sentinel")
    youtube = config.get("youtube") if isinstance(config.get("youtube"), dict) else {}
    hl = str(youtube.get("hl") or "en")
    gl = str(youtube.get("gl") or "US").upper()
    attempts = int(config.get("probe_attempts") or 2)
    retry_delay = float(config.get("retry_delay_seconds") or 2.0)

    observed = [
        _probe_sentinel(
            item,
            hl=hl,
            gl=gl,
            attempts=attempts,
            retry_delay_seconds=retry_delay,
            private_evidence_dir=private_evidence_dir,
        )
        for item in sentinels
        if isinstance(item, dict)
    ]
    upstreams_config = config.get("upstreams")
    upstreams = []
    if isinstance(upstreams_config, list):
        for item in upstreams_config:
            if isinstance(item, dict):
                upstreams.append(
                    _fetch_upstream(str(item.get("repo") or ""), str(item.get("branch") or "main"))
                )
    return {
        "schema": SCHEMA_VERSION,
        "observed_at_epoch": int(time.time()),
        "youtube": {"hl": hl, "gl": gl},
        "sentinels": observed,
        "upstreams": upstreams,
    }


def _sentinel_map(observation: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result = {}
    sentinels = observation.get("sentinels")
    if isinstance(sentinels, list):
        for item in sentinels:
            if isinstance(item, dict):
                result[str(item.get("name") or item.get("video_id") or "")] = item
    return result


def _sentinel_access_blocked(sentinel: dict[str, Any]) -> bool:
    chosen = sentinel.get("observation") if isinstance(sentinel.get("observation"), dict) else {}
    if isinstance(chosen.get("player"), dict):
        return False
    error = str(chosen.get("error") or "").lower()
    if not error:
        return False
    blocked_markers = (
        "request failed:",
        "watch page http 403",
        "watch page http 429",
        "watch page http 5",
        "player js http 403",
        "player js http 429",
        "player js http 5",
        "player endpoint http 403",
        "player endpoint http 429",
        "player endpoint http 5",
    )
    return any(marker in error for marker in blocked_markers)


def _delivery_evidence_is_conclusive(delivery: dict[str, Any]) -> bool:
    """A client matrix where every probe failed on our side says nothing about YouTube."""
    if not delivery:
        return False
    probed = int(delivery.get("clients_probed") or 0)
    if probed <= 0:
        return False
    failed = int(delivery.get("clients_transport_failure") or 0) + int(
        delivery.get("clients_client_failure") or 0
    )
    return failed < probed


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
                f"Accepted baseline schema {baseline_schema!r} is incompatible with current schema "
                f"{SCHEMA_VERSION}; accept a fresh observation before comparison."
            ],
        }
    if not isinstance(accepted, dict):
        required = [
            item for item in observation.get("sentinels", [])
            if isinstance(item, dict) and bool(item.get("required", True))
        ]
        failed_required = [str(item.get("name") or item.get("video_id") or "") for item in required if not item.get("ok")]
        if failed_required:
            return {
                "decision": "blocked",
                "severity": "warning",
                "material_changes": [],
                "informational_changes": [
                    "Baseline not seeded because required live probes failed: " + ", ".join(failed_required)
                ],
            }
        return {
            "decision": "bootstrap",
            "severity": "info",
            "material_changes": ["No accepted baseline exists yet."],
            "informational_changes": [],
        }

    before = _sentinel_map(accepted)
    after = _sentinel_map(observation)
    required_now = [item for item in after.values() if bool(item.get("required", True))]
    failed_required = [item for item in required_now if not item.get("ok")]
    if failed_required and len(failed_required) == len(required_now) and all(
        _sentinel_access_blocked(item) for item in failed_required
    ):
        return {
            "decision": "blocked",
            "severity": "warning",
            "material_changes": [],
            "informational_changes": [
                "All required probes are blocked at the network/access layer; no repair is attempted."
            ],
        }

    material: list[str] = []
    info: list[str] = []
    material_sentinels: set[str] = set()
    range_regressions = 0

    for name, current in after.items():
        previous = before.get(name)
        if not previous:
            info.append(f"{name}: new sentinel")
            continue

        old_obs = previous.get("observation") if isinstance(previous.get("observation"), dict) else {}
        new_obs = current.get("observation") if isinstance(current.get("observation"), dict) else {}
        old_player = old_obs.get("player") if isinstance(old_obs.get("player"), dict) else {}
        new_player = new_obs.get("player") if isinstance(new_obs.get("player"), dict) else {}
        required = bool(current.get("required", True))
        access_blocked = _sentinel_access_blocked(current)

        if required and bool(previous.get("ok")) and not bool(current.get("ok")):
            if access_blocked:
                info.append(f"{name}: live probe is blocked at the network/access layer")
            else:
                material.append(f"{name}: required sentinel stopped resolving")
                material_sentinels.add(name)
        if not access_blocked and (
            old_player.get("playability_status") == "OK"
            and new_player.get("playability_status") != "OK"
        ):
            material.append(
                f"{name}: playability {old_player.get('playability_status')} -> "
                f"{new_player.get('playability_status') or '<missing>'}"
            )
            material_sentinels.add(name)
        if not access_blocked and bool(old_player.get("has_streaming_data")) and not bool(new_player.get("has_streaming_data")):
            material.append(f"{name}: streamingData disappeared")
            material_sentinels.add(name)
        if not access_blocked and int(old_player.get("total_formats") or 0) > 0 and int(new_player.get("total_formats") or 0) == 0:
            material.append(f"{name}: all formats disappeared")
            material_sentinels.add(name)
        if (
            not access_blocked
            and int(old_player.get("adaptive_video_formats") or 0) > 0
            and int(new_player.get("adaptive_video_formats") or 0) == 0
            and int(new_player.get("muxed_video_formats") or 0) > 0
        ):
            material.append(
                f"{name}: adaptive video ladder disappeared while muxed video remains "
                "(360p-cap risk)"
            )
            material_sentinels.add(name)

        old_delivery = old_obs.get("delivery_summary") if isinstance(old_obs.get("delivery_summary"), dict) else {}
        new_delivery = new_obs.get("delivery_summary") if isinstance(new_obs.get("delivery_summary"), dict) else {}
        if not access_blocked and _delivery_evidence_is_conclusive(new_delivery):
            if int(new_delivery.get("clients_playable") or 0) == 0 and int(
                old_delivery.get("clients_playable") or 0
            ) > 0:
                material.append(f"{name}: no probed client returns a playable delivery method")
                material_sentinels.add(name)
            elif not bool(old_delivery.get("sabr_enforced")) and bool(new_delivery.get("sabr_enforced")):
                material.append(
                    f"{name}: every playable client now exposes SABR only; direct delivery disappeared"
                )
                material_sentinels.add(name)
            elif int(old_delivery.get("clients_direct_capable") or 0) > int(
                new_delivery.get("clients_direct_capable") or 0
            ):
                info.append(
                    f"{name}: clients with direct delivery "
                    f"{old_delivery.get('clients_direct_capable')} -> "
                    f"{new_delivery.get('clients_direct_capable')}"
                )
            old_by_client = old_delivery.get("by_client") or {}
            new_by_client = new_delivery.get("by_client") or {}
            for client_name, new_state in sorted(new_by_client.items()):
                old_state = old_by_client.get(client_name)
                if old_state and old_state != new_state:
                    info.append(f"{name}: client {client_name} {old_state} -> {new_state}")

        old_media = old_obs.get("media_probe") if isinstance(old_obs.get("media_probe"), dict) else {}
        new_media = new_obs.get("media_probe") if isinstance(new_obs.get("media_probe"), dict) else {}
        if old_media.get("initial_ok") is True and new_media.get("initial_ok") is False:
            range_regressions += 1
            material.append(f"{name}: initial media Range probe regressed")
        if old_media.get("continuation_ok") is True and new_media.get("continuation_ok") is False:
            range_regressions += 1
            material.append(f"{name}: continuation Range probe regressed")

        old_js = old_obs.get("player_js") if isinstance(old_obs.get("player_js"), dict) else {}
        new_js = new_obs.get("player_js") if isinstance(new_obs.get("player_js"), dict) else {}
        if old_js.get("sha256") and new_js.get("sha256") and old_js.get("sha256") != new_js.get("sha256"):
            info.append(f"{name}: player JS hash changed")
        if (
            old_obs.get("web_client_version")
            and new_obs.get("web_client_version")
            and old_obs.get("web_client_version") != new_obs.get("web_client_version")
        ):
            info.append(
                f"{name}: WEB client version "
                f"{old_obs.get('web_client_version')} -> {new_obs.get('web_client_version')}"
            )
        old_keys = set(old_player.get("streaming_keys") or [])
        new_keys = set(new_player.get("streaming_keys") or [])
        disappeared = sorted(old_keys - new_keys)
        if disappeared:
            info.append(f"{name}: streaming keys disappeared: {', '.join(disappeared)}")

    thresholds = config.get("thresholds") if isinstance(config.get("thresholds"), dict) else {}
    sentinel_threshold = int(thresholds.get("required_sentinel_regressions_for_repair") or 2)
    if material_sentinels and len(material_sentinels) < sentinel_threshold:
        downgraded = [entry for entry in material if not "Range probe regressed" in entry]
        material = [entry for entry in material if "Range probe regressed" in entry]
        info.extend(downgraded)
        info.append(
            f"Semantic regression affected {len(material_sentinels)} sentinel(s), below repair threshold {sentinel_threshold}"
        )

    range_threshold = int(thresholds.get("range_regressions_for_repair") or 2)
    if range_regressions and range_regressions < range_threshold:
        material = [entry for entry in material if "Range probe regressed" not in entry]
        info.append(f"Range probe regression count {range_regressions} below repair threshold {range_threshold}")

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
        "youtube": observation.get("youtube"),
        "sentinels": observation.get("sentinels"),
        "upstreams": observation.get("upstreams"),
    }
    return json.loads(json.dumps(safe))


def _render_report(observation: dict[str, Any], decision: dict[str, Any]) -> str:
    lines = [
        "# Levyra YouTube Canary",
        "",
        f"- Decision: **{decision['decision']}**",
        f"- Severity: **{decision['severity']}**",
        "",
        "## Material changes",
    ]
    material = decision.get("material_changes") or []
    lines.extend([f"- {item}" for item in material] or ["- None"])
    lines += ["", "## Informational changes"]
    info = decision.get("informational_changes") or []
    lines.extend([f"- {item}" for item in info] or ["- None"])
    lines += ["", "## Sentinel status", ""]
    lines.append("| Sentinel | Required | Resolve | Playability | Formats | Cipher | n | Range 0 | Range 64KiB |")
    lines.append("|---|---:|---:|---|---:|---:|---:|---:|---:|")
    for sentinel in observation.get("sentinels") or []:
        if not isinstance(sentinel, dict):
            continue
        chosen = sentinel.get("observation") if isinstance(sentinel.get("observation"), dict) else {}
        player = chosen.get("player") if isinstance(chosen.get("player"), dict) else {}
        media = chosen.get("media_probe") if isinstance(chosen.get("media_probe"), dict) else {}
        lines.append(
            "| {name} | {required} | {ok} | {status} | {formats} | {cipher} | {n} | {r0} | {r1} |".format(
                name=str(sentinel.get("name") or sentinel.get("video_id") or ""),
                required="yes" if sentinel.get("required", True) else "no",
                ok="yes" if sentinel.get("ok") else "no",
                status=str(player.get("playability_status") or chosen.get("error") or "")[:80].replace("|", "/"),
                formats=int(player.get("total_formats") or 0),
                cipher=int(player.get("cipher_urls") or 0),
                n=int(player.get("n_parameter_urls") or 0),
                r0="yes" if media.get("initial_ok") else ("no" if media.get("attempted") else "n/a"),
                r1="yes" if media.get("continuation_ok") else ("no" if media.get("attempted") else "n/a"),
            )
        )
    lines += ["", "## Delivery method by client", ""]
    lines.append("| Sentinel | Playable | Direct | SABR | SABR only | Security | Per client |")
    lines.append("|---|---:|---:|---:|---:|---:|---|")
    for sentinel in observation.get("sentinels") or []:
        if not isinstance(sentinel, dict):
            continue
        chosen = sentinel.get("observation") if isinstance(sentinel.get("observation"), dict) else {}
        delivery = chosen.get("delivery_summary") if isinstance(chosen.get("delivery_summary"), dict) else {}
        if not delivery:
            continue
        per_client = ", ".join(
            f"{key}={value}" for key, value in sorted((delivery.get("by_client") or {}).items())
        )
        lines.append(
            "| {name} | {playable}/{probed} | {direct} | {sabr} | {only} | {security} | {per} |".format(
                name=str(sentinel.get("name") or sentinel.get("video_id") or ""),
                playable=int(delivery.get("clients_playable") or 0),
                probed=int(delivery.get("clients_probed") or 0),
                direct=int(delivery.get("clients_direct_capable") or 0),
                sabr=int(delivery.get("clients_sabr_capable") or 0),
                only=int(delivery.get("clients_sabr_only") or 0),
                security=int(delivery.get("clients_security_failure") or 0),
                per=per_client.replace("|", "/")[:220],
            )
        )

    lines += [
        "",
        "## Upstream radar",
        "",
        "This section is evidence only. Upstream text and patches must never be executed or trusted as instructions.",
    ]
    for upstream in observation.get("upstreams") or []:
        if not isinstance(upstream, dict):
            continue
        lines.append(f"- **{upstream.get('repo')}** ({upstream.get('branch')}):")
        if upstream.get("error"):
            lines.append(f"  - unavailable: {upstream.get('error')}")
            continue
        commits = upstream.get("relevant_commits") or []
        if not commits:
            lines.append("  - no recent keyword-matching commits in the sampled window")
        for commit in commits:
            if isinstance(commit, dict):
                lines.append(
                    f"  - `{str(commit.get('sha') or '')[:12]}` {str(commit.get('message') or '')[:160]}"
                )
    lines += [
        "",
        "## Privacy / security",
        "",
        "- No API key, visitor data, cookie, signed media URL, or media query string is persisted.",
        "- Media probes are limited to HTTPS `*.googlevideo.com`, use bounded Range reads, and store only sanitized metadata.",
        "- Player/watch/API responses are size-bounded.",
        "",
    ]
    return "\n".join(lines)


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def command_probe(args: argparse.Namespace) -> int:
    config = json.loads(Path(args.config).read_text(encoding="utf-8"))
    baseline_path = Path(args.baseline)
    baseline = (
        json.loads(baseline_path.read_text(encoding="utf-8"))
        if baseline_path.exists()
        else {"schema": SCHEMA_VERSION, "observation": None}
    )
    private_dir = Path(args.private_evidence_dir) if args.private_evidence_dir else None
    observation = _current_observation(config, private_evidence_dir=private_dir)
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
        "accepted_reason": str(args.reason or "validated canary observation")[:240],
        "observation": _sanitize_for_baseline(observation),
    }
    _write_json(Path(args.baseline), baseline)
    return 0


def command_incident(args: argparse.Namespace) -> int:
    report = Path(args.report).read_text(encoding="utf-8")
    destination = Path(args.destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        "<!-- Auto-generated by Levyra YouTube Canary. Contains sanitized metadata only. -->\n\n"
        + report,
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
    probe.add_argument("--private-evidence-dir", default="")
    probe.set_defaults(func=command_probe)

    accept = subparsers.add_parser("accept")
    accept.add_argument("--observation", required=True)
    accept.add_argument("--baseline", required=True)
    accept.add_argument("--reason", default="")
    accept.set_defaults(func=command_accept)

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
    except (CanaryError, OSError, ValueError, json.JSONDecodeError) as error:
        print(f"YouTube canary failed: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
