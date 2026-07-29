from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pytest
import requests

from levyra_editorial.collector import (
    build_catalog,
    load_config,
    normalize_playlist_items,
    validate_catalog_dict,
    write_catalog,
)
from levyra_editorial.spotify import (
    API_BASE_URL,
    SERVER_TIME_URL,
    TOKEN_URL,
    AuthenticationError,
    SpotifyWebClient,
    decode_totp_secret,
    generate_totp,
    normalize_sp_dc,
    select_latest_totp_secret,
)


class FakeClient:
    """Deterministic source used by collector unit tests."""

    def get_playlist_metadata(self, playlist_id: str, market: str) -> dict:
        return {
            "id": playlist_id,
            "name": "Source title",
            "description": "  A   compact\n description. ",
            "external_urls": {"spotify": f"https://open.spotify.com/playlist/{playlist_id}"},
            "images": [{"url": "https://image.example/playlist.jpg"}],
            "snapshot_id": "snapshot-1",
            "tracks": {"total": 2},
        }

    def iter_playlist_items(self, playlist_id: str, market: str) -> list[dict]:
        return [
            {
                "track": {
                    "id": "track1",
                    "uri": "spotify:track:track1",
                    "type": "track",
                    "name": "First track",
                    "duration_ms": 185_000,
                    "explicit": False,
                    "external_ids": {"isrc": "ITABC2600001"},
                    "external_urls": {"spotify": "https://open.spotify.com/track/track1"},
                    "artists": [{"id": "artist1", "name": "Artist One"}],
                    "album": {
                        "id": "album1",
                        "name": "Album One",
                        "release_date": "2026-07-01",
                        "images": [{"url": "https://image.example/album.jpg"}],
                        "external_urls": {"spotify": "https://open.spotify.com/album/album1"},
                    },
                }
            },
            {
                "track": {
                    "id": "local1",
                    "uri": "spotify:local:local1",
                    "type": "track",
                    "name": "Local track",
                    "duration_ms": 120_000,
                    "is_local": True,
                    "artists": [{"name": "Local Artist"}],
                    "album": {"name": "Local Album"},
                }
            },
        ]


class FakeResponse:
    """Small response double for authentication tests."""

    def __init__(
        self,
        *,
        payload: object | None = None,
        headers: dict[str, str] | None = None,
        status_code: int = 200,
    ) -> None:
        self._payload = payload
        self.headers = headers or {}
        self.status_code = status_code

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise requests.HTTPError(f"HTTP {self.status_code}")

    def json(self) -> object:
        return self._payload


class SecretTimeoutSession:
    """Session double that fails before the TOTP dictionary is available."""

    def get(self, *_args: object, **_kwargs: object) -> FakeResponse:
        raise requests.Timeout("secret dictionary timeout")

    def close(self) -> None:
        return None


class ServerTimeTimeoutSession:
    """Session double that fails for both source-clock strategies."""

    def get(self, url: str, *_args: object, **_kwargs: object) -> FakeResponse:
        if url == SERVER_TIME_URL:
            raise requests.Timeout("server time timeout")
        return FakeResponse(payload={"61": [44, 55, 47, 42]})

    def head(self, *_args: object, **_kwargs: object) -> FakeResponse:
        raise requests.Timeout("fallback server time timeout")

    def close(self) -> None:
        return None


class ServerTimeHeaderSession:
    """Session double that exposes an HTTP Date fallback."""

    def get(self, url: str, *_args: object, **_kwargs: object) -> FakeResponse:
        assert url == SERVER_TIME_URL
        return FakeResponse(
            payload={},
            headers={"Date": "Wed, 29 Jul 2026 12:00:00 GMT"},
        )

    def head(self, *_args: object, **_kwargs: object) -> FakeResponse:
        raise AssertionError("The secondary fallback should not be needed.")

    def close(self) -> None:
        return None


class AuthenticationSession:
    """Record the live-style authentication request order without network access."""

    def __init__(self, successful_attempt: tuple[str, str]) -> None:
        self.successful_attempt = successful_attempt
        self.token_attempts: list[tuple[str, str]] = []
        self.server_time_headers: dict[str, str] = {}

    def get(self, url: str, **kwargs: Any) -> FakeResponse:
        if url == SERVER_TIME_URL:
            self.server_time_headers = dict(kwargs.get("headers") or {})
            return FakeResponse(payload={"serverTime": "1785326400"})
        if url == TOKEN_URL:
            params = dict(kwargs.get("params") or {})
            attempt = (str(params.get("productType")), str(params.get("reason")))
            self.token_attempts.append(attempt)
            if attempt == self.successful_attempt:
                return FakeResponse(
                    payload={
                        "accessToken": "temporary-token",
                        "clientId": "web-client",
                        "accessTokenExpirationTimestampMs": 1_785_330_000_000,
                    }
                )
            return FakeResponse(payload={"accessToken": ""})
        if url == f"{API_BASE_URL}/me":
            return FakeResponse(payload={"id": "editorial-account"})
        return FakeResponse(payload={"61": [44, 55, 47, 42]})

    def head(self, *_args: object, **_kwargs: object) -> FakeResponse:
        raise AssertionError("The Spotify server-time endpoint should be preferred.")

    def close(self) -> None:
        return None


def test_normalize_sp_dc_accepts_value_and_cookie_string() -> None:
    value = "A" * 40
    assert normalize_sp_dc(value) == value
    assert normalize_sp_dc(f"foo=1; sp_dc={value}; bar=2") == value


@pytest.mark.parametrize("value", ["", "short", "contains whitespace"])
def test_normalize_sp_dc_rejects_malformed_values(value: str) -> None:
    with pytest.raises(AuthenticationError):
        normalize_sp_dc(value)


def test_totp_generator_matches_rfc_6238_vector() -> None:
    secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    assert generate_totp(secret, 59, digits=8) == "94287082"


def test_totp_secret_selection_uses_latest_numeric_version() -> None:
    secret_dict = {
        "bad": [1, 2],
        "59": [123, 105, 79],
        "61": [44, 55, 47, 42],
    }
    version, secret = select_latest_totp_secret(secret_dict)
    assert version == 61
    assert secret == decode_totp_secret([44, 55, 47, 42])
    assert secret


def test_authentication_wraps_secret_dictionary_request_failures() -> None:
    client = SpotifyWebClient("A" * 40, session=SecretTimeoutSession())

    with pytest.raises(AuthenticationError, match="secret dictionary could not be retrieved"):
        client.authenticate()


def test_authentication_wraps_server_time_request_failures() -> None:
    client = SpotifyWebClient("A" * 40, session=ServerTimeTimeoutSession())

    with pytest.raises(AuthenticationError, match="server time could not be retrieved"):
        client.authenticate()


def test_server_time_accepts_http_date_when_json_field_is_missing() -> None:
    client = SpotifyWebClient("A" * 40, session=ServerTimeHeaderSession())

    assert client._fetch_server_time() == 1_785_326_400


def test_authentication_prefers_current_mobile_web_player_flow(caplog: pytest.LogCaptureFixture) -> None:
    cookie = "A" * 40
    session = AuthenticationSession(("mobile-web-player", "transport"))
    client = SpotifyWebClient(cookie, session=session)

    client.authenticate()

    assert session.token_attempts == [("mobile-web-player", "transport")]
    assert session.server_time_headers["Cookie"] == f"sp_dc={cookie}"
    assert session.server_time_headers["App-Platform"] == "WebPlayer"
    assert session.server_time_headers["Origin"] == "https://open.spotify.com"
    assert cookie not in caplog.text
    assert "temporary-token" not in caplog.text


def test_authentication_falls_back_without_repeating_successful_profiles() -> None:
    session = AuthenticationSession(("web-player", "transport"))
    client = SpotifyWebClient("A" * 40, session=session)

    client.authenticate()

    assert session.token_attempts == [
        ("mobile-web-player", "transport"),
        ("mobile-web-player", "init"),
        ("web-player", "transport"),
    ]


def test_collector_builds_compact_account_free_catalog(tmp_path: Path) -> None:
    config_path = tmp_path / "config.json"
    config_path.write_text(
        json.dumps(
            {
                "schemaVersion": 1,
                "collections": [
                    {
                        "id": "top-50-italy",
                        "kind": "chart",
                        "market": "IT",
                        "playlistId": "37i9dQZEVXbIQnj7RRhdSX",
                        "title": "Top 50 Italia",
                    }
                ],
            }
        ),
        encoding="utf-8",
    )
    config = load_config(config_path)
    catalog = build_catalog(config, FakeClient(), generated_at="2026-07-29T12:00:00Z")
    payload = catalog.to_dict()

    assert payload["schemaVersion"] == 1
    assert payload["collections"][0]["title"] == "Top 50 Italia"
    assert payload["collections"][0]["description"] == "A compact description."
    assert payload["collections"][0]["totalSourceItems"] == 2
    assert len(payload["collections"][0]["tracks"]) == 1
    assert payload["collections"][0]["tracks"][0]["position"] == 1
    assert payload["collections"][0]["tracks"][0]["isrc"] == "ITABC2600001"
    assert "owner" not in json.dumps(payload).lower()
    assert "sp_dc" not in json.dumps(payload).lower()

    output = tmp_path / "catalog.json"
    write_catalog(catalog, output)
    assert json.loads(output.read_text(encoding="utf-8")) == payload


def test_playlist_positions_preserve_source_order_when_items_are_skipped() -> None:
    items = FakeClient().iter_playlist_items("playlist12345", "IT")
    items.insert(0, {"track": None})
    tracks = normalize_playlist_items(items)
    assert [track.position for track in tracks] == [2]


def test_catalog_validation_rejects_credential_keys() -> None:
    payload = {
        "schemaVersion": 1,
        "generatedAt": "2026-07-29T12:00:00Z",
        "collections": [
            {
                "id": "top-50-italy",
                "tracks": [{"position": 1, "id": "x", "title": "Song"}],
                "access_token": "secret",
            }
        ],
    }
    with pytest.raises(ValueError):
        validate_catalog_dict(payload)


def test_config_rejects_duplicate_collection_ids(tmp_path: Path) -> None:
    config_path = tmp_path / "config.json"
    collection = {
        "id": "same",
        "kind": "chart",
        "market": "IT",
        "playlistId": "37i9dQZEVXbIQnj7RRhdSX",
    }
    config_path.write_text(
        json.dumps({"schemaVersion": 1, "collections": [collection, collection]}),
        encoding="utf-8",
    )
    with pytest.raises(ValueError):
        load_config(config_path)
