package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val GlacierGenrePortraitGrade = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
internal fun GlacierGenreChartRail(zones: List<ExploreZone>, kicker: String, onOpen: (ExploreZone) -> Unit) {
    if (zones.isEmpty()) return
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        itemsIndexed(zones, key = { _, zone -> "glacier-genre-chart-${zone.id}" }, contentType = { _, _ -> "glacier-genre-chart" }) { index, zone ->
            GlacierGenreChartCard(
                zone = zone,
                rank = index + 1,
                kicker = kicker,
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, "glacier-genre-chart-${zone.id}"),
                onClick = { onOpen(zone) }
            )
        }
    }
}

@Composable
private fun GlacierGenreChartCard(zone: ExploreZone, rank: Int, kicker: String, modifier: Modifier, onClick: () -> Unit) {
    val cover = rememberGenreChartCover(zone)
    val tone = remember(zone.id) { genreChartCoverTone(zone) }
    val deep = remember(tone) { lerp(tone, MonoDesign.void, 0.72f) }
    val description = "$kicker ${zone.label}"
    Column(
        modifier.width(MonoDesign.cardWide)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = description)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Box(
            Modifier.size(MonoDesign.cardWide).clip(MonoDesign.artShape)
                .background(Brush.linearGradient(listOf(lerp(tone, MonoDesign.void, 0.45f), deep)))
                .border(1.dp, MonoDesign.hairline, MonoDesign.artShape)
        ) {
            val portrait = cover.artworkUrl.trim()
            if (portrait.isNotEmpty()) {
                AsyncImage(
                    model = portrait,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = GlacierGenrePortraitGrade,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MonoWaveLine(
                    playing = false,
                    color = tone.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(40.dp).padding(horizontal = 16.dp)
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to MonoDesign.void.copy(alpha = 0.35f),
                        0.38f to Color.Transparent,
                        0.62f to deep.copy(alpha = 0.35f),
                        1f to deep.copy(alpha = 0.92f)
                    )
                )
            )
            Row(
                Modifier.fillMaxWidth().padding(10.dp).clearAndSetSemantics {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    kicker.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoCodeFont),
                    color = MonoDesign.text,
                    maxLines = 1,
                    modifier = Modifier.clip(MonoDesign.pillShape).border(1.dp, MonoDesign.hairlineStrong, MonoDesign.pillShape)
                        .background(MonoDesign.void.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Box(Modifier.weight(1f))
                Text(monoRank(rank), style = monoRankStyle(12), color = MonoDesign.text.copy(alpha = 0.72f))
            }
            Text(
                zone.label,
                style = MaterialTheme.typography.titleMedium,
                color = MonoDesign.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp).clearAndSetSemantics {}
            )
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(tone))
        }
        Text(
            cover.artists.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MonoDesign.textSecondary,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics {}
        )
    }
}
