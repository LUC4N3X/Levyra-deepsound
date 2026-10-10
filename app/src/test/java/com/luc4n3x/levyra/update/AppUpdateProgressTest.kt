package com.luc4n3x.levyra.update

import com.luc4n3x.levyra.data.parseUpdatePublishedAt
import com.luc4n3x.levyra.ui.isLaunchableUpdateTarget
import com.luc4n3x.levyra.ui.update.formatUpdateReleaseDate
import com.luc4n3x.levyra.ui.update.updateMetaLine
import com.luc4n3x.levyra.ui.update.levyraUpdateNoteLines
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateProgressTest {
    @Test
    fun percentIsReportedOnlyWhenTheTotalSizeIsKnown() {
        assertEquals(0, updateProgressPercent(0L, 100L))
        assertEquals(50, updateProgressPercent(50L, 100L))
        assertEquals(100, updateProgressPercent(100L, 100L))
        assertNull(updateProgressPercent(50L, null))
        assertNull(updateProgressPercent(50L, 0L))
    }

    @Test
    fun unknownContentLengthNeverProducesAFakeEtaOrPercent() {
        val line = formatUpdateTransferLine(
            downloadedBytes = 3L * 1024L * 1024L,
            totalBytes = null,
            bytesPerSecond = 2.0 * 1024L * 1024L
        )
        assertTrue(line.contains("3.0 MB"))
        assertTrue(line.contains("2.0 MB/s"))
        assertTrue(!line.contains("~"))
        assertNull(updateProgressPercent(3L * 1024L * 1024L, null))
    }

    @Test
    fun etaRequiresAMeasuredSpeedAndRemainingBytes() {
        assertNull(updateEtaMs(remainingBytes = 1_000_000L, bytesPerSecond = null))
        assertNull(updateEtaMs(remainingBytes = 0L, bytesPerSecond = 1_000_000.0))
        assertNull(updateEtaMs(remainingBytes = 1_000_000L, bytesPerSecond = 10.0))
        assertEquals(2_000L, updateEtaMs(remainingBytes = 2_000_000L, bytesPerSecond = 1_000_000.0))
    }

    @Test
    fun speedSmoothingDampensSpikesWithoutIgnoringThem() {
        val first = smoothUpdateSpeed(previous = null, sampleBytesPerSecond = 1_000_000.0)
        assertEquals(1_000_000.0, first!!, 0.001)
        val second = smoothUpdateSpeed(previous = first, sampleBytesPerSecond = 5_000_000.0)!!
        assertTrue(second > first)
        assertTrue(second < 5_000_000.0)
        assertEquals(second, smoothUpdateSpeed(previous = second, sampleBytesPerSecond = 0.0)!!, 0.001)
    }

    @Test
    fun transferLineShowsRealDownloadedAndTotalBytes() {
        val line = formatUpdateTransferLine(
            downloadedBytes = 13_002_342L,
            totalBytes = 33_345_536L,
            bytesPerSecond = 5_976_883.0
        )
        assertTrue(line.startsWith("12.4 MB / 31.8 MB"))
        assertTrue(line.contains("5.7 MB/s"))
        assertTrue(line.contains("~3 s"))
        assertEquals("12.4 MB / 31.8 MB · 5.7 MB/s · ~3 s", line)
    }

    @Test
    fun byteAndDurationFormattingStaysStableAcrossLocales() {
        assertEquals("1.0 MB", formatUpdateBytes(1024L * 1024L))
        assertEquals("512 KB", formatUpdateBytes(512L * 1024L))
        assertEquals("900 B", formatUpdateBytes(900L))
        assertEquals("0 B", formatUpdateBytes(-5L))
        assertEquals("9 s", formatUpdateDuration(9_400L))
        assertEquals("1 m 20 s", formatUpdateDuration(80_000L))
        assertEquals("1 h 01 m", formatUpdateDuration(3_660_000L))
        assertNull(formatUpdateDuration(null))
        assertNull(formatUpdateSpeed(null))
        assertNull(formatUpdateSpeed(12.0))
    }

    @Test
    fun speedTrackerIgnoresSamplesTakenTooCloseTogether() {
        val tracker = AppUpdateSpeedTracker()
        assertNull(tracker.sample(downloadedBytes = 0L, atElapsedMs = 0L))
        assertNull(tracker.sample(downloadedBytes = 64L * 1024L, atElapsedMs = 100L))
        val measured = tracker.sample(downloadedBytes = 1024L * 1024L, atElapsedMs = 1_000L)
        assertNotNull(measured)
        assertTrue(measured!! > 0.0)
    }

    @Test
    fun speedTrackerRecoversWhenADownloadRestarts() {
        val tracker = AppUpdateSpeedTracker()
        tracker.sample(downloadedBytes = 0L, atElapsedMs = 0L)
        tracker.sample(downloadedBytes = 2L * 1024L * 1024L, atElapsedMs = 1_000L)
        assertNotNull(tracker.bytesPerSecond)
        tracker.sample(downloadedBytes = 0L, atElapsedMs = 1_500L)
        tracker.reset()
        assertNull(tracker.bytesPerSecond)
    }

    @Test
    fun releaseNotesAreStrippedOfMarkdownAndRedundantVersionHeadings() {
        val lines = levyraUpdateNoteLines(
            notes = """
                ## Levyra v2.4.1
                - **Faster** skips
                - `cache` recovery
                ---
                - [Details](https://example.com)
                - Faster skips
            """.trimIndent(),
            version = "2.4.1"
        )
        assertEquals(listOf("Faster skips", "cache recovery", "Details"), lines)
    }

    @Test
    fun settingsDownloadOnlyLaunchesHttpsOrTheInternalInstallHandoff() {
        assertTrue(isLaunchableUpdateTarget("https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertTrue(isLaunchableUpdateTarget(AppUpdateContract.INSTALL_URI))
        assertFalse(isLaunchableUpdateTarget("http://github.com/LUC4N3X/Levyra-deepsound/releases"))
        assertFalse(isLaunchableUpdateTarget("javascript:alert(1)"))
        assertFalse(isLaunchableUpdateTarget("file:///data/local/tmp/evil.apk"))
        assertFalse(isLaunchableUpdateTarget("levyra-internal://updates/other"))
        assertFalse(isLaunchableUpdateTarget(""))
    }

    @Test
    fun releaseMetaLineOmitsWhatIsNotKnown() {
        val published = parseUpdatePublishedAt("2026-04-22T04:56:00Z")
        assertEquals(
            "31.8 MB",
            updateMetaLine(publishedAtEpochMs = 0L, assetSizeBytes = 33_345_536L, languageCode = "it")
        )
        assertTrue(
            updateMetaLine(publishedAtEpochMs = published, assetSizeBytes = 0L, languageCode = "it")
                .contains("2026")
        )
        assertEquals(
            "",
            updateMetaLine(publishedAtEpochMs = 0L, assetSizeBytes = 0L, languageCode = "it")
        )
        assertTrue(
            updateMetaLine(publishedAtEpochMs = published, assetSizeBytes = 33_345_536L, languageCode = "it")
                .endsWith(" · 31.8 MB")
        )
    }

    @Test
    fun publishedDatesAreParsedAndValidatedOffTheUiThread() {
        assertEquals(0L, parseUpdatePublishedAt(""))
        assertEquals(0L, parseUpdatePublishedAt("   "))
        assertEquals(0L, parseUpdatePublishedAt("22/04/2026"))
        assertEquals(0L, parseUpdatePublishedAt("2026-04-22"))
        assertTrue(parseUpdatePublishedAt("2026-04-22T04:56:00Z") > 0L)
    }

    @Test
    fun malformedReleaseDatesNeverCrashTheUpdateScreen() {
        assertNull(formatUpdateReleaseDate(0L, "it"))
        assertNull(formatUpdateReleaseDate(-1L, "it"))
        assertNotNull(formatUpdateReleaseDate(parseUpdatePublishedAt("2026-04-22T04:56:00Z"), "en"))
    }

    @Test
    fun fdroidBuildKeepsTheGithubUpdaterDisabled() {
        val buildFile = repositoryFile("app/build.gradle.kts")
        val installer = repositoryFile("app/src/main/java/com/luc4n3x/levyra/update/AppUpdateInstaller.kt")
        val activity = repositoryFile("app/src/main/java/com/luc4n3x/levyra/MainActivity.kt")
        val viewModel = repositoryFile("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")

        assertTrue(
            Files.readString(buildFile)
                .contains("UPSTREAM_UPDATES_ENABLED\", (!isFdroidBuild).toString()")
        )
        assertTrue(Files.readString(installer).contains("!BuildConfig.UPSTREAM_UPDATES_ENABLED"))
        assertTrue(Files.readString(viewModel).contains("!BuildConfig.UPSTREAM_UPDATES_ENABLED"))
        val activitySource = Files.readString(activity)
        assertTrue(activitySource.contains("!BuildConfig.UPSTREAM_UPDATES_ENABLED"))
        assertTrue(activitySource.contains("BuildConfig.UPSTREAM_UPDATES_ENABLED &&"))
        assertTrue(activitySource.contains("!activityUiState.showOnboarding"))
    }

    private fun repositoryFile(relativePath: String): Path = sequenceOf(
        Path.of(relativePath),
        Path.of(relativePath.removePrefix("app/"))
    ).firstOrNull(Files::exists) ?: error("$relativePath not found")
}
