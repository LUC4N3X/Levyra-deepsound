package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.i18n.LevyraPrismStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@Composable
private fun <T> PrismChoice(title: String, choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LevyraText, modifier = Modifier.semantics { heading() })
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected, onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(label, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
internal fun PrismSettingsPanel(settings: PrismSettings, onSettings: (PrismSettings) -> Unit) {
    val copy = LevyraPrismStrings.forCode(LocalLevyraStrings.current.code)
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrismChoice(copy.appearance, listOf(PrismAppearance.Night to copy.night, PrismAppearance.Light to copy.light, PrismAppearance.Automatic to copy.automatic), settings.appearance) { onSettings(settings.copy(appearance = it)) }
        PrismChoice(copy.player, PrismPlayerLayout.entries.map { it to "Prism ${it.name}" }, settings.playerLayout) { onSettings(settings.copy(playerLayout = it)) }
        PrismChoice(copy.translucency, listOf(PrismTranslucency.Automatic to copy.automatic, PrismTranslucency.Reduced to copy.reduced), settings.translucency) { onSettings(settings.copy(translucency = it)) }
    }
}

@Composable
internal fun IvorySettingsPanel(settings: IvorySettings, onSettings: (IvorySettings) -> Unit) {
    val copy = LevyraPrismStrings.forCode(LocalLevyraStrings.current.code)
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrismChoice(copy.appearance, listOf(IvoryAppearance.Light to copy.light, IvoryAppearance.Dark to copy.dark, IvoryAppearance.Automatic to copy.automatic), settings.appearance) { onSettings(settings.copy(appearance = it)) }
    }
}

@Composable
internal fun TorqueSettingsPanel(settings: TorqueSettings, onSettings: (TorqueSettings) -> Unit) {
    val copy = LevyraPrismStrings.forCode(LocalLevyraStrings.current.code)
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrismChoice(copy.accent, listOf(TorqueAccent.Cover to copy.cover, TorqueAccent.Red to copy.red), settings.accent) { onSettings(settings.copy(accent = it)) }
    }
}
