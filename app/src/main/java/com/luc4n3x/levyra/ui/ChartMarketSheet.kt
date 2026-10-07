package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ChartMarket
import com.luc4n3x.levyra.domain.ChartMarketDirectory
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartMarketSheet(
    regions: List<ChartRegion>,
    selectedId: String,
    languageCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
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
    val suggested = remember(markets, selectedId, deviceRegionId) {
        ChartMarketDirectory.suggested(markets, selectedId, deviceRegionId)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(markets, query) { ChartMarketDirectory.filter(markets, query) }
    val browsing = query.isBlank()
    var committed by remember { mutableStateOf(false) }

    val choose: (String) -> Unit = choose@{ regionId ->
        if (committed) return@choose
        committed = true
        onSelect(regionId)
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (sheetState.isVisible) committed = false else onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LevyraPanel,
        contentColor = LevyraText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Text(
                text = strings.chartMarketSheetTitle,
                color = LevyraText,
                fontSize = 20.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(20.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics { heading() }
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = LevyraMuted) },
                trailingIcon = if (query.isEmpty()) {
                    null
                } else {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, strings.clear, tint = LevyraMuted)
                        }
                    }
                },
                placeholder = {
                    Text(
                        strings.chartMarketSearchHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = LevyraMuted
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LevyraText,
                    unfocusedTextColor = LevyraText,
                    focusedBorderColor = LevyraCyan.copy(alpha = 0.72f),
                    unfocusedBorderColor = LevyraAdaptiveHairline,
                    focusedContainerColor = LevyraPanelSoft.copy(alpha = 0.68f),
                    unfocusedContainerColor = LevyraPanelSoft.copy(alpha = 0.48f),
                    cursorColor = LevyraCyan
                )
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .selectableGroup(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp)
            ) {
                when {
                    browsing -> {
                        if (suggested.isNotEmpty()) {
                            item(key = "chart-market-suggested-label", contentType = "chart-market-label") {
                                ChartMarketSectionLabel(strings.chartMarketSuggested)
                            }
                            items(
                                items = suggested,
                                key = { "chart-market-suggested-${it.region.id}" },
                                contentType = { "chart-market-row" }
                            ) { market ->
                                ChartMarketRow(
                                    market = market,
                                    selected = market.region.id == selectedId,
                                    supporting = strings.chartMarketYourRegion.takeIf { market.region.id == deviceRegionId },
                                    onClick = { choose(market.region.id) }
                                )
                            }
                        }
                        item(key = "chart-market-all-label", contentType = "chart-market-label") {
                            ChartMarketSectionLabel(strings.chartMarketAllCountries)
                        }
                        items(
                            items = markets,
                            key = { "chart-market-all-${it.region.id}" },
                            contentType = { "chart-market-row" }
                        ) { market ->
                            ChartMarketRow(
                                market = market,
                                selected = market.region.id == selectedId,
                                supporting = null,
                                onClick = { choose(market.region.id) }
                            )
                        }
                    }
                    results.isEmpty() -> item(key = "chart-market-empty", contentType = "chart-market-empty") {
                        ChartMarketEmptyResult(strings.chartMarketNoResults(query.trim()))
                    }
                    else -> items(
                        items = results,
                        key = { "chart-market-result-${it.region.id}" },
                        contentType = { "chart-market-row" }
                    ) { market ->
                        ChartMarketRow(
                            market = market,
                            selected = market.region.id == selectedId,
                            supporting = null,
                            onClick = { choose(market.region.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartMarketSectionLabel(text: String) {
    Text(
        text = text,
        color = LevyraMuted,
        fontSize = 13.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 6.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ChartMarketRow(
    market: ChartMarket,
    selected: Boolean,
    supporting: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LevyraCyan.copy(alpha = 0.12f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LevyraAdaptiveChip)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(market.region.emoji, fontSize = 19.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = market.displayName,
                color = LevyraText,
                fontSize = 16.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(16.sp),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ChartMarketEmptyResult(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = LevyraMuted,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = message,
            color = LevyraMuted,
            fontSize = 14.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(14.sp),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
