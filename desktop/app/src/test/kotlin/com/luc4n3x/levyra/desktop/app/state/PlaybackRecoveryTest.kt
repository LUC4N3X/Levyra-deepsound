package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.core.sponsorblock.SponsorBlockRepository
import com.luc4n3x.levyra.desktop.core.storage.*
import com.luc4n3x.levyra.desktop.core.stream.*
import com.luc4n3x.levyra.desktop.player.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun recoveryResumesInPlaceAndPlayingDoesNotResetItsLimit() = runTest {
        val player = FakePlayer()
        val resolver = FakeResolver()
        val controller = controller(backgroundScope, player, resolver)
        try {
            controller.playTrack(track("one"))
            runCurrent()
            player.events.emit(PlayerEvent.Playing)
            player.events.emit(PlayerEvent.TimeChanged(65_000))
            player.events.emit(PlayerEvent.Failed("STREAM_EXPIRED"))
            runCurrent()
            assertEquals(listOf(0L, 65_000L), player.starts.map { it.second })
            assertEquals(1, resolver.invalidations)
            player.events.emit(PlayerEvent.Playing)
            player.events.emit(PlayerEvent.Failed("HTTP_403"))
            runCurrent()
            assertEquals(2, player.starts.size)
            assertEquals(PlaybackStatus.IDLE, controller.state.value.status)
        } finally { controller.shutdown() }
    }

    @Test fun pauseDuringRecoveryCancelsResolutionUntilUserResumes() = runTest {
        val player = FakePlayer()
        val resolver = FakeResolver()
        val controller = controller(backgroundScope, player, resolver)
        try {
            controller.playTrack(track("one"))
            runCurrent()
            player.events.emit(PlayerEvent.Playing)
            player.events.emit(PlayerEvent.TimeChanged(61_000))
            resolver.pending = CompletableDeferred()
            player.events.emit(PlayerEvent.Failed("BRIDGE_UPSTREAM_TRUNCATED"))
            runCurrent()
            controller.togglePlayPause()
            resolver.pending!!.complete(Unit)
            runCurrent()
            assertEquals(1, player.starts.size)
            assertEquals(PlaybackStatus.PAUSED, controller.state.value.status)
            controller.togglePlayPause()
            runCurrent()
            assertEquals(61_000L, player.starts.last().second)
        } finally { controller.shutdown() }
    }

    @Test fun skippedResolutionCannotTakeOverNewTrack() = runTest {
        val player = FakePlayer()
        val resolver = FakeResolver().apply { pending = CompletableDeferred() }
        val controller = controller(backgroundScope, player, resolver)
        try {
            controller.playTrack(track("one"))
            runCurrent()
            val obsolete = resolver.pending!!
            resolver.pending = null
            controller.playTrack(track("two"))
            runCurrent()
            obsolete.complete(Unit)
            runCurrent()
            assertEquals(listOf("https://audio.example/two"), player.starts.map { it.first })
            assertEquals("two", controller.state.value.current?.id)
        } finally { controller.shutdown() }
    }

    @Test fun failedSourceWhilePausedIsRefreshedBeforeTheFirstResumeAttempt() = runTest {
        val player = FakePlayer()
        val resolver = FakeResolver()
        val controller = controller(backgroundScope, player, resolver)
        try {
            controller.playTrack(track("one"))
            runCurrent()
            player.events.emit(PlayerEvent.Playing)
            player.events.emit(PlayerEvent.TimeChanged(61_000))
            runCurrent()
            controller.togglePlayPause()
            player.events.emit(PlayerEvent.Failed("HTTP_403"))
            runCurrent()
            assertEquals(PlaybackStatus.PAUSED, controller.state.value.status)
            assertEquals(1, player.starts.size)
            assertEquals(1, resolver.invalidations)
            controller.togglePlayPause()
            runCurrent()
            assertEquals("https://audio.example/one?fresh=1", player.starts.last().first)
            assertEquals(61_000L, player.starts.last().second)
        } finally { controller.shutdown() }
    }

    @Test fun selectingAnotherCardStopsOldAudioAndStartsTheSelectionAutomatically() = runTest {
        val player = FakePlayer()
        val resolver = FakeResolver()
        val controller = controller(backgroundScope, player, resolver)
        try {
            controller.playTrack(track("one"))
            runCurrent()
            player.events.emit(PlayerEvent.Playing)
            runCurrent()
            resolver.pending = CompletableDeferred()
            controller.playTracks(listOf(track("one"), track("two")), 1)
            runCurrent()
            assertEquals("two", controller.state.value.current?.id)
            assertNull("Old audio must stop while the selected source resolves", player.playingUrl)
            resolver.pending!!.complete(Unit)
            runCurrent()
            assertEquals("https://audio.example/two", player.playingUrl)
            assertEquals(0L, player.starts.last().second)
        } finally { controller.shutdown() }
    }

    private fun controller(scope: kotlinx.coroutines.CoroutineScope, player: FakePlayer, resolver: FakeResolver): PlaybackController {
        val paths = AppPaths(temporary.newFolder().toPath()).prepare()
        val settings = SettingsStore.create(paths).apply {
            update { it.copy(sponsorBlock = false, preloadNextTrack = false, autoplayRadio = false) }
        }
        return PlaybackController(scope, resolver, CatalogRepository(), settings, LibraryStore.create(paths),
            SessionStore.create(paths), SponsorBlockRepository(), { player })
    }

    private fun track(id: String) = Track(id, id, "Artist", videoUrl = Track.watchUrlOf(id), durationMs = 200_000)

    private class FakeResolver : StreamResolver {
        var invalidations = 0
        var pending: CompletableDeferred<Unit>? = null
        override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
            pending?.await()
            val suffix = if (invalidations == 0) "" else "?fresh=$invalidations"
            return ResolvedAudio("https://audio.example/${track.id}$suffix", "AAC", 0, track.durationMs, "", track.title, track.artist)
        }
        override fun invalidate(track: Track) { invalidations++ }
    }

    private class FakePlayer : AudioPlayer {
        override val events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 32)
        val starts = mutableListOf<Pair<String, Long>>()
        var playingUrl: String? = null
        override fun play(url: String, startAtMs: Long) { starts += url to startAtMs; playingUrl = url }
        override fun prepare(url: String, startAtMs: Long) = false
        override fun startPrepared() = false
        override fun createCompanion(): AudioPlayer? = null
        override fun resume() = Unit
        override fun pause() = Unit
        override fun stop() { playingUrl = null }
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(volume: Int) = Unit
        override fun setMuted(muted: Boolean) = Unit
        override fun applyEqualizer(enabled: Boolean, preamp: Float, amps: List<Float>) = Unit
        override fun outputDevices() = emptyList<AudioOutputDevice>()
        override fun applyOutputDevice(deviceId: String) = Unit
        override fun setSpeed(speed: Float) = true
        override fun positionMs() = 0L
        override fun durationMs() = 200_000L
        override fun isPlaying() = playingUrl != null
        override fun close() = Unit
    }
}
