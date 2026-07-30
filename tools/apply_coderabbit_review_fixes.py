from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one anchor, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


# Locale-invariant ISRC normalization and regression coverage.
write(
    "app/src/main/java/com/luc4n3x/levyra/data/RecordingIdentity.kt",
    '''package com.luc4n3x.levyra.data

import java.util.Locale

internal enum class RecordingIdentityMatch {
    Exact,
    Conflict,
    Unknown
}

internal fun normalizedIsrc(value: String): String = value
    .uppercase(Locale.ROOT)
    .filter(Char::isLetterOrDigit)
    .takeIf { it.matches(Regex("[A-Z]{2}[A-Z0-9]{3}[0-9]{7}")) }
    .orEmpty()

internal fun recordingIdentityMatch(reference: String, candidate: String): RecordingIdentityMatch {
    val expected = normalizedIsrc(reference)
    val actual = normalizedIsrc(candidate)
    if (expected.isBlank() || actual.isBlank()) return RecordingIdentityMatch.Unknown
    return if (expected == actual) RecordingIdentityMatch.Exact else RecordingIdentityMatch.Conflict
}
''',
)

recording_test = "app/src/test/java/com/luc4n3x/levyra/data/RecordingIdentityTest.kt"
replace_once(
    recording_test,
    "import org.junit.Test\n",
    "import org.junit.Test\nimport java.util.Locale\n",
)
replace_once(
    recording_test,
    "    @Test\n    fun exactIsrcWinsBeforeTextMatching() {\n",
    '''    @Test
    fun normalizationIsLocaleInvariantForTurkishDevices() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("TRI012345678", normalizedIsrc("tr-i01-23-45678"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun exactIsrcWinsBeforeTextMatching() {
''',
)

# Normalize both provider values and every localized constant through one representation.
write(
    "app/src/main/java/com/luc4n3x/levyra/domain/ReleaseType.kt",
    '''package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

enum class ReleaseType {
    Album,
    Single,
    Compilation,
    Ep,
    Unknown
}

val ReleaseType.isFullAlbum: Boolean
    get() = this == ReleaseType.Album

val ReleaseType.isSingleLike: Boolean
    get() = this == ReleaseType.Single || this == ReleaseType.Ep

private fun normalizeReleaseLabel(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun normalizedLabelSet(vararg labels: String): Set<String> =
    labels.mapTo(linkedSetOf(), ::normalizeReleaseLabel)

fun releaseTypeFromProviderLabel(value: String): ReleaseType {
    val normalized = normalizeReleaseLabel(value)
    if (normalized.isBlank()) return ReleaseType.Unknown
    val tokens = normalized.split(' ').filter(String::isNotBlank).toSet()
    return when {
        normalized == "ep" || "ep" in tokens || normalized.contains("extended play") -> ReleaseType.Ep
        COMPILATION_LABELS.any { label ->
            normalized == label || label in tokens || normalized.contains(label)
        } -> ReleaseType.Compilation
        SINGLE_LABELS.any { label -> normalized == label || label in tokens } -> ReleaseType.Single
        ALBUM_LABELS.any { label -> normalized == label || label in tokens } -> ReleaseType.Album
        else -> ReleaseType.Unknown
    }
}

private val ALBUM_LABELS = normalizedLabelSet(
    "album", "albumo", "alben", "albom", "albumes", "albumi", "专辑", "專輯", "アルバム", "앨범",
    "अल्बम", "อัลบั้ม", "אלבום", "ألبوم"
)

private val SINGLE_LABELS = normalizedLabelSet(
    "single", "singolo", "singoli", "sencillo", "sencillos", "singl", "singel", "單曲", "单曲",
    "シングル", "싱글", "एकल", "ซิงเกิล", "סינגל", "أغنية منفردة"
)

private val COMPILATION_LABELS = normalizedLabelSet(
    "compilation", "compilations", "raccolta", "raccolte", "anthology", "best of", "greatest hits", "合集",
    "合輯", "コンピレーション", "컴필레이션", "संकलन", "รวมเพลง", "אוסף", "تجميع"
)
''',
)

release_test = "app/src/test/java/com/luc4n3x/levyra/domain/ReleaseTypeTest.kt"
replace_once(
    release_test,
    "        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel(\"Album\"))\n",
    '''        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("Album"))
        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("ألبوم"))
        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("อัลบั้ม"))
        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("अल्बम"))
''',
)

# Album-specific search must never resolve to singles, EPs, or compilations.
yt_repo = "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt"
yt_text = read(yt_repo)
old_album_guard = "        if (releaseType == ReleaseType.Unknown) return null\n"
if yt_text.count(old_album_guard) != 2:
    raise RuntimeError(f"{yt_repo}: expected two album parser guards, found {yt_text.count(old_album_guard)}")
write(yt_repo, yt_text.replace(old_album_guard, "        if (releaseType != ReleaseType.Album) return null\n", 2))

# Add a real localized strings-contract entry for all 26 supported languages.
strings_path = "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraStrings.kt"
replace_once(
    strings_path,
    '    val singlesAndEps: String get() = value("singlesAndEps")\n',
    '    val singlesAndEps: String get() = value("singlesAndEps")\n'
    '    val compilations: String get() = value("compilations")\n',
)

translations = {
    "en": "Compilations",
    "it": "Raccolte",
    "es": "Recopilaciones",
    "fr": "Compilations",
    "de": "Kompilationen",
    "pt": "Compilações",
    "nl": "Verzamelalbums",
    "pl": "Kompilacje",
    "ro": "Compilații",
    "el": "Συλλογές",
    "sv": "Samlingar",
    "da": "Opsamlinger",
    "cs": "Kompilace",
    "uk": "Збірки",
    "ru": "Сборники",
    "tr": "Derlemeler",
    "ar": "ألبومات تجميعية",
    "zh": "合辑",
    "ja": "コンピレーション",
    "ko": "컴필레이션",
    "hi": "संकलन",
    "id": "Kompilasi",
    "vi": "Tuyển tập",
    "th": "อัลบั้มรวมเพลง",
    "fil": "Mga compilation",
    "he": "אוספים",
}
localization_files = (
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraAdditionalStrings.kt",
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraFilipinoStrings.kt",
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraHebrewStrings.kt",
)
seen_codes: set[str] = set()
function_pattern = re.compile(r"\bfun\s+([a-z]+)LocalizationEntries\s*\(")
for path in localization_files:
    output: list[str] = []
    current_code: str | None = None
    for line in read(path).splitlines(keepends=True):
        match = function_pattern.search(line)
        if match:
            current_code = match.group(1)
        output.append(line)
        if '"singlesAndEps" to ' in line:
            if current_code not in translations:
                raise RuntimeError(f"{path}: no compilation translation for {current_code!r}")
            indent = line[: len(line) - len(line.lstrip())]
            newline = "\r\n" if line.endswith("\r\n") else "\n"
            output.append(f'{indent}"compilations" to "{translations[current_code]}",{newline}')
            seen_codes.add(current_code)
    write(path, "".join(output))
if seen_codes != set(translations):
    missing = sorted(set(translations) - seen_codes)
    unexpected = sorted(seen_codes - set(translations))
    raise RuntimeError(f"Localized compilation coverage mismatch; missing={missing}, unexpected={unexpected}")

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    '                                ArtistSectionTitle(if (strings.code.startsWith("it")) "Raccolte" else "Compilations")\n',
    "                                ArtistSectionTitle(strings.compilations)\n",
)

# Optional central enrichment must degrade safely when the query-limit environment value is malformed.
resilient_path = "tools/levyra-editorial/levyra_editorial/resilient.py"
replace_once(
    resilient_path,
    '''        except YoutubeMusicError as error:
            LOGGER.warning("Central YouTube Music enrichment disabled: %s", error)
''',
    '''        except (YoutubeMusicError, ValueError) as error:
            LOGGER.warning("Central YouTube Music enrichment disabled: %s", error)
''',
)

# Spotify metadata batches get one bounded Retry-After retry, while retaining 401 refresh behavior.
spotify_path = "tools/levyra-editorial/levyra_editorial/spotify.py"
spotify_text = read(spotify_path)
start = spotify_text.index("    def enrich_track_metadata(")
end = spotify_text.index("    def _api_headers(", start)
spotify_method = '''    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        """Best-effort ISRC and release metadata without weakening Pathfinder reads."""
        if self._access_token is None:
            self.authenticate()
        ids = [
            str(item.get("track", {}).get("id") or "").strip()
            for item in items
            if isinstance(item.get("track"), Mapping)
        ]
        missing = [
            track_id
            for track_id in dict.fromkeys(ids)
            if track_id and track_id not in self._track_metadata
        ]
        for offset in range(0, len(missing), 50):
            chunk = missing[offset : offset + 50]
            if not chunk:
                continue

            def request_batch() -> requests.Response:
                return self._session.get(
                    f"{API_BASE_URL}/tracks",
                    params={"ids": ",".join(chunk)},
                    headers=self._api_headers(),
                    timeout=self._timeout,
                )

            try:
                response = request_batch()
                if response.status_code == 401:
                    self.authenticate()
                    response = request_batch()
                if response.status_code == 429:
                    delay = _bounded_retry_after(response.headers.get("Retry-After"))
                    LOGGER.warning(
                        "Spotify track metadata rate-limited; retrying once in %d second(s).",
                        delay,
                    )
                    if delay > 0:
                        time.sleep(delay)
                    response = request_batch()
                    if response.status_code == 401:
                        self.authenticate()
                        response = request_batch()
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

'''
write(spotify_path, spotify_text[:start] + spotify_method + spotify_text[end:])

pathfinder_test = "tools/levyra-editorial/tests/test_pathfinder.py"
pathfinder_text = read(pathfinder_test)
pathfinder_text = pathfinder_text.replace(
    "import pytest\n",
    "import pytest\n\nimport levyra_editorial.spotify as spotify_module\n",
    1,
)
pathfinder_text += '''

class SequencedSession:
    def __init__(self, responses: list[FakeResponse]) -> None:
        self.responses = list(responses)
        self.calls: list[tuple[str, dict[str, Any]]] = []

    def get(self, url: str, **kwargs: Any) -> FakeResponse:
        self.calls.append((url, kwargs))
        return self.responses.pop(0)

    def close(self) -> None:
        return None


def test_track_metadata_enrichment_retries_one_rate_limited_batch(monkeypatch: pytest.MonkeyPatch) -> None:
    session = SequencedSession(
        [
            FakeResponse({}, status_code=429, headers={"Retry-After": "1"}),
            FakeResponse(
                {
                    "tracks": [
                        {
                            "id": "track12345",
                            "external_ids": {"isrc": "ITB002000001"},
                            "album": {
                                "album_type": "album",
                                "total_tracks": 10,
                                "release_date": "2026-07-01",
                            },
                        }
                    ]
                }
            ),
        ]
    )
    waits: list[int] = []
    monkeypatch.setattr(spotify_module.time, "sleep", waits.append)
    client = authenticated_client(session)
    items = [{"track": {"id": "track12345", "album": {}}}]

    enriched = client.enrich_track_metadata(items)

    assert waits == [1]
    assert [url for url, _ in session.calls] == [
        f"{API_BASE_URL}/tracks",
        f"{API_BASE_URL}/tracks",
    ]
    assert enriched[0]["track"]["external_ids"]["isrc"] == "ITB002000001"
    assert enriched[0]["track"]["album"]["album_type"] == "album"
'''
write(pathfinder_test, pathfinder_text)

# Cache a failed YouTube Music bootstrap so one source outage cannot consume the whole CI timeout.
ytm_path = "tools/levyra-editorial/levyra_editorial/youtube_music.py"
replace_once(
    ytm_path,
    "        self._bootstrap_lock = threading.Lock()\n",
    "        self._bootstrap_lock = threading.Lock()\n        self._bootstrap_failed = False\n",
)
replace_once(
    ytm_path,
    '''    def _bootstrap(self) -> None:
        if self._api_key and self._client_version:
            return
        with self._bootstrap_lock:
            if self._api_key and self._client_version:
                return
            response = self._session.get(
                HOME_URL,
                headers={"Cookie": self._cookie, "Origin": ORIGIN, "Referer": HOME_URL},
                timeout=self._timeout,
            )
            response.raise_for_status()
            body = response.text
            key = re.search(r'"INNERTUBE_API_KEY":"([^"\\\\]+)"', body)
            version = re.search(r'"INNERTUBE_CLIENT_VERSION":"([^"\\\\]+)"', body)
            visitor = re.search(r'"VISITOR_DATA":"([^"\\\\]+)"', body)
            if key is None or version is None:
                raise YoutubeMusicError("Unable to bootstrap the central YouTube Music session.")
            self._api_key = key.group(1)
            self._client_version = version.group(1)
            self._visitor_data = visitor.group(1) if visitor else ""
''',
    '''    def _bootstrap(self) -> None:
        if self._api_key and self._client_version:
            return
        if self._bootstrap_failed:
            raise YoutubeMusicError("Central YouTube Music session bootstrap previously failed.")
        with self._bootstrap_lock:
            if self._api_key and self._client_version:
                return
            if self._bootstrap_failed:
                raise YoutubeMusicError("Central YouTube Music session bootstrap previously failed.")
            try:
                response = self._session.get(
                    HOME_URL,
                    headers={"Cookie": self._cookie, "Origin": ORIGIN, "Referer": HOME_URL},
                    timeout=self._timeout,
                )
                response.raise_for_status()
                body = response.text
                key = re.search(r'"INNERTUBE_API_KEY":"([^"\\\\]+)"', body)
                version = re.search(r'"INNERTUBE_CLIENT_VERSION":"([^"\\\\]+)"', body)
                visitor = re.search(r'"VISITOR_DATA":"([^"\\\\]+)"', body)
                if key is None or version is None:
                    raise YoutubeMusicError("Unable to bootstrap the central YouTube Music session.")
                self._api_key = key.group(1)
                self._client_version = version.group(1)
                self._visitor_data = visitor.group(1) if visitor else ""
            except (requests.RequestException, YoutubeMusicError):
                self._bootstrap_failed = True
                raise
''',
)

ytm_test = "tools/levyra-editorial/tests/test_youtube_music.py"
ytm_test_text = read(ytm_test).replace(
    "    YoutubeMusicError,\n",
    "    YoutubeMusicError,\n    YoutubeMusicWebClient,\n",
    1,
)
ytm_test_text += '''

class BootstrapResponse:
    text = "<html>missing innertube configuration</html>"

    def raise_for_status(self) -> None:
        return None


class BootstrapSession:
    def __init__(self) -> None:
        self.headers: dict[str, str] = {}
        self.get_calls = 0

    def get(self, *_args: object, **_kwargs: object) -> BootstrapResponse:
        self.get_calls += 1
        return BootstrapResponse()

    def close(self) -> None:
        return None


def test_bootstrap_failure_is_cached_after_first_attempt() -> None:
    session = BootstrapSession()
    client = YoutubeMusicWebClient(
        "SAPISID=abcdefghijklmnopqrstuvwxyz123456",
        session=session,
    )

    with pytest.raises(YoutubeMusicError, match="bootstrap"):
        client._bootstrap()
    with pytest.raises(YoutubeMusicError, match="previously failed"):
        client._bootstrap()

    assert session.get_calls == 1
'''
write(ytm_test, ytm_test_text)

# Regression for malformed optional query limits: initialization must degrade instead of crashing collection.
resilient_test = "tools/levyra-editorial/tests/test_resilient.py"
resilient_test_text = read(resilient_test)
resilient_test_text = resilient_test_text.replace(
    "from levyra_editorial.resilient import build_resilient_catalog\n",
    "import levyra_editorial.resilient as resilient_module\n"
    "from levyra_editorial.resilient import build_resilient_catalog\n",
    1,
)
resilient_test_text += '''


def test_optional_youtube_music_client_value_error_is_caught(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: object,
) -> None:
    monkeypatch.setenv("LEVYRA_EDITORIAL_YTM_COOKIE", "SAPISID=abcdefghijklmnopqrstuvwxyz123456")
    monkeypatch.setenv("LEVYRA_EDITORIAL_YTM_MAX_QUERIES", "not-a-number")

    spotify = object()
    monkeypatch.setattr(resilient_module, "SpotifyWebClient", lambda _secret: spotify)
    monkeypatch.setattr(resilient_module, "load_config", lambda _path: {"collections": []})

    captured: dict[str, object] = {}

    class FakeClient:
        def __init__(self, spotify_client: object, youtube_client: object | None) -> None:
            captured["spotify"] = spotify_client
            captured["youtube"] = youtube_client

        def close(self) -> None:
            return None

    monkeypatch.setattr(resilient_module, "CentralEditorialClient", FakeClient)
    monkeypatch.setattr(
        resilient_module,
        "build_resilient_catalog",
        lambda _config, _client: (_ for _ in ()).throw(ValueError("stop after initialization")),
    )

    with pytest.raises(ValueError, match="stop after initialization"):
        resilient_module.run_collection(tmp_path, tmp_path)

    assert captured == {"spotify": spotify, "youtube": None}
'''
write(resilient_test, resilient_test_text)
