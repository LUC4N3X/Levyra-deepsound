package com.luc4n3x.levyra.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.EditorialChartsRepository
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val GLOBAL_MARKET = "GLOBAL"
private const val CHART_SEEN_PREFS = "levyra_spotify_chart_entries"

internal object SpotifyTopChartNavigator {
    var market by mutableStateOf<String?>(null)
}

internal fun spotifyTopChartCoverUrl(market: String): String =
    "https://charts-images.scdn.co/assets/locale_en/regional/daily/region_${market.lowercase(Locale.ROOT)}_default.jpg"

internal fun spotifyTopChartCountry(market: String, languageCode: String): String {
    val locale = Locale.forLanguageTag(languageCode)
    return if (market.equals(GLOBAL_MARKET, ignoreCase = true)) {
        LevyraSpotifyChartStrings.globalName(languageCode)
    } else {
        Locale("", market).getDisplayCountry(locale).ifBlank { market }
    }
}

@Composable
private fun rememberSpotifyTopCharts(): Map<String, List<Track>> {
    val context = LocalContext.current
    val charts by produceState(emptyMap<String, List<Track>>(), context) {
        value = runCatching { EditorialChartsRepository.get(context).cachedAllMarkets(50) }.getOrDefault(emptyMap())
    }
    return charts
}

private fun orderedMarkets(markets: Set<String>): List<String> {
    val home = Locale.getDefault().country.uppercase(Locale.ROOT)
    return buildList {
        if (home in markets) add(home)
        if (GLOBAL_MARKET in markets) add(GLOBAL_MARKET)
        markets.filterNot { it == home || it == GLOBAL_MARKET }.forEach(::add)
    }
}

@Composable
internal fun SpotifyTopChartsShelf(title: String, inset: Dp) {
    val charts = rememberSpotifyTopCharts()
    if (charts.isEmpty()) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifyChartStrings.forCode(strings.code) }
    val markets = remember(charts.keys) { orderedMarkets(charts.keys) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title, color = SpotifyDesign.text, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = inset).semantics { heading() }
        )
        LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(markets, key = { "spotify-top50-$it" }) { market ->
                val description = copy.chartDescription.format(spotifyTopChartCountry(market, strings.code))
                Column(
                    Modifier.width(SpotifyTopChartCardSize)
                        .levyraPressable(onClick = { SpotifyTopChartNavigator.market = market }, onClickLabel = description, role = Role.Button)
                ) {
                    StableRemoteArtwork(
                        spotifyTopChartCoverUrl(market), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(SpotifyTopChartCardSize).clip(RoundedCornerShape(4.dp)).background(SpotifyDesign.placeholder)
                    )
                    Text(
                        description, color = SpotifyDesign.subdued, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

private val SpotifyTopChartCardSize = 152.dp
private val SpotifyChartCoverSize = 244.dp
private val SpotifyChartTopBar = 64.dp

@Composable
internal fun SpotifyTopChartOverlay(
    currentId: String?,
    isPlaying: Boolean,
    bottomInset: Dp,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val market = SpotifyTopChartNavigator.market ?: return
    val charts = rememberSpotifyTopCharts()
    val tracks = charts[market].orEmpty()
    SpotifyTopChartScreen(market, tracks, currentId, isPlaying, bottomInset, onPlay, onTrackActions) {
        SpotifyTopChartNavigator.market = null
    }
}

@Composable
private fun SpotifyTopChartScreen(
    market: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    bottomInset: Dp,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifyChartStrings.forCode(strings.code) }
    val country = remember(market, strings.code) { spotifyTopChartCountry(market, strings.code) }
    val cover = remember(market) { spotifyTopChartCoverUrl(market) }
    val coverTrack = remember(market, tracks) {
        tracks.firstOrNull()?.copy(id = "spotify-top50-$market", thumbnailUrl = cover, largeThumbnailUrl = cover)
    }
    val newEntries = rememberChartNewEntries(market, tracks)
    val date = remember(strings.code) {
        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(strings.code)))
    }
    val coverPx = with(LocalDensity.current) { SpotifyChartCoverSize.toPx() }
    SpotifyPlaylistPage(
        title = "Top 50 - $country",
        toneTrack = coverTrack,
        fallbackTone = Color(0xFF2F6FC7),
        heroCollapsePx = coverPx,
        showFindBar = true,
        description = copy.chartDescription.format(country),
        ownerLabel = "Spotify",
        meta = listOfNotNull(date, newEntries.takeIf { it > 0 }?.let(copy::newEntries)).joinToString(" \u2022 "),
        thumbUrl = cover,
        tracks = tracks,
        currentId = currentId,
        isPlaying = isPlaying,
        bottomInset = bottomInset,
        onPlay = onPlay,
        onTrackActions = onTrackActions,
        onDismiss = onDismiss
    ) { headerScroll, progress ->
        Box(Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
            StableRemoteArtwork(
                cover, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(SpotifyChartCoverSize)
                    .graphicsLayer {
                        val scale = 1f - progress * 0.3f
                        scaleX = scale
                        scaleY = scale
                        translationY = headerScroll.coerceAtMost(coverPx) * 0.55f
                        alpha = 1f - progress
                    }
                    .shadow(24.dp, RoundedCornerShape(2.dp))
                    .background(SpotifyDesign.placeholder)
            )
        }
    }
}

@Composable
internal fun SpotifyPlaylistPage(
    title: String,
    toneTrack: Track?,
    fallbackTone: Color,
    heroCollapsePx: Float,
    showFindBar: Boolean,
    description: String,
    ownerLabel: String,
    meta: String,
    thumbUrl: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    bottomInset: Dp,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    onDismiss: () -> Unit,
    descriptionContent: (@Composable () -> Unit)? = null,
    heroBehindTopBar: Boolean = false,
    hero: @Composable (headerScroll: Float, progress: Float) -> Unit
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifyChartStrings.forCode(strings.code) }
    val tone by if (toneTrack != null) rememberSpotifyCoverTone(toneTrack) { lerp(it, Color.White, 0.08f) } else remember { mutableStateOf(fallbackTone) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coverPx = heroCollapsePx
    val headerScroll by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE else listState.firstVisibleItemScrollOffset.toFloat() }
    }
    val coverProgress = (headerScroll / coverPx).coerceIn(0f, 1f)
    var playRowY by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val topBarPx = with(density) { SpotifyChartTopBar.toPx() }
    var statusBarPx by remember { mutableIntStateOf(0) }
    val pinned = listState.firstVisibleItemIndex > 0 || playRowY <= topBarPx + statusBarPx
    val barAlpha = ((headerScroll - coverPx * 0.6f) / (coverPx * 0.4f)).coerceIn(0f, 1f)
    val playTracks: () -> Unit = { tracks.firstOrNull()?.let { onPlay(tracks, it) } }

    Box(Modifier.fillMaxSize().background(SpotifyDesign.base)) {
        Box(
            Modifier.fillMaxWidth().height(560.dp).graphicsLayer { translationY = -headerScroll.coerceAtMost(2000f) * 0.5f }
                .background(Brush.verticalGradient(0f to tone, 1f to SpotifyDesign.base))
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 16.dp)
        ) {
            item(key = "chart-header") {
                Column(Modifier.fillMaxWidth().then(if (heroBehindTopBar) Modifier else Modifier.statusBarsPadding().padding(top = SpotifyChartTopBar))) {
                    if (showFindBar) SpotifyPlaylistFindBar(copy.findInPlaylist, copy.sort)
                    hero(headerScroll, coverProgress)
                    if (descriptionContent != null) descriptionContent() else Text(
                        description, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    )
                    Row(Modifier.padding(start = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SpotifyLogoBadge(Modifier.size(24.dp))
                        Text(ownerLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        meta,
                        color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    SpotifyPlaylistActions(thumbUrl, tracks, pinned, onPlay, playTracks) { playRowY = it }
                }
            }
            itemsIndexed(tracks, key = { index, track -> "chart-row-$index-${track.id}" }) { index, track ->
                val active = track.id == currentId
                SpotifyPlaylistTrackRow(index, track, active, isPlaying, onPlay = { onPlay(tracks, track) }, onActions = { onTrackActions(track) })
            }
        }
        SpotifyPlaylistTopBar(title, tone, barAlpha, pinned, playTracks, onDismiss) { statusBarPx = it }
    }
}

@Composable
private fun SpotifyPlaylistFindBar(findLabel: String, sortLabel: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(findLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
        }
        Box(
            Modifier.height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text(sortLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SpotifyPlaylistActions(
    thumbUrl: String,
    tracks: List<Track>,
    pinned: Boolean,
    onPlay: (List<Track>, Track) -> Unit,
    playTracks: () -> Unit,
    onRowPositioned: (Int) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).height(56.dp)
            .onGloballyPositioned { onRowPositioned(it.positionInRoot().y.toInt()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 28.dp, height = 36.dp)) {
            StableRemoteArtwork(
                thumbUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.offset(x = 4.dp).size(width = 24.dp, height = 34.dp).clip(RoundedCornerShape(3.dp)).graphicsLayer { alpha = 0.6f }
            )
            StableRemoteArtwork(
                tracks.firstOrNull()?.thumbnailUrl.orEmpty(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 24.dp, height = 36.dp).clip(RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.width(20.dp))
        listOf(Icons.Rounded.AddCircleOutline, Icons.Rounded.ArrowCircleDown, Icons.Rounded.Share).forEach { icon ->
            Icon(icon, null, tint = SpotifyDesign.subdued, modifier = Modifier.padding(end = 20.dp).size(26.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(48.dp).levyraPressable(onClick = {
                val shuffled = tracks.shuffled()
                shuffled.firstOrNull()?.let { onPlay(shuffled, it) }
            }, onClickLabel = strings.shuffle, role = Role.Button),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Shuffle, strings.shuffle, tint = SpotifyDesign.green, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.width(8.dp))
        SpotifyChartPlayButton(Modifier.graphicsLayer { alpha = if (pinned) 0f else 1f }, strings.playAll, playTracks)
    }
}

@Composable
private fun SpotifyPlaylistTrackRow(
    index: Int,
    track: Track,
    active: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onActions: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().height(64.dp).levyraPressable(onClick = onPlay, onClickLabel = track.title, role = Role.Button)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (active) LevyraPlayingIndicator(playing = isPlaying, color = SpotifyDesign.green, size = 12.dp)
            else Text((index + 1).toString(), color = SpotifyDesign.text, fontSize = 15.sp)
        }
        Box(Modifier.size(48.dp).background(SpotifyDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (active) SpotifyDesign.greenText else SpotifyDesign.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(48.dp).levyraPressable(onClick = onActions, onClickLabel = strings.options, role = Role.Button),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.MoreVert, strings.options, tint = SpotifyDesign.subdued) }
    }
}

@Composable
private fun SpotifyPlaylistTopBar(
    title: String,
    tone: Color,
    barAlpha: Float,
    pinned: Boolean,
    playTracks: () -> Unit,
    onDismiss: () -> Unit,
    onPositioned: (Int) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box(
        Modifier.fillMaxWidth().background(lerp(tone, Color.Black, 0.45f).copy(alpha = barAlpha))
            .onGloballyPositioned { onPositioned(it.positionInRoot().y.toInt()) }
            .statusBarsPadding().height(SpotifyChartTopBar)
    ) {
        Box(
            Modifier.padding(start = 12.dp).align(Alignment.CenterStart).size(40.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f * (1f - barAlpha))).levyraPressable(onClick = onDismiss, onClickLabel = strings.back, role = Role.Button),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = Color.White) }
        Text(
            title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 64.dp, end = 120.dp).graphicsLayer { alpha = barAlpha }
        )
        Box(
            Modifier.padding(end = 12.dp).align(Alignment.CenterEnd).size(40.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f * (1f - barAlpha))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.MoreVert, strings.options, tint = Color.White) }
        if (pinned) SpotifyChartPlayButton(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp).offset(y = 28.dp), strings.playAll, playTracks
        )
    }
}

@Composable
private fun SpotifyChartPlayButton(modifier: Modifier, label: String, onClick: () -> Unit) {
    Box(
        modifier.size(56.dp).clip(CircleShape).background(SpotifyDesign.green)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Rounded.PlayArrow, label, tint = Color.Black, modifier = Modifier.size(32.dp)) }
}

@Composable
private fun SpotifyLogoBadge(modifier: Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.luc4n3x.levyra.R.drawable.levyra_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}

@Composable
private fun rememberChartNewEntries(market: String, tracks: List<Track>): Int {
    val context = LocalContext.current
    var count by remember(market) { mutableIntStateOf(0) }
    LaunchedEffect(market, tracks) {
        if (tracks.isEmpty()) return@LaunchedEffect
        val prefs = context.getSharedPreferences(CHART_SEEN_PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val ids = tracks.map { "${it.title}\u001f${it.artist}".lowercase(Locale.ROOT) }.toSet()
        val savedDate = prefs.getString("$market.date", null)
        val previous = if (savedDate == today) prefs.getStringSet("$market.previous", emptySet()).orEmpty()
        else prefs.getStringSet("$market.current", emptySet()).orEmpty()
        if (savedDate != today) {
            prefs.edit().putString("$market.date", today).putStringSet("$market.previous", previous).putStringSet("$market.current", ids).apply()
        }
        count = if (previous.isEmpty()) 0 else ids.count { it !in previous }
    }
    return count
}
