package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.catalog.artistCreditMatches
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.ui.HomeSoundtrackPortraits
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoundtrackUiState(
    val artist: String = "",
    val portraitUrl: String = "",
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false
)

class SoundtrackController(
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository
) {
    private val state = MutableStateFlow(SoundtrackUiState())
    val soundtrack: StateFlow<SoundtrackUiState> = state.asStateFlow()
    private var job: Job? = null
    private val artists = MutableStateFlow<Map<String, CollectionRef>>(emptyMap())
    val artistReferences: StateFlow<Map<String, CollectionRef>> = artists.asStateFlow()
    private val requestedPortraits = ConcurrentHashMap.newKeySet<String>()

    fun requestPortraits(names: List<String>) {
        names.asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && requestedPortraits.add(it.lowercase()) }
            .take(PORTRAIT_BATCH_LIMIT)
            .forEach { name ->
                scope.launch {
                    val ref = safely { catalog.findArtist(name) }
                    if (ref != null) {
                        artists.update { it + (name.lowercase() to ref.copy(artworkUrl = highResolutionPortrait(ref.artworkUrl))) }
                    } else {
                        requestedPortraits.remove(name.lowercase())
                    }
                }
            }
    }

    fun load(languageCode: String) {
        val artist = HomeSoundtrackPortraits.artistForDay(languageCode, LocalDate.now().toEpochDay())
        val current = state.value
        if (current.artist == artist && (current.loading || current.tracks.isNotEmpty())) return
        job?.cancel()
        val selectedPortrait = if (artist.equals("Billie Eilish", ignoreCase = true)) BILLIE_PORTRAIT else HomeSoundtrackPortraits.fixedPortrait(artist).orEmpty()
        state.value = SoundtrackUiState(artist = artist, portraitUrl = selectedPortrait, loading = true)
        job = scope.launch {
            val portrait = async {
                if (selectedPortrait.isNotBlank()) return@async selectedPortrait
                safely { catalog.search(artist, SearchFilter.ARTISTS) }?.collections.orEmpty()
                    .firstOrNull { artistCreditMatches(it.title, artist) && it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
            }
            val songs = async { safely { catalog.search(artist, SearchFilter.SONGS) }?.tracks.orEmpty() }
            val tracks = songs.await().filter { soundtrackPrimaryArtist(it.artist).equals(soundtrackPrimaryArtist(artist), ignoreCase = true) }
            state.value = state.value.copy(
                portraitUrl = highResolutionPortrait(portrait.await()),
                tracks = tracks.take(SOUNDTRACK_LEAD_LIMIT),
                loading = false
            )
        }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val BILLIE_PORTRAIT = "https://www.apple.com/newsroom/images/2024/11/billie-eilish-is-apple-musics-artist-of-the-year-for-2024/article/Apple-Music-2024-Artist-of-the-Year-Billie-Eilish_big.jpg.large_2x.jpg"
        const val SOUNDTRACK_LEAD_LIMIT = 10
        const val PORTRAIT_BATCH_LIMIT = 16
    }
}

fun soundtrackPrimaryArtist(rawArtist: String): String {
    var value = rawArtist.trim()
    listOf(",", " & ", " feat. ", " feat ", " featuring ", " x ").forEach { separator ->
        val index = value.indexOf(separator, ignoreCase = true)
        if (index > 0) value = value.substring(0, index).trim()
    }
    return value
}

internal fun highResolutionPortrait(url: String): String =
    url.replace(Regex("=w\\d+-h\\d+[^&?/]*$"), "=w1200-h1200-l90-rj")
        .replace(Regex("=s\\d+[^&?/]*$"), "=s1200")
