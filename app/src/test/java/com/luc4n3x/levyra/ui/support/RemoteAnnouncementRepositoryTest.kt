package com.luc4n3x.levyra.ui.support

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.io.ByteArrayInputStream
import java.io.InputStream
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
    fun fdroidBuildDisablesRemoteAnnouncementsAndSupportCard() {
        val buildFile = sequenceOf(
            Path.of("app/build.gradle.kts"),
            Path.of("build.gradle.kts")
        ).firstOrNull(Files::exists) ?: error("app/build.gradle.kts not found")
        val repositorySource = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/support/RemoteAnnouncementRepository.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/support/RemoteAnnouncementRepository.kt")
        ).firstOrNull(Files::exists) ?: error("RemoteAnnouncementRepository.kt not found")
        val promptSource = repositorySource.resolveSibling("OpenSourceSupportPrompt.kt")

        assertTrue(Files.readString(buildFile).contains("REMOTE_ANNOUNCEMENTS_ENABLED\", (!isFdroidBuild).toString()"))
        assertTrue(Files.readString(repositorySource).contains("!BuildConfig.REMOTE_ANNOUNCEMENTS_ENABLED"))
        assertTrue(Files.readString(promptSource).contains("!BuildConfig.REMOTE_ANNOUNCEMENTS_ENABLED"))
    }

    @Test
    fun packagedCatalogIsValidCompleteAndLocalized() {
        val catalog = loadPackagedCatalog()
        assertEquals(2, catalog.schemaVersion)
        val announcement = catalog.announcements.single { it.id == BUILT_IN_SUPPORT_ANNOUNCEMENT_ID }
        val expectedLanguages = LevyraLanguageCatalog.languages.map { it.code }.toSet()

        assertEquals(expectedLanguages, announcement.translations.keys)
        assertEquals(AnnouncementStyle.OPEN_SOURCE, announcement.style)
        assertFalse(announcement.enabled)
        assertNull(RemoteAnnouncementRules.select(catalog, "en", Int.MAX_VALUE, emptySet(), Long.MAX_VALUE))
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

        val italian = RemoteAnnouncementRules.presentationFor(announcement, "it-IT")
        val arabic = RemoteAnnouncementRules.presentationFor(announcement, "ar-SA")
        val hebrew = RemoteAnnouncementRules.presentationFor(announcement, "he-IL")
        val unknown = RemoteAnnouncementRules.presentationFor(announcement, "xx-YY")

        assertEquals("Ti piace Levyra?", italian?.copy?.title)
        assertEquals("Più tardi", italian?.copy?.laterAction)
        assertTrue(arabic?.copy?.body.orEmpty().contains("GitHub"))
        assertTrue(hebrew?.copy?.body.orEmpty().contains("GitHub"))
        assertEquals(announcement.translations.getValue("en"), unknown?.copy)
    }

    @Test
    fun selectorHonorsPriorityVersionTimeAndCompletion() {
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
            completedIds = emptySet(),
            nowMs = now
        )
        assertEquals("notice-high", selected?.id)
        assertEquals("A useful announcement", selected?.copy?.title)

        val afterCompletion = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "en",
            versionCode = 2_031_800,
            completedIds = setOf("notice-high"),
            nowMs = now
        )
        assertEquals("notice-low", afterCompletion?.id)
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
                completedIds = emptySet(),
                nowMs = now
            )
        )
    }

    @Test
    fun selectorContinuesPastAnIneligibleHigherPriorityCampaign() {
        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli()
        val support = announcement(
            id = "support-snoozed",
            priority = 100,
            style = AnnouncementStyle.OPEN_SOURCE
        )
        val notice = announcement(
            id = "notice-ready",
            priority = 50,
            style = AnnouncementStyle.INFO
        )
        val catalog = RemoteAnnouncementCatalog(2, listOf(support, notice))

        val selected = RemoteAnnouncementRules.select(
            catalog = catalog,
            languageCode = "en",
            versionCode = 2_031_800,
            completedIds = emptySet(),
            nowMs = now,
            isAnnouncementEligible = { it.id != "support-snoozed" }
        )

        assertEquals("notice-ready", selected?.id)
    }

    @Test
    fun parserUsesTitleForActionlessSettingsFallback() {
        val raw = """
            {
              "schemaVersion": 2,
              "announcements": [
                {
                  "id": "service-notice",
                  "enabled": true,
                  "priority": 10,
                  "style": "info",
                  "minimumVersionCode": 1,
                  "translations": {
                    "en": {
                      "badge": "NOTICE",
                      "title": "Service notice",
                      "body": "This informational notice has enough text to satisfy validation.",
                      "dismiss": "Later"
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val copy = RemoteAnnouncementParser.parse(raw)
            ?.announcements
            ?.single()
            ?.translations
            ?.get("en")

        assertNotNull(copy)
        assertEquals("Service notice", copy?.settingsTitle)
    }

    @Test
    fun boundedPayloadReaderAcceptsContentWithinLimit() {
        val raw = """{"schemaVersion":2,"announcements":[]}"""
        val bytes = raw.toByteArray()

        val decoded = readBoundedAnnouncementPayload(
            input = ByteArrayInputStream(bytes),
            declaredLength = bytes.size.toLong(),
            maxBytes = bytes.size
        )

        assertEquals(raw, decoded)
    }

    @Test
    fun boundedPayloadReaderRejectsOversizedDeclaredContentWithoutReading() {
        val unreadable = object : InputStream() {
            override fun read(): Int = error("Oversized payload must be rejected before reading")
        }

        assertNull(
            readBoundedAnnouncementPayload(
                input = unreadable,
                declaredLength = 65L,
                maxBytes = 64
            )
        )
    }

    @Test
    fun boundedPayloadReaderRejectsOversizedContentWithUnknownLength() {
        val bytes = ByteArray(65) { 'x'.code.toByte() }

        assertNull(
            readBoundedAnnouncementPayload(
                input = ByteArrayInputStream(bytes),
                declaredLength = -1L,
                maxBytes = 64
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
        endAtMs: Long? = null,
        style: AnnouncementStyle = AnnouncementStyle.INFO
    ): RemoteAnnouncement = RemoteAnnouncement(
        id = id,
        enabled = true,
        priority = priority,
        style = style,
        minimumVersionCode = minimumVersionCode,
        maximumVersionCode = null,
        startAtMs = startAtMs,
        endAtMs = endAtMs,
        actionUrl = LEVYRA_REPOSITORY_URL,
        translations = mapOf("en" to english)
    )
}
