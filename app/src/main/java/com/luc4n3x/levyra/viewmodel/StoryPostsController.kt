package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.data.storypost.StoryPost
import com.luc4n3x.levyra.data.storypost.StoryPostDraft
import com.luc4n3x.levyra.data.storypost.StoryPostImportError
import com.luc4n3x.levyra.data.storypost.StoryPostImportException
import com.luc4n3x.levyra.data.storypost.StoryPostRepository
import com.luc4n3x.levyra.data.storypost.StoryPostSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface StoryPostImportState {
    data object Idle : StoryPostImportState
    data object Reading : StoryPostImportState
    data class Preview(
        val draft: StoryPostDraft,
        val selection: StoryPostSelection,
        val saving: Boolean = false
    ) : StoryPostImportState
    data class Failed(val error: StoryPostImportError) : StoryPostImportState
}

class StoryPostsController(context: Context, private val scope: CoroutineScope) {
    private val repository = StoryPostRepository.get(context)
    private val _importState = MutableStateFlow<StoryPostImportState>(StoryPostImportState.Idle)
    private var importJob: Job? = null

    val posts: StateFlow<List<StoryPost>> = repository.posts
    val importState: StateFlow<StoryPostImportState> = _importState.asStateFlow()

    fun importFolder(treeUri: Uri) = inspect { repository.inspectFolder(treeUri) }

    fun importZip(uri: Uri) = inspect { repository.inspectZip(uri) }

    fun select(selection: StoryPostSelection) {
        _importState.update { state ->
            if (state is StoryPostImportState.Preview && !state.saving) state.copy(selection = selection) else state
        }
    }

    fun save() {
        val preview = _importState.value as? StoryPostImportState.Preview ?: return
        if (preview.saving) return
        _importState.value = preview.copy(saving = true)
        importJob = scope.launch {
            _importState.value = try {
                repository.save(preview.draft, preview.selection)
                StoryPostImportState.Idle
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                repository.discard(preview.draft)
                StoryPostImportState.Failed(failureOf(error))
            }
        }
    }

    fun dismiss() {
        val state = _importState.value
        if (state is StoryPostImportState.Preview && state.saving) return
        importJob?.cancel()
        if (state is StoryPostImportState.Preview) repository.discard(state.draft)
        _importState.value = StoryPostImportState.Idle
    }

    fun delete(postId: String) {
        scope.launch { repository.delete(postId) }
    }

    private fun inspect(block: suspend () -> StoryPostDraft) {
        val previous = _importState.value
        if (previous is StoryPostImportState.Preview) {
            if (previous.saving) return
            repository.discard(previous.draft)
        }
        importJob?.cancel()
        _importState.value = StoryPostImportState.Reading
        importJob = scope.launch {
            _importState.value = try {
                val draft = block()
                val plan = draft.plan
                StoryPostImportState.Preview(
                    draft = draft,
                    selection = StoryPostSelection(
                        audioPath = plan.defaultAudio?.path,
                        videoPath = plan.defaultVideo?.path,
                        captionPath = plan.defaultCaption?.path
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                StoryPostImportState.Failed(failureOf(error))
            }
        }
    }

    private fun failureOf(error: Throwable): StoryPostImportError {
        if (error is StoryPostImportException) return error.reason
        Timber.w(error, "Story post import failed")
        return if (error is java.util.zip.ZipException) StoryPostImportError.InvalidArchive else StoryPostImportError.Unreadable
    }
}
