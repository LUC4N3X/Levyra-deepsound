package com.luc4n3x.levyra.player

object LevyraPipBridge {
    data class State(
        val videoMode: Boolean = false,
        val playing: Boolean = false,
        val aspectRatio: Float = 16f / 9f,
        val inPictureInPicture: Boolean = false
    ) {
        val canEnter: Boolean
            get() = videoMode && !inPictureInPicture
    }

    @Volatile
    private var state = State()

    @Volatile
    private var enterAction: (() -> Boolean)? = null

    @Volatile
    private var updateAction: ((State) -> Unit)? = null

    fun bind(enter: () -> Boolean, update: (State) -> Unit) {
        enterAction = enter
        updateAction = update
        update(state)
    }

    fun unbind() {
        enterAction = null
        updateAction = null
    }

    fun updatePlayback(videoMode: Boolean, playing: Boolean, aspectRatio: Float) {
        val safeAspect = aspectRatio.takeIf { it.isFinite() && it in 0.42f..2.39f } ?: 16f / 9f
        state = state.copy(videoMode = videoMode, playing = playing, aspectRatio = safeAspect)
        updateAction?.invoke(state)
    }

    fun updatePictureInPictureMode(active: Boolean) {
        state = state.copy(inPictureInPicture = active)
        updateAction?.invoke(state)
    }

    fun current(): State = state

    fun enter(): Boolean {
        if (!state.canEnter) return false
        return enterAction?.invoke() == true
    }
}
