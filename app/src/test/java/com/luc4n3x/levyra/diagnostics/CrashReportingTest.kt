package com.luc4n3x.levyra.diagnostics

import com.luc4n3x.levyra.player.redactDiagnosticText
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashReportingTest {
    @get:Rule val temporary = TemporaryFolder()
    private val environment = CrashEnvironment("3.0.7-debug", "3000700, debug", "9", 28, "Example", "Phone")

    private fun report(id: String = "1700000000000-1", error: Throwable = IllegalStateException("failure")) =
        createCrashReport(id, 1_700_000_000_000, Thread("test-worker"), error, environment,
            CrashContext("Player", "Default", "state=READY"), ::redactDiagnosticText)

    @Test fun reportSurvivesAStoreRestartAndIsOnlyOfferedOnce() {
        val directory = temporary.newFolder()
        val expected = report()
        CrashReportStore(directory).save(expected)
        val restarted = CrashReportStore(directory)
        assertEquals(expected, restarted.claimPending())
        assertNull(restarted.claimPending())
        assertEquals(listOf(expected), restarted.reports())
    }

    @Test fun retentionIsBoundedAndReportsCanBeDeleted() {
        val directory = temporary.newFolder()
        val storage = CrashReportStore(directory)
        repeat(9) { storage.save(report("${1_700_000_000_000 + it}-1")) }
        assertEquals(5, storage.reports().size)
        assertEquals("1700000000008-1", storage.reports().first().id)
        assertTrue(storage.delete(storage.reports().first().id))
        assertEquals(4, storage.reports().size)
        assertNull(storage.claimPending())
    }

    @Test fun malformedOrUntrustedReportIdsCannotEscapeTheStore() {
        val directory = temporary.newFolder()
        val storage = CrashReportStore(directory)
        assertNull(storage.read("../../private"))
        assertFalse(storage.delete("../private"))
        File(directory, "pending").writeText("../../private")
        assertNull(storage.claimPending())
        assertFalse(File(directory, "pending").exists())
        File(directory, "1700000000000-2.txt").writeText("invalid")
        assertTrue(storage.reports().isEmpty())
    }

    @Test fun stackTraceKeepsCausesSuppressedErrorsAndCircularReferences() {
        val first = IllegalStateException("outer")
        val second = IllegalArgumentException("inner", first)
        first.initCause(second)
        first.addSuppressed(UnsupportedOperationException("suppressed"))
        val body = report(error = first).body
        assertTrue(body.contains("Caused by: java.lang.IllegalArgumentException"))
        assertTrue(body.contains("Suppressed: java.lang.UnsupportedOperationException"))
        assertTrue(body.contains("CIRCULAR REFERENCE"))
        assertTrue(body.contains("CrashReportingTest"))
        assertTrue(body.contains("test-worker"))
        assertTrue(body.contains("API 28"))
    }

    @Test fun sensitiveTextIsRemovedWithoutFlatteningStackFrames() {
        val value = """
            Authorization: Bearer secret-bearer
            Cookie: user=alice; token=test-cookie; arbitrary_name=private-cookie-value
            {"access_token":"quoted secret with spaces"}
            failed https://example.test/private?password=example-url
            /storage/emulated/0/Music/private-song.mp3
            C:\Users\Alice\private.txt
            alice@example.test 192.168.1.20
            eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature
                at com.example.Player.next(Player.kt:42)
        """.trimIndent()
        val safe = redactDiagnosticText(value)
        listOf("secret-bearer", "test-cookie", "private-cookie-value", "quoted secret", "example-url", "private-song", "Alice", "alice@example", "192.168.1.20", "eyJhbGci").forEach {
            assertFalse("Sensitive data survived: $it", safe.contains(it))
        }
        assertTrue(safe.contains("at com.example.Player.next(Player.kt:42)"))
        assertTrue(safe.contains('\n'))
    }

    @Test fun githubDraftPrefillsTheStandardBodyWithCompleteLogsAndWarning() {
        val error = IllegalStateException("failure").apply {
            stackTrace = arrayOf(StackTraceElement("CrashReportingTest", "next", "Player.kt", 42))
        }
        val draft = crashIssueDraft(report(error = error))
        assertFalse(draft.needsPaste)
        val uri = URI(draft.url)
        assertEquals("github.com", uri.host)
        assertEquals("/LUC4N3X/Levyra-deepsound/issues/new", uri.path)
        val fields = uri.rawQuery.split('&').associate {
            val pair = it.split('=', limit = 2)
            pair[0] to URLDecoder.decode(pair[1], "UTF-8")
        }
        assertEquals("", fields["template"])
        assertTrue(fields["title"]!!.startsWith("[Crash] IllegalStateException on Android 9"))
        assertTrue(fields["body"]!!.contains("CrashReportingTest"))
        assertTrue(fields["body"]!!.contains("DO NOT DELETE THESE LOGS"))
        assertTrue(fields["body"]!!.contains("## Steps to reproduce"))
        assertEquals(draft.clipboard, fields["body"])
        assertFalse(fields.containsKey("logs"))
        assertFalse(fields.containsKey("preflight"))
        assertFalse(fields.containsKey("labels"))
    }

    @Test fun largeReportFallsBackToClipboardWithoutLosingTheTrace() {
        val original = report().copy(body = report().body + "\n" + "frame\n".repeat(5_000))
        val draft = crashIssueDraft(original)
        assertTrue(draft.needsPaste)
        assertTrue(draft.clipboard.endsWith(original.body))
        assertTrue(draft.url.length <= 6_000)
        val decoded = URLDecoder.decode(draft.url, "UTF-8")
        assertTrue(decoded.contains("complete copied report"))
        assertTrue(decoded.contains("IllegalStateException"))
        assertTrue(decoded.contains("DO NOT DELETE"))
    }

    @Test fun italianReportKeepsWarningAndUnicodeDiagnosticsInBoundedDraft() {
        val original = report().copy(
            body = "## Logs or error details\nIllegalStateException: inizio\n" + "音楽 🎵\n".repeat(5_000) + "Caused by: fine",
            presentation = CrashPresentation(language = "it")
        )
        val draft = crashIssueDraft(original)
        val decoded = URLDecoder.decode(URI(draft.url).rawQuery.substringAfter("&body="), "UTF-8")
        assertTrue(draft.needsPaste)
        assertTrue(draft.url.length <= 6_000)
        assertTrue(decoded.contains("NON CANCELLARLI"))
        assertTrue(decoded.contains("IllegalStateException: inizio"))
        assertTrue(decoded.contains("Caused by: fine"))
        assertFalse(decoded.contains('?'))
        assertTrue(draft.clipboard.endsWith(original.body))

        for (sample in listOf("🎵".repeat(1_000), "a" + "🎵".repeat(1_000), "🎵".repeat(1_000) + "a")) {
            val emojiDraft = crashIssueDraft(original.copy(body = "## Logs or error details\n$sample"))
            val emojiDecoded = URLDecoder.decode(URI(emojiDraft.url).rawQuery.substringAfter("&body="), "UTF-8")
            assertTrue(emojiDraft.needsPaste)
            assertTrue(emojiDraft.url.length <= 6_000)
            assertFalse(emojiDecoded.contains('?'))
            val marker = "\n[... truncated; full report copied ...]\n"
            assertTrue(emojiDecoded.contains(marker))
            val head = emojiDecoded.substringAfter("## Logs or error details\n````text\n").substringBefore(marker)
            val tail = emojiDecoded.substringAfter(marker).substringBefore("\n````\n")
            assertFalse(head.lastOrNull()?.isHighSurrogate() == true)
            assertFalse(tail.firstOrNull()?.isLowSurrogate() == true)
        }
    }

    @Test fun captureFailuresStillDelegateAndTerminate() {
        val calls = mutableListOf<String>()
        val handler = FatalCrashHandler(
            Thread.UncaughtExceptionHandler { _, _ -> calls += "previous" },
            capture = { _, _ -> calls += "save"; throw IllegalStateException("disk unavailable") },
            terminate = { calls += "terminate" }
        )
        handler.uncaughtException(Thread.currentThread(), IllegalStateException("fatal"))
        assertEquals(listOf("save", "previous", "terminate"), calls)
    }

    @Test fun concurrentFatalErrorsOnlyPersistOneReportBeforeTermination() {
        val saved = AtomicInteger()
        val finished = CountDownLatch(1)
        val writing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val earlyTerminations = AtomicInteger()
        val handler = FatalCrashHandler(null, { _, _ ->
            writing.countDown()
            assertTrue(release.await(1, TimeUnit.SECONDS))
            saved.incrementAndGet()
            finished.countDown()
        }, { if (finished.count > 0) earlyTerminations.incrementAndGet() })
        val first = Thread { handler.uncaughtException(Thread.currentThread(), Error("first")) }
        first.start()
        assertTrue(writing.await(1, TimeUnit.SECONDS))
        val second = Thread { handler.uncaughtException(Thread.currentThread(), Error("second")) }
        second.start()
        release.countDown()
        first.join(2_000)
        second.join(2_000)
        assertEquals(1, saved.get())
        assertEquals(0, earlyTerminations.get())
    }
}
