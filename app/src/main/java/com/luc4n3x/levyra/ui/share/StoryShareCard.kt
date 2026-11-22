package com.luc4n3x.levyra.ui.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal enum class StoryTemplate { Aura, Vinyl, Poster, Glass }

internal enum class StoryTone { Artwork, Midnight }

internal data class StoryShareOptions(
    val template: StoryTemplate = StoryTemplate.Aura,
    val tone: StoryTone = StoryTone.Artwork,
    val showProgress: Boolean = true,
    val showBrand: Boolean = true
)

internal data class StoryShareContent(
    val title: String,
    val artist: String,
    val album: String,
    val positionMs: Long,
    val durationMs: Long,
    val eyebrow: String
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal class StoryShareColors(val start: Int, val end: Int) {
    val deep: Int = ColorUtils.blendARGB(start, Color.BLACK, 0.72f)
    val ink: Int = ColorUtils.blendARGB(end, Color.BLACK, 0.86f)
    val glow: Int = ColorUtils.blendARGB(start, Color.WHITE, 0.18f)
    val accent: Int = readableAccent(ColorUtils.blendARGB(start, end, 0.35f))

    private fun readableAccent(color: Int): Int {
        var candidate = color
        var step = 0
        while (ColorUtils.calculateContrast(candidate, Color.rgb(16, 16, 22)) < 4.5 && step < 12) {
            candidate = ColorUtils.blendARGB(candidate, Color.WHITE, 0.14f)
            step += 1
        }
        return candidate
    }
}

internal object StoryShareCard {
    const val EXPORT_WIDTH_PX = 1080
    const val PREVIEW_WIDTH_PX = 540
    private const val DESIGN_WIDTH = 1080f
    private const val DESIGN_HEIGHT = 1920f
    private const val MARGIN = 96f
    private const val ARTWORK_TIMEOUT_MS = 6_000L
    private const val SHARE_DIRECTORY = "share/story"
    private const val MAX_CACHE_FILES = 4
    private const val BRAND = "LEVYRA"
    private const val FALLBACK_START = 0xFF26B2D6.toInt()
    private const val FALLBACK_END = 0xFF6F4CFF.toInt()
    private const val MIDNIGHT = 0xFF0B0C11.toInt()

    fun heightFor(widthPx: Int): Int = widthPx * 16 / 9

    suspend fun loadArtwork(context: Context, track: Track): Bitmap? = withContext(Dispatchers.IO) {
        val file = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?: LevyraArtworkCache.localFile(context, track, highRes = false)
            ?: run {
                withTimeoutOrNull(ARTWORK_TIMEOUT_MS) { LevyraArtworkCache.cachePersistent(context, listOf(track), limit = 1) }
                LevyraArtworkCache.localFile(context, track, highRes = true)
                    ?: LevyraArtworkCache.localFile(context, track, highRes = false)
            }
        ensureActive()
        file?.takeIf(File::isFile)?.let { decodeSquare(it) }
    }

    fun colors(track: Track, artwork: Bitmap?): StoryShareColors {
        val start = track.accentStart.takeIf { it != 0 } ?: FALLBACK_START
        val end = track.accentEnd.takeIf { it != 0 } ?: FALLBACK_END
        val palette = artwork?.let { ArtworkPaletteCache.extract(it, start, end) }
        return StoryShareColors(palette?.start ?: start, palette?.end ?: end)
    }

    fun render(
        content: StoryShareContent,
        artwork: Bitmap?,
        colors: StoryShareColors,
        options: StoryShareOptions,
        widthPx: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightFor(widthPx), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(widthPx / DESIGN_WIDTH, widthPx / DESIGN_WIDTH)
        drawBackground(canvas, artwork, colors, options)
        when (options.template) {
            StoryTemplate.Aura -> drawAura(canvas, content, artwork, colors, options)
            StoryTemplate.Vinyl -> drawVinyl(canvas, content, artwork, colors, options)
            StoryTemplate.Poster -> drawPoster(canvas, content, artwork, colors, options)
            StoryTemplate.Glass -> drawGlass(canvas, content, artwork, colors, options)
        }
        if (options.showBrand) drawBrand(canvas, if (options.template == StoryTemplate.Poster) Paint.Align.RIGHT else Paint.Align.CENTER)
        return bitmap
    }

    suspend fun export(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
        directory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MAX_CACHE_FILES - 1)?.forEach { it.delete() }
        val file = File(directory, "levyra-story-${System.currentTimeMillis()}.png")
        val written = try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            false
        }
        if (!written || file.length() <= 0L) {
            file.delete()
            return@withContext null
        }
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
        } catch (_: IllegalArgumentException) {
            file.delete()
            null
        }
    }

    fun shareIntent(uri: Uri, caption: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        clipData = ClipData("story", arrayOf("image/png"), ClipData.Item(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val canSaveToGallery: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "levyra-story-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Levyra")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        val written = try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            false
        }
        if (!written) {
            resolver.delete(uri, null, null)
            return@withContext false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        if (resolver.update(uri, values, null, null) != 1) {
            resolver.delete(uri, null, null)
            return@withContext false
        }
        true
    }

    private fun drawBackground(canvas: Canvas, artwork: Bitmap?, colors: StoryShareColors, options: StoryShareOptions) {
        val full = RectF(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT)
        if (options.tone == StoryTone.Artwork && artwork != null) {
            val soft = softArtwork(artwork)
            canvas.drawBitmap(soft, null, RectF(-240f, -120f, DESIGN_WIDTH + 240f, DESIGN_HEIGHT + 120f), Paint(Paint.FILTER_BITMAP_FLAG))
            soft.recycle()
            canvas.drawRect(full, Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, DESIGN_HEIGHT,
                    intArrayOf(withAlpha(colors.deep, 120), withAlpha(colors.deep, 170), withAlpha(colors.ink, 245)),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            })
        } else {
            canvas.drawColor(MIDNIGHT)
            canvas.drawRect(full, Paint().apply {
                shader = RadialGradient(DESIGN_WIDTH * 0.2f, DESIGN_HEIGHT * 0.18f, DESIGN_WIDTH * 1.1f, withAlpha(colors.start, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            })
            canvas.drawRect(full, Paint().apply {
                shader = RadialGradient(DESIGN_WIDTH * 0.95f, DESIGN_HEIGHT * 0.86f, DESIGN_WIDTH, withAlpha(colors.end, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            })
        }
        canvas.drawRect(full, Paint().apply {
            shader = LinearGradient(0f, DESIGN_HEIGHT * 0.78f, 0f, DESIGN_HEIGHT, Color.TRANSPARENT, withAlpha(Color.BLACK, 120), Shader.TileMode.CLAMP)
        })
    }

    private fun drawAura(canvas: Canvas, content: StoryShareContent, artwork: Bitmap?, colors: StoryShareColors, options: StoryShareOptions) {
        drawEyebrow(canvas, content.eyebrow, colors, DESIGN_WIDTH / 2f, 236f, Paint.Align.CENTER)
        val cover = RectF(160f, 360f, 920f, 1120f)
        canvas.drawCircle(cover.centerX(), cover.centerY(), 560f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cover.centerX(), cover.centerY(), 560f, withAlpha(colors.glow, 150), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        })
        drawCover(canvas, artwork, cover, 56f, colors)
        val bottom = drawTitleBlock(canvas, content, 1230f, Paint.Align.CENTER, titleSize = 74f, maxTitleLines = 2)
        if (options.showProgress) drawProgress(canvas, content, colors, max(bottom + 70f, 1490f))
    }

    private fun drawVinyl(canvas: Canvas, content: StoryShareContent, artwork: Bitmap?, colors: StoryShareColors, options: StoryShareOptions) {
        drawEyebrow(canvas, content.eyebrow, colors, MARGIN, 236f, Paint.Align.LEFT)
        val cover = RectF(MARGIN, 380f, MARGIN + 640f, 1020f)
        val discCenterX = cover.right + 40f
        val discCenterY = cover.centerY()
        val radius = 330f
        canvas.drawCircle(discCenterX, discCenterY, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(discCenterX - 80f, discCenterY - 90f, radius * 1.3f, Color.rgb(46, 46, 52), Color.rgb(8, 8, 10), Shader.TileMode.CLAMP)
        })
        val groove = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        var ring = radius - 22f
        var index = 0
        while (ring > 110f) {
            groove.color = withAlpha(Color.WHITE, if (index % 3 == 0) 26 else 12)
            canvas.drawCircle(discCenterX, discCenterY, ring, groove)
            ring -= 13f
            index += 1
        }
        canvas.drawArc(
            RectF(discCenterX - radius + 18f, discCenterY - radius + 18f, discCenterX + radius - 18f, discCenterY + radius - 18f),
            300f,
            50f,
            false,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 46f
                color = withAlpha(Color.WHITE, 16)
                strokeCap = Paint.Cap.ROUND
            }
        )
        canvas.drawCircle(discCenterX, discCenterY, 104f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent })
        canvas.drawCircle(discCenterX, discCenterY, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MIDNIGHT })
        drawCover(canvas, artwork, cover, 28f, colors)
        val bottom = drawTitleBlock(canvas, content, 1150f, Paint.Align.LEFT, titleSize = 78f, maxTitleLines = 3)
        if (options.showProgress) drawProgress(canvas, content, colors, max(bottom + 70f, 1500f))
    }

    private fun drawPoster(canvas: Canvas, content: StoryShareContent, artwork: Bitmap?, colors: StoryShareColors, options: StoryShareOptions) {
        val cover = RectF(0f, 0f, DESIGN_WIDTH, DESIGN_WIDTH)
        if (artwork != null) {
            canvas.drawBitmap(artwork, null, cover, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        } else {
            canvas.drawRect(cover, Paint().apply { shader = LinearGradient(0f, 0f, DESIGN_WIDTH, DESIGN_WIDTH, colors.start, colors.end, Shader.TileMode.CLAMP) })
        }
        val base = if (options.tone == StoryTone.Artwork) colors.ink else MIDNIGHT
        canvas.drawRect(0f, DESIGN_WIDTH * 0.45f, DESIGN_WIDTH, DESIGN_WIDTH + 2f, Paint().apply {
            shader = LinearGradient(0f, DESIGN_WIDTH * 0.45f, 0f, DESIGN_WIDTH, Color.TRANSPARENT, base, Shader.TileMode.CLAMP)
        })
        canvas.drawRect(0f, DESIGN_WIDTH, DESIGN_WIDTH, DESIGN_HEIGHT, Paint().apply { color = base })
        drawEyebrow(canvas, content.eyebrow, colors, MARGIN, 1110f, Paint.Align.LEFT)
        val bottom = drawTitleBlock(canvas, content, 1150f, Paint.Align.LEFT, titleSize = 112f, maxTitleLines = 3, condensed = true)
        canvas.drawRect(MARGIN, bottom + 44f, MARGIN + 120f, bottom + 52f, Paint().apply { color = colors.accent })
        if (options.showProgress) drawProgress(canvas, content, colors, max(bottom + 130f, 1560f))
    }

    private fun drawGlass(canvas: Canvas, content: StoryShareContent, artwork: Bitmap?, colors: StoryShareColors, options: StoryShareOptions) {
        val card = RectF(84f, 300f, DESIGN_WIDTH - 84f, 1640f)
        canvas.drawRoundRect(RectF(card.left, card.top + 30f, card.right, card.bottom + 30f), 72f, 72f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.BLACK, 90)
            maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
        })
        canvas.drawRoundRect(card, 72f, 72f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, card.top, 0f, card.bottom, withAlpha(Color.WHITE, 46), withAlpha(Color.WHITE, 16), Shader.TileMode.CLAMP)
        })
        canvas.drawRoundRect(card, 72f, 72f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            shader = LinearGradient(0f, card.top, 0f, card.bottom, withAlpha(Color.WHITE, 120), withAlpha(Color.WHITE, 26), Shader.TileMode.CLAMP)
        })
        canvas.drawRoundRect(RectF(card.left + 40f, card.top + 14f, card.right - 40f, card.top + 18f), 2f, 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(card.left, 0f, card.centerX(), 0f, Color.TRANSPARENT, withAlpha(Color.WHITE, 140), Shader.TileMode.MIRROR)
        })
        drawEyebrow(canvas, content.eyebrow, colors, DESIGN_WIDTH / 2f, 220f, Paint.Align.CENTER)
        val cover = RectF(card.left + 70f, card.top + 70f, card.right - 70f, card.top + 70f + (card.width() - 140f))
        drawCover(canvas, artwork, cover, 44f, colors)
        val bottom = drawTitleBlock(canvas, content, cover.bottom + 110f, Paint.Align.CENTER, titleSize = 66f, maxTitleLines = 2, width = card.width() - 140f)
        if (options.showProgress) drawProgress(canvas, content, colors, min(max(bottom + 60f, 1460f), card.bottom - 110f), inset = card.left + 70f)
    }

    private fun drawEyebrow(canvas: Canvas, text: String, colors: StoryShareColors, x: Float, y: Float, align: Paint.Align) {
        if (text.isBlank()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 210)
            textSize = 30f
            letterSpacing = 0.24f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val label = text.uppercase()
        val width = paint.measureText(label)
        val left = when (align) {
            Paint.Align.LEFT -> x
            Paint.Align.RIGHT -> x - width - 34f
            Paint.Align.CENTER -> x - (width + 34f) / 2f
        }
        canvas.drawCircle(left + 9f, y - 10f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent })
        canvas.drawText(label, left + 34f, y, paint)
    }

    private fun drawTitleBlock(
        canvas: Canvas,
        content: StoryShareContent,
        top: Float,
        align: Paint.Align,
        titleSize: Float,
        maxTitleLines: Int,
        width: Float = DESIGN_WIDTH - MARGIN * 2f,
        condensed: Boolean = false
    ): Float {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = titleSize
            typeface = Typeface.create(if (condensed) "sans-serif-condensed" else "sans-serif-black", Typeface.BOLD)
            letterSpacing = -0.015f
        }
        val alignment = if (align == Paint.Align.CENTER) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        val title = layout(content.title, titlePaint, width.toInt(), alignment, maxTitleLines, 1.02f)
        val left = if (align == Paint.Align.CENTER) (DESIGN_WIDTH - width) / 2f else MARGIN
        canvas.save()
        canvas.translate(left, top)
        title.draw(canvas)
        canvas.restore()
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 200)
            textSize = titleSize * 0.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val subtitle = listOf(content.artist, content.album).filter { it.isNotBlank() }.distinct().joinToString("  ·  ")
        val artist = layout(subtitle, artistPaint, width.toInt(), alignment, 1, 1f)
        val artistTop = top + title.height + titleSize * 0.32f
        canvas.save()
        canvas.translate(left, artistTop)
        artist.draw(canvas)
        canvas.restore()
        return artistTop + artist.height
    }

    private fun drawProgress(canvas: Canvas, content: StoryShareContent, colors: StoryShareColors, y: Float, inset: Float = MARGIN) {
        if (content.durationMs <= 0L) return
        val start = inset
        val end = DESIGN_WIDTH - inset
        canvas.drawRoundRect(RectF(start, y, end, y + 10f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, 56) })
        val fillEnd = start + (end - start) * content.progress
        canvas.drawRoundRect(RectF(start, y, max(start + 10f, fillEnd), y + 10f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(start, 0f, end, 0f, colors.accent, Color.WHITE, Shader.TileMode.CLAMP)
        })
        canvas.drawCircle(fillEnd, y + 5f, 17f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(14f, 0f, 3f, withAlpha(Color.BLACK, 110))
        })
        val time = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 170)
            textSize = 30f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText(formatTime(content.positionMs), start, y + 64f, time)
        time.textAlign = Paint.Align.RIGHT
        canvas.drawText("-" + formatTime(content.durationMs - content.positionMs), end, y + 64f, time)
    }

    private fun drawBrand(canvas: Canvas, align: Paint.Align) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 150)
            textSize = 28f
            letterSpacing = 0.42f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val glyph = 30f
        val total = glyph + 18f + paint.measureText(BRAND)
        val left = if (align == Paint.Align.RIGHT) DESIGN_WIDTH - MARGIN - total else (DESIGN_WIDTH - total) / 2f
        val baseline = DESIGN_HEIGHT - 120f
        val cx = left + glyph / 2f
        val cy = baseline - 10f
        canvas.drawCircle(cx, cy, glyph / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(Color.WHITE, 150)
        })
        canvas.drawCircle(cx, cy, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, 170) })
        canvas.drawText(BRAND, left + glyph + 18f, baseline, paint)
    }

    private fun drawCover(canvas: Canvas, artwork: Bitmap?, target: RectF, radius: Float, colors: StoryShareColors) {
        canvas.drawRoundRect(RectF(target.left + 10f, target.top + 36f, target.right - 10f, target.bottom + 36f), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.BLACK, 130)
            maskFilter = BlurMaskFilter(48f, BlurMaskFilter.Blur.NORMAL)
        })
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(target, radius, radius, Path.Direction.CW) })
        if (artwork != null) {
            canvas.drawBitmap(artwork, null, target, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        } else {
            canvas.drawRect(target, Paint().apply { shader = LinearGradient(target.left, target.top, target.right, target.bottom, colors.start, colors.end, Shader.TileMode.CLAMP) })
        }
        canvas.restore()
        canvas.drawRoundRect(target, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = withAlpha(Color.WHITE, 34)
        })
    }

    private fun layout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment, maxLines: Int, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(alignment)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, spacing)
            .setIncludePad(false)
            .build()

    private fun softArtwork(artwork: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(artwork, 48, 48, true)
        val tiny = Bitmap.createScaledBitmap(small, 12, 12, true)
        if (small !== artwork && small !== tiny) small.recycle()
        val soft = Bitmap.createScaledBitmap(tiny, 96, 96, true)
        if (tiny !== artwork && tiny !== soft) tiny.recycle()
        return soft
    }

    private fun decodeSquare(file: File): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 900) sample *= 2
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.let { decoded ->
            val side = min(decoded.width, decoded.height)
            if (decoded.width == decoded.height) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side).also { decoded.recycle() }
            }
        }
    } catch (_: OutOfMemoryError) {
        null
    }

    internal fun formatTime(ms: Long): String {
        val total = ms.coerceAtLeast(0L) / 1000L
        return "%d:%02d".format(total / 60L, total % 60L)
    }

    private fun withAlpha(color: Int, alpha: Int): Int = ColorUtils.setAlphaComponent(color, alpha.coerceIn(0, 255))
}
