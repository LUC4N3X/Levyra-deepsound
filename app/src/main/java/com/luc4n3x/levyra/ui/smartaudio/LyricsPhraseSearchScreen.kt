package com.luc4n3x.levyra.ui.smartaudio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.LyricsPhraseConfidence
import com.luc4n3x.levyra.data.LyricsPhraseHit
import com.luc4n3x.levyra.domain.LyricsPhraseVersionKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartAudioCopy
import com.luc4n3x.levyra.ui.i18n.SmartAudioText
import com.luc4n3x.levyra.ui.i18n.smartAudioCopy
import com.luc4n3x.levyra.viewmodel.LyricsPhraseSearchStatus
import com.luc4n3x.levyra.viewmodel.LyricsPhraseSearchUiState
import kotlin.math.PI
import kotlin.math.sin

private const val MAX_PHRASE_CHARS = 200

@Composable
internal fun LyricsSearchEntry(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Rounded.FormatQuote, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        Text(
            copy[SmartAudioText.LyricsTitle],
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun LyricsPhraseSearchScreen(
    state: LyricsPhraseSearchUiState,
    animated: Boolean,
    onSearch: (String) -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val strings = LocalLevyraStrings.current
    val copy = smartAudioCopy(strings.code)
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf(state.query) }
    Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.close, tint = scheme.onSurface)
                }
                Text(copy[SmartAudioText.LyricsTitle], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = scheme.onSurface, modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { typed ->
                    query = typed.replace('\n', ' ').take(MAX_PHRASE_CHARS)
                    if ('\n' in typed) onSearch(query)
                },
                placeholder = { Text(copy[SmartAudioText.LyricsHint]) },
                shape = RoundedCornerShape(28.dp),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = { onSearch(query) }) {
                        Icon(Icons.Rounded.Search, copy[SmartAudioText.LyricsTitle], tint = scheme.primary)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            AnimatedContent(
                targetState = state.status,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "lyrics-phrase-status",
                modifier = Modifier.fillMaxSize().padding(top = 12.dp)
            ) { status ->
                when (status) {
                    LyricsPhraseSearchStatus.IDLE -> PhraseMessage(copy[SmartAudioText.LyricsIntro], copy[SmartAudioText.LyricsTip])
                    LyricsPhraseSearchStatus.TOO_SHORT -> PhraseMessage(copy[SmartAudioText.LyricsTooShort], copy[SmartAudioText.LyricsTip])
                    LyricsPhraseSearchStatus.SEARCHING -> PhraseSearching(copy, animated)
                    LyricsPhraseSearchStatus.NO_MATCH -> PhraseMessage(copy[SmartAudioText.LyricsNoMatch], copy[SmartAudioText.LyricsNoMatchHint])
                    LyricsPhraseSearchStatus.UNAVAILABLE -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        PhraseMessage(copy[SmartAudioText.LyricsUnavailable], null)
                        Button(onClick = { onSearch(query) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy[SmartAudioText.LyricsRetry]) }
                    }
                    LyricsPhraseSearchStatus.RESULTS,
                    LyricsPhraseSearchStatus.RESULTS_UNVERIFIED -> PhraseResults(state, copy, onPlay)
                }
            }
        }
    }
}

@Composable
private fun PhraseMessage(title: String, detail: String?) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 28.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, textAlign = TextAlign.Center)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PhraseSearching(copy: SmartAudioCopy, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 36.dp)) {
        val phase = if (animated) {
            val transition = rememberInfiniteTransition(label = "lyrics-phrase-wave")
            val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "lyrics-phrase-phase")
            value
        } else {
            0.25f
        }
        val bar = scheme.primary
        Canvas(Modifier.width(92.dp).height(40.dp)) {
            val count = 7
            val barWidth = size.width / (count * 2 - 1)
            repeat(count) { index ->
                val wave = 0.35f + 0.65f * ((sin((phase + index / count.toFloat()) * 2f * PI.toFloat()) + 1f) / 2f)
                val height = size.height * wave
                drawRoundRect(bar, Offset(index * 2 * barWidth, (size.height - height) / 2f), Size(barWidth, height), CornerRadius(barWidth / 2f))
            }
        }
        Text(copy[SmartAudioText.LyricsSearching], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun PhraseResults(state: LyricsPhraseSearchUiState, copy: SmartAudioCopy, onPlay: (Track, List<Track>) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tracks = state.hits.map(LyricsPhraseHit::track)
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.status == LyricsPhraseSearchStatus.RESULTS_UNVERIFIED) {
            item(key = "unverified") {
                Text(copy[SmartAudioText.LyricsUnverifiedBanner], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
        }
        itemsIndexed(state.hits, key = { _, hit -> hit.track.id }) { index, hit ->
            PhraseHitRow(hit, copy, first = index == 0) { onPlay(hit.track, tracks) }
        }
    }
}

@Composable
private fun PhraseHitRow(hit: LyricsPhraseHit, copy: SmartAudioCopy, first: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strong = first && hit.confidence != LyricsPhraseConfidence.UNVERIFIED
    Surface(
        color = if (strong) scheme.primaryContainer else scheme.surfaceContainer,
        shape = RoundedCornerShape(if (strong) 30.dp else 18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        val content = if (strong) scheme.onPrimaryContainer else scheme.onSurface
        val muted = if (strong) scheme.onPrimaryContainer.copy(alpha = 0.76f) else scheme.onSurfaceVariant
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = hit.track.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(if (strong) 72.dp else 56.dp).clip(RoundedCornerShape(if (strong) 22.dp else 12.dp)).background(scheme.surfaceContainerHighest)
            )
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hit.track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hit.track.artist, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (hit.snippet.isNotBlank()) {
                    Text("“${hit.snippet}”", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConfidenceDots(hit.confidence, content)
                    Text(phraseLabel(hit, copy), style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun phraseLabel(hit: LyricsPhraseHit, copy: SmartAudioCopy): String {
    val confidence = copy[
        when (hit.confidence) {
            LyricsPhraseConfidence.EXACT -> SmartAudioText.LyricsExact
            LyricsPhraseConfidence.HIGH -> SmartAudioText.LyricsHigh
            LyricsPhraseConfidence.POSSIBLE -> SmartAudioText.LyricsPossible
            LyricsPhraseConfidence.UNVERIFIED -> SmartAudioText.LyricsUnverified
        }
    ]
    val version = when (hit.version) {
        LyricsPhraseVersionKind.ORIGINAL -> null
        LyricsPhraseVersionKind.REMIX -> SmartAudioText.LyricsRemix
        LyricsPhraseVersionKind.COVER -> SmartAudioText.LyricsCover
        LyricsPhraseVersionKind.LIVE -> SmartAudioText.LyricsLive
        LyricsPhraseVersionKind.KARAOKE -> SmartAudioText.LyricsKaraoke
    }
    return if (version == null) confidence else "$confidence · ${copy[version]}"
}

@Composable
private fun ConfidenceDots(confidence: LyricsPhraseConfidence, color: androidx.compose.ui.graphics.Color) {
    val filled = when (confidence) {
        LyricsPhraseConfidence.EXACT -> 3
        LyricsPhraseConfidence.HIGH -> 2
        LyricsPhraseConfidence.POSSIBLE -> 1
        LyricsPhraseConfidence.UNVERIFIED -> 0
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (index < filled) color else color.copy(alpha = 0.22f)))
        }
    }
}
