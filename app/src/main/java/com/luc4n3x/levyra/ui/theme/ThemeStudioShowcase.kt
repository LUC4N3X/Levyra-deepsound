package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress

@Composable
internal fun ThemeStudioShowcase(
    palette: ThemeStudioPalette,
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    artwork: (@Composable () -> Unit)? = null
) {
    val background by animateColorAsState(palette.background, LevyraPlayerDesign.emphasizedTween(420), label = "showcase-bg")
    val surface by animateColorAsState(palette.surface, LevyraPlayerDesign.emphasizedTween(420), label = "showcase-surface")
    val accent by animateColorAsState(palette.accent, LevyraPlayerDesign.emphasizedTween(420), label = "showcase-accent")
    val text by animateColorAsState(palette.text, LevyraPlayerDesign.emphasizedTween(420), label = "showcase-text")
    val muted by animateColorAsState(palette.muted, LevyraPlayerDesign.emphasizedTween(420), label = "showcase-muted")

    Surface(
        color = background,
        border = BorderStroke(LevyraPlayerDesign.Hairline, muted.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(ShowcaseCorner),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(ShowcaseArtwork)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surface)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        artwork()
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = muted, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .size(ShowcasePlay)
                        .background(accent, RoundedCornerShape(18.dp))
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(28.dp))
                }
            }
            LevyraWavyProgress(
                progress = 0.42f,
                color = accent,
                trackColor = muted.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
            )
        }
    }
}

internal fun themeStudioRowShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(ThemeRowOuterCorner)
    index == 0 -> RoundedCornerShape(topStart = ThemeRowOuterCorner, topEnd = ThemeRowOuterCorner, bottomStart = ThemeRowInnerCorner, bottomEnd = ThemeRowInnerCorner)
    index == count - 1 -> RoundedCornerShape(topStart = ThemeRowInnerCorner, topEnd = ThemeRowInnerCorner, bottomStart = ThemeRowOuterCorner, bottomEnd = ThemeRowOuterCorner)
    else -> RoundedCornerShape(ThemeRowInnerCorner)
}

@Composable
internal fun ThemeStudioThemeRow(
    palette: LevyraPalette,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    defaultLabel: String? = null,
    selectedLabel: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = palette.label
                val states = listOfNotNull(selectedLabel.takeIf { selected }, defaultLabel)
                if (states.isNotEmpty()) stateDescription = states.joinToString(", ")
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ThemeRowPreview)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.black)
                    .border(LevyraPlayerDesign.Hairline, palette.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(26.dp).background(palette.cyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.black, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(palette.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (defaultLabel != null) {
                    Text(defaultLabel, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                }
            }
            RadioButton(selected = selected, onClick = null, modifier = Modifier.clearAndSetSemantics { })
            trailing()
        }
    }
}

private val ShowcaseCorner = 28.dp
private val ShowcaseArtwork = 64.dp
private val ShowcasePlay = 56.dp
private val ThemeRowPreview = 44.dp
private val ThemeRowOuterCorner = 24.dp
private val ThemeRowInnerCorner = 6.dp
