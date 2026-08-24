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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Builds a private, local-only 1080x1080 image card for social sharing.
 *
 * The file lives under cache/share/lyrics and is exposed only through the
 * dedicated non-exported FileProvider declared in AndroidManifest.xml.
 */
internal object LyricsShareCard {
    const val CARD_SIZE = 1080
    private const val MAX_SELECTED_LINES = 8
    private const val MAX_TEXT_CHARS = 1000
    private const val MAX_CACHE_FILES = 10
    private const val BRAND = "LEVYRA"

    fun buildShareText(track: Track, selectedLyrics: String): String = buildShareCaption(track, selectedLyrics)

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

        currentCoroutineContext().ensureActive()
        val cover = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?.takeIf(File::isFile)
            ?.let(::decodeCover)
        val file = File(directory, "lyrics-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        val written = try {
            currentCoroutineContext().ensureActive()
            bitmap = render(track, text, cover)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: OutOfMemoryError) {
            false
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

        // Background: deep artwork gradient
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                CARD_SIZE.toFloat(),
                CARD_SIZE.toFloat(),
                darken(accentStart, 0.58f),
                darken(accentEnd, 0.76f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(), background)

        // Soft atmospheric glow
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                CARD_SIZE * 0.78f,
                CARD_SIZE * 0.18f,
                CARD_SIZE * 0.62f,
                withAlpha(accentStart, 85),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CARD_SIZE * 0.78f, CARD_SIZE * 0.18f, CARD_SIZE * 0.62f, glow)

        // Glassmorphic panel
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(48, 255, 255, 255) }
        canvas.drawRoundRect(
            RectF(64f, 64f, CARD_SIZE - 64f, CARD_SIZE - 64f),
            56f,
            56f,
            panel
        )

        // Brand pill top left
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255)
            textSize = 26f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(BRAND, 108f, 134f, brandPaint)

        // Title and artist
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 255, 255, 255)
            textSize = 26f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        val metadataWidth = if (cover != null) 600f else CARD_SIZE - 216f
        drawEllipsized(canvas, track.title.ifBlank { BRAND }, titlePaint, 108f, 204f, metadataWidth)
        drawEllipsized(canvas, track.artist, artistPaint, 108f, 246f, metadataWidth)
        if (cover != null) drawCover(canvas, cover)

        // Selected lyrics
        val rawLines = selectedLyrics.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(MAX_SELECTED_LINES)
            .toList()
        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val availableWidth = CARD_SIZE - 216f
        val availableHeight = CARD_SIZE - 430f
        val lyricsLayout = fitLyrics(rawLines.joinToString("\n"), lyricPaint, availableWidth, availableHeight)
        val y = ((CARD_SIZE - lyricsLayout.height) / 2f + 40f).coerceAtLeast(350f)
        canvas.save()
        canvas.translate(108f, y)
        lyricsLayout.draw(canvas)
        canvas.restore()

        // Footer brand
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(135, 255, 255, 255)
            textSize = 22f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        canvas.drawText(BRAND, 108f, CARD_SIZE - 108f, footerPaint)
        return bitmap
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap) {
        val target = RectF(CARD_SIZE - 316f, 104f, CARD_SIZE - 108f, 312f)
        val path = Path().apply { addRoundRect(target, 32f, 32f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(cover, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(target, 32f, 32f, border)
    }

    private fun fitLyrics(
        source: String,
        paint: TextPaint,
        maxWidth: Float,
        maxHeight: Float
    ): StaticLayout {
        var size = 54f
        while (size >= 28f) {
            paint.textSize = size
            val layout = buildLyricsLayout(source, paint, maxWidth.toInt())
            if (layout.lineCount <= 12 && layout.height <= maxHeight) return layout
            size -= 3f
        }
        paint.textSize = 28f
        return buildLyricsLayout(source, paint, maxWidth.toInt(), maxLines = 12)
    }

    private fun buildLyricsLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int = Int.MAX_VALUE
    ): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.28f)
            .setMaxLines(maxLines)
        if (maxLines != Int.MAX_VALUE) {
            builder.setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width)
        }
        return builder.build()
    }

    private fun decodeCover(file: File): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = coverSampleSize(bounds.outWidth, bounds.outHeight)
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: Exception) {
        null
    }

    internal fun coverSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > COVER_TARGET_SIZE * 2) sample *= 2
        return sample
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

    private const val COVER_TARGET_SIZE = 250
}
