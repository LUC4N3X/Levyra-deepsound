package com.luc4n3x.levyra.ui.regional

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLanguageOption
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.homestudio.HomeStudioSkinTokens
import com.luc4n3x.levyra.ui.homestudio.rememberHomeStudioLocale
import com.luc4n3x.levyra.ui.i18n.LanguagePickerCopy
import com.luc4n3x.levyra.ui.i18n.LevyraLanguagePickerStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlinx.coroutines.launch
import java.util.Locale

internal class LanguagePickerAutomatic(val label: String, val body: String)

internal fun filterLanguages(languages: List<LevyraLanguageOption>, query: String): List<LevyraLanguageOption> {
    val needle = query.trim()
    if (needle.isEmpty()) return languages
    return languages.filter { option ->
        option.nativeName.contains(needle, ignoreCase = true) ||
            option.englishName.contains(needle, ignoreCase = true) ||
            option.code.equals(needle, ignoreCase = true)
    }
}

internal fun suggestedLanguages(
    languages: List<LevyraLanguageOption>,
    selectedCode: String?,
    deviceLanguage: String
): List<LevyraLanguageOption> {
    val codes = listOfNotNull(selectedCode, LevyraLanguageCatalog.normalize(deviceLanguage), "en").distinct()
    return codes.mapNotNull { code -> languages.firstOrNull { it.code == code } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePickerSheet(
    title: String,
    note: String?,
    copy: LanguagePickerCopy,
    selectedCode: String?,
    automatic: LanguagePickerAutomatic?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()
    val haptics = LocalLevyraHaptics.current
    val tokens = HomeStudioSkinTokens.current()
    val locale = rememberHomeStudioLocale()
    var query by rememberSaveable { mutableStateOf("") }
    var committed by remember { mutableStateOf(false) }
    val languages = LevyraLanguageCatalog.selectableLanguages
    val selected = remember(selectedCode) { languages.firstOrNull { it.code == selectedCode } }
    val deviceLanguage = remember { Locale.getDefault().toLanguageTag() }
    val suggested = remember(selectedCode, deviceLanguage) { suggestedLanguages(languages, selectedCode, deviceLanguage) }
    val filtered = remember(query) { filterLanguages(languages, query) }
    val searching = query.isNotBlank()
    val choose: (String) -> Unit = choose@{ code ->
        if (committed) return@choose
        committed = true
        haptics.perform(LevyraHapticAction.Confirm)
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            onSelect(code)
        }
    }
    val columns = if (LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz) 1 else 2
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LevyraPanel,
        contentColor = LevyraText
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(if (columns == 1) 0.dp else 10.dp)
        ) {
            fullWidth("picker-hero") {
                if (tokens.expressive) ExpressiveLanguageHero(title, note, selected, automatic)
                else LanguagePickerHero(tokens, tokens.title(title, locale), note, selected, automatic)
            }
            fullWidth("picker-search") {
                LanguageSearchField(tokens, query, copy, onChange = { query = it })
            }
            if (automatic != null && !searching) automaticLanguageItem(automatic, tokens, selectedCode == null) { choose("") }
            if (searching) {
                searchResultItems(filtered, copy, tokens, selectedCode, columns == 1, choose)
            } else {
                val shownSuggestions = suggested.takeIf { it.size > 1 }.orEmpty()
                languageSection("picker-suggested", tokens.title(copy.suggested, locale), shownSuggestions, tokens, selectedCode, columns == 1, choose)
                languageSection("picker-all", tokens.title(copy.allLanguages, locale), languages.filterNot { it in shownSuggestions }, tokens, selectedCode, columns == 1, choose)
            }
        }
    }
}

private fun LazyGridScope.automaticLanguageItem(
    automatic: LanguagePickerAutomatic,
    tokens: HomeStudioSkinTokens,
    selected: Boolean,
    onClick: () -> Unit
) {
    fullWidth("picker-automatic") {
        LanguageTile(
            tokens = tokens,
            index = 0,
            name = automatic.label,
            supporting = automatic.body,
            selected = selected,
            rows = true,
            onClick = onClick
        ) { Icon(Icons.Rounded.GraphicEq, null, tint = if (selected) tokens.onAccent else tokens.accent, modifier = Modifier.size(22.dp)) }
    }
}

private fun LazyGridScope.searchResultItems(
    results: List<LevyraLanguageOption>,
    copy: LanguagePickerCopy,
    tokens: HomeStudioSkinTokens,
    selectedCode: String?,
    rows: Boolean,
    choose: (String) -> Unit
) {
    if (results.isEmpty()) {
        fullWidth("picker-empty") {
            Text(
                copy.noResults, color = LevyraMuted, fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 8.dp)
            )
        }
    } else {
        languageSection("picker-results", null, results, tokens, selectedCode, rows, choose)
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) { content() }
}

private fun LazyGridScope.languageSection(
    key: String,
    title: String?,
    languages: List<LevyraLanguageOption>,
    tokens: HomeStudioSkinTokens,
    selectedCode: String?,
    rows: Boolean,
    choose: (String) -> Unit
) {
    if (languages.isEmpty()) return
    if (title != null) {
        fullWidth("$key-title") {
            Text(
                title, color = LevyraText, style = tokens.display.copy(fontSize = 17.sp, lineHeight = 22.sp),
                modifier = Modifier.padding(top = 14.dp, start = 4.dp, bottom = 2.dp).semantics { heading() }
            )
        }
    }
    val mosaicStart = if (languages.size % 2 == 0 && languages.size < 5) 1 else 0
    languages.forEachIndexed { index, language ->
        val position = index + mosaicStart
        if (tokens.expressive) {
            item(
                key = "$key-${language.code}",
                span = { GridItemSpan(if (languageTileIsWide(position)) maxLineSpan else 1) },
                contentType = if (languageTileIsWide(position)) "language-wide" else "language-tile"
            ) { ExpressiveLanguageTile(language, position, language.code == selectedCode) { choose(language.code) } }
        } else item(key = "$key-${language.code}", contentType = "language-tile") {
            LanguageTile(
                tokens = tokens,
                index = language.code.hashCode() + index,
                name = language.nativeName,
                supporting = LevyraLanguagePickerStrings.greeting(language.code),
                selected = language.code == selectedCode,
                rows = rows,
                onClick = { choose(language.code) }
            ) { Text(language.flag, fontSize = 22.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun languageBadgeShape(tokens: HomeStudioSkinTokens, index: Int, selected: Boolean): Shape {
    if (!tokens.expressive) return tokens.thumbShape
    val polygon = when {
        selected -> MaterialShapes.Cookie12Sided
        else -> when (Math.floorMod(index, 6)) {
            0 -> MaterialShapes.Cookie4Sided
            1 -> MaterialShapes.Clover4Leaf
            2 -> MaterialShapes.Sunny
            3 -> MaterialShapes.Cookie6Sided
            4 -> MaterialShapes.Pill
            else -> MaterialShapes.Gem
        }
    }
    return polygon.toShape()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguagePickerHero(
    tokens: HomeStudioSkinTokens,
    title: String,
    note: String?,
    selected: LevyraLanguageOption?,
    automatic: LanguagePickerAutomatic?
) {
    val animations = LocalAnimationsEnabled.current
    Column(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(tokens.cardShape).background(tokens.cardLifted).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.semantics { heading() })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedContent(
                targetState = selected?.code,
                transitionSpec = {
                    if (animations) {
                        fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 0.6f) togetherWith fadeOut() + scaleOut(targetScale = 1.2f)
                    } else {
                        fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
                    }
                },
                label = "language-hero-badge"
            ) { code ->
                val option = LevyraLanguageCatalog.selectableLanguages.firstOrNull { it.code == code }
                Box(
                    Modifier.size(76.dp)
                        .background(tokens.accent, if (tokens.expressive) MaterialShapes.Cookie9Sided.toShape() else tokens.thumbShape)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center
                ) {
                    if (option != null) Text(option.flag, fontSize = 34.sp)
                    else Icon(Icons.Rounded.GraphicEq, null, tint = tokens.onAccent, modifier = Modifier.size(36.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    selected?.nativeName ?: automatic?.label.orEmpty(), color = LevyraText,
                    style = tokens.display.copy(fontSize = 27.sp, lineHeight = 32.sp), maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                val secondary = selected?.englishName?.takeUnless { it.equals(selected.nativeName, ignoreCase = true) }
                if (secondary != null) Text(secondary, color = LevyraMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (note != null) Text(note, color = LevyraMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun LanguageSearchField(tokens: HomeStudioSkinTokens, query: String, copy: LanguagePickerCopy, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(tokens.controlShape).background(tokens.card).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = LevyraMuted, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(copy.search, color = LevyraMuted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = LevyraText, fontSize = 16.sp),
                cursorBrush = SolidColor(tokens.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = copy.search }
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, copy.clearSearch, tint = LevyraMuted) }
        } else {
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun LanguageTileBadge(
    tokens: HomeStudioSkinTokens,
    index: Int,
    selected: Boolean,
    rows: Boolean,
    turn: () -> Float,
    leading: @Composable () -> Unit
) {
    val fill = if (selected) tokens.onAccent.copy(alpha = 0.18f) else tokens.cardLifted
    Box(
        Modifier.size(if (rows) 42.dp else 46.dp)
            .graphicsLayer { rotationZ = turn() }
            .background(fill, languageBadgeShape(tokens, index, selected))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) { Box(Modifier.graphicsLayer { rotationZ = -turn() }) { leading() } }
}

@Composable
private fun LanguageTileCheck(tokens: HomeStudioSkinTokens, selected: Boolean) {
    AnimatedVisibility(selected, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        Icon(Icons.Rounded.Check, null, tint = tokens.onAccent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun LanguageTileTexts(tokens: HomeStudioSkinTokens, name: String, supporting: String?, selected: Boolean, modifier: Modifier) {
    val nameColor = if (selected) tokens.onAccent else LevyraText
    val supportColor = if (selected) tokens.onAccent.copy(alpha = 0.78f) else LevyraMuted
    Column(modifier) {
        Text(name, color = nameColor, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (supporting != null) Text(supporting, color = supportColor, fontSize = 12.5.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LanguageTile(
    tokens: HomeStudioSkinTokens,
    index: Int,
    name: String,
    supporting: String?,
    selected: Boolean,
    rows: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit
) {
    val animations = LocalAnimationsEnabled.current
    val background by animateColorAsState(
        when {
            selected -> tokens.accent
            rows -> Color.Transparent
            else -> tokens.card
        },
        label = "language-tile-background"
    )
    val badgeTurn by animateFloatAsState(
        if (selected && animations) 90f else 0f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "language-tile-turn"
    )
    val outlined = !selected && !rows
    val tileModifier = Modifier.fillMaxWidth().heightIn(min = if (rows) 60.dp else 112.dp)
        .clip(tokens.cardShape)
        .background(background)
        .then(if (outlined) Modifier.border(1.dp, tokens.line, tokens.cardShape) else Modifier)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp)
    val badge: @Composable () -> Unit = { LanguageTileBadge(tokens, index, selected, rows, { badgeTurn }, leading) }
    val check: @Composable () -> Unit = { LanguageTileCheck(tokens, selected) }
    val texts: @Composable (Modifier) -> Unit = { modifier -> LanguageTileTexts(tokens, name, supporting, selected, modifier) }
    if (rows) {
        Row(tileModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            badge()
            texts(Modifier.weight(1f))
            check()
        }
    } else {
        Column(tileModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                badge()
                check()
            }
            texts(Modifier.fillMaxWidth())
        }
    }
}
