package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.catalog.LocaleCatalog
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.model.AppLanguage

@Composable
fun LanguagePicker(
    selected: AppLanguage,
    label: String,
    contentDescription: String,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        PickerSurface(
            countryCode = selected.defaultCountry,
            label = label,
            value = selected.nativeName,
            contentDescription = contentDescription,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(330.dp)
                .heightIn(max = 520.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            LocaleCatalog.languages.forEach { language ->
                val active = language == selected
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CountryFlag(
                                countryCode = language.defaultCountry,
                                modifier = Modifier.width(38.dp).height(25.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language.nativeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (language.nativeName != language.englishName) {
                                    Text(
                                        text = language.englishName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    leadingIcon = if (active) activeIndicator() else null,
                    onClick = {
                        expanded = false
                        if (!active) onSelected(language)
                    }
                )
            }
        }
    }
}

@Composable
fun CountryPicker(
    selectedCode: String,
    label: String,
    contentDescription: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = LocaleCatalog.country(selectedCode)
    Box(modifier = modifier) {
        PickerSurface(
            countryCode = selected.code,
            label = label,
            value = selected.nativeName,
            contentDescription = contentDescription,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(330.dp)
                .heightIn(max = 520.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            LocaleCatalog.countries.forEach { country ->
                val active = country.code.equals(selected.code, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CountryFlag(
                                countryCode = country.code,
                                modifier = Modifier.width(38.dp).height(25.dp)
                            )
                            Text(
                                text = country.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = country.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = if (active) activeIndicator() else null,
                    onClick = {
                        expanded = false
                        if (!active) onSelected(country.code)
                    }
                )
            }
        }
    }
}

@Composable
private fun PickerSurface(
    countryCode: String,
    label: String,
    value: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(270.dp)
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            CountryFlag(
                countryCode = countryCode,
                modifier = Modifier.width(40.dp).height(27.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = LevyraIcons.ChevronDown,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

private fun activeIndicator(): @Composable (() -> Unit) = {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
