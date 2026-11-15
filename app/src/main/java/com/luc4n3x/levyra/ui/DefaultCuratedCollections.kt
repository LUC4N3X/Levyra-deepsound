package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.CuratedCollection
import com.luc4n3x.levyra.data.CuratedCollectionCard
import com.luc4n3x.levyra.domain.ExploreCategory
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
            curatedCollectionShelf(CuratedCollection.Decades, copy.decadesTitle, copy.decadesSubtitle, collections[CuratedCollection.Decades].orEmpty()),
            curatedCollectionShelf(CuratedCollection.Everyday, copy.everydayTitle, copy.everydaySubtitle, collections[CuratedCollection.Everyday].orEmpty())
        )
    }
}

@Composable
internal fun rememberDecadeCategories(code: String): List<ExploreCategory> {
    val collectionsModel = viewModel<DefaultCuratedCollectionsViewModel>(key = "default-curated-collections")
    val collections by collectionsModel.collections.collectAsStateWithLifecycle()
    LaunchedEffect(code) { collectionsModel.load(code) }
    val decades = collections[CuratedCollection.Decades].orEmpty()
    return remember(decades, code) { decadeCategories(decades, curatedCollectionsCopy(code).decadesTitle) }
}

@Composable
internal fun rememberCuratedCollectionOpener(code: String, openPlaylist: (PlaylistHit) -> Unit, openCategory: (ExploreCategory) -> Unit): (PlaylistHit) -> Unit {
    val collectionsModel = viewModel<DefaultCuratedCollectionsViewModel>(key = "default-curated-collections")
    val collections by collectionsModel.collections.collectAsStateWithLifecycle()
    val categories = remember(collections, code) {
        val copy = curatedCollectionsCopy(code)
        curatedCollectionCategories(collections[CuratedCollection.Decades].orEmpty(), copy.decadesTitle) +
            curatedCollectionCategories(collections[CuratedCollection.Everyday].orEmpty(), copy.everydayTitle)
    }
    return remember(categories, openPlaylist, openCategory) {
        { hit -> categories[hit]?.let(openCategory) ?: openPlaylist(hit) }
    }
}

internal fun curatedCollectionCategories(cards: List<CuratedCollectionCard>, section: String): Map<PlaylistHit, ExploreCategory> =
    cards.filter { it.categoryParams.isNotBlank() && it.subtitle.isNotBlank() }
        .associate { it.toPlaylistHit() to ExploreCategory(title = it.subtitle, params = it.categoryParams, section = section) }

private fun CuratedCollectionCard.toPlaylistHit() =
    PlaylistHit(title = title, author = subtitle, thumbnailUrl = thumbnailUrl, playlistId = playlistId)

internal fun decadeCategories(cards: List<CuratedCollectionCard>, sectionTitle: String): List<ExploreCategory> =
    cards.filter { it.categoryParams.isNotBlank() && it.subtitle.isNotBlank() }
        .map { ExploreCategory(title = it.subtitle, params = it.categoryParams, section = sectionTitle, sectionIndex = DECADE_SECTION_INDEX) }
        .distinctBy { it.params }

private const val DECADE_SECTION_INDEX = -1

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
                playlist = card.toPlaylistHit()
            )
        }
    )
}
