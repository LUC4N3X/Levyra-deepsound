package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TopResultArtistHero(
    hero: Track,
    artist: ArtistHit?,
    overline: String,
    overlineColor: Color,
    cardColor: Color,
    enabled: Boolean,
    onArtist: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val name = artist?.name?.ifBlank { null } ?: hero.artist
    val officialPortrait = rememberOfficialArtistPortrait(name)
    val portrait = when {
        officialPortrait == null -> ""
        officialPortrait.isNotBlank() -> officialPortrait
        else -> artist?.thumbnailUrl?.ifBlank { null } ?: hero.largeThumbnailUrl.ifBlank { hero.thumbnailUrl }
    }
    val portraitUrl = remember(portrait) { highResolutionHomeArtistArtworkUrl(portrait) }
    val subtitle = artist?.subscribers?.trim()?.takeIf(String::isNotBlank) ?: strings.artistLabel
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TopResultHeroHeight)
            .clickable(enabled = enabled, onClickLabel = strings.openArtist, role = Role.Button, onClick = onArtist)
    ) {
        if (portraitUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(portraitUrl)
                    .crossfade(LocalAnimationsEnabled.current)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.35f),
                modifier = Modifier.matchParentSize()
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to cardColor.copy(alpha = 0.05f),
                        0.38f to cardColor.copy(alpha = 0.18f),
                        0.68f to cardColor.copy(alpha = 0.72f),
                        1f to cardColor
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 96.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = overline,
                    color = overlineColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = name,
                    color = LevyraText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = subtitle,
                    color = LevyraMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.padding(bottom = 4.dp).size(28.dp)
            )
        }
    }
}

private val TopResultHeroHeight = 248.dp

@Composable
internal fun SearchTrackOverflowMenu(
    trackId: String,
    isFavorite: Boolean,
    canDownload: Boolean,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var expanded by remember(trackId) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = strings.more, tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (isFavorite) LevyraPink else LocalContentColor.current
                    )
                },
                onClick = {
                    expanded = false
                    onFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                onClick = {
                    expanded = false
                    onAddToPlaylist()
                }
            )
            if (canDownload) {
                DropdownMenuItem(
                    text = { Text(strings.download) },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = {
                        expanded = false
                        onDownload()
                    }
                )
            }
        }
    }
}
