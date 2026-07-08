package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.architecture.mobius.LevyraInit
import com.luc4n3x.levyra.architecture.mobius.LevyraNext

object PlayerInit : LevyraInit<PlayerModel, PlayerEffect> {
    override fun init(model: PlayerModel): LevyraNext<PlayerModel, PlayerEffect> {
        val normalized = model.normalized()
        val track = normalized.currentTrack
        return if (track == null) {
            LevyraNext.next(normalized)
        } else {
            LevyraNext.dispatch(
                normalized,
                PlayerEffect.PersistSnapshot(track, normalized.safePositionMs, normalized.isPlaying),
                PlayerEffect.LoadLyrics(track)
            )
        }
    }
}
