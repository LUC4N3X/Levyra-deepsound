package com.luc4n3x.levyra.ui.lyrics

internal class LyricsShareResource<T : Any>(private val release: (T) -> Unit) {
    private val lock = Any()
    private var value: T? = null
    private var users = 0
    private var closed = false

    fun set(newValue: T) {
        val rejected = synchronized(lock) {
            if (closed || value != null) {
                newValue
            } else {
                value = newValue
                null
            }
        }
        rejected?.let(release)
    }

    fun <R> use(block: (T?) -> R): R {
        val current = synchronized(lock) {
            if (closed) null else value?.also { users++ }
        }
        try {
            return block(current)
        } finally {
            if (current != null) {
                val idle = synchronized(lock) {
                    users--
                    takeIfIdleAndClosed()
                }
                idle?.let(release)
            }
        }
    }

    fun close() {
        val idle = synchronized(lock) {
            closed = true
            takeIfIdleAndClosed()
        }
        idle?.let(release)
    }

    private fun takeIfIdleAndClosed(): T? =
        if (closed && users == 0) value.also { value = null } else null
}

internal data class LyricsSharePreviewKey(
    val content: LyricsShareCardContent,
    val style: LyricsShareCardStyle
)

internal sealed interface LyricsSharePreviewState<out T> {
    val key: LyricsSharePreviewKey

    data class Ready<T>(override val key: LyricsSharePreviewKey, val image: T) : LyricsSharePreviewState<T>
    data class Failed(override val key: LyricsSharePreviewKey) : LyricsSharePreviewState<Nothing>
}

internal fun <T> LyricsSharePreviewState<T>?.readyFor(key: LyricsSharePreviewKey): T? = when (this) {
    is LyricsSharePreviewState.Ready -> if (this.key == key) image else null
    else -> null
}

internal fun LyricsSharePreviewState<*>?.failedFor(key: LyricsSharePreviewKey): Boolean =
    this is LyricsSharePreviewState.Failed && this.key == key
