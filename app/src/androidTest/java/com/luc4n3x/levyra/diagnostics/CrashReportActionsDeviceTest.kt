package com.luc4n3x.levyra.diagnostics

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.ui.i18n.supportCopy
import com.luc4n3x.levyra.player.redactDiagnosticText
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashReportActionsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun fileTransportUsesOnlyTheSelectedReadOnlyCacheUri() {
        val directory = File(context.cacheDir, "share/diagnostics").apply { mkdirs() }
        val file = File(directory, "transport-test.txt").apply { writeText("Sanitized diagnostic fixture") }
        try {
            val report = CrashReport("1700000000000-1", "Diagnostic fixture", file.readText())
            val send = crashReportShareIntent(context, report, file)
            val uri = send.clipData!!.getItemAt(0).uri
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.share-files", uri.authority)
            assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, send.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            assertEquals(uri, send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))
            assertEquals("text/plain", send.type)
            assertEquals(report.body, context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.share-files", File(context.filesDir, "crash-reports/private.txt"))
                fail("Private report files must not be exposed by the provider")
            } catch (_: IllegalArgumentException) {
            }
        } finally {
            file.delete()
        }
    }

    @Test fun recoveryActionsCopyShareOpenGithubAndDeleteOnlyTheSelectedReport() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("crashUi") == "true")
        val storage = CrashReporting.store(context)
        assumeTrue(storage.reports().size < CrashReportStore.MAX_REPORTS)
        val previousPending = storage.claimPending()
        val id = "${System.currentTimeMillis()}-${android.os.Process.myPid()}"
        val report = createCrashReport(id, System.currentTimeMillis(), Thread("DiagnosticUiTest"),
            IllegalStateException("UI diagnostic fixture"), CrashEnvironment("debug", "test", "14", 34, "Test", "Emulator"),
            CrashContext("Diagnostic UI test", "Default"), ::redactDiagnosticText)
        storage.save(report)
        storage.claimPending()
        val words = supportCopy("en")
        try {
            instrumentation.runOnMainSync {
                context.startActivity(Intent(context, CrashReportsActivity::class.java)
                    .putExtra(CrashReportsActivity.EXTRA_REPORT_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            click(words.copy)
            lateinit var clipboard: ClipboardManager
            instrumentation.runOnMainSync { clipboard = context.getSystemService(ClipboardManager::class.java) }
            await("Clipboard must contain the complete report") {
                clipboard.primaryClip?.getItemAt(0)?.text?.toString() == report.body
            }
            click(words.share)
            await("Android share chooser must open") {
                val name = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
                name == "android" || name == "com.android.intentresolver"
            }
            back()
            click(words.github)
            await("GitHub must open in an external browser") {
                val name = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
                name.isNotBlank() && name != context.packageName
            }
            back()
            click(words.delete)
            await("Selected report must be deleted") { storage.read(id) == null }
            click(words.close)
        } finally {
            storage.delete(id)
            File(context.cacheDir, "share/diagnostics/$id.txt").delete()
            if (previousPending != null) storage.save(previousPending)
        }
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        assertTrue(message, condition())
    }

    private fun find(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString() == label || node.contentDescription?.toString() == label) return node
        for (index in 0 until node.childCount) find(node.getChild(index), label)?.let { return it }
        return null
    }

    private fun click(label: String) {
        await("Control missing: $label") { find(instrumentation.uiAutomation.rootInActiveWindow, label) != null }
        var node = find(instrumentation.uiAutomation.rootInActiveWindow, label)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun back() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent 4").close()
    }
}
