package com.luc4n3x.levyra.data.locallibrary

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import timber.log.Timber

internal class LocalMediaStoreScanner(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) == PackageManager.PERMISSION_GRANTED

    fun mountedVolumes(): Set<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching { MediaStore.getExternalVolumeNames(context) }
            .getOrDefault(setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY))
            .mapTo(linkedSetOf()) { it.lowercase(Locale.ROOT) }
    } else {
        setOf(LEGACY_VOLUME)
    }

    fun changeToken(volumes: Set<String>): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            volumes.sorted().joinToString("|") { volume ->
                "$volume=${MediaStore.getVersion(context, volume)}:${MediaStore.getGeneration(context, volume)}"
            }
        }.getOrNull()
    }

    suspend fun scan(volumes: Set<String>): List<ScannedLocalAudio>? {
        val results = ArrayList<ScannedLocalAudio>()
        for (volume in volumes) {
            coroutineContext.ensureActive()
            results += queryVolume(volume) ?: return null
        }
        return results
    }

    private suspend fun queryVolume(volume: String): List<ScannedLocalAudio>? {
        val collection = collectionFor(volume)
        val (selection, args) = selection()
        return try {
            context.contentResolver.query(collection, projection(), selection, args, null)?.use { cursor ->
                readRows(cursor, volume, collection)
            }
        } catch (denied: SecurityException) {
            Timber.w("Local media query denied for volume %s", volume)
            null
        } catch (unavailable: IllegalArgumentException) {
            Timber.w(unavailable, "Local media volume unavailable")
            emptyList()
        }
    }

    private suspend fun readRows(cursor: Cursor, volume: String, collection: Uri): List<ScannedLocalAudio> {
        val columns = ScanColumns(cursor)
        val rows = ArrayList<ScannedLocalAudio>(cursor.count.coerceAtLeast(0))
        while (cursor.moveToNext()) {
            if (rows.size % CANCELLATION_CHECK_INTERVAL == 0) coroutineContext.ensureActive()
            val id = cursor.getLong(columns.id)
            val filePath = cursor.stringOrEmpty(columns.data)
            val relativePath = if (columns.relativePath >= 0) {
                cursor.stringOrEmpty(columns.relativePath)
            } else {
                relativePathFromFilePath(filePath)
            }
            rows += ScannedLocalAudio(
                volumeName = volume,
                mediaStoreId = id,
                contentUri = ContentUris.withAppendedId(collection, id).toString(),
                filePath = filePath,
                relativePath = relativePath,
                displayName = cursor.stringOrEmpty(columns.displayName),
                title = cursor.stringOrEmpty(columns.title),
                artist = cursor.stringOrEmpty(columns.artist),
                album = cursor.stringOrEmpty(columns.album),
                albumArtist = cursor.stringOrEmpty(columns.albumArtist),
                genre = cursor.stringOrEmpty(columns.genre),
                year = cursor.intOrZero(columns.year),
                trackField = cursor.intOrZero(columns.track),
                trackText = cursor.stringOrEmpty(columns.cdTrack),
                discText = cursor.stringOrEmpty(columns.disc),
                durationMs = cursor.longOrZero(columns.duration),
                mimeType = cursor.stringOrEmpty(columns.mimeType),
                bitrate = cursor.intOrZero(columns.bitrate),
                sizeBytes = cursor.longOrZero(columns.size),
                dateAddedMs = cursor.longOrZero(columns.dateAdded) * 1_000L,
                dateModifiedMs = cursor.longOrZero(columns.dateModified) * 1_000L,
                albumId = cursor.longOrZero(columns.albumId)
            )
        }
        return rows
    }

    private fun collectionFor(volume: String): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(volume)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    @Suppress("DEPRECATION")
    private fun projection(): Array<String> = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.DATA)
        add(MediaStore.Audio.Media.DISPLAY_NAME)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.ALBUM_ID)
        add(MediaStore.Audio.Media.YEAR)
        add(MediaStore.Audio.Media.TRACK)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.MIME_TYPE)
        add(MediaStore.Audio.Media.SIZE)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DATE_MODIFIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(MediaStore.Audio.Media.ALBUM_ARTIST)
            add(MediaStore.Audio.Media.GENRE)
            add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
            add(MediaStore.Audio.Media.DISC_NUMBER)
            add(MediaStore.Audio.Media.BITRATE)
        }
    }.toTypedArray()

    @Suppress("DEPRECATION")
    private fun selection(): Pair<String, Array<String>> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?) AND " +
            "${MediaStore.Audio.Media.SIZE} > 0 AND ${MediaStore.Audio.Media.IS_PENDING} = 0" to
            arrayOf("$LEVYRA_DOWNLOAD_RELATIVE_PATH%")
    } else {
        "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DATA} LIKE ?) AND " +
            "${MediaStore.Audio.Media.SIZE} > 0" to
            arrayOf("%/$LEVYRA_DOWNLOAD_RELATIVE_PATH%")
    }

    private class ScanColumns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        @Suppress("DEPRECATION")
        val data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val displayName = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val album = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val albumId = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val year = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val track = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val mimeType = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val size = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val dateAdded = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModified = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val relativePath = cursor.getColumnIndex(COLUMN_RELATIVE_PATH)
        val albumArtist = cursor.getColumnIndex(COLUMN_ALBUM_ARTIST)
        val genre = cursor.getColumnIndex(COLUMN_GENRE)
        val cdTrack = cursor.getColumnIndex(COLUMN_CD_TRACK_NUMBER)
        val disc = cursor.getColumnIndex(COLUMN_DISC_NUMBER)
        val bitrate = cursor.getColumnIndex(COLUMN_BITRATE)
    }

    companion object {
        const val LEGACY_VOLUME = "external"
        private const val CANCELLATION_CHECK_INTERVAL = 256
        private const val COLUMN_RELATIVE_PATH = "relative_path"
        private const val COLUMN_ALBUM_ARTIST = "album_artist"
        private const val COLUMN_GENRE = "genre"
        private const val COLUMN_CD_TRACK_NUMBER = "cd_track_number"
        private const val COLUMN_DISC_NUMBER = "disc_number"
        private const val COLUMN_BITRATE = "bitrate"

        fun requiredPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

private fun Cursor.stringOrEmpty(index: Int): String =
    if (index < 0 || isNull(index)) "" else getString(index).orEmpty()

private fun Cursor.longOrZero(index: Int): Long =
    if (index < 0 || isNull(index)) 0L else runCatching { getLong(index) }.getOrDefault(0L)

private fun Cursor.intOrZero(index: Int): Int =
    if (index < 0 || isNull(index)) 0 else runCatching { getInt(index) }.getOrDefault(0)
