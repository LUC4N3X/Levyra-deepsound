package com.luc4n3x.levyra.data.local

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
    fun migrate31To32CreatesPlaylistSyncTableWithCascadeDelete() {
        helper.createDatabase(TEST_DB, 31).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, coverMode, createdAt) " +
                    "VALUES ('pl-1', 'Favorites Mix', '', 'AUTOMATIC', 1000)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 32, true, *LevyraDatabase.MIGRATIONS)
        migrated.execSQL("PRAGMA foreign_keys = ON")

        migrated.query("SELECT id, name FROM playlists WHERE id = 'pl-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("pl-1", cursor.getString(0))
            assertEquals("Favorites Mix", cursor.getString(1))
        }

        migrated.execSQL(
            "INSERT INTO playlist_sync (playlistId, accountId, remotePlaylistId, remoteTitle, syncedVideoIds, autoSync, lastSyncedAt, lastSyncAttemptAt, lastSyncError, lastUnmatchedCount) " +
                "VALUES ('pl-1', 'acct-hash', 'PL_REMOTE', 'Favorites Mix', 'vid1,vid2', 1, 2000, 2000, '', 0)"
        )

        migrated.query("SELECT remotePlaylistId, autoSync FROM playlist_sync WHERE playlistId = 'pl-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("PL_REMOTE", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }

        migrated.execSQL("DELETE FROM playlists WHERE id = 'pl-1'")
        migrated.query("SELECT * FROM playlist_sync WHERE playlistId = 'pl-1'").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
    }

    private companion object {
        const val TEST_DB = "migration-31-32-test"
    }
}
