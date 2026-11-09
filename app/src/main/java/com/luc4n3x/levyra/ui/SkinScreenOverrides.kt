package com.luc4n3x.levyra.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.HomeViewModel

internal fun skinOwnsCuratedShelf(): Boolean = LevyraIsTorque || LevyraIsIvory

internal fun skinOwnsDiscoveryEntry(): Boolean = LevyraIsTorque || LevyraIsIvory

internal fun LazyListScope.skinNewReleasesShelf(
    ready: Boolean,
    showNewReleases: Boolean,
    showAlbumsForYou: Boolean,
    viewModel: HomeViewModel,
    inset: Dp,
    albums: List<AlbumHit>,
    fallbackTitle: String
): Boolean {
    val torque = LevyraIsTorque
    if (!torque && !LevyraIsIvory) return false
    if (!ready || (!showNewReleases && !showAlbumsForYou)) return true
    val fallback = if (showAlbumsForYou) albums else emptyList()
    item(key = "skin-new-releases", contentType = "skin-new-releases") {
        if (torque) TorqueNewReleases(viewModel, inset, showNewReleases, fallback, fallbackTitle)
        else IvoryNewReleases(viewModel, inset, showNewReleases, fallback, fallbackTitle)
    }
    return true
}

@Composable
internal fun SkinCuratedShelf(viewModel: HomeViewModel, title: String, fans: List<StudioCollection>, inset: Dp, onOpen: (String) -> Unit) {
    val fallback: @Composable () -> Unit = { if (fans.isNotEmpty()) SkinCollectionsShelf(title, fans, inset, onOpen) }
    when {
        LevyraIsTorque -> TorqueCuratedPlaylists(viewModel, inset, fallback)
        LevyraIsIvory -> IvoryCuratedPlaylists(viewModel, inset, fallback)
        else -> fallback()
    }
}

@Composable
internal fun SkinSearchFilters(chips: List<Pair<SearchFilter, String>>, selected: SearchFilter, onSelect: (SearchFilter) -> Unit): Boolean {
    if (LevyraIsPrismOnly) {
        PrismSearchFilters(chips, selected, onSelect)
        return true
    }
    if (!LevyraIsTorque) return false
    Row(
        modifier = Modifier.recentSearchesBleed(RecentSearchGutter).horizontalScroll(rememberScrollState()).padding(horizontal = RecentSearchGutter),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (filter, label) -> TorqueSlantChip(label, filter == selected) { onSelect(filter) } }
    }
    return true
}

@Composable
internal fun TorqueExploreRow(
    row: ExploreRow,
    state: LevyraUiState,
    viewModel: ExploreViewModel,
    editorial: ExploreEditorialContent?,
    fresh: ExploreFreshSection,
    onShortcut: (ExploreShortcut) -> Unit,
    onSelectFreshScope: (ExploreFreshScope) -> Unit,
    onPlayTrack: (Track) -> Unit,
    trackActions: @Composable (Track) -> Unit
): Boolean {
    if (!LevyraIsTorque || editorial == null) return false
    val strings = LocalLevyraStrings.current
    when {
        row is ExploreRow.Header && row.anchor == ExploreAnchor.Fresh -> Box(Modifier.padding(top = 14.dp, end = TorqueGutter - 4.dp)) {
            SkinSectionTitle(exploreEditorialCopy(strings.code).releasesTitle, TorqueGutter) { TorqueShowAll { onShortcut(ExploreShortcut.NewReleases) } }
        }
        row == ExploreRow.FreshScopes -> Row(Modifier.padding(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            fresh.scopes.forEach { scope ->
                TorqueSlantChip(if (scope == ExploreFreshScope.Local) fresh.regionLabel else strings.freshScopeWorld, scope == fresh.feed.scope) { onSelectFreshScope(scope) }
            }
        }
        row == ExploreRow.FreshSpotlight -> TorqueReleaseShelf(
            releases = remember(fresh.feed) { exploreReleaseShelf(fresh.feed) },
            currentTrackId = state.currentTrack?.id,
            isPlaying = state.isPlaying,
            isResolving = state.isResolving,
            onPlay = onPlayTrack,
            onOpenAlbum = { track -> viewModel.openAlbum(trackAlbumHit(track)) },
            actions = trackActions
        )
        else -> return false
    }
    return true
}
