package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

class VlcAudioPlayer private constructor(
    private val factory: MediaPlayerFactory,
    val nativePath: String
) : AudioPlayer {

    private val mediaPlayer: MediaPlayer = factory.mediaPlayers().newMediaPlayer()
    private val released = AtomicBoolean(false)
    private val eventFlow = MutableSharedFlow<PlayerEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )

    override val events: SharedFlow<PlayerEvent> = eventFlow.asSharedFlow()

    init {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun opening(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Opening)
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                emit(PlayerEvent.Buffering(newCache))
            }

            override fun playing(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Playing)
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Paused)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Stopped)
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Finished)
            }

            override fun error(mediaPlayer: MediaPlayer) {
                emit(PlayerEvent.Failed("Riproduzione interrotta da VLC"))
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                emit(PlayerEvent.TimeChanged(newTime.coerceAtLeast(0L)))
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                emit(PlayerEvent.LengthChanged(newLength.coerceAtLeast(0L)))
            }
        })
    }

    override fun play(url: String, startAtMs: Long) {
        if (released.get()) return
        val options = buildList {
            add(":http-user-agent=${ExtractorHttp.DESKTOP_USER_AGENT}")
            add(":http-referrer=https://www.youtube.com/")
            add(":no-video")
            if (startAtMs > 0L) {
                add(":start-time=%.3f".format(Locale.ROOT, startAtMs / 1000.0))
            }
        }
        mediaPlayer.media().play(url, *options.toTypedArray())
    }

    override fun resume() {
        if (released.get()) return
        mediaPlayer.controls().setPause(false)
    }

    override fun pause() {
        if (released.get()) return
        mediaPlayer.controls().setPause(true)
    }

    override fun stop() {
        if (released.get()) return
        mediaPlayer.controls().stop()
    }

    override fun seekTo(positionMs: Long) {
        if (released.get()) return
        if (!mediaPlayer.status().isSeekable) return
        mediaPlayer.controls().setTime(positionMs.coerceAtLeast(0L))
    }

    override fun setVolume(volume: Int) {
        if (released.get()) return
        mediaPlayer.audio().setVolume(volume.coerceIn(0, 100))
    }

    override fun setMuted(muted: Boolean) {
        if (released.get()) return
        mediaPlayer.audio().setMute(muted)
    }

    override fun applyEqualizer(enabled: Boolean, preamp: Float, amps: List<Float>) {
        if (released.get()) return
        if (!enabled) {
            runCatching { mediaPlayer.audio().setEqualizer(null) }
            return
        }
        runCatching {
            val equalizer = factory.equalizer().newEqualizer()
            equalizer.setPreamp(preamp)
            val bandCount = equalizer.bandCount()
            for (band in 0 until bandCount) {
                equalizer.setAmp(band, amps.getOrElse(band) { 0f })
            }
            mediaPlayer.audio().setEqualizer(equalizer)
        }
    }

    override fun setSpeed(speed: Float) {
        if (released.get()) return
        runCatching { mediaPlayer.controls().setRate(speed.coerceIn(MIN_RATE, MAX_RATE)) }
    }

    override fun positionMs(): Long = if (released.get()) 0L else mediaPlayer.status().time().coerceAtLeast(0L)

    override fun durationMs(): Long = if (released.get()) 0L else mediaPlayer.status().length().coerceAtLeast(0L)

    override fun isPlaying(): Boolean = !released.get() && mediaPlayer.status().isPlaying

    override fun close() {
        if (!released.compareAndSet(false, true)) return
        runCatching { mediaPlayer.release() }
        runCatching { factory.release() }
    }

    private fun emit(event: PlayerEvent) {
        eventFlow.tryEmit(event)
    }

    companion object {
        private const val MIN_RATE = 0.5f
        private const val MAX_RATE = 2f

        private val FACTORY_ARGUMENTS = arrayOf(
            "--intf=dummy",
            "--no-video",
            "--no-metadata-network-access",
            "--no-sub-autodetect-file",
            "--network-caching=3000",
            "--quiet"
        )

        fun create(preferredDirectory: String = "", bundledDirectory: Path? = null): VlcAudioPlayer {
            val discovery = VlcNativeLocator.discover(preferredDirectory, bundledDirectory)
            if (!discovery.available) {
                throw AudioPlayerUnavailableException(
                    buildString {
                        append("Runtime VLC non trovato. Installa VLC a 64 bit oppure indica la cartella in Impostazioni.")
                        if (discovery.searchedDirectories.isNotEmpty()) {
                            append(" Percorsi verificati: ")
                            append(discovery.searchedDirectories.joinToString(", "))
                        }
                    }
                )
            }
            val factory = try {
                MediaPlayerFactory(*FACTORY_ARGUMENTS)
            } catch (error: Throwable) {
                throw AudioPlayerUnavailableException(
                    "Impossibile inizializzare libvlc da ${discovery.path}: ${error.message.orEmpty()}",
                    error
                )
            }
            return VlcAudioPlayer(factory, discovery.path)
        }
    }
}
