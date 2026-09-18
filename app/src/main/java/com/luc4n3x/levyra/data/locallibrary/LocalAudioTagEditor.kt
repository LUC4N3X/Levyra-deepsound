package com.luc4n3x.levyra.data.locallibrary

import android.app.RecoverableSecurityException
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.luc4n3x.levyra.data.local.LocalMediaEntity
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

    suspend fun write(row: LocalMediaEntity, edits: LocalTagEdits): LocalTagWriteResult =
        withContext(Dispatchers.IO) {
            writeInternal(row, edits)
        }

    private fun writeInternal(row: LocalMediaEntity, edits: LocalTagEdits): LocalTagWriteResult {
        val format = LocalEmbeddedTagWriter.formatOf(row.mimeType, row.displayName)
        if (format == LocalEditableTagFormat.Unsupported) return LocalTagWriteResult.UnsupportedFormat
        if (row.sizeBytes > LocalEmbeddedTagWriter.MAX_INPUT_BYTES) return LocalTagWriteResult.FileTooLarge

        val uri = runCatching { Uri.parse(row.contentUri) }.getOrNull()
            ?: return LocalTagWriteResult.FileUnavailable
        val session = createSession(uri, format)
        return try {
            writeSession(row, edits, session)
        } finally {
            session.input.delete()
            session.output.delete()
        }
    }

    private fun createSession(uri: Uri, format: LocalEditableTagFormat): EditSession {
        val workspace = File(appContext.cacheDir, "local-tag-editor").apply { mkdirs() }
        val extension = when (format) {
            LocalEditableTagFormat.M4a -> ".m4a"
            LocalEditableTagFormat.Mp3 -> ".mp3"
            LocalEditableTagFormat.Flac -> ".flac"
            LocalEditableTagFormat.Unsupported -> ".audio"
        }
        return EditSession(
            uri = uri,
            format = format,
            input = File.createTempFile("source-", extension, workspace),
            output = File.createTempFile("edited-", extension, workspace)
        )
    }

    private fun writeSession(
        row: LocalMediaEntity,
        edits: LocalTagEdits,
        session: EditSession
    ): LocalTagWriteResult {
        val sourceFailure = when (copySource(session.uri, session.input)) {
            CopySourceResult.Ok -> null
            CopySourceResult.TooLarge -> LocalTagWriteResult.FileTooLarge
            CopySourceResult.Unavailable -> LocalTagWriteResult.FileUnavailable
        }
        if (sourceFailure != null) return sourceFailure

        val writerResult = LocalEmbeddedTagWriter.write(
            input = session.input,
            output = session.output,
            format = session.format,
            edits = edits
        )
        writerFailure(writerResult, session.output)?.let { return it }

        val deepTags = LocalDeepTagReader.read(session.output)
        replaceMedia(row, session)?.let { return it }
        rescanEditedFile(row)
        return successResult(row, edits, deepTags, session.output)
    }

    private fun writerFailure(
        result: LocalEmbeddedTagWriteResult,
        output: File
    ): LocalTagWriteResult? {
        if (result.success && output.length() > 0L) return null
        return when (result.reason) {
            "input_too_large" -> LocalTagWriteResult.FileTooLarge
            "unsupported_format", "unsupported_id3_version", "unsupported_id3_flags" ->
                LocalTagWriteResult.UnsupportedFormat
            else -> LocalTagWriteResult.Failed
        }
    }

    private fun replaceMedia(row: LocalMediaEntity, session: EditSession): LocalTagWriteResult? {
        val writable = try {
            resolver.openFileDescriptor(session.uri, "rw")
        } catch (denied: SecurityException) {
            return permissionResult(session.uri, denied) ?: LocalTagWriteResult.Failed
        } ?: return LocalTagWriteResult.FileUnavailable

        return try {
            writable.use { descriptor -> replaceContent(descriptor.fileDescriptor, session.output) }
            null
        } catch (error: Exception) {
            Timber.w(error, "Local tag write failed, restoring original")
            restoreOriginal(row, session)
            LocalTagWriteResult.Failed
        }
    }

    private fun restoreOriginal(row: LocalMediaEntity, session: EditSession) {
        val restored = runCatching {
            resolver.openFileDescriptor(session.uri, "rw")?.use { descriptor ->
                replaceContent(descriptor.fileDescriptor, session.input)
            } ?: false
        }.getOrDefault(false)
        if (!restored) Timber.e("Local tag restore failed for %s", row.identityKey)
    }

    private fun rescanEditedFile(row: LocalMediaEntity) {
        if (row.filePath.isBlank()) return
        runCatching {
            val mimeTypes = row.mimeType
                .takeIf { it.isNotBlank() }
                ?.let { arrayOf(it) }
            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(row.filePath),
                mimeTypes
            ) { _, _ -> }
        }
    }

    private fun successResult(
        row: LocalMediaEntity,
        edits: LocalTagEdits,
        deepTags: LocalDeepTags,
        output: File
    ): LocalTagWriteResult.Success {
        val edited = row.withTagEdits(edits, deepTags)
        val writtenSize = output.length()
        val now = System.currentTimeMillis()
        return LocalTagWriteResult.Success(
            edited.copy(
                sizeBytes = writtenSize,
                contentFingerprint = localContentFingerprint(writtenSize, edited.durationMs, edited.title),
                dateModifiedMs = now,
                lastSeenAt = now
            )
        )
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
                        if (total > LocalEmbeddedTagWriter.MAX_INPUT_BYTES) return CopySourceResult.TooLarge
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

    private data class EditSession(
        val uri: Uri,
        val format: LocalEditableTagFormat,
        val input: File,
        val output: File
    )

    private enum class CopySourceResult { Ok, TooLarge, Unavailable }

    private companion object {
        const val COPY_BUFFER = 128 * 1024
    }
}
