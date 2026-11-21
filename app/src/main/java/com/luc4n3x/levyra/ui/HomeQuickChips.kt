package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun QuickChips(languageCode: String, onClick: (String) -> Unit) {
    val chips = LevyraContentLocales.quickSearches(languageCode)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chip ->
            QuickSearchChip(label = chip, onClick = { onClick(chip) })
        }
    }
}

@Composable
internal fun QuickSearchChip(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = 22.dp,
        pressed = 12.dp,
        label = "search-quick-chip-corner"
    )
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            ),
        contentAlignment = Alignment.Center
    ) {
        if (LevyraIsDefaultSkin) {
            DefaultQuickSearchPill(label, RoundedCornerShape(corner))
            return@Box
        }
        Text(
            text = label,
            color = if (LevyraSkinManagesLayout) LevyraText else scheme.onSurface,
            style = LevyraType.cardTitle.copy(fontWeight = if (LevyraSkinManagesLayout) FontWeight.Medium else FontWeight.SemiBold),
            modifier = Modifier
                .clip(if (LevyraSkinManagesLayout) SkinChrome.chipShape else RoundedCornerShape(corner))
                .background(if (LevyraSkinManagesLayout) LevyraPanel else scheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun DefaultQuickSearchPill(label: String, shape: RoundedCornerShape) {
    val accent = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed).accent
    val fill = if (LevyraIsLight) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.06f)
    val edge = if (LevyraIsLight) Color(0x1F11131F) else Color.White.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Text(text = label, color = LevyraText.copy(alpha = 0.92f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
