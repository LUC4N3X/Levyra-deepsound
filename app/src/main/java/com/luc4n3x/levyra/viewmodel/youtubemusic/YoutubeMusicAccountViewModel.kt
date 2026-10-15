package com.luc4n3x.levyra.viewmodel.youtubemusic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.youtubemusic.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal enum class YoutubeMusicAccountError { Network, SignIn, Protocol, Storage, Limit }
internal typealias YoutubeMusicAccountNotice = YoutubeMusicImportOutcome

internal data class YoutubeMusicAccountUiState(
    val restoring: Boolean = true,
    val accounts: List<YoutubeMusicAccount> = emptyList(),
    val active: YoutubeMusicAccount? = null,
    val collection: YoutubeMusicCollection = YoutubeMusicCollection.Playlists,
    val pages: Map<YoutubeMusicCollection, YoutubeMusicLibraryPage> = emptyMap(),
    val detail: YoutubeMusicLibraryEntry? = null,
    val detailPage: YoutubeMusicLibraryPage? = null,
    val busy: Boolean = false,
    val showLogin: Boolean = false,
    val checkingLogin: Boolean = false,
    val error: YoutubeMusicAccountError? = null,
    val notice: YoutubeMusicAccountNotice? = null,
    val home: YoutubeMusicHome? = null,
    val homeFailed: Boolean = false
) {
    val currentPage: YoutubeMusicLibraryPage? get() = if (detail != null) detailPage else pages[collection]
    val homeSource: YoutubeMusicHomeSource get() = youtubeMusicHomeSource(restoring, active?.id, home, homeFailed)
}

internal class YoutubeMusicAccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicAccountRepository(application.applicationContext)
    private val homeCache = YoutubeMusicHomeCache(application.applicationContext)
    private val _state = MutableStateFlow(YoutubeMusicAccountUiState())
    val state = _state.asStateFlow()
    val homeSource: StateFlow<YoutubeMusicHomeSource> = _state
        .map { it.homeSource }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.homeSource)
    private var saved = YoutubeMusicSavedSessions()
    private val storageMutex = Mutex()
    private var operation: Job? = null
    private var generation = 0L
    private var language = "en"
    private val requestedPages = hashSetOf<String>()
    private var homeJob: Job? = null
    private var homeKey: String? = null
    private var fetchedHomeKey: String? = null

    init {
        viewModelScope.launch {
            try {
                val (restored, cachedHome) = withContext(Dispatchers.IO) {
                    val sessions = repository.sessions.load()
                    val activeId = sessions.activeId
                    val home = activeId?.let { id -> runCatching { homeCache.load(id) }.getOrNull() }
                    if (activeId == null) runCatching { homeCache.clear() }
                    sessions to home
                }
                saved = restored
                if (cachedHome != null) _state.update { it.copy(home = cachedHome) }
                publishSession()
            } catch (_: Exception) {
                _state.update { it.copy(error = YoutubeMusicAccountError.Storage, home = null) }
            } finally {
                _state.update { it.copy(restoring = false) }
            }
        }
    }

    fun loadHome(languageCode: String) {
        val session = saved.active ?: return
        val inFlight = homeKey.takeIf { homeJob?.isActive == true }
        if (!youtubeMusicHomeNeedsFetch(session.account.id, languageCode, fetchedHomeKey, inFlight)) return
        val key = youtubeMusicHomeKey(session.account.id, languageCode)
        homeJob?.cancel()
        homeKey = key
        homeJob = viewModelScope.launch {
            val home = try {
                repository.home(session, languageCode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (saved.activeId == session.account.id) _state.update { it.copy(homeFailed = true) }
                return@launch
            }
            if (saved.activeId != home.accountId) return@launch
            fetchedHomeKey = key
            _state.update { it.copy(home = home, homeFailed = false) }
            withContext(NonCancellable + Dispatchers.IO) {
                storageMutex.withLock {
                    if (saved.activeId == home.accountId) runCatching { homeCache.save(home) }
                }
            }
        }
    }

    suspend fun refreshHomeAndAwait(languageCode: String) {
        if (saved.active == null) return
        fetchedHomeKey = null
        loadHome(languageCode)
        homeJob?.join()
    }

    fun onNetworkRestored(languageCode: String) {
        loadHome(languageCode)
        if (state.value.error == YoutubeMusicAccountError.Network && !state.value.busy && !state.value.showLogin) retry()
    }

    fun open(languageCode: String) {
        language = languageCode
        if (!state.value.restoring && !state.value.busy && !state.value.showLogin && saved.active != null && state.value.currentPage == null) refresh()
    }

    fun beginLogin() {
        if (state.value.restoring) return
        cancelOperation()
        _state.update { it.copy(showLogin = true, error = null, notice = null) }
    }

    fun cancelLogin() {
        cancelOperation()
        _state.update { it.copy(showLogin = false, checkingLogin = false) }
    }

    fun acceptCookie(cookie: String) {
        if (state.value.busy || state.value.checkingLogin || !state.value.showLogin) return
        runOperation {
            _state.update { it.copy(checkingLogin = true) }
            val (accounts, verified) = verifyLogin(cookie)
            val selected = accounts.firstOrNull { it.selected } ?: accounts.first()
            coroutineContext.ensureActive()
            mutateSessions { it.withLogin(accounts.map { account ->
                YoutubeMusicSession(if (account.id == selected.id) verified else account, cookie)
            }, selected.id) }
            publishSession(clearLibrary = true)
            _state.update { it.copy(showLogin = false, checkingLogin = false) }
            loadPage(false, verifySession = false)
        }
    }

    private suspend fun verifyLogin(cookie: String): Pair<List<YoutubeMusicAccount>, YoutubeMusicAccount> {
        repeat(MAX_LOGIN_ATTEMPTS) { attempt ->
            try {
                val accounts = repository.client.accounts(cookie)
                if (accounts.isEmpty()) throw YoutubeMusicSessionExpired()
                val selected = accounts.firstOrNull { it.selected } ?: accounts.first()
                return accounts to repository.client.verify(YoutubeMusicSession(selected, cookie), language)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (attempt == MAX_LOGIN_ATTEMPTS - 1 || !isRetryableLoginError(error)) throw error
                delay(LOGIN_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw YoutubeMusicSessionExpired()
    }

    private fun isRetryableLoginError(error: Exception): Boolean = when (error) {
        is YoutubeMusicAccountHttpException -> error.status == 408 || error.status == 425 ||
            error.status == 429 || error.status in 500..599
        is YoutubeMusicSessionExpired, is YoutubeMusicProtocolException -> true
        is java.io.IOException -> true
        else -> false
    }

    fun switchAccount(id: String) {
        if (state.value.restoring) return
        runOperation(sessionId = id) {
            val session = saved.sessions.firstOrNull { it.account.id == id } ?: return@runOperation
            val verified = repository.client.verify(session, language)
            coroutineContext.ensureActive()
            mutateSessions { current -> current.copy(
                activeId = id,
                sessions = current.sessions.map { if (it.account.id == id) YoutubeMusicSession(verified, it.cookie) else it }
            ) }
            publishSession(clearLibrary = true)
            loadPage(false, verifySession = false)
        }
    }

    fun logout(id: String) {
        if (state.value.restoring) return
        runOperation {
            val wasActive = saved.activeId == id
            mutateSessions { it.without(id) }
            publishSession(clearLibrary = wasActive)
        }
    }

    fun logoutAll() {
        if (state.value.restoring) return
        runOperation {
            mutateSessions { YoutubeMusicSavedSessions() }
            publishSession(clearLibrary = true)
        }
    }

    fun selectCollection(collection: YoutubeMusicCollection) {
        cancelOperation()
        requestedPages.clear()
        _state.update { it.copy(collection = collection, detail = null, detailPage = null, error = null, notice = null) }
        if (state.value.currentPage == null && saved.active != null) refresh()
    }

    fun openDetail(entry: YoutubeMusicLibraryEntry) {
        cancelOperation()
        requestedPages.clear()
        _state.update { it.copy(detail = entry, detailPage = null, error = null, notice = null) }
        refresh()
    }

    fun closeDetail() {
        cancelOperation()
        requestedPages.clear()
        _state.update { it.copy(detail = null, detailPage = null, error = null, notice = null) }
    }

    fun refresh() {
        if (saved.active == null || state.value.restoring) return
        runOperation { loadPage(false) }
    }

    fun retry() {
        if (state.value.error == YoutubeMusicAccountError.Storage && saved.active == null) {
            runOperation {
                saved = withContext(Dispatchers.IO) {
                    try { repository.sessions.load() } catch (_: Exception) { throw YoutubeMusicAccountStorageException() }
                }
                publishSession(clearLibrary = true)
                if (saved.active != null) loadPage(false)
            }
        } else refresh()
    }

    fun loadMore() {
        if (state.value.busy || state.value.currentPage?.continuation.isNullOrBlank()) return
        runOperation { loadPage(true) }
    }

    private suspend fun loadPage(append: Boolean, verifySession: Boolean = !append) {
        val session = saved.active ?: return
        if (verifySession) repository.client.verify(session, language)
        val snapshot = state.value
        val old = snapshot.currentPage
        val continuation = preparePageToken(old, append)
        val collection = snapshot.detail?.collection ?: snapshot.collection
        val browseId = snapshot.detail?.browseId ?: collection.browseId
        val page = repository.page(session, collection, language, browseId, continuation, albumOf(snapshot.detail))
        coroutineContext.ensureActive()
        if (saved.activeId != session.account.id) return
        val combined = mergePage(old, page, continuation, append)
        _state.update { if (snapshot.detail != null) it.copy(detailPage = combined) else it.copy(pages = it.pages + (snapshot.collection to combined)) }
    }

    private fun albumOf(detail: YoutubeMusicLibraryEntry?): YoutubeMusicLibraryEntry? =
        detail?.takeIf { it.collection == YoutubeMusicCollection.Albums }

    private fun preparePageToken(old: YoutubeMusicLibraryPage?, append: Boolean): String {
        val token = if (append) old?.continuation.orEmpty() else ""
        if (!append) requestedPages.clear()
        if (token.isNotBlank() && (token in requestedPages || requestedPages.size >= 200)) throw YoutubeMusicLibraryLimit()
        return token
    }

    private fun mergePage(old: YoutubeMusicLibraryPage?, page: YoutubeMusicLibraryPage, token: String, append: Boolean): YoutubeMusicLibraryPage {
        if (token.isNotBlank()) requestedPages += token
        if (page.continuation.isNotBlank() && (page.continuation == token || page.continuation in requestedPages)) throw YoutubeMusicProtocolException()
        val combined = if (append) page.copy(
            entries = (old?.entries.orEmpty() + page.entries).distinctBy { it.browseId },
            tracks = (old?.tracks.orEmpty() + page.tracks).distinctBy { it.id }
        ) else page
        if (combined.entries.size + combined.tracks.size > 10000) throw YoutubeMusicLibraryLimit()
        return combined
    }

    fun importCurrent(onImported: () -> Unit) {
        val snapshot = state.value
        val session = saved.active ?: return
        if (snapshot.busy) return
        val collection = snapshot.detail?.collection ?: snapshot.collection
        if (collection == YoutubeMusicCollection.Artists || (snapshot.detail == null && collection != YoutubeMusicCollection.Likes)) return
        val browseId = snapshot.detail?.browseId ?: collection.browseId
        val title = snapshot.detail?.title ?: com.luc4n3x.levyra.ui.i18n.youtubeMusicCopy(language).likes
        runOperation {
            val tracks = repository.fullTracks(session, collection, browseId, language, albumOf(snapshot.detail))
            coroutineContext.ensureActive()
            if (saved.activeId != session.account.id) return@runOperation
            val favorites: (suspend (List<com.luc4n3x.levyra.domain.Track>) -> Unit)? =
                if (collection == YoutubeMusicCollection.Likes && snapshot.detail == null) {
                    { items -> com.luc4n3x.levyra.data.FavoritesStore(getApplication<Application>()).addFavorites(items) }
                } else null
            val notice = importYoutubeMusicItems(tracks, { items -> repository.importTracks(session, browseId, title, items) }, favorites)
            _state.update { it.copy(notice = notice) }
            onImported()
        }
    }

    fun importSong(track: com.luc4n3x.levyra.domain.Track, onImported: () -> Unit) {
        val session = saved.active ?: return
        if (state.value.busy) return
        runOperation {
            val notice = importYoutubeMusicItems(listOf(track), { repository.importTracks(session, "songs", "YouTube Music", it) })
            _state.update { it.copy(notice = notice) }
            onImported()
        }
    }

    private suspend fun mutateSessions(transform: (YoutubeMusicSavedSessions) -> YoutubeMusicSavedSessions) {
        withContext(NonCancellable + Dispatchers.IO) {
            storageMutex.withLock {
                val next = transform(saved)
                val previousId = saved.activeId
                try {
                    repository.sessions.save(next)
                } catch (_: Exception) {
                    throw YoutubeMusicAccountStorageException()
                }
                saved = next
                val accountChanged = previousId != next.activeId
                if (accountChanged) runCatching { homeCache.clear() }
                withContext(Dispatchers.Main.immediate) {
                    if (accountChanged) fetchedHomeKey = null
                    publishSession(clearLibrary = accountChanged)
                }
            }
        }
    }

    private fun publishSession(clearLibrary: Boolean = false) {
        if (clearLibrary) requestedPages.clear()
        _state.update { it.copy(
            accounts = saved.sessions.map { it.account }, active = saved.active?.account,
            home = it.home?.takeIf { home -> home.accountId == saved.activeId },
            homeFailed = it.homeFailed && it.active?.id == saved.activeId,
            pages = if (clearLibrary) emptyMap() else it.pages,
            detail = if (clearLibrary) null else it.detail, detailPage = if (clearLibrary) null else it.detailPage,
            error = null, notice = null
        ) }
    }

    private fun cancelOperation() {
        generation++
        operation?.cancel()
        _state.update { it.copy(busy = false, checkingLogin = false) }
    }

    private fun runOperation(sessionId: String? = saved.activeId, block: suspend () -> Unit) {
        cancelOperation()
        val ownGeneration = generation
        _state.update { it.copy(busy = true, error = null, notice = null) }
        operation = viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != ownGeneration) return@launch
                val kind = when (error) {
                    is YoutubeMusicSessionExpired -> YoutubeMusicAccountError.SignIn
                    is YoutubeMusicProtocolException -> YoutubeMusicAccountError.Protocol
                    is YoutubeMusicLibraryLimit -> YoutubeMusicAccountError.Limit
                    is YoutubeMusicAccountStorageException, is java.security.GeneralSecurityException -> YoutubeMusicAccountError.Storage
                    else -> YoutubeMusicAccountError.Network
                }
                if (kind == YoutubeMusicAccountError.SignIn && !state.value.showLogin && saved.activeId == sessionId) {
                    try {
                        mutateSessions { it.copy(activeId = null) }
                        publishSession(clearLibrary = true)
                    } catch (_: YoutubeMusicAccountStorageException) {
                        _state.update { it.copy(error = YoutubeMusicAccountError.Storage) }
                        return@launch
                    }
                }
                _state.update { it.copy(error = kind) }
            } finally {
                if (generation == ownGeneration) _state.update { it.copy(busy = false, checkingLogin = false) }
            }
        }
    }

    private companion object {
        const val MAX_LOGIN_ATTEMPTS = 3
        const val LOGIN_RETRY_DELAY_MS = 500L
    }
}
