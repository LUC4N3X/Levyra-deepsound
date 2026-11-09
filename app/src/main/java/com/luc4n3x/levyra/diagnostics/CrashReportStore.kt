package com.luc4n3x.levyra.diagnostics

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class CrashReportStore(private val directory: File) {
    fun save(report: CrashReport) {
        require(REPORT_ID.matches(report.id))
        val bytes = "${report.title}\n@ui ${report.presentation.encode()}\n${report.body}".toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_REPORT_BYTES)
        directory.mkdirs()
        check(directory.isDirectory)
        RandomAccessFile(File(directory, "report.lock"), "rw").use { lockFile ->
            val lock = lockFile.channel.tryLock() ?: throw IOException("Crash capture already in progress")
            try {
                directory.listFiles().orEmpty().filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
                writeDiagnosticBytes(reportFile(report.id), bytes)
                writeDiagnosticBytes(File(directory, "pending"), report.id.toByteArray(Charsets.UTF_8))
                reportFiles().drop(MAX_REPORTS).forEach { it.delete() }
            } finally {
                lock.release()
            }
        }
    }

    fun claimPending(): CrashReport? {
        val pending = File(directory, "pending")
        val claimed = File(directory, "pending-${System.nanoTime()}.claimed")
        if (!pending.renameTo(claimed)) return null
        return try {
            claimed.takeIf { it.length() <= 80 }?.readText(Charsets.UTF_8)?.let(::read)
        } finally {
            claimed.delete()
        }
    }

    fun reports(): List<CrashReport> = reportFiles().take(MAX_REPORTS).mapNotNull { file ->
        runCatching { read(file.nameWithoutExtension) }.getOrNull()
    }

    fun read(id: String): CrashReport? {
        if (!REPORT_ID.matches(id)) return null
        val file = reportFile(id)
        if (!file.isFile || file.length() > MAX_REPORT_BYTES) return null
        val content = file.readText(Charsets.UTF_8)
        val separator = content.indexOf('\n')
        if (separator !in 1..240) return null
        val remainder = content.substring(separator + 1)
        val uiEnd = remainder.indexOf('\n')
        val presentation = if (remainder.startsWith("@ui ") && uiEnd in 1..4_100) {
            CrashPresentation.decode(remainder.substring(4, uiEnd))
        } else null
        return CrashReport(id, content.substring(0, separator),
            if (remainder.startsWith("@ui ") && uiEnd in 1..4_100) remainder.substring(uiEnd + 1) else remainder,
            presentation ?: CrashPresentation())
    }

    fun delete(id: String): Boolean {
        if (!REPORT_ID.matches(id)) return false
        return reportFile(id).delete()
    }

    private fun reportFile(id: String): File = File(directory, "$id.txt")

    private fun reportFiles(): List<File> = directory.listFiles().orEmpty()
        .filter { it.isFile && it.extension == "txt" && REPORT_ID.matches(it.nameWithoutExtension) }
        .sortedByDescending { it.nameWithoutExtension.substringBefore('-').toLongOrNull() ?: 0L }

    companion object {
        const val MAX_REPORTS = 5
        const val MAX_REPORT_BYTES = 1_048_576
        private val REPORT_ID = Regex("[0-9]{10,17}-[0-9]{1,10}")
    }
}

internal fun writeDiagnosticBytes(target: File, bytes: ByteArray) {
    val temporary = File(target.parentFile, "${target.name}.tmp")
    FileOutputStream(temporary).use { output ->
        output.write(bytes)
        output.flush()
        output.fd.sync()
    }
    try {
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } catch (_: AtomicMoveNotSupportedException) {
        if (!temporary.renameTo(target)) throw IOException("Cannot persist crash report")
    }
}
