package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.RadioTabCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.i18n.radioTabCopy
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

internal val LevyraRadioReplacesSearch: Boolean
    get() = LevyraIsAtelier || levyraSearchOwnsBrowse(LevyraIsSpotify, LevyraIsAppleMusic, LevyraIsQobuz || LevyraIsYouTubeMusic)

internal fun levyraBarTabs(order: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> =
    if (radioReplacesSearch) order.filterNot { it == LevyraTab.Search || it == LevyraTab.Radio } + LevyraTab.Radio else order

internal fun navStudioRows(order: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> =
    if (radioReplacesSearch) order - LevyraTab.Search else order

internal fun navStudioOrder(full: List<LevyraTab>, rows: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> {
    val at = full.indexOf(LevyraTab.Search)
    if (!radioReplacesSearch || at < 0) return rows
    return rows.toMutableList().apply { add(at.coerceAtMost(size), LevyraTab.Search) }
}

@Composable
internal fun rememberLevyraBarEntries(order: List<LevyraTab>): List<LevyraTabEntry> {
    val all = rememberLevyraTabEntries()
    val radio = LevyraRadioReplacesSearch
    return remember(all, order, radio) {
        levyraBarTabs(order, radio).mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
}

internal fun radioTabEntry(code: String, nocturne: Boolean): LevyraTabEntry =
    LevyraTabEntry(LevyraTab.Radio, if (nocturne) Icons.Outlined.Radio else Icons.Rounded.Radio, Icons.Outlined.Radio, radioTabCopy(code).tab)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraTopBarSearchAction(onClick: () -> Unit) {
    val label = LocalLevyraStrings.current.search
    if (LevyraSkinManagesLayout) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, label, tint = LevyraText) }
    } else {
        HomeHeaderIconButton(icon = Icons.Rounded.Search, contentDescription = label, polygon = MaterialShapes.Cookie4Sided, onClick = onClick)
    }
}

@Composable
internal fun LevyraTopLevelSearchRow(
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onJam: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        content()
        if (onJam != null) LevyraTopBarJamAction(onJam)
        if (onSearch != null) LevyraTopBarSearchAction(onSearch)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraTopBarJamAction(onClick: () -> Unit) {
    val label = LocalLevyraStrings.current.jamTitle
    if (LevyraSkinManagesLayout) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Groups, label, tint = LevyraText) }
    } else {
        HomeHeaderIconButton(icon = Icons.Rounded.Groups, contentDescription = label, polygon = MaterialShapes.Cookie4Sided, onClick = onClick)
    }
}

@Composable
internal fun RadioTabScreen(viewModel: LevyraViewModel, state: LevyraUiState, backEnabled: Boolean) {
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { radioTabCopy(code) }
    if (com.luc4n3x.levyra.ui.theme.LevyraActivePalette.id == com.luc4n3x.levyra.ui.theme.PrismPaletteId) {
        PrismRadioScreen(
            languageCode = state.languageCode,
            currentStationId = state.currentTrack?.takeIf { it.isLiveRadio() }?.id?.removePrefix("live-radio:"),
            isPlaying = state.isPlaying,
            title = copy.tab,
            onPlay = viewModel::playLiveRadio,
            onSearch = { viewModel.selectTab(LevyraTab.Search) }
        )
        return
    }
    if (LevyraIsAtelier) {
        AtelierRadioScreen(
            languageCode = state.languageCode,
            currentStationId = state.currentTrack?.takeIf { it.isLiveRadio() }?.id?.removePrefix("live-radio:"),
            isPlaying = state.isPlaying,
            title = copy.tab,
            onPlay = viewModel::playLiveRadio,
            onSearch = { viewModel.selectTab(LevyraTab.Search) }
        )
        return
    }
    Box(Modifier.fillMaxSize()) {
        LiveRadioScreen(
            languageCode = state.languageCode,
            currentStationId = state.currentTrack?.takeIf { it.isLiveRadio() }?.id?.removePrefix("live-radio:"),
            isPlaying = state.isPlaying,
            backEnabled = false,
            onBack = {},
            onPlay = viewModel::playLiveRadio,
            tabTitle = copy.tab,
            onSearch = { viewModel.selectTab(LevyraTab.Search) }
        )
    }
}
