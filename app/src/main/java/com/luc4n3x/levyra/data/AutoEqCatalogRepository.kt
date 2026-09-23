package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AutoEqCatalog
import com.luc4n3x.levyra.domain.AutoEqCatalogEntry
import com.luc4n3x.levyra.domain.AutoEqImporter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

internal interface AutoEqCatalogSource {
    suspend fun loadCatalog(): AutoEqCatalog?
    suspend fun loadProfile(entry: AutoEqCatalogEntry): String?
    fun releaseMemory()
}

internal class AutoEqCatalogRepository(context: Context) : AutoEqCatalogSource {
    private val appContext = context.applicationContext
    private val indexFile = File(appContext.filesDir, INDEX_FILE)
    private val profileDirectory = File(appContext.cacheDir, PROFILE_DIRECTORY)
    private val loadMutex = Mutex()

    @Volatile
    private var catalog: AutoEqCatalog? = null

    private val preferences by lazy { appContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE) }
    private val client: OkHttpClient by lazy {
        LevyraHttpClientFactory.feeds(appContext).newBuilder()
            .cache(null)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun loadCatalog(): AutoEqCatalog? = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            val nowMs = System.currentTimeMillis()
            val cached = catalog ?: readCachedCatalog()?.also { catalog = it }
            if (cached != null && !isStale(nowMs)) return@withLock cached
            val refreshed = refreshCatalog(hasCachedIndex = cached != null, nowMs = nowMs)
            (refreshed ?: cached)?.also { catalog = it }
        }
    }

    override suspend fun loadProfile(entry: AutoEqCatalogEntry): String? = withContext(Dispatchers.IO) {
        for (path in listOf(entry.parametricEqPath, entry.graphicEqPath)) {
            if (!isSafeProfilePath(path)) continue
            val cacheFile = File(profileDirectory, profileCacheName(path))
            readCachedProfile(cacheFile, path)?.let { return@withContext it }
            val text = downloadProfile(path) ?: continue
            storeProfile(cacheFile, text)
            return@withContext text
        }
        null
    }

    override fun releaseMemory() {
        catalog = null
    }

    private fun readCachedCatalog(): AutoEqCatalog? {
        if (!indexFile.isFile || indexFile.length() !in 1..MAX_INDEX_BYTES.toLong()) return null
        return try {
            AutoEqCatalog.parseIndex(indexFile.readText(Charsets.UTF_8)).takeIf { it.size >= MIN_VALID_ENTRIES }
        } catch (error: IOException) {
            Timber.d(error, "AutoEQ catalog cache unreadable")
            null
        }
    }

    private fun isStale(nowMs: Long): Boolean {
        val checkedAt = preferences.getLong(KEY_CHECKED_AT, 0L)
        return checkedAt <= 0L || checkedAt > nowMs || nowMs - checkedAt >= REFRESH_INTERVAL_MS
    }

    private fun refreshCatalog(hasCachedIndex: Boolean, nowMs: Long): AutoEqCatalog? {
        val etag = preferences.getString(KEY_ETAG, null)?.takeIf { hasCachedIndex && it.isNotBlank() }
        val request = Request.Builder()
            .url(INDEX_URL)
            .header("Accept", "text/markdown, text/plain")
            .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
            .apply { etag?.let { header("If-None-Match", it) } }
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == HTTP_NOT_MODIFIED && etag != null -> {
                        preferences.edit().putLong(KEY_CHECKED_AT, nowMs).apply()
                        null
                    }
                    !response.isSuccessful -> null
                    else -> {
                        val body = response.body
                        val bytes = body.byteStream().use { readBounded(it, body.contentLength(), MAX_INDEX_BYTES) }
                            ?: return@use null
                        val parsed = AutoEqCatalog.parseIndex(bytes.toString(Charsets.UTF_8))
                        if (parsed.size < MIN_VALID_ENTRIES) return@use null
                        writeAtomically(indexFile, bytes)
                        preferences.edit()
                            .putString(KEY_ETAG, response.header("ETag").orEmpty())
                            .putLong(KEY_CHECKED_AT, nowMs)
                            .apply()
                        parsed
                    }
                }
            }
        } catch (error: IOException) {
            Timber.d(error, "AutoEQ catalog refresh failed")
            null
        }
    }

    private fun downloadProfile(path: String): String? {
        val request = Request.Builder()
            .url("$REPOSITORY_ROOT/$path")
            .header("Accept", "text/plain")
            .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body
                val bytes = body.byteStream().use { readBounded(it, body.contentLength(), AutoEqImporter.MAX_INPUT_CHARS) }
                    ?: return@use null
                bytes.toString(Charsets.UTF_8).takeIf { isValidProfile(path, it) }
            }
        } catch (error: IOException) {
            Timber.d(error, "AutoEQ profile download failed")
            null
        }
    }

    private fun readCachedProfile(file: File, path: String): String? {
        if (!file.isFile || file.length() !in 1..AutoEqImporter.MAX_INPUT_CHARS.toLong()) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            if (!isValidProfile(path, text)) return null
            file.setLastModified(System.currentTimeMillis())
            text
        } catch (error: IOException) {
            Timber.d(error, "AutoEQ profile cache unreadable")
            null
        }
    }

    private fun isValidProfile(path: String, text: String): Boolean =
        if (path.endsWith(PARAMETRIC_SUFFIX)) {
            AutoEqImporter.parseParametric(text) is AutoEqImporter.ParametricParseResult.Success
        } else {
            AutoEqImporter.parse(text) is AutoEqImporter.ParseResult.Success
        }

    private fun storeProfile(file: File, text: String) {
        try {
            profileDirectory.mkdirs()
            writeAtomically(file, text.toByteArray(Charsets.UTF_8))
            profileDirectory.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_CACHED_PROFILES)
                ?.forEach { it.delete() }
        } catch (error: IOException) {
            Timber.d(error, "AutoEQ profile cache write failed")
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(target)) {
            target.delete()
            if (!temporary.renameTo(target)) {
                temporary.delete()
                throw IOException("Unable to replace ${target.name}")
            }
        }
    }

    companion object {
        private const val REPOSITORY_ROOT = "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master"
        private const val INDEX_URL = "$REPOSITORY_ROOT/results/INDEX.md"
        private const val STORE_NAME = "levyra_autoeq_catalog"
        private const val KEY_ETAG = "index_etag"
        private const val KEY_CHECKED_AT = "index_checked_at"
        private const val INDEX_FILE = "autoeq/INDEX.md"
        private const val PROFILE_DIRECTORY = "autoeq_profiles"
        private const val HTTP_NOT_MODIFIED = 304
        private const val MAX_INDEX_BYTES = 4 * 1_024 * 1_024
        private const val MIN_VALID_ENTRIES = 100
        private const val MAX_CACHED_PROFILES = 48
        private const val REFRESH_INTERVAL_MS = 7L * 24L * 60L * 60L * 1_000L
        private const val SAFE_PATH_CHARACTERS =
            "%()!$&'+,-./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~"

        private const val GRAPHIC_SUFFIX = "%20GraphicEQ.txt"
        private const val PARAMETRIC_SUFFIX = "%20ParametricEQ.txt"

        internal fun isSafeProfilePath(path: String): Boolean {
            if (!path.startsWith("results/") ||
                (!path.endsWith(GRAPHIC_SUFFIX) && !path.endsWith(PARAMETRIC_SUFFIX))
            ) return false
            if (path.any { it !in SAFE_PATH_CHARACTERS }) return false
            return path.split('/').none { it.isEmpty() || it == "." || it == ".." }
        }

        internal fun profileCacheName(path: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(path.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                .take(32) + ".txt"

        internal fun readBounded(input: InputStream, declaredLength: Long, maxBytes: Int): ByteArray? {
            if (declaredLength > maxBytes) return null
            val output = ByteArrayOutputStream(declaredLength.takeIf { it in 1..maxBytes.toLong() }?.toInt() ?: 8_192)
            val chunk = ByteArray(8_192)
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                if (output.size() + read > maxBytes) return null
                output.write(chunk, 0, read)
            }
            return output.toByteArray()
        }
    }
}
