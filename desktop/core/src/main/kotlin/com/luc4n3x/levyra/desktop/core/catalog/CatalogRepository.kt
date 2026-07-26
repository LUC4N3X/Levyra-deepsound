package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.charts.PlayableMatcher
import com.luc4n3x.levyra.desktop.core.model.CatalogPage
import com.luc4n3x.levyra.desktop.core.model.CollectionDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.PageCursor
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelTabInfo
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class CatalogRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val service: StreamingService get() = ServiceList.YouTube

    suspend fun search(query: String, filter: SearchFilter): CatalogPage = withContext(dispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext CatalogPage()
        val handler = service.searchQHFactory.fromQuery(
            trimmed,
            SearchFilterCatalog.contentFilters(service, filter),
            emptyList()
        )
        val info = SearchInfo.getInfo(service, handler)
        CatalogMapper.toPage(info.relatedItems.orEmpty(), info.nextPage, filter)
    }

    suspend fun searchMore(
        query: String,
        filter: SearchFilter,
        cursor: PageCursor
    ): CatalogPage = withContext(dispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext CatalogPage()
        val handler = service.searchQHFactory.fromQuery(
            trimmed,
            SearchFilterCatalog.contentFilters(service, filter),
            emptyList()
        )
        val page = SearchInfo.getMoreItems(service, handler, cursor.page)
        CatalogMapper.toPage(page.items.orEmpty(), page.nextPage, filter)
    }

    suspend fun suggestions(query: String): List<String> = withContext(dispatcher) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        runCatching { service.suggestionExtractor?.suggestionList(trimmed).orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
    }

    suspend fun collection(ref: CollectionRef): CollectionDetail = withContext(dispatcher) {
        when (ref.kind) {
            CollectionKind.ARTIST -> artistDetail(ref)
            else -> playlistDetail(ref)
        }
    }

    suspend fun collectionMore(ref: CollectionRef, cursor: PageCursor): CatalogPage =
        withContext(dispatcher) {
            when (ref.kind) {
                CollectionKind.ARTIST -> CatalogPage()
                else -> {
                    val page = PlaylistInfo.getMoreItems(service, ref.url, cursor.page)
                    CatalogMapper.toPage(page.items.orEmpty(), page.nextPage, null)
                }
            }
        }

    suspend fun collectionFromUrl(url: String): CollectionDetail = withContext(dispatcher) {
        val normalized = url.trim()
        require(normalized.isNotBlank()) { "URL vuoto" }
        val ref = CollectionRef(
            id = CatalogMapper.collectionIdOf(normalized),
            title = normalized,
            url = normalized,
            kind = if (isChannelUrl(normalized)) CollectionKind.ARTIST else CollectionKind.PLAYLIST
        )
        collection(ref)
    }

    private fun isChannelUrl(url: String): Boolean =
        runCatching { service.channelLHFactory.acceptUrl(url) }.getOrDefault(false)

    suspend fun findPlayable(track: Track): Track? = withContext(dispatcher) {
        if (track.videoUrl.isNotBlank()) return@withContext track
        val query = listOf(track.title, track.artist)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        if (query.isEmpty()) return@withContext null
        val songs = runCatching { search(query, SearchFilter.SONGS).tracks }.getOrDefault(emptyList())
        val candidates = songs.ifEmpty {
            runCatching { search(query, SearchFilter.VIDEOS).tracks }.getOrDefault(emptyList())
        }
        val match = PlayableMatcher.best(track, candidates) ?: return@withContext null
        match.copy(
            title = track.title.ifBlank { match.title },
            artist = track.artist.ifBlank { match.artist },
            album = track.album.ifBlank { match.album },
            artworkUrl = track.artworkUrl.ifBlank { match.artworkUrl }
        )
    }

    suspend fun radio(track: Track, limit: Int = 25): List<Track> = withContext(dispatcher) {
        val info = StreamInfo.getInfo(service, track.videoUrl)
        info.relatedItems
            .orEmpty()
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull(CatalogMapper::toTrack)
            .filter { it.id != track.id }
            .distinctBy { it.id }
            .take(limit)
    }

    private fun playlistDetail(ref: CollectionRef): CollectionDetail {
        val info = PlaylistInfo.getInfo(service, ref.url)
        val page = CatalogMapper.toPage(info.relatedItems.orEmpty(), info.nextPage, null)
        val artwork = ref.artworkUrl.ifBlank { info.thumbnailUrl.orEmpty() }
        return CollectionDetail(
            ref = ref.copy(
                title = info.name.orEmpty().ifBlank { ref.title },
                subtitle = CatalogMapper.cleanArtist(info.uploaderName.orEmpty()).ifBlank { ref.subtitle },
                artworkUrl = artwork.ifBlank { page.tracks.firstOrNull()?.artworkUrl.orEmpty() },
                itemCount = info.streamCount.coerceAtLeast(0L).takeIf { it > 0 } ?: page.tracks.size.toLong()
            ),
            page = page
        )
    }

    private fun artistDetail(ref: CollectionRef): CollectionDetail {
        val info = ChannelInfo.getInfo(service, ref.url)
        val tabs = info.tabs.orEmpty()
        val merged = preferredTabs(tabs)
            .mapNotNull { tab -> runCatching { ChannelTabInfo.getInfo(service, tab) }.getOrNull() }
            .map { tab -> CatalogMapper.toPage(tab.relatedItems.orEmpty(), null, null) }
            .fold(CatalogPage()) { accumulator, page -> accumulator.mergedWith(page) }
        val page = CatalogPage(
            tracks = merged.tracks.distinctBy { it.id },
            collections = merged.collections.distinctBy { it.id },
            cursor = null
        )
        return CollectionDetail(
            ref = ref.copy(
                title = info.name.orEmpty().ifBlank { ref.title },
                artworkUrl = ref.artworkUrl.ifBlank { info.avatarUrl.orEmpty() },
                itemCount = page.tracks.size.toLong()
            ),
            page = page
        )
    }

    private fun preferredTabs(tabs: List<ListLinkHandler>): List<ListLinkHandler> {
        val order = listOf(ChannelTabs.TRACKS, ChannelTabs.VIDEOS, ChannelTabs.ALBUMS, ChannelTabs.PLAYLISTS)
        return order.mapNotNull { name ->
            tabs.firstOrNull { tab -> tab.contentFilters.orEmpty().any { it.name == name } }
        }
    }
}
