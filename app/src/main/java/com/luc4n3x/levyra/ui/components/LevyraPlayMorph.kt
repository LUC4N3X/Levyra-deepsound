package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val PlayMorph: Morph by lazy {
    Morph(MaterialShapes.Square.normalized(), MaterialShapes.Cookie12Sided.normalized())
}

internal class LevyraPlayMorphShape(private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = PlayMorph.toPath(progress.coerceIn(0f, 1f), android.graphics.Path()).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

@Composable
internal fun rememberLevyraPlayMorphShape(playing: Boolean, animated: Boolean): Shape {
    val progress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "levyra-play-morph"
    )
    return remember(progress) { LevyraPlayMorphShape(progress) }
}
