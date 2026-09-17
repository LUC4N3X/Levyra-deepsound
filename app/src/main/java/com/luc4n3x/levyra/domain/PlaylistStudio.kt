package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

enum class PlaylistCoverStyle {
    Current,
    Automatic,
    Artwork,
    Mosaic,
    Spotlight,
    Signal,
    Photo;

    val rendered: Boolean
        get() = this == Artwork || this == Mosaic || this == Spotlight || this == Signal

    companion object {
        fun from(value: String?): PlaylistCoverStyle =
            entries.firstOrNull { it.name == value } ?: Automatic
    }
}

data class PlaylistStudioPhoto(
    val uri: String,
    val viewportSizePx: Int,
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float
)

data class PlaylistStudioDraft(
    val playlistId: String? = null,
    val name: String = "",
    val tracks: List<Track> = emptyList(),
    val coverStyle: PlaylistCoverStyle = PlaylistCoverStyle.Automatic,
    val coverTrackId: String? = null,
    val photo: PlaylistStudioPhoto? = null,
    val customCoverUrl: String = ""
) {
    val isNew: Boolean get() = playlistId == null
    val trackIds: Set<String> by lazy(LazyThreadSafetyMode.NONE) { tracks.mapTo(HashSet(tracks.size)) { it.id } }
    val canSave: Boolean get() = name.isNotBlank() && (coverStyle != PlaylistCoverStyle.Photo || photo != null)

    fun contains(trackId: String): Boolean = trackId in trackIds
}

const val PLAYLIST_STUDIO_NAME_MAX_LENGTH = 120

object PlaylistStudioEdits {

    fun startFrom(playlist: Playlist): PlaylistStudioDraft = PlaylistStudioDraft(
        playlistId = playlist.id,
        name = playlist.name,
        tracks = playlist.tracks.filter { it.id.isNotBlank() }.distinctBy { it.id },
        coverStyle = if (playlist.coverMode == PlaylistCoverMode.CUSTOM) {
            PlaylistCoverStyle.Current
        } else {
            PlaylistCoverStyle.Automatic
        },
        customCoverUrl = if (playlist.coverMode == PlaylistCoverMode.CUSTOM) playlist.coverUrl else ""
    )

    fun startNew(name: String = "", seed: List<Track> = emptyList()): PlaylistStudioDraft = PlaylistStudioDraft(
        name = name.take(PLAYLIST_STUDIO_NAME_MAX_LENGTH),
        tracks = seed.filter { it.id.isNotBlank() }.distinctBy { it.id }
    )

    fun rename(draft: PlaylistStudioDraft, name: String): PlaylistStudioDraft {
        val clean = name.replace('\n', ' ').take(PLAYLIST_STUDIO_NAME_MAX_LENGTH)
        return if (clean == draft.name) draft else draft.copy(name = clean)
    }

    fun add(draft: PlaylistStudioDraft, track: Track): PlaylistStudioDraft {
        if (track.id.isBlank() || draft.contains(track.id)) return draft
        return draft.copy(tracks = draft.tracks + track.copy(streamUrl = ""))
    }

    fun remove(draft: PlaylistStudioDraft, trackId: String): PlaylistStudioDraft {
        if (!draft.contains(trackId)) return draft
        val remaining = draft.tracks.filterNot { it.id == trackId }
        val coverTrackId = draft.coverTrackId?.takeIf { it != trackId }
        return draft.copy(tracks = remaining, coverTrackId = coverTrackId)
    }

    fun toggle(draft: PlaylistStudioDraft, track: Track): PlaylistStudioDraft =
        if (draft.contains(track.id)) remove(draft, track.id) else add(draft, track)

    fun move(draft: PlaylistStudioDraft, fromIndex: Int, toIndex: Int): PlaylistStudioDraft {
        val tracks = draft.tracks
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices || fromIndex == toIndex) return draft
        val reordered = tracks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        return draft.copy(tracks = reordered)
    }

    fun setCoverStyle(draft: PlaylistStudioDraft, style: PlaylistCoverStyle): PlaylistStudioDraft {
        if (style == PlaylistCoverStyle.Current && draft.customCoverUrl.isBlank()) return draft
        return draft.copy(coverStyle = style)
    }

    fun setCoverTrack(draft: PlaylistStudioDraft, trackId: String): PlaylistStudioDraft {
        if (!draft.contains(trackId)) return draft
        return draft.copy(coverStyle = PlaylistCoverStyle.Artwork, coverTrackId = trackId)
    }

    fun setPhoto(draft: PlaylistStudioDraft, photo: PlaylistStudioPhoto): PlaylistStudioDraft =
        draft.copy(coverStyle = PlaylistCoverStyle.Photo, photo = photo)
}

data class PlaylistStudioStats(
    val trackCount: Int,
    val durationMs: Long,
    val artistCount: Int,
    val offlineCount: Int
)

private val ArtistSeparators = Regex("\\s*(?:,|&|/|;|\\bfeat\\.?|\\bft\\.?)\\s*", RegexOption.IGNORE_CASE)

fun playlistStudioStats(tracks: List<Track>, downloadedTrackIds: Set<String>): PlaylistStudioStats {
    val artists = HashSet<String>()
    var duration = 0L
    var offline = 0
    tracks.forEach { track ->
        duration += track.durationMs.coerceAtLeast(0L)
        if (track.id in downloadedTrackIds) offline++
        track.artist.split(ArtistSeparators).forEach { name ->
            val normalized = normalizeStudioText(name)
            if (normalized.isNotEmpty()) artists += normalized
        }
    }
    return PlaylistStudioStats(
        trackCount = tracks.size,
        durationMs = duration,
        artistCount = artists.size,
        offlineCount = offline
    )
}

class StudioSearchIndex(val tracks: List<Track>) {
    private val keys: Array<String> = Array(tracks.size) { index ->
        val track = tracks[index]
        normalizeStudioText("${track.title}\u0000${track.artist}\u0000${track.album}")
    }

    fun filter(query: String): List<Track> {
        val needle = normalizeStudioText(query)
        if (needle.isEmpty()) return tracks
        val result = ArrayList<Track>()
        keys.forEachIndexed { index, key ->
            if (key.contains(needle)) result += tracks[index]
        }
        return result
    }
}

fun mergeStudioCandidates(vararg sources: List<Track>): List<Track> {
    val seen = HashSet<String>()
    val merged = ArrayList<Track>()
    sources.forEach { source ->
        source.forEach { track ->
            if (track.id.isNotBlank() && track.title.isNotBlank() && seen.add(track.id)) {
                merged += track.copy(streamUrl = "")
            }
        }
    }
    return merged
}

private val CombiningMarks = Regex("\\p{Mn}+")

fun normalizeStudioText(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)

data class PlaylistCoverPlan(
    val style: PlaylistCoverStyle,
    val title: String,
    val artworkUrls: List<String>,
    val primaryColor: Int,
    val secondaryColor: Int,
    val seed: Long,
    val photo: PlaylistStudioPhoto? = null
)

const val PLAYLIST_COVER_MOSAIC_TILES = 4
private const val LevyraCoverCyan = 0xFF22D3EE.toInt()
private const val LevyraCoverViolet = 0xFF8B5CF6.toInt()
private const val LevyraCoverPink = 0xFFEC4899.toInt()
private val LevyraCoverPairs = listOf(
    LevyraCoverCyan to LevyraCoverViolet,
    LevyraCoverViolet to LevyraCoverPink,
    LevyraCoverPink to LevyraCoverCyan
)

fun playlistStudioArtwork(track: Track): String =
    track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }

fun distinctStudioArtworks(tracks: List<Track>, limit: Int): List<String> {
    if (limit <= 0) return emptyList()
    val urls = LinkedHashSet<String>()
    val albums = HashSet<String>()
    for (track in tracks) {
        val url = playlistStudioArtwork(track)
        if (url.isBlank() || url in urls) continue
        val album = normalizeStudioText(track.album)
        if (album.isNotEmpty() && !albums.add("$album\u0000${normalizeStudioText(track.artist)}")) continue
        urls += url
        if (urls.size >= limit) break
    }
    return urls.toList()
}

fun buildPlaylistCoverPlan(draft: PlaylistStudioDraft): PlaylistCoverPlan? {
    val style = draft.coverStyle
    if (style == PlaylistCoverStyle.Current || style == PlaylistCoverStyle.Automatic) return null
    val seed = stableStudioSeed(draft.name, draft.tracks)
    val (primary, secondary) = studioPalette(draft.tracks, seed)
    val artworks = when (style) {
        PlaylistCoverStyle.Artwork -> {
            val chosen = draft.coverTrackId?.let { id -> draft.tracks.firstOrNull { it.id == id } }
            listOfNotNull((chosen ?: draft.tracks.firstOrNull { playlistStudioArtwork(it).isNotBlank() })
                ?.let(::playlistStudioArtwork)
                ?.takeIf(String::isNotBlank))
        }
        PlaylistCoverStyle.Mosaic -> distinctStudioArtworks(draft.tracks, PLAYLIST_COVER_MOSAIC_TILES)
        PlaylistCoverStyle.Spotlight -> distinctStudioArtworks(draft.tracks, 1)
        else -> emptyList()
    }
    return PlaylistCoverPlan(
        style = style,
        title = draft.name.trim(),
        artworkUrls = artworks,
        primaryColor = primary,
        secondaryColor = secondary,
        seed = seed,
        photo = draft.photo.takeIf { style == PlaylistCoverStyle.Photo }
    )
}

fun stableStudioSeed(name: String, tracks: List<Track>): Long {
    var hash = FNV_OFFSET
    normalizeStudioText(name).forEach { char -> hash = fnvMix(hash, char.code.toLong()) }
    tracks.take(PLAYLIST_COVER_MOSAIC_TILES).forEach { track ->
        track.id.forEach { char -> hash = fnvMix(hash, char.code.toLong()) }
    }
    return hash
}

private fun studioPalette(tracks: List<Track>, seed: Long): Pair<Int, Int> {
    val source = tracks.firstOrNull { it.accentStart != 0 || it.accentEnd != 0 }
    if (source != null) {
        val second = tracks.firstOrNull { it !== source && (it.accentStart != 0 || it.accentEnd != 0) }
        val secondary = second?.accentStart?.takeIf { it != source.accentStart } ?: source.accentEnd
        return opaque(source.accentStart) to opaque(secondary)
    }
    val pair = LevyraCoverPairs[Math.floorMod(seed, LevyraCoverPairs.size.toLong()).toInt()]
    return pair
}

private fun opaque(color: Int): Int = color or (0xFF shl 24)

private const val FNV_OFFSET = -3750763034362895579L
private const val FNV_PRIME = 1099511628211L

private fun fnvMix(hash: Long, value: Long): Long = (hash xor value) * FNV_PRIME
