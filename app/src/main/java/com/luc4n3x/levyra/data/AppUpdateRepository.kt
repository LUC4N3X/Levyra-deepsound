package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Build
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.nexus.update.LevyraUpdateArtifact
import com.luc4n3x.levyra.nexus.update.LevyraUpdateSelector
import com.luc4n3x.levyra.nexus.update.LevyraVersionComparator
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
            val body = response.body.string()
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
        val assets = releaseAssets(root)
        val selected = LevyraUpdateSelector.selectArtifact(
            artifacts = assets.map(ReleaseAsset::toNexusArtifact),
            sdk = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.orEmpty().toList()
        )
        val fallback = assets.firstOrNull { it.directApk } ?: assets.firstOrNull()
        val downloadUrl = selected?.downloadUrl?.ifBlank { releaseUrl }
            ?: fallback?.downloadUrl?.ifBlank { releaseUrl }
            ?: releaseUrl
        val assetName = selected?.name ?: fallback?.name.orEmpty()
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
            assetName = assetName,
            directApk = selected?.isApk ?: fallback?.directApk ?: false,
            isNewer = LevyraVersionComparator.compare(latestVersion, current) > 0
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
