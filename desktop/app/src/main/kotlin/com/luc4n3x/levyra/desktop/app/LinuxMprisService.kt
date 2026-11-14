package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.player.PlaybackStatus
import com.luc4n3x.levyra.desktop.player.RepeatMode
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.swing.Swing
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MprisRoot : DBusInterface {
    fun Raise()
    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MprisPlayer : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offset: Long)
    fun SetPosition(trackId: DBusPath, position: Long)
    fun OpenUri(uri: String)
    class Seeked(path: String, position: Long) : DBusSignal(path, position)
}

internal class LinuxMprisService(
    private val controller: PlaybackController,
    private val onRaise: () -> Unit,
    private val onQuit: () -> Unit,
    private val onOpen: (String) -> Unit,
    private val commandDispatcher: CoroutineDispatcher = Dispatchers.Swing
) : MprisRoot, MprisPlayer, Properties, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = AtomicReference<DBusConnection?>()
    private val closed = AtomicBoolean()
    private val started = AtomicBoolean()

    fun start() {
        if (!DesktopPlatform.isLinux || System.getenv("DBUS_SESSION_BUS_ADDRESS").isNullOrBlank()) return
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val bus = runCatching { DBusConnectionBuilder.forSessionBus().withShared(false).build() }.getOrNull() ?: return@launch
            if (closed.get()) { bus.close(); return@launch }
            connection.set(bus)
            try {
                bus.exportObject(PATH, this@LinuxMprisService)
                bus.requestBusName("org.mpris.MediaPlayer2.levyra")
                launch {
                    controller.seekEvents.collect { position ->
                        bus.sendMessage(MprisPlayer.Seeked(PATH, position.coerceIn(0, Long.MAX_VALUE / 1_000L) * 1_000L))
                    }
                }
                controller.state.map { it.copy(positionMs = 0, sleepRemainingMs = 0) }.distinctUntilChanged().collect { state ->
                    bus.sendMessage(Properties.PropertiesChanged(PATH, PLAYER, playerProperties(state) - "Position", emptyList()))
                }
            } catch (error: Exception) {
                if (error !is CancellationException && !closed.get()) System.getLogger("Levyra.MPRIS").log(System.Logger.Level.WARNING, "MPRIS unavailable", error)
            } finally {
                connection.compareAndSet(bus, null)
                bus.close()
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scope.cancel()
        thread(isDaemon = true, name = "levyra-mpris-close") { runCatching { connection.getAndSet(null)?.close() } }
    }

    override fun getObjectPath(): String = PATH
    override fun isRemote(): Boolean = false
    private fun dispatch(action: () -> Unit) { if (!closed.get()) scope.launch(commandDispatcher) { action() } }
    override fun Raise() = dispatch(onRaise)
    override fun Quit() = dispatch(onQuit)
    override fun PlayPause() = dispatch(controller::togglePlayPause)
    override fun Stop() = dispatch(controller::stop)
    override fun Next() = dispatch { if (controller.state.value.queue.hasNext) advance { controller.next() } }
    override fun Previous() = dispatch { advance(controller::previous) }
    override fun Pause() = dispatch { if (controller.state.value.status in ACTIVE) controller.togglePlayPause() }
    override fun Play() = dispatch { if (controller.state.value.status !in ACTIVE) controller.togglePlayPause() }
    override fun OpenUri(uri: String) = dispatch { if (DesktopLinkRouter.launchPayload(arrayOf(uri)).isNotBlank()) onOpen(uri) }
    override fun Seek(offset: Long) = dispatch {
        val state = controller.state.value
        val remaining = (state.durationMs - state.positionMs).coerceAtLeast(0L)
        seek(state.positionMs + (offset / 1_000L).coerceIn(-state.positionMs, remaining))
    }
    override fun SetPosition(trackId: DBusPath, position: Long) = dispatch {
        val state = controller.state.value
        if (trackId == trackPath(state) && position >= 0 && position / 1_000L <= state.durationMs) seek(position / 1_000L)
    }

    private fun advance(action: () -> Unit) {
        val wasActive = controller.state.value.status in ACTIVE
        action()
        if (!wasActive && controller.state.value.status in ACTIVE) controller.togglePlayPause()
    }

    private fun seek(position: Long) {
        val state = controller.state.value
        if (state.current == null || state.durationMs <= 0L) return
        val bounded = position.coerceIn(0, state.durationMs)
        controller.seekTo(bounded)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A> Get(interfaceName: String, propertyName: String): A =
        (GetAll(interfaceName)[propertyName] ?: throw DBusExecutionException("Unknown property")) as A

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> = when (interfaceName) {
        ROOT -> mapOf(
            "Identity" to Variant("Levyra"), "DesktopEntry" to Variant("levyra-Levyra"),
            "CanQuit" to Variant(true), "CanRaise" to Variant(true), "HasTrackList" to Variant(false),
            "SupportedUriSchemes" to Variant(arrayOf("levyra", "https", "http")),
            "SupportedMimeTypes" to Variant(arrayOf("audio/mpeg", "audio/flac", "audio/mp4", "audio/ogg"))
        )
        PLAYER -> playerProperties(controller.state.value)
        else -> throw DBusExecutionException("Unknown interface")
    }

    override fun <A> Set(interfaceName: String, propertyName: String, value: A) {
        if (interfaceName != PLAYER) throw DBusExecutionException("Read-only interface")
        val supplied = if (value is Variant<*>) value.value else value
        when (propertyName) {
            "Volume" -> {
                val volume = (supplied as? Double)?.takeIf { it.isFinite() } ?: throw DBusExecutionException("Invalid volume")
                dispatch { controller.setVolume((volume.coerceIn(0.0, 1.0) * 100).toInt()) }
            }
            "Rate" -> {
                val rate = (supplied as? Double)?.takeIf { it in 0.75..2.0 } ?: throw DBusExecutionException("Invalid rate")
                dispatch { controller.setSpeed(rate.toFloat()) }
            }
            "Shuffle" -> {
                val shuffle = supplied as? Boolean ?: throw DBusExecutionException("Invalid shuffle")
                dispatch { if (controller.state.value.queue.shuffle != shuffle) controller.toggleShuffle() }
            }
            "LoopStatus" -> setLoop(supplied)
            else -> throw DBusExecutionException("Read-only property")
        }
    }

    private fun setLoop(value: Any?) {
        val desired = when (value) { "None" -> RepeatMode.OFF; "Track" -> RepeatMode.ONE; "Playlist" -> RepeatMode.ALL; else -> throw DBusExecutionException("Invalid loop mode") }
        dispatch { repeat(3) { if (controller.state.value.queue.repeat != desired) controller.cycleRepeat() } }
    }

    internal fun playerProperties(state: PlaybackUiState): Map<String, Variant<*>> {
        val metadata = mutableMapOf<String, Variant<*>>()
        state.current?.let { track ->
            metadata["mpris:trackid"] = Variant(trackPath(state))
            metadata["mpris:length"] = Variant(state.durationMs.coerceIn(0, Long.MAX_VALUE / 1_000L) * 1_000L)
            metadata["xesam:title"] = Variant(track.title)
            metadata["xesam:artist"] = Variant(arrayOf(track.artist))
            metadata["xesam:album"] = Variant(track.album)
            if (track.artworkUrl.isNotBlank()) metadata["mpris:artUrl"] = Variant(track.artworkUrl)
        }
        return mapOf(
            "PlaybackStatus" to Variant(when (state.status) { PlaybackStatus.PAUSED -> "Paused"; in ACTIVE -> "Playing"; else -> "Stopped" }),
            "LoopStatus" to Variant(when (state.queue.repeat) { RepeatMode.OFF -> "None"; RepeatMode.ONE -> "Track"; RepeatMode.ALL -> "Playlist" }),
            "Rate" to Variant(state.speed.toDouble()), "Shuffle" to Variant(state.queue.shuffle),
            "Metadata" to Variant(metadata, "a{sv}"), "Volume" to Variant(if (state.muted) 0.0 else state.volume / 100.0),
            "Position" to Variant(state.positionMs.coerceIn(0, Long.MAX_VALUE / 1_000L) * 1_000L),
            "MinimumRate" to Variant(0.75), "MaximumRate" to Variant(2.0),
            "CanGoNext" to Variant(state.queue.hasNext), "CanGoPrevious" to Variant(state.current != null),
            "CanPlay" to Variant(state.current != null), "CanPause" to Variant(state.current != null),
            "CanSeek" to Variant(state.current != null && state.durationMs > 0), "CanControl" to Variant(true)
        )
    }

    private fun trackPath(state: PlaybackUiState): DBusPath {
        val digest = MessageDigest.getInstance("SHA-256").digest(state.current?.id.orEmpty().toByteArray(Charsets.UTF_8))
        return DBusPath("$PATH/track/" + digest.joinToString("") { "%02x".format(it) })
    }

    companion object {
        const val PATH = "/org/mpris/MediaPlayer2"
        const val ROOT = "org.mpris.MediaPlayer2"
        const val PLAYER = "org.mpris.MediaPlayer2.Player"
        private val ACTIVE = setOf(PlaybackStatus.PLAYING, PlaybackStatus.OPENING, PlaybackStatus.BUFFERING)
    }
}
