package com.luc4n3x.levyra.player.visualizer

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.roundToLong

internal class PcmHistory(capacityLog2: Int = DEFAULT_CAPACITY_LOG2) {
    class Segment(
        val startFrame: Long,
        val periodUid: Any?,
        val offsetUs: Long,
        val sampleRate: Double
    )

    class Location(val endFrame: Long, val sampleRate: Double)

    val capacity: Int = 1 shl capacityLog2
    private val mask = (capacity - 1).toLong()
    private val reuseMargin = capacity / 4

    @Volatile
    private var ring: FloatArray? = null

    @Volatile
    private var validFromFrame: Long = Long.MAX_VALUE

    @Volatile
    var writeFrame: Long = 0L
        private set

    @Volatile
    private var segments: List<Segment> = emptyList()

    private var pendingInputRate = 0
    private var decimation = 1
    private var decimationSum = 0f
    private var decimationCount = 0

    val enabled: Boolean get() = ring != null

    fun setEnabled(enabled: Boolean) {
        if (enabled == (ring != null)) return
        if (enabled) {
            validFromFrame = Long.MAX_VALUE
            ring = FloatArray(capacity)
        } else {
            ring = null
            validFromFrame = Long.MAX_VALUE
        }
    }

    fun configure(inputSampleRate: Int) {
        pendingInputRate = inputSampleRate
    }

    fun beginSegment(periodUid: Any?, offsetUs: Long) {
        if (pendingInputRate <= 0) return
        decimation = ceil(pendingInputRate / MAX_STORED_RATE).toInt().coerceAtLeast(1)
        decimationSum = 0f
        decimationCount = 0
        val segment = Segment(writeFrame, periodUid, offsetUs, pendingInputRate.toDouble() / decimation)
        val current = segments
        segments = if (current.size < MAX_SEGMENTS) current + segment else current.drop(1) + segment
    }

    fun append(input: ByteBuffer, encoding: Int, channelCount: Int) {
        if (segments.isEmpty() || channelCount <= 0) return
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = bytesPerSample * channelCount
        val start = input.position()
        val frames = (input.limit() - start) / frameBytes
        if (frames <= 0) return
        val target = ring
        if (target == null) {
            val total = decimationCount + frames
            writeFrame += total / decimation
            decimationCount = total % decimation
            decimationSum = 0f
            return
        }
        if (validFromFrame == Long.MAX_VALUE) {
            validFromFrame = writeFrame
            decimationSum = 0f
            decimationCount = 0
        }
        input.order(ByteOrder.LITTLE_ENDIAN)
        val inverseChannels = 1f / channelCount
        val step = decimation
        var frame = writeFrame
        var sum = decimationSum
        var count = decimationCount
        var offset = start
        repeat(frames) {
            var mixed = 0f
            for (channel in 0 until channelCount) {
                mixed += if (bytesPerSample == 4) {
                    input.getFloat(offset)
                } else {
                    input.getShort(offset) / 32768f
                }
                offset += bytesPerSample
            }
            sum += mixed * inverseChannels
            count++
            if (count == step) {
                target[(frame and mask).toInt()] = sum / step
                frame++
                sum = 0f
                count = 0
            }
        }
        decimationSum = sum
        decimationCount = count
        writeFrame = frame
    }

    fun locate(periodUid: Any?, positionUs: Long, chainRootUid: Any?, chainOffsetUs: Long): Location? {
        val snapshot = segments
        if (snapshot.isEmpty()) return null
        val head = writeFrame
        val oldest = maxOf(head - capacity + reuseMargin, validFromFrame)
        locateIn(snapshot, periodUid, positionUs, head, oldest)?.let { return it }
        if (chainRootUid != null && (chainRootUid != periodUid || chainOffsetUs != 0L)) {
            locateIn(snapshot, chainRootUid, positionUs + chainOffsetUs, head, oldest)?.let { return it }
        }
        val latest = snapshot[snapshot.lastIndex]
        return if (latest.periodUid == null) frameIn(latest, positionUs, head, head, oldest) else null
    }

    private fun locateIn(
        snapshot: List<Segment>,
        periodUid: Any?,
        positionUs: Long,
        head: Long,
        oldest: Long
    ): Location? {
        if (periodUid == null) return null
        var index = snapshot.lastIndex
        while (index >= 0) {
            val segment = snapshot[index]
            if (segment.periodUid == periodUid) {
                val end = if (index == snapshot.lastIndex) head else snapshot[index + 1].startFrame
                frameIn(segment, positionUs, end, head, oldest)?.let { return it }
            }
            index--
        }
        return null
    }

    private fun frameIn(segment: Segment, positionUs: Long, end: Long, head: Long, oldest: Long): Location? {
        val frame = segment.startFrame +
            ((positionUs - segment.offsetUs) * segment.sampleRate / 1_000_000.0).roundToLong()
        if (frame < segment.startFrame || frame > end || frame > head || frame < oldest) return null
        return Location(frame, segment.sampleRate)
    }

    fun hasSegment(periodUid: Any): Boolean = segments.any { it.periodUid == periodUid }

    fun availableEnd(): Long = writeFrame

    fun read(endFrame: Long, out: FloatArray): Boolean {
        val source = ring ?: return false
        val length = out.size
        if (length > capacity - reuseMargin) return false
        val start = endFrame - length
        if (start < validFromFrame || endFrame > writeFrame) return false
        if (start < writeFrame - capacity + reuseMargin) return false
        val startIndex = (start and mask).toInt()
        val firstPart = minOf(length, capacity - startIndex)
        System.arraycopy(source, startIndex, out, 0, firstPart)
        if (firstPart < length) System.arraycopy(source, 0, out, firstPart, length - firstPart)
        return start >= writeFrame - capacity + reuseMargin
    }

    companion object {
        const val DEFAULT_CAPACITY_LOG2 = 18
        const val MAX_STORED_RATE = 48_000.0
        private const val MAX_SEGMENTS = 16
    }
}
