package com.luc4n3x.levyra.domain

enum class FloatingNavSide(val storageId: String) {
    Left("left"),
    Right("right");

    val opposite: FloatingNavSide get() = if (this == Left) Right else Left

    companion object {
        fun from(value: String?): FloatingNavSide = entries.firstOrNull { it.storageId == value } ?: Right
    }
}

data class LevyraFloatingNavSettings(
    val enabled: Boolean = false,
    val side: FloatingNavSide = FloatingNavSide.Right,
    val anchor: Float = DEFAULT_ANCHOR
) {
    fun normalized(): LevyraFloatingNavSettings =
        copy(anchor = if (anchor.isFinite()) anchor.coerceIn(0f, 1f) else DEFAULT_ANCHOR)

    companion object {
        const val DEFAULT_ANCHOR = 0.42f
    }
}
