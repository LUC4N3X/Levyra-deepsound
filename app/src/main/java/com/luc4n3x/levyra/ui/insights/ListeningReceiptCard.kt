package com.luc4n3x.levyra.ui.insights

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.content.FileProvider
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.ui.artwork.ArtworkMediaStore
import com.luc4n3x.levyra.ui.lyrics.LyricsShareResource
import com.luc4n3x.levyra.ui.lyrics.isRtlText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal data class ListeningReceiptPalette(val start: Int, val end: Int)

internal class ListeningReceiptAssets(
    val covers: List<Bitmap?>,
    val artist: Bitmap?,
    val logo: Bitmap?
) {
    fun recycle() {
        covers.forEach { it?.recycle() }
        artist?.recycle()
        logo?.recycle()
    }
}

internal typealias ListeningReceiptCovers = LyricsShareResource<ListeningReceiptAssets>

internal fun listeningReceiptCovers(): ListeningReceiptCovers = LyricsShareResource { it.recycle() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object ListeningReceiptCard {
    const val EXPORT_WIDTH_PX = 1080
    const val PREVIEW_WIDTH_PX = 540

    private const val WIDTH = 1080f
    private const val HEIGHT = 1350f
    private const val MARGIN = 72f
    private const val COLUMN_SPLIT = 680f
    private const val TEAR_Y = 604f
    private const val PANEL_TOP = 630f
    private const val PANEL_BOTTOM = 908f
    private const val LIST_HEADING_Y = 1036f
    private const val LIST_TOP = 1056f
    private const val ROW_STEP = 48f
    private const val THUMB = 40f
    private const val COVER_PX = 480
    private const val ARTIST_PX = 320
    private const val LOGO_ALPHA_THRESHOLD = 16
    private const val ARTWORK_TIMEOUT_MS = 6_000L
    private const val MAX_CACHE_FILES = 4
    private const val MAX_CACHE_AGE_MS = 24L * 60L * 60L * 1000L
    private const val SHARE_DIRECTORY = "share/receipt"
    private const val FALLBACK_START = 0xFF26B2D6.toInt()
    private const val FALLBACK_END = 0xFF6F4CFF.toInt()
    private const val FALLBACK_VIVID = 0xFF4FD8F0.toInt()

    private val blackFace: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val mediumFace: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val regularFace: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)

    fun heightFor(widthPx: Int): Int = widthPx * 5 / 4

    suspend fun loadAssets(
        context: Context,
        urls: List<String>,
        artistUrl: String,
        into: ListeningReceiptCovers
    ) {
        val appContext = context.applicationContext
        val assets = withContext(Dispatchers.IO) {
            coroutineScope {
                val covers = urls.map { url -> async { loadSquare(appContext, url, COVER_PX) } }
                val artist = async { artistUrl.takeIf(String::isNotBlank)?.let { loadSquare(appContext, it, ARTIST_PX) } }
                val logo = async { decodeLogo(appContext) }
                ListeningReceiptAssets(covers.awaitAll(), artist.await(), logo.await())
            }
        }
        into.set(assets)
    }

    fun resolvePalette(assets: ListeningReceiptAssets?): ListeningReceiptPalette {
        val cover = assets?.covers?.firstOrNull { it != null && !it.isRecycled }
            ?: assets?.artist?.takeUnless(Bitmap::isRecycled)
            ?: return ListeningReceiptPalette(FALLBACK_START, FALLBACK_END)
        val palette = ArtworkPaletteCache.extract(cover, FALLBACK_START, FALLBACK_END)
        return ListeningReceiptPalette(palette.start, palette.end)
    }

    suspend fun createShareIntent(
        context: Context,
        content: ListeningReceiptContent,
        assets: ListeningReceiptCovers,
        style: ListeningReceiptStyle,
        palette: ListeningReceiptPalette,
        caption: String
    ): Intent? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARE_DIRECTORY)
        if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
        prune(directory)
        val file = File(directory, "levyra-receipt-${System.currentTimeMillis()}.png")
        val written = try {
            FileOutputStream(file).use { output -> exportPng(content, assets, style, palette, output) }
        } catch (cancelled: CancellationException) {
            file.delete()
            throw cancelled
        } catch (error: IOException) {
            Timber.w(error, "Listening receipt export failed")
            false
        }
        if (!written || file.length() <= 0L) {
            file.delete()
            return@withContext null
        }
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
        } catch (_: IllegalArgumentException) {
            file.delete()
            return@withContext null
        }
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(context.contentResolver, "receipt", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun saveToGallery(
        context: Context,
        content: ListeningReceiptContent,
        assets: ListeningReceiptCovers,
        style: ListeningReceiptStyle,
        palette: ListeningReceiptPalette
    ): Boolean = ArtworkMediaStore.save(
        context = context,
        fileName = "levyra-receipt-${System.currentTimeMillis()}.png",
        mimeType = "image/png"
    ) { output -> exportPng(content, assets, style, palette, output) }

    private fun exportPng(
        content: ListeningReceiptContent,
        assets: ListeningReceiptCovers,
        style: ListeningReceiptStyle,
        palette: ListeningReceiptPalette,
        output: OutputStream
    ): Boolean {
        val bitmap = assets.use { render(content, it, style, palette, EXPORT_WIDTH_PX) } ?: return false
        return try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } finally {
            bitmap.recycle()
        }
    }

    fun render(
        content: ListeningReceiptContent,
        assets: ListeningReceiptAssets?,
        style: ListeningReceiptStyle,
        palette: ListeningReceiptPalette,
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
            canvas.scale(widthPx / WIDTH, widthPx / WIDTH)
            val theme = theme(style, palette)
            val background = backgroundShader(theme)
            val covers = assets?.covers.orEmpty()
            drawBackground(canvas, theme, background)
            drawHero(canvas, content, covers, theme, background)
            drawHeader(canvas, content, assets?.logo?.takeUnless(Bitmap::isRecycled), theme)
            drawTear(canvas, theme)
            drawPanel(canvas, content, theme)
            drawWaveform(canvas, content.waveform, theme)
            drawTopTracks(canvas, content, covers, theme)
            drawTopArtists(canvas, content, assets?.artist?.takeUnless(Bitmap::isRecycled), theme)
            drawFooter(canvas, content, theme)
            completed = true
            return bitmap
        } finally {
            if (!completed) bitmap.recycle()
        }
    }

    private class Theme(
        val top: Int,
        val bottom: Int,
        val accent: Int,
        val accentAlt: Int,
        val ink: Int,
        val muted: Int,
        val faint: Int,
        val onAccent: Int,
        val panel: Int,
        val tile: Int,
        val notch: Int,
        val shadowAlpha: Int,
        val blobAlpha: Int,
        val outline: Int,
        val glow: Boolean,
        val gradientNumber: Boolean
    )

    private fun theme(style: ListeningReceiptStyle, palette: ListeningReceiptPalette): Theme {
        val accent = vivid(palette.start)
        val accentAlt = vivid(palette.end)
        return when (style) {
            ListeningReceiptStyle.Aurora -> Theme(
                top = tone(palette.start, value = 0.30f),
                bottom = 0xFF060709.toInt(),
                accent = accent,
                accentAlt = accentAlt,
                ink = Color.WHITE,
                muted = Color.argb(180, 255, 255, 255),
                faint = Color.argb(54, 255, 255, 255),
                onAccent = 0xFF07080B.toInt(),
                panel = Color.argb(20, 255, 255, 255),
                tile = withAlpha(accent, 44),
                notch = 0xFF020203.toInt(),
                shadowAlpha = 150,
                blobAlpha = 46,
                outline = Color.argb(46, 255, 255, 255),
                glow = true,
                gradientNumber = true
            )
            ListeningReceiptStyle.Noir -> Theme(
                top = Color.BLACK,
                bottom = Color.BLACK,
                accent = accent,
                accentAlt = accentAlt,
                ink = Color.WHITE,
                muted = Color.argb(166, 255, 255, 255),
                faint = Color.argb(46, 255, 255, 255),
                onAccent = Color.BLACK,
                panel = 0xFF101114.toInt(),
                tile = withAlpha(accent, 38),
                notch = 0xFF17181C.toInt(),
                shadowAlpha = 170,
                blobAlpha = 30,
                outline = Color.argb(40, 255, 255, 255),
                glow = false,
                gradientNumber = false
            )
            ListeningReceiptStyle.Paper -> {
                val deepAccent = deep(palette.start)
                Theme(
                    top = 0xFFF6F2E9.toInt(),
                    bottom = 0xFFEAE3D5.toInt(),
                    accent = deepAccent,
                    accentAlt = deep(palette.end),
                    ink = 0xFF16140F.toInt(),
                    muted = Color.argb(168, 22, 20, 15),
                    faint = Color.argb(46, 22, 20, 15),
                    onAccent = 0xFFF6F2E9.toInt(),
                    panel = Color.argb(150, 255, 255, 255),
                    tile = withAlpha(deepAccent, 30),
                    notch = 0xFFD6CDBB.toInt(),
                    shadowAlpha = 70,
                    blobAlpha = 34,
                    outline = Color.argb(70, 255, 255, 255),
                    glow = false,
                    gradientNumber = false
                )
            }
        }
    }

    private fun backgroundShader(theme: Theme): Shader =
        LinearGradient(0f, 0f, 0f, HEIGHT, theme.top, theme.bottom, Shader.TileMode.CLAMP)

    private fun drawBackground(canvas: Canvas, theme: Theme, background: Shader) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = background
        canvas.drawRect(0f, 0f, WIDTH, HEIGHT, paint)
        if (theme.glow) {
            paint.shader = RadialGradient(
                WIDTH * 0.78f, HEIGHT * 0.14f, WIDTH * 0.82f,
                withAlpha(theme.accent, 78), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, WIDTH, HEIGHT, paint)
            paint.shader = RadialGradient(
                0f, HEIGHT * 0.56f, WIDTH * 0.72f,
                withAlpha(theme.accentAlt, 52), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, WIDTH, HEIGHT, paint)
        } else {
            paint.shader = RadialGradient(
                WIDTH * 0.85f, 0f, WIDTH * 0.9f,
                withAlpha(theme.accent, 30), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, WIDTH, HEIGHT, paint)
        }
    }

    private class CoverSlot(
        val polygon: RoundedPolygon,
        val centerX: Float,
        val centerY: Float,
        val size: Float,
        val rotation: Float
    )

    private fun coverSlots(count: Int): List<CoverSlot> = when (count) {
        0, 1 -> listOf(CoverSlot(MaterialShapes.Cookie9Sided, 650f, 300f, 470f, -6f))
        2 -> listOf(
            CoverSlot(MaterialShapes.Cookie9Sided, 668f, 284f, 450f, -6f),
            CoverSlot(MaterialShapes.Clover4Leaf, 352f, 448f, 256f, 10f)
        )
        else -> listOf(
            CoverSlot(MaterialShapes.Cookie9Sided, 652f, 282f, 440f, -6f),
            CoverSlot(MaterialShapes.Clover4Leaf, 346f, 446f, 250f, 10f),
            CoverSlot(MaterialShapes.Sunny, 904f, 490f, 186f, 14f)
        )
    }

    private fun drawHero(
        canvas: Canvas,
        content: ListeningReceiptContent,
        covers: List<Bitmap?>,
        theme: Theme,
        background: Shader
    ) {
        val slotCount = maxOf(content.artworkUrls.size, content.coverInitials.size)
            .coerceIn(1, ListeningReceiptModel.MAX_COVERS)
        val slots = coverSlots(slotCount)
        val blob = shapePath(MaterialShapes.SoftBurst, 700f, 290f, 700f, 15f)
        canvas.drawPath(blob, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                380f, 0f, 1060f, 620f,
                withAlpha(theme.accent, theme.blobAlpha), withAlpha(theme.accentAlt, theme.blobAlpha),
                Shader.TileMode.CLAMP
            )
        })

        val main = slots.first()
        val radius = main.size / 2f + 34f
        val orbitBounds = RectF(main.centerX - radius, main.centerY - radius, main.centerX + radius, main.centerY + radius)
        val orbit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5f
            color = withAlpha(theme.accent, 220)
        }
        canvas.drawArc(orbitBounds, 196f, 112f, false, orbit)
        orbit.color = withAlpha(theme.accentAlt, 120)
        orbit.strokeWidth = 3f
        canvas.drawArc(orbitBounds, 326f, 54f, false, orbit)
        val headAngle = Math.toRadians(308.0)
        canvas.drawCircle(
            main.centerX + radius * Math.cos(headAngle).toFloat(),
            main.centerY + radius * Math.sin(headAngle).toFloat(),
            10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.accent }
        )

        slots.forEachIndexed { index, slot ->
            val path = shapePath(slot.polygon, slot.centerX, slot.centerY, slot.size, slot.rotation)
            if (index > 0) {
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = background
                    style = Paint.Style.STROKE
                    strokeWidth = 28f
                })
            }
            drawShadow(canvas, path, theme)
            val cover = covers.getOrNull(index)?.takeUnless(Bitmap::isRecycled)
            if (cover != null) {
                drawClipped(canvas, path, cover)
            } else {
                drawPlaceholder(canvas, path, content.coverInitials.getOrNull(index) ?: "♪", slot.size * 0.36f, theme)
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = theme.outline
            })
        }
    }

    private fun shapePath(polygon: RoundedPolygon, centerX: Float, centerY: Float, size: Float, rotation: Float): Path {
        val path = polygon.toPath(Path())
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val target = RectF(centerX - size / 2f, centerY - size / 2f, centerX + size / 2f, centerY + size / 2f)
        val matrix = Matrix()
        matrix.setRectToRect(bounds, target, Matrix.ScaleToFit.CENTER)
        matrix.postRotate(rotation, centerX, centerY)
        path.transform(matrix)
        return path
    }

    private fun drawShadow(canvas: Canvas, path: Path, theme: Theme, offset: Float = 24f, blur: Float = 42f) {
        val shadow = Path(path).apply { offset(0f, offset) }
        canvas.drawPath(shadow, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(theme.shadowAlpha, 0, 0, 0)
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        })
    }

    private fun drawClipped(canvas: Canvas, path: Path, image: Bitmap) {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val side = maxOf(bounds.width(), bounds.height())
        val target = RectF(
            bounds.centerX() - side / 2f,
            bounds.centerY() - side / 2f,
            bounds.centerX() + side / 2f,
            bounds.centerY() + side / 2f
        )
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(image, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    private fun drawPlaceholder(canvas: Canvas, path: Path, initial: String, textSize: Float, theme: Theme) {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                theme.accent, theme.accentAlt, Shader.TileMode.CLAMP
            )
        })
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            this.textSize = textSize
            color = withAlpha(theme.onAccent, 210)
            textAlign = Paint.Align.CENTER
        }
        val metrics = text.fontMetrics
        canvas.drawText(initial, bounds.centerX(), bounds.centerY() - (metrics.ascent + metrics.descent) / 2f, text)
    }

    private fun drawHeader(canvas: Canvas, content: ListeningReceiptContent, logo: Bitmap?, theme: Theme) {
        val wordmarkX = if (logo != null) {
            val size = 62f
            val scale = size / maxOf(logo.width, logo.height)
            val width = logo.width * scale
            val height = logo.height * scale
            val target = RectF(MARGIN, 110f - height, MARGIN + width, 110f)
            canvas.drawBitmap(logo, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            MARGIN + width + 14f
        } else {
            MARGIN
        }
        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            textSize = 30f
            letterSpacing = 0.32f
            color = theme.ink
        }
        canvas.drawText(content.brand, wordmarkX, 104f, brand)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            color = theme.ink
            letterSpacing = -0.02f
        }
        val title = fitLayout(content.title, titlePaint, 320, 64f, 38f, maxLines = 3, maxHeight = 164f)
        canvas.save()
        canvas.translate(MARGIN, 148f)
        title.draw(canvas)
        canvas.restore()

        val chipTop = 148f + title.height + 22f
        val chipText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            textSize = 24f
            letterSpacing = 0.12f
            color = theme.onAccent
        }
        val label = TextUtils.ellipsize(content.periodShort, chipText, 220f, TextUtils.TruncateAt.END).toString()
        val chipWidth = chipText.measureText(label) + 40f
        canvas.drawRoundRect(
            RectF(MARGIN, chipTop, MARGIN + chipWidth, chipTop + 46f),
            23f, 23f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.accent }
        )
        val metrics = chipText.fontMetrics
        canvas.drawText(label, MARGIN + 20f, chipTop + 23f - (metrics.ascent + metrics.descent) / 2f, chipText)
    }

    private fun drawTear(canvas: Canvas, theme: Theme) {
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = theme.faint
            pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f)
        }
        canvas.drawLine(52f, TEAR_Y, WIDTH - 52f, TEAR_Y, dash)
        val notch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.notch }
        canvas.drawCircle(0f, TEAR_Y, 26f, notch)
        canvas.drawCircle(WIDTH, TEAR_Y, 26f, notch)
    }

    private fun drawPanel(canvas: Canvas, content: ListeningReceiptContent, theme: Theme) {
        val panel = RectF(48f, PANEL_TOP, WIDTH - 48f, PANEL_BOTTOM)
        canvas.drawRoundRect(panel, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.panel })

        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mediumFace
            textSize = 22f
            letterSpacing = 0.2f
            color = theme.accent
        }
        val numberLeft = 92f
        val tileLeft = COLUMN_SPLIT + 4f
        drawLine(canvas, content.minutesLabel, label, numberLeft, 690f, tileLeft - numberLeft - 24f)

        val numberWidth = tileLeft - numberLeft - 32f
        val number = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            letterSpacing = -0.04f
            color = theme.ink
        }
        var size = 220f
        number.textSize = size
        while (size > 90f && number.measureText(content.minutesValue) > numberWidth) {
            size -= 6f
            number.textSize = size
        }
        val baseline = 864f
        if (theme.gradientNumber) {
            number.shader = LinearGradient(
                0f, baseline - size * 0.74f, 0f, baseline,
                theme.ink, blend(theme.ink, theme.accent, 0.6f), Shader.TileMode.CLAMP
            )
        }
        val value = TextUtils.ellipsize(content.minutesValue, number, numberWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(value, numberLeft - 6f, baseline, number)

        val tileRight = WIDTH - 72f
        val tileHeight = 72f
        val gap = 6f
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.tile }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = blackFace
            textSize = 34f
            color = theme.ink
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mediumFace
            textSize = 22f
            color = theme.muted
        }
        val count = content.stats.size
        content.stats.forEachIndexed { index, stat ->
            val top = PANEL_TOP + 26f + index * (tileHeight + gap)
            val rect = RectF(tileLeft, top, tileRight, top + tileHeight)
            val topRadius = if (index == 0) 30f else 10f
            val bottomRadius = if (index == count - 1) 30f else 10f
            canvas.drawPath(roundedRect(rect, topRadius, bottomRadius), tilePaint)
            val textBaseline = top + tileHeight / 2f + 12f
            val valueText = TextUtils.ellipsize(stat.value, valuePaint, rect.width() * 0.55f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(valueText, tileLeft + 22f, textBaseline, valuePaint)
            val offset = 22f + valuePaint.measureText(valueText) + 10f
            drawLine(canvas, stat.label, labelPaint, tileLeft + offset, textBaseline, rect.width() - offset - 18f)
        }
    }

    private fun roundedRect(rect: RectF, topRadius: Float, bottomRadius: Float): Path = Path().apply {
        addRoundRect(
            rect,
            floatArrayOf(topRadius, topRadius, topRadius, topRadius, bottomRadius, bottomRadius, bottomRadius, bottomRadius),
            Path.Direction.CW
        )
    }

    private fun drawWaveform(canvas: Canvas, values: List<Float>, theme: Theme) {
        if (values.isEmpty()) return
        val left = MARGIN
        val right = WIDTH - MARGIN
        val center = 960f
        val maxHalf = 28f
        val step = (right - left) / values.size
        val barWidth = (step * 0.6f).coerceIn(4f, 18f)
        val active = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(left, 0f, right, 0f, theme.accent, theme.accentAlt, Shader.TileMode.CLAMP)
        }
        val idle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.faint }
        values.forEachIndexed { index, value ->
            val half = if (value <= 0f) barWidth / 2f else barWidth / 2f + value.coerceIn(0f, 1f) * (maxHalf - barWidth / 2f)
            val x = left + step * index + (step - barWidth) / 2f
            canvas.drawRoundRect(
                RectF(x, center - half, x + barWidth, center + half),
                barWidth / 2f, barWidth / 2f,
                if (value <= 0f) idle else active
            )
        }
    }

    private fun drawTopTracks(canvas: Canvas, content: ListeningReceiptContent, covers: List<Bitmap?>, theme: Theme) {
        if (content.topTracks.isEmpty()) return
        val columnRight = COLUMN_SPLIT - 36f
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mediumFace
            textSize = 22f
            letterSpacing = 0.2f
            color = theme.muted
        }
        drawLine(canvas, content.topTracksLabel, heading, MARGIN, LIST_HEADING_Y, columnRight - MARGIN)
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.ink }
        val artist = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regularFace
            textSize = 21f
            color = theme.muted
        }
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = theme.accent
        }
        content.topTracks.forEachIndexed { index, track ->
            val top = LIST_TOP + index * ROW_STEP
            val thumbPath = roundedRect(RectF(MARGIN, top, MARGIN + THUMB, top + THUMB), 12f, 12f)
            val thumb = covers.getOrNull(track.artworkIndex)?.takeUnless(Bitmap::isRecycled)
            if (thumb != null) {
                drawClipped(canvas, thumbPath, thumb)
            } else {
                drawPlaceholder(canvas, thumbPath, ListeningReceiptModel.initial(track.title), 18f, theme)
            }
            if (index == 0) canvas.drawPath(thumbPath, highlight)
            val baseline = top + 29f
            title.typeface = if (index == 0) blackFace else mediumFace
            title.textSize = if (index == 0) 27f else 25f
            val textLeft = MARGIN + THUMB + 16f
            val available = columnRight - textLeft
            val showArtist = track.artist.isNotBlank() && !isRtlText(track.title)
            val titleWidth = if (showArtist) available * 0.64f else available
            val titleText = TextUtils.ellipsize(track.title, title, titleWidth, TextUtils.TruncateAt.END).toString()
            drawLine(canvas, titleText, title, textLeft, baseline, available)
            val artistLeft = textLeft + title.measureText(titleText) + 12f
            if (showArtist && columnRight - artistLeft > 60f) {
                drawLine(canvas, track.artist, artist, artistLeft, baseline, columnRight - artistLeft)
            }
        }
    }

    private fun drawTopArtists(canvas: Canvas, content: ListeningReceiptContent, portrait: Bitmap?, theme: Theme) {
        val width = WIDTH - MARGIN - COLUMN_SPLIT
        var y = LIST_HEADING_Y
        if (content.topArtists.isNotEmpty()) {
            val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = mediumFace
                textSize = 22f
                letterSpacing = 0.2f
                color = theme.muted
            }
            drawLine(canvas, content.topArtistLabel, heading, COLUMN_SPLIT, y, width)
            val portraitSize = 100f
            val path = shapePath(
                MaterialShapes.Cookie12Sided,
                COLUMN_SPLIT + portraitSize / 2f,
                LIST_TOP + portraitSize / 2f,
                portraitSize,
                0f
            )
            drawShadow(canvas, path, theme, offset = 10f, blur = 18f)
            if (portrait != null) {
                drawClipped(canvas, path, portrait)
            } else {
                drawPlaceholder(canvas, path, ListeningReceiptModel.initial(content.topArtists.first()), 40f, theme)
            }
            val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = blackFace
                color = theme.ink
                letterSpacing = -0.01f
            }
            val nameLeft = COLUMN_SPLIT + portraitSize + 18f
            val name = fitLayout(
                content.topArtists.first(), namePaint, (WIDTH - MARGIN - nameLeft).toInt(),
                36f, 24f, maxLines = 2, maxHeight = 88f
            )
            canvas.save()
            canvas.translate(nameLeft, LIST_TOP + (portraitSize - name.height) / 2f)
            name.draw(canvas)
            canvas.restore()
            y = LIST_TOP + portraitSize + 40f
            val other = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = mediumFace
                textSize = 23f
                color = theme.muted
            }
            content.topArtists.drop(1).forEachIndexed { index, artist ->
                drawLine(canvas, String.format(Locale.ROOT, "%02d  %s", index + 2, artist), other, COLUMN_SPLIT, y, width)
                y += 36f
            }
        }
        if (content.peakLine.isNotBlank()) {
            val peak = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = mediumFace
                textSize = 21f
                color = theme.accent
            }
            drawLine(canvas, content.peakLine, peak, COLUMN_SPLIT, maxOf(y, 1280f), width)
        }
    }

    private fun drawFooter(canvas: Canvas, content: ListeningReceiptContent, theme: Theme) {
        val baseline = 1322f
        val barcodeWidth = 236f
        val date = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mediumFace
            textSize = 21f
            letterSpacing = 0.06f
            color = theme.muted
        }
        drawLine(canvas, content.dateRange, date, MARGIN, baseline, WIDTH - MARGIN * 2f - barcodeWidth - 32f)
        val random = Random(content.signature)
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(theme.ink, 190) }
        val end = WIDTH - MARGIN
        var x = end - barcodeWidth
        while (x < end) {
            val barWidth = (2 + random.nextInt(5)).toFloat().coerceAtMost(end - x)
            canvas.drawRect(x, baseline - 26f, x + barWidth, baseline + 2f, bar)
            x += barWidth + 2 + random.nextInt(4)
        }
    }

    private fun drawLine(canvas: Canvas, text: String, paint: TextPaint, x: Float, baseline: Float, maxWidth: Float) {
        if (text.isBlank() || maxWidth <= 0f) return
        val candidate = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        val rtl = isRtlText(text)
        paint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        canvas.drawText(candidate, if (rtl) x + maxWidth else x, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun fitLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        startSize: Float,
        minSize: Float,
        maxLines: Int,
        maxHeight: Float
    ): StaticLayout {
        var size = startSize
        while (size > minSize) {
            paint.textSize = size
            val layout = buildLayout(text, paint, width, Int.MAX_VALUE)
            if (layout.lineCount <= maxLines && layout.height <= maxHeight) return layout
            size -= 2f
        }
        paint.textSize = minSize
        return buildLayout(text, paint, width, maxLines)
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(
                if (isRtlText(text)) TextDirectionHeuristics.FIRSTSTRONG_RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR
            )
            .setIncludePad(false)
            .setLineSpacing(0f, 0.96f)
            .setMaxLines(maxLines)
        if (maxLines != Int.MAX_VALUE) {
            builder.setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width)
        }
        return builder.build()
    }

    private suspend fun loadSquare(context: Context, url: String, sizePx: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(LevyraArtworkCache.large(url))
            .size(Size(sizePx, sizePx))
            .allowHardware(false)
            .build()
        val source = try {
            val result = withTimeoutOrNull(ARTWORK_TIMEOUT_MS) { SingletonImageLoader.get(context).execute(request) }
            (result as? SuccessResult)?.image?.toBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Listening receipt artwork unavailable")
            null
        } ?: return null
        currentCoroutineContext().ensureActive()
        return squareCopy(source, sizePx)
    }

    private fun squareCopy(source: Bitmap, sizePx: Int): Bitmap? {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
        return try {
            val side = minOf(source.width, source.height)
            val left = (source.width - side) / 2
            val top = (source.height - side) / 2
            val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            Canvas(output).drawBitmap(
                source,
                Rect(left, top, left + side, top + side),
                Rect(0, 0, sizePx, sizePx),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            output
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun decodeLogo(context: Context): Bitmap? = try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.levyra_logo, options)?.let { full ->
            val bounds = opaqueBounds(full)
            if (bounds == null || (bounds.width() == full.width && bounds.height() == full.height)) {
                full
            } else {
                Bitmap.createBitmap(full, bounds.left, bounds.top, bounds.width(), bounds.height()).also { cropped ->
                    if (cropped !== full) full.recycle()
                }
            }
        }
    } catch (_: OutOfMemoryError) {
        null
    }

    private fun opaqueBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((pixels[y * width + x] ushr 24) > LOGO_ALPHA_THRESHOLD) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        return if (right < left || bottom < top) null else Rect(left, top, right + 1, bottom + 1)
    }

    private fun prune(directory: File) {
        val now = System.currentTimeMillis()
        directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.forEachIndexed { position, file ->
                if (position >= MAX_CACHE_FILES - 1 || now - file.lastModified() > MAX_CACHE_AGE_MS) file.delete()
            }
    }

    private fun vivid(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        if (hsv[1] < 0.12f) return FALLBACK_VIVID
        hsv[1] = hsv[1].coerceIn(0.5f, 0.82f)
        hsv[2] = 1f
        return Color.HSVToColor(hsv)
    }

    private fun tone(color: Int, value: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        if (hsv[1] < 0.12f) Color.colorToHSV(FALLBACK_END, hsv)
        hsv[1] = hsv[1].coerceIn(0.5f, 0.85f)
        hsv[2] = value
        return Color.HSVToColor(hsv)
    }

    private fun deep(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        if (hsv[1] < 0.12f) Color.colorToHSV(FALLBACK_START, hsv)
        hsv[1] = hsv[1].coerceIn(0.58f, 0.9f)
        hsv[2] = 0.46f
        return Color.HSVToColor(hsv)
    }

    private fun blend(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
