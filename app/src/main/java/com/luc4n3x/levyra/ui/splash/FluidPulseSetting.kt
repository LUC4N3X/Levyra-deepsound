package com.luc4n3x.levyra.ui.splash

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.ui.SettingsToggle
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.VisualCraftText
import com.luc4n3x.levyra.ui.i18n.visualCraftCopy

internal fun LazyListScope.startupAnimationSettingItem(
    strings: LevyraStrings,
    interfaceSettings: LevyraInterfaceSettings,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit
) {
    item {
        val craft = strings.visualCraftCopy
        SettingsToggle(
            icon = Icons.Rounded.AutoAwesome,
            title = craft[VisualCraftText.StartupAnimation],
            subtitle = craft[VisualCraftText.StartupAnimationSubtitle],
            checked = interfaceSettings.startupAnimation,
            onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(startupAnimation = it)) }
        )
    }
}
