package com.luc4n3x.levyra.ui.playlistimport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportEntry
import com.luc4n3x.levyra.nexus.playlistimport.ImportEntryStatus
import com.luc4n3x.levyra.nexus.playlistimport.ImportFlag
import com.luc4n3x.levyra.nexus.playlistimport.ImportReviewFilter
import com.luc4n3x.levyra.nexus.playlistimport.MatchConfidence
import com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation
import com.luc4n3x.levyra.nexus.playlistimport.MatchSignal
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportReview
import com.luc4n3x.levyra.ui.i18n.PlaylistImportHubCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.PlaylistImportController
import com.luc4n3x.levyra.viewmodel.PlaylistImportStep
import com.luc4n3x.levyra.viewmodel.PlaylistImportUiState
import kotlin.math.abs

private const val WIDE_LAYOUT_DP = 840

@Composable
internal fun PlaylistImportReviewScreen(
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE_LAYOUT_DP.dp
        val focused = state.focusedPosition?.let { state.entries.getOrNull(it) }
        Row(modifier = Modifier.fillMaxSize()) {
            ReviewList(
                state = state,
                copy = copy,
                controller = controller,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            if (wide && focused != null) {
                Surface(
                    color = LevyraPanel,
                    modifier = Modifier.width(420.dp).fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                ) {
                    TrackReviewContent(focused, state, copy, controller, Modifier.fillMaxSize())
                }
            }
        }
        if (!wide && focused != null) TrackReviewSheet(focused, state, copy, controller)
    }
}

@Composable
private fun ReviewList(
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController,
    modifier: Modifier
) {
    val counts = state.counts
    val visible = remember(state.entries, state.filter) {
        state.entries.indices.filter { PlaylistImportReview.matches(state.entries[it], state.filter) }
    }
    val saving = state.step == PlaylistImportStep.SAVING
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "top") { ImportTopBar(copy["reviewTitle"], controller::close, copy["back"]) }
            item(key = "hero") {
                state.descriptor?.let { descriptor ->
                    PlaylistImportHero(descriptor, state.entries, counts.total, copy) {
                        Text(
                            copy.format("matchPercent", "percent" to counts.matchPercent),
                            color = LevyraText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item(key = "name") {
                OutlinedTextField(
                    value = state.playlistName,
                    onValueChange = controller::setPlaylistName,
                    label = { Text(copy["playlistName"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = ImportRowShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LevyraText,
                        unfocusedTextColor = LevyraText,
                        focusedBorderColor = LevyraCyan.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        cursorColor = LevyraCyan
                    )
                )
            }
            item(key = "filters") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ImportReviewFilter.ALL to "${copy["filterAll"]} ${counts.total}",
                        ImportReviewFilter.MATCHED to "${copy["filterMatched"]} ${counts.matched}",
                        ImportReviewFilter.REVIEW to "${copy["filterReview"]} ${counts.review}",
                        ImportReviewFilter.MISSING to "${copy["filterMissing"]} ${counts.missing + counts.skipped}",
                        ImportReviewFilter.DUPLICATES to "${copy["filterRepeats"]} ${counts.duplicates}"
                    ).forEach { (filter, label) ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { controller.setFilter(filter) },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LevyraCyan.copy(alpha = 0.18f),
                                selectedLabelColor = LevyraText,
                                labelColor = LevyraMuted
                            )
                        )
                    }
                }
            }
            if (state.filter == ImportReviewFilter.REVIEW && counts.review > 0) {
                item(key = "accept-all") {
                    TextButton(onClick = controller::acceptAllSuggestions, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy.format("acceptSuggestions", "count" to counts.review), color = LevyraCyan)
                    }
                }
            }
            items(visible, key = { state.entries[it].identity.position }) { position ->
                ReviewRow(state.entries[position], copy) { controller.openEntry(position) }
            }
        }
        ReviewBottomBar(
            ready = counts.ready,
            attention = counts.review + counts.missing,
            merged = counts.mergedRepeats,
            saving = saving,
            failure = state.failure,
            copy = copy,
            onConfirm = controller::confirm,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReviewBottomBar(
    ready: Int,
    attention: Int,
    merged: Int,
    saving: Boolean,
    failure: com.luc4n3x.levyra.domain.PlaylistImportFailureKind?,
    copy: PlaylistImportHubCopy,
    onConfirm: () -> Unit,
    modifier: Modifier
) {
    val strings = com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current
    Surface(color = LevyraInk.copy(alpha = 0.97f), modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Text(
                copy.format("readyAttention", "ready" to ready, "attention" to attention),
                color = LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (merged > 0) Text(copy.format("mergedNote", "count" to merged), color = LevyraMuted, fontSize = 12.sp)
            if (failure != null) {
                Text(issueMessage(failure, null, copy, strings.code), color = LevyraPink, fontSize = 12.sp)
            }
            Button(
                onClick = onConfirm,
                enabled = ready > 0 && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                shape = ImportShape
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LevyraOnAccent, strokeWidth = 2.dp)
                } else {
                    Text(copy.format("importAction", "count" to ready), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private data class StatusVisual(val icon: ImageVector, val tint: Color, val labelKey: String)

private fun statusVisual(entry: ImportEntry): StatusVisual = when (entry.status) {
    ImportEntryStatus.MATCHED, ImportEntryStatus.PENDING -> StatusVisual(Icons.Rounded.CheckCircle, LevyraCyan, "statusMatched")
    ImportEntryStatus.REVIEW -> StatusVisual(Icons.AutoMirrored.Rounded.HelpOutline, LevyraOrange, "statusReview")
    ImportEntryStatus.MISSING -> StatusVisual(Icons.Rounded.RemoveCircleOutline, LevyraPink, "statusMissing")
    ImportEntryStatus.SKIPPED -> StatusVisual(Icons.Rounded.Block, LevyraMuted, "statusSkipped")
}

internal fun durationLabel(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun ReviewRow(entry: ImportEntry, copy: PlaylistImportHubCopy, onClick: () -> Unit) {
    val visual = statusVisual(entry)
    val status = copy[visual.labelKey]
    val selected = entry.selected?.candidate
    val description = listOfNotNull(
        entry.identity.label,
        selected?.let { "${it.title} — ${it.artistLine}" },
        copy.format("statusDescription", "status" to status)
    ).joinToString(". ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ImportRowShape)
            .clickable(onClick = onClick)
            .background(if (entry.status == ImportEntryStatus.REVIEW) LevyraOrange.copy(alpha = 0.05f) else Color.Transparent)
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ArtworkBox(
            url = entry.identity.artworkUrl.ifBlank { selected?.artworkUrl.orEmpty() },
            modifier = Modifier.size(48.dp).clip(ImportRowShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.identity.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.identity.artistLine, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected != null && entry.status != ImportEntryStatus.SKIPPED) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (selected.origin == CandidateOrigin.LOCAL) Icons.Rounded.PhoneAndroid else Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = LevyraMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "${selected.title} · ${selected.artistLine}",
                        color = LevyraMuted.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (ImportFlag.DUPLICATE_SOURCE in entry.flags || ImportFlag.COLLISION in entry.flags) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Repeat, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(12.dp))
                    Text(
                        if (ImportFlag.COLLISION in entry.flags) copy["collisionNote"] else copy["repeatedNote"],
                        color = LevyraMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(visual.icon, contentDescription = null, tint = visual.tint, modifier = Modifier.size(16.dp))
                Text(status, color = visual.tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            val duration = durationLabel(entry.identity.durationMs.takeIf { it > 0 } ?: selected?.durationMs ?: 0L)
            if (duration.isNotBlank()) Text(duration, color = LevyraMuted, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackReviewSheet(
    entry: ImportEntry,
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(
        onDismissRequest = controller::closeEntry,
        sheetState = sheetState,
        containerColor = LevyraPanel
    ) {
        TrackReviewContent(entry, state, copy, controller, Modifier.fillMaxWidth())
    }
}

internal fun confidenceLabel(confidence: MatchConfidence, copy: PlaylistImportHubCopy): String = when (confidence) {
    MatchConfidence.EXACT -> copy["confExact"]
    MatchConfidence.EXCELLENT -> copy["confExcellent"]
    MatchConfidence.GOOD -> copy["confGood"]
    MatchConfidence.REVIEW -> copy["confReview"]
    MatchConfidence.UNRESOLVED -> copy["confUnresolved"]
}

internal fun reasonLabels(evaluation: MatchEvaluation, copy: PlaylistImportHubCopy): List<String> =
    evaluation.reasons.mapNotNull { reason ->
        when (reason.signal) {
            MatchSignal.DIRECT_ID -> copy["rDirect"]
            MatchSignal.ISRC_MATCH -> copy["rIsrc"]
            MatchSignal.ISRC_CONFLICT -> copy.format("rVersionConflict", "detail" to "ISRC")
            MatchSignal.TITLE_EXACT -> copy["rTitleExact"]
            MatchSignal.TITLE_CLOSE -> copy["rTitleClose"]
            MatchSignal.TITLE_WEAK, MatchSignal.TITLE_MISMATCH -> copy["rTitleWeak"]
            MatchSignal.ARTIST_EXACT, MatchSignal.ARTIST_PRIMARY -> copy["rArtistExact"]
            MatchSignal.ARTIST_PARTIAL -> copy["rArtistPartial"]
            MatchSignal.ARTIST_FEATURED_ONLY -> copy["rArtistFeatured"]
            MatchSignal.ARTIST_MISMATCH -> copy["rArtistMismatch"]
            MatchSignal.ARTIST_UNKNOWN -> null
            MatchSignal.ALBUM_SAME -> copy["rAlbumSame"]
            MatchSignal.ALBUM_EDITION, MatchSignal.ALBUM_DIFFERENT -> copy["rAlbumDifferent"]
            MatchSignal.DURATION_CLOSE, MatchSignal.DURATION_DRIFT, MatchSignal.DURATION_FAR -> {
                val seconds = abs(reason.deltaMs) / 1000.0
                copy.format("rDuration", "delta" to "%s%.1f s".format(if (reason.deltaMs >= 0) "+" else "−", seconds))
            }
            MatchSignal.DURATION_UNKNOWN -> copy["rDurationUnknown"]
            MatchSignal.VERSION_MATCH -> copy["rVersionMatch"]
            MatchSignal.VERSION_CONFLICT, MatchSignal.VERSION_SOFT_DIFFERENCE ->
                copy.format("rVersionConflict", "detail" to reason.detail.replace('_', ' '))
            MatchSignal.EXPLICIT_CONFLICT -> copy["rExplicit"]
            MatchSignal.SONG_ENTITY -> copy["rSong"]
            MatchSignal.VIDEO_UPLOAD -> copy["rVideo"]
            MatchSignal.UNAVAILABLE -> copy["rUnavailable"]
        }
    }

@Composable
internal fun TrackReviewContent(
    entry: ImportEntry,
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController,
    modifier: Modifier
) {
    val position = entry.identity.position
    var query by rememberSaveable(position) { mutableStateOf("${entry.identity.title} ${entry.identity.primaryArtist}".trim()) }
    var origin by rememberSaveable(position) { mutableStateOf(CandidateOrigin.ONLINE) }
    val manual = state.manualSearch?.takeIf { it.position == position }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "source") {
            SectionLabel(copy["sheetSource"])
            CandidateSummary(
                title = entry.identity.title,
                artist = entry.identity.artistLine,
                detail = listOf(entry.identity.album, durationLabel(entry.identity.durationMs)).filter(String::isNotBlank).joinToString(" · "),
                artwork = entry.identity.artworkUrl
            )
        }
        entry.selected?.let { selected ->
            item(key = "selected") {
                SectionLabel(copy["sheetSelected"])
                EvaluationCard(selected, copy, isSelected = !entry.skipped, onUse = null)
            }
        }
        val alternatives = entry.alternatives.filter { it.candidate.id != entry.selected?.candidate?.id }
        if (alternatives.isNotEmpty()) {
            item(key = "alternatives-label") { SectionLabel(copy["sheetAlternatives"]) }
            items(alternatives, key = { "alt-${it.candidate.id}" }) { evaluation ->
                EvaluationCard(evaluation, copy, isSelected = false) {
                    controller.chooseCandidate(position, evaluation.candidate.id)
                    controller.closeEntry()
                }
            }
        }
        item(key = "manual") {
            SectionLabel(copy["searchManually"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CandidateOrigin.ONLINE to copy["searchOnline"], CandidateOrigin.LOCAL to copy["searchLocal"]).forEach { (value, label) ->
                    FilterChip(
                        selected = origin == value,
                        onClick = {
                            origin = value
                            if (manual != null) controller.manualSearch(position, query, value)
                        },
                        label = { Text(label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(copy["searchPlaceholder"]) },
                modifier = Modifier.fillMaxWidth(),
                shape = ImportRowShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { controller.manualSearch(position, query, origin) }),
                trailingIcon = {
                    androidx.compose.material3.IconButton(onClick = { controller.manualSearch(position, query, origin) }) {
                        Icon(Icons.Rounded.Search, contentDescription = copy["searchManually"])
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = LevyraText, unfocusedTextColor = LevyraText)
            )
        }
        if (manual != null) {
            if (manual.loading) {
                item(key = "manual-loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LevyraCyan, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (manual.results.isEmpty()) {
                item(key = "manual-empty") { Text(copy["noResults"], color = LevyraMuted, fontSize = 13.sp) }
            } else {
                items(manual.results, key = { "manual-${it.candidate.id}" }) { evaluation ->
                    EvaluationCard(evaluation, copy, isSelected = false) {
                        controller.chooseManual(position, evaluation.candidate)
                        controller.closeEntry()
                    }
                }
            }
        }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (entry.selected != null && entry.status == ImportEntryStatus.REVIEW) {
                    Button(
                        onClick = {
                            controller.acceptSuggestion(position)
                            controller.closeEntry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(copy["useMatch"]) }
                }
                OutlinedButton(
                    onClick = {
                        controller.skip(position)
                        controller.closeEntry()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(copy["skipTrack"]) }
                TextButton(onClick = { controller.restoreAutomatic(position) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(copy["restoreAuto"], color = LevyraCyan)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = LevyraMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CandidateSummary(title: String, artist: String, detail: String, artwork: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtworkBox(artwork, Modifier.size(56.dp).clip(ImportRowShape))
        Column(Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = LevyraMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) Text(detail, color = LevyraMuted.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EvaluationCard(
    evaluation: MatchEvaluation,
    copy: PlaylistImportHubCopy,
    isSelected: Boolean,
    onUse: (() -> Unit)?
) {
    val candidate = evaluation.candidate
    Surface(
        color = if (isSelected) LevyraCyan.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        shape = ImportRowShape,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onUse != null) Modifier.clip(ImportRowShape).clickable(onClick = onUse) else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CandidateSummary(
                title = candidate.title,
                artist = candidate.artistLine,
                detail = listOf(
                    candidate.album,
                    durationLabel(candidate.durationMs),
                    if (candidate.origin == CandidateOrigin.LOCAL) copy["local"] else copy["online"]
                ).filter(String::isNotBlank).joinToString(" · "),
                artwork = candidate.artworkUrl
            )
            Text(
                "${confidenceLabel(evaluation.confidence, copy)} · ${evaluation.score}",
                color = if (evaluation.confidence.autoAccepted) LevyraCyan else LevyraOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            val reasons = reasonLabels(evaluation, copy)
            if (reasons.isNotEmpty()) {
                Text(reasons.joinToString(" · "), color = LevyraMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            if (onUse != null) {
                OutlinedButton(onClick = onUse, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["useMatch"]) }
            }
        }
    }
}
