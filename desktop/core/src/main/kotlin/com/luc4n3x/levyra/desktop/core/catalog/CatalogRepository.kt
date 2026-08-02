package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.charts.PlayableMatcher
import com.luc4n3x.levyra.desktop.core.model.ArtistDetail
import com.luc4n3x.levyra.desktop.core.model.CatalogPage
import com.luc4n3x.levyra.desktop.core.model.CollectionDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.PageCursor
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
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

    private suspend fun artistDetail(ref: CollectionRef): CollectionDetail {
        val info = ChannelInfo.getInfo(service, ref.url)
        val resolvedName = chooseDesktopArtistName(ref.title, info.name.orEmpty())
        val tabPages = info.tabs.orEmpty().mapNotNull { handler ->
            val page = runCatching { ChannelTabInfo.getInfo(service, handler) }
                .getOrNull()
                ?.let { CatalogMapper.toPage(it.relatedItems.orEmpty(), null, null) }
                ?: return@mapNotNull null
            ArtistTabPage(desktopArtistTabKind(handler), page)
        }
        val merged = tabPages
            .map(ArtistTabPage::page)
            .fold(CatalogPage()) { accumulator, page -> accumulator.mergedWith(page) }

        val tracksFromTab = tabPages
            .filter { it.kind == DesktopArtistTabKind.TRACKS }
            .flatMap { it.page.tracks }
            .distinctBy { it.id }
        val topTracks = tracksFromTab
            .ifEmpty { merged.tracks.filter { artistLabelMatches(it.artist, resolvedName) } }
            .ifEmpty { fallbackArtistTracks(ref.title.ifBlank { resolvedName }) }
            .distinctBy { it.id }
            .take(MAX_ARTIST_TRACKS)

        val videos = tabPages
            .filter { it.kind == DesktopArtistTabKind.VIDEOS }
            .flatMap { it.page.tracks }
            .filterNot { video -> topTracks.any { it.id == video.id } }
            .distinctBy { it.id }
            .take(MAX_ARTIST_VIDEOS)

        val albumsFromTab = tabPages
            .filter { it.kind == DesktopArtistTabKind.ALBUMS }
            .flatMap { it.page.collections }
            .map { it.copy(kind = CollectionKind.ALBUM) }
            .distinctBy { it.id }
        val albums = albumsFromTab
            .ifEmpty { fallbackArtistAlbums(ref.title.ifBlank { resolvedName }) }
            .distinctBy { it.id }
            .take(MAX_ARTIST_ALBUMS)

        val playlists = tabPages
            .filter { it.kind == DesktopArtistTabKind.PLAYLISTS }
            .flatMap { it.page.collections }
            .map { it.copy(kind = CollectionKind.PLAYLIST) }
            .distinctBy { it.id }
            .take(MAX_ARTIST_PLAYLISTS)

        val relatedArtists = info.relatedItems
            .orEmpty()
            .filterIsInstance<ChannelInfoItem>()
            .mapNotNull(CatalogMapper::toCollection)
            .filterNot { artistLabelMatches(it.title, resolvedName) }
            .distinctBy { it.id }
            .take(MAX_RELATED_ARTISTS)

        val portrait = desktopArtistArtworkUrl(
            info.avatarUrl.orEmpty().ifBlank { ref.artworkUrl },
            ARTIST_PORTRAIT_SIZE
        )
        val banner = info.bannerUrl.orEmpty().trim()
        val biography = info.description.orEmpty()
            .replace(Regex("\\s*\\n+\\s*"), "\n")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
        val artist = ArtistDetail(
            name = resolvedName,
            biography = biography,
            portraitUrl = portrait,
            bannerUrl = banner,
            subscriberCount = info.subscriberCount,
            tracks = topTracks,
            videos = videos,
            albums = albums,
            playlists = playlists,
            relatedArtists = relatedArtists
        )
        val page = CatalogPage(
            tracks = (topTracks + videos).distinctBy { it.id },
            collections = (albums + playlists + relatedArtists).distinctBy { it.id },
            cursor = null
        )
        return CollectionDetail(
            ref = ref.copy(
                title = resolvedName,
                artworkUrl = portrait,
                itemCount = topTracks.size.toLong()
            ),
            page = page,
            artist = artist
        )
    }

    private suspend fun fallbackArtistTracks(name: String): List<Track> {
        val page = runCatching { search(name, SearchFilter.SONGS) }.getOrDefault(CatalogPage())
        val matching = page.tracks.filter { artistLabelMatches(it.artist, name) }
        return matching.ifEmpty { page.tracks }.take(MAX_ARTIST_TRACKS)
    }

    private suspend fun fallbackArtistAlbums(name: String): List<CollectionRef> {
        val page = runCatching { search(name, SearchFilter.ALBUMS) }.getOrDefault(CatalogPage())
        val matching = page.collections.filter { artistLabelMatches(it.subtitle, name) }
        return matching.ifEmpty { page.collections }
            .map { it.copy(kind = CollectionKind.ALBUM) }
            .take(MAX_ARTIST_ALBUMS)
    }

    private data class ArtistTabPage(
        val kind: DesktopArtistTabKind?,
        val page: CatalogPage
    )

    private companion object {
        const val MAX_ARTIST_TRACKS = 20
        const val MAX_ARTIST_VIDEOS = 16
        const val MAX_ARTIST_ALBUMS = 24
        const val MAX_ARTIST_PLAYLISTS = 16
        const val MAX_RELATED_ARTISTS = 16
        const val ARTIST_PORTRAIT_SIZE = 720
    }
}

internal enum class DesktopArtistTabKind {
    TRACKS,
    VIDEOS,
    ALBUMS,
    PLAYLISTS
}

internal fun desktopArtistTabKind(handler: ListLinkHandler): DesktopArtistTabKind? {
    val labels = handler.contentFilters.orEmpty().map { it.name }
    return desktopArtistTabKind(labels)
}

internal fun desktopArtistTabKind(labels: List<String>): DesktopArtistTabKind? {
    val tokens = labels.map { it.trim().lowercase(Locale.ROOT) }
    fun matches(primary: String, aliases: List<String>): Boolean {
        val expected = primary.lowercase(Locale.ROOT)
        return tokens.any { token ->
            token == expected || aliases.any { alias -> token.contains(alias) }
        }
    }
    return when {
        matches(ChannelTabs.TRACKS, listOf("track", "song", "brani", "canzoni")) -> DesktopArtistTabKind.TRACKS
        matches(ChannelTabs.VIDEOS, listOf("video", "clip")) -> DesktopArtistTabKind.VIDEOS
        matches(ChannelTabs.ALBUMS, listOf("album", "release", "discografia")) -> DesktopArtistTabKind.ALBUMS
        matches(ChannelTabs.PLAYLISTS, listOf("playlist", "mix")) -> DesktopArtistTabKind.PLAYLISTS
        else -> null
    }
}

internal fun chooseDesktopArtistName(requestedName: String, resolvedName: String): String {
    val requested = CatalogMapper.cleanArtist(requestedName).trim()
    val resolved = CatalogMapper.cleanArtist(resolvedName).trim()
    if (resolved.isBlank()) return requested
    if (requested.isBlank()) return resolved
    val requestedKey = desktopArtistIdentity(requested)
    val resolvedKey = desktopArtistIdentity(resolved)
    val looksLikeTrackTitle = !requested.contains(" - ") &&
        resolved.startsWith("$requested - ", ignoreCase = true)
    return when {
        looksLikeTrackTitle -> requested
        requestedKey == resolvedKey -> requested
        else -> resolved
    }
}

internal fun desktopArtistArtworkUrl(url: String, size: Int = 720): String {
    val clean = url.trim()
    if (clean.isBlank()) return ""
    val youtubeImage = clean.contains("yt3.", ignoreCase = true) ||
        clean.contains("googleusercontent.com", ignoreCase = true) ||
        clean.contains("ggpht.com", ignoreCase = true)
    if (!youtubeImage) return clean
    val suffix = clean.substringAfterLast('=', "")
    if (suffix.isBlank() || (!suffix.startsWith('s') && !suffix.startsWith('w'))) return clean
    val base = clean.substringBeforeLast('=')
    return "$base=s${size.coerceIn(256, 1280)}-c-k-c0x00ffffff-no-rj"
}

private fun artistLabelMatches(candidate: String, artistName: String): Boolean {
    val candidateKey = desktopArtistIdentity(candidate)
    val artistKey = desktopArtistIdentity(artistName)
    if (candidateKey.isBlank() || artistKey.isBlank()) return false
    return candidateKey == artistKey ||
        candidateKey.contains(artistKey) ||
        artistKey.contains(candidateKey)
}

private fun desktopArtistIdentity(value: String): String = CatalogMapper.cleanArtist(value)
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim()
