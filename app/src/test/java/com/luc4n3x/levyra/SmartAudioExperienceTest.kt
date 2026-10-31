package com.luc4n3x.levyra

import com.luc4n3x.levyra.data.LyricsPhraseConfidence
import com.luc4n3x.levyra.data.LyricsPhraseHit
import com.luc4n3x.levyra.data.LyricsPhraseRanking
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LyricsPhraseMatcher
import com.luc4n3x.levyra.domain.LyricsPhraseVersionKind
import com.luc4n3x.levyra.domain.MixChapterParser
import com.luc4n3x.levyra.domain.NightListeningCurve
import com.luc4n3x.levyra.domain.NightListeningPreset
import com.luc4n3x.levyra.domain.RawMixChapter
import com.luc4n3x.levyra.domain.SmartAudioDeviceCategory
import com.luc4n3x.levyra.domain.SmartAudioEqMode
import com.luc4n3x.levyra.domain.SmartAudioNightMode
import com.luc4n3x.levyra.domain.SmartAudioProfile
import com.luc4n3x.levyra.domain.SmartAudioResolver
import com.luc4n3x.levyra.ui.i18n.SmartAudioText
import com.luc4n3x.levyra.ui.i18n.smartAudioLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.smartAudioRawEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAudioExperienceTest {
    @Test
    fun chaptersParseCommonDescriptionFormats() {
        val description = """
            Tracklist:
            00:00 Intro
            03:45 - First Track
            12:30 Second Track
            [1:05:20] Final Mix
            Follow me on socials
        """.trimIndent()
        val chapters = MixChapterParser.parseDescription(description, 2L * 3600L * 1000L)
        assertEquals(listOf("Intro", "First Track", "Second Track", "Final Mix"), chapters.map { it.title })
        assertEquals(listOf(0L, 225_000L, 750_000L, 3_920_000L), chapters.map { it.startMs })
    }

    @Test
    fun chaptersAreNotInventedFromInvalidOrStrayTimestamps() {
        assertTrue(MixChapterParser.parseDescription("Check 12:30 for the drop", 3_600_000L).isEmpty())
        assertTrue(MixChapterParser.parseDescription("05:00 Late start\n08:00 Next", 3_600_000L).isEmpty())
        assertTrue(MixChapterParser.parseDescription("", 3_600_000L).isEmpty())
        val outOfRange = MixChapterParser.parseDescription("00:00 A\n99:61 Bad\n02:00:00 Past end\n01:00 B", 600_000L)
        assertEquals(listOf("A", "B"), outOfRange.map { it.title })
    }

    @Test
    fun chaptersDropDuplicatesAndHandleHundredsWithFastLookup() {
        val raw = (0 until 800).map { RawMixChapter(it * 60_000L, "Track $it") } + RawMixChapter(0L, "Duplicate")
        val chapters = MixChapterParser.normalize(raw, 800 * 60_000L)
        assertEquals(800, chapters.size)
        assertEquals(417, MixChapterParser.indexAt(chapters, 417 * 60_000L + 5L))
        assertEquals(0, MixChapterParser.indexAt(chapters, -1L))
        assertEquals(799, MixChapterParser.indexAt(chapters, Long.MAX_VALUE))
    }

    @Test
    fun nightPresetsGetProgressivelyStrongerAndNeverBoostPeaks() {
        var previous = -1f
        NightListeningPreset.entries.forEach { preset ->
            val reduction = NightListeningCurve.gainReductionDb(-6f, preset)
            assertTrue(preset.name, reduction > previous)
            previous = reduction
            assertTrue(preset.name, NightListeningCurve.outputDb(0f, preset) < 0f)
            assertEquals(preset.name, 0f, NightListeningCurve.gainReductionDb(-70f, preset), 0.0001f)
        }
    }

    @Test
    fun nightCurveIsContinuousAcrossTheKnee() {
        val preset = NightListeningPreset.NIGHT
        var db = -60f
        var last = NightListeningCurve.gainReductionDb(db, preset)
        while (db < 0f) {
            db += 0.25f
            val current = NightListeningCurve.gainReductionDb(db, preset)
            assertTrue(current >= last)
            assertTrue(current - last < 0.25f)
            last = current
        }
    }

    @Test
    fun smartProfilesStayOffByDefaultAndOverrideOnlyAtRuntime() {
        val profile = SmartAudioProfile(
            deviceKey = "bt",
            deviceName = "Buds",
            category = SmartAudioDeviceCategory.BLUETOOTH_HEADPHONES,
            eqMode = SmartAudioEqMode.OFF,
            nightMode = SmartAudioNightMode.ON,
            nightPreset = NightListeningPreset.NIGHT
        )
        val global = LevyraAudioSettings(equalizerEnabled = true, smartAudioProfiles = listOf(profile))
        assertFalse(LevyraAudioSettings().smartAudioProfilesEnabled)
        assertEquals(global, SmartAudioResolver.resolve(global, "bt"))
        val enabled = global.copy(smartAudioProfilesEnabled = true)
        val resolved = SmartAudioResolver.resolve(enabled, "bt")
        assertFalse(resolved.equalizerEnabled)
        assertTrue(resolved.nightListeningEnabled)
        assertEquals(NightListeningPreset.NIGHT, resolved.nightListeningPreset)
        assertTrue(enabled.equalizerEnabled)
        assertEquals(enabled, SmartAudioResolver.resolve(enabled, "speaker"))
        assertEquals(enabled, SmartAudioResolver.resolve(enabled, null))
    }

    @Test
    fun smartProfilePreampIsBoundedAgainstVolumeJumps() {
        val profile = SmartAudioProfile("k", "n", SmartAudioDeviceCategory.USB, preampDb = 40f).normalized()
        assertEquals(SmartAudioProfile.MAX_PREAMP_DB, profile!!.preampDb, 0f)
        assertNull(SmartAudioProfile(" ", "n", SmartAudioDeviceCategory.USB).normalized())
    }

    @Test
    fun phraseMatcherHandlesExactPartialTypoAndAccents() {
        val lyrics = listOf("Hello from the other side", "I must have called a thousand times", "To tell you I'm sorry")
        assertTrue(LyricsPhraseMatcher.match("I must have called a thousand times", lyrics)!!.score > 0.93f)
        assertTrue(LyricsPhraseMatcher.match("i must have called a thousand times", listOf("I must've called a thousand times"))!!.score > 0.93f)
        assertTrue(LyricsPhraseMatcher.match("must called thousand times", lyrics)!!.score > LyricsPhraseMatcher.VERIFIED_SCORE)
        assertTrue(LyricsPhraseMatcher.match("i must have calld a thosand times", lyrics)!!.score > LyricsPhraseMatcher.VERIFIED_SCORE)
        assertTrue(LyricsPhraseMatcher.match("Felice di stare lassù", listOf("E volavo felice di stare lassu"))!!.score > 0.9f)
        assertTrue(LyricsPhraseMatcher.match("the cat sat on my keyboard", lyrics)!!.score < LyricsPhraseMatcher.VERIFIED_SCORE)
    }

    @Test
    fun phraseMatcherSupportsCyrillicAndUnspacedScripts() {
        assertTrue(LyricsPhraseMatcher.match("группа крови на рукаве", listOf("Группа крови на рукаве,"))!!.score > 0.93f)
        assertTrue(LyricsPhraseMatcher.match("君の名は", listOf("僕は君の名は知らない"))!!.score > 0.75f)
        assertTrue(LyricsPhraseMatcher.isSearchable("君の名"))
        assertFalse(LyricsPhraseMatcher.isSearchable("hi"))
        assertEquals(listOf("तुम", "ही", "हो"), LyricsPhraseMatcher.tokens(LyricsPhraseMatcher.normalize("तुम ही हो")))
        assertTrue(LyricsPhraseMatcher.match("तुम ही हो", listOf("अब तुम ही हो"))!!.score > 0.9f)
        assertTrue(LyricsPhraseMatcher.match("तुम ही हो", listOf("कभी अलविदा ना कहना"))!!.score < LyricsPhraseMatcher.VERIFIED_SCORE)
    }

    @Test
    fun rankingDeduplicatesPrefersOriginalAndRejectsContradictedLyrics() {
        val hit = { id: String, title: String, kind: LyricsPhraseVersionKind ->
            LyricsPhraseHit(track(id, title), LyricsPhraseConfidence.EXACT, 1f, "", kind)
        }
        val ordered = LyricsPhraseRanking.order(
            listOf(
                hit("a", "Hello (Remix)", LyricsPhraseVersionKind.REMIX),
                hit("b", "Hello", LyricsPhraseVersionKind.ORIGINAL),
                hit("c", "Hello (Official)", LyricsPhraseVersionKind.ORIGINAL)
            )
        )
        assertEquals(listOf("b"), ordered.map { it.track.id })
        assertNull(LyricsPhraseRanking.verify(track("x", "Other"), 0, "i must have called a thousand times", listOf("nothing alike here at all")))
        assertEquals(
            LyricsPhraseConfidence.UNVERIFIED,
            LyricsPhraseRanking.verify(track("y", "Hello"), 0, "phrase here", null)!!.confidence
        )
        assertNotNull(LyricsPhraseRanking.verify(track("z", "Hello"), 0, "phrase here", listOf("phrase here")))
        assertEquals(LyricsPhraseVersionKind.COVER, LyricsPhraseMatcher.versionKind("Hello (Piano Cover)"))
    }

    @Test
    fun unverifiedCandidatesNeedATitleOverlapAndLongPhrasesYieldFragments() {
        assertTrue(LyricsPhraseRanking.titleOverlaps(track("a", "A Thousand Times"), "called thousand times"))
        assertFalse(LyricsPhraseRanking.titleOverlaps(track("b", "Best Sea Animals Poem"), "xqzv pflk wmrt bbqz"))
        assertEquals(2, LyricsPhraseRanking.fragments("one two three four five").size)
        assertTrue(LyricsPhraseRanking.fragments("one two").isEmpty())
    }

    @Test
    fun everyLanguageShipsCompleteSmartAudioCopy() {
        assertEquals(LevyraLanguageCatalog.languages.map { it.code }.toSet(), smartAudioLocalizationCodes())
        val english = smartAudioRawEntries("en")
        smartAudioLocalizationCodes().forEach { code ->
            val entries = smartAudioRawEntries(code)
            assertEquals(code, SmartAudioText.entries.size, entries.size)
            assertTrue(code, entries.none(String::isBlank))
            listOf(
                SmartAudioText.SmartCount to "{n}",
                SmartAudioText.SmartProfileActive to "{name}",
                SmartAudioText.ChaptersCount to "{n}",
                SmartAudioText.ChaptersLeft to "{time}"
            ).forEach { (key, token) ->
                assertTrue("$code ${key.name}", entries[key.ordinal].contains(token))
            }
            if (code != "en") assertFalse(code, entries == english)
        }
    }

    private fun track(id: String, title: String) = com.luc4n3x.levyra.domain.Track(
        id = id, title = title, artist = "Adele", album = "", durationMs = 200_000L, streamUrl = "", videoUrl = "",
        thumbnailUrl = "", largeThumbnailUrl = "", source = "", moodTags = emptySet(), energy = 0, vocal = 0,
        replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )
}
