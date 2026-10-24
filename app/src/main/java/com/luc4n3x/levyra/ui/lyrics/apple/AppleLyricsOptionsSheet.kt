package com.luc4n3x.levyra.ui.lyrics.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppleSheetSurface = Color(0xFF1C1C1E)
private val AppleSheetGroup = Color(0xFF2C2C2E)
private val AppleSheetSeparator = Color(0xFF38383A)
private val AppleSheetLabel = Color.White
private val AppleSheetSecondary = Color(0x99EBEBF5)
private val AppleSheetAccent = Color(0xFFFC3C44)

@Immutable
internal class AppleLyricsToggle(val label: String, val checked: Boolean, val onChange: (Boolean) -> Unit)

@Immutable
internal class AppleLyricsChoice(val label: String, val selected: Boolean, val onSelect: () -> Unit)

@Immutable
internal class AppleLyricsAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleLyricsOptionsSheet(
    title: String,
    subtitle: String?,
    styleTitle: String,
    styles: List<AppleLyricsChoice>,
    toggles: List<AppleLyricsToggle>,
    actions: List<AppleLyricsAction>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppleSheetSurface,
        contentColor = AppleSheetLabel,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        val family = androidx.compose.runtime.remember { com.luc4n3x.levyra.ui.theme.levyraFontFamilyFor(com.luc4n3x.levyra.domain.LevyraFontPreset.Inter) }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily = family)
        ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 16.dp).padding(bottom = 16.dp)
        ) {
            Text(title, color = AppleSheetLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = AppleSheetSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            Spacer(Modifier.size(16.dp))
            AppleSheetCaption(styleTitle)
            AppleSheetGroup {
                styles.forEachIndexed { index, choice ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = choice.selected, role = Role.RadioButton, onClick = choice.onSelect)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(choice.label, color = AppleSheetLabel, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (choice.selected) Icon(Icons.Rounded.Check, null, tint = AppleSheetAccent, modifier = Modifier.size(22.dp))
                    }
                    if (index < styles.lastIndex) AppleSheetDivider()
                }
            }
            if (toggles.isNotEmpty()) {
                Spacer(Modifier.size(20.dp))
                AppleSheetGroup {
                    toggles.forEachIndexed { index, toggle ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .toggleable(value = toggle.checked, role = Role.Switch, onValueChange = toggle.onChange)
                                .padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(toggle.label, color = AppleSheetLabel, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Switch(
                                checked = toggle.checked, onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF30D158), checkedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0x52787880), uncheckedThumbColor = Color.White,
                                    uncheckedBorderColor = Color.Transparent, checkedBorderColor = Color.Transparent
                                )
                            )
                        }
                        if (index < toggles.lastIndex) AppleSheetDivider()
                    }
                }
            }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.size(20.dp))
                AppleSheetGroup {
                    actions.forEachIndexed { index, action ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = action.onClick)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(action.label, color = AppleSheetLabel, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(action.icon, null, tint = AppleSheetLabel, modifier = Modifier.size(22.dp))
                        }
                        if (index < actions.lastIndex) AppleSheetDivider()
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AppleSheetCaption(text: String) {
    Text(
        text.uppercase(), color = AppleSheetSecondary, fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun AppleSheetGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppleSheetGroup)) { content() }
}

@Composable
private fun AppleSheetDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = AppleSheetSeparator)
}
