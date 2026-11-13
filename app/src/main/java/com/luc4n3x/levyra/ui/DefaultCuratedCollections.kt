package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.CuratedCollection
import com.luc4n3x.levyra.data.CuratedCollectionCard
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.ui.i18n.curatedCollectionsCopy
import com.luc4n3x.levyra.viewmodel.DefaultCuratedCollectionsViewModel

@Composable
internal fun rememberDefaultCuratedCollectionShelves(code: String): List<YoutubeMusicHomeShelf> {
    val collectionsModel = viewModel<DefaultCuratedCollectionsViewModel>(key = "default-curated-collections")
    val collections by collectionsModel.collections.collectAsStateWithLifecycle()
    LaunchedEffect(code) { collectionsModel.load(code) }
    return remember(collections, code) {
        val copy = curatedCollectionsCopy(code)
        listOfNotNull(
            curatedCollectionShelf(CuratedCollection.Everyday, copy.everydayTitle, copy.everydaySubtitle, collections[CuratedCollection.Everyday].orEmpty()),
            curatedCollectionShelf(CuratedCollection.Decades, copy.decadesTitle, copy.decadesSubtitle, collections[CuratedCollection.Decades].orEmpty())
        )
    }
}

internal fun curatedCollectionShelf(collection: CuratedCollection, title: String, subtitle: String, cards: List<CuratedCollectionCard>): YoutubeMusicHomeShelf? {
    if (cards.isEmpty()) return null
    return YoutubeMusicHomeShelf(
        key = "collection-${collection.cacheName}",
        title = title,
        strapline = subtitle,
        thumbnailUrl = cards.first().thumbnailUrl,
        style = YoutubeMusicHomeShelfStyle.Cards,
        items = cards.map { card ->
            YoutubeMusicHomeItem(
                key = "collection-${collection.cacheName}-${card.playlistId}",
                kind = YoutubeMusicHomeItemKind.Playlist,
                title = card.title,
                subtitle = card.subtitle,
                thumbnailUrl = card.thumbnailUrl,
                playlist = PlaylistHit(title = card.title, author = card.subtitle, thumbnailUrl = card.thumbnailUrl, playlistId = card.playlistId)
            )
        }
    )
}
