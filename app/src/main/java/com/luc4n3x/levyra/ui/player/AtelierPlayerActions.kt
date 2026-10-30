package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.credits.TrackCreditGroup
import com.luc4n3x.levyra.data.credits.TrackCreditPerson
import com.luc4n3x.levyra.data.credits.TrackCreditSection
import com.luc4n3x.levyra.data.credits.TrackCredits
import com.luc4n3x.levyra.data.credits.TrackCreditsParser
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.atelierUppercaseLabel
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TrackCreditsCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private val AtelierSheetArtwork = 64.dp
private val AtelierSheetIcon = 22.dp
private val AtelierSheetIndent = 42.dp

@Composable
internal fun AtelierPlayerActions(
    track: Track,
    artworkUrl: String,
    actions: List<PlayerSheetAction>,
    engagementContent: (@Composable () -> Unit)?,
    discoverContent: (@Composable () -> Unit)?,
    onPerform: (PlayerSheetAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        AtelierPlayerActionsHeader(track, artworkUrl)
        engagementContent?.let {
            Column(Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, bottom = 16.dp)) { it() }
        }
        HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairlineStrong)
        actions.forEachIndexed { index, action ->
            key(action.key) {
                AtelierPlayerActionRow(action, onPerform)
                if (index < actions.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline, modifier = Modifier.padding(start = AtelierDesign.gutter + AtelierSheetIndent))
                }
            }
        }
        discoverContent?.let {
            Spacer(Modifier.height(AtelierDesign.itemGap))
            Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)) { it() }
        }
    }
}

@Composable
private fun AtelierPlayerActionsHeader(track: Track, artworkUrl: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SeamlessArtworkImage(
            url = artworkUrl,
            modifier = Modifier.size(AtelierSheetArtwork).clip(AtelierDesign.sleeveShape).border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)
        ) {
            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title, style = MaterialTheme.typography.titleLarge, color = AtelierDesign.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
            if (track.artist.isNotBlank()) {
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AtelierPlayerActionRow(action: PlayerSheetAction, onPerform: (PlayerSheetAction) -> Unit) {
    val highlighted = action.enabled && action.active
    val iconTint = when {
        !action.enabled -> AtelierDesign.textFaint
        highlighted -> AtelierDesign.accent
        else -> AtelierDesign.text
    }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = AtelierDesign.rowHeight)
            .clickable(enabled = action.enabled && !action.busy, role = if (action.toggle) Role.Switch else Role.Button) { onPerform(action) }
            .semantics {
                contentDescription = listOfNotNull(action.label, action.value).joinToString(", ")
                if (action.toggle) toggleableState = ToggleableState(action.active)
            }
            .padding(horizontal = AtelierDesign.gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (action.busy) {
            CircularProgressIndicator(Modifier.size(AtelierSheetIcon), color = AtelierDesign.accent, strokeWidth = 2.dp)
        } else {
            PlayerIcon(action.icon, iconTint, Modifier.size(AtelierSheetIcon))
        }
        Spacer(Modifier.width(AtelierSheetIndent - AtelierSheetIcon))
        Text(
            action.label, style = MaterialTheme.typography.titleMedium,
            color = if (action.enabled) AtelierDesign.text else AtelierDesign.textFaint,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        action.value?.let { value ->
            Spacer(Modifier.width(12.dp))
            Text(
                value, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (action.toggle && action.active) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Check, null, tint = AtelierDesign.accent, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AtelierTrackCreditsSheet(
    track: Track,
    artists: List<ArtistHit>,
    credits: TrackCredits?,
    releaseRows: List<Pair<String, String>>,
    copy: TrackCreditsCopy,
    sheetState: SheetState,
    onOpenArtist: (Int) -> Unit,
    onDismiss: () -> Unit,
    onToggleFollow: ((Int) -> Unit)?,
    isFollowed: (Int) -> Boolean,
    onRetry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = AtelierDesign.panelCorner, topEnd = AtelierDesign.panelCorner),
        containerColor = AtelierDesign.cabinet,
        contentColor = AtelierDesign.text
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, bottom = 32.dp)
        ) {
            Text(
                atelierUppercaseLabel(copy.title), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.accent,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(6.dp))
            Text(track.title, style = MaterialTheme.typography.headlineSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (track.artist.isNotBlank()) {
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairlineStrong)
            if (credits == null) {
                Text(copy.loading, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                AtelierCreditsContent(track, artists, credits, releaseRows, copy, onOpenArtist, onToggleFollow, isFollowed, onRetry)
            }
        }
    }
}

@Composable
private fun AtelierCreditsContent(
    track: Track,
    artists: List<ArtistHit>,
    credits: TrackCredits,
    releaseRows: List<Pair<String, String>>,
    copy: TrackCreditsCopy,
    onOpenArtist: (Int) -> Unit,
    onToggleFollow: ((Int) -> Unit)?,
    isFollowed: (Int) -> Boolean,
    onRetry: () -> Unit
) {
    val artistNames = remember(artists, track.artist) { artists.map(ArtistHit::name).ifEmpty { listOf(track.artist) } }
    val display = remember(credits, artistNames) { TrackCreditsParser.withArtists(credits, artistNames) }
    display.sections.filter { it.people.isNotEmpty() }.forEach { section ->
        key(section.group, section.title) {
            AtelierCreditsSection(section, copy, onOpenArtist, onToggleFollow.takeIf { artists.isNotEmpty() }, isFollowed)
        }
    }
    if (releaseRows.isNotEmpty()) {
        AtelierCreditsHeading(copy.release)
        releaseRows.forEach { (label, value) ->
            key(label) { AtelierCreditsFact(label, value) }
        }
    }
    if (credits.isEmpty || !credits.remoteResolved) {
        AtelierCreditsNotice(credits.remoteResolved, copy, onRetry)
    }
    if (credits.provider.isNotBlank()) {
        Text(
            "${copy.sourcePrefix} · ${credits.provider}", style = MaterialTheme.typography.bodySmall,
            color = AtelierDesign.textFaint, modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
private fun AtelierCreditsHeading(title: String) {
    Text(
        atelierUppercaseLabel(title), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.textSecondary,
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
private fun AtelierCreditsSection(
    section: TrackCreditSection,
    copy: TrackCreditsCopy,
    onOpenArtist: (Int) -> Unit,
    onToggleFollow: ((Int) -> Unit)?,
    isFollowed: (Int) -> Boolean
) {
    val title = when (section.group) {
        TrackCreditGroup.Artists -> copy.artists
        TrackCreditGroup.Performance -> copy.performance
        TrackCreditGroup.Composition -> copy.composition
        TrackCreditGroup.Production -> copy.production
        TrackCreditGroup.Other -> section.title.ifBlank { copy.otherCredits }
    }
    val artistSection = section.group == TrackCreditGroup.Artists
    AtelierCreditsHeading(title)
    section.people.forEachIndexed { index, person ->
        val roles = when {
            !artistSection -> person.roles.joinToString(" · ")
            index == 0 -> copy.mainArtist
            else -> copy.participatingArtist
        }
        key(index, person.name) {
            AtelierCreditsPersonRow(person, roles, copy.openArtist, onOpenArtist, onToggleFollow.takeIf { artistSection }, isFollowed)
        }
    }
}

@Composable
private fun AtelierCreditsPersonRow(
    person: TrackCreditPerson,
    roles: String,
    openLabel: String,
    onOpenArtist: (Int) -> Unit,
    onToggleFollow: ((Int) -> Unit)?,
    isFollowed: (Int) -> Boolean
) {
    val strings = LocalLevyraStrings.current
    val linkedIndex = person.linkedArtistIndex
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f)
                .then(
                    if (linkedIndex != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = openLabel) { onOpenArtist(linkedIndex) }
                    } else {
                        Modifier
                    }
                )
                .heightIn(min = 56.dp)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(person.name, style = MaterialTheme.typography.titleMedium, color = if (linkedIndex != null) AtelierDesign.accentBright else AtelierDesign.text)
            if (roles.isNotBlank()) {
                Text(roles, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary)
            }
        }
        if (onToggleFollow != null && linkedIndex != null) {
            val followed = isFollowed(linkedIndex)
            val label = if (followed) strings.followingArtist else strings.followArtist
            Box(
                Modifier.size(48.dp).clip(AtelierDesign.cardShape).clickable(role = Role.Button, onClickLabel = label) { onToggleFollow(linkedIndex) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd, label,
                    tint = if (followed) AtelierDesign.accent else AtelierDesign.text, modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
}

@Composable
private fun AtelierCreditsFact(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.text)
    }
    HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
}

@Composable
private fun AtelierCreditsNotice(remoteResolved: Boolean, copy: TrackCreditsCopy, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(if (remoteResolved) Icons.Rounded.Info else Icons.Rounded.CloudOff, null, tint = AtelierDesign.accent, modifier = Modifier.size(24.dp))
        Text(if (remoteResolved) copy.emptyTitle else copy.unavailableTitle, style = MaterialTheme.typography.titleMedium, color = AtelierDesign.text)
        Text(if (remoteResolved) copy.emptyBody else copy.unavailableBody, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary)
        Box(
            Modifier.padding(top = 6.dp).heightIn(min = 48.dp).clip(AtelierDesign.cardShape)
                .border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.cardShape)
                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) { Text(copy.retry, style = MaterialTheme.typography.labelLarge, color = AtelierDesign.text) }
    }
}
