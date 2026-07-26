package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.model.CatalogPage
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.PageCursor
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object CatalogMapper {

    fun toTrack(item: StreamInfoItem): Track? {
        val url = item.url.orEmpty()
        if (url.isBlank()) return null
        val videoId = Track.videoIdOf(url)
        if (videoId.isBlank()) return null
        return Track(
            id = videoId,
            title = item.name.orEmpty().ifBlank { videoId },
            artist = cleanArtist(item.uploaderName.orEmpty()),
            videoUrl = url,
            artworkUrl = item.thumbnailUrl.orEmpty(),
            durationMs = item.duration.coerceAtLeast(0L) * 1000L
        )
    }

    fun toCollection(item: PlaylistInfoItem, kind: CollectionKind): CollectionRef? {
        val url = item.url.orEmpty()
        if (url.isBlank()) return null
        return CollectionRef(
            id = collectionIdOf(url),
            title = item.name.orEmpty().ifBlank { url },
            subtitle = cleanArtist(item.uploaderName.orEmpty()),
            artworkUrl = item.thumbnailUrl.orEmpty(),
            url = url,
            kind = kind,
            itemCount = item.streamCount.coerceAtLeast(0L)
        )
    }

    fun toCollection(item: ChannelInfoItem): CollectionRef? {
        val url = item.url.orEmpty()
        if (url.isBlank()) return null
        return CollectionRef(
            id = collectionIdOf(url),
            title = item.name.orEmpty().ifBlank { url },
            subtitle = "",
            artworkUrl = item.thumbnailUrl.orEmpty(),
            url = url,
            kind = CollectionKind.ARTIST,
            itemCount = item.streamCount.coerceAtLeast(0L)
        )
    }

    fun toPage(items: List<InfoItem>, nextPage: Page?, filter: SearchFilter?): CatalogPage {
        val tracks = ArrayList<Track>(items.size)
        val collections = ArrayList<CollectionRef>(items.size)
        val playlistKind = if (filter == SearchFilter.ALBUMS) CollectionKind.ALBUM else CollectionKind.PLAYLIST
        items.forEach { item ->
            when (item) {
                is StreamInfoItem -> toTrack(item)?.let(tracks::add)
                is PlaylistInfoItem -> toCollection(item, playlistKind)?.let(collections::add)
                is ChannelInfoItem -> toCollection(item)?.let(collections::add)
                else -> Unit
            }
        }
        return CatalogPage(
            tracks = tracks.distinctBy { it.id },
            collections = collections.distinctBy { it.id },
            cursor = nextPage?.let(::PageCursor)
        )
    }

    fun collectionIdOf(url: String): String {
        val listParam = url.substringAfter("list=", "")
        if (listParam.isNotBlank()) {
            return listParam.takeWhile { it != '&' && it != '#' }
        }
        val channelParam = url.substringAfter("/channel/", "")
        if (channelParam.isNotBlank()) {
            return channelParam.takeWhile { it != '?' && it != '/' && it != '#' }
        }
        return url
    }

    fun cleanArtist(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        val withoutTopic = trimmed.removeSuffix(" - Topic").trim()
        return withoutTopic.ifBlank { trimmed }
    }
}
