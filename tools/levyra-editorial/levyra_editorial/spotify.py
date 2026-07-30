from __future__ import annotations

import base64
import hashlib
import hmac
import json
import logging
import os
import re
import struct
import time
from collections.abc import Mapping, Sequence
from email.utils import parsedate_to_datetime
from typing import Any
from urllib.parse import urlparse

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

LOGGER = logging.getLogger(__name__)

OPEN_SPOTIFY_URL = "https://open.spotify.com/"
SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
TOKEN_URL = "https://open.spotify.com/api/token"
API_BASE_URL = "https://api.spotify.com/v1"
PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v1/query"

DEFAULT_SECRET_DICT_URL = (
    "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/"
    "4cd9440671af3a419bad112164a193ea1374e0e1/secrets/secretDict.json"
)
DEFAULT_PLAYLIST_QUERY_HASH = (
    "a65e12194ed5fc443a1cdebed5fabe33ca5b07b987185d63c72483867ad13cb4"
)
DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/135.0.0.0 Safari/537.36"
)
SPOTIFY_APP_VERSION = "1.2.61.20.g3b4cd5b2"
TOKEN_ATTEMPTS = (
    ("mobile-web-player", "transport"),
    ("mobile-web-player", "init"),
    ("web-player", "transport"),
    ("web-player", "init"),
)


class EditorialSourceError(RuntimeError):
    """Base exception for editorial source failures."""


class AuthenticationError(EditorialSourceError):
    """Raised when a web-player session cannot be converted to a bearer token."""


class SourceApiError(EditorialSourceError):
    """Raised when the metadata source returns an unusable response."""


def normalize_sp_dc(raw_value: str) -> str:
    """Extract and validate the ``sp_dc`` value from a raw value or cookie string."""
    raw = raw_value.strip()
    if not raw:
        raise AuthenticationError("The editorial session secret is empty.")

    value = raw
    if "sp_dc=" in raw:
        for segment in raw.split(";"):
            name, separator, candidate = segment.strip().partition("=")
            if separator and name == "sp_dc":
                value = candidate.strip()
                break

    if not value or any(character.isspace() for character in value):
        raise AuthenticationError("The editorial session secret is malformed.")
    if len(value) < 20:
        raise AuthenticationError("The editorial session secret is unexpectedly short.")
    return value


def decode_totp_secret(cipher_bytes: list[int]) -> str:
    """Decode one versioned web-player TOTP cipher entry into a Base32 secret."""
    invalid = any(
        not isinstance(value, int) or value not in range(256)
        for value in cipher_bytes
    )
    if not cipher_bytes or invalid:
        raise AuthenticationError("The TOTP secret dictionary contains invalid bytes.")
    transformed = [
        value ^ ((index % 33) + 9)
        for index, value in enumerate(cipher_bytes)
    ]
    joined = "".join(str(value) for value in transformed).encode("ascii")
    return base64.b32encode(joined).decode("ascii").rstrip("=")


def generate_totp(
    secret: str,
    timestamp_seconds: int,
    *,
    digits: int = 6,
    interval: int = 30,
) -> str:
    """Generate an RFC 6238 SHA-1 time-based one-time password."""
    if digits not in range(6, 9) or interval <= 0:
        raise ValueError("Unsupported TOTP parameters.")
    padded = secret + ("=" * ((8 - len(secret) % 8) % 8))
    try:
        key = base64.b32decode(padded, casefold=True)
    except (ValueError, TypeError) as error:
        raise AuthenticationError("The decoded TOTP secret is invalid.") from error
    counter = int(timestamp_seconds) // interval
    digest = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    code = (
        struct.unpack(">I", digest[offset : offset + 4])[0] & 0x7FFFFFFF
    ) % (10**digits)
    return f"{code:0{digits}d}"


def select_latest_totp_secret(
    secret_dict: Mapping[str, Any],
) -> tuple[int, str]:
    """Select and decode the highest numeric TOTP secret version."""
    candidates: list[tuple[int, list[int]]] = []
    for raw_version, raw_secret in secret_dict.items():
        try:
            version = int(raw_version)
        except (TypeError, ValueError):
            continue
        if isinstance(raw_secret, list):
            candidates.append((version, raw_secret))
    if not candidates:
        raise AuthenticationError("No usable TOTP secret is available.")
    version, cipher = max(candidates, key=lambda item: item[0])
    return version, decode_totp_secret(cipher)


def build_session() -> requests.Session:
    """Create a retrying HTTP session with short, bounded backoff."""
    retry = Retry(
        total=1,
        connect=1,
        read=1,
        status=1,
        backoff_factor=0.4,
        backoff_max=2.0,
        status_forcelist=(500, 502, 503, 504),
        allowed_methods=frozenset({"GET", "HEAD"}),
        respect_retry_after_header=False,
    )
    adapter = HTTPAdapter(max_retries=retry)
    session = requests.Session()
    session.mount("https://", adapter)
    session.headers.update(
        {
            "User-Agent": DEFAULT_USER_AGENT,
            "Accept": "application/json",
        }
    )
    return session


class SpotifyWebClient:
    """Read public editorial metadata through a dedicated web-player session.

    Authentication is performed with the dedicated source account's ``sp_dc``
    session. Playlist reads use the web player's persisted-query endpoint rather
    than the developer Web API, avoiding the shared-runner rate limit observed
    on ``api.spotify.com/v1/playlists``.
    """

    def __init__(
        self,
        sp_dc: str,
        *,
        session: requests.Session | None = None,
        secret_dict_url: str | None = None,
        playlist_query_hash: str | None = None,
        timeout_seconds: float = 8.0,
    ) -> None:
        self._sp_dc = normalize_sp_dc(sp_dc)
        self._session = session or build_session()
        self._secret_dict_url = validate_secret_dict_url(
            secret_dict_url
            or os.environ.get("LEVYRA_EDITORIAL_TOTP_SECRETS_URL")
            or DEFAULT_SECRET_DICT_URL
        )
        self._playlist_query_hash = validate_playlist_query_hash(
            playlist_query_hash
            or os.environ.get("LEVYRA_EDITORIAL_PLAYLIST_QUERY_HASH")
            or DEFAULT_PLAYLIST_QUERY_HASH
        )
        self._timeout = timeout_seconds
        self._access_token: str | None = None
        self._client_id: str | None = None
        self._expires_at_ms = 0
        self._playlist_pages: dict[tuple[str, int], dict[str, Any]] = {}
        self._track_metadata: dict[str, Mapping[str, Any]] = {}

    def authenticate(self) -> None:
        """Exchange the session cookie for a short-lived web-player access token."""
        LOGGER.info("Preparing editorial source authentication.")
        secret_dict = self._fetch_totp_secret_dictionary()
        totp_version, totp_secret = select_latest_totp_secret(secret_dict)
        last_error: Exception | None = None
        for product_type, reason in TOKEN_ATTEMPTS:
            LOGGER.info(
                "Trying editorial token profile %s / %s.",
                product_type,
                reason,
            )
            server_time = self._fetch_server_time()
            otp = generate_totp(totp_secret, server_time)
            try:
                token_data = self._request_access_token(
                    product_type=product_type,
                    reason=reason,
                    otp=otp,
                    totp_version=totp_version,
                )
                self._accept_token_response(token_data, server_time)
                LOGGER.info(
                    "Editorial source authentication succeeded with the %s profile.",
                    product_type,
                )
                return
            except (
                requests.RequestException,
                ValueError,
                AuthenticationError,
            ) as error:
                last_error = error
                self._access_token = None
                self._client_id = None
                LOGGER.warning(
                    "Editorial token profile %s / %s failed: %s.",
                    product_type,
                    reason,
                    _safe_authentication_failure(error),
                )

        raise AuthenticationError(
            "The editorial source session could not be authenticated. "
            "Rotate LEVYRA_EDITORIAL_SP_DC and retry the workflow."
        ) from last_error

    def get_playlist_metadata(
        self,
        playlist_id: str,
    ) -> dict[str, Any]:
        """Fetch public metadata for one configured playlist."""
        normalized_id = self._validate_playlist_id(playlist_id)
        page = self._get_playlist_page(normalized_id, offset=0)
        playlist = _playlist_union(page)
        content = _mapping(playlist.get("content"))
        total = _non_negative_int(content.get("totalCount")) if content else 0
        images = _playlist_images(playlist)
        return {
            "id": normalized_id,
            "name": _string(playlist.get("name")) or normalized_id,
            "description": _string(playlist.get("description")) or "",
            "external_urls": {
                "spotify": f"https://open.spotify.com/playlist/{normalized_id}"
            },
            "images": images,
            "snapshot_id": _playlist_snapshot_id(playlist),
            "tracks": {"total": total},
        }

    def iter_playlist_items(
        self,
        playlist_id: str,
    ) -> list[dict[str, Any]]:
        """Fetch every track item from one configured playlist, preserving order."""
        normalized_id = self._validate_playlist_id(playlist_id)
        offset = 0
        output: list[dict[str, Any]] = []
        total: int | None = None

        while total is None or offset < total:
            page = self._get_playlist_page(normalized_id, offset=offset)
            playlist = _playlist_union(page)
            content = _mapping(playlist.get("content"))
            if content is None:
                raise SourceApiError(
                    "The editorial playlist response is missing its content page."
                )
            raw_items = content.get("items")
            if not isinstance(raw_items, list):
                raise SourceApiError(
                    "The editorial playlist response has an invalid item list."
                )
            if total is None:
                total = _non_negative_int(content.get("totalCount"))
            converted = [
                converted_item if converted_item is not None else {"track": None}
                for raw_item in raw_items
                for converted_item in [
                    _convert_playlist_item(raw_item)
                    if isinstance(raw_item, Mapping)
                    else None
                ]
            ]
            output.extend(converted)

            consumed = len(raw_items)
            if consumed == 0:
                break
            offset += consumed
            if total is None or total <= offset:
                break

        return output

    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        """Best-effort ISRC and release metadata without weakening Pathfinder reads."""
        if self._access_token is None:
            self.authenticate()
        ids = [
            str(item.get("track", {}).get("id") or "").strip()
            for item in items
            if isinstance(item.get("track"), Mapping)
        ]
        missing = [track_id for track_id in dict.fromkeys(ids) if track_id and track_id not in self._track_metadata]
        for offset in range(0, len(missing), 50):
            chunk = missing[offset : offset + 50]
            if not chunk:
                continue
            try:
                response = self._session.get(
                    f"{API_BASE_URL}/tracks",
                    params={"ids": ",".join(chunk)},
                    headers=self._api_headers(),
                    timeout=self._timeout,
                )
                if response.status_code == 401:
                    self.authenticate()
                    response = self._session.get(
                        f"{API_BASE_URL}/tracks",
                        params={"ids": ",".join(chunk)},
                        headers=self._api_headers(),
                        timeout=self._timeout,
                    )
                if response.status_code >= 400:
                    LOGGER.warning(
                        "Spotify track metadata enrichment skipped after HTTP %s.",
                        response.status_code,
                    )
                    continue
                payload = response.json()
            except (requests.RequestException, ValueError, AuthenticationError) as error:
                LOGGER.warning(
                    "Spotify track metadata enrichment skipped: %s.",
                    _safe_authentication_failure(error),
                )
                continue
            raw_tracks = payload.get("tracks") if isinstance(payload, Mapping) else None
            if not isinstance(raw_tracks, list):
                continue
            for raw_track in raw_tracks:
                if not isinstance(raw_track, Mapping):
                    continue
                track_id = _string(raw_track.get("id"))
                if track_id:
                    self._track_metadata[track_id] = raw_track

        for item in items:
            track = item.get("track")
            if not isinstance(track, dict):
                continue
            enriched = self._track_metadata.get(str(track.get("id") or ""))
            if not isinstance(enriched, Mapping):
                continue
            external_ids = enriched.get("external_ids")
            if isinstance(external_ids, Mapping):
                track["external_ids"] = dict(external_ids)
            for key in ("track_number", "disc_number"):
                if isinstance(enriched.get(key), int):
                    track[key] = enriched[key]
            album = track.get("album")
            enriched_album = enriched.get("album")
            if isinstance(album, dict) and isinstance(enriched_album, Mapping):
                for key in ("album_type", "total_tracks", "release_date"):
                    value = enriched_album.get(key)
                    if value is not None:
                        album[key] = value
        return items

    def _api_headers(self) -> dict[str, str]:
        if self._access_token is None:
            raise AuthenticationError("The editorial source is not authenticated.")
        headers = {
            "Authorization": f"Bearer {self._access_token}",
            "Accept": "application/json",
        }
        if self._client_id:
            headers["Client-Id"] = self._client_id
        return headers

    def close(self) -> None:
        """Close the underlying HTTP session."""
        self._session.close()

    def _get_playlist_page(
        self,
        playlist_id: str,
        *,
        offset: int,
        limit: int = 100,
    ) -> dict[str, Any]:
        cache_key = (playlist_id, offset)
        cached = self._playlist_pages.get(cache_key)
        if cached is not None:
            return cached

        if self._access_token is None:
            self.authenticate()

        params = {
            "operationName": "fetchPlaylist",
            "variables": json.dumps(
                {
                    "uri": f"spotify:playlist:{playlist_id}",
                    "offset": offset,
                    "limit": limit,
                    "enableWatchFeedEntrypoint": False,
                },
                separators=(",", ":"),
            ),
            "extensions": json.dumps(
                {
                    "persistedQuery": {
                        "version": 1,
                        "sha256Hash": self._playlist_query_hash,
                    }
                },
                separators=(",", ":"),
            ),
        }

        response = self._pathfinder_request(params)
        if response.status_code == 401:
            self.authenticate()
            response = self._pathfinder_request(params)

        if response.status_code == 429:
            delay = _bounded_retry_after(response.headers.get("Retry-After"))
            if delay > 0:
                LOGGER.warning(
                    "Editorial pathfinder rate-limited; retrying once in %d second(s).",
                    delay,
                )
                time.sleep(delay)
                response = self._pathfinder_request(params)

        if response.status_code >= 400:
            raise SourceApiError(
                "The editorial pathfinder request failed with "
                f"HTTP {response.status_code}."
            )

        try:
            payload = response.json()
        except ValueError as error:
            raise SourceApiError(
                "The editorial pathfinder returned invalid JSON."
            ) from error
        if not isinstance(payload, dict):
            raise SourceApiError(
                "The editorial pathfinder returned an invalid response shape."
            )

        errors = payload.get("errors")
        if isinstance(errors, list) and errors:
            messages = {
                str(error.get("message") or "")
                for error in errors
                if isinstance(error, Mapping)
            }
            if "PersistedQueryNotFound" in messages:
                raise SourceApiError(
                    "Spotify rotated the fetchPlaylist query hash. "
                    "Update LEVYRA_EDITORIAL_PLAYLIST_QUERY_HASH."
                )
            raise SourceApiError(
                "The editorial pathfinder returned a GraphQL error."
            )

        _playlist_union(payload)
        self._playlist_pages[cache_key] = payload
        return payload

    def _pathfinder_request(
        self,
        params: Mapping[str, Any],
    ) -> requests.Response:
        return self._session.get(
            PATHFINDER_URL,
            params=params,
            headers=self._pathfinder_headers(),
            timeout=self._timeout,
        )

    def _fetch_totp_secret_dictionary(self) -> dict[str, Any]:
        try:
            response = self._session.get(
                self._secret_dict_url,
                timeout=self._timeout,
            )
            response.raise_for_status()
        except requests.RequestException as error:
            raise AuthenticationError(
                "The TOTP secret dictionary could not be retrieved."
            ) from error

        try:
            payload = response.json()
        except ValueError as error:
            raise AuthenticationError(
                "The TOTP secret dictionary is not valid JSON."
            ) from error
        if not isinstance(payload, dict):
            raise AuthenticationError(
                "The TOTP secret dictionary has an invalid shape."
            )
        return payload

    def _fetch_server_time(self) -> int:
        request_error: Exception | None = None
        try:
            response = self._session.get(
                SERVER_TIME_URL,
                headers=self._server_time_headers(),
                timeout=self._timeout,
            )
            response.raise_for_status()
            try:
                payload = response.json()
            except ValueError:
                payload = None
            if isinstance(payload, dict):
                server_time = _positive_timestamp(payload.get("serverTime"))
                if server_time is not None:
                    return server_time
            date_timestamp = _http_date_timestamp(response.headers.get("Date"))
            if date_timestamp is not None:
                return date_timestamp
        except requests.RequestException as error:
            request_error = error

        try:
            fallback_response = self._session.head(
                OPEN_SPOTIFY_URL,
                headers={"Accept": "*/*"},
                timeout=self._timeout,
            )
            fallback_response.raise_for_status()
        except requests.RequestException as error:
            raise AuthenticationError(
                "The source server time could not be retrieved."
            ) from (request_error or error)

        fallback_timestamp = _http_date_timestamp(
            fallback_response.headers.get("Date")
        )
        if fallback_timestamp is None:
            raise AuthenticationError(
                "The source returned an invalid server time."
            )
        return fallback_timestamp

    def _request_access_token(
        self,
        *,
        product_type: str,
        reason: str,
        otp: str,
        totp_version: int,
    ) -> dict[str, Any]:
        response = self._session.get(
            TOKEN_URL,
            params={
                "reason": reason,
                "productType": product_type,
                "totp": otp,
                "totpServer": otp,
                "totpVer": totp_version,
            },
            headers=self._web_player_headers(),
            timeout=self._timeout,
        )
        if response.status_code >= 400:
            raise AuthenticationError(
                f"token endpoint returned HTTP {response.status_code}"
            )
        try:
            payload = response.json()
        except ValueError as error:
            raise AuthenticationError(
                "token endpoint returned invalid JSON"
            ) from error
        if not isinstance(payload, dict):
            raise AuthenticationError(
                "token endpoint returned an invalid response shape"
            )
        return payload

    def _accept_token_response(
        self,
        token_data: Mapping[str, Any],
        server_time: int,
    ) -> None:
        access_token = str(token_data.get("accessToken", "")).strip()
        if not access_token:
            raise AuthenticationError(
                "token endpoint returned an empty access token"
            )
        if token_data.get("isAnonymous") is True:
            raise AuthenticationError(
                "token endpoint returned an anonymous session"
            )

        expires_at_ms = _positive_timestamp(
            token_data.get("accessTokenExpirationTimestampMs")
        )
        if (
            expires_at_ms is not None
            and expires_at_ms <= server_time * 1000
        ):
            raise AuthenticationError(
                "token endpoint returned an expired access token"
            )

        self._access_token = access_token
        self._client_id = (
            str(token_data.get("clientId", "")).strip() or None
        )
        self._expires_at_ms = expires_at_ms or 0

    def _web_player_headers(self) -> dict[str, str]:
        return {
            "Accept": "application/json",
            "App-Platform": "WebPlayer",
            "Cookie": f"sp_dc={self._sp_dc}",
            "Origin": OPEN_SPOTIFY_URL.rstrip("/"),
            "Referer": OPEN_SPOTIFY_URL,
        }

    def _server_time_headers(self) -> dict[str, str]:
        headers = self._web_player_headers()
        headers["Spotify-App-Version"] = SPOTIFY_APP_VERSION
        return headers

    def _pathfinder_headers(self) -> dict[str, str]:
        if self._access_token is None:
            raise AuthenticationError(
                "The editorial source is not authenticated."
            )
        headers = {
            "Authorization": f"Bearer {self._access_token}",
            "Accept": "application/json",
            "App-Platform": "WebPlayer",
            "Origin": OPEN_SPOTIFY_URL.rstrip("/"),
            "Referer": OPEN_SPOTIFY_URL,
        }
        if self._client_id:
            headers["Client-Id"] = self._client_id
        return headers

    @staticmethod
    def _validate_playlist_id(value: str) -> str:
        normalized = value.strip()
        if (
            len(normalized) not in range(10, 80)
            or not normalized.isalnum()
        ):
            raise SourceApiError(
                "A configured playlist ID is invalid."
            )
        return normalized


def _playlist_union(payload: Mapping[str, Any]) -> dict[str, Any]:
    data = _mapping(payload.get("data"))
    playlist = _mapping(data.get("playlistV2")) if data else None
    if playlist is None:
        raise SourceApiError(
            "The editorial pathfinder response is missing playlistV2."
        )
    return dict(playlist)


def _convert_playlist_item(
    item: Mapping[str, Any],
) -> dict[str, Any] | None:
    item_v2 = _mapping(item.get("itemV2"))
    data = _mapping(item_v2.get("data")) if item_v2 else None
    if data is None or data.get("__typename") != "Track":
        return None

    uri = _string(data.get("uri"))
    name = _string(data.get("name"))
    duration = _mapping(data.get("trackDuration"))
    duration_ms = (
        _positive_timestamp(duration.get("totalMilliseconds"))
        if duration
        else None
    )
    if not uri or not name or duration_ms is None:
        return None
    track_id = _spotify_id(uri)
    if not track_id:
        return None

    artists = _artists(data.get("artists"))
    if not artists:
        return None

    album_node = _mapping(data.get("albumOfTrack")) or {}
    album_uri = _string(album_node.get("uri"))
    album_id = _spotify_id(album_uri)
    album_name = _string(album_node.get("name")) or name
    images = _cover_art_images(album_node)
    external_album = (
        f"https://open.spotify.com/album/{album_id}"
        if album_id
        else None
    )

    rating = _mapping(data.get("contentRating"))
    explicit = bool(rating and rating.get("label") == "EXPLICIT")

    track = {
        "id": track_id,
        "uri": uri,
        "type": "track",
        "name": name,
        "duration_ms": duration_ms,
        "explicit": explicit,
        "is_local": False,
        "external_ids": {},
        "external_urls": {
            "spotify": f"https://open.spotify.com/track/{track_id}"
        },
        "artists": artists,
        "album": {
            "id": album_id,
            "name": album_name,
            "release_date": _playlist_release_date(item),
            "images": images,
            "external_urls": {
                "spotify": external_album
            } if external_album else {},
        },
    }
    return {"track": track}


def _artists(value: Any) -> list[dict[str, str | None]]:
    node = _mapping(value)
    items = node.get("items") if node else None
    if not isinstance(items, Sequence) or isinstance(items, (str, bytes)):
        return []

    output: list[dict[str, str | None]] = []
    for item in items:
        if not isinstance(item, Mapping):
            continue
        profile = _mapping(item.get("profile"))
        name = _string(profile.get("name")) if profile else None
        uri = _string(item.get("uri"))
        if not name:
            continue
        output.append(
            {
                "id": _spotify_id(uri),
                "name": name,
            }
        )
    return output


def _cover_art_images(value: Mapping[str, Any]) -> list[dict[str, Any]]:
    cover_art = _mapping(value.get("coverArt"))
    sources = cover_art.get("sources") if cover_art else None
    return _image_sources(sources)


def _playlist_images(
    playlist: Mapping[str, Any],
) -> list[dict[str, Any]]:
    images = _mapping(playlist.get("images"))
    items = images.get("items") if images else None
    if not isinstance(items, Sequence) or isinstance(items, (str, bytes)):
        return []
    for item in items:
        if not isinstance(item, Mapping):
            continue
        sources = _image_sources(item.get("sources"))
        if sources:
            return sources

    attributes = playlist.get("attributes")
    if isinstance(attributes, Sequence) and not isinstance(
        attributes,
        (str, bytes),
    ):
        for attribute in attributes:
            if not isinstance(attribute, Mapping):
                continue
            if attribute.get("key") not in {
                "image_url",
                "header_image_url_desktop",
            }:
                continue
            url = _string(attribute.get("value"))
            if url and url.startswith("https://"):
                return [{"url": url}]
    return []


def _image_sources(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, Sequence) or isinstance(value, (str, bytes)):
        return []
    output: list[dict[str, Any]] = []
    for source in value:
        if not isinstance(source, Mapping):
            continue
        url = _string(source.get("url"))
        if not url or not url.startswith("https://"):
            continue
        image: dict[str, Any] = {"url": url}
        width = _non_negative_int(
            source.get("width", source.get("maxWidth"))
        )
        height = _non_negative_int(
            source.get("height", source.get("maxHeight"))
        )
        if width:
            image["width"] = width
        if height:
            image["height"] = height
        output.append(image)
    return output


def _playlist_release_date(item: Mapping[str, Any]) -> str | None:
    item_v3 = _mapping(item.get("itemV3"))
    data = _mapping(item_v3.get("data")) if item_v3 else None
    identity = _mapping(data.get("identityTrait")) if data else None
    parent = (
        _mapping(identity.get("contentHierarchyParent"))
        if identity
        else None
    )
    publishing = (
        _mapping(parent.get("publishingMetadataTrait"))
        if parent
        else None
    )
    first_published = (
        _mapping(publishing.get("firstPublishedAt"))
        if publishing
        else None
    )
    return (
        _string(first_published.get("isoString"))
        if first_published
        else None
    )


def _playlist_snapshot_id(
    playlist: Mapping[str, Any],
) -> str | None:
    attributes = playlist.get("attributes")
    if not isinstance(attributes, Sequence) or isinstance(
        attributes,
        (str, bytes),
    ):
        return None
    for attribute in attributes:
        if not isinstance(attribute, Mapping):
            continue
        if attribute.get("key") in {
            "correlation-id",
            "revision",
            "snapshot_id",
        }:
            value = _string(attribute.get("value"))
            if value:
                return value
    return None


def _safe_authentication_failure(error: Exception) -> str:
    if isinstance(error, AuthenticationError):
        return str(error)
    if isinstance(error, requests.Timeout):
        return "request timed out"
    if isinstance(error, requests.ConnectionError):
        return "network connection failed"
    if isinstance(error, requests.HTTPError):
        status_code = getattr(
            getattr(error, "response", None),
            "status_code",
            None,
        )
        return (
            f"request failed with HTTP {status_code}"
            if status_code
            else "HTTP request failed"
        )
    if isinstance(error, ValueError):
        return "invalid token response"
    return type(error).__name__


def _bounded_retry_after(value: str | None) -> int:
    if not value:
        return 1
    try:
        return min(max(int(value), 0), 5)
    except ValueError:
        # `Retry-After` may legally be an HTTP-date. Fall back to the absent-header default so a
        # rate-limited page still gets its single retry instead of failing immediately.
        return 1


def validate_secret_dict_url(value: str) -> str:
    """Allow only the pinned third-party dictionary path over HTTPS."""
    normalized = value.strip()
    parsed = urlparse(normalized)
    expected_path = re.fullmatch(
        r"/xyloflake/spot-secrets-go/[0-9a-f]{40}/secrets/secretDict\.json",
        parsed.path,
    )
    if (
        parsed.scheme != "https"
        or parsed.hostname != "raw.githubusercontent.com"
        or parsed.username is not None
        or parsed.password is not None
        or parsed.port is not None
        or parsed.query
        or parsed.fragment
        or expected_path is None
    ):
        raise AuthenticationError("The TOTP dictionary URL is not allowlisted.")
    return normalized


def validate_playlist_query_hash(value: str) -> str:
    normalized = value.strip().lower()
    if re.fullmatch(r"[0-9a-f]{64}", normalized) is None:
        raise AuthenticationError("The playlist query hash is malformed.")
    return normalized


def _spotify_id(uri: str | None) -> str | None:
    if not uri:
        return None
    candidate = uri.rsplit(":", maxsplit=1)[-1].strip()
    return candidate if candidate.isalnum() else None


def _mapping(value: Any) -> Mapping[str, Any] | None:
    return value if isinstance(value, Mapping) else None


def _string(value: Any) -> str | None:
    if not isinstance(value, str):
        return None
    normalized = value.strip()
    return normalized or None


def _non_negative_int(value: Any) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        return 0
    return max(value, 0)


def _positive_timestamp(value: Any) -> int | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, int) and value > 0:
        return value
    if isinstance(value, str):
        try:
            parsed = int(value)
        except ValueError:
            return None
        return parsed if parsed > 0 else None
    return None


def _http_date_timestamp(value: str | None) -> int | None:
    if not value:
        return None
    try:
        return int(parsedate_to_datetime(value).timestamp())
    except (TypeError, ValueError, OverflowError):
        return None
