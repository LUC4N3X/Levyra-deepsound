package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.EditorialChartsRepository
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.apple.AppleMetadataEnricher
import com.luc4n3x.levyra.data.locallibrary.LocalLibraryRepository
import com.luc4n3x.levyra.data.locallibrary.toLocalTrack
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.playlistimport.AppleMusicPlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.BandcampAlbumAdapter
import com.luc4n3x.levyra.data.playlistimport.DeezerPlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.ImportAlbumSource
import com.luc4n3x.levyra.data.playlistimport.ImportSearchBackoff
import com.luc4n3x.levyra.data.playlistimport.JioSaavnPlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.LevyraSharePlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportCatalog
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportFetcher
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportMetadata
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSessionStore
import com.luc4n3x.levyra.data.playlistimport.SpotifyPlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.TextPlaylistAdapter
import com.luc4n3x.levyra.data.playlistimport.YoutubePlaylistAdapter
import com.luc4n3x.levyra.data.spotify.SpotifySearchClient
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.playlistimport.PlaylistImportNotice
import com.luc4n3x.levyra.feature.playlistimport.PlaylistImportService
import com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy
import com.luc4n3x.levyra.ui.playlistimport.playlistImportProgressLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

interface PlaylistImportHost {
    fun languageCode(): String

    fun playlistsChanged()

    fun playlistAppended(playlistId: String) = Unit

    fun openPlaylist(playlistId: String)
}

class PlaylistImportEngine private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = YoutubeMusicRepository(appContext)
    private val playlistStore = PlaylistStore(appContext)
    private val localLibrary = LocalLibraryRepository.get(appContext)
    private val preferences = LevyraPreferences(appContext)

    @Volatile
    private var host: PlaylistImportHost? = null

    private fun languageCode(): String = host?.languageCode() ?: preferences.languageCode()

    private fun country(): String = ChartsCatalog.defaultRegionForLanguage(languageCode()).country

    private val spotifySearch by lazy {
        SpotifySearchClient(
            catalogSearch = EditorialChartsRepository.get(appContext)::searchSpotifyMetadata,
            countryProvider = ::country
        )
    }

    val catalog: PlaylistImportCatalog by lazy {
        val metadata = PlaylistImportMetadata(
            AppleMetadataEnricher(appContext),
            { query -> spotifySearch.search(query, 12).songs },
            ::country
        )
        PlaylistImportCatalog(
            repository = repository,
            localTracks = { localLibrary.availableMedia.first().map { it.toLocalTrack() } },
            languageCode = ::languageCode,
            metadataSearch = metadata::search,
            youtubeSearch = { query -> repository.searchYoutubeForImport(query, 12) },
            albumSource = object : ImportAlbumSource {
                override suspend fun albums(query: String, languageCode: String) =
                    repository.searchAlbumsForImport(query, languageCode)

                override suspend fun tracks(album: AlbumHit, languageCode: String) =
                    repository.albumTracksForImport(album, languageCode)
            },
            durationLookup = { videoId, languageCode -> repository.videoDurationForImport(videoId, languageCode) },
            searchBackoff = ImportSearchBackoff()
        )
    }

    val controller: PlaylistImportController by lazy {
        val fetcher = PlaylistImportFetcher(LevyraHttpClientFactory.media(appContext))
        PlaylistImportController(
            scope = scope,
            adapters = listOf(
                LevyraSharePlaylistAdapter(),
                TextPlaylistAdapter(),
                YoutubePlaylistAdapter(repository, ::languageCode),
                SpotifyPlaylistAdapter(fetcher),
                DeezerPlaylistAdapter(fetcher),
                AppleMusicPlaylistAdapter(fetcher),
                JioSaavnPlaylistAdapter(fetcher),
                BandcampAlbumAdapter(fetcher)
            ),
            catalog = catalog,
            store = PlaylistImportSessionStore(File(appContext.filesDir, "playlist-import-sessions")),
            gateway = object : PlaylistImportGateway {
                override fun languageCode(): String = this@PlaylistImportEngine.languageCode()

                override suspend fun commit(name: String, tracks: List<Track>, playlistId: String) =
                    playlistStore.createWithTracks(name, tracks, playlistId)

                override suspend fun append(playlistId: String, tracks: List<Track>) {
                    playlistStore.addTracks(playlistId, tracks)
                    host?.playlistAppended(playlistId)
                }

                override fun playlistsChanged() {
                    host?.playlistsChanged()
                }

                override fun openPlaylist(playlistId: String) {
                    host?.openPlaylist(playlistId)
                }
            }
        ).also(::keepAliveWhileBusy)
    }

    fun attach(next: PlaylistImportHost) {
        host = next
    }

    fun detach(previous: PlaylistImportHost) {
        if (host === previous) host = null
    }

    private fun keepAliveWhileBusy(target: PlaylistImportController) {
        scope.launch {
            target.state.collect { state ->
                val notice = state.takeIf { it.busy }?.let {
                    val copy = playlistImportHubCopy(languageCode())
                    PlaylistImportNotice(
                        title = copy["title"],
                        text = playlistImportProgressLine(it, copy),
                        done = it.progressDone,
                        total = it.progressTotal
                    )
                }
                PlaylistImportService.update(appContext, notice)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PlaylistImportEngine? = null

        fun get(context: Context): PlaylistImportEngine =
            instance ?: synchronized(this) {
                instance ?: PlaylistImportEngine(context).also { instance = it }
            }
    }
}
