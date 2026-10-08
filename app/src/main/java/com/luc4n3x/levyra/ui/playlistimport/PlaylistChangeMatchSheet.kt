package com.luc4n3x.levyra.ui.playlistimport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistChangeMatchSheet(
    track: Track,
    onSearch: (String, CandidateOrigin, (List<Pair<MatchEvaluation, Track>>) -> Unit) -> Unit,
    onReplace: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { playlistImportHubCopy(strings.code) }
    var query by rememberSaveable(track.id, track.playlistEntryId) { mutableStateOf("${track.title} ${track.artist}".trim()) }
    var origin by rememberSaveable(track.id, track.playlistEntryId) { mutableStateOf(CandidateOrigin.ONLINE) }
    var loading by remember(track.id, track.playlistEntryId) { mutableStateOf(true) }
    var results by remember(track.id, track.playlistEntryId) { mutableStateOf<List<Pair<MatchEvaluation, Track>>>(emptyList()) }
    var requestId by remember(track.id, track.playlistEntryId) { mutableStateOf(0) }
    fun search() {
        val id = ++requestId
        loading = true
        onSearch(query, origin) {
            if (id == requestId) {
                results = it
                loading = false
            }
        }
    }
    LaunchedEffect(track.id, track.playlistEntryId, origin) { search() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = LevyraPanel
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(copy["changeMatch"], color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${track.title} — ${track.artist}", color = LevyraMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(CandidateOrigin.ONLINE to copy["searchOnline"], CandidateOrigin.LOCAL to copy["searchLocal"]).forEach { (value, label) ->
                            FilterChip(
                                selected = origin == value,
                                onClick = { origin = value },
                                label = { Text(label) },
                                shapes = FilterChipDefaults.shapes(
                                    shape = MaterialTheme.shapes.small,
                                    selectedShape = MaterialTheme.shapes.extraLarge,
                                    pressedShape = MaterialTheme.shapes.medium
                                ),
                                colors = FilterChipDefaults.tonalFilterChipColors(),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ImportRowShape,
                        placeholder = { Text(copy["searchPlaceholder"]) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        trailingIcon = {
                            LevyraExpressiveIconButton(onClick = ::search) {
                                Icon(Icons.Rounded.Search, contentDescription = copy["searchManually"])
                            }
                        }
                    )
                }
            }
            when {
                loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.size(28.dp))
                    }
                }
                results.isEmpty() -> item(key = "empty") { Text(copy["noResults"], color = LevyraMuted, fontSize = 13.sp) }
                else -> items(results, key = { it.second.id }) { (evaluation, candidate) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ImportRowShape)
                            .clickable { onReplace(candidate.copy(playlistEntryId = track.playlistEntryId)) }
                            .heightIn(min = 64.dp)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArtworkBox(candidate.thumbnailUrl, Modifier.size(48.dp).clip(ImportRowShape))
                        Column(Modifier.weight(1f)) {
                            Text(candidate.title, color = LevyraText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(candidate.artist, candidate.album, durationLabel(candidate.durationMs)).filter(String::isNotBlank).joinToString(" · "),
                                color = LevyraMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                confidenceLabel(evaluation.confidence, copy),
                                color = if (evaluation.confidence.autoAccepted) LevyraCyan else LevyraOrange,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}