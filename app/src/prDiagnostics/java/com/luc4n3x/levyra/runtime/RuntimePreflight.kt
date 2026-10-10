package com.luc4n3x.levyra.runtime

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.PlaybackAudioStrategy
import com.luc4n3x.levyra.data.PlaybackVideoStrategy
import com.luc4n3x.levyra.data.AppUpdateRepository
import com.luc4n3x.levyra.data.validateLevyraReleaseMetadataUrl
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

@Serializable
internal enum class PreflightStatus {
    PASS,
    WARNING,
    FAIL
}

@Serializable
internal data class PreflightResult(
    val checkId: String,
    val component: String,
    val status: PreflightStatus,
    val message: String,
    val details: String? = null
)

@Serializable
internal data class PreflightReport(
    val schemaVersion: Int = 1,
    val generatedAtMs: Long,
    val status: PreflightStatus,
    val results: List<PreflightResult>
) {
    companion object {
        fun from(generatedAtMs: Long, results: List<PreflightResult>): PreflightReport {
            val status = when {
                results.any { it.status == PreflightStatus.FAIL } -> PreflightStatus.FAIL
                results.any { it.status == PreflightStatus.WARNING } -> PreflightStatus.WARNING
                else -> PreflightStatus.PASS
            }
            return PreflightReport(generatedAtMs = generatedAtMs, status = status, results = results)
        }
    }
}

internal object RuntimePreflight {
    private const val MAX_ASSET_BYTES = 1_048_576

    fun run(context: Context): PreflightReport = PreflightReport.from(
        generatedAtMs = System.currentTimeMillis(),
        results = listOf(
            diagnosticsVariantCheck(),
            extractorRegistryCheck(),
            runtimeConfigCheck(context),
            providerAllowlistCheck(context),
            canvasConfigurationCheck(),
            editorialCatalogCheck(context)
        )
    )

    private fun diagnosticsVariantCheck(): PreflightResult = if (
        BuildConfig.INTERNAL_DIAGNOSTICS && BuildConfig.INTERNAL_DIAGNOSTICS_MARKER == DIAGNOSTICS_MARKER
    ) {
        pass("build.variant", "build", "PR diagnostics variant marker is valid")
    } else {
        fail("build.variant", "build", "PR diagnostics variant marker is invalid")
    }

    private fun extractorRegistryCheck(): PreflightResult {
        val audio = PlaybackAudioStrategy.entries.toSet()
        val video = PlaybackVideoStrategy.entries.toSet()
        val valid = extractorRegistryComplete(audio.map { it.name }.toSet(), video.map { it.name }.toSet())
        return if (valid) {
            pass("resolver.registry", "resolver", "Resolver strategy registry is complete", "audio=${audio.size},video=${video.size}")
        } else {
            fail("resolver.registry", "resolver", "Resolver strategy registry is incomplete")
        }
    }

    private fun runtimeConfigCheck(context: Context): PreflightResult {
        val valid = readJsonAsset(context, "player_configs.json")
        return when {
            !valid -> fail("runtime.player-config", "extractor", "Bundled player configuration is invalid")
            BuildConfig.YOUTUBE_INNERTUBE_API_KEY.isBlank() -> warning("runtime.player-config", "extractor", "Runtime API identifier is unavailable")
            else -> pass("runtime.player-config", "extractor", "Bundled player configuration is valid")
        }
    }

    private fun providerAllowlistCheck(context: Context): PreflightResult {
        return when (providerPreflightStatus(providerEndpointAllowed(BuildConfig.UPDATE_LATEST_URL)) {
            AppUpdateRepository(context).validateLatestReleaseMetadata()
        }) {
            PreflightStatus.PASS -> pass("provider.allowlist", "network", "Provider endpoint and bounded metadata response are valid")
            PreflightStatus.WARNING -> warning("provider.allowlist", "network", "Provider endpoint is valid but metadata is unavailable")
            PreflightStatus.FAIL -> fail("provider.allowlist", "network", "Provider endpoint allowlist is invalid")
        }
    }

    private fun canvasConfigurationCheck(): PreflightResult {
        val quality = LevyraCanvasQuality.entries.map { it.name }.toSet()
        val source = LevyraCanvasSource.entries.map { it.name }.toSet()
        val valid = canvasConfigurationComplete(quality, source)
        return if (valid) {
            pass("canvas.configuration", "canvas", "Canvas configuration is internally consistent", "quality=${quality.size},source=${source.size}")
        } else {
            fail("canvas.configuration", "canvas", "Canvas configuration contains duplicate values")
        }
    }

    private fun editorialCatalogCheck(context: Context): PreflightResult {
        val catalogValid = readJsonAsset(context, "editorial/spotify-bootstrap.json")
        val announcementsValid = readJsonAsset(context, "config/announcements.json")
        return if (catalogValid && announcementsValid) {
            pass("catalog.assets", "catalog", "Bundled catalog assets are valid JSON")
        } else {
            fail("catalog.assets", "catalog", "A bundled catalog asset is invalid")
        }
    }

    private fun readJsonAsset(context: Context, path: String): Boolean = runCatching {
        val output = ByteArrayOutputStream()
        context.assets.open(path).use { input ->
            val buffer = ByteArray(8_192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                check(total <= MAX_ASSET_BYTES)
                output.write(buffer, 0, read)
            }
        }
        jsonDocumentValid(output.toString(Charsets.UTF_8.name()))
    }.getOrDefault(false)

    private fun pass(id: String, component: String, message: String, details: String? = null) =
        PreflightResult(id, component, PreflightStatus.PASS, message, details)

    private fun warning(id: String, component: String, message: String, details: String? = null) =
        PreflightResult(id, component, PreflightStatus.WARNING, message, details)

    private fun fail(id: String, component: String, message: String, details: String? = null) =
        PreflightResult(id, component, PreflightStatus.FAIL, message, details)
}

internal fun extractorRegistryComplete(audio: Set<String>, video: Set<String>): Boolean =
    audio.containsAll(setOf("REEL_MUXED", "REEL_AUDIO", "PERSISTED", "DIRECT", "SEARCH")) &&
        video.containsAll(setOf("PERSISTED", "STANDARD", "REEL"))

internal fun providerEndpointAllowed(value: String): Boolean = validateLevyraReleaseMetadataUrl(value) != null

internal fun providerPreflightStatus(configAllowed: Boolean, probe: () -> Boolean): PreflightStatus {
    if (!configAllowed) return PreflightStatus.FAIL
    return try {
        if (probe()) PreflightStatus.PASS else PreflightStatus.WARNING
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        PreflightStatus.WARNING
    }
}

internal fun canvasConfigurationComplete(quality: Set<String>, source: Set<String>): Boolean =
    quality.containsAll(setOf("Auto", "DataSaver", "High")) &&
        source.containsAll(setOf("Auto", "Community", "Apple", "Tidal"))

internal fun jsonDocumentValid(value: String): Boolean = runCatching {
    val normalized = value.trim()
    when {
        normalized.startsWith("{") -> JSONObject(normalized)
        normalized.startsWith("[") -> JSONArray(normalized)
        else -> return@runCatching false
    }
    true
}.getOrDefault(false)
