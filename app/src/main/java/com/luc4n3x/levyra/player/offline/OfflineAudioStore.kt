package com.luc4n3x.levyra.player.offline

import android.content.Context
import com.luc4n3x.levyra.data.LevyraVaultOperationMutex
import com.luc4n3x.levyra.data.collections.stableHash
import com.luc4n3x.levyra.data.local.LevyraDatabase
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val OFFLINE_AUDIO_DIRECTORY = "offline_audio"
internal const val OFFLINE_AUDIO_STAGING_DIRECTORY = "offline_audio_staging"
internal const val OFFLINE_STORAGE_FULL = "offline_storage_full"
internal const val LEGACY_OFFLINE_CACHE_WORKSPACE = "levyra_offline_export"

private const val STABLE_ID_LENGTH = 40
private const val RESUME_PREFIX = "resume-"
private val OfflineAudioStoreUriSegment = "/files/$OFFLINE_AUDIO_DIRECTORY/"
private val StagingRetentionMs = TimeUnit.HOURS.toMillis(2)
private val ResumeRetentionMs = TimeUnit.DAYS.toMillis(7)
private val UnregisteredAudioGraceMs = TimeUnit.HOURS.toMillis(1)

internal fun isOfflineResumeArtifact(fileName: String): Boolean =
    fileName.startsWith(RESUME_PREFIX) && (fileName.endsWith(".part") || fileName.endsWith(".part.id"))

internal fun offlineStagingRetentionMs(fileName: String): Long =
    if (isOfflineResumeArtifact(fileName)) ResumeRetentionMs else StagingRetentionMs

internal class OfflineStorageFullException(cause: Throwable? = null) : IOException(OFFLINE_STORAGE_FULL, cause)

internal fun isOfflineStorageExhausted(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is OfflineStorageFullException) return true
        val message = current.message.orEmpty()
        if (message.contains("ENOSPC") || message.contains("No space left", ignoreCase = true)) return true
        current = current.cause
    }
    return false
}

internal fun isOfflineAudioStoreUri(rawUri: String): Boolean =
    rawUri.startsWith("file:", ignoreCase = true) && rawUri.contains(OfflineAudioStoreUriSegment)

internal fun offlineAudioStableId(trackId: String, preset: String, quality: String): String =
    stableHash("$trackId|$preset|$quality", STABLE_ID_LENGTH)

internal class OfflineAudioStore(filesDir: File) {
    val audioDirectory = File(filesDir, OFFLINE_AUDIO_DIRECTORY)
    val stagingDirectory = File(filesDir, OFFLINE_AUDIO_STAGING_DIRECTORY)

    fun stagingWorkspace(): File = stagingDirectory.ensureDirectory()

    fun commit(staged: File, stableId: String, extension: String): File {
        val expectedBytes = staged.length()
        if (!staged.isFile || expectedBytes <= 0L) throw IOException("Offline audio staging file is empty")
        val target = File(audioDirectory.ensureDirectory(), "$stableId.${safeExtension(extension)}")
        RandomAccessFile(staged, "rw").use { it.fd.sync() }
        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        target.setLastModified(System.currentTimeMillis())
        syncDirectory(audioDirectory)
        if (target.length() != expectedBytes) {
            runCatching { target.delete() }
            throw IOException("Offline audio commit changed the file length")
        }
        return target
    }

    fun isAbsent(rawUri: String): Boolean {
        if (!isOfflineAudioStoreUri(rawUri)) return false
        val file = File(audioDirectory, rawUri.substringAfterLast('/'))
        return !file.isFile || file.length() <= 0L
    }

    fun sweep(referencedUris: Collection<String>, nowMs: Long = System.currentTimeMillis()) {
        stagingDirectory.listFiles()?.forEach { file ->
            if (nowMs - file.lastModified() > offlineStagingRetentionMs(file.name)) runCatching { file.delete() }
        }
        val referencedNames = referencedUris
            .filter(::isOfflineAudioStoreUri)
            .mapTo(hashSetOf()) { it.substringAfterLast('/') }
        audioDirectory.listFiles()?.forEach { file ->
            val unregistered = file.name !in referencedNames
            if (unregistered && nowMs - file.lastModified() > UnregisteredAudioGraceMs) runCatching { file.delete() }
        }
    }

    private fun File.ensureDirectory(): File {
        if (!isDirectory && !mkdirs() && !isDirectory) throw IOException("Offline audio directory unavailable")
        return this
    }

    private fun safeExtension(extension: String): String =
        extension.filter(Char::isLetterOrDigit).ifBlank { "audio" }

    private fun syncDirectory(directory: File) {
        runCatching {
            FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) }
        }
    }
}

internal object OfflineAudioMaintenance {
    suspend fun run(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val store = OfflineAudioStore(appContext.filesDir)
        val database = LevyraDatabase.get(appContext)
        LevyraVaultOperationMutex.withLock {
            val downloads = database.downloadedTracksDao()
            val rows = downloads.all()
            val absent = rows.filter { store.isAbsent(it.uri) }
            absent.forEach { downloads.deleteById(it.id) }
            store.sweep(referencedUris = (rows - absent.toSet()).map { it.uri })
        }
        runCatching { File(appContext.cacheDir, LEGACY_OFFLINE_CACHE_WORKSPACE).deleteRecursively() }
    }
}
