package com.luc4n3x.levyra.diagnostics

import android.os.Build
import com.luc4n3x.levyra.ui.i18n.supportCopy
import java.io.PrintWriter
import java.io.Writer
import java.net.URLEncoder
import java.time.Instant

internal data class CrashEnvironment(
    val version: String,
    val build: String,
    val androidVersion: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String
)

internal data class CrashContext(
    val screen: String = "Unavailable",
    val skin: String = "Unavailable",
    val playback: String = "Unavailable",
    val presentation: CrashPresentation = CrashPresentation()
)

internal data class CrashReport(val id: String, val title: String, val body: String, val presentation: CrashPresentation = CrashPresentation())

internal fun createCrashReport(
    id: String,
    timestampMs: Long,
    thread: Thread,
    error: Throwable,
    environment: CrashEnvironment,
    context: CrashContext,
    redact: (String) -> String
): CrashReport {
    val trace = LimitedTraceWriter()
    error.printStackTrace(PrintWriter(trace))
    val kind = error.javaClass.simpleName.ifBlank { error.javaClass.name }
    val title = redact("[Crash] $kind on Android ${environment.androidVersion} - Levyra ${environment.version}")
        .replace('\n', ' ').take(240)
    val body = buildString {
        appendLine("## Problem description")
        appendLine("Levyra closed unexpectedly because of an uncaught $kind.")
        appendLine()
        appendLine("## Platform")
        appendLine("Android")
        appendLine()
        appendLine("## Affected area")
        appendLine("App startup or crash")
        appendLine()
        appendLine("## Steps to reproduce")
        appendLine("Please describe what you were doing immediately before the crash.")
        appendLine("1. ")
        appendLine("2. ")
        appendLine()
        appendLine("## Actual behavior")
        appendLine("The application terminated unexpectedly.")
        appendLine()
        appendLine("## Expected behavior")
        appendLine("Please describe what you expected to happen.")
        appendLine()
        appendLine("## Reproduction frequency")
        appendLine("Please select how often this happens.")
        appendLine()
        appendLine("## Levyra version")
        appendLine("${environment.version} (${environment.build})")
        appendLine()
        appendLine("## Installation source")
        appendLine("Please select where you installed Levyra.")
        appendLine()
        appendLine("## System information")
        appendLine("${environment.manufacturer} ${environment.model}")
        appendLine("Android ${environment.androidVersion}, API ${environment.apiLevel}")
        appendLine()
        appendLine("## Logs or error details")
        appendLine("Crash time (UTC): ${Instant.ofEpochMilli(timestampMs)}")
        appendLine("Thread: ${thread.name} (id=${thread.stableId()})")
        appendLine("Last screen: ${context.screen}")
        appendLine("Skin: ${context.skin}")
        appendLine()
        appendLine("````text")
        appendLine(trace.toString().replace("````", "[code fence]"))
        if (trace.truncated) appendLine("[Stack trace exceeded the 262144-character safety limit.]")
        appendLine("````")
        appendLine()
        appendLine("## Additional context")
        appendLine(context.playback)
        appendLine()
        appendLine("This report captures an uncaught Java/Kotlin exception. Native crashes, ANRs and system kills are not automatically captured.")
        appendLine("Sensitive patterns have been removed. Please review the report before publishing it.")
    }
    return CrashReport(id, title, redact(body), context.presentation)
}

internal data class CrashIssueDraft(val url: String, val clipboard: String, val needsPaste: Boolean)

internal fun crashIssueDraft(report: CrashReport): CrashIssueDraft {
    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    val words = supportCopy(report.presentation.language)
    val warning = "**${words.keepLogs}**\n\n"
    val body = warning + report.body
    val base = "https://github.com/LUC4N3X/Levyra-deepsound/issues/new?template=&title=${encode(report.title.take(240))}&body="
    val complete = base + encode(body)
    if (complete.length <= 6_000) return CrashIssueDraft(complete, body, false)
    val diagnostics = report.body.substringAfter("## Logs or error details\n", report.body)
        .replace("````", "[code fence]")
    val prefix = warning + words.pasteOnGithub + "\n\n## Logs or error details\n````text\n"
    val suffix = "\n````\n"
    fun excerpt(length: Int): String {
        if (length >= diagnostics.length) return diagnostics
        var headEnd = (length + 1) / 2
        if (headEnd > 0 && diagnostics[headEnd - 1].isHighSurrogate() && diagnostics[headEnd].isLowSurrogate()) headEnd--
        var tailStart = diagnostics.length - (length / 2)
        if (tailStart < diagnostics.length && diagnostics[tailStart - 1].isHighSurrogate() && diagnostics[tailStart].isLowSurrogate()) tailStart++
        return diagnostics.substring(0, headEnd) + "\n[... truncated; full report copied ...]\n" + diagnostics.substring(tailStart)
    }
    var low = 0
    var high = minOf(diagnostics.length, 6_000)
    while (low < high) {
        val middle = (low + high + 1) / 2
        if ((base + encode(prefix + excerpt(middle) + suffix)).length <= 6_000) low = middle else high = middle - 1
    }
    return CrashIssueDraft(base + encode(prefix + excerpt(low) + suffix), body, true)
}

private class LimitedTraceWriter : Writer() {
    private val text = StringBuilder()
    var truncated = false
        private set

    override fun write(buffer: CharArray, offset: Int, count: Int) {
        val remaining = (262_144 - text.length).coerceAtLeast(0)
        text.append(buffer, offset, minOf(count, remaining))
        if (count > remaining) truncated = true
    }

    override fun flush() = Unit
    override fun close() = Unit
    override fun toString(): String = text.toString()
}

private fun Thread.stableId(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) threadId() else legacyThreadId()

@Suppress("DEPRECATION")
private fun Thread.legacyThreadId(): Long = id
