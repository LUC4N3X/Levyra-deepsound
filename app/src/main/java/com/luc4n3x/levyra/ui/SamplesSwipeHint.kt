package com.luc4n3x.levyra.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val SAMPLES_HINT_PREFS = "levyra_samples_swipe_hint"
private const val SAMPLES_HINT_SEEN = "seen"
private const val SAMPLES_HINT_TIMEOUT_MS = 6_000L

@Composable
internal fun SamplesSwipeHint(pagerState: PagerState, languageCode: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(SAMPLES_HINT_PREFS, Context.MODE_PRIVATE) }
    val seenAtStart = remember(prefs) { prefs.getBoolean(SAMPLES_HINT_SEEN, false) }
    if (seenAtStart) return
    var visible by remember { mutableStateOf(true) }
    val dismiss = {
        visible = false
        prefs.edit().putBoolean(SAMPLES_HINT_SEEN, true).apply()
    }
    LaunchedEffect(pagerState) {
        val start = pagerState.currentPage
        snapshotFlow { pagerState.currentPage != start || pagerState.isScrollInProgress }.first { it }
        dismiss()
    }
    LaunchedEffect(Unit) {
        delay(SAMPLES_HINT_TIMEOUT_MS)
        dismiss()
    }
    val text = remember(languageCode) { LevyraSpotifySkinStrings.forCode(languageCode).swipeHint }
    val motion = rememberInfiniteTransition(label = "samples-hint")
    val frame by motion.animateFloat(
        initialValue = 0f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart),
        label = "samples-hint-frame"
    )
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(250)), modifier = modifier) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SwipeHandAnimation(frame, Modifier.size(width = 96.dp, height = 164.dp))
                Text(
                    text, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }
    }
}

private fun keyframes(frame: Float, vararg points: Pair<Float, Float>): Float {
    if (frame <= points.first().first) return points.first().second
    points.toList().zipWithNext().forEach { (a, b) ->
        if (frame <= b.first) {
            val t = ((frame - a.first) / (b.first - a.first)).coerceIn(0f, 1f)
            val eased = FastOutSlowInEasing.transform(t)
            return a.second + (b.second - a.second) * eased
        }
    }
    return points.last().second
}

@Composable
private fun SwipeHandAnimation(frame: Float, modifier: Modifier) {
    val hand = rememberVectorPainter(Icons.Rounded.TouchApp)
    Canvas(modifier) {
        val unit = size.width / 480f
        val touchScale = keyframes(frame, 5f to 0f, 15f to 1f, 70.5f to 1f, 78f to 0f)
        if (touchScale > 0f) {
            drawCircle(
                Color(0xFFD0D0D0), radius = 59.5f * unit * touchScale, center = Offset(240f * unit, 695f * unit),
                style = Stroke(width = 10f * unit)
            )
        }
        val trailAlpha = keyframes(frame, 51f to 0f, 55f to 1f, 68f to 1f, 71.5f to 0f)
        if (trailAlpha > 0f) {
            val height = keyframes(frame, 55f to 155f, 64f to 765f) * unit
            val centerY = (390f + keyframes(frame, 55f to 305f, 64f to 0f)) * unit
            val width = keyframes(frame, 55f to 155f, 64f to 167f) * unit
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f * trailAlpha), Color.White.copy(alpha = 0.45f * trailAlpha)), startY = centerY - height / 2f, endY = centerY + height / 2f),
                topLeft = Offset(240f * unit - width / 2f, centerY - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(width / 2f)
            )
        }
        val x = keyframes(frame, 25f to 300f, 34f to 292f, 55f to 292f, 65f to 276f, 71f to 276f, 85f to 300f) * unit
        val y = keyframes(frame, 25f to 754f, 34f to 746f, 55f to 746f, 65f to 142f, 71f to 142f, 85f to 754f) * unit
        val rotation = keyframes(frame, 25f to -35f, 55f to -35f, 65f to -22f, 80f to -22f, 85f to -35f) + 35f
        val handSize = 230f * unit
        translate(x - handSize * 0.45f, y - handSize * 0.12f) {
            rotate(rotation, pivot = Offset(handSize * 0.45f, handSize * 0.12f)) {
                with(hand) { draw(Size(handSize, handSize), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)) }
            }
        }
    }
}
