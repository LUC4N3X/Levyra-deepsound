package com.luc4n3x.levyra.desktop.player

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

object VlcMp3Transcoder {
    private val permits = Semaphore(MAX_CONCURRENT_TRANSCODES)

    suspend fun transcode(
        source: Path,
        target: Path,
        preferredDirectory: String = "",
        bundledDirectory: Path? = null,
        bitrateKbps: Int = DEFAULT_BITRATE_KBPS
    ) = permits.withPermit {
        withContext(Dispatchers.IO) {
            require(Files.isRegularFile(source)) { "File sorgente non disponibile: $source" }
            Files.createDirectories(target.parent)
            Files.deleteIfExists(target)

            val discovery = VlcNativeLocator.discover(preferredDirectory, bundledDirectory)
            if (!discovery.available) {
                throw IOException("Runtime VLC non trovato: impossibile convertire il download in MP3")
            }

            val factory = MediaPlayerFactory(*FACTORY_ARGUMENTS)
            val player = factory.mediaPlayers().newMediaPlayer()
            val completion = CompletableDeferred<Unit>()
            val succeeded = AtomicBoolean(false)
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) {
                    completion.complete(Unit)
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    completion.completeExceptionally(IOException("Conversione MP3 interrotta da VLC"))
                }
            })

            try {
                val options = vlcMp3TranscodeOptions(target, bitrateKbps)
                val started = player.media().play(source.toUri().toString(), *options)
                if (!started) throw IOException("VLC non ha avviato la conversione MP3")
                withTimeout(TimeUnit.MINUTES.toMillis(TRANSCODE_TIMEOUT_MINUTES)) {
                    completion.await()
                }
                if (!Files.isRegularFile(target) || Files.size(target) <= 0L) {
                    throw IOException("VLC non ha prodotto un MP3 valido")
                }
                if (!validateMp3Stream(target)) {
                    throw IOException("Il file prodotto non è un MP3 valido")
                }
                succeeded.set(true)
            } finally {
                runCatching { player.controls().stop() }
                runCatching { player.release() }
                runCatching { factory.release() }
                if (!succeeded.get()) {
                    runCatching { Files.deleteIfExists(target) }
                }
            }
        }
    }

    private val FACTORY_ARGUMENTS = arrayOf(
        "--intf=dummy",
        "--no-video",
        "--no-metadata-network-access",
        "--no-sub-autodetect-file",
        "--quiet"
    )

    private const val DEFAULT_BITRATE_KBPS = 256
    private const val MAX_CONCURRENT_TRANSCODES = 2
    private const val TRANSCODE_TIMEOUT_MINUTES = 30L
}

internal fun vlcMp3TranscodeOptions(
    target: Path,
    bitrateKbps: Int = 256
): Array<String> {
    val safeBitrate = bitrateKbps.coerceIn(96, 320)
    val destination = escapeVlcSoutPath(target.toAbsolutePath().normalize().toString())
    return arrayOf(
        ":no-video",
        ":sout=#transcode{vcodec=none,acodec=mp3,ab=$safeBitrate,channels=2,samplerate=44100}:std{access=file,mux=raw,dst='$destination'}",
        ":sout-keep"
    )
}

internal fun escapeVlcSoutPath(path: String): String = path
    .replace('\\', '/')
    .replace("'", "\\'")

internal fun validateMp3Stream(path: Path): Boolean {
    runCatching {
        Files.newInputStream(path).use { stream ->
            val header = ByteArray(3)
            val read = stream.read(header)
            if (read < 2) return false
            
            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                return true
            }
            val b0 = header[0].toInt() and 0xFF
            val b1 = header[1].toInt() and 0xFF
            if (b0 == 0xFF && (b1 and 0xFE) == 0xFA) {
                return true
            }
        }
    }.onFailure { return false }
    return false
}
