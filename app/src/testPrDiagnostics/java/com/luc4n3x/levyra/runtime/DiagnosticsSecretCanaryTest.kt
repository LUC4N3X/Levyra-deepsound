package com.luc4n3x.levyra.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsSecretCanaryTest {
    private val canaries = listOf(
        "LEVYRA_SECRET_CANARY_123456",
        "Bearer SUPER_SECRET_TOKEN",
        "cookie_secret_value",
        "signature=SECRET",
        "api_key=dummy_secret_key_value",
        "AI" + "zaSy" + "A".repeat(35),
        "https://media.example/audio?signature=SECRET"
    )

    @Test
    fun canariesNeverReachModelsOrExportEntries() {
        val events = canaries.mapIndexed { index, canary ->
            NetworkEvent(
                timestampMs = index.toLong(),
                uptimeMs = index.toLong(),
                provider = SafeDiagnosticMetadata.provider(canary),
                requestCategory = NetworkCategory.CONNECT,
                statusCode = 0,
                latencyMs = 10L,
                retry = 0,
                outcome = DiagnosticOutcome.FAILURE,
                failure = FailureCategory.NETWORK,
                redirectCount = 0
            )
        }
        val recorder = BoundedFlightRecorder()
        val detector = RuntimeAnomalyDetector()
        events.forEach { event ->
            recorder.record(event)
            detector.observe(event)
        }
        val recordedEvents = recorder.snapshot()
        val anomalies = detector.snapshot()
        val uiModel = RuntimeDiagnosticsSnapshot(
            active = true,
            eventCount = recordedEvents.size,
            anomalies = anomalies,
            currentMemory = null,
            playerState = DiagnosticPlayerState.UNKNOWN,
            resolverState = null,
            preflight = null
        )
        val output = ByteArrayOutputStream()

        RuntimeDiagnosticsExporter.writeArchive(
            output = output,
            summary = diagnosticsJson.encodeToString(DiagnosticsSummary.serializer(), DiagnosticsSummary(
                generatedAtMs = 1L,
                eventCount = uiModel.eventCount,
                anomalyCount = uiModel.anomalies.size,
                eventCounts = recordedEvents.groupingBy { it.category }.eachCount(),
                currentPssKb = null,
                peakPssKb = null,
                playerState = uiModel.playerState,
                preflightStatus = null
            )),
            events = recordedEvents,
            anomalies = diagnosticsJson.encodeToString(anomalies),
            environment = "{}",
            build = "{}"
        )

        val exported = unzip(output.toByteArray())
        val modelText = recordedEvents.joinToString("\n") { it.encodeJson() } +
            anomalies.toString() + uiModel.toString()
        canaries.forEach { canary ->
            assertFalse(modelText.contains(canary))
            assertFalse(exported.contains(canary))
        }
    }

    @Test
    fun exportRejectsEverySyntheticSecretMarker() {
        canaries.forEach { canary ->
            assertThrows(IllegalArgumentException::class.java) {
                RuntimeDiagnosticsExporter.writeArchive(
                    output = ByteArrayOutputStream(),
                    summary = canary,
                    events = emptyList(),
                    anomalies = "[]",
                    environment = "{}",
                    build = "{}"
                )
            }
        }
    }

    @Test
    fun diagnosticEventsCannotDeclareRawSensitiveFields() {
        val eventTypes = listOf(
            MemorySampleEvent::class.java,
            PlayerEvent::class.java,
            ResolverEvent::class.java,
            CacheEvent::class.java,
            DspEvent::class.java,
            CanvasEvent::class.java,
            NetworkEvent::class.java,
            HotOperationEvent::class.java,
            PreflightEvent::class.java
        )
        val forbiddenFragments = listOf(
            "response", "header", "cookie", "token", "url", "payload",
            "account", "authorization", "apikey", "password", "signature"
        )

        eventTypes.forEach { type ->
            val fieldNames = type.declaredFields.map { it.name.lowercase() }
            assertTrue(
                "${type.simpleName} exposes a sensitive raw field: $fieldNames",
                fieldNames.none { field ->
                    field == "request" || field.startsWith("rawrequest") ||
                        forbiddenFragments.any(field::contains)
                }
            )
        }
    }

    private fun unzip(bytes: ByteArray): String {
        val output = StringBuilder()
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += entry.name
                output.append(zip.readBytes().toString(Charsets.UTF_8))
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertEquals(5, entries.size)
        return output.toString()
    }
}
