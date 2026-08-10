package com.luc4n3x.levyra.desktop.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.components.levyraIconPainter
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraMotion
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor

@Composable
fun LevyraSidebar(
    destination: Destination,
    hasActiveTrack: Boolean,
    isPlaying: Boolean,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val shellShape = RoundedCornerShape(20.dp)
    val shellBrush = Brush.verticalGradient(
        colors = listOf(
            accent.copy(alpha = 0.13f),
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surface
        )
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            accent.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        )
    )

    Box(
        modifier = modifier
            .width(228.dp)
            .fillMaxHeight()
            .clip(shellShape)
            .background(shellBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shellShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 13.dp, vertical = 16.dp)
        ) {
            SidebarBrand(
                appName = strings.appName,
                accent = accent
            )

            Spacer(modifier = Modifier.height(27.dp))
            SidebarSectionLabel(strings.navSectionExplore)
            Spacer(modifier = Modifier.height(8.dp))

            SidebarItem(
                icon = LevyraIcons.Home,
                label = strings.navHome,
                selected = destination == Destination.HOME,
                onClick = { onNavigate(Destination.HOME) }
            )
            SidebarItem(
                icon = LevyraIcons.Chart,
                label = strings.navDiscover,
                selected = destination == Destination.DISCOVER,
                onClick = { onNavigate(Destination.DISCOVER) }
            )
            SidebarItem(
                icon = LevyraIcons.Search,
                label = strings.navSearch,
                selected = destination == Destination.SEARCH || destination == Destination.COLLECTION,
                onClick = { onNavigate(Destination.SEARCH) }
            )

            Spacer(modifier = Modifier.height(22.dp))
            SidebarSectionLabel(strings.navSectionLibrary)
            Spacer(modifier = Modifier.height(8.dp))

            SidebarItem(
                icon = OfflineIcons.Library,
                label = strings.navLibrary,
                selected = destination == Destination.LIBRARY || destination == Destination.PLAYLIST,
                onClick = { onNavigate(Destination.LIBRARY) }
            )
            SidebarItem(
                icon = LevyraIcons.Disc,
                label = strings.navNowPlaying,
                selected = destination == Destination.NOW_PLAYING,
                trailing = if (hasActiveTrack) {
                    {
                        SidebarPlayingIndicator(
                            isPlaying = isPlaying,
                            color = if (destination == Destination.NOW_PLAYING) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                accent
                            }
                        )
                    }
                } else {
                    null
                },
                onClick = { onNavigate(Destination.NOW_PLAYING) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(10.dp))

            SidebarItem(
                icon = LevyraIcons.Settings,
                label = strings.navSettings,
                selected = destination == Destination.SETTINGS,
                onClick = { onNavigate(Destination.SETTINGS) }
            )
        }
    }
}

@Composable
private fun SidebarBrand(
    appName: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(15.dp),
            color = accent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent)
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = levyraIconPainter(),
                    contentDescription = appName,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "DESKTOP",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.25.sp
                ),
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SidebarSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.65.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val interactionSource = remember(label) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    val targetBackground = when {
        selected -> accent.copy(alpha = 0.17f)
        hovered || focused -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
        else -> Color.Transparent
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 160)
    )
    val targetContentColor = when {
        selected -> MaterialTheme.colorScheme.onSurface
        hovered || focused -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 140)
    )
    val iconBackground by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = 0.22f)
        } else if (hovered || focused) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 160)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.28f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (selected) 26.dp else 12.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (selected) accent else Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) accent else contentColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            trailing?.invoke()
        }
    }
}

@Composable
private fun SidebarPlayingIndicator(
    isPlaying: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "sidebarPlaying")
    val first by transition.animateFloat(
        initialValue = 5f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sidebarPlayingFirst"
    )
    val second by transition.animateFloat(
        initialValue = 13f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 430),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sidebarPlayingSecond"
    )
    val third by transition.animateFloat(
        initialValue = 7f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 610),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sidebarPlayingThird"
    )

    Row(
        modifier = Modifier
            .width(22.dp)
            .height(17.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(first, second, third).forEach { animatedHeight ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((if (isPlaying) animatedHeight else 5f).dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color.copy(alpha = if (isPlaying) 0.95f else LevyraMotion.HOVER_ALPHA + 0.32f))
            )
        }
    }
}
