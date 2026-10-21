package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.apple.AppleMetadataEnricher
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation
import com.luc4n3x.levyra.nexus.playlistimport.MatchSignal
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import kotlinx.coroutines.CancellationException

internal class PlaylistImportMetadata(
    private val apple: AppleMetadataEnricher,
    private val spotify: suspend (String) -> List<Track>,
    private val country: () -> String
) {
    suspend fun search(identity: ImportedTrackIdentity): List<MatchCandidate> {
        val query = "${identity.title} ${identity.primaryArtist}".trim()
        val spotifyTracks = try {
            spotify(query).take(12).map { it.toMatchCandidate().candidate.copy(kind = CandidateKind.SONG) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
        if (corroboratingMetadata(identity, spotifyTracks).isNotEmpty()) return spotifyTracks
        val appleTracks = apple.searchImportMetadata(identity.title, identity.primaryArtist, country()).map { item ->
            MatchCandidate(
                id = "apple:${item.songId}", title = item.name, artists = listOf(item.artistName),
                album = item.albumName, durationMs = item.durationMs, isrc = item.isrc,
                explicit = if (item.explicit) true else null, kind = CandidateKind.SONG
            )
        }
        return spotifyTracks + appleTracks
    }
}

internal fun corroborateImportIdentity(identity: ImportedTrackIdentity, metadata: List<MatchCandidate>): ImportedTrackIdentity {
    val plausible = corroboratingMetadata(identity, metadata)
    if (plausible.isEmpty()) return identity
    val best = plausible.maxBy { it.score }
    if (best.reasons.any { it.signal == MatchSignal.ISRC_MATCH }) {
        return identity.copy(
            title = best.candidate.title,
            artists = best.candidate.artists,
            featuredArtists = emptyList(),
            durationMs = identity.durationMs.takeIf { it > 0 } ?: best.candidate.durationMs,
            album = identity.album.ifBlank { best.candidate.album }
        )
    }
    return identity.copy(
        durationMs = identity.durationMs.takeIf { it > 0 } ?: best.candidate.durationMs,
        album = identity.album.ifBlank { best.candidate.album }
    )
}

private fun corroboratingMetadata(identity: ImportedTrackIdentity, metadata: List<MatchCandidate>): List<MatchEvaluation> {
    val evaluations = metadata.filter { it.durationMs > 0L }.map { PlaylistMatchEngine.evaluate(identity, it) }
    val sameIsrc = evaluations.filter { evaluation -> evaluation.reasons.any { it.signal == MatchSignal.ISRC_MATCH } }
    val corroborated = sameIsrc.ifEmpty {
        evaluations.filter { evaluation ->
            val signals = evaluation.reasons.map { it.signal }.toSet()
            evaluation.confidence.autoAccepted && MatchSignal.TITLE_EXACT in signals &&
                MatchSignal.ARTIST_EXACT in signals && MatchSignal.VERSION_CONFLICT !in signals &&
                MatchSignal.ISRC_CONFLICT !in signals
        }
    }
    val album = MusicIdentityText.album(identity.album).core
    val plausible = corroborated.filter { album.isNotEmpty() && MusicIdentityText.album(it.candidate.album).core == album }
        .ifEmpty { corroborated }
    if (plausible.isEmpty()) return emptyList()
    val durations = plausible.map { it.candidate.durationMs }
    return if (durations.max() - durations.min() > 5_000L) emptyList() else plausible
}
