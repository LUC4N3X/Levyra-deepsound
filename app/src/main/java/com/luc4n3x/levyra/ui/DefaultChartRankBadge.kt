package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors

@Composable
internal fun DefaultChartRankBadge(rank: Int, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    Box(
        modifier = modifier
            .padding(end = 6.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) tone.accent else tone.softContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            color = if (isCurrent) tone.onAccent else tone.accent,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
