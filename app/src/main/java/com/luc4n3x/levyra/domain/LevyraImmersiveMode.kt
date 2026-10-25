package com.luc4n3x.levyra.domain

data class LevyraImmersiveSettings(
    val hideStatusBar: Boolean = false,
    val hideNavigationBar: Boolean = false,
    val fullImmersive: Boolean = false,
    val everywhere: Boolean = false
) {
    val statusBarHidden: Boolean get() = fullImmersive || hideStatusBar
    val navigationBarHidden: Boolean get() = fullImmersive || hideNavigationBar

    fun hiddenBars(fullPlayerVisible: Boolean): ImmersiveBars =
        if (fullPlayerVisible || everywhere) {
            ImmersiveBars(status = statusBarHidden, navigation = navigationBarHidden)
        } else {
            ImmersiveBars.None
        }
}

data class ImmersiveBars(val status: Boolean, val navigation: Boolean) {
    val any: Boolean get() = status || navigation

    companion object {
        val None = ImmersiveBars(status = false, navigation = false)
        val All = ImmersiveBars(status = true, navigation = true)
    }
}
