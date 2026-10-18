package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.credits.TrackCreditGroup
import com.luc4n3x.levyra.data.credits.TrackCredits
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TrackCreditsCopy
import com.luc4n3x.levyra.ui.i18n.trackCreditsCopy
import java.util.Locale

@Immutable
internal class PlayerLinerNotesState(
    val trackId: String,
    val text: String?,
    val onClick: () -> Unit
)

private const val LinerNotesNameLimit = 2
private const val LinerNotesRevealMs = 320

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerLinerNotes(
    notes: PlayerLinerNotesState?,
    track: Track,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (notes == null) return
    val animated = LocalAnimationsEnabled.current
    val text = notes.text ?: return
    val current = notes.trackId == track.id
    val alpha by animateFloatAsState(
        targetValue = if (current) 1f else 0f,
        animationSpec = tween(if (animated) LinerNotesRevealMs else 0),
        label = "player-liner-notes"
    )
    val label = LocalLevyraStrings.current.trackCreditsCopy().title
    Row(
        modifier = modifier
            .padding(top = 6.dp)
            .heightIn(min = 30.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .clickable(enabled = current, onClickLabel = label, role = Role.Button, onClick = notes.onClick)
            .padding(start = 2.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(MaterialShapes.Cookie6Sided.toShape())
                .background(color.copy(alpha = color.alpha * 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            color = color.copy(alpha = color.alpha * 0.86f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun playerLinerNotesText(credits: TrackCredits, copy: TrackCreditsCopy): String? {
    val producers = credits.sections
        .filter { it.group == TrackCreditGroup.Production }
        .flatMap { it.people }
        .filter { person -> person.roles.any { "produc" in it.lowercase(Locale.ROOT) } }
        .map { it.name }
        .distinct()
    val writers = credits.sections
        .filter { it.group == TrackCreditGroup.Composition }
        .flatMap { it.people }
        .map { it.name }
        .distinct()
    val lead = when {
        producers.isNotEmpty() -> "${copy.producedBy} ${linerNames(producers)}"
        writers.isNotEmpty() -> "${copy.writtenBy} ${linerNames(writers)}"
        else -> null
    }
    val label = credits.release.label.trim().takeIf(String::isNotBlank)
    return listOfNotNull(lead, label).joinToString(" · ").takeIf(String::isNotBlank)
}

private fun linerNames(names: List<String>): String {
    val shown = names.take(LinerNotesNameLimit).joinToString(", ")
    val rest = names.size - LinerNotesNameLimit
    return if (rest > 0) "$shown +$rest" else shown
}
