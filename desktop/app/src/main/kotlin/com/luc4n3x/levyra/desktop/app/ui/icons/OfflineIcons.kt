package com.luc4n3x.levyra.desktop.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

private fun offlineStrokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
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

object OfflineIcons {
    val Library: ImageVector by lazy {
        offlineStrokeIcon("LevyraLibrary") {
            moveTo(5f, 4.5f)
            lineTo(5f, 19.5f)
            moveTo(10f, 4.5f)
            lineTo(10f, 19.5f)
            moveTo(15f, 6f)
            lineTo(18.7f, 5f)
            lineTo(21f, 18.5f)
            lineTo(17.3f, 19.2f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        offlineStrokeIcon("LevyraDownload") {
            moveTo(12f, 4f)
            lineTo(12f, 15f)
            moveTo(7.5f, 11f)
            lineTo(12f, 15.5f)
            lineTo(16.5f, 11f)
            moveTo(5f, 19f)
            lineTo(19f, 19f)
        }
    }

    val Folder: ImageVector by lazy {
        offlineStrokeIcon("LevyraFolder") {
            moveTo(3.5f, 7f)
            curveTo(3.5f, 5.9f, 4.4f, 5f, 5.5f, 5f)
            lineTo(10f, 5f)
            lineTo(12f, 7.2f)
            lineTo(18.5f, 7.2f)
            curveTo(19.6f, 7.2f, 20.5f, 8.1f, 20.5f, 9.2f)
            lineTo(20.5f, 17f)
            curveTo(20.5f, 18.1f, 19.6f, 19f, 18.5f, 19f)
            lineTo(5.5f, 19f)
            curveTo(4.4f, 19f, 3.5f, 18.1f, 3.5f, 17f)
            close()
        }
    }

    val History: ImageVector by lazy {
        offlineStrokeIcon("LevyraHistory") {
            moveTo(5f, 8f)
            lineTo(5f, 4.5f)
            moveTo(5f, 4.5f)
            lineTo(8.5f, 4.5f)
            moveTo(5.3f, 5.2f)
            curveTo(7.1f, 3.8f, 9.4f, 3f, 12f, 3f)
            curveTo(17f, 3f, 21f, 7f, 21f, 12f)
            curveTo(21f, 17f, 17f, 21f, 12f, 21f)
            curveTo(8.1f, 21f, 4.7f, 18.6f, 3.5f, 15.2f)
            moveTo(12f, 7f)
            lineTo(12f, 12f)
            lineTo(15.5f, 14f)
        }
    }

    val Check: ImageVector by lazy {
        offlineStrokeIcon("LevyraCheck") {
            moveTo(5f, 12.5f)
            lineTo(9.5f, 17f)
            lineTo(19f, 7f)
        }
    }
}
