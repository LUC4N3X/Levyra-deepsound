package com.luc4n3x.levyra.ui

/** Keeps expensive decorative motion out of an active home scroll. */
internal fun shouldAnimateHomeBackdrop(animationsEnabled: Boolean, isScrolling: Boolean): Boolean {
    return animationsEnabled && !isScrolling
}
