package com.luc4n3x.levyra.player

import java.io.File
import java.security.MessageDigest

internal data class PlaybackProcessMemory(
    val rssKb: Long,
    val rssAnonKb: Long,
    val pssAnonKb: Long,
    val privateDirtyKb: Long,
    val swapKb: Long,
    val swapPssKb: Long,
    val threads: Int,
    val fileDescriptors: Int
)

internal data class PlaybackMemoryDiagnosticSample(
    val timestampMs: Long,
    val elapsedRealtimeMs: Long,
    val processMemory: PlaybackProcessMemory,
    val nativeHeapBytes: Long,
    val javaHeapBytes: Long,
    val playbackState: Int,
    val playWhenReady: Boolean,
    val isPlaying: Boolean,
    val audioSessionId: Int,
    val positionBucket30s: Long,
    val mediaId: String,
    val transitionCount: Long,
    val activeResolverJobs: Int,
    val prefetchActiveCount: Int,
    val recoveryActive: Boolean,
    val transitionActive: Boolean,
    val memoryGuardHighSamples: Int
)

internal class PlaybackMemoryDiagnosticLog(
    private val output: File,
    private val procRoot: File = File("/proc/self")
) {
    fun prepare() {
        output.parentFile?.mkdirs()
        val validExistingLog = output.isFile &&
            output.useLines { lines -> lines.firstOrNull() == HEADER }
        if (!validExistingLog) output.writeText(HEADER + "\n")
    }

    fun processMemory(): PlaybackProcessMemory {
        val status = runCatching { File(procRoot, "status").readText() }.getOrDefault("")
        val smapsRollup = runCatching { File(procRoot, "smaps_rollup").readText() }.getOrDefault("")
        val descriptors = runCatching { File(procRoot, "fd").list()?.size ?: -1 }.getOrDefault(-1)
        return parsePlaybackProcessMemory(status, smapsRollup, descriptors)
    }

    fun append(sample: PlaybackMemoryDiagnosticSample): Boolean {
        val row = (sample.toCsvLine() + "\n").toByteArray()
        if (output.length() + row.size > MAX_FILE_BYTES) return false
        output.appendBytes(row)
        return true
    }

    companion object {
        const val FILE_NAME = "issue-427-playback-memory.csv"
        const val MAX_FILE_BYTES = 512L * 1024L
        const val SAMPLE_INTERVAL_MS = 5_000L
        internal const val HEADER = "timestamp_ms,elapsed_realtime_ms,rss_kb,rss_anon_kb,pss_anon_kb," +
            "private_dirty_kb,swap_kb,swap_pss_kb,native_heap_bytes,java_heap_bytes,threads,fd_count," +
            "playback_state,play_when_ready,is_playing,audio_session_id,position_bucket_30s,media_id_hash," +
            "transition_count,active_resolver_jobs,prefetch_active_count,recovery_active,transition_active," +
            "memory_guard_high_samples,audio_decoder_count,video_decoder_count"
    }
}

internal fun parsePlaybackProcessMemory(
    status: String,
    smapsRollup: String,
    fileDescriptors: Int
): PlaybackProcessMemory {
    val statusValues = procKbValues(status)
    val smapsValues = procKbValues(smapsRollup)
    return PlaybackProcessMemory(
        rssKb = statusValues["VmRSS"] ?: -1L,
        rssAnonKb = statusValues["RssAnon"] ?: -1L,
        pssAnonKb = smapsValues["Pss_Anon"] ?: -1L,
        privateDirtyKb = smapsValues["Private_Dirty"] ?: -1L,
        swapKb = smapsValues["Swap"] ?: statusValues["VmSwap"] ?: -1L,
        swapPssKb = smapsValues["SwapPss"] ?: -1L,
        threads = statusValues["Threads"]?.toInt() ?: -1,
        fileDescriptors = fileDescriptors
    )
}

internal fun diagnosticMediaIdentity(mediaId: String): String {
    val normalized = mediaId.trim()
    if (normalized.isEmpty()) return "none"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
    return digest.take(8).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

private fun procKbValues(source: String): Map<String, Long> = buildMap {
    source.lineSequence().forEach { line ->
        val separator = line.indexOf(':')
        if (separator <= 0) return@forEach
        val value = line.substring(separator + 1)
            .trim()
            .substringBefore(' ')
            .toLongOrNull()
            ?: return@forEach
        put(line.substring(0, separator), value)
    }
}

private fun PlaybackMemoryDiagnosticSample.toCsvLine(): String = listOf(
    timestampMs,
    elapsedRealtimeMs,
    processMemory.rssKb,
    processMemory.rssAnonKb,
    processMemory.pssAnonKb,
    processMemory.privateDirtyKb,
    processMemory.swapKb,
    processMemory.swapPssKb,
    nativeHeapBytes,
    javaHeapBytes,
    processMemory.threads,
    processMemory.fileDescriptors,
    playbackState,
    playWhenReady,
    isPlaying,
    audioSessionId,
    positionBucket30s,
    diagnosticMediaIdentity(mediaId).csvCell(),
    transitionCount,
    activeResolverJobs,
    prefetchActiveCount,
    recoveryActive,
    transitionActive,
    memoryGuardHighSamples,
    -1,
    -1
).joinToString(",")

private fun String.csvCell(): String = "\"" +
    take(128)
        .replace("\r", " ")
        .replace("\n", " ")
        .replace("\"", "\"\"") +
    "\""
