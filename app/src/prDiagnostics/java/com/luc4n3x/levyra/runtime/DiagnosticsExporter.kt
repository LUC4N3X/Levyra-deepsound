package com.luc4n3x.levyra.runtime

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.luc4n3x.levyra.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class DiagnosticsSummary(
    val schemaVersion: Int = 1,
    val marker: String = DIAGNOSTICS_MARKER,
    val generatedAtMs: Long,
    val eventCount: Int,
    val anomalyCount: Int,
    val eventCounts: Map<DiagnosticCategory, Int>,
    val currentPssKb: Long?,
    val peakPssKb: Long?,
    val playerState: DiagnosticPlayerState,
    val preflightStatus: PreflightStatus?
)

@Serializable
internal data class DiagnosticsEnvironment(
    val schemaVersion: Int = 1,
    val androidSdk: Int,
    val manufacturer: String,
    val model: String,
    val abis: List<String>,
    val memoryClassMb: Int,
    val locale: String,
    val processUptimeMs: Long
)

@Serializable
internal data class DiagnosticsBuild(
    val schemaVersion: Int = 1,
    val versionName: String,
    val versionCode: Int,
    val commitSha: String?,
    val diagnosticsMarker: String = DIAGNOSTICS_MARKER
)

internal object RuntimeDiagnosticsExporter {
    private val forbiddenPatterns = listOf(
        Regex("(?i)LEVYRA_SECRET_CANARY_[A-Z0-9_]+"),
        Regex("(?i)bearer\\s+\\S+"),
        Regex("(?i)cookie[_-]?secret(?:[_-]?value)?"),
        Regex("(?i)(authorization|cookie|access[_-]?token|refresh[_-]?token|api[_-]?key|signature)\\s*[:=]"),
        Regex("AIza[0-9A-Za-z_-]{20,}"),
        Regex("(?i)https?://")
    )

    suspend fun export(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            writeArchive(context.applicationContext, output)
        } ?: error("Unable to open diagnostics export destination")
    }

    fun writeArchive(context: Context, output: OutputStream): Long {
        val events = RuntimeDiagnostics.eventSnapshot()
        val anomalies = RuntimeDiagnostics.anomalySnapshot()
        val runtime = RuntimeDiagnostics.snapshot()
        val latestMemory = runtime.currentMemory
        val summary = DiagnosticsSummary(
            generatedAtMs = System.currentTimeMillis(),
            eventCount = events.size,
            anomalyCount = anomalies.size,
            eventCounts = events.groupingBy { it.category }.eachCount(),
            currentPssKb = latestMemory?.pssKb,
            peakPssKb = latestMemory?.peakPssKb,
            playerState = runtime.playerState,
            preflightStatus = runtime.preflight?.status
        )
        val environment = DiagnosticsEnvironment(
            androidSdk = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.take(80),
            model = Build.MODEL.take(80),
            abis = Build.SUPPORTED_ABIS.take(8),
            memoryClassMb = context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 0,
            locale = Locale.getDefault().toLanguageTag().take(35),
            processUptimeMs = SystemClock.elapsedRealtime()
        )
        val build = DiagnosticsBuild(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            commitSha = BuildConfig.INTERNAL_DIAGNOSTICS_COMMIT.takeIf { it.isNotBlank() }
        )
        return writeArchive(
            output = output,
            summary = diagnosticsJson.encodeToString(summary),
            events = events,
            anomalies = diagnosticsJson.encodeToString(anomalies),
            environment = diagnosticsJson.encodeToString(environment),
            build = diagnosticsJson.encodeToString(build)
        )
    }

    internal fun writeArchive(
        output: OutputStream,
        summary: String,
        events: List<DiagnosticEvent>,
        anomalies: String,
        environment: String,
        build: String
    ): Long {
        val entries = linkedMapOf(
            "summary.json" to summary.toByteArray(),
            "events.ndjson" to encodeEvents(events),
            "anomalies.json" to anomalies.toByteArray(),
            "environment.json" to environment.toByteArray(),
            "build.json" to build.toByteArray()
        )
        val total = entries.values.sumOf { it.size.toLong() }
        require(total <= MAX_DIAGNOSTICS_EXPORT_BYTES) { "Diagnostics export exceeds its fixed size bound" }
        entries.values.forEach(::requireSafe)
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return total
    }

    private fun encodeEvents(events: List<DiagnosticEvent>): ByteArray {
        val output = ByteArrayOutputStream()
        for (event in events) {
            val line = (event.encodeJson() + "\n").toByteArray()
            if (output.size().toLong() + line.size > MAX_EVENTS_EXPORT_BYTES) break
            output.write(line)
        }
        return output.toByteArray()
    }

    private fun requireSafe(bytes: ByteArray) {
        val value = bytes.toString(Charsets.UTF_8)
        require(forbiddenPatterns.none { it.containsMatchIn(value) }) { "Diagnostics export rejected unsafe content" }
    }
}
