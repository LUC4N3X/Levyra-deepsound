package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val ArtworkMeshGrid = 6
internal const val ArtworkMeshTexture = 32
internal const val ArtworkMeshSourcePx = 120
private const val MeshVibrance = 1.12f
private const val MeshLightnessFloor = 0.045f

@Immutable
internal class ArtworkMeshTexels(val texels: IntArray, val topEdge: IntArray, val topLuminance: Float)

internal fun buildArtworkMesh(pixels: IntArray, width: Int, height: Int, seed: Int): ArtworkMeshTexels? {
    if (width < 1 || height < 1 || pixels.size < width * height) return null
    val cols = ArtworkMeshGrid
    val rows = ArtworkMeshGrid
    val cells = cols * rows
    val red = LongArray(cells)
    val green = LongArray(cells)
    val blue = LongArray(cells)
    val count = IntArray(cells)
    for (y in 0 until height) {
        val rowBase = ((height - 1 - y) * rows / height) * cols
        for (x in 0 until width) {
            val cell = rowBase + x * cols / width
            val p = pixels[y * width + x]
            red[cell] += ((p shr 16) and 0xFF).toLong()
            green[cell] += ((p shr 8) and 0xFF).toLong()
            blue[cell] += (p and 0xFF).toLong()
            count[cell]++
        }
    }
    val grid = IntArray(cells) { c ->
        val n = count[c].coerceAtLeast(1)
        lifted(argb((red[c] / n).toInt(), (green[c] / n).toInt(), (blue[c] / n).toInt()))
    }
    val topRow = (rows - 1) * cols until rows * cols
    val topPixels = topRow.sumOf { count[it].toLong() }.coerceAtLeast(1L)
    val topLuminance = (topRow.sumOf { 0.2126 * red[it] + 0.7152 * green[it] + 0.0722 * blue[it] } / topPixels / 255.0)
        .toFloat().coerceIn(0f, 1f)
    val texels = resample(rotateBelowSeam(grid, cols, rows, seed), cols, rows, ArtworkMeshTexture)
    val topEdge = resample(grid.copyOfRange((rows - 1) * cols, rows * cols), cols, 1, ArtworkMeshTexture)
        .copyOfRange(0, ArtworkMeshTexture)
    return ArtworkMeshTexels(texels, topEdge, topLuminance)
}

private fun rotateBelowSeam(grid: IntArray, cols: Int, rows: Int, seed: Int): IntArray {
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = grid.copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val src = if (mirror) cols - 1 - x else x
            out[base + x] = grid[base + (src + shift) % cols]
        }
    }
    return out
}

private fun resample(grid: IntArray, cols: Int, rows: Int, size: Int): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            val upper = lerpArgb(grid[y0 * cols + x0], grid[y0 * cols + x1], wx)
            val lower = lerpArgb(grid[y1 * cols + x0], grid[y1 * cols + x1], wx)
            out[ty * size + tx] = lerpArgb(upper, lower, wy)
        }
    }
    return out
}

private fun smoothstep(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

private fun lerpArgb(from: Int, to: Int, t: Float): Int {
    if (t <= 0f) return from
    if (t >= 1f) return to
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
    return argb(channel(16), channel(8), channel(0))
}

private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

private fun lifted(color: Int): Int {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val lightness = (maxC + minC) / 2f
    val delta = maxC - minC
    if (delta == 0f) {
        val grey = (lightness.coerceAtLeast(MeshLightnessFloor) * 255f).roundToInt()
        return argb(grey, grey, grey)
    }
    val saturation = (delta / (1f - abs(2f * lightness - 1f)) * MeshVibrance).coerceIn(0f, 1f)
    val hue = when (maxC) {
        r -> ((g - b) / delta).mod(6f)
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f
    return hsl(hue, saturation, lightness.coerceIn(MeshLightnessFloor, 1f))
}

private fun hsl(h: Float, s: Float, l: Float): Int {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = l - c / 2f
    val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun byte(v: Float): Int = ((v + m) * 255f).roundToInt().coerceIn(0, 255)
    return argb(byte(r), byte(g), byte(b))
}
