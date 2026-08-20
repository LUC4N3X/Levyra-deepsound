package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import java.net.URI
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

internal data class YoutubePlaybackHttpOptions(
    val userAgent: String,
    val referrer: String?
)

internal fun youtubePlaybackHttpOptions(url: String): YoutubePlaybackHttpOptions? {
    val host = runCatching { URI(url).host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
    val youtubeMedia = host.endsWith("googlevideo.com") ||
        host.endsWith("youtube.com") ||
        host.endsWith("youtube-nocookie.com") ||
        host.endsWith("ytimg.com")
    if (!youtubeMedia) return null

    val userAgent = when {
        runCatching { YoutubeParsingHelper.isIosStreamingUrl(url) }.getOrDefault(false) ->
            YoutubeParsingHelper.getIosUserAgent(null)
        runCatching { YoutubeParsingHelper.isAndroidStreamingUrl(url) }.getOrDefault(false) ->
            YoutubeParsingHelper.getAndroidUserAgent(null)
        else -> YOUTUBE_MOBILE_WEB_USER_AGENT
    }
    val web = runCatching { YoutubeParsingHelper.isWebStreamingUrl(url) }.getOrDefault(false)
    val embedded = runCatching {
        YoutubeParsingHelper.isTvHtml5SimplyEmbeddedPlayerStreamingUrl(url)
    }.getOrDefault(false)
    val referrer = when {
        embedded -> "https://www.youtube.com/embed/"
        web -> "https://www.youtube.com/"
        else -> null
    }
    return YoutubePlaybackHttpOptions(userAgent, referrer)
}

private const val YOUTUBE_MOBILE_WEB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"

private val youtubeRequestNumber = AtomicLong(1L)

internal fun youtubePlaybackUrl(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull() ?: return url
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    val path = uri.path.orEmpty().lowercase(Locale.ROOT)
    if (!host.endsWith("googlevideo.com") || !path.contains("videoplayback")) return url
    if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return url
    if (Regex("(?:^|[?&])sq=", RegexOption.IGNORE_CASE).containsMatchIn(url)) return url
    if (Regex("(?:^|[?&])rn=", RegexOption.IGNORE_CASE).containsMatchIn(url)) return url

    val fragmentIndex = url.indexOf('#')
    val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val separator = if ('?' in base) '&' else '?'
    return "$base${separator}rn=${youtubeRequestNumber.getAndIncrement()}$fragment"
}

internal fun shouldBridgeYoutubePlayback(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    val googleVideo = host == "googlevideo.com" || host.endsWith(".googlevideo.com")
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443) &&
        googleVideo
}

internal class MediaReplacementGuard {
    private val replacing = AtomicBoolean(false)

    fun begin() {
        replacing.set(true)
    }

    fun opened() {
        replacing.set(false)
    }

    fun shouldSuppressTerminalEvent(): Boolean = replacing.get()
}

class VlcAudioPlayer private constructor(
    private val sharedFactory: SharedMediaPlayerFactory,
    val nativePath: String
) : AudioPlayer {

    private val mediaPlayer: MediaPlayer = sharedFactory.factory.mediaPlayers().newMediaPlayer()
    private val released = AtomicBoolean(false)
    private val replacementGuard = MediaReplacementGuard()
    private val bridgeLock = Any()
    private val eventFlow = MutableSharedFlow<PlayerEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )

    private var lastPublishedTimeMs: Long = Long.MIN_VALUE
    private var lastTimePublishNanos: Long = 0L

    @Volatile
    private var loadedUrl: String = ""

    @Volatile
    private var requestedPaused: Boolean = false

    @Volatile
    private var playbackStarted: Boolean = false

    @Volatile
    private var activeBridgeUrl: String = ""

    @Volatile
    private var streamBridge: YoutubeLocalStreamBridge? = null

    @Volatile
    private var selectedOutputDeviceId: String = AudioOutputDevice.SYSTEM_DEFAULT_ID

    @Volatile
    private var appliedOutputDeviceId: String? = null

    override val events: SharedFlow<PlayerEvent> = eventFlow.asSharedFlow()

    init {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun opening(mediaPlayer: MediaPlayer) {
                if (
                    replacementGuard.shouldSuppressTerminalEvent() &&
                    !eventTargetsLoadedMedia(mediaPlayer, allowUnknownMrl = false)
                ) return
                replacementGuard.opened()
                emit(PlayerEvent.Opening)
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                if (shouldIgnoreReplacementEvent(mediaPlayer)) return
                emit(PlayerEvent.Buffering(newCache))
            }

            override fun playing(mediaPlayer: MediaPlayer) {
                if (shouldIgnoreReplacementEvent(mediaPlayer)) return
                replacementGuard.opened()
                requestedPaused = false
                playbackStarted = true
                emit(PlayerEvent.Playing)
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                if (shouldIgnoreReplacementEvent(mediaPlayer)) return
                requestedPaused = true
                emit(PlayerEvent.Paused)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                if (replacementGuard.shouldSuppressTerminalEvent()) return
                emit(PlayerEvent.Stopped)
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                if (shouldIgnoreTerminalEvent(mediaPlayer)) return
                val finishedUrl = loadedUrl
                loadedUrl = ""
                requestedPaused = false
                playbackStarted = false
                closeBridgeSession(finishedUrl)
                emit(PlayerEvent.Finished)
            }

            override fun error(mediaPlayer: MediaPlayer) {
                if (shouldIgnoreTerminalEvent(mediaPlayer)) return
                val failedUrl = loadedUrl
                val reason = streamBridge?.failureReason(failedUrl)
                    ?: if (playbackStarted || streamBridge?.didOpenUpstream(failedUrl) == true) {
                        "VLC_DEMUX_FAILED"
                    } else {
                        "VLC_OPEN_FAILED"
                    }
                loadedUrl = ""
                requestedPaused = false
                playbackStarted = false
                closeBridgeSession(failedUrl)
                emit(PlayerEvent.Failed(reason))
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                if (shouldIgnoreReplacementEvent(mediaPlayer)) return
                publishTimeChanged(newTime.coerceAtLeast(0L))
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                if (shouldIgnoreReplacementEvent(mediaPlayer)) return
                emit(PlayerEvent.LengthChanged(newLength.coerceAtLeast(0L)))
            }
        })
    }

    override fun play(url: String, startAtMs: Long) {
        if (released.get()) return
        beginMediaReplacement()
        val playbackUrl = playbackLocation(url) ?: return
        loadedUrl = playbackUrl
        requestedPaused = false
        playbackStarted = false
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        val started = runCatching {
            mediaPlayer.media().play(playbackUrl, *mediaOptions(playbackUrl, startAtMs))
        }.getOrDefault(false)
        if (!started) {
            loadedUrl = ""
            closeBridgeSession(playbackUrl)
            emit(PlayerEvent.Failed("VLC_OPEN_FAILED"))
        }
    }

    override fun prepare(url: String, startAtMs: Long): Boolean {
        if (released.get()) return false
        beginMediaReplacement()
        val playbackUrl = playbackLocation(url) ?: return false
        loadedUrl = playbackUrl
        requestedPaused = true
        playbackStarted = false
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        val started = runCatching {
            mediaPlayer.media().startPaused(playbackUrl, *mediaOptions(playbackUrl, startAtMs))
        }.getOrDefault(false)
        if (!started) {
            loadedUrl = ""
            requestedPaused = false
            closeBridgeSession(playbackUrl)
        }
        return started
    }

    override fun startPrepared(): Boolean {
        if (released.get()) return false
        val started = runCatching {
            mediaPlayer.controls().start()
        }.getOrDefault(false)
        if (started) {
            requestedPaused = false
        } else {
            val failedUrl = loadedUrl
            loadedUrl = ""
            requestedPaused = false
            playbackStarted = false
            closeBridgeSession(failedUrl)
        }
        return started
    }

    override fun createCompanion(): AudioPlayer? {
        if (released.get()) return null
        val retained = sharedFactory.retain() ?: return null
        return runCatching {
            VlcAudioPlayer(retained, nativePath).also { companion ->
                companion.applyOutputDevice(selectedOutputDeviceId)
            }
        }.getOrElse {
            retained.release()
            null
        }
    }

    private fun mediaOptions(url: String, startAtMs: Long): Array<String> = buildList {
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            val youtube = youtubePlaybackHttpOptions(url)
            add(":http-user-agent=${youtube?.userAgent ?: ExtractorHttp.DESKTOP_USER_AGENT}")
            youtube?.referrer?.let { add(":http-referrer=$it") }
            if (youtube != null || url == activeBridgeUrl) {
                add(":http-reconnect")
            }
        }
        add(":no-video")
        if (startAtMs > 0L) {
            add(":start-time=%.3f".format(Locale.ROOT, startAtMs / 1000.0))
        }
    }.toTypedArray()

    override fun resume() {
        if (released.get()) return
        requestedPaused = false
        mediaPlayer.controls().setPause(false)
    }

    override fun pause() {
        if (released.get()) return
        requestedPaused = true
        mediaPlayer.controls().setPause(true)
    }

    override fun stop() {
        if (released.get()) return
        val stoppedUrl = loadedUrl
        loadedUrl = ""
        requestedPaused = false
        playbackStarted = false
        mediaPlayer.controls().stop()
        closeBridgeSession(stoppedUrl)
    }

    override fun seekTo(positionMs: Long) {
        if (released.get()) return
        if (!mediaPlayer.status().isSeekable) return
        resetTimeThrottle(positionMs)
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
            val equalizer = sharedFactory.factory.equalizer().newEqualizer()
            equalizer.setPreamp(preamp)
            val bandCount = equalizer.bandCount()
            for (band in 0 until bandCount) {
                equalizer.setAmp(band, amps.getOrElse(band) { 0f })
            }
            mediaPlayer.audio().setEqualizer(equalizer)
        }
    }

    override fun outputDevices(): List<AudioOutputDevice> {
        if (released.get()) return emptyList()
        val devices = runCatching {
            sharedFactory.factory.audio().audioOutputs().flatMap { output ->
                output.devices.map { device ->
                    AudioOutputDevice.create(
                        outputName = output.name.orEmpty(),
                        deviceId = device.deviceId.orEmpty(),
                        label = device.longName.orEmpty().ifBlank { output.description.orEmpty() }
                    )
                }
            }
        }.getOrDefault(emptyList())
        return AudioOutputDevice.sanitize(devices)
    }

    override fun applyOutputDevice(deviceId: String) {
        if (released.get()) return
        val target = deviceId.trim()
        if (selectedOutputDeviceId == target && appliedOutputDeviceId == target) return
        selectedOutputDeviceId = target
        if (loadedUrl.isBlank()) {
            pushOutputDevice()
            return
        }
        restartLoadedMediaForOutputChange()
    }

    override fun setSpeed(speed: Float): Boolean {
        if (released.get()) return false
        return runCatching {
            mediaPlayer.controls().setRate(DesktopSettings.normalizeSpeed(speed))
        }.getOrDefault(false)
    }

    override fun positionMs(): Long = if (released.get()) 0L else mediaPlayer.status().time().coerceAtLeast(0L)

    override fun durationMs(): Long = if (released.get()) 0L else mediaPlayer.status().length().coerceAtLeast(0L)

    override fun isPlaying(): Boolean = !released.get() && mediaPlayer.status().isPlaying

    override fun close() {
        if (!released.compareAndSet(false, true)) return
        replacementGuard.begin()
        val closingUrl = loadedUrl
        loadedUrl = ""
        closeBridgeSession(closingUrl)
        runCatching { mediaPlayer.release() }
        val closingBridge = synchronized(bridgeLock) {
            streamBridge.also { streamBridge = null }
        }
        runCatching { closingBridge?.close() }
        sharedFactory.release()
    }

    private fun restartLoadedMediaForOutputChange() {
        val url = loadedUrl
        if (url.isBlank() || released.get()) {
            pushOutputDevice()
            return
        }
        val resumeAtMs = positionMs()
        val resumePaused = requestedPaused
        replacementGuard.begin()
        playbackStarted = false
        runCatching { mediaPlayer.controls().stop() }
        pushOutputDevice()
        resetTimeThrottle(resumeAtMs)
        val restarted = runCatching {
            if (resumePaused) {
                mediaPlayer.media().startPaused(url, *mediaOptions(url, resumeAtMs))
            } else {
                mediaPlayer.media().play(url, *mediaOptions(url, resumeAtMs))
            }
        }.getOrDefault(false)
        if (!restarted) {
            loadedUrl = ""
            requestedPaused = false
            playbackStarted = false
            closeBridgeSession(url)
            emit(PlayerEvent.Failed("Impossibile riaprire l'uscita audio selezionata"))
        }
    }

    private fun publishTimeChanged(positionMs: Long) {
        val now = System.nanoTime()
        val elapsedMs = if (lastTimePublishNanos == 0L) {
            Long.MAX_VALUE
        } else {
            (now - lastTimePublishNanos) / NANOS_PER_MILLISECOND
        }
        val jumped = lastPublishedTimeMs == Long.MIN_VALUE ||
            abs(positionMs - lastPublishedTimeMs) >= FORCE_TIME_EVENT_DELTA_MS

        if (!jumped && elapsedMs < TIME_EVENT_INTERVAL_MS) return

        lastPublishedTimeMs = positionMs
        lastTimePublishNanos = now
        emit(PlayerEvent.TimeChanged(positionMs))
    }

    private fun pushOutputDevice() {
        if (released.get()) return
        val selection = AudioOutputDevice.fromPersistedId(selectedOutputDeviceId)
        runCatching {
            mediaPlayer.audio().setOutputDevice(
                selection.outputName.ifEmpty { null },
                selection.deviceId.ifEmpty { null }
            )
            appliedOutputDeviceId = selectedOutputDeviceId
        }
    }

    private fun resetTimeThrottle(positionMs: Long) {
        lastPublishedTimeMs = positionMs.coerceAtLeast(0L)
        lastTimePublishNanos = 0L
    }

    private fun emit(event: PlayerEvent) {
        eventFlow.tryEmit(event)
    }

    private fun playbackLocation(url: String): String? {
        if (!shouldBridgeYoutubePlayback(url)) return youtubePlaybackUrl(url)
        return runCatching {
            bridge().openSession(url).also { activeBridgeUrl = it }
        }.getOrElse {
            emit(PlayerEvent.Failed("BRIDGE_UPSTREAM_FAILED"))
            null
        }
    }

    private fun beginMediaReplacement() {
        val previousUrl = loadedUrl
        val previousBridgeUrl = activeBridgeUrl
        if (previousUrl.isNotBlank()) {
            replacementGuard.begin()
            loadedUrl = ""
            requestedPaused = false
            playbackStarted = false
            runCatching { mediaPlayer.controls().stop() }
            runCatching { mediaPlayer.media().reset() }
            closeBridgeSession(previousUrl)
        }
        if (previousBridgeUrl != previousUrl) closeBridgeSession(previousBridgeUrl)
    }

    private fun shouldIgnoreTerminalEvent(eventPlayer: MediaPlayer): Boolean {
        if (released.get() || replacementGuard.shouldSuppressTerminalEvent()) return true
        return !eventTargetsLoadedMedia(eventPlayer)
    }

    private fun shouldIgnoreReplacementEvent(eventPlayer: MediaPlayer): Boolean =
        replacementGuard.shouldSuppressTerminalEvent() &&
            !eventTargetsLoadedMedia(eventPlayer, allowUnknownMrl = false)

    private fun eventTargetsLoadedMedia(
        eventPlayer: MediaPlayer,
        allowUnknownMrl: Boolean = true
    ): Boolean {
        val expectedUrl = loadedUrl
        if (expectedUrl.isBlank()) return false
        val currentUrl = runCatching { eventPlayer.media().info().mrl().orEmpty() }.getOrDefault("")
        return currentUrl == expectedUrl || allowUnknownMrl && currentUrl.isBlank()
    }

    private fun bridge(): YoutubeLocalStreamBridge = synchronized(bridgeLock) {
        check(!released.get()) { "Player already closed" }
        streamBridge ?: YoutubeLocalStreamBridge().also { streamBridge = it }
    }

    private fun closeBridgeSession(localUrl: String) {
        if (localUrl.isBlank()) return
        synchronized(bridgeLock) {
            streamBridge?.closeSession(localUrl)
            if (activeBridgeUrl == localUrl) activeBridgeUrl = ""
        }
    }

    private class SharedMediaPlayerFactory private constructor(
        val factory: MediaPlayerFactory
    ) {
        private val references = AtomicInteger(1)
        private val factoryReleased = AtomicBoolean(false)

        fun retain(): SharedMediaPlayerFactory? {
            while (true) {
                val current = references.get()
                if (current <= 0) return null
                if (references.compareAndSet(current, current + 1)) return this
            }
        }

        fun release() {
            val remaining = references.decrementAndGet()
            if (remaining == 0 && factoryReleased.compareAndSet(false, true)) {
                runCatching { factory.release() }
            }
        }

        companion object {
            fun create(factory: MediaPlayerFactory): SharedMediaPlayerFactory = SharedMediaPlayerFactory(factory)
        }
    }

    companion object {
        private const val TIME_EVENT_INTERVAL_MS = 200L
        private const val FORCE_TIME_EVENT_DELTA_MS = 1_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L

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
            val sharedFactory = SharedMediaPlayerFactory.create(factory)
            return try {
                VlcAudioPlayer(sharedFactory, discovery.path)
            } catch (error: Throwable) {
                sharedFactory.release()
                throw AudioPlayerUnavailableException(
                    "Impossibile creare il player libvlc da ${discovery.path}: ${error.message.orEmpty()}",
                    error
                )
            }
        }
    }
}
