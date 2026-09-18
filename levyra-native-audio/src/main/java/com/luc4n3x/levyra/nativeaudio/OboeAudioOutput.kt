package com.luc4n3x.levyra.nativeaudio

import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioOutput
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class OboeAudioOutput private constructor(
    private var handle: Long,
    private val sampleRate: Int,
    private val frameSizeBytes: Int,
    requestedAudioSessionId: Int,
    private val requestedPreferredDeviceId: Int,
    private val events: Events
) : AudioOutput {

    interface Events {
        fun onOutputFailure()
        fun onPreferredDeviceChanged(deviceId: Int)
    }

    private val listeners = CopyOnWriteArraySet<AudioOutput.Listener>()
    private val ownerHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val audioSessionId = OboeNative.nativeGetSessionId(handle).takeIf { it > 0 } ?: requestedAudioSessionId
    private val bufferSizeInFrames = OboeNative.nativeGetBufferSizeInFrames(handle).toLong()

    private var released = false
    private var playing = false
    private var stopped = false
    private var outputLost = false
    private var failureReported = false
    private var routeChangeRequested = false
    private var writtenBytes = 0L
    private var lastPlayedFrames = 0L
    private var lastProgressElapsedMs = SystemClock.elapsedRealtime()
    private var positionAdvancingReported = false
    private var lastUnderrunCount = 0
    private var heapCopyBuffer: ByteBuffer? = null

    override fun play() {
        if (released || stopped) return
        playing = true
        positionAdvancingReported = false
        lastProgressElapsedMs = SystemClock.elapsedRealtime()
        if (OboeNative.nativePlay(handle) < 0) {
            markOutputLost()
        }
    }

    override fun pause() {
        if (released || stopped) return
        playing = false
        if (OboeNative.nativePause(handle) < 0) {
            markOutputLost()
        }
    }

    override fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean {
        if (released) return false
        maybeReportUnderrun()
        val errorCode = OboeNative.nativeGetErrorCode(handle)
        if (errorCode != 0 || routeChangeRequested) {
            return failWrite(if (errorCode != 0) errorCode else ERROR_ROUTE_CHANGED)
        }
        val remaining = buffer.remaining()
        if (remaining == 0) return true
        val result = if (buffer.isDirect) {
            OboeNative.nativeWrite(handle, buffer, buffer.position(), remaining)
        } else {
            writeHeapBuffer(buffer, remaining)
        }
        if (result < 0) {
            return failWrite(result)
        }
        buffer.position(buffer.position() + result)
        writtenBytes += result
        return result == remaining
    }

    override fun flush() {
        if (released) return
        playing = false
        if (OboeNative.nativeFlush(handle) < 0) {
            markOutputLost()
        }
        writtenBytes = 0L
        stopped = false
        lastPlayedFrames = 0L
        positionAdvancingReported = false
        lastProgressElapsedMs = SystemClock.elapsedRealtime()
    }

    override fun stop() {
        if (released || stopped) return
        stopped = true
        OboeNative.nativeStop(handle)
    }

    override fun release() {
        if (released) return
        released = true
        playing = false
        val releasedHandle = handle
        handle = 0L
        heapCopyBuffer = null
        releaseExecutor.execute {
            try {
                OboeNative.nativeRelease(releasedHandle)
            } finally {
                if (ownerHandler.looper.thread.isAlive) {
                    ownerHandler.post { listeners.forEach(AudioOutput.Listener::onReleased) }
                }
            }
        }
    }

    override fun setVolume(volume: Float) {
        if (released) return
        OboeNative.nativeSetVolume(handle, volume.coerceIn(0f, 1f))
    }

    override fun isOffloadedPlayback(): Boolean = false

    override fun getAudioSessionId(): Int = audioSessionId

    override fun getSampleRate(): Int = sampleRate

    override fun getBufferSizeInFrames(): Long = bufferSizeInFrames

    override fun getPositionUs(): Long = Util.sampleCountToDurationUs(updatePlayedFrames(), sampleRate)

    override fun getPlaybackParameters(): PlaybackParameters = PlaybackParameters.DEFAULT

    override fun isStalled(): Boolean {
        if (outputLost) return true
        if (released || !playing || stopped) return false
        val playedFrames = updatePlayedFrames()
        return writtenFrames() > playedFrames &&
            SystemClock.elapsedRealtime() - lastProgressElapsedMs > STALL_TIMEOUT_MS
    }

    override fun addListener(listener: AudioOutput.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: AudioOutput.Listener) {
        listeners.remove(listener)
    }

    override fun setPlaybackParameters(playbackParams: PlaybackParameters) = Unit

    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) = Unit

    override fun setOffloadEndOfStream() = Unit

    override fun attachAuxEffect(effectId: Int) = Unit

    override fun setAuxEffectSendLevel(level: Float) = Unit

    override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) {
        val deviceId = preferredDevice?.id ?: 0
        events.onPreferredDeviceChanged(deviceId)
        if (deviceId != requestedPreferredDeviceId) {
            routeChangeRequested = true
        }
    }

    private fun writtenFrames(): Long = writtenBytes / frameSizeBytes

    private fun updatePlayedFrames(): Long {
        if (released) return lastPlayedFrames
        val played = OboeNative.nativeGetPlayedFrames(handle).coerceIn(lastPlayedFrames, maxOf(lastPlayedFrames, writtenFrames()))
        if (played != lastPlayedFrames) {
            lastPlayedFrames = played
            lastProgressElapsedMs = SystemClock.elapsedRealtime()
            if (playing && !positionAdvancingReported) {
                positionAdvancingReported = true
                val playedMs = Util.usToMs(Util.sampleCountToDurationUs(played, sampleRate))
                val playoutStartMs = System.currentTimeMillis() - playedMs
                listeners.forEach { it.onPositionAdvancing(playoutStartMs) }
            }
        } else if (!playing || stopped) {
            lastProgressElapsedMs = SystemClock.elapsedRealtime()
        }
        return played
    }

    private fun maybeReportUnderrun() {
        val underrunCount = OboeNative.nativeGetUnderrunCount(handle)
        if (underrunCount > lastUnderrunCount) {
            listeners.forEach(AudioOutput.Listener::onUnderrun)
        }
        lastUnderrunCount = underrunCount
    }

    private fun failWrite(errorCode: Int): Boolean {
        markOutputLost()
        if (writtenBytes > 0L) {
            throw AudioOutput.WriteException(errorCode, true)
        }
        return false
    }

    private fun markOutputLost() {
        outputLost = true
        if (!failureReported) {
            failureReported = true
            events.onOutputFailure()
        }
    }

    private fun writeHeapBuffer(buffer: ByteBuffer, remaining: Int): Int {
        val copy = heapCopyBuffer?.takeIf { it.capacity() >= remaining }
            ?: ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder()).also { heapCopyBuffer = it }
        copy.clear()
        copy.put(buffer.duplicate())
        return OboeNative.nativeWrite(handle, copy, 0, remaining)
    }

    companion object {
        private const val STALL_TIMEOUT_MS = 2_000L
        private const val ERROR_ROUTE_CHANGED = -899

        private val releaseExecutor = ThreadPoolExecutor(
            0,
            1,
            1L,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(RELEASE_QUEUE_CAPACITY),
            { runnable -> Thread(runnable, "LevyraOboeRelease").apply { isDaemon = true } },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
        private const val RELEASE_QUEUE_CAPACITY = 2

        fun open(
            sampleRate: Int,
            channelCount: Int,
            audioSessionId: Int,
            preferredDeviceId: Int,
            ringCapacityFrames: Int,
            usage: Int,
            contentType: Int,
            events: Events
        ): Result<OboeAudioOutput> {
            val error = IntArray(1)
            val handle = OboeNative.nativeOpen(
                sampleRate,
                channelCount,
                audioSessionId,
                preferredDeviceId,
                ringCapacityFrames,
                usage,
                contentType,
                error
            )
            if (handle == 0L) {
                return Result.failure(IllegalStateException("Oboe stream open failed: ${error[0]}"))
            }
            return Result.success(
                OboeAudioOutput(
                    handle,
                    sampleRate,
                    channelCount * 2,
                    audioSessionId,
                    preferredDeviceId,
                    events
                )
            )
        }
    }
}
