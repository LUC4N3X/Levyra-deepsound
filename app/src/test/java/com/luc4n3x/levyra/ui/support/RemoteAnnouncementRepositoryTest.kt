package com.luc4n3x.levyra.ui.support

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAnnouncementRepositoryTest {
    private val english = OpenSourceSupportCopy(
        badge = "NOTICE",
        title = "A useful announcement",
        body = "This is a complete announcement body that is long enough for the remote configuration rules.",
        starAction = "Open",
        continueAction = "Continue"
    )

    @Test
    fun checkedInCatalogIsValidAndCoversEverySupportedLanguage() {
        val config = sequenceOf(
            Path.of("config/announcements.json"),
            Path.of("../config/announcements.json")
        ).firstOrNull(Files::exists) ?: error("Remote announcement config not found")
        val catalog = RemoteAnnouncementParser.parse(Files.readString(config))
        assertNotNull(catalog)
        val announcement = catalog!!.announcements.single { it.id == BUILT_IN_SUPPORT_ANNOUNCEMENT_ID }
        val expectedLanguages = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expectedLanguages, announcement.translations.keys)
        assertEquals(AnnouncementStyle.OPEN_SOURCE, announcement.style)
        assertEquals(LEVYRA_REPOSITORY_URL, announcement.actionUrl)
    }

    @Test
    fun selectorHonorsPriorityVersionTimeAndDismissal() {
        val now = Instant.parse("2026-07-29T12:00:00Z").toEpochMilli()
        val lowerPriority = announcement(id = "notice-low", priority = 10)
        val higherPriority = announcement(id = "notice-high", priority = 90)
        val future = announcement(
            id = "notice-future",
            priority = 100,
            startAtMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()
        )
        val catalog = RemoteAnnouncementCatalog(1, listOf(lowerPriority, future, higherPriority))

        val selected = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "it-IT",
            versionCode = 2_031_700,
            dismissedIds = emptySet(),
            nowMs = now
        )
        assertEquals("notice-high", selected?.id)
        assertEquals("A useful announcement", selected?.copy?.title)

        val afterDismissal = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "en",
            versionCode = 2_031_700,
            dismissedIds = setOf("notice-high"),
            nowMs = now
        )
        assertEquals("notice-low", afterDismissal?.id)
    }

    @Test
    fun selectorRejectsUnsupportedVersionAndExpiredContent() {
        val now = Instant.parse("2026-07-29T12:00:00Z").toEpochMilli()
        val versionLocked = announcement(
            id = "version-locked",
            priority = 100,
            minimumVersionCode = 3_000_000
        )
        val expired = announcement(
            id = "expired",
            priority = 90,
            endAtMs = Instant.parse("2026-07-01T00:00:00Z").toEpochMilli()
        )
        val catalog = RemoteAnnouncementCatalog(1, listOf(versionLocked, expired))

        assertNull(
            RemoteAnnouncementRules.select(
                catalog = catalog,
                languageCode = "en",
                versionCode = 2_031_700,
                dismissedIds = emptySet(),
                nowMs = now
            )
        )
    }

    @Test
    fun actionLinksAreRestrictedToOfficialGithubPaths() {
        assertTrue(RemoteAnnouncementRules.isSafeActionUrl(LEVYRA_REPOSITORY_URL))
        assertTrue(RemoteAnnouncementRules.isSafeActionUrl("https://github.com/LUC4N3X/Levyra-deepsound/releases"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("http://github.com/LUC4N3X/Levyra-deepsound"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("https://example.com/LUC4N3X/Levyra-deepsound"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("https://github.com/another-owner/project"))
    }

    private fun announcement(
        id: String,
        priority: Int,
        minimumVersionCode: Int = 1,
        startAtMs: Long? = null,
        endAtMs: Long? = null
    ): RemoteAnnouncement = RemoteAnnouncement(
        id = id,
        enabled = true,
        priority = priority,
        style = AnnouncementStyle.INFO,
        minimumVersionCode = minimumVersionCode,
        maximumVersionCode = null,
        startAtMs = startAtMs,
        endAtMs = endAtMs,
        actionUrl = LEVYRA_REPOSITORY_URL,
        translations = mapOf("en" to english)
    )
}
