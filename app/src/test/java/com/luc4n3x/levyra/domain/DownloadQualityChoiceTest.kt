package com.luc4n3x.levyra.domain

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQualityChoiceTest {
    private val payload = """{"id":"abc","title":"Song"}"""

    @Test
    fun choiceSurvivesTheDurablePayloadUsedByRetryAndResume() {
        val choice = DownloadQualityChoice(DownloadQualitySource.Lossless, kbps = 2_800, bitDepth = 24, sampleRateHz = 96_000)
        val durable = withDownloadRepairIdentity(withDownloadQualityChoice(payload, choice), 42L)

        assertEquals(choice, downloadQualityChoice(durable))
        assertEquals(42L, downloadRepairIdentity(durable))
        assertEquals(choice, downloadQualityChoice(withDownloadQualityChoice(durable, null)))
    }

    @Test
    fun automaticDownloadsCarryNoChoice() {
        assertSame(payload, withDownloadQualityChoice(payload, null))
        assertNull(downloadQualityChoice(payload))
        assertNull(downloadQualityChoice("not json"))
        assertNull(downloadQualityChoice("""{"levyraDownloadQuality":{"source":"future"}}"""))
    }

    @Test
    fun profileKeysSeparateQualityProfiles() {
        assertEquals("LosslessHiRes", DownloadQualityChoice(DownloadQualitySource.Lossless, bitDepth = 24, sampleRateHz = 96_000).profileKey)
        assertEquals("Lossless", DownloadQualityChoice(DownloadQualitySource.Lossless, bitDepth = 16, sampleRateHz = 44_100).profileKey)
        assertEquals("HQ320", DownloadQualityChoice(DownloadQualitySource.HighBitrate, kbps = 320).profileKey)
        assertEquals("Standard", DownloadQualityChoice(DownloadQualitySource.Standard, kbps = 129).profileKey)
    }

    @Test
    fun workerReadsTheChoiceFromThePayloadAndPassesItToTheExportPipeline() {
        val worker = source("player/offline/work/OfflineExportWorker.kt")
        assertTrue(worker.contains("val qualityChoice = downloadQualityChoice(payload)"))
        assertTrue(worker.contains("qualityChoice?.profileKey ?: settings.storedQualityKey"))
        assertTrue(worker.contains("qualityChoice = qualityChoice"))
        assertTrue(worker.contains("trackPayload = task.payload"))

        val pipeline = source("player/offline/OfflineExportPipeline.kt")
        assertTrue(pipeline.contains("qualityChoice?.source"))
        assertTrue(pipeline.contains("!qualityChoice.isSatisfiedBy(resolved)) throw DownloadQualityUnavailableException()"))

        val exporter = source("player/offline/OfflineAudioExporter.kt")
        assertTrue(exporter.contains("if (!choice.isSatisfiedBy(resolved)) throw DownloadQualityUnavailableException()"))
    }

    @Test
    fun automaticAndBatchDownloadsBypassThePicker() {
        val viewModel = source("viewmodel/LevyraViewModel.kt")
        assertTrue(viewModel.contains("withDownloadQualityChoice(TrackPayloadCodec.encode(track), qualityChoice)"))
        assertEquals(3, Regex("""startTrackDownload\(track, null\)""").findAll(viewModel).count())
        assertTrue(viewModel.contains("openDownloadQualityPicker(track, downloadKey)"))
    }

    private fun source(relative: String): String = Files.readString(
        sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relative"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relative")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relative")
    )
}
