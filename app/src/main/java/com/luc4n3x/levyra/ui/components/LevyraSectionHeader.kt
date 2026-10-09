package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
                        .weight(1f, fill = false)
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
    trailingIcon: ImageVector? = null
) {
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
                .heightIn(min = SectionActionVisualHeight)
                .clip(shape)
                .background(LevyraAdaptiveChip)
                .padding(
                    start = if (leadingIcon != null) 10.dp else 14.dp,
                    end = if (trailingIcon != null) 8.dp else 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = LevyraText, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                color = LevyraText,
                style = LevyraType.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            trailingIcon?.let {
                Icon(it, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun LevyraSectionPlayAll(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LevyraSectionAction(
        label = LocalLevyraStrings.current.playAll,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = Icons.Rounded.PlayArrow
    )
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
