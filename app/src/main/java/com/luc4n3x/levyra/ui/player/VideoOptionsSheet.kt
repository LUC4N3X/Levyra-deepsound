package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.VideoSubtitleTrack
import com.luc4n3x.levyra.ui.exploreFullBleed
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.viewmodel.VideoQualityUiState
import com.luc4n3x.levyra.viewmodel.VideoSourceOption
import com.luc4n3x.levyra.viewmodel.VideoSourceUiState
import java.util.Locale

private val VideoOptionsGutter = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoOptionsSheet(
    quality: VideoQualityUiState,
    subtitles: List<VideoSubtitleTrack>,
    selectedSubtitleId: String?,
    sources: VideoSourceUiState,
    showSources: Boolean,
    switching: Boolean,
    onPictureInPicture: () -> Unit,
    onQuality: (String?) -> Unit,
    onSubtitle: (String?) -> Unit,
    onSource: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = VideoOptionsGutter, end = VideoOptionsGutter, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "title") {
                Text(
                    strings.videoOptions,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item(key = "pip") {
                FilledTonalButton(
                    onClick = onPictureInPicture,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(strings.pictureInPicture, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
            if (quality.available) {
                item(key = "quality") {
                    VideoOptionsSection(strings.videoQuality) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = VideoOptionsGutter),
                            modifier = Modifier.exploreFullBleed(VideoOptionsGutter)
                        ) {
                            item(key = "auto") {
                                FilterChip(
                                    selected = false,
                                    enabled = !quality.switching,
                                    onClick = { onQuality(null) },
                                    label = { Text(strings.videoQualityAuto) }
                                )
                            }
                            items(quality.ladder, key = { it.label }) { rung ->
                                FilterChip(
                                    selected = quality.activeLabel == rung.label,
                                    enabled = !quality.switching,
                                    onClick = { onQuality(rung.label) },
                                    label = { Text(rung.label) }
                                )
                            }
                        }
                    }
                }
            }
            if (subtitles.isNotEmpty()) {
                item(key = "subtitles") {
                    VideoOptionsSection(strings.subtitlesLabel) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = VideoOptionsGutter),
                            modifier = Modifier.exploreFullBleed(VideoOptionsGutter)
                        ) {
                            item(key = "off") {
                                FilterChip(
                                    selected = selectedSubtitleId == null,
                                    onClick = { onSubtitle(null) },
                                    label = { Text(strings.subtitlesOff) }
                                )
                            }
                            items(subtitles, key = { it.id }) { subtitle ->
                                FilterChip(
                                    selected = selectedSubtitleId == subtitle.id,
                                    onClick = { onSubtitle(subtitle.id) },
                                    label = { Text(subtitle.label.ifBlank { subtitle.languageCode }) }
                                )
                            }
                        }
                    }
                }
            }
            if (showSources) {
                item(key = "sources-header") {
                    VideoOptionsSection(strings.videoSource, strings.videoSourceSubtitle) {}
                }
                when {
                    sources.options.isEmpty() && sources.loading -> item(key = "sources-loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            LevyraLoadingIndicator(color = scheme.primary)
                        }
                    }
                    sources.options.none { it.videoId != sources.activeVideoId } && !sources.loading -> item(key = "sources-empty") {
                        Text(
                            strings.videoSourceEmpty,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                    else -> items(sources.options, key = { "source-${it.videoId}" }) { option ->
                        VideoSourceRow(
                            option = option,
                            selected = option.videoId == sources.activeVideoId,
                            enabled = !switching,
                            officialLabel = strings.videoSourceOfficial,
                            onSelect = { onSource(option.videoId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoOptionsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun VideoSourceRow(
    option: VideoSourceOption,
    selected: Boolean,
    enabled: Boolean,
    officialLabel: String,
    onSelect: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainer)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 72.dp)
            .padding(10.dp)
    ) {
        Box(Modifier.width(116.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerHighest)) {
            AsyncImage(
                model = option.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (option.durationMs > 0L) {
                Text(
                    formatVideoSourceDuration(option.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(scheme.inverseSurface.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                option.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (option.official) {
                    Surface(shape = RoundedCornerShape(50), color = scheme.tertiaryContainer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = scheme.onTertiaryContainer, modifier = Modifier.size(12.dp))
                            Text(officialLabel, style = MaterialTheme.typography.labelSmall, color = scheme.onTertiaryContainer)
                        }
                    }
                }
                Text(
                    option.channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}

private fun formatVideoSourceDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
