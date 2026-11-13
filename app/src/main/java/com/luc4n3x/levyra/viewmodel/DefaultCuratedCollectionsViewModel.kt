package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.CURATED_COLLECTION_TTL_MS
import com.luc4n3x.levyra.data.CuratedCollection
import com.luc4n3x.levyra.data.CuratedCollectionCard
import com.luc4n3x.levyra.data.YoutubeMusicCuratedCollectionRepository
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultCuratedCollectionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicCuratedCollectionRepository(application, YoutubeMusicRepository(application))
    private val _collections = MutableStateFlow<Map<CuratedCollection, List<CuratedCollectionCard>>>(emptyMap())
    val collections: StateFlow<Map<CuratedCollection, List<CuratedCollectionCard>>> = _collections.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private var language = ""
    private var requestedAtMs = 0L
    private var job: Job? = null

    fun load(languageCode: String) {
        val now = System.currentTimeMillis()
        val sameLanguage = language == languageCode
        if (sameLanguage && (job?.isActive == true || now - requestedAtMs in 0 until CURATED_COLLECTION_TTL_MS)) return
        job?.cancel()
        if (!sameLanguage) _collections.value = emptyMap()
        language = languageCode
        requestedAtMs = now
        _loading.value = true
        job = viewModelScope.launch {
            try {
                coroutineScope {
                    CuratedCollection.entries.forEach { collection ->
                        launch {
                            try {
                                val cached = repository.cached(collection, languageCode)
                                if (cached != null) publish(languageCode, collection, cached.cards)
                                if (cached?.isFresh() == true) return@launch
                                val fresh = repository.refresh(collection, languageCode)
                                if (fresh != null) publish(languageCode, collection, fresh)
                                else if (language == languageCode) requestedAtMs = 0L
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Throwable) {
                                Timber.w(error, "Curated collection load failed for %s", collection)
                                if (language == languageCode) requestedAtMs = 0L
                            }
                        }
                    }
                }
            } finally {
                if (language == languageCode) _loading.value = false
            }
        }
    }

    private fun publish(languageCode: String, collection: CuratedCollection, cards: List<CuratedCollectionCard>) {
        if (language != languageCode) return
        _collections.update { it + (collection to cards) }
    }
}
