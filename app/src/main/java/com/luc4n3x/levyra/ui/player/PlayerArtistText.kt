package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.luc4n3x.levyra.domain.ArtistCredit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistCredits

private const val ArtistAnnotationTag = "levyra_player_artist"

@Composable
internal fun PlayerArtistText(
    track: Track,
    color: Color,
    style: TextStyle,
    onClickLabel: String,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val credits = remember(track.artist, track.artistBrowseIds) {
        artistCredits(track.artist, track.artistBrowseIds)
    }
    val annotated = remember(track.artist, credits) {
        annotatedArtistCredits(track.artist, credits)
    }
    var layoutResult by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }
    val interactive = enabled && credits.isNotEmpty()
    val latestOnArtistClick by rememberUpdatedState(onArtistClick)
    val accessibilityActions = credits.mapIndexed { index, credit ->
        CustomAccessibilityAction(
            label = "$onClickLabel: ${credit.name}",
            action = {
                latestOnArtistClick(index)
                true
            }
        )
    }

    Text(
        text = annotated,
        color = color,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .semantics {
                if (interactive) {
                    role = Role.Button
                    onClick(label = onClickLabel) {
                        latestOnArtistClick(0)
                        true
                    }
                    if (credits.size > 1) {
                        customActions = accessibilityActions
                    }
                }
            }
            .pointerInput(annotated, interactive) {
                if (interactive) {
                    detectTapGestures { position ->
                        val layout = layoutResult ?: return@detectTapGestures
                        if (annotated.isEmpty()) return@detectTapGestures
                        val offset = layout.getOffsetForPosition(position)
                            .coerceIn(0, annotated.length - 1)
                        if (!layout.getBoundingBox(offset).contains(position)) {
                            return@detectTapGestures
                        }
                        val artistIndex = annotated
                            .getStringAnnotations(
                                tag = ArtistAnnotationTag,
                                start = offset,
                                end = (offset + 1).coerceAtMost(annotated.length)
                            )
                            .firstOrNull()
                            ?.item
                            ?.toIntOrNull()

                        if (artistIndex != null) {
                            latestOnArtistClick(artistIndex)
                        }
                    }
                }
            }
    )
}

internal fun annotatedArtistCredits(
    artistText: String,
    credits: List<ArtistCredit>
): AnnotatedString = buildAnnotatedString {
    append(artistText)
    var searchStart = 0
    credits.forEachIndexed { index, credit ->
        val start = artistText.indexOf(
            string = credit.name,
            startIndex = searchStart.coerceAtMost(artistText.length),
            ignoreCase = true
        )
        if (start >= 0) {
            val end = (start + credit.name.length).coerceAtMost(artistText.length)
            addStringAnnotation(
                tag = ArtistAnnotationTag,
                annotation = index.toString(),
                start = start,
                end = end
            )
            searchStart = end
        }
    }
}
