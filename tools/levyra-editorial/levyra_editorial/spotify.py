from __future__ import annotations

import base64
import hashlib
import hmac
import logging
import os
import struct
from collections.abc import Mapping
from email.utils import parsedate_to_datetime
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

LOGGER = logging.getLogger(__name__)

OPEN_SPOTIFY_URL = "https://open.spotify.com/"
SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
TOKEN_URL = "https://open.spotify.com/api/token"
API_BASE_URL = "https://api.spotify.com/v1"
DEFAULT_SECRET_DICT_URL = (
    "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"
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
    invalid = any(not isinstance(value, int) or value not in range(256) for value in cipher_bytes)
    if not cipher_bytes or invalid:
        raise AuthenticationError("The TOTP secret dictionary contains invalid bytes.")
    transformed = [value ^ ((index % 33) + 9) for index, value in enumerate(cipher_bytes)]
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
    code = (struct.unpack(">I", digest[offset : offset + 4])[0] & 0x7FFFFFFF) % (10**digits)
    return f"{code:0{digits}d}"


def select_latest_totp_secret(secret_dict: Mapping[str, Any]) -> tuple[int, str]:
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

    The session cookie is used only to obtain a short-lived bearer token. It is
    never returned by this class, serialized, or included in log messages.
    """

    def __init__(
        self,
        sp_dc: str,
        *,
        session: requests.Session | None = None,
        secret_dict_url: str | None = None,
        timeout_seconds: float = 8.0,
    ) -> None:
        self._sp_dc = normalize_sp_dc(sp_dc)
        self._session = session or build_session()
        self._secret_dict_url = (
            secret_dict_url
            or os.environ.get("LEVYRA_EDITORIAL_TOTP_SECRETS_URL")
            or DEFAULT_SECRET_DICT_URL
        )
        self._timeout = timeout_seconds
        self._access_token: str | None = None
        self._client_id: str | None = None
        self._expires_at_ms = 0

    def authenticate(self) -> None:
        """Exchange the session cookie for a short-lived web-player access token."""
        LOGGER.info("Preparing editorial source authentication.")
        secret_dict = self._fetch_totp_secret_dictionary()
        totp_version, totp_secret = select_latest_totp_secret(secret_dict)
        server_time = self._fetch_server_time()
        otp = generate_totp(totp_secret, server_time)

        last_error: Exception | None = None
        for product_type, reason in TOKEN_ATTEMPTS:
            LOGGER.info("Trying editorial token profile %s / %s.", product_type, reason)
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
            except (requests.RequestException, ValueError, AuthenticationError) as error:
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

    def get_playlist_metadata(self, playlist_id: str, market: str) -> dict[str, Any]:
        """Fetch public metadata for one configured playlist."""
        normalized_id = self._validate_playlist_id(playlist_id)
        response = self._api_get(
            f"/playlists/{normalized_id}",
            params={
                "market": _api_market(market),
                "fields": (
                    "id,name,description,external_urls,images,snapshot_id,"
                    "tracks(total)"
                ),
            },
        )
        return _require_object(response, "playlist metadata")

    def iter_playlist_items(self, playlist_id: str, market: str) -> list[dict[str, Any]]:
        """Fetch every track item from one configured playlist, preserving order."""
        normalized_id = self._validate_playlist_id(playlist_id)
        url: str | None = f"{API_BASE_URL}/playlists/{normalized_id}/tracks"
        params: dict[str, Any] | None = {
            "market": _api_market(market),
            "limit": 100,
            "offset": 0,
            "additional_types": "track",
        }
        items: list[dict[str, Any]] = []

        while url:
            payload = self._api_get_url(url, params=params)
            page = _require_object(payload, "playlist items")
            raw_items = page.get("items", [])
            if not isinstance(raw_items, list):
                raise SourceApiError("The playlist items response has an invalid shape.")
            items.extend(item for item in raw_items if isinstance(item, dict))
            next_url = page.get("next")
            url = next_url if isinstance(next_url, str) and next_url.startswith("https://") else None
            params = None

        return items

    def close(self) -> None:
        """Close the underlying HTTP session."""
        self._session.close()

    def _fetch_totp_secret_dictionary(self) -> dict[str, Any]:
        try:
            response = self._session.get(self._secret_dict_url, timeout=self._timeout)
            response.raise_for_status()
        except requests.RequestException as error:
            raise AuthenticationError(
                "The TOTP secret dictionary could not be retrieved."
            ) from error

        try:
            payload = response.json()
        except ValueError as error:
            raise AuthenticationError("The TOTP secret dictionary is not valid JSON.") from error
        if not isinstance(payload, dict):
            raise AuthenticationError("The TOTP secret dictionary has an invalid shape.")
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
            raise AuthenticationError("The source server time could not be retrieved.") from (
                request_error or error
            )

        fallback_timestamp = _http_date_timestamp(fallback_response.headers.get("Date"))
        if fallback_timestamp is None:
            raise AuthenticationError("The source returned an invalid server time.")
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
            raise AuthenticationError("token endpoint returned invalid JSON") from error
        if not isinstance(payload, dict):
            raise AuthenticationError("token endpoint returned an invalid response shape")
        return payload

    def _accept_token_response(self, token_data: Mapping[str, Any], server_time: int) -> None:
        access_token = str(token_data.get("accessToken", "")).strip()
        if not access_token:
            raise AuthenticationError("token endpoint returned an empty access token")
        if token_data.get("isAnonymous") is True:
            raise AuthenticationError("token endpoint returned an anonymous session")

        expires_at_ms = _positive_timestamp(token_data.get("accessTokenExpirationTimestampMs"))
        if expires_at_ms is not None and expires_at_ms <= server_time * 1000:
            raise AuthenticationError("token endpoint returned an expired access token")

        self._access_token = access_token
        self._client_id = str(token_data.get("clientId", "")).strip() or None
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

    def _api_get(self, path: str, *, params: Mapping[str, Any] | None = None) -> Any:
        return self._api_get_url(f"{API_BASE_URL}{path}", params=params)

    def _api_get_url(self, url: str, *, params: Mapping[str, Any] | None = None) -> Any:
        if self._access_token is None:
            self.authenticate()
        response = self._session.get(
            url,
            params=params,
            headers=self._api_headers(),
            timeout=self._timeout,
        )
        if response.status_code == 401:
            self.authenticate()
            response = self._session.get(
                url,
                params=params,
                headers=self._api_headers(),
                timeout=self._timeout,
            )
        if response.status_code >= 400:
            raise SourceApiError(
                f"The editorial source request failed with HTTP {response.status_code}."
            )
        try:
            return response.json()
        except ValueError as error:
            raise SourceApiError("The editorial source returned invalid JSON.") from error

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

    @staticmethod
    def _validate_playlist_id(value: str) -> str:
        normalized = value.strip()
        if len(normalized) not in range(10, 80) or not normalized.isalnum():
            raise SourceApiError("A configured playlist ID is invalid.")
        return normalized


def _safe_authentication_failure(error: Exception) -> str:
    if isinstance(error, AuthenticationError):
        return str(error)
    if isinstance(error, requests.Timeout):
        return "request timed out"
    if isinstance(error, requests.ConnectionError):
        return "network connection failed"
    if isinstance(error, requests.HTTPError):
        status_code = getattr(getattr(error, "response", None), "status_code", None)
        return f"request failed with HTTP {status_code}" if status_code else "HTTP request failed"
    if isinstance(error, ValueError):
        return "invalid token response"
    return type(error).__name__


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


def _api_market(market: str) -> str:
    normalized = market.strip().upper()
    return "from_token" if normalized in {"", "GLOBAL", "WORLD"} else normalized


def _require_object(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise SourceApiError(f"The {label} response has an invalid shape.")
    return value
