package com.luc4n3x.levyra.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.luc4n3x.levyra.R
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.domain.ExploreZone

@Composable
internal fun DefaultHomeBrowseHeader(
    zones: List<ExploreZone>,
    chipState: LazyListState,
    loading: Boolean,
    onSelect: (ExploreZone) -> Unit,
    onSettings: () -> Unit
) {
    val logoLabel = stringResource(R.string.app_name)
    val logoSemantics = Modifier.semantics {
        contentDescription = logoLabel
        role = Role.Image
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 320.dp) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeLogoBadge(logoSemantics)
                    DefaultHomeHeaderActions(loading, onSettings)
                }
                val gutter = LevyraHomeDesign.HorizontalInset
                HomeGenreChips(
                    zones = zones,
                    contentPadding = PaddingValues(horizontal = gutter),
                    onSelect = onSelect,
                    modifier = Modifier.recentSearchesBleed(gutter),
                    compactPills = true,
                    state = chipState
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val gutter = LevyraHomeDesign.HorizontalInset
                Box(Modifier.weight(1f).bleedStart(gutter)) {
                    HomeGenreChips(
                        zones = zones,
                        contentPadding = PaddingValues(start = gutter + LevyraHomeDesign.SettingsControlHeight + 8.dp),
                        onSelect = onSelect,
                        compactPills = true,
                        state = chipState
                    )
                    Box(Modifier.align(Alignment.CenterStart).padding(start = gutter)) {
                        HomeLogoBadge(logoSemantics.pointerInput(Unit) { detectTapGestures(onTap = {}) })
                    }
                }
                DefaultHomeHeaderActions(loading, onSettings)
            }
        }
    }
}

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

private fun Modifier.bleedStart(gutter: Dp): Modifier = layout { measurable, constraints ->
    val gutterPx = gutter.roundToPx()
    val width = constraints.maxWidth + gutterPx
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(if (layoutDirection == LayoutDirection.Ltr) -gutterPx else 0, 0)
    }
}
