package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun SearchArtistSuggestions(
    title: String,
    artists: List<ArtistHit>,
    fallbackNames: List<String>,
    loading: Boolean,
    onArtistClick: (ArtistHit) -> Unit,
    onFallbackClick: (String) -> Unit,
) {
    val visibleArtists = artists
        .asSequence()
        .filter { it.name.isNotBlank() && it.thumbnailUrl.isNotBlank() }
        .distinctBy { artist -> artist.browseId.ifBlank { artist.name.trim().lowercase() } }
        .take(7)
        .toList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = LevyraType.sectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() },
        )

        when {
            visibleArtists.isNotEmpty() -> {
                visibleArtists.forEach { artist ->
                    SearchArtistRow(
                        name = artist.name,
                        detail = artist.subscribers,
                        onClick = { onArtistClick(artist) },
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }

            loading -> {
                repeat(5) {
                    SearchArtistSuggestionSkeleton()
                }
            }

            else -> {
                fallbackNames.take(7).forEach { name ->
                    SearchArtistRow(
                        name = name,
                        detail = "",
                        onClick = { onFallbackClick(name) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchArtistRow(
    name: String,
    detail: String,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SearchArtistRowHeight)
            .clip(SearchArtistRowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SearchArtistAvatarSize)
                .clip(CircleShape),
        ) {
            avatar()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                style = LevyraType.contentTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = LevyraType.metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SearchArtistSuggestionSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SearchArtistRowHeight)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SearchArtistAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            )
        }
    }
}

private val SearchArtistAvatarSize = 56.dp
private val SearchArtistRowHeight = 72.dp
private val SearchArtistRowShape = RoundedCornerShape(12.dp)
