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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal class LocalAudioTagEditor(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val artworkProcessor = LocalArtworkProcessor(appContext)

    suspend fun write(row: LocalMediaEntity, edits: LocalTagEdits): LocalTagWriteResult {
        val result = withContext(Dispatchers.IO) { writeInternal(row, edits) }
        if (result is LocalTagWriteResult.Success) awaitRescan(row)
        return result
    }

    suspend fun readEmbeddedLyrics(row: LocalMediaEntity): String = withContext(Dispatchers.IO) {
        LocalDeepTagReader.readEmbeddedLyrics(appContext, row)
    }

    private fun writeInternal(row: LocalMediaEntity, edits: LocalTagEdits): LocalTagWriteResult {
        val format = LocalEmbeddedTagWriter.formatOf(row.mimeType, row.displayName)
        if (format == LocalEditableTagFormat.Unsupported) return LocalTagWriteResult.UnsupportedFormat
        val sizeLimit = localTagEditLimitBytes(Runtime.getRuntime().maxMemory())
        if (row.sizeBytes > sizeLimit) return LocalTagWriteResult.FileTooLarge

        val uri = runCatching { Uri.parse(row.contentUri) }.getOrNull()
            ?: return LocalTagWriteResult.FileUnavailable
        val workspace = File(appContext.cacheDir, "local-tag-editor").apply { mkdirs() }
        if (workspace.usableSpace < localTagWorkspaceBytes(row.sizeBytes)) return LocalTagWriteResult.InsufficientSpace

        val artwork = when (val edit = edits.artwork) {
            LocalArtworkEdit.Keep -> LocalArtworkWrite.Keep
            LocalArtworkEdit.Remove -> LocalArtworkWrite.Remove
            is LocalArtworkEdit.Replace -> artworkProcessor.prepare(edit.sourceUri)
                ?: return LocalTagWriteResult.InvalidArtwork
        }
        val session = try {
            createSession(workspace, uri, format)
        } catch (error: IOException) {
            Timber.w(error, "Local tag workspace unavailable")
            return LocalTagWriteResult.InsufficientSpace
        }
        return try {
            writeSession(row, edits, artwork, session, sizeLimit)
        } finally {
            session.input.delete()
            session.output.delete()
        }
    }

    @Throws(IOException::class)
    private fun createSession(workspace: File, uri: Uri, format: LocalEditableTagFormat): EditSession {
        val extension = when (format) {
            LocalEditableTagFormat.M4a -> ".m4a"
            LocalEditableTagFormat.Mp3 -> ".mp3"
            LocalEditableTagFormat.Flac -> ".flac"
            LocalEditableTagFormat.Unsupported -> ".audio"
        }
        val input = File.createTempFile("source-", extension, workspace)
        val output = try {
            File.createTempFile("edited-", extension, workspace)
        } catch (error: IOException) {
            input.delete()
            throw error
        }
        return EditSession(uri = uri, format = format, input = input, output = output)
    }

    private fun writeSession(
        row: LocalMediaEntity,
        edits: LocalTagEdits,
        artwork: LocalArtworkWrite,
        session: EditSession,
        sizeLimit: Long
    ): LocalTagWriteResult {
        val sourceFailure = when (copySource(session.uri, session.input, sizeLimit)) {
            CopySourceResult.Ok -> null
            CopySourceResult.TooLarge -> LocalTagWriteResult.FileTooLarge
            CopySourceResult.Unavailable -> LocalTagWriteResult.FileUnavailable
            CopySourceResult.NoSpace -> LocalTagWriteResult.InsufficientSpace
        }
        if (sourceFailure != null) return sourceFailure

        val writerResult = try {
            LocalEmbeddedTagWriter.write(
                input = session.input,
                output = session.output,
                format = session.format,
                edits = edits,
                artwork = artwork
            )
        } catch (error: IOException) {
            Timber.w(error, "Local tag working copy could not be written")
            return if (session.output.usableSpace < session.input.length()) {
                LocalTagWriteResult.InsufficientSpace
            } else {
                LocalTagWriteResult.Failed
            }
        }
        writerFailure(writerResult, session.output)?.let { return it }
        if (!LocalEmbeddedTagWriter.verify(session.output, edits, artwork)) {
            Timber.w("Local tag working copy failed validation for %s", row.identityKey)
            return LocalTagWriteResult.Failed
        }

        val deepTags = LocalDeepTagReader.read(session.output)
        replaceMedia(row, session)?.let { return it }
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
            "unsupported_artwork" -> LocalTagWriteResult.InvalidArtwork
            else -> LocalTagWriteResult.Failed
        }
    }

    private fun replaceMedia(row: LocalMediaEntity, session: EditSession): LocalTagWriteResult? {
        val probe = try {
            resolver.openFileDescriptor(session.uri, "rw")
        } catch (denied: SecurityException) {
            return permissionResult(session.uri, denied) ?: LocalTagWriteResult.WriteDenied
        } catch (missing: FileNotFoundException) {
            Timber.w(missing, "Local media not writable")
            return if (localFileMissing(row)) LocalTagWriteResult.FileUnavailable else LocalTagWriteResult.WriteDenied
        } ?: return LocalTagWriteResult.FileUnavailable
        runCatching { probe.close() }

        val outcome = replaceWithRollback(session.output, session.input) { source ->
            val descriptor = resolver.openFileDescriptor(session.uri, "rw") ?: throw FileNotFoundException("media_unavailable")
            descriptor.use { replaceContent(it.fileDescriptor, source) }
        }
        return when (outcome) {
            LocalReplaceOutcome.Replaced -> null
            LocalReplaceOutcome.RolledBack -> LocalTagWriteResult.Failed
            LocalReplaceOutcome.RollbackFailed -> {
                Timber.e("Local tag restore failed for %s", row.identityKey)
                LocalTagWriteResult.Failed
            }
        }
    }

    private fun localFileMissing(row: LocalMediaEntity): Boolean {
        val path = row.filePath.trim()
        return path.isNotEmpty() && !File(path).exists()
    }

    private suspend fun awaitRescan(row: LocalMediaEntity) {
        if (row.filePath.isBlank()) return
        val mimeTypes = row.mimeType.takeIf { it.isNotBlank() }?.let { arrayOf(it) }
        val completed = withTimeoutOrNull(RESCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    MediaScannerConnection.scanFile(appContext, arrayOf(row.filePath), mimeTypes) { _, _ ->
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }.onFailure { error ->
                    Timber.w(error, "Local media rescan could not start")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        if (completed == null) Timber.w("Local media rescan timed out for %s", row.identityKey)
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

    private fun copySource(uri: Uri, target: File, sizeLimit: Long): CopySourceResult {
        val stream = try {
            resolver.openInputStream(uri)
        } catch (error: FileNotFoundException) {
            Timber.d(error, "Local tag source missing")
            return CopySourceResult.Unavailable
        } catch (error: SecurityException) {
            Timber.d(error, "Local tag source not readable")
            return CopySourceResult.Unavailable
        } ?: return CopySourceResult.Unavailable
        return try {
            val copied = stream.use { input ->
                target.outputStream().buffered().use { output -> copyBounded(input, output, sizeLimit) }
            }
            when {
                !copied -> CopySourceResult.TooLarge
                target.isFile && target.length() > 0L -> CopySourceResult.Ok
                else -> CopySourceResult.Unavailable
            }
        } catch (error: IOException) {
            Timber.d(error, "Local tag source copy failed")
            if (target.usableSpace < COPY_BUFFER) CopySourceResult.NoSpace else CopySourceResult.Unavailable
        }
    }

    @Throws(IOException::class)
    private fun copyBounded(input: java.io.InputStream, output: java.io.OutputStream, sizeLimit: Long): Boolean {
        val buffer = ByteArray(COPY_BUFFER)
        var total = 0L
        var read = input.read(buffer)
        while (read >= 0) {
            total += read
            if (total > sizeLimit) return false
            output.write(buffer, 0, read)
            read = input.read(buffer)
        }
        return true
    }

    @Throws(IOException::class)
    private fun replaceContent(fileDescriptor: java.io.FileDescriptor, source: File) {
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

    private enum class CopySourceResult { Ok, TooLarge, Unavailable, NoSpace }

    private companion object {
        const val COPY_BUFFER = 128 * 1024
        const val RESCAN_TIMEOUT_MS = 5_000L
    }
}

internal enum class LocalReplaceOutcome { Replaced, RolledBack, RollbackFailed }

internal fun replaceWithRollback(edited: File, original: File, write: (File) -> Unit): LocalReplaceOutcome {
    try {
        write(edited)
        return LocalReplaceOutcome.Replaced
    } catch (error: IOException) {
        Timber.w(error, "Local tag write failed, restoring original")
    } catch (error: RuntimeException) {
        Timber.w(error, "Local tag write interrupted, restoring original")
    }
    return try {
        write(original)
        LocalReplaceOutcome.RolledBack
    } catch (error: IOException) {
        Timber.e(error, "Local tag restore failed")
        LocalReplaceOutcome.RollbackFailed
    } catch (error: RuntimeException) {
        Timber.e(error, "Local tag restore interrupted")
        LocalReplaceOutcome.RollbackFailed
    }
}

internal fun localTagEditLimitBytes(maxMemoryBytes: Long): Long =
    minOf(LocalEmbeddedTagWriter.MAX_INPUT_BYTES, maxMemoryBytes / IN_MEMORY_EDIT_COPIES)

internal fun localTagWorkspaceBytes(sizeBytes: Long): Long = sizeBytes * 2L + WORKSPACE_MARGIN_BYTES

private const val IN_MEMORY_EDIT_COPIES = 4L
private const val WORKSPACE_MARGIN_BYTES = 8L * 1024L * 1024L
