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
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
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
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Renders the lyric share card locally and hands it to the Android share sheet.
 *
 * Exported files live under cache/share/lyrics and are exposed only through the
 * dedicated non-exported FileProvider declared in AndroidManifest.xml.
 */
internal object LyricsShareCard {
    const val EXPORT_WIDTH_PX = 1080
    const val PREVIEW_WIDTH_PX = 540

    private const val ASPECT_WIDTH = 4
    private const val ASPECT_HEIGHT = 5
    private const val DESIGN_WIDTH = 1440f
    private const val DESIGN_HEIGHT = DESIGN_WIDTH * ASPECT_HEIGHT / ASPECT_WIDTH
    private const val MARGIN = 120f
    private const val COVER_SIZE = 220f
    private const val COVER_RADIUS = 44f
    private const val LYRICS_TOP = 430f
    private const val LYRICS_BOTTOM = 1540f
    private const val FOOTER_BASELINE = 1690f
    private const val LYRICS_START_SIZE = 96f
    private const val LYRICS_MIN_SIZE = 52f
    private const val LYRICS_SIZE_STEP = 4f
    private const val LYRICS_MAX_LAYOUT_LINES = 12
    private const val LYRICS_LINE_SPACING = 1.30f
    private const val BLUR_STEP_WIDTH = 72
    private const val BLUR_STEP_HEIGHT = 90
    private const val BLUR_FINAL_WIDTH = 18
    private const val BLUR_FINAL_HEIGHT = 23
    private const val COVER_TARGET_SIZE = 300
    private const val ARTWORK_TIMEOUT_MS = 6_000L
    private const val MAX_CACHE_FILES = 4
    private const val MAX_CACHE_AGE_MS = 24L * 60L * 60L * 1000L
    private const val SHARE_DIRECTORY = "share/lyrics"
    private const val BRAND = "LEVYRA"
    private const val FALLBACK_START = 0xFF26B2D6.toInt()
    private const val FALLBACK_END = 0xFF6F4CFF.toInt()
    private const val MINIMAL_BACKGROUND = 0xFF0C0D12.toInt()

    fun heightFor(widthPx: Int): Int = widthPx * ASPECT_HEIGHT / ASPECT_WIDTH

    suspend fun loadArtwork(
        context: Context,
        track: Track,
        into: LyricsShareResource<Bitmap>
    ): Unit = withContext(Dispatchers.IO) {
        ensureActive()
        val file = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?: LevyraArtworkCache.localFile(context, track, highRes = false)
            ?: run {
                withTimeoutOrNull(ARTWORK_TIMEOUT_MS) {
                    LevyraArtworkCache.cachePersistent(context, listOf(track), limit = 1)
                }
                LevyraArtworkCache.localFile(context, track, highRes = true)
                    ?: LevyraArtworkCache.localFile(context, track, highRes = false)
            }
        ensureActive()
        val cover = file?.takeIf(File::isFile)?.let(::decodeCover)
        if (cover != null) into.set(cover)
    }

    suspend fun createShareIntent(
        context: Context,
        content: LyricsShareCardContent,
        artwork: LyricsShareResource<Bitmap>,
        style: LyricsShareCardStyle,
        accents: Pair<Int, Int>
    ): Intent? = withContext(Dispatchers.IO) {
        if (content.lyrics.isEmpty()) return@withContext null
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
        prune(directory)

        val file = File(directory, "lyrics-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        var written = false
        try {
            ensureActive()
            val rendered = artwork.use { cover ->
                render(content, cover, style, accents, EXPORT_WIDTH_PX)
            }
            bitmap = rendered
            ensureActive()
            written = rendered != null && FileOutputStream(file).use { output ->
                rendered.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            written = false
        } finally {
            bitmap?.recycle()
            if (!written || !file.isFile || file.length() <= 0L) {
                written = false
                file.delete()
            }
        }
        if (!written) return@withContext null

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
        } catch (_: IllegalArgumentException) {
            file.delete()
            return@withContext null
        }

        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, lyricsShareCaption(content))
            clipData = ClipData.newUri(context.contentResolver, "lyrics", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun resolveAccents(track: Track, artwork: Bitmap?): Pair<Int, Int> {
        val start = LyricsShareCardColors.background(track.accentStart, FALLBACK_START)
        val end = LyricsShareCardColors.background(track.accentEnd, FALLBACK_END)
        val palette = artwork?.let {
            ArtworkPaletteCache.extract(bitmap = it, fallbackStart = start, fallbackEnd = end)
        } ?: return start to end
        return LyricsShareCardColors.background(palette.start, start) to
            LyricsShareCardColors.background(palette.end, end)
    }

    fun render(
        content: LyricsShareCardContent,
        artwork: Bitmap?,
        style: LyricsShareCardStyle,
        accents: Pair<Int, Int>,
        widthPx: Int
    ): Bitmap? {
        val bitmap = try {
            Bitmap.createBitmap(widthPx, heightFor(widthPx), Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return null
        }
        var completed = false
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(widthPx / DESIGN_WIDTH, widthPx / DESIGN_WIDTH)
            val showArtwork = artwork != null && style != LyricsShareCardStyle.MINIMAL
            drawBackground(canvas, style, artwork, accents)
            drawHeader(canvas, content, artwork.takeIf { showArtwork })
            drawLyrics(canvas, content.lyrics)
            drawFooter(canvas)
            completed = true
            return bitmap
        } finally {
            if (!completed) bitmap.recycle()
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        style: LyricsShareCardStyle,
        artwork: Bitmap?,
        accents: Pair<Int, Int>
    ) {
        val (start, end) = accents
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (style == LyricsShareCardStyle.MINIMAL) {
            canvas.drawColor(MINIMAL_BACKGROUND)
            paint.shader = RadialGradient(
                DESIGN_WIDTH * 0.85f,
                0f,
                DESIGN_WIDTH * 0.9f,
                withAlpha(start, 70),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
            return
        }
        paint.shader = LinearGradient(
            0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, start, end, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)

        if (style == LyricsShareCardStyle.ARTWORK && artwork != null) {
            drawSoftArtwork(canvas, artwork)
        } else {
            paint.shader = RadialGradient(
                DESIGN_WIDTH * 0.80f,
                DESIGN_HEIGHT * 0.12f,
                DESIGN_WIDTH * 0.75f,
                withAlpha(lighten(start), 90),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
        }

        paint.shader = RadialGradient(
            DESIGN_WIDTH / 2f,
            DESIGN_HEIGHT / 2f,
            DESIGN_HEIGHT * 0.78f,
            Color.TRANSPARENT,
            Color.argb(120, 0, 0, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT, paint)
    }

    private fun drawSoftArtwork(canvas: Canvas, artwork: Bitmap) {
        var stepped: Bitmap? = null
        var soft: Bitmap? = null
        try {
            stepped = Bitmap.createScaledBitmap(artwork, BLUR_STEP_WIDTH, BLUR_STEP_HEIGHT, true)
            soft = Bitmap.createScaledBitmap(stepped, BLUR_FINAL_WIDTH, BLUR_FINAL_HEIGHT, true)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 115 }
            canvas.drawBitmap(soft, null, RectF(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT), paint)
            canvas.drawColor(Color.argb(100, 0, 0, 0))
        } catch (_: OutOfMemoryError) {
            canvas.drawColor(Color.argb(60, 0, 0, 0))
        } finally {
            if (stepped != null && stepped !== artwork && stepped !== soft) stepped.recycle()
            if (soft != null && soft !== artwork) soft.recycle()
        }
    }

    private fun drawHeader(canvas: Canvas, content: LyricsShareCardContent, artwork: Bitmap?) {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val textX = if (artwork != null) MARGIN + COVER_SIZE + 44f else MARGIN
        val textWidth = DESIGN_WIDTH - MARGIN - textX
        val hasArtist = content.artist.isNotBlank()
        val titleBaseline = if (artwork != null) {
            MARGIN + COVER_SIZE / 2f + if (hasArtist) -6f else 20f
        } else {
            MARGIN + 70f
        }
        drawEllipsized(canvas, content.title.ifBlank { BRAND }, titlePaint, textX, titleBaseline, textWidth)
        if (hasArtist) {
            drawEllipsized(canvas, content.artist, artistPaint, textX, titleBaseline + 62f, textWidth)
        }
        artwork?.let {
            drawCover(canvas, it, RectF(MARGIN, MARGIN, MARGIN + COVER_SIZE, MARGIN + COVER_SIZE))
        }
    }

    private fun drawLyrics(canvas: Canvas, lines: List<String>) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val width = (DESIGN_WIDTH - MARGIN * 2f).toInt()
        val availableHeight = LYRICS_BOTTOM - LYRICS_TOP
        val text = lines.joinToString("\n")
        val layout = fitLyrics(text, paint, width, availableHeight)
        val top = LYRICS_TOP + ((availableHeight - layout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(MARGIN, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawFooter(canvas: Canvas) {
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(46, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN, FOOTER_BASELINE - 78f, DESIGN_WIDTH - MARGIN, FOOTER_BASELINE - 78f, divider)
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 40f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(BRAND, MARGIN, FOOTER_BASELINE, brand)
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap, target: RectF) {
        val path = Path().apply { addRoundRect(target, COVER_RADIUS, COVER_RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(cover, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(target, COVER_RADIUS, COVER_RADIUS, border)
    }

    private fun fitLyrics(text: String, paint: TextPaint, width: Int, maxHeight: Float): StaticLayout {
        var size = LYRICS_START_SIZE
        while (size >= LYRICS_MIN_SIZE) {
            paint.textSize = size
            val layout = buildLyricsLayout(text, paint, width, Int.MAX_VALUE)
            if (layout.lineCount <= LYRICS_MAX_LAYOUT_LINES && layout.height <= maxHeight) return layout
            size -= LYRICS_SIZE_STEP
        }
        paint.textSize = LYRICS_MIN_SIZE
        return buildLyricsLayout(text, paint, width, LYRICS_MAX_LAYOUT_LINES)
    }

    private fun buildLyricsLayout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(
                if (isRtlText(text)) TextDirectionHeuristics.FIRSTSTRONG_RTL
                else TextDirectionHeuristics.FIRSTSTRONG_LTR
            )
            .setIncludePad(false)
            .setLineSpacing(0f, LYRICS_LINE_SPACING)
            .setMaxLines(maxLines)
        if (maxLines != Int.MAX_VALUE) {
            builder.setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width)
        }
        return builder.build()
    }

    private fun decodeCover(file: File): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply {
                inSampleSize = coverSampleSize(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }
    } catch (_: OutOfMemoryError) {
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

    private fun prune(directory: File) {
        val now = System.currentTimeMillis()
        directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.forEachIndexed { position, file ->
                if (position >= MAX_CACHE_FILES - 1 || now - file.lastModified() > MAX_CACHE_AGE_MS) {
                    file.delete()
                }
            }
    }

    private fun lighten(color: Int): Int = Color.rgb(
        (Color.red(color) + 70).coerceAtMost(255),
        (Color.green(color) + 70).coerceAtMost(255),
        (Color.blue(color) + 70).coerceAtMost(255)
    )

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
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
