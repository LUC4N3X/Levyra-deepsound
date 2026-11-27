package com.luc4n3x.levyra.desktop.core.account

import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccount
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountClient
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountHttpException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkPayload
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkResult
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkStatus
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicProtocolException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSavedSessions
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSession
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionCodec
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionExpired
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

data class DesktopYoutubeMusicAccount(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val active: Boolean
)

class DesktopYoutubeMusicAccounts internal constructor(
    private val file: Path,
    private val box: SecretBox,
    private val client: YoutubeMusicAccountClient = YoutubeMusicAccountClient(),
    private val language: () -> String = { "en" }
) {
    constructor(root: Path, language: () -> String) : this(
        root.resolve(SESSION_FILE),
        platformSecretBox(root),
        YoutubeMusicAccountClient(),
        language
    )

    private val lock = Mutex()
    private var saved = YoutubeMusicSavedSessions()
    private var loaded = false
    private val _accounts = MutableStateFlow<List<DesktopYoutubeMusicAccount>>(emptyList())
    val accounts: StateFlow<List<DesktopYoutubeMusicAccount>> = _accounts.asStateFlow()

    suspend fun load(): Boolean = lock.withLock { stored { ensureLoaded() } != null }

    internal suspend fun activeSession(): YoutubeMusicSession? = lock.withLock {
        stored { ensureLoaded() }?.let { saved.active }
    }

    val activeAccountId: String? get() = accounts.value.firstOrNull { it.active }?.id

    fun newLinkHost(): YoutubeMusicLinkHost = YoutubeMusicLinkHost(::signInResult)

    suspend fun browse(browseId: String, language: String): JSONObject? {
        val session = activeSession() ?: return null
        return try {
            client.browse(session, browseId, language)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signIn(cookie: String, preferredAccountId: String = ""): Boolean =
        signInResult(YoutubeMusicLinkPayload(cookie, preferredAccountId)).status == YoutubeMusicLinkStatus.Accepted

    internal suspend fun signInResult(payload: YoutubeMusicLinkPayload): YoutubeMusicLinkResult {
        val (accounts, verified) = try {
            verify(payload.cookie)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return YoutubeMusicLinkResult(YoutubeMusicLinkStatus.SignInFailed, emptyList())
        }
        val selected = accounts.firstOrNull { it.id == payload.accountId } ?: verified
        val selectedVerified = if (selected.id == verified.id) verified else runCatching {
            client.verify(YoutubeMusicSession(selected, payload.cookie), language())
        }.getOrDefault(selected)
        lock.withLock {
            stored {
                ensureLoaded()
                persist(saved.withLogin(accounts.map { account ->
                    YoutubeMusicSession(if (account.id == selectedVerified.id) selectedVerified else account, payload.cookie)
                }, selectedVerified.id))
            }
        } ?: return YoutubeMusicLinkResult(YoutubeMusicLinkStatus.SignInFailed, emptyList())
        return YoutubeMusicLinkResult(YoutubeMusicLinkStatus.Accepted, listOf(selectedVerified.name).filter(String::isNotBlank))
    }

    suspend fun switchTo(id: String): Boolean = lock.withLock {
        stored {
            ensureLoaded()
            if (saved.sessions.any { it.account.id == id }) persist(saved.copy(activeId = id))
        } != null
    }

    suspend fun signOut(id: String): Boolean = lock.withLock {
        stored {
            ensureLoaded()
            persist(saved.without(id))
        } != null
    }

    private inline fun <T> stored(block: () -> T): T? = try {
        block()
    } catch (_: IOException) {
        null
    }

    internal suspend fun expire(id: String) = signOut(id)

    private suspend fun verify(cookie: String): Pair<List<YoutubeMusicAccount>, YoutubeMusicAccount> {
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val accounts = client.accounts(cookie)
                if (accounts.isEmpty()) throw YoutubeMusicSessionExpired()
                val selected = accounts.firstOrNull { it.selected } ?: accounts.first()
                return accounts to client.verify(YoutubeMusicSession(selected, cookie), language())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (attempt == MAX_ATTEMPTS - 1 || !retryable(error)) throw error
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw YoutubeMusicSessionExpired()
    }

    private fun retryable(error: Exception): Boolean = when (error) {
        is YoutubeMusicAccountHttpException -> error.status == 408 || error.status == 425 || error.status == 429 || error.status in 500..599
        is YoutubeMusicSessionExpired, is YoutubeMusicProtocolException -> true
        is IOException -> true
        else -> false
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        saved = withContext(Dispatchers.IO) {
            runCatching {
                if (!Files.isRegularFile(file)) return@runCatching YoutubeMusicSavedSessions()
                val bytes = Files.readAllBytes(file)
                if (bytes.size > MAX_FILE_BYTES) return@runCatching YoutubeMusicSavedSessions()
                val plain = box.open(bytes)
                try {
                    YoutubeMusicSessionCodec.decode(String(plain, Charsets.UTF_8))
                } finally {
                    plain.fill(0)
                }
            }.getOrElse { error ->
                if (error is JSONException || error is YoutubeMusicProtocolException || error is SecretBoxUnavailable) YoutubeMusicSavedSessions()
                else throw error
            }
        }
        loaded = true
        publish()
    }

    private suspend fun persist(next: YoutubeMusicSavedSessions) {
        withContext(Dispatchers.IO) {
            if (next.sessions.isEmpty()) {
                Files.deleteIfExists(file)
            } else {
                val plain = YoutubeMusicSessionCodec.encode(next).toByteArray(Charsets.UTF_8)
                val sealed = try {
                    box.seal(plain)
                } finally {
                    plain.fill(0)
                }
                Files.createDirectories(file.parent)
                val temp = Files.createTempFile(file.parent, "ytm", ".tmp")
                try {
                    Files.write(temp, sealed)
                    moveAtomically(temp, file)
                } finally {
                    Files.deleteIfExists(temp)
                }
            }
        }
        saved = next
        publish()
    }

    private fun publish() {
        _accounts.value = saved.sessions.map { session ->
            val account = session.account
            DesktopYoutubeMusicAccount(account.id, account.name, account.handle, account.avatarUrl, account.id == saved.activeId)
        }
    }

    private companion object {
        const val SESSION_FILE = "youtube-music-session.bin"
        const val MAX_FILE_BYTES = 1024 * 1024
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 600L
    }
}
