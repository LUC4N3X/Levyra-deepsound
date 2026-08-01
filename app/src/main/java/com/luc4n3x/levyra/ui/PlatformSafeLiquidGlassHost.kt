package com.luc4n3x.levyra.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Entry point for adaptive Liquid Glass.
 *
 * Android 16 currently uses a composition-only renderer instead of recording the complete app
 * hierarchy into a shared GraphicsLayer. This avoids a device/driver-sensitive startup path while
 * preserving the translucent chrome and keeping all controls untouched.
 */
@Composable
fun PlatformSafeLiquidGlassHost(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT < 36) {
        AdaptiveLiquidGlassHost(
            enabled = enabled,
            modifier = modifier,
            content = content
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (enabled) {
            Android16LiquidGlassChrome(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun Android16LiquidGlassChrome(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activityManager = remember(context) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    val constrained = activityManager?.isLowRamDevice == true || powerManager?.isPowerSaveMode == true
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val chromeHeightPx = with(density) { 176.dp.toPx() }
    val borderWidthPx = with(density) { 1.dp.toPx() }
    val tintAlpha = if (constrained) 0.045f else 0.085f
    val sheenPhase = rememberAndroid16SheenPhase(animated = !constrained)

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val chromeTop = (size.height - chromeHeightPx).coerceAtLeast(0f)
        val chromeHeight = size.height - chromeTop

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    colors.surface.copy(alpha = tintAlpha * 0.45f),
                    colors.surface.copy(alpha = tintAlpha)
                ),
                startY = chromeTop,
                endY = size.height
            ),
            topLeft = Offset(0f, chromeTop),
            size = Size(size.width, chromeHeight)
        )

        if (!constrained) {
            val centerX = size.width * sheenPhase
            val sheenWidth = size.width * 0.42f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.onSurface.copy(alpha = 0.055f),
                        Color.Transparent
                    ),
                    startX = centerX - sheenWidth,
                    endX = centerX + sheenWidth
                ),
                topLeft = Offset(0f, chromeTop),
                size = Size(size.width, chromeHeight)
            )
        }

        drawLine(
            color = colors.outlineVariant.copy(alpha = 0.50f),
            start = Offset(0f, chromeTop),
            end = Offset(size.width, chromeTop),
            strokeWidth = borderWidthPx
        )
    }
}

@Composable
private fun rememberAndroid16SheenPhase(animated: Boolean): Float {
    if (!animated) return -0.35f

    val transition = rememberInfiniteTransition(label = "levyraAndroid16LiquidGlass")
    val phase by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "android16LiquidGlassSheen"
    )
    return phase
}
