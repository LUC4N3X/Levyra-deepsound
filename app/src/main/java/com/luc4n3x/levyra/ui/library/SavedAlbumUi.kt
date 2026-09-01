package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.data.SavedAlbumsStore
import com.luc4n3x.levyra.data.savedAlbumIdentityKey
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SavedAlbum
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.launch

@Composable
internal fun rememberSavedLibraryAlbums(derivedAlbums: List<LibraryAlbum>): List<LibraryAlbum> {
    val context = LocalContext.current
    val store = remember(context) { SavedAlbumsStore(context) }
    val savedAlbums by store.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    return remember(derivedAlbums, savedAlbums) {
        mergeSavedLibraryAlbums(derivedAlbums, savedAlbums)
    }
}

internal fun mergeSavedLibraryAlbums(
    derivedAlbums: List<LibraryAlbum>,
    savedAlbums: List<SavedAlbum>
): List<LibraryAlbum> {
    val derivedByIdentity = derivedAlbums.associateBy { savedAlbumIdentityKey(it.toAlbumHit()) }
    val savedIdentities = savedAlbums.mapTo(linkedSetOf()) { savedAlbumIdentityKey(it.album) }
    val savedRows = savedAlbums.map { saved ->
        val album = saved.album
        val identity = savedAlbumIdentityKey(album)
        val derived = derivedByIdentity[identity]
        LibraryAlbum(
            key = identity,
            title = album.title,
            artist = album.artist,
            year = album.year.ifBlank { album.releaseDate.take(4) },
            artworkUrl = album.thumbnailUrl.ifBlank { derived?.artworkUrl.orEmpty() },
            browseId = album.browseId.ifBlank { derived?.browseId.orEmpty() },
            explicit = album.explicit || derived?.explicit == true,
            tracks = derived?.tracks.orEmpty()
        )
    }
    return savedRows + derivedAlbums.filterNot { savedAlbumIdentityKey(it.toAlbumHit()) in savedIdentities }
}

@Composable
fun BoxScope.SavedAlbumBookmarkOverlay(
    album: AlbumHit?,
    visible: Boolean,
    languageCode: String
) {
    if (!visible || album == null) return
    val context = LocalContext.current
    val store = remember(context) { SavedAlbumsStore(context) }
    val savedAlbums by store.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val albumKey = remember(album) { savedAlbumIdentityKey(album) }
    val saved = remember(savedAlbums, albumKey) {
        savedAlbums.any { savedAlbumIdentityKey(it.album) == albumKey }
    }
    val strings = remember(languageCode) { LevyraStrings.forCode(languageCode) }

    Surface(
        onClick = { scope.launch { store.toggle(album) } },
        shape = CircleShape,
        color = LevyraGlass,
        border = BorderStroke(1.dp, LevyraGlassBorder),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .zIndex(100f)
            .statusBarsPadding()
            .padding(top = 8.dp, end = 76.dp)
            .size(48.dp)
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (saved) {
                    "${strings.remove} ${strings.albumPlain}"
                } else {
                    "${strings.save} ${strings.albumPlain}"
                },
                tint = if (saved) LevyraPink else LevyraText,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
