package com.luc4n3x.levyra.ui.jam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@Composable
internal fun JamInvitationCode(code: String, modifier: Modifier = Modifier) {
    val look = LocalJamLook.current
    val scheme = MaterialTheme.colorScheme
    val accent = if (look.skinned) look.accent else scheme.primary
    val foreground = if (look.skinned) look.text else scheme.onSurface
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = if (look.skinned) look.rowShape else MaterialTheme.shapes.large,
        color = if (look.skinned) look.background else scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(LocalLevyraStrings.current.jamSessionCode, color = if (look.skinned) look.textSecondary else scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = code },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                code.split('-').forEach { group ->
                    Surface(color = accent.copy(alpha = 0.08f), shape = MaterialTheme.shapes.small) {
                        Text(group, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), color = foreground,
                            fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}
