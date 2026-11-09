package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TorqueNewReleasesDestination(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    val labels = remember(strings.code) { searchDiscographyCopy(strings.code) }
    val badge: (AlbumHit) -> String = { album ->
        when (album.releaseType) {
            ReleaseType.Album, ReleaseType.Compilation -> labels.album
            ReleaseType.Single -> labels.single
            ReleaseType.Ep -> "EP"
            ReleaseType.Unknown -> ""
        }
    }
    val rest = remember(releases) { releases.drop(1).chunked(2) }
    LazyColumn(Modifier.fillMaxSize().background(LevyraBlack), contentPadding = PaddingValues(bottom = bottomInset + 24.dp)) {
        item(key = "torque-releases-head") {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = TorqueGutter, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).background(LevyraPanel)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = LevyraText) }
                Spacer(Modifier.width(14.dp))
                Text(
                    strings.exploreNewReleases.uppercase(), color = LevyraText, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
                )
                if (releases.isNotEmpty()) Text(
                    releases.size.toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.Black).border(1.dp, LevyraPanel).padding(horizontal = 10.dp, vertical = 6.dp).clearAndSetSemantics { }
                )
            }
        }
        when {
            isLoading && releases.isEmpty() -> item(key = "torque-releases-loading") {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(color = LevyraActivePalette.blue, modifier = Modifier.size(36.dp)) }
            }
            releases.isEmpty() -> item(key = "torque-releases-empty") {
                Text(strings.exploreEmpty, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(TorqueGutter))
            }
            else -> {
                item(key = "torque-releases-lead") {
                    val lead = releases.first()
                    TorqueReleaseLead(lead, badge(lead), strings) { onOpenRelease(lead) }
                }
                itemsIndexed(rest, key = { index, pair -> "torque-releases-$index-${pair.first().browseId.ifBlank { pair.first().title }}" }) { rowIndex, pair ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = TorqueGutter, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { column, album ->
                            TorqueReleaseGridCard(rowIndex * 2 + column + 2, album, badge(album), Modifier.weight(1f)) { onOpenRelease(album) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueReleaseLead(album: AlbumHit, badge: String, strings: LevyraStrings, onOpen: () -> Unit) {
    Box(
        Modifier.padding(horizontal = TorqueGutter).padding(bottom = 10.dp).fillMaxWidth().aspectRatio(1f).background(LevyraPanel)
            .levyraPressable(onClick = onOpen, onClickLabel = strings.openAlbum, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(album.title, badge, album.artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        AsyncImage(album.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.8f), 1f to Color.Black.copy(alpha = 0.92f))))
        Row(Modifier.align(Alignment.TopStart).padding(12.dp).clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
            TorqueRank(1, true)
            if (badge.isNotBlank()) Text(
                badge.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp,
                modifier = Modifier.padding(start = 6.dp).background(Color.Black).padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(36.dp).height(4.dp).background(LevyraActivePalette.blue))
                LevyraMediaTitle(album.title.uppercase(), explicit = album.explicit, color = Color.White, fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Text(listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.padding(start = 12.dp).size(60.dp).clip(CircleShape).background(Color.Black).border(4.dp, Color.White, CircleShape).clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun TorqueReleaseGridCard(rank: Int, album: AlbumHit, badge: String, modifier: Modifier, onOpen: () -> Unit) {
    Column(
        modifier.background(LevyraPanel)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(album.title, badge, album.artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(album.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            Box(Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics { }) { TorqueRank(rank, false) }
            if (badge.isNotBlank()) Text(
                badge.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                modifier = Modifier.align(Alignment.BottomStart).background(Color.Black).padding(horizontal = 8.dp, vertical = 4.dp).clearAndSetSemantics { }
            )
        }
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LevyraMediaTitle(album.title.uppercase(), explicit = album.explicit, color = LevyraText, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(album.artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
