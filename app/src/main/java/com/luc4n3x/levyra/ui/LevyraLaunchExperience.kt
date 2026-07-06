package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
internal fun LevyraLaunchExperience(
    accentStart: Color,
    accentEnd: Color,
    animationsEnabled: Boolean,
    onFinished: () -> Unit
) {
    val reveal = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "levyra-launch-logo-loop")
    val logoBreath by infinite.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levyra-launch-logo-breath"
    )
    val logoGlow by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levyra-launch-logo-glow"
    )

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            onFinished()
            return@LaunchedEffect
        }
        reveal.animateTo(1f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        delay(620)
        reveal.animateTo(0f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        onFinished()
    }

    val alpha = reveal.value.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(LevyraBlack.copy(alpha = 0.965f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentStart.copy(alpha = 0.12f),
                            accentEnd.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 860f
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = logoBreath
                        scaleY = logoBreath
                    }
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    accentStart.copy(alpha = 0.72f),
                                    accentEnd.copy(alpha = 0.62f),
                                    Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension * 0.42f
                        drawCircle(
                            color = LevyraCyan.copy(alpha = logoGlow),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                        drawCircle(
                            color = LevyraViolet.copy(alpha = logoGlow * 0.72f),
                            radius = radius * 0.62f,
                            center = center,
                            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "L",
                        color = Color.White,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            translationY = -2f
                            shadowElevation = 8f
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "LEVYRA",
                color = LevyraText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.8.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "music opens clean",
                color = LevyraMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
        }
    }
}
