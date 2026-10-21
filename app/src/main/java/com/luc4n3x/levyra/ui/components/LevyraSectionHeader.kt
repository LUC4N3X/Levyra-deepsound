package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.LevyraAdaptiveChip
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

private val SectionHeaderMinHeight = 48.dp
private val SectionActionVisualHeight = 34.dp
private val SectionActionRestCorner = 17.dp
private val SectionActionPressedCorner = 10.dp

@Composable
internal fun LevyraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = LevyraText,
    subtitleColor: Color = LevyraMuted,
    onTitleClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val detail = subtitle?.trim().orEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SectionHeaderMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onTitleClick != null) {
                        Modifier
                            .heightIn(min = SectionHeaderMinHeight)
                            .levyraPressable(
                                onClick = onTitleClick,
                                onClickLabel = title,
                                role = Role.Button,
                                pressedScale = LevyraPressScale.Row
                            )
                    } else {
                        Modifier
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionLineGap, Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = titleColor,
                    style = LevyraType.sectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = onTitleClick == null)
                        .semantics { heading() }
                )
                if (onTitleClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = subtitleColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    color = subtitleColor,
                    style = LevyraType.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

@Composable
internal fun LevyraSectionAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    compact: Boolean = false,
    tonal: Boolean = false
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsSpotify) {
        SpotifyTextAction(label, onClick, modifier)
        return
    }
    val palette = sectionActionPalette(tonal)
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = SectionActionRestCorner,
        pressed = SectionActionPressedCorner,
        label = "section-action-corner"
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .heightIn(min = SectionHeaderMinHeight)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = if (compact) 30.dp else SectionActionVisualHeight)
                .clip(shape)
                .background(palette.container)
                .padding(sectionActionPadding(compact, leadingIcon != null, trailingIcon != null)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = palette.content, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
            }
            Text(
                text = label,
                color = palette.content,
                style = if (compact) LevyraType.cardTitle.copy(fontSize = 12.sp, lineHeight = 16.sp) else LevyraType.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            trailingIcon?.let {
                Icon(it, contentDescription = null, tint = palette.trailing, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private class SectionActionPalette(val container: Color, val content: Color, val trailing: Color)

@Composable
private fun sectionActionPalette(tonal: Boolean): SectionActionPalette {
    val colors = MaterialTheme.colorScheme
    return if (tonal) {
        SectionActionPalette(colors.secondaryContainer, colors.onSecondaryContainer, colors.onSecondaryContainer)
    } else {
        SectionActionPalette(LevyraAdaptiveChip, LevyraText, LevyraMuted)
    }
}

private fun sectionActionPadding(compact: Boolean, leading: Boolean, trailing: Boolean): PaddingValues = when {
    compact -> PaddingValues(start = 7.dp, end = 8.dp)
    else -> PaddingValues(start = if (leading) 10.dp else 14.dp, end = if (trailing) 8.dp else 14.dp)
}

@Composable
internal fun LevyraSectionPlayAll(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LevyraPlayAllButton(onClick = onClick, modifier = modifier)
}

@Composable
internal fun LevyraSectionShowAll(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LevyraSectionAction(
        label = LocalLevyraStrings.current.showAll,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    )
}
