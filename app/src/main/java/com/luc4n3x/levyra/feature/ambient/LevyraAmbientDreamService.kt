package com.luc4n3x.levyra.feature.ambient

import android.os.Bundle
import android.service.dreams.DreamService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.ambient.AmbientScreen
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class LevyraAmbientDreamService :
    DreamService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var presenter: AmbientSessionPresenter? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = false

        val sessionPresenter = AmbientSessionPresenter(this, scope)
        presenter = sessionPresenter
        sessionPresenter.connect()

        val strings = LevyraStrings.forCode(LevyraPreferences(this).snapshot().languageCode)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LevyraAmbientDreamService)
            setViewTreeViewModelStoreOwner(this@LevyraAmbientDreamService)
            setViewTreeSavedStateRegistryOwner(this@LevyraAmbientDreamService)
            setContent {
                val state by sessionPresenter.state.collectAsStateWithLifecycle()
                LevyraTheme {
                    CompositionLocalProvider(LocalLevyraStrings provides strings) {
                        AmbientScreen(
                            state = state,
                            onTogglePlay = sessionPresenter::togglePlay,
                            onNext = sessionPresenter::next,
                            onPrevious = sessionPresenter::previous,
                            onExit = null
                        )
                    }
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        presenter?.release()
        presenter = null
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        scope.cancel()
        super.onDestroy()
    }
}
