package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Seed-based procedural artwork placeholder inspired by shapes.gallery geometric assets.
 * Renders dynamic organic squircles, mesh gradients, and glowing concentric rings derived from track metadata.
 */
@Composable
fun ProceduralArtworkCanvas(
    seed: String,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true
) {
    val params = remember(seed, darkTheme) { generateProceduralParams(seed, darkTheme) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(params.backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val centerX = width * params.centerXRatio
            val centerY = height * params.centerYRatio

            // Draw organic background blobs / squircles
            for (blob in params.blobs) {
                val path = Path()
                val radius = (width.coerceAtLeast(height)) * blob.sizeRatio
                val points = 8
                for (i in 0 until points) {
                    val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat()
                    val variation = 1f + (sin(angle * blob.frequency + blob.phase) * blob.amplitude)
                    val r = radius * variation
                    val px = centerX + cos(angle) * r
                    val py = centerY + sin(angle) * r
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(blob.color.copy(alpha = blob.alpha), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius * 1.2f
                    )
                )
            }

            // Draw concentric geometric ring accents
            for (ring in params.rings) {
                val radius = (width.coerceAtMost(height)) * ring.radiusRatio
                drawCircle(
                    color = ring.color.copy(alpha = ring.alpha),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = ring.strokeWidth)
                )
            }
        }
    }
}

private data class BlobParam(
    val sizeRatio: Float,
    val amplitude: Float,
    val frequency: Float,
    val phase: Float,
    val color: Color,
    val alpha: Float
)

private data class RingParam(
    val radiusRatio: Float,
    val strokeWidth: Float,
    val color: Color,
    val alpha: Float
)

private data class ProceduralParams(
    val backgroundBrush: Brush,
    val centerXRatio: Float,
    val centerYRatio: Float,
    val blobs: List<BlobParam>,
    val rings: List<RingParam>
)

private fun generateProceduralParams(seed: String, darkTheme: Boolean): ProceduralParams {
    val hash = abs(seed.hashCode())
    val r1 = (hash and 0xFF) / 255f
    val r2 = ((hash shr 8) and 0xFF) / 255f
    val r3 = ((hash shr 16) and 0xFF) / 255f
    val r4 = ((hash shr 24) and 0xFF) / 255f

    val palette = if (darkTheme) {
        listOf(
            Color(0xFF2997FF), // Cyan
            Color(0xFF818CF8), // Violet
            Color(0xFFEC4899), // Pink
            Color(0xFF60A5FA), // Blue
            Color(0xFF34D399), // Emerald
            Color(0xFFF59E0B)  // Amber
        )
    } else {
        listOf(
            Color(0xFF0066CC),
            Color(0xFF6366F1),
            Color(0xFFDB2777),
            Color(0xFF2563EB),
            Color(0xFF059669),
            Color(0xFFD97706)
        )
    }

    val c1 = palette[(hash) % palette.size]
    val c2 = palette[(hash / 3) % palette.size]
    val c3 = palette[(hash / 7) % palette.size]

    val baseBg = if (darkTheme) Color(0xFF0A0A0E) else Color(0xFFF1F5F9)
    val endBg = if (darkTheme) Color(0xFF14141E) else Color(0xFFE2E8F0)

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            c1.copy(alpha = if (darkTheme) 0.35f else 0.2f),
            baseBg,
            endBg,
            c2.copy(alpha = if (darkTheme) 0.25f else 0.15f)
        )
    )

    val blobs = listOf(
        BlobParam(0.45f + r1 * 0.2f, 0.15f + r2 * 0.1f, 3f, r3 * 6.28f, c1, 0.45f),
        BlobParam(0.35f + r2 * 0.2f, 0.20f + r3 * 0.1f, 4f, r4 * 6.28f, c2, 0.35f),
        BlobParam(0.25f + r3 * 0.2f, 0.10f + r4 * 0.1f, 5f, r1 * 6.28f, c3, 0.30f)
    )

    val rings = listOf(
        RingParam(0.25f + r1 * 0.15f, 2f + r2 * 3f, c1, 0.3f),
        RingParam(0.40f + r2 * 0.15f, 1.5f + r3 * 2f, Color.White, 0.18f),
        RingParam(0.55f + r3 * 0.15f, 1f + r4 * 2f, c2, 0.22f)
    )

    return ProceduralParams(
        backgroundBrush = backgroundBrush,
        centerXRatio = 0.4f + r1 * 0.2f,
        centerYRatio = 0.4f + r2 * 0.2f,
        blobs = blobs,
        rings = rings
    )
}
