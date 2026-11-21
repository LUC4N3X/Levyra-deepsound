package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DefaultMoodsSheet(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val state by viewModel.browseState.collectAsStateWithLifecycle()
    LaunchedEffect(strings) { viewModel.ensureExplore(strings) }
    val browse = rememberLevyraBrowse(state, viewModel::ensureExploreCategoryArtwork)
    val podcastsArtwork = rememberSearchPodcastsArtwork(strings.code)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            levyraSearchBrowseItems(
                browse = browse,
                onOpenZone = { zone ->
                    onDismiss()
                    viewModel.openExploreZone(zone)
                },
                onOpenCategory = { category ->
                    onDismiss()
                    viewModel.openExploreCategoryPage(category)
                },
                podcastsTitle = podcastCopy(strings.code).title,
                podcastsArtwork = podcastsArtwork,
                onOpenPodcasts = {
                    onDismiss()
                    viewModel.openExplorePodcasts()
                }
            )
        }
    }
}
