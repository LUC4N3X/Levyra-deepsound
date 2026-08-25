package com.luc4n3x.levyra.ui.artwork

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber

internal object ArtworkMediaStore {

    private const val RELATIVE_PATH = "Pictures/Levyra"
    private const val MIME_TYPE = "image/jpeg"

    suspend fun save(context: Context, fileName: String, write: (OutputStream) -> Boolean): Boolean =
        withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                saveToAppPictures(context, fileName, write)
            } else {
                saveToMediaStore(context, fileName, write)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(
        context: Context,
        fileName: String,
        write: (OutputStream) -> Boolean
    ): Boolean {
        var pendingUri: Uri? = null
        val resolver = context.contentResolver
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
                put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return false
            pendingUri = uri
            val written = resolver.openOutputStream(uri)?.use { output -> write(output) } ?: false
            if (!written) {
                resolver.delete(uri, null, null)
                pendingUri = null
                return false
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            pendingUri = null
            true
        } catch (cancelled: CancellationException) {
            pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Artwork save failed")
            pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
            false
        }
    }

    private fun saveToAppPictures(
        context: Context,
        fileName: String,
        write: (OutputStream) -> Boolean
    ): Boolean {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return false
        val target = File(directory, fileName)
        return try {
            FileOutputStream(target).use { output -> write(output) }
        } catch (cancelled: CancellationException) {
            runCatching { target.delete() }
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Artwork save failed")
            runCatching { target.delete() }
            false
        }
    }
}
