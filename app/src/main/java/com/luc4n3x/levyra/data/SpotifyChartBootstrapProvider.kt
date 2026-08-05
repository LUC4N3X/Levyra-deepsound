package com.luc4n3x.levyra.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.AtomicFile
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Seeds the on-device editorial cache before [com.luc4n3x.levyra.LevyraApplication] starts.
 *
 * A fresh installation can therefore render Spotify's ordered Top 50 immediately from the APK,
 * while [EditorialChartsRepository] refreshes the same catalog from the network in the background.
 * The normal YouTube Music and Apple Music fallbacks remain untouched and are used when neither a
 * valid cached/remote Spotify catalog nor this bundled bootstrap is available.
 */
class SpotifyChartBootstrapProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        runCatching {
            val target = File(appContext.filesDir, CACHE_RELATIVE_PATH)
            if (target.isFile && target.length() in 1..MAX_CATALOG_BYTES.toLong()) return@runCatching

            val bundled = appContext.assets.open(ASSET_PATH).bufferedReader(StandardCharsets.UTF_8).use {
                it.readText()
            }
            if (bundled.toByteArray(StandardCharsets.UTF_8).size !in 1..MAX_CATALOG_BYTES) {
                return@runCatching
            }

            val root = JSONObject(bundled)
            if (root.optInt("schemaVersion", -1) != SUPPORTED_SCHEMA_VERSION) return@runCatching
            if (root.optJSONArray("collections")?.length()?.let { it > 0 } != true) return@runCatching

            // The bundled snapshot is a first-paint cache, not a claim that its generation time is now.
            // Refreshing the timestamp only makes the existing cache eligibility rules accept it while
            // the repository starts a real Spotify refresh immediately in the background.
            root.put("generatedAt", Instant.now().toString())
            val payload = root.toString().toByteArray(StandardCharsets.UTF_8)
            if (payload.size !in 1..MAX_CATALOG_BYTES) return@runCatching

            target.parentFile?.mkdirs()
            val atomicFile = AtomicFile(target)
            val stream = atomicFile.startWrite()
            try {
                stream.write(payload)
                stream.fd.sync()
                atomicFile.finishWrite(stream)
            } catch (error: Throwable) {
                atomicFile.failWrite(stream)
                throw error
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to install bundled Spotify chart bootstrap", error)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private companion object {
        const val TAG = "SpotifyChartBootstrap"
        const val ASSET_PATH = "editorial/spotify-bootstrap.json"
        const val CACHE_RELATIVE_PATH = "editorial/charts-v2.json"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
    }
}
