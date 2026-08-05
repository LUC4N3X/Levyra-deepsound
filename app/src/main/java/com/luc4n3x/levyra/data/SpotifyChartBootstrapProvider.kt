package com.luc4n3x.levyra.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

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
        runCatching { installIfNeeded(appContext) }
            .onFailure { error ->
                Log.w(TAG, "Unable to install bundled Spotify chart bootstrap", error)
            }
        return true
    }

    private fun installIfNeeded(appContext: Context) {
        val target = File(appContext.filesDir, CACHE_RELATIVE_PATH)
        val marker = appContext.getSharedPreferences(BOOTSTRAP_STATE, Context.MODE_PRIVATE)
        val cachePresent = target.isFile && target.length() in 1..MAX_CATALOG_BYTES.toLong()
        if (cachePresent && marker.getInt(KEY_BOOTSTRAP_VERSION, 0) >= BOOTSTRAP_VERSION) return

        val bundled = appContext.assets.open(ASSET_PATH).bufferedReader(StandardCharsets.UTF_8).use {
            it.readText()
        }
        if (bundled.toByteArray(StandardCharsets.UTF_8).size !in 1..MAX_CATALOG_BYTES) return

        val root = JSONObject(bundled)
        if (root.optInt("schemaVersion", -1) != SUPPORTED_SCHEMA_VERSION) return
        if (root.optJSONArray("collections")?.length()?.let { it > 0 } != true) return

        val now = System.currentTimeMillis()
        if (!cachePresent) {
            // The bundled snapshot is a first-paint cache, not a claim that its source was generated now.
            // Refreshing this local timestamp only makes the existing cache eligibility rules accept it;
            // the real Spotify catalog refresh still starts immediately in the background.
            root.put("generatedAt", Instant.ofEpochMilli(now).toString())
            writeCatalog(target, root.toString().toByteArray(StandardCharsets.UTF_8))
        }

        seedArtworkCache(appContext, root, now)
        marker.edit().putInt(KEY_BOOTSTRAP_VERSION, BOOTSTRAP_VERSION).apply()
    }

    private fun writeCatalog(target: File, payload: ByteArray) {
        if (payload.size !in 1..MAX_CATALOG_BYTES) return
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
    }

    /**
     * The chart rows already ship with Spotify artwork and normalized metadata. Priming the resolver's
     * cache prevents its first-install enrichment pass from spending several seconds re-looking-up
     * those same 50 covers before Home can render.
     */
    private fun seedArtworkCache(appContext: Context, root: JSONObject, now: Long) {
        val collection = preferredCollection(root.optJSONArray("collections")) ?: return
        val tracks = collection.optJSONArray("tracks") ?: return
        val editor = appContext
            .getSharedPreferences(ARTWORK_CACHE_NAME, Context.MODE_PRIVATE)
            .edit()

        for (index in 0 until tracks.length()) {
            val item = tracks.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = parseArtists(item.optJSONArray("artists"))
            val artwork = item.optString("artworkUrl").trim()
            if (title.isBlank() || artist.isBlank() || !artwork.startsWith("https://")) continue

            val album = item.optJSONObject("album")
            val releaseDate = album?.optString("releaseDate").orEmpty().trim()
            val year = releaseDate.take(4).takeIf { value ->
                value.length == 4 && value.all(Char::isDigit)
            }.orEmpty()
            val provider = if (item.optJSONObject("youtubeMusic") != null) {
                "Levyra Editorial + YouTube Music"
            } else {
                "Levyra Editorial"
            }
            val cached = JSONObject()
                .put("thumbnailUrl", artwork)
                .put("largeThumbnailUrl", artwork)
                .put("album", album?.optString("name").orEmpty().trim())
                .put("provider", provider)
                .put("canonicalAlbumUrl", "")
                .put("releaseDate", releaseDate)
                .put("year", year)
                .put("trackNumber", 0)
                .put("discNumber", 0)
                .put("explicit", item.optBoolean("explicit", false))
                .put("isrc", item.optString("isrc").trim())
                .put("upc", "")
                .put("score", 500)
                .put("cachedAt", now)
            editor.putString("artwork.${artworkIdentity(title, artist)}", cached.toString())
        }
        // apply() updates the process-local SharedPreferences snapshot synchronously, so the resolver
        // sees these entries immediately while the disk flush happens off the startup path.
        editor.apply()
    }

    private fun preferredCollection(collections: JSONArray?): JSONObject? {
        if (collections == null) return null
        val deviceMarket = Locale.getDefault().country
            .trim()
            .uppercase(Locale.ROOT)
            .takeIf { it.length == 2 }
            ?: DEFAULT_MARKET
        var italy: JSONObject? = null
        var firstChart: JSONObject? = null
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            if (!collection.optString("kind").equals("chart", ignoreCase = true)) continue
            if (firstChart == null) firstChart = collection
            val market = collection.optString("market").trim().uppercase(Locale.ROOT)
            if (market == deviceMarket) return collection
            if (market == DEFAULT_MARKET) italy = collection
        }
        return italy ?: firstChart
    }

    private fun parseArtists(items: JSONArray?): String {
        if (items == null) return ""
        return buildList {
            for (index in 0 until items.length()) {
                val name = items.optJSONObject(index)?.optString("name").orEmpty().trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinct().joinToString(", ")
    }

    private fun artworkIdentity(title: String, artist: String): String {
        val normalized = listOf(
            ChartFeedParser.normalizeMusicText(title),
            ChartFeedParser.normalizeMusicText(artist)
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }
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
        const val ARTWORK_CACHE_NAME = "levyra_chart_official_artwork"
        const val BOOTSTRAP_STATE = "levyra_spotify_chart_bootstrap"
        const val KEY_BOOTSTRAP_VERSION = "version"
        const val BOOTSTRAP_VERSION = 1
        const val DEFAULT_MARKET = "IT"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
    }
}
