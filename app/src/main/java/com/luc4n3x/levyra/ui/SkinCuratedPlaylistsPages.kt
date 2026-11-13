package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import kotlinx.coroutines.launch

private class CuratedSkinLayout(val gutter: Dp, val gap: Dp, val rowGap: Dp, val cellMin: Dp, val skeleton: Shape, val skeletonBase: Color)

@Composable
private fun SkinCuratedScaffold(
    page: CuratedPageData,
    background: Color,
    layout: CuratedSkinLayout,
    bottomInset: Dp,
    jumpPills: Boolean,
    intro: @Composable (jump: (Int) -> Unit) -> Unit,
    header: @Composable (CuratedPageRow.Header) -> Unit,
    cell: @Composable (item: YoutubeMusicHomeItem, number: Int, modifier: Modifier) -> Unit,
    top: @Composable () -> Unit = {}
) {
    val animated = LocalAnimationsEnabled.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var stickyPx by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(background).pointerInput(Unit) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            top()
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val columns = curatedPageColumns(maxWidth, layout.gutter, layout.gap, layout.cellMin)
                val rows = remember(page.groups, columns) { curatedPageRows(page.groups, columns) }
                val sticky = jumpPills && page.groups.size > 1
                val focused by rememberCuratedFocusedGroup(listState, rows) { stickyPx }
                val jump: (Int) -> Unit = { group ->
                    val header = curatedPageHeaderIndex(rows, group)
                    if (header >= 0) scope.launch {
                        val target = header + curatedLeadingItems(sticky)
                        val offset = if (sticky) -stickyPx else 0
                        if (animated) listState.animateScrollToItem(target, offset) else listState.scrollToItem(target, offset)
                    }
                }
                CuratedGroupedList(
                    rows = rows, page = page, listState = listState, columns = columns,
                    gutter = layout.gutter, itemGap = layout.gap, rowGap = layout.rowGap,
                    contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
                    intro = { intro(jump) },
                    sticky = if (sticky) {
                        { SkinCuratedJumpPills(page, focused, background, layout.gutter, Modifier.onSizeChanged { stickyPx = it.height }, jump) }
                    } else {
                        null
                    },
                    header = header,
                    cell = cell,
                    placeholder = {
                        CuratedSkeletonRow(columns, layout.gutter, layout.gap, layout.skeleton, layout.skeletonBase, modifier = Modifier.padding(top = 24.dp))
                    },
                    empty = {
                        Text(
                            LocalLevyraStrings.current.exploreEmpty, style = MaterialTheme.typography.bodyLarge, color = LevyraMuted,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = layout.gutter, vertical = 40.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SkinCuratedJumpPills(page: CuratedPageData, selected: Int, background: Color, gutter: Dp, modifier: Modifier, onJump: (Int) -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val rowState = rememberLazyListState()
    LaunchedEffect(selected) {
        val target = (selected - 1).coerceAtLeast(0)
        if (animated) rowState.animateScrollToItem(target) else rowState.scrollToItem(target)
    }
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().background(background).padding(vertical = 4.dp).selectableGroup()
    ) {
        itemsIndexed(page.groups, key = { _, shelf -> "skin-curated-jump-${shelf.key}" }, contentType = { _, _ -> "skin-curated-jump" }) { index, shelf ->
            LevyraFilterPill(label = shelf.title, selected = index == selected, onClick = { onJump(index) })
        }
    }
}

private fun curatedCountLabel(count: Int, plural: String): String = "$count · $plural"

@Composable
internal fun TorqueCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    SkinCuratedScaffold(
        page = page,
        background = LevyraBlack,
        layout = CuratedSkinLayout(TorqueGutter, 12.dp, 12.dp, 150.dp, RectangleShape, LevyraPanel),
        bottomInset = bottomInset,
        jumpPills = true,
        intro = {
            Box(Modifier.fillMaxWidth().aspectRatio(1.1f).background(LevyraPanel)) {
                AsyncImage(page.covers.firstOrNull(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.3f to Color.Transparent, 0.65f to Color.Black.copy(alpha = 0.55f), 1f to LevyraBlack)
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.padding(8.dp).size(48.dp).background(Color.Black)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = Color.White)
                }
                if (page.total > 0) Text(
                    page.total.toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black).padding(horizontal = 10.dp, vertical = 6.dp).clearAndSetSemantics { }
                )
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = TorqueGutter, end = TorqueGutter, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        strings.playlistsPlain.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, maxLines = 1,
                        modifier = Modifier.clip(TorqueTagShape).background(LevyraActivePalette.blue).padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                    Text(
                        page.title.uppercase(), color = Color.White, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                    )
                }
            }
        },
        header = { row ->
            Box(Modifier.padding(top = 18.dp, bottom = 4.dp)) {
                SkinSectionTitle(row.shelf.title, TorqueGutter) {
                    Text(
                        row.shelf.items.size.toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.clip(TorqueTagShape).background(LevyraActivePalette.blue).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        cell = { item, number, modifier ->
            TorqueCuratedCard(number, item.title, monoWithoutYoutube(item.subtitle), item.thumbnailUrl, modifier.aspectRatio(TorqueCuratedWidth / TorqueCuratedHeight)) {
                item.playlist?.let(onOpen)
            }
        }
    )
}

@Composable
internal fun IvoryCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val gutter = 20.dp
    SkinCuratedScaffold(
        page = page,
        background = LevyraBlack,
        layout = CuratedSkinLayout(gutter, 14.dp, 20.dp, 150.dp, IvoryDesign.coverShape, LevyraPanel),
        bottomInset = bottomInset,
        jumpPills = true,
        intro = {
            Column(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 8.dp, bottom = 16.dp)) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).clip(CircleShape).background(IvoryDesign.onInk)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = IvoryDesign.ink)
                }
                if (page.covers.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(196.dp).padding(top = 12.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                        page.covers.take(3).forEachIndexed { index, url ->
                            val tilt = (index - 1) * 8f
                            Box(
                                Modifier.offset(x = 64.dp * (index - 1)).size(150.dp)
                                    .graphicsLayer {
                                        rotationZ = tilt
                                        shadowElevation = 8.dp.toPx()
                                        shape = IvoryDesign.coverShape
                                        clip = true
                                    }
                                    .background(LevyraPanel)
                            ) { AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
                if (page.total > 0) Text(
                    curatedCountLabel(page.total, strings.playlistsPlain), color = IvoryDesign.onInk, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    modifier = Modifier.padding(top = 18.dp).clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Text(
                    page.title, color = LevyraText, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).semantics { heading() }
                )
            }
        },
        header = { row ->
            Box(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                IvorySectionTitle(row.shelf.title, gutter) {
                    Text(row.shelf.items.size.toString(), color = LevyraMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        },
        cell = { item, _, modifier -> IvoryCuratedCell(item, modifier) { item.playlist?.let(onOpen) } }
    )
}

@Composable
private fun IvoryCuratedCell(item: YoutubeMusicHomeItem, modifier: Modifier, onOpen: () -> Unit) {
    val subtitle = remember(item.subtitle) { monoWithoutYoutube(item.subtitle) }
    Column(
        modifier.levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(item.title, subtitle).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
            AsyncImage(item.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            IvoryCornerBadge(44.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            item.title, color = LevyraText, style = MaterialTheme.typography.titleMedium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
        Text(
            subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
internal fun PrismCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val background = prismPageBackground
    val gutter = PrismDesign.gutter
    SkinCuratedScaffold(
        page = page,
        background = background,
        layout = CuratedSkinLayout(gutter, 14.dp, 14.dp, 150.dp, PrismDesign.cut(22.dp), PrismAccent.Tint),
        bottomInset = bottomInset,
        jumpPills = true,
        intro = {
            Box(Modifier.fillMaxWidth().aspectRatio(1.05f)) {
                Row(Modifier.fillMaxSize().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    page.covers.ifEmpty { listOf("") }.forEach { url ->
                        PrismSplitArtwork(url, Modifier.weight(1f).fillMaxHeight().background(LevyraBlack))
                    }
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.35f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.55f), 1f to background)
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.padding(8.dp).size(48.dp).clip(PrismDesign.cut(14.dp)).background(Color.Black.copy(alpha = 0.4f))) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = Color.White)
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp)) {
                    if (page.total > 0) Row(
                        Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            curatedCountLabel(page.total, strings.playlistsPlain).uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 1.4.sp, maxLines = 1
                        )
                    }
                    Text(
                        page.title, color = LevyraText, fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-1.2).sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).semantics { heading() }
                    )
                }
            }
        },
        header = { row ->
            PrismShelfHeader(row.shelf.title, gutter) {
                Text(row.shelf.items.size.toString(), color = LevyraMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 12.dp))
            }
        },
        cell = { item, _, modifier ->
            PrismCuratedTile(item, modifier.aspectRatio(PrismCuratedWidth / PrismCuratedHeight)) { item.playlist?.let(onOpen) }
        }
    )
}

@Composable
internal fun AtelierCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val gutter = AtelierDesign.gutter
    SkinCuratedScaffold(
        page = page,
        background = AtelierDesign.room,
        layout = CuratedSkinLayout(gutter, AtelierDesign.itemGap, 22.dp, 140.dp, AtelierDesign.sleeveShape, AtelierDesign.panel),
        bottomInset = bottomInset,
        jumpPills = false,
        top = { AtelierDestinationTopBar(null, strings.back, onDismiss) },
        intro = { jump ->
            Column(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 6.dp, bottom = 4.dp)) {
                Text(
                    page.title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = AtelierDesign.text,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                if (page.total > 0) Text(
                    curatedCountLabel(page.total, strings.playlistsPlain), fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp,
                    color = AtelierDesign.textSecondary, maxLines = 1, modifier = Modifier.padding(top = 6.dp)
                )
                if (page.groups.size > 1) Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    page.groups.forEachIndexed { index, shelf ->
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .levyraPressable(onClick = { jump(index) }, role = Role.Button, onClickLabel = shelf.title, pressedScale = LevyraPressScale.Row),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "№ " + (index + 1).toString().padStart(2, '0'), fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp,
                                color = AtelierDesign.accent, modifier = Modifier.clearAndSetSemantics { }
                            )
                            Text(
                                shelf.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = AtelierDesign.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            Text(shelf.items.size.toString(), style = MaterialTheme.typography.labelLarge, color = AtelierDesign.textSecondary, maxLines = 1)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
                }
            }
        },
        header = { row ->
            Column(Modifier.padding(bottom = AtelierDesign.headerToContent)) {
                AtelierSectionLabel(row.shelf.title) {
                    Text(
                        row.shelf.items.size.toString(), fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 14.sp,
                        color = AtelierDesign.textSecondary, maxLines = 1, modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        },
        cell = { item, _, modifier ->
            AtelierDestinationSleeve(item.title, monoWithoutYoutube(item.subtitle), item.thumbnailUrl, false, modifier) { item.playlist?.let(onOpen) }
        }
    )
}

@Composable
internal fun NocturneCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val p = Nocturne78Palette
    val gutter = 20.dp
    val corner = RoundedCornerShape(Nocturne78Design.artworkCorner)
    SkinCuratedScaffold(
        page = page,
        background = p.black,
        layout = CuratedSkinLayout(gutter, 16.dp, 22.dp, 140.dp, corner, p.panel),
        bottomInset = bottomInset,
        jumpPills = false,
        top = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LevyraBackButton(onClick = onDismiss, contentDescription = strings.back)
            }
        },
        intro = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (page.covers.isNotEmpty()) {
                    Column(Modifier.size(112.dp).clip(corner).background(p.panel).border(1.dp, p.outline, corner).clearAndSetSemantics { }) {
                        page.covers.take(4).chunked(2).forEach { pair ->
                            Row(Modifier.weight(1f)) {
                                pair.forEach { url -> AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight()) }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (page.total > 0) Text(
                        curatedCountLabel(page.total, strings.playlistsPlain).uppercase(), color = p.blue, style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.6.sp, maxLines = 1
                    )
                    Text(
                        page.title, color = p.text, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp).semantics { heading() }
                    )
                    Box(Modifier.padding(top = 12.dp).width(32.dp).height(2.dp).background(p.blue))
                }
            }
        },
        header = { row ->
            Column(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 26.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.shelf.title, color = p.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    Text(row.shelf.items.size.toString(), color = p.muted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(p.outline))
            }
        },
        cell = { item, _, modifier ->
            NocturnePlaylistCard(item.title, monoWithoutYoutube(item.subtitle), item.thumbnailUrl, modifier) { item.playlist?.let(onOpen) }
        }
    )
}
