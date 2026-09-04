package com.luc4n3x.levyra.feature.ambient

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.i18n.LevyraStrings

class LevyraAmbientTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        renderTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        renderTile()
    }

    override fun onClick() {
        super.onClick()
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                REQUEST_TILE_LAUNCH,
                ambientIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        )
    }

    private fun ambientIntent(): Intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, LevyraLaunchActions.SHORTCUT_AMBIENT)
    }

    private fun renderTile() {
        val tile = qsTile ?: return
        val strings = LevyraStrings.forCode(LevyraPreferences(this).snapshot().languageCode)
        tile.label = strings.ambientMode
        tile.state = Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = strings.ambientModeSubtitle
        }
        tile.updateTile()
    }

    private companion object {
        const val REQUEST_TILE_LAUNCH = 5421
    }
}
