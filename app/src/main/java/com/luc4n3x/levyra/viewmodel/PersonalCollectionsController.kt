package com.luc4n3x.levyra.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.data.collections.CollectionSyncError
import com.luc4n3x.levyra.data.collections.PersonalCollectionImportSource
import com.luc4n3x.levyra.data.collections.PersonalCollectionPreview
import com.luc4n3x.levyra.data.collections.PersonalCollectionPreviewResult
import com.luc4n3x.levyra.data.collections.PersonalCollectionRepository
import com.luc4n3x.levyra.data.collections.PersonalCollectionSummary
import com.luc4n3x.levyra.data.collections.PersonalCollectionSyncReport
import com.luc4n3x.levyra.data.collections.PersonalCollectionSyncResult
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
sealed interface PersonalCollectionImportState {
    data object Idle : PersonalCollectionImportState
    data object Loading : PersonalCollectionImportState
    data class Preview(val preview: PersonalCollectionPreview) : PersonalCollectionImportState
    data class Failed(val error: CollectionSyncError) : PersonalCollectionImportState
    data object Saving : PersonalCollectionImportState
}

@Immutable
sealed interface PersonalCollectionEvent {
    data class Imported(val collectionId: String) : PersonalCollectionEvent
    data class Synced(val collectionId: String, val report: PersonalCollectionSyncReport) : PersonalCollectionEvent
    data class SyncFailed(val collectionId: String, val error: CollectionSyncError) : PersonalCollectionEvent
}

class PersonalCollectionsController(context: Context, private val scope: CoroutineScope) {
    private val repository = PersonalCollectionRepository.get(context)
    private val _importState = MutableStateFlow<PersonalCollectionImportState>(PersonalCollectionImportState.Idle)
    private val _syncingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _events = MutableSharedFlow<PersonalCollectionEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var importJob: Job? = null

    val summaries: StateFlow<List<PersonalCollectionSummary>> = repository.observeSummaries()
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    val importState: StateFlow<PersonalCollectionImportState> = _importState.asStateFlow()
    val syncingIds: StateFlow<Set<String>> = _syncingIds.asStateFlow()
    val events: SharedFlow<PersonalCollectionEvent> = _events.asSharedFlow()

    fun tracks(collectionId: String): Flow<List<Track>> = repository.observeTracks(collectionId)

    fun preview(source: PersonalCollectionImportSource, allowCleartext: Boolean) {
        importJob?.cancel()
        _importState.value = PersonalCollectionImportState.Loading
        importJob = scope.launch {
            _importState.value = when (val result = repository.preview(source, allowCleartext)) {
                is PersonalCollectionPreviewResult.Ready -> PersonalCollectionImportState.Preview(result.preview)
                is PersonalCollectionPreviewResult.Failed -> PersonalCollectionImportState.Failed(result.error)
            }
        }
    }

    fun confirmImport(name: String, autoRefresh: Boolean, allowCleartext: Boolean) {
        val preview = (_importState.value as? PersonalCollectionImportState.Preview)?.preview ?: return
        importJob?.cancel()
        _importState.value = PersonalCollectionImportState.Saving
        importJob = scope.launch {
            try {
                val id = repository.save(preview, name, autoRefresh, allowCleartext)
                _importState.value = PersonalCollectionImportState.Idle
                _events.tryEmit(PersonalCollectionEvent.Imported(id))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w("Personal collection save failed: %s", error.javaClass.simpleName)
                _importState.value = PersonalCollectionImportState.Failed(CollectionSyncError.Storage)
            }
        }
    }

    fun resetImport() {
        importJob?.cancel()
        importJob = null
        _importState.value = PersonalCollectionImportState.Idle
    }

    fun refresh(collectionId: String) {
        if (collectionId in _syncingIds.value) return
        _syncingIds.update { it + collectionId }
        scope.launch {
            try {
                when (val result = repository.refresh(collectionId)) {
                    is PersonalCollectionSyncResult.Synced ->
                        _events.tryEmit(PersonalCollectionEvent.Synced(collectionId, result.report))
                    is PersonalCollectionSyncResult.Failed ->
                        _events.tryEmit(PersonalCollectionEvent.SyncFailed(collectionId, result.error))
                    PersonalCollectionSyncResult.Busy -> Unit
                }
            } finally {
                _syncingIds.update { it - collectionId }
            }
        }
    }

    fun updateSettings(collectionId: String, name: String, autoRefresh: Boolean, allowCleartext: Boolean) {
        scope.launch { repository.updateSettings(collectionId, name, autoRefresh, allowCleartext) }
    }

    fun delete(collectionId: String) {
        scope.launch { repository.delete(collectionId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
