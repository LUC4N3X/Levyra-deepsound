package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Build
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.nexus.update.LevyraUpdateArtifact
import com.luc4n3x.levyra.nexus.update.LevyraUpdateSelector
import com.luc4n3x.levyra.nexus.update.LevyraVersionComparator
import com.luc4n3x.levyra.update.AppUpdateContract
import com.luc4n3x.levyra.update.isPublicUpdateAddress
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.ResponseBody
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.json.JSONObject

internal data class InstallableAppUpdate(
    val info: AppUpdateInfo,
    val assetDownloadUrl: String,
    val assetSizeBytes: Long
)

class AppUpdateRepository(context: Context) {
    private val baseClient = LevyraHttpClientFactory.general(context.applicationContext)
    private val client = baseClient.newBuilder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (!hostname.equals(UPDATE_METADATA_HOST, ignoreCase = true)) {
                    throw UnknownHostException("Update metadata host not allowed")
                }
                val addresses = baseClient.dns.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !isPublicUpdateAddress(it) }) {
                    throw UnknownHostException("Update metadata destination is not public")
                }
                return addresses
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .connectTimeout(UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(UPDATE_METADATA_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(UPDATE_METADATA_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        parseLatestRelease(fetchLatestRelease()).info
    }

    internal suspend fun latestInstallable(): InstallableAppUpdate = withContext(Dispatchers.IO) {
        val update = parseLatestRelease(fetchLatestRelease())
        if (!update.info.isNewer) throw IllegalStateException("No newer Levyra release is available")
        if (!update.info.directApk || update.assetDownloadUrl.isBlank()) {
            throw IllegalStateException("The latest Levyra release has no installable APK")
        }
        update
    }

    internal fun validateLatestReleaseMetadata(): Boolean {
        val root = fetchLatestRelease()
        return root.optString("tag_name").isNotBlank() || root.optString("name").isNotBlank()
    }

    private fun fetchLatestRelease(): JSONObject {
        val endpoint = validateLevyraReleaseMetadataUrl(BuildConfig.UPDATE_LATEST_URL)
            ?: throw IOException("Invalid update metadata endpoint")
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isRedirect) throw IOException("Update metadata redirect rejected")
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    404 -> "Nessuna release pubblicata per LEVYRA"
                    403 -> "Controllo aggiornamenti temporaneamente limitato"
                    else -> "Controllo aggiornamenti non riuscito (${response.code})"
                }
                throw IllegalStateException(message)
            }
            if (!updateMetadataContentTypeAccepted(response.header("Content-Type"))) {
                throw IOException("Unexpected update metadata content type")
            }
            val body = readUpdateMetadataBody(response.body)
            return JSONObject(body)
        }
    }

    private fun parseLatestRelease(root: JSONObject): InstallableAppUpdate {
        val latestTag = root.optString("tag_name").ifBlank { root.optString("name") }
        val latestVersion = normalizeDisplayVersion(latestTag)
        val releaseUrl = root.optString("html_url")
        val assets = releaseAssets(root)
        val selected = LevyraUpdateSelector.selectArtifact(
            artifacts = assets.map(ReleaseAsset::toNexusArtifact),
            sdk = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.orEmpty().toList()
        )
        val assetDownloadUrl = selected?.downloadUrl.orEmpty()
        val directApk = selected?.isApk == true && assetDownloadUrl.isNotBlank()
        val downloadUrl = if (directApk && BuildConfig.UPSTREAM_UPDATES_ENABLED) {
            AppUpdateContract.INSTALL_URI
        } else {
            assetDownloadUrl.ifBlank { releaseUrl }
        }
        val assetName = selected?.name.orEmpty()
        val releaseTitle = root.optString("name").ifBlank { "LEVYRA $latestVersion" }
        val notes = root.optString("body").trim()
        val current = BuildConfig.VERSION_NAME
        val info = AppUpdateInfo(
            currentVersionName = current,
            latestVersionName = latestVersion,
            latestTag = latestTag.ifBlank { latestVersion },
            releaseTitle = releaseTitle,
            releaseNotes = notes,
            publishedAtEpochMs = parseUpdatePublishedAt(root.optString("published_at")),
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl.ifBlank { assetDownloadUrl },
            assetName = assetName,
            directApk = directApk,
            isNewer = LevyraVersionComparator.compare(latestVersion, current) > 0,
            assetSizeBytes = selected?.sizeBytes?.coerceAtLeast(0L) ?: 0L
        )
        return InstallableAppUpdate(
            info = info,
            assetDownloadUrl = assetDownloadUrl,
            assetSizeBytes = selected?.sizeBytes?.coerceAtLeast(0L) ?: 0L
        )
    }

    private fun releaseAssets(root: JSONObject): List<ReleaseAsset> {
        val assets = root.optJSONArray("assets") ?: return emptyList()
        return buildList {
            for (index in 0 until assets.length()) {
                val item = assets.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val url = item.optString("browser_download_url").trim()
                if (url.isBlank()) continue
                val contentType = item.optString("content_type").trim()
                val directApk = name.endsWith(".apk", ignoreCase = true) ||
                    contentType.contains("android.package-archive", ignoreCase = true)
                add(
                    ReleaseAsset(
                        name = name,
                        downloadUrl = url,
                        directApk = directApk,
                        contentType = contentType,
                        sizeBytes = item.optLong("size", 0L).coerceAtLeast(0L)
                    )
                )
            }
        }
    }

    private fun normalizeDisplayVersion(value: String): String {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        val match = Regex("\\d+(?:\\.\\d+){0,3}(?:[-+][0-9A-Za-z.-]+)?").find(clean)?.value
        return match ?: clean.ifBlank { BuildConfig.VERSION_NAME }
    }

    private data class ReleaseAsset(
        val name: String,
        val downloadUrl: String,
        val directApk: Boolean,
        val contentType: String,
        val sizeBytes: Long
    ) {
        fun toNexusArtifact(): LevyraUpdateArtifact = LevyraUpdateArtifact(
            name = name,
            downloadUrl = downloadUrl,
            sizeBytes = sizeBytes,
            contentType = contentType,
            abi = LevyraUpdateSelector.inferAbi(name)
        )
    }
}

internal const val MAX_UPDATE_METADATA_BYTES = 1_048_576L
internal const val UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS = 5L
internal const val UPDATE_METADATA_READ_TIMEOUT_SECONDS = 10L
internal const val UPDATE_METADATA_CALL_TIMEOUT_SECONDS = 12L
private const val UPDATE_METADATA_HOST = "api.github.com"
private const val UPDATE_METADATA_PATH = "/repos/LUC4N3X/Levyra-deepsound/releases/latest"

internal fun validateLevyraReleaseMetadataUrl(value: String): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return null
    if (!url.host.equals(UPDATE_METADATA_HOST, ignoreCase = true)) return null
    if (url.encodedPath != UPDATE_METADATA_PATH || url.query != null) return null
    return url
}

internal fun updateMetadataContentTypeAccepted(value: String?): Boolean {
    val mime = value?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return mime == "application/json" || mime == "application/vnd.github+json"
}

internal fun readUpdateMetadataBody(body: ResponseBody): String {
    val declaredLength = body.contentLength()
    if (declaredLength > MAX_UPDATE_METADATA_BYTES) throw IOException("Update metadata is too large")
    val source = body.source()
    source.request(MAX_UPDATE_METADATA_BYTES + 1L)
    if (source.buffer.size > MAX_UPDATE_METADATA_BYTES) throw IOException("Update metadata exceeded size limit")
    return source.buffer.readUtf8()
}

internal fun parseUpdatePublishedAt(raw: String): Long {
    val value = raw.trim()
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .getOrDefault(0L)
        .coerceAtLeast(0L)
}
