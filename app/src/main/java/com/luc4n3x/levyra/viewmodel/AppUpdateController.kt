package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.AppUpdateRepository
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppUpdateController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val appUpdateRepository: AppUpdateRepository
) {
    private var updateJob: Job? = null
    private var sessionDismissedUpdateVersion: String? = null

    fun checkForUpdates(silent: Boolean = false) {
        if (!BuildConfig.UPSTREAM_UPDATES_ENABLED) return
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            uiState.update {
                it.copy(
                    isCheckingUpdates = true,
                    updateMessage = if (silent) it.updateMessage else null
                )
            }
            val result = runCatching { appUpdateRepository.latest() }
            result.onSuccess { info ->
                val dismissed = sessionDismissedUpdateVersion
                uiState.update {
                    it.copy(
                        updateInfo = info,
                        isCheckingUpdates = false,
                        showUpdatePrompt = info.isNewer && (!silent || dismissed != info.latestVersionName),
                        updateMessage = when {
                            silent -> null
                            info.isNewer -> LevyraStrings.forCode(it.languageCode).appUpdateAvailable(info.latestVersionName)
                            else -> LevyraStrings.forCode(it.languageCode).appUpToDate
                        }
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                uiState.update {
                    it.copy(
                        isCheckingUpdates = false,
                        updateMessage = if (silent) null else LevyraStrings.forCode(it.languageCode).appUpdateCheckFailed
                    )
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        sessionDismissedUpdateVersion = uiState.value.updateInfo?.latestVersionName
        uiState.update { it.copy(showUpdatePrompt = false) }
    }

    fun clearUpdateMessage() {
        uiState.update { it.copy(updateMessage = null) }
    }
}
