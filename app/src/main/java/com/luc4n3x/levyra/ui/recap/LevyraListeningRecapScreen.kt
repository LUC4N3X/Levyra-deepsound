package com.luc4n3x.levyra.ui.recap

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.recap.Daypart
import com.luc4n3x.levyra.domain.recap.ListeningPulsePoint
import com.luc4n3x.levyra.domain.recap.ListeningRecapPeriod
import com.luc4n3x.levyra.domain.recap.ListeningRecapSummary
import com.luc4n3x.levyra.domain.recap.RecapHighlightStat
import com.luc4n3x.levyra.domain.recap.TopAlbumStat
import com.luc4n3x.levyra.domain.recap.TopArtistStat
import com.luc4n3x.levyra.domain.recap.TopTrackStat
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSkeletonBlock
import com.luc4n3x.levyra.ui.components.levyraConnectedRowSurface
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DayTextStyle

private val RecapPeriods = listOf(
    ListeningRecapPeriod.Days7,
    ListeningRecapPeriod.Days30,
    ListeningRecapPeriod.Days365,
    ListeningRecapPeriod.AllTime
)

@Composable
fun LevyraListeningRecapOverlay(
    recap: ListeningRecapSummary,
    period: ListeningRecapPeriod,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSelectPeriod: (ListeningRecapPeriod) -> Unit,
    onPlayTrack: ((TopTrackStat) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val isDark = MaterialTheme.colorScheme.background.run { (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }
    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(LevyraInk, LevyraBlack))
    } else {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background))
    }

    val style = LevyraConnectedDefaults.style(accent = LevyraCyan)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item(contentType = "recap-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back,
                            tint = LevyraText
                        )
                    }
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(
                            text = strings.listeningRecap,
                            color = LevyraText,
                            fontSize = 24.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(24.sp),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = strings.recapSubtitle,
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Period Selector Chips
            item(contentType = "recap-period-selector") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(style.gap)
                ) {
                    RecapPeriods.forEachIndexed { index, entry ->
                        RecapPeriodChip(
                            label = periodLabel(entry, strings),
                            selected = entry == period,
                            position = LevyraConnectedPosition.of(index, RecapPeriods.size),
                            style = style,
                            accent = LevyraCyan,
                            onClick = { onSelectPeriod(entry) }
                        )
                    }
                }
            }

            // Loading Skeleton vs Empty vs Content
            if (loading && !recap.hasSignal) {
                item(contentType = "recap-loading") {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LevyraSkeletonBlock(width = 240.dp, height = 32.dp, accent = LevyraCyan)
                        LevyraSkeletonBlock(width = 340.dp, height = 110.dp, accent = LevyraCyan)
                        LevyraSkeletonBlock(width = 340.dp, height = 80.dp, accent = LevyraCyan)
                        LevyraSkeletonBlock(width = 340.dp, height = 80.dp, accent = LevyraCyan)
                    }
                }
            } else if (!recap.hasSignal) {
                item(contentType = "recap-empty") {
                    RecapEmptyView(strings = strings)
                }
            } else {
                // Hero Section
                item(contentType = "recap-hero") {
                    RecapHeroCard(
                        recap = recap,
                        period = period,
                        strings = strings,
                        number = number,
                        isDark = isDark
                    )
                }

                // Highlights Section
                item(contentType = "recap-highlights-title") {
                    RecapSectionTitle(title = strings.highlightsTitle, icon = Icons.Rounded.AutoAwesome, accent = LevyraViolet)
                }
                item(contentType = "recap-highlights") {
                    RecapHighlightsGrid(
                        highlights = recap.highlights,
                        strings = strings,
                        locale = locale,
                        number = number,
                        isDark = isDark
                    )
                }

                // Top Tracks Section
                if (recap.topTracks.isNotEmpty()) {
                    item(contentType = "recap-tracks-title") {
                        RecapSectionTitle(title = strings.topTracksTitle, icon = Icons.Rounded.Headphones, accent = LevyraCyan)
                    }
                    itemsIndexed(
                        recap.topTracks,
                        key = { _, track -> "recap-track-${track.trackId}-${track.rank}" },
                        contentType = { _, _ -> "recap-track-row" }
                    ) { index, track ->
                        TopTrackRow(
                            track = track,
                            position = LevyraConnectedPosition.of(index, recap.topTracks.size),
                            style = style,
                            strings = strings,
                            onPlay = { onPlayTrack?.invoke(track) }
                        )
                    }
                }

                // Top Artists Section
                if (recap.topArtists.isNotEmpty()) {
                    item(contentType = "recap-artists-title") {
                        RecapSectionTitle(title = strings.topArtistsTitle, icon = Icons.Rounded.Person, accent = LevyraPink)
                    }
                    itemsIndexed(
                        recap.topArtists,
                        key = { _, artist -> "recap-artist-${artist.name}-${artist.rank}" },
                        contentType = { _, _ -> "recap-artist-row" }
                    ) { index, artist ->
                        TopArtistRow(
                            artist = artist,
                            position = LevyraConnectedPosition.of(index, recap.topArtists.size),
                            style = style,
                            strings = strings,
                            onOpen = { onOpenArtist?.invoke(artist.name) }
                        )
                    }
                }

                // Top Albums Section
                if (recap.topAlbums.isNotEmpty()) {
                    item(contentType = "recap-albums-title") {
                        RecapSectionTitle(title = strings.topAlbumsTitle, icon = Icons.Rounded.Album, accent = LevyraOrange)
                    }
                    itemsIndexed(
                        recap.topAlbums,
                        key = { _, album -> "recap-album-${album.title}-${album.rank}" },
                        contentType = { _, _ -> "recap-album-row" }
                    ) { index, album ->
                        TopAlbumRow(
                            album = album,
                            position = LevyraConnectedPosition.of(index, recap.topAlbums.size),
                            style = style,
                            strings = strings
                        )
                    }
                }

                // Daily Activity Pulse Timeline
                if (recap.dailyActivity.any { it.listenedMs > 0L }) {
                    item(contentType = "recap-pulse-title") {
                        RecapSectionTitle(title = strings.pulseProActivity, icon = Icons.Rounded.CalendarMonth, accent = LevyraCyan)
                    }
                    item(contentType = "recap-pulse-chart") {
                        RecapTimelineChart(
                            points = recap.dailyActivity,
                            locale = locale,
                            strings = strings,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RecapPeriodChip(
    label: String,
    selected: Boolean,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .levyraConnectedRowSurface(position, style, selected = selected)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Row,
                role = Role.Tab,
                onClickLabel = label
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else LevyraMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecapHeroCard(
    recap: ListeningRecapSummary,
    period: ListeningRecapPeriod,
    strings: LevyraStrings,
    number: NumberFormat,
    isDark: Boolean
) {
    val shape = RoundedCornerShape(26.dp)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)
    val panelBg = if (isDark) LevyraPanel.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = panelBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LevyraCyan.copy(alpha = if (isDark) 0.12f else 0.08f),
                            LevyraViolet.copy(alpha = if (isDark) 0.08f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Period Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LevyraCyan.copy(alpha = 0.14f))
                        .border(1.dp, LevyraCyan.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = periodLabel(period, strings).uppercase(),
                        color = LevyraCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                // Dominant Metric (Minutes Listened)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatRecapDuration(recap.totalListenMs, strings),
                        color = LevyraText,
                        fontSize = 38.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(38.sp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                    Text(
                        text = strings.pulseMinutes,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Secondary Stats Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecapMiniStat(
                        icon = Icons.Rounded.PlayArrow,
                        value = number.format(recap.totalPlays),
                        label = strings.pulsePlays,
                        accent = LevyraCyan,
                        isDark = isDark
                    )
                    RecapMiniStat(
                        icon = Icons.Rounded.MusicNote,
                        value = number.format(recap.uniqueTracks),
                        label = strings.statTracks,
                        accent = LevyraViolet,
                        isDark = isDark
                    )
                    RecapMiniStat(
                        icon = Icons.Rounded.Person,
                        value = number.format(recap.uniqueArtists),
                        label = strings.statArtists,
                        accent = LevyraPink,
                        isDark = isDark
                    )
                    if (recap.uniqueAlbums > 0) {
                        RecapMiniStat(
                            icon = Icons.Rounded.Album,
                            value = number.format(recap.uniqueAlbums),
                            label = strings.albumsPlain,
                            accent = LevyraOrange,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RecapMiniStat(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    isDark: Boolean = true
) {
    val bg = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.03f)
    val border = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Text(text = value, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecapHighlightsGrid(
    highlights: RecapHighlightStat,
    strings: LevyraStrings,
    locale: Locale,
    number: NumberFormat,
    isDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Streak + Favorite Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                accent = LevyraOrange,
                title = strings.streakHighlight,
                headline = "${highlights.currentStreakDays}d",
                subtitle = if (highlights.bestStreakDays > 0) "Max: ${highlights.bestStreakDays}d" else "",
                isDark = isDark
            )
            val daypartName = formatDaypart(highlights.favoriteDaypart, strings.code)
            val hourText = if (highlights.favoriteHour >= 0) "${highlights.favoriteHour.toString().padStart(2, '0')}:00" else ""
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Schedule,
                accent = LevyraViolet,
                title = strings.favoriteTimeHighlight,
                headline = daypartName,
                subtitle = if (hourText.isNotBlank()) "${strings.pulseProPeak}: $hourText" else "",
                isDark = isDark
            )
        }

        // Row 2: Most Active Day + Discovery vs Repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val mostActiveText = highlights.mostActiveDayDate?.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            ) ?: ""
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
                accent = LevyraCyan,
                title = strings.mostActiveDayHighlight,
                headline = if (highlights.mostActiveDayMinutes > 0) "${highlights.mostActiveDayMinutes}m" else "—",
                subtitle = mostActiveText,
                isDark = isDark
            )
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Explore,
                accent = LevyraPink,
                title = strings.discoveryHighlight,
                headline = "${highlights.discoveryRate}%",
                subtitle = "${highlights.repeatRate}% ${strings.pulsePlays}",
                isDark = isDark
            )
        }

        // Row 3: Most Replayed Track (if any)
        if (highlights.mostReplayedTrack != null) {
            val track = highlights.mostReplayedTrack
            RecapHighlightCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Replay,
                accent = Color(0xFFFFC857),
                title = strings.replayHighlight,
                headline = track.title,
                subtitle = "${track.artist} · ${track.plays} ${strings.pulsePlays}",
                isDark = isDark
            )
        }
    }
}

@Composable
private fun RecapHighlightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accent: Color,
    title: String,
    headline: String,
    subtitle: String,
    isDark: Boolean
) {
    val cardShape = RoundedCornerShape(18.dp)
    val panelBg = if (isDark) LevyraPanel.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)

    Surface(
        modifier = modifier,
        shape = cardShape,
        color = panelBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Text(
                    text = title,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = headline,
                color = LevyraText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = LevyraMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopTrackRow(
    track: TopTrackStat,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    strings: LevyraStrings,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
            .levyraPressable(
                onClick = onPlay,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank Indicator
        Text(
            text = "#${track.rank}",
            color = if (track.rank == 1) LevyraCyan else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        // Artwork
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = LevyraMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Title and Artist
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Listening Stat
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${track.plays} ${strings.pulsePlays}",
                color = LevyraCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${track.totalMinutes} ${strings.pulseMinuteShort}",
                color = LevyraMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TopArtistRow(
    artist: TopArtistStat,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    strings: LevyraStrings,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
            .levyraPressable(
                onClick = onOpen,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = artist.name
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#${artist.rank}",
            color = if (artist.rank == 1) LevyraPink else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LevyraPink.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = artist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = LevyraPink,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = artist.name,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.trackCount} ${strings.statTracks}",
                color = LevyraMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${artist.plays} ${strings.pulsePlays}",
                color = LevyraPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${artist.totalMinutes} ${strings.pulseMinuteShort}",
                color = LevyraMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TopAlbumRow(
    album: TopAlbumStat,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    strings: LevyraStrings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#${album.rank}",
            color = if (album.rank == 1) LevyraOrange else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LevyraOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (album.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = LevyraOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.title,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                color = LevyraMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${album.plays} ${strings.pulsePlays}",
                color = LevyraOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${album.totalMinutes} ${strings.pulseMinuteShort}",
                color = LevyraMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecapTimelineChart(
    points: List<ListeningPulsePoint>,
    locale: Locale,
    strings: LevyraStrings,
    isDark: Boolean
) {
    val peakMs = points.maxOfOrNull { it.listenedMs } ?: 0L
    val panelBg = if (isDark) LevyraPanel.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = panelBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val fraction = if (peakMs > 0L) {
                        (point.listenedMs.toFloat() / peakMs.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        0.08f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (point.listenedMs == peakMs && peakMs > 0L) {
                                    LevyraCyan
                                } else if (point.listenedMs > 0L) {
                                    LevyraCyan.copy(alpha = 0.65f)
                                } else {
                                    Color.White.copy(alpha = 0.06f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecapSectionTitle(
    title: String,
    icon: ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            color = LevyraText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun RecapEmptyView(strings: LevyraStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(LevyraCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = strings.emptyRecapTitle,
            color = LevyraText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = strings.emptyRecapSubtitle,
            color = LevyraMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun periodLabel(period: ListeningRecapPeriod, strings: LevyraStrings): String = when (period) {
    ListeningRecapPeriod.Days7 -> strings.recapPeriod7Days
    ListeningRecapPeriod.Days30 -> strings.recapPeriod30Days
    ListeningRecapPeriod.Days365 -> strings.recapPeriod365Days
    ListeningRecapPeriod.AllTime -> strings.recapPeriodAllTime
}

private fun formatRecapDuration(listenedMs: Long, strings: LevyraStrings): String {
    val totalMinutes = listenedMs / 60_000L
    val hours = totalMinutes / 60L
    val remainingMinutes = totalMinutes % 60L
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "$totalMinutes ${strings.pulseMinuteShort}"
    }
}

private fun formatDaypart(daypart: Daypart, code: String): String = when (code) {
    "it" -> when (daypart) {
        Daypart.Morning -> "Mattina"
        Daypart.Afternoon -> "Pomeriggio"
        Daypart.Evening -> "Sera"
        Daypart.Night -> "Notte"
    }
    "es" -> when (daypart) {
        Daypart.Morning -> "Mañana"
        Daypart.Afternoon -> "Tarde"
        Daypart.Evening -> "Noche"
        Daypart.Night -> "Madrugada"
    }
    "fr" -> when (daypart) {
        Daypart.Morning -> "Matin"
        Daypart.Afternoon -> "Après-midi"
        Daypart.Evening -> "Soirée"
        Daypart.Night -> "Nuit"
    }
    "de" -> when (daypart) {
        Daypart.Morning -> "Morgen"
        Daypart.Afternoon -> "Nachmittag"
        Daypart.Evening -> "Abend"
        Daypart.Night -> "Nacht"
    }
    "pt" -> when (daypart) {
        Daypart.Morning -> "Manhã"
        Daypart.Afternoon -> "Tarde"
        Daypart.Evening -> "Noite"
        Daypart.Night -> "Madrugada"
    }
    "ru" -> when (daypart) {
        Daypart.Morning -> "Утро"
        Daypart.Afternoon -> "День"
        Daypart.Evening -> "Вечер"
        Daypart.Night -> "Ночь"
    }
    "ja" -> when (daypart) {
        Daypart.Morning -> "朝"
        Daypart.Afternoon -> "昼"
        Daypart.Evening -> "夕方"
        Daypart.Night -> "夜"
    }
    else -> when (daypart) {
        Daypart.Morning -> "Morning"
        Daypart.Afternoon -> "Afternoon"
        Daypart.Evening -> "Evening"
        Daypart.Night -> "Night"
    }
}
