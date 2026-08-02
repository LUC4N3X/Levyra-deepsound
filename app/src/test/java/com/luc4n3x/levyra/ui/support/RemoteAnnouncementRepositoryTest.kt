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
        laterAction = "Maybe later",
        settingsTitle = "Support the project",
        settingsSubtitle = "Leave a star so more people can discover it."
    )

    @Test
    fun packagedCatalogIsValidCompleteAndLocalized() {
        val catalog = loadPackagedCatalog()
        assertEquals(2, catalog.schemaVersion)
        val announcement = catalog.announcements.single { it.id == BUILT_IN_SUPPORT_ANNOUNCEMENT_ID }
        val expectedLanguages = LevyraLanguageCatalog.languages.map { it.code }.toSet()

        assertEquals(expectedLanguages, announcement.translations.keys)
        assertEquals(AnnouncementStyle.OPEN_SOURCE, announcement.style)
        assertEquals(2_031_800, announcement.minimumVersionCode)
        assertEquals(LEVYRA_REPOSITORY_URL, announcement.actionUrl)
        announcement.translations.forEach { (code, copy) ->
            listOf(
                copy.badge,
                copy.title,
                copy.body,
                copy.starAction,
                copy.laterAction,
                copy.settingsTitle,
                copy.settingsSubtitle
            ).forEach { value ->
                assertTrue("Blank announcement copy for $code", value.isNotBlank())
            }
            assertTrue("Announcement body is too short for $code", copy.body.length >= 20)
        }

        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli()
        val italian = RemoteAnnouncementRules.select(catalog, "it-IT", 2_031_800, emptySet(), now)
        val arabic = RemoteAnnouncementRules.select(catalog, "ar-SA", 2_031_800, emptySet(), now)
        val hebrew = RemoteAnnouncementRules.select(catalog, "he-IL", 2_031_800, emptySet(), now)
        val unknown = RemoteAnnouncementRules.select(catalog, "xx-YY", 2_031_800, emptySet(), now)

        assertEquals("Ti piace Levyra?", italian?.copy?.title)
        assertEquals("Più tardi", italian?.copy?.laterAction)
        assertTrue(arabic?.copy?.body.orEmpty().contains("GitHub"))
        assertTrue(hebrew?.copy?.body.orEmpty().contains("GitHub"))
        assertEquals(announcement.translations.getValue("en"), unknown?.copy)
    }

    @Test
    fun selectorHonorsPriorityVersionTimeAndDismissal() {
        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli()
        val lowerPriority = announcement(id = "notice-low", priority = 10)
        val higherPriority = announcement(id = "notice-high", priority = 90)
        val future = announcement(
            id = "notice-future",
            priority = 100,
            startAtMs = Instant.parse("2026-08-05T00:00:00Z").toEpochMilli()
        )
        val catalog = RemoteAnnouncementCatalog(1, listOf(lowerPriority, future, higherPriority))

        val selected = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "it-IT",
            versionCode = 2_031_800,
            dismissedIds = emptySet(),
            nowMs = now
        )
        assertEquals("notice-high", selected?.id)
        assertEquals("A useful announcement", selected?.copy?.title)

        val afterDismissal = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "en",
            versionCode = 2_031_800,
            dismissedIds = setOf("notice-high"),
            nowMs = now
        )
        assertEquals("notice-low", afterDismissal?.id)
    }

    @Test
    fun selectorRejectsUnsupportedVersionAndExpiredContent() {
        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli()
        val versionLocked = announcement(
            id = "version-locked",
            priority = 100,
            minimumVersionCode = 3_000_000
        )
        val expired = announcement(
            id = "expired",
            priority = 90,
            endAtMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()
        )
        val catalog = RemoteAnnouncementCatalog(1, listOf(versionLocked, expired))

        assertNull(
            RemoteAnnouncementRules.select(
                catalog = catalog,
                languageCode = "en",
                versionCode = 2_031_800,
                dismissedIds = emptySet(),
                nowMs = now
            )
        )
    }

    @Test
    fun promptPolicyWaitsForUsagePositiveListeningAndSnoozeExpiry() {
        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli()

        assertFalse(
            RemoteAnnouncementPromptPolicy.isEligible(
                launchCount = 2,
                hasPositiveListeningMoment = true,
                snoozedUntilMs = 0L,
                nowMs = now
            )
        )
        assertFalse(
            RemoteAnnouncementPromptPolicy.isEligible(
                launchCount = 3,
                hasPositiveListeningMoment = false,
                snoozedUntilMs = 0L,
                nowMs = now
            )
        )
        assertFalse(
            RemoteAnnouncementPromptPolicy.isEligible(
                launchCount = 3,
                hasPositiveListeningMoment = true,
                snoozedUntilMs = now + 1L,
                nowMs = now
            )
        )
        assertTrue(
            RemoteAnnouncementPromptPolicy.isEligible(
                launchCount = 3,
                hasPositiveListeningMoment = true,
                snoozedUntilMs = now,
                nowMs = now
            )
        )
    }

    @Test
    fun positiveMomentCanComeFromHistoryOrCurrentListeningTime() {
        assertFalse(RemoteAnnouncementPromptPolicy.hasPositiveListeningMoment(2, 89_999L))
        assertTrue(RemoteAnnouncementPromptPolicy.hasPositiveListeningMoment(3, 0L))
        assertTrue(RemoteAnnouncementPromptPolicy.hasPositiveListeningMoment(0, 90_000L))
    }

    @Test
    fun actionLinksAreRestrictedToOfficialGithubPaths() {
        assertTrue(RemoteAnnouncementRules.isSafeActionUrl(LEVYRA_REPOSITORY_URL))
        assertTrue(RemoteAnnouncementRules.isSafeActionUrl("https://github.com/LUC4N3X/Levyra-deepsound/releases"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("http://github.com/LUC4N3X/Levyra-deepsound"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("https://example.com/LUC4N3X/Levyra-deepsound"))
        assertFalse(RemoteAnnouncementRules.isSafeActionUrl("https://github.com/another-owner/project"))
    }

    private fun loadPackagedCatalog(): RemoteAnnouncementCatalog {
        val candidates = sequenceOf(
            Path.of("app/src/main/assets").resolve(BUNDLED_ANNOUNCEMENTS_ASSET),
            Path.of("src/main/assets").resolve(BUNDLED_ANNOUNCEMENTS_ASSET)
        )
        val config = candidates.firstOrNull(Files::exists)
            ?: error("Packaged announcement catalog not found")
        val catalog = RemoteAnnouncementParser.parse(Files.readString(config))
        assertNotNull(catalog)
        return requireNotNull(catalog)
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
