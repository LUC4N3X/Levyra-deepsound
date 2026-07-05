package com.luc4n3x.levyra.widget

object LevyraWidgetBridge {
    @Volatile
    var onToggle: (() -> Unit)? = null

    @Volatile
    var onNext: (() -> Unit)? = null

    @Volatile
    var onPrevious: (() -> Unit)? = null

    fun clear() {
        onToggle = null
        onNext = null
        onPrevious = null
    }
}
