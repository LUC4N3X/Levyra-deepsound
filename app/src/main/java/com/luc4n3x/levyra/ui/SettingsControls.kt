package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.SkinChrome
import com.luc4n3x.levyra.ui.components.levyraMarquee
import kotlinx.coroutines.launch

@Composable
internal fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    groupIndex: Int? = null,
    groupCount: Int? = null
) {
    SettingsTile(onClick = onClick, groupIndex = groupIndex, groupCount = groupCount) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsTileIcon(icon = icon, accent = LocalSettingsAccent.current ?: LevyraPink)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsTileText(title = title, subtitle = subtitle)
                if (value.isNotBlank()) {
                    Text(
                        text = value,
                        color = settingsSelectionColor(),
                        fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LevyraMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun SettingsChoiceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    SettingsValueRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
        onClick = { sheetOpen = true }
    )
    if (sheetOpen) {
        SettingsChoiceSheet(
            title = title,
            description = subtitle,
            options = options,
            selected = selected,
            onSelect = onSelect,
            onDismiss = { sheetOpen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsChoiceSheet(
    title: String,
    description: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()
    val haptics = LocalLevyraHaptics.current
    var committed by remember { mutableStateOf(false) }
    val choose: (String) -> Unit = choose@{ value ->
        if (committed) return@choose
        committed = true
        haptics.perform(LevyraHapticAction.Confirm)
        onSelect(value)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LevyraPanel,
        contentColor = LevyraText
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "settings-choice-header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = LevyraText,
                        fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = if (SkinChrome.active) FontWeight.SemiBold else FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            color = LevyraMuted,
                            fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            itemsIndexed(options, key = { _, option -> option.first }) { index, option ->
                SettingsChoiceOption(
                    label = option.second,
                    active = option.first == selected,
                    index = index,
                    count = options.size,
                    onClick = { choose(option.first) }
                )
            }
        }
    }
}

@Composable
private fun SettingsChoiceOption(label: String, active: Boolean, index: Int, count: Int, onClick: () -> Unit) {
    SettingsTile(
        onClick = onClick,
        groupIndex = index,
        groupCount = count,
        modifier = Modifier.semantics {
            selected = active
            role = Role.RadioButton
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = label,
                color = if (active) settingsSelectionColor() else LevyraText,
                fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
                fontSize = 16.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = if (LocalAnimationsEnabled.current) 1 else Int.MAX_VALUE,
                modifier = Modifier.weight(1f).levyraMarquee(LocalAnimationsEnabled.current, 3000)
            )
            SettingsSelectionMark(active)
        }
    }
}

@Composable
private fun SettingsSelectionMark(active: Boolean) {
    val animate = LocalAnimationsEnabled.current
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (animate) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "settingsSelectionMark"
    )
    val fill = settingsSelectionColor()
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, if (active) fill else LevyraMuted.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = progress
                    scaleY = progress
                    alpha = progress.coerceIn(0f, 1f)
                }
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = settingsSelectionContent(), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun settingsSelectionColor(): Color {
    if (!SkinChrome.active) return LocalSettingsAccent.current ?: LevyraCyan
    val container = SkinChrome.chipSelectedContainer
    return if (container == Color.Transparent) SkinChrome.chipSelectedContent else container
}

private fun settingsSelectionContent(): Color {
    if (!SkinChrome.active) return LevyraBlack
    return if (SkinChrome.chipSelectedContainer == Color.Transparent) LevyraBlack else SkinChrome.chipSelectedContent
}

internal fun SettingsGroupCollector.settingsDisclosureSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    rows: List<@Composable () -> Unit>
) {
    val count = rows.size + 1
    standaloneItem {
        SettingsDisclosureRow(
            title = title,
            summary = summary,
            expanded = expanded,
            groupCount = if (expanded) count else 1,
            onToggle = onToggle
        )
    }
    standaloneItem {
        val animate = LocalAnimationsEnabled.current
        val motion = spring<androidx.compose.ui.unit.IntSize>(stiffness = Spring.StiffnessMediumLow)
        AnimatedVisibility(
            visible = expanded,
            enter = if (animate) expandVertically(motion) + fadeIn() else EnterTransition.None,
            exit = if (animate) shrinkVertically(motion) + fadeOut() else ExitTransition.None
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.forEachIndexed { index, row ->
                    CompositionLocalProvider(LocalSettingsGroupSlot provides SettingsGroupSlot(index + 1, count)) { row() }
                }
            }
        }
    }
}

@Composable
private fun SettingsDisclosureRow(title: String, summary: String, expanded: Boolean, groupCount: Int, onToggle: () -> Unit) {
    val animate = LocalAnimationsEnabled.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (animate) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "settingsDisclosureChevron"
    )
    SettingsTile(
        onClick = onToggle,
        groupIndex = 0,
        groupCount = groupCount,
        modifier = Modifier.semantics {
            if (expanded) collapse { onToggle(); true } else expand { onToggle(); true }
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsTileIcon(icon = Icons.Rounded.Tune, accent = LocalSettingsAccent.current ?: LevyraCyan)
            SettingsTileText(title = title, subtitle = if (expanded) "" else summary, modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

internal fun Modifier.settingsEntrance(progress: () -> Float, order: Int): Modifier = graphicsLayer {
    val start = order.coerceAtMost(SETTINGS_ENTRANCE_MAX_ORDER) * SETTINGS_ENTRANCE_STAGGER
    val local = ((progress() - start) / SETTINGS_ENTRANCE_SPAN).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(local)
    alpha = eased
    translationY = (1f - eased) * 16.dp.toPx()
}

private const val SETTINGS_ENTRANCE_MAX_ORDER = 14
private const val SETTINGS_ENTRANCE_STAGGER = 0.035f
private const val SETTINGS_ENTRANCE_SPAN = 0.5f

@Composable
internal fun SettingsHubCategoryRow(
    title: String,
    summary: String,
    icon: ImageVector,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SettingsTile(onClick = onClick, groupIndex = index, groupCount = count, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LevyraText.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = LevyraText.copy(alpha = 0.88f), modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (LocalAnimationsEnabled.current) 1 else Int.MAX_VALUE,
                    modifier = Modifier.levyraMarquee(LocalAnimationsEnabled.current, 3000)
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LevyraMuted.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private const val SETTINGS_TITLE_MARQUEE_DELAY = 3000

@Composable
internal fun SettingsTileTitle(title: String, maxLines: Int) {
    val marquee = LocalAnimationsEnabled.current && maxLines == Int.MAX_VALUE
    Text(
        title,
        color = LevyraText,
        fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
        fontSize = if (LevyraIsAppleMusic || LevyraIsAtelier) 17.sp else 16.sp,
        fontWeight = settingsTileTitleWeight(),
        maxLines = if (marquee) 1 else maxLines,
        overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = Modifier.levyraMarquee(marquee, SETTINGS_TITLE_MARQUEE_DELAY)
    )
}

private fun settingsTileTitleWeight(): FontWeight = when {
    LevyraIsAppleMusic -> FontWeight.Normal
    SkinChrome.active -> FontWeight.Medium
    else -> FontWeight.Bold
}

@Composable
internal fun settingsSectionLabelColor(): Color = when {
    LevyraIsPrism && !LevyraIsTorque -> LevyraMuted
    SkinChrome.active -> LevyraCyan
    else -> LocalSettingsAccent.current ?: LevyraCyan
}
