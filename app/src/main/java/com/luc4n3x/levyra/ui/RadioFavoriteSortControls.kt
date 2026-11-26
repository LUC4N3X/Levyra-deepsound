package com.luc4n3x.levyra.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.feature.radio.RadioFavoriteFacts
import com.luc4n3x.levyra.feature.radio.RadioFavoriteGroup
import com.luc4n3x.levyra.feature.radio.RadioFavoriteGroupLabels
import com.luc4n3x.levyra.feature.radio.RadioFavoriteSort
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.feature.radio.radioFavoriteGroups
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.radioFavoriteSortCopy
import java.util.Locale

@Composable
internal fun rememberRadioFavoriteGroups(
    favorites: List<RadioStation>,
    sort: RadioFavoriteSort,
    languageCode: String
): List<RadioFavoriteGroup<RadioStation>> = remember(favorites, sort, languageCode) {
    val copy = radioFavoriteSortCopy(languageCode)
    val radioStrings = LevyraLiveRadioCatalog.forCode(languageCode)
    radioFavoriteGroups(
        items = favorites,
        sort = sort,
        locale = Locale.forLanguageTag(languageCode),
        labels = RadioFavoriteGroupLabels(
            countryName = { localizedCountryName(it, languageCode) },
            genreName = radioStrings::category,
            unknownCountry = copy.unknownCountry,
            otherGenre = copy.otherGenre
        ),
        facts = ::radioFavoriteFacts
    )
}

internal fun radioFavoriteFacts(station: RadioStation) =
    RadioFavoriteFacts(station.name, station.countryCode, station.country, station.tags, station.addedAt)

@Composable
internal fun RadioFavoriteSortButton(
    sort: RadioFavoriteSort,
    languageCode: String,
    accent: Color,
    content: Color,
    container: Color,
    onSort: (RadioFavoriteSort) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(languageCode) { radioFavoriteSortCopy(languageCode) }
    var expanded by remember { mutableStateOf(false) }
    val label = copy.label(sort)
    Box(modifier) {
        Surface(
            color = container,
            shape = CircleShape,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = 36.dp)
                .widthIn(max = 196.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = copy.sortBy, role = Role.Button) { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = copy.sortBy, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    text = label,
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = copy.sortBy,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() }
            )
            RadioFavoriteSort.entries.forEach { option ->
                if (option == RadioFavoriteSort.NameAscending || option == RadioFavoriteSort.Country) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                val selected = option == sort
                DropdownMenuItem(
                    text = {
                        Text(
                            copy.label(option),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    trailingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        expanded = false
                        if (!selected) onSort(option)
                    },
                    modifier = Modifier.semantics { this.selected = selected }
                )
            }
        }
    }
}

@Composable
internal fun RadioFavoriteGroupHeader(title: String, color: Color, inset: Dp, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth().padding(start = inset, end = inset, top = 14.dp, bottom = 8.dp).semantics { heading() }
    )
}

private val RadioFavoriteSort.icon: ImageVector
    get() = when (this) {
        RadioFavoriteSort.Newest -> Icons.Rounded.Schedule
        RadioFavoriteSort.Oldest -> Icons.Rounded.History
        RadioFavoriteSort.NameAscending, RadioFavoriteSort.NameDescending -> Icons.Rounded.SortByAlpha
        RadioFavoriteSort.Country -> Icons.Rounded.Public
        RadioFavoriteSort.Genre -> Icons.Rounded.Category
    }
