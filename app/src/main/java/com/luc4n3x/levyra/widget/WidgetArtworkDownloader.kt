package com.luc4n3x.levyra.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

internal object WidgetArtworkDownloader {
    fun decode(url: String, minSide: Int): Bitmap? {
        val bytes = download(url) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= minSide && bounds.outHeight / (sample * 2) >= minSide) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun download(url: String): ByteArray? {
        val safeUrl = SafeImageUrlPolicy.sanitize(url)
        if (safeUrl.isEmpty()) return null
        var connection: HttpURLConnection? = null
        try {
            connection = URL(safeUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            if (!SafeImageUrlPolicy.isAllowedImageMimeType(connection.contentType)) return null
            val stream = connection.inputStream
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var totalRead = 0
            while (true) {
                val read = stream.read(chunk)
                if (read == -1) break
                totalRead += read
                if (totalRead > SafeImageUrlPolicy.MAX_IMAGE_PAYLOAD_BYTES) return null
                buffer.write(chunk, 0, read)
            }
            return buffer.toByteArray()
        } finally {
            connection?.disconnect()
        }
    }
}
