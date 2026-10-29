package com.luc4n3x.levyra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.qobuz.QobuzAlbum
import com.luc4n3x.levyra.data.qobuz.QobuzPlaylist
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign

internal object QobuzText {
    val pageTitle: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium, color = QobuzDesign.textPrimary)
    val sectionTitle: TextStyle @Composable get() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = QobuzDesign.textPrimary)
    val baseline: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge.copy(color = QobuzDesign.textSecondary)
    val cardTitle: TextStyle @Composable get() = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = QobuzDesign.textPrimary)
    val cardSubtitle: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 18.sp, color = QobuzDesign.textSecondary)
    val category: TextStyle @Composable get() = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = QobuzDesign.textAccent)
}

@Composable
internal fun QobuzPageHeader(title: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().height(QobuzDesign.topBarHeight).padding(start = QobuzDesign.gutter, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = QobuzText.pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() })
        actions()
    }
}

@Composable
internal fun QobuzIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(26.dp))
    }
}

@Composable
internal fun QobuzChip(label: String, selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val background = if (selected) QobuzDesign.buttonPrimary else QobuzDesign.surfaceSecondary
    val content = if (selected) QobuzDesign.buttonPrimaryContent else QobuzDesign.textPrimary
    Box(
        Modifier
            .heightIn(min = QobuzDesign.chipHeight)
            .clip(QobuzDesign.chipShape)
            .background(background)
            .border(1.dp, if (selected) background else QobuzDesign.border, QobuzDesign.chipShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (icon != null) 22.dp else QobuzDesign.chipHorizontal, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, label, tint = content, modifier = Modifier.size(20.dp))
        else Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal), color = content, maxLines = 1)
    }
}

@Composable
internal fun QobuzSectionHeader(
    title: String,
    baseline: String?,
    seeAllLabel: String,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = QobuzText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() })
            if (onSeeAll != null) {
                Spacer(Modifier.width(12.dp))
                QobuzSeeAll(seeAllLabel, onSeeAll)
            }
        }
        if (!baseline.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(baseline, style = QobuzText.baseline)
        }
    }
}

@Composable
private fun QobuzSeeAll(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.height(QobuzDesign.seeAllHeight).clip(QobuzDesign.buttonShape).background(QobuzDesign.surfaceSecondary)
                .border(1.dp, QobuzDesign.border, QobuzDesign.buttonShape).padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = QobuzDesign.textPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun QobuzArtworkBox(url: String, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape = QobuzDesign.cardShape, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.clip(shape).background(QobuzDesign.placeholder)) {
        if (url.isNotBlank()) coil3.compose.AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.MusicNote, null, tint = QobuzDesign.placeholderIcon, modifier = Modifier.align(Alignment.Center).size(40.dp))
        content()
    }
}

@Composable
internal fun QobuzHiResBadge(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(2.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 2.dp, vertical = 1.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hi·Res", color = QobuzDesign.hiResGold, fontSize = 7.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Box(Modifier.width(22.dp).height(7.dp).background(QobuzDesign.hiResGold), contentAlignment = Alignment.Center) {
            Text("AUDIO", color = Color(0xFF1A1A1A), fontSize = 5.sp, lineHeight = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun QobuzExplicitBadge(label: String) {
    Box(
        Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(QobuzDesign.textSecondary.copy(alpha = 0.55f))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text("E", color = QobuzDesign.textInvert, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun QobuzAlbumCard(album: QobuzAlbum, copy: QobuzSkinCopy, width: Dp, onClick: () -> Unit, showAward: Boolean = false) {
    Column(Modifier.cardWidth(width).clickable(role = Role.Button, onClick = onClick)) {
        QobuzArtworkBox(album.coverUrl, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(album.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (album.hiRes) {
                Spacer(Modifier.width(6.dp))
                QobuzHiResBadge(copy.hiRes)
            }
        }
        if (showAward && album.award.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                album.award, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = QobuzDesign.textAccent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(QobuzDesign.buttonShape).border(1.dp, QobuzDesign.textAccent, QobuzDesign.buttonShape)
                    .background(QobuzDesign.surfaceAccent).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun QobuzPlaylistCard(playlist: QobuzPlaylist, caption: String, width: Dp, onClick: () -> Unit) =
    QobuzPlaylistCard(playlist.title, playlist.imageUrl, caption, width, onClick)

@Composable
internal fun QobuzPlaylistCard(title: String, imageUrl: String, caption: String, width: Dp, onClick: () -> Unit) {
    val playlist = QobuzPlaylist("", "", title, imageUrl)
    Column(Modifier.cardWidth(width).clickable(role = Role.Button, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(QobuzDesign.cardShape).background(Color(0xFF2B2522))) {
            if (playlist.imageUrl.isNotBlank()) {
                coil3.compose.AsyncImage(
                    playlist.imageUrl, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(18.dp), alpha = 0.55f
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0x99000000)))))
                QobuzArtworkBox(
                    playlist.imageUrl,
                    Modifier.align(Alignment.Center).offset(y = (-8).dp).fillMaxWidth(0.89f).aspectRatio(2.15f),
                    RoundedCornerShape(4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = QobuzText.category, maxLines = 1)
    }
}

@Composable
internal fun QobuzCategoryTile(title: String, colors: Pair<Color, Color>, width: Dp, onClick: () -> Unit) {
    Box(
        Modifier.width(width).height(QobuzDesign.tagCardHeight).clip(RoundedCornerShape(QobuzDesign.tagCorner))
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height * 1.05f
            val center = Offset(size.width * 0.86f, -size.height * 0.18f)
            drawCircle(Color.Black.copy(alpha = 0.34f), radius, center)
            var ring = radius * 0.94f
            while (ring > radius * 0.42f) {
                drawCircle(Color.White.copy(alpha = 0.035f), ring, center, style = Stroke(0.8.dp.toPx()))
                ring -= 3.dp.toPx()
            }
            drawCircle(colors.first.copy(alpha = 0.55f), radius * 0.3f, center)
            drawCircle(Color.Black.copy(alpha = 0.5f), radius * 0.04f, center)
        }
        Text(
            title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
internal fun QobuzArtistCard(artist: ArtistHit, copy: QobuzSkinCopy, followed: Boolean, onOpen: () -> Unit, onFollow: () -> Unit) {
    Column(Modifier.width(QobuzDesign.artistCircle), horizontalAlignment = Alignment.CenterHorizontally) {
        QobuzArtworkBox(
            artist.thumbnailUrl, Modifier.size(QobuzDesign.artistCircle).clickable(role = Role.Button, onClickLabel = artist.name, onClick = onOpen),
            CircleShape
        )
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(copy.artist, style = QobuzText.cardSubtitle, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.heightIn(min = 48.dp).clickable(role = Role.Switch, onClick = onFollow).semantics { selected = followed },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.height(32.dp).clip(QobuzDesign.buttonShape)
                    .background(if (followed) QobuzDesign.buttonPrimary else QobuzDesign.surfaceSecondary)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tint = if (followed) QobuzDesign.buttonPrimaryContent else QobuzDesign.textPrimary
                Icon(if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd, null, tint = tint, modifier = Modifier.size(18.dp))
                Text(if (followed) copy.following else copy.follow, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = tint)
            }
        }
    }
}

@Composable
internal fun QobuzRankedRow(rank: Int?, track: Track, copy: QobuzSkinCopy, current: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.height(QobuzDesign.rankRow + 8.dp).combinedClickableCompat(onClick, onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                "%02d".format(rank), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium),
                color = QobuzDesign.textPrimary, modifier = Modifier.widthIn(min = QobuzDesign.rankNumberWidth)
            )
        }
        Box(Modifier.size(QobuzDesign.rankArtwork).clip(QobuzDesign.artworkShape).background(QobuzDesign.placeholder)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = if (current) QobuzDesign.textAccent else QobuzDesign.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    QobuzExplicitBadge(copy.explicit)
                    Spacer(Modifier.width(6.dp))
                }
                Text(track.artist, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun QobuzTrackCard(track: Track, width: Dp, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.width(width).combinedClickableCompat(onClick, onLongClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(QobuzDesign.cardShape).background(QobuzDesign.placeholder)) {
            CoverImage(track, Modifier.fillMaxSize(), highRes = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun QobuzPlayCircle(label: String, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color.Black).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PlayArrow, label, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

internal val QobuzHomeIcon: androidx.compose.ui.graphics.vector.ImageVector get() = Icons.Rounded.Home

private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))

internal val QobuzRowPadding = PaddingValues(horizontal = QobuzDesign.gutter)

private val QobuzTagGradients = listOf(
    Color(0xFFE3A532) to Color(0xFFE2702B),
    Color(0xFF8A2BE2) to Color(0xFF4510D2),
    Color(0xFFD3203A) to Color(0xFF8E1B2E),
    Color(0xFFE2541B) to Color(0xFFE0A33A),
    Color(0xFF9C3B78) to Color(0xFFC0487F),
    Color(0xFF3E55C9) to Color(0xFF6A4FCF),
    Color(0xFF1F8A70) to Color(0xFF136050),
    Color(0xFFB8486B) to Color(0xFF7A2D58)
)

internal fun qobuzTagPalette(identity: String): Pair<Color, Color> =
    QobuzTagGradients[(identity.lowercase().hashCode() and Int.MAX_VALUE) % QobuzTagGradients.size]

private fun Modifier.cardWidth(width: Dp): Modifier = if (width == Dp.Unspecified) fillMaxWidth() else width(width)
