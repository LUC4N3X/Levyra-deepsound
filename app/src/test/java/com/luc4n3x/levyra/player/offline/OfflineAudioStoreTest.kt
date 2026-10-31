package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.reconcileDownloadedTracks
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineAudioStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val privateUri = "file:///data/user/0/com.luc4n3x.levyra/files/offline_audio/abc.m4a"
    private val mediaStoreUri = "content://media/external_primary/audio/media/42"
    private val documentUri = "content://com.android.externalstorage.documents/tree/1234%3AMusic/document/1234%3AMusic%2Fa.m4a"

    private fun store(): OfflineAudioStore = OfflineAudioStore(temporaryFolder.root)

    private fun staged(store: OfflineAudioStore, name: String, content: String): File =
        File(store.stagingWorkspace(), name).apply { writeText(content) }

    private fun download(uri: String, title: String = "Song", fileName: String = "Artist - Song.m4a") = DownloadEntity(
        trackId = "track",
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 1_000L,
        fileName = fileName,
        uri = uri,
        mimeType = "audio/mp4",
        embeddedMetadata = true,
        downloadPreset = "Automatic",
        downloadQuality = "High",
        savedAt = 1L
    )

    @Test
    fun committedAudioLivesInThePrivateDirectoryAndLeavesNoStagingFile() {
        val store = store()
        val part = staged(store, "resume-track.m4a.part", "audio")

        val committed = store.commit(part, "stable", "m4a")

        assertEquals(File(File(temporaryFolder.root, "offline_audio"), "stable.m4a"), committed)
        assertEquals("audio", committed.readText())
        assertFalse(part.exists())
        assertTrue(store.stagingDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun recommittingReplacesTheFinalFileInsteadOfDuplicatingIt() {
        val store = store()
        store.commit(staged(store, "first.part", "old"), "stable", "flac")

        val replaced = store.commit(staged(store, "second.part", "new-audio"), "stable", "flac")

        assertEquals("new-audio", replaced.readText())
        assertEquals(listOf("stable.flac"), store.audioDirectory.list().orEmpty().toList())
    }

    @Test
    fun anEmptyOrMissingStagingFileNeverReachesTheFinalDirectory() {
        val store = store()
        val empty = staged(store, "empty.part", "")

        assertThrows(IOException::class.java) { store.commit(empty, "stable", "m4a") }
        assertThrows(IOException::class.java) { store.commit(File(store.stagingDirectory, "gone.part"), "stable", "m4a") }
        assertFalse(File(store.audioDirectory, "stable.m4a").exists())
    }

    @Test
    fun extensionsCannotEscapeTheAudioDirectory() {
        val store = store()

        val committed = store.commit(staged(store, "x.part", "audio"), "stable", "../../evil")

        assertEquals(store.audioDirectory, committed.parentFile)
        assertEquals("stable.evil", committed.name)
    }

    @Test
    fun stableIdsSeparateQualityProfilesAndStayDeterministic() {
        val base = offlineAudioStableId("track", "Automatic", "lossless")

        assertEquals(base, offlineAudioStableId("track", "Automatic", "lossless"))
        assertNotEquals(base, offlineAudioStableId("track", "Automatic", "aac"))
        assertNotEquals(base, offlineAudioStableId("other", "Automatic", "lossless"))
    }

    @Test
    fun sweepKeepsResumableDataOfFailedOrPausedTasksForAsLongAsTheExporter() {
        val store = store()
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - TimeUnit.HOURS.toMillis(3)
        val orphanRaw = staged(store, "raw-1.m4a", "x").apply { setLastModified(threeHoursAgo) }
        val orphanTagged = staged(store, "tagged-1.m4a", "x").apply { setLastModified(threeHoursAgo) }
        val failedResume = staged(store, "resume-failed.m4a.part", "x").apply { setLastModified(now - TimeUnit.DAYS.toMillis(6)) }
        val failedIdentity = staged(store, "resume-failed.m4a.part.id", "x").apply { setLastModified(now - TimeUnit.DAYS.toMillis(6)) }
        val staleResume = staged(store, "resume-paused.m4a.part", "x").apply { setLastModified(now - TimeUnit.DAYS.toMillis(8)) }
        val freshRaw = staged(store, "raw-2.m4a", "x")

        store.sweep(emptyList(), now)

        assertFalse(orphanRaw.exists())
        assertFalse(orphanTagged.exists())
        assertFalse(staleResume.exists())
        assertTrue(failedResume.exists())
        assertTrue(failedIdentity.exists())
        assertTrue(freshRaw.exists())
    }

    @Test
    fun stagingRetentionIsTheSameRuleTheExporterApplies() {
        assertEquals(TimeUnit.DAYS.toMillis(7), offlineStagingRetentionMs("resume-key.flac.part"))
        assertEquals(TimeUnit.DAYS.toMillis(7), offlineStagingRetentionMs("resume-key.flac.part.id"))
        assertEquals(TimeUnit.HOURS.toMillis(2), offlineStagingRetentionMs("resume-key.flac"))
        assertEquals(TimeUnit.HOURS.toMillis(2), offlineStagingRetentionMs("raw-cache-1.m4a"))
        val exporter = read("java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        assertTrue(exporter.contains("offlineStagingRetentionMs(file.name)"))
        assertFalse(exporter.contains("TimeUnit.DAYS.toMillis(7)"))
    }

    @Test
    fun registrationIsSerializedWithBackupRestore() {
        val exporter = read("java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        val backup = read("java/com/luc4n3x/levyra/data/LevyraBackupManager.kt")
        val register = exporter.substringAfter("private suspend fun registerUnderVaultLock").substringBefore("\n    }")

        assertTrue(register.contains("LevyraVaultOperationMutex.withLock"))
        assertTrue(register.contains("registerDownload(download)"))
        val restore = backup.substringAfter("retainedOfflineAudio").substringBefore("applySnapshot(target, downloads)")
        assertTrue(backup.substringBefore("val retainedOfflineAudio").substringAfterLast("suspend fun").contains("LevyraVaultOperationMutex.withLock"))
        assertTrue(restore.contains("reconcileDownloadedTracks"))
    }

    @Test
    fun sweepDeletesOnlyUnregisteredFinalAudioPastItsGracePeriod() {
        val store = store()
        val now = System.currentTimeMillis()
        val registered = store.commit(staged(store, "a.part", "x"), "registered", "m4a")
        val orphan = store.commit(staged(store, "b.part", "x"), "orphan", "m4a")
        val justCommitted = store.commit(staged(store, "c.part", "x"), "fresh", "m4a")
        val old = now - TimeUnit.HOURS.toMillis(2)
        registered.setLastModified(old)
        orphan.setLastModified(old)

        store.sweep(
            listOf("file:///data/user/0/pkg/files/offline_audio/registered.m4a", mediaStoreUri),
            now
        )

        assertTrue(registered.exists())
        assertFalse(orphan.exists())
        assertTrue(justCommitted.exists())
    }

    @Test
    fun onlyPrivateUrisAreEverConsideredAbsent() {
        val store = store()
        store.commit(staged(store, "a.part", "x"), "present", "m4a")

        assertTrue(isOfflineAudioStoreUri(privateUri))
        assertFalse(isOfflineAudioStoreUri(mediaStoreUri))
        assertFalse(isOfflineAudioStoreUri(documentUri))
        assertFalse(isOfflineAudioStoreUri("file:///storage/emulated/0/Music/Levyra/a.m4a"))
        assertTrue(store.isAbsent(privateUri))
        assertFalse(store.isAbsent("file:///data/user/0/pkg/files/offline_audio/present.m4a"))
        assertFalse(store.isAbsent(mediaStoreUri))
        assertFalse(store.isAbsent(documentUri))
        assertFalse(store.isAbsent("file:///storage/emulated/0/Music/Levyra/a.m4a"))
    }

    @Test
    fun storageExhaustionIsRecognisedThroughWrappedCauses() {
        assertTrue(isOfflineStorageExhausted(OfflineStorageFullException()))
        assertTrue(isOfflineStorageExhausted(IOException("write failed: ENOSPC (No space left on device)")))
        assertTrue(isOfflineStorageExhausted(IOException("segment failed", IOException("No space left on device"))))
        assertFalse(isOfflineStorageExhausted(IOException("HTTP 403")))
    }

    @Test
    fun restoreKeepsReadableLegacyAndPrivateRowsAndDropsAbsentOfflineFiles() {
        val missingPrivate = "file:///data/user/0/pkg/files/offline_audio/missing.m4a"
        val restored = reconcileDownloadedTracks(
            backedUp = listOf(download(mediaStoreUri), download(documentUri, "Saf"), download(missingPrivate, "Gone"), download(privateUri, "Kept")),
            discovered = emptyList(),
            isReadable = { it != missingPrivate }
        )

        assertEquals(setOf(mediaStoreUri, documentUri, privateUri), restored.map { it.uri }.toSet())
    }

    @Test
    fun aLegacyFileNeverAdoptsAPrivateRowWithTheSameMetadataNorTheReverse() {
        val legacyUri = "content://media/external_primary/audio/media/7"
        val fromPrivate = reconcileDownloadedTracks(
            backedUp = listOf(download(privateUri)),
            discovered = listOf(download(legacyUri)),
            isReadable = { true }
        )
        val fromLegacy = reconcileDownloadedTracks(
            backedUp = listOf(download(documentUri)),
            discovered = listOf(download(privateUri)),
            isReadable = { true }
        )

        assertEquals(setOf(privateUri, legacyUri), fromPrivate.map { it.uri }.toSet())
        assertEquals(setOf(documentUri, privateUri), fromLegacy.map { it.uri }.toSet())
    }

    @Test
    fun aLibraryScanKeepsDownloadsChangedWhileItRan() {
        val unchanged = download("file:///files/offline_audio/a.m4a")
        val scanned = download("file:///files/offline_audio/b.m4a", title = "Old")
        val deleted = download("content://media/external/audio/media/9")
        val discovered = download("content://media/external/audio/media/10")
        val updated = scanned.copy(title = "New")
        val registered = download("file:///files/offline_audio/c.m4a")

        val result = com.luc4n3x.levyra.data.locallibrary.keepDownloadsChangedDuringScan(
            merged = listOf(unchanged, scanned, deleted, discovered),
            scanned = listOf(unchanged, scanned, deleted),
            fresh = listOf(unchanged, updated, registered)
        )

        assertEquals(
            setOf(unchanged, updated, registered, discovered),
            result.toSet()
        )
    }

    @Test
    fun storageFullFailureKeepsItsLocalizedMessage() {
        val viewModel = read("java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
        assertFalse(viewModel.contains("offlineExportMessage = cleanUserError(message)"))
        assertTrue(viewModel.contains("offlineExportMessage = message ?: cleanUserError(null)"))

        val italian = com.luc4n3x.levyra.ui.i18n.LevyraStrings.forCode("it")
        assertEquals(italian.offlineStorageFull, italian.localizeUserError(OFFLINE_STORAGE_FULL))
        assertTrue(italian.offlineStorageFull != italian.operationFailed)
    }

    @Test
    fun theFinalPipelineNeverPublishesOutsidePrivateStorage() {
        val exporter = read("java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        val store = read("java/com/luc4n3x/levyra/player/offline/OfflineAudioStore.kt")

        listOf(exporter, store).forEach { source ->
            listOf(
                "MediaStore",
                "DocumentsContract",
                "DownloadFolderAccess",
                "Environment.",
                "destinationTreeUri",
                "folderMode",
                "ContentValues",
                "DIRECTORY_MUSIC"
            ).forEach { forbidden -> assertFalse(forbidden, source.contains(forbidden)) }
        }
        assertTrue(exporter.contains("val workspace = audioStore.stagingWorkspace()"))
        assertTrue(exporter.contains("audioStore.commit(staged, stableId, container.extension)"))
    }

    @Test
    fun theFolderPickerAndPublicDestinationAreGoneFromTheUi() {
        val app = read("java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        val overlay = read("java/com/luc4n3x/levyra/ui/DownloadsFolderOverlay.kt")
        val viewModel = read("java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")

        assertFalse(app.contains("downloadLocationLauncher"))
        assertFalse(app.contains("onSelectDownloadLocation"))
        assertFalse(app.contains("downloadFolderOrganization"))
        assertFalse(overlay.contains("Music/Levyra"))
        assertFalse(viewModel.contains("Music/Levyra"))
    }

    @Test
    fun privateAudioIsExcludedFromBackupTransferAndFileSharing() {
        val backup = read("res/xml/backup_rules.xml")
        val extraction = read("res/xml/data_extraction_rules.xml")
        val manifest = read("AndroidManifest.xml")

        assertTrue(backup.contains("""<include domain="sharedpref" path="." />"""))
        assertFalse(backup.contains("domain=\"file\""))
        val cloud = extraction.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        assertTrue(cloud.contains("""<include domain="sharedpref" path="." />"""))
        assertFalse(cloud.contains("domain=\"file\""))
        val transfer = extraction.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        assertTrue(transfer.contains("""<exclude domain="file" path="$OFFLINE_AUDIO_DIRECTORY/" />"""))
        assertTrue(transfer.contains("""<exclude domain="file" path="$OFFLINE_AUDIO_STAGING_DIRECTORY/" />"""))
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        listOf("res/xml/share_file_paths.xml", "res/xml/update_file_paths.xml").forEach { paths ->
            val content = read(paths)
            assertFalse(content.contains("files-path"))
            assertFalse(content.contains("root-path"))
            assertFalse(content.contains("offline_audio"))
        }
    }

    @Test
    fun legacyDeletionAndLocalLibraryRecognitionRemainIntact() {
        val coordinator = read("java/com/luc4n3x/levyra/ui/library/OfflineDeleteCoordinator.kt")
        val viewModel = read("java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
        val backup = read("java/com/luc4n3x/levyra/data/LevyraBackupManager.kt")
        val metadata = read("java/com/luc4n3x/levyra/data/locallibrary/LocalMediaMetadata.kt")
        val access = read("java/com/luc4n3x/levyra/data/DownloadFolderAccess.kt")

        assertTrue(coordinator.contains("MediaStore.createDeleteRequest"))
        assertTrue(viewModel.contains("resolver.delete(uri, null, null)"))
        assertTrue(backup.contains("scanLevyraDownloads()"))
        assertTrue(metadata.contains("""LEVYRA_DOWNLOAD_RELATIVE_PATH = "Music/Levyra/""""))
        assertFalse(access.contains("releasePersistableUriPermission"))
    }

    private fun read(relativePath: String): String {
        val path = sequenceOf(
            Path.of("app/src/main/$relativePath"),
            Path.of("src/main/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
        return Files.readString(path)
    }
}
