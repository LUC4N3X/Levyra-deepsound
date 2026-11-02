package com.luc4n3x.levyra.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevyraDatabaseMigration27To28Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevyraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate27To28KeepsPlaylistsAndAddsEmptyPodcastTables() {
        helper.createDatabase(TEST_DB, 27).use { db ->
            db.execSQL(
                "INSERT INTO playlists (id, name, coverUrl, createdAt, updatedAt, hidden, coverMode) " +
                    "VALUES ('p28', 'Before podcasts', '', 100, 200, 0, 'AUTO')"
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 28, true, *LevyraDatabase.MIGRATIONS)

        migrated.query("SELECT name FROM playlists WHERE id = 'p28'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Before podcasts", cursor.getString(0))
        }
        listOf("podcast_subscriptions", "podcast_episode_progress").forEach { table ->
            migrated.query("SELECT COUNT(*) FROM $table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun historyKeepsNewestEpisodesAndMarkingPlayedResetsPosition() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java).build()
        try {
            val dao = database.podcastLibraryDao()
            (1..5).forEach { index ->
                dao.saveProgress(PodcastEpisodeProgressEntity("e$index", "{}", index * 1_000L, 60_000L, false, index.toLong()))
            }
            dao.trimHistory(limit = 3)
            assertEquals(listOf("e5", "e4", "e3"), dao.history().first().map { it.episodeId })

            dao.setCompleted("e5", true)
            val played = dao.history().first().first { it.episodeId == "e5" }
            assertTrue(played.completed)
            assertEquals(0L, played.positionMs)

            dao.subscribe(PodcastSubscriptionEntity("MPSPshow0001", "Show", "Host", "", 1L))
            assertEquals(listOf("MPSPshow0001"), dao.subscriptions().first().map { it.browseId })
            dao.unsubscribe("MPSPshow0001")
            assertTrue(dao.subscriptions().first().isEmpty())
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-27-28-test"
    }
}
