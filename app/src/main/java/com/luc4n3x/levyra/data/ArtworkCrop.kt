package com.luc4n3x.levyra.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.FaceDetector
import android.os.Build
import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Dimension
import coil3.size.Size
import coil3.transform.Transformation
import com.luc4n3x.levyra.domain.ArtworkCropMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class ArtworkCropFace(val centerX: Float, val centerY: Float, val radius: Float)

internal class ArtworkCropRect(val left: Float, val top: Float, val width: Float, val height: Float)

internal object SmartArtworkCropPlanner {
    private const val SQUARE_TOLERANCE = 0.03f
    private const val MIN_ZOOM_SIDE_SHARE = 0.6f
    private const val ZOOM_STEPS = 8
    private const val ZOOM_COST = 0.35f
    private const val EMPTY_AREA_COST = 1.2f
    private const val COMPOSITION_COST = 0.12f
    private const val FACE_WEIGHT = 3f
    private const val UNIFORM_LINE_DEVIATION = 7f
    private const val MAX_BAR_SHARE = 0.4f

    fun isSquare(width: Int, height: Int): Boolean =
        width > 0 && height > 0 && abs(width - height) <= max(width, height) * SQUARE_TOLERANCE

    fun center(width: Int, height: Int): ArtworkCropRect {
        val side = min(width, height).toFloat()
        return ArtworkCropRect((width - side) / 2f / width, (height - side) / 2f / height, side / width, side / height)
    }

    fun skinShare(pixels: IntArray, width: Int, height: Int): Float {
        if (width <= 0 || height <= 0) return 0f
        var skin = 0
        for (index in 0 until width * height) if (skinLikelihood(pixels[index]) > 0.5f) skin += 1
        return skin.toFloat() / (width * height)
    }

    fun plan(
        pixels: IntArray,
        width: Int,
        height: Int,
        allowZoom: Boolean,
        faces: List<ArtworkCropFace> = emptyList()
    ): ArtworkCropRect {
        if (width < 4 || height < 4 || pixels.size < width * height || isSquare(width, height)) return center(width, height)
        val content = contentBounds(pixels, width, height).including(faces, width, height)
        val saliency = saliencyMap(pixels, width, height, content, faces)
        val total = IntegralImage(saliency, width, height)
        val weightedX = IntegralImage(FloatArray(saliency.size) { saliency[it] * (it % width) }, width, height)
        val weightedY = IntegralImage(FloatArray(saliency.size) { saliency[it] * (it / width) }, width, height)
        val totalSaliency = total.sum(0, 0, width, height)
        if (totalSaliency <= 1e-4f) return centeredOn(content, width, height, allowZoom)
        val maxSide = min(width, height)
        val contentSide = min(content.width, content.height)
        val minSide = if (allowZoom) max(2, min(maxSide, (contentSide * MIN_ZOOM_SIDE_SHARE).roundToInt())) else maxSide
        val sides = if (allowZoom) {
            ((0..ZOOM_STEPS).map { step -> maxSide - (maxSide - minSide) * step / ZOOM_STEPS } + contentSide.coerceIn(minSide, maxSide)).distinct()
        } else {
            listOf(maxSide)
        }
        var best = Candidate(0, 0, maxSide, Float.NEGATIVE_INFINITY)
        for (side in sides) {
            for (y in 0..height - side) {
                for (x in 0..width - side) {
                    val inside = total.sum(x, y, side, side)
                    val coverage = inside / totalSaliency
                    val outsideContent = 1f - content.overlap(x, y, side).toFloat() / (side * side)
                    val zoom = if (allowZoom) ZOOM_COST * max(0f, 1f - side.toFloat() / contentSide) else 0f
                    val composition = if (inside > 1e-4f) {
                        val dx = (weightedX.sum(x, y, side, side) / inside - (x + side / 2f)) / (side / 2f)
                        val dy = (weightedY.sum(x, y, side, side) / inside - (y + side / 2f)) / (side / 2f)
                        COMPOSITION_COST * (dx * dx + dy * dy)
                    } else {
                        0f
                    }
                    val score = coverage - EMPTY_AREA_COST * outsideContent - zoom - composition
                    if (score > best.score + 1e-5f) best = Candidate(x, y, side, score)
                }
            }
        }
        return ArtworkCropRect(
            best.x.toFloat() / width,
            best.y.toFloat() / height,
            best.side.toFloat() / width,
            best.side.toFloat() / height
        )
    }

    private class Candidate(val x: Int, val y: Int, val side: Int, val score: Float)

    internal class ContentBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top

        fun overlap(x: Int, y: Int, side: Int): Int {
            val w = min(x + side, right) - max(x, left)
            val h = min(y + side, bottom) - max(y, top)
            return if (w > 0 && h > 0) w * h else 0
        }

        fun contains(x: Int, y: Int): Boolean = x in left until right && y in top until bottom

        fun including(faces: List<ArtworkCropFace>, width: Int, height: Int): ContentBounds = faces.fold(this) { bounds, face ->
            val radius = face.radius * width
            ContentBounds(
                min(bounds.left, (face.centerX * width - radius).toInt().coerceAtLeast(0)),
                min(bounds.top, (face.centerY * height - radius).toInt().coerceAtLeast(0)),
                max(bounds.right, (face.centerX * width + radius).toInt().coerceAtMost(width)),
                max(bounds.bottom, (face.centerY * height + radius).toInt().coerceAtMost(height))
            )
        }
    }

    private fun centeredOn(content: ContentBounds, width: Int, height: Int, allowZoom: Boolean): ArtworkCropRect {
        val side = if (allowZoom) min(content.width, content.height) else min(width, height)
        val x = ((content.left + content.right - side) / 2).coerceIn(0, width - side)
        val y = ((content.top + content.bottom - side) / 2).coerceIn(0, height - side)
        return ArtworkCropRect(x.toFloat() / width, y.toFloat() / height, side.toFloat() / width, side.toFloat() / height)
    }

    internal fun contentBounds(pixels: IntArray, width: Int, height: Int): ContentBounds {
        val maxRows = (height * MAX_BAR_SHARE).toInt()
        val maxColumns = (width * MAX_BAR_SHARE).toInt()
        var top = 0
        while (top < maxRows && lineUniform(pixels, width, height, top, rows = true)) top += 1
        var bottom = height
        while (height - bottom < maxRows && lineUniform(pixels, width, height, bottom - 1, rows = true)) bottom -= 1
        var left = 0
        while (left < maxColumns && lineUniform(pixels, width, height, left, rows = false)) left += 1
        var right = width
        while (width - right < maxColumns && lineUniform(pixels, width, height, right - 1, rows = false)) right -= 1
        if (bottom - top < 2 || right - left < 2) return ContentBounds(0, 0, width, height)
        return ContentBounds(left, top, right, bottom)
    }

    private fun lineUniform(pixels: IntArray, width: Int, height: Int, line: Int, rows: Boolean): Boolean {
        val count = if (rows) width else height
        var sum = 0f
        var sumSquares = 0f
        for (i in 0 until count) {
            val l = luminance(if (rows) pixels[line * width + i] else pixels[i * width + line])
            sum += l
            sumSquares += l * l
        }
        val mean = sum / count
        return sqrt((sumSquares / count - mean * mean).coerceAtLeast(0f)) <= UNIFORM_LINE_DEVIATION
    }

    private fun saliencyMap(
        pixels: IntArray,
        width: Int,
        height: Int,
        content: ContentBounds,
        faces: List<ArtworkCropFace>
    ): FloatArray {
        val luma = FloatArray(width * height) { luminance(pixels[it]) }
        val map = FloatArray(width * height)
        for (y in content.top until content.bottom) {
            for (x in content.left until content.right) {
                val index = y * width + x
                val l = luma[index]
                val right = if (content.contains(x + 1, y)) luma[index + 1] else l
                val left = if (content.contains(x - 1, y)) luma[index - 1] else l
                val down = if (content.contains(x, y + 1)) luma[index + width] else l
                val up = if (content.contains(x, y - 1)) luma[index - width] else l
                val edge = (abs(4f * l - right - left - down - up) / 255f).coerceIn(0f, 1f)
                val color = pixels[index]
                val midTone = if (l in 18f..238f) 1f else 0f
                map[index] = 0.6f * edge +
                    0.22f * chroma(color) * midTone * (0.35f + edge) +
                    0.9f * skinLikelihood(color) * (0.3f + edge)
            }
        }
        faces.forEach { face ->
            val cx = face.centerX * width
            val cy = face.centerY * height
            val radius = (face.radius * width).coerceAtLeast(1f)
            val x0 = (cx - radius).toInt().coerceAtLeast(content.left)
            val x1 = (cx + radius).toInt().coerceAtMost(content.right - 1)
            val y0 = (cy - radius).toInt().coerceAtLeast(content.top)
            val y1 = (cy + radius).toInt().coerceAtMost(content.bottom - 1)
            for (y in y0..y1) for (x in x0..x1) {
                val dx = (x - cx) / radius
                val dy = (y - cy) / radius
                val falloff = 1f - (dx * dx + dy * dy)
                if (falloff > 0f) map[y * width + x] += FACE_WEIGHT * falloff
            }
        }
        return map
    }

    private fun luminance(color: Int): Float =
        0.299f * (color shr 16 and 0xFF) + 0.587f * (color shr 8 and 0xFF) + 0.114f * (color and 0xFF)

    private fun chroma(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        return (max(r, max(g, b)) - min(r, min(g, b))) / 255f
    }

    private fun skinLikelihood(color: Int): Float {
        val r = (color shr 16 and 0xFF).toFloat()
        val g = (color shr 8 and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        if (y < 45f || y > 245f) return 0f
        val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
        val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
        val dCb = (cb - 104f) / 22f
        val dCr = (cr - 152f) / 18f
        return (1f - (dCb * dCb + dCr * dCr)).coerceIn(0f, 1f)
    }

    private class IntegralImage(values: FloatArray, width: Int, height: Int) {
        private val stride = width + 1
        private val table = FloatArray(stride * (height + 1))

        init {
            for (y in 0 until height) {
                var row = 0f
                for (x in 0 until width) {
                    row += values[y * width + x]
                    table[(y + 1) * stride + x + 1] = table[y * stride + x + 1] + row
                }
            }
        }

        fun sum(x: Int, y: Int, w: Int, h: Int): Float =
            table[(y + h) * stride + x + w] - table[y * stride + x + w] - table[(y + h) * stride + x] + table[y * stride + x]
    }
}

internal object ArtworkCropPolicy {
    @Volatile
    var mode: ArtworkCropMode = ArtworkCropMode.Center
}

internal val ArtworkCropExempt = Extras.Key(default = false)

internal class ArtworkCropInterceptor(private val smartCropAllowed: Boolean) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val mode = ArtworkCropPolicy.mode
        if (!smartCropAllowed || mode == ArtworkCropMode.Center || chain.request.getExtra(ArtworkCropExempt)) return chain.proceed()
        if (chain.request.transformations.isNotEmpty() || !chain.size.isSquareSlot()) return chain.proceed()
        val request = chain.request.newBuilder()
            .transformations(
                SmartArtworkCropTransformation(
                    allowZoom = mode == ArtworkCropMode.Smart,
                    hardwareOutput = chain.request.allowHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                )
            )
            .build()
        return chain.withRequest(request).proceed()
    }

    private fun Size.isSquareSlot(): Boolean {
        val w = (width as? Dimension.Pixels)?.px ?: return false
        val h = (height as? Dimension.Pixels)?.px ?: return false
        return SmartArtworkCropPlanner.isSquare(w, h)
    }
}

internal class SmartArtworkCropTransformation(
    private val allowZoom: Boolean,
    private val hardwareOutput: Boolean = false
) : Transformation() {
    override val cacheKey: String = "levyra-smart-crop-v1-${if (allowZoom) "zoom" else "fixed"}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val cropped = cropToSquare(input)
        if (!hardwareOutput || cropped.config == Bitmap.Config.HARDWARE) return cropped
        return cropped.copy(Bitmap.Config.HARDWARE, false) ?: cropped
    }

    private fun cropToSquare(input: Bitmap): Bitmap {
        if (SmartArtworkCropPlanner.isSquare(input.width, input.height)) return input
        val source = input.softwareCopy()
        val rect = runCatching { planCrop(source) }.getOrElse { SmartArtworkCropPlanner.center(source.width, source.height) }
        val side = min(
            min((rect.width * source.width).roundToInt(), (rect.height * source.height).roundToInt()),
            min(source.width, source.height)
        ).coerceAtLeast(1)
        val left = (rect.left * source.width).roundToInt().coerceIn(0, source.width - side)
        val top = (rect.top * source.height).roundToInt().coerceIn(0, source.height - side)
        return Bitmap.createBitmap(source, left, top, side, side)
    }

    private fun planCrop(source: Bitmap): ArtworkCropRect {
        val scale = ANALYSIS_SIDE.toFloat() / max(source.width, source.height)
        val width = (source.width * scale).roundToInt().coerceAtLeast(4)
        val height = (source.height * scale).roundToInt().coerceAtLeast(4)
        val analysis = Bitmap.createScaledBitmap(source, width, height, true)
        val pixels = IntArray(width * height)
        analysis.getPixels(pixels, 0, width, 0, 0, width, height)
        val faces = if (SmartArtworkCropPlanner.skinShare(pixels, width, height) >= FACE_SEARCH_SKIN_SHARE) {
            detectFaces(source)
        } else {
            emptyList()
        }
        return SmartArtworkCropPlanner.plan(pixels, width, height, allowZoom, faces)
    }

    private fun Bitmap.softwareCopy(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this

    private fun detectFaces(source: Bitmap): List<ArtworkCropFace> = runCatching {
        val scale = FACE_SIDE.toFloat() / max(source.width, source.height)
        val width = ((source.width * scale).roundToInt() / 2 * 2).coerceAtLeast(2)
        val height = (source.height * scale).roundToInt().coerceAtLeast(2)
        val faceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        Canvas(faceBitmap).drawBitmap(source, null, android.graphics.Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
        val found = arrayOfNulls<FaceDetector.Face>(MAX_FACES)
        val count = FaceDetector(width, height, MAX_FACES).findFaces(faceBitmap, found)
        val point = android.graphics.PointF()
        found.take(count).filterNotNull().filter { it.confidence() >= MIN_FACE_CONFIDENCE }.map { face ->
            face.getMidPoint(point)
            ArtworkCropFace(point.x / width, point.y / height, face.eyesDistance() * FACE_RADIUS_EYES / width)
        }
    }.getOrDefault(emptyList())

    override fun equals(other: Any?): Boolean = other is SmartArtworkCropTransformation && other.allowZoom == allowZoom

    override fun hashCode(): Int = allowZoom.hashCode()

    private companion object {
        const val ANALYSIS_SIDE = 96
        const val FACE_SIDE = 320
        const val MAX_FACES = 3
        const val MIN_FACE_CONFIDENCE = 0.3f
        const val FACE_RADIUS_EYES = 1.8f
        const val FACE_SEARCH_SKIN_SHARE = 0.01f
    }
}
