package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun DefaultHomeHeaderActions(loading: Boolean, onSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val navigation = LocalMusicExperiences.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HeaderToolbarButton(Icons.Rounded.Replay, "Levyra Rewind", navigation.rewind, Modifier.testTag("open-rewind"))
        HeaderToolbarButton(Icons.Rounded.Settings, strings.settings, onSettings, loading = loading)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeaderToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    val light = LevyraIsLight
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            pressedShape = MaterialShapes.Cookie9Sided.toShape()
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (light) Color.White.copy(alpha = 0.86f) else Color.Black.copy(alpha = 0.42f),
            contentColor = LevyraText.copy(alpha = 0.94f)
        ),
        modifier = modifier.size(HeaderToolbarButtonSize)
    ) {
        if (loading) {
            LevyraLoadingIndicator(modifier = Modifier.size(24.dp).semantics { contentDescription = label }, color = LevyraText)
        } else {
            Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp))
        }
    }
}

private val HeaderToolbarButtonSize = 46.dp
