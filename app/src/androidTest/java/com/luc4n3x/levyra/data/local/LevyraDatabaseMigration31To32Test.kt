package com.luc4n3x.levyra.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigration31To32Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate31To32KeepsPlaylistsAndCascadesSyncLinks() {
        helper.createDatabase(TEST_DB, 31).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) VALUES ('pl-1', 'Road trip', '', 100, 200)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 32, true, *LevyraDatabase.MIGRATIONS)
        migrated.execSQL("PRAGMA foreign_keys=ON")

        migrated.query("SELECT name, updatedAt FROM playlists WHERE id = 'pl-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Road trip", cursor.getString(0))
            assertEquals(200L, cursor.getLong(1))
        }
        migrated.execSQL(
            "INSERT INTO playlist_sync (playlistId, accountId, remotePlaylistId, localTitle, remoteTitle, videoIds, autoSync, synced, " +
                "localUpdatedAt, lastSyncedAt, lastError, unmatched, keptLocal) " +
                "VALUES ('pl-1', 'acc', 'PLremote', 'Road trip', 'Road trip', 'AAAAAAAAAAA', 1, 1, 200, 300, '', 0, 0)"
        )
        val duplicateRejected = try {
            migrated.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt) VALUES ('pl-2', 'Copy', '', 100, 200)"
            )
            migrated.execSQL(
                "INSERT INTO playlist_sync (playlistId, accountId, remotePlaylistId, localTitle, remoteTitle, videoIds, autoSync, synced, " +
                    "localUpdatedAt, lastSyncedAt, lastError, unmatched, keptLocal) " +
                    "VALUES ('pl-2', 'acc', 'PLremote', 'Copy', 'Copy', '', 1, 0, 0, 0, '', 0, 0)"
            )
            false
        } catch (_: SQLiteConstraintException) {
            true
        }
        assertTrue(duplicateRejected)

        migrated.execSQL("DELETE FROM playlists WHERE id = 'pl-1'")
        migrated.query("SELECT COUNT(*) FROM playlist_sync").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertFalse(cursor.getInt(0) > 0)
        }
    }

    private companion object {
        const val TEST_DB = "migration-31-32-test"
    }
}
