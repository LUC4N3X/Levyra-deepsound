package com.luc4n3x.levyra.player.car

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.media.MediaPlaybackManager
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.player.AndroidAutoLibrary
import com.luc4n3x.levyra.player.PlaybackService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/** Template-only Android Auto surface. Playback and browse data remain owned by PlaybackService. */
@UnstableApi
class LevyraCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = if (BuildConfig.DEBUG) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    } else {
        HostValidator.Builder(applicationContext)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }

    override fun onCreateSession(): Session = LevyraCarSession()
}

@UnstableApi
private class LevyraCarSession : Session() {
    private var browserFuture: ListenableFuture<MediaBrowser>? = null

    @Volatile
    private var playbackTokenRegistered = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) = connectPlayback()

            override fun onDestroy(owner: LifecycleOwner) {
                playbackTokenRegistered = false
                browserFuture?.let(MediaController::releaseFuture)
                browserFuture = null
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = if (intent.action in SHOW_PLAYBACK_ACTIONS) {
        LevyraNowPlayingCarScreen(carContext, ::browser, ::playbackReady)
    } else {
        LevyraLibraryCarScreen(carContext, ::browser, ::playbackReady)
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.action !in SHOW_PLAYBACK_ACTIONS) return
        val manager = carContext.getCarService(ScreenManager::class.java)
        if (manager.top !is LevyraNowPlayingCarScreen) {
            manager.popToRoot()
            manager.push(LevyraNowPlayingCarScreen(carContext, ::browser, ::playbackReady))
        }
    }

    private fun browser(): ListenableFuture<MediaBrowser> = browserFuture ?: MediaBrowser.Builder(
        carContext,
        SessionToken(carContext, ComponentName(carContext, PlaybackService::class.java))
    ).setListener(object : MediaBrowser.Listener {
        override fun onDisconnected(controller: MediaController) {
            playbackTokenRegistered = false
            invalidatePlaybackScreen()
            browserFuture?.let(MediaController::releaseFuture)
            browserFuture = null
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) connectPlayback()
        }
    }).buildAsync().also { browserFuture = it }

    private fun connectPlayback() {
        playbackTokenRegistered = false
        val executor = ContextCompat.getMainExecutor(carContext)
        val connection = browser()
        connection.addListener({
            runCatching {
                val result = connection.get().sendCustomCommand(
                    SessionCommand(PlaybackService.ACTION_GET_PLATFORM_TOKEN, Bundle.EMPTY),
                    Bundle.EMPTY
                )
                result.addListener({
                    runCatching {
                        val response = result.get()
                        val token = BundleCompat.getParcelable(
                            response.extras,
                            PlaybackService.KEY_PLATFORM_TOKEN,
                            android.media.session.MediaSession.Token::class.java
                        )
                        if (response.resultCode == SessionResult.RESULT_SUCCESS && token != null) {
                            (carContext.getCarService(CarContext.MEDIA_PLAYBACK_SERVICE) as MediaPlaybackManager)
                                .registerMediaPlaybackToken(MediaSessionCompat.Token.fromToken(token))
                            playbackTokenRegistered = true
                            invalidatePlaybackScreen()
                        }
                    }.onFailure {
                        playbackTokenRegistered = false
                        Timber.w(it, "Android Auto playback token registration failed")
                    }
                }, executor)
            }.onFailure {
                playbackTokenRegistered = false
                Timber.w(it, "Android Auto MediaBrowser connection failed")
            }
        }, executor)
    }

    private fun playbackReady(): Boolean = playbackTokenRegistered

    private fun invalidatePlaybackScreen() {
        (carContext.getCarService(ScreenManager::class.java).top as? LevyraNowPlayingCarScreen)?.invalidate()
    }

    private companion object {
        val SHOW_PLAYBACK_ACTIONS = setOf(
            "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK",
            "MEDIA_SHOW_PLAYBACK_VIEW"
        )
    }
}

@UnstableApi
private class LevyraLibraryCarScreen(
    carContext: CarContext,
    private val browserProvider: () -> ListenableFuture<MediaBrowser>,
    private val playbackReady: () -> Boolean
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeTab = AndroidAutoLibrary.ID_HOME
    private val children = mutableMapOf<String, List<MediaItem>?>()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = scope.cancel()
        })
        load(activeTab)
    }

    override fun onGetTemplate(): Template {
        val tabs = TabTemplate.Builder(object : TabTemplate.TabCallback {
            override fun onTabSelected(tabContentId: String) {
                activeTab = tabContentId
                load(tabContentId)
                invalidate()
            }
        }).setHeaderAction(Action.APP_ICON)
        CAR_TABS.forEach { tab ->
            tabs.addTab(
                Tab.Builder()
                    .setContentId(tab.id)
                    .setTitle(tab.title)
                    .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, tab.icon)).build())
                    .build()
            )
        }
        val list = ListTemplate.Builder()
            .addAction(searchAction())
            .addAction(nowPlayingAction())
        val loaded = children[activeTab]
        val contents = if (loaded == null) {
            list.setLoading(true).build()
        } else {
            list.setSingleList(mediaItemList(loaded, "Nessun contenuto")).build()
        }
        return tabs
            .setActiveTabContentId(activeTab)
            .setTabContents(TabContents.Builder(contents).build())
            .build()
    }

    private fun load(parentId: String) {
        if (children.containsKey(parentId)) return
        children[parentId] = null
        scope.launch {
            children[parentId] = loadChildren(browserProvider, parentId)
            invalidate()
        }
    }

    private fun mediaItemList(items: List<MediaItem>, emptyMessage: String): ItemList {
        val builder = ItemList.Builder().setNoItemsMessage(emptyMessage)
        items.take(MAX_ROWS).forEach { item -> builder.addItem(mediaRow(item)) }
        return builder.build()
    }

    private fun mediaRow(item: MediaItem): Row = Row.Builder()
        .setTitle(item.mediaMetadata.title?.toString().orEmpty().ifBlank { item.mediaId })
        .apply {
            (item.mediaMetadata.subtitle ?: item.mediaMetadata.artist)?.toString()
                ?.takeIf(String::isNotBlank)?.let(::addText)
            setBrowsable(item.mediaMetadata.isBrowsable == true)
        }
        .setOnClickListener {
            if (item.mediaMetadata.isBrowsable == true) {
                screenManager.push(
                    LevyraBrowseCarScreen(
                        carContext,
                        browserProvider,
                        playbackReady,
                        item.mediaId,
                        item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Levyra" }
                    )
                )
            } else {
                playItem(carContext, browserProvider, item)
                screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
            }
        }
        .build()

    private fun searchAction(): Action = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_widget_note)).build())
        .setOnClickListener { screenManager.push(LevyraSearchCarScreen(carContext, browserProvider, playbackReady)) }
        .build()

    private fun nowPlayingAction(): Action = Action.Builder(Action.MEDIA_PLAYBACK)
        .setOnClickListener { screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady)) }
        .build()

    private data class CarTab(val id: String, val title: String, val icon: Int)

    private companion object {
        const val MAX_ROWS = 80
        val CAR_TABS = listOf(
            CarTab(AndroidAutoLibrary.ID_HOME, "Home", R.drawable.ic_widget_note),
            CarTab(AndroidAutoLibrary.ID_DOWNLOADS, "Download", R.drawable.ic_widget_download),
            CarTab(AndroidAutoLibrary.ID_FAVORITES, "Preferiti", R.drawable.ic_widget_heart),
            CarTab(AndroidAutoLibrary.ID_PLAYLISTS, "Playlist", R.drawable.ic_notification_shuffle)
        )
    }
}

@UnstableApi
private class LevyraBrowseCarScreen(
    carContext: CarContext,
    private val browserProvider: () -> ListenableFuture<MediaBrowser>,
    private val playbackReady: () -> Boolean,
    private val parentId: String,
    private val title: String
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var children: List<MediaItem>? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = scope.cancel()
        })
        scope.launch {
            children = loadChildren(browserProvider, parentId)
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val builder = ListTemplate.Builder()
            .addAction(Action.Builder(Action.MEDIA_PLAYBACK).setOnClickListener {
                screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
            }.build())
            .setHeader(Header.Builder().setStartHeaderAction(Action.BACK).setTitle(title).build())
        val loaded = children ?: return builder.setLoading(true).build()
        val list = ItemList.Builder().setNoItemsMessage("Nessun contenuto")
        loaded.take(80).forEach { item ->
            list.addItem(Row.Builder()
                .setTitle(item.mediaMetadata.title?.toString().orEmpty().ifBlank { item.mediaId })
                .apply {
                    (item.mediaMetadata.subtitle ?: item.mediaMetadata.artist)?.toString()
                        ?.takeIf(String::isNotBlank)?.let(::addText)
                    setBrowsable(item.mediaMetadata.isBrowsable == true)
                }
                .setOnClickListener {
                    if (item.mediaMetadata.isBrowsable == true) {
                        screenManager.push(
                            LevyraBrowseCarScreen(
                                carContext,
                                browserProvider,
                                playbackReady,
                                item.mediaId,
                                item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Levyra" }
                            )
                        )
                    } else {
                        playItem(carContext, browserProvider, item)
                        screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
                    }
                }.build())
        }
        return builder.setSingleList(list.build()).build()
    }
}

@UnstableApi
private class LevyraSearchCarScreen(
    carContext: CarContext,
    private val browserProvider: () -> ListenableFuture<MediaBrowser>,
    private val playbackReady: () -> Boolean
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private var generation = 0L
    private var searching = false
    private var results: List<MediaItem>? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = scope.cancel()
        })
    }

    override fun onGetTemplate(): Template {
        val builder = SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) = Unit
            override fun onSearchSubmitted(searchText: String) = search(searchText)
        }).setHeaderAction(Action.BACK).setSearchHint("Cerca brani").setShowKeyboardByDefault(results == null)
        if (searching) return builder.setLoading(true).build()
        results?.let { items ->
            val list = ItemList.Builder().setNoItemsMessage("Nessun risultato")
            items.take(80).forEach { item ->
                list.addItem(Row.Builder()
                    .setTitle(item.mediaMetadata.title?.toString().orEmpty().ifBlank { item.mediaId })
                    .apply { item.mediaMetadata.artist?.toString()?.takeIf(String::isNotBlank)?.let(::addText) }
                    .setOnClickListener {
                        playItem(carContext, browserProvider, item)
                        screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
                    }.build())
            }
            builder.setItemList(list.build())
        }
        return builder.build()
    }

    private fun search(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val requestGeneration = ++generation
        searchJob?.cancel()
        searching = true
        invalidate()
        searchJob = scope.launch {
            val found = try {
                withContext(Dispatchers.IO) {
                    val browser = browserProvider().get(CAR_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    browser.search(clean, null).get(CAR_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    browser.getSearchResult(clean, 0, 80, null)
                        .get(CAR_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .value?.toList().orEmpty()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Android Auto template search failed")
                emptyList()
            }
            if (requestGeneration == generation) {
                results = found
                searching = false
                invalidate()
            }
        }
    }
}

@UnstableApi
private class LevyraNowPlayingCarScreen(
    carContext: CarContext,
    private val browserProvider: () -> ListenableFuture<MediaBrowser>,
    private val playbackReady: () -> Boolean
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        if (!playbackReady()) {
            return ListTemplate.Builder()
                .setLoading(true)
                .setHeader(Header.Builder().setTitle("Connessione al player").build())
                .build()
        }
        return MediaPlaybackTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("In riproduzione")
                    .addEndHeaderAction(
                        Action.Builder()
                            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_widget_note)).build())
                            .setOnClickListener {
                                screenManager.push(
                                    LevyraBrowseCarScreen(
                                        carContext,
                                        browserProvider,
                                        playbackReady,
                                        AndroidAutoLibrary.ID_QUEUE,
                                        "Coda"
                                    )
                                )
                            }.build()
                    ).build()
            ).build()
    }
}

@UnstableApi
private suspend fun loadChildren(
    browserProvider: () -> ListenableFuture<MediaBrowser>,
    parentId: String
): List<MediaItem> = try {
    withContext(Dispatchers.IO) {
        val browser = browserProvider().get(CAR_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        browser.getChildren(parentId, 0, 80, null)
            .get(CAR_BROWSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .value?.toList().orEmpty()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Timber.w(error, "Android Auto template browse failed for $parentId")
    emptyList()
}

private const val CAR_CONNECTION_TIMEOUT_SECONDS = 8L
private const val CAR_BROWSE_TIMEOUT_SECONDS = 15L
private const val CAR_SEARCH_TIMEOUT_SECONDS = 20L

@UnstableApi
private fun playItem(
    context: android.content.Context,
    browserProvider: () -> ListenableFuture<MediaBrowser>,
    item: MediaItem
) {
    val future = browserProvider()
    future.addListener({
        runCatching {
            future.get().apply {
                setMediaItem(item)
                prepare()
                play()
            }
        }.onFailure { Timber.w(it, "Android Auto template playback failed") }
    }, ContextCompat.getMainExecutor(context))
}
