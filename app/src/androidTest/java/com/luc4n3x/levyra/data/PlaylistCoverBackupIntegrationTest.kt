package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.data.local.LEVYRA_DATABASE_VERSION
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistCoverBackupIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: LevyraDatabase
    private lateinit var backupManager: LevyraBackupManager
    private lateinit var coverStore: PlaylistCoverStore
    private lateinit var backupFile: File

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = LevyraDatabase.get(context)
        database.clearAllTables()
        backupManager = LevyraBackupManager(context)
        coverStore = PlaylistCoverStore(context)
        backupFile = File(context.cacheDir, "playlist-cover-${System.nanoTime()}.levyra")
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
        backupFile.delete()
    }

    @Test
    fun newBackupRestoresCustomCoverIntoOwnedStorage() = runBlocking {
        val coverReference = coverStore.restore(PLAYLIST_ID, validJpeg())
        database.playlistDao().upsertPlaylist(customPlaylist(coverReference))
        backupManager.exportTo(Uri.fromFile(backupFile))

        database.playlistDao().clearAll()
        coverStore.delete(coverReference)
        backupManager.restoreFrom(Uri.fromFile(backupFile))

        val restored = requireNotNull(database.playlistDao().playlist(PLAYLIST_ID))
        assertEquals(PlaylistCoverMode.CUSTOM.name, restored.coverMode)
        assertNotNull(coverStore.readBackup(restored.coverUrl))
    }

    @Test
    fun legacyBackupWithoutCoverModeRestoresAsAutomatic() = runBlocking {
        writeLegacyBackup(
            JSONObject()
                .put("settings", JSONObject())
                .put("favorites", JSONArray())
                .put("followedArtists", JSONArray())
                .put(
                    "playlists",
                    JSONArray().put(
                        JSONObject()
                            .put("id", PLAYLIST_ID)
                            .put("name", "Legacy playlist")
                            .put("coverUrl", "https://example.test/legacy.jpg")
                            .put("createdAt", 10L)
                            .put("updatedAt", 10L)
                            .put("tracks", JSONArray())
                    )
                )
                .put("history", JSONArray())
                .put("queue", JSONObject().put("items", JSONArray()).put("state", JSONObject.NULL))
        )

        backupManager.restoreFrom(Uri.fromFile(backupFile))

        val restored = requireNotNull(database.playlistDao().playlist(PLAYLIST_ID))
        assertEquals(PlaylistCoverMode.AUTO.name, restored.coverMode)
        assertEquals("https://example.test/legacy.jpg", restored.coverUrl)
    }

    @Test
    fun malformedCustomCoverIsRejectedBeforeExistingDataChanges() = runBlocking {
        database.playlistDao().upsertPlaylist(
            PlaylistEntity("sentinel", "Keep me", "", 1L, 1L)
        )
        val coverEntry = playlistCoverBackupEntry(PLAYLIST_ID)
        val sections = baseVaultSections().apply {
            put(
                LevyraBackupManager.PLAYLISTS_ENTRY,
                JSONArray().put(
                    JSONObject()
                        .put("id", PLAYLIST_ID)
                        .put("name", "Malformed cover")
                        .put("coverUrl", "")
                        .put("coverMode", PlaylistCoverMode.CUSTOM.name)
                        .put("coverEntry", coverEntry)
                        .put("createdAt", 1L)
                        .put("updatedAt", 1L)
                        .put("tracks", JSONArray())
                ).toString().toByteArray()
            )
            put(coverEntry, "not-a-jpeg".toByteArray())
        }
        writeVault(sections)

        var rejected = false
        try {
            backupManager.restoreFrom(Uri.fromFile(backupFile))
        } catch (_: IOException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals("Keep me", database.playlistDao().playlist("sentinel")?.name)
        assertTrue(database.playlistDao().playlist(PLAYLIST_ID) == null)
    }

    private fun customPlaylist(reference: String) = PlaylistEntity(
        id = PLAYLIST_ID,
        name = "Custom cover",
        coverUrl = reference,
        createdAt = 1L,
        updatedAt = 1L,
        coverMode = PlaylistCoverMode.CUSTOM.name
    )

    private fun validJpeg(): ByteArray = ByteArrayOutputStream().use { output ->
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).run {
            eraseColor(0xFF205070.toInt())
            compress(Bitmap.CompressFormat.JPEG, 90, output)
            recycle()
        }
        output.toByteArray()
    }

    private fun writeLegacyBackup(payload: JSONObject) {
        val payloadBytes = payload.toString().toByteArray()
        val manifest = JSONObject()
            .put("schemaVersion", LevyraBackupManager.LEGACY_SCHEMA_VERSION)
            .put("payloadSha256", sha256(payloadBytes))
        ZipOutputStream(backupFile.outputStream()).use { zip ->
            zip.writeEntry(LevyraBackupManager.LEGACY_PAYLOAD_ENTRY, payloadBytes)
            zip.writeEntry(LevyraBackupManager.MANIFEST_ENTRY, manifest.toString().toByteArray())
        }
    }

    private fun writeVault(sections: Map<String, ByteArray>) {
        val manifestSections = JSONObject()
        sections.forEach { (name, bytes) ->
            manifestSections.put(
                name,
                JSONObject().put("sha256", sha256(bytes)).put("bytes", bytes.size)
            )
        }
        val manifest = JSONObject()
            .put("formatVersion", LevyraBackupManager.FORMAT_VERSION)
            .put("appVersion", "test")
            .put("platform", LevyraBackupManager.PLATFORM)
            .put("databaseVersion", LEVYRA_DATABASE_VERSION)
            .put("createdAt", 1L)
            .put("sections", manifestSections)
        ZipOutputStream(backupFile.outputStream()).use { zip ->
            sections.forEach { (name, bytes) -> zip.writeEntry(name, bytes) }
            zip.writeEntry(LevyraBackupManager.MANIFEST_ENTRY, manifest.toString().toByteArray())
        }
    }

    private fun baseVaultSections(): MutableMap<String, ByteArray> = linkedMapOf(
        LevyraBackupManager.SETTINGS_ENTRY to "{}".toByteArray(),
        LevyraBackupManager.FAVORITES_ENTRY to "[]".toByteArray(),
        LevyraBackupManager.FOLLOWED_ARTISTS_ENTRY to "[]".toByteArray(),
        LevyraBackupManager.PLAYLISTS_ENTRY to "[]".toByteArray(),
        LevyraBackupManager.HISTORY_ENTRY to "[]".toByteArray(),
        LevyraBackupManager.DOWNLOADS_ENTRY to "[]".toByteArray(),
        LevyraBackupManager.QUEUE_ENTRY to "{\"items\":[],\"state\":null}".toByteArray(),
        LevyraBackupManager.ORGANIZATION_ENTRY to "{\"tags\":[],\"excludedArtists\":[]}".toByteArray()
    )

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val PLAYLIST_ID = "playlist-cover-backup"
    }
}
