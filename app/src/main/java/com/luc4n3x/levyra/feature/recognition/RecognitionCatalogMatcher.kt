package com.luc4n3x.levyra.feature.recognition

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlinx.coroutines.CancellationException

class RecognitionCatalogMatcher(private val repository: YoutubeMusicRepository) {

    suspend fun match(result: RecognitionResult, languageCode: String): Track? {
        val title = result.title.trim()
        val artist = result.artist.trim()
        if (title.isBlank()) return null

        val query = listOf(title, artist).filter(String::isNotBlank).joinToString(" ")
        val candidates = try {
            repository.search(query, CANDIDATE_LIMIT, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }

        val byVideoId = result.youtubeVideoId.trim().takeIf { it.isNotBlank() }?.let { videoId ->
            candidates.firstOrNull { it.id == videoId }
        }
        if (byVideoId != null && !conflictsByIsrc(result, byVideoId)) return byVideoId

        val byIsrc = result.isrc.trim().takeIf { it.isNotBlank() }?.let { isrc ->
            candidates.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }
        }
        if (byIsrc != null) return byIsrc

        val byText = try {
            repository.searchSongMatch(title, artist, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }
        return byText?.takeUnless { conflictsByIsrc(result, it) }
    }

    private fun conflictsByIsrc(result: RecognitionResult, candidate: Track): Boolean {
        val expected = result.isrc.trim().uppercase(Locale.ROOT)
        val actual = candidate.isrc.trim().uppercase(Locale.ROOT)
        return expected.isNotBlank() && actual.isNotBlank() && expected != actual
    }

    private companion object {
        const val CANDIDATE_LIMIT = 12
    }
}
