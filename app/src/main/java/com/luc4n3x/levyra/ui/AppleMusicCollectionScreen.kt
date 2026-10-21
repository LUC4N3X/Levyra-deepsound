package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

@Composable
internal fun AppleMusicCollectionScreen(
    title: String,
    curator: String,
    caption: String,
    description: String,
    artworkUrl: String,
    artworkTrack: Track?,
    tracks: List<Track>,
    currentId: String?,
    loading: Boolean,
    failed: Boolean,
    bottomInset: Dp,
    onDismiss: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTrackActions: (Track) -> Unit,
    onRetry: (() -> Unit)? = null,
    ranked: Boolean = false,
    onCurator: (() -> Unit)? = null
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalLevyraStrings.current
    val copy = LevyraAppleMusicStrings.forCode(strings.code)
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 420 } }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = bottomInset + 24.dp)
        ) {
            item(key = "apple-collection-header", contentType = "apple-collection-header") {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.padding(horizontal = AppleMusicDesign.collectionArtworkInset).padding(top = 10.dp).fillMaxWidth().aspectRatio(1f)
                            .shadow(18.dp, AppleMusicDesign.artworkShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.26f))
                            .clip(AppleMusicDesign.artworkShape).background(AppleMusicDesign.placeholder)
                            .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.artworkShape)
                    ) {
                        when {
                            artworkUrl.isNotBlank() -> StableRemoteArtwork(artworkUrl, title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
                            artworkTrack != null -> CoverImage(artworkTrack, Modifier.fillMaxSize(), highRes = true)
                        }
                    }
                    Text(
                        title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = AppleMusicDesign.label,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp).semantics { heading() }
                    )
                    if (curator.isNotBlank()) {
                        Text(
                            curator, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal), color = AppleMusicDesign.accent,
                            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .then(if (onCurator != null) Modifier.clickable(role = Role.Button, onClick = onCurator) else Modifier)
                        )
                    }
                    if (caption.isNotBlank()) {
                        Text(
                            caption.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppleMusicDesign.secondaryLabel,
                            textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        )
                    }
                    if (description.isNotBlank()) {
                        Text(
                            description, style = MaterialTheme.typography.bodyMedium, color = AppleMusicDesign.secondaryLabel,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        )
                    }
                    AppleMusicPillButtons(strings.playAll, strings.shuffle, tracks.isNotEmpty(), onPlayAll, onShuffle)
                    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = AppleMusicDesign.separator)
                }
            }
            when {
                tracks.isNotEmpty() -> {
                    itemsIndexed(tracks, key = { index, track -> "${track.id}:$index" }, contentType = { _, _ -> "apple-collection-row" }) { index, track ->
                        Box(Modifier.padding(start = 16.dp, end = 6.dp)) {
                            AppleMusicSongRow(
                                track = track,
                                current = track.id == currentId,
                                explicitLabel = copy.explicit,
                                divider = index < tracks.lastIndex,
                                onPlay = { onPlay(track) },
                                onMore = { onTrackActions(track) },
                                leading = if (ranked) {
                                    {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier.size(AppleMusicDesign.songArtwork).clip(AppleMusicDesign.thumbShape).background(AppleMusicDesign.placeholder)
                                                    .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.thumbShape)
                                            ) { CoverImage(track, Modifier.fillMaxSize()) }
                                            Text(
                                                "${index + 1}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = AppleMusicDesign.label, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    item(key = "apple-collection-footer", contentType = "apple-collection-footer") {
                        Text(
                            copy.songs(tracks.size), style = MaterialTheme.typography.bodyMedium, color = AppleMusicDesign.secondaryLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                }
                loading -> item(key = "apple-collection-loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        LevyraLoadingIndicator(color = AppleMusicDesign.secondaryLabel, modifier = Modifier.size(28.dp))
                    }
                }
                failed -> item(key = "apple-collection-failed") {
                    Box(Modifier.padding(top = 16.dp)) { AppleMusicNotice(copy.collectionUnavailable, copy.editorialRetry.takeIf { onRetry != null }, onRetry) }
                }
            }
        }
        AppleMusicNavigationBar(title = title, collapsed = collapsed, onBack = onDismiss)
    }
}

@Composable
internal fun AppleMusicNavigationBar(title: String, collapsed: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(if (collapsed) AppleMusicDesign.chrome else Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(AppleMusicDesign.raised.copy(alpha = if (collapsed) 0f else 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, LocalLevyraStrings.current.back, tint = AppleMusicDesign.accent, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                if (collapsed) title else "", style = MaterialTheme.typography.titleMedium, color = AppleMusicDesign.label,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(48.dp))
        }
        if (collapsed) HorizontalDivider(thickness = 0.5.dp, color = AppleMusicDesign.separator)
    }
}
