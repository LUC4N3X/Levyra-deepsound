package com.luc4n3x.levyra.domain

object LevyraYtMusicPairing {
    private val pairs: Map<NavStyle, MiniPlayerStyle> = mapOf(
        NavStyle.YtMusic to MiniPlayerStyle.YtMusic,
        NavStyle.ConnectedDock to MiniPlayerStyle.Connected
    )

    private fun navPartner(mini: MiniPlayerStyle): NavStyle? = pairs.entries.firstOrNull { it.value == mini }?.key

    fun miniStyleFor(previous: NavStyle, next: NavStyle, current: MiniPlayerStyle): MiniPlayerStyle {
        pairs[next]?.let { return it }
        return if (current == pairs[previous]) MiniPlayerStyle.Original else current
    }

    fun navStyleFor(previous: MiniPlayerStyle, next: MiniPlayerStyle, current: NavStyle): NavStyle {
        navPartner(next)?.let { return it }
        return if (current == navPartner(previous)) NavStyle.Original else current
    }
}
