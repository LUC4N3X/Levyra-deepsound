#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    content = read(path)
    count = content.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one exact match, found {count}: {old[:120]!r}")
    write(path, content.replace(old, new, 1))


def replace_all_exact(path: str, old: str, new: str, expected: int) -> None:
    content = read(path)
    count = content.count(old)
    if count != expected:
        raise RuntimeError(f"{path}: expected {expected} matches, found {count}: {old[:120]!r}")
    write(path, content.replace(old, new))


# 1/2 + query budget: variant matching must use tokens/phrases and exempt canonical title variants.
path = "tools/levyra-editorial/levyra_editorial/youtube_music.py"
replace_once(path,
'''FORBIDDEN_OFFICIAL_VIDEO_VARIANT = re.compile(
    r"(?i)(?:^|[^a-z0-9])(?:live(?:\\s+performance)?|performance|concert|festival|acoustic|session|stage|ceremony|halftime|half\\s+time|award\\s+show|tour)(?:$|[^a-z0-9])"
)
''',
'''OFFICIAL_VIDEO_VARIANT_TERMS = {
    "live",
    "live performance",
    "performance",
    "concert",
    "festival",
    "acoustic",
    "session",
    "stage",
    "ceremony",
    "halftime",
    "half time",
    "award show",
    "tour",
}
''')
replace_once(path,
'''def _contains_unrequested_variant(target_title: str, candidate: Mapping[str, Any]) -> bool:
    target_blob = _text_key(target_title)
    candidate_blob = _text_key(f"{candidate.get('title') or ''} {candidate.get('album') or ''}")
    return any(term in candidate_blob and term not in target_blob for term in HARD_VARIANT_TERMS)
''',
'''def _matched_variant_terms(value: str, terms: set[str]) -> set[str]:
    normalized = _text_key(value)
    tokens = _tokens(normalized)
    return {
        term
        for term in terms
        if (term in normalized if " " in term else term in tokens)
    }


def _contains_unrequested_variant(target_title: str, candidate: Mapping[str, Any]) -> bool:
    target_terms = _matched_variant_terms(target_title, HARD_VARIANT_TERMS)
    candidate_terms = _matched_variant_terms(
        f"{candidate.get('title') or ''} {candidate.get('album') or ''}",
        HARD_VARIANT_TERMS,
    )
    return bool(candidate_terms - target_terms)
''')
replace_once(path,
'''    if FORBIDDEN_OFFICIAL_VIDEO_VARIANT.search(title_key):
        return None
''',
'''    target_variants = _matched_variant_terms(title, OFFICIAL_VIDEO_VARIANT_TERMS)
    candidate_variants = _matched_variant_terms(title_key, OFFICIAL_VIDEO_VARIANT_TERMS)
    if candidate_variants - target_variants:
        return None
''')
replace_once(path,
'''        maximum = max(90_000, round(duration_ms * 0.40))
''',
'''        maximum = max(45_000, round(duration_ms * 0.20))
''')
replace_once(path,
'''        self._cache: dict[str, dict[str, Any] | None] = {}
        self._query_count = 0
        self._max_queries = max(1, int(os.environ.get("LEVYRA_EDITORIAL_YTM_MAX_QUERIES", "700")))
''',
'''        self._cache: dict[str, dict[str, Any] | None] = {}
        self._request_count = 0
        legacy_recording_budget = max(
            1,
            int(os.environ.get("LEVYRA_EDITORIAL_YTM_MAX_QUERIES", "1000")),
        )
        self._max_requests = max(
            1,
            int(
                os.environ.get(
                    "LEVYRA_EDITORIAL_YTM_MAX_REQUESTS",
                    str(legacy_recording_budget * 2),
                )
            ),
        )
''')
replace_once(path,
'''    def close(self) -> None:
        self._session.close()

    def _authorization(self) -> str:
''',
'''    def close(self) -> None:
        self._session.close()

    def _reserve_request(self) -> bool:
        with self._cache_lock:
            if self._request_count >= self._max_requests:
                return False
            self._request_count += 1
            return True

    def _authorization(self) -> str:
''')
replace_once(path,
'''        with self._cache_lock:
            if cache_key in self._cache:
                return self._cache[cache_key]
            if self._query_count >= self._max_queries:
                self._cache[cache_key] = None
                return None
            self._query_count += 1

        audio_result: dict[str, Any] | None = None
        try:
            payload = self._search(f"{title} {artist}")
''',
'''        with self._cache_lock:
            if cache_key in self._cache:
                return self._cache[cache_key]

        if not self._reserve_request():
            with self._cache_lock:
                self._cache[cache_key] = None
            return None

        audio_result: dict[str, Any] | None = None
        try:
            payload = self._search(f"{title} {artist}")
''')
replace_once(path,
'''        official_video: dict[str, Any] | None = None
        if verified_audio is not None:
            try:
                official_video = self._resolve_official_video(title, artist, duration_ms)
''',
'''        official_video: dict[str, Any] | None = None
        if verified_audio is not None and self._reserve_request():
            try:
                official_video = self._resolve_official_video(title, artist, duration_ms)
''')

# Python regressions.
path = "tools/levyra-editorial/tests/test_youtube_music.py"
append = r'''


def test_variant_terms_use_word_boundaries_in_titles_and_albums() -> None:
    cases = [
        ("One More Time", "Discovery"),
        ("One More Time", "Alive 2007"),
        ("One More Time", "Undercover"),
    ]
    for title, album in cases:
        mapping = select_youtube_music_mapping(
            title,
            "Daft Punk",
            320_000,
            [
                {
                    "videoId": "Audio123456",
                    "title": title,
                    "artist": "Daft Punk",
                    "album": album,
                    "durationMs": 320_000,
                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV",
                }
            ],
        )
        assert mapping is not None
        assert mapping["audioVideoId"] == "Audio123456"


def test_official_video_allows_variant_word_when_it_is_in_canonical_title() -> None:
    mapping = select_official_youtube_video(
        "Live Forever",
        "Oasis",
        260_000,
        [
            {
                "videoId": "Video123456",
                "title": "Oasis - Live Forever (Official Video)",
                "owner": "Oasis",
                "channelId": "UCOfficial",
                "durationMs": 260_000,
                "verifiedArtist": True,
            }
        ],
    )
    assert mapping is not None
    assert mapping["videoId"] == "Video123456"


def test_official_video_still_rejects_unrequested_live_variant() -> None:
    mapping = select_official_youtube_video(
        "Forever",
        "Exact Artist",
        180_000,
        [
            {
                "videoId": "Video123456",
                "title": "Exact Artist - Forever Live (Official Video)",
                "owner": "Exact Artist",
                "channelId": "UCOfficial",
                "durationMs": 180_000,
                "verifiedArtist": True,
            }
        ],
    )
    assert mapping is None
'''
content = read(path)
if "test_variant_terms_use_word_boundaries_in_titles_and_albums" in content:
    raise RuntimeError("python regressions already present")
write(path, content.rstrip() + append + "\n")

# 16: type-safe ISRC validation in both live and publish jobs.
replace_all_exact(
    ".github/workflows/editorial-catalog.yml",
    '''if jq -e 'any(.. | objects | select(has("isrc")); (.isrc | test("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$") | not))' build/editorial/catalog.json >/dev/null; then''',
    '''if jq -e 'any(.. | objects | select(has("isrc")); ((.isrc | type) != "string" or (.isrc | test("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$") | not)))' build/editorial/catalog.json >/dev/null; then''',
    2,
)


# Parser: stable catalog row IDs and audio URL, so duplicate YouTube mappings cannot delete rows.
path = "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt"
replace_once(path,
'''            val identity = chartIdentity("$title|$artist")
            val palette = PALETTES[identity.seed % PALETTES.size]
''',
'''            val identity = chartIdentity("$title|$artist")
            val catalogTrackId = publishedCatalogTrackId(item.optString("id"))
                .ifBlank { "chart-${identity.id}" }
            val palette = PALETTES[identity.seed % PALETTES.size]
''')
replace_once(path,
'''                id = youtubePlaybackId.ifBlank { "chart-${identity.id}" },
''',
'''                id = catalogTrackId,
''')
replace_once(path,
'''                videoUrl = "",
''',
'''                videoUrl = youtubePlaybackId
                    .takeIf(String::isNotBlank)
                    ?.let { "https://www.youtube.com/watch?v=$it" }
                    .orEmpty(),
''')
replace_once(path,
'''    private fun publishedYoutubeVideoId(value: String?): String {
''',
'''    private fun publishedCatalogTrackId(value: String?): String {
        val normalized = value.orEmpty().trim()
        return normalized.takeIf {
            it.length in 1..128 && it.matches(Regex("[A-Za-z0-9_-]+"))
        }.orEmpty()
    }

    private fun publishedYoutubeVideoId(value: String?): String {
''')

# Parser tests.
path = "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt"
replace_once(path,
'''        assertEquals("Audio123456", track.id)
        assertEquals("", track.videoUrl)
''',
'''        assertTrue(track.id.startsWith("chart-"))
        assertEquals("https://www.youtube.com/watch?v=Audio123456", track.videoUrl)
''')
insert_before = '''    @Test
    fun validatesArtworkHostAndScheme() {
'''
new_test = '''    @Test
    fun keepsDistinctChartRowsThatShareTheSameAudioMapping() {
        val body = catalog(
            collections = collection(
                "IT",
                track(
                    title = "First",
                    youtubeMusic = """{
                        "audioVideoId": "Audio123456",
                        "audioConfidence": 99
                    }"""
                ) + "," + track(
                    title = "Second",
                    youtubeMusic = """{
                        "audioVideoId": "Audio123456",
                        "audioConfidence": 99
                    }"""
                )
            )
        )

        val tracks = EditorialCatalogParser.parse(body, loadedAt = 0L)!!
            .byMarket.getValue("IT")

        assertEquals(2, tracks.size)
        assertEquals(2, tracks.map { it.id }.distinct().size)
        assertTrue(tracks.all { it.videoUrl.endsWith("Audio123456") })
    }

'''
replace_once(path, insert_before, new_test + insert_before)


# Regex compatibility scan: do not mistake // inside Kotlin/Java strings for comments.
write(
    "scripts/check_android_regex_compatibility.py",
r'''#!/usr/bin/env python3
"""Reject host-only Java regex features, including inline ``(?U)``, before Android builds."""

from __future__ import annotations

import re
import sys
from pathlib import Path

SOURCE_ROOTS = (Path("app/src/main"), Path("third_party/LevyraNexus/src/main"))
SOURCE_SUFFIXES = {".kt", ".java"}
INLINE_FLAGS = re.compile(r"\(\?([idmsuxU-]+)(?::|\))")
UNSUPPORTED_CONSTANTS = (
    "Pattern.CANON_EQ",
    "Pattern.UNICODE_CHARACTER_CLASS",
)


def strip_comments(text: str) -> str:
    output: list[str] = []
    index = 0
    state = "code"
    while index < len(text):
        if state == "code":
            if text.startswith("//", index):
                output.extend((" ", " "))
                index += 2
                state = "line_comment"
            elif text.startswith("/*", index):
                output.extend((" ", " "))
                index += 2
                state = "block_comment"
            elif text.startswith('"""', index):
                output.append('"""')
                index += 3
                state = "triple_string"
            elif text[index] == '"':
                output.append(text[index])
                index += 1
                state = "string"
            elif text[index] == "'":
                output.append(text[index])
                index += 1
                state = "char"
            else:
                output.append(text[index])
                index += 1
        elif state == "line_comment":
            if text[index] == "\n":
                output.append("\n")
                state = "code"
            else:
                output.append(" ")
            index += 1
        elif state == "block_comment":
            if text.startswith("*/", index):
                output.extend((" ", " "))
                index += 2
                state = "code"
            else:
                output.append("\n" if text[index] == "\n" else " ")
                index += 1
        elif state == "triple_string":
            if text.startswith('"""', index):
                output.append('"""')
                index += 3
                state = "code"
            else:
                output.append(text[index])
                index += 1
        else:
            current = text[index]
            output.append(current)
            index += 1
            if current == "\\" and index < len(text):
                output.append(text[index])
                index += 1
            elif state == "string" and current == '"':
                state = "code"
            elif state == "char" and current == "'":
                state = "code"
    return "".join(output)


def main() -> int:
    findings: list[str] = []
    for root in SOURCE_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path.suffix not in SOURCE_SUFFIXES:
                continue
            source = strip_comments(path.read_text(encoding="utf-8"))
            for line_number, line in enumerate(source.splitlines(), start=1):
                for match in INLINE_FLAGS.finditer(line):
                    if "U" in match.group(1):
                        findings.append(
                            f"{path}:{line_number}: Android does not support the inline Unicode "
                            f"character-class flag: {match.group(0)}"
                        )
                for constant in UNSUPPORTED_CONSTANTS:
                    if constant in line:
                        findings.append(
                            f"{path}:{line_number}: Android does not support {constant}."
                        )

    if findings:
        print("Android regex compatibility check failed:", file=sys.stderr)
        print("\n".join(f"- {finding}" for finding in findings), file=sys.stderr)
        return 1

    print("Android regex compatibility check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
''')


for temporary in (
    ".github/workflows/apply-editorial-audit-fixes.yml",
    "tools/apply_editorial_audit_fixes.py",
):
    target = ROOT / temporary
    if target.exists():
        target.unlink()

print("Applied editorial audit fixes.")
