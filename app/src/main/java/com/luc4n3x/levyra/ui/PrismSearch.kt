package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.PrismPaletteId

internal val LevyraIsPrismOnly: Boolean get() = LevyraActivePalette.id == PrismPaletteId

private val prismSearchSurface: Color get() = if (LevyraActivePalette.isLight) Color(0xFFF4EEF2) else Color(0xFF110E13)
private val prismSearchHairline: Color get() = LevyraText.copy(alpha = 0.10f)

@Composable
internal fun PrismSearchHeader(
    query: String,
    isSearching: Boolean,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onVoice: () -> Unit,
    onRecognize: () -> Unit,
    onLyrics: (() -> Unit)?
) {
    val strings = LocalLevyraStrings.current
    var focused by remember { mutableStateOf(false) }
    val shape = PrismDesign.cut(16.dp)
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(PrismDesign.cut(14.dp)).background(prismSearchSurface)
                .border(1.dp, prismSearchHairline, PrismDesign.cut(14.dp))
                .clickable(role = Role.Button, onClickLabel = strings.back, onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = LevyraText) }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.weight(1f).heightIn(min = 52.dp).clip(shape).background(prismSearchSurface)
                .border(1.dp, if (focused) PrismAccent.gradient() else SolidColor(prismSearchHairline), shape)
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 14.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = LevyraPink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(strings.search, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LevyraText),
                        cursorBrush = SolidColor(LevyraPink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = strings.searchPlaceholder }
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, strings.clear, tint = LevyraMuted) }
                } else {
                    if (onLyrics != null) IconButton(onClick = onLyrics, modifier = Modifier.size(42.dp)) { Icon(Icons.Rounded.FormatQuote, strings.lyrics, tint = LevyraPink, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onVoice, modifier = Modifier.size(42.dp)) { Icon(Icons.Rounded.Mic, strings.voice, tint = LevyraMuted, modifier = Modifier.size(20.dp)) }
                    if (recognitionAvailable) {
                        IconButton(onClick = onRecognize, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.GraphicEq, strings.recognizeMusic, tint = if (recognitionBusy) LevyraPink else LevyraMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (isSearching && query.isNotEmpty()) {
                LinearProgressIndicator(
                    color = LevyraPink, trackColor = Color.Transparent,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp).fillMaxWidth().height(2.dp)
                )
            }
        }
    }
}

@Composable
internal fun PrismSearchFilters(chips: List<Pair<SearchFilter, String>>, selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        items(chips, key = { it.first.name }) { (filter, label) ->
            val active = filter == selected
            val shape = PrismDesign.cut(12.dp)
            Box(
                Modifier.heightIn(min = 40.dp).clip(shape)
                    .background(if (active) PrismAccent.gradient(false) else SolidColor(prismSearchSurface))
                    .border(1.dp, if (active) SolidColor(Color.Transparent) else SolidColor(prismSearchHairline), shape)
                    .clickable(role = Role.Tab, onClick = { onSelect(filter) })
                    .semantics { this.selected = active }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, color = if (active) Color.White else LevyraText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                )
            }
        }
    }
}
