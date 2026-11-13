package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

private val HomeGenreChartCardWidth = 168.dp

@Composable
internal fun HomeGenreChartCarousel(
    zones: List<ExploreZone>,
    kicker: String,
    contentPadding: PaddingValues,
    onOpen: (ExploreZone) -> Unit,
    modifier: Modifier = Modifier
) {
    if (zones.isEmpty()) return
    val listState = rememberLazyListState()
    val motion = defaultRailMotionEnabled()
    LazyRow(
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        items(items = zones, key = { zone -> "home-genre-chart-${zone.id}" }) { zone ->
            val cover = rememberGenreChartCover(zone)
            Column(
                modifier = Modifier
                    .width(HomeGenreChartCardWidth)
                    .defaultRailItemMotion(motion, listState, "home-genre-chart-${zone.id}")
                    .levyraPressable(
                        onClick = { onOpen(zone) },
                        role = Role.Button,
                        pressedScale = LevyraPressScale.Tile,
                        onClickLabel = "$kicker ${zone.label}"
                    )
            ) {
                GenreChartCover(
                    genreKey = zone.id,
                    genreTitle = zone.label,
                    accent = genreChartCoverTone(zone),
                    artworkUrl = cover.artworkUrl,
                    kicker = kicker,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = zone.label,
                    style = LevyraType.cardTitle,
                    color = LevyraText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
                run {
                    Text(
                        text = cover.artists.joinToString(", "),
                        style = LevyraType.caption,
                        color = LevyraMuted,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
