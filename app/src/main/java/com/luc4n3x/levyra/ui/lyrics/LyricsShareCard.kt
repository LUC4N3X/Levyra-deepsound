package com.luc4n3x.levyra.ui.lyrics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds a private, local-only image for the Android share sheet.
 *
 * The file lives under cache/share/lyrics and is exposed only through the
 * dedicated non-exported FileProvider declared in AndroidManifest.xml.
 */
internal object LyricsShareCard {
    private const val CARD_SIZE = 1440
    private const val MAX_SELECTED_LINES = 5
    private const val MAX_TEXT_CHARS = 900
    private const val MAX_CACHE_FILES = 12
    private const val BRAND = "LEVYRA"

    suspend fun createShareIntent(
        context: Context,
        track: Track,
        selectedLyrics: String
    ): Intent? = withContext(Dispatchers.IO) {
        val text = selectedLyrics.trim().take(MAX_TEXT_CHARS)
        if (text.isBlank()) return@withContext null

        val directory = File(context.cacheDir, "share/lyrics")
        if (!directory.exists() && !directory.mkdirs()) return@withContext null
        prune(directory)

        val cover = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?.takeIf(File::isFile)
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }
        val file = File(directory, "lyrics-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        val written = try {
            bitmap = render(track, text, cover)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (_: Exception) {
            false
        } finally {
            cover?.recycle()
            bitmap?.recycle()
        }
        if (!written || !file.isFile || file.length() <= 0L) {
            runCatching { file.delete() }
            return@withContext null
        }

        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.share-files",
                file
            )
        }.getOrNull() ?: return@withContext null

        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, buildShareCaption(track, text))
            clipData = ClipData.newUri(context.contentResolver, "lyrics", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun render(track: Track, selectedLyrics: String, cover: Bitmap?): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accentStart = opaque(track.accentStart, Color.rgb(38, 178, 214))
        val accentEnd = opaque(track.accentEnd, Color.rgb(111, 76, 255))

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                CARD_SIZE.toFloat(),
                CARD_SIZE.toFloat(),
                darken(accentStart, 0.70f),
                darken(accentEnd, 0.82f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(), background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                CARD_SIZE * 0.76f,
                CARD_SIZE * 0.20f,
                CARD_SIZE * 0.66f,
                withAlpha(accentStart, 92),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CARD_SIZE * 0.76f, CARD_SIZE * 0.20f, CARD_SIZE * 0.66f, glow)

        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(62, 255, 255, 255) }
        canvas.drawRoundRect(
            RectF(92f, 92f, CARD_SIZE - 92f, CARD_SIZE - 92f),
            76f,
            76f,
            panel
        )

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 37f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(BRAND, 150f, 176f, brandPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(184, 255, 255, 255)
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        val metadataWidth = if (cover != null) 820f else CARD_SIZE - 300f
        drawEllipsized(canvas, track.title.ifBlank { BRAND }, titlePaint, 150f, 266f, metadataWidth)
        drawEllipsized(canvas, track.artist, artistPaint, 150f, 318f, metadataWidth)
        if (cover != null) drawCover(canvas, cover)

        val rawLines = selectedLyrics.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(MAX_SELECTED_LINES)
            .toList()
        val lyricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val availableWidth = CARD_SIZE - 300f
        val availableHeight = CARD_SIZE - 570f
        val wrapped = fitLyrics(rawLines, lyricPaint, availableWidth, availableHeight)
        val lineHeight = lyricPaint.textSize * 1.28f
        val blockHeight = wrapped.size * lineHeight
        var y = ((CARD_SIZE + blockHeight) / 2f - blockHeight + 50f).coerceAtLeast(465f)
        wrapped.forEach { line ->
            canvas.drawText(line, 150f, y, lyricPaint)
            y += lineHeight
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 255, 255, 255)
            textSize = 27f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        canvas.drawText(BRAND, 150f, CARD_SIZE - 146f, footerPaint)
        return bitmap
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap) {
        val target = RectF(CARD_SIZE - 444f, 144f, CARD_SIZE - 144f, 444f)
        val path = Path().apply { addRoundRect(target, 42f, 42f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(cover, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(target, 42f, 42f, border)
    }

    private fun fitLyrics(
        source: List<String>,
        paint: Paint,
        maxWidth: Float,
        maxHeight: Float
    ): List<String> {
        var size = 70f
        while (size >= 42f) {
            paint.textSize = size
            val wrapped = source.flatMap { wrap(it, paint, maxWidth) }
            val height = wrapped.size * size * 1.28f
            if (wrapped.size <= 12 && height <= maxHeight) return wrapped
            size -= 4f
        }
        paint.textSize = 42f
        return source.flatMap { wrap(it, paint, maxWidth) }.take(12)
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        val lines = ArrayList<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun drawEllipsized(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float
    ) {
        if (text.isBlank()) return
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, y, paint)
            return
        }
        var candidate = text
        while (candidate.length > 1 && paint.measureText("$candidate…") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        canvas.drawText("$candidate…", x, y, paint)
    }

    private fun buildShareCaption(track: Track, lyrics: String): String = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(" — ").append(track.artist)
        append("\n\n")
        append(lyrics)
    }

    private fun prune(directory: File) {
        directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_CACHE_FILES - 1)
            ?.forEach { runCatching { it.delete() } }
    }

    private fun opaque(color: Int, fallback: Int): Int {
        val candidate = if (color == 0) fallback else color
        return Color.rgb(Color.red(candidate), Color.green(candidate), Color.blue(candidate))
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}
