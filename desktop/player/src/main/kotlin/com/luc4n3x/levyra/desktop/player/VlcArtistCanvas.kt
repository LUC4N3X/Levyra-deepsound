package com.luc4n3x.levyra.desktop.player

import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat

class VlcArtistCanvas private constructor(
    private val factory: MediaPlayerFactory,
    private val player: EmbeddedMediaPlayer
) : AutoCloseable {
    override fun close() {
        try {
            player.controls().stop()
            player.release()
        } finally {
            factory.release()
        }
    }

    companion object {
        fun open(file: Path, preferredDirectory: String, bundledDirectory: Path?, onFrame: (BufferedImage) -> Unit): VlcArtistCanvas? {
            if (!VlcNativeLocator.discover(preferredDirectory, bundledDirectory).available) return null
            val factory = MediaPlayerFactory("--no-audio", "--no-video-title-show", "--no-sub-autodetect-file", "--quiet")
            val player = try {
                factory.mediaPlayers().newEmbeddedMediaPlayer()
            } catch (failure: Throwable) {
                factory.release()
                throw failure
            }
            val canvas = VlcArtistCanvas(factory, player)
            try {
                val frame = CanvasFrames(onFrame)
                player.videoSurface().set(factory.videoSurfaces().newVideoSurface(frame.format, frame, true))
                if (player.media().play(file.toString(), ":no-audio", ":input-repeat=65535")) return canvas
            } catch (failure: Throwable) {
                canvas.close()
                throw failure
            }
            canvas.close()
            return null
        }
    }
}

private class CanvasFrames(private val onFrame: (BufferedImage) -> Unit) : RenderCallbackAdapter() {
    @Volatile private var width = 0
    @Volatile private var height = 0
    private var lastFrame = 0L

    val format = object : BufferFormatCallback {
        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            val scale = minOf(1.0, 640.0 / maxOf(sourceWidth, sourceHeight).coerceAtLeast(1))
            val w = (sourceWidth * scale).toInt().coerceAtLeast(1)
            val h = (sourceHeight * scale).toInt().coerceAtLeast(1)
            setBuffer(IntArray(w * h))
            width = w
            height = h
            return RV32BufferFormat(w, h)
        }

        override fun newFormatSize(bufferWidth: Int, bufferHeight: Int, displayWidth: Int, displayHeight: Int) = Unit

        override fun allocatedBuffers(buffers: Array<ByteBuffer>) = Unit
    }

    override fun onDisplay(mediaPlayer: MediaPlayer, rgbBuffer: IntArray) {
        val now = System.nanoTime()
        if (now - lastFrame < TimeUnit.MILLISECONDS.toNanos(66)) return
        val w = width
        val h = height
        if (w <= 0 || h <= 0 || w * h != rgbBuffer.size) return
        lastFrame = now
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, w, h, rgbBuffer, 0, w)
        onFrame(image)
    }
}
