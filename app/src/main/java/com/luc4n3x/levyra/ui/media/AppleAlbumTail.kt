package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumRelatedItem
import com.luc4n3x.levyra.domain.AlbumRelatedShelf
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

@Composable
internal fun AppleAlbumTail(
    footer: String,
    shelves: List<AlbumRelatedShelf>,
    colors: ImmersiveMediaColors,
    bottomSpace: androidx.compose.ui.unit.Dp,
    onOpen: (AlbumRelatedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val band = lerp(colors.base, Color.Black, AppleAlbumShelfDarken)
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(horizontal = AppleAlbumTailGutter), thickness = 0.5.dp, color = colors.hairline)
        if (footer.isNotBlank()) {
            Text(
                footer, color = colors.contentMuted, fontSize = 17.sp, lineHeight = 22.sp,
                modifier = Modifier.padding(start = AppleAlbumTailGutter, end = AppleAlbumTailGutter, top = 18.dp, bottom = 24.dp)
            )
        }
        Column(
            Modifier.fillMaxWidth().background(if (shelves.isNotEmpty()) band else colors.base).padding(top = 20.dp, bottom = 32.dp + bottomSpace),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            shelves.forEach { shelf -> AppleAlbumShelf(shelf, colors, onOpen) }
        }
    }
}

@Composable
private fun AppleAlbumShelf(shelf: AlbumRelatedShelf, colors: ImmersiveMediaColors, onOpen: (AlbumRelatedItem) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            shelf.title, color = colors.content, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = AppleAlbumTailGutter).semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppleAlbumTailGutter),
            horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
        ) {
            items(shelf.items, key = { "${shelf.title}-${it.id}-${it.url}" }) { item ->
                Column(Modifier.width(AppleAlbumCardWidth).clickable(role = Role.Button) { onOpen(item) }) {
                    AsyncImage(
                        model = item.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(AppleMusicDesign.artworkShape).background(colors.secondaryFill)
                    )
                    Text(
                        item.title, color = colors.content, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(item.subtitle, color = colors.contentMuted, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val AppleAlbumTailGutter = 16.dp
private val AppleAlbumCardWidth = 160.dp
private const val AppleAlbumShelfDarken = 0.14f
