package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.i18n.LevyraPrismStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PrismChoice(title: String, choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LevyraText, modifier = Modifier.semantics { heading() })
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { (value, label) ->
                FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

@Composable
internal fun PrismSettingsPanel(settings: PrismSettings, onSettings: (PrismSettings) -> Unit) {
    val copy = LevyraPrismStrings.forCode(LocalLevyraStrings.current.code)
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Levyra Prism", style = MaterialTheme.typography.headlineSmall, color = LevyraText)
        PrismChoice(copy.appearance, listOf(PrismAppearance.Night to copy.night, PrismAppearance.Light to copy.light, PrismAppearance.Automatic to copy.automatic), settings.appearance) { onSettings(settings.copy(appearance = it)) }
        PrismChoice(copy.colors, listOf(PrismColors.Adaptive to copy.adaptive, PrismColors.Ultraviolet to "Ultraviolet", PrismColors.Glacier to "Glacier"), settings.colors) { onSettings(settings.copy(colors = it)) }
        PrismChoice(copy.player, PrismPlayerLayout.entries.map { it to "Prism ${it.name}" }, settings.playerLayout) { onSettings(settings.copy(playerLayout = it)) }
        PrismChoice(copy.translucency, listOf(PrismTranslucency.Automatic to copy.automatic, PrismTranslucency.Reduced to copy.reduced), settings.translucency) { onSettings(settings.copy(translucency = it)) }
    }
}
