package com.luc4n3x.levyra.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigration30To31Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate30To31KeepsDownloadsAndAddsEmptyArtwork() {
        helper.createDatabase(TEST_DB, 30).use { db ->
            db.execSQL(
                "INSERT INTO downloaded_tracks (trackId, title, artist, album, durationMs, fileName, uri, mimeType, " +
                    "embeddedMetadata, downloadPreset, downloadQuality, savedAt, ownership) " +
                    "VALUES ('track-1', 'Song', 'Artist', 'Album', 180000, 'Artist - Song.flac', " +
                    "'content://media/external/audio/media/41', 'audio/flac', 0, 'lossless', 'flac', 500, 'MANUAL')"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 31, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT trackId, uri, mimeType, ownership, artworkUrl FROM downloaded_tracks").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("track-1", cursor.getString(0))
            assertEquals("content://media/external/audio/media/41", cursor.getString(1))
            assertEquals("audio/flac", cursor.getString(2))
            assertEquals("MANUAL", cursor.getString(3))
            assertEquals("", cursor.getString(4))
        }
    }

    private companion object {
        const val TEST_DB = "migration-30-31-test"
    }
}
