package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.DownloadQualityProbeSnapshot
import com.luc4n3x.levyra.player.offline.reconcileDownloadQualitySelection

data class DownloadQualityPickerState(
    val track: Track,
    val snapshot: DownloadQualityProbeSnapshot = DownloadQualityProbeSnapshot(pending = 1),
    val selected: DownloadQualityChoice? = null,
    val userSelected: Boolean = false
) {
    fun withSnapshot(next: DownloadQualityProbeSnapshot): DownloadQualityPickerState = copy(
        snapshot = next,
        selected = reconcileDownloadQualitySelection(next.options, selected, userSelected)
    )

    fun select(choice: DownloadQualityChoice): DownloadQualityPickerState =
        if (snapshot.options.any { it.choice == choice }) copy(selected = choice, userSelected = true) else this
}

internal data class CachedDownloadQualityProbe(
    val downloadKey: String,
    val snapshot: DownloadQualityProbeSnapshot,
    val probedAtMs: Long
)
