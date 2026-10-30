package com.luc4n3x.levyra.ui.atelier

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
internal data class AtelierShelfItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val tracks: List<Track> = emptyList(),
    val album: AlbumHit? = null,
    val playlistId: String? = null
) {
    val letter: Char get() = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '#'
}

private const val ShelfCapacity = 160

private fun normalized(value: String): String = value.trim().lowercase(Locale.ROOT)

internal fun buildAtelierShelf(
    libraryTracks: List<Track>,
    playlists: List<Playlist>,
    suggestedAlbums: List<AlbumHit>
): List<AtelierShelfItem> {
    val grouped = LinkedHashMap<String, MutableList<Track>>()
    libraryTracks.forEach { track ->
        if (track.album.isBlank()) return@forEach
        grouped.getOrPut("${normalized(track.album)}|${normalized(track.artist)}") { ArrayList() }
            .takeIf { group -> group.none { it.id == track.id } }?.add(track)
    }
    val albums = grouped.mapNotNull { (key, tracks) ->
        val first = tracks.first()
        val cover = first.largeThumbnailUrl.ifBlank { first.thumbnailUrl }
        if (cover.isBlank()) null
        else AtelierShelfItem("album:$key", first.album, first.artist, cover, tracks = tracks)
    }
    val owned = playlists.filter { !it.hidden && it.tracks.isNotEmpty() }.map { playlist ->
        val first = playlist.tracks.first()
        AtelierShelfItem(
            "playlist:${playlist.id}", playlist.name, first.artist,
            playlist.coverUrl.ifBlank { first.largeThumbnailUrl.ifBlank { first.thumbnailUrl } },
            tracks = playlist.tracks, playlistId = playlist.id
        )
    }.filter { it.coverUrl.isNotBlank() }
    val known = albums.mapTo(HashSet()) { it.key }
    val suggested = suggestedAlbums.filter { it.thumbnailUrl.isNotBlank() }.mapNotNull { hit ->
        val key = "album:${normalized(hit.title)}|${normalized(hit.artist)}"
        if (!known.add(key)) null else AtelierShelfItem(key, hit.title, hit.artist, hit.thumbnailUrl, album = hit)
    }
    return (albums + owned + suggested)
        .sortedBy { normalized(it.title) }
        .take(ShelfCapacity)
}

internal fun AtelierShelfItem.contains(track: Track?): Boolean =
    track != null && tracks.any { it.id == track.id }

internal fun AtelierShelfItem.albumDetail(): AlbumHit =
    album ?: AlbumHit(title, subtitle, "", coverUrl, "$title $subtitle")

internal fun atelierShelfLetterMarks(items: List<AtelierShelfItem>): List<Pair<Int, Char>> {
    val marks = ArrayList<Pair<Int, Char>>()
    items.forEachIndexed { index, item ->
        if (marks.lastOrNull()?.second != item.letter) marks += index to item.letter
    }
    return marks
}

internal fun atelierSmooth(edge0: Float, edge1: Float, value: Float): Float {
    if (edge0 >= edge1) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * t * (t * (t * 6f - 15f) + 10f)
}

internal fun atelierLerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

internal object AtelierSlot {
    const val X = 0
    const val Y = 1
    const val Z = 2
    const val YAW = 3
    const val PITCH = 4
    const val ROLL = 5
    const val ALPHA = 6
    const val SIZE = 7

    const val CameraDistance = 3.2f
    const val FrontWidthFraction = 0.56f
    const val VisibleDepth = 7f

    private const val FrontX = 0.14f
    private const val FrontY = 0.27f
    private const val FrontZ = -0.55f
    private const val FrontYaw = -11f
    private const val FrontPitch = 2.5f
    private const val FrontRoll = -1.4f
    private const val SideYaw = 50f

    fun fill(depth: Float, enter: Float, out: FloatArray) {
        val entry = enter.coerceIn(0f, 1f)
        val advance = atelierSmooth(0.56f, 1f, entry)
        val d = depth + (1f - advance) * 0.95f
        val fly = atelierSmooth(0.04f + min(abs(depth), 5f) * 0.035f, 0.62f, entry)
        val side = if (d >= 0f) 1f else -1f
        val reach = abs(d)
        val blend = atelierSmooth(0f, 1f, min(reach, 1f))
        val beyond = max(reach - 1f, 0f)
        val sideX = if (side > 0f) -0.06f + beyond * 0.17f else -0.72f - beyond * 0.17f
        val sideY = -0.33f - beyond * 0.035f
        val sideZ = 0.02f + beyond * 0.34f
        val sideYaw = if (side > 0f) -SideYaw else SideYaw
        out[X] = atelierLerp(FrontX, sideX, blend) + (1f - fly) * 1.15f
        out[Y] = atelierLerp(FrontY, sideY, blend) - (1f - fly) * 0.7f
        out[Z] = atelierLerp(FrontZ, sideZ, blend) + (1f - fly) * 1.8f
        out[YAW] = atelierLerp(FrontYaw, sideYaw, blend)
        out[PITCH] = atelierLerp(FrontPitch, 0f, blend)
        out[ROLL] = atelierLerp(FrontRoll, 0f, blend)
        val edge = 1f - atelierSmooth(VisibleDepth - 2.5f, VisibleDepth, reach)
        out[ALPHA] = edge * fly
    }
}

internal class AtelierCamera {
    var centerX = 0f
    var centerY = 0f
    var focal = 1f

    fun frame(width: Float, height: Float, verticalAnchor: Float) {
        centerX = width * 0.5f
        centerY = height * verticalAnchor
        focal = min(width, height * 0.62f) * AtelierSlot.FrontWidthFraction * (AtelierSlot.CameraDistance - 0.55f)
    }

    fun project(slot: FloatArray, quad: FloatArray) {
        val yaw = Math.toRadians(slot[AtelierSlot.YAW].toDouble())
        val pitch = Math.toRadians(slot[AtelierSlot.PITCH].toDouble())
        val roll = Math.toRadians(slot[AtelierSlot.ROLL].toDouble())
        val cy = cos(yaw).toFloat()
        val sy = sin(yaw).toFloat()
        val cp = cos(pitch).toFloat()
        val sp = sin(pitch).toFloat()
        val cr = cos(roll).toFloat()
        val sr = sin(roll).toFloat()
        for (corner in 0 until 4) {
            val u = if (corner == 0 || corner == 3) -0.5f else 0.5f
            val v = if (corner < 2) -0.5f else 0.5f
            val rx = u * cr - v * sr
            val ry = u * sr + v * cr
            val yawX = rx * cy
            val yawZ = -rx * sy
            val pitchY = ry * cp - yawZ * sp
            val pitchZ = ry * sp + yawZ * cp
            val depth = (slot[AtelierSlot.Z] + pitchZ + AtelierSlot.CameraDistance).coerceAtLeast(0.2f)
            val scale = focal / depth
            quad[corner * 2] = centerX + (slot[AtelierSlot.X] + yawX) * scale
            quad[corner * 2 + 1] = centerY + (slot[AtelierSlot.Y] + pitchY) * scale
        }
    }
}

internal fun atelierQuadContains(quad: FloatArray, x: Float, y: Float): Boolean {
    var positive = 0
    var negative = 0
    for (corner in 0 until 4) {
        val next = (corner + 1) % 4
        val cross = (quad[next * 2] - quad[corner * 2]) * (y - quad[corner * 2 + 1]) -
            (quad[next * 2 + 1] - quad[corner * 2 + 1]) * (x - quad[corner * 2])
        if (cross >= 0f) positive++ else negative++
    }
    return positive == 0 || negative == 0
}

internal fun atelierAmbientFrom(pixels: IntArray): Pair<Int, Int> {
    var bestSaturation = -1f
    var primary = 0xFF2A2556.toInt()
    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    pixels.forEach { pixel ->
        val r = pixel shr 16 and 0xFF
        val g = pixel shr 8 and 0xFF
        val b = pixel and 0xFF
        redSum += r
        greenSum += g
        blueSum += b
        val high = max(r, max(g, b))
        val low = min(r, min(g, b))
        val saturation = if (high == 0) 0f else (high - low) / high.toFloat() * sqrt(high / 255f)
        if (saturation > bestSaturation) {
            bestSaturation = saturation
            primary = pixel
        }
    }
    val count = pixels.size.coerceAtLeast(1)
    val mean = (0xFF shl 24) or ((redSum / count).toInt() shl 16) or ((greenSum / count).toInt() shl 8) or (blueSum / count).toInt()
    return atelierAmbientTone(primary, 0.42f) to atelierAmbientTone(mean, 0.30f)
}

private fun atelierAmbientTone(color: Int, targetValue: Float): Int {
    val r = (color shr 16 and 0xFF) / 255f
    val g = (color shr 8 and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val high = max(r, max(g, b)).coerceAtLeast(0.01f)
    val scale = targetValue / high
    fun channel(value: Float) = (value * scale).coerceIn(0f, 1f).times(255f).toInt()
    return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}

internal object AtelierDeckGeometry {
    const val Aspect = 0.74f
    const val PlatterX = 0.405f
    const val PlatterY = 0.355f
    const val PlatterRadius = 0.315f
    const val RecordRadius = 0.292f
    const val LabelRadius = 0.098f
    const val PivotX = 0.865f
    const val PivotY = 0.155f
    const val ArmLength = 0.50f
    const val RestAngle = -3f
    const val OuterGroove = 0.282f
    const val InnerGroove = 0.122f

    fun armAngleForRadius(radius: Float): Float {
        var low = 0f
        var high = 60f
        repeat(18) {
            val middle = (low + high) * 0.5f
            val radians = Math.toRadians(middle.toDouble())
            val x = PivotX - ArmLength * sin(radians).toFloat() - PlatterX
            val y = PivotY + ArmLength * cos(radians).toFloat() - PlatterY
            if (sqrt(x * x + y * y) > radius) low = middle else high = middle
        }
        return (low + high) * 0.5f
    }

    fun grooveRadius(progress: Float): Float = atelierLerp(OuterGroove, InnerGroove, progress.coerceIn(0f, 1f))
}

internal object AtelierStageClock {
    fun scrim(clock: Float) = atelierSmooth(0.08f, 0.46f, clock)
    fun sleeve(clock: Float) = atelierSmooth(0f, 0.46f, clock)
    fun deckRise(clock: Float) = atelierSmooth(0.18f, 0.64f, clock)
    fun deckAlpha(clock: Float) = atelierSmooth(0.18f, 0.42f, clock)
    fun recordFlight(clock: Float) = atelierSmooth(0.46f, 0.90f, clock)
    fun recordAlpha(clock: Float) = atelierSmooth(0.28f, 0.40f, clock)
    fun armEngage(clock: Float) = atelierSmooth(0.90f, 1f, clock)
    fun controls(clock: Float) = atelierSmooth(0.80f, 1f, clock)
}
