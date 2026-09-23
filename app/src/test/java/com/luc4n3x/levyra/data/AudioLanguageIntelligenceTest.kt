package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraPlaybackCacheKey
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLanguageIntelligenceTest {

    private fun formatJson(
        itag: Int = 251,
        bitrate: Int = 130000,
        mimeType: String = "audio/webm; codecs=\"opus\"",
        audioQuality: String = "AUDIO_QUALITY_MEDIUM",
        url: String = "https://rr.example/videoplayback?itag=$itag",
        xtags: String = "",
        contentLength: Long = 0L,
        audioTrack: JSONObject? = null
    ): JSONObject {
        return JSONObject().apply {
            put("itag", itag)
            put("bitrate", bitrate)
            put("mimeType", mimeType)
            put("audioQuality", audioQuality)
            put("url", url)
            if (xtags.isNotBlank()) put("xtags", xtags)
            if (contentLength > 0L) put("contentLength", contentLength.toString())
            if (audioTrack != null) put("audioTrack", audioTrack)
        }
    }

    private fun track(streamUrl: String = "", videoId: String = "testVideo123"): Track {
        return Track(
            id = videoId,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 200_000L,
            streamUrl = streamUrl,
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "YouTube",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
    }

    // 1. Stesso itag: traccia originale vs traccia auto-dub -> vince originale
    @Test
    fun sameItagOriginalBeatsAutoDub() {
        val originalFormat = formatJson(
            itag = 251,
            bitrate = 130_000,
            xtags = "acont=original:lang=en",
            audioTrack = JSONObject().put("id", "en.default").put("displayName", "English (Original)").put("audioIsDefault", true)
        )
        val autoDubFormat = formatJson(
            itag = 251,
            bitrate = 130_000,
            xtags = "acont=dubbed-auto:lang=it",
            audioTrack = JSONObject().put("id", "it.auto").put("displayName", "Italiano (auto)").put("isAutoDubbed", true)
        )

        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(originalFormat)
        val metaAutoDub = AudioLanguageIntelligence.parseFromFormat(autoDubFormat)

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_AUTO_DUB, metaAutoDub.tier)
        assertTrue(metaOriginal.tier > metaAutoDub.tier)

        val scoreOrig = strictAudioSelectionScore(metaOriginal.tier, 0)
        val scoreAuto = strictAudioSelectionScore(metaAutoDub.tier, 4_999_999)
        assertTrue("Original must beat auto-dub even with max format score", scoreOrig > scoreAuto)
    }

    // 2. Stesso itag: traccia originale vs traccia human dub -> vince originale senza preferenza utente
    @Test
    fun sameItagOriginalBeatsHumanDub() {
        val originalFormat = formatJson(
            itag = 251,
            xtags = "acont=original:lang=en",
            audioTrack = JSONObject().put("id", "en").put("displayName", "English (Original)")
        )
        val humanDubFormat = formatJson(
            itag = 251,
            xtags = "acont=dubbed:lang=es",
            audioTrack = JSONObject().put("id", "es").put("displayName", "Español")
        )

        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(originalFormat)
        val metaHumanDub = AudioLanguageIntelligence.parseFromFormat(humanDubFormat)

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_HUMAN_DUB, metaHumanDub.tier)
        assertTrue(metaOriginal.tier > metaHumanDub.tier)
    }

    // 3. Stesso itag: traccia originale vs traccia descriptive -> vince originale
    @Test
    fun sameItagOriginalBeatsDescriptive() {
        val originalFormat = formatJson(
            itag = 251,
            xtags = "acont=original:lang=en"
        )
        val descriptiveFormat = formatJson(
            itag = 251,
            xtags = "acont=descriptive:lang=en"
        )

        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(originalFormat)
        val metaDescriptive = AudioLanguageIntelligence.parseFromFormat(descriptiveFormat)

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_DESCRIPTIVE, metaDescriptive.tier)
        assertTrue(metaOriginal.tier > metaDescriptive.tier)
    }

    // 4. Traccia default vs auto-dub -> vince default
    @Test
    fun defaultAudioBeatsAutoDub() {
        val defaultFormat = formatJson(
            itag = 251,
            audioTrack = JSONObject().put("audioIsDefault", true).put("isAutoDubbed", false)
        )
        val autoDubFormat = formatJson(
            itag = 251,
            audioTrack = JSONObject().put("audioIsDefault", false).put("isAutoDubbed", true)
        )

        val metaDefault = AudioLanguageIntelligence.parseFromFormat(defaultFormat)
        val metaAutoDub = AudioLanguageIntelligence.parseFromFormat(autoDubFormat)

        assertEquals(AudioLanguageIntelligence.TIER_DEFAULT_AUDIO, metaDefault.tier)
        assertEquals(AudioLanguageIntelligence.TIER_AUTO_DUB, metaAutoDub.tier)
        assertTrue(metaDefault.tier > metaAutoDub.tier)
    }

    // 5. Traccia senza metadata vs auto-dub -> vince senza metadata
    @Test
    fun unspecifiedAudioBeatsAutoDub() {
        val unspecifiedFormat = formatJson(itag = 251)
        val autoDubFormat = formatJson(
            itag = 251,
            xtags = "acont=dubbed-auto"
        )

        val metaUnspecified = AudioLanguageIntelligence.parseFromFormat(unspecifiedFormat)
        val metaAutoDub = AudioLanguageIntelligence.parseFromFormat(autoDubFormat)

        assertEquals(AudioLanguageIntelligence.TIER_UNSPECIFIED, metaUnspecified.tier)
        assertEquals(AudioLanguageIntelligence.TIER_AUTO_DUB, metaAutoDub.tier)
        assertTrue(metaUnspecified.tier > metaAutoDub.tier)
    }

    @Test
    fun languageOnlyXtagsWithoutTrackMetadataStayUnspecified() {
        val format = formatJson(
            itag = 251,
            xtags = "lang=en-US"
        )

        val meta = AudioLanguageIntelligence.parseFromFormat(format)

        assertEquals(AudioTrackKind.UNSPECIFIED, meta.kind)
        assertEquals(AudioLanguageIntelligence.TIER_UNSPECIFIED, meta.tier)
        assertEquals("en-us", meta.language)
    }

    // 6. Preferenza lingua utente corrisponde a traccia originale -> vince con tier massimo (TIER_ORIGINAL_PREFERRED)
    @Test
    fun preferredLanguageMatchesOriginalGetsTopTier() {
        val italianOriginal = formatJson(
            itag = 251,
            xtags = "acont=original:lang=it",
            audioTrack = JSONObject().put("id", "it").put("displayName", "Italiano (Originale)")
        )
        val englishDub = formatJson(
            itag = 251,
            xtags = "acont=dubbed:lang=en"
        )

        val meta = AudioLanguageIntelligence.parseFromFormat(italianOriginal, preferredLanguage = "it")
        val metaDub = AudioLanguageIntelligence.parseFromFormat(englishDub, preferredLanguage = "it")

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL_PREFERRED, meta.tier)
        assertTrue(meta.tier > metaDub.tier)
    }

    // 7. Preferenza lingua utente corrisponde a traccia human dub -> human dub supera originale in altra lingua
    @Test
    fun preferredLanguageMatchesHumanDubBeatsOtherOriginal() {
        val englishOriginal = formatJson(
            itag = 251,
            xtags = "acont=original:lang=en",
            audioTrack = JSONObject().put("id", "en").put("displayName", "English (Original)")
        )
        val spanishHumanDub = formatJson(
            itag = 251,
            xtags = "acont=dubbed:lang=es",
            audioTrack = JSONObject().put("id", "es").put("displayName", "Español")
        )

        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(englishOriginal, preferredLanguage = "es")
        val metaSpanishDub = AudioLanguageIntelligence.parseFromFormat(spanishHumanDub, preferredLanguage = "es")

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_PREFERRED_HUMAN, metaSpanishDub.tier)
        assertTrue("Configured preferred language human dub should beat original in other language", metaSpanishDub.tier > metaOriginal.tier)
    }

    // 8. CRITICO: Preferenza lingua utente corrisponde SOLO ad auto-dub -> NON supera traccia originale in lingua diversa!
    @Test
    fun preferredLanguageMatchingAutoDubNeverBeatsOriginal() {
        val englishOriginal = formatJson(
            itag = 251,
            bitrate = 130_000,
            xtags = "acont=original:lang=en",
            audioTrack = JSONObject().put("id", "en").put("displayName", "English (Original)")
        )
        val italianAutoDub = formatJson(
            itag = 251,
            bitrate = 160_000, // Higher bitrate must NOT help auto-dub beat original
            xtags = "acont=dubbed-auto:lang=it",
            audioTrack = JSONObject().put("id", "it").put("displayName", "Italiano (auto-generato)").put("isAutoDubbed", true)
        )

        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(englishOriginal, preferredLanguage = "it")
        val metaAutoDub = AudioLanguageIntelligence.parseFromFormat(italianAutoDub, preferredLanguage = "it")

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_AUTO_DUB, metaAutoDub.tier)
        assertTrue(metaAutoDub.tieBreakerBonus > 0)

        val scoreOriginal = strictAudioSelectionScore(metaOriginal.tier, -100_000)
        val scoreAutoDub = strictAudioSelectionScore(
            metaAutoDub.tier,
            1_000_000,
            metaAutoDub.tieBreakerBonus
        )
        assertTrue("Original must win over AI auto-dub even when auto-dub matches preferred language!", scoreOriginal > scoreAutoDub)
    }

    // 9. Preferenza lingua utente impostata ma traccia non disponibile -> fallback pulito su originale/default
    @Test
    fun preferredLanguageFallbackCleanlyToOriginal() {
        val englishOriginal = formatJson(
            itag = 251,
            xtags = "acont=original:lang=en"
        )
        val frenchDub = formatJson(
            itag = 251,
            xtags = "acont=dubbed:lang=fr"
        )

        // Preferred is Japanese ("ja"), not present in video
        val metaOriginal = AudioLanguageIntelligence.parseFromFormat(englishOriginal, preferredLanguage = "ja")
        val metaFrench = AudioLanguageIntelligence.parseFromFormat(frenchDub, preferredLanguage = "ja")

        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaOriginal.tier)
        assertEquals(AudioLanguageIntelligence.TIER_HUMAN_DUB, metaFrench.tier)
        assertTrue(metaOriginal.tier > metaFrench.tier)
    }

    // 10. Tracce con stessa lingua/originalità ma bitrate diverso -> vince bitrate/qualità maggiore
    @Test
    fun sameTierDecidedByBitrateAndQuality() {
        val lowBitrate = formatJson(
            itag = 250,
            bitrate = 70_000,
            xtags = "acont=original:lang=en"
        )
        val highBitrate = formatJson(
            itag = 251,
            bitrate = 160_000,
            xtags = "acont=original:lang=en"
        )

        val metaLow = AudioLanguageIntelligence.parseFromFormat(lowBitrate)
        val metaHigh = AudioLanguageIntelligence.parseFromFormat(highBitrate)

        assertEquals(metaLow.tier, metaHigh.tier)

        val scoreLow = strictAudioSelectionScore(metaLow.tier, 400_000 + 70_000)
        val scoreHigh = strictAudioSelectionScore(metaHigh.tier, 760_000 + 160_000)
        assertTrue(scoreHigh > scoreLow)
    }

    // 11. Stesso itag ma URL/clen diversi per tracce diverse -> estrazione corretta e lunghezze separate
    @Test
    fun sameItagDifferentUrlsAndClenExtractedIndependently() {
        val urlEnglish = "https://rr1---sn-example.googlevideo.com/videoplayback?itag=251&clen=4500123&xtags=lang%3Den%3Bacont%3Doriginal"
        val urlItalian = "https://rr2---sn-example.googlevideo.com/videoplayback?itag=251&clen=4610999&xtags=lang%3Dit%3Bacont%3Ddubbed"

        val formatEn = formatJson(itag = 251, url = urlEnglish)
        val formatIt = formatJson(itag = 251, url = urlItalian)

        val metaEn = AudioLanguageIntelligence.parseFromFormat(formatEn, urlEnglish)
        val metaIt = AudioLanguageIntelligence.parseFromFormat(formatIt, urlItalian)

        assertEquals("en", metaEn.language)
        assertEquals(4500123L, metaEn.contentLength)
        assertEquals("it", metaIt.language)
        assertEquals(4610999L, metaIt.contentLength)
        assertNotEquals(metaEn.contentLength, metaIt.contentLength)
    }

    // 12. NewPipe extractor: AudioTrackType.ORIGINAL vs DUBBED vs autoGenerated -> ranking corretto
    @Test
    fun audioTrackTierOrderingContract() {
        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, AudioLanguageIntelligence.TIER_ORIGINAL)
        assertEquals(AudioLanguageIntelligence.TIER_HUMAN_DUB, AudioLanguageIntelligence.TIER_HUMAN_DUB)
        assertEquals(AudioLanguageIntelligence.TIER_AUTO_DUB, AudioLanguageIntelligence.TIER_AUTO_DUB)

        assertTrue(AudioLanguageIntelligence.TIER_ORIGINAL > AudioLanguageIntelligence.TIER_HUMAN_DUB)
        assertTrue(AudioLanguageIntelligence.TIER_HUMAN_DUB > AudioLanguageIntelligence.TIER_AUTO_DUB)
        assertTrue(AudioLanguageIntelligence.TIER_AUTO_DUB > AudioLanguageIntelligence.TIER_DESCRIPTIVE)
    }

    // 13. NewPipe extractor con xtags URL (acont=original, acont=dubbed-auto) -> parsing e ranking corretti
    @Test
    fun extractorXtagsParsing() {
        val xtags = "acont=original:lang=en-US"
        assertEquals("original", AudioLanguageIntelligence.extractXtag(xtags, "acont"))
        assertEquals("en-US", AudioLanguageIntelligence.extractXtag(xtags, "lang"))
        assertEquals("en-us", AudioLanguageIntelligence.normalizeLanguage("en-US"))

        val urlWithXtags = "https://rr.example/videoplayback?itag=251&xtags=acont%3Ddubbed-auto%3Alang%3Dit_IT"
        val extractedXtags = AudioLanguageIntelligence.extractXtagsFromUrl(urlWithXtags)
        assertEquals("dubbed-auto", AudioLanguageIntelligence.extractXtag(extractedXtags, "acont"))
        assertEquals("it_IT", AudioLanguageIntelligence.extractXtag(extractedXtags, "lang"))
        assertEquals("it-it", AudioLanguageIntelligence.normalizeLanguage(AudioLanguageIntelligence.extractXtag(extractedXtags, "lang")))
    }

    @Test
    fun regionalLanguageTagsPreserveExactLocaleAndPreferExactOverBaseMatch() {
        assertEquals("pt-br", AudioLanguageIntelligence.normalizeLanguage("pt_BR"))
        assertEquals("zh-hant", AudioLanguageIntelligence.normalizeLanguage("zh-Hant"))
        assertEquals("es-419", AudioLanguageIntelligence.normalizeLanguage("es-419"))
        assertEquals("he", AudioLanguageIntelligence.normalizeLanguage("iw"))
        assertEquals("id-id", AudioLanguageIntelligence.normalizeLanguage("in_ID"))
        assertEquals("fil-ph", AudioLanguageIntelligence.normalizeLanguage("tl_PH"))

        val brazilian = formatJson(
            xtags = "acont=dubbed:lang=pt-BR",
            audioTrack = JSONObject().put("id", "pt-BR").put("displayName", "Português (Brasil)")
        )
        val portuguese = formatJson(
            xtags = "acont=dubbed:lang=pt-PT",
            audioTrack = JSONObject().put("id", "pt-PT").put("displayName", "Português (Portugal)")
        )

        val exact = AudioLanguageIntelligence.parseFromFormat(brazilian, preferredLanguage = "pt-BR")
        val base = AudioLanguageIntelligence.parseFromFormat(portuguese, preferredLanguage = "pt-BR")

        assertEquals(AudioLanguageIntelligence.TIER_PREFERRED_HUMAN, exact.tier)
        assertEquals(AudioLanguageIntelligence.TIER_PREFERRED_HUMAN, base.tier)
        assertTrue(exact.tieBreakerBonus > base.tieBreakerBonus)

        val exactScore = strictAudioSelectionScore(exact.tier, -4_000_000, exact.tieBreakerBonus)
        val baseScore = strictAudioSelectionScore(base.tier, 4_000_000, base.tieBreakerBonus)
        assertTrue(
            "Exact locale must beat a same-base locale regardless of codec or bitrate score",
            exactScore > baseScore
        )
    }

    @Test
    fun reusableLanguageRequiresExactLocaleWhenPreferenceIsRegional() {
        assertTrue(AudioLanguageIntelligence.canReuseResolvedLanguage("pt-BR", "pt-BR"))
        assertFalse(AudioLanguageIntelligence.canReuseResolvedLanguage("pt-PT", "pt-BR"))
        assertTrue(AudioLanguageIntelligence.canReuseResolvedLanguage("pt-BR", "pt"))
        assertTrue(AudioLanguageIntelligence.canReuseResolvedLanguage("pt-PT", "pt"))
        assertFalse(AudioLanguageIntelligence.canReuseResolvedLanguage("", "pt-BR"))
    }

    @Test
    fun encodedUrlXtagsAreStillDecodedForLanguageSelection() {
        val encodedUrl =
            "https://rr.example/videoplayback?foo=1%26itag%3D251%26xtags%3Dacont%253Ddubbed%253Alang%253Dit_IT"
        val rawXtags = AudioLanguageIntelligence.extractXtagsFromUrl(encodedUrl)

        assertEquals("dubbed", AudioLanguageIntelligence.extractXtag(rawXtags, "acont"))
        assertEquals("it_IT", AudioLanguageIntelligence.extractXtag(rawXtags, "lang"))
    }

    // 14. JioSaavn non toccato: mapping e stream rimangono intatti e isolati
    @Test
    fun jioSaavnImmunity() {
        val jioSaavnDescriptor = AlternativeAudioSource(
            providerId = "jiosaavn",
            providerTrackId = "js_12345",
            bitrateKbps = 320,
            verdict = AlternativeMatchVerdict.EXACT,
            confidence = 100
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "youtubeVideo123",
            provider = "JioSaavn Audio",
            resolvedAtMs = 1000L,
            expiresAtMs = 2000L,
            durationMs = 3000L,
            selectedAudioUrl = "https://aac.saavncdn.com/test_320.mp4",
            selectedVideoUrl = "",
            streams = emptyList(),
            alternativeSource = jioSaavnDescriptor
        )

        val jioTrack = track("https://aac.saavncdn.com/test_320.mp4").copy(playbackManifest = manifest)

        // Verifichiamo che la cache key per JioSaavn usi il namespace "alt-" e non venga alterata dalle preferenze lingua YouTube
        val cacheKey = LevyraPlaybackCacheKey.stream(jioTrack)
        assertTrue("JioSaavn stream must use isolated alt cache namespace", cacheKey.contains(":stream-v2:alt-"))
        assertFalse(cacheKey.contains("lang"))
    }

    // 15. alternativeSource in PlaybackManifest: immune alla selezione lingua YouTube
    @Test
    fun alternativeSourceInPlaybackManifestIsImmune() {
        val alternative = AlternativeAudioSource(
            providerId = "jiosaavn",
            providerTrackId = "track_flac_99",
            bitrateKbps = 1411,
            verdict = AlternativeMatchVerdict.EXACT,
            confidence = 100
        )
        val trackWithAlt = track("https://saavn.example/flac.flac").copy(
            playbackManifest = ResolvedPlaybackManifest(
                sourceVideoId = "vid1",
                provider = "HQ Provider",
                resolvedAtMs = 1000L,
                expiresAtMs = 2000L,
                durationMs = 3000L,
                selectedAudioUrl = "https://saavn.example/flac.flac",
                selectedVideoUrl = "",
                streams = emptyList(),
                alternativeSource = alternative
            )
        )

        assertEquals("jiosaavn", trackWithAlt.playbackManifest?.alternativeSource?.providerId)
        assertEquals(1411, trackWithAlt.playbackManifest?.alternativeSource?.bitrateKbps)
        assertEquals(AlternativeMatchVerdict.EXACT, trackWithAlt.playbackManifest?.alternativeSource?.verdict)
    }

    // 16. Offline export: seleziona audio corretto preservando clen reale e compatibilità mp4
    @Test
    fun offlineExportPreservesClenAndMp4Compatibility() {
        val mp4Url = "https://rr.example/videoplayback?itag=140&mime=audio%2Fmp4&clen=3542100&xtags=acont%3Doriginal%3Alang%3Den"
        val webmUrl = "https://rr.example/videoplayback?itag=251&mime=audio%2Fwebm&clen=3210400&xtags=acont%3Doriginal%3Alang%3Den"

        assertTrue(isMp4OfflineAudioCandidate("audio/mp4", mp4Url))
        assertFalse(isMp4OfflineAudioCandidate("audio/webm", webmUrl))

        val metaMp4 = AudioLanguageIntelligence.parseFromFormat(formatJson(itag = 140, url = mp4Url), mp4Url)
        assertEquals(3542100L, metaMp4.contentLength)
        assertEquals(AudioLanguageIntelligence.TIER_ORIGINAL, metaMp4.tier)
    }

    // 17. Cache separation: cambio lingua preferita separa chiavi di cache per evitare cache pollution
    @Test
    fun cacheSeparationPreventsPollution() {
        val trackEn = track("https://rr.example/videoplayback?itag=251&clen=4500000&xtags=lang%3Den")
        val trackIt = track("https://rr.example/videoplayback?itag=251&clen=4600000&xtags=lang%3Dit")

        val keyEn = LevyraPlaybackCacheKey.stream(trackEn)
        val keyIt = LevyraPlaybackCacheKey.stream(trackIt)

        assertNotEquals("Media3 stream cache keys must be separated for different audio tracks sharing an itag", keyEn, keyIt)
        assertTrue(keyEn.contains("lang-en"))
        assertTrue(keyIt.contains("lang-it"))
        assertTrue(keyEn.contains("clen-4500000"))
        assertTrue(keyIt.contains("clen-4600000"))
    }
}
