package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.domain.DownloadQualitySource
import com.luc4n3x.levyra.domain.DownloadQualityUnavailableException
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.Track
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber

internal sealed interface DownloadQualityProbeOutcome {
    data class Found(val option: DownloadQualityOption) : DownloadQualityProbeOutcome
    data object Unavailable : DownloadQualityProbeOutcome
    data object Failed : DownloadQualityProbeOutcome
}

internal class DownloadQualityProbeAccumulator(sources: Int) {
    private val found = ArrayList<DownloadQualityOption>(sources)
    private var pending = sources
    private var failed = 0

    fun snapshot(): DownloadQualityProbeSnapshot =
        DownloadQualityProbeSnapshot(rankDownloadQualityOptions(found), pending, failed)

    fun record(outcome: DownloadQualityProbeOutcome): DownloadQualityProbeSnapshot {
        pending = (pending - 1).coerceAtLeast(0)
        when (outcome) {
            is DownloadQualityProbeOutcome.Found -> found += outcome.option
            DownloadQualityProbeOutcome.Failed -> failed += 1
            DownloadQualityProbeOutcome.Unavailable -> Unit
        }
        return snapshot()
    }
}

internal class DownloadQualityProbe(private val resolver: PlaybackResolver) {
    fun probe(track: Track, settings: LevyraDownloadSettings): Flow<DownloadQualityProbeSnapshot> = channelFlow {
        val sources = resolver.downloadQualitySources()
        val accumulator = DownloadQualityProbeAccumulator(sources.size)
        val lock = Mutex()
        send(accumulator.snapshot())
        sources.forEach { source ->
            launch {
                val outcome = probeSource(track, source, settings)
                send(lock.withLock { accumulator.record(outcome) })
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun probeSource(
        track: Track,
        source: DownloadQualitySource,
        settings: LevyraDownloadSettings
    ): DownloadQualityProbeOutcome = try {
        val resolved = withTimeout(PROBE_SOURCE_TIMEOUT_MS) {
            resolver.resolveForOffline(
                track.copy(streamUrl = ""),
                downloadResolverAudioQuality(source, settings),
                source
            )
        }
        resolved.downloadQualityOption(source)
            ?.let(DownloadQualityProbeOutcome::Found)
            ?: DownloadQualityProbeOutcome.Unavailable
    } catch (error: TimeoutCancellationException) {
        DownloadQualityProbeOutcome.Failed
    } catch (error: CancellationException) {
        throw error
    } catch (error: DownloadQualityUnavailableException) {
        DownloadQualityProbeOutcome.Unavailable
    } catch (error: Throwable) {
        Timber.d(error, "Download quality probe %s unavailable", source)
        if (error.isConnectivityFailure()) DownloadQualityProbeOutcome.Failed else DownloadQualityProbeOutcome.Unavailable
    }

    private fun Throwable.isConnectivityFailure(): Boolean = generateSequence(this) { it.cause }.any {
        it is UnknownHostException || it is ConnectException || it is InterruptedIOException
    }

    private companion object {
        const val PROBE_SOURCE_TIMEOUT_MS = 45_000L
    }
}
