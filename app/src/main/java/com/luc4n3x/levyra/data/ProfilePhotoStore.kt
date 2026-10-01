package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.luc4n3x.levyra.data.locallibrary.LocalArtworkProcessor
import java.io.File
import java.io.IOException
import timber.log.Timber

internal const val PROFILE_PHOTO_EDGE_PX = 256
private const val PROFILE_PHOTO_DIRECTORY = "profile"
private const val PROFILE_PHOTO_FILE = "photo.jpg"
private const val PROFILE_PHOTO_JPEG_QUALITY = 88

internal data class ProfilePhotoCrop(val left: Int, val top: Int, val size: Int)

internal fun profilePhotoSquareCrop(width: Int, height: Int): ProfilePhotoCrop {
    val size = minOf(width, height).coerceAtLeast(0)
    return ProfilePhotoCrop(
        left = ((width - size) / 2).coerceAtLeast(0),
        top = ((height - size) / 2).coerceAtLeast(0),
        size = size
    )
}

internal fun profilePhotoSampleSize(width: Int, height: Int, targetEdge: Int): Int {
    val shortEdge = minOf(width, height)
    if (shortEdge <= 0 || targetEdge <= 0) return 1
    var sample = 1
    while (shortEdge / (sample * 2) >= targetEdge) sample *= 2
    return sample
}

internal class ProfilePhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, PROFILE_PHOTO_DIRECTORY)
    private val file = File(directory, PROFILE_PHOTO_FILE)

    fun current(): File? = file.takeIf { it.isFile && it.length() > 0L }

    fun save(uri: Uri): File? {
        val prepared = LocalArtworkProcessor(appContext).prepare(uri.toString()) ?: return null
        val bytes = prepared.bytes
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = profilePhotoSampleSize(bounds.outWidth, bounds.outHeight, PROFILE_PHOTO_EDGE_PX)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return null
        var square: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val crop = profilePhotoSquareCrop(decoded.width, decoded.height)
            if (crop.size <= 0) return null
            square = Bitmap.createBitmap(decoded, crop.left, crop.top, crop.size, crop.size)
            scaled = Bitmap.createScaledBitmap(square, PROFILE_PHOTO_EDGE_PX, PROFILE_PHOTO_EDGE_PX, true)
            if (!directory.isDirectory && !directory.mkdirs()) return null
            val temp = File(directory, "$PROFILE_PHOTO_FILE.tmp")
            val written = temp.outputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, PROFILE_PHOTO_JPEG_QUALITY, output)
            }
            if (!written || !temp.renameTo(file)) {
                temp.delete()
                return null
            }
            file
        } catch (error: IOException) {
            Timber.w(error, "Profile photo could not be stored")
            null
        } finally {
            if (scaled != null && scaled !== square && scaled !== decoded) scaled.recycle()
            if (square != null && square !== decoded) square.recycle()
            decoded.recycle()
        }
    }

    fun clear(): Boolean = !file.exists() || file.delete()
}
