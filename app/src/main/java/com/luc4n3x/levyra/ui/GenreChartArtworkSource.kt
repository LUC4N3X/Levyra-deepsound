package com.luc4n3x.levyra.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.luc4n3x.levyra.data.AppleArtistArtworkRepository
import com.luc4n3x.levyra.data.ArtistPortraitFallbackRepository
import com.luc4n3x.levyra.data.ExploreMoodArtwork
import com.luc4n3x.levyra.data.ExploreMoodArtworkCache
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.data.lastfm.LastFmGenreDiscoveryRepository
import com.luc4n3x.levyra.data.lastfm.LastFmGenreTags
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

private const val GenreChartLeadArtists = 3
private const val GenreChartBackupArtists = 4
private const val GenreChartCaptionArtists = 4
private val GenreChartFeaturedArtists = mapOf(
    "latino" to listOf("Bad Bunny", "Rauw Alejandro"),
    "rap-drill" to listOf("Kendrick Lamar", "Travis Scott")
)

@Immutable
internal data class GenreChartCoverData(val artworkUrl: String, val artists: List<String>)

private object GenreChartLastFmSource {
    @Volatile
    private var repository: LastFmGenreDiscoveryRepository? = null
    val artists = ConcurrentHashMap<String, List<String>>()

    fun get(context: Context): LastFmGenreDiscoveryRepository =
        repository ?: synchronized(this) {
            repository ?: LastFmGenreDiscoveryRepository(context.applicationContext).also { repository = it }
        }
}

internal fun genreChartPortraitCandidates(artists: List<String>, rotationBucket: Long): List<String> {
    val lead = artists.take(GenreChartLeadArtists)
    if (lead.isEmpty()) return emptyList()
    val start = Math.floorMod(rotationBucket, lead.size.toLong()).toInt()
    return lead.drop(start) + lead.take(start) + artists.drop(GenreChartLeadArtists).take(GenreChartBackupArtists)
}

@Composable
internal fun rememberGenreChartCover(zone: ExploreZone): GenreChartCoverData {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val fallback = rememberExploreMoodArtworkUrl(zone)
    val rotationBucket = remember(zone.id) { exploreGenreRotationBucket(System.currentTimeMillis()) }
    val cacheKey = remember(zone.id, strings.code) { "genre-chart-v3:${strings.code}:${zone.id}" }
    var portrait by remember(cacheKey) { mutableStateOf(ExploreMoodArtworkCache.peek(cacheKey)?.url.orEmpty()) }
    var artists by remember(cacheKey) { mutableStateOf(GenreChartLastFmSource.artists[cacheKey].orEmpty()) }
    LaunchedEffect(cacheKey, rotationBucket) {
        val app = context.applicationContext
        val cached = ExploreMoodArtworkCache.load(app, cacheKey)
        if (cached != null) portrait = cached.url
        val tags = LastFmGenreTags.forZone(zone.id)
        if (tags.isEmpty()) return@LaunchedEffect
        val loaded = try {
            val repository = GenreChartLastFmSource.get(app)
            repository.cached(tags, strings.code)?.artists?.takeIf { it.isNotEmpty() }?.let { stored ->
                GenreChartLastFmSource.artists[cacheKey] = stored
                artists = stored
            }
            repository.discovery(tags, strings.code)?.artists.orEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Genre chart artists unavailable for %s", zone.id)
            emptyList()
        }
        if (loaded.isNotEmpty()) {
            GenreChartLastFmSource.artists[cacheKey] = loaded
            artists = loaded
        }
        if (cached != null && cached.rotation == rotationBucket) return@LaunchedEffect
        val candidates = (GenreChartFeaturedArtists[zone.id].orEmpty() + genreChartPortraitCandidates(loaded, rotationBucket)).distinct()
        if (candidates.isEmpty()) return@LaunchedEffect
        val spotify = SpotifyArtistArtworkRepository.get(app)
        val apple = AppleArtistArtworkRepository.get(app)
        val fallbackPortraits = ArtistPortraitFallbackRepository.get(app)
        val resolved = ExploreMoodPortraitLookupSemaphore.withPermit {
            resolveExploreMoodPortrait(
                candidates = candidates,
                providers = listOf(
                    apple::resolveArtistPortrait,
                    spotify::resolveArtistPortrait,
                    fallbackPortraits::youtubePortrait,
                    fallbackPortraits::deezerPortrait
                )
            )
        }
        if (resolved.isNotBlank()) {
            ExploreMoodArtworkCache.store(app, cacheKey, ExploreMoodArtwork(resolved, rotationBucket))
            portrait = resolved
        }
    }
    return GenreChartCoverData(portrait.ifBlank { fallback }, artists.take(GenreChartCaptionArtists))
}
