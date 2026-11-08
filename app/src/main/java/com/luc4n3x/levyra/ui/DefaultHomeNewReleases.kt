package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.viewmodel.HomeViewModel

internal data class DefaultHomeAlbumShelf(val albums: List<AlbumHit>, val title: String)

@Composable
internal fun rememberDefaultHomeAlbumShelf(
    viewModel: HomeViewModel,
    enabled: Boolean,
    selectedChartId: String?,
    languageCode: String,
    personalAlbums: List<AlbumHit>,
    personalTitle: String
): DefaultHomeAlbumShelf {
    if (!enabled || !LevyraIsDefaultSkin || LevyraIsTorque || LevyraIsIvory) {
        return remember(personalAlbums, personalTitle) { DefaultHomeAlbumShelf(personalAlbums, personalTitle) }
    }
    val newThisWeek by viewModel.appleNewThisWeek.collectAsStateWithLifecycle()
    LifecycleResumeEffect(selectedChartId, languageCode) {
        viewModel.ensureAppleNewThisWeek()
        onPauseOrDispose { }
    }
    return remember(newThisWeek.albums, personalAlbums, personalTitle, languageCode) {
        val releases = newThisWeek.albums.distinctBy(::atelierAlbumKey)
        if (releases.isEmpty()) DefaultHomeAlbumShelf(personalAlbums, personalTitle)
        else DefaultHomeAlbumShelf(releases, LevyraMonoHomeStrings.forCode(languageCode).newAlbums)
    }
}
