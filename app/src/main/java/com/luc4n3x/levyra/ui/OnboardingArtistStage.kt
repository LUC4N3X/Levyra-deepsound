package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.OnboardingArtist
import com.luc4n3x.levyra.domain.OnboardingArtistSection
import com.luc4n3x.levyra.domain.OnboardingArtists
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingArtistStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun SetupArtistStage(
    languageCode: String,
    artwork: Map<String, List<String>>,
    selected: Set<String>,
    accent: Color,
    ink: Color,
    muted: Color,
    surface: Color,
    onAccent: Color,
    onRequestArtwork: (String, Set<String>) -> Unit,
    onToggle: (String) -> Unit
) {
    val catalog = remember(languageCode) { OnboardingArtists.sections(languageCode) }
    val copy = LevyraOnboardingArtistStrings.forCode(languageCode)
    val strings = LevyraStrings.forCode(languageCode)
    val sections = remember(catalog, selected, strings.artists) {
        val knownIds = catalog.flatMap { it.artists }.map { it.id }.toSet()
        val otherSelections = OnboardingArtists.selectedNames(selected).map(::OnboardingArtist).filter { it.id !in knownIds }
        if (otherSelections.isEmpty()) catalog
        else listOf(OnboardingArtistSection("selected-artists", strings.artists, otherSelections)) + catalog
    }
    var query by rememberSaveable(languageCode) { mutableStateOf("") }
    var expanded by rememberSaveable(languageCode) { mutableStateOf(emptyList<String>()) }
    val visibleSections = remember(sections, query, expanded) {
        val needle = artistSearchKey(query.trim())
        sections.map { section ->
            section.copy(artists = when {
                needle.isNotEmpty() -> section.artists.filter { artistSearchKey(it.name).contains(needle) }
                section.id in expanded -> section.artists
                else -> section.artists.take(OnboardingArtists.InitialSectionSize)
            })
        }.filter { it.artists.isNotEmpty() }
    }
    val visibleIds = remember(visibleSections) { visibleSections.flatMap { it.artists }.map { it.id }.toSet() }
    val gridState = rememberLazyGridState()
    LaunchedEffect(languageCode, visibleIds) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.filter { it in visibleIds }.toSet()
        }.distinctUntilChanged().collect { ids -> onRequestArtwork(languageCode, ids) }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "artist-intro", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(copy.title, color = ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Text(copy.body, color = muted, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { index ->
                            Box(Modifier.size(8.dp).background(if (selected.size > index) accent else muted.copy(alpha = 0.25f), CircleShape))
                        }
                    }
                    Text(
                        "${selected.size} · ${strings.artists}", color = if (selected.size >= 3) accent else muted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(strings.search) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = strings.clear) } }
                    } else null,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ink, unfocusedTextColor = ink, cursorColor = accent,
                        focusedBorderColor = accent, unfocusedBorderColor = muted.copy(alpha = 0.35f),
                        focusedLabelColor = accent, unfocusedLabelColor = muted,
                        focusedLeadingIconColor = muted, unfocusedLeadingIconColor = muted,
                        focusedTrailingIconColor = muted, unfocusedTrailingIconColor = muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (visibleIds.any { artwork[it]?.isEmpty() == true }) {
                    TextButton(onClick = { onRequestArtwork(languageCode, visibleIds) }) {
                        Text(strings.updateRetry, color = accent)
                    }
                }
            }
        }
        if (visibleSections.isEmpty()) {
            item(key = "artist-empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(copy.empty, color = muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp))
            }
        }
        visibleSections.forEach { section ->
            item(key = "section-${section.id}", span = { GridItemSpan(maxLineSpan) }) {
                Text(section.label, color = ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp).semantics { heading() })
            }
            items(section.artists, key = { it.id }) { artist ->
                SetupArtistPortrait(artist, artwork[artist.id]?.firstOrNull(), artwork[artist.id]?.isEmpty() == true,
                    artist.id in selected, accent, ink, surface, onAccent) {
                    onToggle(artist.id)
                }
            }
            if (query.isBlank() && section.id !in expanded && sections.first { it.id == section.id }.artists.size > section.artists.size) {
                item(key = "more-${section.id}", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { expanded = expanded + section.id }) {
                            Text("${strings.more} · ${section.label}", color = accent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupArtistPortrait(
    artist: OnboardingArtist,
    portrait: String?,
    portraitFailed: Boolean,
    selected: Boolean,
    accent: Color,
    ink: Color,
    surface: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(if (selected) 0.92f else 1f,
        if (animated) spring(dampingRatio = 0.7f, stiffness = 400f) else snap(), label = "artist-selection-scale")
    val outline by animateColorAsState(if (selected) accent else Color.Transparent,
        if (animated) spring() else snap(), label = "artist-selection-outline")
    Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).border(3.dp, outline, CircleShape).padding(6.dp)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape).background(surface),
                contentAlignment = Alignment.Center
            ) {
                if (portrait != null) AsyncImage(model = portrait, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else if (portraitFailed) Icon(Icons.Rounded.Person, contentDescription = null, tint = ink.copy(alpha = 0.65f), modifier = Modifier.size(40.dp))
                else CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent, strokeWidth = 2.dp)
            }
            if (selected) Box(
                Modifier.align(Alignment.BottomEnd).size(28.dp).background(accent, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(18.dp))
            }
        }
        Text(artist.name, color = if (selected) accent else ink, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp))
    }
}

private fun artistSearchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }.lowercase(Locale.ROOT)
