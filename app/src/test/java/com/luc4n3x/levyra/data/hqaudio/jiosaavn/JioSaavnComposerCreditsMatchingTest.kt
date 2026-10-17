package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchEvaluation
import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchSelection
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackMatcher
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import com.luc4n3x.levyra.data.hqaudio.MatchRejection
import com.luc4n3x.levyra.data.hqaudio.query
import com.luc4n3x.levyra.data.hqaudio.saavnSong
import com.luc4n3x.levyra.data.hqaudio.searchBody
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnComposerCreditsMatchingTest {
    private val matcher = AlternativeTrackMatcher()

    @Test
    fun chaleyaLedByItsMusicDirectorMatchesTheFilmRecording() {
        val chosen = assertAccepted("faloMmjX", matcher.select(chaleya(), parse(chaleyaCompilation, chaleyaUnplugged, chaleyaOriginal)))
        assertEquals(AlternativeMatchVerdict.HIGH, chosen.verdict)
    }

    @Test
    fun chaleyaCompilationAndUnpluggedCopiesStayRejected() {
        assertRejected(MatchRejection.ALBUM_MISMATCH, chaleya(), parse(chaleyaCompilation).single())
        assertRejected(MatchRejection.VERSION_MISMATCH, chaleya(), parse(chaleyaUnplugged).single())
    }

    @Test
    fun chaleyaLedByTheComposerWithoutTheExpectedSingersIsRejected() {
        val otherSingers = song(
            "cover001",
            "Chaleya",
            listOf("Anirudh Ravichander", "Jonita Gandhi"),
            "Jawan",
            200,
            composerSinger("Anirudh Ravichander") + singers("Jonita Gandhi")
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, chaleya(), parse(otherSingers).single())
    }

    @Test
    fun chaleyaMissingOneExpectedSingerFromThePrimaryCreditsIsRejected() {
        val soloCredit = song(
            "solo0001",
            "Chaleya",
            listOf("Anirudh Ravichander", "Arijit Singh"),
            "Jawan",
            200,
            composerSinger("Anirudh Ravichander") + singers("Arijit Singh")
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, chaleya(), parse(soloCredit).single())
    }

    @Test
    fun sajniLedByItsComposerMatchesTheFilmRecording() {
        assertAccepted("FB8WBiWv", matcher.select(sajni(), parse(sajniOriginal)))
    }

    @Test
    fun sajniWithComposerLeadNeedsAnExcellentDuration() {
        val drifted = song("FB8drift", "Sajni", listOf("Ram Sampath", "Arijit Singh", "Prashant Pandey"), "Laapataa Ladies", 175, sajniCredits)
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, sajni(), parse(drifted).single())
    }

    @Test
    fun sajniHomonymByTheSameComposerOnAnotherAlbumIsRejected() {
        val homonym = song(
            "sajni002",
            "Sajni",
            listOf("Ram Sampath", "Arijit Singh"),
            "Talaash",
            170,
            composerSinger("Ram Sampath") + singers("Arijit Singh")
        )
        assertRejected(MatchRejection.ALBUM_MISMATCH, sajni(), parse(homonym).single())
    }

    @Test
    fun sajniRemixLedByTheComposerIsRejected() {
        val remix = song("sajnimix", "Sajni (Remix)", listOf("Ram Sampath", "Arijit Singh"), "Laapataa Ladies", 170, sajniCredits)
        assertRejected(MatchRejection.VERSION_MISMATCH, sajni(), parse(remix).single())
    }

    @Test
    fun channaMereyaAlbumAndDeluxeReleasesAreOneRecording() {
        val selection = matcher.select(channaMereya(), parse(channaMereyaUnplugged, channaMereyaDeluxe, channaMereyaOriginal))
        val chosen = (selection as AlternativeMatchSelection.Accepted).evaluation
        assertTrue(chosen.candidate.providerTrackId in setOf("uiEWT3kP", "36EzeLeh"))
        assertEquals(AlternativeMatchVerdict.HIGH, chosen.verdict)
    }

    @Test
    fun channaMereyaUnpluggedIsNotTheFilmRecording() {
        assertRejected(MatchRejection.VERSION_MISMATCH, channaMereya(), parse(channaMereyaUnplugged).single())
    }

    @Test
    fun channaMereyaComposerLeadWithoutAlbumEvidenceIsRejected() {
        val unverified = channaMereya().copy(album = "")
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, unverified, parse(channaMereyaOriginal).single())
    }

    @Test
    fun channaMereyaCompilationCopyIsRejected() {
        val compilation = song(
            "ghar0001",
            "Channa Mereya (From &quot;Ae Dil Hai Mushkil&quot;)",
            listOf("Pritam", "Arijit Singh"),
            "Ghar",
            289,
            channaMereyaCredits
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, channaMereya(), parse(compilation).single())
    }

    @Test
    fun channaMereyaFilmSourceInTheTitleConfirmsTheSoundtrackAlbum() {
        val evaluation = matcher.evaluate(channaMereya(), parse(channaMereyaOriginal).single())
        assertEquals(AlternativeMatchVerdict.HIGH, evaluation.verdict)
        val otherFilm = channaMereya().copy(
            title = "Channa Mereya (From \"Another Film\")",
            album = "Channa Mereya (From \"Another Film\")"
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, otherFilm, parse(channaMereyaOriginal).single())
    }

    @Test
    fun composerLeadWithoutRoleDataKeepsTheLeadArtistRule() {
        val unroled = song("unroled1", "Channa Mereya", listOf("Pritam", "Arijit Singh"), "Ae Dil Hai Mushkil", 289, emptyMap())
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, channaMereya(), parse(unroled).single())
    }

    @Test
    fun meriAashiquiPrefersTheSoundtrackOverTheCompilationReissue() {
        assertAccepted("Z0teQlYo", matcher.select(meriAashiqui(), parse(meriAashiquiCompilation, meriAashiquiOriginal)))
        assertRejected(MatchRejection.ALBUM_MISMATCH, meriAashiqui(), parse(meriAashiquiCompilation).single())
    }

    @Test
    fun meriAashiquiComposerCreditedAsSingerStillMatchesTheSoundtrack() {
        val composerSinging = song(
            "Z0singer",
            "Meri Aashiqui",
            listOf("Mithoon", "Palak Muchhal", "Arijit Singh"),
            "Aashiqui 2",
            266,
            composerSinger("Mithoon") + singers("Palak Muchhal", "Arijit Singh")
        )
        val evaluation = matcher.evaluate(meriAashiqui(artist = "Palak Muchhal"), parse(composerSinging).single())
        assertEquals(AlternativeMatchVerdict.HIGH, evaluation.verdict)
    }

    @Test
    fun meriAashiquiLedByAnotherSingerIsRejected() {
        val rendition = song(
            "rendit01",
            "Meri Aashiqui",
            listOf("Tulsi Kumar", "Arijit Singh"),
            "Aashiqui 2",
            266,
            singers("Tulsi Kumar", "Arijit Singh")
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, meriAashiqui(artist = "Arijit Singh"), parse(rendition).single())
    }

    @Test
    fun heeriyeWithEveryArtistTaggedAsCreatorMatchesTheSingle() {
        val chosen = assertAccepted("NIidiD9g", matcher.select(heeriye(), parse(heeriyeLofi, heeriyeOriginal, heeriyeOtherFilm)))
        assertEquals(AlternativeMatchVerdict.HIGH, chosen.verdict)
    }

    @Test
    fun heeriyeRemixAndOtherFilmSongStayRejected() {
        assertRejected(MatchRejection.TITLE_MISMATCH, heeriye(), parse(heeriyeLofi).single())
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, heeriye(), parse(heeriyeOtherFilm).single())
    }

    @Test
    fun heeriyeLedByAnActorWithoutTheExpectedSingerFirstIsRejected() {
        val actorLed = song(
            "actorled",
            "Heeriye",
            listOf("Dulquer Salmaan", "Neha Bhasin"),
            "Heeriye",
            194,
            mapOf("Dulquer Salmaan" to listOf("music", "starring"), "Neha Bhasin" to listOf("music"))
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, heeriye(), parse(actorLed).single())
    }

    @Test
    fun tereVaasteLedByItsLyricistMatchesTheFilmRecording() {
        assertAccepted("rBltEr7X", matcher.select(tereVaaste(), parse(tereVaasteUnplugged, tereVaasteOriginal)))
    }

    @Test
    fun tereVaasteUnpluggedIsRejected() {
        assertRejected(MatchRejection.VERSION_MISMATCH, tereVaaste(), parse(tereVaasteUnplugged).single())
    }

    @Test
    fun kunFaayaKunTransliterationMatchesTheSoundtrack() {
        val chosen = assertAccepted("csaEsVWV", matcher.select(kunFayaKun(), parse(kunFaayaKunCompilation, kunFaayaKunOriginal)))
        assertEquals(AlternativeMatchVerdict.HIGH, chosen.verdict)
    }

    @Test
    fun transliteratedTitleNeedsTheSameAlbum() {
        assertRejected(MatchRejection.ALBUM_MISMATCH, kunFayaKun(), parse(kunFaayaKunCompilation).single())
        val unverified = kunFayaKun().copy(album = "")
        assertRejected(MatchRejection.TITLE_MISMATCH, unverified, parse(kunFaayaKunOriginal).single())
    }

    @Test
    fun transliteratedTitleNeedsAnExcellentDuration() {
        val drifted = song("kunDrift", "Kun Faaya Kun", kunArtists, "Rockstar", 475, kunCredits)
        assertRejected(MatchRejection.TITLE_MISMATCH, kunFayaKun(), parse(drifted).single())
    }

    @Test
    fun doubledConsonantsStillSeparateDistinctTitles() {
        val kalla = song("kalla001", "Kalla", listOf("Arijit Singh"), "Rockstar", 240, singers("Arijit Singh"))
        val query = query(title = "Kala", artist = "Arijit Singh", album = "Rockstar", durationMs = 240_000L)
        assertRejected(MatchRejection.TITLE_MISMATCH, query, parse(kalla).single())
    }

    @Test
    fun transliterationIsOnlyTrustedForIndianReleases() {
        val english = saavnSong(
            id = "moon0001",
            title = "Mooon",
            primary = listOf("Singer"),
            album = "Moon",
            duration = 200,
            language = "english"
        )
        val query = query(title = "Moon", artist = "Singer", album = "Moon", durationMs = 200_000L)
        assertRejected(MatchRejection.TITLE_MISMATCH, query, parse(english).single())
    }

    private fun assertAccepted(id: String, selection: AlternativeMatchSelection): AlternativeMatchEvaluation =
        when (selection) {
            is AlternativeMatchSelection.Accepted -> selection.evaluation.also { assertEquals(id, it.candidate.providerTrackId) }
            is AlternativeMatchSelection.Rejected ->
                throw AssertionError("expected $id, rejected ${selection.reason}: ${selection.evaluations.map { it.rejection }}")
        }

    private fun assertRejected(reason: MatchRejection, query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate) {
        val evaluation = matcher.evaluate(query, candidate)
        assertFalse(evaluation.toString(), evaluation.accepted)
        assertEquals(reason, evaluation.rejection)
    }

    private fun parse(vararg songs: JSONObject): List<AlternativeTrackCandidate> =
        requireNotNull(JioSaavnPayloadParser.searchPage(searchBody(*songs), JioSaavnAudioProvider.SEARCH_RESULT_LIMIT)).candidates

    private fun song(
        id: String,
        title: String,
        primary: List<String>,
        album: String,
        duration: Int,
        credits: Map<String, List<String>>
    ): JSONObject = saavnSong(
        id = id,
        title = title,
        primary = primary,
        album = album,
        duration = duration,
        credits = credits,
        language = "hindi"
    )

    private fun composerSinger(name: String) = mapOf(name to listOf("music", "singer"))

    private fun singers(vararg names: String) = names.associateWith { listOf("singer") }

    private fun chaleya() = query(title = "Chaleya", artist = "Arijit Singh, Shilpa Rao", album = "Jawan - Hindi", durationMs = 201_000L)

    private fun sajni() = query(title = "Sajni", artist = "Arijit Singh", album = "Laapataa Ladies", durationMs = 171_000L)

    private fun channaMereya() = query(
        title = "Channa Mereya (From \"Ae Dil Hai Mushkil\")",
        artist = "Arijit Singh",
        album = "Channa Mereya (From \"Ae Dil Hai Mushkil\")",
        durationMs = 290_000L
    )

    private fun meriAashiqui(artist: String = "Arijit Singh, Palak Muchhal") =
        query(title = "Meri Aashiqui", artist = artist, album = "Aashiqui 2", durationMs = 266_000L)

    private fun heeriye() = query(title = "Heeriye", artist = "Jasleen Royal, Arijit Singh", album = "Heeriye", durationMs = 195_000L)

    private fun tereVaaste() = query(
        title = "Tere Vaaste (From \"Zara Hatke Zara Bachke\")",
        artist = "Varun Jain, Sachin-Jigar, Altamash Faridi",
        album = "Tere Vaaste (From \"Zara Hatke Zara Bachke\")",
        durationMs = 190_000L
    )

    private fun kunFayaKun() =
        query(title = "Kun Faya Kun", artist = "A.R. Rahman, Javed Ali, Mohit Chauhan", album = "Rockstar", durationMs = 471_000L)

    private val heeriyeCredits = mapOf(
        "Jasleen Royal" to listOf("music", "starring"),
        "Arijit Singh" to listOf("music"),
        "Dulquer Salmaan" to listOf("music", "starring")
    )

    private val heeriyeOriginal = song(
        "NIidiD9g",
        "Heeriye (feat. Arijit Singh)",
        listOf("Dulquer Salmaan", "Jasleen Royal", "Arijit Singh"),
        "Heeriye (feat. Arijit Singh)",
        194,
        heeriyeCredits
    )

    private val heeriyeLofi = song(
        "XXaCE1xt",
        "Heeriye Lofi Remix (feat. Arijit Singh)",
        listOf("Dulquer Salmaan", "Jasleen Royal", "Arijit Singh"),
        "Heeriye Lofi Remix (feat. Arijit Singh)",
        196,
        heeriyeCredits
    )

    private val heeriyeOtherFilm = song(
        "y_1n5ZOS",
        "Heeriye",
        listOf("Arijit Singh", "Shreya Ghoshal"),
        "Happy Hardy And Heer",
        195,
        singers("Arijit Singh", "Shreya Ghoshal")
    )

    private val tereVaasteCredits = mapOf(
        "Sachin-Jigar" to listOf("music"),
        "Varun Jain" to listOf("music"),
        "Shadab Faridi" to listOf("music"),
        "Altamash Faridi" to listOf("music"),
        "Amitabh Bhattacharya" to listOf("music", "lyricist"),
        "Vicky Kaushal" to listOf("starring")
    )

    private val tereVaasteArtists = listOf("Amitabh Bhattacharya", "Sachin-Jigar", "Varun Jain", "Shadab Faridi", "Altamash Faridi")

    private val tereVaasteOriginal = song(
        "rBltEr7X",
        "Tere Vaaste (From &quot;Zara Hatke Zara Bachke&quot;)",
        tereVaasteArtists,
        "Zara Hatke Zara Bachke",
        189,
        tereVaasteCredits
    )

    private val tereVaasteUnplugged = song("MdCnqlfo", "Tere Vaaste - Unplugged", tereVaasteArtists, "Zara Hatke Zara Bachke", 189, tereVaasteCredits)

    private val kunArtists = listOf("A.R. Rahman", "Javed Ali", "Mohit Chauhan")

    private val kunCredits = composerSinger("A.R. Rahman") + singers("Javed Ali", "Mohit Chauhan") +
        mapOf("Irshad Kamil" to listOf("lyricist"), "Ranbir Kapoor" to listOf("starring"))

    private val kunFaayaKunOriginal = song("csaEsVWV", "Kun Faaya Kun", kunArtists, "Rockstar", 473, kunCredits)

    private val kunFaayaKunCompilation = song("haS4X2V2", "Kun Faaya Kun", kunArtists, "Best Of A.R. Rahman", 469, kunCredits)

    private val chaleyaCredits = composerSinger("Anirudh Ravichander") + singers("Arijit Singh", "Shilpa Rao") +
        mapOf("Kumaar" to listOf("lyricist"), "Shah Rukh Khan" to listOf("starring"), "Nayanthara" to listOf("starring"))

    private val chaleyaOriginal =
        song("faloMmjX", "Chaleya", listOf("Anirudh Ravichander", "Arijit Singh", "Shilpa Rao"), "Jawan", 200, chaleyaCredits)

    private val chaleyaCompilation = song(
        "comp0001",
        "Chaleya (From &quot;Jawan&quot;)",
        listOf("Arijit Singh", "Shilpa Rao", "Anirudh Ravichander", "Kumaar"),
        "Arijit&#039;s Aura",
        200,
        chaleyaCredits
    )

    private val chaleyaUnplugged =
        song("unplug01", "Chaleya (Unplugged)", listOf("Anirudh Ravichander", "Arijit Singh", "Shilpa Rao"), "Jawan", 199, chaleyaCredits)

    private val sajniCredits = composerSinger("Ram Sampath") + singers("Arijit Singh") +
        mapOf("Prashant Pandey" to listOf("singer", "lyricist"), "Nitanshi Goel" to listOf("starring"))

    private val sajniOriginal =
        song("FB8WBiWv", "Sajni", listOf("Ram Sampath", "Arijit Singh", "Prashant Pandey"), "Laapataa Ladies", 170, sajniCredits)

    private val channaMereyaCredits = composerSinger("Pritam") + singers("Arijit Singh") +
        mapOf("Amitabh Bhattacharya" to listOf("lyricist"), "Ranbir Kapoor" to listOf("starring"))

    private val channaMereyaOriginal =
        song("uiEWT3kP", "Channa Mereya", listOf("Pritam", "Arijit Singh"), "Ae Dil Hai Mushkil", 289, channaMereyaCredits)

    private val channaMereyaDeluxe =
        song("36EzeLeh", "Channa Mereya", listOf("Pritam", "Arijit Singh"), "Ae Dil Hai Mushkil  [Deluxe Edition]", 289, channaMereyaCredits)

    private val channaMereyaUnplugged =
        song("xzzuV3iW", "Channa Mereya (Unplugged)", listOf("Pritam", "Arijit Singh"), "Ae Dil Hai Mushkil  [Deluxe Edition]", 165, channaMereyaCredits)

    private val meriAashiquiCredits = mapOf(
        "Mithoon" to listOf("music"),
        "Arijit Singh" to listOf("singer"),
        "Palak Muchhal" to listOf("singer"),
        "Irshad Kamil" to listOf("lyricist")
    )

    private val meriAashiquiOriginal =
        song("Z0teQlYo", "Meri Aashiqui", listOf("Arijit Singh", "Palak Muchhal"), "Aashiqui 2", 266, meriAashiquiCredits)

    private val meriAashiquiCompilation = song(
        "OFkbNs2Q",
        "Meri Aashiqui",
        listOf("Mithoon", "Palak Muchhal", "Arijit Singh"),
        "Sound Of Bollywood - Vol. 2",
        267,
        meriAashiquiCredits
    )
}
