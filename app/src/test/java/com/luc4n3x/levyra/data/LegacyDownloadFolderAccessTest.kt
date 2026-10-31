package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDownloadFolderAccessTest {
    private val tree = "content://com.android.externalstorage.documents/tree/1234-5678%3AMusic"
    private val document = "$tree/document/1234-5678%3AMusic%2FArtist%2FSong.m4a"

    @Test
    fun aSafDownloadResolvesToTheTreeItWasSavedUnder() {
        assertEquals(tree, legacyDownloadTreeUri(document))
        assertEquals(tree, legacyDownloadTreeUri("  $document  "))
    }

    @Test
    fun nonTreeUrisNeverOfferFolderAccess() {
        assertNull(legacyDownloadTreeUri("content://media/external_primary/audio/media/42"))
        assertNull(legacyDownloadTreeUri("file:///data/user/0/pkg/files/offline_audio/a.m4a"))
        assertNull(legacyDownloadTreeUri("content://com.android.externalstorage.documents/document/primary%3AMusic%2Fa.m4a"))
        assertNull(legacyDownloadTreeUri(tree))
        assertNull(legacyDownloadTreeUri(""))
    }

    @Test
    fun onlyTheSameTreeIsAcceptedRegardlessOfEscapeCase() {
        assertTrue(isSameLegacyDownloadTree(tree, tree))
        assertTrue(isSameLegacyDownloadTree("content://com.android.externalstorage.documents/tree/1234-5678%3aMusic", tree))
        assertFalse(isSameLegacyDownloadTree("content://com.android.externalstorage.documents/tree/1234-5678%3A", tree))
        assertFalse(isSameLegacyDownloadTree("content://com.android.externalstorage.documents/tree/primary%3AMusic", tree))
        assertFalse(isSameLegacyDownloadTree("", tree))
    }

    @Test
    fun restoredAccessIsReadOnlyAndNeverADownloadDestination() {
        val access = read("data/DownloadFolderAccess.kt")
        val restore = access.substringAfter("fun restoreLegacyReadAccess").substringBefore("\n    }")
        val exporter = read("player/offline/OfflineAudioExporter.kt")

        assertTrue(restore.contains("takePersistableUriPermission(pickedTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)"))
        assertFalse(restore.contains("FLAG_GRANT_WRITE_URI_PERMISSION"))
        assertTrue(restore.indexOf("isSameLegacyDownloadTree") < restore.indexOf("takePersistableUriPermission"))
        assertFalse(exporter.contains("DownloadFolderAccess"))
        assertFalse(exporter.contains("destinationTreeUri"))
    }

    @Test
    fun theHealthScreenOffersFolderAccessInsteadOfAMissingSettingsPicker() {
        val screen = read("ui/downloadhealth/DownloadHealthScreen.kt")

        assertTrue(screen.contains("ActivityResultContracts.OpenDocumentTree()"))
        assertTrue(screen.contains("legacyDownloadTreeUri(result.download.uri)"))
        assertTrue(screen.contains("strings.offlineRestoreFolderAccess"))
    }

    private fun read(relativePath: String): String = Files.readString(
        sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    )
}
