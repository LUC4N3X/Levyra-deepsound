package com.luc4n3x.levyra.desktop.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.awt.image.BufferedImage
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object ArtworkPalette {
    private const val SAMPLE_STEP = 4
    private const val HUE_BUCKETS = 24
    private const val MIN_SATURATION = 0.22f
    private const val MIN_BRIGHTNESS = 0.18f
    private const val MAX_BRIGHTNESS = 0.96f

    private val cache = ConcurrentHashMap<String, Optional<Color>>()

    suspend fun accentFor(url: String): Color? {
        if (url.isBlank()) return null
        cache[url]?.let { return it.orElse(null) }
        val accent = withContext(Dispatchers.IO) {
            val image = download(url) ?: return@withContext null
            dominant(image)
        }
        cache[url] = Optional.ofNullable(accent)
        return accent
    }

    fun dominant(image: BufferedImage): Color? {
        val bucketWeights = FloatArray(HUE_BUCKETS)
        val bucketRed = FloatArray(HUE_BUCKETS)
        val bucketGreen = FloatArray(HUE_BUCKETS)
        val bucketBlue = FloatArray(HUE_BUCKETS)
        val hsb = FloatArray(3)
        var fallback: Color? = null
        var fallbackScore = -1f

        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                val rgb = image.getRGB(x, y)
                val red = (rgb shr 16) and 0xFF
                val green = (rgb shr 8) and 0xFF
                val blue = rgb and 0xFF
                java.awt.Color.RGBtoHSB(red, green, blue, hsb)
                val saturation = hsb[1]
                val brightness = hsb[2]

                val score = saturation * brightness
                if (score > fallbackScore) {
                    fallbackScore = score
                    fallback = Color(red / 255f, green / 255f, blue / 255f)
                }

                if (saturation >= MIN_SATURATION && brightness in MIN_BRIGHTNESS..MAX_BRIGHTNESS) {
                    val bucket = ((hsb[0] * HUE_BUCKETS).toInt()).coerceIn(0, HUE_BUCKETS - 1)
                    val weight = saturation * brightness
                    bucketWeights[bucket] += weight
                    bucketRed[bucket] += red * weight
                    bucketGreen[bucket] += green * weight
                    bucketBlue[bucket] += blue * weight
                }
                x += SAMPLE_STEP
            }
            y += SAMPLE_STEP
        }

        val best = bucketWeights.indices.maxByOrNull { bucketWeights[it] } ?: return fallback
        val weight = bucketWeights[best]
        if (weight <= 0f) return fallback
        return readable(
            Color(
                red = (bucketRed[best] / weight / 255f).coerceIn(0f, 1f),
                green = (bucketGreen[best] / weight / 255f).coerceIn(0f, 1f),
                blue = (bucketBlue[best] / weight / 255f).coerceIn(0f, 1f)
            )
        )
    }

    fun readable(color: Color): Color {
        val hsb = java.awt.Color.RGBtoHSB(
            (color.red * 255f).toInt().coerceIn(0, 255),
            (color.green * 255f).toInt().coerceIn(0, 255),
            (color.blue * 255f).toInt().coerceIn(0, 255),
            null
        )
        val saturation = hsb[1].coerceIn(0.45f, 0.95f)
        val brightness = hsb[2].coerceIn(0.62f, 0.98f)
        val rgb = java.awt.Color.HSBtoRGB(hsb[0], saturation, brightness)
        return Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f
        )
    }

    private fun download(url: String): BufferedImage? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .build()
        return runCatching {
            ExtractorHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.byteStream().use(ImageIO::read)
            }
        }.getOrNull()
    }
}
