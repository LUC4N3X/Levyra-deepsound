package com.luc4n3x.levyra.data.locallibrary

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.pxOrElse
import java.io.FileNotFoundException
import okio.buffer
import okio.source

private const val LOCAL_ARTWORK_SCHEME = "levyra-local-art"
private const val LOCAL_ARTWORK_PREFIX = "$LOCAL_ARTWORK_SCHEME://art?"
private const val DEFAULT_LOCAL_ARTWORK_PX = 512
private const val MIN_LOCAL_ARTWORK_PX = 96
private const val MAX_LOCAL_ARTWORK_PX = 1024
private val MEDIA_STORE_URI_PREFIX = "content://" + MediaStore.AUTHORITY + "/"
private val LEGACY_ALBUM_ART_URI by lazy(LazyThreadSafetyMode.NONE) {
    android.net.Uri.parse("content://media/external/audio/albumart")
}

fun localArtworkModel(contentUri: String, albumId: Long): String =
    LOCAL_ARTWORK_PREFIX + "a=" + albumId.coerceAtLeast(0L) + "&u=" + contentUri

fun isLocalArtworkModel(value: String): Boolean = value.startsWith(LOCAL_ARTWORK_PREFIX)

internal data class LocalArtworkReference(val contentUri: String, val albumId: Long)

internal fun parseLocalArtworkModel(value: String): LocalArtworkReference? {
    if (!isLocalArtworkModel(value)) return null
    val parameters = value.removePrefix(LOCAL_ARTWORK_PREFIX)
    val contentUri = parameters.substringAfter("&u=", "")
    val albumId = parameters.substringBefore("&u=").removePrefix("a=").toLongOrNull() ?: 0L
    if (!contentUri.startsWith(MEDIA_STORE_URI_PREFIX, ignoreCase = true)) return null
    return LocalArtworkReference(contentUri, albumId)
}

fun mediaSessionArtworkUri(artwork: String): android.net.Uri? {
    if (artwork.isBlank()) return null
    if (!isLocalArtworkModel(artwork)) return android.net.Uri.parse(artwork)
    val albumId = parseLocalArtworkModel(artwork)?.albumId?.takeIf { it > 0L } ?: return null
    return ContentUris.withAppendedId(LEGACY_ALBUM_ART_URI, albumId)
}

class LocalArtworkFetcher private constructor(
    private val reference: LocalArtworkReference,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val target = targetPixels()
            val bitmap = context.contentResolver.loadThumbnail(
                android.net.Uri.parse(reference.contentUri),
                Size(target, target),
                null
            )
            return ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
        }
        val albumId = reference.albumId.takeIf { it > 0L }
            ?: throw FileNotFoundException("No album artwork for local media")
        val stream = context.contentResolver.openInputStream(ContentUris.withAppendedId(LEGACY_ALBUM_ART_URI, albumId))
            ?: throw FileNotFoundException("Album artwork unavailable")
        return SourceFetchResult(
            source = ImageSource(source = stream.source().buffer(), fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    private fun targetPixels(): Int {
        val size = options.size
        if (size.width !is Dimension.Pixels && size.height !is Dimension.Pixels) return DEFAULT_LOCAL_ARTWORK_PX
        val requested = maxOf(
            size.width.pxOrElse { 0 },
            size.height.pxOrElse { 0 }
        )
        return requested.coerceIn(MIN_LOCAL_ARTWORK_PX, MAX_LOCAL_ARTWORK_PX)
    }

    class Factory(context: Context) : Fetcher.Factory<coil3.Uri> {
        private val appContext = context.applicationContext

        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != LOCAL_ARTWORK_SCHEME) return null
            val reference = parseLocalArtworkModel(data.toString()) ?: return null
            return LocalArtworkFetcher(reference, options, appContext)
        }
    }
}
