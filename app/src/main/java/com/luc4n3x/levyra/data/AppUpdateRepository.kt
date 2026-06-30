package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class AppUpdateRepository(context: Context) {
    private val client = LevyraHttpClientFactory.general(context.applicationContext)

    suspend fun latest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.UPDATE_LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    404 -> "Nessuna release pubblicata per LEVYRA"
                    403 -> "Controllo aggiornamenti temporaneamente limitato"
                    else -> "Controllo aggiornamenti non riuscito (${response.code})"
                }
                throw IllegalStateException(message)
            }
            parseLatestRelease(JSONObject(body))
        }
    }

    private fun parseLatestRelease(root: JSONObject): AppUpdateInfo {
        val latestTag = root.optString("tag_name").ifBlank { root.optString("name") }
        val latestVersion = normalizeDisplayVersion(latestTag)
        val releaseUrl = root.optString("html_url")
        val asset = bestDownloadAsset(root)
        val downloadUrl = asset?.downloadUrl?.ifBlank { releaseUrl } ?: releaseUrl
        val releaseTitle = root.optString("name").ifBlank { "LEVYRA $latestVersion" }
        val notes = root.optString("body").trim()
        val current = BuildConfig.VERSION_NAME
        return AppUpdateInfo(
            currentVersionName = current,
            latestVersionName = latestVersion,
            latestTag = latestTag.ifBlank { latestVersion },
            releaseTitle = releaseTitle,
            releaseNotes = notes,
            publishedAt = root.optString("published_at"),
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl.ifBlank { downloadUrl },
            assetName = asset?.name.orEmpty(),
            directApk = asset?.directApk ?: false,
            isNewer = compareVersions(latestVersion, current) > 0
        )
    }

    private fun bestDownloadAsset(root: JSONObject): ReleaseAsset? {
        val assets = root.optJSONArray("assets") ?: return null
        val parsed = buildList {
            for (index in 0 until assets.length()) {
                val item = assets.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val url = item.optString("browser_download_url").trim()
                if (url.isBlank()) continue
                val contentType = item.optString("content_type").lowercase()
                val directApk = name.endsWith(".apk", ignoreCase = true) || contentType.contains("android.package-archive")
                add(ReleaseAsset(name, url, directApk))
            }
        }
        return parsed.firstOrNull { it.directApk && it.name.contains("release", ignoreCase = true) }
            ?: parsed.firstOrNull { it.directApk }
            ?: parsed.firstOrNull()
    }

    private fun normalizeDisplayVersion(value: String): String {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        val match = Regex("\\d+(?:\\.\\d+){0,3}").find(clean)?.value
        return match ?: clean.ifBlank { BuildConfig.VERSION_NAME }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = numericParts(left)
        val b = numericParts(right)
        val size = maxOf(a.size, b.size, 1)
        for (index in 0 until size) {
            val av = a.getOrNull(index) ?: 0
            val bv = b.getOrNull(index) ?: 0
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    private fun numericParts(value: String): List<Int> {
        return Regex("\\d+")
            .findAll(value)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
    }

    private data class ReleaseAsset(
        val name: String,
        val downloadUrl: String,
        val directApk: Boolean
    )
}
