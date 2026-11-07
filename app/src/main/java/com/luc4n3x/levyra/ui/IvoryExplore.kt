package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

private val IvoryGutter = 20.dp

@Composable
internal fun IvoryNewsFront(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val front = remember(stories) { exploreNewsFront(stories) } ?: return
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { exploreEditorialCopy(code) }
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkinSectionTitle(copy.newsTitle, IvoryGutter)
        val lead = front.lead
        Box(
            Modifier.padding(horizontal = IvoryGutter).fillMaxWidth().aspectRatio(0.95f).clip(RoundedCornerShape(IvoryDesign.coverCorner)).background(LevyraPanel)
                .levyraPressable(onClick = { onOpen(lead) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                .semantics(mergeDescendants = true) { }
        ) {
            if (lead.imageUrl.isNotBlank()) AsyncImage(lead.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 76.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    listOfNotNull(lead.source.trim().takeIf(String::isNotEmpty), exploreNewsAgeLabel(copy, nowMs, lead.publishedAtMs)).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, maxLines = 1
                )
                Text(lead.title, color = Color.White, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            IvoryCornerBadge(64.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = IvoryDesign.onInk, modifier = Modifier.size(22.dp))
            }
        }
        if (front.secondary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = IvoryGutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                front.secondary.forEach { story ->
                    key(story.url) {
                        Column(
                            Modifier.weight(1f).levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                                .semantics(mergeDescendants = true) { },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.2f).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
                                if (story.imageUrl.isNotBlank()) AsyncImage(story.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
                            }
                            Text(story.source.trim(), color = IvoryDesign.ink, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(story.title, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs)?.let { Text(it, color = LevyraMuted, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                if (front.secondary.size < ExploreNewsSecondaryCount) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun IvoryTop100Card(label: String, date: String, global: Boolean, modifier: Modifier) {
    val surface = if (global) IvoryDesign.ink else LevyraPanel
    val onSurface = if (global) IvoryDesign.onInk else LevyraText
    Box(modifier.clip(RoundedCornerShape(IvoryDesign.coverCorner)).background(surface)) {
        Text(date, color = onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
        Text(
            "100", color = onSurface, fontSize = 76.sp, lineHeight = 76.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).clearAndSetSemantics { }
        )
        IvoryInkTab(start = true, Modifier.align(Alignment.BottomStart).padding(bottom = 16.dp).fillMaxWidth(0.72f).height(44.dp)) {
            Text(label, color = IvoryDesign.onInk, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 18.dp))
        }
        IvoryCornerBadge(52.dp, Modifier.align(Alignment.BottomEnd)) {
            Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(20.dp))
        }
    }
}
