package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Small, app-owned Ionicons subset used by Levyra's music surfaces.
 *
 * The paths are adapted from ionic-team/ionicons (MIT). Keeping only the icons
 * we actually use avoids adding another runtime dependency or shipping the full pack.
 */
internal object LevyraIonicons {
    val Play: ImageVector = ionicon(
        "Ionicons.Play",
        filled("M133,440a35.37,35.37,0,0,1-17.5-4.67c-12-6.8-19.46-20-19.46-34.33V111c0-14.37,7.46-27.53,19.46-34.33a35.13,35.13,0,0,1,35.77.45L399.12,225.48a36,36,0,0,1,0,61L151.23,434.88A35.5,35.5,0,0,1,133,440Z")
    )

    val AudioMode: ImageVector = ionicon(
        "Ionicons.AudioMode",
        filled("M384,96a16,16,0,0,0-16-16H176a16,16,0,0,0-16,16v227.12A64,64,0,1,0,192,384V160H352V291.12A64,64,0,1,0,384,352Z")
    )

    val VideoMode: ImageVector = ionicon(
        "Ionicons.VideoMode",
        stroked("M112,128H400a48,48,0,0,1,48,48V336a48,48,0,0,1-48,48H112a48,48,0,0,1-48-48V176A48,48,0,0,1,112,128Z", 32f),
        filled("M224,204a12,12,0,0,1,18.2-10.3l96,52a12,12,0,0,1,0,20.6l-96,52A12,12,0,0,1,224,308Z")
    )

    val Pause: ImageVector = ionicon(
        "Ionicons.Pause",
        filled("M208,432H160a16,16,0,0,1-16-16V96a16,16,0,0,1,16-16h48a16,16,0,0,1,16,16V416A16,16,0,0,1,208,432Z"),
        filled("M352,432H304a16,16,0,0,1-16-16V96a16,16,0,0,1,16-16h48a16,16,0,0,1,16,16V416A16,16,0,0,1,352,432Z")
    )

    val SkipPrevious: ImageVector = ionicon(
        "Ionicons.SkipPrevious",
        filled("M112,64a16,16,0,0,1,16,16V216.43L360.77,77.11a35.13,35.13,0,0,1,35.77-.44c12,6.8,19.46,20,19.46,34.33V401c0,14.37-7.46,27.53-19.46,34.33a35.14,35.14,0,0,1-35.77-.45L128,295.57V432a16,16,0,0,1-32,0V80A16,16,0,0,1,112,64Z")
    )

    val SkipNext: ImageVector = ionicon(
        "Ionicons.SkipNext",
        filled("M400,64a16,16,0,0,0-16,16V216.43L151.23,77.11a35.13,35.13,0,0,0-35.77-.44C103.46,83.47,96,96.63,96,111V401c0,14.37,7.46,27.53,19.46,34.33a35.14,35.14,0,0,0,35.77-.45L384,295.57V432a16,16,0,0,0,32,0V80A16,16,0,0,0,400,64Z")
    )

    val Shuffle: ImageVector = ionicon(
        "Ionicons.Shuffle",
        stroked("M400,304L448,352L400,400", 32f),
        stroked("M400,112L448,160L400,208", 32f),
        stroked("M64,352h85.19a80,80,0,0,0,66.56-35.62L256,256", 32f),
        stroked("M64,160h85.19a80,80,0,0,1,66.56,35.62l80.5,120.76A80,80,0,0,0,362.81,352H416", 32f),
        stroked("M416,160H362.81a80,80,0,0,0-66.56,35.62L288,208", 32f)
    )

    val Repeat: ImageVector = ionicon(
        "Ionicons.Repeat",
        stroked("M320,120L368,168L320,216", 32f),
        stroked("M352,168H144a80.24,80.24,0,0,0-80,80v16", 32f),
        stroked("M192,392L144,344L192,296", 32f),
        stroked("M160,344H368a80.24,80.24,0,0,0,80-80V248", 32f)
    )

    val RepeatOne: ImageVector = ionicon(
        "Ionicons.RepeatOne",
        stroked("M320,120L368,168L320,216", 32f),
        stroked("M352,168H144a80.24,80.24,0,0,0-80,80v16", 32f),
        stroked("M192,392L144,344L192,296", 32f),
        stroked("M160,344H368a80.24,80.24,0,0,0,80-80V248", 32f),
        filled("M236,220l20-12v84h-28v-16h12v-56h-4Z")
    )

    val ChevronDown: ImageVector = ionicon("Ionicons.ChevronDown", stroked("M112,184L256,328L400,184", 48f))

    val MoreVertical: ImageVector = ionicon(
        "Ionicons.MoreVertical",
        filled(circlePath(256f, 96f, 48f)),
        filled(circlePath(256f, 256f, 48f)),
        filled(circlePath(256f, 416f, 48f))
    )

    val AddCircle: ImageVector = ionicon(
        "Ionicons.AddCircle",
        stroked(
            "M448,256c0-106-86-192-192-192S64,150,64,256s86,192,192,192S448,362,448,256Z",
            32f,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter,
            miter = 10f
        ),
        stroked("M256,176V336", 32f),
        stroked("M336,256H176", 32f)
    )

    val Heart: ImageVector = ionicon(
        "Ionicons.Heart",
        filled("M256,448a32,32,0,0,1-18-5.57c-78.59-53.35-112.62-89.93-131.39-112.8-40-48.75-59.15-98.8-58.61-153C48.63,114.52,98.46,64,159.08,64c44.08,0,74.61,24.83,92.39,45.51a6,6,0,0,0,9.06,0C278.31,88.81,308.84,64,352.92,64,413.54,64,463.37,114.52,464,176.64c.54,54.21-18.63,104.26-58.61,153-18.77,22.87-52.8,59.45-131.39,112.8A32,32,0,0,1,256,448Z")
    )

    val HeartOutline: ImageVector = ionicon(
        "Ionicons.HeartOutline",
        stroked("M352.92,80C288,80,256,144,256,144s-32-64-96.92-64C106.32,80,64.54,124.14,64,176.81c-1.1,109.33,86.73,187.08,183,252.42a16,16,0,0,0,18,0c96.26-65.34,184.09-143.09,183-252.42C447.46,124.14,405.68,80,352.92,80Z", 32f)
    )

    val Download: ImageVector = ionicon(
        "Ionicons.Download",
        stroked("M336,176h40a40,40,0,0,1,40,40V424a40,40,0,0,1-40,40H136a40,40,0,0,1-40-40V216a40,40,0,0,1,40-40h40", 32f),
        stroked("M176,272L256,352L336,272", 32f),
        stroked("M256,48V336", 32f)
    )

    val Queue: ImageVector = ionicon(
        "Ionicons.Queue",
        stroked("M160,144H448", 32f),
        stroked("M160,256H448", 32f),
        stroked("M160,368H448", 32f),
        stroked(circlePath(80f, 144f, 16f), 32f),
        stroked(circlePath(80f, 256f, 16f), 32f),
        stroked(circlePath(80f, 368f, 16f), 32f)
    )

    val Lyrics: ImageVector = ionicon(
        "Ionicons.Lyrics",
        stroked("M416,221.25V416a48,48,0,0,1-48,48H144a48,48,0,0,1-48-48V96a48,48,0,0,1,48-48h98.75a32,32,0,0,1,22.62,9.37L406.63,198.63A32,32,0,0,1,416,221.25Z", 32f, cap = StrokeCap.Butt, join = StrokeJoin.Round),
        stroked("M256,56V176a32,32,0,0,0,32,32H408", 32f),
        stroked("M176,288H336", 32f),
        stroked("M176,368H336", 32f)
    )

    val Timer: ImageVector = ionicon(
        "Ionicons.Timer",
        stroked("M112.91,128A191.85,191.85,0,0,0,64,254c-1.18,106.35,85.65,193.8,192,194,106.2.2,192-85.83,192-192,0-104.54-83.55-189.61-187.5-192A4.36,4.36,0,0,0,256,68.37V152", 32f),
        filled("M233.38,278.63l-79-113a8.13,8.13,0,0,1,11.32-11.32l113,79a32.5,32.5,0,0,1-37.25,53.26A33.21,33.21,0,0,1,233.38,278.63Z")
    )

    val Equalizer: ImageVector = ionicon(
        "Ionicons.Equalizer",
        stroked("M368,128H448", 32f),
        stroked("M64,128H304", 32f),
        stroked("M368,384H448", 32f),
        stroked("M64,384H304", 32f),
        stroked("M208,256H448", 32f),
        stroked("M64,256H144", 32f),
        stroked(circlePath(336f, 128f, 32f), 32f),
        stroked(circlePath(176f, 256f, 32f), 32f),
        stroked(circlePath(336f, 384f, 32f), 32f)
    )

    val Device: ImageVector = ionicon(
        "Ionicons.Device",
        stroked("M176,16H336A48,48,0,0,1,384,64V448a48,48,0,0,1-48,48H176a48,48,0,0,1-48-48V64A48,48,0,0,1,176,16Z", 32f),
        stroked("M176,16h24a8,8,0,0,1,8,8,16,16,0,0,0,16,16h64a16,16,0,0,0,16-16,8,8,0,0,1,8-8h24", 32f)
    )

    val Settings: ImageVector = ionicon(
        "Ionicons.Settings",
        stroked("M262.29,192.31a64,64,0,1,0,57.4,57.4A64.13,64.13,0,0,0,262.29,192.31ZM416.39,256a154.34,154.34,0,0,1-1.53,20.79l45.21,35.46A10.81,10.81,0,0,1,462.52,326l-42.77,74a10.81,10.81,0,0,1-13.14,4.59l-44.9-18.08a16.11,16.11,0,0,0-15.17,1.75A164.48,164.48,0,0,1,325,400.8a15.94,15.94,0,0,0-8.82,12.14l-6.73,47.89A11.08,11.08,0,0,1,298.77,470H213.23a11.11,11.11,0,0,1-10.69-8.87l-6.72-47.82a16.07,16.07,0,0,0-9-12.22,155.3,155.3,0,0,1-21.46-12.57,16,16,0,0,0-15.11-1.71l-44.89,18.07a10.81,10.81,0,0,1-13.14-4.58l-42.77-74a10.81,10.81,0,0,1,2.45-13.75l38.21-30a16.05,16.05,0,0,0,6-14.08c-.36-4.17-.58-8.33-.58-12.5s.21-8.27.58-12.35a16,16,0,0,0-6.07-13.94l-38.19-30A10.81,10.81,0,0,1,49.48,186l42.77-74a10.81,10.81,0,0,1,13.14-4.59l44.9,18.08a16.11,16.11,0,0,0,15.17-1.75A164.48,164.48,0,0,1,187,111.2a15.94,15.94,0,0,0,8.82-12.14l6.73-47.89A11.08,11.08,0,0,1,213.23,42h85.54a11.11,11.11,0,0,1,10.69,8.87l6.72,47.82a16.07,16.07,0,0,0,9,12.22,155.3,155.3,0,0,1,21.46,12.57,16,16,0,0,0,15.11,1.71l44.89-18.07a10.81,10.81,0,0,1,13.14-4.58l42.77,74a10.8,10.8,0,0,1-2.45,13.75l-38.21,30a16.05,16.05,0,0,0-6.05,14.08C416.17,247.67,416.39,251.83,416.39,256Z", 32f)
    )

    val Share: ImageVector = ionicon(
        "Ionicons.Share",
        stroked(circlePath(128f, 256f, 48f), 32f),
        stroked(circlePath(384f, 112f, 48f), 32f),
        stroked(circlePath(384f, 400f, 48f), 32f),
        stroked("M169.83,279.53L342.17,376.47", 32f),
        stroked("M342.17,135.53L169.83,232.47", 32f)
    )

    fun prewarm() {
        // Pre-initializes all eager ImageVector properties
    }
}

private data class IonPath(
    val data: String,
    val fill: Boolean,
    val strokeWidth: Float,
    val cap: StrokeCap,
    val join: StrokeJoin,
    val miter: Float
)

private fun filled(data: String): IonPath = IonPath(
    data = data,
    fill = true,
    strokeWidth = 0f,
    cap = StrokeCap.Butt,
    join = StrokeJoin.Miter,
    miter = 4f
)

private fun stroked(
    data: String,
    width: Float,
    cap: StrokeCap = StrokeCap.Round,
    join: StrokeJoin = StrokeJoin.Round,
    miter: Float = 4f
): IonPath = IonPath(
    data = data,
    fill = false,
    strokeWidth = width,
    cap = cap,
    join = join,
    miter = miter
)

private fun ionicon(name: String, vararg paths: IonPath): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 512f,
        viewportHeight = 512f
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = PathParser().parsePathString(path.data).toNodes().toList(),
                fill = if (path.fill) SolidColor(Color.Black) else null,
                stroke = if (path.strokeWidth > 0f) SolidColor(Color.Black) else null,
                strokeLineWidth = path.strokeWidth,
                strokeLineCap = path.cap,
                strokeLineJoin = path.join,
                strokeLineMiter = path.miter
            )
        }
    }.build()
}

private fun circlePath(cx: Float, cy: Float, radius: Float): String {
    val left = cx - radius
    val right = cx + radius
    return "M$right,$cy A$radius,$radius 0 1,1 $left,$cy A$radius,$radius 0 1,1 $right,$cy Z"
}
