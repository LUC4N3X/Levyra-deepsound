package com.luc4n3x.levyra.extraction.audio

sealed interface AudioResolution<out T> {
    class Resolved<T>(val stream: T) : AudioResolution<T> {
        override fun toString(): String = "Resolved(stream=<redacted>)"
    }

    data object NoPlayableStream : AudioResolution<Nothing>

    data object Inconclusive : AudioResolution<Nothing>
}
