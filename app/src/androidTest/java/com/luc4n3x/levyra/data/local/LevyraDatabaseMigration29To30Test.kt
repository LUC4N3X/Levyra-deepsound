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
class LevyraDatabaseMigration29To30Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate29To30KeepsMotionArtworkAndAddsEmptyPoster() {
        helper.createDatabase(TEST_DB, 29).use { db ->
            db.execSQL(
                "INSERT INTO motion_artwork (identityKey, provider, url, mimeType, width, height, confidence, " +
                    "expiresAt, lastVerifiedAt, configEpoch, negative) " +
                    "VALUES ('song|artist', 'apple', 'https://example.com/v.m3u8', 'application/x-mpegURL', 1080, 1440, 90, 500, 100, 7, 0)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 30, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT url, width, posterUrl FROM motion_artwork WHERE identityKey = 'song|artist'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/v.m3u8", cursor.getString(0))
            assertEquals(1080, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }
    }

    private companion object {
        const val TEST_DB = "migration-29-30-test"
    }
}
