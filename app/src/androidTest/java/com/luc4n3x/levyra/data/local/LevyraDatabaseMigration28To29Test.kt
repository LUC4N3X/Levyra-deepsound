package com.luc4n3x.levyra.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.SavedPlaylistsStore
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SavedPlaylist
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigration28To29Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate28To29KeepsPlaylistsAndAddsEmptySavedPlaylists() {
        helper.createDatabase(TEST_DB, 28).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden, coverMode) " +
                    "VALUES ('p29', 'Before saved playlists', '', 100, 200, 0, 'AUTO')"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 29, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p29'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Before saved playlists", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM saved_playlists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun savedPlaylistPersistsAcrossDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(FILE_DB)
        val hit = PlaylistHit("Essentials", "Levyra", "", playlistId = "RDCLAK5uy_test")
        Room.databaseBuilder(context, LevyraDatabase::class.java, FILE_DB).build().let { database ->
            SavedPlaylistsStore(database.savedPlaylistsDao()).toggle(SavedPlaylist(hit))
            database.close()
        }
        val reopened = Room.databaseBuilder(context, LevyraDatabase::class.java, FILE_DB).build()
        try {
            assertEquals(listOf("youtube:RDCLAK5uy_test"), SavedPlaylistsStore(reopened.savedPlaylistsDao()).load().map { it.key })
        } finally {
            reopened.close()
            context.deleteDatabase(FILE_DB)
        }
    }

    private companion object {
        const val TEST_DB = "migration-28-29-test"
        const val FILE_DB = "saved-playlists-reopen-test"
    }
}
