from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one anchor, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


orbit = "app/src/main/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbit.kt"
replace_once(
    orbit,
    '''    private val artistSeparatorPattern = Regex(
        """(?i)\\b(?:feat|featuring|ft|and|with|e|ed|y|et|und)\\b|[,&;/+]"""
    )''',
    '''    private val artistSeparatorPattern = Regex(
        """(?iU)(?<=\\s)(?:feat\\.?|featuring|ft\\.?|and|with|e|ed|y|et|und)(?=\\s)|[,&;+]"""
    )''',
)
replace_once(
    orbit,
    '''    private data class RecordingFingerprint(
        val isrc: String,
        val title: String,
        val artists: List<String>,
        val durationMs: Long
    ) {
        val primaryArtist: String = artists.firstOrNull().orEmpty()
        val artistSet: Set<String> = artists.toSet()
    }
''',
    '''    private data class RecordingFingerprint(
        val isrc: String,
        val title: String,
        val artists: List<String>,
        val durationMs: Long
    ) {
        val primaryArtist: String = artists.firstOrNull().orEmpty()
        val artistSet: Set<String> = artists.toSet()
    }

    private data class NormalizedTasteSeed(
        val artists: Set<String>,
        val album: String,
        val moodTags: Set<String>
    )

    private data class RankedTrack(
        val track: Track,
        val affinity: Int,
        val metadata: Int,
        val title: String
    )
''',
)
replace_once(
    orbit,
    '''        val tasteSeeds = distinctRecordings(playbackHistory + favoritesPool + restoredOrbit)

        val orderedFallback = fallbackTracks.sortedWith(
            compareByDescending<Track> { tasteAffinity(it, tasteSeeds, normalizedLanguage) }
                .thenByDescending(::recordingMetadataScore)
                .thenBy { normalizedMusicTitle(it.title) }
        )
''',
    '''        val tasteSeeds = distinctRecordings(playbackHistory + favoritesPool + restoredOrbit)
        val normalizedTasteSeeds = tasteSeeds.map { seed ->
            NormalizedTasteSeed(
                artists = normalizedArtists(seed.artist).toSet(),
                album = normalizedMusicTitle(seed.album),
                moodTags = seed.moodTags.map { it.lowercase(Locale.ROOT) }.toSet()
            )
        }

        val orderedFallback = fallbackTracks
            .map { candidate ->
                RankedTrack(
                    track = candidate,
                    affinity = tasteAffinity(candidate, normalizedTasteSeeds, normalizedLanguage),
                    metadata = recordingMetadataScore(candidate),
                    title = normalizedMusicTitle(candidate.title)
                )
            }
            .sortedWith(
                compareByDescending<RankedTrack> { it.affinity }
                    .thenByDescending { it.metadata }
                    .thenBy { it.title }
            )
            .map(RankedTrack::track)
''',
)
replace_once(
    orbit,
    '''    private fun tasteAffinity(track: Track, seeds: List<Track>, languageCode: String): Int {
        if (seeds.isEmpty()) {
            return (if (isLanguagePreferred(track, languageCode)) 80 else 0) + recordingMetadataScore(track)
        }
        val candidateArtists = normalizedArtists(track.artist).toSet()
        val candidateAlbum = normalizedMusicTitle(track.album)
        val artistHits = seeds.count { seed ->
            candidateArtists.intersect(normalizedArtists(seed.artist).toSet()).isNotEmpty()
        }
        val albumHits = seeds.count { normalizedMusicTitle(it.album) == candidateAlbum && candidateAlbum.isNotBlank() }
        val moodHits = seeds.sumOf { seed -> track.moodTags.intersect(seed.moodTags).size }
        return artistHits * 180 + albumHits * 70 + moodHits * 18 +
            (if (isLanguagePreferred(track, languageCode)) 55 else 0) +
            recordingMetadataScore(track)
    }
''',
    '''    private fun tasteAffinity(
        track: Track,
        seeds: List<NormalizedTasteSeed>,
        languageCode: String
    ): Int {
        if (seeds.isEmpty()) {
            return (if (isLanguagePreferred(track, languageCode)) 80 else 0) + recordingMetadataScore(track)
        }
        val candidateArtists = normalizedArtists(track.artist).toSet()
        val candidateAlbum = normalizedMusicTitle(track.album)
        val candidateMoods = track.moodTags.map { it.lowercase(Locale.ROOT) }.toSet()
        val artistHits = seeds.count { seed -> candidateArtists.intersect(seed.artists).isNotEmpty() }
        val albumHits = seeds.count { seed -> seed.album == candidateAlbum && candidateAlbum.isNotBlank() }
        val moodHits = seeds.sumOf { seed -> candidateMoods.intersect(seed.moodTags).size }
        return artistHits * 180 + albumHits * 70 + moodHits * 18 +
            (if (isLanguagePreferred(track, languageCode)) 55 else 0) +
            recordingMetadataScore(track)
    }
''',
)

app = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
replace_once(
    app,
    "    val homeVideoTracks = remember(state.exploreVideos, rawOtherSections, state.charts) {",
    "    val homeVideoTracks = remember(state.exploreVideos, rawOtherSections, state.charts, strings.exploreNewVideos) {",
)
replace_once(
    app,
    "    val otherSections = remember(rawOtherSections, homeVideoTracks) {",
    "    val otherSections = remember(rawOtherSections, homeVideoTracks, strings.exploreNewVideos) {",
)

youtube = "tools/levyra-editorial/levyra_editorial/youtube_music.py"
replace_once(
    youtube,
    '''    return {
        "videoId": best["videoId"],
        "videoConfidence": min(100, 95 + max(0, best_score - 220) // 6),
    }


class YoutubeMusicWebClient:''',
    '''    return {
        "videoId": best["videoId"],
        "videoConfidence": min(100, 95 + max(0, best_score - 220) // 6),
    }


def combine_verified_youtube_mapping(
    audio_result: Mapping[str, Any] | None,
    official_video: Mapping[str, Any] | None,
) -> dict[str, Any] | None:
    """Publish an official video only behind a verified Art Track identity."""
    if not isinstance(audio_result, Mapping):
        return None
    audio_id = str(audio_result.get("audioVideoId") or "").strip()
    audio_confidence = audio_result.get("audioConfidence")
    if not VIDEO_ID.fullmatch(audio_id) or not isinstance(audio_confidence, int) or audio_confidence < 82:
        return None

    result = dict(audio_result)
    result["audioVideoId"] = audio_id
    result.pop("videoId", None)
    result.pop("videoConfidence", None)

    if isinstance(official_video, Mapping):
        video_id = str(official_video.get("videoId") or "").strip()
        video_confidence = official_video.get("videoConfidence")
        if (
            VIDEO_ID.fullmatch(video_id)
            and video_id != audio_id
            and isinstance(video_confidence, int)
            and video_confidence >= 90
        ):
            result["videoId"] = video_id
            result["videoConfidence"] = video_confidence

    confidence_values = [
        value
        for key in ("audioConfidence", "videoConfidence")
        if isinstance((value := result.get(key)), int)
    ]
    result["confidence"] = max(confidence_values)
    return result


class YoutubeMusicWebClient:''',
)
replace_once(
    youtube,
    '''        official_video: dict[str, Any] | None = None
        try:
            official_video = self._resolve_official_video(title, artist, duration_ms)
        except (requests.RequestException, ValueError, YoutubeMusicError) as error:
            LOGGER.warning("Central YouTube official-video query skipped: %s", type(error).__name__)

        result: dict[str, Any] = dict(audio_result or {})
        if official_video:
            result.update(official_video)
        confidence_values = [
            value
            for key in ("audioConfidence", "videoConfidence")
            if isinstance((value := result.get(key)), int)
        ]
        if confidence_values:
            result["confidence"] = max(confidence_values)
        final_result = result or None
''',
    '''        official_video: dict[str, Any] | None = None
        if audio_result is not None:
            try:
                official_video = self._resolve_official_video(title, artist, duration_ms)
            except (requests.RequestException, ValueError, YoutubeMusicError) as error:
                LOGGER.warning("Central YouTube official-video query skipped: %s", type(error).__name__)

        final_result = combine_verified_youtube_mapping(audio_result, official_video)
''',
)

parser = "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt"
replace_once(
    parser,
    '''            val youtubeMusic = item.optJSONObject("youtubeMusic")
            val youtubeAudioVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("audioVideoId"))
            val youtubeOfficialVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("videoId"))
            val youtubePlaybackId = youtubeAudioVideoId.ifBlank { youtubeOfficialVideoId }
            val albumBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("albumBrowseId"))
            val artistBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("artistBrowseId"))
            val youtubeConfidence = maxOf(
                youtubeMusic?.optInt("confidence", 0) ?: 0,
                youtubeMusic?.optInt("audioConfidence", 0) ?: 0,
                youtubeMusic?.optInt("videoConfidence", 0) ?: 0,
            ).coerceIn(0, 100)
''',
    '''            val youtubeMusic = item.optJSONObject("youtubeMusic")
            val genericYoutubeConfidence = youtubeMusic?.optInt("confidence", 0)?.coerceIn(0, 100) ?: 0
            val youtubeAudioConfidence = youtubeMusic
                ?.optInt("audioConfidence", genericYoutubeConfidence)
                ?.coerceIn(0, 100)
                ?: 0
            val youtubeVideoConfidence = youtubeMusic
                ?.optInt("videoConfidence", genericYoutubeConfidence)
                ?.coerceIn(0, 100)
                ?: 0
            val youtubeAudioVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("audioVideoId"))
                .takeIf { youtubeAudioConfidence >= MIN_AUDIO_MAPPING_CONFIDENCE }
                .orEmpty()
            val youtubeOfficialVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("videoId"))
                .takeIf {
                    youtubeAudioVideoId.isNotBlank() &&
                        youtubeVideoConfidence >= MIN_OFFICIAL_VIDEO_CONFIDENCE
                }
                .orEmpty()
            val youtubePlaybackId = youtubeAudioVideoId
            val albumBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("albumBrowseId"))
            val artistBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("artistBrowseId"))
            val youtubeConfidence = maxOf(
                genericYoutubeConfidence,
                youtubeAudioConfidence,
                youtubeVideoConfidence,
            )
''',
)
replace_once(
    parser,
    '''    private const val MAX_ARTWORK_URL_LENGTH = 512
    private const val HTTPS_DEFAULT_PORT = 443
''',
    '''    private const val MAX_ARTWORK_URL_LENGTH = 512
    private const val HTTPS_DEFAULT_PORT = 443
    private const val MIN_AUDIO_MAPPING_CONFIDENCE = 82
    private const val MIN_OFFICIAL_VIDEO_CONFIDENCE = 90
''',
)

orbit_test = Path("app/src/test/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbitIdentityTest.kt")
orbit_test_text = orbit_test.read_text(encoding="utf-8")
orbit_addition = '''
    @Test
    fun preservesRealArtistNamesWhileStillSplittingCredits() {
        val e40 = track(id = "e4000000001", title = "Same", artist = "E-40")
        val forty = track(id = "forty000001", title = "Same", artist = "40")
        val yLaBamba = track(id = "ylabamba001", title = "Same", artist = "Y La Bamba")
        val laBamba = track(id = "labamba0001", title = "Same", artist = "La Bamba")
        val acdc = track(id = "acdc0000001", title = "Same", artist = "AC/DC")
        val splitAcDc = track(id = "acdc0000002", title = "Same", artist = "AC, DC")
        val renee = track(id = "renee000001", title = "Same", artist = "Renée")
        val rene = track(id = "rene0000001", title = "Same", artist = "Ren")

        assertFalse(LevyraPersonalOrbit.sameRecording(e40, forty))
        assertFalse(LevyraPersonalOrbit.sameRecording(yLaBamba, laBamba))
        assertFalse(LevyraPersonalOrbit.sameRecording(acdc, splitAcDc))
        assertFalse(LevyraPersonalOrbit.sameRecording(renee, rene))

        val italianCredits = track(id = "credits00001", title = "Dai Dai", artist = "Shakira e Burna Boy")
        val reorderedCredits = track(id = "credits00002", title = "Dai Dai", artist = "Burna Boy, Shakira")
        assertTrue(LevyraPersonalOrbit.sameRecording(italianCredits, reorderedCredits))
    }

'''
if "fun preservesRealArtistNamesWhileStillSplittingCredits" not in orbit_test_text:
    marker = "    private fun track(\n"
    if orbit_test_text.count(marker) != 1:
        raise RuntimeError("Orbit test insertion marker missing")
    orbit_test.write_text(orbit_test_text.replace(marker, orbit_addition + marker, 1), encoding="utf-8")

youtube_test = Path("tools/levyra-editorial/tests/test_youtube_music.py")
youtube_test_text = youtube_test.read_text(encoding="utf-8")
if "combine_verified_youtube_mapping," not in youtube_test_text:
    youtube_test_text = youtube_test_text.replace(
        "    YoutubeMusicWebClient,\n",
        "    YoutubeMusicWebClient,\n    combine_verified_youtube_mapping,\n",
        1,
    )
youtube_addition = '''


def test_combined_mapping_requires_verified_audio_identity() -> None:
    official = {"videoId": "fcnDmrtj6Sk", "videoConfidence": 99}

    assert combine_verified_youtube_mapping(None, official) is None
    assert combine_verified_youtube_mapping(
        {"audioVideoId": "invalid", "audioConfidence": 99},
        official,
    ) is None

    mapping = combine_verified_youtube_mapping(
        {"audioVideoId": "lFQdcPTTzSg", "audioConfidence": 99},
        official,
    )
    assert mapping is not None
    assert mapping["audioVideoId"] == "lFQdcPTTzSg"
    assert mapping["videoId"] == "fcnDmrtj6Sk"
    assert mapping["confidence"] == 99


def test_resolve_skips_web_video_query_when_audio_identity_is_missing(monkeypatch: pytest.MonkeyPatch) -> None:
    client = YoutubeMusicWebClient("SAPISID=abcdefghijklmnopqrstuvwxyz123456")
    monkeypatch.setattr(client, "_search", lambda _query: {})
    calls = 0

    def official_video(_title: str, _artist: str, _duration_ms: int) -> dict[str, object]:
        nonlocal calls
        calls += 1
        return {"videoId": "fcnDmrtj6Sk", "videoConfidence": 99}

    monkeypatch.setattr(client, "_resolve_official_video", official_video)
    try:
        assert client.resolve("Missing", "Artist", 180_000) is None
        assert calls == 0
    finally:
        client.close()
'''
if "test_combined_mapping_requires_verified_audio_identity" not in youtube_test_text:
    youtube_test_text += youtube_addition
youtube_test.write_text(youtube_test_text, encoding="utf-8")

Path("app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt").write_text(
    '''package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditorialCatalogParserTest {
    @Test
    fun separatesAudioPlaybackFromOfficialVideoCounterpart() {
        val snapshot = EditorialCatalogParser.parse(
            catalog(
                youtubeMusic = """{
                    "audioVideoId": "Audio123456",
                    "audioConfidence": 99,
                    "videoId": "fcnDmrtj6Sk",
                    "videoConfidence": 97,
                    "confidence": 99
                }"""
            ),
            loadedAt = 0L
        )

        assertNotNull(snapshot)
        val track = snapshot!!.byMarket.getValue("IT").single()
        assertEquals("Audio123456", track.id)
        assertEquals("", track.videoUrl)
        assertEquals("fcnDmrtj6Sk", track.counterpartVideoId)
        assertEquals("MUSIC_VIDEO_TYPE_OMV", track.videoType)
        assertEquals(99, track.metadataConfidence)
    }

    @Test
    fun rejectsMalformedUnsupportedAndInvalidTimestampCatalogs() {
        assertNull(EditorialCatalogParser.parse("not-json", loadedAt = 0L))
        assertNull(EditorialCatalogParser.parse(catalog(schemaVersion = 2), loadedAt = 0L))
        assertNull(EditorialCatalogParser.parse(catalog(generatedAt = "not-a-time"), loadedAt = 0L))
    }

    @Test
    fun exposesFreshnessWithoutAcceptingStaleOrFarFutureSnapshots() {
        val stale = EditorialCatalogParser.parse(catalog(generatedAt = "2026-07-01T00:00:00Z"), loadedAt = 0L)!!
        val future = EditorialCatalogParser.parse(catalog(generatedAt = "2026-08-01T00:00:00Z"), loadedAt = 0L)!!
        val now = Instant.parse("2026-07-31T12:00:00Z").toEpochMilli()

        assertFalse(stale.isUsable(now))
        assertFalse(future.isUsable(now))
    }

    @Test
    fun refusesVideoOnlyOrLowConfidenceMappings() {
        val videoOnly = EditorialCatalogParser.parse(
            catalog(youtubeMusic = """{"videoId":"fcnDmrtj6Sk","videoConfidence":99,"confidence":99}"""),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertTrue(videoOnly.id.startsWith("chart-"))
        assertEquals("", videoOnly.counterpartVideoId)

        val lowConfidence = EditorialCatalogParser.parse(
            catalog(youtubeMusic = """{
                "audioVideoId":"Audio123456",
                "audioConfidence":70,
                "videoId":"fcnDmrtj6Sk",
                "videoConfidence":99
            }"""),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertTrue(lowConfidence.id.startsWith("chart-"))
        assertEquals("", lowConfidence.counterpartVideoId)
    }

    @Test
    fun validatesArtworkHostAndScheme() {
        val rejected = EditorialCatalogParser.parse(
            catalog(artworkUrl = "http://evil.example/cover.jpg"),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertEquals("", rejected.thumbnailUrl)

        val acceptedUrl = "https://i.scdn.co/image/ab67616d00001e0203cadf1b3fe324c1dc710ed4"
        val accepted = EditorialCatalogParser.parse(catalog(artworkUrl = acceptedUrl), loadedAt = 0L)!!
            .byMarket.getValue("IT").single()
        assertEquals(acceptedUrl, accepted.thumbnailUrl)
    }

    @Test
    fun selectsRequestedMarketAndUsesItalyOnlyForInvalidCountryCodes() {
        val body = catalog(
            collections = collection("IT", track(title = "Italia")) + "," +
                collection("US", track(title = "USA"))
        )
        val snapshot = EditorialCatalogParser.parse(body, loadedAt = 0L)!!

        assertEquals("USA", snapshot.tracks("US", 10).single().title)
        assertEquals("Italia", snapshot.tracks("invalid", 10).single().title)
        assertTrue(snapshot.tracks("FR", 10).isEmpty())
    }

    private fun catalog(
        schemaVersion: Int = 1,
        generatedAt: String = "2026-07-31T09:00:00Z",
        artworkUrl: String = "",
        youtubeMusic: String? = null,
        collections: String = collection("IT", track(artworkUrl = artworkUrl, youtubeMusic = youtubeMusic))
    ): String = """{
        "schemaVersion": $schemaVersion,
        "generatedAt": "$generatedAt",
        "collections": [$collections]
    }""".trimIndent()

    private fun collection(market: String, track: String): String = """{
        "kind": "chart",
        "market": "$market",
        "tracks": [$track]
    }""".trimIndent()

    private fun track(
        title: String = "Dai Dai",
        artist: String = "Shakira",
        artworkUrl: String = "",
        youtubeMusic: String? = null
    ): String {
        val youtube = youtubeMusic?.let { ",\"youtubeMusic\":$it" }.orEmpty()
        val artwork = artworkUrl.takeIf(String::isNotBlank)
            ?.let { ",\"artworkUrl\":\"$it\"" }
            .orEmpty()
        return """{
            "title": "$title",
            "artists": [{"name": "$artist"}],
            "album": {"name": "$title"},
            "durationMs": 223448$youtube$artwork
        }""".trimIndent()
    }
}
''',
    encoding="utf-8",
)
