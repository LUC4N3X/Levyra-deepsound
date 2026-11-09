package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.hqaudio.lossless.KuwoPayloadParser
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalPayload
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LosslessSitesMatchingTest {
    private val matcher = AlternativeTrackMatcher()

    private val tidalRows = listOf(
        """{"id":20115564,"title":"Get Lucky","version":"feat. Pharrell Williams and Nile Rodgers","duration":370,"explicit":false,"isrc":"USQX91300108","allowStreaming":true,"streamReady":true,"artists":[{"name":"Daft Punk"},{"name":"Pharrell Williams"},{"name":"Nile Rodgers"}],"album":{"title":"Random Access Memories"},"mediaMetadata":{"tags":["LOSSLESS"]}}""",
        """{"id":19823990,"title":"Get Lucky","version":"Radio Edit - feat. Pharrell Williams and Nile Rodgers","duration":248,"explicit":false,"isrc":"USQX91300809","allowStreaming":true,"streamReady":true,"artists":[{"name":"Daft Punk"},{"name":"Pharrell Williams"},{"name":"Nile Rodgers"}],"album":{"title":"Get Lucky (Radio Edit - feat. Pharrell Williams and Nile Rodgers)"},"mediaMetadata":{"tags":["LOSSLESS","HIRES_LOSSLESS"]}}""",
        """{"id":20968321,"title":"Get Lucky","version":"Daft Punk Remix","duration":633,"explicit":false,"isrc":"USQX91301322","allowStreaming":true,"streamReady":true,"artists":[{"name":"Daft Punk"},{"name":"Pharrell Williams"},{"name":"Nile Rodgers"}],"album":{"title":"Get Lucky (feat. Pharrell Williams & Nile Rodgers)"},"mediaMetadata":{"tags":["LOSSLESS","HIRES_LOSSLESS"]}}""",
        """{"id":77440248,"title":"Get lucky feat Nile Rodgers Pharrell Williams","version":null,"duration":369,"explicit":false,"isrc":"FR0Z50017631","allowStreaming":true,"streamReady":true,"artists":[{"name":"Daft Punk"}],"album":{"title":"Until the morning 2"},"mediaMetadata":{"tags":["LOSSLESS"]}}""",
        """{"id":1,"title":"Blocked","duration":200,"allowStreaming":false,"artists":[{"name":"Daft Punk"}]}"""
    )

    private val kuwoSearch = """{"code":0,"msg":"ok","data":{"total":4,"list":[
        {"id":"3363737","name":"Get Lucky (feat. Pharrell Williams and Nile Rodgers)","artist":"Daft Punk&amp;Pharrell Williams&amp;Nile Rodgers","album_name":"Random Access Memories","duration":"369","minfo":[{"level":"ff","bitrate":"2000","format":"flac","size":"41.51Mb"},{"level":"p","bitrate":"320","format":"mp3","size":"14.1Mb"}],"time":1794138524,"sign":"84d4a743fc18d2a255f6b0d187affd9c"},
        {"id":"6982665","name":"Get Lucky","artist":"Daft Punk&Pharrell Williams&Nile Rodgers","album_name":"Wedding Reception Party-Pleasers (Explicit)","duration":"249","minfo":[{"level":"ff","bitrate":"2000","format":"flac","size":"29.54Mb"}],"time":1794138524,"sign":"4ef77740a486e8c8d3ff585564303bfb"},
        {"id":"777","name":"Get Lucky","artist":"Daft Punk","album_name":"Mp3 only","duration":"369","minfo":[{"level":"p","bitrate":"320","format":"mp3","size":"14.1Mb"}],"time":1794138524,"sign":"ba318be3f2410a32457aa295d0281237"},
        {"id":"778","name":"Get Lucky","artist":"Daft Punk","album_name":"Unsigned","duration":"369","minfo":[{"level":"ff","bitrate":"2000","format":"flac"}],"time":1794138524,"sign":"not-a-sign"}
    ]}}"""

    private fun tidalCandidates() = tidalRows.mapNotNull { TidalPayload.candidate(JSONObject(it)) }

    private fun kuwoCandidates() = KuwoPayloadParser.searchPage(kuwoSearch)!!

    private fun selectedId(query: AlternativeTrackQuery, candidates: List<AlternativeTrackCandidate>): String? =
        (matcher.select(query, candidates) as? AlternativeMatchSelection.Accepted)?.evaluation?.candidate?.providerTrackId

    private val albumVersion = query(
        title = "Get Lucky (feat. Pharrell Williams & Nile Rodgers)",
        artist = "Daft Punk",
        album = "Random Access Memories",
        durationMs = 369_000L
    )

    @Test
    fun tidalRowsKeepVersionsInTitleAndDropBlockedTracks() {
        val candidates = tidalCandidates()
        assertEquals(listOf("20115564", "19823990", "20968321", "77440248"), candidates.map { it.providerTrackId })
        assertTrue(candidates[1].title.contains("Radio Edit"))
        assertTrue(candidates[2].title.contains("Remix"))
        assertEquals("HI_RES_LOSSLESS", candidates[1].mediaToken)
        assertEquals("USQX91300108", candidates[0].isrc)
    }

    @Test
    fun kuwoRowsNeedFlacAndTheSiteSignature() {
        val candidates = kuwoCandidates()
        assertEquals(listOf("3363737", "6982665"), candidates.map { it.providerTrackId })
        assertEquals(listOf("Daft Punk"), candidates[0].primaryArtists)
        assertEquals(listOf("Pharrell Williams", "Nile Rodgers"), candidates[0].featuredArtists)
        val signature = KuwoPayloadParser.signature(candidates[0].mediaToken)!!
        assertEquals("84d4a743fc18d2a255f6b0d187affd9c", signature.sign)
        assertEquals("2000", signature.bitrate)
        assertNull(KuwoPayloadParser.signature("1|short|2000"))
    }

    @Test
    fun kuwoStreamUrlMustStayFlacForTheRequestedSong() {
        val flac = """{"code":0,"data":{"url":"https://other-er.kuwo.cn/a/F0.flac","format":"flac","bitrate":2000,"songid":3363737}}"""
        val mp3 = """{"code":0,"data":{"url":"https://other-lv.kuwo.cn/a/M5.mp3","format":"mp3","bitrate":128,"songid":3363737}}"""
        assertEquals("https://other-er.kuwo.cn/a/F0.flac", KuwoPayloadParser.flacUrl(flac, "3363737"))
        assertNull(KuwoPayloadParser.flacUrl(mp3, "3363737"))
        assertNull(KuwoPayloadParser.flacUrl(flac, "6982665"))
        assertNull(KuwoPayloadParser.flacUrl("""{"code":1,"msg":"error"}""", "3363737"))
    }

    @Test
    fun albumVersionIsChosenOnBothSites() {
        assertEquals("20115564", selectedId(albumVersion, tidalCandidates()))
        assertEquals("3363737", selectedId(albumVersion, kuwoCandidates()))
    }

    @Test
    fun matchingNeverFallsBackToAnotherCut() {
        val onlyOtherCuts = tidalCandidates().filter { it.providerTrackId in setOf("19823990", "20968321") }
        assertNull(selectedId(albumVersion, onlyOtherCuts))
        assertNull(selectedId(albumVersion, kuwoCandidates().filter { it.providerTrackId == "6982665" }))
    }

    @Test
    fun radioEditRequestNeverReceivesTheAlbumOrRemixCut() {
        val radioEdit = query(
            title = "Get Lucky (Radio Edit) [feat. Pharrell Williams & Nile Rodgers]",
            artist = "Daft Punk",
            album = "Get Lucky",
            durationMs = 248_000L
        )
        val selected = selectedId(radioEdit, tidalCandidates())
        assertNotEquals("20115564", selected)
        assertNotEquals("20968321", selected)
    }

    private val kuwoBlindingLights = """{"code":0,"data":{"list":[
        {"id":"92022392","name":"Blinding Lights","artist":"The Weeknd","album_name":"After Hours (Explicit)","duration":"200","minfo":[{"bitrate":"2000","format":"flac"}],"time":1,"sign":"84d4a743fc18d2a255f6b0d187affd9c"},
        {"id":"150160327","name":"Blinding Lights","artist":"The Weeknd","album_name":"Blinding Lights (Instrumental)","duration":"202","minfo":[{"bitrate":"2000","format":"flac"}],"time":1,"sign":"84d4a743fc18d2a255f6b0d187affd9c"},
        {"id":"150195943","name":"Blinding Lights","artist":"The Weeknd&Major Lazer","album_name":"Blinding Lights","duration":"197","minfo":[{"bitrate":"2000","format":"flac"}],"time":1,"sign":"84d4a743fc18d2a255f6b0d187affd9c"},
        {"id":"92749568","name":"Blinding Lights","artist":"The Weeknd&Chromatics","album_name":"Blinding Lights","duration":"261","minfo":[{"bitrate":"2000","format":"flac"}],"time":1,"sign":"84d4a743fc18d2a255f6b0d187affd9c"},
        {"id":"158059419","name":"Blinding Lights","artist":"The Weeknd&ROSALÍA","album_name":"Blinding Lights (Remix)","duration":"216","minfo":[{"bitrate":"2000","format":"flac"}],"time":1,"sign":"84d4a743fc18d2a255f6b0d187affd9c"}
    ]}}"""

    @Test
    fun kuwoVersionHiddenInTheAlbumNeverReplacesTheOriginal() {
        val candidates = KuwoPayloadParser.searchPage(kuwoBlindingLights)!!
        val original = query(explicit = null)
        assertEquals("92022392", selectedId(original, candidates))
        assertEquals(true, candidates.first().explicit)
        assertEquals("After Hours", candidates.first().album)
        assertEquals("Blinding Lights (Instrumental)", candidates[1].title)
        listOf("150160327", "150195943", "92749568", "158059419").forEach { id ->
            assertNull("$id must not match", selectedId(original, candidates.filter { it.providerTrackId == id }))
        }
    }

    @Test
    fun officialMusicVideoFindsTheShorterAlbumRecordingOnlyWhenEverythingElseIsExact() {
        val video = query(durationMs = 263_000L, explicit = null).copy(musicVideo = true)
        val kuwo = KuwoPayloadParser.searchPage(kuwoBlindingLights)!!
        assertEquals("92022392", selectedId(video, kuwo))
        listOf("150160327", "150195943", "92749568", "158059419").forEach { id ->
            assertNull("$id must not match the video", selectedId(video, kuwo.filter { it.providerTrackId == id }))
        }
        val jioSaavnAlbumCut = candidate(id = "fW-Mxsnu", duration = 200, explicit = null)
        assertEquals("fW-Mxsnu", selectedId(video, listOf(jioSaavnAlbumCut)))
        assertNull(selectedId(video.copy(musicVideo = false), listOf(jioSaavnAlbumCut)))
        assertNull(selectedId(video, listOf(jioSaavnAlbumCut.copy(album = "Blinding Lights"))))
        assertNull(selectedId(video.copy(durationMs = 400_000L), listOf(jioSaavnAlbumCut)))
        assertNull(selectedId(video, listOf(jioSaavnAlbumCut.copy(durationSeconds = 270))))
    }

    @Test
    fun oneLetterArtistMisspellingMatchesOnlyTheExactSongAndDuration() {
        val misspelled = query(title = "The Hills", artist = "The Weekend", album = "", durationMs = 243_000L, explicit = null)
        val hills = candidate(id = "kfeKlqFe", title = "The Hills", primary = listOf("The Weeknd"), album = "Beauty Behind The Madness", duration = 242, explicit = null)
        assertEquals("kfeKlqFe", selectedId(misspelled, listOf(hills)))
        assertNull(selectedId(misspelled, listOf(hills.copy(durationSeconds = 250))))
        val starboy = query(title = "Starboy (feat. Daft Punk)", artist = "The Weekend", album = "", durationMs = 231_000L, explicit = null)
        val starboyCut = candidate(id = "_Qp6DOPM", title = "Starboy", primary = listOf("The Weeknd"), featured = listOf("Daft Punk"), album = "Starboy", duration = 227, explicit = null)
        assertEquals("_Qp6DOPM", selectedId(starboy, listOf(starboyCut)))
        assertNull(selectedId(misspelled, listOf(hills.copy(title = "The Hills (Remix)"))))
        assertNull(selectedId(misspelled.copy(artist = "The Weekends Band"), listOf(hills)))
        assertNull(selectedId(query(title = "Hello", artist = "Adele", album = "", durationMs = 295_000L), listOf(candidate(title = "Hello", primary = listOf("Adela"), duration = 295))))
    }

    @Test
    fun isrcMismatchRejectsOtherwiseIdenticalRecording() {
        val withIsrc = albumVersion.copy(isrc = "USQX91300108")
        assertEquals("20115564", selectedId(withIsrc, tidalCandidates()))
        assertNull(selectedId(withIsrc.copy(isrc = "GBAAA0000001"), tidalCandidates().filter { it.providerTrackId == "20115564" }))
    }
}
