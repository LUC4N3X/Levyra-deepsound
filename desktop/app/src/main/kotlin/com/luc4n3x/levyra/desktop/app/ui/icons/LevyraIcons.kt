package com.luc4n3x.levyra.desktop.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

private const val CIRCLE_CONTROL = 0.5523f

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    val control = radius * CIRCLE_CONTROL
    moveTo(centerX, centerY - radius)
    curveTo(centerX + control, centerY - radius, centerX + radius, centerY - control, centerX + radius, centerY)
    curveTo(centerX + radius, centerY + control, centerX + control, centerY + radius, centerX, centerY + radius)
    curveTo(centerX - control, centerY + radius, centerX - radius, centerY + control, centerX - radius, centerY)
    curveTo(centerX - radius, centerY - control, centerX - control, centerY - radius, centerX, centerY - radius)
    close()
}

private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathData(block),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()

private fun filledIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathData(block),
        fill = SolidColor(Color.Black)
    ).build()

object LevyraIcons {

    val Play: ImageVector by lazy {
        filledIcon("LevyraPlay") {
            moveTo(7.5f, 4.8f)
            lineTo(19f, 12f)
            lineTo(7.5f, 19.2f)
            close()
        }
    }

    val Pause: ImageVector by lazy {
        filledIcon("LevyraPause") {
            moveTo(7f, 5f)
            lineTo(10.2f, 5f)
            lineTo(10.2f, 19f)
            lineTo(7f, 19f)
            close()
            moveTo(13.8f, 5f)
            lineTo(17f, 5f)
            lineTo(17f, 19f)
            lineTo(13.8f, 19f)
            close()
        }
    }

    val SkipNext: ImageVector by lazy {
        filledIcon("LevyraSkipNext") {
            moveTo(6f, 5f)
            lineTo(15.5f, 12f)
            lineTo(6f, 19f)
            close()
            moveTo(16.8f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 19f)
            lineTo(16.8f, 19f)
            close()
        }
    }

    val SkipPrevious: ImageVector by lazy {
        filledIcon("LevyraSkipPrevious") {
            moveTo(18f, 5f)
            lineTo(8.5f, 12f)
            lineTo(18f, 19f)
            close()
            moveTo(5f, 5f)
            lineTo(7.2f, 5f)
            lineTo(7.2f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }

    val Shuffle: ImageVector by lazy {
        strokeIcon("LevyraShuffle") {
            moveTo(4f, 7f)
            lineTo(8f, 7f)
            lineTo(16f, 17f)
            lineTo(20f, 17f)
            moveTo(17.6f, 14.6f)
            lineTo(20f, 17f)
            lineTo(17.6f, 19.4f)
            moveTo(4f, 17f)
            lineTo(8f, 17f)
            lineTo(10.5f, 13.8f)
            moveTo(14f, 9.4f)
            lineTo(16f, 7f)
            lineTo(20f, 7f)
            moveTo(17.6f, 4.6f)
            lineTo(20f, 7f)
            lineTo(17.6f, 9.4f)
        }
    }

    val Repeat: ImageVector by lazy {
        strokeIcon("LevyraRepeat") {
            moveTo(5f, 11f)
            lineTo(5f, 9f)
            curveTo(5f, 7.9f, 5.9f, 7f, 7f, 7f)
            lineTo(17f, 7f)
            moveTo(15f, 4.8f)
            lineTo(17.2f, 7f)
            lineTo(15f, 9.2f)
            moveTo(19f, 13f)
            lineTo(19f, 15f)
            curveTo(19f, 16.1f, 18.1f, 17f, 17f, 17f)
            lineTo(7f, 17f)
            moveTo(9f, 14.8f)
            lineTo(6.8f, 17f)
            lineTo(9f, 19.2f)
        }
    }

    val RepeatOne: ImageVector by lazy {
        strokeIcon("LevyraRepeatOne") {
            moveTo(5f, 11f)
            lineTo(5f, 9f)
            curveTo(5f, 7.9f, 5.9f, 7f, 7f, 7f)
            lineTo(17f, 7f)
            moveTo(15f, 4.8f)
            lineTo(17.2f, 7f)
            lineTo(15f, 9.2f)
            moveTo(19f, 13f)
            lineTo(19f, 15f)
            curveTo(19f, 16.1f, 18.1f, 17f, 17f, 17f)
            lineTo(7f, 17f)
            moveTo(9f, 14.8f)
            lineTo(6.8f, 17f)
            lineTo(9f, 19.2f)
            moveTo(11f, 11.4f)
            lineTo(12.2f, 10.4f)
            lineTo(12.2f, 14.2f)
        }
    }

    val VolumeHigh: ImageVector by lazy {
        strokeIcon("LevyraVolumeHigh") {
            moveTo(4f, 9.5f)
            lineTo(7.2f, 9.5f)
            lineTo(11.2f, 5.8f)
            lineTo(11.2f, 18.2f)
            lineTo(7.2f, 14.5f)
            lineTo(4f, 14.5f)
            close()
            moveTo(14.6f, 9.4f)
            curveTo(16f, 10.8f, 16f, 13.2f, 14.6f, 14.6f)
            moveTo(17.2f, 7f)
            curveTo(19.7f, 9.5f, 19.7f, 14.5f, 17.2f, 17f)
        }
    }

    val VolumeMuted: ImageVector by lazy {
        strokeIcon("LevyraVolumeMuted") {
            moveTo(4f, 9.5f)
            lineTo(7.2f, 9.5f)
            lineTo(11.2f, 5.8f)
            lineTo(11.2f, 18.2f)
            lineTo(7.2f, 14.5f)
            lineTo(4f, 14.5f)
            close()
            moveTo(14.5f, 9.8f)
            lineTo(19.5f, 14.4f)
            moveTo(19.5f, 9.8f)
            lineTo(14.5f, 14.4f)
        }
    }

    val Queue: ImageVector by lazy {
        strokeIcon("LevyraQueue") {
            moveTo(4f, 6.5f)
            lineTo(16f, 6.5f)
            moveTo(4f, 11f)
            lineTo(16f, 11f)
            moveTo(4f, 15.5f)
            lineTo(11f, 15.5f)
            moveTo(14.5f, 14f)
            lineTo(20f, 17.2f)
            lineTo(14.5f, 20.4f)
            close()
        }
    }

    val Search: ImageVector by lazy {
        strokeIcon("LevyraSearch") {
            circle(11f, 10.6f, 5.4f)
            moveTo(15.2f, 14.8f)
            lineTo(19.8f, 19.6f)
        }
    }

    val Home: ImageVector by lazy {
        strokeIcon("LevyraHome") {
            moveTo(4f, 11f)
            lineTo(12f, 4.4f)
            lineTo(20f, 11f)
            lineTo(20f, 19f)
            lineTo(14.5f, 19f)
            lineTo(14.5f, 14f)
            lineTo(9.5f, 14f)
            lineTo(9.5f, 19f)
            lineTo(4f, 19f)
            close()
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("LevyraSettings") {
            moveTo(4f, 7.5f)
            lineTo(20f, 7.5f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 16.5f)
            lineTo(20f, 16.5f)
            circle(9f, 7.5f, 2.1f)
            circle(15f, 12f, 2.1f)
            circle(8f, 16.5f, 2.1f)
        }
    }

    val Heart: ImageVector by lazy {
        strokeIcon("LevyraHeart") {
            moveTo(12f, 19.2f)
            curveTo(6.4f, 15.4f, 3.8f, 12.6f, 3.8f, 9.5f)
            curveTo(3.8f, 6.9f, 5.8f, 5f, 8.2f, 5f)
            curveTo(9.9f, 5f, 11.2f, 5.9f, 12f, 7.2f)
            curveTo(12.8f, 5.9f, 14.1f, 5f, 15.8f, 5f)
            curveTo(18.2f, 5f, 20.2f, 6.9f, 20.2f, 9.5f)
            curveTo(20.2f, 12.6f, 17.6f, 15.4f, 12f, 19.2f)
            close()
        }
    }

    val HeartFilled: ImageVector by lazy {
        filledIcon("LevyraHeartFilled") {
            moveTo(12f, 19.2f)
            curveTo(6.4f, 15.4f, 3.8f, 12.6f, 3.8f, 9.5f)
            curveTo(3.8f, 6.9f, 5.8f, 5f, 8.2f, 5f)
            curveTo(9.9f, 5f, 11.2f, 5.9f, 12f, 7.2f)
            curveTo(12.8f, 5.9f, 14.1f, 5f, 15.8f, 5f)
            curveTo(18.2f, 5f, 20.2f, 6.9f, 20.2f, 9.5f)
            curveTo(20.2f, 12.6f, 17.6f, 15.4f, 12f, 19.2f)
            close()
        }
    }

    val Add: ImageVector by lazy {
        strokeIcon("LevyraAdd") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("LevyraClose") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    val Trash: ImageVector by lazy {
        strokeIcon("LevyraTrash") {
            moveTo(4.5f, 7f)
            lineTo(19.5f, 7f)
            moveTo(9.5f, 7f)
            lineTo(9.5f, 4.8f)
            lineTo(14.5f, 4.8f)
            lineTo(14.5f, 7f)
            moveTo(6.5f, 7f)
            lineTo(7.4f, 19.4f)
            lineTo(16.6f, 19.4f)
            lineTo(17.5f, 7f)
            moveTo(10.2f, 10f)
            lineTo(10.6f, 16.6f)
            moveTo(13.8f, 10f)
            lineTo(13.4f, 16.6f)
        }
    }

    val Disc: ImageVector by lazy {
        strokeIcon("LevyraDisc") {
            circle(12f, 12f, 8f)
            circle(12f, 12f, 2.4f)
        }
    }

    val Artist: ImageVector by lazy {
        strokeIcon("LevyraArtist") {
            circle(12f, 8.2f, 3.6f)
            moveTo(5.2f, 19.6f)
            curveTo(5.8f, 15.9f, 8.6f, 13.6f, 12f, 13.6f)
            curveTo(15.4f, 13.6f, 18.2f, 15.9f, 18.8f, 19.6f)
        }
    }

    val Playlist: ImageVector by lazy {
        strokeIcon("LevyraPlaylist") {
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 17f)
            lineTo(13f, 17f)
            moveTo(17.5f, 19f)
            lineTo(17.5f, 13.4f)
            lineTo(21f, 12.4f)
            circle(16.2f, 19.2f, 1.6f)
        }
    }

    val Refresh: ImageVector by lazy {
        strokeIcon("LevyraRefresh") {
            moveTo(19f, 12f)
            curveTo(19f, 15.9f, 15.9f, 19f, 12f, 19f)
            curveTo(8.1f, 19f, 5f, 15.9f, 5f, 12f)
            curveTo(5f, 8.1f, 8.1f, 5f, 12f, 5f)
            curveTo(14.5f, 5f, 16.7f, 6.3f, 18f, 8.3f)
            moveTo(18.4f, 4.6f)
            lineTo(18.4f, 8.6f)
            lineTo(14.4f, 8.6f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("LevyraChevronDown") {
            moveTo(6f, 9.5f)
            lineTo(12f, 15.5f)
            lineTo(18f, 9.5f)
        }
    }

    val ChevronUp: ImageVector by lazy {
        strokeIcon("LevyraChevronUp") {
            moveTo(6f, 14.5f)
            lineTo(12f, 8.5f)
            lineTo(18f, 14.5f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        strokeIcon("LevyraChevronLeft") {
            moveTo(14.5f, 5.5f)
            lineTo(8.5f, 12f)
            lineTo(14.5f, 18.5f)
        }
    }

    val More: ImageVector by lazy {
        filledIcon("LevyraMore") {
            circle(12f, 5.6f, 1.7f)
            circle(12f, 12f, 1.7f)
            circle(12f, 18.4f, 1.7f)
        }
    }
}
