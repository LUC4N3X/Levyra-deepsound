package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun LevyraLaunchExperience(
    accentStart: Color,
    accentEnd: Color,
    animationsEnabled: Boolean,
    onFinished: () -> Unit
) {
    val reveal = androidx.compose.runtime.remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "levyra-launch-loop")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2400, easing = LinearEasing)),
        label = "levyra-launch-spin"
    )
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "levyra-launch-wave"
    )

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onFinished()
            return@LaunchedEffect
        }
        reveal.animateTo(1f, animationSpec = tween(620, easing = FastOutSlowInEasing))
        delay(720)
        reveal.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        onFinished()
    }

    val alpha = reveal.value.coerceIn(0f, 1f)
    val scale = 0.92f + alpha * 0.08f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(LevyraBlack.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) * 0.27f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentStart.copy(alpha = 0.24f), accentEnd.copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = radius * 2.6f
                ),
                radius = radius * 2.6f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx())
            )
            repeat(42) { index ->
                val angle = ((index * 8.571f) + spin) * (PI.toFloat() / 180f)
                val energy = ((sin(wave * PI.toFloat() * 2f + index * 0.46f) + 1f) / 2f).coerceIn(0f, 1f)
                val inner = radius * (0.76f + energy * 0.03f)
                val outer = radius * (0.88f + energy * 0.16f)
                val x1 = center.x + cos(angle) * inner
                val y1 = center.y + sin(angle) * inner
                val x2 = center.x + cos(angle) * outer
                val y2 = center.y + sin(angle) * outer
                drawLine(
                    color = if (index % 2 == 0) accentStart.copy(alpha = 0.30f) else accentEnd.copy(alpha = 0.28f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = (1.2f + energy * 1.7f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 18.dp,
                modifier = Modifier.size(138.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    accentStart.copy(alpha = 0.86f),
                                    accentEnd.copy(alpha = 0.84f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(Color.Black.copy(alpha = 0.30f), size.minDimension * 0.45f, center)
                        drawCircle(Color.White.copy(alpha = 0.18f), size.minDimension * 0.33f, center, style = Stroke(width = 1.4.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.10f), size.minDimension * 0.21f, center, style = Stroke(width = 1.dp.toPx()))
                    }
                    Text(
                        text = "L",
                        color = Color.White,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            translationY = -2f
                            shadowElevation = 10f
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "LEVYRA",
                color = LevyraText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LevyraLaunchBars(wave = wave, accentStart = accentStart, accentEnd = accentEnd)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "music that opens instantly",
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun LevyraLaunchBars(wave: Float, accentStart: Color, accentEnd: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val energy = ((sin(wave * PI.toFloat() * 2f + index * 0.72f) + 1f) / 2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((10f + energy * 22f).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (index % 2 == 0) accentStart else LevyraCyan,
                                if (index % 2 == 0) LevyraViolet else accentEnd
                            )
                        )
                    )
            )
        }
    }
}
