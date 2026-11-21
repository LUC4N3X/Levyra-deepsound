package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Surface
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.theme.levyraSoftAccent
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.ui.components.LevyraUtilityStyleActive
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.toShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import com.luc4n3x.levyra.viewmodel.limitUserName
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
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
            itemsIndexed(options, key = { _, option -> option.first }, contentType = { _, _ -> "SettingsControls-options" }) { index, option ->
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
internal fun SettingsHubSearchField(
    query: String,
    placeholder: String,
    clearLabel: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(LevyraCyan),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { contentDescription = placeholder }
            .padding(bottom = if (LevyraIsDefaultSkin) 0.dp else 16.dp)
            .background(
                if (LevyraIsDefaultSkin) defaultSettingsCardColor() else LevyraAdaptiveCard,
                when {
                    SkinChrome.active -> SkinChrome.fieldShape
                    LevyraIsDefaultSkin -> MaterialTheme.shapes.extraLarge
                    else -> CircleShape
                }
            )
            .padding(horizontal = 18.dp, vertical = if (LevyraIsDefaultSkin) 0.dp else 16.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = if (LevyraIsDefaultSkin) Modifier.heightIn(min = 56.dp) else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) Text(placeholder, color = LevyraMuted, fontSize = 15.sp)
                    innerTextField()
                }
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, clearLabel, tint = LevyraMuted)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsStudioShelf(
    title: String,
    subtitle: String,
    accentSeed: Color,
    entries: List<SettingsStudioEntry>
) {
    if (LevyraIsAtelier) return AtelierStudioShelf(title, subtitle, entries)
    val compact = LevyraIsDefaultSkin
    Column(modifier = Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else 6.dp)) {
        if (compact) DefaultSettingsGroupLabel(title)
        else {
            Text(
                title,
                color = LevyraText,
                fontSize = 20.sp,
                fontWeight = if (SkinChrome.active) FontWeight.Medium else FontWeight.Black,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { heading() }
            )
            Text(
                subtitle,
                color = LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp)
            )
        }
        LazyRow(
            modifier = Modifier.recentSearchesBleed(SettingsStudioGutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = SettingsStudioGutter)
        ) {
            items(entries, key = { it.id }, contentType = { "LevyraApp-entries" }) { entry ->
                if (compact) DefaultSettingsStudioShortcut(entry) else SettingsStudioShortcut(entry, accentSeed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsStudioShortcut(entry: SettingsStudioEntry, accentSeed: Color) {
    Surface(
        onClick = entry.onClick,
        shape = RoundedCornerShape(if (SkinChrome.active) SkinChrome.outerCorner else 22.dp),
        color = LevyraAdaptiveCard,
        modifier = Modifier.widthIn(min = 180.dp).width(IntrinsicSize.Min).heightIn(min = 80.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(if (SkinChrome.active) SkinChrome.iconShape else entry.shape.toShape())
                    .background(
                        when {
                            SkinChrome.active -> SkinChrome.iconBackground
                            LevyraUtilityStyleActive -> settingsCategoryTone(accentSeed, solid = true).container
                            else -> LevyraText.copy(alpha = 0.08f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    entry.icon,
                    null,
                    tint = when {
                        SkinChrome.active -> SkinChrome.iconTint
                        LevyraUtilityStyleActive -> settingsCategoryTone(accentSeed, solid = true).content
                        else -> LevyraText.copy(alpha = 0.88f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            SettingsStudioShortcutTitle(entry.title)
        }
    }
}

@Composable
private fun DefaultSettingsStudioShortcut(entry: SettingsStudioEntry) {
    Surface(
        onClick = entry.onClick,
        shape = RoundedCornerShape(22.dp),
        color = defaultSettingsCardColor(),
        modifier = Modifier.widthIn(min = 148.dp).width(IntrinsicSize.Min).heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultSettingsAccentIcon(entry.icon, entry.accent)
            SettingsStudioShortcutTitle(entry.title)
        }
    }
}

@Composable
private fun RowScope.SettingsStudioShortcutTitle(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        softWrap = true,
        modifier = Modifier.weight(1f)
    )
}

@Composable
internal fun DefaultSettingsGroupLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 14.dp, top = 22.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
internal fun DefaultSettingsHubHeader(title: String, backLabel: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
    }
}

@Stable
internal class SettingsSearchFocus(val title: String, val onReached: () -> Unit) {
    var reached by mutableStateOf(false)
}

internal val LocalSettingsSearchFocus = compositionLocalOf<SettingsSearchFocus?> { null }

internal val LocalSettingsTileHighlight = compositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun SettingsUserNameField(
    name: String,
    title: String,
    placeholder: String,
    clearLabel: String,
    onNameChange: (String) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(name) }
    var syncedName by rememberSaveable { mutableStateOf(name) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(name) {
        if (name == syncedName) return@LaunchedEffect
        syncedName = name
        if (!focused) draft = name
    }
    val focusManager = LocalFocusManager.current
    val accent = LocalSettingsAccent.current ?: LevyraCyan
    val latestDraft by rememberUpdatedState(draft)
    val latestFocused by rememberUpdatedState(focused)
    val commit by rememberUpdatedState(onNameChange)
    DisposableEffect(Unit) { onDispose { if (latestFocused) commit(latestDraft) } }
    SettingsTile {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsTileIcon(Icons.Rounded.Person, accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraMuted, style = MaterialTheme.typography.labelMedium)
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = limitUserName(it) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = LevyraText),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = title }
                        .onFocusChanged { state ->
                            if (focused && !state.isFocused) onNameChange(draft)
                            focused = state.isFocused
                        },
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text(placeholder, color = LevyraMuted, style = MaterialTheme.typography.titleMedium)
                            inner()
                        }
                    }
                )
            }
            if (draft.isNotEmpty()) {
                IconButton(onClick = { draft = ""; onNameChange("") }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, clearLabel, tint = LevyraMuted)
                }
            }
        }
    }
}

@Composable
internal fun DefaultSettingsDetailHeader(title: String, summary: String, backLabel: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        DefaultSettingsHubHeader(title, backLabel, onBack)
        if (summary.isNotBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 60.dp, end = 14.dp)
            )
        }
    }
}

@Composable
internal fun DefaultSettingsAccentIcon(icon: ImageVector, accent: Color) {
    val tone = defaultSettingsAccentTone(accent)
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(tone.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun defaultSettingsCardColor(): Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)

@Composable
private fun defaultSettingsAccentTone(accent: Color): SettingsExpressiveTone {
    val colors = levyraUtilityColors(accent)
    return SettingsExpressiveTone(container = colors.accent.copy(alpha = 0.24f), content = colors.accent)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsHubCategoryRow(
    title: String,
    summary: String,
    icon: ImageVector,
    index: Int,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = settingsHubRowStyle(index, accent)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = style.rowInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(style.rowGap)
        ) {
            Box(
                modifier = Modifier
                    .size(style.iconSize)
                    .clip(style.iconShape)
                    .background(style.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = style.iconTint, modifier = Modifier.size(style.glyphSize))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = LevyraText,
                    style = style.title,
                    maxLines = if (style.marquee) 1 else Int.MAX_VALUE,
                    modifier = Modifier.levyraMarquee(style.marquee, 3000)
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = style.summaryColor,
                        style = style.summary
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
    if (LevyraIsDefaultSkin) {
        DefaultSettingsGroupedRow(index, count, modifier, onClick, content)
    } else {
        SettingsTile(onClick = onClick, groupIndex = index, groupCount = count, modifier = modifier, content = content)
    }
}

@Composable
private fun DefaultSettingsGroupedRow(
    index: Int,
    count: Int,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val top = if (index == 0) LevyraCardDesign.SurfaceCorner else 0.dp
    val bottom = if (index == count - 1) LevyraCardDesign.SurfaceCorner else 0.dp
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = defaultSettingsCardColor()
    ) {
        Column {
            if (index > 0) HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp, end = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
            content()
        }
    }
}

@Immutable
private data class SettingsHubRowStyle(
    val iconSize: Dp,
    val iconShape: Shape,
    val iconContainer: Color,
    val iconTint: Color,
    val title: TextStyle,
    val summary: TextStyle,
    val summaryColor: Color,
    val glyphSize: Dp = 24.dp,
    val rowInset: Dp = 15.dp,
    val rowGap: Dp = 16.dp,
    val marquee: Boolean = false
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun settingsHubRowStyle(index: Int, accent: Color): SettingsHubRowStyle {
    val base = LocalTextStyle.current
    val marquee = LocalAnimationsEnabled.current
    if (LevyraIsDefaultSkin) {
        val tone = defaultSettingsAccentTone(accent)
        return SettingsHubRowStyle(
            iconSize = 36.dp,
            iconShape = CircleShape,
            iconContainer = tone.container,
            iconTint = tone.content,
            title = MaterialTheme.typography.titleMedium,
            summary = MaterialTheme.typography.bodyMedium,
            summaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
            glyphSize = 20.dp,
            rowInset = 10.dp,
            rowGap = 12.dp
        )
    }
    if (!LevyraUtilityStyleActive) {
        return SettingsHubRowStyle(
            iconSize = 48.dp,
            iconShape = CircleShape,
            iconContainer = LevyraText.copy(alpha = 0.08f),
            iconTint = LevyraText.copy(alpha = 0.88f),
            title = base.merge(TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)),
            summary = base.merge(TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)),
            summaryColor = LevyraMuted,
            marquee = marquee
        )
    }
    val tone = settingsCategoryTone(accent, solid = true)
    return SettingsHubRowStyle(
        iconSize = 52.dp,
        iconShape = settingsExpressiveShape(index).toShape(),
        iconContainer = tone.container,
        iconTint = tone.content,
        title = MaterialTheme.typography.titleLarge,
        summary = MaterialTheme.typography.bodyMedium,
        summaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
        marquee = marquee
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun settingsExpressiveShape(index: Int): RoundedPolygon = when (index % 6) {
    0 -> MaterialShapes.Cookie9Sided
    1 -> MaterialShapes.Clover4Leaf
    2 -> MaterialShapes.Cookie6Sided
    3 -> MaterialShapes.Sunny
    4 -> MaterialShapes.Cookie4Sided
    else -> MaterialShapes.SoftBurst
}

@Immutable
internal data class SettingsExpressiveTone(val container: Color, val content: Color)

@Composable
internal fun settingsCategoryTone(accent: Color, solid: Boolean = false): SettingsExpressiveTone {
    val colors = levyraUtilityColors(accent)
    return if (solid) {
        SettingsExpressiveTone(container = colors.accent, content = colors.onAccent)
    } else {
        SettingsExpressiveTone(container = colors.softContainer, content = colors.accent)
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
    LevyraUtilityStyleActive -> levyraUtilityColors(LocalSettingsAccent.current ?: LevyraUtilitySeed).accent
    else -> LocalSettingsAccent.current ?: LevyraCyan
}
