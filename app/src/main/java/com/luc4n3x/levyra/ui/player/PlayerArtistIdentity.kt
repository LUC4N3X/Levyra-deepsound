package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private const val PlayerArtistAvatarLimit = 2
private val PlayerTopArtistAvatarSize = 46.dp
private val PlayerTopArtistAvatarStep = 34.dp
private val PlayerArtistPickerAvatar = 78.dp
private val PlayerArtistPickerCardWidth = 118.dp

@Composable
internal fun PlayerTopArtistCluster(
    artists: List<ArtistHit>,
    surfaces: PlayerSurfaceTokens,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.size < 2) return

    val visible = artists.take(PlayerArtistAvatarLimit)
    val overflow = (artists.size - visible.size).coerceAtLeast(0)
    val slots = visible.size + if (overflow > 0) 1 else 0
    val totalWidth = PlayerTopArtistAvatarSize +
        PlayerTopArtistAvatarStep * (slots - 1).coerceAtLeast(0).toFloat()

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(PlayerTopArtistAvatarSize)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            )
    ) {
        visible.forEachIndexed { index, artist ->
            PlayerTopArtistAvatar(
                artist = artist,
                surfaces = surfaces,
                modifier = Modifier
                    .offset(x = PlayerTopArtistAvatarStep * index.toFloat())
                    .zIndex(index.toFloat())
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = PlayerTopArtistAvatarStep * visible.size.toFloat())
                    .zIndex(visible.size.toFloat())
                    .size(PlayerTopArtistAvatarSize)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(surfaces.control)
                    .border(1.dp, surfaces.content.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    color = surfaces.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerTopArtistAvatar(
    artist: ArtistHit,
    surfaces: PlayerSurfaceTokens,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(PlayerTopArtistAvatarSize)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(surfaces.controlQuiet)
            .border(2.dp, surfaces.outline.copy(alpha = 0.72f), CircleShape)
    ) {
        SeamlessArtworkImage(
            url = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerArtistInitial(artist.name, surfaces, 15.sp)
        }
    }
}

@Composable
private fun PlayerArtistInitial(
    name: String,
    surfaces: PlayerSurfaceTokens,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaces.control),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            color = surfaces.content,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun PlayerArtistPickerSheet(
    track: Track,
    artists: List<ArtistHit>,
    title: String,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerSheetFrame(
        surfaces = surfaces,
        animated = animated,
        onDismiss = onDismiss
    ) { dismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LevyraPlayerDesign.Gutter)
                .padding(bottom = LevyraPlayerDesign.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)) {
                Text(
                    text = title,
                    color = surfaces.content,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = track.artist,
                    color = surfaces.contentMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (artists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                        color = surfaces.content
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
                ) {
                    itemsIndexed(
                        items = artists,
                        key = { index, artist -> artist.browseId.ifBlank { "$index:${artist.name}" } }
                    ) { index, artist ->
                        PlayerArtistPickerCard(
                            artist = artist,
                            surfaces = surfaces,
                            onClick = {
                                dismiss()
                                onArtistClick(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerArtistPickerCard(
    artist: ArtistHit,
    surfaces: PlayerSurfaceTokens,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(PlayerArtistPickerCardWidth)
            .clip(LevyraPlayerDesign.ShapeMd)
            .background(surfaces.controlQuiet)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = LevyraPlayerDesign.SpaceMd,
                vertical = LevyraPlayerDesign.SpaceLg
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
    ) {
        Box(
            modifier = Modifier
                .size(PlayerArtistPickerAvatar)
                .clip(CircleShape)
                .background(surfaces.control)
                .border(1.dp, surfaces.content.copy(alpha = 0.14f), CircleShape)
        ) {
            SeamlessArtworkImage(
                url = artist.thumbnailUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerArtistInitial(artist.name, surfaces, 24.sp)
            }
        }
        Text(
            text = artist.name,
            color = surfaces.content,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
