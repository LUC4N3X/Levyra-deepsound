package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun HomePersonalOrbitDots(pageCount: Int, currentPage: Int) {
    if (pageCount <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraHomeDesign.OrbitDotGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val color = if (currentPage == index) LevyraText else LevyraText.copy(alpha = 0.30f)
            Box(Modifier.size(LevyraHomeDesign.OrbitDotSize).background(color, CircleShape))
        }
    }
}
