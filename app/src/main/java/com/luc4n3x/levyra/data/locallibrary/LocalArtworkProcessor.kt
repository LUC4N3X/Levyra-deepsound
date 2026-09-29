package com.luc4n3x.levyra.data.locallibrary

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import timber.log.Timber

internal class LocalArtworkProcessor(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    fun prepare(sourceUri: String): LocalArtworkWrite.Embed? {
        val uri = runCatching { Uri.parse(sourceUri) }.getOrNull() ?: return null
        return try {
            val bounds = readBounds(uri) ?: return null
            val orientation = readOrientation(uri)
            val original = if (
                canEmbedOriginalArtwork(bounds.mimeType, bounds.width, bounds.height, orientation == ExifInterface.ORIENTATION_NORMAL)
            ) {
                readOriginal(uri, bounds)
            } else {
                null
            }
            original ?: transcode(uri, bounds, orientation)
        } catch (error: IOException) {
            Timber.w(error, "Artwork source unavailable")
            null
        } catch (error: SecurityException) {
            Timber.w(error, "Artwork source not readable")
            null
        } catch (error: IllegalArgumentException) {
            Timber.w(error, "Artwork source could not be decoded")
            null
        }
    }

    private fun readBounds(uri: Uri): ArtworkBounds? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ArtworkBounds(0, 0, "") else null
        }
        return ArtworkBounds(options.outWidth, options.outHeight, options.outMimeType.orEmpty())
    }

    private fun readOrientation(uri: Uri): Int = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun readOriginal(uri: Uri, bounds: ArtworkBounds): LocalArtworkWrite.Embed? {
        val bytes = resolver.openInputStream(uri)?.use { it.readBounded(MAX_ORIGINAL_ARTWORK_BYTES) } ?: return null
        val mime = LocalEmbeddedTagWriter.imageMimeType(bytes) ?: return null
        return LocalArtworkWrite.Embed(bytes, mime, bounds.width, bounds.height)
    }

    private fun transcode(uri: Uri, bounds: ArtworkBounds, orientation: Int): LocalArtworkWrite.Embed? {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeScaled(uri)
        } else {
            decodeSampled(uri, bounds)?.let { applyOrientation(it, orientation) }
        } ?: return null
        return try {
            val bytes = ByteArrayOutputStream(256 * 1024).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_JPEG_QUALITY, output)) return null
                output.toByteArray()
            }
            LocalArtworkWrite.Embed(bytes, "image/jpeg", bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeScaled(uri: Uri): Bitmap? {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val target = localArtworkTargetSize(info.size.width, info.size.height, MAX_ARTWORK_EDGE_PX)
            decoder.setTargetSize(target.first, target.second)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeSampled(uri: Uri, bounds: ArtworkBounds): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = localArtworkSampleSize(bounds.width, bounds.height, MAX_ARTWORK_EDGE_PX)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val target = localArtworkTargetSize(decoded.width, decoded.height, MAX_ARTWORK_EDGE_PX)
        if (target.first == decoded.width && target.second == decoded.height) return decoded
        val scaled = Bitmap.createScaledBitmap(decoded, target.first, target.second, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun InputStream.readBounded(limit: Int): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (output.size() + read > limit) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private class ArtworkBounds(val width: Int, val height: Int, val mimeType: String)
}

internal const val MAX_ARTWORK_EDGE_PX = 1_600
internal const val MAX_ORIGINAL_ARTWORK_BYTES = 2 * 1024 * 1024
private const val ARTWORK_JPEG_QUALITY = 92

internal fun canEmbedOriginalArtwork(mimeType: String, width: Int, height: Int, orientationNormal: Boolean): Boolean =
    orientationNormal &&
        (mimeType == "image/jpeg" || mimeType == "image/png") &&
        width in 1..MAX_ARTWORK_EDGE_PX &&
        height in 1..MAX_ARTWORK_EDGE_PX

internal fun localArtworkTargetSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
    if (width <= 0 || height <= 0) return maxEdge to maxEdge
    val longEdge = maxOf(width, height)
    if (longEdge <= maxEdge) return width to height
    val scale = maxEdge.toDouble() / longEdge.toDouble()
    return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
}

internal fun localArtworkSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    val longEdge = maxOf(width, height)
    var sample = 1
    while (longEdge / (sample * 2) >= maxEdge) sample *= 2
    return sample
}
