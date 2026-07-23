package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.LruCache
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class ArtworkPalette(
    val start: Int,
    val end: Int
)

internal object ArtworkPaletteCache {
    private const val preferencesName = "levyra_artwork_palette_v1"
    private const val orderKey = "__order__"
    private const val persistentLimit = 128
    private const val memoryLimit = 96
    private val memory = LruCache<String, ArtworkPalette>(memoryLimit)
    private val lock = Any()

    fun key(
        trackId: String,
        thumbnailUrl: String,
        largeThumbnailUrl: String
    ): String {
        val source = "$trackId|$thumbnailUrl|$largeThumbnailUrl"
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    fun get(context: Context, key: String): ArtworkPalette? {
        synchronized(lock) {
            memory.get(key)?.let { return it }
            val encoded = context.applicationContext
                .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .getString(key, null)
                ?: return null
            val palette = decode(encoded) ?: return null
            memory.put(key, palette)
            return palette
        }
    }

    fun put(context: Context, key: String, palette: ArtworkPalette) {
        synchronized(lock) {
            memory.put(key, palette)
            val preferences = context.applicationContext
                .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            val currentOrder = preferences
                .getString(orderKey, "")
                .orEmpty()
                .split(',')
                .filter { it.isNotBlank() && it != key }
            val newOrder = buildList {
                add(key)
                addAll(currentOrder.take(persistentLimit - 1))
            }
            val evicted = currentOrder.drop(persistentLimit - 1)
            preferences.edit().apply {
                putString(key, encode(palette))
                putString(orderKey, newOrder.joinToString(","))
                evicted.forEach { remove(it) }
            }.apply()
        }
    }

    fun extract(
        bitmap: Bitmap,
        fallbackStart: Int,
        fallbackEnd: Int
    ): ArtworkPalette {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            return ArtworkPalette(fallbackStart, fallbackEnd)
        }
        val sample = if (bitmap.width > 96 || bitmap.height > 96) {
            Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        } else {
            bitmap
        }
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        val buckets = HashMap<Int, ColorBucket>(96)
        val hsv = FloatArray(3)
        pixels.forEach { pixel ->
            if (AndroidColor.alpha(pixel) < 210) return@forEach
            val red = AndroidColor.red(pixel)
            val green = AndroidColor.green(pixel)
            val blue = AndroidColor.blue(pixel)
            AndroidColor.RGBToHSV(red, green, blue, hsv)
            val saturation = hsv[1]
            val value = hsv[2]
            if (value < 0.11f || value > 0.97f || saturation < 0.13f) return@forEach
            val hueBin = (hsv[0] / 15f).toInt().coerceIn(0, 23)
            val saturationBin = (saturation * 4f).toInt().coerceIn(0, 3)
            val valueBin = (value * 4f).toInt().coerceIn(0, 3)
            val bucketKey = hueBin * 16 + saturationBin * 4 + valueBin
            val weight = (0.42f + saturation * 1.58f) * (0.58f + value * 0.92f)
            buckets.getOrPut(bucketKey) { ColorBucket() }.add(red, green, blue, weight)
        }
        if (buckets.isEmpty()) {
            if (sample !== bitmap) sample.recycle()
            return ArtworkPalette(fallbackStart, fallbackEnd)
        }
        val candidates = buckets.values
            .filter { it.weight > 0f }
            .sortedByDescending { it.score() }
            .map { it.toCandidate() }
        val primary = candidates.firstOrNull()
        if (primary == null) {
            if (sample !== bitmap) sample.recycle()
            return ArtworkPalette(fallbackStart, fallbackEnd)
        }
        val secondary = candidates.drop(1).firstOrNull { candidate ->
            hueDistance(primary.hue, candidate.hue) >= 34f && colorDistance(primary, candidate) >= 72f
        } ?: candidates.drop(1).firstOrNull { candidate ->
            hueDistance(primary.hue, candidate.hue) >= 20f
        }
        val start = normalize(primary, valueFloor = 0.48f, valueCeiling = 0.76f)
        val end = if (secondary != null) {
            normalize(secondary, valueFloor = 0.38f, valueCeiling = 0.68f)
        } else {
            deriveCompanion(primary)
        }
        if (sample !== bitmap) sample.recycle()
        return ArtworkPalette(start, end)
    }

    private fun encode(palette: ArtworkPalette): String {
        return "%08X,%08X".format(palette.start, palette.end)
    }

    private fun decode(value: String): ArtworkPalette? {
        val parts = value.split(',')
        if (parts.size != 2) return null
        return runCatching {
            ArtworkPalette(
                start = parts[0].toLong(16).toInt(),
                end = parts[1].toLong(16).toInt()
            )
        }.getOrNull()
    }

    private fun normalize(
        candidate: ColorCandidate,
        valueFloor: Float,
        valueCeiling: Float
    ): Int {
        val saturation = candidate.saturation.coerceIn(0.48f, 0.88f)
        val value = candidate.value.coerceIn(valueFloor, valueCeiling)
        return AndroidColor.HSVToColor(floatArrayOf(candidate.hue, saturation, value))
    }

    private fun deriveCompanion(primary: ColorCandidate): Int {
        val hue = when {
            primary.hue < 55f -> primary.hue + 42f
            primary.hue > 305f -> primary.hue - 42f
            else -> primary.hue + if (primary.hue < 180f) 52f else -52f
        }.let { raw ->
            when {
                raw < 0f -> raw + 360f
                raw >= 360f -> raw - 360f
                else -> raw
            }
        }
        val saturation = (primary.saturation * 0.88f).coerceIn(0.46f, 0.82f)
        val value = (primary.value * 0.78f).coerceIn(0.38f, 0.64f)
        return AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    private fun hueDistance(first: Float, second: Float): Float {
        val distance = abs(first - second)
        return min(distance, 360f - distance)
    }

    private fun colorDistance(first: ColorCandidate, second: ColorCandidate): Float {
        val red = first.red - second.red
        val green = first.green - second.green
        val blue = first.blue - second.blue
        return kotlin.math.sqrt((red * red + green * green + blue * blue).toFloat())
    }

    private class ColorBucket {
        var red = 0f
        var green = 0f
        var blue = 0f
        var weight = 0f
        var count = 0

        fun add(red: Int, green: Int, blue: Int, sampleWeight: Float) {
            this.red += red * sampleWeight
            this.green += green * sampleWeight
            this.blue += blue * sampleWeight
            weight += sampleWeight
            count += 1
        }

        fun score(): Float {
            val averageWeight = weight / max(1, count)
            return weight * (0.72f + averageWeight * 0.28f)
        }

        fun toCandidate(): ColorCandidate {
            val safeWeight = max(weight, 0.0001f)
            val redValue = (red / safeWeight).toInt().coerceIn(0, 255)
            val greenValue = (green / safeWeight).toInt().coerceIn(0, 255)
            val blueValue = (blue / safeWeight).toInt().coerceIn(0, 255)
            val hsv = FloatArray(3)
            AndroidColor.RGBToHSV(redValue, greenValue, blueValue, hsv)
            return ColorCandidate(
                red = redValue,
                green = greenValue,
                blue = blueValue,
                hue = hsv[0],
                saturation = hsv[1],
                value = hsv[2]
            )
        }
    }

    private data class ColorCandidate(
        val red: Int,
        val green: Int,
        val blue: Int,
        val hue: Float,
        val saturation: Float,
        val value: Float
    )
}
