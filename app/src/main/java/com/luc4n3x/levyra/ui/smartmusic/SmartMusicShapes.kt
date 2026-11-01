package com.luc4n3x.levyra.ui.smartmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.Morph
import com.luc4n3x.levyra.ui.components.LevyraMorphShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SelectionMorph: Morph by lazy {
    Morph(MaterialShapes.Square.normalized(), MaterialShapes.Cookie9Sided.normalized())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val OrbitMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.SoftBurst.normalized())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val PinMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Clover4Leaf.normalized())
}

@Composable
internal fun rememberSelectionMorph(selected: Boolean, animated: Boolean, rotation: Float = 0f): Shape =
    rememberMorph(SelectionMorph, selected, animated, rotation)

@Composable
internal fun rememberOrbitMorph(active: Boolean, animated: Boolean, rotation: Float = 0f): Shape =
    rememberMorph(OrbitMorph, active, animated, rotation)

@Composable
internal fun rememberPinMorph(pinned: Boolean, animated: Boolean): Shape =
    rememberMorph(PinMorph, pinned, animated, 0f)

@Composable
private fun rememberMorph(morph: Morph, active: Boolean, animated: Boolean, rotation: Float): Shape {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) else snap(),
        label = "smart-music-morph"
    )
    return remember(morph, progress, rotation) { LevyraMorphShape(morph, progress, rotation) }
}
