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
    validate_playlist_query_hash,
    validate_secret_dict_url,
)


class FakeClient:
    """Deterministic source used by collector unit tests."""

    def get_playlist_metadata(self, playlist_id: str) -> dict:
        return {
            "id": playlist_id,
            "name": "Source title",
            "description": "  A   compact\n description. ",
            "external_urls": {"spotify": f"https://open.spotify.com/playlist/{playlist_id}"},
            "images": [{"url": "https://i.scdn.co/image/playlist-cover"}],
            "snapshot_id": "snapshot-1",
            "tracks": {"total": 2},
        }

    def iter_playlist_items(self, playlist_id: str) -> list[dict]:
        return [
            {
                "track": {
                    "id": "track1",
                    "uri": "spotify:track:track1",
                    "type": "track",
                    "name": "First track",
                    "duration_ms": 185_000,
                    "explicit": False,
                    "external_ids": {},
                    "external_urls": {"spotify": "https://open.spotify.com/track/track1"},
                    "artists": [{"id": "artist1", "name": "Artist One"}],
                    "album": {
                        "id": "album1",
                        "name": "Album One",
                        "release_date": "2026-07-01",
                        "images": [{"url": "https://i.scdn.co/image/test-album-cover"}],
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

    def __init__(
        self,
        successful_attempt: tuple[str, str],
        *,
        anonymous_attempts: set[tuple[str, str]] | None = None,
    ) -> None:
        self.successful_attempt = successful_attempt
        self.anonymous_attempts = anonymous_attempts or set()
        self.token_attempts: list[tuple[str, str]] = []
        self.server_time_headers: dict[str, str] = {}
        self.requested_urls: list[str] = []

    def get(self, url: str, **kwargs: Any) -> FakeResponse:
        self.requested_urls.append(url)
        if url == SERVER_TIME_URL:
            self.server_time_headers = dict(kwargs.get("headers") or {})
            return FakeResponse(payload={"serverTime": "1785326400"})
        if url == TOKEN_URL:
            params = dict(kwargs.get("params") or {})
            attempt = (str(params.get("productType")), str(params.get("reason")))
            self.token_attempts.append(attempt)
            if attempt in self.anonymous_attempts:
                return FakeResponse(
                    payload={
                        "accessToken": "anonymous-token",
                        "isAnonymous": True,
                        "accessTokenExpirationTimestampMs": 1_785_330_000_000,
                    }
                )
            if attempt == self.successful_attempt:
                return FakeResponse(
                    payload={
                        "accessToken": "temporary-token",
                        "clientId": "web-client",
                        "isAnonymous": False,
                        "accessTokenExpirationTimestampMs": 1_785_330_000_000,
                    }
                )
            return FakeResponse(payload={"accessToken": ""})
        return FakeResponse(payload={"61": [44, 55, 47, 42]})

    def head(self, *_args: object, **_kwargs: object) -> FakeResponse:
        raise AssertionError("The source server-time endpoint should be preferred.")

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
    assert session.server_time_headers["Spotify-App-Version"]
    assert session.server_time_headers["Origin"] == "https://open.spotify.com"
    assert f"{API_BASE_URL}/me" not in session.requested_urls
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


def test_authentication_rejects_anonymous_token_then_uses_next_profile() -> None:
    first = ("mobile-web-player", "transport")
    second = ("mobile-web-player", "init")
    session = AuthenticationSession(second, anonymous_attempts={first})
    client = SpotifyWebClient("A" * 40, session=session)

    client.authenticate()

    assert session.token_attempts == [first, second]


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
    collection = payload["collections"][0]
    track = collection["tracks"][0]
    assert collection["title"] == "Top 50 Italia"
    assert collection["description"] == "A compact description."
    assert collection["totalSourceItems"] == 2
    assert len(collection["tracks"]) == 1
    assert track["position"] == 1
    assert track["id"].startswith("levyra-")
    assert "isrc" not in track
    # Cover artwork is published on purpose: chart rows need a real cover for every entry. It must be
    # the CDN URL and nothing else about the source may ride along with it.
    assert track["artworkUrl"] == "https://i.scdn.co/image/test-album-cover"
    assert "artworkUrl" not in track["album"]
    serialized = json.dumps(payload).lower()
    assert "owner" not in serialized
    assert "sp_dc" not in serialized
    assert "open.spotify.com" not in serialized
    assert "sourceid" not in serialized
    assert "sourceurl" not in serialized
    assert "externalurl" not in serialized
    assert "uri" not in track
    assert track["artists"] == [{"name": "Artist One"}]

    output = tmp_path / "catalog.json"
    write_catalog(catalog, output)
    assert json.loads(output.read_text(encoding="utf-8")) == payload


def test_playlist_positions_preserve_source_order_when_items_are_skipped() -> None:
    items = FakeClient().iter_playlist_items("playlist12345")
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




def test_catalog_keeps_separate_audio_and_official_video_ids() -> None:
    item = FakeClient().iter_playlist_items("playlist12345")[0]
    item["track"]["youtube_music"] = {
        "audioVideoId": "Audio123456",
        "audioConfidence": 99,
        "videoId": "Official123",
        "videoConfidence": 97,
        "confidence": 99,
    }

    public = normalize_playlist_items([item])[0].to_dict()

    assert public["youtubeMusic"]["audioVideoId"] == "Audio123456"
    assert public["youtubeMusic"]["videoId"] == "Official123"
    assert public["youtubeMusic"]["audioConfidence"] == 99
    assert public["youtubeMusic"]["videoConfidence"] == 97


def test_secret_dictionary_url_requires_pinned_allowlisted_path() -> None:
    valid = (
        "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/"
        "4cd9440671af3a419bad112164a193ea1374e0e1/secrets/secretDict.json"
    )
    assert validate_secret_dict_url(valid) == valid
    with pytest.raises(AuthenticationError):
        validate_secret_dict_url("https://example.com/secretDict.json")
    with pytest.raises(AuthenticationError):
        validate_secret_dict_url(
            "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/main/secrets/secretDict.json"
        )


def test_playlist_query_hash_is_strictly_validated() -> None:
    assert validate_playlist_query_hash("a" * 64) == "a" * 64
    with pytest.raises(AuthenticationError):
        validate_playlist_query_hash("not-a-hash")



def test_catalog_keeps_public_isrc_and_release_type() -> None:
    item = FakeClient().iter_playlist_items("playlist12345")[0]
    item["track"]["external_ids"] = {"isrc": "ITB002000001"}
    item["track"]["album"]["album_type"] = "album"
    item["track"]["album"]["total_tracks"] = 12
    public = normalize_playlist_items([item])[0].to_dict()
    assert public["isrc"] == "ITB002000001"
    assert public["album"]["type"] == "album"
    assert public["album"]["totalTracks"] == 12
