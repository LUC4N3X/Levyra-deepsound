package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GentleWakePlaybackCoordinatorTest {
    private val station = RadioStation(
        uuid = "station-1",
        name = "Morning FM",
        streamUrl = "https://stream.example.org/live.mp3",
        resolvedStreamUrl = "https://edge.example.org/live.mp3",
        faviconUrl = "https://example.org/icon.png",
        homepageUrl = "",
        country = "Italy",
        countryCode = "IT",
        language = "italian",
        tags = listOf("pop"),
        codec = "MP3",
        bitrateKbps = 128,
        votes = 10,
        clickCount = 10,
        lastCheckOk = true
    )

    @Test
    fun radioPayloadKeepsTheStationStreamSoTheAlarmCanPlayIt() {
        val payload = GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(station.toTrack()))

        val decoded = GentleWakePlaybackCoordinator.decodePayloadTracks(payload).single()

        assertTrue(decoded.isLiveRadio())
        assertEquals("https://edge.example.org/live.mp3", decoded.streamUrl)
        assertTrue(GentleWakePlaybackCoordinator.isRemoteAlarmCandidate(decoded))
    }

    @Test
    fun legacyRadioPayloadWithoutStreamIsNotPlayableAndFallsBackToTheTone() {
        val legacy = JSONArray(GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(station.toTrack())))
        legacy.getJSONObject(0).remove("streamUrl")

        val decoded = GentleWakePlaybackCoordinator.decodePayloadTracks(legacy.toString()).single()

        assertEquals("", decoded.streamUrl)
        assertFalse(GentleWakePlaybackCoordinator.isRemoteAlarmCandidate(decoded))
    }

    @Test
    fun payloadRejectsRadioStreamsThatPointAtLocalTargets() {
        val payload = JSONArray(GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(station.toTrack())))
        payload.getJSONObject(0).put("streamUrl", "http://127.0.0.1:8080/live")

        val decoded = GentleWakePlaybackCoordinator.decodePayloadTracks(payload.toString()).single()

        assertEquals("", decoded.streamUrl)
        assertFalse(GentleWakePlaybackCoordinator.isRemoteAlarmCandidate(decoded))
    }

    @Test
    fun localAudioPayloadKeepsItsUriWhileRemoteSongStreamsAreResolvedAgain() {
        val local = station.toTrack().copy(
            id = "local:42",
            source = "LocalLibrary",
            streamUrl = "content://media/external/audio/media/42"
        )
        val song = station.toTrack().copy(
            id = "dQw4w9WgXcQ",
            source = "YouTube Music",
            streamUrl = "https://rr1.example.googlevideo.com/videoplayback?expire=1"
        )

        val decoded = GentleWakePlaybackCoordinator.decodePayloadTracks(
            GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(local, song))
        )

        assertEquals("content://media/external/audio/media/42", decoded[0].streamUrl)
        assertEquals("", decoded[1].streamUrl)
        assertTrue(GentleWakePlaybackCoordinator.isRemoteAlarmCandidate(decoded[1]))
    }

    @Test
    fun alarmFallsBackToTheToneOnPlayerErrorsAndStalledStreams() {
        val timeout = GentleWakePlaybackCoordinator.SOURCE_STALL_TIMEOUT_MS

        assertTrue(fallback(hasPlayerError = true, playWhenReady = true, ready = false, stalledFor = 0L))
        assertTrue(fallback(hasPlayerError = false, playWhenReady = true, ready = false, stalledFor = timeout))
        assertFalse(fallback(hasPlayerError = false, playWhenReady = true, ready = false, stalledFor = timeout - 1))
        assertFalse(fallback(hasPlayerError = false, playWhenReady = true, ready = true, stalledFor = timeout * 2))
        assertFalse(fallback(hasPlayerError = false, playWhenReady = false, ready = false, stalledFor = timeout * 2))
    }

    private fun fallback(hasPlayerError: Boolean, playWhenReady: Boolean, ready: Boolean, stalledFor: Long) =
        GentleWakePlaybackCoordinator.shouldFallBackToTone(
            hasPlayerError = hasPlayerError,
            playWhenReady = playWhenReady,
            playbackReady = ready,
            lastHealthyAtMs = 1_000L,
            nowMs = 1_000L + stalledFor
        )
}
