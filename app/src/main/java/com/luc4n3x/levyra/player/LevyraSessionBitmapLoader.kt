package com.luc4n3x.levyra.player

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.luc4n3x.levyra.data.ArtworkTier
import com.luc4n3x.levyra.data.ArtworkTierOverride
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.locallibrary.isLocalArtworkModel
import com.luc4n3x.levyra.feature.radio.LiveRadioArtworkLoader
import com.luc4n3x.levyra.feature.radio.RadioUrlPolicy
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val EXTRA_ARTWORK_FALLBACKS = "levyra.artworkFallbacks"

private const val LOW_RAM_ARTWORK_LIMIT_PX = 512
private const val MAX_PARKED_ARTWORK_LOADS = 4
internal val ARTWORK_RETRY_DELAYS_MS = listOf(1_500L, 4_000L, 10_000L)

internal fun artworkFallbacks(primary: String, thumbnail: String, youtubeFallback: String?): ArrayList<String> {
    val primaryIsLocal = isLocalArtworkModel(primary)
    return listOfNotNull(primary, thumbnail, youtubeFallback)
        .map(String::trim)
        .filter { it.isNotBlank() && (it != primary.trim() || primaryIsLocal) }
        .distinct()
        .toCollection(ArrayList())
}

internal fun sessionArtworkModels(
    artworkData: ByteArray?,
    artworkUri: String?,
    fallbacks: List<String>,
    liveRadio: Boolean
): List<Any> {
    val (local, remote) = fallbacks.partition(::isLocalArtworkModel)
    val urls = (local + listOfNotNull(artworkUri) + remote)
        .distinct()
        .filter { !liveRadio || RadioUrlPolicy.isAllowed(it) }
    return listOfNotNull(artworkData) + urls
}

internal fun isTransientArtworkFailure(error: Throwable): Boolean =
    error is IOException && error !is FileNotFoundException

private fun sameArtworkModel(first: Any, second: Any): Boolean =
    if (first is ByteArray && second is ByteArray) first.contentEquals(second) else first == second

internal suspend fun <T : Any> firstLoadedArtwork(
    retryDelaysMs: List<Long> = ARTWORK_RETRY_DELAYS_MS,
    awaitLateCandidates: Boolean = false,
    candidates: suspend () -> List<Any>,
    load: suspend (Any) -> T
): T {
    val conclusive = ArrayList<Any>()
    var lastFailure: Exception? = null
    var pass = 0
    while (true) {
        var transient = false
        for (model in candidates()) {
            if (conclusive.any { sameArtworkModel(it, model) }) continue
            try {
                return load(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastFailure = error
                if (isTransientArtworkFailure(error)) transient = true else conclusive += model
            }
        }
        if (pass >= retryDelaysMs.size || !(transient || awaitLateCandidates)) {
            throw lastFailure ?: FileNotFoundException("No artwork")
        }
        delay(retryDelaysMs[pass++])
    }
}

private class MetadataArtworkKey(private val models: List<Any>) {
    override fun equals(other: Any?): Boolean =
        other is MetadataArtworkKey &&
            models.size == other.models.size &&
            models.indices.all { sameArtworkModel(models[it], other.models[it]) }

    override fun hashCode(): Int =
        models.fold(1) { hash, model -> 31 * hash + if (model is ByteArray) model.contentHashCode() else model.hashCode() }
}

private class MetadataArtworkLoad(val future: SettableFuture<Bitmap>) {
    var job: Job? = null
    var parked = false
}

private fun ListenableFuture<*>.failedOrCancelled(): Boolean =
    isDone && (isCancelled || runCatching { Futures.getDone(this) }.isFailure)

@UnstableApi
internal class LevyraSessionBitmapLoader(
    private val scope: CoroutineScope,
    private val retryDelaysMs: List<Long> = ARTWORK_RETRY_DELAYS_MS,
    private val currentArtworkData: suspend (artworkUri: String) -> ByteArray? = { null },
    private val loadModel: suspend (model: Any, liveRadio: Boolean) -> Bitmap
) : BitmapLoader {
    private val metadataLoads = LinkedHashMap<MetadataArtworkKey, MetadataArtworkLoad>()

    override fun supportsMimeType(mimeType: String): Boolean = MimeTypes.isImage(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = load(listOf(data))

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = load(listOf(uri.toString()))

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        val extras = metadata.extras
        val liveRadio = metadata.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION ||
            extras?.getBoolean(PlaybackService.EXTRA_LIVE_RADIO, false) == true
        val artworkUri = metadata.artworkUri?.toString()
        val models = sessionArtworkModels(
            artworkData = metadata.artworkData,
            artworkUri = artworkUri,
            fallbacks = extras?.getStringArrayList(EXTRA_ARTWORK_FALLBACKS).orEmpty(),
            liveRadio = liveRadio
        )
        val lateDataUri = artworkUri?.takeIf { metadata.artworkData == null && !liveRadio }
        return loadForMetadata(models, liveRadio, lateDataUri)
    }

    private fun loadForMetadata(models: List<Any>, liveRadio: Boolean, lateDataUri: String?): ListenableFuture<Bitmap> {
        if (models.isEmpty() && lateDataUri == null) return noArtwork()
        val key = MetadataArtworkKey(models)
        synchronized(metadataLoads) {
            val reusable = metadataLoads.remove(key)?.takeUnless { it.future.failedOrCancelled() }
            metadataLoads.values.forEach(::park)
            metadataLoads.values.removeAll { it.future.isDone }
            while (metadataLoads.size >= MAX_PARKED_ARTWORK_LOADS) {
                val evicted = metadataLoads.values.first()
                metadataLoads.values.remove(evicted)
                evicted.future.cancel(false)
            }
            val load = reusable ?: MetadataArtworkLoad(SettableFuture.create())
            metadataLoads[key] = load
            if (!load.future.isDone && load.job?.isActive != true) {
                load.parked = false
                val candidates: suspend () -> List<Any> = {
                    lateDataUri?.let { uri -> listOfNotNull<Any>(lateArtworkData(uri)) + models } ?: models
                }
                launch(load.future, liveRadio, lateDataUri != null, candidates, owner = load)
            }
            return load.future
        }
    }

    private fun park(load: MetadataArtworkLoad) {
        if (load.future.isDone) return
        load.parked = true
        load.job?.cancel()
    }

    private suspend fun lateArtworkData(artworkUri: String): ByteArray? = try {
        currentArtworkData(artworkUri)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun load(models: List<Any>): ListenableFuture<Bitmap> {
        if (models.isEmpty()) return noArtwork()
        val future = SettableFuture.create<Bitmap>()
        launch(future, liveRadio = false, awaitLateCandidates = false, candidates = { models }, owner = null)
        return future
    }

    private fun noArtwork(): ListenableFuture<Bitmap> =
        Futures.immediateFailedFuture(FileNotFoundException("No artwork"))

    private fun launch(
        future: SettableFuture<Bitmap>,
        liveRadio: Boolean,
        awaitLateCandidates: Boolean,
        candidates: suspend () -> List<Any>,
        owner: MetadataArtworkLoad?
    ) {
        val job = scope.launch(Dispatchers.Default, CoroutineStart.LAZY) {
            try {
                future.set(
                    firstLoadedArtwork(retryDelaysMs, awaitLateCandidates, candidates) { model -> loadModel(model, liveRadio) }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                future.setException(error)
            }
        }
        job.invokeOnCompletion {
            val superseded = owner != null && synchronized(metadataLoads) { owner.parked || owner.job !== job }
            if (!superseded && !future.isDone) future.cancel(false)
        }
        owner?.job = job
        job.start()
    }

    companion object {
        fun create(context: Context, scope: CoroutineScope): LevyraSessionBitmapLoader {
            val appContext = context.applicationContext
            LevyraArtworkCache.configure(appContext)
            val lowRam = appContext.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
            val sessionLimit = MediaSession.getBitmapDimensionLimit(appContext)
            val limitPx = if (lowRam) minOf(sessionLimit, LOW_RAM_ARTWORK_LIMIT_PX) else sessionLimit
            val service = context as? MediaSessionService
            return LevyraSessionBitmapLoader(
                scope = scope,
                currentArtworkData = { artworkUri -> service?.let { sessionArtworkData(it, artworkUri) } }
            ) { model, liveRadio ->
                val imageLoader: ImageLoader = if (liveRadio) {
                    LiveRadioArtworkLoader.get(appContext)
                } else {
                    SingletonImageLoader.get(appContext)
                }
                val request = ImageRequest.Builder(appContext)
                    .data(model)
                    .size(limitPx)
                    .scale(Scale.FIT)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .apply { extras.set(ArtworkTierOverride, ArtworkTier.Full) }
                    .build()
                when (val result = imageLoader.execute(request)) {
                    is SuccessResult -> result.image.toBitmap()
                    is ErrorResult -> throw (result.throwable as? Exception ?: IllegalStateException(result.throwable))
                }
            }
        }

        private suspend fun sessionArtworkData(service: MediaSessionService, artworkUri: String): ByteArray? {
            val player = service.sessions.firstOrNull()?.player ?: return null
            return withContext(Handler(player.applicationLooper).asCoroutineDispatcher()) {
                player.mediaMetadata.takeIf { it.artworkUri?.toString() == artworkUri }?.artworkData
            }
        }
    }
}
