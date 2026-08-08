package com.luc4n3x.levyra.player.car

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Looper
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
import com.google.common.util.concurrent.MoreExecutors
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.player.AndroidAutoLibrary
import com.luc4n3x.levyra.player.PlaybackService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var browserFuture: ListenableFuture<MediaBrowser>? = null
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    @Volatile
    private var playbackTokenRegistered = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) = connectPlayback(resetAttempts = true)

            override fun onDestroy(owner: LifecycleOwner) {
                playbackTokenRegistered = false
                connectionJob?.cancel()
                reconnectJob?.cancel()
                releaseBrowser()
                scope.cancel()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        if (browserFuture == null && lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            connectPlayback(resetAttempts = true)
        }
        return if (intent.action in SHOW_PLAYBACK_ACTIONS) {
            LevyraNowPlayingCarScreen(carContext, ::browser, ::playbackReady)
        } else {
            LevyraLibraryCarScreen(carContext, ::browser, ::playbackReady)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.action !in SHOW_PLAYBACK_ACTIONS) return
        if (browserFuture == null) connectPlayback(resetAttempts = true)
        val manager = carContext.getCarService(ScreenManager::class.java)
        if (manager.top !is LevyraNowPlayingCarScreen) {
            manager.popToRoot()
            manager.push(LevyraNowPlayingCarScreen(carContext, ::browser, ::playbackReady))
        }
    }

    private fun browser(): ListenableFuture<MediaBrowser> {
        checkMainThread()
        browserFuture?.let { return it }
        return MediaBrowser.Builder(
            carContext,
            SessionToken(carContext, ComponentName(carContext, PlaybackService::class.java))
        ).setListener(object : MediaBrowser.Listener {
            override fun onDisconnected(controller: MediaController) {
                scope.launch { handleDisconnected(controller) }
            }
        }).buildAsync().also { browserFuture = it }
    }

    private fun connectPlayback(resetAttempts: Boolean = false) {
        checkMainThread()
        if (resetAttempts) reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
        connectionJob?.cancel()
        playbackTokenRegistered = false
        connectionJob = scope.launch {
            try {
                val connection = browser().awaitCarFuture(CAR_CONNECTION_TIMEOUT_MS)
                val response = connection.sendCustomCommand(
                    SessionCommand(PlaybackService.ACTION_GET_PLATFORM_TOKEN, Bundle.EMPTY),
                    Bundle.EMPTY
                ).awaitCarFuture(CAR_COMMAND_TIMEOUT_MS)
                val token = BundleCompat.getParcelable(
                    response.extras,
                    PlaybackService.KEY_PLATFORM_TOKEN,
                    android.media.session.MediaSession.Token::class.java
                )
                if (response.resultCode != SessionResult.RESULT_SUCCESS || token == null) {
                    throw IllegalStateException("Playback token unavailable")
                }
                (carContext.getCarService(CarContext.MEDIA_PLAYBACK_SERVICE) as MediaPlaybackManager)
                    .registerMediaPlaybackToken(MediaSessionCompat.Token.fromToken(token))
                playbackTokenRegistered = true
                reconnectAttempts = 0
                invalidatePlaybackScreen()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                playbackTokenRegistered = false
                Timber.w(error, "Android Auto MediaBrowser connection failed")
                scheduleReconnect()
            }
        }
    }

    private fun handleDisconnected(controller: MediaController) {
        checkMainThread()
        val activeFuture = browserFuture ?: return
        val activeController = if (activeFuture.isDone) runCatching { activeFuture.get() }.getOrNull() else null
        if (activeController != null && activeController !== controller) return
        playbackTokenRegistered = false
        invalidatePlaybackScreen()
        releaseBrowser()
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        checkMainThread()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Timber.w("Android Auto reconnect limit reached")
            return
        }
        if (reconnectJob?.isActive == true) return
        releaseBrowser()
        val attempt = ++reconnectAttempts
        reconnectJob = scope.launch {
            delay(RECONNECT_BASE_DELAY_MS * attempt)
            reconnectJob = null
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) connectPlayback()
        }
    }

    private fun releaseBrowser() {
        checkMainThread()
        val owned = browserFuture ?: return
        browserFuture = null
        runCatching { MediaController.releaseFuture(owned) }
            .onFailure { Timber.w(it, "Android Auto MediaBrowser release failed") }
    }

    private fun playbackReady(): Boolean = playbackTokenRegistered

    private fun invalidatePlaybackScreen() {
        (carContext.getCarService(ScreenManager::class.java).top as? LevyraNowPlayingCarScreen)?.invalidate()
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Android Auto session state must stay on the main thread" }
    }

    private companion object {
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val RECONNECT_BASE_DELAY_MS = 1_000L

        val SHOW_PLAYBACK_ACTIONS = setOf(
            "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK",
            "MEDIA_SHOW_PLAYBACK_VIEW"
        )
    }
}

private sealed interface CarBrowseState {
    data object Loading : CarBrowseState
    data class Loaded(val items: List<MediaItem>) : CarBrowseState
    data object Failed : CarBrowseState
}

private sealed interface CarBrowseResult {
    data class Success(val items: List<MediaItem>) : CarBrowseResult
    data object Failure : CarBrowseResult
}

@UnstableApi
private class LevyraLibraryCarScreen(
    carContext: CarContext,
    private val browserProvider: () -> ListenableFuture<MediaBrowser>,
    private val playbackReady: () -> Boolean
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeTab = AndroidAutoLibrary.ID_HOME
    private val children = mutableMapOf<String, CarBrowseState>()

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
                    .setTitle(carContext.getString(tab.titleRes))
                    .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, tab.icon)).build())
                    .build()
            )
        }
        val list = ListTemplate.Builder()
            .addAction(searchAction())
            .addAction(nowPlayingAction())
        val contents = when (val state = children[activeTab]) {
            null, CarBrowseState.Loading -> list.setLoading(true).build()
            CarBrowseState.Failed -> list
                .setSingleList(mediaItemList(emptyList(), carContext.getString(R.string.car_content_unavailable)))
                .build()
            is CarBrowseState.Loaded -> list
                .setSingleList(mediaItemList(state.items, carContext.getString(R.string.car_no_content)))
                .build()
        }
        return tabs
            .setActiveTabContentId(activeTab)
            .setTabContents(TabContents.Builder(contents).build())
            .build()
    }

    private fun load(parentId: String) {
        when (children[parentId]) {
            CarBrowseState.Loading, is CarBrowseState.Loaded -> return
            null, CarBrowseState.Failed -> Unit
        }
        children[parentId] = CarBrowseState.Loading
        scope.launch {
            children[parentId] = when (val result = loadChildren(browserProvider, parentId)) {
                is CarBrowseResult.Success -> CarBrowseState.Loaded(result.items)
                CarBrowseResult.Failure -> CarBrowseState.Failed
            }
            invalidate()
        }
    }

    private fun mediaItemList(items: List<MediaItem>, emptyMessage: String): ItemList {
        val builder = ItemList.Builder().setNoItemsMessage(emptyMessage)
        items.take(MAX_ROWS).forEach { item -> builder.addItem(mediaRow(item, items)) }
        return builder.build()
    }

    private fun mediaRow(item: MediaItem, siblings: List<MediaItem>): Row = Row.Builder()
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
                playItem(carContext, browserProvider, item, siblings)
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

    private data class CarTab(val id: String, val titleRes: Int, val icon: Int)

    private companion object {
        const val MAX_ROWS = 80
        val CAR_TABS = listOf(
            CarTab(AndroidAutoLibrary.ID_HOME, R.string.car_home, R.drawable.ic_widget_note),
            CarTab(AndroidAutoLibrary.ID_DOWNLOADS, R.string.car_downloads, R.drawable.ic_widget_download),
            CarTab(AndroidAutoLibrary.ID_FAVORITES, R.string.car_favorites, R.drawable.ic_widget_heart),
            CarTab(AndroidAutoLibrary.ID_PLAYLISTS, R.string.car_playlists, R.drawable.ic_notification_shuffle)
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
    private var state: CarBrowseState = CarBrowseState.Loading

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = scope.cancel()
        })
        scope.launch {
            state = when (val result = loadChildren(browserProvider, parentId)) {
                is CarBrowseResult.Success -> CarBrowseState.Loaded(result.items)
                CarBrowseResult.Failure -> CarBrowseState.Failed
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val builder = ListTemplate.Builder()
            .addAction(Action.Builder(Action.MEDIA_PLAYBACK).setOnClickListener {
                screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
            }.build())
            .setHeader(Header.Builder().setStartHeaderAction(Action.BACK).setTitle(title).build())
        return when (val loaded = state) {
            CarBrowseState.Loading -> builder.setLoading(true).build()
            CarBrowseState.Failed -> builder
                .setSingleList(ItemList.Builder().setNoItemsMessage(carContext.getString(R.string.car_content_unavailable)).build())
                .build()
            is CarBrowseState.Loaded -> {
                val list = ItemList.Builder().setNoItemsMessage(carContext.getString(R.string.car_no_content))
                loaded.items.take(80).forEach { item ->
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
                                playItem(carContext, browserProvider, item, loaded.items)
                                screenManager.push(LevyraNowPlayingCarScreen(carContext, browserProvider, playbackReady))
                            }
                        }.build())
                }
                builder.setSingleList(list.build()).build()
            }
        }
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
    private var searchFailed = false
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
        }).setHeaderAction(Action.BACK)
            .setSearchHint(carContext.getString(R.string.car_search_tracks))
            .setShowKeyboardByDefault(results == null)
        if (searching) return builder.setLoading(true).build()
        results?.let { items ->
            val emptyMessage = if (searchFailed) R.string.car_content_unavailable else R.string.car_no_results
            val list = ItemList.Builder().setNoItemsMessage(carContext.getString(emptyMessage))
            items.take(80).forEach { item ->
                list.addItem(Row.Builder()
                    .setTitle(item.mediaMetadata.title?.toString().orEmpty().ifBlank { item.mediaId })
                    .apply { item.mediaMetadata.artist?.toString()?.takeIf(String::isNotBlank)?.let(::addText) }
                    .setOnClickListener {
                        playItem(carContext, browserProvider, item, items)
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
        searchFailed = false
        invalidate()
        searchJob = scope.launch {
            val found = try {
                val browser = browserProvider().awaitCarFuture(CAR_CONNECTION_TIMEOUT_MS)
                browser.search(clean, null).awaitCarFuture(CAR_SEARCH_TIMEOUT_MS)
                browser.getSearchResult(clean, 0, 80, null)
                    .awaitCarFuture(CAR_SEARCH_TIMEOUT_MS)
                    .value?.toList().orEmpty()
            } catch (timeout: TimeoutCancellationException) {
                Timber.w(timeout, "Android Auto template search timed out")
                searchFailed = true
                emptyList()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Android Auto template search failed")
                searchFailed = true
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
                .setHeader(Header.Builder().setTitle(carContext.getString(R.string.car_connecting_player)).build())
                .build()
        }
        return MediaPlaybackTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_now_playing))
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
                                        carContext.getString(R.string.car_queue)
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
): CarBrowseResult = try {
    val browser = browserProvider().awaitCarFuture(CAR_CONNECTION_TIMEOUT_MS)
    val items = browser.getChildren(parentId, 0, 80, null)
        .awaitCarFuture(CAR_BROWSE_TIMEOUT_MS)
        .value?.toList().orEmpty()
    CarBrowseResult.Success(items)
} catch (timeout: TimeoutCancellationException) {
    Timber.w(timeout, "Android Auto template browse timed out for $parentId")
    CarBrowseResult.Failure
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Timber.w(error, "Android Auto template browse failed for $parentId")
    CarBrowseResult.Failure
}

private suspend fun <T> ListenableFuture<T>.awaitCarFuture(timeoutMs: Long): T = withTimeout(timeoutMs) {
    suspendCancellableCoroutine { continuation ->
        addListener({
            if (!continuation.isActive) return@addListener
            try {
                continuation.resume(get())
            } catch (error: ExecutionException) {
                continuation.resumeWithException(error.cause ?: error)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, MoreExecutors.directExecutor())
    }
}

private const val CAR_CONNECTION_TIMEOUT_MS = 8_000L
private const val CAR_BROWSE_TIMEOUT_MS = 15_000L
private const val CAR_SEARCH_TIMEOUT_MS = 20_000L
private const val CAR_COMMAND_TIMEOUT_MS = 8_000L

@UnstableApi
private fun playItem(
    context: android.content.Context,
    browserProvider: () -> ListenableFuture<MediaBrowser>,
    item: MediaItem,
    siblings: List<MediaItem>
) {
    val future = browserProvider()
    future.addListener({
        runCatching {
            val browser = future.get()
            val playable = siblings.filter { it.mediaMetadata.isBrowsable != true }
            val queue = playable.ifEmpty { listOf(item) }
            val selectedIndex = queue.indexOfFirst { it.mediaId == item.mediaId }.coerceAtLeast(0)
            browser.setMediaItems(queue, selectedIndex, 0L)
            browser.prepare()
            browser.play()
        }.onFailure { Timber.w(it, "Android Auto template playback failed") }
    }, ContextCompat.getMainExecutor(context))
}
