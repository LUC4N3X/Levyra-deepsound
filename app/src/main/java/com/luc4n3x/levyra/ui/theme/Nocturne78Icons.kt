package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object Nocturne78Icons {
    val Play = lineIcon("Play", "M8,5 L19,12 L8,19 Z")
    val Pause = lineIcon("Pause", "M8,5 L8,19 M16,5 L16,19")
    private val Previous = lineIcon("Previous", "M5,5 L5,19 M19,5 L8,12 L19,19 Z")
    private val Next = lineIcon("Next", "M19,5 L19,19 M5,5 L16,12 L5,19 Z")
    private val Heart = lineIcon("Heart", "M12,20 C10,18 3,14 3,8 C3,3 10,2 12,7 C14,2 21,3 21,8 C21,14 14,18 12,20 Z")
    private val Queue = lineIcon("Queue", "M3,5 L17,5 M3,10 L17,10 M3,15 L11,15 M16,13 L21,16 L16,19 Z")
    private val Record = lineIcon("Record", "M12,3 A9,9 0,1 1,11.99,3 Z M12,9 A3,3 0,1 1,11.99,9 Z M12,5 A7,7 0,0 1,19,12")
    private val Note = lineIcon("Note", "M10,17 L10,5 L20,3 L20,15 M10,17 C10,21 3,22 3,18 C3,14 10,14 10,17 M20,15 C20,19 13,20 13,16 C13,12 20,12 20,15")
    private val Artist = lineIcon("Artist", "M12,3 A4,4 0,1 1,11.99,3 Z M4,21 L4,18 C4,11 20,11 20,18 L20,21 Z")
    private val Download = lineIcon("Download", "M12,3 L12,15 M7,10 L12,15 L17,10 M4,16 L4,21 L20,21 L20,16")
    private val Home = lineIcon("Home", "M3,11 L12,3 L21,11 M5,9 L5,21 L10,21 L10,15 L14,15 L14,21 L19,21 L19,9")
    private val Search = lineIcon("Search", "M10,3 A7,7 0,1 1,9.99,3 Z M15,15 L21,21")
    private val Explore = lineIcon("Explore", "M12,3 A9,9 0,1 1,11.99,3 Z M16,8 L14,14 L8,16 L10,10 Z")
    private val Library = lineIcon("Library", "M4,4 L4,20 L20,20 M8,4 L8,16 L20,16 L20,4 Z M14,7 A3,3 0,1 1,13.99,7 Z")

    fun resolve(icon: ImageVector): ImageVector = when (icon.name.substringAfterLast('.')) {
        "PlayArrow" -> Play
        "Pause" -> Pause
        "SkipPrevious" -> Previous
        "SkipNext" -> Next
        "Favorite", "FavoriteBorder" -> Heart
        "QueueMusic", "PlaylistPlay" -> Queue
        "Album" -> Record
        "MusicNote" -> Note
        "Mic" -> Artist
        "Download", "DownloadDone" -> Download
        "Home" -> Home
        "Search" -> Search
        "Explore" -> Explore
        "LibraryMusic" -> Library
        else -> icon
    }

    private fun lineIcon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = "Nocturne78.$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
    ).build()
}
