from __future__ import annotations

import copy
import re
import threading
import time
from collections import OrderedDict, deque
from collections.abc import Mapping
from concurrent.futures import Future
from typing import Any
from urllib.parse import urlparse

from .spotify import SpotifySearchUnavailable

COUNTRIES = frozenset(
    [
        "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT",
        "AU", "AW", "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI",
        "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY",
        "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
        "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM",
        "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK",
        "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL",
        "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
        "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR",
        "IS", "IT", "JE", "JM", "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN",
        "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS",
        "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK",
        "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW",
        "MX", "MY", "MZ", "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP",
        "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM",
        "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW",
        "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM",
        "SN", "SO", "SR", "SS", "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF",
        "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO", "TR", "TT", "TV", "TW",
        "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
        "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW",
    ]
)


def validate_catalog_request(data: Mapping[str, Any]) -> tuple[str, int, str]:
    if set(data) != {"query", "limit", "country"}:
        raise ValueError("Exactly query, limit and country are required")
    query, limit, country = data["query"], data["limit"], data["country"]
    if not isinstance(query, str) or not 2 <= len(query.strip()) <= 200 or any(ord(c) < 32 for c in query):
        raise ValueError("query must contain 2..200 characters")
    if type(limit) is not int or not 1 <= limit <= 25:
        raise ValueError("limit must be an integer in 1..25")
    if not isinstance(country, str) or country not in COUNTRIES:
        raise ValueError("country must be an ISO 3166-1 alpha-2 code")
    return query.strip(), limit, country


def _node(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _text(value: Any) -> str:
    return value.strip()[:500] if isinstance(value, str) else ""


def _images(value: Any) -> list[dict[str, Any]]:
    output = []
    for source in value[:8] if isinstance(value, list) else []:
        source = _node(source)
        url = _text(source.get("url"))
        try:
            parsed = urlparse(url)
            valid = (
                parsed.scheme == "https"
                and parsed.hostname
                in {
                    "i.scdn.co",
                    "mosaic.scdn.co",
                    "image-cdn-ak.spotifycdn.com",
                    "image-cdn-fa.spotifycdn.com",
                }
                and not parsed.username
                and not parsed.password
                and parsed.port is None
                and not parsed.query
                and not parsed.fragment
            )
        except ValueError:
            valid = False
        if not valid:
            continue
        image = {"url": url}
        for dimension in ("width", "height"):
            number = source.get(dimension)
            if type(number) is int and 0 < number <= 20000:
                image[dimension] = number
        output.append(image)
    return output


def _identity(data: Mapping[str, Any], kind: str) -> dict[str, str] | None:
    uri = _text(data.get("uri"))
    if re.fullmatch(rf"spotify:{kind}:[A-Za-z0-9]{{22}}", uri) is None:
        return None
    return {"uri": uri, "id": uri.rsplit(":", 1)[1], "__typename": kind.capitalize()}


def _artist(data: Mapping[str, Any]) -> dict[str, Any] | None:
    result = _identity(data, "artist")
    name = _text(_node(data.get("profile")).get("name"))
    if not result or not name:
        return None
    result["profile"] = {"name": name}
    sources = _node(_node(data.get("visuals")).get("avatarImage")).get("sources")
    result["visuals"] = {"avatarImage": {"sources": _images(sources)}}
    return result


def _artists(value: Any) -> dict[str, Any]:
    items = _node(value).get("items")
    return (
        {"items": [result for item in items[:25] if (result := _artist(_node(item)))]}
        if isinstance(items, list)
        else {"items": []}
    )


def _album(data: Mapping[str, Any]) -> dict[str, Any] | None:
    result = _identity(data, "album")
    name = _text(data.get("name"))
    if not result or not name:
        return None
    result.update(
        name=name,
        artists=_artists(data.get("artists")),
        coverArt={"sources": _images(_node(data.get("coverArt")).get("sources"))},
    )
    date = _node(data.get("date"))
    clean_date = {}
    year = date.get("year")
    if type(year) is int and 1000 <= year <= 9999:
        clean_date["year"] = year
    iso = _text(date.get("isoString"))
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}(?:T[0-9:.]+Z)?", iso):
        clean_date["isoString"] = iso
    if clean_date:
        result["date"] = clean_date
    return result


def _track(data: Mapping[str, Any]) -> dict[str, Any] | None:
    result = _identity(data, "track")
    name = _text(data.get("name"))
    if not result or not name:
        return None
    result.update(name=name, artists=_artists(data.get("artists")))
    album = _album(_node(data.get("albumOfTrack")))
    if album:
        result["albumOfTrack"] = album
    duration = _node(data.get("duration")).get("totalMilliseconds")
    if type(duration) is int and 0 <= duration <= 86400000:
        result["duration"] = {"totalMilliseconds": duration}
    rating = _node(data.get("contentRating")).get("label")
    if rating in {"EXPLICIT", "NONE", "CLEAN"}:
        result["contentRating"] = {"label": rating}
    isrc = _text(_node(data.get("externalIds")).get("isrc"))
    if re.fullmatch(r"[A-Z]{2}[A-Z0-9]{3}[0-9]{7}", isrc):
        result["externalIds"] = {"isrc": isrc}
    return result


def sanitize_catalog(payload: Mapping[str, Any], limit: int) -> dict[str, Any]:
    search = _node(_node(payload.get("data")).get("searchV2"))
    output: dict[str, Any] = {"topResultsV2": {"itemsV2": []}}
    for name, converter in (("tracksV2", _track), ("artists", _artist), ("albumsV2", _album)):
        items = _node(search.get(name)).get("items")
        if not isinstance(items, list):
            raise SpotifySearchUnavailable("Catalog response unavailable")
        clean = []
        for item in items[:limit]:
            wrapper = _node(item)
            data = (
                _node(_node(wrapper.get("item")).get("data"))
                if name == "tracksV2"
                else _node(wrapper.get("data"))
            )
            if result := converter(data):
                clean.append({"item": {"data": result}} if name == "tracksV2" else {"data": result})
        output[name] = {"items": clean}
    return {"data": {"searchV2": output}}


class CatalogSearchService:
    def __init__(
        self,
        client: Any,
        *,
        ttl: float = 120,
        max_entries: int = 256,
        quota: int = 60,
        concurrency: int = 2,
        clock: Any = time.monotonic,
    ) -> None:
        if ttl <= 0 or min(max_entries, quota, concurrency) < 1:
            raise ValueError("Catalog limits must be positive")
        self.client = client
        self.ttl, self.max_entries, self.quota, self.clock = ttl, max_entries, quota, clock
        self._lock = threading.Lock()
        self._cache: OrderedDict[tuple[str, int, str], tuple[float, dict[str, Any]]] = OrderedDict()
        self._flights: dict[tuple[str, int, str], Future] = {}
        self._queries: deque[float] = deque()
        self._slots = threading.BoundedSemaphore(concurrency)
        self._retry_at = 0.0

    def search(self, data: Mapping[str, Any]) -> dict[str, Any]:
        key = validate_catalog_request(data)
        with self._lock:
            now = self.clock()
            cached = self._cache.get(key)
            if cached and cached[0] > now:
                self._cache.move_to_end(key)
                return copy.deepcopy(cached[1])
            self._cache.pop(key, None)
            future = self._flights.get(key)
            owner = future is None
            if owner:
                while self._queries and self._queries[0] <= now - 60:
                    self._queries.popleft()
                if (
                    now < self._retry_at
                    or len(self._queries) >= self.quota
                    or not self._slots.acquire(blocking=False)
                ):
                    raise SpotifySearchUnavailable("Catalog temporarily unavailable")
                self._queries.append(now)
                future = Future()
                self._flights[key] = future
        if not owner:
            return copy.deepcopy(future.result(timeout=30))
        try:
            result = sanitize_catalog(self.client.search_catalog(*key), key[1])
            with self._lock:
                self._cache[key] = (self.clock() + self.ttl, result)
                while len(self._cache) > self.max_entries:
                    self._cache.popitem(last=False)
            future.set_result(result)
            return copy.deepcopy(result)
        except Exception as error:
            with self._lock:
                self._retry_at = max(self._retry_at, self.clock() + getattr(error, "retry_after", 0))
            future.set_exception(error)
            raise
        finally:
            with self._lock:
                self._flights.pop(key, None)
            self._slots.release()
