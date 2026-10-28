package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.FreshDiscoverySettings
import com.luc4n3x.levyra.domain.FreshDiscoveryWindow
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.freshDiscoveryCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.SkinSwitch

@Composable
internal fun SettingsSwitchIndicator(checked: Boolean) {
    if (LevyraIsNocturne || LevyraIsTorque || LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic) SkinSwitch(checked) else Switch(
        checked = checked,
        onCheckedChange = null,
        thumbContent = if (checked) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (LevyraIsPrism) Color.White else LevyraBlack,
            checkedIconColor = LevyraCyan,
            checkedTrackColor = LevyraCyan,
            uncheckedThumbColor = LevyraMuted,
            uncheckedTrackColor = LevyraAdaptiveTrack,
            uncheckedBorderColor = LevyraMuted.copy(alpha = 0.6f)
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FreshDiscoverySettingsCard(
    settings: FreshDiscoverySettings,
    onSettings: (FreshDiscoverySettings) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { freshDiscoveryCopy(strings.code) }
    val accent = LocalSettingsAccent.current ?: LevyraCyan
    val animated = LocalAnimationsEnabled.current
    val groupCount = if (settings.enabled) 3 else 1
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsTile(
            checked = settings.enabled,
            onCheckedChange = { onSettings(settings.copy(enabled = it)) },
            groupIndex = 0,
            groupCount = groupCount
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsTileIcon(icon = Icons.Rounded.Explore, accent = accent, shape = MaterialShapes.Sunny.toShape())
                    Text(
                        text = copy.title,
                        color = LevyraText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitchIndicator(settings.enabled)
                }
                Text(
                    text = copy.description,
                    color = LevyraMuted,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
        AnimatedVisibility(
            visible = settings.enabled,
            enter = if (animated) expandVertically() + fadeIn() else EnterTransition.None,
            exit = if (animated) shrinkVertically() + fadeOut() else ExitTransition.None
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsTile(groupIndex = 1, groupCount = 3) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            SettingsTileIcon(icon = Icons.Rounded.History, accent = accent)
                            SettingsTileText(
                                title = copy.windowTitle,
                                subtitle = copy.windowDescription,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FreshDiscoveryWindow.entries.forEach { window ->
                                FreshDiscoveryWindowPill(
                                    label = copy.window(window),
                                    selected = window == settings.window,
                                    accent = accent,
                                    animated = animated,
                                    onClick = { onSettings(settings.copy(window = window)) }
                                )
                            }
                        }
                    }
                }
                SettingsTile(
                    checked = settings.allowFavorites,
                    onCheckedChange = { onSettings(settings.copy(allowFavorites = it)) },
                    groupIndex = 2,
                    groupCount = 3
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsTileIcon(icon = Icons.Rounded.Favorite, accent = accent, shape = MaterialShapes.Heart.toShape())
                        SettingsTileText(
                            title = copy.allowFavoritesTitle,
                            subtitle = copy.allowFavoritesDescription,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsSwitchIndicator(settings.allowFavorites)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreshDiscoveryWindowPill(
    label: String,
    selected: Boolean,
    accent: Color,
    animated: Boolean,
    onClick: () -> Unit
) {
    val corner by animateDpAsState(
        targetValue = if (selected) 24.dp else 12.dp,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "fresh-discovery-pill-corner"
    )
    val container by animateColorAsState(
        targetValue = if (selected) accent else LevyraAdaptiveTrack,
        animationSpec = if (animated) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "fresh-discovery-pill-container"
    )
    val content = when {
        !selected -> LevyraText
        LevyraIsPrism -> Color.White
        else -> LevyraBlack
    }
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        )
    }
}
