package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val MAX_PLAYLIST_COVER_BACKUP_BYTES = 4 * 1024 * 1024
private const val MAX_PLAYLIST_COVER_SOURCE_BYTES = 32L * 1024L * 1024L
private const val PLAYLIST_COVER_OUTPUT_PX = 1024
private const val PLAYLIST_COVER_DECODE_PX = 2048
private const val PLAYLIST_COVER_QUALITY = 90
private const val PLAYLIST_COVER_DIRECTORY = "playlist_covers"
private val PlaylistCoverEntryPattern = Regex("data/playlist_covers/[a-f0-9]{64}\\.jpg")

data class PlaylistCoverCrop(
    val viewportSizePx: Int,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

internal data class PlaylistCoverCropRect(
    val left: Int,
    val top: Int,
    val size: Int
)

internal fun playlistCoverCropRect(
    imageWidth: Int,
    imageHeight: Int,
    crop: PlaylistCoverCrop
): PlaylistCoverCropRect {
    require(imageWidth > 0 && imageHeight > 0 && crop.viewportSizePx > 0)
    val viewport = crop.viewportSizePx.toFloat()
    val baseScale = max(viewport / imageWidth, viewport / imageHeight)
    val effectiveScale = baseScale * crop.zoom.coerceIn(1f, 4f)
    val size = (viewport / effectiveScale).roundToInt().coerceIn(1, minOf(imageWidth, imageHeight))
    val half = size / 2f
    val centerX = (imageWidth / 2f - crop.offsetX / effectiveScale).coerceIn(half, imageWidth - half)
    val centerY = (imageHeight / 2f - crop.offsetY / effectiveScale).coerceIn(half, imageHeight - half)
    return PlaylistCoverCropRect(
        left = (centerX - half).roundToInt().coerceIn(0, imageWidth - size),
        top = (centerY - half).roundToInt().coerceIn(0, imageHeight - size),
        size = size
    )
}

internal fun playlistCoverMaxOffset(
    imageWidth: Int,
    imageHeight: Int,
    viewportSizePx: Int,
    zoom: Float
): Pair<Float, Float> {
    if (imageWidth <= 0 || imageHeight <= 0 || viewportSizePx <= 0) return 0f to 0f
    val viewport = viewportSizePx.toFloat()
    val scale = max(viewport / imageWidth, viewport / imageHeight) * zoom.coerceIn(1f, 4f)
    return ((imageWidth * scale - viewport) / 2f).coerceAtLeast(0f) to
        ((imageHeight * scale - viewport) / 2f).coerceAtLeast(0f)
}

internal fun playlistCoverBackupEntry(playlistId: String): String =
    "data/playlist_covers/${sha256(playlistId.toByteArray(Charsets.UTF_8))}.jpg"

internal fun playlistCoverBackupEntryAllowed(name: String): Boolean =
    PlaylistCoverEntryPattern.matches(name)

internal fun playlistCoverPayloadAccepted(bytes: ByteArray): Boolean =
    bytes.size in 3..MAX_PLAYLIST_COVER_BACKUP_BYTES &&
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

internal class PlaylistCoverStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, PLAYLIST_COVER_DIRECTORY)

    suspend fun save(playlistId: String, source: Uri, crop: PlaylistCoverCrop): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        if (!directory.isDirectory) throw IOException("Unable to create playlist cover storage")
        val sourceFile = File(appContext.cacheDir, "playlist-cover-${UUID.randomUUID()}.tmp")
        try {
            copySource(source, sourceFile)
            val request = ImageRequest.Builder(appContext)
                .data(sourceFile)
                .size(Size(PLAYLIST_COVER_DECODE_PX, PLAYLIST_COVER_DECODE_PX))
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(appContext).execute(request) as? SuccessResult
                ?: throw IOException("Unable to decode playlist cover")
            val bitmap = result.image.toBitmap()
            val rect = playlistCoverCropRect(bitmap.width, bitmap.height, crop)
            val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.size, rect.size)
            val outputSize = minOf(PLAYLIST_COVER_OUTPUT_PX, rect.size)
            val output = if (cropped.width == outputSize) cropped else {
                Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)
            }
            val target = targetFile(playlistId)
            writeBitmap(target, output)
            Uri.fromFile(target).toString()
        } finally {
            sourceFile.delete()
        }
    }

    fun readBackup(reference: String): ByteArray? {
        val file = ownedFile(reference) ?: return null
        if (!file.isFile || file.length() !in 1..MAX_PLAYLIST_COVER_BACKUP_BYTES.toLong()) return null
        return file.readBytes().takeIf(::playlistCoverPayloadAccepted)
    }

    fun restore(playlistId: String, bytes: ByteArray): String {
        if (!playlistCoverPayloadAccepted(bytes)) throw IOException("Invalid playlist cover payload")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..PLAYLIST_COVER_DECODE_PX || bounds.outHeight !in 1..PLAYLIST_COVER_DECODE_PX) {
            throw IOException("Invalid playlist cover dimensions")
        }
        directory.mkdirs()
        val target = targetFile(playlistId)
        val atomic = AtomicFile(target)
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(bytes)
            atomic.finishWrite(output)
            output = null
        } catch (error: Throwable) {
            output?.let(atomic::failWrite)
            throw error
        }
        return Uri.fromFile(target).toString()
    }

    fun delete(reference: String) {
        ownedFile(reference)?.delete()
    }

    fun prune(references: Set<String>) {
        val keep = references.mapNotNull(::ownedFile).mapTo(hashSetOf()) { it.name }
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.name !in keep && File(directory, file.name).canonicalFile.parentFile == directory.canonicalFile) {
                file.delete()
            }
        }
    }

    private fun copySource(source: Uri, target: File) {
        val input = appContext.contentResolver.openInputStream(source)
            ?: throw IOException("Unable to open selected playlist cover")
        input.use { sourceStream ->
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = sourceStream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_PLAYLIST_COVER_SOURCE_BYTES) throw IOException("Selected image is too large")
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    private fun writeBitmap(target: File, bitmap: Bitmap) {
        val atomic = AtomicFile(target)
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PLAYLIST_COVER_QUALITY, output)) {
                throw IOException("Unable to encode playlist cover")
            }
            atomic.finishWrite(output)
            output = null
            if (target.length() > MAX_PLAYLIST_COVER_BACKUP_BYTES) {
                target.delete()
                throw IOException("Encoded playlist cover is too large")
            }
        } catch (error: Throwable) {
            output?.let(atomic::failWrite)
            throw error
        }
    }

    private fun targetFile(playlistId: String): File =
        File(directory, playlistCoverBackupEntry(playlistId).substringAfterLast('/'))

    private fun ownedFile(reference: String): File? {
        if (reference.isBlank()) return null
        val uri = runCatching { Uri.parse(reference) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val file = File(path).canonicalFile
        return file.takeIf { it.parentFile == directory.canonicalFile && it.name.matches(Regex("[a-f0-9]{64}\\.jpg")) }
    }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
