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
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Builds a private, local-only image for the Android share sheet.
 *
 * The file lives under cache/share/lyrics and is exposed only through the
 * dedicated non-exported FileProvider declared in AndroidManifest.xml.
 */
internal enum class LyricsShareFormat(val widthPx: Int, val heightPx: Int) {
    SQUARE(1080, 1080),
    STORY(1080, 1920)
}

internal object LyricsShareCard {
    private const val DESIGN_WIDTH = 1440f
    private const val MAX_SELECTED_LINES = 8
    private const val MAX_TEXT_CODE_POINTS = 900
    private const val MAX_CACHE_FILES = 10
    private const val MAX_LAYOUT_LINES = 14
    private const val BRAND = "LEVYRA"

    suspend fun createShareIntent(
        context: Context,
        track: Track,
        selectedLyrics: String,
        format: LyricsShareFormat = LyricsShareFormat.SQUARE
    ): Intent? = withContext(Dispatchers.IO) {
        val text = boundedLyricsShareText(selectedLyrics, MAX_TEXT_CODE_POINTS)
        if (text.isBlank()) return@withContext null

        val directory = File(context.cacheDir, "share/lyrics")
        if (!directory.exists() && !directory.mkdirs()) return@withContext null
        prune(directory)

        currentCoroutineContext().ensureActive()
        val coverFile = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?: run {
                LevyraArtworkCache.cachePersistent(context, listOf(track), limit = 1)
                LevyraArtworkCache.localFile(context, track, highRes = true)
            }
        val cover = coverFile
            ?.takeIf(File::isFile)
            ?.let(::decodeCover)
        val file = File(directory, "lyrics-${format.name.lowercase(Locale.ROOT)}-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        val written = try {
            currentCoroutineContext().ensureActive()
            bitmap = render(track, text, cover, format)
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

    internal fun render(
        track: Track,
        selectedLyrics: String,
        cover: Bitmap?,
        format: LyricsShareFormat
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(format.widthPx, format.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cardScale = format.widthPx / DESIGN_WIDTH
        val designHeight = format.heightPx / cardScale
        canvas.scale(cardScale, cardScale)
        val palette = cover?.let {
            ArtworkPaletteCache.extract(
                bitmap = it,
                fallbackStart = opaque(track.accentStart, Color.rgb(38, 178, 214)),
                fallbackEnd = opaque(track.accentEnd, Color.rgb(111, 76, 255))
            )
        }
        val accentStart = opaque(palette?.start ?: track.accentStart, Color.rgb(38, 178, 214))
        val accentEnd = opaque(palette?.end ?: track.accentEnd, Color.rgb(111, 76, 255))

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                DESIGN_WIDTH,
                designHeight,
                darken(accentStart, 0.70f),
                darken(accentEnd, 0.82f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, DESIGN_WIDTH, designHeight, background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                DESIGN_WIDTH * 0.76f,
                designHeight * 0.18f,
                DESIGN_WIDTH * 0.72f,
                withAlpha(accentStart, 92),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(DESIGN_WIDTH * 0.76f, designHeight * 0.18f, DESIGN_WIDTH * 0.72f, glow)

        val lowerGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                DESIGN_WIDTH * 0.16f,
                designHeight * 0.82f,
                DESIGN_WIDTH * 0.82f,
                withAlpha(accentEnd, 72),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(DESIGN_WIDTH * 0.16f, designHeight * 0.82f, DESIGN_WIDTH * 0.82f, lowerGlow)

        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(62, 255, 255, 255) }
        canvas.drawRoundRect(
            RectF(92f, 92f, DESIGN_WIDTH - 92f, designHeight - 92f),
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

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(184, 255, 255, 255)
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        val story = format == LyricsShareFormat.STORY
        val titleY = if (story) 1_010f else 266f
        val artistY = if (story) 1_068f else 318f
        val metadataWidth = if (story || cover == null) DESIGN_WIDTH - 300f else 820f
        drawEllipsized(canvas, track.title.ifBlank { BRAND }, titlePaint, 150f, titleY, metadataWidth)
        drawEllipsized(canvas, track.artist, artistPaint, 150f, artistY, metadataWidth)
        if (cover != null) {
            val target = if (story) {
                RectF(420f, 250f, 1_020f, 850f)
            } else {
                RectF(DESIGN_WIDTH - 444f, 144f, DESIGN_WIDTH - 144f, 444f)
            }
            drawCover(canvas, cover, target, if (story) 64f else 42f)
        }

        val rawLines = selectedLyrics.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(MAX_SELECTED_LINES)
            .toList()
        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val availableWidth = DESIGN_WIDTH - 300f
        val lyricTop = if (story) 1_230f else 465f
        val lyricBottom = if (story) designHeight - 300f else designHeight - 260f
        val availableHeight = lyricBottom - lyricTop
        val lyricsLayout = fitLyrics(
            source = rawLines.joinToString("\n"),
            paint = lyricPaint,
            maxWidth = availableWidth,
            maxHeight = availableHeight,
            startSize = if (story) 86f else 70f,
            minimumSize = if (story) 46f else 42f,
            maxLines = if (story) 18 else MAX_LAYOUT_LINES
        )
        val y = (lyricTop + (availableHeight - lyricsLayout.height) / 2f).coerceAtLeast(lyricTop)
        canvas.save()
        canvas.translate(150f, y)
        lyricsLayout.draw(canvas)
        canvas.restore()

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 255, 255, 255)
            textSize = 27f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        canvas.drawText(BRAND, 150f, designHeight - 146f, footerPaint)
        return bitmap
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap, target: RectF, radius: Float) {
        val path = Path().apply { addRoundRect(target, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(cover, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(target, radius, radius, border)
    }

    private fun fitLyrics(
        source: String,
        paint: TextPaint,
        maxWidth: Float,
        maxHeight: Float,
        startSize: Float,
        minimumSize: Float,
        maxLines: Int
    ): StaticLayout {
        var size = startSize
        while (size >= minimumSize) {
            paint.textSize = size
            val layout = buildLyricsLayout(source, paint, maxWidth.toInt())
            if (layout.lineCount <= maxLines && layout.height <= maxHeight) return layout
            size -= 4f
        }
        paint.textSize = minimumSize
        return buildLyricsLayout(source, paint, maxWidth.toInt(), maxLines = maxLines)
    }

    private fun buildLyricsLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int = Int.MAX_VALUE
    ): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(
                if (isRtlText(text)) TextDirectionHeuristics.FIRSTSTRONG_RTL
                else TextDirectionHeuristics.FIRSTSTRONG_LTR
            )
            .setIncludePad(false)
            .setLineSpacing(0f, 1.36f)
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
        paint: TextPaint,
        x: Float,
        y: Float,
        maxWidth: Float
    ) {
        if (text.isBlank()) return
        val candidate = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        val rtl = isRtlText(text)
        paint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        canvas.drawText(candidate, if (rtl) x + maxWidth else x, y, paint)
        paint.textAlign = Paint.Align.LEFT
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

    private const val COVER_TARGET_SIZE = 300
}

internal fun boundedLyricsShareText(source: String, maxCodePoints: Int): String {
    val trimmed = source.trim()
    if (trimmed.isEmpty() || maxCodePoints <= 0) return ""
    val count = trimmed.codePointCount(0, trimmed.length)
    if (count <= maxCodePoints) return trimmed
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, maxCodePoints)).trimEnd()
}

internal fun isRtlText(text: String): Boolean {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
        }
        index += Character.charCount(codePoint)
    }
    return false
}
