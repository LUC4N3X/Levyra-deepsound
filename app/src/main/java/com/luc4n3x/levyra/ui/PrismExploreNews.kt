package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import java.util.Locale

@Composable
private fun PrismNewsHeader(title: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = PrismDesign.gutter).padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(22.dp).height(4.dp).clip(CircleShape).background(PrismAccent.gradient()))
        Spacer(Modifier.width(12.dp))
        Text(title, color = LevyraText, style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.4).sp), maxLines = 1, modifier = Modifier.semantics { heading() })
    }
}

private fun prismNewsKicker(story: MusicNewsStory, age: String?): String =
    listOfNotNull(story.source.uppercase(Locale.ROOT).takeIf(String::isNotBlank), age).joinToString(" · ")

@Composable
internal fun PrismNewsFront(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val front = remember(stories) { exploreNewsFront(stories) } ?: return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { exploreEditorialCopy(strings.code) }
    val nowMs = remember(stories) { System.currentTimeMillis() }
    val lead = front.lead
    Column(Modifier.fillMaxWidth()) {
        PrismNewsHeader(copy.newsTitle)
        Box(
            Modifier.padding(horizontal = PrismDesign.gutter).fillMaxWidth().aspectRatio(0.95f).clip(PrismDesign.cut(26.dp)).background(Color(0xFF0B090D))
                .levyraPressable(onClick = { onOpen(lead) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Surface)
        ) {
            PrismSplitArtwork(lead.imageUrl, Modifier.fillMaxSize().clearAndSetSemantics { })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.94f))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Row(
                    Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
                    Spacer(Modifier.width(8.dp))
                    Text(prismNewsKicker(lead, exploreNewsAgeLabel(copy, nowMs, lead.publishedAtMs)), color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                Spacer(Modifier.height(10.dp))
                Text(lead.title, color = Color.White, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = (-0.6).sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        if (front.secondary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = PrismDesign.gutter).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                front.secondary.forEach { story ->
                    Column(
                        Modifier.weight(1f)
                            .levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                            .semantics(mergeDescendants = true) { }
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(PrismDesign.cut(18.dp))) { PrismSplitArtwork(story.imageUrl, Modifier.fillMaxSize().clearAndSetSemantics { }) }
                        Text(
                            story.source.uppercase(Locale.ROOT), color = LevyraPink,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                            maxLines = 1, modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(story.title, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (front.secondary.size < ExploreNewsSecondaryCount) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun PrismNewsDesk(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val desk = remember(stories) { exploreNewsDesk(stories, null) }
    if (desk.isEmpty()) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { exploreEditorialCopy(strings.code) }
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth()) {
        PrismNewsHeader(copy.newsMore)
        desk.forEachIndexed { index, story ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    .levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Row)
                    .semantics(mergeDescendants = true) { }
                    .padding(horizontal = PrismDesign.gutter, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "%02d".format(Locale.ROOT, index + 1),
                    style = TextStyle(brush = PrismAccent.gradient(false), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(42.dp).clearAndSetSemantics { }
                )
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(prismNewsKicker(story, exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs)), color = LevyraMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(story.title, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
                Box(Modifier.size(76.dp).clip(PrismDesign.cut(14.dp))) { PrismSplitArtwork(story.imageUrl, Modifier.fillMaxSize().clearAndSetSemantics { }) }
            }
        }
    }
}
