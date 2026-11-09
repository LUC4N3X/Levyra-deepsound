package com.luc4n3x.levyra.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ChartMarket
import com.luc4n3x.levyra.domain.ChartMarketDirectory
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonoChartMarketSheet(
    regions: List<ChartRegion>,
    selectedId: String,
    languageCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String?,
    automaticLabel: String?
) {
    val strings = LocalLevyraStrings.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()
    val markets = remember(regions, languageCode) { ChartMarketDirectory.markets(regions, languageCode) }
    val deviceRegionId = remember { ChartsCatalog.supportedRegion(Locale.getDefault().country)?.id }
    val suggested = remember(markets, selectedId, deviceRegionId) { ChartMarketDirectory.suggested(markets, selectedId, deviceRegionId) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(markets, query) { ChartMarketDirectory.filter(markets, query) }
    var committed by remember { mutableStateOf(false) }
    val choose: (String) -> Unit = choose@{ regionId ->
        if (committed) return@choose
        committed = true
        onSelect(regionId)
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (sheetState.isVisible) committed = false else onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MonoDesign.surface,
        contentColor = MonoDesign.text,
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp).height(4.dp).clip(MonoDesign.pillShape).background(MonoDesign.hairlineStrong)) }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            MonoOverline(strings.chartMarketAllCountries, Modifier.padding(horizontal = MonoDesign.gutter, vertical = 4.dp))
            Text(
                title ?: strings.chartMarketSheetTitle, style = MaterialTheme.typography.headlineSmall, color = MonoDesign.text,
                modifier = Modifier.padding(horizontal = MonoDesign.gutter).semantics { heading() }
            )
            MonoMarketSearch(query, strings.chartMarketSearchHint, strings.clear, { query = it }) { focusManager.clearFocus() }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)
            ) {
                if (query.isBlank()) {
                    if (automaticLabel != null) {
                        item(key = "auto") {
                            MonoMarketRow(ChartMarket(ChartRegion("", automaticLabel, "🌐", ""), automaticLabel, emptyList()), selectedId.isEmpty(), null) { choose("") }
                        }
                    }
                    if (suggested.isNotEmpty()) {
                        item(key = "suggested-label") { MonoMarketLabel(strings.chartMarketSuggested) }
                        items(suggested, key = { "s-${it.region.id}" }) { market ->
                            MonoMarketRow(market, market.region.id == selectedId, strings.chartMarketYourRegion.takeIf { market.region.id == deviceRegionId }) { choose(market.region.id) }
                        }
                    }
                    item(key = "all-label") { MonoMarketLabel(strings.chartMarketAllCountries) }
                    items(markets, key = { "a-${it.region.id}" }) { market ->
                        MonoMarketRow(market, market.region.id == selectedId, null) { choose(market.region.id) }
                    }
                } else if (results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            strings.chartMarketNoResults(query.trim()), style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 32.dp)
                        )
                    }
                } else {
                    items(results, key = { "r-${it.region.id}" }) { market ->
                        MonoMarketRow(market, market.region.id == selectedId, null) { choose(market.region.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonoMarketSearch(query: String, hint: String, clearLabel: String, onQuery: (String) -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 14.dp)
            .heightIn(min = 48.dp).clip(MonoDesign.pillShape).background(MonoDesign.raised)
            .border(1.dp, MonoDesign.hairline, MonoDesign.pillShape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(hint, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
            }
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MonoDesign.text),
                cursorBrush = SolidColor(MonoDesign.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDone() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { onQuery("") },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, clearLabel, tint = MonoDesign.textSecondary, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun MonoMarketLabel(text: String) {
    MonoOverline(text, Modifier.padding(start = 12.dp, top = 18.dp, bottom = 8.dp).semantics { heading() })
}

@Composable
private fun MonoMarketRow(market: ChartMarket, selected: Boolean, supporting: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .clip(MonoDesign.cardShape)
            .background(if (selected) MonoDesign.accent.copy(alpha = 0.10f) else MonoDesign.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(28.dp).clip(MonoDesign.pillShape).background(if (selected) MonoDesign.accent else MonoDesign.surface))
        Box(
            Modifier.padding(start = 11.dp).size(38.dp).clip(CircleShape).background(MonoDesign.raised).border(1.dp, MonoDesign.hairline, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(market.region.emoji, fontSize = 19.sp, modifier = Modifier.clearAndSetSemantics { }) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                market.displayName, style = MaterialTheme.typography.titleMedium, color = if (selected) MonoDesign.accent else MonoDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
            }
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = MonoDesign.accent, modifier = Modifier.size(20.dp))
    }
}
