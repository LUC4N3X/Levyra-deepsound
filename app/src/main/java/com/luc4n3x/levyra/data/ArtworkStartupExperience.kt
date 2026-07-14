package com.luc4n3x.levyra.data

import android.content.Context
import timber.log.Timber
import java.io.File
import java.util.Locale

internal data class HomeContentAvailability(
    val trackCount: Int = 0,
    val homeSectionCount: Int = 0,
    val homeSectionTrackCount: Int = 0,
    val albumCount: Int = 0,
    val chartCount: Int = 0,
    val personalOrbitCount: Int = 0,
    val releaseRadarCount: Int = 0,
    val similarArtistCount: Int = 0,
    val hasCurrentTrack: Boolean = false
) {
    val hasUsableContent: Boolean
        get() = trackCount > 0 ||
            homeSectionTrackCount > 0 ||
            albumCount > 0 ||
            chartCount > 0 ||
            personalOrbitCount > 0 ||
            releaseRadarCount > 0 ||
            similarArtistCount > 0 ||
            hasCurrentTrack

    fun fingerprint(): String {
        return listOf(
            trackCount,
            homeSectionCount,
            homeSectionTrackCount,
            albumCount,
            chartCount,
            personalOrbitCount,
            releaseRadarCount,
            similarArtistCount,
            if (hasCurrentTrack) 1 else 0
        ).joinToString(":")
    }
}

internal object HomeLoadingPolicy {
    fun showAlbumShimmer(content: HomeContentAvailability, loading: Boolean): Boolean {
        return loading && content.albumCount == 0 && !content.hasUsableContent
    }

    fun showChartShimmer(content: HomeContentAvailability, loading: Boolean): Boolean {
        return loading && content.chartCount == 0 && !content.hasUsableContent
    }
}

internal data class ArtworkMemoryDeviceProfile(
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val lowRamDevice: Boolean,
    val largeHeapEnabled: Boolean
)

internal object ArtworkMemoryCachePolicy {
    private const val MIB = 1024L * 1024L

    fun maxSizeBytes(profile: ArtworkMemoryDeviceProfile): Long {
        val effectiveMemoryMb = if (profile.largeHeapEnabled) {
            maxOf(profile.memoryClassMb, profile.largeMemoryClassMb)
        } else {
            profile.memoryClassMb
        }.coerceAtLeast(64)
        val baseMb = when {
            profile.lowRamDevice || effectiveMemoryMb <= 128 -> 24
            effectiveMemoryMb <= 192 -> 32
            effectiveMemoryMb <= 256 -> 48
            effectiveMemoryMb <= 384 -> 64
            effectiveMemoryMb <= 512 -> 80
            profile.largeHeapEnabled -> 112
            else -> 96
        }
        val heapQuarterMb = (effectiveMemoryMb / 4).coerceAtLeast(16)
        return minOf(baseMb, heapQuarterMb).toLong() * MIB
    }
}

internal enum class ArtworkRequestSource {
    PersistentFile,
    Remote,
    Missing
}

internal data class ArtworkStartupMetricsSnapshot(
    val elapsedMs: Long,
    val firstRealArtworkMs: Long?,
    val uniqueArtworkRequests: Int,
    val persistentArtworkRequests: Int,
    val remoteArtworkRequests: Int,
    val missingArtworkRequests: Int,
    val artworkLoads: Int,
    val artworkDisplays: Int,
    val artworkFailures: Int,
    val artworkModelChanges: Int,
    val placeholdersAfterFirstArtwork: Int,
    val visibleHomeEmissions: Int,
    val shimmerDisplays: Int,
    val shimmerWithUsableContent: Int
) {
    val persistentRequestRate: Double
        get() = if (uniqueArtworkRequests == 0) 0.0 else persistentArtworkRequests.toDouble() / uniqueArtworkRequests.toDouble()

    fun regressionViolations(): List<String> {
        return buildList {
            if (artworkModelChanges > ArtworkStartupRegressionBudget.MAX_ARTWORK_MODEL_CHANGES) add("artwork_model_changes=$artworkModelChanges")
            if (placeholdersAfterFirstArtwork > ArtworkStartupRegressionBudget.MAX_PLACEHOLDERS_AFTER_FIRST_ARTWORK) add("late_placeholders=$placeholdersAfterFirstArtwork")
            if (shimmerWithUsableContent > ArtworkStartupRegressionBudget.MAX_SHIMMER_WITH_USABLE_CONTENT) add("shimmer_with_content=$shimmerWithUsableContent")
            if (visibleHomeEmissions > ArtworkStartupRegressionBudget.MAX_VISIBLE_HOME_EMISSIONS) add("home_emissions=$visibleHomeEmissions")
        }
    }
}

internal object ArtworkStartupRegressionBudget {
    const val MAX_ARTWORK_MODEL_CHANGES = 0
    const val MAX_PLACEHOLDERS_AFTER_FIRST_ARTWORK = 0
    const val MAX_SHIMMER_WITH_USABLE_CONTENT = 0
    const val MAX_VISIBLE_HOME_EMISSIONS = 2
}

internal class ArtworkStartupMetricsCollector(
    private val clockNanos: () -> Long = System::nanoTime
) {
    private val lock = Any()
    private var startedAtNanos = clockNanos()
    private var firstRealArtworkNanos: Long? = null
    private val requestModels = LinkedHashMap<String, String>()
    private val requestSources = LinkedHashMap<String, ArtworkRequestSource>()
    private val loadingKeys = LinkedHashSet<String>()
    private val displayedKeys = LinkedHashSet<String>()
    private val failedKeys = LinkedHashSet<String>()
    private val latePlaceholderKeys = LinkedHashSet<String>()
    private var artworkModelChanges = 0
    private var lastHomeFingerprint: String? = null
    private var visibleHomeEmissions = 0
    private var shimmerDisplays = 0
    private var shimmerWithUsableContent = 0

    fun reset() {
        synchronized(lock) {
            startedAtNanos = clockNanos()
            firstRealArtworkNanos = null
            requestModels.clear()
            requestSources.clear()
            loadingKeys.clear()
            displayedKeys.clear()
            failedKeys.clear()
            latePlaceholderKeys.clear()
            artworkModelChanges = 0
            lastHomeFingerprint = null
            visibleHomeEmissions = 0
            shimmerDisplays = 0
            shimmerWithUsableContent = 0
        }
    }

    fun recordHomeEmission(fingerprint: String, hasUsableContent: Boolean) {
        synchronized(lock) {
            if (!hasUsableContent || fingerprint == lastHomeFingerprint) return
            lastHomeFingerprint = fingerprint
            visibleHomeEmissions += 1
        }
    }

    fun recordShimmer(hasUsableContent: Boolean) {
        synchronized(lock) {
            shimmerDisplays += 1
            if (hasUsableContent) shimmerWithUsableContent += 1
        }
    }

    fun recordArtworkRequest(key: String, modelIdentity: String, source: ArtworkRequestSource) {
        synchronized(lock) {
            val previous = requestModels.put(key, modelIdentity)
            if (previous != null && previous != modelIdentity) artworkModelChanges += 1
            requestSources[key] = source
        }
    }

    fun recordArtworkLoading(key: String) {
        synchronized(lock) {
            loadingKeys += key
            if (key in displayedKeys) latePlaceholderKeys += key
        }
    }

    fun recordArtworkDisplayed(key: String) {
        synchronized(lock) {
            displayedKeys += key
            if (firstRealArtworkNanos == null) firstRealArtworkNanos = clockNanos()
        }
    }

    fun recordArtworkFailure(key: String) {
        synchronized(lock) {
            failedKeys += key
        }
    }

    fun snapshot(): ArtworkStartupMetricsSnapshot {
        synchronized(lock) {
            val now = clockNanos()
            val firstArtwork = firstRealArtworkNanos?.let { ((it - startedAtNanos) / 1_000_000L).coerceAtLeast(0L) }
            return ArtworkStartupMetricsSnapshot(
                elapsedMs = ((now - startedAtNanos) / 1_000_000L).coerceAtLeast(0L),
                firstRealArtworkMs = firstArtwork,
                uniqueArtworkRequests = requestSources.size,
                persistentArtworkRequests = requestSources.values.count { it == ArtworkRequestSource.PersistentFile },
                remoteArtworkRequests = requestSources.values.count { it == ArtworkRequestSource.Remote },
                missingArtworkRequests = requestSources.values.count { it == ArtworkRequestSource.Missing },
                artworkLoads = loadingKeys.size,
                artworkDisplays = displayedKeys.size,
                artworkFailures = failedKeys.size,
                artworkModelChanges = artworkModelChanges,
                placeholdersAfterFirstArtwork = latePlaceholderKeys.size,
                visibleHomeEmissions = visibleHomeEmissions,
                shimmerDisplays = shimmerDisplays,
                shimmerWithUsableContent = shimmerWithUsableContent
            )
        }
    }
}

internal object LevyraArtworkStartupMetrics {
    private val collector = ArtworkStartupMetricsCollector()

    fun beginSession() {
        collector.reset()
    }

    fun recordHomeEmission(fingerprint: String, hasUsableContent: Boolean) {
        collector.recordHomeEmission(fingerprint, hasUsableContent)
    }

    fun recordShimmer(hasUsableContent: Boolean) {
        collector.recordShimmer(hasUsableContent)
    }

    fun recordArtworkRequest(key: String, modelIdentity: String, source: ArtworkRequestSource) {
        collector.recordArtworkRequest(key, modelIdentity, source)
    }

    fun recordArtworkLoading(key: String) {
        collector.recordArtworkLoading(key)
    }

    fun recordArtworkDisplayed(key: String) {
        collector.recordArtworkDisplayed(key)
    }

    fun recordArtworkFailure(key: String) {
        collector.recordArtworkFailure(key)
    }

    fun snapshot(): ArtworkStartupMetricsSnapshot {
        return collector.snapshot()
    }

    fun persistSnapshot(context: Context) {
        val snapshot = collector.snapshot()
        val rate = String.format(Locale.US, "%.1f", snapshot.persistentRequestRate * 100.0)
        val violations = snapshot.regressionViolations()
        val message = "Artwork startup elapsed=${snapshot.elapsedMs}ms first=${snapshot.firstRealArtworkMs ?: -1}ms requests=${snapshot.uniqueArtworkRequests} persistent=${rate}% remote=${snapshot.remoteArtworkRequests} missing=${snapshot.missingArtworkRequests} loads=${snapshot.artworkLoads} displays=${snapshot.artworkDisplays} failures=${snapshot.artworkFailures} modelChanges=${snapshot.artworkModelChanges} latePlaceholders=${snapshot.placeholdersAfterFirstArtwork} homeEmissions=${snapshot.visibleHomeEmissions} shimmer=${snapshot.shimmerDisplays} shimmerWithContent=${snapshot.shimmerWithUsableContent}"
        if (violations.isEmpty()) {
            Timber.i(message)
        } else {
            Timber.w("$message violations=${violations.joinToString(",")}")
        }
        runCatching {
            val directory = File(context.applicationContext.filesDir, "diagnostics").apply { mkdirs() }
            val output = File(directory, "artwork-startup-metrics.json")
            val temporary = File(directory, "artwork-startup-metrics.json.tmp")
            val json = buildString {
                append('{')
                append("\"elapsedMs\":${snapshot.elapsedMs},")
                append("\"firstRealArtworkMs\":${snapshot.firstRealArtworkMs ?: -1L},")
                append("\"uniqueArtworkRequests\":${snapshot.uniqueArtworkRequests},")
                append("\"persistentArtworkRequests\":${snapshot.persistentArtworkRequests},")
                append("\"persistentRequestRate\":${String.format(Locale.US, "%.4f", snapshot.persistentRequestRate)},")
                append("\"remoteArtworkRequests\":${snapshot.remoteArtworkRequests},")
                append("\"missingArtworkRequests\":${snapshot.missingArtworkRequests},")
                append("\"artworkLoads\":${snapshot.artworkLoads},")
                append("\"artworkDisplays\":${snapshot.artworkDisplays},")
                append("\"artworkFailures\":${snapshot.artworkFailures},")
                append("\"artworkModelChanges\":${snapshot.artworkModelChanges},")
                append("\"placeholdersAfterFirstArtwork\":${snapshot.placeholdersAfterFirstArtwork},")
                append("\"visibleHomeEmissions\":${snapshot.visibleHomeEmissions},")
                append("\"shimmerDisplays\":${snapshot.shimmerDisplays},")
                append("\"shimmerWithUsableContent\":${snapshot.shimmerWithUsableContent},")
                append("\"violations\":[")
                append(violations.joinToString(",") { "\"$it\"" })
                append("]}")
            }
            temporary.writeText(json)
            if (output.exists()) output.delete()
            if (!temporary.renameTo(output)) {
                temporary.copyTo(output, overwrite = true)
                temporary.delete()
            }
        }.onFailure { error ->
            Timber.w(error, "Artwork startup metrics persistence failed")
        }
    }
}
