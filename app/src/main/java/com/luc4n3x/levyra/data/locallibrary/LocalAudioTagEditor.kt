package com.luc4n3x.levyra.data.locallibrary

import android.app.RecoverableSecurityException
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.luc4n3x.levyra.data.local.LocalMediaEntity
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagEdits
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class LocalAudioTagEditor(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun write(row: LocalMediaEntity, edits: LocalTagEdits): LocalTagWriteResult = withContext(Dispatchers.IO) {
        if (!row.isSafeTagEditorFormat()) return@withContext LocalTagWriteResult.UnsupportedFormat
        if (row.sizeBytes > LevyraM4aTagWriter.MAX_INPUT_BYTES) return@withContext LocalTagWriteResult.FileTooLarge

        val uri = runCatching { Uri.parse(row.contentUri) }.getOrNull()
            ?: return@withContext LocalTagWriteResult.FileUnavailable
        val workspace = File(appContext.cacheDir, "local-tag-editor").apply { mkdirs() }
        val input = File.createTempFile("source-", ".m4a", workspace)
        val output = File.createTempFile("edited-", ".m4a", workspace)
        try {
            when (copySource(uri, input)) {
                CopySourceResult.Ok -> Unit
                CopySourceResult.TooLarge -> return@withContext LocalTagWriteResult.FileTooLarge
                CopySourceResult.Unavailable -> return@withContext LocalTagWriteResult.FileUnavailable
            }

            val writerResult = LevyraM4aTagWriter.writeTags(
                input = input,
                output = output,
                edits = LevyraM4aTagEdits(
                    title = edits.title.trim(),
                    artist = edits.artist.trim(),
                    album = edits.album.trim(),
                    albumArtist = edits.albumArtist.trim(),
                    genre = edits.genre.trim(),
                    year = edits.year.trim(),
                    trackNumber = edits.trackNumber.trim().toIntOrNull()?.coerceIn(0, 9_999) ?: 0,
                    discNumber = edits.discNumber.trim().toIntOrNull()?.coerceIn(0, 999) ?: 0,
                    composer = edits.composer.trim(),
                    lyricist = edits.lyricist.trim(),
                    comment = edits.comment.trim(),
                    copyright = edits.copyright.trim()
                )
            )
            if (!writerResult.success || output.length() <= 0L) {
                return@withContext if (writerResult.reason == "input_too_large") {
                    LocalTagWriteResult.FileTooLarge
                } else {
                    LocalTagWriteResult.Failed
                }
            }

            val deepTags = LocalDeepTagReader.read(output)
            val writable = try {
                resolver.openFileDescriptor(uri, "rw")
            } catch (denied: SecurityException) {
                return@withContext permissionResult(uri, denied) ?: LocalTagWriteResult.Failed
            } ?: return@withContext LocalTagWriteResult.FileUnavailable

            try {
                writable.use { descriptor -> replaceContent(descriptor.fileDescriptor, output) }
            } catch (error: Exception) {
                Timber.w(error, "Local tag write failed, restoring original")
                val restored = runCatching {
                    resolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                        replaceContent(descriptor.fileDescriptor, input)
                    } ?: false
                }.getOrDefault(false)
                if (!restored) Timber.e("Local tag restore failed for %s", row.identityKey)
                return@withContext LocalTagWriteResult.Failed
            }

            if (row.filePath.isNotBlank()) {
                runCatching {
                    MediaScannerConnection.scanFile(
                        appContext,
                        arrayOf(row.filePath),
                        arrayOf(row.mimeType.takeIf { it.isNotBlank() })
                    ) { _, _ -> }
                }
            }

            val edited = row.withTagEdits(edits, deepTags)
            val writtenSize = output.length()
            LocalTagWriteResult.Success(
                edited.copy(
                    sizeBytes = writtenSize,
                    contentFingerprint = localContentFingerprint(writtenSize, edited.durationMs, edited.title),
                    dateModifiedMs = System.currentTimeMillis(),
                    lastSeenAt = System.currentTimeMillis()
                )
            )
        } finally {
            input.delete()
            output.delete()
        }
    }

    private fun copySource(uri: Uri, target: File): CopySourceResult {
        return try {
            val stream = resolver.openInputStream(uri) ?: return CopySourceResult.Unavailable
            stream.use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(COPY_BUFFER)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        total += read
                        if (total > LevyraM4aTagWriter.MAX_INPUT_BYTES) return CopySourceResult.TooLarge
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (target.isFile && target.length() > 0L) CopySourceResult.Ok else CopySourceResult.Unavailable
        } catch (error: Exception) {
            Timber.d(error, "Local tag source unavailable")
            CopySourceResult.Unavailable
        }
    }

    @Throws(IOException::class)
    private fun replaceContent(fileDescriptor: java.io.FileDescriptor, source: File): Boolean {
        FileOutputStream(fileDescriptor).use { output ->
            val channel = output.channel
            channel.truncate(0L)
            FileInputStream(source).use { input ->
                var position = 0L
                val size = source.length()
                while (position < size) {
                    val copied = input.channel.transferTo(position, size - position, channel)
                    if (copied <= 0L) throw IOException("media_copy_stalled")
                    position += copied
                }
            }
            channel.truncate(source.length())
            channel.force(true)
            output.fd.sync()
        }
        return true
    }

    private fun permissionResult(uri: Uri, error: SecurityException): LocalTagWriteResult.PermissionRequired? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
            return LocalTagWriteResult.PermissionRequired(error.userAction.actionIntent.intentSender)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                MediaStore.createWriteRequest(resolver, listOf(uri)).intentSender
            }.getOrNull()?.let(LocalTagWriteResult::PermissionRequired)
        }
        return null
    }

    private enum class CopySourceResult { Ok, TooLarge, Unavailable }

    private companion object {
        const val COPY_BUFFER = 128 * 1024
    }
}
