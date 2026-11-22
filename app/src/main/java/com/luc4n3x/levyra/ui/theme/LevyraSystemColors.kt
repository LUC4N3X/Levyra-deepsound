package com.luc4n3x.levyra.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object LevyraWallpaperSeed {
    private val seedState = mutableStateOf<Int?>(null)
    @Volatile private var resolved = false
    @Volatile private var listening = false
    private val refreshing = java.util.concurrent.atomic.AtomicBoolean(false)

    val nativeDynamic: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val current: Int? get() = seedState.value

    fun seedNow(context: Context): Int? {
        val app = context.applicationContext
        listen(app)
        if (!resolved && refreshing.compareAndSet(false, true)) {
            val work = Runnable {
                try {
                    refresh(app)
                } finally {
                    refreshing.set(false)
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) Thread(work, "levyra-wallpaper-seed").start() else work.run()
        }
        return seedState.value
    }

    suspend fun ensure(context: Context) {
        val app = context.applicationContext
        if (!resolved) withContext(Dispatchers.IO) { refresh(app) }
        listen(app)
    }

    private fun refresh(context: Context) {
        seedState.value = extract(context)
        resolved = true
    }

    private fun listen(context: Context) {
        if (listening || Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        listening = true
        runCatching {
            WallpaperManager.getInstance(context).addOnColorsChangedListener(
                { colors, which ->
                    if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                        seedState.value = colors?.primaryColor?.toArgb()
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }.onFailure { listening = false }
    }

    private fun extract(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return runCatching {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
        }.getOrNull()
    }
}

@Composable
internal fun rememberSystemColorScheme(dark: Boolean, pureBlack: Boolean): ColorScheme? {
    val context = LocalContext.current
    if (LevyraWallpaperSeed.nativeDynamic) {
        return remember(context, dark) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }
    LaunchedEffect(Unit) { LevyraWallpaperSeed.ensure(context) }
    val seed = LevyraWallpaperSeed.current ?: return null
    return remember(seed, dark, pureBlack) { wallpaperColorScheme(seed, dark, pureBlack) }
}

internal fun wallpaperColorScheme(seed: Int, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed), dark, 0.0, ColorSpec.SpecVersion.SPEC_2025, DynamicScheme.Platform.PHONE)
    val roles = MaterialDynamicColors()
    fun DynamicColor.color() = Color(getArgb(scheme))
    val base = if (dark) {
        darkColorScheme(
            primary = roles.primary.color(), onPrimary = roles.onPrimary.color(),
            primaryContainer = roles.primaryContainer.color(), onPrimaryContainer = roles.onPrimaryContainer.color(),
            inversePrimary = roles.inversePrimary.color(),
            secondary = roles.secondary.color(), onSecondary = roles.onSecondary.color(),
            secondaryContainer = roles.secondaryContainer.color(), onSecondaryContainer = roles.onSecondaryContainer.color(),
            tertiary = roles.tertiary.color(), onTertiary = roles.onTertiary.color(),
            tertiaryContainer = roles.tertiaryContainer.color(), onTertiaryContainer = roles.onTertiaryContainer.color(),
            background = roles.background.color(), onBackground = roles.onBackground.color(),
            surface = roles.surface.color(), onSurface = roles.onSurface.color(),
            surfaceVariant = roles.surfaceVariant.color(), onSurfaceVariant = roles.onSurfaceVariant.color(),
            surfaceTint = roles.surfaceTint.color(),
            inverseSurface = roles.inverseSurface.color(), inverseOnSurface = roles.inverseOnSurface.color(),
            error = roles.error.color(), onError = roles.onError.color(),
            errorContainer = roles.errorContainer.color(), onErrorContainer = roles.onErrorContainer.color(),
            outline = roles.outline.color(), outlineVariant = roles.outlineVariant.color(), scrim = roles.scrim.color(),
            surfaceBright = roles.surfaceBright.color(), surfaceDim = roles.surfaceDim.color(),
            surfaceContainer = roles.surfaceContainer.color(), surfaceContainerHigh = roles.surfaceContainerHigh.color(),
            surfaceContainerHighest = roles.surfaceContainerHighest.color(), surfaceContainerLow = roles.surfaceContainerLow.color(),
            surfaceContainerLowest = roles.surfaceContainerLowest.color()
        )
    } else {
        lightColorScheme(
            primary = roles.primary.color(), onPrimary = roles.onPrimary.color(),
            primaryContainer = roles.primaryContainer.color(), onPrimaryContainer = roles.onPrimaryContainer.color(),
            inversePrimary = roles.inversePrimary.color(),
            secondary = roles.secondary.color(), onSecondary = roles.onSecondary.color(),
            secondaryContainer = roles.secondaryContainer.color(), onSecondaryContainer = roles.onSecondaryContainer.color(),
            tertiary = roles.tertiary.color(), onTertiary = roles.onTertiary.color(),
            tertiaryContainer = roles.tertiaryContainer.color(), onTertiaryContainer = roles.onTertiaryContainer.color(),
            background = roles.background.color(), onBackground = roles.onBackground.color(),
            surface = roles.surface.color(), onSurface = roles.onSurface.color(),
            surfaceVariant = roles.surfaceVariant.color(), onSurfaceVariant = roles.onSurfaceVariant.color(),
            surfaceTint = roles.surfaceTint.color(),
            inverseSurface = roles.inverseSurface.color(), inverseOnSurface = roles.inverseOnSurface.color(),
            error = roles.error.color(), onError = roles.onError.color(),
            errorContainer = roles.errorContainer.color(), onErrorContainer = roles.onErrorContainer.color(),
            outline = roles.outline.color(), outlineVariant = roles.outlineVariant.color(), scrim = roles.scrim.color(),
            surfaceBright = roles.surfaceBright.color(), surfaceDim = roles.surfaceDim.color(),
            surfaceContainer = roles.surfaceContainer.color(), surfaceContainerHigh = roles.surfaceContainerHigh.color(),
            surfaceContainerHighest = roles.surfaceContainerHighest.color(), surfaceContainerLow = roles.surfaceContainerLow.color(),
            surfaceContainerLowest = roles.surfaceContainerLowest.color()
        )
    }
    return if (dark && pureBlack) base.asAmoled() else base
}

private fun ColorScheme.asAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = blend(Color(0xFF0B0B0D), surfaceContainerLow, 0.7f),
    surfaceContainer = blend(Color(0xFF111114), surfaceContainer, 0.6f),
    surfaceContainerHigh = blend(Color(0xFF18181C), surfaceContainerHigh, 0.5f)
)

private fun blend(base: Color, tint: Color, share: Float): Color = Color(
    red = base.red * share + tint.red * (1f - share),
    green = base.green * share + tint.green * (1f - share),
    blue = base.blue * share + tint.blue * (1f - share),
    alpha = 1f
)
