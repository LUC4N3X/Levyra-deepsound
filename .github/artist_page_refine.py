from pathlib import Path

repo = Path('app/src/main/java/com/luc4n3x/levyra/data/ArtistRepository.kt')
vm = Path('app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt')
ui = Path('app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'{label}: pattern not found')
    return text.replace(old, new, 1)


# ArtistRepository: return the root artist page quickly, enrich continuations later.
text = repo.read_text(encoding='utf-8')
text = replace_once(
    text,
    'fetchProfile(resolvedArtist.browseId, resolvedArtist.name.ifBlank { clean })',
    'fetchProfile(resolvedArtist.browseId, resolvedArtist.name.ifBlank { clean }, expandSections = false)',
    'profileFor fast fetch'
)
text = replace_once(
    text,
    'fetchProfile(cleanBrowseId, cleanFallbackName)',
    'fetchProfile(cleanBrowseId, cleanFallbackName, expandSections = false)',
    'profile fast fetch'
)
text = replace_once(
    text,
    'private suspend fun fetchProfile(browseId: String, fallbackName: String): ArtistProfile? {\n        val root = postBrowse(browseId)',
    '''private suspend fun fetchProfile(
        browseId: String,
        fallbackName: String,
        expandSections: Boolean = true
    ): ArtistProfile? {
        val root = runCatching { postBrowseFast(browseId) }
            .getOrNull()
            ?.takeIf { it.optJSONObject("header") != null }
            ?: postBrowse(browseId)''',
    'fetchProfile signature'
)
old_expanded = '''        val expanded = coroutineScope {
            val songsJob = async { songsPointer?.let { pointer -> fetchSongs(pointer, name) }.orEmpty() }
            val albumsJobs = albumPointers.map { pointer -> async { fetchReleases(pointer) } }
            val singlesJobs = singlePointers.map { pointer -> async { fetchReleases(pointer) } }
            val videosJob = async { videoPointer?.let { fetchVideos(it, name) }.orEmpty() }
            ArtistExpandedSections(
                songs = songsJob.await(),
                albums = albumsJobs.awaitAll().flatten(),
                singles = singlesJobs.awaitAll().flatten(),
                videos = videosJob.await()
            )
        }'''
new_expanded = '''        val expanded = if (expandSections) {
            coroutineScope {
                val songsJob = async { songsPointer?.let { pointer -> fetchSongs(pointer, name) }.orEmpty() }
                val albumsJobs = albumPointers.map { pointer -> async { fetchReleases(pointer) } }
                val singlesJobs = singlePointers.map { pointer -> async { fetchReleases(pointer) } }
                val videosJob = async { videoPointer?.let { fetchVideos(it, name) }.orEmpty() }
                ArtistExpandedSections(
                    songs = songsJob.await(),
                    albums = albumsJobs.awaitAll().flatten(),
                    singles = singlesJobs.awaitAll().flatten(),
                    videos = videosJob.await()
                )
            }
        } else {
            ArtistExpandedSections(
                songs = emptyList(),
                albums = emptyList(),
                singles = emptyList(),
                videos = emptyList()
            )
        }'''
text = replace_once(text, old_expanded, new_expanded, 'expanded artist sections')
enrich_marker = '    private suspend fun fetchProfile(\n'
enrich_method = '''    suspend fun enrichProfile(profile: ArtistProfile): ArtistProfile = withContext(Dispatchers.IO) {
        val browseId = profile.browseId.trim()
        if (browseId.isBlank() || profile.name.isBlank()) return@withContext profile

        val enriched = runCatching {
            fetchProfile(
                browseId = browseId,
                fallbackName = profile.name,
                expandSections = true
            )
        }.getOrNull()
            ?.takeIf { artistProfileMatchesRequest(it, browseId, profile.name) }
            ?: return@withContext profile

        val merged = enriched.copy(biography = profile.biography ?: enriched.biography)
        memory[profileBrowseKey(browseId)] = merged
        memory[artistIdentityKey(profile.name)] = merged
        memory[artistIdentityKey(merged.name)] = merged
        merged
    }

'''
if enrich_marker not in text:
    raise SystemExit('enrichProfile insertion marker not found')
text = text.replace(enrich_marker, enrich_method + enrich_marker, 1)
repo.write_text(text, encoding='utf-8')


# ViewModel: biography and continuation shelves must never gate the first visible profile.
text = vm.read_text(encoding='utf-8')
text = text.replace('private const val ARTIST_INITIAL_BIOGRAPHY_WAIT_MS = 4_500L\n', '', 1)
old_vm = '''                val initialBiography = withTimeoutOrNull(ARTIST_INITIAL_BIOGRAPHY_WAIT_MS) {
                    biographyDeferred.await()
                }
                val initialProfile = initialBiography?.let { biography ->
                    artistRepository.mergeBiography(profile, biography)
                } ?: profile
                _state.update {
                    it.copy(
                        artistLoading = false,
                        artistError = null,
                        artistProfile = initialProfile
                    )
                }
                refreshArtistMotionArtwork(initialProfile)
                initialBiography?.let {
                    startArtistLore(initialProfile)
                } ?: run {
                    artistLoreJob?.cancel()
                    artistLoreJob = launchArtistLoreAwait(profile, biographyDeferred)
                }'''
new_vm = '''                _state.update {
                    it.copy(
                        artistLoading = false,
                        artistError = null,
                        artistProfile = profile
                    )
                }
                refreshArtistMotionArtwork(profile)

                artistLoreJob?.cancel()
                artistLoreJob = launchArtistLoreAwait(profile, biographyDeferred)

                val enriched = runCatching { artistRepository.enrichProfile(profile) }.getOrNull()
                if (enriched != null && isActive) {
                    _state.update { current ->
                        val visible = current.artistProfile ?: return@update current
                        if (!current.showArtist || !sameArtistProfile(visible, profile)) return@update current
                        current.copy(
                            artistProfile = enriched.copy(
                                biography = visible.biography ?: enriched.biography
                            )
                        )
                    }
                }'''
text = replace_once(text, old_vm, new_vm, 'artist first-paint ViewModel block')
vm.write_text(text, encoding='utf-8')


# UI: skeleton first, then a calmer YouTube Music-style content hierarchy.
text = ui.read_text(encoding='utf-8')
loading_start = '                state.artistLoading && profile == null -> {'
loading_end = '                state.artistError != null && profile == null -> {'
start = text.index(loading_start)
end = text.index(loading_end, start)
text = text[:start] + '''                state.artistLoading && profile == null -> {
                    item(key = "artist-loading-skeleton") {
                        ArtistLoadingSkeleton()
                    }
                }
''' + text[end:]

sections_start = text.index('                    if (artist.topSongs.isNotEmpty()) {')
sections_end = text.index('                    if (artist.relatedArtists.isNotEmpty()) {', sections_start)
new_sections = '''                    if (artist.topSongs.isNotEmpty()) {
                        item(key = "artist-popular-tracks") {
                            ArtistPopularTracksSection(
                                tracks = artist.topSongs,
                                currentId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                isResolving = state.isResolving,
                                onPlay = onPlay
                            )
                        }
                    }
                    if (artist.albums.isNotEmpty()) {
                        item(key = "artist-albums") {
                            ArtistSectionShelf(title = strings.albumsPlain) {
                                ArtistReleaseRow(artist.albums, artist.name, onOpenRelease)
                            }
                        }
                    }
                    if (artist.singles.isNotEmpty()) {
                        item(key = "artist-singles") {
                            ArtistSectionShelf(title = strings.singlesAndEps) {
                                ArtistReleaseRow(artist.singles, artist.name, onOpenRelease)
                            }
                        }
                    }
                    if (artist.compilations.isNotEmpty()) {
                        item(key = "artist-compilations") {
                            ArtistSectionShelf(title = strings.compilations) {
                                ArtistReleaseRow(artist.compilations, artist.name, onOpenRelease)
                            }
                        }
                    }
                    if (artist.videos.isNotEmpty()) {
                        item(key = "artist-videos") {
                            ArtistSectionShelf(title = strings.video) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
                                ) {
                                    items(
                                        items = artist.videos.take(12),
                                        key = { "artist-video-${it.id}" },
                                        contentType = { "artist-video" }
                                    ) { track ->
                                        VideoGlassCard(
                                            track = track,
                                            isCurrent = track.id == state.currentTrack?.id,
                                            isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                            onClick = { onPlay(track) }
                                        )
                                    }
                                }
                            }
                        }
                    }
'''
text = text[:sections_start] + new_sections + text[sections_end:]

helpers = '''@Composable
private fun ArtistLoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(LevyraPanelSoft, LevyraInk, LevyraBlack)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.width(220.dp).height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                )
                Box(
                    Modifier.width(145.dp).height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.width(132.dp).height(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                    Box(
                        Modifier.width(112.dp).height(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.width(170.dp).height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.09f))
            )
            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.62f).height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.White.copy(alpha = 0.09f))
                        )
                        Box(
                            Modifier.fillMaxWidth(0.42f).height(11.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.055f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSectionShelf(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            ArtistSectionTitle(title)
        }
        content()
    }
}

'''
title_marker = '@Composable\nprivate fun ArtistSectionTitle(title: String) {'
if title_marker not in text:
    raise SystemExit('ArtistSectionTitle insertion marker not found')
text = text.replace(title_marker, helpers + title_marker, 1)

popular_start = text.index('@Composable\nprivate fun ArtistPopularTracksShelf(')
popular_end = text.index('private fun normalizeBiographyPreview(', popular_start)
new_popular = '''@Composable
private fun ArtistPopularTracksSection(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val visibleTracks = remember(tracks) { tracks.distinctBy { it.id }.take(5) }
    if (visibleTracks.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ArtistSectionTitle(strings.popularTracks)
            Surface(
                color = Color.White.copy(alpha = 0.055f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shape = CircleShape,
                modifier = Modifier.heightIn(min = 48.dp).pressable { onPlay(visibleTracks.first()) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(17.dp))
                    Text(
                        text = strings.playAll,
                        color = LevyraText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            visibleTracks.forEachIndexed { index, track ->
                ArtistPopularTrackCompactRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentId,
                    isPlaying = isPlaying && track.id == currentId,
                    isResolving = isResolving && track.id == currentId,
                    onPlay = { onPlay(track) }
                )
            }
        }
    }
}

@Composable
private fun ArtistPopularTrackCompactRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val playCount = remember(track.youtubeViewCount, strings.code) {
        if (track.youtubeViewCount > 0L) formatSearchViewCount(track.youtubeViewCount, strings.code) else ""
    }
    val secondary = remember(track.artist, playCount) {
        listOf(track.artist, playCount).filter { it.isNotBlank() }.joinToString(" • ")
    }
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(rowShape)
            .background(if (isCurrent) LevyraCyan.copy(alpha = 0.075f) else Color.Transparent)
            .pressable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text(
            text = (index + 1).toString(),
            color = if (isCurrent) LevyraCyan else LevyraMuted.copy(alpha = 0.76f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp)
        )
        Box {
            CoverImage(track, Modifier.size(54.dp).clip(RoundedCornerShape(9.dp)))
            if (isPlaying || isResolving) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LevyraCyan
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = strings.playing,
                            tint = LevyraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (track.explicit) {
                    Icon(
                        imageVector = Icons.Rounded.Explicit,
                        contentDescription = null,
                        tint = LevyraMuted.copy(alpha = 0.72f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = secondary,
                    color = LevyraMuted.copy(alpha = 0.86f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!isPlaying && !isResolving) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = strings.play,
                tint = if (isCurrent) LevyraCyan else LevyraMuted.copy(alpha = 0.72f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

'''
text = text[:popular_start] + new_popular + text[popular_end:]

release_start = text.index('@Composable\nprivate fun ArtistReleaseRow(')
release_end = text.index('@Composable\nprivate fun ReleaseRadarRow(', release_start)
new_release = '''@Composable
private fun ArtistReleaseRow(
    releases: List<ArtistRelease>,
    artistName: String,
    onOpen: (ArtistRelease, String) -> Unit
) {
    val visibleReleases = remember(releases) {
        releases.distinctBy { it.browseId.ifBlank { it.title } }.take(24)
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
    ) {
        items(
            items = visibleReleases,
            key = { "rel-${it.browseId.ifBlank { it.title }}" },
            contentType = { "artist-release" }
        ) { release ->
            Column(
                modifier = Modifier.width(152.dp).pressable { onOpen(release, artistName) },
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier.size(152.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LevyraPanelSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (release.thumbnailUrl.isNotBlank()) {
                        StableRemoteArtwork(
                            url = release.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(38.dp))
                    }
                }
                Text(
                    text = release.title,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = release.year.ifBlank { release.subtitle },
                    color = LevyraMuted.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

'''
text = text[:release_start] + new_release + text[release_end:]

video_start = text.index('@Composable\nprivate fun VideoGlassCard(')
video_end = text.index('@Composable\nprivate fun BottomTabsScrim()', video_start)
new_video = '''@Composable
private fun VideoGlassCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    Column(
        modifier = Modifier.width(280.dp).pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(shape)
                .background(LevyraPanelSoft)
                .border(
                    1.dp,
                    if (isCurrent) LevyraCyan.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                StableRemoteArtwork(
                    url = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(Icons.Rounded.Videocam, null, tint = LevyraMuted, modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier.align(Alignment.Center).size(46.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isCurrent && isPlaying) LocalLevyraStrings.current.playing else LocalLevyraStrings.current.play,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = track.title,
            color = LevyraText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = LevyraMuted.copy(alpha = 0.82f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

'''
text = text[:video_start] + new_video + text[video_end:]
ui.write_text(text, encoding='utf-8')
