import threading
from concurrent.futures import ThreadPoolExecutor
from contextlib import contextmanager

import pytest
import requests

from levyra_editorial.metadata import CatalogSearchService, sanitize_catalog, validate_catalog_request
from levyra_editorial.resolver import CanvasResolverService, create_resolver_server
from levyra_editorial.spotify import SpotifySearchUnavailable, SpotifyWebClient

REQUEST = {"query": "public song", "limit": 12, "country": "IT"}


def catalog():
    artist = {
        "uri": "spotify:artist:" + "A" * 22,
        "profile": {"name": "Artist"},
        "visuals": {"avatarImage": {"sources": []}},
        "private": "secret",
    }
    album = {
        "uri": "spotify:album:" + "B" * 22,
        "name": "Album",
        "artists": {"items": [artist]},
        "coverArt": {
            "sources": [{"url": "https://i.scdn.co/image/test"}, {"url": "https://evil.test/token"}]
        },
        "date": {"year": 2024},
    }
    track = {
        "uri": "spotify:track:" + "C" * 22,
        "name": "Song",
        "artists": {"items": [artist]},
        "albumOfTrack": album,
        "duration": {"totalMilliseconds": 123000},
        "externalIds": {"isrc": "USUM71703861"},
        "private": "secret",
    }
    return {
        "data": {
            "searchV2": {
                "tracksV2": {"items": [{"item": {"data": track}}]},
                "artists": {"items": [{"data": artist}]},
                "albumsV2": {"items": [{"data": album}]},
                "playlists": {"private": "secret"},
            }
        },
        "cookie": "secret",
    }


class Client:
    def __init__(self):
        self.calls = []
        self.error = None
        self.entered = threading.Event()
        self.release = None

    def search_catalog(self, *args):
        self.calls.append(args)
        self.entered.set()
        if self.release:
            assert self.release.wait(5)
        if self.error:
            raise self.error
        return catalog()


@pytest.mark.parametrize(
    "patch",
    [
        {"query": "x"},
        {"query": "a" * 201},
        {"query": "a\nb"},
        {"query": 2},
        {"limit": True},
        {"limit": 0},
        {"limit": 26},
        {"limit": 1.0},
        {"country": "ZZ"},
        {"country": "it"},
        {"url": "https://evil.test"},
    ],
)
def test_validation(patch):
    with pytest.raises(ValueError):
        validate_catalog_request(REQUEST | patch)


def test_exact_fields():
    with pytest.raises(ValueError):
        validate_catalog_request({"query": "ok", "limit": 2})
    assert validate_catalog_request(REQUEST) == ("public song", 12, "IT")


def test_sanitization():
    result = sanitize_catalog(catalog(), 12)
    search = result["data"]["searchV2"]
    assert set(search) == {"tracksV2", "artists", "albumsV2", "topResultsV2"}
    assert search["tracksV2"]["items"][0]["item"]["data"]["externalIds"]["isrc"] == "USUM71703861"
    assert "secret" not in str(result) and "evil.test" not in str(result)
    assert search["topResultsV2"] == {"itemsV2": []}
    payload = catalog()
    payload["data"]["searchV2"]["tracksV2"]["items"][0]["item"]["data"]["uri"] = (
        "spotify:playlist:" + "C" * 22
    )
    assert sanitize_catalog(payload, 12)["data"]["searchV2"]["tracksV2"]["items"] == []


@pytest.mark.parametrize(
    "url",
    [
        "http://i.scdn.co/test",
        "https://i.scdn.co.evil.test/test",
        "https://secret@i.scdn.co/test",
        "https://i.scdn.co:443/test",
        "https://i.scdn.co/test?token=secret",
        "https://[invalid/test",
    ],
)
def test_image_allowlist(url):
    payload = catalog()
    payload["data"]["searchV2"]["albumsV2"]["items"][0]["data"]["coverArt"]["sources"] = [{"url": url}]
    assert (
        sanitize_catalog(payload, 12)["data"]["searchV2"]["albumsV2"]["items"][0]["data"]["coverArt"][
            "sources"
        ]
        == []
    )


def test_cache_ttl_lru_and_country():
    clock = [0]
    client = Client()
    service = CatalogSearchService(client, ttl=2, max_entries=1, clock=lambda: clock[0])
    first = service.search(REQUEST)
    first.clear()
    assert service.search(REQUEST)["data"]
    assert len(client.calls) == 1
    service.search(REQUEST | {"country": "US"})
    service.search(REQUEST)
    assert len(client.calls) == 3
    clock[0] = 3
    service.search(REQUEST)
    assert len(client.calls) == 4


def test_singleflight_and_concurrency():
    client = Client()
    client.release = threading.Event()
    service = CatalogSearchService(client, concurrency=1)
    with ThreadPoolExecutor(3) as executor:
        one = executor.submit(service.search, REQUEST)
        assert client.entered.wait(2)
        two = executor.submit(service.search, REQUEST)
        with pytest.raises(SpotifySearchUnavailable):
            service.search(REQUEST | {"query": "other song"})
        client.release.set()
        assert one.result(5) == two.result(5)
    assert len(client.calls) == 1
    assert service.search(REQUEST | {"query": "other song"})


def test_global_quota_and_retry_after():
    clock = [0]
    client = Client()
    service = CatalogSearchService(client, quota=1, clock=lambda: clock[0])
    service.search(REQUEST)
    with pytest.raises(SpotifySearchUnavailable):
        service.search(REQUEST | {"query": "other song"})
    clock[0] = 60
    error = SpotifySearchUnavailable("limited")
    error.retry_after = 120
    client.error = error
    with pytest.raises(SpotifySearchUnavailable):
        service.search(REQUEST | {"query": "other song"})
    clock[0] = 121
    with pytest.raises(SpotifySearchUnavailable):
        service.search(REQUEST | {"query": "third song"})
    assert len(client.calls) == 2


def test_errors_are_not_empty_or_cached():
    client = Client()
    service = CatalogSearchService(client)
    client.error = requests.Timeout("secret")
    with pytest.raises(requests.Timeout):
        service.search(REQUEST)
    client.error = None
    assert service.search(REQUEST)["data"]
    assert len(client.calls) == 2
    with pytest.raises(SpotifySearchUnavailable):
        sanitize_catalog({"data": {}}, 12)


@contextmanager
def server(client, **kwargs):
    http = create_resolver_server(CanvasResolverService(client), port=0, client_key="test-key", **kwargs)
    thread = threading.Thread(target=http.serve_forever, daemon=True)
    thread.start()
    try:
        yield f"http://127.0.0.1:{http.server_port}"
    finally:
        http.shutdown()
        http.server_close()
        thread.join()


def test_http_auth_routes_validation_and_failure():
    client = Client()
    with server(client) as base:
        endpoint = base + "/v1/catalog/search"
        assert requests.post(endpoint, json=REQUEST).status_code == 401
        assert not client.calls
        headers = {"X-Levyra-Key": "test-key"}
        assert requests.get(base + "/health").status_code == 200
        assert requests.get(endpoint).status_code == 404
        assert requests.post(base + "/v1/account", headers=headers, json=REQUEST).status_code == 404
        assert (
            requests.post(endpoint, headers=headers, json=REQUEST | {"url": "https://evil.test"}).status_code
            == 400
        )
        assert requests.post(endpoint, headers=headers, data="x" * 16385).status_code == 413
        assert requests.post(base + "/v1/resolve", headers=headers, json={}).status_code == 400
        response = requests.post(endpoint, headers=headers, json=REQUEST)
        assert response.status_code == 200 and response.json()["data"]
        client.error = requests.Timeout("secret cookie")
        response = requests.post(endpoint, headers=headers, json=REQUEST | {"query": "uncached"})
        assert response.status_code == 503
        assert int(response.headers["Retry-After"]) >= 5
        assert "secret" not in response.text
        client.error = ValueError("secret upstream data")
        response = requests.post(endpoint, headers=headers, json=REQUEST | {"query": "uncached value error"})
        assert response.status_code == 503 and "secret" not in response.text


def test_http_rate_limit():
    with server(Client(), rate_limit=1) as base:
        endpoint = base + "/v1/catalog/search"
        headers = {"X-Levyra-Key": "test-key"}
        assert requests.post(endpoint, headers=headers, json=REQUEST).status_code == 200
        response = requests.post(endpoint, headers=headers, json=REQUEST)
        assert response.status_code == 429 and int(response.headers["Retry-After"]) > 0


class Response:
    def __init__(self, status=200, payload=None, headers=None):
        import json

        self.status_code = status
        self.headers = headers or {}
        self.body = json.dumps(payload if payload is not None else catalog()).encode()
        self.closed = False

    def iter_content(self, chunk_size):
        yield self.body

    def close(self):
        self.closed = True


class Session:
    def __init__(self, responses):
        self.responses = responses
        self.calls = []

    def post(self, url, **kwargs):
        self.calls.append((url, kwargs))
        return self.responses.pop(0)


def authenticated_client(session):
    client = SpotifyWebClient("synthetic-cookie-for-tests-only", session=session)
    client._access_token = "synthetic-token"
    return client


def test_upstream_authenticated_refresh_and_public_operation(monkeypatch):
    rejected, accepted = Response(401), Response()
    session = Session([rejected, accepted])
    client = authenticated_client(session)
    refreshes = []

    def refresh():
        refreshes.append(True)
        client._access_token = "refreshed-token"

    monkeypatch.setattr(client, "authenticate", refresh)
    assert client.search_catalog("song", 12, "IT")["data"]
    assert refreshes == [True] and rejected.closed and accepted.closed
    assert len(session.calls) == 2
    for _url, options in session.calls:
        assert options["json"]["operationName"] == "searchDesktop"
        assert options["json"]["variables"]["includeAudiobooks"] is False
        assert options["json"]["variables"]["numberOfTopResults"] >= 1
        assert options["headers"]["Spotify-Country"] == "IT"
        assert options["allow_redirects"] is False and options["stream"] is True
        assert "Cookie" not in options["headers"]


@pytest.mark.parametrize(
    "response",
    [
        Response(429, headers={"Retry-After": "120"}),
        Response(500),
        Response(302),
        Response(payload={"errors": [{"message": "PersistedQueryNotFound"}]}),
        Response(headers={"Content-Length": str(3 * 1024 * 1024)}),
    ],
)
def test_upstream_unavailable(response):
    session = Session([response])
    with pytest.raises(Exception) as raised:
        authenticated_client(session).search_catalog("song", 12, "IT")
    assert response.closed and len(session.calls) == 1
    if response.status_code == 429:
        assert raised.value.retry_after == 120
