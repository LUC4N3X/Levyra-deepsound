package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Universal Icon Button wrapper with spring physics feedback.
 * Provides a tactile scale animation on press and release.
 */
@Composable
fun SpringIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        scope.launch {
                            scale.animateTo(
                                targetValue = pressedScale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        tryAwaitRelease()
                        scope.launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
