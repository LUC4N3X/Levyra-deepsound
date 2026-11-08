package com.luc4n3x.levyra.diagnostics

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
        appendLine("Thread: ${thread.name} (id=${thread.id})")
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
    val base = "https://github.com/LUC4N3X/Levyra-deepsound/issues/new?template=bug_report.yml&title=${encode(report.title)}"
    val fields = linkedMapOf(
        "description" to report.body.substringAfter("## Problem description\n").substringBefore("\n## Platform").trim(),
        "reproduce" to "Please add the steps that led to this crash.",
        "actual" to "Levyra closed unexpectedly.",
        "expected" to "Please describe what you expected to happen.",
        "version" to report.body.substringAfter("## Levyra version\n").substringBefore("\n## Installation source").trim(),
        "system_information" to report.body.substringAfter("## System information\n").substringBefore("\n## Logs or error details").trim()
    )
    val expanded = base + fields.entries.joinToString("") { (key, value) -> "&$key=${encode(value.take(300))}" }
    val prefix = expanded.takeIf { it.length <= 3_000 } ?: base
    val complete = "$prefix&logs=${encode(report.body)}"
    val needsPaste = complete.length > 6_000
    val url = if (needsPaste) {
        "$prefix&logs=${encode("The complete report has been copied. Paste it here before submitting.")}"
    } else complete
    return CrashIssueDraft(url, report.body, needsPaste)
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
